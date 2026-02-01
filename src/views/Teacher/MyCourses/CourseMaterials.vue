<template>
  <div class="course-materials-page">
    <!-- 顶部导航与标题 -->
    <div class="page-header">
      <div class="header-left">
        <button class="back-btn" @click="goBack">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="icon"><path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" /></svg>
        </button>
        <div class="title-group">
          <h1 class="page-title">课程内容管理</h1>
          <span class="divider">/</span>
          <h2 class="course-name">{{ course.courseName || '加载中...' }}</h2>
        </div>
      </div>
      <div class="header-actions">
        <!-- 预留顶部操作区 -->
      </div>
    </div>

    <!-- 核心布局：调整为 6:4 比例 -->
    <div class="layout-container">

      <!-- 左侧栏 (60%)：信息编辑 & 上传操作 -->
      <aside class="left-panel">

        <!-- 1. 课程信息卡片 -->
        <div class="panel-card course-info-card">
          <div class="card-header">
            <h3 class="card-title">基本信息</h3>
            <button class="text-btn" @click="saveCourse" :disabled="savingCourse">
              {{ savingCourse ? '保存中...' : '保存' }}
            </button>
          </div>
          <div class="card-body">
            <div class="cover-uploader" @click="$refs.coverInput.click()">
              <img v-if="imagePreview || course.resourceUrl" :src="imagePreview || course.resourceUrl" class="cover-image" />
              <div v-else class="upload-placeholder">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="icon-lg"><path stroke-linecap="round" stroke-linejoin="round" d="M2.25 15.75l5.159-5.159a2.25 2.25 0 013.182 0l5.159 5.159m-1.5-1.5l1.409-1.409a2.25 2.25 0 013.182 0l2.909 2.909m-18 3.75h16.5a1.5 1.5 0 001.5-1.5V6a1.5 1.5 0 00-1.5-1.5H3.75A1.5 1.5 0 002.25 6v12a1.5 1.5 0 001.5 1.5zm10.5-11.25h.008v.008h-.008V8.25zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0z" /></svg>
                <span>点击更换封面</span>
              </div>
              <input ref="coverInput" type="file" accept="image/*" @change="onCourseImageChange" hidden />
            </div>

            <div class="form-item">
              <label>课程名称</label>
              <input v-model="course.courseName" class="input-field" placeholder="请输入课程名称" />
            </div>
            <div class="form-item">
              <label>简介描述</label>
              <textarea v-model="course.description" class="textarea-field" rows="4" placeholder="课程简要介绍..."></textarea>
            </div>
            <div class="form-item">
              <label>排序权重 (vindex)</label>
              <input v-model.number="course.vindex" type="number" class="input-field" placeholder="数字越大排序越前" />
            </div>
          </div>
        </div>

        <!-- 2. 上传资源卡片 -->
        <div class="panel-card add-resource-card">
          <!-- 类型切换 Tabs -->
          <div class="type-tabs">
            <div class="tab-item" :class="{ active: currentTab === 'video' }" @click="switchTab('video')">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="tab-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M5.25 5.653c0-.856.917-1.398 1.667-.986l11.54 6.348a1.125 1.125 0 010 1.971l-11.54 6.347a1.125 1.125 0 01-1.667-.985V5.653z" /></svg>
              上传视频
            </div>
            <div class="tab-item" :class="{ active: currentTab === 'document' }" @click="switchTab('document')">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="tab-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" /></svg>
              上传文档
            </div>
          </div>

          <div class="card-body">
            <!-- 视频上传表单 -->
            <div v-if="currentTab === 'video'" class="upload-form">
              <div class="form-row">
                <div class="form-item small">
                  <label>序号</label>
                  <input v-model.number="video.videoIndex" type="number" class="input-field" placeholder="自动" />
                </div>
                <div class="form-item grow">
                  <label>视频标题</label>
                  <input v-model="video.videoTitle" class="input-field" placeholder="输入标题" />
                </div>
              </div>
              <div class="form-item">
                <div class="drop-zone" :class="{ 'has-file': video.file }">
                  <input type="file" ref="videoFileInput" accept=".mp4,.mov,.webm" @change="onVideoFileChange" hidden />

                  <div v-if="!video.file" class="drop-content-empty">
                    <div class="icon-circle video">
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M12 16.5V9.75m0 0l3 3m-3-3l-3 3M6.75 19.5a4.5 4.5 0 01-1.41-8.775 5.25 5.25 0 0110.233-2.33 3 3 0 013.758 3.848A3.752 3.752 0 0118 19.5H6.75z" /></svg>
                    </div>
                    <div class="text-content">
                      <span class="sub-text">支持 MP4, MOV (最大 2GB)</span>
                    </div>
                    <button class="btn-outline" @click.prevent="triggerVideoUpload">选择视频文件</button>
                  </div>

                  <div v-else class="drop-content-file">
                    <div class="file-icon-wrapper">
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-8 h-8 text-primary"><path stroke-linecap="round" stroke-linejoin="round" d="M15.75 10.5l4.72-4.72a.75.75 0 011.28.53v11.38a.75.75 0 01-1.28.53l-4.72-4.72M4.5 18.75h9a2.25 2.25 0 002.25-2.25v-9a2.25 2.25 0 00-2.25-2.25h-9A2.25 2.25 0 002.25 7.5v9a2.25 2.25 0 002.25 2.25z" /></svg>
                    </div>
                    <div class="file-details">
                      <span class="filename">{{ video.file.name }}</span>
                      <span class="filesize">{{ formatFileSize(video.file.size) }} · {{ formatDuration(video.duration) }}</span>
                    </div>
                    <button class="clear-icon" @click.stop="clearVideoFile">×</button>
                  </div>
                </div>
              </div>
              <!-- 上传进度 -->
              <div v-if="uploadProgress > 0" class="upload-status">
                <div class="progress-bar">
                  <div class="progress-inner" :style="{ width: uploadProgress + '%' }"></div>
                </div>
                <div class="status-text">
                  <span>{{ uploadStatusText }}</span>
                  <span class="percent">{{ uploadProgress }}%</span>
                </div>
              </div>
              <button class="btn-primary full-width" :disabled="submittingVideo || !video.file" @click="submitVideo">
                {{ submittingVideo ? '正在上传...' : '确认添加视频' }}
              </button>
              <div v-if="video.file && video.file.size > 5 * 1024 * 1024" class="hint-tag">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" class="w-3 h-3"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.857-9.809a.75.75 0 00-1.214-.882l-3.483 4.79-1.88-1.88a.75.75 0 10-1.06 1.061l2.5 2.5a.75.75 0 001.137-.089l4-5.5z" clip-rule="evenodd" /></svg>
                大文件加速已启用
              </div>
            </div>

            <!-- 文档上传表单 -->
            <div v-if="currentTab === 'document'" class="upload-form">
              <div class="form-row">
                <div class="form-item small">
                  <label>序号</label>
                  <input v-model.number="doc.docIndex" type="number" class="input-field" placeholder="自动" />
                </div>
                <div class="form-item grow">
                  <label>文档标题</label>
                  <input v-model="doc.docTitle" class="input-field" placeholder="输入标题" />
                </div>
              </div>
              <div class="form-item">
                <div class="drop-zone" :class="{ 'has-file': doc.file }">
                  <input type="file" ref="docFileInput" accept=".pdf,.doc,.docx,.ppt,.pptx" @change="onDocFileChange" hidden />

                  <div v-if="!doc.file" class="drop-content-empty">
                    <div class="icon-circle doc">
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" /></svg>
                    </div>
                    <div class="text-content">
                      <span class="sub-text">支持 PDF, Word, PPT</span>
                    </div>
                    <!-- 显眼的上传按钮 -->
                    <button class="btn-outline" @click.prevent="triggerDocUpload">选择文档文件</button>
                  </div>

                  <div v-else class="drop-content-file">
                    <div class="file-icon-wrapper doc-icon">
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-8 h-8"><path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" /></svg>
                    </div>
                    <div class="file-details">
                      <span class="filename">{{ doc.file.name }}</span>
                      <span class="filesize">{{ formatFileSize(doc.file.size) }}</span>
                    </div>
                    <button class="clear-icon" @click.stop="clearDocFile">×</button>
                  </div>
                </div>
              </div>
              <button class="btn-primary full-width" :disabled="submittingDoc || !doc.file" @click="submitDoc">
                {{ submittingDoc ? '正在上传...' : '确认添加文档' }}
              </button>
            </div>
          </div>
        </div>
      </aside>

      <!-- 右侧栏 (40%)：资源列表展示 -->
      <main class="right-panel">
        <div class="panel-card list-container-card">
          <!-- 列表顶部导航 -->
          <div class="list-header">
            <div class="list-tabs">
              <button class="list-tab-btn" :class="{ active: currentTab === 'video' }" @click="switchTab('video')">
                已传视频 <span class="badge">{{ videos.length }}</span>
              </button>
              <button class="list-tab-btn" :class="{ active: currentTab === 'document' }" @click="switchTab('document')">
                已传文档 <span class="badge">{{ documents.length }}</span>
              </button>
            </div>
            <!-- 占位：搜索或过滤 -->
            <div class="list-tools">
              <!-- 可扩展搜索框 -->
            </div>
          </div>

          <!-- 列表内容区 -->
          <div class="list-content-area">

            <!-- 视频列表 -->
            <div v-if="currentTab === 'video'" class="resource-table-wrapper">
              <el-table :data="videos" style="width: 100%" v-loading="loadingVideos" empty-text="暂无已传视频，请从左侧上传" :row-style="{ height: '60px' }">
                <el-table-column prop="videoIndex" label="#" width="50" align="center">
                  <template #default="{ row }"><span class="index-badge video">{{ row.videoIndex }}</span></template>
                </el-table-column>
                <el-table-column label="视频信息" min-width="180">
                  <template #default="{ row }">
                    <div class="info-cell">
                      <div class="cell-text">
                        <div class="cell-title">{{ row.videoTitle }}</div>
                        <div class="cell-sub">ID: {{ row.videoId }}</div>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="duration" label="时长" width="80" align="center">
                  <template #default="{ row }">{{ formatDuration(row.duration) }}</template>
                </el-table-column>
                <el-table-column label="操作" width="120" align="center" fixed="right">
                  <template #default="{ row }">
                    <button class="action-btn edit" @click="openEditVideo(row)">编辑</button>
                    <button class="action-btn delete" @click="confirmDeleteVideo(row)">删除</button>
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <!-- 文档列表 -->
            <div v-if="currentTab === 'document'" class="resource-table-wrapper">
              <el-table :data="documents" style="width: 100%" v-loading="loadingDocs" empty-text="暂无已传文档，请从左侧上传" :row-style="{ height: '60px' }">
                <el-table-column prop="docIndex" label="#" width="50" align="center">
                  <template #default="{ row }"><span class="index-badge doc">{{ row.docIndex }}</span></template>
                </el-table-column>
                <el-table-column label="文档信息" min-width="180">
                  <template #default="{ row }">
                    <div class="info-cell">
                      <div class="cell-text">
                        <div class="cell-title">{{ row.docTitle }}</div>
                        <div class="cell-sub">ID: {{ row.documentId }}</div>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="uploadDate" label="时间" width="110" align="center">
                  <template #default="{ row }">
                    <span style="font-size: 12px; color: #94a3b8;">{{ formatDate(row.uploadDate).split(' ')[0] }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="120" align="center" fixed="right">
                  <template #default="{ row }">
                    <button class="action-btn edit" @click="openEditDoc(row)">编辑</button>
                    <button class="action-btn delete" @click="confirmDeleteDoc(row)">删除</button>
                  </template>
                </el-table-column>
              </el-table>
            </div>

          </div>
        </div>
      </main>
    </div>

    <!-- Modals (Edit/Delete) -->
    <!-- 编辑视频弹窗 -->
    <el-dialog v-model="editVideoVisible" title="编辑视频" width="480px" class="custom-modal" destroy-on-close>
      <div class="modal-form">
        <div class="form-item">
          <label>标题</label>
          <input v-model="editVideo.videoTitle" class="input-field" />
        </div>
        <div class="form-row">
          <div class="form-item half">
            <label>序号</label>
            <input v-model.number="editVideo.videoIndex" type="number" class="input-field" />
          </div>
          <div class="form-item half">
            <label>时长(秒)</label>
            <input v-model.number="editVideo.duration" type="number" class="input-field" />
          </div>
        </div>
        <div class="form-item">
          <label>替换文件 (可选)</label>
          <input type="file" accept=".mp4,.mov,.webm" @change="onEditVideoFileChange" class="file-input-simple" />
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <button class="btn-text" @click="editVideoVisible = false">取消</button>
          <button class="btn-primary" :disabled="savingVideo" @click="submitEditVideo">
            {{ savingVideo ? '保存中...' : '保存修改' }}
          </button>
        </span>
      </template>
    </el-dialog>

    <!-- 编辑文档弹窗 -->
    <el-dialog v-model="editDocVisible" title="编辑文档" width="480px" class="custom-modal" destroy-on-close>
      <div class="modal-form">
        <div class="form-item">
          <label>标题</label>
          <input v-model="editDoc.docTitle" class="input-field" />
        </div>
        <div class="form-item">
          <label>序号</label>
          <input v-model.number="editDoc.docIndex" type="number" class="input-field" />
        </div>
        <div class="form-item">
          <label>替换文件 (可选)</label>
          <input type="file" accept=".pdf,.doc,.docx,.ppt,.pptx" @change="onEditDocFileChange" class="file-input-simple" />
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <button class="btn-text" @click="editDocVisible = false">取消</button>
          <button class="btn-primary" :disabled="savingDoc" @click="submitEditDoc">
            {{ savingDoc ? '保存中...' : '保存修改' }}
          </button>
        </span>
      </template>
    </el-dialog>

    <!-- 删除确认弹窗 -->
    <el-dialog v-model="deleteConfirmVisible" title="确认删除" width="360px" center>
      <div style="text-align: center; color: #666;">确定要删除此资源吗？此操作无法撤销。</div>
      <template #footer>
        <span class="dialog-footer">
          <button class="btn-text" @click="deleteConfirmVisible = false">取消</button>
          <button class="btn-danger" :disabled="deleting" @click="executeDelete">
            {{ deleting ? '删除中...' : '确定删除' }}
          </button>
        </span>
      </template>
    </el-dialog>

  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getAuthHeaders } from '@/services/auth'

export default {
  name: 'CourseMaterials',
  setup() {
    const router = useRouter()
    const routeCourseId = (() => { try { const m = window.location.pathname.match(/\/teacher\/courses\/(\d+)\/materials/); return m ? Number(m[1]) : null } catch { return null } })()
    const base = (import.meta?.env?.VITE_API_BASE_URL || '/api')

    // UI Control
    const currentTab = ref('video') // Controls BOTH Add Form and List View
    const editVideoVisible = ref(false)
    const editDocVisible = ref(false)
    const deleteConfirmVisible = ref(false)
    const deleteTarget = ref(null)
    const deleteType = ref(null)
    const deleting = ref(false)

    // Data
    const course = ref({ courseId: routeCourseId, courseName: '', description: '', vindex: null, resourceUrl: '' })
    const videos = ref([])
    const documents = ref([])
    const loadingVideos = ref(false)
    const loadingDocs = ref(false)
    const savingCourse = ref(false)
    const imageFile = ref(null)
    const imagePreview = ref('')

    // Forms
    const video = ref({ courseId: routeCourseId, videoIndex: null, videoTitle: '', file: null, duration: null })
    const doc = ref({ courseId: routeCourseId, docIndex: null, docTitle: '', file: null })
    const editVideo = ref({ videoId: null, videoTitle: '', duration: null, videoIndex: null, file: null })
    const editDoc = ref({ documentId: null, docTitle: '', docIndex: null, file: null })

    // Input Refs for trigger click
    const videoFileInput = ref(null)
    const docFileInput = ref(null)

    // Upload status
    const submittingVideo = ref(false)
    const submittingDoc = ref(false)
    const savingVideo = ref(false)
    const savingDoc = ref(false)
    const uploadProgress = ref(0)
    const uploadStatusText = ref('')

    // --- Methods ---
    const goBack = () => router.push('/teacher/courses')

    // Switch tab helper
    const switchTab = (tab) => {
      currentTab.value = tab
    }

    // Trigger file input click
    const triggerVideoUpload = () => {
      if (videoFileInput.value) videoFileInput.value.click()
    }
    const triggerDocUpload = () => {
      if (docFileInput.value) docFileInput.value.click()
    }

    const loadData = async () => {
      if (!routeCourseId) return
      try {
        const [cRes, vRes, dRes] = await Promise.all([
          axios.get(`${base}/course/detail`, { params: { courseId: routeCourseId }, headers: getAuthHeaders() }),
          axios.get(`${base}/course/video/list`, { params: { courseId: routeCourseId }, headers: getAuthHeaders() }),
          axios.get(`${base}/course/document/list`, { params: { courseId: routeCourseId }, headers: getAuthHeaders() })
        ])

        if (cRes.data?.code === 200) course.value = { ...cRes.data.data, courseId: routeCourseId }
        if (vRes.data?.code === 200) videos.value = vRes.data.data || []
        if (dRes.data?.code === 200) documents.value = dRes.data.data || []
      } catch (e) { console.error(e) }
    }

    onMounted(loadData)

    // --- Upload Logic ---
    const onCourseImageChange = (e) => {
      const file = e.target.files?.[0]
      if (file) { imageFile.value = file; imagePreview.value = URL.createObjectURL(file) }
    }

    const saveCourse = async () => {
      savingCourse.value = true
      try {
        const payload = { courseId: routeCourseId, courseName: course.value.courseName, description: course.value.description, vindex: course.value.vindex }
        if (imageFile.value) {
          const form = new FormData()
          form.append('course', JSON.stringify(payload))
          form.append('image', imageFile.value)
          await axios.put(`${base}/course/update`, form, { headers: getAuthHeaders() })
        } else {
          await axios.put(`${base}/course/update`, payload, { headers: { ...getAuthHeaders(), 'Content-Type': 'application/json' }})
        }
        ElMessage.success('课程信息已更新')
      } catch (e) { ElMessage.error('更新失败') } finally { savingCourse.value = false }
    }

    const onVideoFileChange = (e) => {
      const f = e.target.files?.[0]
      video.value.file = f
      if (f) {
        const url = URL.createObjectURL(f)
        const el = document.createElement('video')
        el.preload = 'metadata'; el.src = url
        el.onloadedmetadata = () => { video.value.duration = Math.round(el.duration); URL.revokeObjectURL(url) }
      }
    }
    const clearVideoFile = () => { video.value.file = null; video.value.duration = null }

    const uploadVideoInChunks = async (file, params) => {
      const CHUNK_SIZE = 2 * 1024 * 1024
      const totalChunks = Math.ceil(file.size / CHUNK_SIZE)
      try {
        uploadStatusText.value = '初始化...'
        const init = await axios.post(`${base}/chunk/init`, null, { params: { fileName: file.name, fileSize: file.size, totalChunks }, headers: getAuthHeaders() })
        const uploadId = init.data.data.uploadId
        for (let i = 0; i < totalChunks; i++) {
          const chunk = file.slice(i * CHUNK_SIZE, Math.min((i + 1) * CHUNK_SIZE, file.size))
          const form = new FormData()
          form.append('uploadId', uploadId); form.append('chunkIndex', i); form.append('chunk', chunk)
          await axios.post(`${base}/chunk/upload`, form, { headers: getAuthHeaders() })
          uploadProgress.value = Math.round(((i + 1) / totalChunks) * 90)
          uploadStatusText.value = `上传中 ${i+1}/${totalChunks}`
        }
        uploadStatusText.value = '合并中...'
        await axios.post(`${base}/chunk/merge`, null, { params: { uploadId, ...params }, headers: getAuthHeaders() })
        uploadProgress.value = 100
      } catch (e) { throw e }
    }

    const submitVideo = async () => {
      if (!video.value.file) return
      submittingVideo.value = true
      uploadProgress.value = 0
      try {
        const payload = { courseId: routeCourseId, videoIndex: video.value.videoIndex, videoTitle: video.value.videoTitle, duration: video.value.duration }
        if (video.value.file.size > 5 * 1024 * 1024) {
          await uploadVideoInChunks(video.value.file, payload)
        } else {
          uploadStatusText.value = '上传中...'
          const form = new FormData()
          form.append('courseId', payload.courseId)
          if(payload.videoIndex) form.append('videoIndex', payload.videoIndex)
          if(payload.videoTitle) form.append('videoTitle', payload.videoTitle)
          if(payload.duration) form.append('duration', payload.duration)
          form.append('file', video.value.file)
          await axios.post(`${base}/course/video/insert`, form, {
            headers: getAuthHeaders(),
            onUploadProgress: p => uploadProgress.value = Math.round((p.loaded / p.total) * 100)
          })
        }
        ElMessage.success('上传成功')
        video.value = { courseId: routeCourseId, videoIndex: null, videoTitle: '', file: null, duration: null }
        uploadProgress.value = 0
        loadData()
      } catch (e) { ElMessage.error('上传失败'); console.error(e) }
      finally { submittingVideo.value = false }
    }

    const onDocFileChange = (e) => { doc.value.file = e.target.files?.[0] }
    const clearDocFile = () => { doc.value.file = null }
    const submitDoc = async () => {
      if (!doc.value.file) return
      submittingDoc.value = true
      try {
        const form = new FormData()
        form.append('courseId', routeCourseId)
        if(doc.value.docIndex) form.append('docIndex', doc.value.docIndex)
        if(doc.value.docTitle) form.append('docTitle', doc.value.docTitle)
        form.append('file', doc.value.file)
        await axios.post(`${base}/course/document/insert`, form, { headers: getAuthHeaders() })
        ElMessage.success('上传成功')
        doc.value = { courseId: routeCourseId, docIndex: null, docTitle: '', file: null }
        loadData()
      } catch (e) { ElMessage.error('上传失败') }
      finally { submittingDoc.value = false }
    }

    // --- Edit Actions ---
    const openEditVideo = (item) => { editVideo.value = { ...item, file: null }; editVideoVisible.value = true }
    const openEditDoc = (item) => { editDoc.value = { documentId: item.documentId, docTitle: item.docTitle, docIndex: item.docIndex, file: null }; editDocVisible.value = true }
    const onEditVideoFileChange = (e) => {
        editVideo.value.file = e.target.files[0]
        if(editVideo.value.file) {
            const url = URL.createObjectURL(editVideo.value.file); const el = document.createElement('video')
            el.preload = 'metadata'; el.src = url; el.onloadedmetadata = () => { editVideo.value.duration = Math.round(el.duration); URL.revokeObjectURL(url) }
        }
    }
    const onEditDocFileChange = (e) => { editDoc.value.file = e.target.files[0] }

    const submitEditVideo = async () => {
      savingVideo.value = true
      try {
        const form = new FormData(); form.append('videoId', editVideo.value.videoId)
        if(editVideo.value.videoTitle) form.append('videoTitle', editVideo.value.videoTitle)
        if(editVideo.value.videoIndex) form.append('videoIndex', editVideo.value.videoIndex)
        if(editVideo.value.duration) form.append('duration', editVideo.value.duration)
        if(editVideo.value.file) form.append('file', editVideo.value.file)
        await axios.put(`${base}/course/video/update`, form, { headers: getAuthHeaders() })
        ElMessage.success('更新成功'); editVideoVisible.value = false; loadData()
      } catch(e){ ElMessage.error('更新失败') } finally { savingVideo.value = false }
    }
    const submitEditDoc = async () => {
      savingDoc.value = true
      try {
        const form = new FormData(); form.append('documentId', editDoc.value.documentId)
        if(editDoc.value.docTitle) form.append('docTitle', editDoc.value.docTitle)
        if(editDoc.value.docIndex) form.append('docIndex', editDoc.value.docIndex)
        if(editDoc.value.file) form.append('file', editDoc.value.file)
        await axios.put(`${base}/course/document/update`, form, { headers: getAuthHeaders() })
        ElMessage.success('更新成功'); editDocVisible.value = false; loadData()
      } catch(e){ ElMessage.error('更新失败') } finally { savingDoc.value = false }
    }

    const confirmDeleteVideo = (item) => { deleteTarget.value = item; deleteType.value = 'video'; deleteConfirmVisible.value = true }
    const confirmDeleteDoc = (item) => { deleteTarget.value = item; deleteType.value = 'document'; deleteConfirmVisible.value = true }
    const executeDelete = async () => {
      deleting.value = true
      try {
        const url = deleteType.value === 'video' ? `${base}/course/video/delete` : `${base}/course/document/delete`
        const key = deleteType.value === 'video' ? 'videoId' : 'documentId'
        const id = deleteType.value === 'video' ? deleteTarget.value.videoId : deleteTarget.value.documentId
        await axios.delete(url, { params: { [key]: id }, headers: getAuthHeaders() })
        ElMessage.success('删除成功'); deleteConfirmVisible.value = false; loadData()
      } catch(e){ ElMessage.error('删除失败') } finally { deleting.value = false }
    }

    // Formatters
    const formatFileSize = (bytes) => {
      if(!bytes) return '0 B'; const k = 1024; const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    }
    const formatDuration = (s) => {
      if(!s) return '00:00'; const m = Math.floor(s/60); const sec = s%60
      return `${m}:${sec < 10 ? '0'+sec : sec}`
    }
    const formatDate = (r,c,val) => val ? new Date(val).toLocaleDateString() : '-'

    return {
      course, currentTab, videos, documents,
      video, doc, editVideo, editDoc,
      loadingVideos, loadingDocs, savingCourse, savingVideo, savingDoc, submittingVideo, submittingDoc,
      imagePreview, uploadProgress, uploadStatusText,
      editVideoVisible, editDocVisible, deleteConfirmVisible, deleting,
      goBack, switchTab, onCourseImageChange, saveCourse,
      onVideoFileChange, clearVideoFile, submitVideo,
      onDocFileChange, clearDocFile, submitDoc,
      openEditVideo, openEditDoc, onEditVideoFileChange, onEditDocFileChange, submitEditVideo, submitEditDoc,
      confirmDeleteVideo, confirmDeleteDoc, executeDelete,
      formatFileSize, formatDuration, formatDate,
      videoFileInput, docFileInput, triggerVideoUpload, triggerDocUpload
    }
  }
}
</script>

<style scoped>
/* ================= 全局变量 ================= */
:root {
  --primary: #4F46E5;
  --primary-hover: #4338ca;
  --text-main: #1e293b;
  --text-sub: #64748b;
  --bg-page: #f1f5f9;
  --bg-card: #ffffff;
  --border: #e2e8f0;
  --danger: #ef4444;
}

.course-materials-page {
  padding: 24px;
  background-color: var(--bg-page);
  min-height: 100vh;
  color: var(--text-main);
  box-sizing: border-box;
}

/* ================= 头部样式 ================= */
.page-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 24px;
}
.header-left { display: flex; align-items: center; gap: 16px; }
.back-btn {
  width: 36px; height: 36px; border-radius: 50%; border: 1px solid var(--border);
  background: white; color: var(--text-sub); display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.2s;
}
.back-btn:hover { color: var(--primary); border-color: var(--primary); box-shadow: 0 2px 5px rgba(0,0,0,0.05); }
.back-btn svg { width: 18px; height: 18px; }
.title-group { display: flex; align-items: baseline; gap: 12px; }
.page-title { font-size: 20px; font-weight: 700; color: var(--text-main); margin: 0; }
.divider { color: #cbd5e1; font-size: 18px; }
.course-name { font-size: 16px; font-weight: 500; color: var(--text-sub); margin: 0; }

/* ================= 布局容器 ================= */
.layout-container {
  display: flex; gap: 24px;
  max-width: 1400px; margin: 0 auto;
  align-items: flex-start;
}
.left-panel { width: 60%; flex-shrink: 0; display: flex; flex-direction: column; gap: 24px; }
.right-panel { flex: 1; min-width: 0; }

/* ================= 卡片通用 ================= */
.panel-card {
  background: var(--bg-card); border-radius: 12px;
  border: 1px solid var(--border);
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  overflow: hidden;
}
.card-header {
  padding: 16px 20px; border-bottom: 1px solid var(--border);
  display: flex; justify-content: space-between; align-items: center;
  background: #f8fafc;
}
.card-header.no-border { border-bottom: none; background: white; padding-bottom: 0; }
.card-title { font-size: 15px; font-weight: 600; margin: 0; color: var(--text-main); }
.card-body { padding: 20px; }

/* 文本按钮 */
.text-btn { border: none; background: none; color: var(--primary); font-weight: 500; cursor: pointer; font-size: 14px; }
.text-btn:hover { text-decoration: underline; }
.text-btn:disabled { color: var(--text-sub); cursor: not-allowed; text-decoration: none; }

/* ================= 左侧：课程信息 ================= */
.cover-uploader {
  width: 100%; aspect-ratio: 16/9; background: #f1f5f9;
  border-radius: 8px; border: 2px dashed #cbd5e1; margin-bottom: 20px;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; position: relative; overflow: hidden;
  transition: all 0.2s;
}
.cover-uploader:hover { border-color: var(--primary); background: #eef2ff; }
.cover-image { width: 100%; height: 100%; object-fit: cover; }
.upload-placeholder { display: flex; flex-direction: column; align-items: center; gap: 6px; color: var(--text-sub); font-size: 13px; }
.icon-lg { width: 32px; height: 32px; }

/* 表单元素 */
.form-item { margin-bottom: 16px; }
.form-item:last-child { margin-bottom: 0; }
.form-item label { display: block; font-size: 13px; font-weight: 500; color: var(--text-sub); margin-bottom: 6px; }
.input-field, .textarea-field {
  width: 100%; padding: 8px 12px; border: 1px solid var(--border);
  border-radius: 6px; font-size: 14px; background: #fff; box-sizing: border-box;
  transition: border-color 0.2s;
}
.input-field:focus, .textarea-field:focus { outline: none; border-color: var(--primary); }
.textarea-field { resize: vertical; min-height: 80px; }
.form-row { display: flex; gap: 12px; }
.form-item.small { width: 80px; flex-shrink: 0; }
.form-item.grow { flex: 1; }
.form-item.half { width: 50%; }

/* ================= 左侧：添加资源 ================= */
.type-tabs { display: flex; padding: 0 20px 10px; border-bottom: 1px solid var(--border); gap: 20px; }
.tab-item {
  display: flex; align-items: center; gap: 6px; padding-bottom: 10px;
  font-size: 14px; color: var(--text-sub); cursor: pointer; border-bottom: 2px solid transparent;
  transition: all 0.2s; margin-bottom: -11px;
}
.tab-item.active { color: var(--primary); border-bottom-color: var(--primary); font-weight: 500; }
.tab-icon { width: 18px; height: 18px; }

.drop-zone {
  border: 2px dashed #cbd5e1; border-radius: 8px; padding: 20px;
  background: #f8fafc; position: relative; transition: all 0.2s;
  min-height: 140px; display: flex; align-items: center; justify-content: center;
  flex-direction: column;
}
.drop-zone:hover { border-color: var(--primary); background: #f0f4ff; }
.drop-zone.has-file { border-style: solid; border-color: var(--primary); background: #eef2ff; }

/* 拖拽区域空状态 */
.drop-content-empty { display: flex; flex-direction: column; align-items: center; gap: 12px; width: 100%; }
.icon-circle {
  width: 48px; height: 48px; border-radius: 50%; margin: 0 auto;
  display: flex; align-items: center; justify-content: center;
}
.icon-circle.video { background: #ede9fe; color: #7c3aed; }
.icon-circle.doc { background: #dbeafe; color: #2563eb; }
.icon-circle svg { width: 24px; height: 24px; }
.text-content { text-align: center; }
.sub-text { font-size: 12px; color: #64748b; margin-top: 4px; display: block; }

/* 拖拽区域已有文件 */
.drop-content-file { display: flex; align-items: center; gap: 12px; width: 100%; padding: 0 10px; position: relative; }
.file-icon-wrapper { flex-shrink: 0; }
.file-icon-wrapper svg { color: #4F46E5; }
.file-icon-wrapper.doc-icon svg { color: #2563EB; }
.file-details { flex: 1; overflow: hidden; }
.filename { display: block; font-size: 14px; font-weight: 500; color: #1e293b; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.filesize { font-size: 12px; color: #64748b; }
.clear-icon {
  background: none; border: none; color: #94a3b8; font-size: 20px; cursor: pointer; padding: 4px;
  position: absolute; right: -10px; top: -10px;
}
.clear-icon:hover { color: #ef4444; }

/* 描边按钮 */
.btn-outline {
  background: white; border: 1px solid var(--primary); color: var(--primary);
  padding: 6px 16px; border-radius: 6px; font-size: 13px; font-weight: 500;
  cursor: pointer; transition: all 0.2s;
}
.btn-outline:hover { background: #eef2ff; }

/* 进度条 */
.upload-status { margin-bottom: 16px; background: #f1f5f9; padding: 8px 12px; border-radius: 6px; }
.progress-bar { height: 4px; background: #e2e8f0; border-radius: 2px; overflow: hidden; margin-bottom: 6px; }
.progress-inner { height: 100%; background: var(--primary); transition: width 0.2s; }
.status-text { display: flex; justify-content: space-between; font-size: 12px; color: var(--text-sub); }
.hint-tag { margin-top: 10px; display: flex; align-items: center; gap: 4px; font-size: 12px; color: #10b981; background: #ecfdf5; padding: 4px 8px; border-radius: 4px; width: fit-content; }

/* 主按钮 */
.btn-primary {
  background: var(--primary); color: white; border: none; border-radius: 6px;
  padding: 10px 20px; font-size: 14px; font-weight: 500; cursor: pointer; transition: all 0.2s;
}
.btn-primary:hover:not(:disabled) { background: var(--primary-hover); transform: translateY(-1px); }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-primary.full-width { width: 100%; }
.btn-danger { background: var(--danger); color: white; border: none; border-radius: 6px; padding: 8px 16px; cursor: pointer; }
.btn-danger:hover { background: #dc2626; }

/* ================= 右侧：列表 ================= */
.list-container-card { display: flex; flex-direction: column; min-height: 600px; }
.list-header {
  padding: 0 20px; border-bottom: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center; height: 56px;
}
.list-tabs { display: flex; gap: 24px; height: 100%; }
.list-tab-btn {
  background: none; border: none; height: 100%; font-size: 14px; color: var(--text-sub);
  cursor: pointer; border-bottom: 2px solid transparent; display: flex; align-items: center; gap: 8px;
}
.list-tab-btn.active { color: var(--primary); border-bottom-color: var(--primary); font-weight: 600; }
.badge { background: #f1f5f9; padding: 2px 8px; border-radius: 10px; font-size: 12px; color: var(--text-sub); }
.list-tab-btn.active .badge { background: #e0e7ff; color: var(--primary); }

.list-content-area { padding: 20px; }
.index-badge {
  display: inline-block; width: 24px; height: 24px; line-height: 24px; text-align: center;
  border-radius: 50%; font-size: 12px; font-weight: 600;
}
.index-badge.video { background: #f3e8ff; color: #7c3aed; }
.index-badge.doc { background: #dbeafe; color: #2563eb; }

/* 表格单元格内容 */
.info-cell { display: flex; align-items: center; gap: 12px; }
.cell-icon {
  width: 32px; height: 32px; border-radius: 6px; display: flex; align-items: center; justify-content: center;
}
.cell-icon.video { background: #f3e8ff; color: #7c3aed; }
.cell-icon.doc { background: #dbeafe; color: #2563eb; }
.cell-text { display: flex; flex-direction: column; }
.cell-title { font-size: 14px; font-weight: 500; color: var(--text-main); }
.cell-sub { font-size: 12px; color: #94a3b8; }

.action-btn { background: none; border: none; font-size: 13px; cursor: pointer; padding: 4px 8px; border-radius: 4px; }
.action-btn.edit { color: var(--primary); }
.action-btn.edit:hover { background: #eef2ff; }
.action-btn.delete { color: var(--danger); }
.action-btn.delete:hover { background: #fef2f2; }

/* 响应式 */
@media (max-width: 1024px) {
  .layout-container { flex-direction: column; }
  .left-panel { width: 100%; }
}
</style>