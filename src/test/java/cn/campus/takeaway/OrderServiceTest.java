package cn.campus.takeaway;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.campus.takeaway.domain.*;
import cn.campus.takeaway.mapper.AppMapper;
import cn.campus.takeaway.service.OrderService;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

class OrderServiceTest {
    @Test
    void dishPhotosUseStoredImageOrMatchingDemoPhoto() {
        Dish dish = new Dish();
        dish.setName("香辣鸡腿饭");
        assertEquals("/images/chicken-rice.webp", dish.getImagePath());
        dish.setName("番茄鸡蛋面");
        assertEquals("/images/tomato-noodles.webp", dish.getImagePath());
        dish.setName("柠檬茶");
        assertEquals("/images/lemon-tea.webp", dish.getImagePath());
        dish.setName("新餐品");
        assertEquals("/images/menu-assortment.webp", dish.getImagePath());
        dish.setImagePath("/images/shop-custom.webp");
        assertEquals("/images/shop-custom.webp", dish.getImagePath());
    }

    @Test
    void serverRecalculatesAmountFromCurrentDishPrices() {
        AppMapper mapper = mock(AppMapper.class);
        OrderService service = new OrderService(mapper, new cn.campus.takeaway.cart.CartService(mapper));
        Address address = new Address();
        address.setId(1L);
        address.setBuilding("一号楼");
        address.setRoom("302");
        address.setContactName("张三");
        address.setPhone("1");
        Dish dish = new Dish();
        dish.setId(8L);
        dish.setShopId(2L);
        dish.setName("套餐");
        dish.setPrice(new BigDecimal("12.50"));
        dish.setOnSale(true);
        when(mapper.findAddress(1L, 1L)).thenReturn(address);
        when(mapper.findDish(8L)).thenReturn(dish);
        doAnswer(
                        invocation -> {
                            invocation.<Order>getArgument(0).setId(42L);
                            return 1;
                        })
                .when(mapper)
                .insertOrder(any(Order.class));
        Shop shop = new Shop();
        shop.setId(2L);
        shop.setOwnerId(4L);
        shop.setOpen(true);
        when(mapper.findShop(2L)).thenReturn(shop);
        when(mapper.userEnabled(4L)).thenReturn(true);
        String key = "00000000-0000-0000-0000-000000000001";
        when(mapper.lockUser(1L)).thenReturn(1L);
        when(mapper.findCart(1L)).thenReturn(new cn.campus.takeaway.cart.CartState(1L,1L,2L,1L,key));
        when(mapper.cartRows(1L)).thenReturn(java.util.List.of(new cn.campus.takeaway.cart.CartLine(8L,2L,"套餐",new BigDecimal("12.50"),true,"",2,new BigDecimal("25.00"))));
        service.create(1L, key, 1L, 1L);
        ArgumentCaptor<Order> captured = ArgumentCaptor.forClass(Order.class);
        verify(mapper).insertOrder(captured.capture());
        assertEquals(new BigDecimal("25.00"), captured.getValue().getAmount());
    }

    @Test
    void payRejectsIllegalState() {
        AppMapper mapper = mock(AppMapper.class);
        when(mapper.pay(3L, 1L)).thenReturn(0);
        assertThrows(IllegalStateException.class, () -> new OrderService(mapper, new cn.campus.takeaway.cart.CartService(mapper)).pay(3L, 1L));
    }

    @Test
    void secondRiderCannotClaimSameOrder() {
        AppMapper mapper = mock(AppMapper.class);
        when(mapper.claimOrder(3L, 9L)).thenReturn(0);
        assertThrows(
                IllegalStateException.class, () -> new OrderService(mapper, new cn.campus.takeaway.cart.CartService(mapper)).riderClaim(3L, 9L));
    }

    @Test
    void merchantCannotOperateAnotherShop() {
        AppMapper mapper = mock(AppMapper.class);
        Shop shop = new Shop();
        shop.setId(7L);
        when(mapper.findShopByOwner(4L)).thenReturn(shop);
        Order order = new Order();
        order.setShopId(8L);
        when(mapper.findOrder(3L)).thenReturn(order);
        assertThrows(
                IllegalStateException.class, () -> new OrderService(mapper, new cn.campus.takeaway.cart.CartService(mapper)).merchantAccept(3L, 4L));
    }

    @Test
    void closedShopRejectsCheckout() {
        AppMapper mapper = mock(AppMapper.class);
        when(mapper.findAddress(1L, 1L)).thenReturn(new Address());
        Shop shop = new Shop();
        shop.setId(2L);
        shop.setOpen(false);
        when(mapper.findShop(2L)).thenReturn(shop);
        String key="00000000-0000-0000-0000-000000000001";
        when(mapper.lockUser(1L)).thenReturn(1L);
        when(mapper.findCart(1L)).thenReturn(new cn.campus.takeaway.cart.CartState(1L,1L,2L,1L,key));
        when(mapper.cartRows(1L)).thenReturn(java.util.List.of(new cn.campus.takeaway.cart.CartLine(8L,2L,"套餐",BigDecimal.ONE,true,"",1,BigDecimal.ONE)));
        Dish dish=new Dish();dish.setId(8L);dish.setShopId(2L);dish.setOnSale(true);when(mapper.findDish(8L)).thenReturn(dish);
        assertThrows(
                IllegalArgumentException.class,
                () -> new OrderService(mapper, new cn.campus.takeaway.cart.CartService(mapper)).create(1L, key, 1L, 1L));
        verify(mapper, never()).insertOrder(any());
    }

    @Test
    void cancellationRecordsOriginalUnpaidStatus() {
        AppMapper mapper = mock(AppMapper.class);
        Order order = new Order();
        order.setStudentId(1L);
        order.setStatus("UNPAID");
        when(mapper.lockOrder(3L)).thenReturn(order);
        when(mapper.cancel(3L, 1L)).thenReturn(1);
        new OrderService(mapper, new cn.campus.takeaway.cart.CartService(mapper)).cancel(3L, 1L);
        verify(mapper).insertEvent(3L, "UNPAID", "CANCELED", 1L);
    }

    @Test
    void quantityLimitStopsAt99() {
        AppMapper mapper=mock(AppMapper.class);
        var carts=new cn.campus.takeaway.cart.CartService(mapper);
        assertThrows(IllegalArgumentException.class, () -> carts.put(1L,8L,100));
        assertThrows(IllegalArgumentException.class, () -> carts.put(1L,8L,0));
        verifyNoInteractions(mapper);
    }

    @Test
    void invalidInputsAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> cn.campus.takeaway.service.InputChecks.price(new BigDecimal("-1")));
        assertThrows(
                IllegalArgumentException.class,
                () -> cn.campus.takeaway.service.InputChecks.phone("123"));
    }
}
