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

    /**
     * 获取所有在职用户
     */
    public List<User> getActiveUsers() {
        return this.lambdaQuery()
                .eq(User::getIsActive, 1)
                .list();
    }

    /**
     * 获取所有可参与自动分配的用户（在职 + 正常研发成员）
     */
    public List<User> getAssignableUsers() {
        return this.lambdaQuery()
                .eq(User::getIsActive, 1)
                .eq(User::getUserType, 2)
                .list();
    }

    /**
     * 根据手机号和密码登录（仅限在职用户）
     */
    public User login(String phone, String password) {
        return this.lambdaQuery()
                .eq(User::getPhone, phone)
                .eq(User::getPassword, password)
                .eq(User::getIsActive, 1)
                .one();
    }

    /**
     * 根据手机号查询用户
     */
    public User getByPhone(String phone) {
        return this.lambdaQuery()
                .eq(User::getPhone, phone)
                .one();
    }
}