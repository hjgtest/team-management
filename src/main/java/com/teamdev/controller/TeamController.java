package com.teamdev.controller;

import com.teamdev.entity.Team;
import com.teamdev.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 小组管理控制器
 * 提供小组相关的CRUD操作接口
 */
@RestController
@RequestMapping("/api/teams")
public class TeamController {
    
    @Autowired
    private TeamService teamService;
    
    /**
     * 获取所有小组信息
     * @return 包含小组列表的响应对象
     */
    @GetMapping
    public Map<String, Object> getAllTeams() {
        List<Team> teams = teamService.getAllTeams();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", teams);
        result.put("message", "查询成功");
        return result;
    }
    
    /**
     * 根据ID获取小组信息
     * @param id 小组ID
     * @return 包含小组信息的响应对象
     */
    @GetMapping("/{id}")
    public Map<String, Object> getTeamById(@PathVariable Long id) {
        Team team = teamService.getTeamById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", team);
        result.put("message", "查询成功");
        return result;
    }
    
    /**
     * 创建小组
     * @param team 小组对象
     * @return 操作结果响应对象
     */
    @PostMapping
    public Map<String, Object> createTeam(@RequestBody Team team) {
        boolean success = teamService.saveTeam(team);
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("success", success);
        result.put("message", success ? "创建成功" : "创建失败");
        return result;
    }
    
    /**
     * 更新小组信息
     * @param id 小组ID
     * @param team 小组对象
     * @return 操作结果响应对象
     */
    @PutMapping("/{id}")
    public Map<String, Object> updateTeam(@PathVariable Long id, @RequestBody Team team) {
        team.setId(id);
        boolean success = teamService.updateTeam(team);
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("success", success);
        result.put("message", success ? "更新成功" : "更新失败");
        return result;
    }
    
    /**
     * 删除小组
     * @param id 小组ID
     * @return 操作结果响应对象
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteTeam(@PathVariable Long id) {
        boolean success = teamService.deleteTeam(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("success", success);
        result.put("message", success ? "删除成功" : "删除失败");
        return result;
    }
}