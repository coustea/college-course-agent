<template>
  <div class="course-materials">
    <h2>课程内容管理</h2>

    <!-- 合并：课程基本信息编辑 -->
    <div class="section">
      <h3>编辑课程信息</h3>
      <el-form :model="course" label-width="120px">
        <el-form-item label="课程名称"><el-input v-model="course.courseName" placeholder="请输入课程名称" /></el-form-item>
        <el-form-item label="课程描述"><el-input v-model="course.description" type="textarea" :rows="3" placeholder="请输入课程描述" /></el-form-item>
        <el-form-item label="更换封面">
          <input type="file" accept="image/*" @change="onCourseImageChange" />
          <img v-if="imagePreview" :src="imagePreview" alt="预览" class="avatar" />
        </el-form-item>
        <el-form-item label="排序权重(vindex)"><el-input v-model.number="course.vindex" placeholder="例如 1/2/3" /></el-form-item>
        <el-form-item><el-button type="primary" :loading="savingCourse" @click="saveCourse">保存课程</el-button></el-form-item>
      </el-form>
    </div>

    <div class="section">
      <h3>添加视频</h3>
      <el-form :model="video" label-width="120px">
        <el-form-item label="课程ID"><el-input v-model.number="video.courseId" placeholder="请输入课程ID" /></el-form-item>
        <el-form-item label="视频集数(videoIndex)"><el-input v-model.number="video.videoIndex" placeholder="例如 1、2、3..." /></el-form-item>
        <el-form-item label="视频标题"><el-input v-model="video.videoTitle" placeholder="请输入视频标题" /></el-form-item>
        <el-form-item label="视频文件"><input type="file" accept=".mp4,.mov,.webm" @change="onVideoFileChange" /></el-form-item>
        <el-form-item label="时长(秒)"><el-input v-model.number="video.duration" placeholder="例如 1800" /></el-form-item>
        <el-form-item><el-button type="primary" :loading="submittingVideo" @click="submitVideo">提交视频</el-button></el-form-item>
      </el-form>
    </div>

    <div class="section">
      <h3>添加文档</h3>
      <el-form :model="doc" label-width="120px">
        <el-form-item label="课程ID"><el-input v-model.number="doc.courseId" placeholder="请输入课程ID" /></el-form-item>
        <el-form-item label="文档序号(docIndex)"><el-input v-model.number="doc.docIndex" placeholder="例如 1、2、3..." /></el-form-item>
        <el-form-item label="文档标题"><el-input v-model="doc.docTitle" placeholder="请输入文档标题" /></el-form-item>
        <el-form-item label="文档文件"><input type="file" accept=".pdf,.doc,.docx,.ppt,.pptx" @change="onDocFileChange" /></el-form-item>
        <el-form-item><el-button type="primary" :loading="submittingDoc" @click="submitDoc">提交文档</el-button></el-form-item>
      </el-form>
    </div>

    <!-- 当前课程视频列表（可选择一条进行修改） -->
    <div class="section">
      <h3>当前视频</h3>
      <el-table :data="videos" style="width:100%" size="small" v-loading="loadingVideos" @row-click="onVideoRowClick">
        <el-table-column prop="videoId" label="ID" width="80" />
        <el-table-column prop="videoIndex" label="序号" width="80" />
        <el-table-column prop="videoTitle" label="标题" min-width="220" />
        <el-table-column prop="duration" label="时长(秒)" width="100" />
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button size="small" type="primary" @click.stop="openEditVideo(scope.row)">修改</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 当前课程文档列表（可选择一条进行修改） -->
    <div class="section">
      <h3>当前文档</h3>
      <el-table :data="documents" style="width:100%" size="small" v-loading="loadingDocs" @row-click="onDocRowClick">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="docIndex" label="序号" width="80" />
        <el-table-column prop="title" label="标题" min-width="220"><template #default="scope">{{ scope.row.title || scope.row.documentTitle }}</template></el-table-column>
        
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button size="small" type="primary" @click.stop="openEditDoc(scope.row)">修改</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <!-- 视频编辑弹窗 -->
    <el-dialog v-model="editVideoVisible" title="编辑视频" width="520px">
      <el-form :model="editVideo" label-width="100px">
        <el-form-item label="视频ID"><el-input v-model="editVideo.videoId" disabled /></el-form-item>
        <el-form-item label="标题"><el-input v-model="editVideo.videoTitle" /></el-form-item>
        <el-form-item label="时长(秒)"><el-input v-model.number="editVideo.duration" /></el-form-item>
        <el-form-item label="替换文件"><input type="file" accept=".mp4,.mov,.webm" @change="onEditVideoFileChange" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVideoVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingVideo" @click="submitEditVideo">保存</el-button>
      </template>
    </el-dialog>

    <!-- 文档编辑弹窗 -->
    <el-dialog v-model="editDocVisible" title="编辑文档" width="520px">
      <el-form :model="editDoc" label-width="100px">
        <el-form-item label="文档ID"><el-input v-model="editDoc.id" disabled /></el-form-item>
        <el-form-item label="序号"><el-input v-model.number="editDoc.docIndex" /></el-form-item>
        <el-form-item label="标题"><el-input v-model="editDoc.docTitle" /></el-form-item>
        <el-form-item label="替换文件"><input type="file" accept=".pdf,.doc,.docx,.ppt,.pptx" @change="onEditDocFileChange" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDocVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingDoc" @click="submitEditDoc">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getAuthHeaders } from '@/services/auth'

export default {
  name: 'CourseMaterials',
  setup() {
    const routeCourseId = (() => { try { const m = window.location.pathname.match(/\/teacher\/courses\/(\d+)\/materials/); return m ? Number(m[1]) : null } catch { return null } })()
    // 课程信息
    const course = ref({ courseId: routeCourseId, courseName: '', description: '', resourceUrl: '', vindex: null })
    const imageFile = ref(null)
    const imagePreview = ref('')
    const savingCourse = ref(false)
    const video = ref({ courseId: routeCourseId, videoIndex: null, videoTitle: '', file: null, duration: null })
    const doc = ref({ courseId: routeCourseId, docIndex: null, docTitle: '', file: null })
    const submittingVideo = ref(false)
    const submittingDoc = ref(false)
    const savingVideo = ref(false)
    const savingDoc = ref(false)
    const videos = ref([])
    const documents = ref([])
    const loadingVideos = ref(false)
    const loadingDocs = ref(false)
    const base = (import.meta?.env?.VITE_API_BASE_URL || '/api')

    // 加载课程详情
    ;(async function loadCourseDetail() {
      try {
        if (!routeCourseId) return
        const res = await axios.get(`${base}/course/detail`, { params: { courseId: routeCourseId }, headers: getAuthHeaders() })
        const body = res?.data
        if (body && Number(body.code) === 200 && body.data) {
          const c = body.data
          course.value = {
            courseId: c.courseId ?? routeCourseId,
            courseName: c.courseName || c.title || '',
            description: c.description || '',
            resourceUrl: c.resourceUrl || '',
            vindex: c.vindex ?? null,
          }
        }
      } catch {}
    })()

    const onCourseImageChange = (e) => { const file = e?.target?.files?.[0]; if (!file) return; imageFile.value = file; const reader = new FileReader(); reader.onload = () => { imagePreview.value = reader.result }; reader.readAsDataURL(file) }

    const saveCourse = async () => {
      try {
        if (!course.value.courseId) { ElMessage.error('缺少课程ID'); return }
        savingCourse.value = true
        if (imageFile.value) {
          const form = new FormData()
          const payload = { courseId: course.value.courseId, courseName: course.value.courseName, description: course.value.description, vindex: course.value.vindex }
          form.append('course', JSON.stringify(payload))
          form.append('image', imageFile.value)
          const res = await axios.put(`${base}/course/update`, form, { headers: getAuthHeaders() })
          const body = res?.data
          if (body && Number(body.code) === 200) { ElMessage.success('课程信息已更新'); imageFile.value = null } else { ElMessage.error(body?.message || '更新失败') }
        } else {
          const res = await axios.put(`${base}/course/update`, course.value, { headers: getAuthHeaders() })
          const body = res?.data
          if (body && Number(body.code) === 200) { ElMessage.success('课程信息已更新') } else { ElMessage.error(body?.message || '更新失败') }
        }
      } catch (e) { console.error(e); ElMessage.error('更新失败，请稍后重试') } finally { savingCourse.value = false }
    }

    // 从本地文件自动探测视频时长（单位：秒，四舍五入）
    const getVideoDurationFromFile = (file) => new Promise((resolve) => {
      try {
        if (!file) return resolve(null)
        const url = URL.createObjectURL(file)
        const el = document.createElement('video')
        el.preload = 'metadata'
        el.src = url
        el.onloadedmetadata = () => {
          try { URL.revokeObjectURL(url) } catch {}
          const d = Number(el.duration)
          resolve(Number.isFinite(d) ? Math.round(d) : null)
        }
        el.onerror = () => {
          try { URL.revokeObjectURL(url) } catch {}
          resolve(null)
        }
      } catch { resolve(null) }
    })

    const onVideoFileChange = async (e) => {
      video.value.file = e.target.files && e.target.files[0]
      if (video.value.file) {
        const dur = await getVideoDurationFromFile(video.value.file)
        if (dur != null) video.value.duration = dur
      }
    }
    const onVideoRowClick = (row) => { if (!row) return; video.value.videoId = row.videoId || row.id; video.value.videoTitle = row.videoTitle || row.title || ''; video.value.duration = row.duration || null; video.value.videoIndex = row.videoIndex || row.index || null }
    const onDocFileChange = (e) => { doc.value.file = e.target.files && e.target.files[0] }
    const onDocRowClick = (row) => { if (!row) return; doc.value.id = row.id; doc.value.docIndex = row.docIndex || row.index || null; doc.value.docTitle = row.title || row.documentTitle || '' }
    // 弹窗编辑 - 视频
    const editVideoVisible = ref(false)
    const editVideo = ref({ videoId: null, videoTitle: '', duration: null, file: null })
    const openEditVideo = (row) => { editVideo.value = { videoId: row.videoId || row.id, videoTitle: row.videoTitle || row.title || '', duration: row.duration || null, file: null }; editVideoVisible.value = true }
    const onEditVideoFileChange = async (e) => {
      editVideo.value.file = e.target.files && e.target.files[0]
      if (editVideo.value.file) {
        const dur = await getVideoDurationFromFile(editVideo.value.file)
        if (dur != null) editVideo.value.duration = dur
      }
    }
    const submitEditVideo = async () => {
      if (!editVideo.value.videoId) { ElMessage.error('缺少视频ID'); return }
      try {
        savingVideo.value = true
        if (editVideo.value.file) {
          const form = new FormData()
          form.append('videoId', String(editVideo.value.videoId))
          if (editVideo.value.videoTitle) form.append('videoTitle', editVideo.value.videoTitle)
          if (editVideo.value.duration != null) form.append('duration', String(editVideo.value.duration))
          form.append('file', editVideo.value.file)
          const res = await axios.put(`${base}/course/video/update`, form, { headers: getAuthHeaders() })
          const body = res?.data
          if (body && Number(body.code) === 200) { ElMessage.success('视频已更新'); editVideoVisible.value = false; await loadVideos() } else { ElMessage.error(body?.message || '更新失败') }
        } else {
          const res = await axios.put(`${base}/course/video/update`, { videoId: editVideo.value.videoId, videoTitle: editVideo.value.videoTitle, duration: editVideo.value.duration }, { headers: getAuthHeaders() })
          const body = res?.data
          if (body && Number(body.code) === 200) { ElMessage.success('视频已更新'); editVideoVisible.value = false; await loadVideos() } else { ElMessage.error(body?.message || '更新失败') }
        }
      } catch (e) { console.error(e); ElMessage.error('更新失败，请稍后重试') } finally { savingVideo.value = false }
    }
    // 弹窗编辑 - 文档
    const editDocVisible = ref(false)
    const editDoc = ref({ id: null, docIndex: null, docTitle: '', file: null, docUrl: '' })
    const openEditDoc = (row) => { editDoc.value = { id: row.id, docIndex: row.docIndex || row.index || null, docTitle: row.title || row.documentTitle || '', file: null, docUrl: row.fileUrl || row.docUrl || '' }; editDocVisible.value = true }
    const onEditDocFileChange = (e) => { editDoc.value.file = e.target.files && e.target.files[0] }
    const submitEditDoc = async () => {
      if (!editDoc.value.id) { ElMessage.error('缺少文档ID'); return }
      try {
        savingDoc.value = true
        if (editDoc.value.file) {
          const form = new FormData()
          form.append('courseId', String(routeCourseId))
          if (editDoc.value.docIndex != null) form.append('docIndex', String(editDoc.value.docIndex))
          if (editDoc.value.docTitle) form.append('docTitle', editDoc.value.docTitle)
          form.append('file', editDoc.value.file)
          const ins = await axios.post(`${base}/course/document/insert`, form, { headers: getAuthHeaders() })
          const iv = ins?.data
          if (!(iv && Number(iv.code) === 200 && iv.data && iv.data.docUrl)) { ElMessage.error(iv?.message || '文档上传失败'); savingDoc.value = false; return }
          const payload = { id: editDoc.value.id, docIndex: editDoc.value.docIndex, docTitle: editDoc.value.docTitle, docUrl: iv.data.docUrl }
          const res = await axios.put(`${base}/course/document/update`, payload, { headers: getAuthHeaders() })
          const body = res?.data
          if (body && Number(body.code) === 200) { ElMessage.success('文档已更新'); editDocVisible.value = false; await loadDocs() } else { ElMessage.error(body?.message || '更新失败') }
        } else {
          const payload = { id: editDoc.value.id, docIndex: editDoc.value.docIndex, docTitle: editDoc.value.docTitle, docUrl: editDoc.value.docUrl }
          const res = await axios.put(`${base}/course/document/update`, payload, { headers: getAuthHeaders() })
          const body = res?.data
          if (body && Number(body.code) === 200) { ElMessage.success('文档已更新'); editDocVisible.value = false; await loadDocs() } else { ElMessage.error(body?.message || '更新失败') }
        }
      } catch (e) { console.error(e); ElMessage.error('更新失败，请稍后重试') } finally { savingDoc.value = false }
    }

    const submitVideo = async () => {
      try {
        submittingVideo.value = true
        const form = new FormData(); form.append('courseId', video.value.courseId); if (video.value.videoIndex != null) form.append('videoIndex', String(video.value.videoIndex)); if (video.value.videoTitle) form.append('videoTitle', video.value.videoTitle); if (video.value.file) form.append('file', video.value.file)
        const res = await axios.post(`${base}/course/video/insert`, form, { headers: getAuthHeaders() })
        const body = res?.data
        if (body && Number(body.code) === 200) { ElMessage.success('视频添加成功'); video.value.videoTitle = ''; video.value.file = null }
        else { ElMessage.error(body?.message || '视频添加失败') }
      } catch (e) { console.error(e); ElMessage.error('视频添加失败，请稍后重试') } finally { submittingVideo.value = false }
    }

    const submitDoc = async () => {
      try {
        submittingDoc.value = true
        const form = new FormData(); form.append('courseId', doc.value.courseId); if (doc.value.docIndex != null) form.append('docIndex', String(doc.value.docIndex)); if (doc.value.docTitle) form.append('docTitle', doc.value.docTitle); if (doc.value.file) form.append('file', doc.value.file)
        const res = await axios.post(`${base}/course/document/insert`, form, { headers: getAuthHeaders() })
        const body = res?.data
        if (body && Number(body.code) === 200) { ElMessage.success('文档添加成功'); doc.value.docTitle = ''; doc.value.file = null }
        else { ElMessage.error(body?.message || '文档添加失败') }
      } catch (e) { console.error(e); ElMessage.error('文档添加失败，请稍后重试') } finally { submittingDoc.value = false }
    }

    // 加载当前课程的视频/文档列表
    const loadVideos = async () => {
      if (!routeCourseId) { videos.value = []; return }
      try {
        loadingVideos.value = true
        const res = await axios.get(`${base}/course/video/list`, { params: { courseId: routeCourseId }, headers: getAuthHeaders() })
        const body = res?.data
        videos.value = (body && Number(body.code) === 200 && Array.isArray(body.data)) ? body.data : []
      } catch { videos.value = [] } finally { loadingVideos.value = false }
    }

    const loadDocs = async () => {
      if (!routeCourseId) { documents.value = []; return }
      try {
        loadingDocs.value = true
        const res = await axios.get(`${base}/course/document/list`, { params: { courseId: routeCourseId }, headers: getAuthHeaders() })
        const body = res?.data
        documents.value = (body && Number(body.code) === 200 && Array.isArray(body.data)) ? body.data : []
      } catch { documents.value = [] } finally { loadingDocs.value = false }
    }

    // 保存当前选中视频（更新）
    const updateVideo = async () => {
      if (!video.value.videoId) { ElMessage.error('请先在列表中选择一个视频'); return }
      try {
        savingVideo.value = true
        if (video.value.file) {
          const form = new FormData()
          form.append('videoId', String(video.value.videoId))
          if (video.value.videoTitle) form.append('videoTitle', video.value.videoTitle)
          if (video.value.duration != null) form.append('duration', String(video.value.duration))
          form.append('file', video.value.file)
          const res = await axios.put(`${base}/course/video/update`, form, { headers: getAuthHeaders() })
          const body = res?.data
          if (body && Number(body.code) === 200) { ElMessage.success('视频已更新'); video.value.file = null; await loadVideos() } else { ElMessage.error(body?.message || '更新失败') }
        } else {
          const res = await axios.put(`${base}/course/video/update`, { videoId: video.value.videoId, videoTitle: video.value.videoTitle, duration: video.value.duration }, { headers: getAuthHeaders() })
          const body = res?.data
          if (body && Number(body.code) === 200) { ElMessage.success('视频已更新'); await loadVideos() } else { ElMessage.error(body?.message || '更新失败') }
        }
      } catch (e) { console.error(e); ElMessage.error('更新失败，请稍后重试') } finally { savingVideo.value = false }
    }

    // 保存当前选中文档（更新）
    const updateDoc = async () => {
      if (!doc.value.id) { ElMessage.error('请先在列表中选择一个文档'); return }
      try {
        savingDoc.value = true
        if (doc.value.file) {
          // 上传新文件获取 URL，再更新记录
          const form = new FormData()
          form.append('courseId', String(routeCourseId))
          if (doc.value.docIndex != null) form.append('docIndex', String(doc.value.docIndex))
          if (doc.value.docTitle) form.append('docTitle', doc.value.docTitle)
          form.append('file', doc.value.file)
          const ins = await axios.post(`${base}/course/document/insert`, form, { headers: getAuthHeaders() })
          const iv = ins?.data
          if (!(iv && Number(iv.code) === 200 && iv.data && iv.data.docUrl)) { ElMessage.error(iv?.message || '文档上传失败'); savingDoc.value = false; return }
          const payload = { id: doc.value.id, docIndex: doc.value.docIndex, docTitle: doc.value.docTitle, docUrl: iv.data.docUrl }
          const res = await axios.put(`${base}/course/document/update`, payload, { headers: getAuthHeaders() })
          const body = res?.data
          if (body && Number(body.code) === 200) { ElMessage.success('文档已更新'); await loadDocs() } else { ElMessage.error(body?.message || '更新失败') }
        } else {
          const payload = { id: doc.value.id, docIndex: doc.value.docIndex, docTitle: doc.value.docTitle, docUrl: doc.value.docUrl }
          const res = await axios.put(`${base}/course/document/update`, payload, { headers: getAuthHeaders() })
          const body = res?.data
          if (body && Number(body.code) === 200) { ElMessage.success('文档已更新'); await loadDocs() } else { ElMessage.error(body?.message || '更新失败') }
        }
      } catch (e) { console.error(e); ElMessage.error('更新失败，请稍后重试') } finally { savingDoc.value = false }
    }

    // 初始化加载
    ;(async () => { await Promise.all([loadVideos(), loadDocs()]) })()

    return { course, imagePreview, savingCourse, onCourseImageChange, saveCourse, video, doc, submittingVideo, submittingDoc, savingVideo, savingDoc, videos, documents, loadingVideos, loadingDocs, onVideoFileChange, onDocFileChange, onVideoRowClick, onDocRowClick, submitVideo, submitDoc, updateVideo, updateDoc, editVideoVisible, editVideo, openEditVideo, onEditVideoFileChange, submitEditVideo, editDocVisible, editDoc, openEditDoc, onEditDocFileChange, submitEditDoc }
  }
}
</script>

<style scoped>
.course-materials { padding: 12px; }
.section { background: #fff; padding: 16px; border-radius: 8px; margin: 12px 0; }
.avatar { width: 178px; height: 178px; display: block; margin-top: 8px; }
</style>


