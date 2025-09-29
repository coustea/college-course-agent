import axios from "axios"

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

// 获取左侧栏绑定状态（例如：待提交数量、截止时间等）
// params 可携带 userId/classId/courseId 等筛选维度
export async function getWorkSidebarStatus(params = {}, signal) {
  const url = toUrl('/api/work/sidebar-status')
  const resp = await http.get(url, { params, signal })
  // 兼容 { data: {...} } 或直接返回对象
  return resp?.data?.data ?? resp?.data ?? {}
}

// 提交作品
// payload: { title, description, files?: UploadFile[] | File[] }
export async function submitWork(payload = {}, signal) {
  const url = toUrl('/api/work/submit')
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
    files: files.map(f => ({ name: f?.name || '', type: f?.type || 'file' }))
  }, { signal })
  return resp?.data
}

export default {
  getWorkSidebarStatus,
  submitWork
}


