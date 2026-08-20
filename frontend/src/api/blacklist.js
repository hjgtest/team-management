import request from '@/utils/request'

/**
 * 获取月度黑榜统计
 */
export function getMonthlyStats(params) {
  return request({
    url: '/api/blacklist/monthly-stats',
    method: 'get',
    params
  })
}

/**
 * 获取所有黑榜事件
 */
export function getBlacklistEvents(params) {
  return request({
    url: '/api/blacklist/events',
    method: 'get',
    params
  })
}

/**
 * 添加黑榜事件
 */
export function addBlacklistEvent(data) {
  return request({
    url: '/api/blacklist/events',
    method: 'post',
    data
  })
}

/**
 * 更新黑榜事件
 */
export function updateBlacklistEvent(id, data) {
  return request({
    url: `/api/blacklist/events/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除黑榜事件
 */
export function deleteBlacklistEvent(id) {
  return request({
    url: `/api/blacklist/events/${id}`,
    method: 'delete'
  })
}

/**
 * 获取用户历史黑榜事件
 */
export function getUserBlacklistHistory(params) {
  return request({
    url: '/api/blacklist/user-history',
    method: 'get',
    params
  })
}
