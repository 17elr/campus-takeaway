package cn.campus.takeaway.controller;

import cn.campus.takeaway.domain.AppUser;
import cn.campus.takeaway.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    private final AccountService accounts;
    public AccountController(AccountService accounts) { this.accounts=accounts; }
    public record Profile(@NotBlank String realName,@NotBlank String phone) {}
    public record Password(@NotBlank String currentPassword,@NotBlank String newPassword,@NotBlank String confirmPassword) {}
    @GetMapping
    AppUser get(@RequestAttribute("currentUser") AppUser user) { return user; }
    @PutMapping
    Map<String,String> profile(@RequestAttribute("currentUser") AppUser user,@Valid @RequestBody Profile input) {
        accounts.updateProfile(user.getId(),input.realName(),input.phone()); return Map.of("message","资料已保存");
    }
    @PostMapping("/password")
    Map<String,String> password(@RequestAttribute("currentUser") AppUser user,@Valid @RequestBody Password input,HttpServletRequest request) {
        accounts.changePassword(user.getId(),user.getUsername(),input.currentPassword(),input.newPassword(),input.confirmPassword());
        request.getSession().invalidate();
        return Map.of("message","密码已修改，请重新登录");
    }
}
