import request from '@/utils/request'

export function getMyProjects(userId) {
  return request({
    url: '/api/projects/my',
    method: 'get',
    params: { userId }
  })
}

export function getActiveProjects() {
  return request({
    url: '/api/projects/active',
    method: 'get'
  })
}

export function getActiveProjectsWithVersions() {
  return request({
    url: '/api/projects/active-with-versions',
    method: 'get'
  })
}
