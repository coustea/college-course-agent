<template>
  <div class="teacher-home">
    <div class="page-header">
      <div class="header-left">
        <h2>教师工作台</h2>
      </div>
      <div class="header-right">
        <div class="notification-box" @click="goToGroups">
          <el-badge :is-dot="pendingApplicationsCount > 0" class="item">
            <el-icon :size="24" color="#409eff"><Bell /></el-icon>
          </el-badge>
          <span class="notification-text" v-if="pendingApplicationsCount > 0">
            {{ pendingApplicationsCount }}个待审批分组
          </span>
        </div>
      </div>
    </div>

    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #4f46e5, #7c3aed);">
          <i class="fas fa-book"></i>
        </div>
        <div class="stat-content">
          <div class="stat-number">{{ stats.courseCount }}</div>
          <div class="stat-label">课程总数</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #059669, #0d9488);">
          <i class="fas fa-users"></i>
        </div>
        <div class="stat-content">
          <div class="stat-number">{{ stats.studentCount }}</div>
          <div class="stat-label">学生数量</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #ea580c, #dc2626);">
          <i class="fas fa-chart-line"></i>
        </div>
        <div class="stat-content">
          <div class="stat-number">{{ stats.completionRate }}%</div>
          <div class="stat-label">平均完成率</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #ca8a04, #eab308);">
          <i class="fas fa-check-circle"></i>
        </div>
        <div class="stat-content">
          <div class="stat-number">{{ stats.assignmentCount }}</div>
          <div class="stat-label">分组总数</div>
        </div>
      </div>
    </div>

    <div class="teacher-dashboard">
      <div class="dashboard-column">
        <div class="dashboard-card">
          <div class="card-header">
            <h3>最近课程</h3>
            <router-link to="/teacher/courses" class="view-all">查看全部</router-link>
          </div>
          <div class="card-content">
            <div v-if="loading.recentCourses" class="loading">
              <i class="fas fa-spinner fa-spin"></i> 加载中...
            </div>
            <div v-else-if="recentCourses.length === 0" class="empty-state">
              <i class="fas fa-book-open"></i>
              <p>暂无课程</p>
            </div>
            <div v-else class="recent-courses">
              <div
                v-for="course in recentCourses"
                :key="course.id"
                class="recent-course-item"
              >
                <div class="course-info">
                  <h4>{{ course.title }}</h4>
                  <p>{{ course.date }}</p>
                </div>
                <div class="course-stats">
                  <span class="students-count">
                    <i class="fas fa-user-graduate"></i>
                    {{ course.studentCount }}
                  </span>
                  <span class="completion-rate">
                    <i class="fas fa-chart-pie"></i>
                    {{ course.completionRate }}%
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="dashboard-card">
          <div class="card-header">
            <h3>待办事项</h3>
            <button class="add-todo" @click="showAddTodoModal = true">
              <i class="fas fa-plus"></i> 添加
            </button>
          </div>
          <div class="card-content">
            <div v-if="todos.length === 0" class="empty-state">
              <i class="fas fa-check-circle"></i>
              <p>暂无待办事项</p>
            </div>
            <div v-else class="todo-list">
              <div
                v-for="todo in todos"
                :key="todo.id"
                class="todo-item"
                :class="{urgent: todo.priority === 'high'}"
              >
                <div class="todo-checkbox">
                  <input type="checkbox" :id="'todo-'+todo.id" v-model="todo.completed" @change="updateTodo(todo)">
                  <label :for="'todo-'+todo.id"></label>
                </div>
                <div class="todo-content">
                  <h4 :class="{completed: todo.completed}">{{ todo.title }}</h4>
                  <p>{{ todo.dueDate }}</p>
                </div>
                <div class="todo-actions">
                  <button class="icon-btn" @click="editTodo(todo)">
                    <i class="fas fa-edit"></i>
                  </button>
                  <button class="icon-btn" @click="deleteTodo(todo.id)">
                    <i class="fas fa-trash"></i>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="dashboard-column">
        <div class="dashboard-card">
          <div class="card-header">
            <h3>学生动态</h3>
            <div class="pagination-controls">
              <button @click="prevActivityPage" :disabled="activityCurrentPage <= 1">
                <i class="fas fa-chevron-left"></i>
              </button>
              <span>{{ activityCurrentPage }} / {{ activityTotalPages }}</span>
              <button @click="nextActivityPage" :disabled="activityCurrentPage >= activityTotalPages">
                <i class="fas fa-chevron-right"></i>
              </button>
            </div>
          </div>
          <div class="card-content">
            <div v-if="loading.studentActivities" class="loading">
              <i class="fas fa-spinner fa-spin"></i> 加载中...
            </div>
            <div v-else-if="studentActivities.length === 0" class="empty-state">
              <i class="fas fa-user-graduate"></i>
              <p>暂无学生动态</p>
            </div>
            <div v-else class="activity-list">
              <div
                v-for="activity in paginatedActivities"
                :key="activity.id"
                class="activity-item"
              >
                <div class="activity-avatar">
                  <div class="avatar">{{ activity.studentName.charAt(0) }}</div>
                </div>
                <div class="activity-content">
                  <p>
                    <strong>{{ activity.studentName }}</strong>
                    {{ activity.action }}
                    <strong>{{ activity.courseName }}</strong>
                  </p>
                  <span class="activity-time">{{ activity.time }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="dashboard-card">
          <div class="card-header">
            <h3>课程统计</h3>
          </div>
          <div class="card-content">
            <div v-if="loading.courseStats" class="loading">
              <i class="fas fa-spinner fa-spin"></i> 加载中...
            </div>
            <div v-else class="chart-container">
              <canvas id="courseStatsChart" ref="chartCanvas"></canvas>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加/编辑待办事项模态框 -->
    <div v-if="showAddTodoModal" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ editingTodo ? '编辑待办事项' : '添加待办事项' }}</h3>
          <button class="close-btn" @click="closeModal">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="saveTodo">
            <div class="form-group">
              <label for="todoTitle">标题</label>
              <input
                type="text"
                id="todoTitle"
                v-model="todoForm.title"
                required
                placeholder="请输入待办事项标题"
              >
            </div>
            <div class="form-group">
              <label for="todoDueDate">截止日期</label>
              <input
                type="date"
                id="todoDueDate"
                v-model="todoForm.dueDate"
              >
            </div>
            <div class="form-group">
              <label for="todoPriority">优先级</label>
              <select id="todoPriority" v-model="todoForm.priority">
                <option value="low">低</option>
                <option value="medium">中</option>
                <option value="high">高</option>
              </select>
            </div>
            <div class="form-actions">
              <button type="button" class="cancel-btn" @click="closeModal">取消</button>
              <button type="submit" class="save-btn">{{ editingTodo ? '更新' : '添加' }}</button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import Chart from 'chart.js/auto'
import axios from 'axios'
import { useRouter } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'

// 创建axios实例
const api = axios.create({
  baseURL: 'http://localhost:3000/api',
  timeout: 10000
})

// 设置请求拦截器，添加认证token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

const router = useRouter()
const teacherName = ref('')

const stats = ref({
  courseCount: 0,
  studentCount: 0,
  completionRate: 0,
  assignmentCount: 0
})

const recentCourses = ref([])
const todos = ref([])
const studentActivities = ref([])
const courseStats = ref([])
const pendingApplicationsCount = ref(0)

// 加载状态
const loading = ref({
  recentCourses: false,
  studentActivities: false,
  courseStats: false
})

// 学生动态分页
const activityCurrentPage = ref(1)
const activityPageSize = ref(5)

// 待办事项表单
const showAddTodoModal = ref(false)
const editingTodo = ref(null)
const todoForm = ref({
  title: '',
  dueDate: '',
  priority: 'medium',
  completed: false
})

// 图表相关
let chart = null
const chartCanvas = ref(null)

// 计算属性
const activityTotalPages = computed(() => {
  return Math.ceil(studentActivities.value.length / activityPageSize.value)
})

const paginatedActivities = computed(() => {
  const start = (activityCurrentPage.value - 1) * activityPageSize.value
  const end = start + activityPageSize.value
  return studentActivities.value.slice(start, end)
})

// 从本地存储获取未审批数量
const getPendingCountFromStorage = () => {
  const count = localStorage.getItem('pendingGroupsCount')
  return count ? parseInt(count) : 0
}

// 分页控制
const prevActivityPage = () => {
  if (activityCurrentPage.value > 1) {
    activityCurrentPage.value--
  }
}

const nextActivityPage = () => {
  if (activityCurrentPage.value < activityTotalPages.value) {
    activityCurrentPage.value++
  }
}

// 处理未审批数量更新事件
const handlePendingCountUpdate = (event) => {
  pendingApplicationsCount.value = event.detail.count
}

// 获取教师信息
const fetchTeacherInfo = async () => {
  try {
    const response = await api.get('/teacher/profile')
    teacherName.value = response.data.name
  } catch (error) {
    console.error('获取教师信息失败:', error)
  }
}

// 获取统计数据
const fetchStats = async () => {
  try {
    const response = await api.get('/teacher/stats')
    stats.value = response.data
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 获取最近课程
const fetchRecentCourses = async () => {
  loading.value.recentCourses = true
  try {
    const response = await api.get('/teacher/courses/recent')
    recentCourses.value = response.data
  } catch (error) {
    console.error('获取最近课程失败:', error)
  } finally {
    loading.value.recentCourses = false
  }
}

// 获取学生动态
const fetchStudentActivities = async () => {
  loading.value.studentActivities = true
  try {
    const response = await api.get('/teacher/activities')
    studentActivities.value = response.data
    // 重置分页
    activityCurrentPage.value = 1
  } catch (error) {
    console.error('获取学生动态失败:', error)
  } finally {
    loading.value.studentActivities = false
  }
}

// 获取课程统计
const fetchCourseStats = async () => {
  loading.value.courseStats = true
  try {
    const response = await api.get('/teacher/course-stats')
    courseStats.value = response.data

    // 更新图表
    updateChart()
  } catch (error) {
    console.error('获取课程统计失败:', error)
  } finally {
    loading.value.courseStats = false
  }
}

// 获取待审批分组申请数量
const fetchPendingApplicationsCount = async () => {
  try {
    // 先从本地存储获取，如果没有则使用默认值
    const storedCount = getPendingCountFromStorage()
    if (storedCount > 0) {
      pendingApplicationsCount.value = storedCount
    } else {
      // 模拟数据：假设有3个待审批申请
      pendingApplicationsCount.value = 3
      localStorage.setItem('pendingGroupsCount', '3')
    }

    // 实际应该调用API获取真实数据
    // const response = await api.get('/groups/pending-count')
    // pendingApplicationsCount.value = response.data.count
    // localStorage.setItem('pendingGroupsCount', response.data.count.toString())
  } catch (error) {
    console.error('获取待审批申请数量失败:', error)
  }
}

// 更新图表
const updateChart = () => {
  if (chart) {
    chart.destroy()
  }

  if (chartCanvas.value && courseStats.value.length > 0) {
    const ctx = chartCanvas.value.getContext('2d')
    const labels = courseStats.value.map(item => item.courseName)
    const data = courseStats.value.map(item => item.completionRate)

    chart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [{
          label: '学生完成率 (%)',
          data: data,
          backgroundColor: 'rgba(54, 162, 235, 0.5)',
          borderColor: 'rgba(54, 162, 235, 1)',
          borderWidth: 1
        }]
      },
      options: {
        responsive: true,
        scales: {
          y: {
            beginAtZero: true,
            max: 100
          }
        }
      }
    })
  }
}

// 待办事项操作
const loadTodos = () => {
  const savedTodos = localStorage.getItem('teacherTodos')
  if (savedTodos) {
    todos.value = JSON.parse(savedTodos)
  }
}

const saveTodos = () => {
  localStorage.setItem('teacherTodos', JSON.stringify(todos.value))
}

const addTodo = (todo) => {
  todo.id = Date.now()
  todos.value.push(todo)
  saveTodos()
}

const updateTodo = (todo) => {
  const index = todos.value.findIndex(t => t.id === todo.id)
  if (index !== -1) {
    todos.value[index] = todo
    saveTodos()
  }
}

const deleteTodo = (id) => {
  todos.value = todos.value.filter(todo => todo.id !== id)
  saveTodos()
}

const editTodo = (todo) => {
  editingTodo.value = todo
  todoForm.value = { ...todo }
  showAddTodoModal.value = true
}

const saveTodo = () => {
  if (editingTodo.value) {
    // 更新待办事项
    const index = todos.value.findIndex(t => t.id === editingTodo.value.id)
    if (index !== -1) {
      todos.value[index] = { ...todoForm.value, id: editingTodo.value.id }
      saveTodos()
    }
  } else {
    // 添加新待办事项
    addTodo({ ...todoForm.value })
  }

  closeModal()
}

const closeModal = () => {
  showAddTodoModal.value = false
  editingTodo.value = null
  todoForm.value = {
    title: '',
    dueDate: '',
    priority: 'medium',
    completed: false
  }
}

// 跳转到分组页面
const goToGroups = () => {
  router.push('/teacher/students/groups')
}

// 初始化数据
onMounted(() => {
  // 加载本地待办事项
  loadTodos()

  // 获取远程数据
  fetchTeacherInfo()
  fetchStats()
  fetchRecentCourses()
  fetchStudentActivities()
  fetchCourseStats()
  fetchPendingApplicationsCount()

  // 监听未审批数量更新事件
  window.addEventListener('pendingGroupsCountUpdated', handlePendingCountUpdate)
})

onUnmounted(() => {
  if (chart) {
    chart.destroy()
  }
  // 移除事件监听器
  window.removeEventListener('pendingGroupsCountUpdated', handlePendingCountUpdate)
})

// 监听课程统计数据变化，更新图表
watch(courseStats, () => {
  updateChart()
})
</script>

<style scoped>
.teacher-home {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 40px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 16px 24px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header-left {
  display: flex;
  flex-direction: column;
}

.header-right {
  display: flex;
  align-items: center;
}

.page-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 8px 0;
}

.welcome-text {
  color: #6b7280;
  font-size: 16px;
}

.notification-box {
  cursor: pointer;
  padding: 8px 16px;
  border-radius: 8px;
  transition: background-color 0.3s;
  display: flex;
  align-items: center;
  gap: 8px;
}

.notification-box:hover {
  background-color: #f0f0f0;
}

.notification-text {
  font-size: 14px;
  color: #409eff;
  font-weight: 500;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  background: white;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  color: white;
  font-size: 24px;
}

.stat-content {
  flex: 1;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #2c3e50;
  margin-bottom: 5px;
}

.stat-label {
  color: #64748b;
  font-size: 14px;
}

.teacher-dashboard {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.dashboard-card {
  background: white;
  border-radius: 10px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
  overflow: hidden;
}

.card-header {
  padding: 20px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
}

.view-all {
  color: #2563eb;
  font-size: 14px;
  text-decoration: none;
}

.view-all:hover {
  text-decoration: underline;
}

.add-todo {
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 5px 10px;
  cursor: pointer;
  font-size: 14px;
}

.add-todo:hover {
  background: #1d4ed8;
}

.card-content {
  padding: 20px;
}

.loading {
  text-align: center;
  padding: 20px;
  color: #64748b;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #94a3b8;
}

.empty-state i {
  font-size: 48px;
  margin-bottom: 15px;
  display: block;
}

.recent-course-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid #f1f5f9;
}

.recent-course-item:last-child {
  border-bottom: none;
}

.course-info h4 {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 5px;
}

.course-info p {
  color: #64748b;
  font-size: 14px;
}

.course-stats {
  display: flex;
  gap: 15px;
}

.course-stats span {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  color: #64748b;
}

.todo-item {
  display: flex;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid #f1f5f9;
}

.todo-item:last-child {
  border-bottom: none;
}

.todo-item.urgent {
  border-left: 3px solid #ef4444;
  padding-left: 10px;
}

.todo-checkbox {
  margin-right: 15px;
}

.todo-checkbox input[type="checkbox"] {
  display: none;
}

.todo-checkbox label {
  display: block;
  width: 20px;
  height: 20px;
  border: 2px solid #d1d5db;
  border-radius: 4px;
  cursor: pointer;
  position: relative;
}

.todo-checkbox input[type="checkbox"]:checked + label {
  background-color: #2563eb;
  border-color: #2563eb;
}

.todo-checkbox input[type="checkbox"]:checked + label:after {
  content: "✓";
  position: absolute;
  color: white;
  font-size: 12px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.todo-content {
  flex: 1;
}

.todo-content h4 {
  font-size: 16px;
  font-weight: 500;
  color: #2c3e50;
  margin-bottom: 5px;
}

.todo-content h4.completed {
  text-decoration: line-through;
  color: #94a3b8;
}

.todo-content p {
  color: #64748b;
  font-size: 14px;
}

.todo-actions {
  margin-left: 10px;
  display: flex;
  gap: 5px;
}

.icon-btn {
  background: none;
  border: none;
  color: #64748b;
  cursor: pointer;
  padding: 5px;
  border-radius: 4px;
}

.icon-btn:hover {
  background-color: #f1f5f9;
}

.activity-item {
  display: flex;
  padding: 15px 0;
  border-bottom: 1px solid #f1f5f9;
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-avatar {
  margin-right: 15px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #2563eb;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}

.activity-content p {
  color: #2c3e50;
  margin-bottom: 5px;
  line-height: 1.4;
}

.activity-time {
  color: #64748b;
  font-size: 12px;
}

.chart-container {
  position: relative;
  height: 300px;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pagination-controls button {
  background: none;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  padding: 5px 10px;
  cursor: pointer;
}

.pagination-controls button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 10px;
  width: 100%;
  max-width: 500px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #f1f5f9;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #64748b;
}

.modal-body {
  padding: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
  color: #2c3e50;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 10px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 16px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.cancel-btn,
.save-btn {
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
}

.cancel-btn {
  background: #f1f5f9;
  border: 1px solid #d1d5db;
  color: #64748b;
}

.save-btn {
  background: #2563eb;
  border: 1px solid #2563eb;
  color: white;
}

@media (max-width: 1024px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }

  .teacher-dashboard {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: 1fr;
  }

  .teacher-home {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .pagination-controls {
    align-self: flex-end;
  }

  .notification-box {
    padding: 8px;
  }

  .notification-text {
    display: none;
  }
}
</style>
