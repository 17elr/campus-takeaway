package cn.campus.takeaway;

import cn.campus.takeaway.mapper.AppMapper;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;

@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
public class CampusTakeawayApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusTakeawayApplication.class, args);
    }

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.demo-data", havingValue="true")
    CommandLineRunner demoData(AppMapper mapper, PasswordEncoder encoder, DataSource dataSource) {
        return args -> {
            if (mapper.countUsers() > 0) return;
            mapper.insertUser("admin", encoder.encode("admin123"), "平台管理员", "13800000000", "ADMIN");
            mapper.insertUser(
                    "merchant", encoder.encode("merchant123"), "示例商家", "13800000001", "MERCHANT");
            mapper.insertUser("rider", encoder.encode("rider123"), "示例骑手", "13800000002", "RIDER");
            mapper.insertUser(
                    "student", encoder.encode("student123"), "示例学生", "13800000003", "STUDENT");
            long merchant = mapper.findUser("merchant").getId();
            long student = mapper.findUser("student").getId();
            mapper.insertShop(merchant, "校园好味道", "米饭、面食和饮品，校内配送", true);
            long shop = mapper.findShopByOwner(merchant).getId();
            mapper.insertDish(shop, "主食", "香辣鸡腿饭", java.math.BigDecimal.valueOf(18.00));
            mapper.insertDish(shop, "面食", "番茄鸡蛋面", java.math.BigDecimal.valueOf(14.00));
            mapper.insertDish(shop, "饮品", "柠檬茶", java.math.BigDecimal.valueOf(6.00));
            mapper.insertAddress(student, "一号宿舍楼", "302", "示例学生", "13800000003", true);
            var demoMenu = new ResourceDatabasePopulator(new ClassPathResource("db/demo-menu.sql"));
            demoMenu.setSqlScriptEncoding("UTF-8");
            demoMenu.execute(dataSource);
        };
    }
}
