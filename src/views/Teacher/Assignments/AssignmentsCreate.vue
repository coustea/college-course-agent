<template>

  <div class="page-header">
    <div class="header-left">
      <h2>下发作品定期检查</h2>
    </div>
    <div class="header-right">
      <el-button @click="$router.push('/teacher/assignments/list')">
        <i class="fas fa-arrow-left"></i>
        返回列表
      </el-button>
      <el-button type="primary" @click="submitForm" :loading="submitting">
        {{ submitting ? '提交中...' : '下发检查' }}
      </el-button>
      <el-button @click="saveAsDraft">保存为草稿</el-button>
      <el-button @click="resetForm">重置</el-button>
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
      <el-form-item label="检查标题" prop="title">
        <el-input v-model="assignmentForm.title" placeholder="请输入检查标题"/>
      </el-form-item>


      <el-form-item label="截止日期" prop="deadline">
        <el-date-picker
          v-model="assignmentForm.deadline"
          type="datetime"
          placeholder="选择截止日期和时间"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
      </el-form-item>

      <el-form-item label="检查要求" prop="description">
        <el-input
          v-model="assignmentForm.description"
          type="textarea"
          :rows="5"
          placeholder="请输入检查详细要求和说明"
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
            <div class="el-upload__tip">支持PDF、Word、Excel等格式，单个文件不超过10MB</div>
          </template>
        </el-upload>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import {ref, reactive, onMounted} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import axios from 'axios'

const router = useRouter()
const formRef = ref()
const submitting = ref(false)

// 创建axios实例
const api = axios.create({
  baseURL: 'http://localhost:3000/api',
  timeout: 10000
})

// 表单数据
const assignmentForm = reactive({
  title: '',
  courseId: '',
  deadline: '',
  description: '',
  attachments: []
})

// 课程数据
const courses = ref([])
const fileList = ref([])

// 表单验证规则
const rules = reactive({
  title: [
    {required: true, message: '请输入检查标题', trigger: 'blur'},
    {min: 3, max: 100, message: '标题长度在 3 到 100 个字符', trigger: 'blur'}
  ],
  courseId: [
    {required: true, message: '请选择课程', trigger: 'change'}
  ],
  deadline: [
    {required: true, message: '请选择截止日期', trigger: 'change'}
  ],
  description: [
    {required: true, message: '请输入检查要求', trigger: 'blur'}
  ]
})

// 文件上传前的验证
const beforeUpload = (file) => {
  const allowedTypes = [
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  ]
  const maxSize = 10 * 1024 * 1024 // 10MB

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

// 文件变化处理
const handleFileChange = (file, files) => {
  fileList.value = files
  assignmentForm.attachments = files.map(f => f.raw || f)
}

const handleFileRemove = (file, files) => {
  fileList.value = files
  assignmentForm.attachments = files.map(f => f.raw || f)
}

// 获取课程列表
const fetchCourses = async () => {
  try {
    // 实际API调用
    // const response = await api.get('/courses')
    // courses.value = response.data

    // 模拟数据
    courses.value = [
      {id: 1, name: 'Web前端开发'},
      {id: 2, name: '数据库原理'},
      {id: 3, name: '操作系统'}
    ]
  } catch (error) {
    console.error('获取课程列表失败:', error)
    ElMessage.error('获取课程列表失败')
  }
}

// 提交表单
const submitForm = async () => {
  try {
    const valid = await formRef.value.validate()
    if (!valid) return

    submitting.value = true

    // 创建FormData对象，用于文件上传
    const formData = new FormData()
    formData.append('title', assignmentForm.title)
    formData.append('courseId', assignmentForm.courseId)
    formData.append('deadline', assignmentForm.deadline)
    formData.append('description', assignmentForm.description)
    formData.append('type', 'check') // 标记为检查类型

    // 添加附件
    assignmentForm.attachments.forEach((file, index) => {
      formData.append(`attachments`, file)
    })

    // 实际API调用
    const response = await api.post('/assignments', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    if (response.data.success) {
      ElMessage.success('作品检查下发成功')
      router.push('/teacher/assignments/list')
    } else {
      ElMessage.error(response.data.message || '下发失败')
    }
  } catch (error) {
    console.error('提交失败:', error)

    // 模拟成功响应（实际开发中删除）
    if (error.code === 'ERR_NETWORK') {
      ElMessage.success('作品检查下发成功（模拟）')
      router.push('/teacher/assignments/list')
    } else {
      ElMessage.error(error.response?.data?.message || '网络错误，请稍后重试')
    }
  } finally {
    submitting.value = false
  }
}

// 保存为草稿
const saveAsDraft = async () => {
  try {
    const formData = new FormData()
    formData.append('title', assignmentForm.title || '未命名检查')
    formData.append('courseId', assignmentForm.courseId)
    formData.append('deadline', assignmentForm.deadline)
    formData.append('description', assignmentForm.description)
    formData.append('type', 'check')
    formData.append('status', 'draft') // 草稿状态

    assignmentForm.attachments.forEach((file, index) => {
      formData.append(`attachments`, file)
    })

    // 实际API调用
    // await api.post('/assignments/draft', formData, {
    //   headers: {
    //     'Content-Type': 'multipart/form-data'
    //   }
    // })

    ElMessage.success('已保存为草稿')
  } catch (error) {
    console.error('保存草稿失败:', error)
    ElMessage.error('保存草稿失败')
  }
}

// 重置表单
const resetForm = () => {
  formRef.value.resetFields()
  fileList.value = []
  assignmentForm.attachments = []
}

// 初始化数据
onMounted(() => {
  fetchCourses()
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
