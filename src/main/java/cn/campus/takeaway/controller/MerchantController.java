package cn.campus.takeaway.controller;

import cn.campus.takeaway.domain.*;
import cn.campus.takeaway.mapper.AppMapper;
import cn.campus.takeaway.service.CatalogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant")
public class MerchantController {
    private final CatalogService catalog;
    private final AppMapper mapper;
    public MerchantController(CatalogService catalog,AppMapper mapper) { this.catalog=catalog; this.mapper=mapper; }
    public record DishInput(@NotBlank String category,@NotBlank String name,@NotNull BigDecimal price) {}
    public record ShopInput(@NotBlank String name,@NotBlank String description,@NotNull Boolean open) {}
    @GetMapping("/shop")
    Shop shop(@RequestAttribute("currentUser") AppUser user) { return catalog.ownShop(user.getId()); }
    @PutMapping("/shop")
    Map<String,String> update(@RequestAttribute("currentUser") AppUser user,@Valid @RequestBody ShopInput input) {
        catalog.shop(user.getId(),input.name(),input.description(),input.open()); return ok();
    }
    @GetMapping("/dishes")
    PageResult<Dish> dishes(@RequestAttribute("currentUser") AppUser user,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="12") int size,@RequestParam(defaultValue="") String query) {
        PageResult.validate(page,size);
        if(query.length()>80) throw new IllegalArgumentException("搜索词最多 80 字");
        long shopId=catalog.ownShop(user.getId()).getId();
        return new PageResult<>(mapper.pageDishes(shopId,query,size,(long)(page-1)*size),mapper.countDishes(shopId,query),page,size);
    }
    @PostMapping("/dishes")
    Map<String,String> create(@RequestAttribute("currentUser") AppUser user,@Valid @RequestBody DishInput input) {
        catalog.dish(user.getId(),null,input.category(),input.name(),input.price()); return ok();
    }
    @PutMapping("/dishes/{id}")
    Map<String,String> edit(@RequestAttribute("currentUser") AppUser user,@PathVariable long id,@Valid @RequestBody DishInput input) {
        catalog.dish(user.getId(),id,input.category(),input.name(),input.price()); return ok();
    }
    @PostMapping("/dishes/{id}/toggle")
    Map<String,String> toggle(@RequestAttribute("currentUser") AppUser user,@PathVariable long id) { catalog.toggleDish(user.getId(),id); return ok(); }
    private Map<String,String> ok() { return Map.of("message","店铺信息已更新"); }
}
