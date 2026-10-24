package cn.campus.takeaway.auth;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class ApiErrors {
    private ApiErrors() {}
    public static void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
