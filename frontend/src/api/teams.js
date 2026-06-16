import request from '@/utils/request'

export function getTeams() {
  return request({
    url: '/api/teams',
    method: 'get'
  })
}
