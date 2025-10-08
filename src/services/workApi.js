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

// 获取左侧栏绑定状态（例如：待提交数量、截止时间等）
// params 可携带 userId/classId/courseId 等筛选维度
export async function getWorkSidebarStatus(signal) {
  const profile = localStorage.getItem('profile')
  const className = profile?.className
  const url = toUrl(`api/teacherAssignments/byClassName`)
  const resp = await http.post(url, { className, signal })
  console.log("作品作业的列表",resp.data)
  return resp?.data?.data ?? resp?.data ?? {}
}

// 提交作品
// payload: { title, description, files?: UploadFile[] | File[] }
export async function submitWork(payload = {}, signal) {
  // 根据提交身份选择后端接口（个人/小组）
  const resolveSubmitUrl = () => {
    const t = String(stype || '').toLowerCase()
    if (t === 'group' || t === 'team') return 'http://39.96.172.21:9999/api/group-submission/upload'
    return 'http://39.96.172.21:9999/api/personal-submission/upload'
  }
  const userId  = localStorage.getItem('userId')
  const res = await axios .post(resolveSubmitUrl(), {userId,...payload},{
      headers: {
          'Content-Type': 'multipart/form-data',
          Authorization: `Bearer ${localStorage.getItem('token')}`
      }
  })
  console.log("个人提交作品的状态",res.data)
  const files = Array.isArray(payload?.files) ? payload.files : []

  // 判定是否包含二进制文件（Element Plus UploadFile.raw 或 File）
  const hasBinary = files.some(f => {
    const raw = f?.raw ?? f
    return raw instanceof File || (raw && typeof raw === 'object' && typeof raw.size === 'number')
  })

  if (hasBinary) {
    const form = new FormData()
    if (payload.title != null) form.append('title', String(payload.title))
    if (payload.description != null) form.append('description', String(payload.description))
    if (payload.submitType != null) form.append('submitType', String(payload.submitType))
    files.forEach((f, idx) => {
      const raw = f?.raw ?? f
      if (raw) form.append('files', raw, raw.name || `file_${idx + 1}`)
    })
    const resp = await http.post(url, form, { headers: { 'Content-Type': 'multipart/form-data' }, signal })
    return resp?.data
  }

  // 无文件或仅元数据，走 JSON
  const resp = await http.post(url, {
    title: payload?.title ?? '',
    description: payload?.description ?? '',
    files: files.map(f => ({ name: f?.name || '', type: f?.type || 'file' })),
    submitType: payload?.submitType ?? 'individual'
  }, { signal })
  return resp?.data
}

export default {
  getWorkSidebarStatus,
  submitWork
}


