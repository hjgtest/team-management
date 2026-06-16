package com.teamdev.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String phone;
    private Integer roleId;
    private Integer teamId;
    private String gender;

    private Integer isActive;

    private Integer userType;

    private String password;

    @TableField(exist = false)
    private String roleName;

    @TableField(exist = false)
    private String teamName;

    @TableField(exist = false)
    private Integer ongoingProjectsCount;
}