<template>
  <div class="grades-page">
    <!-- 顶部导航 -->
    <div class="page-header-container">
      <el-page-header @back="goBack" title="返回课程列表">
        <template #content>
          <span class="header-title">成绩管理</span>
        </template>
        <template #extra>
          <el-button
            type="primary"
            :icon="Refresh"
            circle
            @click="refreshData"
            :loading="loading"
            title="刷新数据"
          />
        </template>
      </el-page-header>
    </div>

    <!-- 统计卡片区 -->
    <el-row :gutter="20" class="stats-container">
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card blue-theme">
          <div class="stat-body">
            <div class="stat-info">
              <div class="stat-value">{{ totalGrades }}</div>
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
              <div class="stat-value">{{ classAverageScore }}</div>
              <div class="stat-label">班级平均分</div>
            </div>
            <el-icon class="stat-icon"><Trophy /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card green-theme">
          <div class="stat-body">
            <div class="stat-info">
              <div class="stat-value">{{ passRate }}%</div>
              <div class="stat-label">及格率</div>
            </div>
            <el-icon class="stat-icon"><PieChart /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 主内容卡片 -->
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
        :data="filteredStudentGrades"
        v-loading="loading"
        style="width: 100%"
        stripe
        highlight-current-row
        header-cell-class-name="table-header-gray"
        :default-sort="{ prop: 'progress', order: 'descending' }"
      >
        <el-table-column prop="studentNumber" label="学号" width="140" sortable fixed />
        <el-table-column prop="name" label="姓名" width="120" sortable fixed />
        <el-table-column prop="className" label="班级" width="140" sortable />

        <el-table-column label="总体进度" min-width="200" sortable prop="progress">
          <template #default="{ row }">
            <div class="progress-cell">
              <el-progress
                :percentage="row.progress"
                :color="progressColor(row.progress)"
                :stroke-width="12"
              />
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.progress)" size="small" effect="light" round>
              {{ getStatusText(row.progress) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="综合成绩" width="140" align="center" sortable prop="averageScore">
          <template #default="{ row }">
            <div v-if="row.progress >= 100">
              <el-tag
                v-if="row.averageScore !== null"
                :type="getScoreType(row.averageScore)"
                effect="dark"
                class="score-tag"
              >
                {{ row.averageScore }}
              </el-tag>
              <span v-else class="text-gray">暂无</span>
            </div>
            <span v-else class="text-gray">--</span>
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
          <el-empty description="暂无成绩数据" />
        </template>
      </el-table>

      <!-- 分页组件 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="totalGrades"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handlePageSizeChange"
          background
        />
      </div>
    </el-card>

    <!-- 成绩详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      :title="`${currentStudent?.name || '学生'} 的成绩详情`"
      width="850px"
      destroy-on-close
      class="detail-dialog"
    >
      <div v-if="currentStudent" class="detail-content" v-loading="detailLoading">
        <!-- 学生基本信息卡片 (保留了您喜欢的渐变风格) -->
        <el-card class="info-card" shadow="hover">
          <el-descriptions :column="2" border size="large" class="custom-desc">
            <el-descriptions-item label="学号">
              <span class="info-text">{{ currentStudent.studentNumber }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="姓名">
              <span class="info-text">{{ currentStudent.name }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="班级">
              <span class="info-text">{{ currentStudent.className }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="总体进度">
              <el-progress
                :percentage="currentStudent.progress"
                :color="progressColor(currentStudent.progress)"
                :stroke-width="14"
                :show-text="true"
                class="info-progress"
              />
            </el-descriptions-item>
            <el-descriptions-item label="综合成绩" :span="2">
              <div class="info-score-wrapper">
                <el-tag
                  v-if="currentStudent.averageScore !== null"
                  :type="getScoreType(currentStudent.averageScore)"
                  size="large"
                  effect="light"
                  class="large-score-tag"
                >
                  {{ currentStudent.averageScore }} 分
                </el-tag>
                <el-tag v-else type="info" size="large">暂无成绩</el-tag>
              </div>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 成绩详情标签页 -->
        <el-tabs type="border-card" class="detail-tabs">
          <!-- 视频成绩 Tab -->
          <el-tab-pane>
            <template #label>
              <span class="custom-tabs-label">
                <el-icon><VideoPlay /></el-icon> <span>视频成绩 ({{ videoScores.length }})</span>
              </span>
            </template>
            <el-table :data="videoScores" stripe height="350" style="width: 100%">
              <el-table-column label="视频信息" min-width="120">
                 <template #default="{ row }">
                   <el-tag type="info" size="small">ID: {{ row.videoId }}</el-tag>
                 </template>
              </el-table-column>
              <el-table-column label="平均成绩" width="140" align="center">
                <template #default="{ row }">
                  <span :class="getScoreTextClass(row.averageScore)">{{ row.averageScore }} 分</span>
                </template>
              </el-table-column>
              <el-table-column prop="attemptCount" label="答题次数" width="100" align="center" />
              <el-table-column label="分析" width="180">
                <template #default="{ row }">
                  <el-progress :percentage="row.averageScore" :color="getScoreColor(row.averageScore)" />
                </template>
              </el-table-column>
              <template #empty>
                <el-empty description="暂无视频成绩" :image-size="80" />
              </template>
            </el-table>
          </el-tab-pane>

          <!-- 文档成绩 Tab -->
          <el-tab-pane>
            <template #label>
              <span class="custom-tabs-label">
                <el-icon><Document /></el-icon> <span>文档成绩 ({{ documentScores.length }})</span>
              </span>
            </template>
            <el-table :data="documentScores" stripe height="350" style="width: 100%">
              <el-table-column label="文档信息" min-width="120">
                 <template #default="{ row }">
                   <el-tag type="info" size="small">ID: {{ row.documentId }}</el-tag>
                 </template>
              </el-table-column>
              <el-table-column label="平均成绩" width="140" align="center">
                <template #default="{ row }">
                  <span :class="getScoreTextClass(row.averageScore)">{{ row.averageScore }} 分</span>
                </template>
              </el-table-column>
              <el-table-column prop="attemptCount" label="答题次数" width="100" align="center" />
              <el-table-column label="分析" width="180">
                <template #default="{ row }">
                  <el-progress :percentage="row.averageScore" :color="getScoreColor(row.averageScore)" />
                </template>
              </el-table-column>
              <template #empty>
                <el-empty description="暂无文档成绩" :image-size="80" />
              </template>
            </el-table>
          </el-tab-pane>

          <!-- 兜底显示 Tab -->
          <el-tab-pane v-if="allScores.length > 0">
             <template #label>
              <span class="custom-tabs-label">
                <el-icon><Tickets /></el-icon> <span>所有记录 ({{ allScores.length }})</span>
              </span>
            </template>
            <el-alert title="提示：此处显示所有已提交的答题记录" type="warning" :closable="false" show-icon style="margin-bottom: 10px;" />
            <el-table :data="allScores" stripe height="310" style="width: 100%">
               <el-table-column prop="topic" label="主题" min-width="150" show-overflow-tooltip />
               <el-table-column label="成绩" width="120" align="center">
                  <template #default="{ row }">
                     <el-tag :type="getScoreType(row.totalScore)">{{ row.totalScore }} 分</el-tag>
                  </template>
               </el-table-column>
               <el-table-column label="时间" width="160" align="center">
                  <template #default="{ row }">
                     <span class="text-small">{{ formatDateTime(row.createdAt) }}</span>
                  </template>
               </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>

        <el-empty
          v-if="videoScores.length === 0 && documentScores.length === 0 && allScores.length === 0"
          description="暂无任何成绩记录"
          class="dialog-empty"
        />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
        </div>
      </template>
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
  Trophy,
  PieChart,
  Search,
  Refresh,
  VideoPlay,
  Document,
  Tickets,
  Check
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

// API 配置
const API_BASE = import.meta?.env?.VITE_API_BASE_URL || '/api'
const api = axios.create({ baseURL: API_BASE, timeout: 20000 })
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token') || localStorage.getItem('userToken')
  if (token) config.headers = { ...(config.headers || {}), Authorization: `Bearer ${token}` }
  return config
})

// 状态变量
const courseId = ref(null)
const courseList = ref([])
const loading = ref(false)
const loadingCourses = ref(false)
const studentGradesList = ref([])

// 筛选与分页
const searchText = ref('')
const selectedClass = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

// 详情弹窗
const detailVisible = ref(false)
const currentStudent = ref(null)
const detailLoading = ref(false)
const videoScores = ref([])
const documentScores = ref([])
const allScores = ref([])

// --- 计算属性 ---

// 当前课程名称
const courseName = computed(() => {
  const course = courseList.value.find(c => (c.courseId || c.id) === courseId.value)
  return course ? (course.courseName || course.title || '课程') : '课程'
})

// 提取班级列表
const classList = computed(() => {
  const classes = new Set()
  studentGradesList.value.forEach(s => {
    if (s.className) classes.add(s.className)
  })
  return Array.from(classes).sort()
})

// 筛选后的所有数据（用于统计）
const filteredAllGrades = computed(() => {
  let result = studentGradesList.value
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

// --- 统计指标 ---
const totalGrades = computed(() => filteredAllGrades.value.length)

const classAverageScore = computed(() => {
  const studentsWithScore = filteredAllGrades.value.filter(s => s.averageScore !== null)
  if (studentsWithScore.length === 0) return '0.0'
  const sum = studentsWithScore.reduce((acc, s) => acc + s.averageScore, 0)
  return (sum / studentsWithScore.length).toFixed(1)
})

const passRate = computed(() => {
  const studentsWithScore = filteredAllGrades.value.filter(s => s.averageScore !== null)
  if (studentsWithScore.length === 0) return '0.0'
  const passed = studentsWithScore.filter(s => s.averageScore >= 60).length
  return ((passed / studentsWithScore.length) * 100).toFixed(1)
})

// 分页后的数据
const filteredStudentGrades = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredAllGrades.value.slice(start, end)
})

// --- 辅助函数 ---

const goBack = () => router.back()

const handlePageChange = (page) => {
  currentPage.value = page
}

const handlePageSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
}

const progressColor = (pct) => {
  if (pct >= 100) return '#67c23a'
  if (pct >= 80) return '#409eff'
  if (pct >= 60) return '#e6a23c'
  return '#f56c6c'
}

const getScoreType = (score) => {
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
}

const getScoreColor = (score) => {
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
}

const getScoreTextClass = (score) => {
  if (score >= 80) return 'text-success'
  if (score >= 60) return 'text-warning'
  return 'text-danger'
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

// --- 业务逻辑 ---

const refreshData = () => {
  if (courseId.value) {
    loadCourseGrades()
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
    console.error('加载课程列表失败:', err)
    ElMessage.error('课程列表加载失败')
    courseList.value = []
  } finally {
    loadingCourses.value = false
  }
}

const onCourseChange = () => {
  router.replace(`/teacher/courses/${courseId.value}/grades`)
  currentPage.value = 1
  selectedClass.value = ''
  searchText.value = ''
  loadCourseGrades()
}

const loadCourseGrades = async () => {
  if (!courseId.value) return

  loading.value = true
  studentGradesList.value = []

  try {
    // 1. 获取选课学生
    const enrollRes = await api.get(`/teacher/enrollments/students`, { params: { courseId: courseId.value } })
    const enrollBody = enrollRes?.data
    const students = (enrollBody && Number(enrollBody.code) === 200 && Array.isArray(enrollBody.data)) ? enrollBody.data : []

    if (students.length === 0) {
      loading.value = false
      return
    }

    // 2. 并发查询进度和成绩
    // 优化：使用 Promise.all 并行处理请求
    const gradesPromises = students.map(async (student) => {
      try {
        // 并发查询
        const [progRes, scoreRes] = await Promise.all([
          api.get(`/progress/course`, { params: { studentId: student.id, courseId: courseId.value } }).catch(() => null),
          api.get(`/aiexam/average-score`, { params: { studentId: student.id, courseId: courseId.value } }).catch(() => null)
        ])

        // 处理进度
        let progress = 0
        let lastStudyTime = null
        if (progRes?.data?.code === 200 && progRes.data.data) {
          const d = progRes.data.data
          progress = Number(d.completionPercentage || d.coursePercent || 0)
          if (progress >= 0 && progress <= 1) progress *= 100
          progress = Math.max(0, Math.min(100, progress))
          lastStudyTime = d.updatedAt || d.lastUpdated
        }

        // 处理成绩
        let averageScore = null
        if (progress >= 100 && scoreRes?.data?.code === 200 && scoreRes.data.data) {
           averageScore = Number(scoreRes.data.data.averageScore) || null
        }

        return {
          id: student.id,
          studentNumber: student.studentNumber || student.studentId || '-',
          name: student.name || '-',
          className: student.className || '-',
          progress: Math.round(progress),
          averageScore: averageScore,
          lastStudyTime
        }
      } catch (err) {
        console.warn(`Error fetching data for student ${student.id}`, err)
        return {
          id: student.id,
          studentNumber: student.studentNumber || '-',
          name: student.name || '未知',
          className: student.className || '-',
          progress: 0,
          averageScore: null,
          lastStudyTime: null
        }
      }
    })

    studentGradesList.value = await Promise.all(gradesPromises)
  } catch (err) {
    console.error('加载成绩失败:', err)
    ElMessage.error('获取成绩数据失败')
  } finally {
    loading.value = false
  }
}

const viewDetail = async (student) => {
  currentStudent.value = student
  detailVisible.value = true
  videoScores.value = []
  documentScores.value = []
  allScores.value = []
  detailLoading.value = true

  try {
    const res = await api.get('/aiexam/detailed-scores', {
      params: { studentId: student.id, courseId: courseId.value }
    })

    if (res?.data?.code === 200 && res.data.data) {
      const data = res.data.data
      videoScores.value = Array.isArray(data.videoScores) ? data.videoScores : []
      documentScores.value = Array.isArray(data.documentScores) ? data.documentScores : []

      // 兜底逻辑：如果无分类成绩，尝试获取所有列表
      if (videoScores.value.length === 0 && documentScores.value.length === 0) {
         try {
           const allRes = await api.get('/aiexam/list', {
             params: { studentId: student.id, courseId: courseId.value }
           })
           if (allRes?.data?.code === 200 && Array.isArray(allRes.data.data)) {
             allScores.value = allRes.data.data
               .filter(exam => exam.status === 'submitted' && exam.totalScore != null)
               .map(exam => ({
                 id: exam.id,
                 topic: exam.topic || exam.courseName || '未命名测试',
                 totalScore: exam.totalScore,
                 questionCount: exam.questionCount || 0,
                 createdAt: exam.createdAt
               }))
           }
         } catch(e) { console.warn(e) }
      }
    }
  } catch (err) {
    console.error('详细成绩加载失败:', err)
    ElMessage.error('无法加载详细成绩')
  } finally {
    detailLoading.value = false
  }
}

onMounted(async () => {
  await loadCourseList()
  const routeCourseId = Number(route.params.id || route.params.courseId)
  if (routeCourseId) {
    courseId.value = routeCourseId
  } else if (courseList.value.length > 0) {
    courseId.value = courseList.value[0].courseId || courseList.value[0].id
  }

  if (courseId.value) loadCourseGrades()
})

watch([searchText, selectedClass], () => {
  currentPage.value = 1
})
</script>

<style scoped>
.grades-page {
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
.text-small {
  font-size: 12px;
  color: #606266;
}

.text-success { color: #67c23a; font-weight: bold; }
.text-warning { color: #e6a23c; font-weight: bold; }
.text-danger { color: #f56c6c; font-weight: bold; }

.score-tag {
  min-width: 40px;
  text-align: center;
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

/* 学生信息卡片 (保持渐变风格但优化) */
.info-card {
  margin-bottom: 20px;
  border-radius: 8px;
  background: linear-gradient(135deg, #fdfbfb 0%, #ebedee 100%);
  border: none;
}

.info-text {
  font-weight: 500;
  color: #303133;
}

.info-progress {
  width: 200px;
}

.info-score-wrapper {
  display: flex;
  align-items: center;
}

.large-score-tag {
  font-size: 16px;
  padding: 8px 20px;
  height: auto;
}

/* 详情 Tab */
.detail-tabs {
  min-height: 400px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.custom-tabs-label {
  display: flex;
  align-items: center;
  gap: 6px;
}

.dialog-empty {
  padding: 40px 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}

/* 响应式 */
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