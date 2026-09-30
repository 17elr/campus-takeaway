package cn.campus.takeaway.controller;

import cn.campus.takeaway.domain.AppUser;
import cn.campus.takeaway.service.AccountService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {
    private final AccountService accounts;
    public HomeController(AccountService accounts){this.accounts=accounts;}
    @GetMapping("/") String index(@AuthenticationPrincipal AppUser user){ return user==null?"index":"redirect:/home"; }
    @GetMapping("/login") String login(){ return "login"; }
    @GetMapping("/home") String home(@AuthenticationPrincipal AppUser user){ return "redirect:/"+user.getRole().toLowerCase(); }
    @GetMapping("/register") String register(){ return "register"; }
    @PostMapping("/register") String register(@RequestParam String username,@RequestParam String password,@RequestParam String realName,@RequestParam String phone,RedirectAttributes flash){
        accounts.create(username,password,realName,phone,"STUDENT"); flash.addFlashAttribute("success","注册成功，请使用新账号登录"); return "redirect:/login";
    }
}
