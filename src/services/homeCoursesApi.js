// 首页课程接口服务
import { api } from './coursesApi'

// 统一将相对路径转换为可访问 URL：
// - 已是绝对地址: 原样返回
// - 以 /uploads/ 开头: 走相对路径交给 Vite 代理或 Nginx（避免硬编码主机）
// - 其他相对: 补上 / 作为相对路径
const toUrl = (u) => {
    if (!u) return ''
    const s = String(u)
    if (/^(https?:|data:|blob:)/.test(s)) return s
    if (s.startsWith('/uploads/')) return s
    return `/${s.replace(/^\//, '')}`
}

/**
 * 获取首页所有课程数据
 * 仅展示后端数据；失败或无数据时返回空数组
 * @param {AbortSignal} [signal] - 取消请求的信号
 * @returns {Promise<Array>} 课程数据数组
 */
export async function fetchHomeCourses(signal) {
    try {
        // 使用统一 api 实例与相对路径，便于通过 env/proxy 切换环境
        const response = await api.get('/api/course/list', { signal })
        const payload = response?.data
        console.log('首页课程数据', payload)
        const list = Array.isArray(payload)
            ? payload
            : (Array.isArray(payload?.data) ? payload.data : null)

        if (Array.isArray(list) && list.length) {
            const result = []
            for (const c of list) {
                const base = {
                    id: c.courseId,
                    title: c.courseName ?? c.title ?? '未命名课程',
                    description: c.description ?? '',
                    image: toUrl(c.resourceUrl)  || 'https://images.unsplash.com/photo-1501504905252-473c47e087f8?auto=format&fit=crop&w=1200&q=80',
                    startDate: c.startDate || '',
                    endDate: c.endDate || '',
                    teacher: (c?.teacher?.name) || c.teacherName || c.teacher || '无',
                    teacherId: (c?.teacher?.id) || c.teacherId || null,
                    category: c.courseCode,
                }

                const videos = Array.isArray(c.videos) ? c.videos : []
                const docs = Array.isArray(c.documents) ? c.documents : []

                const videoChapters = videos.map((v, i) => ({
                    title: v.title || v.name || `第${i + 1}集`,
                    duration: v.duration,
                    videoUrl: toUrl(v.url || v.videoUrl || v.resourceUrl || ''),
                    // 传递后端的唯一标识，便于统计上报
                    videoId: v.videoId ?? v.id ?? v.videoIndex ?? (i + 1),
                    courseId: c.courseId ?? c.id ?? null
                }))
                const docChapters = docs.map((d, i) => ({
                    title: d.title || d.name || d.docTitle || `第${i + 1}节`,
                    duration: d.duration || d.pages || '',
                    // 兼容后端字段：docUrl / fileUrl / url / resourceUrl
                    fileUrl: toUrl(d.docUrl || d.fileUrl || d.url || d.resourceUrl || ''),
                    html: d.html || d.content || '',
                    // 传递后端的唯一标识，便于统计上报
                    documentId: d.documentId ?? d.id ?? d.docIndex ?? (i + 1),
                    courseId: c.courseId ?? c.id ?? null
                }))

                // B 方案：两种类型都有时视为视频类
                // 分裂为两个课程卡片：视频课程与文档课程分别渲染，避免一方覆盖另一方
                let pushed = false
                if (videoChapters.length > 0) {
                    result.push(normalizeCourse({
                        ...base,
                        type: 'video',
                        chapters: videoChapters,
                        videoUrl: toUrl(videoChapters[0]?.videoUrl || ''),
                        videoCount: videos.length || 0,
                        docCount: docs.length || 0,
                    }))
                    pushed = true
                }
                if (docChapters.length > 0) {
                    const firstDocUrl = docChapters[0]?.fileUrl || ''
                    result.push(normalizeCourse({
                        ...base,
                        type: 'document',
                        chapters: docChapters,
                        fileUrl: toUrl(firstDocUrl),
                        videoCount: videos.length || 0,
                        docCount: docs.length || 0,
                    }))
                    pushed = true
                }
                // 若既无视频也无文档，仍生成一个占位课程卡，保证教师端可编辑
                if (!pushed) {
                    result.push(normalizeCourse({
                        ...base,
                        type: 'document',
                        chapters: [],
                        fileUrl: '',
                        videoCount: 0,
                        docCount: 0,
                    }))
                }
            }

            if (result.length) return result
        }
        return []
    } catch (error) {
        return []
    }
}

// 已移除虚拟课程数据，保留纯后端数据展示

/**
 * 规范化课程对象，尤其是 type 字段
 * - 将后端传来的类型统一为 'video' 或 'document'
 * - 如果类型异常，则基于字段做最小推断
 * @param {any} course
 * @returns {any}
 */
function normalizeCourse(course) {
    const rawType = course?.type
    let normalized = 'document'

    if (typeof rawType === 'string') {
        const t = rawType.toLowerCase()
        if (t === 'video') normalized = 'video'
        else if (t === 'document' || t === 'doc' || t === 'file' || t === 'material') normalized = 'document'
    } else if (typeof rawType === 'number') {
        normalized = rawType === 1 ? 'video' : 'document'
    } else {

        if (Array.isArray(course?.chapters) || course?.videoUrl) normalized = 'video'
    }

    return { ...course, type: normalized }
}
