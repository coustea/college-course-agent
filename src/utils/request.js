// utils/request.js
import axios from 'axios'
import { ElMessage } from 'element-plus'

const normalizeBaseURL = (value) => {
  const baseURL = value || ''
  return baseURL.replace(/\/api\/?$/, '')
}

// 创建 axios 实例。各 service 已经显式使用 /api 前缀，baseURL 不再重复拼接 /api。
const request = axios.create({
  baseURL: normalizeBaseURL(import.meta.env.VITE_API_BASE_URL),
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 从 localStorage 获取 token
    const token = localStorage.getItem('token')
    if (token) {
      // 设置 Authorization header
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    return response
  },
  error => {
    if (error.response) {
      switch (error.response.status) {
        case 401:
          ElMessage.error('未授权，请重新登录')
          localStorage.removeItem('token')
          localStorage.removeItem('userInfo')
          localStorage.removeItem('userRole')
          window.location.href = '/login'
          break
        case 403:
          ElMessage.error('没有权限访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器错误')
          break
        default:
          ElMessage.error(error.response.data?.message || '请求失败')
      }
    }
    return Promise.reject(error)
  }
)

export default request
