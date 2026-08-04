import request from '@/utils/request'

/**
 * 获取月度Bug统计报表
 */
export function getMonthlyReport(params) {
  return request({
    url: '/api/bug-stats/monthly-report',
    method: 'get',
    params
  })
}
