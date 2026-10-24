package cn.campus.takeaway.domain;

import java.math.BigDecimal;

public class Dish {
    private Long id;
    private Long shopId;
    private String shopName;
    private String name;
    private String category;
    private BigDecimal price;
    private Boolean onSale;
    private String imagePath;

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long v) {
        shopId = v;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String v) {
        shopName = v;
    }

    public String getName() {
        return name;
    }

    public void setName(String v) {
        name = v;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String v) {
        category = v;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal v) {
        price = v;
    }

    public Boolean getOnSale() {
        return onSale;
    }

    public void setOnSale(Boolean v) {
        onSale = v;
    }

    public void setImagePath(String value) {
        imagePath = value;
    }

    public String getImagePath() {
        if (imagePath != null && !imagePath.isBlank()) return imagePath;
        return switch (name == null ? "" : name) {
            case "香辣鸡腿饭" -> "/images/chicken-rice.webp";
            case "番茄鸡蛋面" -> "/images/tomato-noodles.webp";
            case "柠檬茶" -> "/images/lemon-tea.webp";
            default -> "/images/menu-assortment.webp";
        };
    }
}
