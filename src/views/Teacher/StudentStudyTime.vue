<template>
  <div class="study-time-page">
    <!-- 顶部导航 -->
    <div class="page-header-container">
      <el-page-header @back="goBack" title="返回首页">
        <template #content>
          <span class="header-title">学生学习时间统计</span>
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
      <el-col :xs="24" :sm="6">
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
      <el-col :xs="24" :sm="6">
        <el-card shadow="hover" class="stat-card green-theme">
          <div class="stat-body">
            <div class="stat-info">
              <div class="stat-value">{{ activeStudents }}</div>
              <div class="stat-label">活跃学生</div>
            </div>
            <el-icon class="stat-icon"><UserFilled /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="6">
        <el-card shadow="hover" class="stat-card orange-theme">
          <div class="stat-body">
            <div class="stat-info">
              <div class="stat-value">{{ totalHours }}</div>
              <div class="stat-label">总学习时长(小时)</div>
            </div>
            <el-icon class="stat-icon"><Clock /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="6">
        <el-card shadow="hover" class="stat-card purple-theme">
          <div class="stat-body">
            <div class="stat-info">
              <div class="stat-value">{{ avgHours }}</div>
              <div class="stat-label">人均学习时长(小时)</div>
            </div>
            <el-icon class="stat-icon"><TrendCharts /></el-icon>
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
            v-model="selectedCourseId"
            placeholder="选择课程"
            @change="onCourseChange"
            filterable
            class="course-select"
            :loading="loadingCourses"
          >
            <el-option
              v-for="course in courseList"
              :key="course.courseId"
              :label="course.courseName"
              :value="course.courseId"
            />
          </el-select>
          <span class="week-label">本周日期：{{ weekDateRange }}</span>
        </div>
        <div class="right-filters">
          <el-input
            v-model="searchText"
            placeholder="搜索姓名或学号"
            class="search-input"
            clearable
            :prefix-icon="Search"
          />
        </div>
      </div>

      <!-- 数据表格 -->
      <el-table
        :data="filteredStudentList"
        style="width: 100%"
        v-loading="loading"
        stripe
        border
        :default-sort="{ prop: 'totalHours', order: 'descending' }"
      >
        <el-table-column type="index" label="排名" width="80" align="center">
          <template #default="{ $index }">
            <el-tag v-if="$index < 3" :type="getRankTagType($index)" effect="dark">
              {{ $index + 1 }}
            </el-tag>
            <span v-else>{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="studentNumber" label="学号" width="150" align="center" />
        <el-table-column prop="studentName" label="姓名" width="150" align="center" />
        <el-table-column prop="totalSeconds" label="学习时长(秒)" width="150" align="center">
          <template #default="{ row }">
            {{ formatSeconds(row.totalSeconds) }}
          </template>
        </el-table-column>
        <el-table-column prop="totalHours" label="学习时长(小时)" width="150" align="center" sortable>
          <template #default="{ row }">
            <el-progress
              :percentage="calculateProgress(row.totalHours)"
              :color="getProgressColor(row.totalHours)"
            >
              <span class="progress-text">{{ row.totalHours }}h</span>
            </el-progress>
          </template>
        </el-table-column>
        <el-table-column prop="lastStudyTime" label="最后学习时间" width="200" align="center">
          <template #default="{ row }">
            {{ row.lastStudyTime ? formatDate(row.lastStudyTime) : '未学习' }}
          </template>
        </el-table-column>
        <el-table-column label="学习状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.totalSeconds > 0 ? 'success' : 'info'" effect="plain">
              {{ row.totalSeconds > 0 ? '已学习' : '未学习' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态提示 -->
      <el-empty
        v-if="!loading && filteredStudentList.length === 0"
        description="暂无学习数据"
      />
    </el-card>

    <!-- 底部提示 -->
    <div class="page-tips">
      <el-alert
        title="统计说明"
        type="info"
        :closable="false"
        show-icon
      >
        <template #default>
          <p>1. 学习时间按周统计，每周一自动开始新的一周统计</p>
          <p>2. 数据每小时更新一次，实时统计学生学习时长</p>
          <p>3. 包括视频观看和文档阅读的学习时间</p>
        </template>
      </el-alert>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import {
  Refresh,
  User,
  UserFilled,
  Clock,
  TrendCharts,
  Search
} from '@element-plus/icons-vue'

const router = useRouter()
const BASE_URL = import.meta.env.VITE_BACKEND_ORIGIN || 'http://localhost:9999'

// 数据
const loading = ref(false)
const loadingCourses = ref(false)
const courseList = ref([])
const selectedCourseId = ref(null)
const studentStudyList = ref([])
const searchText = ref('')

// 统计数据
const totalStudents = ref(0)
const activeStudents = ref(0)
const totalHours = ref('0.00')
const avgHours = ref('0.00')

// 本周日期范围
const weekDateRange = ref('')

// 返回上一页
const goBack = () => {
  router.push('/teacher/home')
}

// 计算本周日期范围
const calculateWeekRange = () => {
  const now = new Date()
  const day = now.getDay()
  const diff = now.getDate() - day + (day === 0 ? -6 : 1)
  const monday = new Date(now.setDate(diff))
  const sunday = new Date(monday)
  sunday.setDate(monday.getDate() + 6)

  const formatDate = (date) => {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }

  weekDateRange.value = `${formatDate(monday)} 至 ${formatDate(sunday)}`
}

// 获取教师课程列表
const fetchCourseList = async () => {
  loadingCourses.value = true
  try {
    const teacherId = localStorage.getItem('userId')
    const token = localStorage.getItem('token')

    const response = await axios.get(`${BASE_URL}/course/teacher`, {
      params: { teacherId },
      headers: { Authorization: `Bearer ${token}` }
    })

    if (response.data.code === 200 && response.data.data) {
      courseList.value = response.data.data.filter(
        course => course.publishStatus === 'published'
      )

      if (courseList.value.length > 0 && !selectedCourseId.value) {
        selectedCourseId.value = courseList.value[0].courseId
        await fetchStudentStudyTime()
      }
    }
  } catch (error) {
    console.error('获取课程列表失败:', error)
  } finally {
    loadingCourses.value = false
  }
}

// 获取学生学习时间统计
const fetchStudentStudyTime = async () => {
  if (!selectedCourseId.value) return

  loading.value = true
  try {
    const teacherId = localStorage.getItem('userId')
    const token = localStorage.getItem('token')

    const response = await axios.get(`${BASE_URL}/api/progress/teacher/course/students/weekly`, {
      params: {
        courseId: selectedCourseId.value,
        teacherId: teacherId
      },
      headers: { Authorization: `Bearer ${token}` }
    })

    if (response.data.code === 200 && response.data.data) {
      studentStudyList.value = response.data.data
      updateStatistics()
    }
  } catch (error) {
    console.error('获取学生学习时间失败:', error)
  } finally {
    loading.value = false
  }
}

// 更新统计数据
const updateStatistics = () => {
  const list = studentStudyList.value
  totalStudents.value = list.length
  activeStudents.value = list.filter(s => s.totalSeconds > 0).length

  const totalSec = list.reduce((sum, s) => sum + (s.totalSeconds || 0), 0)
  totalHours.value = (totalSec / 3600).toFixed(2)

  const active = activeStudents.value > 0 ? activeStudents.value : 1
  avgHours.value = ((totalSec / 3600) / active).toFixed(2)
}

// 课程切换
const onCourseChange = () => {
  fetchStudentStudyTime()
}

// 刷新数据
const refreshData = () => {
  fetchStudentStudyTime()
}

// 过滤后的学生列表
const filteredStudentList = computed(() => {
  if (!searchText.value) {
    return studentStudyList.value
  }
  const search = searchText.value.toLowerCase()
  return studentStudyList.value.filter(student => {
    return (
      student.studentName?.toLowerCase().includes(search) ||
      student.studentNumber?.toLowerCase().includes(search)
    )
  })
})

// 格式化秒数为可读格式
const formatSeconds = (seconds) => {
  if (!seconds || seconds === 0) return '0秒'
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60

  if (hours > 0) {
    return `${hours}小时${minutes}分${secs}秒`
  } else if (minutes > 0) {
    return `${minutes}分${secs}秒`
  } else {
    return `${secs}秒`
  }
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

// 计算进度条百分比（最高10小时为100%）
const calculateProgress = (hours) => {
  const maxHours = 10
  const percentage = Math.min((hours / maxHours) * 100, 100)
  return Math.round(percentage)
}

// 获取进度条颜色
const getProgressColor = (hours) => {
  if (hours >= 8) return '#67c23a'
  if (hours >= 5) return '#409eff'
  if (hours >= 2) return '#e6a23c'
  return '#f56c6c'
}

// 获取排名标签类型
const getRankTagType = (index) => {
  const types = ['danger', 'warning', 'success']
  return types[index] || 'info'
}

// 组件挂载
onMounted(() => {
  calculateWeekRange()
  fetchCourseList()
})
</script>

<style scoped>
.study-time-page {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.page-header-container {
  margin-bottom: 20px;
}

.header-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.stats-container {
  margin-bottom: 20px;
}

.stat-card {
  height: 120px;
  margin-bottom: 20px;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
}

.stat-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  padding: 0 10px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 36px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 10px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  font-weight: 500;
}

.stat-icon {
  font-size: 48px;
  opacity: 0.8;
}

.blue-theme .stat-value {
  color: #409eff;
}

.blue-theme .stat-icon {
  color: #409eff;
}

.green-theme .stat-value {
  color: #67c23a;
}

.green-theme .stat-icon {
  color: #67c23a;
}

.orange-theme .stat-value {
  color: #e6a23c;
}

.orange-theme .stat-icon {
  color: #e6a23c;
}

.purple-theme .stat-value {
  color: #909399;
}

.purple-theme .stat-icon {
  color: #909399;
}

.main-card {
  margin-bottom: 20px;
}

.filter-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 15px;
}

.left-filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.right-filters {
  display: flex;
  gap: 10px;
}

.filter-label,
.week-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.course-select {
  width: 250px;
}

.search-input {
  width: 250px;
}

.progress-text {
  font-size: 12px;
  font-weight: 600;
  color: #303133;
}

.page-tips {
  margin-top: 20px;
}

.page-tips p {
  margin: 5px 0;
  font-size: 14px;
  color: #606266;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .stats-container {
    margin-bottom: 10px;
  }

  .stat-card {
    height: 100px;
    margin-bottom: 10px;
  }

  .stat-value {
    font-size: 28px;
  }

  .stat-icon {
    font-size: 36px;
  }

  .filter-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .left-filters,
  .right-filters {
    width: 100%;
  }

  .course-select,
  .search-input {
    width: 100%;
  }
}
</style>
