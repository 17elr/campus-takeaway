package cn.campus.takeaway.controller;

import cn.campus.takeaway.cache.MenuCache;
import cn.campus.takeaway.domain.Shop;
import cn.campus.takeaway.mapper.AppMapper;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shops")
public class ShopController {
    private final AppMapper mapper;
    private final MenuCache menus;
    public ShopController(AppMapper mapper,MenuCache menus) { this.mapper=mapper; this.menus=menus; }
    @GetMapping
    PageResult<Shop> shops(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="12") int size,@RequestParam(defaultValue="") String query) {
        PageResult.validate(page,size);
        if (query.length()>80) throw new IllegalArgumentException("搜索词最多 80 字");
        return new PageResult<>(mapper.pageShops(false,query,size,(long)(page-1)*size),mapper.countShops(false,query),page,size);
    }
    @GetMapping("/{id}/menu")
    MenuCache.Menu menu(@PathVariable long id,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="12") int size,@RequestParam(defaultValue="") String query) {
        return menus.get(id,page,size,query);
    }
}
