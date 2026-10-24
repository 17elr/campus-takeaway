package cn.campus.takeaway.domain;

public enum OrderStatus {
    UNPAID("待支付"),
    PENDING_ACCEPT("待接单"),
    MAKING("制作中"),
    READY("待配送"),
    DELIVERING("配送中"),
    WAIT_CONFIRM("待确认"),
    COMPLETED("已完成"),
    CANCELED("已取消");
    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
