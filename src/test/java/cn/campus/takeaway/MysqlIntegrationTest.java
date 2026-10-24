package cn.campus.takeaway;

import cn.campus.takeaway.cart.CartService;
import cn.campus.takeaway.domain.*;
import cn.campus.takeaway.mapper.AppMapper;
import cn.campus.takeaway.service.*;
import cn.campus.takeaway.cache.MenuCache;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
    "spring.datasource.url=${TEST_DB_URL:jdbc:mysql://127.0.0.1:13317/campus_takeaway_test?serverTimezone=Asia/Shanghai}",
    "spring.datasource.username=${TEST_DB_USERNAME:root}","spring.datasource.password=${TEST_DB_PASSWORD:}",
    "spring.data.redis.port=${TEST_REDIS_PORT:16377}","spring.session.redis.namespace=takeaway:test:session",
    "app.demo-data=false","app.expiry-enabled=false"})
@EnabledIfEnvironmentVariable(named="RUN_MYSQL_TESTS", matches="true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MysqlIntegrationTest {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @SpyBean AppMapper mapper;
    @Autowired AccountService accounts;
    @Autowired AddressService addresses;
    @Autowired OrderService orders;
    @Autowired CartService carts;
    @Autowired CatalogService catalog;
    @Autowired MenuCache menus;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;
    @Autowired StringRedisTemplate redis;
    @Autowired Environment environment;
    @Autowired DataSource dataSource;
    @Autowired PlatformTransactionManager transactions;
    ConfigurableApplicationContext second;
    int otherPort;
    AppUser student,otherStudent,merchant,otherMerchant,rider,otherRider,admin;
    long shopId,dishId,addressId;

    ConfigurableApplicationContext instance(Integer redisPort) { return instance(redisPort,false); }
    ConfigurableApplicationContext instance(Integer redisPort,boolean expiryEnabled) {
        var args=new ArrayList<>(List.of("--server.port=0", "--spring.datasource.url="+environment.getProperty("spring.datasource.url"),
            "--spring.datasource.username="+environment.getProperty("spring.datasource.username"),
            "--spring.datasource.password="+environment.getProperty("spring.datasource.password"),
            "--spring.data.redis.port="+(redisPort==null ? environment.getProperty("spring.data.redis.port") : redisPort),
            "--spring.session.redis.namespace=takeaway:test:session", "--app.demo-data=false", "--app.expiry-enabled="+expiryEnabled));
        return new SpringApplicationBuilder(CampusTakeawayApplication.class).run(args.toArray(String[]::new));
    }
    @BeforeAll void startSecondInstance() {
        assertEquals("campus_takeaway_test",jdbc.queryForObject("SELECT DATABASE()",String.class));
        second=instance(null);
        otherPort=Integer.parseInt(second.getEnvironment().getProperty("local.server.port"));
        assertNotEquals(port,otherPort);
    }
    @AfterAll void closeSecondInstance() { if(second!=null) second.close(); }
    @BeforeEach void prepare() {
        assertEquals("campus_takeaway_test",jdbc.queryForObject("SELECT DATABASE()",String.class));
        jdbc.execute("DROP TRIGGER IF EXISTS integration_fail_event");
        for(String table:List.of("cart_item","cart","order_events","order_items","orders","addresses","dishes","shops","users")) jdbc.update("DELETE FROM "+table);
        for(String name:List.of("student_a","student_b","merchant_a","merchant_b","rider_a","rider_b"))
            accounts.create(name,"test123",name,"13800000001",name.startsWith("student")?"STUDENT":name.startsWith("merchant")?"MERCHANT":"RIDER");
        mapper.insertUser("admin_a",encoder.encode("test123"),"测试管理员","13800000001","ADMIN");
        student=mapper.findUser("student_a");otherStudent=mapper.findUser("student_b");merchant=mapper.findUser("merchant_a");otherMerchant=mapper.findUser("merchant_b");rider=mapper.findUser("rider_a");otherRider=mapper.findUser("rider_b");admin=mapper.findUser("admin_a");
        shopId=mapper.findShopByOwner(merchant.getId()).getId();
        mapper.insertDish(shopId,"主食","测试套餐",new BigDecimal("12.50"));dishId=mapper.listDishes(shopId).get(0).getId();
        addresses.create(student.getId(),"一号宿舍楼","302","测试学生","13800000001");addressId=mapper.listAddresses(student.getId()).get(0).getId();
        redis.delete("takeaway:menu:v1:"+shopId);
    }
    class Client {
        final CookieManager cookies=new CookieManager(null,CookiePolicy.ACCEPT_ALL);
        final HttpClient http=HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(5)).build();
        String csrf;
        HttpResponse<String> send(String method,String path,Object body) throws Exception {return at(port,method,path,body,true);}
        HttpResponse<String> at(int target,String method,String path,Object body,boolean token) throws Exception {
            var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+target+path)).timeout(Duration.ofSeconds(15)).header("Accept","application/json");
            if(token && csrf!=null) request.header("X-CSRF-Token",csrf);
            if(body!=null) request.header("Content-Type","application/json");
            return http.send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
        }
        JsonNode ok(String method,String path,Object body) throws Exception {
            var response=send(method,path,body);assertEquals(200,response.statusCode(),response.body());return json.readTree(response.body());
        }
        Client login(String username) throws Exception {
            csrf=ok("GET","/api/auth/csrf",null).get("token").asText();
            csrf=ok("POST","/api/auth/login",Map.of("username",username,"password","test123")).get("token").asText();
            return this;
        }
    }
    Client as(AppUser user) throws Exception {return new Client().login(user.getUsername());}
    int count(String table) {return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
    Map<String,Object> checkout(CartService.View cart) {return Map.of("checkoutKey",cart.checkoutKey(),"cartVersion",cart.version(),"addressId",addressId);}
    long createOrder() {var cart=carts.put(student.getId(),dishId,2);return orders.create(student.getId(),cart.checkoutKey(),cart.version(),addressId).getId();}
    void state(long id,String status) {assertEquals(status,mapper.findOrder(id).getStatus());}
    void ready(long id) {orders.pay(id,student.getId());orders.merchantAccept(id,merchant.getId());orders.merchantReady(id,merchant.getId());}
    void failEvents() {jdbc.execute("CREATE TRIGGER integration_fail_event BEFORE INSERT ON order_events FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='integration event failure'");}
    void timeout(long id) {jdbc.update("UPDATE orders SET expires_at=DATE_SUB(NOW(),INTERVAL 1 MINUTE) WHERE id=?",id);}
    OrderExpiry expiry() {return new OrderExpiry(mapper,transactions);}

    @Test void fullOrderFlowUsesServerPricesAndKeepsSnapshots() throws Exception {
        var client=as(student);var cart=carts.put(student.getId(),dishId,2);
        mapper.updateDish(dishId,shopId,"测试套餐","主食",new BigDecimal("14.75"));
        long id=client.ok("POST","/api/student/orders",checkout(cart)).get("id").asLong();
        assertEquals(new BigDecimal("29.50"),mapper.findOrder(id).getAmount());
        String snapshot=mapper.findOrder(id).getAddressSnapshot();
        addresses.edit(student.getId(),addressId,"二号宿舍楼","101","新联系人","13900000001");
        mapper.updateDish(dishId,shopId,"新套餐","主食",new BigDecimal("19.90"));
        assertEquals(snapshot,mapper.findOrder(id).getAddressSnapshot());assertEquals(new BigDecimal("14.75"),mapper.listOrderItems(id).get(0).getUnitPrice());
        for(var step:List.of(new Object[]{student,"pay"},new Object[]{merchant,"accept"},new Object[]{merchant,"ready"},new Object[]{rider,"claim"},new Object[]{rider,"delivered"},new Object[]{student,"confirm"}))
            as((AppUser)step[0]).ok("POST","/api/orders/"+id+"/"+step[1],null);
        state(id,"COMPLETED");assertEquals(7,mapper.listEvents(id).size());
        assertEquals(5,jdbc.queryForObject("SELECT (paid_at IS NOT NULL)+(accepted_at IS NOT NULL)+(ready_at IS NOT NULL)+(delivered_at IS NOT NULL)+(completed_at IS NOT NULL) FROM orders WHERE id=?",Integer.class,id));
        for(var actor:List.of(student,merchant,rider,admin)) assertEquals(200,as(actor).send("GET","/api/orders/"+id,null).statusCode());
    }
    @Test void rolesOwnershipCsrfAndDisabledSessionsAreEnforced() throws Exception {
        long id=createOrder();var s=as(student);
        assertEquals(403,as(otherStudent).send("GET","/api/orders/"+id,null).statusCode());
        assertEquals(403,s.send("POST","/api/merchant/dishes",Map.of("name","x","category","x","price",1)).statusCode());
        assertEquals(403,s.at(port,"DELETE","/api/student/addresses/"+addressId,null,false).statusCode());
        assertEquals(400,as(otherStudent).send("DELETE","/api/student/addresses/"+addressId,null).statusCode());
        assertEquals(400,as(otherMerchant).send("PUT","/api/merchant/dishes/"+dishId,Map.of("name","x","category","x","price",1)).statusCode());
        assertEquals(400,as(admin).send("POST","/api/admin/users",Map.of("username","superadmin","password","test123","realName","x","phone","13800000001","role","ADMIN")).statusCode());
        assertNull(mapper.findUser("superadmin"));
        catalog.enabled(student.getId(),false);assertEquals(401,s.send("GET","/api/auth/me",null).statusCode());
        catalog.enabled(student.getId(),true);assertEquals(401,s.send("GET","/api/auth/me",null).statusCode());
    }
    @Test void checkoutDoubleClickCreatesOneOrderAndStaleFormsCannotClearNewCart() throws Exception {
        var client=as(student);var cart=carts.put(student.getId(),dishId,1);var body=checkout(cart);
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try {
            var a=pool.submit(()->{start.await();return client.at(port,"POST","/api/student/orders",body,true);});
            var b=pool.submit(()->{start.await();return client.at(otherPort,"POST","/api/student/orders",body,true);});start.countDown();
            var first=a.get(15,TimeUnit.SECONDS);var second=b.get(15,TimeUnit.SECONDS);
            assertEquals(200,first.statusCode(),first.body());assertEquals(200,second.statusCode(),second.body());
            long id=json.readTree(first.body()).get("id").asLong();assertEquals(id,json.readTree(second.body()).get("id").asLong());assertEquals(1,count("orders"));
            var fresh=carts.put(student.getId(),dishId,2);
            assertEquals(id,client.ok("POST","/api/student/orders",body).get("id").asLong());assertEquals(2,carts.get(student.getId()).items().get(0).quantity());
            var changed=new HashMap<>(body);changed.put("cartVersion",fresh.version());assertEquals(409,client.send("POST","/api/student/orders",changed).statusCode());
            changed=new HashMap<>(body);changed.put("addressId",addressId+1);assertEquals(409,client.send("POST","/api/student/orders",changed).statusCode());
            carts.put(student.getId(),dishId,3);assertEquals(409,client.send("POST","/api/student/orders",checkout(fresh)).statusCode());assertEquals(3,carts.get(student.getId()).items().get(0).quantity());
        } finally {pool.shutdownNow();}
    }
    @Test void concurrentRiderClaimsHaveExactlyOneWinner() throws Exception {
        long id=createOrder();ready(id);var a=as(rider);var b=as(otherRider);var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try {var first=pool.submit(()->{start.await();return a.send("POST","/api/orders/"+id+"/claim",null).statusCode();});var second=pool.submit(()->{start.await();return b.at(otherPort,"POST","/api/orders/"+id+"/claim",null,true).statusCode();});start.countDown();assertEquals(Set.of(200,409),Set.of(first.get(15,TimeUnit.SECONDS),second.get(15,TimeUnit.SECONDS)));state(id,"DELIVERING");assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM order_events WHERE order_id=? AND to_status='DELIVERING'",Integer.class,id));}finally{pool.shutdownNow();}
    }
    @Test void eventFailureRollsBackOrderItemsAndState() {
        var cart=carts.put(student.getId(),dishId,2);failEvents();
        assertThrows(RuntimeException.class,()->orders.create(student.getId(),cart.checkoutKey(),cart.version(),addressId));
        assertEquals(0,count("orders"));assertEquals(0,count("order_items"));assertEquals(cart,carts.get(student.getId()));
        jdbc.execute("DROP TRIGGER integration_fail_event");long id=createOrder();failEvents();assertThrows(RuntimeException.class,()->orders.pay(id,student.getId()));state(id,"UNPAID");
    }
    @Test void defaultsAreUniqueOwnedAndReassignedAfterDeletion() throws Exception {
        addresses.create(student.getId(),"二号宿舍楼","101","张三","13800000001");var list=mapper.listAddresses(student.getId());long secondId=list.get(1).getId();
        as(student).ok("POST","/api/student/addresses/"+secondId+"/default",null);assertEquals(secondId,mapper.listAddresses(student.getId()).get(0).getId());
        assertEquals(400,as(otherStudent).send("POST","/api/student/addresses/"+secondId+"/default",null).statusCode());
        as(student).ok("DELETE","/api/student/addresses/"+secondId,null);assertTrue(mapper.findAddress(addressId,student.getId()).getIsDefault());
        assertEquals(1,mapper.listAddresses(student.getId()).stream().filter(Address::getIsDefault).count());
        assertThrows(IllegalArgumentException.class,()->addresses.create(student.getId(),"校外","1","张三","13800000001"));
    }
    @Test void cancellationAndIllegalTransitionsPreserveState() {
        long id=createOrder();orders.cancel(id,student.getId());state(id,"CANCELED");assertThrows(IllegalStateException.class,()->orders.pay(id,student.getId()));
        long next=createOrder();orders.pay(next,student.getId());orders.cancel(next,student.getId());state(next,"CANCELED");
        next=createOrder();final long making=next;ready(next);assertThrows(IllegalStateException.class,()->orders.cancel(making,student.getId()));assertThrows(IllegalStateException.class,()->orders.confirm(making,student.getId()));assertThrows(IllegalStateException.class,()->orders.riderDelivered(making,otherRider.getId()));state(next,"READY");
    }
    @Test void roleDashboardsSynchronizeOnlyAtAllowedOrderStages() throws Exception {
        long id=createOrder();assertEquals(0,as(rider).ok("GET","/api/orders?scope=available",null).get("total").asInt());ready(id);
        assertEquals(1,as(rider).ok("GET","/api/orders?scope=available",null).get("total").asInt());orders.riderClaim(id,rider.getId());
        assertEquals(0,as(otherRider).ok("GET","/api/orders?scope=available",null).get("total").asInt());assertEquals(1,as(rider).ok("GET","/api/orders?scope=mine",null).get("total").asInt());
        assertEquals(0,as(otherMerchant).ok("GET","/api/orders",null).get("total").asInt());assertEquals(403,as(otherRider).send("GET","/api/orders/"+id,null).statusCode());
    }
    @Test void expandedDemoMenuIsRepeatableAndPreservesExistingData() {
        mapper.insertUser("merchant",encoder.encode("test123"),"示例商家","13800000001","MERCHANT");mapper.insertShop(mapper.findUser("merchant").getId(),"校园好味道","示例",true);
        long id=createOrder();var populator=new ResourceDatabasePopulator(new ClassPathResource("db/demo-menu.sql"));populator.setSqlScriptEncoding("UTF-8");populator.execute(dataSource);int dishes=count("dishes"),users=count("users");populator.execute(dataSource);
        assertEquals(dishes,count("dishes"));assertEquals(users,count("users"));assertEquals(new BigDecimal("25.00"),mapper.findOrder(id).getAmount());
    }
    @Test void photosArePublicAndStoredImageIsRendered() throws Exception {
        var client=new Client();var response=client.send("GET","/images/chicken-rice.webp",null);assertEquals(200,response.statusCode());assertTrue(response.headers().firstValue("content-type").orElseThrow().contains("image/webp"));
        jdbc.update("UPDATE dishes SET image_path='/images/lemon-tea.webp' WHERE id=?",dishId);
        assertEquals("/images/lemon-tea.webp",client.ok("GET","/api/shops/"+shopId+"/menu",null).path("dishes").path("records").get(0).get("imagePath").asText());assertEquals(404,client.send("GET","/images/missing.webp",null).statusCode());
    }
    @Test void accountProfileUsesCurrentUserAndValidatesInput() throws Exception {
        var client=as(student);client.ok("PUT","/api/account",Map.of("realName","新姓名","phone","13900000001"));assertEquals("新姓名",mapper.findUserById(student.getId()).getRealName());assertEquals("student_b",mapper.findUserById(otherStudent.getId()).getRealName());
        assertEquals(400,client.send("PUT","/api/account",Map.of("realName","","phone","123")).statusCode());assertEquals(400,client.send("PUT","/api/account",Map.of("realName","伪造","phone","13900000001","userId",otherStudent.getId())).statusCode());
        assertFalse(client.ok("GET","/api/auth/me",null).has("password"));
    }
    @Test void passwordChangeValidatesAndExpiresExistingSessions() throws Exception {
        var a=as(student);var b=as(student);
        assertEquals(400,a.send("POST","/api/account/password",Map.of("currentPassword","bad","newPassword","newpass1","confirmPassword","newpass1")).statusCode());
        a.ok("POST","/api/account/password",Map.of("currentPassword","test123","newPassword","newpass1","confirmPassword","newpass1"));assertTrue(encoder.matches("newpass1",mapper.findUserById(student.getId()).getPassword()));
        assertEquals(401,a.send("GET","/api/auth/me",null).statusCode());assertEquals(401,b.at(otherPort,"GET","/api/auth/me",null,true).statusCode());
    }
    @Test void reorderUsesCurrentMenuAndPreservesCartOnFailure() throws Exception {
        long id=createOrder();orders.cancel(id,student.getId());mapper.updateDish(dishId,shopId,"新菜单","主食",new BigDecimal("19.90"));orders.reorder(id,student.getId());assertEquals(new BigDecimal("39.80"),carts.get(student.getId()).total());
        var previous=carts.get(student.getId());mapper.toggleDish(dishId,shopId);assertThrows(IllegalArgumentException.class,()->orders.reorder(id,student.getId()));assertEquals(previous.version(),carts.get(student.getId()).version());assertEquals(previous.items().get(0).quantity(),carts.get(student.getId()).items().get(0).quantity());
        assertEquals(409,as(otherStudent).send("POST","/api/student/orders/"+id+"/reorder",null).statusCode());
    }
    @Test void realHttpLoginAndErrorPagesReturnCorrectStatus() throws Exception {
        var client=new Client();var anonymous=client.ok("GET","/api/auth/csrf",null);client.csrf=anonymous.get("token").asText();String old=client.cookies.getCookieStore().getCookies().get(0).getValue();String oldToken=client.csrf;
        assertEquals(403,client.at(port,"POST","/api/auth/login",Map.of("username","student_a","password","test123"),false).statusCode());
        var response=client.send("POST","/api/auth/login",Map.of("username","student_a","password","test123"));assertEquals(200,response.statusCode());client.csrf=json.readTree(response.body()).get("token").asText();assertNotEquals(oldToken,client.csrf);assertNotEquals(old,client.cookies.getCookieStore().getCookies().get(0).getValue());
        assertTrue(response.headers().allValues("set-cookie").stream().anyMatch(cookie -> cookie.contains("HttpOnly")&&cookie.contains("SameSite=Lax")));
        assertEquals(200,client.at(otherPort,"GET","/api/auth/me",null,true).statusCode());
        for(String path:List.of("/api/missing","/images/missing.webp","/unmatched-page")) assertEquals(404,client.send("GET",path,null).statusCode(),path);
        var html404=client.http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/missing-html-page")).header("Accept","text/html").GET().build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(404,html404.statusCode());assertTrue(html404.body().contains("页面不存在"));assertFalse(html404.body().contains("Whitelabel"));
        assertEquals(400,client.send("GET","/api/orders/not-a-number",null).statusCode());client.ok("POST","/api/auth/logout",null);assertEquals(401,client.send("GET","/api/auth/me",null).statusCode());
    }
    @Test void registrationCannotEscalateAndForgedHeadersDoNotAuthenticate() throws Exception {
        var client=new Client();client.csrf=client.ok("GET","/api/auth/csrf",null).get("token").asText();
        client.ok("POST","/api/auth/register",Map.of("username","registered","password","test123","realName","学生","phone","13800000001"));assertEquals("STUDENT",mapper.findUser("registered").getRole());
        assertEquals(400,client.send("POST","/api/auth/register",Map.of("username","evil","password","test123","realName","学生","phone","13800000001","role","ADMIN")).statusCode());assertNull(mapper.findUser("evil"));
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/admin/users")).header("X-User-Id",""+admin.getId()).header("X-User-Role","ADMIN").GET().build();assertEquals(401,client.http.send(request,HttpResponse.BodyHandlers.ofString()).statusCode());
    }
    @Test void databaseQuantityBoundariesCrossShopClosedAndOffSaleAreRejected() throws Exception {
        carts.put(student.getId(),dishId,1);carts.put(student.getId(),dishId,99);assertEquals(99,carts.get(student.getId()).items().get(0).quantity());
        var client=as(student);for(int quantity:List.of(0,100,-1)) assertEquals(400,client.send("PUT","/api/student/cart/items/"+dishId,Map.of("quantity",quantity)).statusCode());
        long otherShop=mapper.findShopByOwner(otherMerchant.getId()).getId();mapper.insertDish(otherShop,"主食","另店菜品",BigDecimal.ONE);long otherDish=mapper.listDishes(otherShop).get(0).getId();assertThrows(IllegalStateException.class,()->carts.put(student.getId(),otherDish,1));
        var view=carts.get(student.getId());mapper.setShopOpen(shopId,false);assertThrows(IllegalArgumentException.class,()->orders.create(student.getId(),view.checkoutKey(),view.version(),addressId));mapper.setShopOpen(shopId,true);mapper.toggleDish(dishId,shopId);assertThrows(IllegalArgumentException.class,()->orders.create(student.getId(),view.checkoutKey(),view.version(),addressId));assertEquals(99,carts.get(student.getId()).items().get(0).quantity());
    }
    @Test void concurrentCartModificationAndCheckoutDoNotLoseData() throws Exception {
        var view=carts.put(student.getId(),dishId,1);var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try {
            var submit=pool.submit(()->{start.await();try{return orders.create(student.getId(),view.checkoutKey(),view.version(),addressId).getId();}catch(IllegalStateException stale){return 0L;}});
            var change=pool.submit(()->{start.await();return second.getBean(CartService.class).put(student.getId(),dishId,2);});start.countDown();long id=submit.get(10,TimeUnit.SECONDS);change.get(10,TimeUnit.SECONDS);
            assertEquals(2,carts.get(student.getId()).items().get(0).quantity());assertEquals(id==0?0:1,count("orders"));if(id!=0)assertEquals(1,mapper.listOrderItems(id).get(0).getQuantity());
        }finally{pool.shutdownNow();}
    }
    @Test void timeoutScanningIsIdempotentAndRecoversAfterRestart() {
        long id=createOrder();timeout(id);expiry().scan();expiry().scan();state(id,"CANCELED");assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM order_events WHERE order_id=? AND reason='PAYMENT_TIMEOUT'",Integer.class,id));assertThrows(IllegalStateException.class,()->orders.pay(id,student.getId()));
        long next=createOrder();timeout(next);try(var restarted=instance(null,true)) { state(next,"CANCELED"); }
    }
    @Test void timeoutEventFailureRollsBackCancellation() {
        long id=createOrder();timeout(id);failEvents();expiry().scan();state(id,"UNPAID");jdbc.execute("DROP TRIGGER integration_fail_event");expiry().scan();state(id,"CANCELED");
    }
    @Test void paymentAndTimeoutRaceHasOnlyOneLegalTransition() throws Exception {
        long id=createOrder();jdbc.update("UPDATE orders SET expires_at=NOW() WHERE id=?",id);var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try {var a=pool.submit(()->{start.await();try{orders.pay(id,student.getId());}catch(IllegalStateException expired){}return true;});var b=pool.submit(()->{start.await();expiry().scan();return true;});start.countDown();a.get(10,TimeUnit.SECONDS);b.get(10,TimeUnit.SECONDS);assertEquals(2,mapper.listEvents(id).size());assertTrue(Set.of("CANCELED","PENDING_ACCEPT").contains(mapper.findOrder(id).getStatus()));}finally{pool.shutdownNow();}
    }
    @Test void menuCacheUsesRealRedisAndInvalidatesAfterCommit() throws Exception {
        var client=new Client();clearInvocations(mapper);client.ok("GET","/api/shops/"+shopId+"/menu",null);client.ok("GET","/api/shops/"+shopId+"/menu",null);
        verify(mapper,times(1)).pagePublicMenu(eq(shopId),eq(""),eq(12),eq(0L));assertNotNull(redis.opsForValue().get("takeaway:menu:v1:"+shopId));assertTrue(redis.getExpire("takeaway:menu:v1:"+shopId)<=75);
        catalog.dish(merchant.getId(),dishId,"主食","新套餐",new BigDecimal("23.00"));assertNull(redis.opsForValue().get("takeaway:menu:v1:"+shopId));assertEquals("23.0",String.valueOf(client.ok("GET","/api/shops/"+shopId+"/menu",null).path("dishes").path("records").get(0).get("price").asDouble()));
    }
    @Test void checkoutIgnoresStaleCacheAndCacheCanBeDisabled() {
        menus.get(shopId,1,12,"");mapper.updateDish(dishId,shopId,"测试套餐","主食",new BigDecimal("30.00"));long id=createOrder();assertEquals(new BigDecimal("60.00"),mapper.findOrder(id).getAmount());
        var disabled=new MenuCache(mapper,redis,json,false);clearInvocations(mapper);disabled.get(shopId,1,12,"");disabled.get(shopId,1,12,"");verify(mapper,times(2)).pagePublicMenu(eq(shopId),eq(""),eq(12),eq(0L));
    }
    @Test void redisOutageReturns503ForSessionsButPublicMenuFallsBack() throws Exception {
        int offlinePort;
        try(var reserved=new java.net.ServerSocket(0,1,InetAddress.getLoopbackAddress())) { offlinePort=reserved.getLocalPort(); }
        try(var broken=instance(offlinePort)) {
            assertEquals(offlinePort, broken.getBean(org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory.class).getPort());
            assertTrue(broken.containsBean("springSessionRepositoryFilter"));
            var repository=broken.getBean(org.springframework.session.data.redis.RedisSessionRepository.class);
            assertThrows(RuntimeException.class,()->repository.save(repository.createSession()));
            int brokenPort=Integer.parseInt(broken.getEnvironment().getProperty("local.server.port"));var client=new Client();
            assertNotEquals(port,brokenPort);
            assertNotEquals(otherPort,brokenPort);
            assertEquals(503,client.at(brokenPort,"GET","/api/auth/csrf",null,false).statusCode());
            assertEquals(200,client.at(brokenPort,"GET","/api/shops/"+shopId+"/menu",null,false).statusCode());
            var logged=as(student);assertEquals(503,logged.at(brokenPort,"GET","/api/auth/me",null,true).statusCode());
        }
    }
    @Test void paginationCountsFiltersStableOrderAndBatchDetails() throws Exception {
        for(int i=0;i<5;i++)createOrder();var client=as(student);clearInvocations(mapper);
        var first=client.ok("GET","/api/orders?page=1&size=2",null);assertEquals(5,first.get("total").asInt());assertEquals(2,first.get("records").size());assertEquals(1,first.get("records").get(0).get("items").size());verify(mapper,times(1)).batchOrderItems(anyList());verify(mapper,never()).listOrderItems(anyLong());
        var last=client.ok("GET","/api/orders?page=3&size=2",null);assertEquals(1,last.get("records").size());assertTrue(first.get("records").get(0).get("id").asLong()>last.get("records").get(0).get("id").asLong());assertEquals(0,client.ok("GET","/api/orders?status=COMPLETED",null).get("total").asInt());
        assertEquals(0,client.ok("GET","/api/orders?query=nomatch",null).get("total").asInt());for(String parameter:List.of("size=0","size=101","page=0","status=INVALID"))assertEquals(400,client.send("GET","/api/orders?"+parameter,null).statusCode());
        assertEquals(400,client.send("GET","/api/shops?size=101",null).statusCode());assertFalse(as(admin).ok("GET","/api/admin/users",null).toString().contains("password"));
    }

    @Test void checkoutRejectsOtherStudentsAddressWithoutClearingCart() throws Exception {
        addresses.create(otherStudent.getId(),"二号宿舍楼","101","另一学生","13800000001");
        long otherAddress=mapper.listAddresses(otherStudent.getId()).get(0).getId();
        var cart=carts.put(student.getId(),dishId,1);var body=new HashMap<>(checkout(cart));body.put("addressId",otherAddress);
        assertEquals(400,as(student).send("POST","/api/student/orders",body).statusCode());
        assertEquals(cart,carts.get(student.getId()));assertEquals(0,count("orders"));
        assertEquals(400,as(student).send("PUT","/api/student/cart/items/"+dishId,Map.of("quantity",1.5)).statusCode());
    }
    @Test void menuToggleShopAndMerchantDisableInvalidateCommittedCache() {
        menus.get(shopId,1,12,"");catalog.toggleDish(merchant.getId(),dishId);assertNull(redis.opsForValue().get("takeaway:menu:v1:"+shopId));
        assertEquals(0,menus.get(shopId,1,12,"").dishes().total());
        catalog.adminShop(shopId,false);assertNull(redis.opsForValue().get("takeaway:menu:v1:"+shopId));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->menus.get(shopId,1,12,""));
        assertEquals("null",redis.opsForValue().get("takeaway:menu:v1:"+shopId));
        catalog.adminShop(shopId,true);menus.get(shopId,1,12,"");catalog.enabled(merchant.getId(),false);
        assertNull(redis.opsForValue().get("takeaway:menu:v1:"+shopId));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->menus.get(shopId,1,12,""));
    }
}
