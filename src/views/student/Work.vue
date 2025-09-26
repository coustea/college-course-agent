<template>
  <div class="work-page">
    <div class="header">
      <h1 class="page-title">作品提交</h1>
    </div>

    <div class="content">
      <div class="submission-form">
        <h3 class="form-title">提交新作品</h3>
        <el-form :model="submissionForm" label-width="100px">
          <div class="form-row">
            <el-form-item label="作品标题">
              <el-input v-model="submissionForm.title" placeholder="请输入作品标题" />
            </el-form-item>
          </div>

          <div class="form-row">
            <el-form-item label="作品描述">
              <el-input
                v-model="submissionForm.description"
                type="textarea"
                :rows="4"
                placeholder="请描述您的作品"
              />
            </el-form-item>
          </div>

          <div class="form-row">
            <el-form-item label="上传文件">
              <el-upload
                class="upload-demo"
                action="#"
                :on-change="handleFileChange"
                :on-remove="handleFileRemove"
                :file-list="submissionForm.files"
                :auto-upload="false"
                multiple
                list-type="text"
              >
                <el-button size="small" type="primary">点击上传</el-button>
                <template #tip>
                  <div class="el-upload__tip">支持图片、文档、压缩包等格式，单个文件不超过10MB</div>
                </template>
              </el-upload>
            </el-form-item>
          </div>

        </el-form>
        <div class="form-bottom-bar">
          <div class="deadline-text">{{ deadlineText }}</div>
          <el-button type="primary" :loading="submitting" @click="submitWork">提交作品</el-button>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getWorkSidebarStatus, submitWork as submitWorkApi } from '@/services/workApi'

const submissionForm = ref({
  title: '',
  description: '',
  files: []
})

const deadlineText = ref('截止时间：2025-12-31 23:59')
const submitting = ref(false)

onMounted(async () => {
  try {
    const data = await getWorkSidebarStatus()
    if (data && data.deadline) {
      const text = String(data.deadline)
      deadlineText.value = text.startsWith('截止') ? text : `截止时间：${text}`
    }
    try {
      localStorage.setItem('work_sidebar_status', JSON.stringify(data || {}))
      window.dispatchEvent(new CustomEvent('work-sidebar-updated', { detail: data || {} }))
    } catch (e) { console.error(e) }
  } catch (e) { console.error(e) }
})


function handleFileChange(file, fileList) {
  submissionForm.value.files = fileList
}

function handleFileRemove(file, fileList) {
  submissionForm.value.files = fileList
}

function submitWork() {
  if (!submissionForm.value.title) {
    ElMessage.error('请输入作品标题')
    return
  }
  submitting.value = true
  ;(async () => {
    try {
      await submitWorkApi({
        title: submissionForm.value.title,
        description: submissionForm.value.description,
        files: submissionForm.value.files
      })
      ElMessage.success('作品提交成功！')
      submissionForm.value = { title: '', description: '', files: [] }
      try {
        const data = await getWorkSidebarStatus()
        localStorage.setItem('work_sidebar_status', JSON.stringify(data || {}))
        window.dispatchEvent(new CustomEvent('work-sidebar-updated', { detail: data || {} }))
      } catch (e) { console.error(e) }
    } catch (e) {
      ElMessage.error('提交失败，请稍后重试')
    } finally {
      submitting.value = false
    }
  })()
}
</script>

<style scoped>
.work-page { width: 100%; }
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}
.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #2c3e50;
}

.content {
  width: 70%;
}

.submission-form {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 22px;
  margin-bottom: 16px;
}
.form-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 14px 0;
  padding-bottom: 10px;
  border-bottom: 2px solid #f1f5f9;
}
.form-row { margin-bottom: 14px; }

.form-bottom-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-top: 1px solid #f1f5f9;
  margin-top: 10px;
  padding-top: 12px;
}
.deadline-text {
  color: #6b7280;
  font-size: 13px;
}

</style>
