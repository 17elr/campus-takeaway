package cn.campus.takeaway.domain;

import java.math.BigDecimal;

public class OrderItem {
    private Long id;
    private Long orderId;
    private Long dishId;
    private String dishName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotal;

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long v) {
        orderId = v;
    }

    public Long getDishId() {
        return dishId;
    }

    public void setDishId(Long v) {
        dishId = v;
    }

    public String getDishName() {
        return dishName;
    }

    public void setDishName(String v) {
        dishName = v;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal v) {
        unitPrice = v;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer v) {
        quantity = v;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal v) {
        subtotal = v;
    }
}
