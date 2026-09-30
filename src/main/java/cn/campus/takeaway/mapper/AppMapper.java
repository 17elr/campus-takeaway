package cn.campus.takeaway.mapper;

import cn.campus.takeaway.domain.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface AppMapper {
    @Select("SELECT id,username,password_hash,real_name,phone,role,enabled FROM users WHERE username=#{username}") AppUser findUser(String username);
    @Select("SELECT COUNT(*) FROM users") long countUsers();
    @Select("SELECT id FROM users WHERE id=#{id} FOR UPDATE") Long lockUser(long id);
    @Update("UPDATE users SET real_name=#{name},phone=#{phone} WHERE id=#{id} AND enabled=1") int updateProfile(@Param("id") long id,@Param("name") String name,@Param("phone") String phone);
    @Update("UPDATE users SET password_hash=#{password} WHERE id=#{id} AND enabled=1") int updatePassword(@Param("id") long id,@Param("password") String password);
    @Insert("INSERT INTO users(username,password_hash,real_name,phone,role) VALUES(#{username},#{password},#{name},#{phone},#{role})") int insertUser(@Param("username") String username,@Param("password") String password,@Param("name") String name,@Param("phone") String phone,@Param("role") String role);
    @Update("UPDATE users SET enabled=#{enabled} WHERE id=#{id}") int setUserEnabled(@Param("id") long id,@Param("enabled") boolean enabled);
    @Select("SELECT id,username,real_name,phone,role,enabled FROM users ORDER BY id DESC") List<AppUser> listUsers();

    @Insert("INSERT INTO shops(owner_id,name,description,is_open) VALUES(#{ownerId},#{name},#{description},#{open})") int insertShop(@Param("ownerId") long ownerId,@Param("name") String name,@Param("description") String description,@Param("open") boolean open);
    @Select("SELECT id,owner_id,name,description,is_open AS open FROM shops WHERE id=#{id}") Shop findShop(long id);
    @Select("SELECT id,owner_id,name,description,is_open AS open FROM shops WHERE owner_id=#{ownerId}") Shop findShopByOwner(long ownerId);
    @Select("SELECT s.id,s.owner_id,s.name,s.description,s.is_open AS open FROM shops s JOIN users u ON u.id=s.owner_id WHERE s.is_open=1 AND u.enabled=1 ORDER BY s.id") List<Shop> listOpenShops();
    @Select("SELECT id,owner_id,name,description,is_open AS open FROM shops ORDER BY id") List<Shop> listShops();
    @Update("UPDATE shops SET name=#{name},description=#{description},is_open=#{open} WHERE owner_id=#{ownerId}") int updateShop(@Param("ownerId") long ownerId,@Param("name") String name,@Param("description") String description,@Param("open") boolean open);
    @Update("UPDATE shops SET is_open=#{open} WHERE id=#{id}") int setShopOpen(@Param("id") long id,@Param("open") boolean open);
    @Select("SELECT enabled FROM users WHERE id=#{id}") Boolean userEnabled(long id);
    @Select("SELECT id,shop_id,category,name,price,image_path,on_sale FROM dishes WHERE shop_id=#{shopId} ORDER BY id") List<Dish> listDishes(long shopId);
    @Select("SELECT d.id,d.shop_id,d.category,d.name,d.price,d.image_path,d.on_sale,s.name shop_name FROM dishes d JOIN shops s ON s.id=d.shop_id WHERE d.id=#{id}") Dish findDish(long id);
    @Insert("INSERT INTO dishes(shop_id,category,name,price,on_sale) VALUES(#{shopId},#{category},#{name},#{price},1)") int insertDish(@Param("shopId") long shopId,@Param("category") String category,@Param("name") String name,@Param("price") BigDecimal price);
    @Update("UPDATE dishes SET on_sale=NOT on_sale WHERE id=#{id} AND shop_id=#{shopId}") int toggleDish(@Param("id") long id,@Param("shopId") long shopId);
    @Update("UPDATE dishes SET name=#{name},category=#{category},price=#{price} WHERE id=#{id} AND shop_id=#{shopId}") int updateDish(@Param("id") long id,@Param("shopId") long shopId,@Param("name") String name,@Param("category") String category,@Param("price") BigDecimal price);

    @Insert("INSERT INTO addresses(user_id,building,room,contact_name,phone,is_default) VALUES(#{userId},#{building},#{room},#{contactName},#{phone},#{isDefault})") int insertAddress(@Param("userId") long userId,@Param("building") String building,@Param("room") String room,@Param("contactName") String contactName,@Param("phone") String phone,@Param("isDefault") boolean isDefault);
    @Select("SELECT id,user_id,building,room,contact_name,phone,is_default FROM addresses WHERE user_id=#{userId} ORDER BY is_default DESC,id") List<Address> listAddresses(long userId);
    @Select("SELECT id,user_id,building,room,contact_name,phone,is_default FROM addresses WHERE id=#{id} AND user_id=#{userId}") Address findAddress(@Param("id") long id,@Param("userId") long userId);
    @Update("UPDATE addresses SET building=#{building},room=#{room},contact_name=#{contactName},phone=#{phone} WHERE id=#{id} AND user_id=#{userId}") int updateAddress(@Param("id") long id,@Param("userId") long userId,@Param("building") String building,@Param("room") String room,@Param("contactName") String contactName,@Param("phone") String phone);
    @Delete("DELETE FROM addresses WHERE id=#{id} AND user_id=#{userId}") int deleteAddress(@Param("id") long id,@Param("userId") long userId);
    @Update("UPDATE addresses SET is_default=(id=#{id}) WHERE user_id=#{userId}") int defaultAddress(@Param("id") long id,@Param("userId") long userId);

    @Insert("INSERT INTO orders(order_no,student_id,shop_id,amount,address_snapshot,status) VALUES(#{orderNo},#{studentId},#{shopId},#{amount},#{addressSnapshot},#{status})") @Options(useGeneratedKeys=true,keyProperty="id") int insertOrder(Order order);
    @Insert("INSERT INTO order_items(order_id,dish_id,dish_name,unit_price,quantity,subtotal) VALUES(#{orderId},#{dishId},#{dishName},#{unitPrice},#{quantity},#{subtotal})") int insertOrderItem(OrderItem item);
    @Select("SELECT o.id,o.order_no,o.student_id,o.shop_id,s.name shop_name,o.rider_id,o.amount,o.address_snapshot,o.status,o.created_at FROM orders o JOIN shops s ON s.id=o.shop_id WHERE o.id=#{id}") Order findOrder(long id);
    @Select("SELECT * FROM orders WHERE id=#{id} FOR UPDATE") Order lockOrder(long id);
    @Select("SELECT o.id,o.order_no,o.student_id,o.shop_id,s.name shop_name,o.rider_id,o.amount,o.address_snapshot,o.status,o.created_at FROM orders o JOIN shops s ON s.id=o.shop_id WHERE o.student_id=#{studentId} ORDER BY o.id DESC") List<Order> listStudentOrders(long studentId);
    @Select("SELECT id,order_id,dish_id,dish_name,unit_price,quantity,subtotal FROM order_items WHERE order_id=#{orderId} ORDER BY id") List<OrderItem> listOrderItems(long orderId);
    @Select("SELECT o.id,o.order_no,o.student_id,o.shop_id,s.name shop_name,o.rider_id,o.amount,o.address_snapshot,o.status,o.created_at FROM orders o JOIN shops s ON s.id=o.shop_id WHERE o.shop_id=#{shopId} ORDER BY o.id DESC") List<Order> listShopOrders(long shopId);
    @Select("SELECT o.id,o.order_no,o.student_id,o.shop_id,s.name shop_name,o.rider_id,o.amount,o.address_snapshot,o.status,o.created_at FROM orders o JOIN shops s ON s.id=o.shop_id WHERE o.rider_id=#{riderId} OR (o.status='READY' AND o.rider_id IS NULL) ORDER BY o.id DESC") List<Order> listRiderOrders(long riderId);
    @Select("SELECT o.id,o.order_no,o.student_id,o.shop_id,s.name shop_name,o.rider_id,o.amount,o.address_snapshot,o.status,o.created_at FROM orders o JOIN shops s ON s.id=o.shop_id ORDER BY o.id DESC") List<Order> listAllOrders();
    @Update("UPDATE orders SET paid_at=NOW(),status='PENDING_ACCEPT' WHERE id=#{id} AND student_id=#{userId} AND status='UNPAID'") int pay(@Param("id") long id,@Param("userId") long userId);
    @Update("UPDATE orders SET status='CANCELED' WHERE id=#{id} AND student_id=#{userId} AND status IN ('UNPAID','PENDING_ACCEPT')") int cancel(@Param("id") long id,@Param("userId") long userId);
    @Update("UPDATE orders SET status=#{to},accepted_at=IF(#{to}='MAKING',NOW(),accepted_at),ready_at=IF(#{to}='READY',NOW(),ready_at),delivered_at=IF(#{to}='WAIT_CONFIRM',NOW(),delivered_at),completed_at=IF(#{to}='COMPLETED',NOW(),completed_at) WHERE id=#{id} AND status=#{from}") int changeStatus(@Param("id") long id,@Param("from") String from,@Param("to") String to);
    @Update("UPDATE orders SET rider_id=#{riderId},status='DELIVERING' WHERE id=#{orderId} AND status='READY' AND rider_id IS NULL") int claimOrder(@Param("orderId") long orderId,@Param("riderId") long riderId);
    @Insert("INSERT INTO order_events(order_id,from_status,to_status,operator_id) VALUES(#{orderId},#{fromStatus},#{toStatus},#{operatorId})") int insertEvent(@Param("orderId") long orderId,@Param("fromStatus") String from,@Param("toStatus") String to,@Param("operatorId") long operatorId);
    @Select("SELECT e.from_status,e.to_status,u.real_name AS operator_name,e.created_at FROM order_events e JOIN users u ON u.id=e.operator_id WHERE e.order_id=#{id} ORDER BY e.id") List<OrderEvent> listEvents(long id);
}
