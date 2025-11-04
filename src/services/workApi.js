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
  // 兼容两种来源：localStorage.className 或 profile.className
  let className = localStorage.getItem('className')
  console.log("className班级:",className)
  if (!className) {
    try { const p = JSON.parse(localStorage.getItem('profile') || 'null'); className = p?.className || '' } catch {}
  }
  const url = toUrl(`/api/teacherAssignments/byClassName`)
  const form = new FormData()
  form.append('className', className)
  const resp = await http.post(url, form, { headers: { 'Content-Type': 'multipart/form-data' }, signal })
  console.log("作品作业的列表",resp.data)
  return resp?.data?.data ?? resp?.data ?? {}
}
// 提交接口暂时移除，等待后续重写

export async function submitWork() {
  throw new Error('提交接口已移除，将在后续重写后恢复')
}



