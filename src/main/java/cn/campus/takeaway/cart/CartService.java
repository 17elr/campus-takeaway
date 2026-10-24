package cn.campus.takeaway.cart;

import cn.campus.takeaway.domain.Dish;
import cn.campus.takeaway.mapper.AppMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class CartService {
    private final AppMapper mapper;
    public CartService(AppMapper mapper) { this.mapper = mapper; }

    public record View(Long shopId, long version, String checkoutKey, List<CartLine> items, BigDecimal total) {}

    @Transactional
    public View get(long userId) {
        lock(userId);
        return view(userId);
    }

    @Transactional
    public View put(long userId, long dishId, int quantity) {
        if (dishId < 1 || quantity < 1 || quantity > 99)
            throw new IllegalArgumentException("菜品数量须为 1–99 的整数");
        CartState cart = lock(userId);
        Dish dish = available(dishId);
        if (cart.shopId() != null && !cart.shopId().equals(dish.getShopId()))
            throw new IllegalStateException("一次只能选择一家店铺，请先清空购物车");
        mapper.putCartItem(cart.id(), dishId, quantity);
        mapper.rotateCart(cart.id(), dish.getShopId(), UUID.randomUUID().toString());
        return view(userId);
    }

    @Transactional
    public View remove(long userId, long dishId) {
        CartState cart = lock(userId);
        if (mapper.deleteCartItem(cart.id(), dishId) != 1)
            throw new IllegalArgumentException("购物车中没有该菜品");
        Long shopId = mapper.cartRows(cart.id()).isEmpty() ? null : cart.shopId();
        mapper.rotateCart(cart.id(), shopId, UUID.randomUUID().toString());
        return view(userId);
    }

    @Transactional
    public View clear(long userId) {
        CartState cart = lock(userId);
        mapper.clearCartItems(cart.id());
        mapper.rotateCart(cart.id(), null, UUID.randomUUID().toString());
        return view(userId);
    }

    // All writers acquire the same database user lock before their first cart/order read.
    public CartState lock(long userId) {
        if (mapper.lockUser(userId) == null) throw new IllegalArgumentException("账号不存在");
        mapper.ensureCart(userId, UUID.randomUUID().toString());
        return mapper.findCart(userId);
    }

    public Dish available(long dishId) {
        Dish dish = mapper.findDish(dishId);
        if (dish == null || !Boolean.TRUE.equals(dish.getOnSale()))
            throw new IllegalArgumentException("菜品已下架或不存在");
        var shop = mapper.findShop(dish.getShopId());
        if (shop == null || !Boolean.TRUE.equals(shop.getOpen()) || !Boolean.TRUE.equals(mapper.userEnabled(shop.getOwnerId())))
            throw new IllegalArgumentException("店铺休息中或已停用");
        return dish;
    }

    public View view(long userId) {
        var cart = mapper.findCart(userId);
        var items = mapper.cartRows(cart.id());
        return new View(cart.shopId(), cart.version(), cart.checkoutKey(), items,
                items.stream().map(CartLine::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
