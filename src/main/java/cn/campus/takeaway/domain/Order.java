package cn.campus.takeaway.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Order {
    private Long id; private String orderNo; private Long studentId; private Long shopId; private String shopName; private Long riderId;
    private BigDecimal amount; private String addressSnapshot; private String status; private LocalDateTime createdAt; private List<OrderItem> items;
    public Long getId(){return id;} public void setId(Long v){id=v;} public String getOrderNo(){return orderNo;} public void setOrderNo(String v){orderNo=v;}
    public Long getStudentId(){return studentId;} public void setStudentId(Long v){studentId=v;} public Long getShopId(){return shopId;} public void setShopId(Long v){shopId=v;}
    public String getShopName(){return shopName;} public void setShopName(String v){shopName=v;} public Long getRiderId(){return riderId;} public void setRiderId(Long v){riderId=v;}
    public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;} public String getAddressSnapshot(){return addressSnapshot;} public void setAddressSnapshot(String v){addressSnapshot=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;} public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    public List<OrderItem> getItems(){return items;} public void setItems(List<OrderItem> v){items=v;}
    public String getStatusLabel(){ return OrderStatus.valueOf(status).getLabel(); }
}
