<template>
  <div class="course-create">
    <div class="page-header">
      <h2>创建新课程</h2>
      <el-button @click="$router.go(-1)">返回</el-button>
    </div>

    <el-form :model="form" label-width="120px" :rules="rules" ref="formRef">
      <el-form-item label="课程标题" prop="title">
        <el-input v-model="form.title" placeholder="请输入课程标题" />
      </el-form-item>

      <el-form-item label="课程描述" prop="description">
        <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入课程描述" />
      </el-form-item>

      <el-form-item label="课程封面">
        <div class="cover-uploader">
          <input type="file" accept="image/*" @change="onImageChange" />
          <div v-if="imageName" class="file-name">{{ imageName }}</div>
          <div v-if="imagePreview" class="image-preview">
            <img :src="imagePreview" alt="预览" class="avatar" />
          </div>
        </div>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="submitForm">创建课程</el-button>
        <el-button @click="saveAsDraft">保存为草稿</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

export default {
  name: 'CoursesCreate',
  setup() {
    const router = useRouter()
    const formRef = ref()
    const token = ref('')
    const userInfo = (() => { try { return JSON.parse(localStorage.getItem('currentUser') || '{}') } catch { return {} } })()
    const teacherId = ref(userInfo?.id || '')

    const form = reactive({ title: '', description: '' })
    const imageFile = ref(null)
    const imagePreview = ref('')
    const imageName = ref('')

    const rules = { title: [{ required: true, message: '请输入课程标题', trigger: 'blur' }], description: [{ required: true, message: '请输入课程描述', trigger: 'blur' }] }

    onMounted(() => {
      token.value = localStorage.getItem('token') || localStorage.getItem('userToken')
      if (!token.value) { ElMessage.error('用户未登录，请先登录'); router.push('/login'); return }
      if (!teacherId.value) { ElMessage.error('未获取到教师ID，请重新登录'); router.push('/login'); return }
    })

    const onImageChange = (e) => {
      const file = e?.target?.files?.[0]
      if (!file) return
      imageFile.value = file
      imageName.value = file.name || ''
      const reader = new FileReader()
      reader.onload = () => { imagePreview.value = reader.result }
      reader.readAsDataURL(file)
    }

    const submitForm = () => { formRef.value.validate(async (valid) => { if (valid) { await saveCourse('published') } }) }
    const saveAsDraft = () => { saveCourse('draft') }

    const genCourseCode = () => { const ts = Date.now().toString(); const rnd = String(Math.floor(Math.random() * 1000)).padStart(3, '0'); return (`CS${ts}${rnd}`).slice(0, 20) }

    const saveCourse = async (status) => {
      try {
        if (!token.value) { ElMessage.error('用户未登录，请先登录'); router.push('/login'); return }
        const base = import.meta?.env?.VITE_API_BASE_URL || 'http://localhost:9999/api'
        const formData = new FormData()
        formData.append('courseCode', genCourseCode())
        formData.append('courseName', form.title)
        formData.append('description', form.description)
        formData.append('teacherId', userInfo.id)
        if (imageFile.value) formData.append('image', imageFile.value)
        const response = await axios.post(`${base}/course/insert`, formData, { headers: { Authorization: `Bearer ${token.value}` } })
        const body = response?.data
        if (body && Number(body.code) === 200) { ElMessage.success(status === 'published' ? '课程创建成功' : '已保存为草稿'); router.push('/teacher/courses/list') }
        else { ElMessage.error('保存失败: ' + (body?.message || '未知错误')) }
      } catch (error) {
        console.error('保存课程失败:', error)
        if (error.response && error.response.status === 401) { ElMessage.error('登录已过期，请重新登录'); router.push('/login') }
        else { ElMessage.error('保存失败，请稍后重试') }
      }
    }

    return { form, formRef, token, rules, imagePreview, imageName, onImageChange, submitForm, saveAsDraft }
  }
}
</script>

<style scoped>
.course-create { padding: 20px; max-width: 1300px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px; }
.avatar { width: 178px; height: 178px; display: block; }
.cover-uploader { display: flex; flex-direction: column; gap: 10px; }
.file-name { font-size: 13px; color: #606266; }
.image-preview { display: flex; justify-content: center; align-items: center; background: #f5f7fa; padding: 10px; border-radius: 8px; }
</style>


