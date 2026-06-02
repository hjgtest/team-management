package com.teamdev.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teamdev.entity.Role;
import com.teamdev.mapper.RoleMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RoleService extends ServiceImpl<RoleMapper, Role> {
    
    public List<Role> getAllRoles() {
        return this.list();
    }
    
    public Role getRoleById(Long id) {
        return this.getById(id);
    }
    
    public boolean saveRole(Role role) {
        return this.save(role);
    }
    
    public boolean updateRole(Role role) {
        return this.updateById(role);
    }
    
    public boolean deleteRole(Long id) {
        return this.removeById(id);
    }
}