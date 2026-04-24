<template>
  <div class="course-create-page">
    <!-- Page Header -->
    <div class="page-header">
      <div class="header-left">
        <h2>
          <el-icon><EditPen /></el-icon>
          创建新课程
        </h2>
        <p class="subtitle">填写课程信息并发布您的课程</p>
      </div>
      <div class="header-right">
        <el-button @click="$router.go(-1)" size="large">
          <el-icon><ArrowLeft /></el-icon>
          返回列表
        </el-button>
      </div>
    </div>

    <!-- Main Content -->
    <div class="main-content">
      <!-- Left: Form -->
      <div class="form-section">
        <el-form
          :model="form"
          :rules="rules"
          ref="formRef"
          label-width="100px"
          class="course-form"
        >
          <!-- Course Basic Info Card -->
          <div class="form-card">
            <div class="card-title">
              <el-icon><InfoFilled /></el-icon>
              基本信息
            </div>

            <!-- Course Name -->
            <el-form-item label="课程名称" prop="courseName">
              <el-input
                v-model="form.courseName"
                placeholder="例如：高等数学、数据结构与算法"
                maxlength="200"
                show-word-limit
                clearable
                size="large"
              >
                <template #prefix>
                  <el-icon><Memo /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <!-- Course Description -->
            <el-form-item label="课程描述" prop="description">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="5"
                placeholder="请详细描述课程内容、学习目标、适合人群等信息..."
                maxlength="1000"
                show-word-limit
              />
              <div class="form-tip">
                <el-icon><Warning /></el-icon>
                良好的课程描述能帮助学生更好地了解课程内容
              </div>
            </el-form-item>
          </div>

          <!-- Course Settings Card -->
          <div class="form-card">
            <div class="card-title">
              <el-icon><Setting /></el-icon>
              课程设置
            </div>

            <!-- Course Code -->
            <el-form-item label="课程代码">
              <el-input
                v-model="form.courseCode"
                placeholder="自动生成或手动输入，例如：CS202401"
                maxlength="20"
                clearable
                size="large"
              >
                <template #append>
                  <el-button @click="generateCourseCode">自动生成</el-button>
                </template>
              </el-input>
              <div class="form-tip">
                <el-icon><InfoFilled /></el-icon>
                课程代码用于唯一标识课程，可留空自动生成
              </div>
            </el-form-item>

            <!-- Credits -->
            <el-form-item label="学分">
              <el-input-number
                v-model="form.credits"
                :min="0"
                :max="10"
                :step="0.5"
                size="large"
                controls-position="right"
              />
              <span class="unit-label">学分</span>
            </el-form-item>

            <!-- Max Students -->
            <el-form-item label="最大人数">
              <el-input-number
                v-model="form.maxStudents"
                :min="1"
                :max="1000"
                :step="1"
                size="large"
                controls-position="right"
              />
              <span class="unit-label">人</span>
            </el-form-item>

            <!-- Semester -->
            <el-form-item label="学期">
              <el-input
                v-model="form.semester"
                placeholder="例如：2024春季、2024-2025第一学期"
                maxlength="20"
                clearable
                size="large"
              />
            </el-form-item>

            <!-- Date Range -->
            <el-form-item label="开课时间">
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开课日期"
                end-placeholder="结课日期"
                size="large"
                style="width: 100%"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                @change="onDateRangeChange"
              />
            </el-form-item>
          </div>
        </el-form>
      </div>

      <!-- Right: Sidebar -->
      <div class="sidebar-section">
        <!-- Cover Image Upload -->
        <div class="sidebar-card">
          <div class="card-title">
            <el-icon><Picture /></el-icon>
            课程封面
          </div>
          <el-upload
            class="cover-uploader"
            :show-file-list="false"
            :before-upload="beforeUpload"
            :on-change="onImageChange"
            :auto-upload="false"
            accept="image/*"
            drag
          >
            <img v-if="imagePreview" :src="imagePreview" class="cover-image" />
            <div v-else class="upload-placeholder">
              <el-icon class="upload-icon"><Plus /></el-icon>
              <div class="upload-text">
                <p class="upload-title">点击或拖拽上传</p>
                <p class="upload-hint">JPG、PNG、WebP</p>
                <p class="upload-hint">建议 800x600px</p>
              </div>
            </div>
          </el-upload>
          <div v-if="imageName" class="file-info">
            <el-icon><Document /></el-icon>
            <span class="file-name">{{ imageName }}</span>
            <el-button
              v-if="imagePreview"
              type="danger"
              link
              @click="removeImage"
              size="small"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>

        <!-- Course Tips -->
        <div class="sidebar-card tips-card">
          <div class="card-title">
            <el-icon><Bell /></el-icon>
            发布提示
          </div>
          <ul class="tips-list">
            <li>
              <el-icon color="#67c23a"><CircleCheck /></el-icon>
              <span>完整的课程信息能吸引更多学生</span>
            </li>
            <li>
              <el-icon color="#67c23a"><CircleCheck /></el-icon>
              <span>建议上传清晰的课程封面</span>
            </li>
            <li>
              <el-icon color="#e6a23c"><Warning /></el-icon>
              <span>发布后仍可编辑课程信息</span>
            </li>
            <li>
              <el-icon color="#409eff"><InfoFilled /></el-icon>
              <span>草稿不会对学生可见</span>
            </li>
          </ul>
        </div>

        <!-- Action Buttons -->
        <div class="action-buttons">
          <el-button
            type="primary"
            @click="submitForm"
            :loading="isSubmitting"
            size="large"
            style="width: 100%"
          >
            <el-icon v-if="!isSubmitting"><Upload /></el-icon>
            {{ isSubmitting ? '发布中...' : '发布课程' }}
          </el-button>
          <el-button
            @click="saveAsDraft"
            :loading="isSavingDraft"
            :disabled="isSubmitting"
            size="large"
            style="width: 100%"
          >
            <el-icon v-if="!isSavingDraft"><DocumentCopy /></el-icon>
            {{ isSavingDraft ? '保存中...' : '保存为草稿' }}
          </el-button>
          <el-button
            @click="$router.go(-1)"
            size="large"
            style="width: 100%"
          >
            <el-icon><Close /></el-icon>
            取消
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  Plus,
  Document,
  Delete,
  Upload,
  DocumentCopy,
  EditPen,
  InfoFilled,
  Setting,
  Picture,
  Bell,
  CircleCheck,
  Warning,
  Close,
  Memo
} from '@element-plus/icons-vue'

const router = useRouter()
const formRef = ref()
const token = ref('')
const teacherId = ref(localStorage.getItem('userId'))

// Form state - matches database schema
const form = reactive({
  courseName: '',
  courseCode: '',
  description: '',
  credits: 3,
  maxStudents: 100,
  semester: '',
  startDate: '',
  endDate: ''
})

const dateRange = ref([])
const imageFile = ref(null)
const imagePreview = ref('')
const imageName = ref('')
const isSubmitting = ref(false)
const isSavingDraft = ref(false)

// Validation rules
const rules = reactive({
  courseName: [
    { required: true, message: '请输入课程名称', trigger: 'blur' },
    { min: 2, max: 200, message: '课程名称长度应在 2-200 个字符之间', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入课程描述', trigger: 'blur' },
    { min: 10, max: 1000, message: '描述长度应在 10-1000 个字符之间', trigger: 'blur' }
  ]
})

onMounted(() => {
  token.value = localStorage.getItem('token')
  if (!token.value) {
    ElMessage.error('用户未登录，请先登录')
    router.push('/login')
    return
  }
  if (!teacherId.value) {
    ElMessage.error('未获取到教师ID，请重新登录')
    return
  }
})

// Generate course code
const generateCourseCode = () => {
  const ts = Date.now().toString().slice(-8)
  const rnd = String(Math.floor(Math.random() * 1000)).padStart(3, '0')
  form.courseCode = `CS${ts}${rnd}`.slice(0, 20)
  ElMessage.success('已生成课程代码: ' + form.courseCode)
}

// Handle date range change
const onDateRangeChange = (value) => {
  if (value && value.length === 2) {
    form.startDate = value[0]
    form.endDate = value[1]
  } else {
    form.startDate = ''
    form.endDate = ''
  }
}

// Image upload validation
const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const maxSize = 5 * 1024 * 1024 // 5MB

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }

  if (file.size > maxSize) {
    ElMessage.error('图片大小不能超过 5MB!')
    return false
  }

  return true
}

// Handle image change
const onImageChange = (file) => {
  if (!beforeUpload(file.raw)) {
    return
  }

  imageFile.value = file.raw
  imageName.value = file.name

  // Create preview
  const reader = new FileReader()
  reader.onload = (e) => {
    imagePreview.value = e.target.result
  }
  reader.readAsDataURL(file.raw)
}

// Remove image
const removeImage = () => {
  ElMessageBox.confirm(
    '确定要删除已上传的课程封面吗？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    imageFile.value = null
    imagePreview.value = ''
    imageName.value = ''
    ElMessage.success('已删除封面')
  }).catch(() => {})
}

// Submit form (publish course)
const submitForm = async () => {
  try {
    await formRef.value?.validate()

    await ElMessageBox.confirm(
      '确定要发布此课程吗？发布后学生将可以看到此课程。',
      '发布确认',
      {
        confirmButtonText: '发布',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await saveCourse('published')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.warning('请填写所有必填字段')
    }
  }
}

// Save as draft
const saveAsDraft = async () => {
  try {
    await formRef.value?.validate()
    await saveCourse('draft')
  } catch (error) {
    ElMessage.warning('请填写所有必填字段后再保存')
  }
}

// Save course to server
const saveCourse = async (status) => {
  const loading = status === 'published' ? isSubmitting : isSavingDraft
  loading.value = true

  try {
    if (!token.value) {
      ElMessage.error('用户未登录，请先登录')
      router.push('/login')
      return
    }

    const base = import.meta?.env?.VITE_API_BASE_URL || '/api'

    const formData = new FormData()

    // 构建课程对象，匹配后端 Course 实体
    const courseData = {
      courseCode: form.courseCode || generateCourseCodeString(),
      courseName: form.courseName,
      description: form.description,
      teacherId: Number(teacherId.value),
      credits: form.credits || 0,
      maxStudents: form.maxStudents || 100,
      semester: form.semester || '',
      startDate: form.startDate || null,
      endDate: form.endDate || null,
      vindex: 1
    }

    // 将课程对象作为 JSON 字符串传递（后端优先处理）
    formData.append('course', JSON.stringify(courseData))

    // 同时传递单独字段（后端兼容处理）
    formData.append('courseCode', courseData.courseCode)
    formData.append('courseName', courseData.courseName)
    formData.append('description', courseData.description)
    formData.append('teacherId', courseData.teacherId)

    // 上传图片
    if (imageFile.value) {
      formData.append('image', imageFile.value)
    }

    const response = await fetch(`${base}/api/course/insert`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token.value}`
      },
      body: formData
    })

    const body = await response.json()

    if (body && Number(body.code) === 200) {
      ElMessage.success(status === 'published' ? '课程发布成功！' : '已保存为草稿')
      setTimeout(() => {
        router.push('/teacher/courses/list')
      }, 1000)
    } else {
      ElMessage.error('保存失败: ' + (body?.message || '未知错误'))
    }
  } catch (error) {
    console.error('保存课程失败:', error)
    if (error.response && error.response.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      router.push('/login')
    } else {
      ElMessage.error('保存失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}

// 生成课程代码字符串（不更新表单）
const generateCourseCodeString = () => {
  const ts = Date.now().toString().slice(-8)
  const rnd = String(Math.floor(Math.random() * 1000)).padStart(3, '0')
  return `CS${ts}${rnd}`.slice(0, 20)
}
</script>

<style scoped>
/* Page Container */
.course-create-page {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 40px);
}

/* Page Header */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 16px 24px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header-left h2 {
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-left .subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.header-right {
  display: flex;
  gap: 12px;
}

/* Main Content Layout */
.main-content {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 24px;
  align-items: start;
}

/* Form Section */
.form-section {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: 24px;
}

.course-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: #2c3e50;
}

.course-form :deep(.el-input__inner),
.course-form :deep(.el-textarea__inner) {
  color: #606266;
}

.course-form :deep(.el-input__inner::placeholder),
.course-form :deep(.el-textarea__inner::placeholder) {
  color: #a8abb2;
}

/* Form Cards */
.form-card {
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid #e4e7ed;
}

.form-card:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #409eff;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Form Tips */
.form-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.unit-label {
  margin-left: 12px;
  color: #606266;
  font-size: 14px;
}

/* Sidebar Section */
.sidebar-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.sidebar-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.sidebar-card .card-title {
  margin-bottom: 16px;
  padding-bottom: 8px;
}

/* Cover Uploader */
.cover-uploader {
  width: 100%;
}

.cover-uploader :deep(.el-upload) {
  width: 100%;
}

.cover-uploader :deep(.el-upload-dragger) {
  width: 100%;
  height: 240px;
  border-radius: 8px;
  border: 2px dashed #d9d9d9;
  background: #fafafa;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.cover-uploader :deep(.el-upload-dragger:hover) {
  border-color: #409eff;
  background: #f0f7ff;
}

.cover-uploader :deep(.el-upload-dragger.is-dragover) {
  border-color: #409eff;
  background: #e6f3ff;
}

.upload-placeholder {
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.upload-icon {
  font-size: 48px;
  color: #409eff;
}

.upload-text .upload-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px 0;
}

.upload-text .upload-hint {
  font-size: 12px;
  color: #909399;
  margin: 0;
  line-height: 1.4;
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  max-height: 220px;
}

/* File Info */
.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  margin-top: 12px;
  font-size: 13px;
  color: #606266;
  border: 1px solid #e4e7ed;
}

.file-info .el-icon {
  color: #409eff;
  flex-shrink: 0;
}

.file-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Tips Card */
.tips-card {
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f3ff 100%);
}

.tips-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.tips-list li {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(64, 158, 255, 0.1);
  font-size: 14px;
  color: #303133;
  line-height: 1.5;
}

.tips-list li:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.tips-list li .el-icon {
  flex-shrink: 0;
  margin-top: 2px;
}

/* Action Buttons */
.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-buttons .el-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

/* Responsive Design */
@media (max-width: 1200px) {
  .main-content {
    grid-template-columns: 1fr;
  }

  .sidebar-section {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 24px;
  }

  .action-buttons {
    grid-column: 1 / -1;
    display: flex;
    flex-direction: row;
  }

  .action-buttons .el-button {
    flex: 1;
  }
}

@media (max-width: 768px) {
  .course-create-page {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
    padding: 16px;
  }

  .header-right {
    width: 100%;
    justify-content: flex-end;
  }

  .form-section,
  .sidebar-card {
    padding: 16px;
  }

  .course-form :deep(.el-form-item__label) {
    width: 100% !important;
    text-align: left;
    margin-bottom: 8px;
  }

  .sidebar-section {
    grid-template-columns: 1fr;
  }

  .action-buttons {
    flex-direction: column;
  }

  .action-buttons .el-button {
    width: 100%;
  }

  .cover-uploader :deep(.el-upload-dragger) {
    height: 200px;
  }

  .upload-icon {
    font-size: 40px;
  }
}

/* Loading State */
.course-form :deep(.el-button.is-loading .el-icon) {
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* Accessibility */
@media (prefers-reduced-motion: reduce) {
  .course-form :deep(.el-input__wrapper),
  .course-form :deep(.el-textarea__inner),
  .cover-uploader :deep(.el-upload-dragger) {
    transition: none;
  }

  @keyframes rotating {
    from, to {
      transform: rotate(0deg);
    }
  }
}
</style>
