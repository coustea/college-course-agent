import request from '@/utils/request'

const http = request

export async function getWorkSidebarStatus(signal) {
  // 兼容两种来源：localStorage.className 或 profile.className
  let className = localStorage.getItem('className')
  if (!className) {
    try { const p = JSON.parse(localStorage.getItem('profile') || 'null'); className = p?.className || '' } catch {}
  }
  const url = `/api/teacherAssignments/byClassName`
  const form = new FormData()
  form.append('className', className)
  const resp = await http.post(url, form, { headers: { 'Content-Type': 'multipart/form-data' }, signal })
  return resp?.data?.data ?? resp?.data ?? {}
}

// 提交接口暂时移除，等待后续重写
export async function submitWork() {
  throw new Error('提交接口已移除，将在后续重写后恢复')
}
