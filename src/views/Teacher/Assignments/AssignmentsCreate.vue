<template>
  <div class="create-container">
    <div class="create-header">
      <div class="header-left">
        <el-button @click="$router.push('/teacher/assignments/list')" link class="back-link">
          <i class="fas fa-arrow-left"></i> 返回列表
        </el-button>
        <h2 class="header-title">下发作品定期检测</h2>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="submitForm" :loading="submitting" size="large">
          <i class="fas fa-paper-plane" style="margin-right: 6px;"></i>
          {{ submitting ? '提交中...' : '立即下发' }}
        </el-button>
      </div>
    </div>

    <div class="create-content">
      <el-card shadow="never" class="form-card" v-loading="submitting">
        <el-form
          :model="assignmentForm"
          :rules="rules"
          ref="formRef"
          label-position="top"
          class="assignment-form"
          size="large"
        >
          <el-row :gutter="24">
            <el-col :xs="24" :md="16">
              <el-form-item label="检测标题" prop="title">
                <el-input 
                  v-model="assignmentForm.title" 
                  placeholder="请输入检测任务标题（如：第一次阶段性检查）" 
                  maxlength="100"
                  show-word-limit
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="8">
              <el-form-item label="截止时间" prop="deadline">
                <el-date-picker
                  v-model="assignmentForm.deadline"
                  type="datetime"
                  placeholder="选择截止日期"
                  style="width: 100%"
                  value-format="YYYY-MM-DD HH:mm"
                  :default-time="new Date(2000, 1, 1, 23, 59, 59)"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="24">
              <el-form-item label="负责教师" prop="teacherId">
                <el-select
                  v-model="assignmentForm.teacherId"
                  placeholder="请选择教师"
                  filterable
                  style="width: 100%;"
                  :disabled="true"
                >
                  <el-option
                    v-for="t in teachers"
                    :key="t.id"
                    :label="t.name || ('教师#' + t.id)"
                    :value="t.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="检测要求与说明" prop="description">
            <el-input
              v-model="assignmentForm.description"
              type="textarea"
              :rows="6"
              placeholder="请输入本次检测的详细要求、评分标准及注意事项..."
              resize="none"
            />
          </el-form-item>

          <el-form-item label="参考附件（可选）">
            <div class="upload-area">
              <el-upload
                action="#"
                multiple
                drag
                :on-change="handleFileChange"
                :on-remove="handleFileRemove"
                :auto-upload="false"
                :file-list="fileList"
                :before-upload="beforeUpload"
                class="upload-demo"
              >
                <i class="el-icon-upload fas fa-cloud-upload-alt upload-icon"></i>
                <div class="el-upload__text">
                  将文件拖到此处，或 <em>点击上传</em>
                </div>
                <!-- tip slot was removed in Element Plus drag sometimes, putting tip below -->
              </el-upload>
              <div class="upload-tip">
                支持 PDF、Word、Excel 等格式，单个文件不超过 10MB
              </div>
            </div>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const formRef = ref()
const submitting = ref(false)

// === axios实例 ===
const API_BASE = (import.meta?.env?.VITE_API_BASE_URL || '/api')

const api = axios.create({
  baseURL: API_BASE,
  timeout: 20000
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token') || localStorage.getItem('userToken')
  if (token)
    config.headers = { ...(config.headers || {}), Authorization: `Bearer ${token}` }
  return config
})

// === 表单数据 ===
const assignmentForm = reactive({
  title: '',
  teacherId: '',
  deadline: '',
  description: '',
  attachments: []
})

// === 数据源 ===
const teachers = ref([])
const fileList = ref([])

// === 表单验证规则 ===
const rules = reactive({
  title: [
    { required: true, message: '请输入检查标题', trigger: 'blur' },
    { min: 3, max: 100, message: '标题长度在 3 到 100 个字符', trigger: 'blur' }
  ],
  teacherId: [{ required: true, message: '请选择教师', trigger: 'change' }],
  deadline: [{ required: true, message: '请选择截止日期', trigger: 'change' }],
  description: [{ required: true, message: '请输入检查要求', trigger: 'blur' }]
})

// === 文件上传验证 ===
const beforeUpload = (file) => {
  const allowedTypes = [
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  ]
  const maxSize = 10 * 1024 * 1024

  if (!allowedTypes.includes(file.type)) {
    ElMessage.error('文件格式不支持，请上传PDF、Word或Excel文件')
    return false
  }

  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过10MB')
    return false
  }

  return true
}

const handleFileChange = (file, files) => {
  fileList.value = files
  assignmentForm.attachments = files.map((f) => f.raw || f)
}

const handleFileRemove = (file, files) => {
  fileList.value = files
  assignmentForm.attachments = files.map((f) => f.raw || f)
}

// === 获取教师 ===
const fetchTeachers = async () => {
  try {
    const res = await api.get('/teacher/list/teachers')
    const body = res?.data
    const list =
      body && Number(body.code) === 200 && Array.isArray(body.data)
        ? body.data
        : []
    teachers.value = list

    let defaultTid = null
    try {
      const u = JSON.parse(localStorage.getItem('userInfo') || 'null')
      // 优先使用明确的 teacherId，避免使用 userId 导致外键约束失败
      if (u?.teacherId) defaultTid = Number(u.teacherId)
      else if (u?.id) defaultTid = Number(u.id)
    } catch (e) {}
    if (!defaultTid) {
      const tid = localStorage.getItem('teacherId')
      if (tid) defaultTid = Number(tid)
    }

    if (defaultTid && list.some((t) => Number(t.id) === Number(defaultTid)))
      assignmentForm.teacherId = Number(defaultTid)
    else if (list.length > 0) assignmentForm.teacherId = list[0].id
  } catch (e) {
    teachers.value = []
  }
}

// === 提交表单 ===
const submitForm = async () => {
  try {
    const valid = await formRef.value.validate()
    if (!valid) return

    submitting.value = true

    const formData = new FormData()
    const tid = assignmentForm.teacherId
    if (!tid) {
      ElMessage.error('请选择教师')
      submitting.value = false
      return
    }

    formData.append('teacherId', String(tid))
    formData.append('assignmentName', assignmentForm.title)
    if (assignmentForm.description) {
      formData.append('description', assignmentForm.description)
      // 后端参数名为 requirements，这里同时传递保持兼容
      formData.append('requirements', assignmentForm.description)
    }
    if (assignmentForm.deadline)
      formData.append('dueDate', assignmentForm.deadline)

    assignmentForm.attachments.forEach((file) => {
      formData.append('files', file)
    })

    const response = await api.post('/teacherAssignments', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    if (Number(response?.data?.code) === 200) {
      ElMessage.success('作品定期检测下发成功')
      router.push('/teacher/assignments/list')
    } else {
      ElMessage.error(response?.data?.message || '下发失败')
    }
  } catch (error) {
    const msg =
      error?.response?.data?.message ||
      error?.response?.data?.msg ||
      error?.message ||
      '服务器错误(500)'
    ElMessage.error(msg)
  } finally {
    submitting.value = false
  }
}

// === 初始化 ===
onMounted(async () => {
  await fetchTeachers()

  let currentTid = null
  try {
    const u = JSON.parse(localStorage.getItem('userInfo') || 'null')
    if (u?.id) currentTid = Number(u.id)
  } catch (e) {}
  if (!currentTid) {
    const tid = localStorage.getItem('teacherId')
    if (tid) currentTid = Number(tid)
  }

  if (currentTid) {
    assignmentForm.teacherId = currentTid
  }
})
</script>

<style scoped>
.create-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.create-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.header-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.back-link {
  padding: 0;
  height: auto;
  justify-content: flex-start;
  color: #606266;
  font-weight: normal;
}
.back-link:hover {
  color: #409eff;
}
.header-title {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
}

.create-content {
  margin-top: 10px;
}
.form-card {
  border-radius: 12px;
  border: 1px solid #ebeef5;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05) !important;
}

.assignment-form {
  padding: 10px;
}

.upload-area {
  width: 100%;
}
.upload-demo :deep(.el-upload-dragger) {
  width: 100%;
  border: 2px dashed #dcdfe6;
}
.upload-demo :deep(.el-upload-dragger:hover) {
  border-color: #409eff;
}
.upload-icon {
  font-size: 40px;
  color: #a8abb2;
  margin-bottom: 10px;
}
.upload-tip {
  font-size: 13px;
  color: #909399;
  margin-top: 8px;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .create-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  .header-right {
    width: 100%;
  }
  .header-right .el-button {
    width: 100%;
  }
  .create-container {
    padding: 10px;
  }
}
</style>
