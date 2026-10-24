package cn.campus.takeaway.service;

import cn.campus.takeaway.cache.MenuCache;
import cn.campus.takeaway.domain.Shop;
import cn.campus.takeaway.mapper.AppMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class CatalogService {
    private final AppMapper mapper;
    private final MenuCache cache;
    public CatalogService(AppMapper mapper,MenuCache cache) { this.mapper=mapper; this.cache=cache; }
    public Shop ownShop(long userId) {
        var shop=mapper.findShopByOwner(userId);
        if (shop==null) throw new IllegalArgumentException("未找到当前商家的店铺");
        return shop;
    }
    @Transactional
    public void dish(long ownerId,Long id,String category,String name,BigDecimal price) {
        category=InputChecks.text(category,"分类",40);
        name=InputChecks.text(name,"菜品名称",80);
        price=InputChecks.price(price);
        long shopId=ownShop(ownerId).getId();
        if (id==null) mapper.insertDish(shopId,category,name,price);
        else if (mapper.updateDish(id,shopId,name,category,price)!=1) throw new IllegalArgumentException("菜品不存在或无权修改");
        cache.evictAfterCommit(shopId);
    }
    @Transactional
    public void toggleDish(long ownerId,long id) {
        long shopId=ownShop(ownerId).getId();
        if (mapper.toggleDish(id,shopId)!=1) throw new IllegalArgumentException("菜品不存在或无权修改");
        cache.evictAfterCommit(shopId);
    }
    @Transactional
    public void shop(long ownerId,String name,String description,boolean open) {
        long id=ownShop(ownerId).getId();
        mapper.updateShop(ownerId,InputChecks.text(name,"店名",80),InputChecks.text(description,"简介",255),open);
        cache.evictAfterCommit(id);
    }
    @Transactional
    public void adminShop(long id,boolean open) {
        if (mapper.setShopOpen(id,open)!=1) throw new IllegalArgumentException("店铺不存在");
        cache.evictAfterCommit(id);
    }
    @Transactional
    public void enabled(long id,boolean enabled) {
        var user=mapper.findUserById(id);
        if (user==null || "ADMIN".equals(user.getRole())) throw new IllegalArgumentException("账号不存在或不能停用管理员");
        mapper.setUserEnabled(id,enabled);
        var shop=mapper.findShopByOwner(id);
        if (shop!=null) cache.evictAfterCommit(shop.getId());
    }
}
