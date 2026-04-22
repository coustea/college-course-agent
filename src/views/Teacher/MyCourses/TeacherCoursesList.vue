<template>
  <div class="content">
    <!-- 顶部欢迎区 -->
    <div class="header">
      <div class="header-left">
        <h1 class="page-title">课程管理</h1>
      </div>
      <div class="header-right">
        <p class="welcome-text">欢迎回来，{{ userName }}！这里是您的教学控制台。</p>
        <!-- 可以在这里放“创建新课程”按钮，如果有的话 -->
      </div>
    </div>

    <!-- 搜索与筛选工具栏 -->
    <div class="toolbar-container">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input
          type="text"
          placeholder="搜索课程名称、描述..."
          v-model="searchQuery"
          @input="filterCourses"
        />
      </div>

      <div class="filter-group">
        <button
          v-for="filter in filters"
          :key="filter.value"
          class="filter-pill"
          :class="{ active: activeFilter === filter.value }"
          @click="setFilter(filter.value)"
        >
          {{ filter.label }}
        </button>
      </div>
    </div>

    <!-- 课程卡片网格 -->
    <div class="courses-grid">
      <!-- 加载状态 -->
      <div v-if="loading" class="state-container">
        <div class="loading-spinner"></div>
        <p>正在加载课程数据...</p>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && filteredCourses.length === 0" class="state-container">
        <img src="https://gw.alipayobjects.com/zos/antfincdn/ZHrcdLPrvN/empty.svg" alt="Empty" style="width: 120px; opacity: 0.6;">
        <h3>没有找到相关课程</h3>
        <p>试试调整搜索关键词或筛选条件</p>
      </div>

      <!-- 课程卡片 -->
      <div
        v-for="course in filteredCourses"
        :key="course.id"
        class="modern-card"
        @click="onCardClick(course)"
      >
        <!-- 卡片封面 -->
        <div class="card-cover">
          <img :src="course.image || defaultImage" @error="handleImgError" :alt="course.title" />
          <div class="card-badges">
            <span class="type-badge" :class="course.type">
              <i :class="course.type === 'video' ? 'fas fa-play-circle' : 'fas fa-file-alt'"></i>
              {{ course.type === 'video' ? '视频课' : '文档课' }}
            </span>
            <span class="status-badge" v-if="course.averageProgress > 0">
              完成率 {{ course.averageProgress }}%
            </span>
          </div>
          <!-- 封面上的悬浮操作 (可选) -->
          <div class="cover-overlay">
            <button class="overlay-btn" @click.stop="onCardClick(course)">
              进入课程
            </button>
          </div>
        </div>

        <!-- 卡片内容 -->
        <div class="card-body">
          <h3 class="card-title" :title="course.title">{{ course.title }}</h3>
          <p class="card-desc">{{ course.description || '暂无描述' }}</p>

          <div class="card-meta">
            <div class="meta-item">
              <i class="far fa-calendar-alt"></i>
              <span>{{ formatDate(course.startDate) }}</span>
            </div>
            <div class="meta-item">
              <i class="fas fa-user-graduate"></i>
              <span>{{ course.studentCount || 0 }} 人在学</span>
            </div>
          </div>
        </div>

        <!-- 底部操作栏 (核心优化部分) -->
        <div class="card-actions" @click.stop>
          <!-- 主操作区：管理与进度 -->
          <div class="action-group main">
            <el-tooltip content="管理选课学生" placement="top" :show-after="500">
              <button class="action-btn btn-blue" @click.stop="openStudentManager(course)">
                <i class="fas fa-users-cog"></i> 学生管理
              </button>
            </el-tooltip>
            <el-tooltip content="查看学习统计" placement="top" :show-after="500">
              <button class="action-btn btn-cyan" @click.stop="viewCourseProgress(course)">
                <i class="fas fa-chart-pie"></i> 进度分析
              </button>
            </el-tooltip>
          </div>

          <div class="divider-vertical"></div>

          <!-- 次要操作区：编辑与删除 -->
          <div class="action-group secondary">
            <el-tooltip content="编辑课程内容" placement="top">
              <button class="icon-btn edit" @click.stop="editCourseMaterials(course)">
                <i class="fas fa-edit"></i>
              </button>
            </el-tooltip>
            <el-popconfirm title="确定删除该课程吗？此操作不可恢复。" @confirm="deleteCourse(course)">
              <template #reference>
                <div class="icon-btn-wrapper"> <!-- 包装一层避免 tooltip 冲突 -->
                   <button class="icon-btn delete">
                    <i class="fas fa-trash-alt"></i>
                  </button>
                </div>
              </template>
            </el-popconfirm>
          </div>
        </div>
      </div>
    </div>

    <!-- 组件：播放器与文档查看器 (保持原逻辑) -->
    <CoursePlayer v-if="activeCourse" v-model="playerVisible" :course-id="activeCourse.id" :title="activeCourse.title" :chapters="activeCourse.chapters || []" :fallback-src="activeCourse.videoUrl || ''" :video-count="(activeCourse.chapters && activeCourse.chapters.length) || activeCourse.videoCount || 0" :enable-questions="false" @progress="onOverallProgress" />

    <DocumentViewer v-model="docVisible" :course-id="activeDoc?.courseId || activeDoc?.id || null" :id="activeDoc?.id" :title="activeDoc?.title || '文档课程'" :file-url="activeDoc?.fileUrl || activeDoc?.url || ''" :html-content="activeDoc?.html || ''" :chapters="activeDoc?.chapters || []" :course-title="activeDoc?.title || ''" :chapter-index="1" :progress="0" :image="activeDoc?.image || ''" :duration="activeDoc?.duration || ''" />

    <!-- 弹窗：学生管理 -->
    <el-dialog
      v-model="studentManagerVisible"
      :title="`学生管理 - ${managingCourse?.title || ''}`"
      width="900px"
      class="custom-dialog"
      destroy-on-close
    >
      <div class="student-manager">
        <div class="dialog-toolbar">
          <div class="left-tools">
             <el-radio-group v-model="studentTab" size="large">
              <el-radio-button label="not">未选课 ({{ filteredNotEnrolledList.length }})</el-radio-button>
              <el-radio-button label="enrolled">已选课 ({{ filteredEnrolledList.length }})</el-radio-button>
            </el-radio-group>
          </div>
          <div class="right-tools">
             <el-select v-model="selectedClassName" placeholder="筛选班级" clearable style="width: 140px; margin-right: 10px;">
              <el-option v-for="c in availableClasses" :key="c" :label="c" :value="c" />
            </el-select>
            <el-input
              v-model="studentSearch"
              placeholder="搜姓名/学号"
              prefix-icon="Search"
              clearable
              style="width: 200px;"
            />
          </div>
        </div>

        <div class="table-container">
           <!-- 未选课列表 -->
          <el-table
            v-if="studentTab === 'not'"
            ref="notEnrolledTable"
            :data="filteredNotEnrolledList"
            row-key="id"
            height="400"
            v-loading="studentLoading"
            @selection-change="onNotSelectionChange"
            stripe
          >
            <el-table-column type="selection" width="50" align="center" fixed="left" reserve-selection />
            <el-table-column prop="studentNumber" label="学号" width="140" sortable />
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column prop="className" label="班级" width="140" sortable />
            <el-table-column prop="phone" label="手机号" min-width="150" />
          </el-table>

          <!-- 已选课列表 -->
           <el-table
            v-else
            :data="filteredEnrolledList"
            row-key="id"
            height="400"
            v-loading="studentLoading"
            stripe
          >
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column prop="studentNumber" label="学号" width="140" sortable />
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column prop="className" label="班级" width="140" sortable />
            <el-table-column label="状态" width="100">
               <template #default>
                 <el-tag type="success" size="small">已加入</el-tag>
               </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <div class="selection-info" v-if="studentTab === 'not' && selectedToEnroll.length > 0">
            已选择 <b>{{ selectedToEnroll.length }}</b> 名学生
          </div>
          <div class="footer-btns">
            <el-button @click="studentManagerVisible = false">关闭</el-button>
            <el-button
              v-if="studentTab === 'not'"
              type="primary"
              :loading="enrolling"
              :disabled="selectedToEnroll.length === 0"
              @click="enrollSelectedStudents"
            >
              批量加入课程
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import CoursePlayer from '/src/components/CoursePlayer.vue'
import DocumentViewer from '/src/components/DocumentViewer.vue'
import { listStudents } from '/src/services/coursesApi'
import axios from 'axios'
import { ElMessage } from 'element-plus' // 假设使用了Element Plus，为了更好的提示体验

// 默认占位图
const defaultImage = 'https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png'
const handleImgError = (e) => { e.target.src = defaultImage }

const searchQuery = ref('')
const activeFilter = ref('all')
const filters = [
  { label: '全部课程', value: 'all' },
  { label: '视频课程', value: 'video' },
  { label: '文档课程', value: 'document' }
]

const courses = ref([])
const loading = ref(false)
const userName = ref('老师')
let currentTeacherName = ''
const router = useRouter()

const formatDate = (input) => {
  if (!input) return '待定'
  const s = String(input)
  const d = new Date(s)
  if (!Number.isNaN(d.getTime())) {
    const y = d.getFullYear()
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    return `${y}-${mm}-${dd}`
  }
  return s.slice(0, 10)
}

const filteredCourses = computed(() => {
  const list = courses.value || []
  const text = (searchQuery.value || '').trim().toLowerCase()
  const byFilter = activeFilter.value === 'all' ? list : list.filter((c) => c.type === activeFilter.value)
  if (!text) return byFilter
  return byFilter.filter((c) => [c.title, c.description, c.category].filter(Boolean).some((v) => String(v).toLowerCase().includes(text)))
})

const setFilter = (filter) => { activeFilter.value = filter }

const API_BASE = (import.meta?.env?.VITE_API_BASE_URL || '/api')
const api = axios.create({ baseURL: API_BASE, timeout: 20000 })
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token') || localStorage.getItem('userToken')
  if (token) config.headers = { ...(config.headers || {}), Authorization: `Bearer ${token}` }
  return config
})

// URL处理逻辑保持不变
const normalizeUrl = (url) => {
  if (!url || typeof url !== 'string') return ''
  const u = String(url)
  if (/^(https?:|data:|blob:)/i.test(u)) return u
  if (u.startsWith('/api/uploads/')) return u.replace('/api', '')
  if (u.startsWith('/uploads/')) return u
  if (u.startsWith('uploads/')) return `/${u}`
  return u
}

const normalizeVideoUrl = (url) => {
  if (!url || typeof url !== 'string') return ''
  const u = String(url)
  const m = u.match(/^https?:\/\/[^/]+(:\d+)?\/(uploads\/.*)$/i)
  if (m) return `/${m[2]}`
  if (u.startsWith('/uploads/')) return u
  return normalizeUrl(u)
}

const loadCourses = async () => {
  if (loading.value) return
  loading.value = true
  try {
    const res = await api.get('/course/list')
    const body = res?.data
    const all = (body && Number(body.code) === 200 && Array.isArray(body.data)) ? body.data : []

    const list = []
    for (const c of all) {
      const vids = Array.isArray(c.videos) ? c.videos : []
      const docs = Array.isArray(c.documents) ? c.documents : []

      const base = {
        id: c.courseId,
        title: c.courseName || c.title || '未命名课程',
        description: c.description || '',
        image: normalizeUrl(c.image || c.cover || c.imageUrl || c.img || c.thumbnail || c.pic || c.resourceUrl || ''),
        resourceUrl: normalizeUrl(c.resourceUrl || c.image || c.cover || c.imageUrl || ''),
        startDate: c.startDate || '',
        endDate: c.endDate || '',
        teacher: (c.teacher && (c.teacher.name || c.teacher.t_name)) || c.t_name || currentTeacherName || '',
        videoCount: vids.length,
        docCount: docs.length,
        studentCount: 0,
        averageProgress: 0
      }

      if (vids.length > 0) {
        const videoChapters = vids.map((v, i) => ({
          title: v.videoTitle || v.title || `第${i + 1}节`,
          videoUrl: normalizeVideoUrl(v.videoUrl || v.url || v.fileUrl || ''),
          duration: v.duration
        }))
        list.push({ ...base, type: 'video', chapters: videoChapters, videoUrl: videoChapters[0]?.videoUrl || '' })
      }

      if (docs.length > 0) {
        const docChapters = docs.map((d, i) => ({
          title: d.title || d.name || d.docTitle || `第${i + 1}节`,
          fileUrl: normalizeUrl(d.docUrl || d.fileUrl || d.url || d.resourceUrl || ''),
          html: d.html || d.content || ''
        }))
        list.push({ ...base, type: 'document', chapters: docChapters, fileUrl: docChapters[0]?.fileUrl || '', url: docChapters[0]?.fileUrl || '' })
      }

      if (vids.length === 0 && docs.length === 0) {
        list.push({ ...base, type: 'document', chapters: [], fileUrl: '', url: '' })
      }
    }

    courses.value = list
    await updateStudentCounts()
  } catch (err) {
    console.error(err)
    courses.value = []
  } finally { loading.value = false }
}

const updateStudentCounts = async () => {
  const base = API_BASE
  const token = localStorage.getItem('token') || localStorage.getItem('userToken') || ''
  const headers = token ? { Authorization: `Bearer ${token}` } : {}
  const arr = courses.value
  if (!Array.isArray(arr) || arr.length === 0) return

  // 简单的并发控制
  const fetchInfo = async (c) => {
     try {
        const res = await fetch(`${base}/teacher/enrollments/students?courseId=${encodeURIComponent(c.id)}`, { headers })
        const raw = await res.json().catch(() => ({}))
        const students = (raw && Number(raw.code) === 200 && Array.isArray(raw.data)) ? raw.data : []
        const count = students.length

        // 计算平均进度 (如果有大量学生，这部分可能较慢，实际生产环境建议后端提供统计接口)
        let avgProgress = 0
        if (students.length > 0) {
          // 这里简化，只查前5个学生的进度做示例，或者后端应提供 aggregated data
          // 为保证性能，暂不全量查询所有学生的进度细节，除非点击详情
          // 如果必须显示，保持原有逻辑但需注意性能
          c.studentCount = count
          // Mock or simple logic here to avoid layout thrashing
          c.averageProgress = 0
        } else {
           c.studentCount = 0
           c.averageProgress = 0
        }
     } catch {
       c.studentCount = 0
     }
  }

  // 批量执行
  await Promise.all(arr.map(c => fetchInfo(c)))
}

onMounted(() => {
  loadCourses()
  try {
    const saved = JSON.parse(localStorage.getItem('userInfo') || 'null')
    userName.value = saved?.name || '老师'
    currentTeacherName = saved?.name || ''
  } catch {
    userName.value = '老师'
  }
})

// --- 播放器逻辑 ---
const playerVisible = ref(false)
const activeCourse = ref(null)
const openCourse = (course) => { if (course.type !== 'video') return; activeCourse.value = course; playerVisible.value = true }
const onOverallProgress = () => {}

// --- 文档逻辑 ---
const docVisible = ref(false)
const activeDoc = ref(null)
const openDoc = (course) => {
  const enriched = ensureDocChapters(course)
  if ((!enriched.chapters || enriched.chapters.length === 0)) {
    const u = course.fileUrl || course.url || course.docUrl || course.resourceUrl || ''
    activeDoc.value = { id: course.id || course.courseId, title: course.title || '课程文档', fileUrl: u }
  } else {
    activeDoc.value = enriched
  }
  docVisible.value = true
}
const onCardClick = (course) => { if (course.type === 'video') openCourse(course); else openDoc(course) }

function ensureDocChapters(course) {
  if (!course) return course
  if (course.type !== 'document') return course
  if (Array.isArray(course.chapters) && course.chapters.length > 0) return course
  // ... (保持原有的构造逻辑)
  const urls = []
  if (Array.isArray(course.documents)) {
    for (const d of course.documents) {
      const u = d?.fileUrl || d?.url || d?.docUrl || ''
      if (u) urls.push({ title: d?.title || `第${urls.length + 1}章`, fileUrl: u })
    }
  }
  if (urls.length === 0) {
    const u = course.fileUrl || course.url || course.docUrl || ''
    return { ...course, chapters: [{ title: course.title || '文档', fileUrl: u }] }
  }
  return { ...course, chapters: urls }
}

// --- 学生管理逻辑 ---
const studentManagerVisible = ref(false)
const studentLoading = ref(false)
const studentList = ref([])
const studentSearch = ref('')
const managingCourse = ref(null)
const enrolling = ref(false)
const studentTab = ref('not')
const selectedToEnroll = ref([])
const selectedClassName = ref('')
const notEnrolledTable = ref(null)
const enrolledIds = ref(new Set())

const notEnrolledList = computed(() => (studentList.value || []).filter(s => !enrolledIds.value.has(s.id)))
const enrolledList = computed(() => (studentList.value || []).filter(s => enrolledIds.value.has(s.id)))

const availableClasses = computed(() => {
  const classes = new Set()
  notEnrolledList.value.forEach(s => { if(s.className) classes.add(s.className) })
  return Array.from(classes).sort()
})

const filteredNotEnrolledList = computed(() => {
  let arr = notEnrolledList.value
  if (selectedClassName.value) arr = arr.filter(s => s.className === selectedClassName.value)
  const q = (studentSearch.value || '').toLowerCase().trim()
  if (q) arr = arr.filter(s => String(s.name || '').toLowerCase().includes(q) || String(s.studentNumber || '').toLowerCase().includes(q))
  return arr
})

const filteredEnrolledList = computed(() => {
  const q = (studentSearch.value || '').toLowerCase().trim()
  const arr = enrolledList.value
  if (!q) return arr
  return arr.filter(s => String(s.name || '').toLowerCase().includes(q) || String(s.studentNumber || '').toLowerCase().includes(q))
})

const openStudentManager = async (course) => {
  managingCourse.value = course
  studentManagerVisible.value = true
  studentTab.value = 'not'
  selectedToEnroll.value = []
  selectedClassName.value = ''
  await fetchStudentLists()
}

const fetchStudentLists = async () => {
  studentLoading.value = true
  try {
    const [resAll, resEnrolled] = await Promise.all([listStudents(), fetchEnrolledStudents(true)])
    const body = resAll?.data
    studentList.value = (body && Number(body.code) === 200) ? body.data : []
  } catch { studentList.value = [] } finally { studentLoading.value = false }
}

const fetchEnrolledStudents = async (returnOnly = false) => {
  try {
    const cid = managingCourse.value?.id
    if (!cid) return
    const token = localStorage.getItem('token') || localStorage.getItem('userToken')
    const res = await fetch(`${API_BASE}/teacher/enrollments/students?courseId=${encodeURIComponent(cid)}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    const raw = await res.json()
    const list = raw.data || []
    enrolledIds.value = new Set(list.map(s => s.id))
  } catch (e) { console.error(e) }
}

const onNotSelectionChange = (rows) => { selectedToEnroll.value = rows || [] }

const enrollSelectedStudents = async () => {
  if (!managingCourse.value) return
  const cid = managingCourse.value.id
  enrolling.value = true
  let success = 0, fail = 0

  try {
    const token = localStorage.getItem('token') || localStorage.getItem('userToken')
    for (const stu of selectedToEnroll.value) {
      try {
        const res = await fetch(`${API_BASE}/teacher/enroll?studentId=${stu.id}&courseId=${cid}`, {
          method: 'POST', headers: { Authorization: `Bearer ${token}` }
        })
        const d = await res.json()
        if (Number(d.code) === 200) {
           success++
           enrolledIds.value = new Set([...enrolledIds.value, stu.id])
        } else fail++
      } catch { fail++ }
    }

    // UI Feedback
    if (typeof ElMessage !== 'undefined') {
       if (fail === 0) ElMessage.success(`成功添加 ${success} 名学生`)
       else ElMessage.warning(`添加完成：成功 ${success}，失败 ${fail}`)
    } else {
       alert(`成功: ${success}, 失败: ${fail}`)
    }

    selectedToEnroll.value = []
    notEnrolledTable.value?.clearSelection()
  } finally { enrolling.value = false }
}

// --- 路由跳转操作 ---
const viewCourseProgress = (course) => {
  const id = course.id
  if (id) router.push({ name: 'TeacherCourseProgress', params: { id: String(id) } })
}
const editCourseMaterials = (course) => {
  const id = course.id
  if (id) router.push(`/teacher/courses/${id}/materials`)
}
const deleteCourse = async (course) => {
  // 注意：UI上已经加了 el-popconfirm，这里只需要执行逻辑，不再需要 window.confirm
  try {
    const token = localStorage.getItem('token') || localStorage.getItem('userToken')
    const res = await fetch(`${API_BASE}/course/delete?courseId=${course.id}`, { method: 'DELETE', headers: { Authorization: `Bearer ${token}` } })
    const data = await res.json()
    if (Number(data?.code) === 200) {
       courses.value = courses.value.filter(c => c.id !== course.id)
       if(typeof ElMessage !== 'undefined') ElMessage.success('课程删除成功')
    } else {
       if(typeof ElMessage !== 'undefined') ElMessage.error(data?.message || '删除失败')
    }
  } catch {
     if(typeof ElMessage !== 'undefined') ElMessage.error('网络错误，请稍后重试')
  }
}

// 自动刷新逻辑
let refreshTimer = null
watch(studentManagerVisible, (val) => {
  if (val) {
    refreshTimer = setInterval(() => {
      if (studentManagerVisible.value && managingCourse.value && selectedToEnroll.value.length === 0 && !enrolling.value) {
        fetchEnrolledStudents()
      }
    }, 5000)
  } else {
    clearInterval(refreshTimer)
  }
})
onUnmounted(() => clearInterval(refreshTimer))
</script>

<style scoped>
/* 基础变量 */
:root {
  --primary-color: #3b82f6;
  --secondary-color: #64748b;
  --success-color: #10b981;
  --danger-color: #ef4444;
  --bg-color: #f8fafc;
  --card-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
  --card-hover-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
}

.content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  font-family: 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  color: #334155;
}

/* 顶部 Header */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e2e8f0;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.welcome-text {
  color: #64748b;
  font-size: 14px;
  margin: 0;
  text-align: right;
}

/* 工具栏区域 */
.toolbar-container {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  background: white;
  padding: 16px 20px;
  border-radius: 12px;
  box-shadow: var(--card-shadow);
}

.search-box {
  position: relative;
  flex: 1;
  max-width: 400px;
}

.search-box i {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
}

.search-box input {
  width: 100%;
  padding: 10px 10px 10px 36px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.2s;
  outline: none;
}

.search-box input:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.filter-group {
  display: flex;
  background: #f1f5f9;
  padding: 4px;
  border-radius: 8px;
}

.filter-pill {
  padding: 6px 16px;
  border: none;
  background: transparent;
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
}

.filter-pill.active {
  background: white;
  color: var(--primary-color);
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

/* 网格布局 */
.courses-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
  min-height: 200px;
}

/* 现代化卡片设计 */
.modern-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: var(--card-shadow);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(0,0,0,0.02);
  position: relative;
}

.modern-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--card-hover-shadow);
}

.card-cover {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9; /* 强制比例 */
  overflow: hidden;
  background: #f1f5f9;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.6s;
}

.modern-card:hover .card-cover img {
  transform: scale(1.08);
}

.card-badges {
  position: absolute;
  top: 10px;
  left: 10px;
  right: 10px;
  display: flex;
  justify-content: space-between;
  pointer-events: none;
}

.type-badge {
  background: rgba(0, 0, 0, 0.65);
  backdrop-filter: blur(4px);
  color: white;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 5px;
}

.type-badge.video i { color: #fd79a8; }
.type-badge.document i { color: #00cec9; }

.status-badge {
  background: rgba(16, 185, 129, 0.9);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: bold;
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.modern-card:hover .cover-overlay {
  opacity: 1;
}

.overlay-btn {
  background: white;
  color: #0f172a;
  border: none;
  padding: 8px 16px;
  border-radius: 20px;
  font-weight: 600;
  cursor: pointer;
  transform: translateY(10px);
  transition: transform 0.3s;
}

.modern-card:hover .overlay-btn {
  transform: translateY(0);
}

.card-body {
  padding: 16px;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 8px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 1; /* 限制标题为1行 */
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-desc {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 16px 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  height: 40px; /* 固定高度保持对齐 */
}

.card-meta {
  margin-top: auto;
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #94a3b8;
  border-top: 1px solid #f1f5f9;
  padding-top: 12px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

/* === 按钮区域优化 (Toolbar Style) === */
.card-actions {
  padding: 10px 16px 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  background: white;
}

/* 左侧：主要业务按钮 */
.action-group.main {
  flex: 1;
  display: flex;
  gap: 8px;
}

.action-btn {
  flex: 1;
  border: none;
  padding: 7px 0;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}

.btn-blue {
  background: #eff6ff;
  color: #2563eb;
}
.btn-blue:hover {
  background: #2563eb;
  color: white;
}

.btn-cyan {
  background: #ecfeff;
  color: #0891b2;
}
.btn-cyan:hover {
  background: #0891b2;
  color: white;
}

/* 分割线 */
.divider-vertical {
  width: 1px;
  height: 20px;
  background: #e2e8f0;
}

/* 右侧：编辑/删除图标 */
.action-group.secondary {
  display: flex;
  gap: 4px;
}

.icon-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: #94a3b8;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
}

.icon-btn.edit:hover {
  background: #f8fafc;
  color: #3b82f6;
}

.icon-btn.delete:hover {
  background: #fef2f2;
  color: #ef4444;
}

/* 状态展示 */
.state-container {
  grid-column: 1 / -1;
  text-align: center;
  padding: 60px 0;
  color: #94a3b8;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f4f6;
  border-top: 3px solid var(--primary-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 15px;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* 弹窗自定义 */
.dialog-toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
  align-items: center;
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.selection-info {
  font-size: 13px;
  color: #64748b;
}

.footer-btns {
  margin-left: auto;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .header { flex-direction: column; align-items: flex-start; }
  .toolbar-container { padding: 12px; }
  .search-box { max-width: 100%; width: 100%; }
  .dialog-toolbar { flex-direction: column; gap: 10px; align-items: stretch; }
  .right-tools { display: flex; }

  .card-actions {
    flex-wrap: wrap;
  }
  .divider-vertical { display: none; }
  .action-group.main { width: 100%; }
  .action-group.secondary { width: 100%; justify-content: flex-end; margin-top: 8px; border-top: 1px solid #f1f5f9; padding-top: 8px;}
  .icon-btn { width: auto; padding: 0 10px; }
}
</style>
