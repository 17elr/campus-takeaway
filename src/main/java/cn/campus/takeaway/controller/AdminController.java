package cn.campus.takeaway.controller;

import cn.campus.takeaway.mapper.AppMapper;
import cn.campus.takeaway.service.AccountService;
import cn.campus.takeaway.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/admin")
public class AdminController {
    private final AppMapper mapper;private final OrderService orders;private final AccountService accounts;
    public AdminController(AppMapper mapper,OrderService orders,AccountService accounts){this.mapper=mapper;this.orders=orders;this.accounts=accounts;}
    @GetMapping String dashboard(Model model){model.addAttribute("users",mapper.listUsers());model.addAttribute("shops",mapper.listShops());model.addAttribute("orders",orders.allOrders());return "admin";}
    @PostMapping("/users") String create(@RequestParam String username,@RequestParam String password,@RequestParam String realName,@RequestParam String phone,@RequestParam String role,RedirectAttributes flash){
        if(!java.util.Set.of("MERCHANT","RIDER").contains(role))throw new IllegalArgumentException("管理员仅可创建商家或骑手账号");
        accounts.create(username,password,realName,phone,role);flash.addFlashAttribute("success","账号已创建，请告知使用者登录信息");return "redirect:/admin?tab=users";
    }
    @PostMapping("/users/{id}/toggle") String toggle(@PathVariable long id,@RequestParam boolean enabled,RedirectAttributes flash){
        var target=mapper.listUsers().stream().filter(user->user.getId()==id).findFirst().orElseThrow(()->new IllegalArgumentException("账号不存在"));
        if("ADMIN".equals(target.getRole()))throw new IllegalArgumentException("不能停用管理员账号");mapper.setUserEnabled(id,enabled);flash.addFlashAttribute("success",enabled?"账号已启用":"账号已停用，现有登录会话将在下次访问时失效");return "redirect:/admin?tab=users";
    }
    @PostMapping("/shops/{id}/toggle") String shop(@PathVariable long id,@RequestParam boolean open,RedirectAttributes flash){if(mapper.setShopOpen(id,open)!=1)throw new IllegalArgumentException("店铺不存在");flash.addFlashAttribute("success",open?"店铺已开启营业":"店铺已暂停营业");return "redirect:/admin?tab=shops";}
}
