package com.teamdev.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("projects")
public class Project {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("project_name")
    private String projectName;
    
    @TableField("description")
    private String description;
    
    @TableField("start_time")
    private LocalDateTime startTime;
    
    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField(exist = false)
    private Integer status; // 0:未开始, 1:进行中, 2:已结束
    
    // 添加版本列表属性
    @TableField(exist = false)
    private List<ProjectVersion> versions;
}