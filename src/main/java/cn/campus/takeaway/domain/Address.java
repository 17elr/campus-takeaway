package cn.campus.takeaway.domain;

public class Address {
    private Long id; private Long userId; private String building; private String room; private String contactName; private String phone; private Boolean isDefault;
    public Long getId(){return id;} public void setId(Long v){id=v;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
    public String getBuilding(){return building;} public void setBuilding(String v){building=v;} public String getRoom(){return room;} public void setRoom(String v){room=v;}
    public String getContactName(){return contactName;} public void setContactName(String v){contactName=v;} public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public Boolean getIsDefault(){return isDefault;} public void setIsDefault(Boolean v){isDefault=v;}
    public String snapshot(){ return building + " " + room + "，联系人：" + contactName + "，电话：" + phone; }
}
