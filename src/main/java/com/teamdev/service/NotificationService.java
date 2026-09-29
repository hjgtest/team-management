package com.teamdev.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teamdev.entity.Notification;
import com.teamdev.entity.Project;
import com.teamdev.entity.ProjectAssignment;
import com.teamdev.entity.ProjectVersion;
import com.teamdev.entity.User;
import com.teamdev.mapper.NotificationMapper;
import com.teamdev.mapper.ProjectAssignmentMapper;
import com.teamdev.mapper.ProjectVersionMapper;
import com.teamdev.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 通知服务
 * 负责到期预警扫描和通知管理
 */
@Service
public class NotificationService extends ServiceImpl<NotificationMapper, Notification> {

    @Autowired
    private ProjectVersionMapper projectVersionMapper;

    @Autowired
    private ProjectAssignmentMapper projectAssignmentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private com.teamdev.service.ProjectService projectService;

    /**
     * 获取用户的通知列表（按时间倒序）
     */
    public List<Notification> getUserNotifications(int limit) {
        return this.lambdaQuery()
                .orderByDesc(Notification::getCreatedTime)
                .last("LIMIT " + limit)
                .list();
    }

    /**
     * 获取未读通知数量
     */
    public long getUnreadCount() {
        return this.lambdaQuery()
                .eq(Notification::getIsRead, 0)
                .count();
    }

    /**
     * 标记通知为已读
     */
    public boolean markAsRead(Long id) {
        Notification notification = this.getById(id);
        if (notification != null) {
            notification.setIsRead(1);
            return this.updateById(notification);
        }
        return false;
    }

    /**
     * 标记所有通知为已读
     */
    public boolean markAllAsRead() {
        List<Notification> unread = this.lambdaQuery()
                .eq(Notification::getIsRead, 0)
                .list();
        for (Notification n : unread) {
            n.setIsRead(1);
            this.updateById(n);
        }
        return true;
    }

    /**
     * 扫描即将到期的版本，生成预警通知
     * 可被定时任务或手动触发
     */
    public List<Notification> scanAndCreateDeadlineWarnings() {
        LocalDateTime now = LocalDateTime.now();
        List<ProjectVersion> allVersions = projectVersionMapper.selectList(null);
        List<Project> allProjects = projectService.list();
        Map<Long, String> projectNameMap = allProjects.stream()
                .collect(Collectors.toMap(Project::getId, Project::getProjectName, (a, b) -> a));

        // 获取所有分配
        List<ProjectAssignment> allAssignments = projectAssignmentMapper.selectList(null);
        Map<Long, List<ProjectAssignment>> assignByVersion = allAssignments.stream()
                .collect(Collectors.groupingBy(ProjectAssignment::getProjectVersionId));

        // 获取所有用户
        List<User> allUsers = userMapper.selectBatchIds(
                allAssignments.stream().map(ProjectAssignment::getUserId).distinct().collect(Collectors.toList())
        );
        Map<Long, String> userNameMap = allUsers.stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));

        List<Notification> newNotifications = new ArrayList<>();

        for (ProjectVersion version : allVersions) {
            if (version.getEndTime() == null || version.getStartTime() == null) continue;

            LocalDateTime endOfDay = version.getEndTime().toLocalDate().atTime(23, 59, 59);
            long daysLeft = ChronoUnit.DAYS.between(now.toLocalDate(), version.getEndTime().toLocalDate());

            // 只处理未来7天内到期或刚过期的版本
            if (daysLeft > 7 || daysLeft < -1) continue;

            // 已结束的跳过
            if (now.isAfter(endOfDay)) continue;

            String projectName = projectNameMap.getOrDefault(version.getProjectId(), "未知项目");

            // 获取负责人
            List<ProjectAssignment> versionAssigns = assignByVersion.getOrDefault(version.getId(), new ArrayList<>());
            String assigneeNames = versionAssigns.stream()
                    .map(a -> userNameMap.getOrDefault(a.getUserId(), "未知"))
                    .distinct()
                    .collect(Collectors.joining("、"));

            // 确定紧急程度
            int urgency;
            String title;
            if (daysLeft <= 0) {
                urgency = 2;
                title = "【今日到期】" + projectName + " / " + version.getVersionName();
            } else if (daysLeft <= 1) {
                urgency = 2;
                title = "【明天到期】" + projectName + " / " + version.getVersionName();
            } else if (daysLeft <= 3) {
                urgency = 1;
                title = "【" + daysLeft + "天后到期】" + projectName + " / " + version.getVersionName();
            } else {
                urgency = 0;
                title = "【" + daysLeft + "天后到期】" + projectName + " / " + version.getVersionName();
            }

            String content = "负责人：" + (assigneeNames.isEmpty() ? "未分配" : assigneeNames)
                    + "，研发截止：" + version.getEndTime().toLocalDate();
            if (version.getTestTime() != null) {
                content += "，提测：" + version.getTestTime().toLocalDate();
            }

            // 检查是否已有相同通知（避免重复）
            boolean exists = this.lambdaQuery()
                    .eq(Notification::getVersionId, version.getId())
                    .eq(Notification::getType, "DEADLINE_WARNING")
                    .ge(Notification::getCreatedTime, now.toLocalDate().atStartOfDay())
                    .count() > 0;

            if (!exists) {
                Notification notification = new Notification();
                notification.setType("DEADLINE_WARNING");
                notification.setTitle(title);
                notification.setContent(content);
                notification.setProjectId(version.getProjectId());
                notification.setVersionId(version.getId());
                notification.setUrgency(urgency);
                notification.setIsRead(0);
                notification.setCreatedTime(now);
                this.save(notification);
                newNotifications.add(notification);
            }
        }

        return newNotifications;
    }
}
