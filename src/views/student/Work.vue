<template>
  <div class="page-container">
    <div class="header">
      <h1 class="page-title">作品提交</h1>
    </div>
    <div class="content">
      <!-- 列表模式 -->
      <div v-if="mode === 'list'" class="assignment-list">
        <el-empty description="暂无作业" v-if="assignments.length === 0" />
        <el-table v-else :data="assignments" border stripe style="width: 100%">
          <el-table-column prop="title" label="作业标题" min-width="220" />
          <el-table-column prop="course" label="课程" min-width="180" />
          <el-table-column prop="deadline" label="截止时间" width="180" />
          <el-table-column prop="teacher" label="发布教师" width="140" />
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="openDetail(row)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 详情模式：沿用原提交流程 -->
      <div v-else class="submission-form">
        <el-page-header title="返回" @back="backToList" :content="currentAssignment?.title || '提交新作品'" />
        <el-card class="detail-card" shadow="never" style="margin-top: 12px">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="作业标题">{{ currentAssignment?.title || '-' }}</el-descriptions-item>
            <el-descriptions-item label="课程">{{ currentAssignment?.course || '-' }}</el-descriptions-item>
            <el-descriptions-item label="截止时间">{{ currentAssignment?.deadline || '-' }}</el-descriptions-item>
            <el-descriptions-item label="发布教师">{{ currentAssignment?.teacher || '-' }}</el-descriptions-item>
            <el-descriptions-item label="说明" :span="2">{{ currentAssignment?.description || '—' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
        <el-card class="detail-card" shadow="never" style="margin-top: 12px">
          <div class="attach-title">附件</div>
          <div v-if="Array.isArray(currentAssignment?.attachments) && currentAssignment.attachments.length" class="attachments">
            <div v-for="(f, idx) in currentAssignment.attachments" :key="idx" class="attachment-item">
              <i class="fas fa-paperclip" style="margin-right:6px;color:#64748b;"></i>
              <span class="file-name">{{ f.name || ('附件' + (idx + 1)) }}</span>
              <el-link :href="normalizeUrl(f.url)" target="_blank" :download="f.name || ''" type="primary" style="margin-left:auto">下载</el-link>
            </div>
        </div>
          <div v-else class="attachments-empty">暂无附件</div>
        </el-card>
        <el-form :model="submissionForm" label-width="100px">
          <div class="form-row">
            <el-form-item label="提交身份">
              <div class="submit-scope">
                <el-radio-group v-model="submitScope">
                  <el-radio-button label="individual">个人</el-radio-button>
                  <el-radio-button label="group" :disabled="groupStatus !== 'approved'">小组</el-radio-button>
                </el-radio-group>
                <span class="status-chip" :class="`st-${groupStatus}`">{{ groupStatusText }}</span>
              </div>
            </el-form-item>
          </div>
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
        <div class="form-bottom-bar">
          <div class="deadline-text">{{ deadlineText }}</div>
          <el-button type="primary" :loading="submitting" @click="submitWork">提交作品</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed,getCurrentInstance} from 'vue'
import { ElMessage } from 'element-plus'
import { getWorkSidebarStatus, submitPersonalWork, submitTeamWork } from '@/services/workApi'
import axios from "axios";

const { proxy } = getCurrentInstance()
const BASE_URL = proxy.$baseUrl

const mode = ref('list')
const assignments = ref([])
const currentAssignment = ref(null)

const submissionForm = ref({
  title: '',
  description: '',
  files: []
})

const GROUP_STATUS_KEY = 'student_group_status'
const GROUP_INFO_KEY = 'student_group_info'
const groupStatus = ref('none') 
const groupInfo = ref(null)
const submitScope = ref('individual') 
const groupStatusText = computed(() => {
  if (groupStatus.value === 'approved') return '已组队'
  if (groupStatus.value === 'pending') return '审批中'
  return '未组队'
})
const canGroupSubmit = computed(() => groupStatus.value === 'approved')

const deadlineText = ref('截止时间：2025-12-31 23:59')
const submitting = ref(false)
function getDefaultRequirements() {
  return {
    titleRequired: true,
    descriptionRequired: true,
    descriptionMaxLen: 200,
    maxFiles: 3,
    maxFileSizeMB: 10,
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
    // const data = await getWorkSidebarStatus()
    const data = await getTeachAssignments()
    console.log(data)
    if (data?.code === 200 && Array.isArray(data?.data) && data.data.length > 0) {
      const first = data.data[0] || {}
      const dl = first.deadline || first.endTime || first.dueTime || first.dueDate
      if (dl) {
        const text = String(dl)
      deadlineText.value = text.startsWith('截止') ? text : `截止时间：${text}`
      }
    }
    try { applyRequirements(data || {}) } catch (e) { console.error(e) }
    // 如后端返回了作业列表，优先使用
    try {
      const serverAssignments = normalizeAssignments(data || {})
      if (serverAssignments.length > 0) assignments.value = serverAssignments
    } catch {}
    try {
      localStorage.setItem('work_sidebar_status', JSON.stringify(data || {}))
      window.dispatchEvent(new CustomEvent('work-sidebar-updated', { detail: data || {} }))
    } catch (e) { console.error(e) }
  } catch (e) { console.error(e) }

  try {
    const raw = localStorage.getItem('work_sidebar_status')
    const cached = raw ? JSON.parse(raw) : {}
    assignments.value = normalizeAssignments(cached)
  } catch {
    assignments.value = []
  }

  if (!Array.isArray(assignments.value) || assignments.value.length === 0) {
    assignments.value = getMockAssignments()
  }

  // 读取组队状态，决定提交身份默认值
  try {
    loadGroupStatusFromStorage()
  } catch {}
  submitScope.value = groupStatus.value === 'approved' ? 'group' : 'individual'

  // 监听来自“学习分组”页面的状态更新
  try { window.addEventListener('student-group-updated', onGroupUpdated) } catch {}
  try { window.addEventListener('storage', onStorageChanged) } catch {}
})


const getTeachAssignments = async () =>{
  try{
    const className = localStorage.getItem('className')
    console.log(className)
    const res = await axios.post(`${BASE_URL}/teacherAssignments/byClassName`, { className },{
      headers: {
        'Content-Type': 'multipart/form-data',
        Authorization: `Bearer ${localStorage.getItem('token')}`
      }
    })
    console.log(res.data)
    if (res.data.code === 200) {
      return res.data
    }
  } catch (e) { console.error(e) }
}


function onGroupUpdated(e) {
  try {
    const detail = e?.detail || {}
    if (typeof detail.status === 'string') groupStatus.value = detail.status
    if (detail.info && typeof detail.info === 'object') groupInfo.value = detail.info
    if (groupStatus.value !== 'approved' && submitScope.value === 'group') submitScope.value = 'individual'
  } catch {}
}

function onStorageChanged(ev) {
  if (!ev || (ev.key !== GROUP_STATUS_KEY && ev.key !== GROUP_INFO_KEY)) return
  loadGroupStatusFromStorage()
  if (groupStatus.value !== 'approved' && submitScope.value === 'group') submitScope.value = 'individual'
}

function loadGroupStatusFromStorage() {
  try {
    const st = localStorage.getItem(GROUP_STATUS_KEY)
    if (st === 'pending' || st === 'approved' || st === 'none') groupStatus.value = st
    const info = JSON.parse(localStorage.getItem(GROUP_INFO_KEY) || 'null')
    if (info && typeof info === 'object') groupInfo.value = info
  } catch {}
}


function handleFileChange(file, fileList) {
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
  const maxMB = Number(requirements.value.maxFileSizeMB || 0)
  if (maxMB > 0 && file.size > maxMB * bytesPerMB()) {
    ElMessage.error(`文件大小超限（最大 ${maxMB}MB）: ${file.name}`)
    return false
  }
  
  if (!isTypeAllowed(file)) {
    const allowTip = accept.value || '格式受限'
    ElMessage.error(`文件类型不被允许: ${file.name}（允许: ${allowTip}）`)
    return false
  }
  return true
}

function beforeUpload(file) {
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

function normalizeAssignments(data) {
  const list = []
  const arr = Array.isArray(data?.assignments) ? data.assignments : (Array.isArray(data?.data) ? data.data : [])
  for (const item of arr) {
    let attachments = []
    try {
      const raw = item?.attachmentFiles ?? item?.attachments ?? []
      const arrA = typeof raw === 'string' ? (JSON.parse(raw || '[]') || []) : raw
      if (Array.isArray(arrA)) {
        attachments = arrA.map((a, i) => {
          if (typeof a === 'string') {
            const name = a.split('/').pop() || `附件${i + 1}`
            return { name, url: a }
          }
          return {
            name: a?.name || a?.fileName || a?.title || `附件${i + 1}`,
            url: a?.url || a?.fileUrl || a?.resourceUrl || a?.path || ''
          }
        }).filter(x => !!x.url)
      }
    } catch {}
    list.push({
      id: item.id ?? item.assignmentId ?? `${item.title || '作业'}`,
      title: item.title || item.name || '作业',
      description: item.description || item.content || '',
      deadline: item.deadline || item.endTime || item.dueTime || item.dueDate || '',
      course: item.courseName || item.course || String(item.courseId ?? ''),
      teacher: item.teacher || String(item.teacherId ?? ''),
      attachments
    })
  }
  return list
}

function addDays(base, days) {
  const d = new Date(base)
  d.setDate(d.getDate() + days)
  return d
}
function formatDateTime(dt) {
  const p = (n) => String(n).padStart(2, '0')
  const y = dt.getFullYear()
  const m = p(dt.getMonth() + 1)
  const d = p(dt.getDate())
  const h = p(dt.getHours())
  const mi = p(dt.getMinutes())
  return `${y}-${m}-${d} ${h}:${mi}`
}
// 兼容相对/绝对链接的附件地址
function normalizeUrl(u) {
  if (!u) return ''
  const s = String(u)
  if (/^(https?:|data:|blob:)/i.test(s)) return s
  if (s.startsWith('/')) return s
  return `/${s.replace(/^\//,'')}`
}
function getMockAssignments() {
  const now = new Date()
  return [
    { id: 'mock-1', title: '思想道德修养期中报告', course: '思想道德修养与法律基础', deadline: formatDateTime(addDays(now, 7)), teacher: '李老师', description: '围绕社会主义核心价值观撰写2000字分析报告，PDF提交。', attachments: [{ name: '参考模板.docx', url: '/uploads/template.docx' }] },
    { id: 'mock-2', title: '中国近现代史人物小传', course: '中国近现代史纲要', deadline: formatDateTime(addDays(now, 10)), teacher: '王老师', description: '任选一个近现代历史人物，完成不少于1500字人物小传。' },
    { id: 'mock-3', title: '形势与政策热点研判', course: '形势与政策', deadline: formatDateTime(addDays(now, 5)), teacher: '张老师', description: '围绕近期时政热点，完成PPT+讲稿并录制5分钟讲解视频。' },
    { id: 'mock-4', title: '毛泽东思想读书笔记', course: '毛泽东思想和中国特色社会主义理论体系概论', deadline: formatDateTime(addDays(now, 12)), teacher: '赵老师', description: '指定篇目阅读，提交不少于8页读书笔记（图片或PDF）。' },
    { id: 'mock-5', title: '马克思主义原理思维导图', course: '马克思主义基本原理', deadline: formatDateTime(addDays(now, 3)), teacher: '刘老师', description: '用思维导图工具梳理“实践与认识”的核心概念与关系。' }
  ]
}

function openDetail(a) {
  currentAssignment.value = a
  mode.value = 'detail'
}

function backToList() {
  mode.value = 'list'
}

function submitWork() {
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

  // 文件大小/类型复核
  for (const f of submissionForm.value.files) {
    if (!validateSingleFile(f)) return
  }
  // 若选择“小组”，但当前状态不是已组队，强制回退为个人
  if (submitScope.value === 'group' && groupStatus.value !== 'approved') {
    submitScope.value = 'individual'
  }
  submitting.value = true;
  (async () => {
    try {
      const studentId = localStorage.getItem('userId')
      if (submitScope.value === 'group') {
        await submitTeamWork({ files: submissionForm.value.files, assignmentId: currentAssignment.value?.id })
      } else {
        await submitPersonalWork({
          files: submissionForm.value.files,
          assignmentId: currentAssignment.value?.id,
          studentId: Number(studentId) || studentId,
          content: submissionForm.value.description
        })
      }
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
.page-container { max-width: 1200px; margin: 0 auto; padding: 10px; }
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
  width: 100%;
}

.assignment-list { 
  background:#fff; 
  border:1px solid #e5e7eb; 
  border-radius:12px; 
  padding:22px; 
  margin-bottom:16px; 
  box-shadow: 0 2px 6px rgba(0,0,0,.04); 
}
.assignment-items { 
  display:flex; 
  flex-direction:column; 
  gap:12px; 
}
.assignment-item { 
  display:flex; 
  justify-content:space-between;
  align-items:flex-start; 
  border:1px solid #eef2f7; 
  border-radius:10px; 
  padding:12px; 
}
.assignment-main { 
  max-width: 60%; 
}
.assignment-title { 
  font-weight:600; 
  color:#1e293b; 
  margin-bottom:6px; 
}
.assignment-desc { 
  color:#64748b; 
  font-size:13px; 
}
.assignment-meta { 
  display:flex; 
  gap:10px; 
  align-items:center; 
}
.assignment-meta .deadline { 
  color:#6b7280; 
  font-size:12px; 
}

.submission-form {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 22px;
  margin-bottom: 16px;
  box-shadow: 0 2px 6px rgba(0,0,0,.04);
}
.detail-header { 
  display:flex; 
  align-items:center; 
  justify-content:space-between;
  margin-bottom:8px; 
}
.detail-title { 
  font-weight:600; 
  color:#1e293b; 
}
.submission-form .el-form { 
  margin: 16px 0; 
}
.form-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 14px 0;
  padding-bottom: 10px;
  border-bottom: 2px solid #f1f5f9;
}
.form-row { 
  margin-bottom: 14px; 
}

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
.requirements-box ul { 
  margin: 6px 0 0 18px; 
  padding: 0;
  list-style: none;
}
.requirements-box li { 
  line-height: 1.8; 
}

.submit-scope { display:flex; align-items:center; gap:12px; }
.status-chip { display:inline-block; padding:2px 8px; border-radius:999px; font-size:12px; font-weight:700; }
.st-none { background:#ffebee; color:#c62828; }
.st-pending { background:#fff7ed; color:#b45309; }
.st-approved { background:#e8f5e9; color:#2e7d32; }

.attachments { display:flex; flex-direction:column; gap:8px; }
.attachment-item { display:flex; align-items:center; gap:8px; padding:6px 8px; border:1px solid #eef2f7; border-radius:8px; }
.attach-title { font-weight:600; color:#1e293b; margin-bottom:8px; }
.attachments-empty { color:#94a3b8; font-size:13px; padding:8px; border:1px dashed #e5e7eb; border-radius:8px; }

/* 兼容相对地址的下载链接 */

</style>
