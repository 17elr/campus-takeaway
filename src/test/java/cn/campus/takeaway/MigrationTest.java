package cn.campus.takeaway;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="RUN_MYSQL_TESTS",matches="true")
class MigrationTest {
    @Test void additiveUpgradePreservesLegacyIdsHashesOrdersSnapshotsAndEvents() {
        String original=System.getenv().getOrDefault("TEST_DB_URL","jdbc:mysql://127.0.0.1:13317/campus_takeaway_test?serverTimezone=Asia/Shanghai");
        assertTrue(original.matches("jdbc:mysql://[^/]+/campus_takeaway_test(?:\\?.*)?"),"Only the isolated test connection is allowed");
        String user=System.getenv().getOrDefault("TEST_DB_USERNAME","root"),password=System.getenv().getOrDefault("TEST_DB_PASSWORD","");
        String name="campus_takeaway_migration_"+UUID.randomUUID().toString().replace("-","").substring(0,12);
        var control=new JdbcTemplate(new DriverManagerDataSource(original,user,password));
        control.execute("CREATE DATABASE "+name+" CHARACTER SET utf8mb4");
        try {
            String url=original.replace("/campus_takeaway_test","/"+name);
            Flyway.configure().dataSource(url,user,password).target("1").load().migrate();
            var jdbc=new JdbcTemplate(new DriverManagerDataSource(url,user,password));
            String hash=new BCryptPasswordEncoder().encode("legacy123");
            jdbc.update("INSERT INTO users(id,username,password_hash,real_name,phone,role) VALUES(1,'legacy',?,'旧学生','13800000001','STUDENT'),(2,'old_shop',?,'旧商家','13800000002','MERCHANT')",hash,hash);
            jdbc.update("INSERT INTO shops(id,owner_id,name) VALUES(1,2,'原店铺')");
            jdbc.update("INSERT INTO dishes(id,shop_id,category,name,price) VALUES(1,1,'主食','原菜品',12.50)");
            jdbc.update("INSERT INTO orders(id,order_no,student_id,shop_id,amount,address_snapshot,status) VALUES(1,'LEGACY001',1,1,25.00,'一号宿舍楼 302 原联系人','COMPLETED')");
            jdbc.update("INSERT INTO order_items(id,order_id,dish_id,dish_name,unit_price,quantity,subtotal) VALUES(1,1,1,'下单时名称',12.50,2,25.00)");
            jdbc.update("INSERT INTO order_events(id,order_id,to_status,operator_id) VALUES(1,1,'COMPLETED',1)");
            var before=new LinkedHashMap<String,List<Map<String,Object>>>();
            for(String table:List.of("users","shops","dishes","orders","order_items","order_events")) before.put(table,jdbc.queryForList("SELECT * FROM "+table+" ORDER BY id"));
            Flyway.configure().dataSource(url,user,password).load().migrate();
            before.forEach((table,rows)->{
                String columns=String.join(",",rows.get(0).keySet());
                assertEquals(rows,jdbc.queryForList("SELECT "+columns+" FROM "+table+" ORDER BY id"),table);
            });
            assertNull(jdbc.queryForObject("SELECT request_key FROM orders WHERE id=1",String.class));
            assertEquals(0L,jdbc.queryForObject("SELECT auth_version FROM users WHERE id=1",Long.class));
            assertTrue(new BCryptPasswordEncoder().matches("legacy123",jdbc.queryForObject("SELECT password_hash FROM users WHERE id=1",String.class)));
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM cart",Integer.class));
            assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success=1",Integer.class));
        } finally { control.execute("DROP DATABASE "+name); }
    }
}
