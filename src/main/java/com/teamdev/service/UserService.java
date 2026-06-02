package com.teamdev.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teamdev.entity.User;
import com.teamdev.mapper.UserMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {
    
    public List<User> getAllUsers() {
        return this.baseMapper.selectUsersWithRoleTeamAndOngoingProjects();
    }
    
    public List<User> getAllUsersWithoutOngoingProjects() {
        return this.baseMapper.selectUsersWithRoleAndTeam();
    }
    
    public User getUserById(Long id) {
        return this.getById(id);
    }
    
    public boolean saveUser(User user) {
        return this.save(user);
    }
    
    public boolean updateUser(User user) {
        return this.updateById(user);
    }
    
    public boolean deleteUser(Long id) {
        return this.removeById(id);
    }
}