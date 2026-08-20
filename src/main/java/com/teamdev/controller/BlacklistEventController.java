package com.teamdev.controller;

import com.teamdev.config.AdminRequired;
import com.teamdev.entity.BlacklistEvent;
import com.teamdev.service.BlacklistEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 黑榜事件控制器
 */
@RestController
@RequestMapping("/api/blacklist")
public class BlacklistEventController {

    @Autowired
    private BlacklistEventService blacklistEventService;

    /**
     * 获取月度黑榜统计
     */
    @GetMapping("/monthly-stats")
    public Map<String, Object> getMonthlyStats(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return blacklistEventService.getMonthlyStats(year, month);
    }

    /**
     * 获取所有黑榜事件（支持筛选）
     */
    @GetMapping("/events")
    public Map<String, Object> getAllEvents(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long userId) {
        try {
            List<BlacklistEvent> events = blacklistEventService.getAllEvents(year, month, userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", events);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
            result.put("error", e.getClass().getName());
            return result;
        }
    }

    /**
     * 添加黑榜事件（需要管理员权限）
     */
    @PostMapping("/events")
    public Map<String, Object> addEvent(@RequestBody BlacklistEvent event) {
        // 权限检查
        Map<String, Object> authError = AdminRequired.check();
        if (authError != null) return authError;
        
        boolean success = blacklistEventService.addEvent(event);
        
        Map<String, Object> result = new HashMap<>();
        if (success) {
            result.put("code", 200);
            result.put("message", "添加成功");
        } else {
            result.put("code", 500);
            result.put("message", "添加失败");
        }
        return result;
    }

    /**
     * 更新黑榜事件（需要管理员权限）
     */
    @PutMapping("/events/{id}")
    public Map<String, Object> updateEvent(
            @PathVariable Long id,
            @RequestBody BlacklistEvent event) {
        // 权限检查
        Map<String, Object> authError = AdminRequired.check();
        if (authError != null) return authError;
        
        event.setId(id);
        boolean success = blacklistEventService.updateEvent(event);
        
        Map<String, Object> result = new HashMap<>();
        if (success) {
            result.put("code", 200);
            result.put("message", "更新成功");
        } else {
            result.put("code", 500);
            result.put("message", "更新失败");
        }
        return result;
    }

    /**
     * 删除黑榜事件（需要管理员权限）
     */
    @DeleteMapping("/events/{id}")
    public Map<String, Object> deleteEvent(@PathVariable Long id) {
        // 权限检查
        Map<String, Object> authError = AdminRequired.check();
        if (authError != null) return authError;
        
        boolean success = blacklistEventService.deleteEvent(id);
        
        Map<String, Object> result = new HashMap<>();
        if (success) {
            result.put("code", 200);
            result.put("message", "删除成功");
        } else {
            result.put("code", 500);
            result.put("message", "删除失败");
        }
        return result;
    }

    /**
     * 获取用户历史黑榜事件
     */
    @GetMapping("/user-history")
    public Map<String, Object> getUserHistory(
            @RequestParam Long userId,
            @RequestParam(required = false) Integer year) {
        List<BlacklistEvent> events = blacklistEventService.getUserHistory(userId, year);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", events);
        return result;
    }
}
