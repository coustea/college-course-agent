import request from '@/utils/request'

const http = request

function toUrl(u) {
  if (!u) return ''
  const s = String(u)
  if (/^https?:/i.test(s)) return s
  // baseURL is http://localhost:9999 (no /api), so add /api prefix
  return `/api/${s.replace(/^\//, '')}`
}

export async function getStudentsByClassName(className, signal) {
  console.log('className', className)
  const url = toUrl(`/student/class/${className}`)
  const resp = await http.get(url, { params: { className }, signal })
  console.log('学生列表', resp.data)
  return Array.isArray(resp.data) ? resp.data : resp.data.data
}

export async function createStudentGroup(payload, signal) {
  const url = toUrl('student-group')
  const resp = await http.post(url, payload, { signal })
  console.log('创建的学生分组', resp.data)
  return resp?.data
}

// 重新提交小组申请（被驳回后的再次申请）
export async function updateStudentGroup(payload, signal) {
  const url = toUrl('student-group/update')
  const resp = await http.post(url, payload, { signal })
  console.log('重新申请学生分组', resp.data)
  return resp?.data
}
