package cn.campus.takeaway.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SessionFailureFilter extends OncePerRequestFilter {
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Delay the body until Spring Session has committed to Redis, so a failed login cannot look successful.
        var buffered = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(request, buffered);
        } catch (RuntimeException | ServletException failure) {
            boolean redis = false;
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                if (cause.getClass().getName().startsWith("org.springframework.data.redis.")
                        || cause.getClass().getName().startsWith("io.lettuce.")) redis = true;
            }
            if (!redis) throw failure;
            LoggerFactory.getLogger(getClass()).warn("Shared session unavailable: {}", failure.getClass().getSimpleName());
            buffered.reset();
            ApiErrors.write(buffered, 503, "登录服务暂时不可用，请稍后重试");
        }
        buffered.copyBodyToResponse();
    }
}
