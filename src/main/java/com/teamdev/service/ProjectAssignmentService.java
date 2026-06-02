package com.teamdev.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teamdev.entity.ProjectAssignment;
import com.teamdev.mapper.ProjectAssignmentMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProjectAssignmentService extends ServiceImpl<ProjectAssignmentMapper, ProjectAssignment> {
    
    public List<ProjectAssignment> getAssignmentsByVersionId(Long versionId) {
        return baseMapper.selectAssignmentsWithRoleName(versionId);
    }
    
    public boolean saveAssignment(ProjectAssignment assignment) {
        return this.save(assignment);
    }
    
    public boolean updateAssignment(ProjectAssignment assignment) {
        return this.updateById(assignment);
    }
    
    public boolean deleteAssignment(Long id) {
        return this.removeById(id);
    }

    public boolean batchSaveAssignments(List<ProjectAssignment> assignments) {
        return this.saveBatch(assignments);
    }
}