package cn.campus.takeaway.cart;

public record CartState(Long id, Long userId, Long shopId, long version, String checkoutKey) {}
