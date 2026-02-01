<template>
  <div class="teacher-home">
    <!-- 顶部导航区 -->
    <div class="page-header">
      <div class="header-content">
        <div class="welcome-section">
          <h2 class="page-title">工作台</h2>
          <p class="welcome-text">
            {{ getTimeState() }}，<span class="highlight">{{ teacherName }} 老师</span>。
            <span v-if="pendingApplicationsCount > 0">您有 {{ pendingApplicationsCount }} 个分组申请待审批。</span>
            <span v-else>今天也是元气满满的一天！</span>
          </p>
        </div>
        <div class="action-section">
          <div class="notification-badge" @click="goToGroups" :class="{ 'has-new': pendingApplicationsCount > 0 }">
            <el-badge :value="pendingApplicationsCount" :hidden="pendingApplicationsCount === 0" :max="99">
              <div class="icon-box">
                <el-icon :size="20"><Bell /></el-icon>
              </div>
            </el-badge>
          </div>
          <el-button type="primary" class="ai-btn" @click="goToAIChat" round>
            <el-icon class="mr-1"><ChatDotRound /></el-icon>
            AI 教学助手
          </el-button>
        </div>
      </div>
    </div>

    <!-- 核心统计指标 -->
    <div class="stats-grid">
      <div class="stat-card blue">
        <div class="stat-icon">
          <i class="fas fa-book-open"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.courseCount }}</div>
          <div class="stat-label">课程总数</div>
        </div>
      </div>

      <div class="stat-card green">
        <div class="stat-icon">
          <i class="fas fa-user-graduate"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.studentCount }}</div>
          <div class="stat-label">学生总数</div>
        </div>
      </div>

      <div class="stat-card purple">
        <div class="stat-icon">
          <i class="fas fa-chart-line"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.completionRate }}<span class="unit">%</span></div>
          <div class="stat-label">平均完成率</div>
        </div>
      </div>

      <div class="stat-card orange">
        <div class="stat-icon">
          <i class="fas fa-users"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.assignmentCount }}</div>
          <div class="stat-label">学习小组</div>
        </div>
      </div>
    </div>

    <!-- 主体内容区 -->
    <div class="dashboard-grid">
      <!-- 左侧主栏：图表与课程 -->
      <div class="main-column">
        <!-- 学情分析图表 -->
        <div class="dashboard-card chart-card">
          <div class="card-header">
            <h3><i class="fas fa-chart-bar header-icon"></i> 课程学情分析</h3>
            <span class="refresh-btn" @click="fetchCourseStats" title="刷新数据">
              <i class="fas fa-sync-alt" :class="{ 'fa-spin': loading.courseStats }"></i>
            </span>
          </div>
          <div class="card-body chart-body">
            <div class="chart-container">
              <canvas ref="chartCanvas"></canvas>
            </div>
          </div>
        </div>

        <!-- 最近课程 -->
        <div class="dashboard-card">
          <div class="card-header">
            <h3><i class="fas fa-clock header-icon"></i> 最近课程</h3>
            <router-link to="/teacher/courses/list" class="view-more">
              全部课程 <i class="fas fa-chevron-right"></i>
            </router-link>
          </div>
          <div class="card-body">
            <div v-if="loading.recentCourses" class="loading-state">
              <i class="fas fa-spinner fa-spin"></i> 加载中...
            </div>
            <div v-else-if="recentCourses.length === 0" class="empty-state">
              <img src="https://gw.alipayobjects.com/zos/antfincdn/ZHrcdLPrvN/empty.svg" alt="Empty">
              <p>暂无课程数据</p>
            </div>
            <div v-else class="recent-courses-grid">
              <div v-for="course in recentCourses" :key="course.id" class="mini-course-card" @click="goToCourseMaterials(course.id)">
                <div class="course-cover">
                  <img :src="course.coverUrl || defaultCover" @error="handleImgError" alt="cover" />
                  <div class="course-overlay">
                    <button class="manage-btn">管理</button>
                  </div>
                </div>
                <div class="course-info">
                  <h4 class="course-title" :title="course.title">{{ course.title }}</h4>
                  <div class="course-metrics">
                    <span title="学生数"><i class="fas fa-user"></i> {{ course.studentCount }}</span>
                    <span title="完成率"><i class="fas fa-check-circle"></i> {{ course.completionRate }}%</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧边栏：待办与快捷入口 -->
      <div class="side-column">
        <!-- 待办事项 -->
        <div class="dashboard-card todo-card">
          <div class="card-header">
            <h3><i class="fas fa-clipboard-check header-icon"></i> 待办事项</h3>
            <button class="add-btn" @click="showAddTodoModal = true">
              <i class="fas fa-plus"></i>
            </button>
          </div>
          <div class="card-body todo-list-container">
            <div v-if="todos.length === 0" class="empty-state small">
              <p>无待办事项，喝杯咖啡吧 ☕</p>
            </div>
            <transition-group name="list" tag="div" class="todo-list">
              <div
                v-for="todo in todos"
                :key="todo.id"
                class="todo-item"
                :class="{ 'is-completed': todo.completed, 'priority-high': todo.priority === 'high' }"
              >
                <label class="custom-checkbox">
                  <input type="checkbox" v-model="todo.completed" @change="updateTodo(todo)">
                  <span class="checkmark"></span>
                </label>
                <div class="todo-content" @click="editTodo(todo)">
                  <div class="todo-title">{{ todo.title }}</div>
                  <div class="todo-date" v-if="todo.dueDate">
                    <i class="far fa-clock"></i> {{ formatDate(todo.dueDate) }}
                  </div>
                </div>
                <button class="delete-btn" @click.stop="deleteTodo(todo.id)">
                  <i class="fas fa-times"></i>
                </button>
              </div>
            </transition-group>
          </div>
        </div>

        <!-- 快捷入口 (可选) -->
        <div class="dashboard-card quick-actions-card">
           <div class="card-header">
            <h3>快捷入口</h3>
          </div>
          <div class="quick-links">
            <button class="quick-link" @click="router.push('/teacher/students/groups')">
              <div class="icon-circle bg-blue"><i class="fas fa-users"></i></div>
              <span>学生分组</span>
            </button>
            <button class="quick-link" @click="router.push('/teacher/profile')">
              <div class="icon-circle bg-green"><i class="fas fa-user-cog"></i></div>
              <span>个人设置</span>
            </button>
             <button class="quick-link" @click="router.push('/teacher/courses/create')">
              <div class="icon-circle bg-orange"><i class="fas fa-plus-circle"></i></div>
              <span>创建课程</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 待办事项弹窗 -->
    <el-dialog
      v-model="showAddTodoModal"
      :title="editingTodo ? '编辑事项' : '新建待办'"
      width="400px"
      destroy-on-close
      class="custom-dialog"
    >
      <div class="todo-form">
        <div class="form-item">
          <label>标题</label>
          <el-input v-model="todoForm.title" placeholder="要做什么？" />
        </div>
        <div class="form-item">
          <label>截止日期</label>
          <el-date-picker v-model="todoForm.dueDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
        </div>
        <div class="form-item">
          <label>优先级</label>
           <el-radio-group v-model="todoForm.priority">
            <el-radio label="low">普通</el-radio>
            <el-radio label="high"><span style="color: #ef4444">紧急</span></el-radio>
          </el-radio-group>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closeModal">取消</el-button>
          <el-button type="primary" @click="saveTodo" :disabled="!todoForm.title">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import Chart from 'chart.js/auto'
import axios from 'axios'
import { useRouter } from 'vue-router'
import { Bell, ChatDotRound } from '@element-plus/icons-vue'

// 配置
const API_BASE = (import.meta?.env?.VITE_API_BASE_URL || '/api')
const api = axios.create({ baseURL: API_BASE, timeout: 20000 })
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token') || localStorage.getItem('userToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

const defaultCover = 'https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png'
const handleImgError = (e) => { e.target.src = defaultCover }
const router = useRouter()

// 状态
const teacherName = ref('')
const stats = ref({ courseCount: 0, studentCount: 0, completionRate: 0, assignmentCount: 0 })
const recentCourses = ref([])
const todos = ref([])
const courseStats = ref([])
const pendingApplicationsCount = ref(0)
const loading = ref({ recentCourses: false, courseStats: false })

// 待办相关
const showAddTodoModal = ref(false)
const editingTodo = ref(null)
const todoForm = ref({ title: '', dueDate: '', priority: 'low', completed: false })

// 图表
let chart = null
const chartCanvas = ref(null)

// 辅助函数
const getTimeState = () => {
  const h = new Date().getHours()
  if (h < 12) return '上午好'
  if (h < 18) return '下午好'
  return '晚上好'
}

const formatDate = (input) => {
  if (!input) return ''
  const d = new Date(input)
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

// 核心逻辑
const loadCurrentTeacher = () => {
  try {
    const saved = JSON.parse(localStorage.getItem('userInfo') || localStorage.getItem('currentUser') || 'null')
    let name = saved?.name || saved?.username || '老师'
    // 去除可能存在的"老师"后缀（包括带空格和不带空格的情况）
    if (name && name !== '老师') {
      name = name.replace(/\s*老师\s*$/, '').trim()
    }
    teacherName.value = name
  } catch { teacherName.value = '老师' }
}

const fetchStats = async () => {
  try {
    const [coursesRes, groupsRes] = await Promise.all([
      api.get('/course/list'),
      api.post('/student-group/approvalStatus')
    ])

    const allCourses = coursesRes?.data?.data || []
    const groups = groupsRes?.data?.data || []

    // 简单计算：实际项目中建议后端提供聚合接口以提高性能
    // 这里为了演示效果，沿用前端计算逻辑但简化异常处理
    const courseIds = allCourses.map(c => c.courseId || c.id)
    // 模拟计算... 实际开发中应调用后端 /stats 接口
    // 为保持页面响应速度，这里仅更新基础数据，详细进度由 fetchCourseStats 异步更新

    stats.value.courseCount = allCourses.length
    stats.value.assignmentCount = groups.length
    // studentCount 和 completionRate 在 fetchRecentCourses 中会进一步修正或保持默认
  } catch (e) { console.error('Stats error', e) }
}

const fetchRecentCourses = async () => {
  loading.value.recentCourses = true
  try {
    const res = await api.get('/course/list')
    const all = res?.data?.data || []

    // 取前6个课程并并发获取详情
    const targets = all.slice(0, 6)
    const enriched = await Promise.all(targets.map(async (c) => {
      const cid = c.courseId || c.id
      let sCount = 0, rate = 0
      try {
        const sRes = await api.get('/teacher/enrollments/students', { params: { courseId: cid } })
        const studs = sRes?.data?.data || []
        sCount = studs.length
        // 简化的进度逻辑：随机生成演示数据或真实计算
        // 真实环境请解除下方注释
        /*
        if (studs.length) {
           const pArr = await Promise.all(studs.map(s => api.get('/progress/course', { params: { studentId: s.id, courseId: cid } }).catch(()=>({}))))
           const total = pArr.reduce((acc, r) => acc + (r?.data?.data?.completionPercentage || 0), 0)
           rate = Math.round((total * 100) / studs.length)
        }
        */
      } catch {}

      return {
        id: cid,
        title: c.courseName || c.title || '未命名课程',
        coverUrl: c.image || c.cover || c.resourceUrl,
        studentCount: sCount,
        completionRate: rate // 默认为0，待后端完善
      }
    }))

    recentCourses.value = enriched

    // 更新全局统计中的学生总数（简单去重估算）
    stats.value.studentCount = enriched.reduce((acc, c) => acc + c.studentCount, 0) // 仅作示例
  } catch { recentCourses.value = [] }
  finally { loading.value.recentCourses = false }
}

const fetchCourseStats = async () => {
  loading.value.courseStats = true
  try {
    // 调用新的统计接口，获取真实的课程完成率数据
    const statsRes = await api.get('/course/stats/all')
    const allStats = statsRes?.data?.data || []

    // 只展示前8个课程的图表
    const displayStats = allStats.slice(0, 8)

    courseStats.value = displayStats.map(stat => ({
      courseName: stat.courseName,
      completionRate: Math.round(stat.averageCompletion || 0)
    }))

    // 计算所有课程的平均完成率用于顶部卡片
    if (allStats.length > 0) {
      const totalAvg = allStats.reduce((acc, s) => acc + (s.averageCompletion || 0), 0)
      stats.value.completionRate = Math.round(totalAvg / allStats.length)
    } else {
      stats.value.completionRate = 0
    }

    updateChart()
  } catch (e) {
    console.error('获取课程统计数据失败:', e)
    // 如果接口调用失败，设置为0
    courseStats.value = []
    stats.value.completionRate = 0
  }
  finally { loading.value.courseStats = false }
}

const fetchPendingCount = async () => {
  try {
    const res = await api.post('/student-group/approvalStatus', null, { params: { approvalStatus: 'pending' } })
    pendingApplicationsCount.value = (res?.data?.data || []).length
  } catch {}
}

// Chart Logic
const updateChart = () => {
  if (chart) chart.destroy()
  if (!chartCanvas.value) return

  const ctx = chartCanvas.value.getContext('2d')
  // 创建渐变
  const gradient = ctx.createLinearGradient(0, 0, 0, 400)
  gradient.addColorStop(0, 'rgba(59, 130, 246, 0.5)')
  gradient.addColorStop(1, 'rgba(59, 130, 246, 0.05)')

  chart = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: courseStats.value.map(i => i.courseName.length > 6 ? i.courseName.substring(0,6)+'..' : i.courseName),
      datasets: [{
        label: '平均完成率',
        data: courseStats.value.map(i => i.completionRate),
        backgroundColor: gradient,
        borderColor: '#3b82f6',
        borderWidth: 1,
        borderRadius: 4,
        barThickness: 20
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: 'rgba(17, 24, 39, 0.9)',
          padding: 10,
          cornerRadius: 6
        }
      },
      scales: {
        y: {
          beginAtZero: true,
          max: 100,
          grid: { borderDash: [4, 4], color: '#f3f4f6' }
        },
        x: {
          grid: { display: false }
        }
      }
    }
  })
}

// Todos Logic
const loadTodos = () => {
  const saved = localStorage.getItem('teacherTodos')
  todos.value = saved ? JSON.parse(saved) : []
}
const saveTodosToLocal = () => localStorage.setItem('teacherTodos', JSON.stringify(todos.value))

const saveTodo = () => {
  if (editingTodo.value) {
    Object.assign(editingTodo.value, todoForm.value)
  } else {
    todos.value.unshift({ ...todoForm.value, id: Date.now() })
  }
  saveTodosToLocal()
  closeModal()
}
const deleteTodo = (id) => {
  todos.value = todos.value.filter(t => t.id !== id)
  saveTodosToLocal()
}
const updateTodo = () => saveTodosToLocal()
const editTodo = (t) => {
  editingTodo.value = t
  todoForm.value = { ...t }
  showAddTodoModal.value = true
}
const closeModal = () => {
  showAddTodoModal.value = false
  editingTodo.value = null
  todoForm.value = { title: '', dueDate: '', priority: 'low', completed: false }
}

const goToGroups = () => router.push('/teacher/students/groups')
const goToAIChat = () => router.push('/teacher/ai-chat')

// 跳转到课程内容管理页面
const goToCourseMaterials = (courseId) => {
  router.push(`/teacher/courses/${courseId}/materials`)
}

onMounted(() => {
  loadCurrentTeacher()
  loadTodos()
  fetchStats()
  fetchRecentCourses()
  fetchCourseStats()
  fetchPendingCount()
})

onUnmounted(() => { if (chart) chart.destroy() })
</script>

<style scoped>
.teacher-home {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
  color: #1f2937;
}

/* 顶部导航区 */
.page-header {
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

.welcome-text {
  color: #6b7280;
  margin-top: 4px;
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
  cursor: pointer;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: #f3f4f6;
  transition: all 0.3s;
}

.notification-badge:hover {
  background: #e5e7eb;
}

.notification-badge.has-new {
  background: #eff6ff;
  color: #3b82f6;
}

.ai-btn {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  box-shadow: 0 4px 6px -1px rgba(37, 99, 235, 0.2);
  transition: transform 0.2s;
}

.ai-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 8px -1px rgba(37, 99, 235, 0.3);
}

/* 统计卡片区 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  transition: transform 0.2s;
  position: relative;
  overflow: hidden;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.05);
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 100px;
  height: 100px;
  background: linear-gradient(135deg, rgba(255,255,255,0.2), rgba(255,255,255,0));
  border-radius: 50%;
  transform: translate(30%, -30%);
}

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

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-value .unit {
  font-size: 14px;
  font-weight: 500;
  margin-left: 2px;
  opacity: 0.7;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
  margin-top: 4px;
}

/* 颜色变体 */
.stat-card.blue .stat-icon { background: #eff6ff; color: #3b82f6; }
.stat-card.blue .stat-value { color: #1e40af; }
.stat-card.green .stat-icon { background: #f0fdf4; color: #10b981; }
.stat-card.green .stat-value { color: #065f46; }
.stat-card.purple .stat-icon { background: #f5f3ff; color: #8b5cf6; }
.stat-card.purple .stat-value { color: #5b21b6; }
.stat-card.orange .stat-icon { background: #fff7ed; color: #f97316; }
.stat-card.orange .stat-value { color: #9a3412; }

/* 仪表盘主体布局 */
.dashboard-grid {
  display: grid;
  grid-template-columns: 1fr 340px; /* 左宽右窄 */
  gap: 24px;
  align-items: start;
}

@media (max-width: 1024px) {
  .dashboard-grid { grid-template-columns: 1fr; }
}

.main-column, .side-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.dashboard-card {
  background: white;
  border-radius: 16px;
  padding: 0;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
}

.card-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f3f4f6;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
}

.header-icon { color: #9ca3af; font-size: 14px; }

.view-more {
  font-size: 13px;
  color: #6b7280;
  text-decoration: none;
  transition: color 0.2s;
  display: flex;
  align-items: center;
  gap: 4px;
}

.view-more:hover { color: #3b82f6; }

.card-body { padding: 20px; }
.chart-body { height: 320px; padding: 10px 20px; }
.chart-container { width: 100%; height: 100%; position: relative; }

/* 课程迷你卡片 */
.recent-courses-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.mini-course-card {
  background: white;
  border: 1px solid #f3f4f6;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
}

.mini-course-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.05);
  border-color: #e5e7eb;
}

.course-cover {
  height: 120px;
  background: #f9fafb;
  position: relative;
  overflow: hidden;
}

.course-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.course-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.mini-course-card:hover .course-overlay { opacity: 1; }

.manage-btn {
  background: white;
  border: none;
  padding: 6px 16px;
  border-radius: 20px;
  font-weight: 600;
  font-size: 12px;
  color: #1f2937;
  cursor: pointer;
}

.course-info { padding: 12px; }

.course-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.course-metrics {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #6b7280;
}

/* 待办事项 */
.add-btn, .refresh-btn {
  background: #f3f4f6;
  border: none;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  color: #6b7280;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.add-btn:hover, .refresh-btn:hover { background: #e5e7eb; color: #3b82f6; }

.todo-list-container {
  min-height: 200px;
  max-height: 400px;
  overflow-y: auto;
}

.todo-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 8px;
  background: #fff;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.todo-item:hover { background: #f9fafb; }
.todo-item.priority-high { border-left: 3px solid #ef4444; background: #fef2f2; }

.custom-checkbox {
  position: relative;
  width: 20px;
  height: 20px;
  cursor: pointer;
  margin-top: 2px;
}

.custom-checkbox input { opacity: 0; }
.custom-checkbox .checkmark {
  position: absolute;
  top: 0;
  left: 0;
  height: 18px;
  width: 18px;
  background-color: white;
  border: 2px solid #d1d5db;
  border-radius: 4px;
}

.custom-checkbox input:checked ~ .checkmark {
  background-color: #3b82f6;
  border-color: #3b82f6;
}

.custom-checkbox .checkmark:after {
  content: "";
  position: absolute;
  display: none;
  left: 5px;
  top: 1px;
  width: 4px;
  height: 9px;
  border: solid white;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.custom-checkbox input:checked ~ .checkmark:after { display: block; }

.todo-content { flex: 1; cursor: pointer; }
.todo-title { font-size: 14px; color: #374151; transition: color 0.2s; }
.todo-date { font-size: 12px; color: #9ca3af; margin-top: 2px; }
.is-completed .todo-title { text-decoration: line-through; color: #9ca3af; }

.delete-btn {
  background: none;
  border: none;
  color: #9ca3af;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s;
}

.todo-item:hover .delete-btn { opacity: 1; }
.delete-btn:hover { color: #ef4444; }

/* 快捷入口 */
.quick-links {
  display: flex;
  justify-content: space-around;
  padding: 24px 10px;
}

.quick-link {
  background: none;
  border: none;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #4b5563;
  font-size: 12px;
}

.icon-circle {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  transition: transform 0.2s;
}

.quick-link:hover .icon-circle { transform: scale(1.1); }
.bg-blue { background: #eff6ff; color: #3b82f6; }
.bg-green { background: #f0fdf4; color: #10b981; }
.bg-orange { background: #fff7ed; color: #f97316; }

/* 列表动画 */
.list-enter-active, .list-leave-active { transition: all 0.3s ease; }
.list-enter-from, .list-leave-to { opacity: 0; transform: translateX(30px); }

/* 弹窗表单 */
.todo-form { padding: 10px 0; }
.form-item { margin-bottom: 16px; }
.form-item label { display: block; margin-bottom: 6px; font-size: 14px; font-weight: 500; color: #374151; }
</style>