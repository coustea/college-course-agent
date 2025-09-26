<template>
  <div class="course-materials">
    <h2>课程内容管理</h2>

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
  </div>
</template>

<script>
import { ref } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

export default {
  name: 'CourseMaterials',
  setup() {
    const routeCourseId = (() => { try { const m = window.location.pathname.match(/\/teacher\/courses\/(\d+)\/materials/); return m ? Number(m[1]) : null } catch { return null } })()
    const video = ref({ courseId: routeCourseId, videoIndex: null, videoTitle: '', file: null, duration: null })
    const doc = ref({ courseId: routeCourseId, docIndex: null, docTitle: '', file: null })
    const submittingVideo = ref(false)
    const submittingDoc = ref(false)
    const base = (import.meta?.env?.VITE_API_BASE_URL || 'http://localhost:9999/api')

    const onVideoFileChange = (e) => { video.value.file = e.target.files && e.target.files[0] }
    const onDocFileChange = (e) => { doc.value.file = e.target.files && e.target.files[0] }

    const submitVideo = async () => {
      try {
        submittingVideo.value = true
        const form = new FormData(); form.append('courseId', video.value.courseId); if (video.value.videoIndex != null) form.append('videoIndex', String(video.value.videoIndex)); if (video.value.videoTitle) form.append('videoTitle', video.value.videoTitle); if (video.value.file) form.append('file', video.value.file)
        const res = await axios.post(`${base}/course/video/insert`, form)
        const body = res?.data
        if (body && Number(body.code) === 200) { ElMessage.success('视频添加成功'); video.value.videoTitle = ''; video.value.file = null }
        else { ElMessage.error(body?.message || '视频添加失败') }
      } catch (e) { console.error(e); ElMessage.error('视频添加失败，请稍后重试') } finally { submittingVideo.value = false }
    }

    const submitDoc = async () => {
      try {
        submittingDoc.value = true
        const form = new FormData(); form.append('courseId', doc.value.courseId); if (doc.value.docIndex != null) form.append('docIndex', String(doc.value.docIndex)); if (doc.value.docTitle) form.append('docTitle', doc.value.docTitle); if (doc.value.file) form.append('file', doc.value.file)
        const res = await axios.post(`${base}/course/document/insert`, form)
        const body = res?.data
        if (body && Number(body.code) === 200) { ElMessage.success('文档添加成功'); doc.value.docTitle = ''; doc.value.file = null }
        else { ElMessage.error(body?.message || '文档添加失败') }
      } catch (e) { console.error(e); ElMessage.error('文档添加失败，请稍后重试') } finally { submittingDoc.value = false }
    }

    return { video, doc, submittingVideo, submittingDoc, onVideoFileChange, onDocFileChange, submitVideo, submitDoc }
  }
}
</script>

<style scoped>
.course-materials { padding: 12px; }
.section { background: #fff; padding: 16px; border-radius: 8px; margin: 12px 0; }
</style>


