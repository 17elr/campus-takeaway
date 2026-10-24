package cn.campus.takeaway.auth;

import cn.campus.takeaway.mapper.AppMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@Order(0)
public class AuthFilter extends OncePerRequestFilter {
    private final AppMapper mapper;
    public AuthFilter(AppMapper mapper) { this.mapper = mapper; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String role = path.startsWith("/api/student/") ? "STUDENT" : path.startsWith("/api/merchant/") ? "MERCHANT"
                : path.startsWith("/api/rider/") ? "RIDER" : path.startsWith("/api/admin/") ? "ADMIN" : null;
        boolean required = role != null || path.equals("/api/auth/me") || path.equals("/api/auth/logout")
                || path.equals("/api/account") || path.startsWith("/api/account/")
                || path.equals("/api/orders") || path.startsWith("/api/orders/");
        if (required) {
            var session = request.getSession(false);
            var id = session == null ? null : session.getAttribute("userId");
            var user = id instanceof Long userId ? mapper.findUserById(userId) : null;
            if (user == null || !user.isEnabled() || !Long.valueOf(user.getAuthVersion()).equals(session.getAttribute("authVersion"))) {
                if (session != null) session.invalidate();
                ApiErrors.write(response, 401, "登录已失效，请重新登录");
                return;
            }
            if (role != null && !role.equals(user.getRole())) {
                ApiErrors.write(response, 403, "当前角色无权执行此操作");
                return;
            }
            request.setAttribute("currentUser", user);
        }
        response.setHeader("X-Content-Type-Options", "nosniff");
        if (path.startsWith("/api/")) response.setHeader("Cache-Control", "no-store");
        chain.doFilter(request, response);
    }
}
