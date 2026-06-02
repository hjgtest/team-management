package com.teamdev.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("project_assignments")
public class ProjectAssignment {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("project_version_id")
    private Long projectVersionId;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("role_id")
    private String roleId;


    @TableField(exist = false)
    private String roleName;
    
    @TableField("feature")
    private String feature;
    
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}