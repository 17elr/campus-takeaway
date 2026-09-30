package cn.campus.takeaway.domain;

import java.time.LocalDateTime;

public record OrderEvent(String fromStatus, String toStatus, String operatorName, LocalDateTime createdAt) {
    public String getLabel() { return OrderStatus.valueOf(toStatus).getLabel(); }
}
