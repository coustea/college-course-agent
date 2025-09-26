<template>
  <div class="course-edit">
    <h2>编辑课程 #{{ courseId }}</h2>

    <div class="section">
      <h3>更新课程信息</h3>
      <el-form :model="course" label-width="120px">
        <el-form-item label="课程名称"><el-input v-model="course.courseName" placeholder="课程名称（更新）" /></el-form-item>
        <el-form-item label="课程描述"><el-input v-model="course.description" type="textarea" :rows="3" placeholder="请输入课程描述" /></el-form-item>
        <el-form-item label="封面/资源URL"><el-input v-model="course.resourceUrl" placeholder="/uploads/... 或 http(s)://..." /></el-form-item>
        <el-form-item label="更换封面">
          <input type="file" accept="image/*" @change="onCourseImageChange" />
          <img v-if="imagePreview" :src="imagePreview" alt="预览" class="avatar" />
        </el-form-item>
        <el-form-item label="排序权重(vindex)"><el-input v-model.number="course.vindex" placeholder="例如 1/2/3" /></el-form-item>
        <el-form-item><el-button type="primary" :loading="savingCourse" @click="updateCourse">保存课程</el-button></el-form-item>
      </el-form>
    </div>

    <div class="section">
      <h3>更新视频</h3>
      <el-form :model="video" label-width="120px">
        <el-form-item label="视频ID(videoId)"><el-input v-model.number="video.videoId" placeholder="点击下方列表自动带入" /></el-form-item>
        <el-form-item label="视频标题"><el-input v-model="video.videoTitle" placeholder="第1讲 绪论（更新）" /></el-form-item>
        <el-form-item label="视频文件"><input type="file" accept=".mp4,.mov,.webm" @change="onEditVideoFileChange" /></el-form-item>
        <el-form-item label="时长(秒)"><el-input v-model.number="video.duration" placeholder="例如 1810" /></el-form-item>
        <el-form-item><el-button type="primary" :loading="savingVideo" @click="updateVideo">保存视频</el-button></el-form-item>
      </el-form>

      <el-divider>当前课程视频</el-divider>
      <el-table :data="videos" style="width:100%" v-loading="loadingVideos" size="small" @row-click="onVideoRowClick">
        <el-table-column prop="videoId" label="ID" width="80" />
        <el-table-column prop="videoIndex" label="序号" width="80" />
        <el-table-column prop="videoTitle" label="标题" min-width="220" />
        <el-table-column label="地址" min-width="260"><template #default="scope"><span :title="scope.row.videoUrl">{{ scope.row.videoUrl }}</span></template></el-table-column>
        <el-table-column prop="duration" label="时长(秒)" width="100" />
      </el-table>
    </div>

    <div class="section">
      <h3>更新文档</h3>
      <el-form :model="doc" label-width="120px">
        <el-form-item label="文档ID(id)"><el-input v-model.number="doc.id" placeholder="选择列表自动带入" /></el-form-item>
        <el-form-item label="文档序号(docIndex)"><el-input v-model.number="doc.docIndex" placeholder="例如 1" /></el-form-item>
        <el-form-item label="文档标题"><el-input v-model="doc.docTitle" placeholder="讲义1：绪论" /></el-form-item>
        <el-form-item label="文档文件"><input type="file" accept=".pdf,.doc,.docx,.ppt,.pptx" @change="onEditDocFileChange" /></el-form-item>
        <el-form-item><el-button type="primary" :loading="savingDoc" @click="updateDoc">保存文档</el-button></el-form-item>
      </el-form>

      <el-divider>当前课程文档</el-divider>
      <el-table :data="documents" style="width:100%" v-loading="loadingDocs" size="small">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="docIndex" label="序号" width="80" />
        <el-table-column prop="title" label="标题" min-width="220"><template #default="scope">{{ scope.row.title || scope.row.documentTitle }}</template></el-table-column>
        <el-table-column label="地址" min-width="260"><template #default="scope"><span :title="scope.row.fileUrl">{{ scope.row.fileUrl }}</span></template></el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'

export default {
  name: 'CourseEdit',
  setup() {
    const route = useRoute()
    const courseId = Number(route.params.id)
    const token = ref(localStorage.getItem('token') || localStorage.getItem('userToken') || '')
    const base = (import.meta?.env?.VITE_API_BASE_URL || 'http://localhost:9999/api')

    const course = ref({ courseId, courseName: '', description: '', resourceUrl: '', vindex: null })
    const imageFile = ref(null)
    const imagePreview = ref('')

    const video = ref({ videoId: null, videoTitle: '', file: null, duration: null })
    const doc = ref({ id: null, docIndex: null, docTitle: '', docUrl: '', file: null })

    const videos = ref([])
    const documents = ref([])
    const loadingVideos = ref(false)
    const loadingDocs = ref(false)

    const savingCourse = ref(false)
    const savingVideo = ref(false)
    const savingDoc = ref(false)

    const loadVideos = async () => { try { loadingVideos.value = true; const res = await axios.get(`${base}/course/video/list`, { params: { courseId, _t: Date.now() } }); const body = res?.data; videos.value = (body && Number(body.code) === 200 && Array.isArray(body.data)) ? body.data : []; if (Array.isArray(videos.value) && videos.value.length > 0 && (video.value.videoId == null || video.value.videoId === '')) { const v0 = videos.value[0]; video.value.videoId = v0.videoId || v0.id; video.value.videoTitle = v0.videoTitle || v0.title || ''; video.value.duration = v0.duration || null } } catch { videos.value = [] } finally { loadingVideos.value = false } }
    const onVideoRowClick = (row) => { if (!row) return; video.value.videoId = row.videoId || row.id; video.value.videoTitle = row.videoTitle || row.title || ''; video.value.duration = row.duration || null }
    const loadDocs = async () => { try { loadingDocs.value = true; const res = await axios.get(`${base}/course/document/list`, { params: { courseId } }); const body = res?.data; documents.value = (body && Number(body.code) === 200 && Array.isArray(body.data)) ? body.data : [] } catch { documents.value = [] } finally { loadingDocs.value = false } }

    const onCourseImageChange = (e) => { const file = e?.target?.files?.[0]; if (!file) return; imageFile.value = file; const reader = new FileReader(); reader.onload = () => { imagePreview.value = reader.result }; reader.readAsDataURL(file) }

    const updateCourse = async () => { try { savingCourse.value = true; if (imageFile.value) { const form = new FormData(); const payload = { courseId: course.value.courseId, courseName: course.value.courseName, description: course.value.description, vindex: course.value.vindex }; form.append('course', JSON.stringify(payload)); form.append('image', imageFile.value); const res = await axios.put(`${base}/course/update`, form, { headers: { Authorization: `Bearer ${token.value}` } }); const body = res?.data; if (body && Number(body.code) === 200) { ElMessage.success('课程信息已更新'); imageFile.value = null } else { ElMessage.error(body?.message || '更新失败') } } else { const res = await axios.put(`${base}/course/update`, course.value, { headers: { Authorization: `Bearer ${token.value}` } }); const body = res?.data; if (body && Number(body.code) === 200) { ElMessage.success('课程信息已更新') } else { ElMessage.error(body?.message || '更新失败') } } } catch (e) { console.error(e); ElMessage.error('更新失败，请稍后重试') } finally { savingCourse.value = false } }

    const onEditVideoFileChange = (e) => { video.value.file = e.target.files && e.target.files[0] }
    const updateVideo = async () => { if (!video.value.videoId) { ElMessage.error('请填写视频ID'); return } try { savingVideo.value = true; if (video.value.file) { const form = new FormData(); form.append('videoId', String(video.value.videoId)); if (video.value.videoTitle) form.append('videoTitle', video.value.videoTitle); if (video.value.duration != null) form.append('duration', String(video.value.duration)); form.append('file', video.value.file); const res = await axios.put(`${base}/course/video/update`, form); const body = res?.data; if (body && Number(body.code) === 200) { ElMessage.success('视频已更新'); video.value.file = null; loadVideos() } else { ElMessage.error(body?.message || '更新失败') } } else { const res = await axios.put(`${base}/course/video/update`, { videoId: video.value.videoId, videoTitle: video.value.videoTitle, duration: video.value.duration }); const body = res?.data; if (body && Number(body.code) === 200) { ElMessage.success('视频已更新'); loadVideos() } else { ElMessage.error(body?.message || '更新失败') } } } catch (e) { console.error(e); ElMessage.error('更新失败，请稍后重试') } finally { savingVideo.value = false } }

    const onEditDocFileChange = (e) => { doc.value.file = e.target.files && e.target.files[0] }
    const updateDoc = async () => { try { savingDoc.value = true; if (doc.value.file) { const form = new FormData(); form.append('courseId', String(courseId)); if (doc.value.docIndex != null) form.append('docIndex', String(doc.value.docIndex)); if (doc.value.docTitle) form.append('docTitle', doc.value.docTitle); form.append('file', doc.value.file); const ins = await axios.post(`${base}/course/document/insert`, form); const iv = ins?.data; if (!(iv && Number(iv.code) === 200 && iv.data && iv.data.docUrl)) { ElMessage.error(iv?.message || '文档上传失败'); savingDoc.value = false; return } const payload = { id: doc.value.id, docIndex: doc.value.docIndex, docTitle: doc.value.docTitle, docUrl: iv.data.docUrl }; const res = await axios.put(`${base}/course/document/update`, payload); const body = res?.data; if (body && Number(body.code) === 200) { ElMessage.success('文档已更新'); loadDocs() } else { ElMessage.error(body?.message || '更新失败') } } else { const payload = { id: doc.value.id, docIndex: doc.value.docIndex, docTitle: doc.value.docTitle, docUrl: doc.value.docUrl }; const res = await axios.put(`${base}/course/document/update`, payload); const body = res?.data; if (body && Number(body.code) === 200) { ElMessage.success('文档已更新'); loadDocs() } else { ElMessage.error(body?.message || '更新失败') } } } catch (e) { console.error(e); ElMessage.error('更新失败，请稍后重试') } finally { savingDoc.value = false } }

    onMounted(() => { loadVideos(); loadDocs() })

    return { courseId, course, imagePreview, onCourseImageChange, video, doc, videos, documents, loadingVideos, loadingDocs, savingCourse, savingVideo, savingDoc, updateCourse, updateVideo, updateDoc, onEditVideoFileChange, onEditDocFileChange }
  }
}
</script>

<style scoped>
.course-edit { padding: 12px; }
.section { background: #fff; padding: 16px; border-radius: 8px; margin: 12px 0; }
.avatar { width: 178px; height: 178px; display: block; }
</style>


