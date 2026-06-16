import axios from 'axios'
import { getToken, removeToken, removeUser } from './auth'
import router from '../router'

// 创建axios实例
const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API || '', // 不设置baseURL，各调用方已包含完整 /api/ 路径前缀
  timeout: 5000 // 请求超时时间
})

// request拦截器
service.interceptors.request.use(
  config => {
    // 注入 token
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  error => {
    console.log(error) // for debug
    Promise.reject(error)
  }
)

// response拦截器
service.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    // 处理 401 未授权
    if (error.response && error.response.status === 401) {
      removeToken()
      removeUser()
      router.push('/login')
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }
    console.log('err' + error) // for debug
    return Promise.reject(error)
  }
)

export default service