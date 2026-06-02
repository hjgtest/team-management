package com.teamdev.controller;

import com.teamdev.entity.User;
import com.teamdev.entity.Project;
import com.teamdev.service.UserService;
import com.teamdev.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器
 * 提供用户相关的CRUD操作接口
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ProjectService projectService;
    
    /**
     * 获取所有用户信息（包含正在进行的项目数）
     * @return 包含用户列表的响应对象
     */
    @GetMapping
    public Map<String, Object> getAllUsers() {
        List<User> users = userService.getAllUsers();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", users);
        result.put("message", "查询成功");
        return result;
    }
    
    /**
     * 获取所有用户信息（不包含正在进行的项目数）
     * @return 包含用户列表的响应对象
     */
    @GetMapping("/simple")
    public Map<String, Object> getAllUsersSimple() {
        List<User> users = userService.getAllUsersWithoutOngoingProjects();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", users);
        result.put("message", "查询成功");
        return result;
    }
    
    /**
     * 根据ID获取用户信息
     * @param id 用户ID
     * @return 包含用户信息的响应对象
     */
    @GetMapping("/{id}")
    public Map<String, Object> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", user);
        result.put("message", "查询成功");
        return result;
    }
    
    /**
     * 创建新用户
     * @param user 用户对象
     * @return 操作结果响应对象
     */
    @PostMapping
    public Map<String, Object> createUser(@RequestBody User user) {
        boolean success = userService.saveUser(user);
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("success", success);
        result.put("message", success ? "创建成功" : "创建失败");
        return result;
    }
    
    /**
     * 更新用户信息
     * @param id 用户ID
     * @param user 用户对象
     * @return 操作结果响应对象
     */
    @PutMapping("/{id}")
    public Map<String, Object> updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        boolean success = userService.updateUser(user);
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("success", success);
        result.put("message", success ? "更新成功" : "更新失败");
        return result;
    }
    
    /**
     * 删除用户
     * @param id 用户ID
     * @return 操作结果响应对象
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteUser(@PathVariable Long id) {
        boolean success = userService.deleteUser(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("success", success);
        result.put("message", success ? "删除成功" : "删除失败");
        return result;
    }
    
    /**
     * 获取用户正在进行的项目和版本列表
     * @param userId 用户ID
     * @return 包含用户正在进行项目和版本列表的响应对象
     */
    @GetMapping("/{userId}/ongoing-projects")
    public Map<String, Object> getUserOngoingProjects(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> projectsAndVersions = projectService.getUserOngoingProjectsAndVersions(userId);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", projectsAndVersions);
            result.put("message", "查询成功");
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
            e.printStackTrace();
            return result;
        }
    }
}