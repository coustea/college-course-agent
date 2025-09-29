import axios from 'axios'

const BASE = 'http://192.168.52.75:9999'

const http = axios.create({
  baseURL: BASE,
  timeout: 15000
})

function toUrl(u) {
  if (!u) return ''
  const s = String(u)
  if (/^https?:/i.test(s)) return s
  return `${BASE.replace(/\/$/, '')}/${s.replace(/^\//, '')}`
}

/**
 * 修改学生密码/信息（按后端给定路径）
 * @param {number|string} id 学生ID
 * @param {{ currentPassword?: string, newPassword?: string }} payload 负载
 * @param {AbortSignal} [signal]
 */
export async function updateStudentPassword(id, payload = {}, signal) {
  const url = toUrl(`/api/teacher/update/student`)
  const params = { id }
  const body = {
    ...payload
  }
  const resp = await http.post(url, body, { params, signal })
  return resp?.data
}

export default {
  updateStudentPassword
}