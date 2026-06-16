package com.teamdev.controller;

import com.teamdev.entity.Role;
import com.teamdev.service.RoleService;
import com.teamdev.config.AdminRequired;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色管理控制器
 * 提供角色相关的CRUD操作接口
 */
@RestController
@RequestMapping("/api/roles")
public class RoleController {
    
    @Autowired
    private RoleService roleService;
    
    /**
     * 获取所有角色信息
     * @return 包含角色列表的响应对象
     */
    @GetMapping
    public Map<String, Object> getAllRoles() {
        List<Role> roles = roleService.getAllRoles();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", roles);
        result.put("message", "查询成功");
        return result;
    }
    
    /**
     * 根据ID获取角色信息
     * @param id 角色ID
     * @return 包含角色信息的响应对象
     */
    @GetMapping("/{id}")
    public Map<String, Object> getRoleById(@PathVariable Long id) {
        Role role = roleService.getRoleById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", role);
        result.put("message", "查询成功");
        return result;
    }
    
    /**
     * 创建角色
     * @param role 角色对象
     * @return 操作结果响应对象
     */
    @PostMapping
    public Map<String, Object> createRole(@RequestBody Role role) {
        Map<String, Object> authError = AdminRequired.check();
        if (authError != null) return authError;
        boolean success = roleService.saveRole(role);
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("success", success);
        result.put("message", success ? "创建成功" : "创建失败");
        return result;
    }
    
    /**
     * 更新角色信息
     * @param id 角色ID
     * @param role 角色对象
     * @return 操作结果响应对象
     */
    @PutMapping("/{id}")
    public Map<String, Object> updateRole(@PathVariable Long id, @RequestBody Role role) {
        Map<String, Object> authError = AdminRequired.check();
        if (authError != null) return authError;
        role.setId(id);
        boolean success = roleService.updateRole(role);
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("success", success);
        result.put("message", success ? "更新成功" : "更新失败");
        return result;
    }
    
    /**
     * 删除角色
     * @param id 角色ID
     * @return 操作结果响应对象
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteRole(@PathVariable Long id) {
        Map<String, Object> authError = AdminRequired.check();
        if (authError != null) return authError;
        boolean success = roleService.deleteRole(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("success", success);
        result.put("message", success ? "删除成功" : "删除失败");
        return result;
    }
}