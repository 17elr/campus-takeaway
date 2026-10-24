package cn.campus.takeaway.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import java.util.UUID;

@Component
@Order(1)
public class CsrfFilter extends OncePerRequestFilter {
    public static String rotate(HttpSession session) {
        String token = UUID.randomUUID().toString();
        session.setAttribute("csrf", token);
        return token;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod())) {
            var session = request.getSession(false);
            var saved = session == null ? null : session.getAttribute("csrf");
            String supplied = request.getHeader("X-CSRF-Token");
            if (!(saved instanceof String token) || supplied == null
                    || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
                ApiErrors.write(response, 403, "操作凭证已失效，请刷新页面后重试");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
