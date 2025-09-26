// 首页课程接口服务
import { api } from './coursesApi.js'

/**
 * 获取首页所有课程数据
 * 如果没有连接后端或请求失败，则返回假数据
 * @param {AbortSignal} [signal] - 取消请求的信号
 * @returns {Promise<Array>} 课程数据数组
 */
export async function fetchHomeCourses(signal) {
  try {
    // 尝试从后端获取课程数据
    const response = await api.get('/home/courses', { signal })

    if (response.data && Array.isArray(response.data)) {
      return response.data
    }

    // 如果后端返回的数据格式不正确，使用假数据
    return getMockCourses()
  } catch (error) {
    console.warn('获取首页课程数据失败:', error.message)
    // 请求失败时返回假数据
    return getMockCourses()
  }
}

/**
 * 获取假数据课程列表
 * 现有假数据结构,没有接后端使用
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
      html: "<h2>课程简介</h2><p>示例文档内容。</p>"
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
            { title: '第一章：鸦片战争', duration: '15:00', videoUrl: 'https://example.com/video1.mp4' },
            { title: '第二章：太平天国运动', duration: '12:00', videoUrl: 'https://example.com/video2.mp4' }
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
      html: "<p>文档课程示例内容。</p>"
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
                { title: '1.1 毛泽东思想的历史背景', duration: '12:00', videoUrl: 'https://example.com/video5.mp4' },
                { title: '1.2 毛泽东思想的主要内容', duration: '15:00', videoUrl: 'https://example.com/video6.mp4' }
              ]
            },
            {
              title: "毛泽东思想活的灵魂",
              children: [
                { title: '2.1 实事求是', duration: '10:00', videoUrl: 'https://example.com/video7.mp4' },
                { title: '2.2 群众路线', duration: '8:00', videoUrl: 'https://example.com/video8.mp4' }
              ]
            }
          ]
        },
        {
          title: "中国特色社会主义理论体系",
          children: [
            { title: '邓小平理论', duration: '18:00', videoUrl: 'https://example.com/video9.mp4' },
            { title: '三个代表重要思想', duration: '16:00', videoUrl: 'https://example.com/video10.mp4' },
            { title: '科学发展观', duration: '14:00', videoUrl: 'https://example.com/video11.mp4' }
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
    }
  ]
}

/**
 * 根据课程类型筛选课程
 * @param {Array} courses - 课程数组
 * @param {string} type - 课程类型 ('all', 'video', 'document')
 * @returns {Array} 筛选后的课程数组
 */
export function filterCoursesByType(courses, type) {
  if (type === 'all') {
    return courses
  }
  return courses.filter(course => course.type === type)
}
