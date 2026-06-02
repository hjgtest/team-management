import request from '@/utils/request'

export function getReleasedList(days) {
  return request({
    url: '/api/released/list',
    method: 'get',
    params: { days }
  })
}

export function getRecommend(params) {
  return request({
    url: '/api/released/recommend',
    method: 'get',
    params
  })
}
