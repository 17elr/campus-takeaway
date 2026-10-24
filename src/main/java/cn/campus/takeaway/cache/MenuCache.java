package cn.campus.takeaway.cache;

import cn.campus.takeaway.controller.PageResult;
import cn.campus.takeaway.domain.Dish;
import cn.campus.takeaway.mapper.AppMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MenuCache {
    public record ShopInfo(long id, String name, String description, boolean open, String imagePath) {}
    public record Menu(ShopInfo shop, PageResult<Dish> dishes) {}
    private final AppMapper mapper;
    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final boolean enabled;
    public MenuCache(AppMapper mapper, StringRedisTemplate redis, ObjectMapper json, @Value("${cache.enabled:true}") boolean enabled) {
        this.mapper=mapper; this.redis=redis; this.json=json; this.enabled=enabled;
    }
    public Menu get(long shopId, int page, int size, String query) {
        if (shopId < 1 || shopId > 9007199254740991L) throw new IllegalArgumentException("店铺编号不合法");
        PageResult.validate(page,size);
        if (query.length()>80) throw new IllegalArgumentException("搜索词最多 80 字");
        boolean cached = enabled && page==1 && size==12 && query.isEmpty();
        String key="takeaway:menu:v1:"+shopId;
        if (cached) {
            String value=null;
            try { value=redis.opsForValue().get(key); }
            catch (RuntimeException failure) { warn(failure); }
            if ("null".equals(value)) throw missing();
            if (value!=null) {
                try { return json.readValue(value,Menu.class); }
                catch (Exception corrupt) { warn(corrupt); }
            }
        }
        var shop=mapper.findShop(shopId);
        if (shop==null || !Boolean.TRUE.equals(shop.getOpen()) || !Boolean.TRUE.equals(mapper.userEnabled(shop.getOwnerId()))) {
            if (cached) store(key,"null",10);
            throw missing();
        }
        var result=new Menu(new ShopInfo(shop.getId(),shop.getName(),shop.getDescription(),true,shop.getImagePath()),
            new PageResult<>(mapper.pagePublicMenu(shopId,query,size,(long)(page-1)*size),mapper.countPublicMenu(shopId,query),page,size));
        if (cached) {
            try { store(key,json.writeValueAsString(result),60+ThreadLocalRandom.current().nextInt(16)); }
            catch (Exception failure) { warn(failure); }
        }
        return result;
    }
    private void store(String key,String value,long ttl) {
        try { redis.opsForValue().set(key,value,Duration.ofSeconds(ttl)); }
        catch (RuntimeException failure) { warn(failure); }
    }
    public void evictAfterCommit(long shopId) {
        Runnable evict=() -> {
            try { redis.delete("takeaway:menu:v1:"+shopId); }
            catch (RuntimeException failure) { warn(failure); }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive())
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { evict.run(); }
            });
        else evict.run();
    }
    private ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND,"店铺不存在、休息中或已停用"); }
    private void warn(Exception failure) { LoggerFactory.getLogger(getClass()).warn("Menu cache unavailable, using MySQL: {}",failure.getClass().getSimpleName()); }
}
