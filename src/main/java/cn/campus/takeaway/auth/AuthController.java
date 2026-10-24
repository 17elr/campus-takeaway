package cn.campus.takeaway.auth;

import cn.campus.takeaway.domain.AppUser;
import cn.campus.takeaway.mapper.AppMapper;
import cn.campus.takeaway.service.AccountService;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AppMapper mapper;
    private final PasswordEncoder encoder;
    private final AccountService accounts;
    public AuthController(AppMapper mapper, PasswordEncoder encoder, AccountService accounts) {
        this.mapper=mapper; this.encoder=encoder; this.accounts=accounts;
    }
    public record Login(@NotBlank @Size(max=40) String username, @NotBlank @Size(max=64) String password) {}
    public record Registration(@NotBlank String username, @NotBlank String password,
                               @NotBlank String realName, @NotBlank String phone) {}

    @GetMapping("/csrf")
    Map<String,String> csrf(HttpServletRequest request) {
        var session=request.getSession();
        var existing=session.getAttribute("csrf");
        return Map.of("token", existing instanceof String token ? token : CsrfFilter.rotate(session));
    }
    @PostMapping("/login")
    Map<String,Object> login(@Valid @RequestBody Login input, HttpServletRequest request) {
        var user=mapper.findUser(input.username());
        if (user==null || !user.isEnabled() || !encoder.matches(input.password(),user.getPassword()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"用户名或密码不正确，或账号已停用");
        var session=request.getSession();
        request.changeSessionId();
        session.setAttribute("userId",user.getId());
        session.setAttribute("authVersion",user.getAuthVersion());
        return Map.of("user",user,"token",CsrfFilter.rotate(session));
    }
    @PostMapping("/register")
    Map<String,String> register(@Valid @RequestBody Registration input) {
        accounts.create(input.username(),input.password(),input.realName(),input.phone(),"STUDENT");
        return Map.of("message","注册成功，请登录");
    }
    @GetMapping("/me")
    AppUser me(@RequestAttribute("currentUser") AppUser user) { return user; }
    @PostMapping("/logout")
    Map<String,String> logout(HttpServletRequest request) {
        request.getSession().invalidate();
        return Map.of("message","已退出登录");
    }
}
