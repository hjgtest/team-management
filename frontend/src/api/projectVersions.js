import request from '@/utils/request'

export function getProjectVersions(projectId) {
  return request({
    url: '/api/projectVersions/search',
    method: 'get',
    params: {
      projectId: projectId
    }
  })
}

export function addProjectVersion(data) {
  return request({
    url: '/api/projectVersions',
    method: 'post',
    data
  })
}

export function updateProjectVersion(id, data) {
  return request({
    url: `/api/projectVersions/${id}`,
    method: 'put',
    data
  })
}

export function deleteProjectVersion(id) {
  return request({
    url: `/api/projectVersions/${id}`,
    method: 'delete'
  })
}