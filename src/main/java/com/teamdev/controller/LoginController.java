package com.teamdev.controller;

import com.teamdev.entity.User;
import com.teamdev.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    @Autowired
    private UserService userService;

    /**
     * 登录接口
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> loginRequest) {
        Map<String, Object> result = new HashMap<>();
        String phone = loginRequest.get("phone");
        String password = loginRequest.get("password");

        if (phone == null || password == null) {
            result.put("code", 400);
            result.put("message", "手机号和密码不能为空");
            return result;
        }

        User user = userService.login(phone, password);
        if (user == null) {
            result.put("code", 401);
            result.put("message", "手机号或密码错误，或用户已离职");
            return result;
        }

        // 生成 token（简单格式：phone|timestamp，前端后端约定好格式）
        String token = phone + "|" + System.currentTimeMillis();

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        user.setPassword(null);
        data.put("user", user);

        result.put("code", 200);
        result.put("data", data);
        result.put("message", "登录成功");
        return result;
    }

    /**
     * 登出接口
     */
    @PostMapping("/logout")
    public Map<String, Object> logout() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "登出成功");
        return result;
    }
}
