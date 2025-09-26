// api.js，ai完善版，后面还要改
import axios from 'axios'

/** @typedef {{ ok: true, data: any }} OkResult */
/** @typedef {{ ok: false, error: { message: string, status?: number, code?: string, data?: any } }} ErrResult */
/** @typedef {OkResult | ErrResult} Result */

export const api = axios.create({
  baseURL: import.meta?.env?.VITE_API_BASE_URL || '/api',
  timeout: 10000,
})

// 请求拦截器：自动附带 Token 等
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers = { ...(config.headers || {}), Authorization: `Bearer ${token}` }
  }
  return config
})

// 响应拦截器：统一错误结构
api.interceptors.response.use(
  (res) => res,
  (error) => {
    if (axios.isAxiosError(error)) {
      const normalized = {
        message: error.message,
        status: error.response?.status,
        code: error.code,
        data: error.response?.data,
      }
      return Promise.reject(normalized)
    }
    return Promise.reject({ message: 'Unknown error' })
  }
)

/**
 * 通用请求：返回统一 Result
 * @param {import('axios').AxiosRequestConfig} config
 * @returns {Promise<Result>}
 */
async function request(config) {
  try {
    const res = await api.request(config)
    return { ok: true, data: res.data }
  } catch (error) {
    return { ok: false, error }
  }
}

/**
 * 获取用户课程列表
 * @param {string} userId
 * @param {AbortSignal} [signal]
 * @returns {Promise<Result>}
 */
export function fetchUserCourses(userId, signal) {
  return request({
    method: 'get',
    url: `/users/${encodeURIComponent(userId)}/courses`,
    signal,
  })
}

/**
 * 更新“文件(document)”进度（部分更新）
 * @param {string|number} documentId
 * @param {number} progress 例如 0~1 或 0~100，按后端定义
 * @param {AbortSignal} [signal]
 * @returns {Promise<Result>}
 */
export function updateDocumentProgress(documentId, progress, signal) {
  if (typeof progress !== 'number' || Number.isNaN(progress)) {
    return Promise.resolve({ ok: false, error: { message: 'progress 必须是数字' } })
  }
  return request({
    method: 'patch',
    url: `/documents/${encodeURIComponent(documentId)}/progress`,
    data: { progress },
    signal,
  })
}

// 兼容旧函数名，内部转发到 documents 接口
export const updateMaterialProgress = (materialId, progress, signal) =>
  updateDocumentProgress(materialId, progress, signal)

