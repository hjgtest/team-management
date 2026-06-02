package com.teamdev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamdev.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProjectMapper extends BaseMapper<Project> {

}