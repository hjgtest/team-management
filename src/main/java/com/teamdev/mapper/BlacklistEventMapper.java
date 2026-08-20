package com.teamdev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamdev.entity.BlacklistEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 黑榜事件Mapper
 */
@Mapper
public interface BlacklistEventMapper extends BaseMapper<BlacklistEvent> {
}
