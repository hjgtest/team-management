package com.teamdev.config;

import com.teamdev.entity.User;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理员权限校验工具
 * userType=0 为管理员，其他类型只读
 */
public class AdminRequired {

    /**
     * 检查当前请求的用户是否为管理员
     * @return null 表示通过；否则返回含错误信息的响应体
     */
    public static Map<String, Object> check() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return error("无法获取请求上下文");
        }
        HttpServletRequest request = attrs.getRequest();
        User user = (User) request.getAttribute("currentUser");
        if (user == null) {
            return error("未登录，请先登录");
        }
        if (user.getUserType() == null || user.getUserType() != 0) {
            return error("权限不足，仅管理员可执行此操作");
        }
        return null;
    }

    private static Map<String, Object> error(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 403);
        result.put("success", false);
        result.put("message", message);
        return result;
    }
}
