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
    private final cn.campus.takeaway.cart.CartService carts;

    public OrderService(AppMapper mapper, cn.campus.takeaway.cart.CartService carts) {
        this.mapper = mapper;
        this.carts = carts;
    }

    @Transactional(readOnly=true)
    public cn.campus.takeaway.controller.PageResult<Order> page(AppUser user, int page, int size, String status, String scope, String query) {
        cn.campus.takeaway.controller.PageResult.validate(page, size);
        if (status != null && !status.isEmpty()) {
            try { OrderStatus.valueOf(status); } catch (IllegalArgumentException invalid) { throw new IllegalArgumentException("未知订单状态"); }
        }
        if (!java.util.Set.of("mine", "available").contains(scope)) throw new IllegalArgumentException("未知订单范围");
        if (query.length() > 80) throw new IllegalArgumentException("搜索词最多 80 字");
        var rows = mapper.pageOrders(user.getRole(), user.getId(), scope, status, query, (long)(page-1)*size, size);
        if (!rows.isEmpty()) {
            var grouped = mapper.batchOrderItems(rows.stream().map(Order::getId).toList()).stream()
                .collect(java.util.stream.Collectors.groupingBy(OrderItem::getOrderId));
            rows.forEach(order -> order.setItems(grouped.getOrDefault(order.getId(), List.of())));
        }
        return new cn.campus.takeaway.controller.PageResult<>(rows, mapper.countOrders(user.getRole(),user.getId(),scope,status,query),page,size);
    }

    public Order get(long id) {
        var order = mapper.findOrder(id);
        if (order != null) order.setItems(mapper.listOrderItems(id));
        return order;
    }

    @Transactional
    public cn.campus.takeaway.cart.CartService.View reorder(long id, long studentId) {
        var cart = carts.lock(studentId);
        Order order = ownedStudent(id, studentId);
        if (!Set.of("COMPLETED", "CANCELED").contains(order.getStatus()))
            throw new IllegalStateException("请先完成或取消当前订单");
        var items = mapper.listOrderItems(id);
        if (items.isEmpty()) throw new IllegalArgumentException("原订单没有可购买的菜品");
        for (var item : items) {
            var dish = carts.available(item.getDishId());
            if (!Objects.equals(dish.getShopId(), order.getShopId()) || item.getQuantity() < 1 || item.getQuantity() > 99)
                throw new IllegalArgumentException("原订单菜品已变化，请重新选餐");
        }
        mapper.clearCartItems(cart.id());
        for (var item : items) mapper.putCartItem(cart.id(), item.getDishId(), item.getQuantity());
        mapper.rotateCart(cart.id(), order.getShopId(), UUID.randomUUID().toString());
        return carts.view(studentId);
    }

    @Transactional
    public Order create(long studentId, String checkoutKey, long cartVersion, long addressId) {
        if (checkoutKey == null || !checkoutKey.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}") || cartVersion < 0 || addressId < 1)
            throw new IllegalArgumentException("结算参数不合法，请刷新购物车");
        if (mapper.lockUser(studentId) == null) throw new IllegalArgumentException("账号不存在");
        String hash;
        try {
            hash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(
                (checkoutKey + "|" + cartVersion + "|" + addressId).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
        var submitted = mapper.findSubmitted(studentId, checkoutKey);
        if (submitted != null) {
            if (!hash.equals(submitted.getRequestHash())) throw new IllegalStateException("同一结算凭证不能更换地址或购物车版本");
            return get(submitted.getId());
        }
        var cart = mapper.findCart(studentId);
        if (cart == null || cart.version() != cartVersion || !checkoutKey.equals(cart.checkoutKey()))
            throw new IllegalStateException("购物车已变化，请刷新后重新结算");
        var rows = mapper.cartRows(cart.id());
        if (rows.isEmpty()) throw new IllegalArgumentException("购物车为空");
        Address address = mapper.findAddress(addressId, studentId);
        if (address == null) throw new IllegalArgumentException("地址不存在或不属于当前学生");
        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        Long shopId = cart.shopId();
        for (var entry : rows) {
            Dish dish = carts.available(entry.dishId());
            if (dish == null
                    || !Boolean.TRUE.equals(dish.getOnSale())
                    || !Objects.equals(shopId, dish.getShopId()))
                throw new IllegalArgumentException("菜品已下架或不属于同一店铺");
            int quantity = entry.quantity();
            if (quantity < 1 || quantity > 99) throw new IllegalArgumentException("菜品数量不合法");
            BigDecimal subtotal = dish.getPrice().multiply(BigDecimal.valueOf(quantity));
            total = total.add(subtotal);
            OrderItem item = new OrderItem();
            item.setDishId(dish.getId());
            item.setDishName(dish.getName());
            item.setUnitPrice(dish.getPrice());
            item.setQuantity(quantity);
            item.setSubtotal(subtotal);
            items.add(item);
        }
        Order order = new Order();
        order.setOrderNo(
                "CT"
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                        + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        order.setStudentId(studentId);
        order.setShopId(shopId);
        order.setAmount(total);
        order.setAddressSnapshot(address.snapshot());
        order.setStatus(OrderStatus.UNPAID.name());
        order.setRequestKey(checkoutKey);
        order.setCheckoutCartVersion(cartVersion);
        order.setCheckoutAddressId(addressId);
        order.setRequestHash(hash);
        mapper.insertOrder(order);
        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            mapper.insertOrderItem(item);
        }
        mapper.insertEvent(order.getId(), null, OrderStatus.UNPAID.name(), studentId);
        mapper.clearCartItems(cart.id());
        mapper.rotateCart(cart.id(), null, UUID.randomUUID().toString());
        return get(order.getId());
    }

    @Transactional
    public void pay(long id, long userId) {
        transition(
                id, userId, OrderStatus.UNPAID, OrderStatus.PENDING_ACCEPT, mapper.pay(id, userId));
    }

    @Transactional
    public void cancel(long id, long userId) {
        Order order = mapper.lockOrder(id);
        if (order == null || !Objects.equals(order.getStudentId(), userId))
            throw new IllegalStateException("无权操作该订单");
        transition(
                id,
                userId,
                OrderStatus.valueOf(order.getStatus()),
                OrderStatus.CANCELED,
                mapper.cancel(id, userId));
    }

    @Transactional
    public void confirm(long id, long userId) {
        Order o = ownedStudent(id, userId);
        transition(
                id,
                userId,
                OrderStatus.WAIT_CONFIRM,
                OrderStatus.COMPLETED,
                mapper.changeStatus(
                        id, OrderStatus.WAIT_CONFIRM.name(), OrderStatus.COMPLETED.name()));
    }

    @Transactional
    public void merchantAccept(long id, long ownerId) {
        merchantTransition(id, ownerId, OrderStatus.PENDING_ACCEPT, OrderStatus.MAKING);
    }

    @Transactional
    public void merchantReady(long id, long ownerId) {
        merchantTransition(id, ownerId, OrderStatus.MAKING, OrderStatus.READY);
    }

    @Transactional
    public void riderClaim(long id, long riderId) {
        if (mapper.claimOrder(id, riderId) != 1)
            throw new IllegalStateException("订单已被其他骑手接取或状态不允许");
        mapper.insertEvent(id, OrderStatus.READY.name(), OrderStatus.DELIVERING.name(), riderId);
    }

    @Transactional
    public void riderDelivered(long id, long riderId) {
        Order o = mapper.findOrder(id);
        if (o == null || !Objects.equals(o.getRiderId(), riderId))
            throw new IllegalStateException("无权操作该订单");
        transition(
                id,
                riderId,
                OrderStatus.DELIVERING,
                OrderStatus.WAIT_CONFIRM,
                mapper.changeStatus(
                        id, OrderStatus.DELIVERING.name(), OrderStatus.WAIT_CONFIRM.name()));
    }

    private void merchantTransition(long id, long ownerId, OrderStatus from, OrderStatus to) {
        Order o = mapper.findOrder(id);
        Shop shop = mapper.findShopByOwner(ownerId);
        if (o == null || shop == null || !Objects.equals(shop.getId(), o.getShopId()))
            throw new IllegalStateException("无权操作该订单");
        transition(id, ownerId, from, to, mapper.changeStatus(id, from.name(), to.name()));
    }

    private Order ownedStudent(long id, long userId) {
        Order o = mapper.findOrder(id);
        if (o == null || !Objects.equals(o.getStudentId(), userId))
            throw new IllegalStateException("无权操作该订单");
        return o;
    }

    private void transition(
            long id, long operatorId, OrderStatus from, OrderStatus to, int changed) {
        if (changed != 1) throw new IllegalStateException("订单状态已变化，请刷新后重试");
        mapper.insertEvent(id, from.name(), to.name(), operatorId);
    }

    public Order authorizedOrder(long id, AppUser user) {
        Order order = get(id);
        if (order == null)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "订单不存在");
        boolean allowed =
                switch (user.getRole()) {
                    case "ADMIN" -> true;
                    case "STUDENT" -> Objects.equals(order.getStudentId(), user.getId());
                    case "RIDER" ->
                            Objects.equals(order.getRiderId(), user.getId())
                                    || ("READY".equals(order.getStatus())
                                            && order.getRiderId() == null);
                    case "MERCHANT" -> {
                        Shop shop = mapper.findShopByOwner(user.getId());
                        yield shop != null && Objects.equals(order.getShopId(), shop.getId());
                    }
                    default -> false;
                };
        if (!allowed)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "无权查看该订单");
        return order;
    }
}
