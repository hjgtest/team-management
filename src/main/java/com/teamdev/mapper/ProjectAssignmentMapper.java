package com.teamdev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamdev.entity.ProjectAssignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProjectAssignmentMapper extends BaseMapper<ProjectAssignment> {
    
    // 自定义联表查询方法，关联roles表获取roleName
    List<ProjectAssignment> selectAssignmentsWithRoleName(@Param("versionId") Long versionId);
}