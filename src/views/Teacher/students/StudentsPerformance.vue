<template>
  <div class="students-performance">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2>学习表现分析</h2>
      <div class="header-actions">
        <el-button type="primary" :icon="Refresh" @click="refreshData">刷新数据</el-button>
      </div>
    </div>

    <!-- 筛选条件 -->
    <div class="filter-section">
      <el-card shadow="never">
        <div class="filter-content">
          <el-select v-model="filters.courseId" placeholder="选择课程" clearable @change="handleFilterChange" style="width: 200px;">
            <el-option label="全部课程" value="" />
            <el-option
                v-for="course in courseOptions"
                :key="course.id"
                :label="course.name"
                :value="course.id"
            />
          </el-select>

          <el-select v-model="filters.classId" placeholder="选择班级" clearable @change="handleFilterChange" style="width: 200px;">
            <el-option label="全部班级" value="" />
            <el-option
                v-for="classItem in classOptions"
                :key="classItem.id"
                :label="classItem.name"
                :value="classItem.id"
            />
          </el-select>

          <el-date-picker
              v-model="filters.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              @change="handleFilterChange"
              style="width: 350px;"
          />

          <el-select v-model="filters.performanceLevel" placeholder="表现等级" clearable @change="handleFilterChange" style="width: 150px;">
            <el-option label="全部等级" value="" />
            <el-option label="优秀" value="excellent" />
            <el-option label="良好" value="good" />
            <el-option label="一般" value="average" />
            <el-option label="需关注" value="concern" />
          </el-select>

          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </div>
      </el-card>
    </div>

    <!-- 统计指标 -->
    <div class="metrics-section">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover" class="metric-card">
            <div class="metric-content">
              <div class="metric-icon students">
                <el-icon><User /></el-icon>
              </div>
              <div class="metric-info">
                <h3>{{ metrics.totalStudents }}</h3>
                <p>总学生数</p>
                <span class="metric-trend" :class="metrics.studentTrend >= 0 ? 'up' : 'down'">
                  <el-icon><TrendCharts /></el-icon>
                  {{ metrics.studentTrend >= 0 ? '+' : '' }}{{ metrics.studentTrend }}%
                </span>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover" class="metric-card">
            <div class="metric-content">
              <div class="metric-icon time">
                <el-icon><Clock /></el-icon>
              </div>
              <div class="metric-info">
                <h3>{{ metrics.avgStudyTime }}h</h3>
                <p>平均学习时长</p>
                <span class="metric-trend" :class="metrics.timeTrend >= 0 ? 'up' : 'down'">
                  <el-icon><TrendCharts /></el-icon>
                  {{ metrics.timeTrend >= 0 ? '+' : '' }}{{ metrics.timeTrend }}%
                </span>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover" class="metric-card">
            <div class="metric-content">
              <div class="metric-icon score">
                <el-icon><Document /></el-icon>
              </div>
              <div class="metric-info">
                <h3>{{ metrics.avgScore }}</h3>
                <p>平均成绩</p>
                <span class="metric-trend" :class="metrics.scoreTrend >= 0 ? 'up' : 'down'">
                  <el-icon><TrendCharts /></el-icon>
                  {{ metrics.scoreTrend >= 0 ? '+' : '' }}{{ metrics.scoreTrend }}%
                </span>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover" class="metric-card">
            <div class="metric-content">
              <div class="metric-icon concern">
                <el-icon><Warning /></el-icon>
              </div>
              <div class="metric-info">
                <h3>{{ metrics.concernStudents }}</h3>
                <p>需关注学生</p>
                <span class="metric-trend" :class="metrics.concernTrend >= 0 ? 'up' : 'down'">
                  <el-icon><TrendCharts /></el-icon>
                  {{ metrics.concernTrend >= 0 ? '+' : '' }}{{ metrics.concernTrend }}%
                </span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 图表分析 -->
    <div class="charts-section">
      <el-row :gutter="20">
        <el-col :xs="24" :lg="12">
          <el-card shadow="hover">
            <template #header>
              <div class="chart-header">
                <h3>成绩分布分析</h3>
                <el-select v-model="scoreChartCourse" placeholder="选择课程" size="small" @change="updateScoreChart" style="width: 150px;">
                  <el-option label="全部课程" value="all" />
                  <el-option
                      v-for="course in courseOptions"
                      :key="course.id"
                      :label="course.name"
                      :value="course.id"
                  />
                </el-select>
              </div>
            </template>
            <div ref="scoreChartRef" style="height: 300px;"></div>
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="12">
          <el-card shadow="hover">
            <template #header>
              <div class="chart-header">
                <h3>学习时长分布</h3>
                <el-select v-model="timeChartClass" placeholder="选择班级" size="small" @change="updateTimeChart" style="width: 150px;">
                  <el-option label="全部班级" value="all" />
                  <el-option
                      v-for="classItem in classOptions"
                      :key="classItem.id"
                      :label="classItem.name"
                      :value="classItem.id"
                  />
                </el-select>
              </div>
            </template>
            <div ref="timeChartRef" style="height: 300px;"></div>
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="12">
          <el-card shadow="hover">
            <template #header>
              <div class="chart-header">
                <h3>课程完成情况</h3>
              </div>
            </template>
            <div ref="completionChartRef" style="height: 300px;"></div>
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="12">
          <el-card shadow="hover">
            <template #header>
              <div class="chart-header">
                <h3>学习活跃度</h3>
                <el-select v-model="activityChartClass" placeholder="选择班级" size="small" @change="updateActivityChart" style="width: 150px;">
                  <el-option label="全部班级" value="all" />
                  <el-option
                      v-for="classItem in classOptions"
                      :key="classItem.id"
                      :label="classItem.name"
                      :value="classItem.id"
                  />
                </el-select>
              </div>
            </template>
            <div ref="activityChartRef" style="height: 300px;"></div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 学生数据表格 -->
    <div class="table-section">
      <el-card shadow="hover">
        <template #header>
          <div class="table-header">
            <h3>学生表现详情</h3>
            <div class="table-actions">
              <el-button :icon="Refresh" @click="refreshTable">刷新</el-button>
            </div>
          </div>
        </template>

        <el-table
            :data="tableData"
            v-loading="tableLoading"
            stripe
            style="width: 100%"
        >
          <el-table-column prop="studentId" label="学号" width="120" />
          <el-table-column prop="name" label="姓名" width="100" />
          <el-table-column prop="className" label="班级" width="150" />
          <el-table-column prop="courseName" label="课程" width="150" />
          <el-table-column prop="studyTime" label="学习时长(h)" width="120" align="center">
            <template #default="scope">
              <span :class="getStudyTimeClass(scope.row.studyTime)">{{ scope.row.studyTime }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="completionRate" label="完成率" width="100" align="center">
            <template #default="scope">
              <el-tag :type="getCompletionType(scope.row.completionRate)" size="small">
                {{ scope.row.completionRate }}%
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="avgScore" label="平均成绩" width="100" align="center">
            <template #default="scope">
              <span :class="getScoreClass(scope.row.avgScore)">{{ scope.row.avgScore }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="lastLogin" label="最后登录" width="120" />
          <el-table-column label="表现等级" width="100" align="center">
            <template #default="scope">
              <el-tag :type="getPerformanceType(scope.row.performanceLevel)" effect="light">
                {{ getPerformanceText(scope.row.performanceLevel) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right" align="center">
            <template #default="scope">
              <el-button link type="primary" @click="viewStudentDetail(scope.row)">详情</el-button>
              <el-button link type="warning" @click="sendReminder(scope.row)">提醒</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-container">
          <el-pagination
              v-model:current-page="pagination.currentPage"
              v-model:page-size="pagination.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="pagination.total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
          />
        </div>
      </el-card>
    </div>

    <!-- 学生详情对话框 -->
    <el-dialog
        v-model="detailDialogVisible"
        :title="selectedStudent ? `学生详情 - ${selectedStudent.name}` : '学生详情'"
        width="60%"
        top="10vh"
        center
        class="student-detail-dialog"
    >
      <StudentDetail :student="selectedStudent" v-if="detailDialogVisible && selectedStudent" />
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  Search,
  User,
  Clock,
  Document,
  Warning,
  TrendCharts
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import StudentDetail from '@/components/StudentDetail.vue'
import axios from 'axios'

// 模拟数据 - 用于调试
const mockData = {
  // 指标数据
  metrics: {
    totalStudents: 156,
    avgStudyTime: 12.5,
    avgScore: 78.6,
    concernStudents: 18,
    studentTrend: 5.2,
    timeTrend: 3.1,
    scoreTrend: -1.2,
    concernTrend: 8.5
  },

  // 学生数据
  students: [
    {
      id: 1,
      studentId: '20230001',
      name: '张三',
      className: '计算机科学与技术1班',
      courseName: 'Web前端开发',
      studyTime: 15.5,
      completionRate: 80,
      avgScore: 85,
      lastLogin: '2024-01-20',
      performanceLevel: 'excellent',
      email: 'zhangsan@email.com',
      phone: '13800138001'
    },
    {
      id: 2,
      studentId: '20230002',
      name: '李四',
      className: '软件工程2班',
      courseName: '数据结构',
      studyTime: 20.2,
      completionRate: 83,
      avgScore: 92,
      lastLogin: '2024-01-19',
      performanceLevel: 'excellent',
      email: 'lisi@email.com',
      phone: '13800138002'
    },
    {
      id: 3,
      studentId: '20230003',
      name: '王五',
      className: '计算机科学与技术1班',
      courseName: '数据库原理',
      studyTime: 8.7,
      completionRate: 60,
      avgScore: 65,
      lastLogin: '2024-01-18',
      performanceLevel: 'concern',
      email: 'wangwu@email.com',
      phone: '13800138003'
    },
    {
      id: 4,
      studentId: '20230004',
      name: '赵六',
      className: '软件工程2班',
      courseName: 'Web前端开发',
      studyTime: 18.3,
      completionRate: 93,
      avgScore: 88,
      lastLogin: '2024-01-20',
      performanceLevel: 'good',
      email: 'zhaoliu@email.com',
      phone: '13800138004'
    },
    {
      id: 5,
      studentId: '20230005',
      name: '钱七',
      className: '网络工程1班',
      courseName: '数据结构',
      studyTime: 9.2,
      completionRate: 58,
      avgScore: 52,
      lastLogin: '2024-01-17',
      performanceLevel: 'concern',
      email: 'qianqi@email.com',
      phone: '13800138005'
    },
    {
      id: 6,
      studentId: '20230006',
      name: '孙八',
      className: '人工智能1班',
      courseName: '操作系统',
      studyTime: 14.8,
      completionRate: 75,
      avgScore: 78,
      lastLogin: '2024-01-19',
      performanceLevel: 'good',
      email: 'sunba@email.com',
      phone: '13800138006'
    },
    {
      id: 7,
      studentId: '20230007',
      name: '周九',
      className: '数据科学2班',
      courseName: '计算机网络',
      studyTime: 11.3,
      completionRate: 68,
      avgScore: 72,
      lastLogin: '2024-01-16',
      performanceLevel: 'average',
      email: 'zhoujiu@email.com',
      phone: '13800138007'
    },
    {
      id: 8,
      studentId: '20230008',
      name: '吴十',
      className: '计算机科学与技术1班',
      courseName: 'Web前端开发',
      studyTime: 16.7,
      completionRate: 88,
      avgScore: 91,
      lastLogin: '2024-01-20',
      performanceLevel: 'excellent',
      email: 'wushi@email.com',
      phone: '13800138008'
    }
  ],

  // 图表数据
  charts: {
    scoreDistribution: [35, 45, 50, 20, 6],
    timeDistribution: [15, 32, 45, 28, 10],
    completionData: [85, 78, 92, 65, 88, 75, 68, 91],
    activityData: [25, 30, 28, 32, 35, 30, 40]
  }
}

export default {
  name: 'StudentsPerformance',
  components: {
    Warning,
    Document,
    Clock,
    TrendCharts,
    User,
    StudentDetail
  },
  setup() {
    // 响应式数据
    const filters = reactive({
      courseId: '',
      classId: '',
      dateRange: [],
      performanceLevel: '',
      searchText: ''
    })

    const pagination = reactive({
      currentPage: 1,
      pageSize: 10,
      total: 0
    })

    const metrics = reactive({ ...mockData.metrics })
    const tableData = ref([])
    const tableLoading = ref(false)
    const detailDialogVisible = ref(false)
    const selectedStudent = ref(null)

    // 下拉选项数据
    const courseOptions = ref([])

    const classOptions = ref([
      { id: 1, name: '计算机科学与技术1班' },
      { id: 2, name: '软件工程2班' },
      { id: 3, name: '网络工程1班' },
      { id: 4, name: '人工智能1班' },
      { id: 5, name: '数据科学2班' }
    ])

    // 图表相关
    const scoreChartRef = ref(null)
    const timeChartRef = ref(null)
    const completionChartRef = ref(null)
    const activityChartRef = ref(null)
    const scoreChartCourse = ref('all')
    const timeChartClass = ref('all')
    const activityChartClass = ref('all')

    let scoreChartInstance = null
    let timeChartInstance = null
    let completionChartInstance = null
    let activityChartInstance = null

    // API 函数 - 集成在组件中
    // Axios 实例（与全站一致）
    const API_BASE = (import.meta?.env?.VITE_API_BASE_URL || (window?.location?.port === '4173' ? 'http://localhost:9999/api' : '/api'))
    const apiClient = axios.create({ baseURL: API_BASE, timeout: 20000 })
    apiClient.interceptors.request.use((config) => {
      try {
        const token = localStorage.getItem('token') || localStorage.getItem('userToken')
        if (token) config.headers = { ...(config.headers || {}), Authorization: `Bearer ${token}` }
      } catch {}
      return config
    })

    // 工具函数
    function formatDate(input) {
      try {
        if (!input) return '-'
        const d = new Date(input)
        if (isNaN(d.getTime())) return '-'
        const y = d.getFullYear()
        const m = String(d.getMonth() + 1).padStart(2, '0')
        const day = String(d.getDate()).padStart(2, '0')
        return `${y}-${m}-${day}`
      } catch { return '-' }
    }
    function toHours(seconds) {
      const s = Number(seconds || 0)
      return Math.round((s / 3600) * 10) / 10
    }

    // 加载课程选项
    const loadCourseOptions = async () => {
      try {
        const res = await apiClient.get('/course/list')
        const raw = res?.data
        const list = Array.isArray(raw?.data) ? raw.data : (Array.isArray(raw) ? raw : [])
        courseOptions.value = list.map(c => ({ id: c.courseId || c.id, name: c.courseName || c.name }))
      } catch (e) {
        courseOptions.value = []
      }
    }

    // 共享缓存，供图表复用
    let lastFetchedItems = []

    async function realFetchStudents(params) {
      // 仅在选择了课程时加载真实数据
      if (!params.courseId) {
        lastFetchedItems = []
        return { items: [], total: 0 }
      }
      // 获取课程下学生
      const stuRes = await apiClient.get('/teacher/enrollments/students', { params: { courseId: params.courseId } })
      const raw = stuRes?.data
      const students = Array.isArray(raw?.data) ? raw.data : (Array.isArray(raw) ? raw : [])
      // 并发限制
      const maxConcurrency = 8
      let index = 0
      const results = []
      const workers = new Array(Math.min(maxConcurrency, students.length)).fill(0).map(async () => {
        while (index < students.length) {
          const i = index++
          const s = students[i]
          try {
            const prog = await apiClient.get('/progress/course', { params: { studentId: s.id || s.studentId, courseId: params.courseId } })
            const lp = prog?.data?.data || prog?.data || {}
            const completion = Math.round(lp?.completionPercentage || 0)
            const hours = toHours(lp?.timeSpent)
            const perf = completion >= 85 ? 'excellent' : (completion >= 70 ? 'good' : (completion >= 60 ? 'average' : 'concern'))
            const courseName = (() => {
              const found = courseOptions.value.find(c => c.id == params.courseId)
              return found ? found.name : ''
            })()
            results.push({
              id: s.id || s.studentId,
              studentId: s.studentNumber || s.sid || s.username || '',
              name: s.name || s.realName || s.username || '-',
              className: s.className || s.grade || '-',
              courseName,
              studyTime: hours,
              completionRate: completion,
              avgScore: '-',
              lastLogin: formatDate(lp?.lastStudyTime),
              performanceLevel: perf,
              email: s.email,
              phone: s.phone
            })
          } catch (e) {
            // 忽略单条错误
          }
        }
      })
      await Promise.all(workers)
      // 搜索过滤
      let items = results
      if (params.classId) {
        const classItem = classOptions.value.find(c => c.id == params.classId)
        if (classItem) items = items.filter(x => x.className === classItem.name)
      }
      if (params.performanceLevel) {
        items = items.filter(x => x.performanceLevel === params.performanceLevel)
      }
      // 日期范围过滤针对 lastLogin
      if (Array.isArray(params.dateRange) && params.dateRange.length === 2) {
        const [start, end] = params.dateRange
        const startTime = new Date(start).getTime()
        const endTime = new Date(end).getTime()
        items = items.filter(x => {
          const t = new Date(x.lastLogin).getTime()
          return !isNaN(t) && t >= startTime && t <= endTime
        })
      }
      lastFetchedItems = items
      return { items, total: items.length }
    }

    const api = {
      // 获取指标数据
      async getMetrics(params) {
        try {
          const { items } = await realFetchStudents({ ...params })
          const total = items.length
          const avgStudyTime = total === 0 ? 0 : Math.round((items.reduce((s, x) => s + (Number(x.studyTime) || 0), 0) / total) * 10) / 10
          const concernStudents = items.filter(x => x.performanceLevel === 'concern').length
          const avgScore = total === 0 ? 0 : Math.round((items.reduce((s, x) => s + (Number(x.completionRate) || 0), 0) / total))
          const metricsData = {
            totalStudents: total,
            avgStudyTime,
            avgScore,
            concernStudents,
            studentTrend: 0,
            timeTrend: 0,
            scoreTrend: 0,
            concernTrend: 0
          }
          return { success: true, data: metricsData }
        } catch (e) {
          return { success: true, data: { ...mockData.metrics } }
        }
      },

      // 获取学生列表
      async getStudents(params) {
        try {
          const { items, total } = await realFetchStudents(params)
          const startIndex = (params.page - 1) * params.pageSize
          const endIndex = startIndex + parseInt(params.pageSize)
          const paginatedData = items.slice(startIndex, endIndex)
          return { success: true, data: { items: paginatedData, total, page: params.page, pageSize: params.pageSize } }
        } catch (e) {
          // 失败时退回模拟
          const startIndex = (params.page - 1) * params.pageSize
          const endIndex = startIndex + parseInt(params.pageSize)
          const fallback = mockData.students.slice(startIndex, endIndex)
          return { success: true, data: { items: fallback, total: mockData.students.length, page: params.page, pageSize: params.pageSize } }
        }
      },

      // 获取图表数据
      async getChartData(type, params) {
        const items = lastFetchedItems && lastFetchedItems.length ? lastFetchedItems : mockData.students
        const charts = {
          scoreDistribution: [0, 0, 0, 0, 0],
          timeDistribution: [0, 0, 0, 0, 0],
          completionData: [],
          activityData: [25, 30, 28, 32, 35, 30, 40]
        }
        // 用完成率作为成绩分布
        items.forEach(x => {
          const c = Number(x.completionRate || 0)
          if (c >= 90) charts.scoreDistribution[0]++
          else if (c >= 80) charts.scoreDistribution[1]++
          else if (c >= 70) charts.scoreDistribution[2]++
          else if (c >= 60) charts.scoreDistribution[3]++
          else charts.scoreDistribution[4]++
        })
        // 学习时长分布
        items.forEach(x => {
          const h = Number(x.studyTime || 0)
          if (h < 5) charts.timeDistribution[0]++
          else if (h < 10) charts.timeDistribution[1]++
          else if (h < 15) charts.timeDistribution[2]++
          else if (h < 20) charts.timeDistribution[3]++
          else charts.timeDistribution[4]++
        })
        // 完成率曲线
        charts.completionData = items.map(x => Number(x.completionRate || 0))
        return { success: true, data: charts }
      },

      // 发送提醒
      async sendReminder(studentId) {
        console.log('API调用: sendReminder', studentId)
        await new Promise(resolve => setTimeout(resolve, 500))
        return { success: true, message: '提醒发送成功' }
      }
    }

    // 数据获取函数
    const fetchMetrics = async () => {
      try {
        const response = await api.getMetrics(filters)
        if (response.success) {
          Object.assign(metrics, response.data)
        }
      } catch (error) {
        console.error('获取指标数据失败:', error)
        ElMessage.error('获取指标数据失败')
      }
    }

    const fetchStudents = async () => {
      tableLoading.value = true
      try {
        const params = {
          ...filters,
          page: pagination.currentPage,
          pageSize: pagination.pageSize
        }

        const response = await api.getStudents(params)
        if (response.success) {
          tableData.value = response.data.items
          pagination.total = response.data.total
        }
      } catch (error) {
        console.error('获取学生数据失败:', error)
        ElMessage.error('获取学生数据失败')
        // 使用模拟数据作为后备
        const startIndex = (pagination.currentPage - 1) * pagination.pageSize
        const endIndex = startIndex + pagination.pageSize
        tableData.value = mockData.students.slice(startIndex, endIndex)
        pagination.total = mockData.students.length
      } finally {
        tableLoading.value = false
      }
    }

    // 图表相关函数
    const initCharts = () => {
      nextTick(() => {
        // 确保DOM元素存在
        setTimeout(() => {
          if (scoreChartRef.value) {
            scoreChartInstance = echarts.init(scoreChartRef.value)
            updateScoreChart()
          }
          if (timeChartRef.value) {
            timeChartInstance = echarts.init(timeChartRef.value)
            updateTimeChart()
          }
          if (completionChartRef.value) {
            completionChartInstance = echarts.init(completionChartRef.value)
            updateCompletionChart()
          }
          if (activityChartRef.value) {
            activityChartInstance = echarts.init(activityChartRef.value)
            updateActivityChart()
          }
        }, 100)
      })
    }

    const updateScoreChart = async () => {
      if (!scoreChartInstance) return

      try {
        const response = await api.getChartData('score', { courseId: scoreChartCourse.value })
        const chartData = response.data.scoreDistribution

        const option = {
          tooltip: {
            trigger: 'item',
            formatter: '{a} <br/>{b}: {c} ({d}%)'
          },
          legend: {
            orient: 'vertical',
            right: 10,
            top: 'center',
            data: ['优秀(90-100)', '良好(80-89)', '中等(70-79)', '及格(60-69)', '不及格(<60)']
          },
          series: [{
            name: '成绩分布',
            type: 'pie',
            radius: ['40%', '70%'],
            avoidLabelOverlap: false,
            itemStyle: {
              borderRadius: 10,
              borderColor: '#fff',
              borderWidth: 2
            },
            label: {
              show: false,
              position: 'center'
            },
            emphasis: {
              label: {
                show: true,
                fontSize: 18,
                fontWeight: 'bold'
              }
            },
            labelLine: {
              show: false
            },
            data: [
              { value: chartData[0], name: '优秀(90-100)' },
              { value: chartData[1], name: '良好(80-89)' },
              { value: chartData[2], name: '中等(70-79)' },
              { value: chartData[3], name: '及格(60-69)' },
              { value: chartData[4], name: '不及格(<60)' }
            ]
          }]
        }

        scoreChartInstance.setOption(option)
      } catch (error) {
        console.error('更新成绩图表失败:', error)
      }
    }

    const updateTimeChart = async () => {
      if (!timeChartInstance) return

      try {
        const response = await api.getChartData('time', { classId: timeChartClass.value })
        const chartData = response.data.timeDistribution

        const option = {
          tooltip: {
            trigger: 'axis',
            axisPointer: {
              type: 'shadow'
            }
          },
          xAxis: {
            type: 'category',
            data: ['0-5h', '5-10h', '10-15h', '15-20h', '20+h']
          },
          yAxis: {
            type: 'value'
          },
          series: [{
            data: chartData,
            type: 'bar',
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#83bff6' },
                { offset: 0.5, color: '#188df0' },
                { offset: 1, color: '#188df0' }
              ])
            }
          }]
        }

        timeChartInstance.setOption(option)
      } catch (error) {
        console.error('更新时长图表失败:', error)
      }
    }

    const updateCompletionChart = async () => {
      if (!completionChartInstance) return

      try {
        const response = await api.getChartData('completion')
        const chartData = response.data.completionData

        const option = {
          tooltip: {
            trigger: 'axis'
          },
          xAxis: {
            type: 'category',
            data: courseOptions.value.map(course => course.name)
          },
          yAxis: {
            type: 'value',
            max: 100
          },
          series: [{
            data: chartData,
            type: 'line',
            smooth: true,
            lineStyle: {
              width: 3,
              color: '#67c23a'
            },
            itemStyle: {
              color: '#67c23a'
            },
            areaStyle: {
              color: {
                type: 'linear',
                x: 0,
                y: 0,
                x2: 0,
                y2: 1,
                colorStops: [{
                  offset: 0, color: 'rgba(103, 194, 58, 0.3)'
                }, {
                  offset: 1, color: 'rgba(103, 194, 58, 0.1)'
                }]
              }
            }
          }]
        }

        completionChartInstance.setOption(option)
      } catch (error) {
        console.error('更新完成率图表失败:', error)
      }
    }

    const updateActivityChart = async () => {
      if (!activityChartInstance) return

      try {
        const response = await api.getChartData('activity', { classId: activityChartClass.value })
        const chartData = response.data.activityData

        const option = {
          tooltip: {
            trigger: 'axis'
          },
          xAxis: {
            type: 'category',
            data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
          },
          yAxis: {
            type: 'value'
          },
          series: [{
            data: chartData,
            type: 'line',
            smooth: true,
            lineStyle: {
              width: 3,
              color: '#e6a23c'
            },
            itemStyle: {
              color: '#e6a23c'
            },
            areaStyle: {
              color: {
                type: 'linear',
                x: 0,
                y: 0,
                x2: 0,
                y2: 1,
                colorStops: [{
                  offset: 0, color: 'rgba(230, 162, 60, 0.3)'
                }, {
                  offset: 1, color: 'rgba(230, 162, 60, 0.1)'
                }]
              }
            }
          }]
        }

        activityChartInstance.setOption(option)
      } catch (error) {
        console.error('更新活跃度图表失败:', error)
      }
    }

    // 事件处理函数
    const handleFilterChange = () => {
      pagination.currentPage = 1
      refreshData()
    }

    const handleSearch = () => {
      pagination.currentPage = 1
      refreshData()
    }

    const handleReset = () => {
      Object.assign(filters, {
        courseId: '',
        classId: '',
        dateRange: [],
        performanceLevel: '',
        searchText: ''
      })
      pagination.currentPage = 1
      refreshData()
    }

    const handleSizeChange = (newSize) => {
      pagination.pageSize = newSize
      pagination.currentPage = 1
      fetchStudents()
    }

    const handleCurrentChange = (newPage) => {
      pagination.currentPage = newPage
      fetchStudents()
    }

    // 工具函数
    const getCompletionType = (rate) => {
      if (rate >= 80) return 'success'
      if (rate >= 60) return 'warning'
      return 'danger'
    }

    const getScoreClass = (score) => {
      if (score >= 90) return 'high-score'
      if (score >= 70) return 'medium-score'
      return 'low-score'
    }

    const getStudyTimeClass = (time) => {
      if (time >= 15) return 'high-time'
      if (time >= 10) return 'medium-time'
      return 'low-time'
    }

    const getPerformanceType = (level) => {
      const types = {
        excellent: 'success',
        good: 'primary',
        average: 'warning',
        concern: 'danger'
      }
      return types[level] || 'info'
    }

    const getPerformanceText = (level) => {
      const texts = {
        excellent: '优秀',
        good: '良好',
        average: '一般',
        concern: '需关注'
      }
      return texts[level] || '未知'
    }

    // 主要功能函数
    const refreshData = () => {
      fetchMetrics()
      fetchStudents()
      nextTick(() => {
        setTimeout(() => {
          updateScoreChart()
          updateTimeChart()
          updateCompletionChart()
          updateActivityChart()
        }, 300)
      })
    }

    const refreshTable = () => {
      fetchStudents()
    }

    const viewStudentDetail = (student) => {
      selectedStudent.value = student
      detailDialogVisible.value = true
    }

    const sendReminder = async (student) => {
      try {
        await ElMessageBox.confirm(
            `确定要向 ${student.name} 发送学习提醒吗？`,
            '发送提醒',
            {
              confirmButtonText: '确定',
              cancelButtonText: '取消',
              type: 'warning'
            }
        )

        const response = await api.sendReminder(student.id)
        if (response.success) {
          ElMessage.success(response.message)
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('发送提醒失败:', error)
          ElMessage.error('发送提醒失败')
        }
      }
    }

    // 响应式调整图表大小
    const handleResize = () => {
      if (scoreChartInstance) scoreChartInstance.resize()
      if (timeChartInstance) timeChartInstance.resize()
      if (completionChartInstance) completionChartInstance.resize()
      if (activityChartInstance) activityChartInstance.resize()
    }

    // 生命周期
    onMounted(() => {
      // 初始化数据
      loadCourseOptions().then(() => {
        fetchMetrics()
        fetchStudents()
      })

      // 延迟初始化图表，确保DOM渲染完成
      setTimeout(() => {
        initCharts()
      }, 500)

      window.addEventListener('resize', handleResize)
    })

    onUnmounted(() => {
      if (scoreChartInstance) echarts.dispose(scoreChartInstance)
      if (timeChartInstance) echarts.dispose(timeChartInstance)
      if (completionChartInstance) echarts.dispose(completionChartInstance)
      if (activityChartInstance) echarts.dispose(activityChartInstance)
      window.removeEventListener('resize', handleResize)
    })

    return {
      // 图标
      Refresh,
      Search,
      User,
      Clock,
      Document,
      Warning,
      TrendCharts,

      // 响应式数据
      filters,
      pagination,
      metrics,
      tableData,
      tableLoading,
      detailDialogVisible,
      selectedStudent,
      courseOptions,
      classOptions,

      // 图表引用
      scoreChartRef,
      timeChartRef,
      completionChartRef,
      activityChartRef,
      scoreChartCourse,
      timeChartClass,
      activityChartClass,

      // 方法
      handleFilterChange,
      handleSearch,
      handleReset,
      handleSizeChange,
      handleCurrentChange,
      getCompletionType,
      getScoreClass,
      getStudyTimeClass,
      getPerformanceType,
      getPerformanceText,
      refreshData,
      refreshTable,
      viewStudentDetail,
      sendReminder,
      updateScoreChart,
      updateTimeChart,
      updateActivityChart
    }
  }
}
</script>

<style scoped>
.students-performance {
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

.page-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.filter-section {
  margin-bottom: 24px;
}

.filter-content {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.metrics-section {
  margin-bottom: 24px;
}

.metric-card {
  border-radius: 8px;
  transition: all 0.3s ease;
  margin-bottom: 20px;
}

.metric-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.metric-content {
  display: flex;
  align-items: center;
  gap: 15px;
}

.metric-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.metric-icon.students {
  background-color: #10b981;
}

.metric-icon.time {
  background-color: #3b82f6;
}

.metric-icon.score {
  background-color: #f59e0b;
}

.metric-icon.concern {
  background-color: #ef4444;
}

.metric-info h3 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
}

.metric-info p {
  margin: 5px 0 0;
  color: #6b7280;
  font-size: 14px;
}

.metric-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
  margin-top: 5px;
}

.metric-trend.up {
  color: #10b981;
}

.metric-trend.down {
  color: #ef4444;
}

.charts-section {
  margin-bottom: 24px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-section {
  margin-bottom: 24px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-actions {
  display: flex;
  gap: 12px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

/* 分数样式 */
.high-score {
  color: #67c23a;
  font-weight: bold;
}

.medium-score {
  color: #e6a23c;
  font-weight: bold;
}

.low-score {
  color: #f56c6c;
  font-weight: bold;
}

/* 学习时长样式 */
.high-time {
  color: #67c23a;
  font-weight: bold;
}

.medium-time {
  color: #e6a23c;
  font-weight: bold;
}

.low-time {
  color: #f56c6c;
  font-weight: bold;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .filter-content {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-content .el-select,
  .filter-content .el-date-editor {
    width: 100% !important;
  }
}

@media (max-width: 768px) {
  .students-performance {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .header-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .metric-content {
    flex-direction: column;
    text-align: center;
    gap: 10px;
  }

  .table-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .table-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
/* 学生详情弹窗样式 */
.student-detail-dialog {
  max-width: 800px;
  margin: 0 auto;
}

.student-detail-dialog :deep(.el-dialog__body) {
  padding: 10px 20px 20px;
  max-height: 70vh;
  overflow-y: auto;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .student-detail-dialog {
    width: 90% !important;
    top: 5vh !important;
  }
}

</style>