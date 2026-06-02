package com.teamdev.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teamdev.entity.Project;
import com.teamdev.entity.ProjectVersion;
import com.teamdev.entity.ProjectAssignment;
import com.teamdev.entity.User;
import com.teamdev.mapper.ProjectMapper;
import com.teamdev.service.ProjectAssignmentService;
import com.teamdev.service.ProjectVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProjectService extends ServiceImpl<ProjectMapper, Project> {
    
    @Autowired
    private ProjectVersionService projectVersionService;
    
    @Autowired
    private ProjectAssignmentService projectAssignmentService;

    @Autowired
    private UserService userService;
    
    public List<Project> getAllProjects(String projectName) {
        List<Project> projects = getProjectsByName(projectName);
        
        // 为每个项目计算基于版本结束时间的状态
        for (Project project : projects) {
            calculateProjectStatusFromVersions(project);
        }
        
        return projects;
    }
    
    public Project getProjectById(Long id) {
        Project project = this.getById(id);
        if (project != null) {
            calculateProjectStatusFromVersions(project);
        }
        return project;
    }
    
    public boolean saveProject(Project project) {
        return this.save(project);
    }
    
    public boolean updateProject(Project project) {
        return this.updateById(project);
    }
    
    public boolean deleteProject(Long id) {
        return this.removeById(id);
    }
    
    /**
     * 根据项目名称查询项目列表
     * @param projectName 项目名称（支持模糊查询）
     * @return 包含项目列表的响应对象
     */
    public List<Project> getProjectsByName(String projectName) {
        List<Project> projects;
        if (projectName == null || projectName.trim().isEmpty()) {
            projects = this.list();
        } else {
            projects = this.lambdaQuery()
                .like(Project::getProjectName, projectName)
                .list();
        }
        
        // 为每个项目计算基于版本结束时间的状态
        for (Project project : projects) {
            calculateProjectStatusFromVersions(project);
        }
        
        return projects;
    }

    /**
     * 根据项目版本的开始和结束时间计算项目状态
     * @param project 项目对象
     */
    /**
     * 将结束时间转换为当天的23:59:59，确保当天整天都算"进行中"
     */
    private LocalDateTime endOfDay(LocalDateTime endTime) {
        if (endTime == null) return null;
        return endTime.toLocalDate().atTime(23, 59, 59);
    }

    public void calculateProjectStatusFromVersions(Project project) {
        List<ProjectVersion> versions = projectVersionService.getProjectVersionsByProjectId(project.getId());

        if (versions.isEmpty()) {
            // 如果没有版本，则根据项目本身的开始和结束时间来判断
            LocalDateTime now = LocalDateTime.now();
            if (project.getStartTime() != null && now.isBefore(project.getStartTime())) {
                project.setStatus(0); // 未开始
            } else if (project.getEndTime() != null && now.isAfter(endOfDay(project.getEndTime()))) {
                project.setStatus(2); // 已结束
            } else {
                project.setStatus(1); // 进行中
            }
        } else {
            LocalDateTime now = LocalDateTime.now();
            boolean anyVersionInProgress = false;
            boolean allVersionsFinished = true;
            boolean hasValidVersions = false;

            for (ProjectVersion version : versions) {
                LocalDateTime startTime = version.getStartTime();
                LocalDateTime endTime = endOfDay(version.getEndTime());

                if (startTime != null && endTime != null) {
                    hasValidVersions = true;
                    if (!now.isBefore(startTime) && !now.isAfter(endTime)) {
                        anyVersionInProgress = true; // 至少有一个版本正在进行中
                    } else if (now.isAfter(endTime)) {
                        // 版本已结束
                    } else {
                        allVersionsFinished = false; // 至少有一个版本尚未开始
                    }
                }
            }

            // 如果没有任何有效版本，则根据项目时间判断
            if (!hasValidVersions) {
                if (project.getStartTime() != null && now.isBefore(project.getStartTime())) {
                    project.setStatus(0); // 未开始
                } else if (project.getEndTime() != null && now.isAfter(endOfDay(project.getEndTime()))) {
                    project.setStatus(2); // 已结束
                } else {
                    project.setStatus(1); // 进行中
                }
            } else if (anyVersionInProgress) {
                project.setStatus(1); // 进行中 - 至少有一个版本正在进行中
            } else if (allVersionsFinished) {
                project.setStatus(2); // 已结束 - 所有版本均已结束
            } else {
                project.setStatus(0); // 未开始 - 至少有一个版本尚未开始
            }
        }
    }
    
    /**
     * 获取用户正在进行的项目列表
     * @param userId 用户ID
     * @return 用户正在进行的项目列表
     */
    public List<Project> getUserOngoingProjects(Long userId) {
        // 获取用户参与的所有项目分配
        List<ProjectAssignment> assignments = projectAssignmentService.lambdaQuery()
            .eq(ProjectAssignment::getUserId, userId)
            .list();
            
        // 获取这些分配对应的项目版本ID
        List<Long> versionIds = assignments.stream()
            .map(ProjectAssignment::getProjectVersionId)
            .distinct()
            .collect(Collectors.toList());
            
        if (versionIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取这些版本对应的所有项目
        List<ProjectVersion> versions = projectVersionService.lambdaQuery()
            .in(ProjectVersion::getId, versionIds)
            .list();
            
        // 获取项目ID并去重
        List<Long> projectIds = versions.stream()
            .map(ProjectVersion::getProjectId)
            .distinct()
            .collect(Collectors.toList());
            
        if (projectIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 查询这些项目并计算状态
        List<Project> projects = this.lambdaQuery()
            .in(Project::getId, projectIds)
            .list();
            
        // 过滤出正在进行中的项目
        LocalDateTime now = LocalDateTime.now();
        return projects.stream()
            .filter(project -> {
                calculateProjectStatusFromVersions(project);
                return project.getStatus() == 1; // 状态为1表示进行中
            })
            .collect(Collectors.toList());
    }

    /**
     * 获取用户正在进行的项目和版本列表
     * @param userId 用户ID
     * @return 用户正在进行的项目和版本列表
     */
    public List<Map<String, Object>> getUserOngoingProjectsAndVersions(Long userId) {
        // 获取用户参与的所有项目分配
        List<ProjectAssignment> assignments = projectAssignmentService.lambdaQuery()
            .eq(ProjectAssignment::getUserId, userId)
            .list();
            
        // 获取这些分配对应的项目版本ID
        List<Long> versionIds = assignments.stream()
            .map(ProjectAssignment::getProjectVersionId)
            .distinct()
            .collect(Collectors.toList());
            
        if (versionIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取这些版本信息
        List<ProjectVersion> versions = projectVersionService.lambdaQuery()
            .in(ProjectVersion::getId, versionIds)
            .list();
            
        // 获取版本对应的项目ID并去重
        List<Long> projectIds = versions.stream()
            .map(ProjectVersion::getProjectId)
            .distinct()
            .collect(Collectors.toList());
            
        if (projectIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 查询项目信息
        List<Project> projects = this.lambdaQuery()
            .in(Project::getId, projectIds)
            .list();
            
        // 构建结果：按项目分组，每个项目包含其进行中的版本
        Map<Long, Map<String, Object>> projectMap = new HashMap<>();
        
        // 初始化项目信息
        for (Project project : projects) {
            Map<String, Object> projectInfo = new HashMap<>();
            projectInfo.put("id", project.getId());
            projectInfo.put("projectName", project.getProjectName());
            projectInfo.put("description", project.getDescription());
            projectInfo.put("startTime", project.getStartTime());
            projectInfo.put("endTime", project.getEndTime());
            projectInfo.put("versions", new ArrayList<>());
            projectMap.put(project.getId(), projectInfo);
        }
        
        // 添加进行中的版本到对应项目
        LocalDateTime now = LocalDateTime.now();
        for (ProjectVersion version : versions) {
            // 计算版本状态（结束时间包含当天整天）
            boolean isOngoing = false;
            if (version.getStartTime() != null && version.getEndTime() != null) {
                LocalDateTime endTime = endOfDay(version.getEndTime());
                isOngoing = !now.isBefore(version.getStartTime()) && !now.isAfter(endTime);
            }
            
            if (isOngoing) {
                Map<String, Object> projectInfo = projectMap.get(version.getProjectId());
                if (projectInfo != null) {
                    Map<String, Object> versionInfo = buildVersionInfoMap(version);
                    versionInfo.put("status", 1); // 进行中
                    
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> versionsList = (List<Map<String, Object>>) projectInfo.get("versions");
                    versionsList.add(versionInfo);
                }
            }
        }
        
        // 过滤掉没有进行中版本的项目
        return projectMap.values().stream()
            .filter(project -> !((List<?>) project.get("versions")).isEmpty())
            .collect(Collectors.toList());
    }
    
    /**
     * 获取所有项目和版本列表（用于导出）
     * @return 所有项目和版本的层级结构数据
     */
    public List<Map<String, Object>> getAllProjectsAndVersions() {
        // 获取所有项目
        List<Project> projects = this.list();
        
        // 获取所有项目版本
        List<ProjectVersion> versions = projectVersionService.list();
        
        // 按项目 ID 分组版本
        Map<Long, List<ProjectVersion>> versionsByProjectId = new HashMap<>();
        for (ProjectVersion version : versions) {
            if (!versionsByProjectId.containsKey(version.getProjectId())) {
                versionsByProjectId.put(version.getProjectId(), new ArrayList<>());
            }
            versionsByProjectId.get(version.getProjectId()).add(version);
        }
        
        // 获取所有项目分配
        List<ProjectAssignment> assignments = projectAssignmentService.list();
        
        // 按版本 ID 分组分配
        Map<Long, List<ProjectAssignment>> assignmentsByVersionId = new HashMap<>();
        for (ProjectAssignment assignment : assignments) {
            if (!assignmentsByVersionId.containsKey(assignment.getProjectVersionId())) {
                assignmentsByVersionId.put(assignment.getProjectVersionId(), new ArrayList<>());
            }
            assignmentsByVersionId.get(assignment.getProjectVersionId()).add(assignment);
        }
        
        // 获取所有用户信息，用于映射用户名
        List<User> users = userService.list();
        Map<Long, String> userMap = new HashMap<>();
        for (User user : users) {
            userMap.put(user.getId(), user.getUsername());
        }
        
        // 构建结果：按项目分组，每个项目包含其版本和负责人
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Project project : projects) {
            Map<String, Object> projectInfo = new HashMap<>();
            projectInfo.put("id", project.getId());
            projectInfo.put("projectName", project.getProjectName());
            projectInfo.put("description", project.getDescription());
            projectInfo.put("startTime", project.getStartTime());
            projectInfo.put("endTime", project.getEndTime());
            projectInfo.put("versions", new ArrayList<>());
            
            // 获取该项目的所有版本
            List<ProjectVersion> projectVersions = versionsByProjectId.get(project.getId());
            if (projectVersions == null) {
                projectVersions = new ArrayList<>();
            }
            
            for (ProjectVersion version : projectVersions) {
                Map<String, Object> versionInfo = new HashMap<>();
                versionInfo.put("id", version.getId());
                versionInfo.put("versionNumber", version.getVersionName());
                versionInfo.put("description", version.getDescription());
                versionInfo.put("startTime", version.getStartTime());
                versionInfo.put("endTime", version.getEndTime());
                versionInfo.put("testTime", version.getTestTime());
                versionInfo.put("launchTime", version.getLaunchTime());
                versionInfo.put("productManager", version.getProductManager());
                versionInfo.put("prototypeUrl", version.getPrototypeUrl());
                
                // 获取该版本的所有分配
                List<ProjectAssignment> versionAssignments = assignmentsByVersionId.get(version.getId());
                if (versionAssignments == null) {
                    versionAssignments = new ArrayList<>();
                }
                
                // 收集负责该版本的用户名
                List<String> userNames = new ArrayList<>();
                for (ProjectAssignment assignment : versionAssignments) {
                    String username = userMap.get(assignment.getUserId());
                    if (username != null) {
                        userNames.add(username);
                    } else {
                        userNames.add("未知用户");
                    }
                }
                versionInfo.put("userNames", userNames); // 修改为 userNames 而不是 userIds
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> versionsList = (List<Map<String, Object>>) projectInfo.get("versions");
                versionsList.add(versionInfo);
            }
            
            result.add(projectInfo);
        }
        
        return result;
    }
    
    /**
     * 获取指定用户参与的所有项目和版本列表（用于按人统计导出）
     * @param userId 用户 ID
     * @return 用户参与的项目和版本列表
     */
    public List<Map<String, Object>> getUserProjectsAndVersions(Long userId) {
        // 获取用户参与的所有项目分配
        List<ProjectAssignment> assignments = projectAssignmentService.lambdaQuery()
            .eq(ProjectAssignment::getUserId, userId)
            .list();
            
        if (assignments.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取这些分配对应的项目版本 ID
        List<Long> versionIds = assignments.stream()
            .map(ProjectAssignment::getProjectVersionId)
            .distinct()
            .collect(Collectors.toList());
            
        if (versionIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取这些版本信息
        List<ProjectVersion> versions = projectVersionService.lambdaQuery()
            .in(ProjectVersion::getId, versionIds)
            .list();
            
        // 获取版本对应的项目 ID 并去重
        List<Long> projectIds = versions.stream()
            .map(ProjectVersion::getProjectId)
            .distinct()
            .collect(Collectors.toList());
            
        if (projectIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 查询项目信息
        List<Project> projects = this.lambdaQuery()
            .in(Project::getId, projectIds)
            .list();
            
        // 构建结果：按项目分组，每个项目包含其版本信息
        Map<Long, Map<String, Object>> projectMap = new HashMap<>();
        
        // 初始化项目信息
        for (Project project : projects) {
            Map<String, Object> projectInfo = new HashMap<>();
            projectInfo.put("id", project.getId());
            projectInfo.put("projectName", project.getProjectName());
            projectInfo.put("description", project.getDescription());
            projectInfo.put("startTime", project.getStartTime());
            projectInfo.put("endTime", project.getEndTime());
            projectInfo.put("versions", new ArrayList<>());
            projectMap.put(project.getId(), projectInfo);
        }
        
        // 添加版本到对应项目
        for (ProjectVersion version : versions) {
            Map<String, Object> projectInfo = projectMap.get(version.getProjectId());
            if (projectInfo != null) {
                Map<String, Object> versionInfo = new HashMap<>();
                versionInfo.put("id", version.getId());
                versionInfo.put("versionNumber", version.getVersionName());
                versionInfo.put("description", version.getDescription());
                versionInfo.put("startTime", version.getStartTime());
                versionInfo.put("endTime", version.getEndTime());
                versionInfo.put("testTime", version.getTestTime());
                versionInfo.put("launchTime", version.getLaunchTime());
                versionInfo.put("productManager", version.getProductManager());
                versionInfo.put("prototypeUrl", version.getPrototypeUrl());
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> versionsList = (List<Map<String, Object>>) projectInfo.get("versions");
                versionsList.add(versionInfo);
            }
        }
        
        return new ArrayList<>(projectMap.values());
    }

    /**
     * 构建版本信息 Map（通用方法，包含所有字段）
     */
    private Map<String, Object> buildVersionInfoMap(ProjectVersion version) {
        Map<String, Object> versionInfo = new HashMap<>();
        versionInfo.put("id", version.getId());
        versionInfo.put("versionNumber", version.getVersionName());
        versionInfo.put("description", version.getDescription());
        versionInfo.put("startTime", version.getStartTime());
        versionInfo.put("endTime", version.getEndTime());
        versionInfo.put("testTime", version.getTestTime());
        versionInfo.put("launchTime", version.getLaunchTime());
        versionInfo.put("productManager", version.getProductManager());
        versionInfo.put("prototypeUrl", version.getPrototypeUrl());
        return versionInfo;
    }
}