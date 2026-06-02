import request from '@/utils/request'

export function getUsers() {
  return request({
    url: '/api/users',
    method: 'get'
  })
}

export function getUserById(id) {
  return request({
    url: `/api/users/${id}`,
    method: 'get'
  })
}

export function addUser(data) {
  return request({
    url: '/api/users',
    method: 'post',
    data
  })
}

export function updateUser(id, data) {
  return request({
    url: `/api/users/${id}`,
    method: 'put',
    data
  })
}

export function deleteUser(id) {
  return request({
    url: `/api/users/${id}`,
    method: 'delete'
  })
}

export function getUserOngoingProjects(userId) {
  return request({
    url: `/api/users/${userId}/ongoing-projects`,
    method: 'get'
  })
}