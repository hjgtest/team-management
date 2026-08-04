package com.teamdev.controller;

import com.teamdev.service.BugStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bug-stats")
public class BugStatsController {

    @Autowired
    private BugStatsService bugStatsService;

    /**
     * 获取月度Bug统计报表
     * @param year 年份
     * @param month 月份
     * @param projectId 项目ID（可选）
     * @param roleId 角色ID（可选）
     * @return Bug统计报表数据
     */
    @GetMapping("/monthly-report")
    public Map<String, Object> getMonthlyReport(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String roleId) {
        return bugStatsService.getMonthlyReport(year, month, projectId, roleId);
    }
}
