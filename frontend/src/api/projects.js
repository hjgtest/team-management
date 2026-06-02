import request from '@/utils/request'

export function getProjects(projectName) {
  return request({
    url: '/api/projects',
    method: 'get',
    params: { projectName }
  })
}

export function addProject(data) {
  return request({
    url: '/api/projects',
    method: 'post',
    data
  })
}

export function updateProject(id, data) {
  return request({
    url: `/api/projects/${id}`,
    method: 'put',
    data
  })
}

export function deleteProject(id) {
  return request({
    url: `/api/projects/${id}`,
    method: 'delete'
  })
}