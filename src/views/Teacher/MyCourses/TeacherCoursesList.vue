<template>
  <div class="content">
    <div class="header">
      <h1 class="page-title">课程列表</h1>
      <div class="welcome-text">欢迎回来，{{ userName }}！</div>
    </div>

    <div class="search-container">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input type="text" placeholder="搜索课程名称、关键词..." v-model="searchQuery" @input="filterCourses" />
      </div>

      <div class="search-filters">
        <button class="filter-btn" :class="{active: activeFilter === 'all'}" @click="setFilter('all')">全部课程</button>
        <button class="filter-btn" :class="{active: activeFilter === 'document'}" @click="setFilter('document')">文档课程</button>
        <button class="filter-btn" :class="{active: activeFilter === 'video'}" @click="setFilter('video')">视频课程</button>
      </div>
    </div>

    <div class="courses-container">
      <div v-if="loading" class="loading-container">
        <div class="loading-spinner"></div>
        <p>正在加载课程数据...</p>
      </div>

      <div v-for="course in filteredCourses" :key="course.id" class="course-card" @click="onCardClick(course)">
        <div class="course-image">
          <img :src="course.image" :alt="course.title" />
          <div class="course-type-badge" :class="course.type === 'video' ? 'video' : 'file'">
            <i :class="course.type === 'video' ? 'fas fa-video' : 'fas fa-file-alt'"></i>
            {{ course.type === 'video' ? '视频课程' : '文档课程' }}
          </div>
        </div>
        <div class="course-content">
          <h3 class="course-title">{{ course.title }}</h3>
          <div class="course-category"><i class="fas fa-tag"></i><span>{{ course.description }}</span></div>
          <div class="course-meta">
            <span>{{ formatDate(course.startDate) }} ~ {{ formatDate(course.endDate) }}</span>
            <span>{{ course.teacher || '教师待定' }}</span>
          </div>
          <div class="teacher-actions" @click.stop>
            <button class="btn-secondary" @click.stop="openStudentManager(course)">学生管理</button>
            <button class="btn-secondary" @click.stop="editCourse(course)">修改课程</button>
            <button class="btn-primary" @click.stop="editCourseMaterials(course)">编辑课程内容</button>
            <button class="btn-danger" @click.stop="deleteCourse(course)">删除课程</button>
          </div>
        </div>
      </div>

      <div v-if="!loading && filteredCourses.length === 0" class="no-results">
        <i class="fas fa-search"></i>
        <h3>没有找到相关课程</h3>
        <p>请尝试其他搜索关键词或筛选条件</p>
      </div>
    </div>

    <CoursePlayer v-if="activeCourse" v-model="playerVisible" :course-id="activeCourse.id" :title="activeCourse.title" :chapters="activeCourse.chapters || []" :fallback-src="activeCourse.videoUrl || ''" :video-count="(activeCourse.chapters && activeCourse.chapters.length) || activeCourse.videoCount || 0" :enable-questions="false" @progress="onOverallProgress" />

    <DocumentViewer v-model="docVisible" :id="activeDoc?.id" :title="activeDoc?.title || '文档课程'" :file-url="activeDoc?.fileUrl || activeDoc?.url || ''" :html-content="activeDoc?.html || ''" :chapters="activeDoc?.chapters || []" :course-title="activeDoc?.title || ''" :chapter-index="1" :progress="0" :image="activeDoc?.image || ''" :duration="activeDoc?.duration || ''" />

    <el-dialog v-model="studentManagerVisible" :title="'学生管理 - ' + (managingCourse?.title || '课程')" width="820px">
      <div class="student-manager">
        <div class="toolbar">
          <el-input v-model="studentSearch" placeholder="搜索姓名或学号" clearable style="max-width: 300px;" />
          <div class="actions">
            <el-button type="primary" :loading="enrolling" :disabled="selectedToEnroll.length===0" @click="enrollSelectedStudents">加入选中学生</el-button>
          </div>
        </div>
        <el-tabs v-model="studentTab">
          <el-tab-pane label="未选课学生" name="not">
            <el-table :data="filteredNotEnrolledList" style="width: 100%" height="360" v-loading="studentLoading" @selection-change="onNotSelectionChange">
              <el-table-column type="selection" width="48" fixed="left" />
              <el-table-column prop="studentNumber" label="学号" width="140" />
              <el-table-column prop="name" label="姓名" width="120" />
              <el-table-column prop="className" label="班级" width="120" />
              <el-table-column prop="phone" label="手机号" width="150" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="已选课学生" name="enrolled">
            <el-table :data="filteredEnrolledList" style="width: 100%" height="360" v-loading="studentLoading">
              <el-table-column prop="studentNumber" label="学号" width="140" />
              <el-table-column prop="name" label="姓名" width="120" />
              <el-table-column prop="className" label="班级" width="120" />
              <el-table-column prop="phone" label="手机号" width="150" />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
      <template #footer>
        <el-button @click="studentManagerVisible = false">关闭</el-button>
        <el-button type="primary" :loading="enrolling" :disabled="selectedToEnroll.length===0" @click="enrollSelectedStudents">加入选中学生</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import CoursePlayer from '/src/components/CoursePlayer.vue'
import DocumentViewer from '/src/components/DocumentViewer.vue'
import { listStudents } from '/src/services/coursesApi'
import axios from 'axios'

const searchQuery = ref('')
const activeFilter = ref('all')
const courses = ref([])
const loading = ref(false)
const userName = ref('老师')
let currentTeacherName = ''
const router = useRouter()

const formatDate = (input) => {
  if (!input) return '-'
  const s = String(input)
  const m = s.match(/^\d{4}-\d{2}-\d{2}/)
  if (m) return m[0]
  const d = new Date(s)
  if (!Number.isNaN(d.getTime())) {
    const y = d.getFullYear(); const mm = String(d.getMonth() + 1).padStart(2, '0'); const dd = String(d.getDate()).padStart(2, '0')
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

const API_BASE = (import.meta?.env?.VITE_API_BASE_URL || (window?.location?.port === '4173' ? 'http://localhost:9999/api' : '/api'))
const api = axios.create({ baseURL: API_BASE, timeout: 20000 })
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token') || localStorage.getItem('userToken')
  if (token) config.headers = { ...(config.headers || {}), Authorization: `Bearer ${token}` }
  return config
})
const backendHost = (() => { try { const p = window?.location?.port; if (p === '4173' || p === '5173') return 'http://localhost:9999' } catch {} return '' })()
const normalizeUrl = (url) => {
  if (!url || typeof url !== 'string') return ''
  if (/^(https?:|data:|blob:)/i.test(url)) return url
  if (url.startsWith('/uploads/')) return (backendHost || '') + url
  return url
}

// 为视频播放构造同源路径，避免 CORS：将 http(s)://.../uploads/** 重写为同源 /uploads/**
const normalizeVideoUrl = (url) => {
  if (!url || typeof url !== 'string') return ''
  const u = String(url)
  // 直接后端完整地址 → 改为同源 /uploads/**（已由 Vite 代理到后端）
  const m = u.match(/^https?:\/\/[^/]+(:\d+)?\/(uploads\/.*)$/i)
  if (m) return `/${m[2]}`
  if (u.startsWith('/uploads/')) return u
  // 其他情况复用 normalizeUrl
  return normalizeUrl(u)
}

const loadCourses = async () => {
  if (loading.value) return
  loading.value = true
  try {
    // 获取教师ID
    let teacherId = null
    try { const u = JSON.parse(localStorage.getItem('userInfo') || 'null'); if (u?.id) teacherId = Number(u.id) } catch {}
    if (!teacherId) { const tid = localStorage.getItem('teacherId'); if (tid) teacherId = Number(tid) }

    // 优先调用教师聚合接口
    let list = []
    if (teacherId) {
      try {
        const res = await api.get('/teacher/videos', { params: { teacherId } })
        const raw = res?.data
        const data = raw?.data || {}
        const coursesArr = Array.isArray(data?.courses) ? data.courses : []
        list = coursesArr.map(c => {
          const chapters = Array.isArray(c.videos)
            ? c.videos.map(v => ({ title: v.title || v.videoTitle || '视频', videoUrl: normalizeVideoUrl(v.url || v.videoUrl || v.fileUrl || ''), duration: v.duration }))
            : []
          const firstVideoUrl = chapters[0]?.videoUrl || normalizeVideoUrl(c.videoUrl || '')
          return {
            id: c.id || c.course_id || c.courseId,
            title: c.title || c.course_name || c.courseName || '未命名课程',
            description: c.description || '',
            image: normalizeUrl(c.image || c.resourceUrl || ''),
            resourceUrl: normalizeUrl(c.resourceUrl || c.image || ''),
            type: 'video',
            startDate: c.createTime || c.start_date || '',
            endDate: '',
            teacher: currentTeacherName || '',
            videoCount: Number(c.videoCount || (Array.isArray(c.videos) ? c.videos.length : 0) || 0),
            docCount: Number(c.documentCount || 0),
            studentCount: Number(c.studentCount || 0),
            chapters,
            videoUrl: firstVideoUrl
          }
        })
      } catch {}
    }

    // 回退：全量列表再过滤当前教师
    if (!Array.isArray(list) || list.length === 0) {
      try {
        const res = await api.get('/course/list')
        const body = res?.data
        const all = (body && Number(body.code) === 200 && Array.isArray(body.data)) ? body.data : []
        const filtered = teacherId ? all.filter(c => Number(c.teacherId) === teacherId) : all
        list = filtered.map(c => {
          const vids = Array.isArray(c.videos) ? c.videos : []
          const chapters = vids.map(v => ({
            title: v.videoTitle || v.title || `第${(v.videoIndex || v.index || 0) + 1}节`,
            videoUrl: normalizeVideoUrl(v.videoUrl || v.url || v.fileUrl || ''),
            duration: v.duration
          }))
          const firstVideoUrl = chapters[0]?.videoUrl || ''
          return {
            id: c.courseId,
            title: c.courseName || '未命名课程',
            description: c.description || '',
            image: normalizeUrl(c.resourceUrl || ''),
            resourceUrl: normalizeUrl(c.resourceUrl || ''),
            type: vids.length > 0 ? 'video' : 'document',
            startDate: c.startDate || '',
            endDate: c.endDate || '',
            teacher: (c.teacher && (c.teacher.name || c.teacher.t_name)) || c.t_name || currentTeacherName || '',
            videoCount: vids.length,
            docCount: Array.isArray(c.documents) ? c.documents.length : 0,
            studentCount: 0,
            chapters,
            videoUrl: firstVideoUrl
          }
        })
      } catch {}
    }

    courses.value = Array.isArray(list) ? list : []
    // 动态刷新学生数
    await updateStudentCounts()
  } catch { courses.value = [] }
  finally { loading.value = false }
}

const updateStudentCounts = async () => {
  const base = API_BASE
  const token = localStorage.getItem('token') || localStorage.getItem('userToken') || ''
  const headers = token ? { Authorization: `Bearer ${token}` } : {}
  const arr = courses.value
  if (!Array.isArray(arr) || arr.length === 0) return
  let idx = 0
  const concurrency = 5
  const workers = []
  for (let i = 0; i < Math.min(concurrency, arr.length); i++) {
    workers.push((async () => {
      while (true) {
        const cur = idx++
        if (cur >= arr.length) break
        const c = arr[cur]
        if (!c || !c.id) continue
        try {
          const res = await fetch(`${base}/teacher/enrollments/students?courseId=${encodeURIComponent(c.id)}`, { headers })
          const raw = await res.json().catch(() => ({}))
          const count = (raw && Number(raw.code) === 200 && Array.isArray(raw.data)) ? raw.data.length : 0
          courses.value[cur] = { ...courses.value[cur], studentCount: count }
        } catch { courses.value[cur] = { ...courses.value[cur], studentCount: 0 } }
      }
    })())
  }
  await Promise.all(workers)
}

onMounted(() => {
  loadCourses()
  try {
    const saved = JSON.parse(localStorage.getItem('userInfo') || 'null')
    userName.value = saved?.name || '老师'
    currentTeacherName = saved?.name || ''
  } catch {
    userName.value = '老师'
    currentTeacherName = ''
  }
})

const playerVisible = ref(false)
const activeCourse = ref(null)
const openCourse = (course) => { if (course.type !== 'video') return; activeCourse.value = course; playerVisible.value = true }
const onOverallProgress = () => {}

const docVisible = ref(false)
const activeDoc = ref(null)
const openDoc = (course) => {
  const enriched = ensureDocChapters(course)
  // 若章节为空但课程有 resourceUrl，作为单文档打开
  if ((!enriched.chapters || enriched.chapters.length === 0)) {
    const u = course.fileUrl || course.url || course.docUrl || course.resourceUrl || ''
    activeDoc.value = { id: course.id || course.courseId, title: course.title || '课程文档', fileUrl: u }
  } else {
    activeDoc.value = enriched
  }
  docVisible.value = true
}
const onCardClick = (course) => { if (course.type === 'video') openCourse(course); else openDoc(course) }
const filterCourses = () => {}

function ensureDocChapters(course) {
  if (!course) return course
  if (course.type !== 'document') return course
  const hasChapters = Array.isArray(course.chapters) && course.chapters.length > 0
  if (hasChapters) return course
  const urls = []
  if (Array.isArray(course.documents)) {
    for (const d of course.documents) {
      const u = d?.fileUrl || d?.url || d?.docUrl || ''
      const t = d?.title || d?.name || d?.docTitle || `第${urls.length + 1}章`
      if (u) urls.push({ title: t, fileUrl: u })
    }
  }
  if (urls.length === 0) {
    const u = course.fileUrl || course.url || course.docUrl || ''
    const item = { title: course.title || '文档', fileUrl: u, html: course.html || '' }
    return { ...course, chapters: [item] }
  }
  return { ...course, chapters: urls }
}

// 学生管理
const studentManagerVisible = ref(false)
const studentLoading = ref(false)
const studentList = ref([])
const studentSearch = ref('')
const managingCourse = ref(null)
const enrolling = ref(false)
const studentTab = ref('not')
const selectedToEnroll = ref([])

const enrolledIds = ref(new Set())
const notEnrolledList = computed(() => (studentList.value || []).filter(s => !enrolledIds.value.has(s.id)))
const enrolledList = computed(() => (studentList.value || []).filter(s => enrolledIds.value.has(s.id)))
const filteredNotEnrolledList = computed(() => {
  const q = (studentSearch.value || '').toLowerCase().trim()
  const arr = notEnrolledList.value
  if (!q) return arr
  return arr.filter(s => {
    const name = String(s.name || '').toLowerCase()
    const no = String(s.studentNumber || s.studentId || '').toLowerCase()
    return name.includes(q) || no.includes(q)
  })
})
const filteredEnrolledList = computed(() => {
  const q = (studentSearch.value || '').toLowerCase().trim()
  const arr = enrolledList.value
  if (!q) return arr
  return arr.filter(s => {
    const name = String(s.name || '').toLowerCase()
    const no = String(s.studentNumber || s.studentId || '').toLowerCase()
    return name.includes(q) || no.includes(q)
  })
})
const openStudentManager = async (course) => {
  managingCourse.value = course
  studentManagerVisible.value = true
  studentTab.value = 'not'
  selectedToEnroll.value = []
  await fetchStudentLists()
}
const fetchStudentLists = async () => {
  try {
    studentLoading.value = true
    const [resAll, resEnrolled] = await Promise.all([
      listStudents(),
      fetchEnrolledStudents(),
    ])
    const body = resAll?.data
    const arr = (body && Number(body.code) === 200 && Array.isArray(body.data)) ? body.data : []
    studentList.value = arr
  } catch { studentList.value = [] } finally { studentLoading.value = false }
}
const fetchEnrolledStudents = async () => {
  try {
    const base = (import.meta?.env?.VITE_API_BASE_URL || (window?.location?.port === '4173' ? 'http://localhost:9999/api' : '/api'))
    const cid = managingCourse.value?.id || managingCourse.value?.courseId
    if (!cid) { enrolledIds.value = new Set(); return }
    const res = await fetch(`${base}/teacher/enrollments/students?courseId=${encodeURIComponent(cid)}`)
    const raw = await res.json().catch(() => ({}))
    const list = (raw && Number(raw.code) === 200 && Array.isArray(raw.data)) ? raw.data : []
    enrolledIds.value = new Set(list.map(s => s.id))
  } catch { enrolledIds.value = new Set() }
}
const onNotSelectionChange = (rows) => { selectedToEnroll.value = rows || [] }
const enrollSelectedStudents = async () => {
  if (!managingCourse.value) return
  const cid = managingCourse.value.id || managingCourse.value.courseId
  if (!cid) return
  const base = (import.meta?.env?.VITE_API_BASE_URL || (window?.location?.port === '4173' ? 'http://localhost:9999/api' : '/api'))
  const token = localStorage.getItem('token') || localStorage.getItem('userToken') || ''
  const headers = token ? { Authorization: `Bearer ${token}` } : {}
  try {
    enrolling.value = true
    for (const stu of selectedToEnroll.value) {
      const sid = stu.id; if (!sid) continue
      await fetch(`${base}/teacher/enroll?studentId=${encodeURIComponent(sid)}&courseId=${encodeURIComponent(cid)}`, { method: 'POST', headers })
    }
    alert('已加入所选学生')
    studentManagerVisible.value = false
  } catch { alert('操作失败，请稍后重试') } finally { enrolling.value = false }
}

// 跳转
const editCourse = (course) => { const id = course.id || course.courseId; if (!id) return; router.push(`/teacher/courses/edit/${id}`) }
const editCourseMaterials = (course) => { const id = course.id || course.courseId; if (!id) return; router.push(`/teacher/courses/${id}/materials`) }
const deleteCourse = async (course) => {
  try {
    const ok = window.confirm(`确定要删除课程 “${course.title || ''}” 吗？此操作不可恢复。`)
    if (!ok) return
    const base = (import.meta?.env?.VITE_API_BASE_URL || (window?.location?.port === '4173' ? 'http://localhost:9999/api' : '/api'))
    const res = await fetch(`${base}/course/delete?courseId=${encodeURIComponent(course.id || course.courseId)}`, { method: 'DELETE' })
    const data = await res.json().catch(() => ({}))
    if (Number(data?.code) === 200) { courses.value = courses.value.filter(c => (c.id || c.courseId) !== (course.id || course.courseId)); alert('课程删除成功') } else { alert(`删除失败：${data?.message || res.status}`) }
  } catch { alert('删除失败，请稍后重试') }
}
</script>

<style scoped>
/* 复制自学生端 Home.vue，略作精简 */
.content { max-width: 1200px; margin: 0 auto; width: 100%; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px; }
.page-title { font-size: 24px; font-weight: bold; color: #2c3e50; }
.welcome-text { color: #666; font-size: 16px; }
.search-container { background: white; border-radius: 10px; padding: 25px; box-shadow: 0 4px 10px rgba(0,0,0,0.05); margin-bottom: 30px; }
.search-box { display: flex; align-items: center; background: #f0f5ff; border-radius: 8px; padding: 10px 15px; margin-bottom: 20px; }
.search-box input { flex-grow: 1; border: none; background: transparent; padding: 10px; font-size: 16px; outline: none; }
.search-box i { color: #2563eb; font-size: 20px; margin-right: 10px; }
.search-filters { display: flex; gap: 15px; }
.filter-btn { padding: 8px 16px; background: #f0f5ff; border: none; border-radius: 6px; color: #2563eb; cursor: pointer; transition: all 0.3s; }
.filter-btn.active { background: #2563eb; color: white; }
.filter-btn:hover { background: #dbeafe; }
.filter-btn.active:hover { background: #2563eb; }
.courses-container { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 20px; }
.course-card { background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 10px rgba(0,0,0,0.05); transition: transform 0.3s; }
.course-card:hover { transform: translateY(-5px); box-shadow: 0 8px 20px rgba(0,0,0,0.1); }
.course-image { height: 160px; overflow: hidden; position: relative; }
.course-image img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.5s; }
.course-card:hover .course-image img { transform: scale(1.05); }
.course-type-badge { position: absolute; top: 12px; right: 12px; padding: 6px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; color: white; display: flex; align-items: center; gap: 4px; }
.course-type-badge.video { background: linear-gradient(135deg, #ff6b6b, #ee5a24); }
.course-type-badge.file { background: linear-gradient(135deg, #4ecdc4, #44bd87); }
.course-content { padding: 20px; }
.course-title { font-size: 18px; font-weight: 600; margin-bottom: 10px; color: #2c3e50; }
.course-category { display: flex; align-items: center; gap: 6px; margin-bottom: 12px; font-size: 12px; color: #1890ff; background: #f0f8ff; padding: 4px 8px; border-radius: 12px; width: fit-content; }
.course-meta { display: flex; justify-content: space-between; color: #94a3b8; font-size: 13px; }
.teacher-actions { display: flex; gap: 4px; margin-top: 8px; }
.btn-primary { padding: 4px 8px; background: #2563eb; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 12px; }
.btn-primary:hover { background: #1d4ed8; }
.btn-secondary { padding: 4px 8px; background: #64748b; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 12px; }
.btn-secondary:hover { background: #475569; }
.btn-danger { padding: 4px 8px; background: #ef4444; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 12px; }
.btn-danger:hover { background: #dc2626; }
.no-results { text-align: center; padding: 40px; color: #94a3b8; grid-column: 1 / -1; }
.no-results i { font-size: 48px; margin-bottom: 15px; display: block; }
.loading-container { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 20px; color: #94a3b8; grid-column: 1 / -1; }
.loading-spinner { width: 40px; height: 40px; border: 4px solid #f3f4f6; border-top: 4px solid #2563eb; border-radius: 50%; animation: spin 1s linear infinite; margin-bottom: 15px; }
@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
@media (max-width: 992px) { .courses-container { grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); } }
@media (max-width: 768px) { .header { flex-direction: column; align-items: flex-start; gap: 10px; } .search-filters { flex-wrap: wrap; } .courses-container { grid-template-columns: 1fr; } }
</style>


