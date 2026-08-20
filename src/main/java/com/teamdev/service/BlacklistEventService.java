package com.teamdev.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.teamdev.entity.BlacklistEvent;
import com.teamdev.mapper.BlacklistEventMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 黑榜事件服务类
 */
@Service
public class BlacklistEventService {

    @Autowired
    private BlacklistEventMapper blacklistEventMapper;

    /**
     * 获取月度黑榜统计
     */
    public Map<String, Object> getMonthlyStats(Integer year, Integer month) {
        // 查询该月所有黑榜事件
        QueryWrapper<BlacklistEvent> wrapper = new QueryWrapper<>();
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
        
        wrapper.between("event_date", firstDay, lastDay);
        List<BlacklistEvent> events = blacklistEventMapper.selectList(wrapper);

        // 按用户统计
        Map<Long, UserBlacklistStats> statsMap = new HashMap<>();
        
        for (BlacklistEvent event : events) {
            Long userId = event.getUserId();
            if (!statsMap.containsKey(userId)) {
                UserBlacklistStats stats = new UserBlacklistStats();
                stats.setUserId(userId);
                stats.setEventCount(0);
                stats.setSeverityBreakdown(new HashMap<>());
                statsMap.put(userId, stats);
            }
            
            UserBlacklistStats stats = statsMap.get(userId);
            stats.setEventCount(stats.getEventCount() + 1);
            
            // 严重程度分布
            String severityKey = getSeverityText(event.getSeverityLevel());
            stats.getSeverityBreakdown().merge(severityKey, 1, Integer::sum);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalEvents", events.size());
        result.put("userStats", statsMap.values());
        
        return result;
    }

    /**
     * 获取用户在指定月份的黑榜事件数
     */
    public int getUserEventCount(Long userId, Integer year, Integer month) {
        QueryWrapper<BlacklistEvent> wrapper = new QueryWrapper<>();
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
        
        wrapper.eq("user_id", userId)
               .between("event_date", firstDay, lastDay);
        
        return blacklistEventMapper.selectCount(wrapper).intValue();
    }

    /**
     * 添加黑榜事件
     */
    public boolean addEvent(BlacklistEvent event) {
        return blacklistEventMapper.insert(event) > 0;
    }

    /**
     * 删除黑榜事件
     */
    public boolean deleteEvent(Long id) {
        return blacklistEventMapper.deleteById(id) > 0;
    }

    /**
     * 更新黑榜事件
     */
    public boolean updateEvent(BlacklistEvent event) {
        return blacklistEventMapper.updateById(event) > 0;
    }

    /**
     * 获取用户历史黑榜事件
     */
    public List<BlacklistEvent> getUserHistory(Long userId, Integer year) {
        QueryWrapper<BlacklistEvent> wrapper = new QueryWrapper<>();
        
        if (year != null) {
            LocalDate firstDay = LocalDate.of(year, 1, 1);
            LocalDate lastDay = LocalDate.of(year, 12, 31);
            wrapper.between("event_date", firstDay, lastDay);
        }
        
        wrapper.eq("user_id", userId)
               .orderByDesc("event_date");
        
        return blacklistEventMapper.selectList(wrapper);
    }

    /**
     * 获取所有黑榜事件（支持筛选）
     */
    public List<BlacklistEvent> getAllEvents(Integer year, Integer month, Long userId) {
        QueryWrapper<BlacklistEvent> wrapper = new QueryWrapper<>();
        
        if (year != null && month != null) {
            LocalDate firstDay = LocalDate.of(year, month, 1);
            LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
            wrapper.between("event_date", firstDay, lastDay);
        }
        
        if (userId != null) {
            wrapper.eq("user_id", userId);
        }
        
        wrapper.orderByDesc("event_date", "created_at");
        
        return blacklistEventMapper.selectList(wrapper);
    }

    /**
     * 获取严重程度文本
     */
    private String getSeverityText(Integer level) {
        if (level == null) return "未知";
        switch (level) {
            case 1: return "轻微";
            case 2: return "一般";
            case 3: return "严重";
            default: return "未知";
        }
    }

    /**
     * 用户黑榜统计数据
     */
    public static class UserBlacklistStats {
        private Long userId;
        private Integer eventCount;
        private Map<String, Integer> severityBreakdown;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Integer getEventCount() { return eventCount; }
        public void setEventCount(Integer eventCount) { this.eventCount = eventCount; }
        public Map<String, Integer> getSeverityBreakdown() { return severityBreakdown; }
        public void setSeverityBreakdown(Map<String, Integer> severityBreakdown) { this.severityBreakdown = severityBreakdown; }
    }
}
