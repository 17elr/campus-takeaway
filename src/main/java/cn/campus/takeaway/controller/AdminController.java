package cn.campus.takeaway.controller;

import cn.campus.takeaway.domain.*;
import cn.campus.takeaway.mapper.AppMapper;
import cn.campus.takeaway.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AppMapper mapper;
    private final AccountService accounts;
    private final CatalogService catalog;
    public AdminController(AppMapper mapper,AccountService accounts,CatalogService catalog) { this.mapper=mapper; this.accounts=accounts; this.catalog=catalog; }
    public record AccountInput(@NotBlank String username,@NotBlank String password,@NotBlank String realName,@NotBlank String phone,@NotBlank String role) {}
    public record Enabled(@NotNull Boolean enabled) {}
    public record Open(@NotNull Boolean open) {}
    @GetMapping("/users")
    PageResult<AppUser> users(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="12") int size,@RequestParam(defaultValue="") String query) {
        check(page,size,query); return new PageResult<>(mapper.pageUsers(query,size,(long)(page-1)*size),mapper.countFilteredUsers(query),page,size);
    }
    @PostMapping("/users")
    Map<String,String> create(@Valid @RequestBody AccountInput input) {
        if (!java.util.Set.of("MERCHANT","RIDER").contains(input.role())) throw new IllegalArgumentException("管理员仅可创建商家或骑手");
        accounts.create(input.username(),input.password(),input.realName(),input.phone(),input.role()); return ok();
    }
    @PutMapping("/users/{id}/enabled")
    Map<String,String> enabled(@PathVariable long id,@Valid @RequestBody Enabled input) { catalog.enabled(id,input.enabled()); return ok(); }
    @GetMapping("/shops")
    PageResult<Shop> shops(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="12") int size,@RequestParam(defaultValue="") String query) {
        check(page,size,query); return new PageResult<>(mapper.pageShops(true,query,size,(long)(page-1)*size),mapper.countShops(true,query),page,size);
    }
    @PutMapping("/shops/{id}/open")
    Map<String,String> open(@PathVariable long id,@Valid @RequestBody Open input) { catalog.adminShop(id,input.open()); return ok(); }
    @GetMapping("/dishes")
    PageResult<Dish> dishes(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="12") int size,@RequestParam(defaultValue="") String query) {
        check(page,size,query); return new PageResult<>(mapper.pageDishes(null,query,size,(long)(page-1)*size),mapper.countDishes(null,query),page,size);
    }
    private void check(int page,int size,String query) {
        PageResult.validate(page,size); if(query.length()>80) throw new IllegalArgumentException("搜索词最多 80 字");
    }
    private Map<String,String> ok() { return Map.of("message","管理操作已完成"); }
}
