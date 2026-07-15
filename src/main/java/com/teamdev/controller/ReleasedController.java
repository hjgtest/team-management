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
 * 释放人员管理
 * 查看即将/已释放的人员，为分配人员提供推荐
 */
@RestController
@RequestMapping("/api/released")
public class ReleasedController {

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectVersionService projectVersionService;

    @Autowired
    private ProjectAssignmentService projectAssignmentService;

    @Autowired
    private TeamService teamService;

    /**
     * 检查用户在指定时间范围内是否有任务冲突
     * @param userId 用户ID
     * @param startTime 时间范围开始
     * @param endTime 时间范围结束
     * @param assignByUser 按用户分组的分配记录
     * @param versionMap 版本映射
     * @return 有冲突返回true，否则返回false
     */
    private boolean hasConflictInTimeRange(Long userId,
                                           LocalDateTime startTime,
                                           LocalDateTime endTime,
                                           Map<Long, List<ProjectAssignment>> assignByUser,
                                           Map<Long, ProjectVersion> versionMap) {
        List<ProjectAssignment> userAssigns = assignByUser.getOrDefault(userId, new ArrayList<>());

        for (ProjectAssignment a : userAssigns) {
            ProjectVersion v = versionMap.get(a.getProjectVersionId());
            if (v == null || v.getStartTime() == null || v.getEndTime() == null) continue;

            // 检查时间是否重叠：两个时间范围 [s1, e1] 和 [s2, e2] 重叠的条件是：s1 < e2 && s2 < e1
            if (!v.getEndTime().isBefore(startTime) && !v.getStartTime().isAfter(endTime)) {
                return true; // 有冲突
            }
        }

        return false; // 无冲突
    }

    /**
     * 获取所有人员的释放状态
     * @param days 未来 N 天内即将释放
     */
    @GetMapping("/list")
    public Map<String, Object> getReleasedList(@RequestParam(defaultValue = "7") int days) {
        Map<String, Object> result = new HashMap<>();

        List<User> allUsers = userService.getActiveUsers();
        List<Project> allProjects = projectService.list();
        List<ProjectVersion> allVersions = projectVersionService.list();
        List<ProjectAssignment> allAssignments = projectAssignmentService.list();
        List<Team> allTeams = teamService.list();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureLimit = now.plusDays(days);

        // 构建映射
        Map<Long, String> projectNameMap = allProjects.stream()
                .collect(Collectors.toMap(Project::getId, Project::getProjectName, (a, b) -> a));
        Map<Long, ProjectVersion> versionMap = allVersions.stream()
                .collect(Collectors.toMap(ProjectVersion::getId, v -> v, (a, b) -> a));
        Map<Long, String> teamNameMap = allTeams.stream()
                .collect(Collectors.toMap(Team::getId, Team::getTeamName, (a, b) -> a));

        // 按用户分组分配
        Map<Long, List<ProjectAssignment>> assignByUser = new HashMap<>();
        for (ProjectAssignment a : allAssignments) {
            assignByUser.computeIfAbsent(a.getUserId(), k -> new ArrayList<>()).add(a);
        }

        List<Map<String, Object>> list = new ArrayList<>();

        for (User user : allUsers) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("roleName", user.getRoleName());
            userInfo.put("teamName", teamNameMap.getOrDefault(user.getTeamId(), "未分组"));
            userInfo.put("teamId", user.getTeamId());

            List<ProjectAssignment> userAssigns = assignByUser.getOrDefault(user.getId(), new ArrayList<>());

            // 进行中的任务
            List<Map<String, Object>> ongoingTasks = new ArrayList<>();
            LocalDateTime latestEndTime = null;
            boolean allFinished = true;

            for (ProjectAssignment a : userAssigns) {
                ProjectVersion v = versionMap.get(a.getProjectVersionId());
                if (v == null || v.getEndTime() == null) continue;

                LocalDateTime endOfDay = v.getEndTime().toLocalDate().atTime(23, 59, 59);
                boolean isOngoing = v.getStartTime() != null
                        && !now.isBefore(v.getStartTime())
                        && !now.isAfter(endOfDay);

                if (isOngoing) {
                    allFinished = false;
                    Map<String, Object> task = new HashMap<>();
                    task.put("projectName", projectNameMap.getOrDefault(v.getProjectId(), "未知项目"));
                    task.put("versionName", v.getVersionName());
                    task.put("endTime", v.getEndTime());
                    task.put("daysLeft", ChronoUnit.DAYS.between(now.toLocalDate(), v.getEndTime().toLocalDate()));
                    task.put("feature", a.getFeature());
                    ongoingTasks.add(task);

                    if (latestEndTime == null || v.getEndTime().isAfter(latestEndTime)) {
                        latestEndTime = v.getEndTime();
                    }
                }
            }

            // 按到期时间升序
            ongoingTasks.sort(Comparator.comparingLong(t -> (Long) t.get("daysLeft")));

            userInfo.put("tasks", ongoingTasks);
            userInfo.put("taskCount", ongoingTasks.size());

            // 状态判断
            String status;
            if (allFinished && ongoingTasks.isEmpty()) {
                status = "released";       // 已释放
            } else if (latestEndTime != null && ChronoUnit.DAYS.between(now.toLocalDate(), latestEndTime.toLocalDate()) <= days) {
                status = "upcoming";       // 即将释放
            } else {
                status = "busy";           // 忙碌中
            }

            userInfo.put("status", status);
            userInfo.put("releaseDate", latestEndTime);

            if (latestEndTime != null) {
                long daysLeft = ChronoUnit.DAYS.between(now.toLocalDate(), latestEndTime.toLocalDate());
                userInfo.put("daysLeft", daysLeft);
            } else {
                userInfo.put("daysLeft", -1);
            }

            // 只返回 已释放 和 即将释放 的
            if (!"busy".equals(status)) {
                list.add(userInfo);
            }
        }

        // 排序: 已释放优先, 然后按释放时间升序
        list.sort((a, b) -> {
            String sa = (String) a.get("status");
            String sb = (String) b.get("status");
            if (sa.equals(sb)) {
                long da = ((Number) a.getOrDefault("daysLeft", 999L)).longValue();
                long db = ((Number) b.getOrDefault("daysLeft", 999L)).longValue();
                return Long.compare(da, db);
            }
            if ("released".equals(sa)) return -1;
            if ("released".equals(sb)) return 1;
            return 0;
        });

        result.put("code", 200);
        result.put("data", list);
        result.put("message", "查询成功");
        return result;
    }

    /**
     * 获取可用人员列表（已释放 + 即将释放），支持按角色筛选
     * @param projectVersionId 当前分配的版本ID，用于标记做过该项目的人员，并基于该版本时间范围判断冲突
     */
    @GetMapping("/recommend")
    public Map<String, Object> getRecommend(@RequestParam(required = false) Long teamId,
                                            @RequestParam(required = false) Integer roleId,
                                            @RequestParam(defaultValue = "14") int days,
                                            @RequestParam(required = false) Long projectVersionId) {
        Map<String, Object> result = new HashMap<>();

        List<User> allUsers = userService.getAssignableUsers();
        List<Project> allProjects = projectService.list();
        List<ProjectVersion> allVersions = projectVersionService.list();
        List<ProjectAssignment> allAssignments = projectAssignmentService.list();
        List<Team> allTeams = teamService.list();

        LocalDateTime now = LocalDateTime.now();

        // 获取当前版本的所属项目ID和时间范围
        Long currentProjectId = null;
        LocalDateTime versionStartTime = null;
        LocalDateTime versionEndTime = null;
        Map<Long, ProjectVersion> verMap = allVersions.stream()
                .collect(Collectors.toMap(ProjectVersion::getId, v -> v, (a, b) -> a));
        if (projectVersionId != null) {
            ProjectVersion cur = verMap.get(projectVersionId);
            if (cur != null) {
                currentProjectId = cur.getProjectId();
                versionStartTime = cur.getStartTime();
                versionEndTime = cur.getEndTime();
            }
        }
        Map<Long, String> projectNameMap = allProjects.stream()
                .collect(Collectors.toMap(Project::getId, Project::getProjectName, (a, b) -> a));
        Map<Long, String> teamNameMap = allTeams.stream()
                .collect(Collectors.toMap(Team::getId, Team::getTeamName, (a, b) -> a));

        // 按用户分组分配
        Map<Long, List<ProjectAssignment>> assignByUser = new HashMap<>();
        for (ProjectAssignment a : allAssignments) {
            assignByUser.computeIfAbsent(a.getUserId(), k -> new ArrayList<>()).add(a);
        }

        List<Map<String, Object>> list = new ArrayList<>();

        for (User user : allUsers) {
            if (teamId != null && !teamId.equals(user.getTeamId())) continue;
            if (roleId != null && !roleId.equals(user.getRoleId())) continue;

            Map<String, Object> info = new HashMap<>();
            info.put("userId", user.getId());
            info.put("username", user.getUsername());
            info.put("roleName", user.getRoleName());
            info.put("roleId", user.getRoleId());
            info.put("teamName", teamNameMap.getOrDefault(user.getTeamId(), "未分组"));

            List<ProjectAssignment> userAssigns = assignByUser.getOrDefault(user.getId(), new ArrayList<>());

            int ongoingCount = 0;
            LocalDateTime latestEnd = null;

            // 历史项目参与：该用户参与过的当前项目版本
            List<String> historyVersions = new ArrayList<>();
            for (ProjectAssignment a : userAssigns) {
                ProjectVersion v = verMap.get(a.getProjectVersionId());
                if (v == null) continue;

                // 计算负载（基于当前时间）
                if (v.getEndTime() != null) {
                    LocalDateTime endOfDay = v.getEndTime().toLocalDate().atTime(23, 59, 59);
                    if (v.getStartTime() != null && !now.isBefore(v.getStartTime()) && !now.isAfter(endOfDay)) {
                        ongoingCount++;
                        if (latestEnd == null || v.getEndTime().isAfter(latestEnd)) {
                            latestEnd = v.getEndTime();
                        }
                    }
                }

                // 如果匹配当前项目，记录历史版本
                if (currentProjectId != null && currentProjectId.equals(v.getProjectId())) {
                    historyVersions.add(v.getVersionName());
                }
            }

            info.put("ongoingCount", ongoingCount);
            info.put("hasHistory", !historyVersions.isEmpty());
            info.put("historyVersions", historyVersions);

            if (latestEnd != null) {
                info.put("releaseDate", latestEnd);
                info.put("daysLeft", ChronoUnit.DAYS.between(now.toLocalDate(), latestEnd.toLocalDate()));
            } else {
                info.put("releaseDate", null);
                info.put("daysLeft", -1);
            }

            // 新增：基于当前版本时间范围判断是否有冲突
            boolean hasConflict = false;
            if (versionStartTime != null && versionEndTime != null) {
                hasConflict = hasConflictInTimeRange(user.getId(), versionStartTime, versionEndTime, assignByUser, verMap);
            }
            info.put("hasConflictInVersionTime", hasConflict);

            // 状态: 基于是否在版本时间范围内有冲突来判断
            String status;
            if (hasConflict) {
                status = "busy";  // 在版本时间范围内有冲突，标记为忙碌
            } else if (ongoingCount == 0) {
                status = "released";  // 当前无进行中任务
            } else if (((Number) info.get("daysLeft")).longValue() <= days) {
                status = "upcoming";  // 即将释放
            } else {
                status = "available";  // 当前忙，但在版本时间范围内无冲突，可用
            }
            info.put("status", status);

            // 返回所有匹配角色的人员（按智能排序：已释放 → 即将释放 → 可用 → 忙碌）
            list.add(info);
        }

        // 排序: 1. 做过本项目优先 2. 状态(已释放 > 即将释放 > 可用 > 忙碌) 3. 任务数 4. 释放时间
        list.sort((a, b) -> {
            // 做过本项目优先
            boolean ha = (boolean) a.getOrDefault("hasHistory", false);
            boolean hb = (boolean) b.getOrDefault("hasHistory", false);
            if (ha != hb) return ha ? -1 : 1;

            // 状态排序
            String sa = (String) a.get("status");
            String sb = (String) b.get("status");
            int orderA = getStatusOrder(sa);
            int orderB = getStatusOrder(sb);
            if (orderA != orderB) return Integer.compare(orderA, orderB);

            // 任务数
            int countA = (int) a.get("ongoingCount");
            int countB = (int) b.get("ongoingCount");
            if (countA != countB) return Integer.compare(countA, countB);

            // 释放时间
            long daysA = ((Number) a.get("daysLeft")).longValue();
            long daysB = ((Number) b.get("daysLeft")).longValue();
            return Long.compare(daysA, daysB);
        });

        result.put("code", 200);
        result.put("data", list);
        result.put("message", "查询成功");
        return result;
    }

    /**
     * 获取状态排序顺序
     */
    private int getStatusOrder(String status) {
        switch (status) {
            case "released": return 1;
            case "upcoming": return 2;
            case "available": return 3;
            case "busy": return 4;
            default: return 5;
        }
    }
}
