package cn.campus.takeaway.mapper;

import cn.campus.takeaway.domain.*;

import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface AppMapper {
    @Select(
            "SELECT id,username,password_hash,real_name,phone,role,enabled,auth_version FROM users WHERE"
                + " username=#{username}")
    AppUser findUser(String username);

    @Select("SELECT COUNT(*) FROM users")
    long countUsers();

    @Select("SELECT id FROM users WHERE id=#{id} FOR UPDATE")
    Long lockUser(long id);

    @Update("UPDATE users SET real_name=#{name},phone=#{phone} WHERE id=#{id} AND enabled=1")
    int updateProfile(
            @Param("id") long id, @Param("name") String name, @Param("phone") String phone);

    @Update("UPDATE users SET password_hash=#{password},auth_version=auth_version+1 WHERE id=#{id} AND enabled=1")
    int updatePassword(@Param("id") long id, @Param("password") String password);

    @Insert(
            "INSERT INTO users(username,password_hash,real_name,phone,role)"
                + " VALUES(#{username},#{password},#{name},#{phone},#{role})")
    int insertUser(
            @Param("username") String username,
            @Param("password") String password,
            @Param("name") String name,
            @Param("phone") String phone,
            @Param("role") String role);

    @Update("UPDATE users SET enabled=#{enabled},auth_version=auth_version+1 WHERE id=#{id}")
    int setUserEnabled(@Param("id") long id, @Param("enabled") boolean enabled);

    @Insert(
            "INSERT INTO shops(owner_id,name,description,is_open)"
                + " VALUES(#{ownerId},#{name},#{description},#{open})")
    int insertShop(
            @Param("ownerId") long ownerId,
            @Param("name") String name,
            @Param("description") String description,
            @Param("open") boolean open);

    @Select("SELECT id,owner_id,name,description,is_open AS open FROM shops WHERE id=#{id}")
    Shop findShop(long id);

    @Select(
            "SELECT id,owner_id,name,description,is_open AS open FROM shops WHERE"
                + " owner_id=#{ownerId}")
    Shop findShopByOwner(long ownerId);

    @Update(
            "UPDATE shops SET name=#{name},description=#{description},is_open=#{open} WHERE"
                + " owner_id=#{ownerId}")
    int updateShop(
            @Param("ownerId") long ownerId,
            @Param("name") String name,
            @Param("description") String description,
            @Param("open") boolean open);

    @Update("UPDATE shops SET is_open=#{open} WHERE id=#{id}")
    int setShopOpen(@Param("id") long id, @Param("open") boolean open);

    @Select("SELECT enabled FROM users WHERE id=#{id}")
    Boolean userEnabled(long id);

    @Select(
            "SELECT id,shop_id,category,name,price,image_path,on_sale FROM dishes WHERE"
                + " shop_id=#{shopId} ORDER BY id")
    List<Dish> listDishes(long shopId);

    @Select(
            "SELECT d.id,d.shop_id,d.category,d.name,d.price,d.image_path,d.on_sale,s.name"
                + " shop_name FROM dishes d JOIN shops s ON s.id=d.shop_id WHERE d.id=#{id}")
    Dish findDish(long id);

    @Insert(
            "INSERT INTO dishes(shop_id,category,name,price,on_sale)"
                + " VALUES(#{shopId},#{category},#{name},#{price},1)")
    int insertDish(
            @Param("shopId") long shopId,
            @Param("category") String category,
            @Param("name") String name,
            @Param("price") BigDecimal price);

    @Update("UPDATE dishes SET on_sale=NOT on_sale WHERE id=#{id} AND shop_id=#{shopId}")
    int toggleDish(@Param("id") long id, @Param("shopId") long shopId);

    @Update(
            "UPDATE dishes SET name=#{name},category=#{category},price=#{price} WHERE id=#{id} AND"
                + " shop_id=#{shopId}")
    int updateDish(
            @Param("id") long id,
            @Param("shopId") long shopId,
            @Param("name") String name,
            @Param("category") String category,
            @Param("price") BigDecimal price);

    @Insert(
            "INSERT INTO addresses(user_id,building,room,contact_name,phone,is_default)"
                + " VALUES(#{userId},#{building},#{room},#{contactName},#{phone},#{isDefault})")
    int insertAddress(
            @Param("userId") long userId,
            @Param("building") String building,
            @Param("room") String room,
            @Param("contactName") String contactName,
            @Param("phone") String phone,
            @Param("isDefault") boolean isDefault);

    @Select(
            "SELECT id,user_id,building,room,contact_name,phone,is_default FROM addresses WHERE"
                + " user_id=#{userId} ORDER BY is_default DESC,id")
    List<Address> listAddresses(long userId);

    @Select(
            "SELECT id,user_id,building,room,contact_name,phone,is_default FROM addresses WHERE"
                + " id=#{id} AND user_id=#{userId}")
    Address findAddress(@Param("id") long id, @Param("userId") long userId);

    @Update(
            "UPDATE addresses SET"
                + " building=#{building},room=#{room},contact_name=#{contactName},phone=#{phone}"
                + " WHERE id=#{id} AND user_id=#{userId}")
    int updateAddress(
            @Param("id") long id,
            @Param("userId") long userId,
            @Param("building") String building,
            @Param("room") String room,
            @Param("contactName") String contactName,
            @Param("phone") String phone);

    @Delete("DELETE FROM addresses WHERE id=#{id} AND user_id=#{userId}")
    int deleteAddress(@Param("id") long id, @Param("userId") long userId);

    @Update("UPDATE addresses SET is_default=(id=#{id}) WHERE user_id=#{userId}")
    int defaultAddress(@Param("id") long id, @Param("userId") long userId);

    @Insert(
            "INSERT INTO orders(order_no,student_id,shop_id,amount,address_snapshot,status,request_key,checkout_cart_version,checkout_address_id,request_hash,expires_at)"
                + " VALUES(#{orderNo},#{studentId},#{shopId},#{amount},#{addressSnapshot},#{status},#{requestKey},#{checkoutCartVersion},#{checkoutAddressId},#{requestHash},DATE_ADD(NOW(),INTERVAL 15 MINUTE))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertOrder(Order order);

    @Insert(
            "INSERT INTO order_items(order_id,dish_id,dish_name,unit_price,quantity,subtotal)"
                + " VALUES(#{orderId},#{dishId},#{dishName},#{unitPrice},#{quantity},#{subtotal})")
    int insertOrderItem(OrderItem item);

    @Select(
            "SELECT o.id,o.order_no,o.student_id,o.shop_id,s.name"
                + " shop_name,o.rider_id,o.amount,o.address_snapshot,o.status,o.created_at,o.expires_at,o.request_key,o.checkout_cart_version,o.checkout_address_id,o.request_hash FROM"
                + " orders o JOIN shops s ON s.id=o.shop_id WHERE o.id=#{id}")
    Order findOrder(long id);

    @Select("SELECT * FROM orders WHERE id=#{id} FOR UPDATE")
    Order lockOrder(long id);

    @Select(
            "SELECT id,order_id,dish_id,dish_name,unit_price,quantity,subtotal FROM order_items"
                + " WHERE order_id=#{orderId} ORDER BY id")
    List<OrderItem> listOrderItems(long orderId);

    @Update(
            "UPDATE orders SET paid_at=NOW(),status='PENDING_ACCEPT' WHERE id=#{id} AND"
                + " student_id=#{userId} AND status='UNPAID' AND (expires_at IS NULL OR expires_at > NOW())")
    int pay(@Param("id") long id, @Param("userId") long userId);

    @Update(
            "UPDATE orders SET status='CANCELED' WHERE id=#{id} AND student_id=#{userId} AND status"
                + " IN ('UNPAID','PENDING_ACCEPT')")
    int cancel(@Param("id") long id, @Param("userId") long userId);

    @Update(
            "UPDATE orders SET"
                + " status=#{to},accepted_at=IF(#{to}='MAKING',NOW(),accepted_at),ready_at=IF(#{to}='READY',NOW(),ready_at),delivered_at=IF(#{to}='WAIT_CONFIRM',NOW(),delivered_at),completed_at=IF(#{to}='COMPLETED',NOW(),completed_at)"
                + " WHERE id=#{id} AND status=#{from}")
    int changeStatus(@Param("id") long id, @Param("from") String from, @Param("to") String to);

    @Update(
            "UPDATE orders SET rider_id=#{riderId},status='DELIVERING' WHERE id=#{orderId} AND"
                + " status='READY' AND rider_id IS NULL")
    int claimOrder(@Param("orderId") long orderId, @Param("riderId") long riderId);

    @Insert(
            "INSERT INTO order_events(order_id,from_status,to_status,operator_id)"
                + " VALUES(#{orderId},#{fromStatus},#{toStatus},#{operatorId})")
    int insertEvent(
            @Param("orderId") long orderId,
            @Param("fromStatus") String from,
            @Param("toStatus") String to,
            @Param("operatorId") long operatorId);

    @Select(
            "SELECT e.from_status,e.to_status,u.real_name AS operator_name,e.created_at,e.reason FROM"
                + " order_events e JOIN users u ON u.id=e.operator_id WHERE e.order_id=#{id} ORDER"
                + " BY e.id")
    List<OrderEvent> listEvents(long id);

    @Select("SELECT id,username,password_hash,real_name,phone,role,enabled,auth_version FROM users WHERE id=#{id}")
    AppUser findUserById(long id);

    @Insert("INSERT INTO cart(user_id,checkout_key) VALUES(#{userId},#{key}) ON DUPLICATE KEY UPDATE user_id=user_id")
    int ensureCart(@Param("userId") long userId, @Param("key") String key);

    @Select("SELECT id,user_id,shop_id,version,checkout_key FROM cart WHERE user_id=#{userId}")
    cn.campus.takeaway.cart.CartState findCart(long userId);

    @Select("SELECT d.id dish_id,d.shop_id,d.name,d.price,d.on_sale,COALESCE(d.image_path,'/images/menu-assortment.webp') image_path,ci.quantity,d.price*ci.quantity subtotal FROM cart_item ci JOIN dishes d ON d.id=ci.dish_id WHERE ci.cart_id=#{cartId} ORDER BY d.id")
    List<cn.campus.takeaway.cart.CartLine> cartRows(long cartId);

    @Insert("INSERT INTO cart_item(cart_id,dish_id,quantity) VALUES(#{cartId},#{dishId},#{quantity}) ON DUPLICATE KEY UPDATE quantity=#{quantity}")
    int putCartItem(@Param("cartId") long cartId,@Param("dishId") long dishId,@Param("quantity") int quantity);

    @Delete("DELETE FROM cart_item WHERE cart_id=#{cartId} AND dish_id=#{dishId}")
    int deleteCartItem(@Param("cartId") long cartId,@Param("dishId") long dishId);

    @Delete("DELETE FROM cart_item WHERE cart_id=#{cartId}")
    int clearCartItems(long cartId);

    @Update("UPDATE cart SET shop_id=#{shopId},version=version+1,checkout_key=#{key} WHERE id=#{id}")
    int rotateCart(@Param("id") long id,@Param("shopId") Long shopId,@Param("key") String key);

    @Select("SELECT * FROM orders WHERE student_id=#{userId} AND request_key=#{key}")
    Order findSubmitted(@Param("userId") long userId,@Param("key") String key);

    @Select("<script>SELECT * FROM order_items WHERE order_id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> ORDER BY id</script>")
    List<OrderItem> batchOrderItems(@Param("ids") List<Long> ids);

    String ORDER_FILTER = """
        <where><choose>
        <when test="role == 'STUDENT'">o.student_id=#{userId}</when>
        <when test="role == 'MERCHANT'">s.owner_id=#{userId}</when>
        <when test="role == 'RIDER'"><choose><when test="scope == 'available'">o.status='READY' AND o.rider_id IS NULL</when><otherwise>o.rider_id=#{userId}</otherwise></choose></when>
        <when test="role == 'ADMIN'">1=1</when><otherwise>1=0</otherwise></choose>
        <if test="status != null and status != ''"> AND o.status=#{status}</if>
        <if test="query != null and query != ''"> AND (o.order_no LIKE CONCAT('%',#{query},'%') OR s.name LIKE CONCAT('%',#{query},'%'))</if></where>
        """;

    @Select("<script>SELECT o.*,s.name shop_name FROM orders o JOIN shops s ON s.id=o.shop_id " + ORDER_FILTER + " ORDER BY o.id DESC LIMIT #{size} OFFSET #{offset}</script>")
    List<Order> pageOrders(@Param("role") String role,@Param("userId") long userId,@Param("scope") String scope,@Param("status") String status,@Param("query") String query,@Param("offset") long offset,@Param("size") int size);

    @Select("<script>SELECT COUNT(*) FROM orders o JOIN shops s ON s.id=o.shop_id " + ORDER_FILTER + "</script>")
    long countOrders(@Param("role") String role,@Param("userId") long userId,@Param("scope") String scope,@Param("status") String status,@Param("query") String query);

    String SHOP_FILTER = " WHERE (#{admin}=true OR (s.is_open=1 AND u.enabled=1)) AND (s.name LIKE CONCAT('%',#{query},'%') OR s.description LIKE CONCAT('%',#{query},'%')) ";
    @Select("SELECT s.id,s.owner_id,s.name,s.description,s.is_open AS open,(SELECT d.image_path FROM dishes d WHERE d.shop_id=s.id AND d.on_sale=1 AND d.image_path IS NOT NULL ORDER BY d.id LIMIT 1) image_path FROM shops s JOIN users u ON u.id=s.owner_id" + SHOP_FILTER + "ORDER BY s.id LIMIT #{size} OFFSET #{offset}")
    List<Shop> pageShops(@Param("admin") boolean admin,@Param("query") String query,@Param("size") int size,@Param("offset") long offset);
    @Select("SELECT COUNT(*) FROM shops s JOIN users u ON u.id=s.owner_id" + SHOP_FILTER)
    long countShops(@Param("admin") boolean admin,@Param("query") String query);

    @Select("SELECT id,username,real_name,phone,role,enabled,auth_version FROM users WHERE username LIKE CONCAT('%',#{query},'%') OR real_name LIKE CONCAT('%',#{query},'%') ORDER BY id DESC LIMIT #{size} OFFSET #{offset}")
    List<AppUser> pageUsers(@Param("query") String query,@Param("size") int size,@Param("offset") long offset);
    @Select("SELECT COUNT(*) FROM users WHERE username LIKE CONCAT('%',#{query},'%') OR real_name LIKE CONCAT('%',#{query},'%')")
    long countFilteredUsers(String query);

    @Select("SELECT d.*,s.name shop_name FROM dishes d JOIN shops s ON s.id=d.shop_id WHERE (#{shopId} IS NULL OR d.shop_id=#{shopId}) AND (d.name LIKE CONCAT('%',#{query},'%') OR d.category LIKE CONCAT('%',#{query},'%')) ORDER BY d.id DESC LIMIT #{size} OFFSET #{offset}")
    List<Dish> pageDishes(@Param("shopId") Long shopId,@Param("query") String query,@Param("size") int size,@Param("offset") long offset);
    @Select("SELECT COUNT(*) FROM dishes WHERE (#{shopId} IS NULL OR shop_id=#{shopId}) AND (name LIKE CONCAT('%',#{query},'%') OR category LIKE CONCAT('%',#{query},'%'))")
    long countDishes(@Param("shopId") Long shopId,@Param("query") String query);

    @Select("SELECT id FROM orders WHERE status='UNPAID' AND expires_at <= NOW() ORDER BY expires_at,id LIMIT 100")
    List<Long> expiredOrderIds();
    @Update("UPDATE orders SET status='CANCELED' WHERE id=#{id} AND status='UNPAID' AND expires_at <= NOW()")
    int expireOrder(long id);
    @Insert("INSERT INTO order_events(order_id,from_status,to_status,operator_id,reason) SELECT id,'UNPAID','CANCELED',student_id,'PAYMENT_TIMEOUT' FROM orders WHERE id=#{id}")
    int insertTimeoutEvent(long id);

    @Select("SELECT id,shop_id,category,name,price,image_path,on_sale FROM dishes WHERE shop_id=#{shopId} AND on_sale=1 AND (name LIKE CONCAT('%',#{query},'%') OR category LIKE CONCAT('%',#{query},'%')) ORDER BY id LIMIT #{size} OFFSET #{offset}")
    List<Dish> pagePublicMenu(@Param("shopId") long shopId,@Param("query") String query,@Param("size") int size,@Param("offset") long offset);
    @Select("SELECT COUNT(*) FROM dishes WHERE shop_id=#{shopId} AND on_sale=1 AND (name LIKE CONCAT('%',#{query},'%') OR category LIKE CONCAT('%',#{query},'%'))")
    long countPublicMenu(@Param("shopId") long shopId,@Param("query") String query);
}
