package com.teamdev.controller;

import com.teamdev.entity.ProjectVersion;
import com.teamdev.service.ProjectVersionService;
import com.teamdev.config.AdminRequired;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目版本管理控制器
 * 提供项目版本相关的CRUD操作接口
 */
@RestController
@RequestMapping("/api/projectVersions")
public class ProjectVersionController {

    @Autowired
    private ProjectVersionService projectVersionService;

    /**
     * 获取所有项目版本记录
     * @return 包含项目版本列表的响应对象
     */
    @GetMapping("/search")
    public Map<String, Object> getAllProjectVersions(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String versionName) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<ProjectVersion> versions = projectVersionService.getAllProjectVersions(projectId, versionName);

            // 为每个项目版本计算状态
            LocalDateTime now = LocalDateTime.now();
            for (ProjectVersion version : versions) {
                // 结束时间取当天的23:59:59，确保当天整天都算"进行中"
                LocalDateTime endOfDay = version.getEndTime() != null
                        ? version.getEndTime().toLocalDate().atTime(23, 59, 59)
                        : null;
                if (version.getStartTime() != null && now.isBefore(version.getStartTime())) {
                    version.setStatus(0); // 未开始
                } else if (endOfDay != null && now.isAfter(endOfDay)) {
                    version.setStatus(2); // 已结束
                } else {
                    version.setStatus(1); // 进行中
                }
            }

            result.put("code", 200);
            result.put("data", versions);
            result.put("message", "查询成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return result;
    }

    /**
     * 根据ID获取项目版本记录
     * @param id 项目版本ID
     * @return 包含项目版本信息的响应对象
     */
    @GetMapping("/{id}")
    public Map<String, Object> getProjectVersionById(@PathVariable Long id) {
        ProjectVersion version = projectVersionService.getProjectVersionById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", version);
        result.put("message", "查询成功");
        return result;
    }

    /**
     * 创建项目版本记录
     * @param version 项目版本对象
     * @return 操作结果响应对象
     */
    @PostMapping
    public Map<String, Object> createProjectVersion(@RequestBody ProjectVersion version) {
        Map<String, Object> authError = AdminRequired.check();
        if (authError != null) return authError;
        boolean success = projectVersionService.saveProjectVersion(version);
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("success", success);
        result.put("message", success ? "创建成功" : "创建失败");
        return result;
    }

    /**
     * 更新项目版本记录
     * @param id 项目版本ID
     * @param version 项目版本对象
     * @return 操作结果响应对象
     */
    @PutMapping("/{id}")
    public Map<String, Object> updateProjectVersion(@PathVariable Long id, @RequestBody ProjectVersion version) {
        Map<String, Object> authError = AdminRequired.check();
        if (authError != null) return authError;
        version.setId(id);
        boolean success = projectVersionService.updateProjectVersion(version);
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("success", success);
        result.put("message", success ? "更新成功" : "更新失败");
        return result;
    }

    /**
     * 删除项目版本记录
     * @param id 项目版本ID
     * @return 操作结果响应对象
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteProjectVersion(@PathVariable Long id) {
        Map<String, Object> authError = AdminRequired.check();
        if (authError != null) return authError;
        boolean success = projectVersionService.deleteProjectVersion(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("success", success);
        result.put("message", success ? "删除成功" : "删除失败");
        return result;
    }
}
