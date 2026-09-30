package cn.campus.takeaway.service;

import cn.campus.takeaway.domain.*;
import cn.campus.takeaway.mapper.AppMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class OrderService {
    private final AppMapper mapper;
    public OrderService(AppMapper mapper){this.mapper=mapper;}

    public List<Order> studentOrders(long userId){ return withItems(mapper.listStudentOrders(userId)); }
    public List<Order> shopOrders(long shopId){ return withItems(mapper.listShopOrders(shopId)); }
    public List<Order> riderOrders(long riderId){ return withItems(mapper.listRiderOrders(riderId)); }
    public List<Order> allOrders(){ return withItems(mapper.listAllOrders()); }
    public Order get(long id){ var order=mapper.findOrder(id); if(order!=null) order.setItems(mapper.listOrderItems(id)); return order; }
    private List<Order> withItems(List<Order> orders){ orders.forEach(o->o.setItems(mapper.listOrderItems(o.getId()))); return orders; }

    public Cart reorder(long id,long studentId){
        Order order=ownedStudent(id,studentId);
        if(!Set.of("COMPLETED","CANCELED").contains(order.getStatus()))throw new IllegalStateException("请先完成或取消当前订单");
        Shop shop=mapper.findShop(order.getShopId());
        if(shop==null || !Boolean.TRUE.equals(shop.getOpen()) || !Boolean.TRUE.equals(mapper.userEnabled(shop.getOwnerId())))throw new IllegalArgumentException("店铺休息中或已停用，暂时无法再次购买");
        Cart cart=new Cart();
        for(var item:mapper.listOrderItems(id)){
            Dish dish=mapper.findDish(item.getDishId());
            if(dish==null || !Boolean.TRUE.equals(dish.getOnSale()) || !Objects.equals(dish.getShopId(),order.getShopId()))throw new IllegalArgumentException("原订单中有菜品已下架，请到店铺重新选餐");
            if(item.getQuantity()<1 || item.getQuantity()>99)throw new IllegalArgumentException("原订单菜品数量不合法，请重新选餐");
            for(int index=0;index<item.getQuantity();index++)cart.add(shop.getId(),dish.getId());
        }
        if(cart.getQuantities().isEmpty())throw new IllegalArgumentException("原订单没有可购买的菜品");
        return cart;
    }

    @Transactional
    public Order create(long studentId, Cart cart, long addressId){
        if(cart==null || cart.getQuantities().isEmpty()) throw new IllegalArgumentException("购物车为空");
        Address address=mapper.findAddress(addressId,studentId); if(address==null) throw new IllegalArgumentException("地址不存在");
        Shop shop=mapper.findShop(cart.getShopId());
        if(shop==null || !Boolean.TRUE.equals(shop.getOpen()) || !Boolean.TRUE.equals(mapper.userEnabled(shop.getOwnerId()))) throw new IllegalArgumentException("店铺休息中或已停用，暂时无法下单");
        BigDecimal total=BigDecimal.ZERO; List<OrderItem> items=new ArrayList<>(); Long shopId=cart.getShopId();
        for(var entry: cart.getQuantities().entrySet()){
            Dish dish=mapper.findDish(entry.getKey());
            if(dish==null || !Boolean.TRUE.equals(dish.getOnSale()) || !Objects.equals(shopId,dish.getShopId())) throw new IllegalArgumentException("菜品已下架或不属于同一店铺");
            int quantity=entry.getValue(); if(quantity<1 || quantity>99) throw new IllegalArgumentException("菜品数量不合法");
            BigDecimal subtotal=dish.getPrice().multiply(BigDecimal.valueOf(quantity)); total=total.add(subtotal);
            OrderItem item=new OrderItem(); item.setDishId(dish.getId()); item.setDishName(dish.getName()); item.setUnitPrice(dish.getPrice()); item.setQuantity(quantity); item.setSubtotal(subtotal); items.add(item);
        }
        Order order=new Order(); order.setOrderNo("CT"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))+UUID.randomUUID().toString().substring(0,6).toUpperCase());
        order.setStudentId(studentId); order.setShopId(shopId); order.setAmount(total); order.setAddressSnapshot(address.snapshot()); order.setStatus(OrderStatus.UNPAID.name()); mapper.insertOrder(order);
        for(OrderItem item: items){ item.setOrderId(order.getId()); mapper.insertOrderItem(item); }
        mapper.insertEvent(order.getId(),null,OrderStatus.UNPAID.name(),studentId);
        return order;
    }

    @Transactional public void pay(long id,long userId){ transition(id,userId,OrderStatus.UNPAID,OrderStatus.PENDING_ACCEPT,mapper.pay(id,userId)); }
    @Transactional public void cancel(long id,long userId){ Order order=mapper.lockOrder(id); if(order==null || !Objects.equals(order.getStudentId(),userId)) throw new IllegalStateException("无权操作该订单"); transition(id,userId,OrderStatus.valueOf(order.getStatus()),OrderStatus.CANCELED,mapper.cancel(id,userId)); }
    @Transactional public void confirm(long id,long userId){ Order o=ownedStudent(id,userId); transition(id,userId,OrderStatus.WAIT_CONFIRM,OrderStatus.COMPLETED,mapper.changeStatus(id,OrderStatus.WAIT_CONFIRM.name(),OrderStatus.COMPLETED.name())); }
    @Transactional public void merchantAccept(long id,long ownerId){ merchantTransition(id,ownerId,OrderStatus.PENDING_ACCEPT,OrderStatus.MAKING); }
    @Transactional public void merchantReady(long id,long ownerId){ merchantTransition(id,ownerId,OrderStatus.MAKING,OrderStatus.READY); }
    @Transactional public void riderClaim(long id,long riderId){ if(mapper.claimOrder(id,riderId)!=1) throw new IllegalStateException("订单已被其他骑手接取或状态不允许"); mapper.insertEvent(id,OrderStatus.READY.name(),OrderStatus.DELIVERING.name(),riderId); }
    @Transactional public void riderDelivered(long id,long riderId){ Order o=mapper.findOrder(id); if(o==null || !Objects.equals(o.getRiderId(),riderId)) throw new IllegalStateException("无权操作该订单"); transition(id,riderId,OrderStatus.DELIVERING,OrderStatus.WAIT_CONFIRM,mapper.changeStatus(id,OrderStatus.DELIVERING.name(),OrderStatus.WAIT_CONFIRM.name())); }
    private void merchantTransition(long id,long ownerId,OrderStatus from,OrderStatus to){ Order o=mapper.findOrder(id); Shop shop=mapper.findShopByOwner(ownerId); if(o==null || shop==null || !Objects.equals(shop.getId(),o.getShopId())) throw new IllegalStateException("无权操作该订单"); transition(id,ownerId,from,to,mapper.changeStatus(id,from.name(),to.name())); }
    private Order ownedStudent(long id,long userId){ Order o=mapper.findOrder(id); if(o==null || !Objects.equals(o.getStudentId(),userId)) throw new IllegalStateException("无权操作该订单"); return o; }
    private void transition(long id,long operatorId,OrderStatus from,OrderStatus to,int changed){ if(changed!=1) throw new IllegalStateException("订单状态已变化，请刷新后重试"); mapper.insertEvent(id,from.name(),to.name(),operatorId); }
    public Order authorizedOrder(long id, AppUser user) {
        Order order=get(id); if(order==null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"订单不存在");
        boolean allowed=switch(user.getRole()) {
            case "ADMIN" -> true;
            case "STUDENT" -> Objects.equals(order.getStudentId(),user.getId());
            case "RIDER" -> Objects.equals(order.getRiderId(),user.getId()) || ("READY".equals(order.getStatus()) && order.getRiderId()==null);
            case "MERCHANT" -> { Shop shop=mapper.findShopByOwner(user.getId()); yield shop!=null && Objects.equals(order.getShopId(),shop.getId()); }
            default -> false;
        };
        if(!allowed) throw new org.springframework.security.access.AccessDeniedException("无权查看该订单");
        return order;
    }
}
