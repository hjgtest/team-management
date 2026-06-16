package com.teamdev.config;

import com.teamdev.entity.User;
import com.teamdev.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();

        // 白名单：登录/登出接口 + 静态资源 + 前端路由根路径
        if (requestURI.equals("/") ||
            requestURI.equals("/index.html") ||
            requestURI.equals("/favicon.ico") ||
            requestURI.startsWith("/css/") ||
            requestURI.startsWith("/js/") ||
            requestURI.startsWith("/img/") ||
            requestURI.startsWith("/static/") ||
            requestURI.contains("/api/auth/login") ||
            requestURI.contains("/api/auth/logout") ||
            requestURI.contains("/h2-console")) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeResponse(response, 401, "未登录，请先登录");
            return false;
        }

        String token = authHeader.substring(7);

        try {
            // token 格式：phone|timestamp
            String phone = token.split("\\|")[0];

            User user = userService.getByPhone(phone);
            if (user == null || (user.getIsActive() != null && user.getIsActive() == 0)) {
                writeResponse(response, 401, "用户不存在或已离职");
                return false;
            }

            request.setAttribute("currentUser", user);
            return true;

        } catch (Exception e) {
            writeResponse(response, 401, "登录已过期，请重新登录");
            return false;
        }
    }

    private void writeResponse(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();
        result.put("code", status);
        result.put("message", message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
