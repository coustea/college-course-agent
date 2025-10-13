import axios from "axios"

const BASE = 'http://39.96.172.21:9999'

const http = axios.create({
  baseURL: BASE,
  timeout: 15000
})

http.interceptors.request.use((config) => {
    try {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers = { ...(config.headers || {}), Authorization: `Bearer ${token}` }
        }
    } catch (e) {
        alert(`读取登录信息失败：${e?.message || e}`)
    }
    return config
})
function toUrl(u) {
  if (!u) return ''
  const s = String(u)
  if (/^https?:/i.test(s)) return s
  return `${BASE.replace(/\/$/, '')}/${s.replace(/^\//, '')}`
}

export async function getWorkSidebarStatus(signal) {
  const profile = localStorage.getItem('profile')
  const className = profile?.className
  const url = toUrl(`api/teacherAssignments/byClassName`)
  const resp = await http.post(url, { className, signal })
  console.log("作品作业的列表",resp.data)
  return resp?.data?.data ?? resp?.data ?? {}
}

async function uploadTo(url, payload = {}, signal) {
  const files = Array.isArray(payload?.files) ? payload.files : []
  const form = new FormData()
  if (payload.assignmentId != null) form.append('assignmentId', String(payload.assignmentId))
  if (payload.studentId != null) form.append('studentId', String(payload.studentId))
  if (payload.content != null) form.append('content', String(payload.content))
  // 后端要求单文件字段名为 file，只取第一个文件
  const first = files[0]
  if (first) {
    const raw = first?.raw ?? first
    if (raw) form.append('file', raw, raw.name || 'file')
  }
  console.log(0)
  const resp = await http.post(url, form, { headers: {
    Authorization: `Bearer ${localStorage.getItem('token')}`,
    'Content-Type': 'multipart/form-data',
  }, signal })
  console.log(1)
  console.log("上传的作品作业状态",resp.data)
  return resp.data
}

// 提交接口暂时移除，等待后续重写

export async function submitWork() {
  throw new Error('提交接口已移除，将在后续重写后恢复')
}



