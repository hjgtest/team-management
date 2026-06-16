package com.teamdev.controller;

import com.teamdev.dto.SmartScheduleRequest;
import com.teamdev.dto.SmartScheduleResult;
import com.teamdev.service.SmartScheduleService;
import com.teamdev.config.AdminRequired;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 智能排期控制器
 */
@RestController
@RequestMapping("/api/smart-schedule")
public class SmartScheduleController {

    @Autowired
    private SmartScheduleService smartScheduleService;

    /**
     * 执行智能排期
     * @param request 排期请求（包含项目ID、版本号、产品经理、原型地址、分配列表）
     * @return 排期结果
     */
    @PostMapping("/schedule")
    public Map<String, Object> schedule(@RequestBody SmartScheduleRequest request) {
        Map<String, Object> result = new HashMap<>();

        // 管理员权限校验
        Map<String, Object> authError = AdminRequired.check();
        if (authError != null) return authError;

        // 参数校验
        if (request.getProjectId() == null) {
            result.put("code", 400);
            result.put("success", false);
            result.put("message", "请选择项目");
            return result;
        }
        if (request.getVersionName() == null || request.getVersionName().trim().isEmpty()) {
            result.put("code", 400);
            result.put("success", false);
            result.put("message", "请输入版本号");
            return result;
        }
        if (request.getAssignments() == null || request.getAssignments().isEmpty()) {
            result.put("code", 400);
            result.put("success", false);
            result.put("message", "请至少添加一个分配项");
            return result;
        }

        try {
            SmartScheduleResult scheduleResult = smartScheduleService.schedule(request);

            result.put("code", 200);
            result.put("success", scheduleResult.isSuccess());
            result.put("message", scheduleResult.getMessage());
            result.put("data", scheduleResult);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("success", false);
            result.put("message", "智能排期失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取项目的推荐人员列表（用于智能排期预览）
     * @param projectId 项目ID
     * @param roleId 角色ID（可选）
     */
    @GetMapping("/recommend")
    public Map<String, Object> getRecommendations(@RequestParam Long projectId,
                                                  @RequestParam(required = false) String roleId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 复用 ReleasedController 中的推荐逻辑
            // 这里返回按智能排期优先级排序的人员列表
            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", new HashMap<>()); // 暂时返回空，前端暂时不需要预览
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
        }

        return result;
    }
}
