package com.teamdev.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("project_versions")
public class ProjectVersion {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    @TableField("version_name")
    private String versionName;

    @TableField("description")
    private String description;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("test_time")
    private LocalDateTime testTime;

    @TableField("launch_time")
    private LocalDateTime launchTime;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    @TableField("product_manager")
    private String productManager;

    @TableField("prototype_url")
    private String prototypeUrl;

    @TableField(exist = false)
    private Integer status; // 0:未开始, 1:进行中, 2:已结束

    @TableField("bug_count")
    private Integer bugCount;

    @TableField("bug_resolved_list")
    private String bugResolvedList;

    @TableField("bug_activated_groups")
    private String bugActivatedGroups;

    @TableField("is_delayed")
    private Boolean isDelayed;

    @TableField("delayed_end_time")
    private LocalDateTime delayedEndTime;

    @TableField(exist = false)
    private Project project;
}
