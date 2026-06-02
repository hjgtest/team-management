package com.teamdev.controller;

import com.teamdev.entity.*;
import com.teamdev.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 仪表盘控制器
 * 提供任务概览、即将到期任务、人员负载等汇总信息
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectVersionService projectVersionService;

    @Autowired
    private ProjectAssignmentService projectAssignmentService;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    /**
     * 获取仪表盘概览数据
     */
    @GetMapping("/overview")
    public Map<String, Object> getOverview() {
        Map<String, Object> result = new HashMap<>();

        List<Project> allProjects = projectService.getAllProjects(null);
        List<User> allUsers = userService.getAllUsers();

        // 项目统计
        long ongoingProjects = allProjects.stream().filter(p -> p.getStatus() != null && p.getStatus() == 1).count();
        long finishedProjects = allProjects.stream().filter(p -> p.getStatus() != null && p.getStatus() == 2).count();
        long notStartedProjects = allProjects.stream().filter(p -> p.getStatus() != null && p.getStatus() == 0).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProjects", allProjects.size());
        stats.put("ongoingProjects", ongoingProjects);
        stats.put("finishedProjects", finishedProjects);
        stats.put("notStartedProjects", notStartedProjects);
        stats.put("totalUsers", allUsers.size());

        result.put("code", 200);
        result.put("data", stats);
        result.put("message", "查询成功");
        return result;
    }

    /**
     * 获取即将到期的版本列表（未来N天内到期的进行中版本）
     */
    @GetMapping("/upcoming-deadlines")
    public Map<String, Object> getUpcomingDeadlines(@RequestParam(defaultValue = "7") int days) {
        Map<String, Object> result = new HashMap<>();

        // 获取所有版本
        List<ProjectVersion> allVersions = projectVersionService.list();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusDays(days);

        // 获取所有项目用于名称映射
        List<Project> allProjects = projectService.list();
        Map<Long, String> projectNameMap = new HashMap<>();
        for (Project p : allProjects) {
            projectNameMap.put(p.getId(), p.getProjectName());
        }

        // 获取所有分配
        List<ProjectAssignment> allAssignments = projectAssignmentService.list();
        Map<Long, List<ProjectAssignment>> assignmentsByVersion = new HashMap<>();
        for (ProjectAssignment a : allAssignments) {
            assignmentsByVersion.computeIfAbsent(a.getProjectVersionId(), k -> new ArrayList<>()).add(a);
        }

        // 获取所有用户
        List<User> allUsers = userService.list();
        Map<Long, String> userNameMap = new HashMap<>();
        for (User u : allUsers) {
            userNameMap.put(u.getId(), u.getUsername());
        }

        List<Map<String, Object>> upcomingList = new ArrayList<>();

        for (ProjectVersion version : allVersions) {
            if (version.getEndTime() == null || version.getStartTime() == null) continue;

            LocalDateTime endOfDay = version.getEndTime().toLocalDate().atTime(23, 59, 59);

            // 只取进行中或即将到期的（endTime在now之后且在deadline之前）
            boolean isNotFinished = !now.isAfter(endOfDay);
            boolean isWithinRange = !endOfDay.isAfter(deadline.toLocalDate().atTime(23, 59, 59));

            if (isNotFinished && isWithinRange) {
                Map<String, Object> item = new HashMap<>();
                item.put("versionId", version.getId());
                item.put("versionName", version.getVersionName());
                item.put("projectId", version.getProjectId());
                item.put("projectName", projectNameMap.getOrDefault(version.getProjectId(), "未知项目"));
                item.put("startTime", version.getStartTime());
                item.put("endTime", version.getEndTime());
                item.put("testTime", version.getTestTime());
                item.put("launchTime", version.getLaunchTime());
                item.put("productManager", version.getProductManager());
                item.put("prototypeUrl", version.getPrototypeUrl());

                // 计算研发截止剩余天数
                long daysLeft = ChronoUnit.DAYS.between(now.toLocalDate(), version.getEndTime().toLocalDate());
                item.put("daysLeft", daysLeft);

                // 计算提测剩余天数
                if (version.getTestTime() != null) {
                    long testDaysLeft = ChronoUnit.DAYS.between(now.toLocalDate(), version.getTestTime().toLocalDate());
                    item.put("testDaysLeft", testDaysLeft);
                }

                // 计算上线剩余天数
                if (version.getLaunchTime() != null) {
                    long launchDaysLeft = ChronoUnit.DAYS.between(now.toLocalDate(), version.getLaunchTime().toLocalDate());
                    item.put("launchDaysLeft", launchDaysLeft);
                }

                // 紧急程度：0=今天到期，1=明天到期，2=3天内，3=一周内
                int urgency;
                if (daysLeft <= 0) urgency = 0;
                else if (daysLeft == 1) urgency = 1;
                else if (daysLeft <= 3) urgency = 2;
                else urgency = 3;
                item.put("urgency", urgency);

                // 负责人员
                List<ProjectAssignment> versionAssignments = assignmentsByVersion.getOrDefault(version.getId(), new ArrayList<>());
                List<String> assignees = versionAssignments.stream()
                        .map(a -> userNameMap.getOrDefault(a.getUserId(), "未知"))
                        .distinct()
                        .collect(Collectors.toList());
                item.put("assignees", assignees);

                upcomingList.add(item);
            }
        }

        // 按剩余天数排序
        upcomingList.sort(Comparator.comparingLong(a -> (Long) a.get("daysLeft")));

        result.put("code", 200);
        result.put("data", upcomingList);
        result.put("message", "查询成功");
        return result;
    }

    /**
     * 获取人员任务负载概览
     */
    @GetMapping("/user-workload")
    public Map<String, Object> getUserWorkload() {
        Map<String, Object> result = new HashMap<>();

        List<User> allUsers = userService.getAllUsers();
        List<ProjectAssignment> allAssignments = projectAssignmentService.list();
        List<ProjectVersion> allVersions = projectVersionService.list();
        List<Project> allProjects = projectService.list();

        LocalDateTime now = LocalDateTime.now();

        // 构建映射
        Map<Long, ProjectVersion> versionMap = new HashMap<>();
        for (ProjectVersion v : allVersions) {
            versionMap.put(v.getId(), v);
        }
        Map<Long, String> projectNameMap = new HashMap<>();
        for (Project p : allProjects) {
            projectNameMap.put(p.getId(), p.getProjectName());
        }

        // 按用户分组分配
        Map<Long, List<ProjectAssignment>> assignmentsByUser = new HashMap<>();
        for (ProjectAssignment a : allAssignments) {
            assignmentsByUser.computeIfAbsent(a.getUserId(), k -> new ArrayList<>()).add(a);
        }

        List<Map<String, Object>> workloadList = new ArrayList<>();

        for (User user : allUsers) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("roleName", user.getRoleName());
            userInfo.put("teamName", user.getTeamName());

            List<ProjectAssignment> userAssignments = assignmentsByUser.getOrDefault(user.getId(), new ArrayList<>());

            int ongoingCount = 0;
            LocalDateTime earliestDeadline = null;
            List<Map<String, Object>> tasks = new ArrayList<>();

            for (ProjectAssignment assignment : userAssignments) {
                ProjectVersion version = versionMap.get(assignment.getProjectVersionId());
                if (version == null || version.getEndTime() == null) continue;

                LocalDateTime endOfDay = version.getEndTime().toLocalDate().atTime(23, 59, 59);
                boolean isOngoing = version.getStartTime() != null
                        && !now.isBefore(version.getStartTime())
                        && !now.isAfter(endOfDay);

                if (isOngoing) {
                    ongoingCount++;
                    if (earliestDeadline == null || version.getEndTime().isBefore(earliestDeadline)) {
                        earliestDeadline = version.getEndTime();
                    }

                    Map<String, Object> task = new HashMap<>();
                    task.put("projectName", projectNameMap.getOrDefault(version.getProjectId(), "未知"));
                    task.put("versionName", version.getVersionName());
                    task.put("endTime", version.getEndTime());
                    task.put("testTime", version.getTestTime());
                    task.put("launchTime", version.getLaunchTime());
                    task.put("productManager", version.getProductManager());
                    task.put("feature", assignment.getFeature());
                    long daysLeft = ChronoUnit.DAYS.between(now.toLocalDate(), version.getEndTime().toLocalDate());
                    task.put("daysLeft", daysLeft);
                    if (version.getTestTime() != null) {
                        task.put("testDaysLeft", ChronoUnit.DAYS.between(now.toLocalDate(), version.getTestTime().toLocalDate()));
                    }
                    if (version.getLaunchTime() != null) {
                        task.put("launchDaysLeft", ChronoUnit.DAYS.between(now.toLocalDate(), version.getLaunchTime().toLocalDate()));
                    }
                    tasks.add(task);
                }
            }

            // 按截止时间排序
            tasks.sort(Comparator.comparingLong(a -> (Long) a.get("daysLeft")));

            userInfo.put("ongoingCount", ongoingCount);
            userInfo.put("earliestDeadline", earliestDeadline);
            userInfo.put("tasks", tasks);
            workloadList.add(userInfo);
        }

        // 按进行中任务数降序排序
        workloadList.sort((a, b) -> (int) b.get("ongoingCount") - (int) a.get("ongoingCount"));

        result.put("code", 200);
        result.put("data", workloadList);
        result.put("message", "查询成功");
        return result;
    }
}
