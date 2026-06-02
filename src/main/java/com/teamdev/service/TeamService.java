package com.teamdev.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teamdev.entity.Team;
import com.teamdev.mapper.TeamMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TeamService extends ServiceImpl<TeamMapper, Team> {
    
    public List<Team> getAllTeams() {
        return this.list();
    }
    
    public Team getTeamById(Long id) {
        return this.getById(id);
    }
    
    public boolean saveTeam(Team team) {
        return this.save(team);
    }
    
    public boolean updateTeam(Team team) {
        return this.updateById(team);
    }
    
    public boolean deleteTeam(Long id) {
        return this.removeById(id);
    }
}