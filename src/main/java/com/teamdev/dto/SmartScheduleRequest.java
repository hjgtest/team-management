package com.teamdev.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 智能排期请求
 */
public class SmartScheduleRequest {
    /** 项目ID */
    private Long projectId;
    /** 版本号 */
    private String versionName;
    /** 产品经理 */
    private String productManager;
    /** 原型地址 */
    private String prototypeUrl;
    /** 版本开始时间 */
    private LocalDateTime startTime;
    /** 版本结束时间 */
    private LocalDateTime endTime;
    /** 提测时间 */
    private LocalDateTime testTime;
    /** 上线时间 */
    private LocalDateTime launchTime;
    /** 可选的小组ID列表，为空则表示不限制小组 */
    private List<Long> teamIds;

    /** 需要分配的角色和功能列表 */
    private List<AssignmentItem> assignments;

    public static class AssignmentItem {
        /** 角色ID */
        private String roleId;
        /** 负责功能描述 */
        private String feature;

        public String getRoleId() {
            return roleId;
        }

        public void setRoleId(String roleId) {
            this.roleId = roleId;
        }

        public String getFeature() {
            return feature;
        }

        public void setFeature(String feature) {
            this.feature = feature;
        }
    }

    public List<Long> getTeamIds() {
        return teamIds;
    }

    public void setTeamIds(List<Long> teamIds) {
        this.teamIds = teamIds;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getProductManager() {
        return productManager;
    }

    public void setProductManager(String productManager) {
        this.productManager = productManager;
    }

    public String getPrototypeUrl() {
        return prototypeUrl;
    }

    public void setPrototypeUrl(String prototypeUrl) {
        this.prototypeUrl = prototypeUrl;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getTestTime() {
        return testTime;
    }

    public void setTestTime(LocalDateTime testTime) {
        this.testTime = testTime;
    }

    public LocalDateTime getLaunchTime() {
        return launchTime;
    }

    public void setLaunchTime(LocalDateTime launchTime) {
        this.launchTime = launchTime;
    }

    public List<AssignmentItem> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<AssignmentItem> assignments) {
        this.assignments = assignments;
    }
}
