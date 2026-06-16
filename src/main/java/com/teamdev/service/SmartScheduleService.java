package com.teamdev.service;

import com.teamdev.dto.SmartScheduleRequest;
import com.teamdev.dto.SmartScheduleResult;
import com.teamdev.entity.ProjectAssignment;
import com.teamdev.entity.ProjectVersion;
import com.teamdev.entity.Role;
import com.teamdev.entity.User;
import com.teamdev.mapper.ProjectAssignmentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 智能排期服务
 * 根据项目和人员情况自动进行人员分配
 *
 * 排期优先级：
 * 1. 做过该项目 且 手头任务 < 4 → 第一优先
 * 2. 空闲的人（手头任务 = 0）→ 第二优先
 * 3. 手头任务 < 4 但没做过该项目 → 第三优先
 *
 * 时间自动计算：根据分配人员的当前任务情况，自动安排合适的开始和结束时间
 */
@Service
public class SmartScheduleService {

    @Autowired
    private ProjectVersionService projectVersionService;

    @Autowired
    private ProjectAssignmentService projectAssignmentService;

    @Autowired
    private ProjectAssignmentMapper projectAssignmentMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private RoleService roleService;

    /**
     * 用户扩展信息（用于排序和筛选）
     */
    private static class UserInfo {
        Long userId;
        String username;
        Integer roleId;
        Integer teamId;
        int ongoingCount;
        boolean hasHistory;
        LocalDateTime availableTime; // 该用户可以开始新任务的最早时间
    }

    /**
     * 候选人信息
     */
    private static class UserProfile {
        User user;
        int ongoingCount;
        boolean hasHistory;
        String priorityDesc;
    }

    /**
     * 执行智能排期
     */
    @Transactional
    public SmartScheduleResult schedule(SmartScheduleRequest request) {
        SmartScheduleResult result = new SmartScheduleResult();
        LocalDateTime now = LocalDateTime.now();

        // 1. 加载所有用户、版本、分配数据（只查在职且正常研发成员）
        List<User> allUsers = userService.getAssignableUsers();
        List<ProjectVersion> allVersions = projectVersionService.list();
        List<ProjectAssignment> allAssignments = projectAssignmentService.list();

        // 构建版本映射
        Map<Long, ProjectVersion> versionMap = new HashMap<>();
        for (ProjectVersion v : allVersions) {
            versionMap.put(v.getId(), v);
        }

        // 按用户分组分配
        Map<Long, List<ProjectAssignment>> assignByUser = new HashMap<>();
        for (ProjectAssignment a : allAssignments) {
            assignByUser.computeIfAbsent(a.getUserId(), k -> new ArrayList<>()).add(a);
        }

        // 2. 构建用户工作负载信息
        Map<Long, UserInfo> userInfoMap = new HashMap<>();
        for (User user : allUsers) {
            UserInfo info = new UserInfo();
            info.userId = user.getId();
            info.username = user.getUsername();
            info.roleId = user.getRoleId();
            info.teamId = user.getTeamId();

            List<ProjectAssignment> userAssigns = assignByUser.getOrDefault(user.getId(), new ArrayList<>());
            int ongoingCount = 0;
            boolean hasHistory = false;
            LocalDateTime latestEndTime = null;

            for (ProjectAssignment a : userAssigns) {
                ProjectVersion v = versionMap.get(a.getProjectVersionId());
                if (v == null) continue;

                // 计算进行中任务数
                if (v.getEndTime() != null && v.getStartTime() != null) {
                    LocalDateTime endOfDay = v.getEndTime().toLocalDate().atTime(23, 59, 59);
                    boolean isOngoing = !now.isBefore(v.getStartTime()) && !now.isAfter(endOfDay);
                    if (isOngoing) {
                        ongoingCount++;
                    }
                }

                // 检查是否做过当前项目
                if (request.getProjectId().equals(v.getProjectId())) {
                    hasHistory = true;
                }

                // 记录最晚结束时间
                if (v.getEndTime() != null) {
                    if (latestEndTime == null || v.getEndTime().isAfter(latestEndTime)) {
                        latestEndTime = v.getEndTime();
                    }
                }
            }

            info.ongoingCount = ongoingCount;
            info.hasHistory = hasHistory;
            // 可用时间 = 最晚任务结束时间+1天，如果早于现在则从现在开始
            info.availableTime = (latestEndTime != null && latestEndTime.isAfter(now))
                    ? latestEndTime.plusDays(1)
                    : now;

            userInfoMap.put(user.getId(), info);
        }

        // 3. 为每个分配项智能推荐人员
        List<SmartScheduleResult.ScheduleDetail> details = new ArrayList<>();
        Set<Long> assignedUserIds = new HashSet<>();
        List<LocalDateTime> assignedAvailableTimes = new ArrayList<>();

        for (SmartScheduleRequest.AssignmentItem item : request.getAssignments()) {
            SmartScheduleResult.ScheduleDetail detail = new SmartScheduleResult.ScheduleDetail();
            detail.setRoleId(item.getRoleId());
            detail.setFeature(item.getFeature());

            // 获取角色名称
            try {
                Long roleIdLong = Long.valueOf(item.getRoleId());
                Role role = roleService.getById(roleIdLong);
                if (role != null) {
                    detail.setRoleName(role.getRoleName());
                }
            } catch (NumberFormatException e) {
                // ignore
            }

            // 智能推荐人员
            UserProfile bestCandidate = findBestCandidate(
                    item.getRoleId(),
                    request.getProjectId(),
                    request.getTeamIds(),
                    userInfoMap,
                    assignedUserIds,
                    versionMap,
                    assignByUser,
                    now
            );

            if (bestCandidate != null) {
                detail.setUserId(bestCandidate.user.getId());
                detail.setUsername(bestCandidate.user.getUsername());
                detail.setOngoingCount(bestCandidate.ongoingCount);
                detail.setHasHistory(bestCandidate.hasHistory);
                detail.setPriorityDesc(bestCandidate.priorityDesc);

                // 记录该人员的可用时间
                UserInfo ui = userInfoMap.get(bestCandidate.user.getId());
                if (ui != null) {
                    assignedAvailableTimes.add(ui.availableTime);
                }

                assignedUserIds.add(bestCandidate.user.getId());

                // 更新该用户的ongoingCount（因为分配了新任务）
                if (ui != null) {
                    ui.ongoingCount++;
                }
            } else {
                detail.setPriorityDesc("未找到合适人员（所有人员任务均已饱和）");
            }

            details.add(detail);
        }

        // 4. 自动计算版本时间
        LocalDateTime startTime;
        if (assignedAvailableTimes.isEmpty()) {
            startTime = now;
        } else {
            // 开始时间 = 被分配人员中最晚的可用时间
            startTime = assignedAvailableTimes.stream()
                    .max(LocalDateTime::compareTo)
                    .orElse(now);
            // 如果最晚可用时间早于现在，从现在开始
            if (startTime.isBefore(now)) {
                startTime = now;
            }
        }
        // 默认版本周期为14天
        LocalDateTime endTime = startTime.plusDays(14);

        // 5. 创建项目版本
        ProjectVersion version = new ProjectVersion();
        version.setProjectId(request.getProjectId());
        version.setVersionName(request.getVersionName());
        version.setProductManager(request.getProductManager());
        version.setPrototypeUrl(request.getPrototypeUrl());
        version.setStartTime(startTime);
        version.setEndTime(endTime);
        version.setTestTime(endTime.minusDays(3));
        version.setLaunchTime(endTime);

        boolean saved = projectVersionService.saveProjectVersion(version);
        if (!saved) {
            result.setSuccess(false);
            result.setMessage("创建项目版本失败");
            return result;
        }

        result.setVersionId(version.getId());
        result.setStartTime(startTime);
        result.setEndTime(endTime);

        // 6. 创建分配记录
        for (SmartScheduleResult.ScheduleDetail detail : details) {
            if (detail.getUserId() != null) {
                ProjectAssignment assignment = new ProjectAssignment();
                assignment.setProjectVersionId(version.getId());
                assignment.setUserId(detail.getUserId());
                assignment.setRoleId(detail.getRoleId());
                assignment.setFeature(detail.getFeature());
                projectAssignmentService.saveAssignment(assignment);
            }
        }

        result.setSuccess(true);
        result.setMessage("智能排期完成");
        result.setDetails(details);

        return result;
    }

    /**
     * 智能推荐：按优先级查找最合适的人员
     *
     * 优先级：
     * 1. 做过该项目 且 手头任务 < 4
     * 2. 空闲的人（手头任务 = 0）
     * 3. 手头任务 < 4 但没做过该项目
     */
    private UserProfile findBestCandidate(String targetRoleId,
                                         Long projectId,
                                         List<Long> teamIds,
                                         Map<Long, UserInfo> userInfoMap,
                                         Set<Long> assignedUserIds,
                                         Map<Long, ProjectVersion> versionMap,
                                         Map<Long, List<ProjectAssignment>> assignByUser,
                                         LocalDateTime now) {
        List<UserProfile> candidates = new ArrayList<>();

        for (Map.Entry<Long, UserInfo> entry : userInfoMap.entrySet()) {
            Long userId = entry.getKey();
            UserInfo info = entry.getValue();

            // 跳过已分配的用户
            if (assignedUserIds.contains(userId)) {
                continue;
            }

            // 按小组筛选（如果指定了小组）
            if (teamIds != null && !teamIds.isEmpty() && info.teamId != null) {
                if (!teamIds.contains(Long.valueOf(info.teamId))) {
                    continue;
                }
            }

            User user = new User();
            user.setId(info.userId);
            user.setUsername(info.username);
            user.setRoleId(info.roleId);

            UserProfile profile = new UserProfile();
            profile.user = user;
            profile.ongoingCount = info.ongoingCount;
            profile.hasHistory = info.hasHistory;
            profile.priorityDesc = "";

            candidates.add(profile);
        }

        // 按优先级排序
        candidates.sort((a, b) -> {
            // 优先级1：做过该项目 & 任务<4
            boolean aP1 = a.hasHistory && a.ongoingCount < 4;
            boolean bP1 = b.hasHistory && b.ongoingCount < 4;
            if (aP1 != bP1) return aP1 ? -1 : 1;

            // 优先级2：空闲的人（任务=0）
            boolean aP2 = a.ongoingCount == 0;
            boolean bP2 = b.ongoingCount == 0;
            if (aP2 != bP2) return aP2 ? -1 : 1;

            // 同优先级内，按任务数升序
            if (a.ongoingCount != b.ongoingCount) {
                return Integer.compare(a.ongoingCount, b.ongoingCount);
            }

            // 角色匹配度优先（如果指定了角色）
            if (targetRoleId != null) {
                Integer aRoleId = a.user.getRoleId();
                Integer bRoleId = b.user.getRoleId();
                boolean aRoleMatch = aRoleId != null && targetRoleId.equals(String.valueOf(aRoleId));
                boolean bRoleMatch = bRoleId != null && targetRoleId.equals(String.valueOf(bRoleId));
                if (aRoleMatch != bRoleMatch) return aRoleMatch ? -1 : 1;
            }

            return 0;
        });

        // 设置优先级描述
        for (UserProfile p : candidates) {
            if (p.hasHistory && p.ongoingCount < 4) {
                p.priorityDesc = "第一优先：做过本项目且任务数<4（当前" + p.ongoingCount + "件）";
            } else if (p.ongoingCount == 0) {
                p.priorityDesc = "第二优先：空闲人员（当前0件任务）";
            } else if (p.ongoingCount < 4) {
                p.priorityDesc = "第三优先：任务数<4且未做过本项目（当前" + p.ongoingCount + "件）";
            } else {
                p.priorityDesc = "任务已满（当前" + p.ongoingCount + "件）";
            }
        }

        // 返回第一个可用的候选人（任务数<4）
        for (UserProfile p : candidates) {
            if (p.ongoingCount < 4) {
                return p;
            }
        }

        // 如果所有人任务都>=4，返回任务最少的那个
        if (!candidates.isEmpty()) {
            return candidates.get(0);
        }

        return null;
    }
}
