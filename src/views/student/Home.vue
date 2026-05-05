<template>
  <div class="student-dashboard">
    <!-- 顶部欢迎与导航区 -->
    <div class="dashboard-header">
      <div class="header-content">
        <div class="welcome-section">
          <h1 class="page-title">学习中心</h1>
          <p class="welcome-text">
            {{ getTimeState() }}，<span class="highlight">{{ userName || '同学' }}</span>。
            <span>保持好奇，探索未知！</span>
          </p>
        </div>
        <div class="action-section">
          <div class="notification-badge">
            <el-icon :size="20"><Bell /></el-icon>
            <span class="dot"></span>
          </div>
          <el-button type="primary" class="ai-btn" round>
            <el-icon class="mr-1"><ChatDotRound /></el-icon>
            AI 学习助手
          </el-button>
        </div>
      </div>
    </div>

    <!-- 核心指标统计区 -->
    <div class="stats-grid">
      <div class="stat-card blue">
        <div class="stat-icon">
          <i class="fas fa-book-reader"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ courses.length }}</div>
          <div class="stat-label">在修课程</div>
        </div>
      </div>

      <div class="stat-card green">
        <div class="stat-icon">
          <i class="fas fa-clock"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ studentStatistics.weeklyStudyHours.toFixed(1) }}<span class="unit">h</span></div>
          <div class="stat-label">本周学习</div>
        </div>
      </div>

      <div class="stat-card purple">
        <div class="stat-icon">
          <i class="fas fa-tasks"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ todos.filter(t => !t.completed).length }}</div>
          <div class="stat-label">待办任务</div>
        </div>
      </div>

      <div class="stat-card orange">
        <div class="stat-icon">
          <i class="fas fa-fire"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ studentStatistics.consecutiveDays }}<span class="unit">天</span></div>
          <div class="stat-label">连续打卡</div>
        </div>
      </div>
    </div>

    <!-- 主体内容布局 -->
    <div class="dashboard-layout">
      <!-- 左侧：课程学习区 -->
      <div class="main-column">

        <!-- 1. 课程列表工具栏 (已提升至顶部) -->
        <div class="section-toolbar">
          <h3 class="section-title"><i class="fas fa-layer-group"></i> 课程中心</h3>
          <div class="toolbar-actions">
            <!-- 搜索框优化 -->
            <div class="search-box">
              <i class="fas fa-search"></i>
              <input v-model="searchQuery" placeholder="搜索感兴趣的课程..." />
            </div>

            <!-- 筛选按钮优化 -->
            <div class="filter-tabs">
              <button :class="{ active: activeFilter === 'all' }" @click="setFilter('all')">全部</button>
              <button :class="{ active: activeFilter === 'video' }" @click="setFilter('video')">视频</button>
              <button :class="{ active: activeFilter === 'document' }" @click="setFilter('document')">文档</button>
            </div>
          </div>
        </div>

        <!-- 2. 推荐/最近学习 (Hero Course) -->
        <!-- 仅在无搜索关键词且显示全部时展示，避免干扰搜索结果 -->
        <div v-if="filteredCourses.length > 0 && !searchQuery && activeFilter === 'all'" class="hero-course-card" @click="onCardClick(filteredCourses[0])">
          <div class="hero-content">
            <span class="badge">最近学习</span>
            <h2 class="hero-title">{{ filteredCourses[0].title }}</h2>
            <p class="hero-desc">{{ filteredCourses[0].description || '暂无描述' }}</p>
            <div class="hero-meta">
              <span><i class="fas fa-chalkboard-teacher"></i> {{ filteredCourses[0].teacher || '讲师待定' }}</span>
              <span><i class="far fa-calendar-alt"></i> {{ formatDate(filteredCourses[0].startDate) }}</span>
            </div>
            <button class="continue-btn">
              <i class="fas fa-play"></i> 继续学习
            </button>
          </div>
          <div class="hero-image">
            <img :src="filteredCourses[0].image || defaultCover" alt="Course Cover">
            <div class="overlay"></div>
          </div>
        </div>

        <!-- 3. 课程网格 -->
        <div class="courses-grid">
          <div v-if="loading" class="loading-state">
            <i class="fas fa-spinner fa-spin"></i> 加载中...
          </div>

          <div v-else-if="filteredCourses.length === 0" class="empty-state">
            <img src="https://gw.alipayobjects.com/zos/antfincdn/ZHrcdLPrvN/empty.svg" alt="Empty">
            <p>暂无相关课程</p>
          </div>

          <div
            v-else
            v-for="course in filteredCourses"
            :key="course.id"
            class="course-card"
            @click="onCardClick(course)"
          >
            <div class="card-cover">
              <img :src="course.image || defaultCover" @error="handleImgError" />
              <span class="type-tag" :class="course.type">
                {{ course.type === 'video' ? '视频' : '文档' }}
              </span>
              <div class="play-overlay">
                <i class="fas fa-play-circle"></i>
              </div>
            </div>
            <div class="card-body">
              <h4 class="course-name" :title="course.title">{{ course.title }}</h4>
              <div class="course-info">
                <span><i class="fas fa-user"></i> {{ course.teacher || '待定' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：个人任务与快捷入口 -->
      <div class="side-column">
        <!-- 学习计划/待办 -->
        <div class="side-card todo-card">
          <div class="card-header">
            <h3><i class="fas fa-list-ul"></i> 学习计划</h3>
            <button class="add-btn" @click="showAddTodoModal = true"><i class="fas fa-plus"></i></button>
          </div>
          <div class="todo-list">
            <div v-if="todos.length === 0" class="empty-tips">暂无计划，添加一个吧~</div>
            <div
              v-for="todo in todos"
              :key="todo.id"
              class="todo-item"
              :class="{ completed: todo.completed }"
            >
              <label class="checkbox-wrapper">
                <input type="checkbox" v-model="todo.completed" @change="saveTodos">
                <span class="checkmark"></span>
              </label>
              <span class="todo-text">{{ todo.title }}</span>
              <button class="del-btn" @click="deleteTodo(todo.id)"><i class="fas fa-times"></i></button>
            </div>
          </div>
        </div>

        <!-- 快捷入口 -->
        <div class="side-card quick-actions">
          <div class="card-header">
            <h3>快捷入口</h3>
          </div>
          <div class="action-grid">
            <div class="action-item">
              <div class="icon-box bg-blue"><i class="fas fa-history"></i></div>
              <span>观看历史</span>
            </div>
            <div class="action-item">
              <div class="icon-box bg-red"><i class="fas fa-heart"></i></div>
              <span>我的收藏</span>
            </div>
            <div class="action-item" @click="router.push('/student/mistake-book')">
              <div class="icon-box bg-green"><i class="fas fa-book"></i></div>
              <span>错题本</span>
            </div>
            <div class="action-item">
              <div class="icon-box bg-orange"><i class="fas fa-user-cog"></i></div>
              <span>账号设置</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 弹窗：新建待办 -->
    <el-dialog v-model="showAddTodoModal" title="添加学习计划" width="400px" class="custom-dialog">
      <el-input v-model="newTodoTitle" placeholder="例如：看完第一章视频" @keyup.enter="addTodo" />
      <template #footer>
        <el-button @click="showAddTodoModal = false">取消</el-button>
        <el-button type="primary" @click="addTodo" :disabled="!newTodoTitle">添加</el-button>
      </template>
    </el-dialog>

    <!-- 组件：播放器与文档查看器 -->
    <CoursePlayer
      v-if="activeCourse"
      v-model="playerVisible"
      :course-id="activeCourse.id"
      :title="activeCourse.title"
      :chapters="activeCourse.chapters || []"
      :fallback-src="activeCourse.videoUrl || ''"
      :enable-questions="true"
      @progress="onOverallProgress"
    />

    <DocumentViewer
      v-model="docVisible"
      :id="activeDoc?.id"
      :course-id="activeDoc?.courseId || activeDoc?.id"
      :title="activeDoc?.title || '文档课程'"
      :file-url="activeDoc?.fileUrl || activeDoc?.url || ''"
      :html-content="activeDoc?.html || ''"
      :chapters="activeDoc?.chapters || []"
      :course-title="activeDoc?.title || ''"
      :chapter-index="1"
      :progress="0"
      :image="activeDoc?.image || ''"
      :enable-questions="true"
      :duration="activeDoc?.duration || ''"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import axios from 'axios'
import { Bell, ChatDotRound } from '@element-plus/icons-vue'
import CoursePlayer from '/src/components/CoursePlayer.vue'
import DocumentViewer from '/src/components/DocumentViewer.vue'
import { fetchHomeCourses } from '/src/services/homeCoursesApi'
import { fetchStudentStatistics } from '/src/services/studentStatisticsApi'

// 基础配置
const router = useRouter()
const { proxy } = getCurrentInstance()
const BASE_URL = proxy.$baseUrl
const defaultCover = 'https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png'
const handleImgError = (e) => { e.target.src = defaultCover }

// 状态
const userName = ref('')
const courses = ref([])
const loading = ref(false)
const searchQuery = ref('')
const activeFilter = ref('all')

// 学生统计数据
const studentStatistics = ref({
  courseCount: 0,
  weeklyStudyHours: 0,
  consecutiveDays: 0
})

// 待办事项状态
const showAddTodoModal = ref(false)
const newTodoTitle = ref('')
const todos = ref([])

// 获取时间状态
const getTimeState = () => {
  const h = new Date().getHours()
  if (h < 5) return '夜深了'
  if (h < 11) return '上午好'
  if (h < 13) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
}

const formatDate = (input) => {
  if (!input) return '待定'
  const d = new Date(input)
  if (isNaN(d.getTime())) return input
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

// 初始化
const getStudentInfo = async () => {
  try {
    const userId = localStorage.getItem('userId')
    if (!userId) return
    const res = await axios.post(`${BASE_URL}/student/by-id`, { userId }, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
        'content-type': 'multipart/form-data'
      }
    })
    if (res.data.code === 200) userName.value = res.data.data.name
  } catch (e) { console.error(e) }
}

const loadCourses = async () => {
  loading.value = true
  try {
    const list = await fetchHomeCourses()
    courses.value = Array.isArray(list) ? list : []
  } catch { courses.value = [] }
  finally { loading.value = false }
}

const loadTodos = () => {
  const saved = localStorage.getItem('studentTodos')
  todos.value = saved ? JSON.parse(saved) : [
    { id: 1, title: '完成马克思主义基本原理课程', completed: false },
    { id: 2, title: '复习思想道德与法治笔记', completed: true }
  ]
}

const loadStudentStatistics = async () => {
  try {
    const userId = localStorage.getItem('userId')
    if (!userId) return
    const stats = await fetchStudentStatistics(userId)
    studentStatistics.value = stats
  } catch (e) {
    console.error('加载学生统计数据失败:', e)
  }
}

// 筛选逻辑
const filteredCourses = computed(() => {
  let list = courses.value
  if (activeFilter.value !== 'all') {
    list = list.filter(c => c.type === activeFilter.value)
  }
  const q = searchQuery.value.toLowerCase().trim()
  if (q) {
    list = list.filter(c =>
      (c.title || '').toLowerCase().includes(q) ||
      (c.description || '').toLowerCase().includes(q)
    )
  }
  return list
})

const setFilter = (val) => activeFilter.value = val

// 待办操作
const saveTodos = () => localStorage.setItem('studentTodos', JSON.stringify(todos.value))
const addTodo = () => {
  if (!newTodoTitle.value.trim()) return
  todos.value.unshift({ id: Date.now(), title: newTodoTitle.value, completed: false })
  saveTodos()
  newTodoTitle.value = ''
  showAddTodoModal.value = false
}
const deleteTodo = (id) => {
  todos.value = todos.value.filter(t => t.id !== id)
  saveTodos()
}

// 播放逻辑
const playerVisible = ref(false)
const docVisible = ref(false)
const activeCourse = ref(null)
const activeDoc = ref(null)

const onCardClick = (course) => {
  if (course.type === 'video') {
    activeCourse.value = course
    playerVisible.value = true
  } else {
    activeDoc.value = ensureDocChapters(course)
    docVisible.value = true
  }
}

const onOverallProgress = () => {}

function ensureDocChapters(course) {
  if (!course || course.type !== 'document') return course
  if (Array.isArray(course.chapters) && course.chapters.length > 0) return course

  const urls = []
  if (Array.isArray(course.documents)) {
    course.documents.forEach((d, i) => {
      const u = d?.fileUrl || d?.url || d?.docUrl
      if (u) urls.push({ title: d.title || `第${i + 1}章`, fileUrl: u })
    })
  }
  if (urls.length === 0) {
    const u = course.fileUrl || course.url || course.docUrl
    if (u) return { ...course, chapters: [{ title: course.title, fileUrl: u }] }
  } else {
    return { ...course, chapters: urls }
  }
  return course
}

onMounted(() => {
  getStudentInfo()
  loadCourses()
  loadTodos()
  loadStudentStatistics()
})
</script>

<style scoped>
/* 基础变量与布局 */
.student-dashboard {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  min-height: 100vh;
  background-color: #f5f7fa;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: #1f2937;
}

/* 1. 顶部 Header */
.dashboard-header {
  background: white;
  padding: 20px 24px;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  margin-bottom: 24px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 4px 0;
  color: #111827;
}

.welcome-text {
  color: #6b7280;
  font-size: 14px;
}

.highlight {
  color: #3b82f6;
  font-weight: 600;
  margin: 0 4px;
}

.action-section {
  display: flex;
  align-items: center;
  gap: 20px;
}

.notification-badge {
  position: relative;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  color: #6b7280;
}

.notification-badge:hover { background: #e5e7eb; color: #3b82f6; }

.dot {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 8px;
  height: 8px;
  background: #ef4444;
  border-radius: 50%;
  border: 2px solid #f3f4f6;
}

.ai-btn {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border: none;
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.3);
}

/* 2. 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  padding: 24px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  transition: transform 0.2s;
}

.stat-card:hover { transform: translateY(-3px); box-shadow: 0 10px 15px -3px rgba(0,0,0,0.05); }

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-right: 16px;
}

.stat-value { font-size: 28px; font-weight: 700; line-height: 1; margin-bottom: 4px; }
.stat-value .unit { font-size: 14px; font-weight: 500; margin-left: 4px; opacity: 0.7; }
.stat-label { font-size: 13px; color: #6b7280; }

.blue .stat-icon { background: #eff6ff; color: #3b82f6; }
.blue .stat-value { color: #1e40af; }
.green .stat-icon { background: #f0fdf4; color: #10b981; }
.green .stat-value { color: #065f46; }
.purple .stat-icon { background: #f5f3ff; color: #8b5cf6; }
.purple .stat-value { color: #5b21b6; }
.orange .stat-icon { background: #fff7ed; color: #f97316; }
.orange .stat-value { color: #9a3412; }

/* 3. 主体布局 (2+1) */
.dashboard-layout {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 24px;
  align-items: start;
}

/* 左侧栏 */
.main-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* Hero Course (最近学习) */
.hero-course-card {
  background: linear-gradient(135deg, #1e293b, #0f172a);
  border-radius: 20px;
  padding: 32px;
  display: flex;
  justify-content: space-between;
  color: white;
  position: relative;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 10px 25px -5px rgba(15, 23, 42, 0.3);
}

.hero-content {
  position: relative;
  z-index: 2;
  max-width: 60%;
}

.hero-course-card .badge {
  background: rgba(255,255,255,0.15);
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: #fbbf24;
  margin-bottom: 12px;
  display: inline-block;
}

.hero-title { font-size: 24px; margin: 0 0 12px 0; font-weight: 700; }
.hero-desc { color: #94a3b8; font-size: 14px; margin-bottom: 24px; line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.hero-meta { display: flex; gap: 20px; font-size: 13px; color: #cbd5e1; margin-bottom: 24px; }
.continue-btn { background: #3b82f6; color: white; border: none; padding: 10px 24px; border-radius: 30px; font-weight: 600; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; gap: 8px; }
.continue-btn:hover { background: #2563eb; transform: scale(1.05); }

.hero-image {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: 50%;
  mask-image: linear-gradient(to right, transparent, black);
  -webkit-mask-image: linear-gradient(to right, transparent, black);
}
.hero-image img { width: 100%; height: 100%; object-fit: cover; }

/* 课程工具栏 */
.section-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  background: white;
  padding: 16px 24px;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04); /* 柔和阴影 */
  border: 1px solid rgba(229, 231, 235, 0.5); /* 极细微边框 */
}

.section-title { font-size: 18px; font-weight: 600; margin: 0; display: flex; align-items: center; gap: 8px; color: #111827; }
.toolbar-actions { display: flex; gap: 16px; flex-wrap: wrap; align-items: center; }

/* 搜索框 - 胶囊风格 */
.search-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 999px; /* 胶囊圆角 */
  padding: 8px 16px;
  display: flex;
  align-items: center;
  transition: all 0.3s ease;
  width: 220px;
}

.search-box:focus-within {
  background: white;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1); /* 聚焦光环 */
  width: 280px; /* 展开动画 */
}

.search-box input { border: none; outline: none; margin-left: 8px; font-size: 14px; width: 100%; background: transparent; color: #334155; }
.search-box input::placeholder { color: #cbd5e1; }
.search-box i { color: #94a3b8; font-size: 14px; }

/* 筛选按钮 - 分段控制器风格 */
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
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.filter-tabs button:hover:not(.active) {
  color: #475569;
  background: rgba(255,255,255,0.5);
}

.filter-tabs button.active {
  background: white;
  color: #3b82f6;
  font-weight: 600;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

/* 课程网格 */
.courses-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
}

.course-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  cursor: pointer;
  transition: all 0.3s;
}

.course-card:hover { transform: translateY(-4px); box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1); }

.card-cover { position: relative; height: 120px; overflow: hidden; }
.card-cover img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.5s; }
.course-card:hover .card-cover img { transform: scale(1.1); }

.type-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  color: white;
  background: rgba(0,0,0,0.6);
  backdrop-filter: blur(4px);
}
.type-tag.video { background: rgba(59, 130, 246, 0.9); }
.type-tag.document { background: rgba(16, 185, 129, 0.9); }

.play-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 32px;
  opacity: 0;
  transition: opacity 0.3s;
}
.course-card:hover .play-overlay { opacity: 1; }

.card-body { padding: 12px; }
.course-name { font-size: 14px; font-weight: 600; margin: 0 0 8px 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.course-info { font-size: 12px; color: #9ca3af; }

/* 右侧栏 */
.side-column { display: flex; flex-direction: column; gap: 24px; }

.side-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.side-card .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.side-card h3 { font-size: 16px; font-weight: 600; margin: 0; display: flex; align-items: center; gap: 8px; }
.add-btn { background: #f3f4f6; border: none; width: 28px; height: 28px; border-radius: 6px; color: #6b7280; cursor: pointer; transition: all 0.2s; }
.add-btn:hover { background: #e5e7eb; color: #3b82f6; }

/* 待办列表 */
.todo-list { max-height: 300px; overflow-y: auto; }
.todo-item { display: flex; align-items: center; gap: 10px; padding: 10px 0; border-bottom: 1px solid #f9fafb; transition: all 0.2s; }
.todo-item.completed .todo-text { text-decoration: line-through; color: #9ca3af; }
.todo-text { flex: 1; font-size: 14px; color: #374151; }
.del-btn { background: none; border: none; color: #d1d5db; cursor: pointer; opacity: 0; transition: all 0.2s; }
.todo-item:hover .del-btn { opacity: 1; }
.del-btn:hover { color: #ef4444; }

.checkbox-wrapper { position: relative; cursor: pointer; width: 18px; height: 18px; }
.checkbox-wrapper input { opacity: 0; width: 0; height: 0; }
.checkmark { position: absolute; top: 0; left: 0; height: 18px; width: 18px; border: 2px solid #d1d5db; border-radius: 4px; }
.checkbox-wrapper input:checked ~ .checkmark { background: #3b82f6; border-color: #3b82f6; }
.checkmark:after { content: ""; position: absolute; display: none; left: 5px; top: 1px; width: 4px; height: 9px; border: solid white; border-width: 0 2px 2px 0; transform: rotate(45deg); }
.checkbox-wrapper input:checked ~ .checkmark:after { display: block; }

/* 快捷入口 */
.action-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.action-item { background: #f9fafb; padding: 16px; border-radius: 12px; display: flex; flex-direction: column; align-items: center; gap: 8px; cursor: pointer; transition: all 0.2s; }
.action-item:hover { background: #f3f4f6; transform: translateY(-2px); }
.action-item span { font-size: 12px; color: #4b5563; }
.icon-box { width: 40px; height: 40px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 18px; }
.bg-blue { background: #eff6ff; color: #3b82f6; }
.bg-red { background: #fef2f2; color: #ef4444; }
.bg-green { background: #f0fdf4; color: #10b981; }
.bg-orange { background: #fff7ed; color: #f97316; }

/* 响应式 */
@media (max-width: 1024px) {
  .dashboard-layout { grid-template-columns: 1fr; }
  .hero-course-card { flex-direction: column; }
  .hero-content { max-width: 100%; }
  .hero-image { display: none; }
}

@media (max-width: 768px) {
  .header-content { flex-direction: column; align-items: flex-start; gap: 16px; }
  .section-toolbar { flex-direction: column; align-items: stretch; padding: 12px; }
  .toolbar-actions { flex-direction: column; align-items: stretch; }
  .search-box { width: 100%; }
  .search-box:focus-within { width: 100%; }
  .filter-tabs { justify-content: space-between; }
  .filter-tabs button { flex: 1; }
}
</style>
