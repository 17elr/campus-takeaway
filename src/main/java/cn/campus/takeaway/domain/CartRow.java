package cn.campus.takeaway.domain;

import java.math.BigDecimal;

public record CartRow(Dish dish, int quantity, BigDecimal subtotal) { }
