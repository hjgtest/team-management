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

    /**
     * 获取月度Bug统计报表
     */
    public Map<String, Object> getMonthlyReport(Integer year, Integer month, Long projectId, String roleId) {
        // 1. 查询该月所有版本
        List<ProjectVersion> versions = queryVersionsInMonth(year, month, projectId);

        // 2. 收集所有涉及的用户ID
        Set<Long> userIds = new HashSet<>();
        for (ProjectVersion version : versions) {
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

            statsMap.put(userId, stats);
        }

        // 5. 遍历版本，累加统计数据
        Set<Long> processedVersions = new HashSet<>();
        for (ProjectVersion version : versions) {
            if (version.getBugResolvedList() == null || version.getBugResolvedList().isEmpty()) {
                continue;
            }

            // 计算工作天数
            long workDays = calculateWorkDays(version);
            if (workDays <= 0) continue;

            // 解析Bug数据
            JSONArray bugList = JSON.parseArray(version.getBugResolvedList());
            for (int i = 0; i < bugList.size(); i++) {
                JSONObject bugItem = bugList.getJSONObject(i);
                Long userId = bugItem.getLong("userId");
                if (userId == null || !statsMap.containsKey(userId)) continue;

                UserBugStats stats = statsMap.get(userId);

                // 累加项目和版本数（去重）
                if (!processedVersions.contains(version.getId())) {
                    stats.setProjectCount(stats.getProjectCount() + 1);
                    stats.setVersionCount(stats.getVersionCount() + 1);
                    stats.setWorkDays(stats.getWorkDays() + (int) workDays);
                }

                // 累加功能模块数
                int featureCount = countFeaturesForUser(version.getId(), userId);
                stats.setFeatureCount(stats.getFeatureCount() + featureCount);

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

            processedVersions.add(version.getId());
        }

        // 6. 计算Bug密度和质量分
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

        // 7. 按质量分排序
        List<UserBugStats> rankings = statsMap.values().stream()
                .sorted((a, b) -> Double.compare(b.getQualityScore(), a.getQualityScore()))
                .collect(Collectors.toList());

        // 8. 添加排名
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRank(i + 1);
        }

        // 9. 构建返回结果
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
     * 查询指定月份的所有版本
     */
    private List<ProjectVersion> queryVersionsInMonth(Integer year, Integer month, Long projectId) {
        QueryWrapper<ProjectVersion> wrapper = new QueryWrapper<>();
        
        // 构造月份时间范围
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
        LocalDateTime startOfMonth = firstDay.atStartOfDay();
        LocalDateTime endOfMonth = lastDay.atTime(23, 59, 59);

        wrapper.between("start_time", startOfMonth, endOfMonth)
               .or()
               .between("end_time", startOfMonth, endOfMonth)
               .or()
               .le("start_time", startOfMonth)
               .ge("end_time", endOfMonth);

        if (projectId != null) {
            wrapper.eq("project_id", projectId);
        }

        return projectVersionMapper.selectList(wrapper);
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
