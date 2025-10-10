<template>
  <div class="page-header">
    <div class="header-left">
      <h2>下发作品定期检测</h2>
    </div>
    <div class="header-right">
      <el-button @click="$router.push('/teacher/assignments/list')">
        <i class="fas fa-arrow-left"></i>
        返回列表
      </el-button>
      <el-button type="primary" @click="submitForm" :loading="submitting">
        {{ submitting ? '提交中...' : '下发检测' }}
      </el-button>
    </div>
  </div>

  <div class="assignment-create">
    <el-form
      :model="assignmentForm"
      :rules="rules"
      ref="formRef"
      label-width="120px"
      class="assignment-form"
    >
      <el-form-item label="检测标题" prop="title">
        <el-input v-model="assignmentForm.title" placeholder="请输入检测标题" />
      </el-form-item>

      <el-form-item label="教师" prop="teacherId">
        <el-select
          v-model="assignmentForm.teacherId"
          placeholder="请选择教师"
          filterable
          style="width: 320px;"
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

      <el-form-item label="截止日期" prop="deadline">
        <el-date-picker
          v-model="assignmentForm.deadline"
          type="datetime"
          placeholder="选择截止日期和时间"
          value-format="YYYY-MM-DD HH:mm"
        />
      </el-form-item>

      <el-form-item label="检测要求" prop="description">
        <el-input
          v-model="assignmentForm.description"
          type="textarea"
          :rows="5"
          placeholder="请输入检测详细要求和说明"
        />
      </el-form-item>

      <el-form-item label="参考附件">
        <el-upload
          action="#"
          multiple
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          :auto-upload="false"
          :file-list="fileList"
          :before-upload="beforeUpload"
        >
          <el-button type="primary">
            <i class="fas fa-upload"></i> 选择文件
          </el-button>
          <template #tip>
            <div class="el-upload__tip">
              支持PDF、Word、Excel等格式，单个文件不超过10MB
            </div>
          </template>
        </el-upload>
      </el-form-item>
    </el-form>
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
const API_BASE = (import.meta?.env?.VITE_API_BASE_URL || 'http://localhost:9999/api')

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
      if (u?.id) defaultTid = u.id
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
    if (assignmentForm.description)
      formData.append('description', assignmentForm.description)
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
.assignment-create {
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

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

.header-left {
  display: flex;
  flex-direction: column;
}

.page-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 8px 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.assignment-form {
  max-width: 800px;
}

:deep(.el-upload-list) {
  margin-top: 10px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .header-left {
    margin-bottom: 16px;
  }

  .header-right {
    width: 100%;
    justify-content: flex-end;
  }

  .assignment-form {
    max-width: 100%;
  }
}
</style>
