package com.teamdev.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.teamdev.entity.ProjectAssignment;
import com.teamdev.entity.ProjectVersion;
import com.teamdev.entity.User;
import com.teamdev.mapper.ProjectAssignmentMapper;
import com.teamdev.mapper.ProjectVersionMapper;
import com.teamdev.mapper.UserMapper;
import com.teamdev.service.BlacklistEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BugStatsService {

    @Autowired
    private ProjectVersionMapper projectVersionMapper;

    @Autowired
    private ProjectAssignmentMapper projectAssignmentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BlacklistEventService blacklistEventService;

    /**
     * 获取月度Bug统计报表
     */
    public Map<String, Object> getMonthlyReport(Integer year, Integer month, Long projectId, String roleId) {
        // 1. 查询该月所有版本
        List<ProjectVersion> versions = queryVersionsInMonth(year, month, projectId);

        // 2. 收集所有涉及的用户ID（包括有Bug的和无Bug的）
        Set<Long> userIds = new HashSet<>();
        for (ProjectVersion version : versions) {
            // 从Bug记录中收集用户
            if (version.getBugResolvedList() != null && !version.getBugResolvedList().isEmpty()) {
                JSONArray bugList = JSON.parseArray(version.getBugResolvedList());
                for (int i = 0; i < bugList.size(); i++) {
                    JSONObject bugItem = bugList.getJSONObject(i);
                    Long userId = bugItem.getLong("userId");
                    if (userId != null) {
                        userIds.add(userId);
                    }
                }
            }
            
            // 从分配表中收集所有参与人员（关键修复：确保无Bug人员也被统计）
            QueryWrapper<ProjectAssignment> assignmentWrapper = new QueryWrapper<>();
            assignmentWrapper.eq("project_version_id", version.getId());
            List<ProjectAssignment> assignments = projectAssignmentMapper.selectList(assignmentWrapper);
            for (ProjectAssignment assignment : assignments) {
                if (assignment.getUserId() != null) {
                    userIds.add(assignment.getUserId());
                }
            }
        }

        // 3. 查询用户信息
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        }

        // 4. 统计每个用户的Bug数据
        Map<Long, UserBugStats> statsMap = new HashMap<>();
        for (Long userId : userIds) {
            User user = userMap.get(userId);
            if (user == null) continue;

            UserBugStats stats = new UserBugStats();
            stats.setUserId(userId);
            stats.setUsername(user.getUsername());
            stats.setProjectCount(0);
            stats.setVersionCount(0);
            stats.setWorkDays(0);
            stats.setFeatureCount(0);
            stats.setBugLevels(new ArrayList<>());
            stats.setTotalBugs(0);
            stats.setBugDensity(0.0);
            stats.setQualityScore(100.0);
            stats.setBlacklistCount(0);
            stats.setBlacklistDensity(0.0);
            stats.setBlacklistAdjustment(0.0);
            stats.setFinalScore(100.0);

            statsMap.put(userId, stats);
        }

        // 5. 批量加载所有版本的分配记录（避免 N+1 查询）
        List<Long> versionIds = versions.stream()
                .map(ProjectVersion::getId)
                .collect(Collectors.toList());
        
        Map<Long, List<ProjectAssignment>> assignmentsByVersion = new HashMap<>();
        if (!versionIds.isEmpty()) {
            QueryWrapper<ProjectAssignment> allAssignWrapper = new QueryWrapper<>();
            allAssignWrapper.in("project_version_id", versionIds);
            List<ProjectAssignment> allAssignments = projectAssignmentMapper.selectList(allAssignWrapper);
            for (ProjectAssignment a : allAssignments) {
                assignmentsByVersion.computeIfAbsent(a.getProjectVersionId(), k -> new ArrayList<>()).add(a);
            }
        }

        // 6. 遍历版本，累加统计数据（使用预加载的分配数据）
        Set<Long> processedVersions = new HashSet<>();
        for (ProjectVersion version : versions) {
            // 计算工作天数
            long workDays = calculateWorkDays(version);
            if (workDays <= 0) continue;

            // 从预加载的数据中获取分配记录
            List<ProjectAssignment> assignments = assignmentsByVersion.getOrDefault(version.getId(), new ArrayList<>());
            
            for (ProjectAssignment assignment : assignments) {
                Long userId = assignment.getUserId();
                if (userId == null || !statsMap.containsKey(userId)) continue;

                UserBugStats stats = statsMap.get(userId);

                // 累加项目和版本数（去重）
                if (!processedVersions.contains(version.getId())) {
                    stats.setProjectCount(stats.getProjectCount() + 1);
                    stats.setVersionCount(stats.getVersionCount() + 1);
                    stats.setWorkDays(stats.getWorkDays() + (int) workDays);
                }

                // 从预加载的分配数据中统计功能模块数（避免额外查询）
                String feature = assignment.getFeature();
                if (feature != null && !feature.trim().isEmpty()) {
                    stats.setFeatureCount(stats.getFeatureCount() + 1);
                }
            }

            // 再解析Bug数据，统计Bug数量
            if (version.getBugResolvedList() != null && !version.getBugResolvedList().isEmpty()) {
                JSONArray bugList = JSON.parseArray(version.getBugResolvedList());
                for (int i = 0; i < bugList.size(); i++) {
                    JSONObject bugItem = bugList.getJSONObject(i);
                    Long userId = bugItem.getLong("userId");
                    if (userId == null || !statsMap.containsKey(userId)) continue;

                    UserBugStats stats = statsMap.get(userId);

                    // 累加各级别Bug数
                    JSONArray levels = bugItem.getJSONArray("levels");
                    if (levels != null) {
                        for (int j = 0; j < levels.size(); j++) {
                            JSONObject level = levels.getJSONObject(j);
                            String levelName = level.getString("name");
                            Integer count = level.getInteger("count");
                            if (count != null && count > 0) {
                                addOrUpdateBugLevel(stats, levelName, count);
                                stats.setTotalBugs(stats.getTotalBugs() + count);
                            }
                        }
                    }
                }
            }

            processedVersions.add(version.getId());
        }

        // 7. 计算Bug密度和质量分
        for (UserBugStats stats : statsMap.values()) {
            if (stats.getWorkDays() > 0) {
                // Bug密度 = 总Bug数 / 工作天数
                double density = (double) stats.getTotalBugs() / stats.getWorkDays();
                stats.setBugDensity(Math.round(density * 100.0) / 100.0);

                // 质量分 = 100 - (Bug密度 × 10)，最低0分
                double score = 100 - (density * 10);
                stats.setQualityScore(Math.max(0, Math.round(score * 10.0) / 10.0));
            }
        }

        // 8. 计算黑榜密度和调整分
        calculateBlacklistAdjustment(statsMap, year, month);

        // 9. 按最终得分排序
        List<UserBugStats> rankings = statsMap.values().stream()
                .sorted((a, b) -> Double.compare(b.getFinalScore(), a.getFinalScore()))
                .collect(Collectors.toList());

        // 10. 添加排名
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRank(i + 1);
        }

        // 11. 构建返回结果
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalUsers", rankings.size());
        summary.put("avgBugDensity", rankings.isEmpty() ? 0 : 
            rankings.stream().mapToDouble(UserBugStats::getBugDensity).average().orElse(0));
        if (!rankings.isEmpty()) {
            summary.put("bestPerformer", rankings.get(0).getUsername());
        }
        result.put("summary", summary);
        result.put("rankings", rankings);

        return result;
    }

    /**
     * 查询指定月份的所有版本（用于调试）
     */
    private List<ProjectVersion> queryVersionsInMonth(Integer year, Integer month, Long projectId) {
        QueryWrapper<ProjectVersion> wrapper = new QueryWrapper<>();
        
        // 构造月份时间范围
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
        LocalDateTime startOfMonth = firstDay.atStartOfDay();
        LocalDateTime endOfMonth = lastDay.atTime(23, 59, 59);

        System.out.println("========== Bug统计查询参数 ==========");
        System.out.println("查询月份: " + year + "-" + month);
        System.out.println("时间范围: " + startOfMonth + " ~ " + endOfMonth);
        
        // 查询在该月有上线时间或研发结束时间的版本
        // 方式1: launch_time在月份内
        // 方式2: launch_time为空且end_time在月份内
        // 使用SQL拼接避免MyBatis-Plus嵌套条件生成问题
        wrapper.apply(
            "(launch_time BETWEEN {0} AND {1} OR (launch_time IS NULL AND end_time BETWEEN {0} AND {1}))",
            startOfMonth, endOfMonth
        );

        if (projectId != null) {
            wrapper.eq("project_id", projectId);
        }

        List<ProjectVersion> versions = projectVersionMapper.selectList(wrapper);
        System.out.println("查询到版本数: " + versions.size());
        for (ProjectVersion v : versions) {
            System.out.println("版本ID=" + v.getId() + ", 名称=" + v.getVersionName() + ", status=" + v.getStatus() + ", launch_time=" + v.getLaunchTime() + ", end_time=" + v.getEndTime());
        }
        System.out.println("=====================================");
        
        return versions;
    }

    /**
     * 计算版本工作天数
     */
    private long calculateWorkDays(ProjectVersion version) {
        if (version.getStartTime() == null || version.getEndTime() == null) {
            return 0;
        }
        LocalDate startDate = version.getStartTime().toLocalDate();
        LocalDate endDate = version.getEndTime().toLocalDate();
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /**
     * 统计用户在某个版本中负责的功能模块数
     */
    private int countFeaturesForUser(Long versionId, Long userId) {
        QueryWrapper<ProjectAssignment> wrapper = new QueryWrapper<>();
        wrapper.eq("project_version_id", versionId)
               .eq("user_id", userId)
               .isNotNull("feature")
               .ne("feature", "");
        Long count = projectAssignmentMapper.selectCount(wrapper);
        return count != null ? count.intValue() : 0;
    }

    /**
     * 添加或更新Bug级别统计
     */
    private void addOrUpdateBugLevel(UserBugStats stats, String levelName, Integer count) {
        boolean found = false;
        for (BugLevelInfo info : stats.getBugLevels()) {
            if (info.getLevelName().equals(levelName)) {
                info.setCount(info.getCount() + count);
                found = true;
                break;
            }
        }
        if (!found) {
            BugLevelInfo info = new BugLevelInfo();
            info.setLevelName(levelName);
            info.setCount(count);
            stats.getBugLevels().add(info);
        }
    }

    /**
     * 计算黑榜密度和调整分
     */
    private void calculateBlacklistAdjustment(Map<Long, UserBugStats> statsMap, Integer year, Integer month) {
        if (statsMap.isEmpty()) return;

        // 统计总功能数
        int totalFeatures = statsMap.values().stream()
                .mapToInt(UserBugStats::getFeatureCount)
                .sum();
        
        if (totalFeatures == 0) return;

        // 统计总黑榜事件数
        int totalBlacklistEvents = 0;
        Map<Long, Integer> userBlacklistCount = new HashMap<>();
        
        for (Long userId : statsMap.keySet()) {
            int eventCount = blacklistEventService.getUserEventCount(userId, year, month);
            userBlacklistCount.put(userId, eventCount);
            totalBlacklistEvents += eventCount;
        }

        // 计算团队平均黑榜密度
        double avgBlacklistDensity = (double) totalBlacklistEvents / totalFeatures;

        // 计算每个用户的黑榜调整分
        for (Map.Entry<Long, UserBugStats> entry : statsMap.entrySet()) {
            Long userId = entry.getKey();
            UserBugStats stats = entry.getValue();
            
            if (stats.getFeatureCount() > 0) {
                // 个人黑榜密度
                double personalDensity = (double) userBlacklistCount.get(userId) / stats.getFeatureCount();
                
                // 黑榜调整分 = (平均密度 - 个人密度) × 20
                // 个人密度越低越好，低于平均值则加分，高于平均值则扣分
                double adjustment = (avgBlacklistDensity - personalDensity) * 20;
                
                stats.setBlacklistCount(userBlacklistCount.get(userId));
                stats.setBlacklistDensity(Math.round(personalDensity * 1000.0) / 1000.0);
                stats.setBlacklistAdjustment(Math.round(adjustment * 10.0) / 10.0);
                
                // 最终得分 = Bug质量分 + 黑榜调整分
                double finalScore = stats.getQualityScore() + adjustment;
                stats.setFinalScore(Math.max(0, Math.round(finalScore * 10.0) / 10.0));
            } else {
                stats.setBlacklistCount(0);
                stats.setBlacklistDensity(0.0);
                stats.setBlacklistAdjustment(0.0);
                stats.setFinalScore(stats.getQualityScore());
            }
        }
    }

    /**
     * 用户Bug统计数据
     */
    public static class UserBugStats {
        private Integer rank;
        private Long userId;
        private String username;
        private Integer projectCount;
        private Integer versionCount;
        private Integer workDays;
        private Integer featureCount;
        private List<BugLevelInfo> bugLevels;
        private Integer totalBugs;
        private Double bugDensity;
        private Double qualityScore;
        private Integer blacklistCount;
        private Double blacklistDensity;
        private Double blacklistAdjustment;
        private Double finalScore;

        // Getters and Setters
        public Integer getRank() { return rank; }
        public void setRank(Integer rank) { this.rank = rank; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public Integer getProjectCount() { return projectCount; }
        public void setProjectCount(Integer projectCount) { this.projectCount = projectCount; }
        public Integer getVersionCount() { return versionCount; }
        public void setVersionCount(Integer versionCount) { this.versionCount = versionCount; }
        public Integer getWorkDays() { return workDays; }
        public void setWorkDays(Integer workDays) { this.workDays = workDays; }
        public Integer getFeatureCount() { return featureCount; }
        public void setFeatureCount(Integer featureCount) { this.featureCount = featureCount; }
        public List<BugLevelInfo> getBugLevels() { return bugLevels; }
        public void setBugLevels(List<BugLevelInfo> bugLevels) { this.bugLevels = bugLevels; }
        public Integer getTotalBugs() { return totalBugs; }
        public void setTotalBugs(Integer totalBugs) { this.totalBugs = totalBugs; }
        public Double getBugDensity() { return bugDensity; }
        public void setBugDensity(Double bugDensity) { this.bugDensity = bugDensity; }
        public Double getQualityScore() { return qualityScore; }
        public void setQualityScore(Double qualityScore) { this.qualityScore = qualityScore; }
        public Integer getBlacklistCount() { return blacklistCount; }
        public void setBlacklistCount(Integer blacklistCount) { this.blacklistCount = blacklistCount; }
        public Double getBlacklistDensity() { return blacklistDensity; }
        public void setBlacklistDensity(Double blacklistDensity) { this.blacklistDensity = blacklistDensity; }
        public Double getBlacklistAdjustment() { return blacklistAdjustment; }
        public void setBlacklistAdjustment(Double blacklistAdjustment) { this.blacklistAdjustment = blacklistAdjustment; }
        public Double getFinalScore() { return finalScore; }
        public void setFinalScore(Double finalScore) { this.finalScore = finalScore; }
    }

    /**
     * Bug级别信息
     */
    public static class BugLevelInfo {
        private String levelName;
        private Integer count;

        // Getters and Setters
        public String getLevelName() { return levelName; }
        public void setLevelName(String levelName) { this.levelName = levelName; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
    }
}
