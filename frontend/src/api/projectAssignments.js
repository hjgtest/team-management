import request from '@/utils/request'

export function getProjectAssignments(projectVersionId) {
  return request({
    url: `/api/projectAssignments/version/${projectVersionId}`,
    method: 'get'
  })
}

export function addProjectAssignment(data) {
  return request({
    url: '/api/projectAssignments',
    method: 'post',
    data
  })
}

export function deleteProjectAssignment(id) {
  return request({
    url: `/api/projectAssignments/${id}`,
    method: 'delete'
  })
}

export function batchAddProjectAssignments(data) {
  return request({
    url: '/api/projectAssignments/batch',
    method: 'post',
    data
  })
}