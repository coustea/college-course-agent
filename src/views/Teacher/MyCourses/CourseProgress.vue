<template>
  <div class="progress-page">
    <!-- 顶部导航区 -->
    <div class="page-header-container">
      <el-page-header @back="goBack" title="返回课程列表">
        <template #content>
          <span class="header-title">课程进度管理</span>
        </template>
        <template #extra>
          <el-button type="primary" :icon="Refresh" circle @click="refreshData" :loading="loading" title="刷新数据" />
        </template>
      </el-page-header>
    </div>

    <!-- 统计卡片区 -->
    <el-row :gutter="20" class="stats-container">
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card blue-theme">
          <div class="stat-body">
            <div class="stat-info">
              <div class="stat-value">{{ totalStudents }}</div>
              <div class="stat-label">总学生数</div>
            </div>
            <el-icon class="stat-icon"><User /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card orange-theme">
          <div class="stat-body">
            <div class="stat-info">
              <div class="stat-value">{{ averageProgress.toFixed(1) }}%</div>
              <div class="stat-label">平均进度</div>
            </div>
            <el-icon class="stat-icon"><DataLine /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card green-theme">
          <div class="stat-body">
            <div class="stat-info">
              <div class="stat-value">{{ completedStudents }}</div>
              <div class="stat-label">完成人数</div>
            </div>
            <el-icon class="stat-icon"><Select /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 主表格区域 -->
    <el-card class="main-card" shadow="never">
      <!-- 筛选工具栏 -->
      <div class="filter-toolbar">
        <div class="left-filters">
          <span class="filter-label">当前课程：</span>
          <el-select
            v-model="courseId"
            placeholder="选择课程"
            @change="onCourseChange"
            filterable
            class="course-select"
            :loading="loadingCourses"
          >
            <el-option
              v-for="course in courseList"
              :key="course.courseId || course.id"
              :label="course.courseName || course.title || '未命名课程'"
              :value="course.courseId || course.id"
            />
          </el-select>
        </div>

        <div class="right-filters">
          <el-select
            v-model="selectedClass"
            placeholder="筛选班级"
            clearable
            class="class-select"
          >
            <el-option label="全部班级" value="" />
            <el-option
              v-for="cls in classList"
              :key="cls"
              :label="cls"
              :value="cls"
            />
          </el-select>
          <el-input
            v-model="searchText"
            placeholder="搜索姓名或学号"
            class="search-input"
            clearable
            :prefix-icon="Search"
          />
        </div>
      </div>

      <!-- 表格内容 -->
      <el-table
        :data="filteredStudentProgress"
        v-loading="loading"
        style="width: 100%"
        stripe
        highlight-current-row
        :default-sort="{ prop: 'progress', order: 'descending' }"
        header-cell-class-name="table-header-gray"
      >
        <el-table-column prop="studentNumber" label="学号" width="140" sortable fixed />
        <el-table-column prop="name" label="姓名" width="120" sortable fixed />
        <el-table-column prop="className" label="班级" width="140" sortable />

        <el-table-column label="总体进度" min-width="220" sortable prop="progress">
          <template #default="{ row }">
            <div class="progress-cell">
              <el-progress
                :percentage="row.progress"
                :color="progressColor(row.progress)"
                :stroke-width="12"
                :format="formatProgress"
              />
            </div>
          </template>
        </el-table-column>

        <el-table-column label="答题正确率" width="160" sortable prop="examAccuracy" align="center">
          <template #default="{ row }">
            <el-tag
              v-if="row.examAccuracy >= 0"
              :type="getAccuracyType(row.examAccuracy)"
              effect="plain"
              round
            >
              {{ row.examAccuracy.toFixed(1) }}%
            </el-tag>
            <span v-else class="text-gray">--</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.progress)" size="small">
              {{ getStatusText(row.progress) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="最后学习时间" width="180" sortable prop="lastStudyTime">
          <template #default="{ row }">
            <span class="time-text">{{ formatDateTime(row.lastStudyTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">
              详情
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无学生数据" />
        </template>
      </el-table>

      <!-- 分页组件 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="totalStudents"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handlePageSizeChange"
          background
        />
      </div>
    </el-card>

    <!-- 学生详细进度对话框 -->
    <el-dialog
      v-model="detailVisible"
      :title="detailDialogTitle"
      width="800px"
      destroy-on-close
      class="detail-dialog"
    >
      <div v-if="currentStudent" class="detail-content">
        <!-- 头部概览 -->
        <div class="detail-header">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="学号">{{ currentStudent.studentNumber }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ currentStudent.name }}</el-descriptions-item>
            <el-descriptions-item label="班级">{{ currentStudent.className }}</el-descriptions-item>
            <el-descriptions-item label="总体进度">
              <el-progress :percentage="currentStudent.progress" :color="progressColor(currentStudent.progress)" />
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <el-tabs type="border-card" class="detail-tabs">
          <el-tab-pane label="视频学习进度">
            <el-table :data="detailVideoProgress" style="width: 100%" height="300" stripe>
              <el-table-column prop="title" label="视频标题" show-overflow-tooltip />
              <el-table-column label="观看时长" width="120" align="right">
                <template #default="{ row }">
                  {{ formatSeconds(row.watchedSeconds || 0) }}
                </template>
              </el-table-column>
              <el-table-column label="进度" width="180" align="center">
                <template #default="{ row }">
                  <el-progress :percentage="row.percentage || 0" :stroke-width="10" />
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                   <el-icon v-if="row.completed" color="#67c23a"><Select /></el-icon>
                   <el-icon v-else color="#909399"><VideoPause /></el-icon>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="文档阅读进度">
            <el-table :data="detailDocProgress" style="width: 100%" height="300" stripe>
              <el-table-column prop="title" label="文档标题" show-overflow-tooltip />
              <el-table-column label="阅读比例" width="120" align="right">
                <template #default="{ row }">
                  {{ (row.maxScrollPct || 0).toFixed(1) }}%
                </template>
              </el-table-column>
              <el-table-column label="进度" width="180" align="center">
                <template #default="{ row }">
                  <el-progress :percentage="row.percentage || 0" :stroke-width="10" />
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-icon v-if="row.completed" color="#67c23a"><Select /></el-icon>
                  <el-icon v-else color="#909399"><Reading /></el-icon>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="测验分析">
            <div class="exam-analysis-panel">
               <div v-if="detailExamAccuracy >= 0" class="exam-score-box">
                  <p>课程平均正确率</p>
                  <h2 :class="getScoreClass(detailExamAccuracy)">{{ detailExamAccuracy.toFixed(1) }}%</h2>
                  <el-progress
                    type="dashboard"
                    :percentage="detailExamAccuracy"
                    :color="getAccuracyColor(detailExamAccuracy)"
                  />
               </div>
               <el-empty v-else description="暂无测验数据" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import {
  User,
  DataLine,
  Select,
  Search,
  Refresh,
  VideoPause,
  Reading
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

// API 配置
const API_BASE = import.meta?.env?.VITE_API_BASE_URL || '/api'
const api = axios.create({ baseURL: API_BASE, timeout: 30000 }) // 增加超时时间以应对并发请求
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token') || localStorage.getItem('userToken')
  if (token) config.headers = { ...(config.headers || {}), Authorization: `Bearer ${token}` }
  return config
})

// 状态变量
const courseId = ref(null)
const courseList = ref([])
const loadingCourses = ref(false)
const loading = ref(false)
const studentProgressList = ref([])

// 筛选与分页
const searchText = ref('')
const selectedClass = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

// 详情弹窗
const detailVisible = ref(false)
const currentStudent = ref(null)
const detailVideoProgress = ref([])
const detailDocProgress = ref([])
const detailExamAccuracy = ref(-1)

// --- 计算属性 ---

const detailDialogTitle = computed(() => {
  return currentStudent.value ? `${currentStudent.value.name} 的学习详情` : '学生详情'
})

const classList = computed(() => {
  const classes = new Set()
  studentProgressList.value.forEach(s => {
    if (s.className) classes.add(s.className)
  })
  return Array.from(classes).sort()
})

const filteredAllStudents = computed(() => {
  let result = studentProgressList.value

  if (selectedClass.value) {
    result = result.filter(s => s.className === selectedClass.value)
  }

  const q = searchText.value.toLowerCase().trim()
  if (q) {
    result = result.filter(s => {
      const name = String(s.name || '').toLowerCase()
      const no = String(s.studentNumber || '').toLowerCase()
      return name.includes(q) || no.includes(q)
    })
  }

  return result
})

const totalStudents = computed(() => filteredAllStudents.value.length)

const averageProgress = computed(() => {
  if (filteredAllStudents.value.length === 0) return 0
  const sum = filteredAllStudents.value.reduce((acc, s) => acc + (s.progress || 0), 0)
  return sum / filteredAllStudents.value.length
})

const completedStudents = computed(() => filteredAllStudents.value.filter(s => s.progress >= 100).length)

const filteredStudentProgress = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredAllStudents.value.slice(start, end)
})

// --- 样式辅助函数 ---

const progressColor = (pct) => {
  if (pct >= 100) return '#67c23a'
  if (pct >= 80) return '#409eff'
  if (pct >= 60) return '#e6a23c'
  return '#f56c6c'
}

const formatProgress = (percentage) => {
  return percentage === 100 ? '完成' : `${percentage}%`
}

const getAccuracyType = (acc) => {
  if (acc >= 80) return 'success'
  if (acc >= 60) return 'warning'
  return 'danger'
}

const getAccuracyColor = (acc) => {
  if (acc >= 80) return '#67c23a'
  if (acc >= 60) return '#e6a23c'
  return '#f56c6c'
}

const getStatusType = (progress) => {
  if (progress >= 100) return 'success'
  if (progress > 0) return 'primary'
  return 'info'
}

const getStatusText = (progress) => {
  if (progress >= 100) return '已完成'
  if (progress > 0) return '进行中'
  return '未开始'
}

const getScoreClass = (score) => {
  if (score >= 80) return 'text-success'
  if (score >= 60) return 'text-warning'
  return 'text-danger'
}

const formatDateTime = (dt) => {
  if (!dt) return '--'
  try {
    const d = new Date(dt)
    if (isNaN(d.getTime())) return '--'
    return d.toLocaleString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return '--'
  }
}

const formatSeconds = (sec) => {
  const s = Number(sec) || 0
  if (s === 0) return '0:00'
  const m = Math.floor(s / 60)
  const ss = Math.floor(s % 60)
  return `${m}:${String(ss).padStart(2, '0')}`
}

// --- 核心逻辑 ---

const goBack = () => router.back()

const handlePageChange = (page) => {
  currentPage.value = page
}

const handlePageSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
}

const refreshData = () => {
  if (courseId.value) {
    loadCourseProgress()
    ElMessage.success('数据已刷新')
  }
}

const loadCourseList = async () => {
  loadingCourses.value = true
  try {
    const res = await api.get('/course/list')
    const body = res?.data
    if (body && Number(body.code) === 200 && Array.isArray(body.data)) {
      courseList.value = body.data
    } else {
      courseList.value = []
    }
  } catch (err) {
    console.error('加载课程失败:', err)
    ElMessage.error('课程列表加载失败')
  } finally {
    loadingCourses.value = false
  }
}

const onCourseChange = () => {
  router.replace(`/teacher/courses/${courseId.value}/progress`)
  currentPage.value = 1 // 重置分页
  selectedClass.value = '' // 重置班级筛选
  searchText.value = ''    // 重置搜索
  loadCourseProgress()
}

// 核心业务：加载学生进度（含 N+1 并发请求处理）
const loadCourseProgress = async () => {
  if (!courseId.value) return

  loading.value = true
  studentProgressList.value = [] // 清空旧数据

  try {
    // 1. 获取选课学生列表
    const enrollRes = await api.get(`/teacher/enrollments/students`, { params: { courseId: courseId.value } })
    const enrollBody = enrollRes?.data
    const students = (enrollBody && Number(enrollBody.code) === 200 && Array.isArray(enrollBody.data)) ? enrollBody.data : []

    if (students.length === 0) {
      loading.value = false
      return
    }

    // 2. 构造并发请求池
    const promises = students.map(async (student) => {
      // 默认空数据结构
      const resultItem = {
        id: student.id,
        studentNumber: student.studentNumber || student.studentId || '--',
        name: student.name || '未知',
        className: student.className || '未分配',
        progress: 0,
        examAccuracy: -1,
        lastStudyTime: null
      }

      try {
        // 使用 Promise.allSettled 避免单个请求失败导致整体挂掉
        // 注意：这里保留了原始逻辑，但实际生产中建议后端提供批量接口
        const [progRes, accRes] = await Promise.all([
          api.get(`/progress/course`, { params: { studentId: student.id, courseId: courseId.value } }).catch(() => null),
          api.get(`/aiexam/accuracy`, { params: { studentId: student.id, courseId: courseId.value } }).catch(() => null)
        ])

        // 解析进度
        if (progRes?.data?.code === 200 && progRes.data.data) {
          const d = progRes.data.data
          // 兼容多种字段名
          let p = Number(d.completionPercentage ?? d.completion_percentage ?? d.coursePercent ?? 0)
          // 归一化到 0-100
          if (p >= 0 && p <= 1) p *= 100
          resultItem.progress = Math.min(100, Math.max(0, Math.round(p)))
          resultItem.lastStudyTime = d.updatedAt || d.lastUpdated || null
        }

        // 解析正确率
        if (accRes?.data?.code === 200 && accRes.data.data) {
          const d = accRes.data.data
          let pct = -1
          if (d.percentage != null) pct = Number(d.percentage)
          else if (d.accuracy != null) pct = Number(d.accuracy) * 100

          if (pct >= 0) resultItem.examAccuracy = pct
        }
      } catch (innerErr) {
        console.warn(`Fetch error for student ${student.id}`, innerErr)
      }
      return resultItem
    })

    // 等待所有完成
    studentProgressList.value = await Promise.all(promises)

  } catch (err) {
    console.error('加载进度异常:', err)
    ElMessage.error('获取学生进度数据失败')
  } finally {
    loading.value = false
  }
}

const viewDetail = async (student) => {
  currentStudent.value = student
  detailVisible.value = true
  // 重置详情数据
  detailVideoProgress.value = []
  detailDocProgress.value = []
  detailExamAccuracy.value = -1

  try {
    const [allRes, accRes] = await Promise.all([
      api.get(`/progress/course/all`, { params: { studentId: student.id, courseId: courseId.value } }),
      api.get(`/aiexam/accuracy`, { params: { studentId: student.id, courseId: courseId.value } })
    ])

    // 处理详情列表
    if (allRes?.data?.code === 200 && allRes.data.data) {
      const d = allRes.data.data
      const videos = Array.isArray(d.videos) ? d.videos : []
      const docs = Array.isArray(d.documents) ? d.documents : []

      detailVideoProgress.value = videos.map((v, i) => ({
        ...v,
        title: v.title || v.videoTitle || v.name || `视频 ${i + 1}`,
        percentage: Number(v.percentage || v.progress || 0),
        completed: (Number(v.percentage || v.progress || 0) >= 100)
      }))

      detailDocProgress.value = docs.map((d, i) => ({
        ...d,
        title: d.title || d.docTitle || d.name || `文档 ${i + 1}`,
        percentage: Number(d.percentage || d.progress || 0),
        maxScrollPct: Number(d.maxScrollPct || d.max_scroll_percentage || 0),
        completed: (Number(d.percentage || d.progress || 0) >= 100)
      }))
    }

    // 处理详情页正确率
    if (accRes?.data?.code === 200 && accRes.data.data) {
      const d = accRes.data.data
      let pct = -1
      if (d.percentage != null) pct = Number(d.percentage)
      else if (d.accuracy != null) pct = Number(d.accuracy) * 100
      if (pct >= 0) detailExamAccuracy.value = pct
    }

  } catch (err) {
    console.error('详情加载失败:', err)
    ElMessage.warning('部分详情加载失败')
  }
}

// --- 生命周期与监听 ---

onMounted(async () => {
  await loadCourseList()

  const routeCourseId = Number(route.params.id || route.params.courseId)

  // 只有在路由中明确指定了课程ID时才自动加载
  if (routeCourseId) {
    courseId.value = routeCourseId
    loadCourseProgress()
  }
  // 否则不自动选择课程，让教师手动选择
})

watch([searchText, selectedClass], () => {
  currentPage.value = 1
})
</script>

<style scoped>
.progress-page {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

/* 顶部 Header */
.page-header-container {
  background: #fff;
  padding: 16px 24px;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.header-title {
  font-weight: 600;
  font-size: 18px;
  color: #303133;
}

/* 统计卡片 */
.stats-container {
  margin-bottom: 20px;
}

.stat-card {
  border: none;
  border-radius: 8px;
  transition: transform 0.2s, box-shadow 0.2s;
  height: 100%;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.stat-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 6px;
}

.stat-icon {
  font-size: 40px;
  padding: 10px;
  border-radius: 12px;
  opacity: 0.8;
}

/* 统计卡片主题色 */
.blue-theme .stat-icon {
  background-color: #ecf5ff;
  color: #409eff;
}
.orange-theme .stat-icon {
  background-color: #fdf6ec;
  color: #e6a23c;
}
.green-theme .stat-icon {
  background-color: #f0f9eb;
  color: #67c23a;
}

/* 主内容卡片 */
.main-card {
  border-radius: 8px;
}

/* 筛选工具栏 */
.filter-toolbar {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 16px;
}

.left-filters {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.right-filters {
  display: flex;
  gap: 12px;
}

.course-select {
  width: 260px;
}
.class-select {
  width: 160px;
}
.search-input {
  width: 220px;
}

/* 表格样式 */
.progress-cell {
  padding-right: 20px;
}

.text-gray {
  color: #909399;
  font-size: 12px;
}

.time-text {
  font-size: 13px;
  color: #606266;
}

:deep(.table-header-gray) {
  background-color: #f5f7fa !important;
  color: #606266;
  font-weight: 600;
}

/* 分页 */
.pagination-container {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

/* 详情弹窗 */
.detail-content {
  padding: 0 10px;
}

.detail-header {
  margin-bottom: 24px;
}

.detail-tabs {
  min-height: 350px;
}

.exam-analysis-panel {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.exam-score-box {
  text-align: center;
}

.exam-score-box h2 {
  font-size: 36px;
  margin: 10px 0 20px;
}

.text-success { color: #67c23a; }
.text-warning { color: #e6a23c; }
.text-danger { color: #f56c6c; }

/* 响应式调整 */
@media (max-width: 768px) {
  .filter-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .right-filters {
    flex-direction: column;
  }

  .course-select, .class-select, .search-input {
    width: 100%;
  }
}
</style>