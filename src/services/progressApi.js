import axios from 'axios'
const API_BASE = (import.meta?.env?.VITE_API_BASE_URL || (window?.location?.port === '4173' ? 'http://localhost:9999/api' : '/api'))
const api = axios.create({ baseURL: API_BASE, timeout: 20000 })
api.interceptors.request.use((config) => {
    try {
        const token = localStorage.getItem('token') || localStorage.getItem('userToken')
        if (token) {
            config.headers = { ...(config.headers || {}), Authorization: `Bearer ${token}` }
        }
    } catch {}
    return config
})
/**
 * 远程学习进度 API
 * 说明：为兼容原本本地接口的用法，这里提供相同方法名，内部改为请求后端。
 * 后端需提供以下接口（示例）：
 * - GET    /progress/:courseId                -> { overall: number, videos: { [index]: number } }
 * - PATCH  /progress/:courseId/videos/:index  -> { progress: number, overall: number }
 * - GET    /progress                          -> [{ courseId, overall, lastUpdated }]
 */

export async function getVideoProgress(courseId, chapterIndex) {
    try {
        const res = await api.get(`/progress/${encodeURIComponent(courseId)}`)
        return res?.data?.videos?.[chapterIndex] ?? 0
    } catch {
        return 0
    }
}

export async function setVideoProgress(courseId, chapterIndex, progress) {
    try {
        const res = await api.patch(`/progress/${encodeURIComponent(courseId)}/videos/${encodeURIComponent(chapterIndex)}`, {
            progress
        })
        return res?.data?.overall ?? 0
    } catch {
        return 0
    }
}

export async function getOverallProgress(courseId) {
    try {
        const res = await api.get(`/progress/${encodeURIComponent(courseId)}`)
        return res?.data?.overall ?? 0
    } catch {
        return 0
    }
}

export async function getAllCoursesSummary() {
    try {
        const res = await api.get(`/progress`)
        return Array.isArray(res?.data) ? res.data : []
    } catch {
        return []
    }
}

export async function resetCourseProgress(courseId) {
    try {
        await api.delete(`/progress/${encodeURIComponent(courseId)}`)
    } catch {}
}

/**
 * 心跳/暂停/结束 上报学习时长
 * 建议每 5~10 秒调用一次；暂停、结束或长跳转时补一次。
 */
export async function reportLearningHeartbeat(payload, signal) {
    try {
        const body = {
            courseId: payload.courseId,
            deltaSec: payload.deltaSec,
            eventType: payload.eventType || 'heartbeat',
            videoIndex: payload.videoIndex,
            currentTimeSec: payload.currentTimeSec,
            durationSec: payload.durationSec,
        }
        await api.post(`/progress/course/heartbeat`, body, { signal })
    } catch {}
}

/**
 * 获取课程完成度（统一为 0~1）
 */
export async function getCourseCompletion(courseId, signal) {
    try {
        const res = await api.get(`/progress/course`, { params: { courseId }, signal })
        const value = res?.data?.completionPercentage ?? res?.data
        if (typeof value === 'number') {
            return value > 1 ? Math.min(1, Math.max(0, value / 100)) : Math.min(1, Math.max(0, value))
        }
        return 0
    } catch {
        return 0
    }
}

/**
 * 获取学习时间分布
 * @param {'7d'|'30d'} range 最近一周或最近一月
 * @returns {Promise<{days: string[], video: number[], doc: number[]}>}
 */
export async function getTimeDistribution(range = '7d', signal) {
    try {
        const res = await api.get(`/progress/time-distribution`, { params: { range }, signal })
        const d = res?.data || {}
        if (Array.isArray(d.days) && Array.isArray(d.video) && Array.isArray(d.doc)) {
            return d
        }
    } catch {}
    // 本地兜底：生成等长的轻量数据，避免空图
    const length = range === '30d' ? 30 : 7
    const days = Array.from({ length }, (_, i) => {
        const date = new Date()
        date.setDate(date.getDate() - (length - 1 - i))
        const m = String(date.getMonth() + 1).padStart(2, '0')
        const d = String(date.getDate()).padStart(2, '0')
        return `${m}-${d}`
    })
    const video = days.map(() => Number((0.3 + Math.random() * 1.2).toFixed(2)))
    const doc = days.map(() => Number((0.2 + Math.random() * 0.9).toFixed(2)))
    return { days, video, doc }
}


