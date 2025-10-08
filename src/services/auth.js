import axios from 'axios'

// 统一设置/清理前端登录会话（token、role、ids等），并配置 axios 默认 Authorization
export function setAuthSession(payload = {}) {
  try {
    const token = String(payload.token || '')
    const role = payload.role ? String(payload.role) : ''
    const userId = payload.userId != null ? String(payload.userId) : ''
    const userName = payload.username || payload.userName || ''

    if (userId) localStorage.setItem('userId', userId)
    if (userName) localStorage.setItem('userName', String(userName))
    if (token) localStorage.setItem('token', token)
    if (role) localStorage.setItem('userRole', role)

    if (payload.teacherId != null) localStorage.setItem('teacherId', String(payload.teacherId))
    if (payload.studentId != null) localStorage.setItem('studentId', String(payload.studentId))
    if (payload.profile) localStorage.setItem('profile', JSON.stringify(payload.profile))

    // 设置 axios 全局默认 Authorization 头
    if (token) {
      axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
    } else {
      delete axios.defaults.headers.common['Authorization']
    }
  } catch (e) {
    console.error('setAuthSession error:', e)
  }
}

export function clearAuthSession() {
  try {
    localStorage.removeItem('userId')
    localStorage.removeItem('userName')
    localStorage.removeItem('token')
    localStorage.removeItem('userRole')
    localStorage.removeItem('teacherId')
    localStorage.removeItem('studentId')
    localStorage.removeItem('profile')
    delete axios.defaults.headers.common['Authorization']
  } catch (e) {
    console.error('clearAuthSession error:', e)
  }
}

// 返回包含 Authorization: Bearer <token> 的请求头
// 用于需要手动传 headers 的场景（例如临时 axios/fetch 调用）
export function getAuthHeaders(extra = {}) {
  const token = localStorage.getItem('token')
  return token
    ? { ...extra, Authorization: `Bearer ${token}` }
    : { ...extra }
}


