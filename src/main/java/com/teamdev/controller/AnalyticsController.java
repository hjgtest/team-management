package com.teamdev.controller;

import com.teamdev.entity.*;
import com.teamdev.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据分析控制器
 * 提供团队负载热力图、月度趋势报表、仪表盘增强数据等接口
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

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

    @Autowired
    private RoleService roleService;

    /**
     * 团队负载热力图数据
     * 按小组分组，展示每个成员的当前任务数和任务详情
     */
    @GetMapping("/workload-heatmap")
    public Map<String, Object> getWorkloadHeatmap() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<User> allUsers = userService.getActiveUsers();
            List<ProjectAssignment> allAssignments = projectAssignmentService.list();
            List<ProjectVersion> allVersions = projectVersionService.list();
            List<Team> allTeams = teamService.list();
            List<Role> allRoles = roleService.list();

            LocalDateTime now = LocalDateTime.now();

            // 构建映射
            Map<Long, ProjectVersion> versionMap = allVersions.stream()
                    .collect(Collectors.toMap(ProjectVersion::getId, v -> v, (a, b) -> a));
            Map<Long, String> teamNameMap = allTeams.stream()
                    .collect(Collectors.toMap(Team::getId, Team::getTeamName, (a, b) -> a));
            Map<Long, String> roleNameMap = allRoles.stream()
                    .collect(Collectors.toMap(Role::getId, Role::getRoleName, (a, b) -> a));

            // 按用户分组分配
            Map<Long, List<ProjectAssignment>> assignByUser = allAssignments.stream()
                    .collect(Collectors.groupingBy(ProjectAssignment::getUserId));

            // 按小组分组用户
            Map<Integer, List<Map<String, Object>>> teamMembers = new LinkedHashMap<>();
            for (Team team : allTeams) {
                teamMembers.put(team.getId().intValue(), new ArrayList<>());
            }
            teamMembers.put(null, new ArrayList<>()); // 未分组

            for (User user : allUsers) {
                Map<String, Object> memberInfo = new HashMap<>();
                memberInfo.put("userId", user.getId());
                memberInfo.put("username", user.getUsername());
                memberInfo.put("roleName", roleNameMap.getOrDefault(user.getRoleId(), "未分配"));

                List<ProjectAssignment> userAssigns = assignByUser.getOrDefault(user.getId(), new ArrayList<>());
                int ongoingCount = 0;
                List<Map<String, Object>> tasks = new ArrayList<>();

                for (ProjectAssignment a : userAssigns) {
                    ProjectVersion v = versionMap.get(a.getProjectVersionId());
                    if (v == null || v.getEndTime() == null || v.getStartTime() == null) continue;

                    LocalDateTime endOfDay = v.getEndTime().toLocalDate().atTime(23, 59, 59);
                    if (!now.isBefore(v.getStartTime()) && !now.isAfter(endOfDay)) {
                        ongoingCount++;
                        Map<String, Object> task = new HashMap<>();
                        task.put("versionName", v.getVersionName());
                        task.put("feature", a.getFeature());
                        task.put("daysLeft", ChronoUnit.DAYS.between(now.toLocalDate(), v.getEndTime().toLocalDate()));
                        tasks.add(task);
                    }
                }

                memberInfo.put("ongoingCount", ongoingCount);
                memberInfo.put("maxCapacity", 4);
                memberInfo.put("tasks", tasks);

                Integer teamId = user.getTeamId();
                if (teamMembers.containsKey(teamId)) {
                    teamMembers.get(teamId).add(memberInfo);
                } else {
                    teamMembers.get(null).add(memberInfo);
                }
            }

            // 构建结果
            List<Map<String, Object>> groups = new ArrayList<>();
            for (Team team : allTeams) {
                List<Map<String, Object>> members = teamMembers.get(team.getId().intValue());
                if (members != null && !members.isEmpty()) {
                    Map<String, Object> group = new HashMap<>();
                    group.put("teamId", team.getId());
                    group.put("teamName", team.getTeamName());
                    group.put("members", members);
                    group.put("avgLoad", members.stream()
                            .mapToInt(m -> (int) m.get("ongoingCount")).average().orElse(0));
                    groups.add(group);
                }
            }
            // 未分组的
            List<Map<String, Object>> ungrouped = teamMembers.get(null);
            if (ungrouped != null && !ungrouped.isEmpty()) {
                Map<String, Object> group = new HashMap<>();
                group.put("teamId", 0);
                group.put("teamName", "未分组");
                group.put("members", ungrouped);
                group.put("avgLoad", ungrouped.stream()
                        .mapToInt(m -> (int) m.get("ongoingCount")).average().orElse(0));
                groups.add(group);
            }

            result.put("code", 200);
            result.put("data", groups);
            result.put("message", "查询成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 月度趋势报表
     * 返回最近 N 个月的团队绩效趋势数据
     */
    @GetMapping("/monthly-trend")
    public Map<String, Object> getMonthlyTrend(@RequestParam(defaultValue = "6") int months) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<ProjectVersion> allVersions = projectVersionService.list();
            List<ProjectAssignment> allAssignments = projectAssignmentService.list();
            List<Project> allProjects = projectService.list();

            // 构建映射
            Map<Long, ProjectVersion> versionMap = allVersions.stream()
                    .collect(Collectors.toMap(ProjectVersion::getId, v -> v, (a, b) -> a));

            // 按月统计
            LocalDate today = LocalDate.now();
            List<Map<String, Object>> monthlyData = new ArrayList<>();

            for (int i = months - 1; i >= 0; i--) {
                LocalDate monthDate = today.minusMonths(i);
                int year = monthDate.getYear();
                int month = monthDate.getMonthValue();
                LocalDate firstDay = LocalDate.of(year, month, 1);
                LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());

                Map<String, Object> monthStat = new HashMap<>();
                monthStat.put("year", year);
                monthStat.put("month", month);
                monthStat.put("label", year + "-" + String.format("%02d", month));

                // 统计该月结束的版本数
                long finishedVersions = allVersions.stream()
                        .filter(v -> v.getEndTime() != null
                                && !v.getEndTime().toLocalDate().isBefore(firstDay)
                                && !v.getEndTime().toLocalDate().isAfter(lastDay))
                        .count();
                monthStat.put("finishedVersions", finishedVersions);

                // 统计该月延期的版本数
                long delayedVersions = allVersions.stream()
                        .filter(v -> v.getIsDelayed() != null && v.getIsDelayed()
                                && v.getEndTime() != null
                                && !v.getEndTime().toLocalDate().isBefore(firstDay)
                                && !v.getEndTime().toLocalDate().isAfter(lastDay))
                        .count();
                monthStat.put("delayedVersions", delayedVersions);

                // 统计该月活跃的分配数（任务量）
                long activeAssignments = allAssignments.stream()
                        .filter(a -> {
                            ProjectVersion v = versionMap.get(a.getProjectVersionId());
                            if (v == null || v.getStartTime() == null || v.getEndTime() == null) return false;
                            return !v.getEndTime().toLocalDate().isBefore(firstDay)
                                    && !v.getStartTime().toLocalDate().isAfter(lastDay);
                        })
                        .count();
                monthStat.put("activeAssignments", activeAssignments);

                // 统计该月活跃项目数
                long activeProjects = allProjects.stream()
                        .filter(p -> {
                            if (p.getStartTime() == null || p.getEndTime() == null) return false;
                            return !p.getEndTime().toLocalDate().isBefore(firstDay)
                                    && !p.getStartTime().toLocalDate().isAfter(lastDay);
                        })
                        .count();
                monthStat.put("activeProjects", activeProjects);

                // 统计该月参与人数
                Set<Long> activeUsers = allAssignments.stream()
                        .filter(a -> {
                            ProjectVersion v = versionMap.get(a.getProjectVersionId());
                            if (v == null || v.getStartTime() == null || v.getEndTime() == null) return false;
                            return !v.getEndTime().toLocalDate().isBefore(firstDay)
                                    && !v.getStartTime().toLocalDate().isAfter(lastDay);
                        })
                        .map(ProjectAssignment::getUserId)
                        .collect(Collectors.toSet());
                monthStat.put("activeUsers", activeUsers.size());

                monthlyData.add(monthStat);
            }

            result.put("code", 200);
            result.put("data", monthlyData);
            result.put("message", "查询成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 仪表盘增强数据
     * 提供图表所需的汇总数据
     */
    @GetMapping("/dashboard-charts")
    public Map<String, Object> getDashboardCharts() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Project> allProjects = projectService.list();
            List<ProjectVersion> allVersions = projectVersionService.list();
            List<ProjectAssignment> allAssignments = projectAssignmentService.list();
            List<User> allUsers = userService.getActiveUsers();

            LocalDateTime now = LocalDateTime.now();
            Map<Long, ProjectVersion> versionMap = allVersions.stream()
                    .collect(Collectors.toMap(ProjectVersion::getId, v -> v, (a, b) -> a));

            // 1. 项目状态分布
            Map<String, Object> projectStatusDist = new HashMap<>();
            long notStarted = allProjects.stream().filter(p -> {
                projectService.calculateProjectStatusFromVersions(p);
                return p.getStatus() != null && p.getStatus() == 0;
            }).count();
            long ongoing = allProjects.stream().filter(p -> {
                projectService.calculateProjectStatusFromVersions(p);
                return p.getStatus() != null && p.getStatus() == 1;
            }).count();
            long finished = allProjects.stream().filter(p -> {
                projectService.calculateProjectStatusFromVersions(p);
                return p.getStatus() != null && p.getStatus() == 2;
            }).count();
            projectStatusDist.put("notStarted", notStarted);
            projectStatusDist.put("ongoing", ongoing);
            projectStatusDist.put("finished", finished);

            // 2. 人员负载分布（用于饼图）
            Map<Long, List<ProjectAssignment>> assignByUser = allAssignments.stream()
                    .collect(Collectors.groupingBy(ProjectAssignment::getUserId));
            int idle = 0, light = 0, normal = 0, busy = 0;
            for (User user : allUsers) {
                int count = 0;
                for (ProjectAssignment a : assignByUser.getOrDefault(user.getId(), new ArrayList<>())) {
                    ProjectVersion v = versionMap.get(a.getProjectVersionId());
                    if (v != null && v.getStartTime() != null && v.getEndTime() != null) {
                        LocalDateTime endOfDay = v.getEndTime().toLocalDate().atTime(23, 59, 59);
                        if (!now.isBefore(v.getStartTime()) && !now.isAfter(endOfDay)) {
                            count++;
                        }
                    }
                }
                if (count == 0) idle++;
                else if (count <= 1) light++;
                else if (count <= 3) normal++;
                else busy++;
            }
            Map<String, Object> workloadDist = new HashMap<>();
            workloadDist.put("idle", idle);
            workloadDist.put("light", light);
            workloadDist.put("normal", normal);
            workloadDist.put("busy", busy);

            // 3. 最近30天到期版本趋势（用于折线图）
            List<Map<String, Object>> deadlineTrend = new ArrayList<>();
            for (int i = 29; i >= 0; i--) {
                LocalDate date = now.toLocalDate().minusDays(i);
                long count = allVersions.stream()
                        .filter(v -> v.getEndTime() != null && v.getEndTime().toLocalDate().equals(date))
                        .count();
                Map<String, Object> point = new HashMap<>();
                point.put("date", date.toString());
                point.put("count", count);
                deadlineTrend.add(point);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("projectStatusDist", projectStatusDist);
            data.put("workloadDist", workloadDist);
            data.put("deadlineTrend", deadlineTrend);

            result.put("code", 200);
            result.put("data", data);
            result.put("message", "查询成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }
}
