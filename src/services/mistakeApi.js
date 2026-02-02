import axios from 'axios'

const fallbackBase = 'http://localhost:9999/api'

export const api = axios.create({ baseURL: import.meta?.env?.VITE_API_BASE_URL || fallbackBase, timeout: 15000 })

// 请求拦截器：自动携带token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')  // 与登录时存储的key保持一致
  if (token) {
    config.headers = { ...(config.headers || {}), Authorization: `Bearer ${token}` }
  }
  return config
})

// 响应拦截器：统一处理错误
api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      // token过期或无效，清除登录信息并跳转登录页
      localStorage.removeItem('userId')
      localStorage.removeItem('userName')
      localStorage.removeItem('token')
      localStorage.removeItem('userRole')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// 根据学生ID获取错题列表
export const getWrongQuestionsByStudentId = (studentId, courseId = null) => {
  return api.get('/wrong-question/list', { params: { studentId, courseId } })
}

// 获取错题详情
export const getWrongQuestionDetail = (id) => {
  return api.get('/wrong-question/detail', { params: { id } })
}

// 复习错题
export const reviewWrongQuestion = (id, isCorrect) => {
  return api.post('/wrong-question/review', null, { params: { id, isCorrect } })
}

// 获取错题统计信息
export const getWrongQuestionStats = (studentId, courseId = null) => {
  return api.get('/wrong-question/statistics', { params: { studentId, courseId } })
}

// 标记为已掌握
export const markAsMastered = (id) => {
  return api.post('/wrong-question/mark-mastered', null, { params: { id } })
}

// 取消标记为已掌握
export const cancelMastered = (id) => {
  return api.post('/wrong-question/cancel-mastered', null, { params: { id } })
}

// 删除错题
export const deleteWrongQuestion = (id) => {
  return api.delete('/wrong-question/delete', { params: { id } })
}

// 批量标记为已掌握
export const batchMarkAsMastered = (ids) => {
  return api.post('/wrong-question/batch-mark-mastered', ids)
}

// 批量删除错题
export const batchDeleteWrongQuestions = (ids) => {
  return api.delete('/wrong-question/batch-delete', { data: ids })
}

// 更新错题笔记
export const updateNote = (id, note) => {
  return api.post('/wrong-question/update-note', note, { params: { id } })
}

