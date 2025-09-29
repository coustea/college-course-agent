// 首页课程接口服务
import axios from 'axios'

// 将后端 resourceUrl/相对路径转换为可访问的完整 URL
// 按需把所有相对地址统一拼上后端主机前缀
const BACKEND_HOST = (() => {
    try {
        const p = window?.location?.port
        if (p === '4173' || p === '5173') return 'http://192.168.52.75:9999'
    } catch (e) { console.error(e) }
    return 'http://192.168.52.75:9999'
})()
const toUrl = (u) => {
    if (!u) return ''
    const s = String(u)
    if (/^(https?:|data:|blob:)/.test(s)) return s
    return `${BACKEND_HOST.replace(/\/$/, '')}/${s.replace(/^\//, '')}`
}

/**
 * 获取首页所有课程数据
 * 如果没有连接后端或请求失败，则返回假数据
 * @param {AbortSignal} [signal] - 取消请求的信号
 * @returns {Promise<Array>} 课程数据数组
 */
export async function fetchHomeCourses(signal) {
    try {
        // 使用相对路径，走 Axios baseURL，便于部署与环境切换
        const response = await axios.get('http://192.168.52.75:9999/api/course/list', { signal })
        const payload = response?.data
        console.log('首页课程数据', payload)
        // 兼容两种返回：直接数组，或 { code, data: [] }
        const list = Array.isArray(payload)
            ? payload
            : (Array.isArray(payload?.data) ? payload.data : null)

        if (Array.isArray(list) && list.length) {
            const result = []
            for (const c of list) {
                const base = {
                    id: c.courseId,
                    title: c.courseName ?? '未命名课程',
                    description: c.description ?? '',
                    image: toUrl(c.resourceUrl)  || 'https://images.unsplash.com/photo-1501504905252-473c47e087f8?auto=format&fit=crop&w=1200&q=80',
                    startDate: c.startDate || '',
                    endDate: c.endDate || '',
                    teacher: c.teacher.name || '无',
                    teacherId: c.teacher.id || null,
                    category: c.courseCode || c.category || '',
                }

                const videos = Array.isArray(c.videos) ? c.videos : []
                const docs = Array.isArray(c.documents) ? c.documents : []

                const videoChapters = videos.map((v, i) => ({
                    title: v.title || v.name || `第${i + 1}集`,
                    duration: v.duration,
                    videoUrl: toUrl(v.url || v.videoUrl || v.resourceUrl || '')
                }))
                const docChapters = docs.map((d, i) => ({
                    title: d.title || d.name || d.docTitle || `第${i + 1}节`,
                    duration: d.duration || d.pages || '',
                    // 兼容后端字段：docUrl / fileUrl / url / resourceUrl
                    fileUrl: toUrl(d.docUrl || d.fileUrl || d.url || d.resourceUrl || ''),
                    html: d.html || d.content || ''
                }))

                // B 方案：两种类型都有时视为视频类
                const isVideo = videos.length > 0
                const type = isVideo ? 'video' : 'document'
                const chapters = isVideo ? videoChapters : docChapters

                result.push(normalizeCourse({
                    ...base,
                    type,
                    chapters,
                    videoUrl: isVideo ? toUrl(videoChapters[0]?.videoUrl || '') : '',
                    videoCount: videos.length || 0,
                    docCount: docs.length || 0,
                    // 文档课程兜底：若无章节但有 resourceUrl，则作为单文档
                    fileUrl: (!isVideo && (!chapters || chapters.length === 0) && base.image) ? toUrl(base.image) : ''
                }))
            }

            // 成功时优先展示后端拼装的卡片
            if (result.length) return result
        }
        // 非法结构或空数组：退回到虚拟课程
        return getMockCourses()
    } catch (error) {
        // 请求异常：继续展示虚拟课程
        return getMockCourses()
    }
}

/**
 * 获取假数据课程列表
 * 现有假数据结构,没有接后端使用用来观察前端页面效果
 * @returns {Array} 假数据课程数组
 */
function getMockCourses() {
    return [
        {
            id: 1,
            title: "思想道德修养与法律基础",
            description: "本课程主要讲解马克思主义基本原理和中国特色社会主义理论体系",
            type: "document",
            image: "https://images.unsplash.com/photo-1501504905252-473c47e087f8?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=800&q=80",
            date: "2024-09-15",
            duration: "12章",
            category: "思想政治",
            url: "",
        },
        {
            id: 2,
            title: "中国近现代史纲要",
            description: "全面介绍中国从1840年至今的历史发展进程和重大历史事件",
            type: "video",
            image: "https://images.unsplash.com/photo-1532094349884-543bc11b234d?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=800&q=80",
            date: "2024-09-10",
            duration: "45分钟",
            category: "历史",
            chapters: [
                {
                    title: "第一部分：近代史开端",
                    children: [
                        { title: '第一章：鸦片战争', duration: '15:00', videoUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4' },
                        { title: '第二章：太平天国运动', duration: '12:00', videoUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4' }
                    ]
                },
                {
                    title: "第二部分：辛亥革命与新民主主义",
                    children: [
                        { title: '第三章：辛亥革命', duration: '18:00', videoUrl: 'https://example.com/video3.mp4' },
                        { title: '第四章：新民主主义革命', duration: '20:00', videoUrl: 'https://example.com/video4.mp4' }
                    ]
                }
            ],
        },
        {
            id: 3,
            title: "马克思主义基本原理",
            description: "深入学习马克思主义哲学、政治经济学和科学社会主义的基本原理",
            type: "document",
            image: "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=800&q=80",
            date: "2025-09-05",
            duration: "8章",
            category: "哲学",
        },
        {
            id: 4,
            title: "毛泽东思想和中国特色社会主义理论体系概论",
            description: "系统学习毛泽东思想和中国特色社会主义理论体系的形成与发展",
            type: "video",
            image: "https://images.unsplash.com/photo-1562813733-b31f71025d54?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=800&q=80",
            date: "2025-09-01",
            duration: "60分钟",
            category: "理论与实践",
            chapters: [
                {
                    title: "毛泽东思想",
                    children: [
                        {
                            title: "毛泽东思想形成与发展",
                            children: [
                                { title: '1.1 毛泽东思想的历史背景', duration: '12:00', videoUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4' },
                                { title: '1.2 毛泽东思想的主要内容', duration: '15:00', videoUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4' }
                            ]
                        },
                        {
                            title: "毛泽东思想活的灵魂",
                            children: [
                                { title: '2.1 实事求是', duration: '10:00', videoUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4' },
                                { title: '2.2 群众路线', duration: '8:00', videoUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4' }
                            ]
                        }
                    ]
                },
                {
                    title: "中国特色社会主义理论体系",
                    children: [
                        { title: '邓小平理论', duration: '18:00', videoUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4' },
                        { title: '三个代表重要思想', duration: '16:00', videoUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4' },
                        { title: '科学发展观', duration: '14:00', videoUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4' }
                    ]
                }
            ],
        },
        {
            id: 5,
            title: "形势与政策",
            description: "分析当前国际国内形势，解读党和国家重大方针政策",
            type: "document",
            image: "https://images.unsplash.com/photo-1450101499163-c8848c66ca85?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=800&q=80",
            date: "2025-08-28",
            duration: "6章",
            category: "时政"
        },
        {
            id: 6,
            title: "习近平新时代中国特色社会主义思想",
            description: "深入学习习近平新时代中国特色社会主义思想的核心要义和实践要求",
            type: "video",
            image: "https://images.unsplash.com/photo-1589652717521-10c0d092dea9?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=800&q=80",
            date: "2025-08-25",
            duration: "50分钟",
            category: "思想理论"
        },
        // 新增：带声音的视频课程用于本地测试音量与键盘控制
        {
            id: 7,
            title: "示例带声音视频（测试音量与控制）",
            description: "用于验证音量调节、空格播放/暂停、方向键快退限制等功能",
            type: "video",
            image: "https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=800&q=80",
            date: "2025-09-16",
            duration: "10分钟",
            category: "功能测试",
            // 单视频课程：走 fallbackSrc
            videoUrl: "https://www.w3schools.com/html/mov_bbb.mp4"
        }
    ]
}

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
