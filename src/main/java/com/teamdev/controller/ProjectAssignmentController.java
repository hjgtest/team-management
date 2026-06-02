package com.teamdev.controller;

import com.teamdev.entity.ProjectAssignment;
import com.teamdev.service.ProjectAssignmentService;
import com.teamdev.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目分配管理控制器
 * 提供项目分配相关的CRUD操作接口
 */
@RestController
@RequestMapping("/api/projectAssignments")
public class ProjectAssignmentController {
    
    @Autowired
    private ProjectAssignmentService projectAssignmentService;
    
    @Autowired
    private ProjectService projectService;
    
    /**
     * 获取所有项目分配记录
     * @return 包含项目分配列表的响应对象
     */
    @GetMapping
    public Map<String, Object> getAllAssignments() {
        List<ProjectAssignment> assignments = projectAssignmentService.list();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", assignments);
        result.put("message", "查询成功");
        return result;
    }
    
    /**
     * 根据版本ID获取项目分配记录
     * @param versionId 版本ID
     * @return 包含项目分配列表的响应对象
     */
    @GetMapping("/version/{versionId}")
    public Map<String, Object> getAssignmentsByVersionId(@PathVariable Long versionId) {
        List<ProjectAssignment> assignments = projectAssignmentService.getAssignmentsByVersionId(versionId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", assignments);
        result.put("message", "查询成功");
        return result;
    }
    
    /**
     * 创建项目分配记录
     * @param assignment 项目分配对象
     * @return 操作结果响应对象
     */
    @PostMapping
    public Map<String, Object> createAssignment(@RequestBody ProjectAssignment assignment) {
        boolean success = projectAssignmentService.saveAssignment(assignment);
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("success", success);
        result.put("message", success ? "创建成功" : "创建失败");
        return result;
    }
    
    /**
     * 更新项目分配记录
     * @param id 项目分配ID
     * @param assignment 项目分配对象
     * @return 操作结果响应对象
     */
    @PutMapping("/{id}")
    public Map<String, Object> updateAssignment(@PathVariable Long id, @RequestBody ProjectAssignment assignment) {
        assignment.setId(id);
        boolean success = projectAssignmentService.updateAssignment(assignment);
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("success", success);
        result.put("message", success ? "更新成功" : "更新失败");
        return result;
    }
    
    /**
     * 删除项目分配记录
     * @param id 项目分配ID
     * @return 操作结果响应对象
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteAssignment(@PathVariable Long id) {
        boolean success = projectAssignmentService.deleteAssignment(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("success", success);
        result.put("message", success ? "删除成功" : "删除失败");
        return result;
    }

    /**
     * 批量创建项目分配记录
     * @param assignments 项目分配对象列表
     * @return 操作结果响应对象
     */
    @PostMapping("/batch")
    public Map<String, Object> batchCreateAssignments(@RequestBody List<ProjectAssignment> assignments) {
        boolean success = projectAssignmentService.saveBatch(assignments);
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("success", success);
        result.put("message", success ? "批量保存成功" : "批量保存失败");
        return result;
    }

    /**
     * 导出任务数据（项目→版本→负责人层级结构）
     * @return 包含层级结构数据的响应对象
     */
    @GetMapping("/export")
    public Map<String, Object> exportTasksData() {
        try {
            // 使用ProjectService中已有的方法获取层级数据
            // 这里我们获取所有用户进行中的项目和版本数据（可以调整为所有项目）
            // 为了导出所有数据，我们可以调用类似的方法
            
            // 获取所有项目
            List<Map<String, Object>> allProjectsAndVersions = projectService.getAllProjectsAndVersions();
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", allProjectsAndVersions);
            result.put("message", "导出数据获取成功");
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "导出数据获取失败: " + e.getMessage());
            return result;
        }
    }
}