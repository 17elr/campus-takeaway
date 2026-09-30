package cn.campus.takeaway.config;

import cn.campus.takeaway.domain.AppUser;
import cn.campus.takeaway.mapper.AppMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

public class ActiveAccountFilter extends OncePerRequestFilter {
    private final AppMapper mapper;
    public ActiveAccountFilter(AppMapper mapper) { this.mapper=mapper; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        if(authentication!=null && authentication.getPrincipal() instanceof AppUser user) {
            var current=mapper.findUser(user.getUsername());
            boolean disabled=current==null || !current.isEnabled();
            boolean changed=!disabled && (!java.util.Objects.equals(user.getPassword(),current.getPassword()) || !java.util.Objects.equals(user.getRole(),current.getRole()));
            if(disabled || changed){
                var session=request.getSession(false); if(session!=null) session.invalidate();
                SecurityContextHolder.clearContext(); response.sendRedirect(disabled?"/login?disabled":"/login?expired"); return;
            }
            user.setRealName(current.getRealName());user.setPhone(current.getPhone());
        }
        chain.doFilter(request,response);
    }
}
