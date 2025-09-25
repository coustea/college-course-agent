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

export async function getStudentsByGrade(grade, signal) {
    const url = toUrl(`/api/student/by-grade`)
    const resp = await http.get(url, { params: { grade }, signal })
    console.log('学生分组列表',resp.data)
    return Array.isArray(resp?.data) ? resp.data : (resp?.data?.data || [])
}

export async function createStudentGroup(payload, signal) {
    const url = toUrl(`/api/student-group/insert`)
    const resp = await http.post(url, payload, { signal })
    console.log('创建的学生分组', resp.data)
    return resp?.data
}

export default {
    getStudentsByGrade,
    createStudentGroup
}
