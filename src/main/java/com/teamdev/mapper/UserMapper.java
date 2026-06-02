package com.teamdev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamdev.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    @Select("SELECT u.*, r.role_name as roleName, t.team_name as teamName " +
            "FROM users u " +
            "LEFT JOIN roles r ON u.role_id = r.id " +
            "LEFT JOIN teams t ON u.team_id = t.id ORDER BY t.team_name")
    List<User> selectUsersWithRoleAndTeam();
    
    @Select("SELECT u.id, u.username, u.phone, u.role_id, u.team_id, u.gender, " +
            "r.role_name as roleName, t.team_name as teamName, " +
            "COALESCE(COUNT(CASE WHEN (pv.start_time <= NOW() AND pv.end_time >= NOW()) THEN pa.id END), 0) as ongoingProjectsCount " +
            "FROM users u " +
            "LEFT JOIN roles r ON u.role_id = r.id " +
            "LEFT JOIN teams t ON u.team_id = t.id " +
            "LEFT JOIN project_assignments pa ON u.id = pa.user_id " +
            "LEFT JOIN project_versions pv ON pa.project_version_id = pv.id " +
            "GROUP BY u.id, u.username, u.phone, u.role_id, u.team_id, u.gender, r.role_name, t.team_name " +
            "ORDER BY t.team_name")
    List<User> selectUsersWithRoleTeamAndOngoingProjects();
}