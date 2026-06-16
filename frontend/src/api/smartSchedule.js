import request from '@/utils/request'

/**
 * 智能排期 - 执行排期
 * @param {Object} data - 排期请求数据
 * @param {number} data.projectId - 项目ID
 * @param {string} data.versionName - 版本号
 * @param {string} data.productManager - 产品经理
 * @param {string} data.prototypeUrl - 原型地址
 * @param {string} data.startTime - 开始时间
 * @param {string} data.endTime - 结束时间
 * @param {string} [data.testTime] - 提测时间
 * @param {string} [data.launchTime] - 上线时间
 * @param {Array} data.assignments - 分配列表 [{roleId, feature}]
 */
export function smartSchedule(data) {
  return request({
    url: '/api/smart-schedule/schedule',
    method: 'post',
    data
  })
}

/**
 * 获取智能排期推荐人员
 * @param {number} projectId - 项目ID
 * @param {string} [roleId] - 角色ID（可选）
 */
export function getScheduleRecommend(projectId, roleId) {
  return request({
    url: '/api/smart-schedule/recommend',
    method: 'get',
    params: { projectId, roleId }
  })
}
