package com.teamdev.dto;

/**
 * 智能排期结果
 */
public class SmartScheduleResult {
    /** 是否成功 */
    private boolean success;
    /** 消息 */
    private String message;
    /** 创建的版本ID */
    private Long versionId;
    /** 排期详情列表 */
    private java.util.List<ScheduleDetail> details;
    /** 自动计算的版本开始时间 */
    private java.time.LocalDateTime startTime;
    /** 自动计算的版本结束时间 */
    private java.time.LocalDateTime endTime;

    public static class ScheduleDetail {
        /** 角色ID */
        private String roleId;
        /** 角色名称 */
        private String roleName;
        /** 负责功能 */
        private String feature;
        /** 分配的用户ID */
        private Long userId;
        /** 分配的用户名 */
        private String username;
        /** 排期优先级说明 */
        private String priorityDesc;
        /** 该用户当前进行中任务数 */
        private int ongoingCount;
        /** 是否做过该项目 */
        private boolean hasHistory;

        public String getRoleId() {
            return roleId;
        }

        public void setRoleId(String roleId) {
            this.roleId = roleId;
        }

        public String getRoleName() {
            return roleName;
        }

        public void setRoleName(String roleName) {
            this.roleName = roleName;
        }

        public String getFeature() {
            return feature;
        }

        public void setFeature(String feature) {
            this.feature = feature;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPriorityDesc() {
            return priorityDesc;
        }

        public void setPriorityDesc(String priorityDesc) {
            this.priorityDesc = priorityDesc;
        }

        public int getOngoingCount() {
            return ongoingCount;
        }

        public void setOngoingCount(int ongoingCount) {
            this.ongoingCount = ongoingCount;
        }

        public boolean isHasHistory() {
            return hasHistory;
        }

        public void setHasHistory(boolean hasHistory) {
            this.hasHistory = hasHistory;
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getVersionId() {
        return versionId;
    }

    public void setVersionId(Long versionId) {
        this.versionId = versionId;
    }

    public java.util.List<ScheduleDetail> getDetails() {
        return details;
    }

    public void setDetails(java.util.List<ScheduleDetail> details) {
        this.details = details;
    }

    public java.time.LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(java.time.LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public java.time.LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(java.time.LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
