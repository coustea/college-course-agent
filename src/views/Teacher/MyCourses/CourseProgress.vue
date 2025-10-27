<template>
  <div class="progress-page">
    <div class="header">
      <el-page-header @back="goBack" :title="courseName || '课程进度管理'" />
    </div>

    <div class="stats-cards">
      <el-card class="stat-card">
        <div class="stat-content">
          <i class="fas fa-users stat-icon"></i>
          <div>
            <div class="stat-value">{{ totalStudents }}</div>
            <div class="stat-label">总学生数</div>
          </div>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-content">
          <i class="fas fa-chart-line stat-icon"></i>
          <div>
            <div class="stat-value">{{ averageProgress.toFixed(1) }}%</div>
            <div class="stat-label">平均进度</div>
          </div>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-content">
          <i class="fas fa-check-circle stat-icon"></i>
          <div>
            <div class="stat-value">{{ completedStudents }}</div>
            <div class="stat-label">完成人数</div>
          </div>
        </div>
      </el-card>
    </div>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>学生学习进度列表</span>
          <div style="display: flex; gap: 12px;">
            <el-select
              v-model="selectedClass"
              placeholder="选择班级"
              clearable
              style="width: 200px;"
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
              placeholder="搜索学生姓名或学号"
              style="width: 300px;"
              clearable
            >
              <template #prefix>
                <i class="fas fa-search"></i>
              </template>
            </el-input>
          </div>
        </div>
      </template>

      <el-table
        :data="filteredStudentProgress"
        v-loading="loading"
        style="width: 100%"
        :default-sort="{ prop: 'progress', order: 'descending' }"
      >
        <el-table-column prop="studentNumber" label="学号" width="120" sortable />
        <el-table-column prop="name" label="姓名" width="100" sortable />
        <el-table-column prop="className" label="班级" width="100" sortable />
        <el-table-column label="总体进度" width="200" sortable prop="progress">
          <template #default="{ row }">
            <el-progress
              :percentage="row.progress"
              :color="progressColor(row.progress)"
              :stroke-width="18"
            />
          </template>
        </el-table-column>
        <el-table-column label="答题正确率" width="140" sortable prop="examAccuracy" align="center">
          <template #default="{ row }">
            <el-tag 
              v-if="row.examAccuracy >= 0" 
              :type="row.examAccuracy >= 80 ? 'success' : (row.examAccuracy >= 60 ? 'warning' : 'danger')"
              size="large"
            >
              {{ row.examAccuracy.toFixed(1) }}%
            </el-tag>
            <el-tag v-else type="info" size="small">暂无数据</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="完成状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.progress >= 100 ? 'success' : (row.progress >= 60 ? 'warning' : 'info')" size="small">
              {{ row.progress >= 100 ? '已完成' : (row.progress >= 60 ? '进行中' : '未开始') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后学习时间" width="160" sortable prop="lastStudyTime">
          <template #default="{ row }">
            {{ formatDateTime(row.lastStudyTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewDetail(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 学生详细进度对话框 -->
    <el-dialog
      v-model="detailVisible"
      :title="`${currentStudent?.name || ''} 的学习详情`"
      width="900px"
      destroy-on-close
    >
      <div v-if="currentStudent" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="学号">{{ currentStudent.studentNumber }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ currentStudent.name }}</el-descriptions-item>
          <el-descriptions-item label="班级">{{ currentStudent.className }}</el-descriptions-item>
          <el-descriptions-item label="总体进度">
            <el-progress :percentage="currentStudent.progress" :color="progressColor(currentStudent.progress)" />
          </el-descriptions-item>
        </el-descriptions>

        <div style="margin-top: 20px;">
          <h4 style="margin-bottom: 10px;">视频进度</h4>
          <el-table :data="detailVideoProgress" style="width: 100%" max-height="300">
            <el-table-column prop="title" label="标题" min-width="180" />
            <el-table-column label="观看时长" width="120">
              <template #default="{ row }">
                {{ formatSeconds(row.watchedSeconds || 0) }}
              </template>
            </el-table-column>
            <el-table-column label="进度" width="160">
              <template #default="{ row }">
                <el-progress :percentage="row.percentage || 0" :stroke-width="12" />
              </template>
            </el-table-column>
            <el-table-column label="完成状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.completed ? 'success' : 'info'" size="small">
                  {{ row.completed ? '已完成' : '未完成' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div style="margin-top: 20px;">
          <h4 style="margin-bottom: 10px;">文档进度</h4>
          <el-table :data="detailDocProgress" style="width: 100%" max-height="300">
            <el-table-column prop="title" label="标题" min-width="180" />
            <el-table-column label="阅读进度" width="120">
              <template #default="{ row }">
                {{ (row.maxScrollPct || 0).toFixed(1) }}%
              </template>
            </el-table-column>
            <el-table-column label="进度" width="160">
              <template #default="{ row }">
                <el-progress :percentage="row.percentage || 0" :stroke-width="12" />
              </template>
            </el-table-column>
            <el-table-column label="完成状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.completed ? 'success' : 'info'" size="small">
                  {{ row.completed ? '已完成' : '未完成' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div style="margin-top: 20px;" v-if="detailExamAccuracy >= 0">
          <h4 style="margin-bottom: 10px;">课程答题正确率</h4>
          <el-tag
            :type="detailExamAccuracy >= 80 ? 'success' : (detailExamAccuracy >= 60 ? 'warning' : 'danger')"
            size="large"
          >
            {{ detailExamAccuracy.toFixed(1) }}%
          </el-tag>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const route = useRoute()

const API_BASE = import.meta?.env?.VITE_API_BASE_URL || '/api'
const api = axios.create({ baseURL: API_BASE, timeout: 20000 })
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token') || localStorage.getItem('userToken')
  if (token) config.headers = { ...(config.headers || {}), Authorization: `Bearer ${token}` }
  return config
})

const courseId = ref(null)
const courseName = ref('')
const loading = ref(false)
const studentProgressList = ref([])
const searchText = ref('')
const selectedClass = ref('')
const detailVisible = ref(false)
const currentStudent = ref(null)
const detailVideoProgress = ref([])
const detailDocProgress = ref([])
const detailExamAccuracy = ref(-1)

// 提取所有班级列表
const classList = computed(() => {
  const classes = new Set()
  studentProgressList.value.forEach(s => {
    if (s.className) classes.add(s.className)
  })
  return Array.from(classes).sort()
})

const totalStudents = computed(() => filteredStudentProgress.value.length)
const averageProgress = computed(() => {
  if (filteredStudentProgress.value.length === 0) return 0
  const sum = filteredStudentProgress.value.reduce((acc, s) => acc + (s.progress || 0), 0)
  return sum / filteredStudentProgress.value.length
})
const completedStudents = computed(() => filteredStudentProgress.value.filter(s => s.progress >= 100).length)

const filteredStudentProgress = computed(() => {
  let result = studentProgressList.value
  
  // 按班级筛选
  if (selectedClass.value) {
    result = result.filter(s => s.className === selectedClass.value)
  }
  
  // 按姓名/学号搜索
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

const progressColor = (pct) => {
  if (pct >= 80) return '#67c23a'
  if (pct >= 60) return '#e6a23c'
  if (pct >= 30) return '#409eff'
  return '#909399'
}

const formatDateTime = (dt) => {
  if (!dt) return '-'
  try {
    const d = new Date(dt)
    if (isNaN(d.getTime())) return '-'
    return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
  } catch {
    return '-'
  }
}

const formatSeconds = (sec) => {
  const s = Number(sec) || 0
  const m = Math.floor(s / 60)
  const ss = Math.floor(s % 60)
  return `${m}:${String(ss).padStart(2, '0')}`
}

const goBack = () => {
  router.back()
}

// 加载课程下所有学生的进度
const loadCourseProgress = async () => {
  loading.value = true
  try {
    console.log('开始加载课程进度，课程ID:', courseId.value)
    
    // 1. 获取课程信息
    const courseRes = await api.get(`/course/detail`, { params: { courseId: courseId.value } })
    console.log('课程信息响应:', courseRes)
    const courseBody = courseRes?.data
    if (courseBody && Number(courseBody.code) === 200 && courseBody.data) {
      courseName.value = courseBody.data.courseName || courseBody.data.title || '课程'
      console.log('课程名称:', courseName.value)
    }

    // 2. 获取选课学生列表
    const enrollUrl = `/teacher/enrollments/students?courseId=${courseId.value}`
    console.log('请求选课学生列表:', enrollUrl)
    const enrollRes = await api.get(`/teacher/enrollments/students`, { params: { courseId: courseId.value } })
    console.log('选课学生响应:', enrollRes)
    const enrollBody = enrollRes?.data
    const students = (enrollBody && Number(enrollBody.code) === 200 && Array.isArray(enrollBody.data)) ? enrollBody.data : []
    console.log('选课学生数量:', students.length, '学生列表:', students)

    if (students.length === 0) {
      console.warn('该课程没有选课学生')
      studentProgressList.value = []
      return
    }

    // 3. 并发查询每个学生的进度和答题正确率
    console.log('开始查询每个学生的进度...')
    const progressPromises = students.map(async (student) => {
      try {
        // 并发查询进度和答题正确率
        const [progRes, accRes] = await Promise.all([
          api.get(`/progress/course`, { params: { studentId: student.id, courseId: courseId.value } }),
          api.get(`/aiexam/accuracy`, { params: { studentId: student.id, courseId: courseId.value } })
        ])
        
        // 处理进度数据
        const progBody = progRes?.data
        let progress = 0
        let lastStudyTime = null
        if (progBody && Number(progBody.code) === 200 && progBody.data) {
          const d = progBody.data
          progress = Number(d.completionPercentage || d.completion_percentage || d.coursePercent || 0)
          if (progress >= 0 && progress <= 1) progress *= 100
          if (!Number.isFinite(progress)) progress = 0
          progress = Math.max(0, Math.min(100, progress))
          lastStudyTime = d.updatedAt || d.lastUpdated || null
        }
        
        // 处理答题正确率数据
        let examAccuracy = -1
        const accBody = accRes?.data
        if (accBody && Number(accBody.code) === 200 && accBody.data) {
          let pct = 0
          if (accBody.data.percentage != null) {
            pct = Number(accBody.data.percentage)
          } else if (accBody.data.accuracy != null) {
            pct = Number(accBody.data.accuracy) * 100
          }
          if (Number.isFinite(pct)) {
            examAccuracy = Math.max(0, Math.min(100, pct))
          }
        }
        
        return {
          id: student.id,
          studentNumber: student.studentNumber || student.studentId || '-',
          name: student.name || '-',
          className: student.className || '-',
          progress: Math.round(progress),
          examAccuracy: examAccuracy,
          lastStudyTime
        }
      } catch (err) {
        console.error(`获取学生 ${student.name} 进度失败:`, err)
        return {
          id: student.id,
          studentNumber: student.studentNumber || student.studentId || '-',
          name: student.name || '-',
          className: student.className || '-',
          progress: 0,
          examAccuracy: -1,
          lastStudyTime: null
        }
      }
    })

    studentProgressList.value = await Promise.all(progressPromises)
    console.log('最终学生进度列表:', studentProgressList.value)
  } catch (err) {
    console.error('加载课程进度失败:', err)
    studentProgressList.value = []
  } finally {
    loading.value = false
  }
}

// 查看学生详细进度
const viewDetail = async (student) => {
  currentStudent.value = student
  detailVisible.value = true
  detailVideoProgress.value = []
  detailDocProgress.value = []
  detailExamAccuracy.value = -1

  try {
    // 获取详细进度
    const allRes = await api.get(`/progress/course/all`, { params: { studentId: student.id, courseId: courseId.value } })
    const allBody = allRes?.data
    if (allBody && Number(allBody.code) === 200 && allBody.data) {
      const d = allBody.data
      const videos = Array.isArray(d.videos) ? d.videos : []
      const docs = Array.isArray(d.documents) ? d.documents : []

      // 提取视频标题
      const pickVideoTitle = (v) => {
        if (!v) return ''
        return v.title || v.videoTitle || v.name || v.video_title || ''
      }

      // 提取文档标题
      const pickDocTitle = (d) => {
        if (!d) return ''
        return d.title || d.name || d.docTitle || d.documentTitle || d.document_title || ''
      }

      detailVideoProgress.value = videos.map((v, i) => ({
        ...v,
        title: pickVideoTitle(v) || `第${i + 1}节`,
        percentage: Number(v.percentage || v.progress || 0)
      }))

      detailDocProgress.value = docs.map((d, i) => ({
        ...d,
        title: pickDocTitle(d) || `第${i + 1}节`,
        percentage: Number(d.percentage || d.progress || 0)
      }))
    }

    // 获取答题正确率
    const accRes = await api.get(`/aiexam/accuracy`, { params: { studentId: student.id, courseId: courseId.value } })
    const accBody = accRes?.data
    if (accBody && Number(accBody.code) === 200 && accBody.data) {
      let pct = 0
      if (accBody.data.percentage != null) {
        pct = Number(accBody.data.percentage)
      } else if (accBody.data.accuracy != null) {
        pct = Number(accBody.data.accuracy) * 100
      }
      if (Number.isFinite(pct)) {
        detailExamAccuracy.value = Math.max(0, Math.min(100, pct))
      }
    }
  } catch (err) {
    console.error('加载学生详细进度失败:', err)
  }
}

onMounted(() => {
  courseId.value = Number(route.params.id || route.params.courseId)
  console.log('CourseProgress mounted, courseId:', courseId.value, 'route.params:', route.params)
  if (!courseId.value) {
    console.error('课程ID不存在，返回课程列表')
    router.push('/teacher/courses/list')
    return
  }
  loadCourseProgress()
})
</script>

<style scoped>
.progress-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.header {
  margin-bottom: 24px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  border-radius: 8px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  font-size: 48px;
  color: #409eff;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #2c3e50;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.table-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-content h4 {
  color: #2c3e50;
  font-weight: 600;
}
</style>

