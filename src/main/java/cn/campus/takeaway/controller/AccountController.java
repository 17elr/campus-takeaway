package cn.campus.takeaway.controller;

import cn.campus.takeaway.domain.AppUser;
import cn.campus.takeaway.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/account")
public class AccountController {
    private final AccountService accounts;
    public AccountController(AccountService accounts){this.accounts=accounts;}
    @GetMapping String account(){return "account";}
    @PostMapping("/profile") String profile(@AuthenticationPrincipal AppUser user,@RequestParam String realName,@RequestParam String phone,RedirectAttributes flash){
        accounts.updateProfile(user.getId(),realName,phone);
        user.setRealName(realName.strip());user.setPhone(phone);
        flash.addFlashAttribute("success","个人资料已保存");return "redirect:/account";
    }
    @PostMapping("/password") String password(@AuthenticationPrincipal AppUser user,@RequestParam String currentPassword,@RequestParam String newPassword,@RequestParam String confirmPassword,HttpServletRequest request,HttpServletResponse response){
        accounts.changePassword(user.getId(),user.getUsername(),currentPassword,newPassword,confirmPassword);
        new SecurityContextLogoutHandler().logout(request,response,null);
        return "redirect:/login?changed";
    }
}
