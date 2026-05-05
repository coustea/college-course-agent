<template>
  <div class="course-center">
    <div class="center-header">
      <h1 class="page-title">选课中心</h1>
      <p class="subtitle">浏览并选择你感兴趣的课程</p>
    </div>

    <!-- 搜索和筛选工具栏 -->
    <div class="toolbar">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input v-model="searchQuery" placeholder="搜索课程名称或描述..." />
      </div>
      <div class="filter-tabs">
        <button :class="{ active: activeFilter === 'all' }" @click="setFilter('all')">全部</button>
        <button :class="{ active: activeFilter === 'video' }" @click="setFilter('video')">视频课程</button>
        <button :class="{ active: activeFilter === 'document' }" @click="setFilter('document')">文档课程</button>
      </div>
    </div>

    <!-- 课程列表 -->
    <div v-if="loading" class="loading-state">
      <i class="fas fa-spinner fa-spin"></i> 加载中...
    </div>

    <div v-else-if="filteredCourses.length === 0" class="empty-state">
      <img src="https://gw.alipayobjects.com/zos/antfincdn/ZHrcdLPrvN/empty.svg" alt="Empty">
      <p>暂无可选课程</p>
    </div>

    <div v-else class="course-list">
      <div
        v-for="course in filteredCourses"
        :key="course.courseId"
        class="course-item"
      >
        <div class="course-cover">
          <img :src="course.coverImage || defaultCover" alt="课程封面" />
          <span class="type-badge" :class="course.type">
            {{ course.type === 'video' ? '视频' : '文档' }}
          </span>
        </div>

        <div class="course-info">
          <h3 class="course-title">{{ course.courseName }}</h3>
          <p class="course-desc">{{ course.description || '暂无描述' }}</p>

          <div class="course-meta">
            <span><i class="fas fa-user"></i> {{ course.teacherName || '讲师待定' }}</span>
            <span><i class="fas fa-star"></i> {{ course.credits || 0 }} 学分</span>
            <span><i class="fas fa-users"></i> 已选 {{ getEnrollmentCount(course.courseId) }} 人</span>
          </div>

          <div class="course-actions">
            <button
              v-if="isEnrolled(course.courseId)"
              class="btn enrolled"
              disabled
            >
              <i class="fas fa-check"></i> 已选
            </button>
            <button
              v-else
              class="btn enroll"
              @click="handleEnroll(course)"
              :disabled="enrolling"
            >
              <i class="fas fa-plus"></i> {{ enrolling ? '选课中...' : '选课' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPublishedCourses } from '@/services/coursesApi'
import { enrollCourse, getStudentEnrollments } from '@/services/enrollmentApi'

const router = useRouter()
const { proxy } = getCurrentInstance()
const BASE_URL = proxy.$baseUrl
const defaultCover = 'https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png'

// 状态
const loading = ref(false)
const enrolling = ref(false)
const searchQuery = ref('')
const activeFilter = ref('all')
const allCourses = ref([])
const enrolledCourseIds = ref(new Set())
const enrollmentCounts = ref({})

// 筛选后的课程列表
const filteredCourses = computed(() => {
  let list = allCourses.value

  // 类型筛选
  if (activeFilter.value !== 'all') {
    list = list.filter(c => c.type === activeFilter.value)
  }

  // 搜索筛选
  const q = searchQuery.value.toLowerCase().trim()
  if (q) {
    list = list.filter(c =>
      (c.courseName || '').toLowerCase().includes(q) ||
      (c.description || '').toLowerCase().includes(q)
    )
  }

  return list
})

// 加载已发布课程
const loadPublishedCourses = async () => {
  loading.value = true
  try {
    const response = await getPublishedCourses()
    if (response.data.code === 200) {
      const courses = response.data.data || []
      allCourses.value = courses.map(course => ({
        ...course,
        coverImage: course.resourceUrl, // 后端返回 resourceUrl，映射为 coverImage
        type: detectCourseType(course)
      }))
    }
  } catch (error) {
    console.error('加载课程失败:', error)
    ElMessage.error('加载课程失败')
  } finally {
    loading.value = false
  }
}

// 检测课程类型
const detectCourseType = (course) => {
  // 这里可以根据课程的实际内容判断类型
  // 暂时默认返回 'video'
  return 'video'
}

// 加载学生已选课程
const loadMyEnrollments = async () => {
  try {
    const userId = localStorage.getItem('userId')
    if (!userId) return

    const response = await getStudentEnrollments(userId)
    if (response.data.code === 200) {
      const enrollments = response.data.data || []
      // Enrollment 结构: { enrollmentId, course: { courseId, ... }, ... }
      enrolledCourseIds.value = new Set(enrollments.map(e => e.course?.courseId).filter(Boolean))
    }
  } catch (error) {
    console.error('加载已选课程失败:', error)
  }
}

// 检查是否已选
const isEnrolled = (courseId) => {
  return enrolledCourseIds.value.has(courseId)
}

// 获取选课人数
const getEnrollmentCount = (courseId) => {
  return enrollmentCounts.value[courseId] || 0
}

// 选课操作
const handleEnroll = async (course) => {
  const userId = localStorage.getItem('userId')
  if (!userId) {
    ElMessage.warning('请先登录')
    return
  }

  enrolling.value = true
  try {
    const response = await enrollCourse(userId, course.courseId)
    if (response.data.code === 200) {
      ElMessage.success(`成功选课：${course.courseName}`)
      // 更新已选课程列表
      enrolledCourseIds.value.add(course.courseId)
    } else {
      ElMessage.error(response.data.message || '选课失败')
    }
  } catch (error) {
    console.error('选课失败:', error)
    ElMessage.error(error.response?.data?.message || '选课失败')
  } finally {
    enrolling.value = false
  }
}

// 设置筛选
const setFilter = (filter) => {
  activeFilter.value = filter
}

onMounted(() => {
  loadPublishedCourses()
  loadMyEnrollments()
})
</script>

<style scoped>
.course-center {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.center-header {
  background: white;
  padding: 24px;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  margin-bottom: 24px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px 0;
  color: #111827;
}

.subtitle {
  color: #6b7280;
  font-size: 14px;
  margin: 0;
}

.toolbar {
  background: white;
  padding: 16px 24px;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.search-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  padding: 8px 16px;
  display: flex;
  align-items: center;
  width: 320px;
  transition: all 0.3s;
}

.search-box:focus-within {
  background: white;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.search-box input {
  border: none;
  outline: none;
  margin-left: 8px;
  font-size: 14px;
  width: 100%;
  background: transparent;
}

.search-box i {
  color: #94a3b8;
}

.filter-tabs {
  background: #f1f5f9;
  padding: 4px;
  border-radius: 10px;
  display: flex;
  gap: 2px;
}

.filter-tabs button {
  background: transparent;
  border: none;
  padding: 6px 16px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  transition: all 0.2s;
}

.filter-tabs button.active {
  background: white;
  color: #3b82f6;
  font-weight: 600;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.loading-state, .empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #9ca3af;
}

.empty-state img {
  width: 200px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.course-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.course-item {
  background: white;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  gap: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  transition: all 0.3s;
}

.course-item:hover {
  box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1);
  transform: translateY(-2px);
}

.course-cover {
  position: relative;
  width: 200px;
  height: 120px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
}

.course-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.type-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  color: white;
  backdrop-filter: blur(4px);
}

.type-badge.video {
  background: rgba(59, 130, 246, 0.9);
}

.type-badge.document {
  background: rgba(16, 185, 129, 0.9);
}

.course-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.course-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: #111827;
}

.course-desc {
  font-size: 13px;
  color: #6b7280;
  margin: 0 0 12px 0;
  line-height: 1.5;
  flex: 1;
}

.course-meta {
  display: flex;
  gap: 20px;
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 12px;
}

.course-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.course-actions {
  display: flex;
  justify-content: flex-end;
}

.btn {
  padding: 8px 24px;
  border-radius: 8px;
  border: none;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.btn.enroll {
  background: #3b82f6;
  color: white;
}

.btn.enroll:hover:not(:disabled) {
  background: #2563eb;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.btn.enroll:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn.enrolled {
  background: #10b981;
  color: white;
  cursor: default;
}

@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-box {
    width: 100%;
  }

  .course-item {
    flex-direction: column;
  }

  .course-cover {
    width: 100%;
    height: 160px;
  }
}
</style>
