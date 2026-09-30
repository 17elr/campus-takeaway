package cn.campus.takeaway;

import cn.campus.takeaway.domain.*;
import cn.campus.takeaway.mapper.AppMapper;
import cn.campus.takeaway.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named="RUN_MYSQL_TESTS",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:mysql://localhost:3306/campus_takeaway_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
})
@AutoConfigureMockMvc
class MysqlIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AppMapper mapper;
    @Autowired OrderService orders;
    @Autowired AddressService addresses;
    @Autowired AccountService accounts;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @LocalServerPort int port;
    AppUser student,otherStudent,merchant,otherMerchant,rider,otherRider,admin;
    long shopId,dishId,addressId;

    @BeforeEach void prepare(){
        // Destructive cleanup is restricted to this dedicated test database.
        assertEquals("campus_takeaway_test",jdbc.queryForObject("SELECT DATABASE()",String.class));
        jdbc.execute("DROP TRIGGER IF EXISTS integration_fail_event");
        for(String table:List.of("order_events","order_items","orders","addresses","dishes","shops","users"))jdbc.update("DELETE FROM "+table);
        for(String name:List.of("student_a","student_b","merchant_a","merchant_b","rider_a","rider_b"))
            accounts.create(name,"test123",name,"13800000001",name.startsWith("student")?"STUDENT":name.startsWith("merchant")?"MERCHANT":"RIDER");
        mapper.insertUser("admin_a",encoder.encode("test123"),"测试管理员","13800000001","ADMIN");
        student=mapper.findUser("student_a");otherStudent=mapper.findUser("student_b");
        merchant=mapper.findUser("merchant_a");otherMerchant=mapper.findUser("merchant_b");
        rider=mapper.findUser("rider_a");otherRider=mapper.findUser("rider_b");admin=mapper.findUser("admin_a");
        shopId=mapper.findShopByOwner(merchant.getId()).getId();
        mapper.insertDish(shopId,"主食","测试套餐",new BigDecimal("12.50"));
        dishId=mapper.listDishes(shopId).get(0).getId();
        addresses.create(student.getId(),"一号宿舍楼","302","测试学生","13800000001");
        addressId=mapper.listAddresses(student.getId()).get(0).getId();
    }
    Cart cart(){Cart cart=new Cart();cart.add(shopId,dishId);return cart;}
    long createOrder(){return orders.create(student.getId(),cart(),addressId).getId();}
    MvcResult postAs(AppUser actor,String path,String... values)throws Exception{
        var request=post(path).with(user(actor)).with(csrf());
        for(int i=0;i<values.length;i+=2)request.param(values[i],values[i+1]);
        return mvc.perform(request).andExpect(status().is3xxRedirection()).andReturn();
    }
    void state(long id,String expected){assertEquals(expected,mapper.findOrder(id).getStatus());}
    int count(String table){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
    String token(String html,String name){
        var matcher=Pattern.compile("name=\""+name+"\"[^>]*value=\"([^\"]+)\"").matcher(html);
        assertTrue(matcher.find(),"Missing rendered form token: "+name);return matcher.group(1);
    }

    @Test void fullOrderFlowUsesServerPricesAndKeepsSnapshots()throws Exception{
        var session=new MockHttpSession();
        for(int i=0;i<2;i++)mvc.perform(post("/student/cart/add").session(session).with(user(student)).with(csrf()).param("dishId",""+dishId)).andExpect(status().is3xxRedirection());
        String html=mvc.perform(get("/student/cart").session(session).with(user(student))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(html.contains("checkoutToken"));assertTrue(html.contains("selected=\"selected\""));
        mapper.updateDish(dishId,shopId,"测试套餐","主食",new BigDecimal("14.75"));
        var created=mvc.perform(post("/student/orders").session(session).with(user(student)).with(csrf()).param("addressId",""+addressId).param("checkoutToken",token(html,"checkoutToken")).param("amount","0.01")).andExpect(status().is3xxRedirection()).andReturn();
        long id=Long.parseLong(created.getResponse().getRedirectedUrl().substring("/orders/".length()));
        assertEquals(new BigDecimal("29.50"),mapper.findOrder(id).getAmount());
        String snapshot=mapper.findOrder(id).getAddressSnapshot();
        addresses.edit(student.getId(),addressId,"二号宿舍楼","101","新联系人","13900000001");
        mapper.updateDish(dishId,shopId,"新套餐","主食",new BigDecimal("19.90"));
        assertEquals(snapshot,mapper.findOrder(id).getAddressSnapshot());
        var item=mapper.listOrderItems(id).get(0);assertEquals("测试套餐",item.getDishName());assertEquals(new BigDecimal("14.75"),item.getUnitPrice());assertEquals(2,item.getQuantity());
        mvc.perform(get("/orders/"+id).with(user(student))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("模拟")));
        postAs(student,"/student/orders/"+id+"/pay");state(id,"PENDING_ACCEPT");
        postAs(merchant,"/merchant/orders/"+id+"/accept");state(id,"MAKING");
        postAs(merchant,"/merchant/orders/"+id+"/ready");state(id,"READY");
        mvc.perform(get("/rider").param("tab","available").with(user(rider))).andExpect(status().isOk());
        postAs(rider,"/rider/orders/"+id+"/claim");state(id,"DELIVERING");
        postAs(rider,"/rider/orders/"+id+"/delivered");state(id,"WAIT_CONFIRM");
        postAs(student,"/student/orders/"+id+"/confirm");state(id,"COMPLETED");
        assertEquals(7,mapper.listEvents(id).size());
        assertEquals(5,jdbc.queryForObject("SELECT (paid_at IS NOT NULL)+(accepted_at IS NOT NULL)+(ready_at IS NOT NULL)+(delivered_at IS NOT NULL)+(completed_at IS NOT NULL) FROM orders WHERE id=?",Integer.class,id));
        for(var actor:List.of(student,merchant,rider,admin)){
            mvc.perform(get("/"+actor.getRole().toLowerCase()).param("tab","orders").with(user(actor))).andExpect(status().isOk());
            mvc.perform(get("/orders/"+id).with(user(actor))).andExpect(status().isOk()).andExpect(model().attribute("tab","orders"));
        }
    }

    @Test void rolesOwnershipCsrfAndDisabledSessionsAreEnforced()throws Exception{
        long id=createOrder();
        mvc.perform(get("/orders/"+id).with(user(otherStudent))).andExpect(status().isForbidden());
        mvc.perform(post("/merchant/orders/"+id+"/accept").with(user(student)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/student/addresses/"+addressId+"/delete").with(user(student))).andExpect(status().isForbidden());
        assertNotNull(mapper.findAddress(addressId,student.getId()));
        assertNotNull(postAs(otherStudent,"/student/orders/"+id+"/pay").getFlashMap().get("error"));state(id,"UNPAID");
        assertNotNull(postAs(otherMerchant,"/merchant/orders/"+id+"/accept").getFlashMap().get("error"));
        assertNotNull(postAs(otherStudent,"/student/addresses/"+addressId+"/default").getFlashMap().get("error"));
        assertTrue(mapper.findAddress(addressId,student.getId()).getIsDefault());
        assertNotNull(postAs(admin,"/admin/users","username","superadmin","password","test123","realName","拒绝提权","phone","13800000001","role","ADMIN").getFlashMap().get("error"));
        assertNull(mapper.findUser("superadmin"));
        mvc.perform(post("/register").with(csrf()).param("username","registered").param("password","test123").param("realName","注册学生").param("phone","13800000001").param("role","ADMIN")).andExpect(status().is3xxRedirection());
        assertEquals("STUDENT",mapper.findUser("registered").getRole());
        mapper.setUserEnabled(student.getId(),false);
        mvc.perform(get("/student").with(user(student))).andExpect(redirectedUrl("/login?disabled"));
        assertEquals(1,count("order_events"));
    }

    @Test void checkoutDoubleClickCreatesOneOrderAndStaleFormsCannotClearNewCart()throws Exception{
        var session=new MockHttpSession();Cart cart=cart();session.setAttribute("cart",cart);String key=cart.getCheckoutToken();
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try{
            Callable<String> submit=()->{assertTrue(start.await(5,TimeUnit.SECONDS));return mvc.perform(post("/student/orders").session(session).with(user(student)).with(csrf()).param("addressId",""+addressId).param("checkoutToken",key)).andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();};
            var first=pool.submit(submit);var second=pool.submit(submit);start.countDown();
            String location=first.get(10,TimeUnit.SECONDS);assertEquals(location,second.get(10,TimeUnit.SECONDS));
            assertEquals(1,count("orders"));assertEquals(1,count("order_items"));assertEquals(1,count("order_events"));assertTrue(cart.getQuantities().isEmpty());
            cart.add(shopId,dishId);String stale=cart.getCheckoutToken();cart.add(shopId,dishId);
            var rejected=mvc.perform(post("/student/orders").session(session).with(user(student)).with(csrf()).param("addressId",""+addressId).param("checkoutToken",stale)).andExpect(redirectedUrl("/student/cart")).andReturn();
            assertNotNull(rejected.getFlashMap().get("error"));assertEquals(1,count("orders"));assertEquals(2,cart.getQuantities().get(dishId));
            mvc.perform(post("/student/orders").session(session).with(user(student)).with(csrf()).param("addressId",""+addressId).param("checkoutToken",key)).andExpect(redirectedUrl(location));
            assertEquals(2,cart.getQuantities().get(dishId));
        }finally{pool.shutdownNow();}
    }

    @Test void concurrentRiderClaimsHaveExactlyOneWinner()throws Exception{
        long id=createOrder();orders.pay(id,student.getId());orders.merchantAccept(id,merchant.getId());orders.merchantReady(id,merchant.getId());
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try{
            List<Future<Boolean>> results=new ArrayList<>();
            for(var actor:List.of(rider,otherRider))results.add(pool.submit(()->{assertTrue(start.await(5,TimeUnit.SECONDS));try{orders.riderClaim(id,actor.getId());return true;}catch(IllegalStateException taken){return false;}}));
            start.countDown();int winners=0;for(var result:results)if(result.get(10,TimeUnit.SECONDS))winners++;
            assertEquals(1,winners);state(id,"DELIVERING");
            assertTrue(Set.of(rider.getId(),otherRider.getId()).contains(mapper.findOrder(id).getRiderId()));
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM order_events WHERE order_id=? AND to_status='DELIVERING'",Integer.class,id));
        }finally{pool.shutdownNow();}
    }

    @Test void eventFailureRollsBackOrderItemsAndState(){
        jdbc.execute("CREATE TRIGGER integration_fail_event BEFORE INSERT ON order_events FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='integration rollback check'");
        try{
            assertThrows(DataAccessException.class,()->createOrder());assertEquals(0,count("orders"));assertEquals(0,count("order_items"));assertEquals(0,count("order_events"));
        }finally{jdbc.execute("DROP TRIGGER integration_fail_event");}
        long id=createOrder();
        jdbc.execute("CREATE TRIGGER integration_fail_event BEFORE INSERT ON order_events FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='integration rollback check'");
        try{
            assertThrows(DataAccessException.class,()->orders.pay(id,student.getId()));state(id,"UNPAID");assertEquals(1,mapper.listEvents(id).size());
            assertNull(jdbc.queryForObject("SELECT paid_at FROM orders WHERE id=?",java.sql.Timestamp.class,id));
        }finally{jdbc.execute("DROP TRIGGER integration_fail_event");}
    }

    @Test void defaultsAreUniqueOwnedAndReassignedAfterDeletion()throws Exception{
        assertTrue(mapper.findAddress(addressId,student.getId()).getIsDefault());
        addresses.create(student.getId(),"二号宿舍楼","201","测试学生","13800000001");
        long second=mapper.listAddresses(student.getId()).stream().filter(a->a.getId()!=addressId).findFirst().orElseThrow().getId();
        addresses.setDefault(student.getId(),second);
        assertTrue(mapper.findAddress(second,student.getId()).getIsDefault());assertFalse(mapper.findAddress(addressId,student.getId()).getIsDefault());
        assertThrows(IllegalArgumentException.class,()->addresses.setDefault(otherStudent.getId(),second));
        assertThrows(IllegalArgumentException.class,()->addresses.create(student.getId(),"校外小区","201","学生","13800000001"));
        var session=new MockHttpSession();session.setAttribute("cart",cart());
        var page=mvc.perform(get("/student/cart").session(session).with(user(student))).andExpect(status().isOk()).andReturn();
        assertEquals(second,page.getModelAndView().getModel().get("selectedAddressId"));
        mvc.perform(get("/student").param("tab","addresses").with(user(student))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("默认收货地址")));
        addresses.delete(student.getId(),second);assertTrue(mapper.findAddress(addressId,student.getId()).getIsDefault());
        addresses.create(student.getId(),"三号宿舍楼","301","测试学生","13800000001");
        long third=mapper.listAddresses(student.getId()).stream().filter(a->a.getId()!=addressId).findFirst().orElseThrow().getId();
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try{
            var first=pool.submit(()->{assertTrue(start.await(5,TimeUnit.SECONDS));addresses.setDefault(student.getId(),addressId);return true;});
            var other=pool.submit(()->{assertTrue(start.await(5,TimeUnit.SECONDS));addresses.setDefault(student.getId(),third);return true;});
            start.countDown();assertTrue(first.get(10,TimeUnit.SECONDS));assertTrue(other.get(10,TimeUnit.SECONDS));
            assertEquals(1,mapper.listAddresses(student.getId()).stream().filter(Address::getIsDefault).count());
        }finally{pool.shutdownNow();}
    }

    @Test void cancellationAndIllegalTransitionsPreserveState(){
        long unpaid=createOrder();orders.cancel(unpaid,student.getId());state(unpaid,"CANCELED");
        assertEquals("UNPAID",mapper.listEvents(unpaid).get(1).fromStatus());assertThrows(IllegalStateException.class,()->orders.pay(unpaid,student.getId()));
        long pending=createOrder();orders.pay(pending,student.getId());orders.cancel(pending,student.getId());state(pending,"CANCELED");
        assertEquals("PENDING_ACCEPT",mapper.listEvents(pending).get(2).fromStatus());
        long id=createOrder();assertThrows(IllegalStateException.class,()->orders.confirm(id,student.getId()));
        assertThrows(IllegalStateException.class,()->orders.merchantAccept(id,merchant.getId()));assertThrows(IllegalStateException.class,()->orders.riderClaim(id,rider.getId()));
        orders.pay(id,student.getId());assertThrows(IllegalStateException.class,()->orders.pay(id,student.getId()));
        orders.merchantAccept(id,merchant.getId());assertThrows(IllegalStateException.class,()->orders.cancel(id,student.getId()));state(id,"MAKING");
        orders.merchantReady(id,merchant.getId());orders.riderClaim(id,rider.getId());
        assertThrows(IllegalStateException.class,()->orders.riderDelivered(id,otherRider.getId()));state(id,"DELIVERING");assertEquals(5,mapper.listEvents(id).size());
        mapper.setShopOpen(shopId,false);assertThrows(IllegalArgumentException.class,()->createOrder());
        mapper.setShopOpen(shopId,true);mapper.toggleDish(dishId,shopId);assertThrows(IllegalArgumentException.class,()->createOrder());
        mapper.toggleDish(dishId,shopId);mapper.setUserEnabled(merchant.getId(),false);assertThrows(IllegalArgumentException.class,()->createOrder());
        assertEquals(3,count("orders"));
    }

    @Test void photosArePublicAndStoredImageIsRendered()throws Exception{
        for(String photo:List.of("chicken-rice.webp","tomato-noodles.webp","lemon-tea.webp","meal-cover.webp"))
            mvc.perform(get("/images/"+photo)).andExpect(status().isOk()).andExpect(content().contentType("image/webp"));
        mvc.perform(get("/images/credits.html")).andExpect(status().isOk());
        jdbc.update("UPDATE dishes SET image_path=? WHERE id=?","/images/lemon-tea.webp",dishId);
        assertEquals("/images/lemon-tea.webp",mapper.findDish(dishId).getImagePath());
        mvc.perform(get("/student/shop/"+shopId).with(user(student))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("src=\"/images/lemon-tea.webp\"")));
    }

    @Test void accountProfileUsesCurrentUserAndValidatesInput()throws Exception{
        mvc.perform(get("/account")).andExpect(status().is3xxRedirection());
        for(var actor:List.of(student,merchant,rider,admin))
            mvc.perform(get("/account").with(user(actor))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString(actor.getUsername())));
        mvc.perform(post("/account/profile").with(user(student)).param("realName","改名").param("phone","13800000002")).andExpect(status().isForbidden());
        for(var invalid:List.of(new String[]{"","13800000001"},new String[]{"测试学生","123"})){
            var result=postAs(student,"/account/profile","realName",invalid[0],"phone",invalid[1]);
            assertNotNull(result.getFlashMap().get("error"));
            assertEquals("student_a",mapper.findUser("student_a").getRealName());
        }
        postAs(student,"/account/profile","realName","  新学生  ","phone","13800000002","role","ADMIN","username","admin_a","id",""+otherStudent.getId());
        var saved=mapper.findUser("student_a");
        assertEquals("新学生",saved.getRealName());assertEquals("13800000002",saved.getPhone());assertEquals("STUDENT",saved.getRole());
        assertEquals("student_b",mapper.findUser("student_b").getRealName());
    }

    HttpClient loggedInClient(String username,String password)throws Exception{
        var client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).connectTimeout(Duration.ofSeconds(5)).build();
        var page=httpGet(client,"/login");
        var response=httpPost(client,"/login",Map.of("username",username,"password",password,"_csrf",token(page.body(),"_csrf")));
        assertEquals(302,response.statusCode());assertTrue(response.headers().firstValue("location").orElseThrow().endsWith("/home"));
        return client;
    }
    HttpResponse<String> httpGet(HttpClient client,String path)throws Exception{
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("Accept","text/html").GET().build(),HttpResponse.BodyHandlers.ofString());
    }
    HttpResponse<String> httpPost(HttpClient client,String path,Map<String,String> params)throws Exception{
        String form=params.entrySet().stream().map(e->URLEncoder.encode(e.getKey(),StandardCharsets.UTF_8)+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8)).collect(java.util.stream.Collectors.joining("&"));
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("Accept","text/html").header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form)).build(),HttpResponse.BodyHandlers.ofString());
    }

    @Test void passwordChangeValidatesAndExpiresExistingSessions()throws Exception{
        String oldHash=student.getPassword();
        mvc.perform(post("/account/password").with(user(student)).param("currentPassword","test123").param("newPassword","changed123").param("confirmPassword","changed123")).andExpect(status().isForbidden());
        for(var invalid:List.of(new String[]{"wrong123","changed123","changed123"},new String[]{"test123","changed123","different123"},new String[]{"test123","test123","test123"},new String[]{"test123","short","short"},new String[]{"test123","密".repeat(30),"密".repeat(30)})){
            var result=postAs(student,"/account/password","currentPassword",invalid[0],"newPassword",invalid[1],"confirmPassword",invalid[2]);
            assertNotNull(result.getFlashMap().get("error"));assertEquals(oldHash,mapper.findUser("student_a").getPassword());
        }
        var current=loggedInClient("student_a","test123");var other=loggedInClient("student_a","test123");
        var page=httpGet(current,"/account");assertEquals(200,page.statusCode());
        var changed=httpPost(current,"/account/password",Map.of("currentPassword","test123","newPassword","changed123","confirmPassword","changed123","_csrf",token(page.body(),"_csrf")));
        assertEquals(302,changed.statusCode());assertTrue(changed.headers().firstValue("location").orElseThrow().endsWith("/login?changed"));
        String newHash=mapper.findUser("student_a").getPassword();assertNotEquals(oldHash,newHash);assertTrue(encoder.matches("changed123",newHash));
        assertEquals(302,httpGet(current,"/student").statusCode());
        var expired=httpGet(other,"/student");assertEquals(302,expired.statusCode());assertTrue(expired.headers().firstValue("location").orElseThrow().contains("/login"));
        mvc.perform(get("/student").with(user(student))).andExpect(status().is3xxRedirection());
        var oldLogin=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();var loginPage=httpGet(oldLogin,"/login");
        var rejected=httpPost(oldLogin,"/login",Map.of("username","student_a","password","test123","_csrf",token(loginPage.body(),"_csrf")));
        assertTrue(rejected.headers().firstValue("location").orElseThrow().endsWith("/login?error"));
        assertEquals(200,httpGet(loggedInClient("student_a","changed123"),"/account").statusCode());
    }

    @Test void reorderUsesCurrentMenuAndPreservesCartOnFailure()throws Exception{
        Cart original=cart();original.add(shopId,dishId);long id=orders.create(student.getId(),original,addressId).getId();
        var session=new MockHttpSession();var existing=cart();session.setAttribute("cart",existing);
        var inProgress=mvc.perform(post("/student/orders/"+id+"/reorder").session(session).with(user(student)).with(csrf())).andExpect(status().is3xxRedirection()).andReturn();
        assertNotNull(inProgress.getFlashMap().get("error"));assertSame(existing,session.getAttribute("cart"));
        orders.pay(id,student.getId());orders.merchantAccept(id,merchant.getId());orders.merchantReady(id,merchant.getId());orders.riderClaim(id,rider.getId());orders.riderDelivered(id,rider.getId());orders.confirm(id,student.getId());
        mapper.updateDish(dishId,shopId,"新套餐","主食",new BigDecimal("20.00"));
        mvc.perform(post("/student/orders/"+id+"/reorder").session(session).with(user(student))).andExpect(status().isForbidden());
        var foreign=mvc.perform(post("/student/orders/"+id+"/reorder").session(session).with(user(otherStudent)).with(csrf())).andExpect(status().is3xxRedirection()).andReturn();
        assertNotNull(foreign.getFlashMap().get("error"));assertSame(existing,session.getAttribute("cart"));assertEquals(1,count("orders"));
        mvc.perform(post("/student/orders/"+id+"/reorder").session(session).with(user(student)).with(csrf())).andExpect(redirectedUrl("/student/cart"));
        var rebuilt=(Cart)session.getAttribute("cart");assertEquals(Map.of(dishId,2),rebuilt.getQuantities());assertEquals(shopId,rebuilt.getShopId());
        long next=orders.create(student.getId(),rebuilt,addressId).getId();assertEquals(new BigDecimal("40.00"),mapper.findOrder(next).getAmount());
        assertEquals(new BigDecimal("25.00"),mapper.findOrder(id).getAmount());assertEquals("测试套餐",mapper.listOrderItems(id).get(0).getDishName());
        assertEquals(new BigDecimal("12.50"),mapper.listOrderItems(id).get(0).getUnitPrice());
        for(int failure=0;failure<3;failure++){
            if(failure==0)mapper.toggleDish(dishId,shopId);else if(failure==1)mapper.setShopOpen(shopId,false);else mapper.setUserEnabled(merchant.getId(),false);
            var rejected=mvc.perform(post("/student/orders/"+id+"/reorder").session(session).with(user(student)).with(csrf())).andExpect(status().is3xxRedirection()).andReturn();
            assertNotNull(rejected.getFlashMap().get("error"));assertSame(rebuilt,session.getAttribute("cart"));assertEquals(Map.of(dishId,2),rebuilt.getQuantities());
            if(failure==0)mapper.toggleDish(dishId,shopId);else if(failure==1)mapper.setShopOpen(shopId,true);else mapper.setUserEnabled(merchant.getId(),true);
        }
        orders.cancel(next,student.getId());
        mvc.perform(post("/student/orders/"+next+"/reorder").session(session).with(user(student)).with(csrf())).andExpect(redirectedUrl("/student/cart"));
        assertEquals(Map.of(dishId,2),((Cart)session.getAttribute("cart")).getQuantities());assertEquals(2,count("orders"));
    }

    @Test void realHttpLoginAndErrorPagesReturnCorrectStatus()throws Exception{
        var client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).connectTimeout(Duration.ofSeconds(5)).build();
        String base="http://localhost:"+port;
        var login=client.send(HttpRequest.newBuilder(URI.create(base+"/login")).header("Accept","text/html").GET().build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,login.statusCode());String csrf=token(login.body(),"_csrf");
        String form="username=student_a&password=test123&_csrf="+URLEncoder.encode(csrf,StandardCharsets.UTF_8);
        var authenticated=client.send(HttpRequest.newBuilder(URI.create(base+"/login")).header("Accept","text/html").header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form)).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(302,authenticated.statusCode());assertTrue(authenticated.headers().firstValue("location").orElseThrow().endsWith("/home"));
        assertEquals(200,client.send(HttpRequest.newBuilder(URI.create(base+"/student")).header("Accept","text/html").GET().build(),HttpResponse.BodyHandlers.ofString()).statusCode());
        for(var entry:Map.of("/admin",403,"/orders/999999999",404,"/orders/not-a-number",400,"/does-not-exist",404).entrySet()){
            var response=client.send(HttpRequest.newBuilder(URI.create(base+entry.getKey())).header("Accept","text/html").GET().build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(entry.getValue(),response.statusCode(),entry.getKey());assertTrue(response.body().contains("校园好味道"),entry.getKey()+": "+response.body());assertFalse(response.body().contains("Whitelabel Error Page"));
        }
        var denied=client.send(HttpRequest.newBuilder(URI.create(base+"/student/cart/clear")).header("Accept","text/html").POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(403,denied.statusCode());assertTrue(denied.body().contains("当前操作未获授权"));
        mapper.setUserEnabled(student.getId(),false);
        var expired=client.send(HttpRequest.newBuilder(URI.create(base+"/student")).header("Accept","text/html").GET().build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(302,expired.statusCode());assertTrue(expired.headers().firstValue("location").orElseThrow().endsWith("/login?disabled"));
    }
}
