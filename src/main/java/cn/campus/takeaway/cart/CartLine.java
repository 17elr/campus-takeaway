package cn.campus.takeaway.cart;

import java.math.BigDecimal;

public record CartLine(long dishId, long shopId, String name, BigDecimal price, boolean onSale,
                       String imagePath, int quantity, BigDecimal subtotal) {}
