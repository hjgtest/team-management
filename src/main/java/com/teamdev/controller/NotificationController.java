package com.teamdev.controller;

import com.teamdev.entity.Notification;
import com.teamdev.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知控制器
 * 提供站内通知的查询和管理接口
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * 获取通知列表
     */
    @GetMapping
    public Map<String, Object> getNotifications(@RequestParam(defaultValue = "50") int limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Notification> notifications = notificationService.getUserNotifications(limit);
            long unreadCount = notificationService.getUnreadCount();
            result.put("code", 200);
            result.put("data", notifications);
            result.put("unreadCount", unreadCount);
            result.put("message", "查询成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread-count")
    public Map<String, Object> getUnreadCount() {
        Map<String, Object> result = new HashMap<>();
        try {
            long count = notificationService.getUnreadCount();
            result.put("code", 200);
            result.put("data", count);
            result.put("message", "查询成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 标记通知为已读
     */
    @PutMapping("/{id}/read")
    public Map<String, Object> markAsRead(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        boolean success = notificationService.markAsRead(id);
        result.put("code", success ? 200 : 500);
        result.put("message", success ? "操作成功" : "通知不存在");
        return result;
    }

    /**
     * 标记所有通知为已读
     */
    @PutMapping("/read-all")
    public Map<String, Object> markAllAsRead() {
        Map<String, Object> result = new HashMap<>();
        boolean success = notificationService.markAllAsRead();
        result.put("code", success ? 200 : 500);
        result.put("message", "操作成功");
        return result;
    }

    /**
     * 手动触发到期预警扫描
     */
    @PostMapping("/scan-deadlines")
    public Map<String, Object> scanDeadlines() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Notification> newNotifications = notificationService.scanAndCreateDeadlineWarnings();
            result.put("code", 200);
            result.put("data", newNotifications.size());
            result.put("message", "扫描完成，新增 " + newNotifications.size() + " 条预警");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "扫描失败: " + e.getMessage());
        }
        return result;
    }
}
