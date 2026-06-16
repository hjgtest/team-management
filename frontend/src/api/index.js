import axios from 'axios'
import { getToken, removeToken, removeUser } from '@/utils/auth'

// 创建独立的 axios 实例：自动注入 token，但保留完整响应外壳（response.data 才是接口返回体）
// 与 request.js 的区别：response 拦截器不剥外壳，保证 response.data.data 取数组的代码正常工作
const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

api.interceptors.request.use(
  config => {
    const token = getToken()
    if (token) config.headers['Authorization'] = 'Bearer ' + token
    return config
  },
  error => Promise.reject(error)
)

api.interceptors.response.use(
  response => response,  // 返回完整 axios 响应，不剥外壳
  error => {
    if (error.response && error.response.status === 401) {
      removeToken()
      removeUser()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// 用户相关API
export const userApi = {
  // 获取所有用户
  getAllUsers: () => api.get('/users'),
  // 获取单个用户
  getUserById: (id) => api.get(`/users/${id}`),
  // 创建用户
  createUser: (data) => api.post('/users', data),
  // 更新用户
  updateUser: (id, data) => api.put(`/users/${id}`, data),
  // 删除用户
  deleteUser: (id) => api.delete(`/users/${id}`)
}

// 角色相关API
export const roleApi = {
  // 获取所有角色
  getAllRoles: () => api.get('/roles'),
  // 获取单个角色
  getRoleById: (id) => api.get(`/roles/${id}`),
  // 创建角色
  createRole: (data) => api.post('/roles', data),
  // 更新角色
  updateRole: (id, data) => api.put(`/roles/${id}`, data),
  // 删除角色
  deleteRole: (id) => api.delete(`/roles/${id}`)
}

// 小组相关API
export const teamApi = {
  // 获取所有小组
  getAllTeams: () => api.get('/teams'),
  // 获取单个小组
  getTeamById: (id) => api.get(`/teams/${id}`),
  // 创建小组
  createTeam: (data) => api.post('/teams', data),
  // 更新小组
  updateTeam: (id, data) => api.put(`/teams/${id}`, data),
  // 删除小组
  deleteTeam: (id) => api.delete(`/teams/${id}`)
}

// 项目相关API
export const projectApi = {
  // 获取所有项目
  getAllProjects: () => api.get('/projects'),
  // 获取单个项目
  getProjectById: (id) => api.get(`/projects/${id}`),
  // 创建项目
  createProject: (data) => api.post('/projects', data),
  // 更新项目
  updateProject: (id, data) => api.put(`/projects/${id}`, data),
  // 删除项目
  deleteProject: (id) => api.delete(`/projects/${id}`)
}

// 项目版本相关API
export const projectVersionApi = {
  // 获取项目的所有版本
  getProjectVersions: (projectId) => api.get(`/projects/${projectId}/versions`),
  // 获取单个版本
  getProjectVersionById: (id) => api.get(`/project-versions/${id}`),
  // 创建版本
  createProjectVersion: (data) => api.post('/project-versions', data),
  // 更新版本
  updateProjectVersion: (id, data) => api.put(`/project-versions/${id}`, data),
  // 删除版本
  deleteProjectVersion: (id) => api.delete(`/project-versions/${id}`)
}

// 项目分配相关API
export const projectAssignmentApi = {
  // 获取版本的所有分配
  getProjectAssignments: (versionId) => api.get(`/project-versions/${versionId}/assignments`),
  // 创建分配
  createProjectAssignment: (data) => api.post('/project-assignments', data),
  // 删除分配
  deleteProjectAssignment: (id) => api.delete(`/project-assignments/${id}`)
}

// 认证相关API（使用独立的 axios 实例，不经过 token 拦截器）
const authHttp = axios.create({
  baseURL: '/api',
  timeout: 10000
})
authHttp.interceptors.response.use(
  response => response.data,
  error => Promise.reject(error)
)

export const authApi = {
  // 登录
  login: (phone, password) => authHttp.post('/auth/login', { phone, password }),
  // 登出
  logout: () => authHttp.post('/auth/logout')
}

// 导出便捷方法
export const getProjects = projectApi.getAllProjects
export const getProject = projectApi.getProjectById
export const addProject = projectApi.createProject
export const updateProject = projectApi.updateProject
export const deleteProject = projectApi.deleteProject

export const getProjectVersions = projectVersionApi.getProjectVersions
export const getProjectVersion = projectVersionApi.getProjectVersionById
export const addProjectVersion = projectVersionApi.createProjectVersion
export const updateProjectVersion = projectVersionApi.updateProjectVersion
export const deleteProjectVersion = projectVersionApi.deleteProjectVersion

export const getProjectAssignments = projectAssignmentApi.getProjectAssignments
export const addProjectAssignment = projectAssignmentApi.createProjectAssignment
export const deleteProjectAssignment = projectAssignmentApi.deleteProjectAssignment

export default api
