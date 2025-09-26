<template>
  <div class="teacher-courses">
    <div class="page-header">
      <h2>课程管理</h2>
      <el-button type="primary" @click="navigateToCreate"><el-icon><Plus/></el-icon>创建新课程</el-button>
    </div>

    <div class="filter-bar">
      <el-input v-model="searchQuery" placeholder="搜索课程名称..." :prefix-icon="Search" style="width:300px;margin-right:16px;" clearable />
      <el-select v-model="categoryFilter" placeholder="按分类筛选" clearable>
        <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
      </el-select>
      <el-select v-model="statusFilter" placeholder="按状态筛选" clearable style="margin-left:12px;">
        <el-option label="已发布" value="published" />
        <el-option label="草稿" value="draft" />
      </el-select>
    </div>

    <div class="courses-grid">
      <div class="course-card" v-for="course in paginatedCourses" :key="course.id">
        <div class="course-image">
          <img :src="course.image || 'https://via.placeholder.com/300x180?text=课程封面'" :alt="course.title" />
          <div class="course-status-badge" :class="course.status">{{ course.status === 'published' ? '已发布' : '草稿' }}</div>
          <div class="course-stats-overlay">
            <div class="stat-item"><el-icon><User/></el-icon><span>{{ course.studentCount || 0 }} 学生</span></div>
            <div class="stat-item"><el-icon><VideoPlay/></el-icon><span>{{ course.videoCount || 0 }} 视频</span></div>
            <div class="stat-item"><el-icon><Document/></el-icon><span>{{ course.documentCount || 0 }} 文档</span></div>
          </div>
        </div>

        <div class="course-content">
          <div class="course-header">
            <h3 class="course-title">{{ course.title }}</h3>
            <div class="course-actions">
              <el-dropdown trigger="click">
                <el-button link><el-icon><More/></el-icon></el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="editCourse(course)"><el-icon><Edit/></el-icon> 编辑课程</el-dropdown-item>
                    <el-dropdown-item @click="manageMaterials(course)"><el-icon><Setting/></el-icon> 管理内容</el-dropdown-item>
                    <el-dropdown-item v-if="course.status==='draft'" @click="publishCourse(course)"><el-icon><Upload/></el-icon> 发布课程</el-dropdown-item>
                    <el-dropdown-item v-else @click="unpublishCourse(course)"><el-icon><Download/></el-icon> 下架课程</el-dropdown-item>
                    <el-dropdown-item divided @click="deleteCourse(course)"><el-icon color="#f56c6c"><Delete/></el-icon><span style="color:#f56c6c;">删除课程</span></el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>

          <p class="course-description">{{ course.description }}</p>

          <div class="course-meta">
            <div class="meta-item"><el-icon><PriceTag/></el-icon><span>{{ getCategoryName(course.categoryId) }}</span></div>
            <div class="meta-item"><el-icon><Calendar/></el-icon><span>{{ formatDate(course.createTime) }}</span></div>
          </div>

          <div class="course-progress">
            <div class="progress-info"><span>平均完成率: {{ course.completionRate || 0 }}%</span></div>
            <el-progress :percentage="course.completionRate || 0" :color="progressColor(course.completionRate || 0)" :show-text="false" />
          </div>

          <div class="teacher-actions">
            <el-button size="small" @click="viewAnalytics(course)"><el-icon><DataAnalysis/></el-icon>数据分析</el-button>
            <el-button size="small" type="primary" @click="viewStudents(course)"><el-icon><User/></el-icon>学生管理</el-button>
            <el-button size="small" plain @click="openVideoPicker(course)"><el-icon><VideoPlay/></el-icon>观看视频</el-button>
            <el-button size="small" plain @click="previewCourseDocs(course)"><el-icon><Document/></el-icon>查看文档</el-button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading-state"><el-icon class="is-loading"><Loading/></el-icon><p>加载中...</p></div>
    <div v-else-if="filteredCourses.length === 0" class="empty-state"><el-icon><Notebook/></el-icon><p>暂无课程</p><el-button type="primary" @click="navigateToCreate">创建第一个课程</el-button></div>

    <div class="pagination" v-if="filteredCourses.length > 0">
      <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[12,24,36,48]" :total="filteredCourses.length" layout="total, sizes, prev, pager, next, jumper" background @update:current-page="handleCurrentPageChange" @update:page-size="handlePageSizeChange" />
    </div>

    <!-- 预览弹窗组件 -->
    <CoursePlayer v-model="playerVisible" :course-id="playerCourseId" :title="playerTitle" :chapters="playerChapters" :start-index="playerStartIndex" />
    <DocumentViewer v-model="docVisible" :title="docTitle" :file-url="docFileUrl" :html-content="docHtmlContent" :progress="docProgress" :id="docId" :image="docImage" :duration="docDuration" />
    <el-dialog v-model="videoPickerVisible" :title="videoPickerTitle" width="560px">
      <el-table :data="videoPickerList" style="width: 100%" size="small" height="360">
        <el-table-column label="#" width="60"><template #default="scope">{{ (scope.row.videoIndex ?? scope.$index) + 1 }}</template></el-table-column>
        <el-table-column prop="videoTitle" label="标题" min-width="240" />
        <el-table-column prop="duration" label="时长(秒)" width="100" />
        <el-table-column label="操作" width="120" fixed="right"><template #default="scope"><el-button size="small" type="primary" @click="openVideoAt(videoPickerCourse, scope.$index)">播放</el-button></template></el-table-column>
      </el-table>
      <template #footer><el-button @click="videoPickerVisible = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, User, VideoPlay, Document, More, Edit, Setting, Upload, Download, Delete, PriceTag, Calendar, DataAnalysis, Notebook, Loading } from '@element-plus/icons-vue'
import axios from 'axios'

export default {
  name: 'CoursesList',
  components: { Plus, Search, User, VideoPlay, Document, More, Edit, Setting, Upload, Download, Delete, PriceTag, Calendar, DataAnalysis, Notebook, Loading, CoursePlayer: () => import('@/components/CoursePlayer.vue'), DocumentViewer: () => import('@/components/DocumentViewer.vue') },
  setup() {
    const router = useRouter()
    const token = ref(localStorage.getItem('token') || '')
    const backendHost = (() => { try { if (window?.location?.port === '4173') return 'http://localhost:9999' } catch {} return '' })()

    const courses = ref([])
    const categories = ref([]) // 先不请求后端分类接口，保持空列表
    const loading = ref(false)
    const searchQuery = ref('')
    const categoryFilter = ref('')
    const statusFilter = ref('')
    const currentPage = ref(1)
    const pageSize = ref(12)

    const loadCourses = async () => {
      loading.value = true
      try {
        const base = (import.meta?.env?.VITE_API_BASE_URL || 'http://localhost:9999/api')
        const res = await axios.get(`${base}/course/list`)
        const body = res?.data
        if (body && Number(body.code) === 200 && Array.isArray(body.data)) {
          courses.value = body.data.map(c => ({ id: c.courseId ?? c.id, title: c.courseName ?? c.title ?? '', description: c.description ?? '', image: c.resourceUrl ?? '', resourceUrl: c.resourceUrl ?? '', status: 'published', categoryId: null, createTime: c.startDate ?? null, studentCount: 0, videoCount: Array.isArray(c.videos) ? c.videos.length : (c.videoCount ?? 0) ?? 0, documentCount: Array.isArray(c.documents) ? c.documents.length : (c.documentCount ?? 0) ?? 0, completionRate: 0 }))
          try { await updateStudentCounts() } catch {}
        } else { courses.value = []; ElMessage.error(body?.message || '加载课程失败') }
      } catch (error) { console.error('加载课程失败:', error); if (error.response?.status === 401) { ElMessage.error('登录已过期，请重新登录'); router.push('/login') } else { ElMessage.error('加载课程失败，请稍后重试') } }
      finally { loading.value = false }
    }

    const updateStudentCounts = async () => {
      const base = (import.meta?.env?.VITE_API_BASE_URL || 'http://localhost:9999/api')
      const headers = token.value ? { Authorization: `Bearer ${token.value}` } : {}
      const list = courses.value
      if (!Array.isArray(list) || list.length === 0) return
      let idx = 0
      const concurrency = 5
      const workers = []
      for (let i = 0; i < Math.min(concurrency, list.length); i++) {
        workers.push((async () => {
          while (true) {
            const current = idx++
            if (current >= list.length) break
            const course = list[current]
            if (!course || !course.id) continue
            try {
              const res = await axios.get(`${base}/teacher/enrollments/students`, { params: { courseId: course.id }, headers })
              const body = res?.data
              const count = (body && Number(body.code) === 200 && Array.isArray(body.data)) ? body.data.length : 0
              courses.value[current] = { ...courses.value[current], studentCount: count }
            } catch { courses.value[current] = { ...courses.value[current], studentCount: 0 } }
          }
        })())
      }
      await Promise.all(workers)
    }

    // 移除分类加载以避免 404，如需恢复可接入实际后端路径
    const loadCategories = async () => { categories.value = [] }

    const getCategoryName = (categoryId) => { const category = categories.value.find(c => c.id === categoryId); return category ? category.name : '未知分类' }
    const formatDate = (dateString) => { if (!dateString) return ''; const date = new Date(dateString); return date.toLocaleDateString('zh-CN') }

    const filteredCourses = computed(() => {
      let result = courses.value
      if (searchQuery.value) {
        const query = searchQuery.value.toLowerCase()
        result = result.filter(course => course.title.toLowerCase().includes(query) || course.description.toLowerCase().includes(query))
      }
      if (categoryFilter.value) result = result.filter(course => course.categoryId == categoryFilter.value)
      if (statusFilter.value) result = result.filter(course => course.status === statusFilter.value)
      return result
    })
    const paginatedCourses = computed(() => { const start = (currentPage.value - 1) * pageSize.value; const end = start + pageSize.value; return filteredCourses.value.slice(start, end) })

    const progressColor = (p) => { if (p >= 80) return '#67c23a'; if (p >= 60) return '#e6a23c'; if (p >= 40) return '#f56c6c'; return '#909399' }

    const navigateToCreate = () => { router.push('/teacher/courses/create') }
    const editCourse = (course) => { router.push(`/teacher/courses/edit/${course.id}`) }
    const manageMaterials = (course) => { router.push(`/teacher/courses/${course.id}/materials`) }

    const publishCourse = async (course) => {
      try {
        ElMessageBox.confirm(`确定要发布课程 "${course.title}" 吗？发布后学生将可以看到此课程。`, '发布确认', { confirmButtonText: '发布', cancelButtonText: '取消', type: 'warning' }).then(async () => {
          ElMessage.success('课程发布成功'); loadCourses()
        }).catch(() => {})
      } catch (error) { console.error(error); ElMessage.error('发布失败，请稍后重试') }
    }
    const unpublishCourse = async (course) => {
      try {
        ElMessageBox.confirm(`确定要下架课程 "${course.title}" 吗？`, '下架确认', { confirmButtonText: '下架', cancelButtonText: '取消', type: 'warning' }).then(async () => {
          ElMessage.warning('课程已下架'); loadCourses()
        }).catch(() => {})
      } catch (error) { console.error(error); ElMessage.error('下架失败，请稍后重试') }
    }
    const deleteCourse = async (course) => {
      try {
        ElMessageBox.confirm(`确定要删除课程 "${course.title}" 吗？此操作不可恢复。`, '删除确认', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'error', confirmButtonClass: 'el-button--danger' }).then(async () => {
          const base = (import.meta?.env?.VITE_API_BASE_URL || 'http://localhost:9999/api')
          const response = await axios.delete(`${base}/course/delete`, { params: { courseId: course.id }, headers: { Authorization: `Bearer ${token.value}` } })
          const body = response?.data
          if (body && Number(body.code) === 200) { ElMessage.success('课程删除成功'); loadCourses() } else { ElMessage.error(body?.message || '删除失败') }
        }).catch(() => {})
      } catch (error) { console.error(error); ElMessage.error('删除失败，请稍后重试') }
    }

    const normalizeUrl = (url) => { if (!url || typeof url !== 'string') return ''; if (/^https?:\/\//i.test(url) || url.startsWith('data:')) return encodeURI(url); if (url.startsWith('/uploads/')) return encodeURI((backendHost || '') + url); return encodeURI(url) }

    const playerVisible = ref(false)
    const playerTitle = ref('')
    const playerCourseId = ref(null)
    const playerChapters = ref([])
    const playerStartIndex = ref(0)

    const docVisible = ref(false)
    const docTitle = ref('')
    const docFileUrl = ref('')
    const docHtmlContent = ref('')
    const docProgress = ref(0)
    const docId = ref(null)
    const docImage = ref('')
    const docDuration = ref('')

    const courseVideosCache = ref({})
    const loadCourseVideos = async (courseId) => { try { const base = import.meta?.env?.VITE_API_BASE_URL || 'http://localhost:9999/api'; const headers = token.value ? { Authorization: `Bearer ${token.value}` } : {}; const res = await axios.get(`${base}/course/video/list`, { params: { courseId }, headers }); const body = res?.data; if (body && Number(body.code) === 200 && Array.isArray(body.data)) return body.data; return [] } catch { return [] } }
    const getCourseVideos = async (courseId) => { const key = String(courseId); const cached = courseVideosCache.value[key]; if (cached && Array.isArray(cached)) return cached; const list = await loadCourseVideos(courseId); courseVideosCache.value[key] = list; return list }
    const loadCourseDocs = async (courseId) => { try { const base = import.meta?.env?.VITE_API_BASE_URL || 'http://localhost:9999/api'; const headers = token.value ? { Authorization: `Bearer ${token.value}` } : {}; const res = await axios.get(`${base}/course/document/list`, { params: { courseId }, headers }); const body = res?.data; if (body && Number(body.code) === 200 && Array.isArray(body.data)) return body.data; return [] } catch { return [] } }

    const openVideoPicker = async (course) => { const cid = course.id || course.courseId; if (!cid) { ElMessage.error('缺少课程ID'); return } const list = await getCourseVideos(cid); if (!list.length) { ElMessage.info('该课程暂无视频'); return } videoPickerCourse.value = course; videoPickerTitle.value = (course.title || course.courseName || '课程') + ' - 选择视频'; videoPickerList.value = list; videoPickerVisible.value = true }
    const openVideoAt = async (course, index) => { const cid = course.id || course.courseId; const list = await getCourseVideos(cid); if (!list.length) { ElMessage.info('该课程暂无视频'); return } playerCourseId.value = cid; playerTitle.value = course.title || course.courseName || '课程视频'; playerChapters.value = list.map(v => ({ title: v.videoTitle || `第${(v.videoIndex ?? 0) + 1}节`, videoUrl: normalizeUrl(v.videoUrl), duration: v.duration ? String(v.duration) : '' })); playerStartIndex.value = Math.max(0, Math.min(index || 0, list.length - 1)); videoPickerVisible.value = false; playerVisible.value = true }

    const previewCourseDocs = async (course) => { const cid = course.id || course.courseId; if (!cid) { ElMessage.error('缺少课程ID'); return } const list = await loadCourseDocs(cid); if (!list.length) { ElMessage.info('该课程暂无文档'); const url = (course.resourceUrl || course.image || '').toString(); if (/\.(pdf|docx?|pptx?)(\?.*)?$/i.test(url)) { docTitle.value = course.title || '课程文档'; docFileUrl.value = normalizeUrl(url); docHtmlContent.value = ''; docProgress.value = 0; docId.value = cid; docImage.value = ''; docDuration.value = ''; docVisible.value = true } return } const d = list[0]; docTitle.value = d.title || d.documentTitle || '课程文档'; docFileUrl.value = d.fileUrl ? normalizeUrl(d.fileUrl) : ''; docHtmlContent.value = d.content || ''; docProgress.value = 0; docId.value = d.id || null; docImage.value = ''; docDuration.value = ''; docVisible.value = true }

    const videoPickerVisible = ref(false)
    const videoPickerTitle = ref('选择视频')
    const videoPickerList = ref([])
    const videoPickerCourse = ref(null)

    onMounted(() => { if (!token.value) { ElMessage.error('用户未登录，请先登录'); router.push('/login'); return } loadCourses() })

    return { courses, categories, loading, searchQuery, categoryFilter, statusFilter, currentPage, pageSize, filteredCourses, paginatedCourses, progressColor, navigateToCreate, editCourse, manageMaterials, publishCourse, unpublishCourse, deleteCourse, getCategoryName, formatDate, playerVisible, playerTitle, playerCourseId, playerChapters, playerStartIndex, docVisible, docTitle, docFileUrl, docHtmlContent, docProgress, docId, docImage, docDuration, openVideoPicker, openVideoAt, videoPickerVisible, videoPickerTitle, videoPickerList, videoPickerCourse, previewCourseDocs }
  }
}
</script>

<style scoped>
.teacher-courses { padding: 20px; background-color: #f5f7fa; min-height: calc(100vh - 40px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; padding: 16px 24px; background: white; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.page-header h2 { font-size: 24px; font-weight: 600; color: #2c3e50; margin: 0; }
.filter-bar { display: flex; align-items: center; margin-bottom: 0; padding: 16px 24px; background: white; border-radius: 8px 8px 0 0; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.courses-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(350px, 1fr)); gap: 24px; margin-bottom: 24px; }
.course-card { background: white; border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.1); overflow: hidden; transition: all 0.3s ease; border: 1px solid #e8e8e8; }
.course-card:hover { transform: translateY(-4px); box-shadow: 0 8px 25px rgba(0,0,0,0.15); }
.course-image { position: relative; height: 180px; overflow: hidden; }
.course-image img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.3s ease; }
.course-card:hover .course-image img { transform: scale(1.05); }
.course-status-badge { position: absolute; top: 12px; right: 12px; padding: 6px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; color: white; }
.course-status-badge.published { background: linear-gradient(135deg, #4ecdc4, #44bd87); }
.course-status-badge.draft { background: linear-gradient(135deg, #ffd666, #ffc53d); }
.course-stats-overlay { position: absolute; bottom: 0; left: 0; right: 0; background: linear-gradient(transparent, rgba(0, 0, 0, 0.8)); padding: 12px; display: flex; justify-content: space-around; }
.stat-item { display: flex; align-items: center; gap: 4px; color: white; font-size: 12px; }
.course-content { padding: 20px; }
.course-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px; }
.course-title { font-size: 18px; font-weight: 600; color: #2c3e50; margin: 0; line-height: 1.4; flex: 1; margin-right: 12px; }
.course-actions { flex-shrink: 0; }
.course-description { color: #666; font-size: 14px; line-height: 1.6; margin-bottom: 16px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.course-meta { display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 16px; }
.meta-item { display: flex; align-items: center; gap: 4px; font-size: 12px; color: #888; }
.course-progress { margin-bottom: 16px; }
.progress-info { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; font-size: 12px; color: #666; }
.teacher-actions { display: flex; gap: 4px; }
.teacher-actions .el-button { flex: initial; padding: 4px 8px; }
.loading-state { text-align: center; padding: 60px 20px; color: #999; background: white; border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.1); }
.loading-state .el-icon { font-size: 48px; margin-bottom: 16px; color: #409EFF; animation: rotating 2s linear infinite; }
@keyframes rotating { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.empty-state { text-align: center; padding: 60px 20px; color: #999; background: white; border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.1); }
.empty-state .el-icon { font-size: 64px; margin-bottom: 16px; color: #ddd; }
.empty-state p { font-size: 16px; margin-bottom: 20px; }
.pagination { display: flex; justify-content: center; margin-top: 24px; }
@media (max-width: 768px) {
  .courses-grid { grid-template-columns: 1fr; gap: 16px; }
  .filter-bar { flex-direction: column; gap: 12px; }
  .filter-bar .el-input { width: 100% !important; margin-right: 0 !important; }
  .page-header { flex-direction: column; align-items: flex-start; gap: 16px; }
}
</style>


