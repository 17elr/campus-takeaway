package cn.campus.takeaway.domain;

public class Shop {
    private Long id; private Long ownerId; private String name; private String description; private Boolean open;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getOwnerId(){return ownerId;} public void setOwnerId(Long v){ownerId=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public Boolean getOpen(){return open;} public void setOpen(Boolean v){open=v;}
}
