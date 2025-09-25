<template>
  <div class="course-create">
    <div class="page-header">
      <h2>创建新课程</h2>
      <el-button @click="$router.go(-1)">返回</el-button>
    </div>

    <div class="form-container">
      <el-form :model="form" label-width="120px" :rules="rules" ref="formRef">
        <el-form-item label="课程标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入课程标题"/>
        </el-form-item>

        <el-form-item label="课程分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择课程分类" style="width: 100%;">
            <el-option
              v-for="category in categories"
              :key="category"
              :label="category"
              :value="category"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="课程描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请输入课程描述"
          />
        </el-form-item>

        <el-form-item label="课程封面" prop="image">
          <el-upload
            class="avatar-uploader"
            action="/api/upload"
            :show-file-list="false"
            :before-upload="beforeAvatarUpload"
            :on-success="handleAvatarSuccess"
            :headers="{ Authorization: `Bearer ${token}` }"
          >
            <img v-if="form.image" :src="form.image" class="avatar" alt="课程封面"/>
            <el-icon v-else class="avatar-uploader-icon">
              <Plus/>
            </el-icon>
          </el-upload>
          <div class="upload-tip">点击上传课程封面，支持 JPG/PNG 格式，大小不超过 2MB</div>
        </el-form-item>

        <!-- 新增课程文件上传区域 -->
        <el-form-item label="课程文件" prop="files">
          <el-upload
            class="course-files-uploader"
            action="/api/upload"
            :headers="{ Authorization: `Bearer ${token}` }"
            :file-list="form.files"
            :before-upload="beforeFileUpload"
            :on-success="handleFileSuccess"
            :on-remove="handleFileRemove"
            :on-error="handleFileError"
            :on-progress="handleFileProgress"
            multiple
            list-type="text"
          >
            <el-button type="primary">
              <el-icon class="el-icon--left"><Upload /></el-icon>
              选择文件
            </el-button>
            <template #tip>
              <div class="upload-tip">支持 PDF、Word、PPT、Excel、ZIP 格式，单个文件大小不超过 50MB</div>
            </template>
          </el-upload>

          <!-- 上传进度显示 -->
          <div v-if="uploadProgress.visible" class="upload-progress">
            <div class="progress-info">
              <span>上传进度: {{ uploadProgress.currentFile }}</span>
              <span>{{ uploadProgress.percent }}%</span>
            </div>
            <el-progress
              :percentage="uploadProgress.percent"
              :status="uploadProgress.status"
              :stroke-width="8"
            />
          </div>
        </el-form-item>

        <el-form-item class="form-actions">
          <el-button type="primary" @click="submitForm">创建课程</el-button>
          <el-button @click="saveAsDraft">保存为草稿</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
import {ref, reactive, onMounted} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {Plus, Upload} from '@element-plus/icons-vue'
import axios from 'axios'

export default {
  name: 'CoursesCreate',
  components: {
    Plus,
    Upload
  },
  setup() {
    const router = useRouter()
    const formRef = ref()
    const token = ref('')

    const form = reactive({
      title: '',
      category: '',
      description: '',
      image: '',
      files: [], // 新增：存储课程文件信息
      status: 'draft'
    })

    // 上传进度状态
    const uploadProgress = reactive({
      visible: false,
      percent: 0,
      currentFile: '',
      status: '' // 'success', 'exception', 或空字符串
    })

    // 从后端获取分类数据
    const categories = ref([])

    const rules = {
      title: [
        {required: true, message: '请输入课程标题', trigger: 'blur'}
      ],
      category: [
        {required: true, message: '请选择课程分类', trigger: 'change'}
      ],
      description: [
        {required: true, message: '请输入课程描述', trigger: 'blur'}
      ],
      image: [
        {required: true, message: '请上传课程封面', trigger: 'change'}
      ]
      // 注意：课程文件不是必填项，所以不添加required验证
    }

    onMounted(() => {
      // 从本地存储获取token
      token.value = localStorage.getItem('token')

      // 如果没有token，提示用户登录
      if (!token.value) {
        ElMessage.error('用户未登录，请先登录')
        router.push('/login')
        return
      }

      // 加载真实分类数据
      loadCategories()
    })

    const loadCategories = async () => {
      try {
        const response = await axios.get('/api/categories', {
          headers: {
            'Authorization': `Bearer ${token.value}`
          }
        })
        if (response.data.success && response.data.data) {
          categories.value = response.data.data
        } else {
          ElMessage.error('加载分类数据失败: ' + (response.data.message || '未知错误'))
        }
      } catch (error) {
        console.error('加载分类数据失败:', error)
        ElMessage.error('加载分类数据失败，请稍后重试')
      }
    }

    const beforeAvatarUpload = (file) => {
      const isJPG = file.type === 'image/jpeg'
      const isPNG = file.type === 'image/png'
      const isLt2M = file.size / 1024 / 1024 < 2

      if (!isJPG && !isPNG) {
        ElMessage.error('封面图片只能是 JPG 或 PNG 格式!')
        return false
      }
      if (!isLt2M) {
        ElMessage.error('封面图片大小不能超过 2MB!')
        return false
      }

      // 验证token
      if (!token.value) {
        ElMessage.error('用户未登录，无法上传图片')
        return false
      }

      return true
    }

    const handleAvatarSuccess = (response, file) => {
      if (response && response.success) {
        form.image = response.data.url
        ElMessage.success('图片上传成功')
      } else {
        ElMessage.error(response?.message || '图片上传失败')
      }
    }

    // 新增：课程文件上传前的验证
    const beforeFileUpload = (file) => {
      // 验证文件类型
      const allowedTypes = [
        'application/pdf',
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'application/vnd.ms-powerpoint',
        'application/vnd.openxmlformats-officedocument.presentationml.presentation',
        'application/vnd.ms-excel',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'application/zip'
      ]

      const isAllowedType = allowedTypes.includes(file.type)
      const isLt50M = file.size / 1024 / 1024 < 50

      if (!isAllowedType) {
        ElMessage.error('文件格式不支持! 请上传PDF、Word、PPT、Excel或ZIP文件')
        return false
      }
      if (!isLt50M) {
        ElMessage.error('文件大小不能超过 50MB!')
        return false
      }

      // 验证token
      if (!token.value) {
        ElMessage.error('用户未登录，无法上传文件')
        return false
      }

      // 显示上传进度
      uploadProgress.visible = true
      uploadProgress.percent = 0
      uploadProgress.currentFile = file.name
      uploadProgress.status = ''

      return true
    }

    // 新增：课程文件上传成功处理
    const handleFileSuccess = (response, file) => {
      if (response && response.success) {
        // 将上传成功的文件信息添加到form.files中
        form.files.push({
          name: file.name,
          url: response.data.url,
          size: file.size,
          type: file.type
        })

        uploadProgress.percent = 100
        uploadProgress.status = 'success'

        setTimeout(() => {
          uploadProgress.visible = false
        }, 1500)

        ElMessage.success(`文件 "${file.name}" 上传成功`)
      } else {
        uploadProgress.status = 'exception'
        ElMessage.error(response?.message || `文件 "${file.name}" 上传失败`)
      }
    }

    // 新增：课程文件上传进度处理
    const handleFileProgress = (event, file) => {
      uploadProgress.percent = Math.round(event.percent)
    }

    // 新增：课程文件上传错误处理
    const handleFileError = (error, file) => {
      uploadProgress.status = 'exception'
      console.error('文件上传失败:', error)
      ElMessage.error(`文件 "${file.name}" 上传失败`)
    }

    // 新增：课程文件删除处理
    const handleFileRemove = (file, fileList) => {
      // 从form.files中移除对应的文件
      const index = form.files.findIndex(f => f.name === file.name)
      if (index !== -1) {
        form.files.splice(index, 1)
      }
      ElMessage.success(`文件 "${file.name}" 已移除`)
    }

    const submitForm = () => {
      formRef.value.validate(async (valid) => {
        if (valid) {
          form.status = 'published'
          await saveCourse()
        }
      })
    }

    const saveAsDraft = () => {
      form.status = 'draft'
      saveCourse()
    }

    const saveCourse = async () => {
      try {
        // 验证token
        if (!token.value) {
          ElMessage.error('用户未登录，请先登录')
          router.push('/login')
          return
        }

        // 配置axios请求头
        const config = {
          headers: {
            'Authorization': `Bearer ${token.value}`,
            'Content-Type': 'application/json'
          }
        }

        // 准备要发送的数据
        const courseData = {
          title: form.title,
          category: form.category,
          description: form.description,
          image: form.image,
          files: form.files, // 新增：包含课程文件信息
          status: form.status
        }

        const response = await axios.post(
          '/api/courses',
          courseData,
          config
        )

        if (response.data.success) {
          ElMessage.success(form.status === 'published' ? '课程创建成功' : '课程已保存为草稿')
          router.push('/teacher/courses/list')
        } else {
          ElMessage.error('保存失败: ' + (response.data.message || '未知错误'))
        }

      } catch (error) {
        console.error('保存课程失败:', error)
        if (error.response && error.response.status === 401) {
          ElMessage.error('登录已过期，请重新登录')
          router.push('/login')
        } else {
          ElMessage.error('保存失败，请稍后重试')
        }
      }
    }

    return {
      form,
      formRef,
      token,
      categories,
      rules,
      uploadProgress,
      beforeAvatarUpload,
      handleAvatarSuccess,
      beforeFileUpload,
      handleFileSuccess,
      handleFileProgress,
      handleFileError,
      handleFileRemove,
      submitForm,
      saveAsDraft
    }
  }
}
</script>

<style scoped>
.course-create {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 40px);
  max-width: 1300px;
  margin: 0 auto;
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

.page-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0;
}

.form-container {
  background: white;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.avatar-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 178px;
  height: 178px;
  transition: border-color 0.3s;
}

.avatar-uploader:hover {
  border-color: #409EFF;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 178px;
  text-align: center;
  line-height: 178px;
}

.avatar {
  width: 178px;
  height: 178px;
  display: block;
  object-fit: cover;
}

.upload-tip {
  color: #999;
  font-size: 12px;
  margin-top: 8px;
}

/* 新增：课程文件上传样式 */
.course-files-uploader {
  width: 100%;
}

.course-files-uploader :deep(.el-upload-list) {
  max-height: 200px;
  overflow-y: auto;
  margin-top: 10px;
}

.upload-progress {
  margin-top: 15px;
  padding: 10px;
  background-color: #f8f9fa;
  border-radius: 4px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
}

.form-actions {
  margin-top: 30px;
  display: flex;
  justify-content: center;
  gap: 20px;
}

.form-actions .el-button {
  padding: 12px 30px;
}

@media (max-width: 768px) {
  .course-create {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .form-container {
    padding: 16px;
  }

  .avatar-uploader,
  .avatar-uploader-icon,
  .avatar {
    width: 140px;
    height: 140px;
    line-height: 140px;
  }

  .form-actions {
    flex-direction: column;
    gap: 10px;
  }

  .form-actions .el-button {
    width: 100%;
  }
}
</style>
