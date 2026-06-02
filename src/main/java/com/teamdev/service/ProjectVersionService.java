package com.teamdev.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teamdev.entity.Project;
import com.teamdev.entity.ProjectVersion;
import com.teamdev.mapper.ProjectVersionMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProjectVersionService extends ServiceImpl<ProjectVersionMapper, ProjectVersion> {
    
    public List<ProjectVersion> getAllProjectVersions(Long projectId) {
        return this.lambdaQuery()
                .eq(ProjectVersion::getProjectId, projectId)
                .orderByDesc(ProjectVersion::getCreatedTime)
                .list();
    }
    
    public ProjectVersion getProjectVersionById(Long id) {
        return this.getById(id);
    }
    
    public boolean saveProjectVersion(ProjectVersion projectVersion) {
        return this.save(projectVersion);
    }
    
    public boolean updateProjectVersion(ProjectVersion projectVersion) {
        return this.updateById(projectVersion);
    }
    
    public boolean deleteProjectVersion(Long id) {
        return this.removeById(id);
    }
    
    public List<ProjectVersion> getProjectVersionsByProjectId(Long projectId) {
        return this.lambdaQuery()
            .eq(ProjectVersion::getProjectId, projectId)
            .orderByDesc(ProjectVersion::getEndTime)
            .list();
    }
}