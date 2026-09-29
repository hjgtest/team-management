package com.teamdev.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知实体
 */
@Data
@TableName("notifications")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 通知类型：DEADLINE_WARNING-到期预警, SYSTEM-系统通知 */
    @TableField("type")
    private String type;

    /** 通知标题 */
    @TableField("title")
    private String title;

    /** 通知内容 */
    @TableField("content")
    private String content;

    /** 关联项目ID（可选） */
    @TableField("project_id")
    private Long projectId;

    /** 关联版本ID（可选） */
    @TableField("version_id")
    private Long versionId;

    /** 紧急程度：0-普通, 1-紧急, 2-非常紧急 */
    @TableField("urgency")
    private Integer urgency;

    /** 是否已读：0-未读, 1-已读 */
    @TableField("is_read")
    private Integer isRead;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
