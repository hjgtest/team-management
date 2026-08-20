package com.teamdev.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 黑榜事件实体类
 */
@Data
@TableName("blacklist_events")
public class BlacklistEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 事件类型
     */
    @TableField("event_type")
    private String eventType;

    /**
     * 事件描述
     */
    @TableField("event_description")
    private String eventDescription;

    /**
     * 严重程度: 1-轻微, 2-一般, 3-严重
     */
    @TableField("severity_level")
    private Integer severityLevel;

    /**
     * 事件发生日期
     */
    @TableField("event_date")
    private LocalDate eventDate;

    /**
     * 记录人ID
     */
    @TableField("recorded_by")
    private Long recordedBy;

    /**
     * 关联项目ID(可选)
     */
    @TableField("project_id")
    private Long projectId;

    /**
     * 关联版本ID(可选)
     */
    @TableField("version_id")
    private Long versionId;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
