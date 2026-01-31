<template>
  <div class="grades-page">
    <div class="header">
      <el-page-header @back="goBack" title="成绩管理" />
    </div>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>学生成绩列表</span>
          <div style="display: flex; gap: 12px;">
            <el-select
              v-model="courseId"
              placeholder="选择课程"
              @change="onCourseChange"
              filterable
              style="width: 300px;"
              :loading="courseList.length === 0"
            >
              <el-option
                v-for="course in courseList"
                :key="course.courseId || course.id"
                :label="course.courseName || course.title || '未命名课程'"
                :value="course.courseId || course.id"
              />
            </el-select>
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
              style="width: 250px;"
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
        :data="filteredStudentGrades"
        v-loading="loading"
        style="width: 100%"
      >
        <el-table-column prop="studentNumber" label="学号" width="120" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="className" label="班级" width="100" />
        <el-table-column label="总体进度" width="200">
          <template #default="{ row }">
            <el-progress
              :percentage="row.progress"
              :color="progressColor(row.progress)"
              :stroke-width="18"
            />
          </template>
        </el-table-column>
        <el-table-column label="完成状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.progress >= 100 ? 'success' : (row.progress >= 60 ? 'warning' : 'info')" size="small">
              {{ row.progress >= 100 ? '已完成' : (row.progress >= 60 ? '进行中' : '未开始') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="成绩" width="120" align="center">
          <template #default="{ row }">
            <div v-if="row.progress >= 100">
              <el-tag
                v-if="row.averageScore !== null"
                :type="row.averageScore >= 80 ? 'success' : (row.averageScore >= 60 ? 'warning' : 'danger')"
                size="large"
              >
                {{ row.averageScore }}
              </el-tag>
              <el-tag v-else type="info" size="small">暂无成绩</el-tag>
            </div>
            <div v-else>
              <el-tag type="info" size="small">未完成</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="viewDetail(row)"
            >
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件 -->
      <div style="margin-top: 20px; display: flex; justify-content: center;">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="totalGrades"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handlePageSizeChange"
        />
      </div>
    </el-card>

    <!-- 成绩详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      :title="`${currentStudent?.name || ''} 的成绩详情`"
      width="900px"
      destroy-on-close
    >
      <div v-if="currentStudent" class="detail-content" v-loading="detailLoading">
        <!-- 学生基本信息 -->
        <el-card class="info-card" shadow="never">
          <el-descriptions :column="2" border size="large">
            <el-descriptions-item label="学号" label-class-name="desc-label">
              <el-text size="large">{{ currentStudent.studentNumber }}</el-text>
            </el-descriptions-item>
            <el-descriptions-item label="姓名" label-class-name="desc-label">
              <el-text size="large" type="primary">{{ currentStudent.name }}</el-text>
            </el-descriptions-item>
            <el-descriptions-item label="班级" label-class-name="desc-label">
              <el-text size="large">{{ currentStudent.className }}</el-text>
            </el-descriptions-item>
            <el-descriptions-item label="总体进度" label-class-name="desc-label">
              <el-progress 
                :percentage="currentStudent.progress" 
                :color="progressColor(currentStudent.progress)"
                :stroke-width="20"
              />
            </el-descriptions-item>
            <el-descriptions-item label="总体成绩" label-class-name="desc-label" :span="2">
              <el-tag
                v-if="currentStudent.averageScore !== null"
                :type="currentStudent.averageScore >= 80 ? 'success' : (currentStudent.averageScore >= 60 ? 'warning' : 'danger')"
                size="large"
                effect="dark"
                style="font-size: 18px; padding: 8px 16px;"
              >
                {{ currentStudent.averageScore }} 分
              </el-tag>
              <el-tag v-else type="info" size="large">暂无成绩</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 视频成绩 -->
        <div class="score-section" v-if="videoScores.length > 0">
          <div class="section-header">
            <i class="fas fa-video section-icon"></i>
            <span class="section-title">视频学习成绩</span>
            <el-tag size="small" type="primary">共 {{ videoScores.length }} 个视频</el-tag>
          </div>
          <el-table 
            :data="videoScores" 
            style="width: 100%" 
            :show-header="true"
            stripe
            class="score-table"
          >
            <el-table-column prop="videoId" label="视频ID" width="100" align="center">
              <template #default="{ row }">
                <el-tag type="info" size="small">视频 #{{ row.videoId }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="平均成绩" width="150" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="row.averageScore >= 80 ? 'success' : (row.averageScore >= 60 ? 'warning' : 'danger')"
                  size="large"
                  effect="dark"
                >
                  {{ row.averageScore }} 分
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="答题次数" width="120" align="center">
              <template #default="{ row }">
                <el-tag type="primary" size="small">{{ row.attemptCount }} 次</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="成绩分析" align="center">
              <template #default="{ row }">
                <div class="score-analysis">
                  <el-progress 
                    :percentage="row.averageScore" 
                    :color="row.averageScore >= 80 ? '#67c23a' : (row.averageScore >= 60 ? '#e6a23c' : '#f56c6c')"
                    :stroke-width="18"
                  >
                    <span style="font-size: 12px; font-weight: bold;">{{ row.averageScore }}%</span>
                  </el-progress>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 文档成绩 -->
        <div class="score-section" v-if="documentScores.length > 0">
          <div class="section-header">
            <i class="fas fa-file-alt section-icon"></i>
            <span class="section-title">文档学习成绩</span>
            <el-tag size="small" type="success">共 {{ documentScores.length }} 个文档</el-tag>
          </div>
          <el-table 
            :data="documentScores" 
            style="width: 100%" 
            :show-header="true"
            stripe
            class="score-table"
          >
            <el-table-column prop="documentId" label="文档ID" width="100" align="center">
              <template #default="{ row }">
                <el-tag type="info" size="small">文档 #{{ row.documentId }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="平均成绩" width="150" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="row.averageScore >= 80 ? 'success' : (row.averageScore >= 60 ? 'warning' : 'danger')"
                  size="large"
                  effect="dark"
                >
                  {{ row.averageScore }} 分
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="答题次数" width="120" align="center">
              <template #default="{ row }">
                <el-tag type="success" size="small">{{ row.attemptCount }} 次</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="成绩分析" align="center">
              <template #default="{ row }">
                <div class="score-analysis">
                  <el-progress 
                    :percentage="row.averageScore" 
                    :color="row.averageScore >= 80 ? '#67c23a' : (row.averageScore >= 60 ? '#e6a23c' : '#f56c6c')"
                    :stroke-width="18"
                  >
                    <span style="font-size: 12px; font-weight: bold;">{{ row.averageScore }}%</span>
                  </el-progress>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 所有成绩（兜底显示） -->
        <div class="score-section" v-if="!detailLoading && videoScores.length === 0 && documentScores.length === 0 && allScores.length > 0">
          <div class="section-header">
            <i class="fas fa-list-alt section-icon"></i>
            <span class="section-title">所有提交的成绩</span>
            <el-tag size="small" type="warning">共 {{ allScores.length }} 次答题</el-tag>
          </div>
          <el-alert 
            title="提示" 
            type="warning" 
            description="这些题目生成时未关联具体视频或文档，显示所有已提交的成绩。" 
            show-icon 
            :closable="false"
            style="margin-bottom: 16px;"
          />
          <el-table 
            :data="allScores" 
            style="width: 100%" 
            :show-header="true"
            stripe
            class="score-table"
          >
            <el-table-column prop="id" label="试卷ID" width="100" align="center">
              <template #default="{ row }">
                <el-tag type="info" size="small">#{{ row.id }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="topic" label="主题" min-width="150" />
            <el-table-column label="题目数" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small">{{ row.questionCount }} 题</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="成绩" width="150" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="row.totalScore >= 80 ? 'success' : (row.totalScore >= 60 ? 'warning' : 'danger')"
                  size="large"
                  effect="dark"
                >
                  {{ row.totalScore }} 分
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="提交时间" width="180" align="center">
              <template #default="{ row }">
                {{ formatDateTime(row.createdAt) }}
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 无成绩提示 -->
        <el-empty 
          v-if="!detailLoading && videoScores.length === 0 && documentScores.length === 0 && allScores.length === 0"
          description="暂无成绩数据"
          :image-size="120"
        >
          <el-alert 
            title="提示" 
            type="info" 
            description="该学生尚未完成任何答题，或成绩尚未生成。" 
            show-icon 
            :closable="false"
          />
        </el-empty>
      </div>

      <template #footer>
        <el-button type="primary" @click="detailVisible = false" size="large">
          <i class="fas fa-check"></i> 关闭
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
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
const courseList = ref([])
const loading = ref(false)
const studentGradesList = ref([])
const searchText = ref('')
const selectedClass = ref('')
const detailVisible = ref(false)
const currentStudent = ref(null)
const detailLoading = ref(false)
const videoScores = ref([])
const documentScores = ref([])
const allScores = ref([]) // 所有成绩（兜底显示）

// 分页相关
const currentPage = ref(1)
const pageSize = ref(10)

// 当前课程名称
const courseName = computed(() => {
  const course = courseList.value.find(c => (c.courseId || c.id) === courseId.value)
  return course ? (course.courseName || course.title || '课程') : '课程'
})

// 提取所有班级列表
const classList = computed(() => {
  const classes = new Set()
  studentGradesList.value.forEach(s => {
    if (s.className) classes.add(s.className)
  })
  return Array.from(classes).sort()
})

// 筛选后的完整数据（用于统计和分页）
const filteredAllGrades = computed(() => {
  let result = studentGradesList.value

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

const totalGrades = computed(() => filteredAllGrades.value.length)

// 当前页显示的数据（分页后）
const filteredStudentGrades = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredAllGrades.value.slice(start, end)
})

// 分页事件处理
const handlePageChange = (page) => {
  currentPage.value = page
}

const handlePageSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1 // 重置到第一页
}

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
    return d.toLocaleString('zh-CN', { 
      year: 'numeric', 
      month: '2-digit', 
      day: '2-digit', 
      hour: '2-digit', 
      minute: '2-digit' 
    })
  } catch {
    return '-'
  }
}

const goBack = () => {
  router.back()
}

// 加载教师的所有课程
const loadCourseList = async () => {
  try {
    console.log('开始加载课程列表...')
    const res = await api.get('/course/list')
    console.log('课程列表接口响应:', res)
    const body = res?.data
    if (body && Number(body.code) === 200 && Array.isArray(body.data)) {
      courseList.value = body.data
      console.log('课程列表加载成功，共', courseList.value.length, '个课程:', courseList.value)
    } else {
      console.warn('课程列表数据格式不正确:', body)
      courseList.value = []
    }
  } catch (err) {
    console.error('加载课程列表失败:', err)
    courseList.value = []
  }
}

// 课程切换事件
const onCourseChange = () => {
  console.log('课程切换到:', courseId.value)
  // 更新URL（不刷新页面）
  router.replace(`/teacher/courses/${courseId.value}/grades`)
  // 重新加载成绩数据
  loadCourseGrades()
}

// 加载课程下所有学生的成绩
const loadCourseGrades = async () => {
  if (!courseId.value) return

  loading.value = true
  try {
    console.log('开始加载课程成绩，课程ID:', courseId.value)

    // 1. 获取选课学生列表
    const enrollUrl = `/teacher/enrollments/students?courseId=${courseId.value}`
    console.log('请求选课学生列表:', enrollUrl)
    const enrollRes = await api.get(`/teacher/enrollments/students`, { params: { courseId: courseId.value } })
    console.log('选课学生响应:', enrollRes)
    const enrollBody = enrollRes?.data
    const students = (enrollBody && Number(enrollBody.code) === 200 && Array.isArray(enrollBody.data)) ? enrollBody.data : []
    console.log('选课学生数量:', students.length, '学生列表:', students)

    if (students.length === 0) {
      console.warn('该课程没有选课学生')
      studentGradesList.value = []
      return
    }

    // 2. 并发查询每个学生的进度和成绩
    console.log('开始查询每个学生的进度和成绩...')
    const gradesPromises = students.map(async (student) => {
      try {
        // 并发查询进度和成绩
        const [progRes, scoreRes] = await Promise.all([
          api.get(`/progress/course`, { params: { studentId: student.id, courseId: courseId.value } }),
          // 只有当进度达到100%时才查询成绩
          api.get(`/aiexam/average-score`, { params: { studentId: student.id, courseId: courseId.value } }).catch(err => {
            // 忽略成绩查询错误，因为可能还未生成成绩
            return null
          })
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

        // 处理成绩数据（只有进度达到100%时才有成绩）
        let averageScore = null
        if (progress >= 100 && scoreRes?.data) {
          const scoreBody = scoreRes.data
          if (scoreBody && Number(scoreBody.code) === 200 && scoreBody.data) {
            averageScore = Number(scoreBody.data.averageScore) || null
          }
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
        console.error(`获取学生 ${student.name} 数据失败:`, err)
        return {
          id: student.id,
          studentNumber: student.studentNumber || student.studentId || '-',
          name: student.name || '-',
          className: student.className || '-',
          progress: 0,
          averageScore: null,
          lastStudyTime: null
        }
      }
    })

    studentGradesList.value = await Promise.all(gradesPromises)
    console.log('最终学生成绩列表:', studentGradesList.value)
  } catch (err) {
    console.error('加载课程成绩失败:', err)
    studentGradesList.value = []
  } finally {
    loading.value = false
  }
}

// 查看学生成绩详情
const viewDetail = async (student) => {
  currentStudent.value = student
  detailVisible.value = true
  videoScores.value = []
  documentScores.value = []
  allScores.value = []
  detailLoading.value = true

  try {
    // 调用详细成绩接口
    const res = await api.get('/aiexam/detailed-scores', {
      params: {
        studentId: student.id,
        courseId: courseId.value
      }
    })
    
    console.log('详细成绩响应:', res.data)
    
    if (res.data && res.data.code === 200 && res.data.data) {
      const data = res.data.data
      videoScores.value = Array.isArray(data.videoScores) ? data.videoScores : []
      documentScores.value = Array.isArray(data.documentScores) ? data.documentScores : []
      
      console.log('视频成绩:', videoScores.value)
      console.log('文档成绩:', documentScores.value)
      
      // 如果没有视频和文档成绩，尝试获取所有已提交的成绩（兜底方案）
      if (videoScores.value.length === 0 && documentScores.value.length === 0) {
        console.log('没有找到视频和文档成绩，尝试获取所有成绩...')
        try {
          const allRes = await api.get('/aiexam/list', {
            params: {
              studentId: student.id,
              courseId: courseId.value
            }
          })
          if (allRes.data && allRes.data.code === 200 && Array.isArray(allRes.data.data)) {
            allScores.value = allRes.data.data
              .filter(exam => exam.status === 'submitted' && exam.totalScore != null)
              .map(exam => ({
                id: exam.id,
                topic: exam.topic || exam.courseName || '题目',
                totalScore: exam.totalScore,  // 统一使用totalScore
                questionCount: exam.questionCount || 0,
                createdAt: exam.createdAt
              }))
            console.log('找到所有成绩:', allScores.value)
          }
        } catch (e) {
          console.error('获取所有成绩失败:', e)
        }
      }
    }
  } catch (err) {
    console.error('获取详细成绩失败:', err)
  } finally {
    detailLoading.value = false
  }
}

onMounted(async () => {
  // 先加载课程列表
  await loadCourseList()

  // 设置当前课程ID
  const routeCourseId = Number(route.params.id || route.params.courseId)
  console.log('GradesPage mounted, routeCourseId:', routeCourseId, 'route.params:', route.params)

  if (routeCourseId) {
    courseId.value = routeCourseId
  } else if (courseList.value.length > 0) {
    // 如果没有指定课程ID，默认选择第一个课程
    courseId.value = courseList.value[0].courseId || courseList.value[0].id
  } else {
    console.error('没有可用的课程')
    return
  }

  // 加载成绩数据
  loadCourseGrades()
})

// 当搜索条件或筛选条件改变时，重置到第一页
watch([searchText, selectedClass], () => {
  currentPage.value = 1
})
</script>

<style scoped>
.grades-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.header {
  margin-bottom: 24px;
}

.table-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 详情对话框样式 */
.detail-content {
  padding: 10px;
}

.info-card {
  margin-bottom: 24px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.info-card :deep(.el-descriptions__label) {
  color: rgba(255, 255, 255, 0.9);
  font-weight: 600;
  background: rgba(255, 255, 255, 0.1) !important;
}

.info-card :deep(.el-descriptions__content) {
  color: white;
  background: rgba(255, 255, 255, 0.05) !important;
}

.info-card :deep(.el-progress__text) {
  color: white !important;
}

/* 成绩区块 */
.score-section {
  margin-top: 24px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

.score-section:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid #e0e0e0;
}

.section-icon {
  font-size: 24px;
  color: #409eff;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
  flex: 1;
}

/* 成绩表格美化 */
.score-table {
  border-radius: 8px;
  overflow: hidden;
}

.score-table :deep(.el-table__header) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.score-table :deep(.el-table__header th) {
  background: transparent !important;
  color: white !important;
  font-weight: 600;
  font-size: 14px;
}

.score-table :deep(.el-table__row) {
  transition: all 0.2s ease;
}

.score-table :deep(.el-table__row:hover) {
  background: #e3f2fd !important;
}

.score-analysis {
  padding: 0 12px;
}

/* 空状态样式 */
.detail-content :deep(.el-empty) {
  padding: 40px 20px;
}

/* 描述列表标签 */
:deep(.desc-label) {
  font-weight: 600 !important;
  font-size: 14px !important;
}

/* 对话框底部按钮 */
.el-dialog__footer .el-button {
  min-width: 120px;
  border-radius: 8px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.el-dialog__footer .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
}

/* 响应式布局 */
@media (max-width: 768px) {
  .score-section {
    padding: 12px;
  }
  
  .section-header {
    flex-wrap: wrap;
  }
  
  .section-title {
    font-size: 16px;
  }
}
</style>
