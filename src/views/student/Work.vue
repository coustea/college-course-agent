<template>
  <div class="work-page">
    <div class="header">
      <h1 class="page-title">作品提交</h1>
    </div>

    <div class="content">
      <div class="submission-form">
        <h3 class="form-title">提交新作品</h3>
        <div class="requirements-box" v-if="requirements">
          <div>作品要求：</div>
          <ul>
            <li v-if="requirements.titleRequired">需要填写作品标题</li>
            <li v-if="requirements.descriptionRequired">需要填写作品描述<span v-if="requirements.descriptionMaxLen">（不超过 {{ requirements.descriptionMaxLen }} 字）</span></li>
            <li v-if="requirements.maxFiles">最多上传 {{ requirements.maxFiles }} 个文件</li>
            <li v-if="requirements.maxFileSizeMB">单个文件不超过 {{ requirements.maxFileSizeMB }}MB</li>
            <li v-if="(requirements.allowedExtensions && requirements.allowedExtensions.length)
                       || (requirements.allowedMimeTypes && requirements.allowedMimeTypes.length)">
              允许的类型：
              <span v-if="requirements.allowedExtensions && requirements.allowedExtensions.length">{{ requirements.allowedExtensions.join(', ') }}</span>
              <span v-if="requirements.allowedMimeTypes && requirements.allowedMimeTypes.length">（{{ requirements.allowedMimeTypes.join(', ') }}）</span>
            </li>
            <li v-if="requirements.extraNotes">{{ requirements.extraNotes }}</li>
          </ul>
        </div>
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
                :before-upload="beforeUpload"
                :on-exceed="onExceed"
                :file-list="submissionForm.files"
                :auto-upload="false"
                :limit="requirements.maxFiles || 0"
                :accept="accept"
                multiple
                list-type="text"
              >
                <el-button size="small" type="primary">点击上传</el-button>
                <template #tip>
                  <div class="el-upload__tip">
                    支持类型：
                    <template v-if="accept">{{ accept }}</template>
                    <template v-else>不限</template>；
                    <template v-if="requirements.maxFileSizeMB">单个不超过 {{ requirements.maxFileSizeMB }}MB；</template>
                    <template v-if="requirements.maxFiles">最多 {{ requirements.maxFiles }} 个文件</template>
                  </div>
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
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getWorkSidebarStatus, submitWork as submitWorkApi } from '@/services/workApi'

const submissionForm = ref({
  title: '',
  description: '',
  files: []
})

const deadlineText = ref('截止时间：2025-12-31 23:59')
const submitting = ref(false)
function getDefaultRequirements() {
  return {
    titleRequired: true,
    descriptionRequired: true,
    descriptionMaxLen: 200,
    maxFiles: 3,
    maxFileSizeMB: 10,
    // 常见文档/压缩与图片、mp4 视频
    allowedExtensions: ['.pdf', '.doc', '.docx', '.ppt', '.pptx', '.zip'],
    allowedMimeTypes: ['image/*', 'video/mp4'],
    extraNotes: '请按要求提交，命名规范：班级-学号-姓名-作品名。'
  }
}

const requirements = ref(getDefaultRequirements())

const accept = computed(() => {
  const exts = Array.isArray(requirements.value.allowedExtensions) ? requirements.value.allowedExtensions : []
  const mimes = Array.isArray(requirements.value.allowedMimeTypes) ? requirements.value.allowedMimeTypes : []
  const list = [...exts, ...mimes].filter(Boolean)
  return list.length ? list.join(',') : ''
})

onMounted(async () => {
  try {
    const data = await getWorkSidebarStatus()
    if (data && data.deadline) {
      const text = String(data.deadline)
      deadlineText.value = text.startsWith('截止') ? text : `截止时间：${text}`
    }
    try { applyRequirements(data || {}) } catch (e) { console.error(e) }
    try {
      localStorage.setItem('work_sidebar_status', JSON.stringify(data || {}))
      window.dispatchEvent(new CustomEvent('work-sidebar-updated', { detail: data || {} }))
    } catch (e) { console.error(e) }
  } catch (e) { console.error(e) }
})


function handleFileChange(file, fileList) {
  // 过滤不满足要求的文件
  const passed = []
  for (const f of fileList) {
    if (validateSingleFile(f)) passed.push(f)
  }
  // 数量限制
  const limit = Number(requirements.value.maxFiles || 0)
  submissionForm.value.files = limit > 0 ? passed.slice(0, limit) : passed
}

function handleFileRemove(file, fileList) {
  submissionForm.value.files = fileList
}

function bytesPerMB() { return 1024 * 1024 }
function fileNameExt(name) {
  const s = String(name || '')
  const i = s.lastIndexOf('.')
  return i >= 0 ? s.slice(i).toLowerCase() : ''
}
function isTypeAllowed(file) {
  const exts = (requirements.value.allowedExtensions || []).map(s => String(s).toLowerCase())
  const mimes = (requirements.value.allowedMimeTypes || []).map(s => String(s).toLowerCase())
  if (exts.length === 0 && mimes.length === 0) return true
  const ext = fileNameExt(file.name)
  const type = String(file.type || '').toLowerCase()
  const okExt = ext && exts.includes(ext)
  const okMime = mimes.some(pattern => {
    if (!pattern) return false
    if (pattern.endsWith('/*')) {
      const prefix = pattern.replace('/*','')
      return type.startsWith(prefix)
    }
    return type === pattern
  })
  return okExt || okMime
}

function validateSingleFile(file) {
  // size
  const maxMB = Number(requirements.value.maxFileSizeMB || 0)
  if (maxMB > 0 && file.size > maxMB * bytesPerMB()) {
    ElMessage.error(`文件大小超限（最大 ${maxMB}MB）: ${file.name}`)
    return false
  }
  // type
  if (!isTypeAllowed(file)) {
    const allowTip = accept.value || '格式受限'
    ElMessage.error(`文件类型不被允许: ${file.name}（允许: ${allowTip}）`)
    return false
  }
  return true
}

function beforeUpload(file) {
  // element-plus before-upload: 返回 false/Promise.reject 可阻止加入
  return validateSingleFile(file)
}

function onExceed() {
  const limit = Number(requirements.value.maxFiles || 0)
  if (limit > 0) ElMessage.warning(`最多可选择 ${limit} 个文件`)
}

function applyRequirements(data) {
  const r = data?.requirements || data?.rules || data?.policy || {}
  const d = getDefaultRequirements()
  const has = (obj, key) => Object.prototype.hasOwnProperty.call(obj || {}, key)

  // 标题/描述是否必填
  const titleRequired = has(r, 'titleRequired') ? !!r.titleRequired : (has(r, 'requireTitle') ? !!r.requireTitle : d.titleRequired)
  const descriptionRequired = has(r, 'descriptionRequired') ? !!r.descriptionRequired : (has(r, 'requireDescription') ? !!r.requireDescription : d.descriptionRequired)

  // 描述最大长度
  let descriptionMaxLen = d.descriptionMaxLen
  if (has(r, 'descriptionMaxLen')) descriptionMaxLen = Number(r.descriptionMaxLen) || 0
  else if (has(r, 'descMaxLen')) descriptionMaxLen = Number(r.descMaxLen) || 0
  else if (has(r, 'maxDescriptionLength')) descriptionMaxLen = Number(r.maxDescriptionLength) || 0

  // 文件数量
  let maxFiles = d.maxFiles
  if (has(r, 'maxFiles')) maxFiles = Number(r.maxFiles) || 0
  else if (has(r, 'fileLimit')) maxFiles = Number(r.fileLimit) || 0

  // 单文件大小（MB）
  let maxFileSizeMB = d.maxFileSizeMB
  if (has(r, 'maxFileSizeMB')) maxFileSizeMB = Number(r.maxFileSizeMB) || 0
  else if (has(r, 'maxSizeMB')) maxFileSizeMB = Number(r.maxSizeMB) || 0
  else if (has(r, 'maxSizeBytes')) maxFileSizeMB = Math.ceil(Number(r.maxSizeBytes) / bytesPerMB()) || 0
  else if (has(r, 'maxSize')) maxFileSizeMB = Math.ceil(Number(r.maxSize) / bytesPerMB()) || 0

  // 类型限制（若后端明确提供空数组，则表示不限制，应覆盖默认）
  let allowedExtensions = d.allowedExtensions
  let allowedMimeTypes = d.allowedMimeTypes
  if (has(r, 'allowedExtensions') || has(r, 'extensions') || has(r, 'exts')) {
    const extRaw = r.allowedExtensions ?? r.extensions ?? r.exts
    const normExts = Array.isArray(extRaw) ? extRaw.map(s => String(s).trim().toLowerCase()) : []
    allowedExtensions = normExts.map(s => (s && s[0] !== '.' ? `.${s}` : s)).filter(Boolean)
  }
  if (has(r, 'allowedMimeTypes') || has(r, 'mimeTypes') || has(r, 'types') || has(r, 'allowedTypes')) {
    const mimeRaw = r.allowedMimeTypes ?? r.mimeTypes ?? r.types ?? r.allowedTypes
    allowedMimeTypes = Array.isArray(mimeRaw) ? mimeRaw.map(s => String(s).trim().toLowerCase()) : []
  }

  // 备注
  let extraNotes = d.extraNotes
  if (has(r, 'text') || has(r, 'notes') || has(r, 'note') || has(data || {}, 'requirementText')) {
    extraNotes = String(r.text ?? r.notes ?? r.note ?? data?.requirementText ?? '')
  }

  requirements.value = {
    titleRequired,
    descriptionRequired,
    descriptionMaxLen,
    maxFiles,
    maxFileSizeMB,
    allowedExtensions,
    allowedMimeTypes,
    extraNotes
  }
}

function submitWork() {
  // 标题/描述校验
  if (requirements.value.titleRequired && !submissionForm.value.title) {
    ElMessage.error('请填写作品标题')
    return
  }
  if (requirements.value.descriptionRequired && !submissionForm.value.description) {
    ElMessage.error('请填写作品描述')
    return
  }
  const maxLen = Number(requirements.value.descriptionMaxLen || 0)
  if (maxLen > 0 && submissionForm.value.description && submissionForm.value.description.length > maxLen) {
    ElMessage.error(`作品描述不能超过 ${maxLen} 个字符`)
    return
  }
  // 文件数量限制
  const limit = Number(requirements.value.maxFiles || 0)
  if (limit > 0 && submissionForm.value.files.length > limit) {
    ElMessage.error(`最多可提交 ${limit} 个文件`)
    return
  }
  // 文件大小/类型复核
  for (const f of submissionForm.value.files) {
    if (!validateSingleFile(f)) return
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

.requirements-box {
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  padding: 12px;
  margin: 10px 0 16px 0;
  color: #334155;
  font-size: 13px;
}
.requirements-box ul { margin: 6px 0 0 18px; }
.requirements-box li { line-height: 1.8; }

</style>
