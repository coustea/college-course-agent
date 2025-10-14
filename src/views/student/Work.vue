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
          <el-table-column prop="deadline" label="截止时间" width="180" />
          <el-table-column prop="publishedAt" label="发布时间" width="180" />
          <el-table-column prop="teacher" label="发布教师" width="140" />
          <el-table-column label="操作" width="260" align="center">
            <template #default="{ row }">
              <template v-if="getRowScore(row) != null">
                <span style="color:#16a34a;font-weight:600;">成绩：{{ getRowScore(row) }}分</span>
                <el-button style="margin-left:8px" size="small" @click="openGrade(row)">查看成绩</el-button>
              </template>
              <template v-else-if="isRowSubmitted(row)">
                <el-button type="warning" size="small" @click="openEdit(row)">修改</el-button>
                <el-button style="margin-left:8px" size="small" @click="openGrade(row)">查看成绩</el-button>
              </template>
              <template v-else>
                <el-button type="primary" size="small" @click="openDetail(row)">查看详情</el-button>
                <el-button style="margin-left:8px" size="small" @click="openGrade(row)">查看成绩</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 详情模式：沿用原提交流程 -->
      <div v-else-if="mode === 'detail'" class="submission-form">
        <el-page-header title="返回" @back="backToList" :content="currentAssignment?.title || '提交新作品'" />
        <el-card class="detail-card" shadow="never" style="margin-top: 12px">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="作业标题">{{ currentAssignment?.title || '-' }}</el-descriptions-item>
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
            <li v-if="requirements.descriptionRequired">需要填写作品描述<span v-if="requirements.descriptionMaxLen">（不超过 {{
                requirements.descriptionMaxLen
              }} 字）</span></li>
            <li v-if="requirements.maxFiles">最多上传 {{ requirements.maxFiles }} 个文件</li>
            <li v-if="requirements.maxFileSizeMB">单个文件不超过 {{ requirements.maxFileSizeMB }}MB</li>
            <li v-if="(requirements.allowedExtensions && requirements.allowedExtensions.length)
                       || (requirements.allowedMimeTypes && requirements.allowedMimeTypes.length)">
              允许的类型：
              <span v-if="requirements.allowedExtensions && requirements.allowedExtensions.length">{{
                  requirements.allowedExtensions.join(', ')
                }}</span>
              <span v-if="requirements.allowedMimeTypes && requirements.allowedMimeTypes.length">（{{
                  requirements.allowedMimeTypes.join(', ')
                }}）</span>
            </li>
            <li v-if="requirements.extraNotes">{{ requirements.extraNotes }}</li>
          </ul>
        </div>
        <div class="form-bottom-bar">
          <div class="deadline-text">{{ deadlineText }}</div>
          <el-tooltip :disabled="canSubmitWork" content="仅组长可提交" placement="top">
            <span>
              <el-button type="primary" :disabled="!canSubmitWork" :loading="submitting"
                         @click="isEditing ? submitWorkUpdate() : submitWork()">
                {{ isEditing ? '提交修改' : '提交作品' }}
              </el-button>
            </span>
          </el-tooltip>
        </div>
      </div>

      <!-- 成绩查看模式 -->
      <div v-else-if="mode === 'grade'" class="submission-form">
        <el-page-header title="返回" @back="backToList" :content="(currentAssignment?.title || '') + ' - 成绩'" />
        <el-card class="detail-card" shadow="never" style="margin-top: 12px">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="作业标题">{{ currentAssignment?.title || '-' }}</el-descriptions-item>
            <el-descriptions-item label="截止时间">{{ currentAssignment?.deadline || '-' }}</el-descriptions-item>
            <el-descriptions-item label="发布教师">{{ currentAssignment?.teacher || '-' }}</el-descriptions-item>
            <el-descriptions-item label="说明" :span="2">{{ currentAssignment?.description || '—' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
        <el-card class="detail-card" shadow="never" style="margin-top: 12px">
          <div class="attach-title" style="margin-bottom:8px;">小组成绩</div>
          <div v-if="gradeMembers.length">
            <div v-for="m in gradeMembers" :key="m.name" style="display:flex;align-items:center;gap:10px;margin:8px 0;">
              <span style="font-weight:600;min-width:120px;">{{ m.name }}</span>
              <span :class="levelClass(m.level)"><span class="dot"></span>{{ levelText(m.level) }}</span>
              <span style="margin-left:12px;color:#111827;font-weight:600;">{{ m.score != null ? (m.score + ' 分') : '暂无成绩' }}</span>
            </div>
          </div>
          <div v-else class="attachments-empty">暂无成绩</div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed,getCurrentInstance} from 'vue'
import { ElMessage } from 'element-plus'
import { getWorkSidebarStatus } from '@/services/workApi'
import axios from "axios"

const { proxy } = getCurrentInstance()
const BASE_URL = proxy.$baseUrl

const mode = ref('list')
const assignments = ref([])
const currentAssignment = ref(null)
const isEditing = ref(false)
// 成绩查看
const gradeMembers = ref([])
function levelTagType(level) {
  const key = String(level || '').toLowerCase()
  const map = { excellent: 'success', good: 'primary', average: 'warning', concern: 'danger' }
  return map[key] || 'info'
}
function levelText(level) {
  const key = String(level || '').toLowerCase()
  const map = { excellent: '优秀', good: '良好', average: '一般', concern: '需关注' }
  return map[key] || (level || '-')
}
function levelClass(level) {
  const key = String(level || '').toLowerCase()
  const allow = ['excellent', 'good', 'average', 'concern']
  const k = allow.includes(key) ? key : 'unknown'
  return `level-chip level-${k}`
}

const submissionForm = ref({
  title: '',
  description: '',
  files: []
})

const GROUP_STATUS_KEY = 'student_group_status'
const GROUP_INFO_KEY = 'student_group_info'
const groupStatus = ref('none') 
const groupInfo = ref(null)
const groupId = ref(null)
// 组队与角色
const currentUserName = computed(() => {
  try { return localStorage.getItem('studentName') || '' } catch { return '' }
})
const isLeader = computed(() => {
  const info = groupInfo.value || {}
  const leader = String(info.leaderName || '')
  return leader && leader === String(currentUserName.value || '')
})
const canSubmitWork = computed(() => {
  if (groupStatus.value === 'approved') return isLeader.value
  return groupStatus.value !== 'pending'

})
const SUBMIT_STATE_KEY = 'assignment_submission_state_v1'
const GRADES_STATE_KEY = 'assignment_grades_state_v1'

async function getGroupInfo() {
  try {
    const res = await axios.post(
      `${BASE_URL}/groupMember/getById`,
      { studentId: localStorage.getItem('userId') },
      {
        headers: {
          'Content-Type': 'multipart/form-data',
          Authorization: `Bearer ${localStorage.getItem('token')}`,
        }
      }
    );

    console.log("再次获取小组信息用来判断组长:",res.data)
    if (res.data.code === 200) {
      groupId.value = res.data.data.groupId;
      console.log("groupId:", groupId.value)
      // 继续获取小组详情，基于角色判断出组长
      if (groupId.value) {
        const headers = {
          Authorization: `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'multipart/form-data'
        }
        const fd = new FormData();
        fd.append('groupId', groupId.value)
        const detail = await axios.post(`${BASE_URL}/student-group/getByGroupId`, fd, {headers})
        console.log('group detail', detail?.data)
        if (detail?.data?.code === 200 && detail?.data?.data) {
          const group = detail.data.data
          const st = String(group?.approvalStatus || '').toLowerCase()
          groupStatus.value = st === 'approval' ? 'approved' : (st === 'pending' ? 'pending' : (st === 'rejected' ? 'rejected' : 'none'))
          const members = Array.isArray(group.groupMemberList) ? group.groupMemberList : []
          const roleOf = (m) => String(m?.role || m?.memberRole || m?.position || '').toLowerCase()
          let leaderIdx = members.findIndex(m => m?.isLeader === true || m?.leader === true || roleOf(m) === 'leader')
          if (leaderIdx < 0) leaderIdx = 0
          const leaderItem = members[leaderIdx] || {}
          const leaderName = leaderItem?.studentName || group.leaderName
          const restNames = members.filter((_, i) => i !== leaderIdx).map(m => m.studentName).filter(Boolean)
          groupInfo.value = {
            groupName: group.groupName || '',
            leaderName,
            memberNames: restNames,
            taskDescription: group.groupDescription || ''
          }
          // 持久化，供其他页面/刷新后的 isLeader 计算
          localStorage.setItem(GROUP_STATUS_KEY, groupStatus.value)
          localStorage.setItem(GROUP_INFO_KEY, JSON.stringify(groupInfo.value))
        }
      }
    }
    return res.data;
  } catch (err) {
    console.error("获取小组信息失败：", err)
  }
}


function readJson(key, def = {}) {
  try {
    const v = localStorage.getItem(key);
    return v ? JSON.parse(v) : def
  } catch {
    return def
  }
}

function writeJson(key, val) {
  try {
    localStorage.setItem(key, JSON.stringify(val))
  } catch {
  }
}

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

function loadSubmissionState() {
  submissionState.value = readJson(SUBMIT_STATE_KEY, {})
  gradesState.value = readJson(GRADES_STATE_KEY, {})
}

const submissionState = ref({})
const gradesState = ref({})
function isRowSubmitted(row) {
  const id = String(row?.id ?? row?.assignmentId ?? '')
  const state = submissionState.value 
  return !!(id && state[id] && state[id].status === 'submitted')
}
function getRowScore(row) {
  const id = String(row?.id ?? row?.assignmentId ?? '')
  const g = gradesState.value || {}
  const v = id ? g[id]?.score : null
  return typeof v === 'number' ? v : null
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
    await getGroupInfo()
    const data = await getTeachAssignments()
    console.log("获取教师分配的作品:",data)
    if (data?.code === 200 && Array.isArray(data?.data) && data.data.length > 0) {
      const first = data.data[0] || {}
      const dl = first.deadline || first.endTime || first.dueTime || first.dueDate
      if (dl) {
        const text = String(dl)
      deadlineText.value = text.startsWith('截止') ? text : `截止时间：${text}`
      }
    }
    try { applyRequirements(data || {}) } catch (e) { console.error(e) }
    
      const serverAssignments = normalizeAssignments(data || {})
      if (serverAssignments.length > 0) assignments.value = serverAssignments
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

  // 仍保留状态读取（用于其他文案或权限），但不影响提交方式
   loadGroupStatusFromStorage() 
  // 新增：初始加载本地已提交/成绩状态，避免刷新后按钮回退
   loadSubmissionState() 

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
    console.log("获取作品列表:",res.data)
    if (res.data.code === 200) {
      return res.data
    }
  } catch (e) { console.error(e) }
}


function onGroupUpdated(e) {
  try {
    const detail = e?.detail
    if (typeof detail.status === 'string') groupStatus.value = detail.status
    if (detail.info && typeof detail.info === 'object') groupInfo.value = detail.info
    // if (groupStatus.value !== 'approved' && submitScope.value === 'group') submitScope.value = 'individual'
  } catch {
  }
}

function onStorageChanged(ev) {
  if (!ev) return
  if (ev.key === GROUP_STATUS_KEY || ev.key === GROUP_INFO_KEY) {
    loadGroupStatusFromStorage()
  }
  if (ev.key === SUBMIT_STATE_KEY || ev.key === GRADES_STATE_KEY) {
    loadSubmissionState()
  }
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
      // 使用后端 assignmentId 作为唯一键，避免回退为标题导致第一行误命中
      id: item.assignmentId ?? item.id,
      title: item.assignmentName || '作业',
      description: item.description,
      deadline: item.dueDate,
      publishedAt: item.createdAt,
      teacher: item.teacherName,
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
    {
      id: 'mock-1',
      title: '思想道德修养期中报告',
      course: '思想道德修养与法律基础',
      deadline: formatDateTime(addDays(now, 7)),
      teacher: '李老师',
      description: '围绕社会主义核心价值观撰写2000字分析报告，PDF提交。',
      attachments: [{name: '参考模板.docx', url: '/uploads/template.docx'}]
    },
    {
      id: 'mock-2',
      title: '中国近现代史人物小传',
      course: '中国近现代史纲要',
      deadline: formatDateTime(addDays(now, 10)),
      teacher: '王老师',
      description: '任选一个近现代历史人物，完成不少于1500字人物小传。'
    },
    {
      id: 'mock-3',
      title: '形势与政策热点研判',
      course: '形势与政策',
      deadline: formatDateTime(addDays(now, 5)),
      teacher: '张老师',
      description: '围绕近期时政热点，完成PPT+讲稿并录制5分钟讲解视频。'
    },
    {
      id: 'mock-4',
      title: '毛泽东思想读书笔记',
      course: '毛泽东思想和中国特色社会主义理论体系概论',
      deadline: formatDateTime(addDays(now, 12)),
      teacher: '赵老师',
      description: '指定篇目阅读，提交不少于8页读书笔记（图片或PDF）。'
    },
    {
      id: 'mock-5',
      title: '马克思主义原理思维导图',
      course: '马克思主义基本原理',
      deadline: formatDateTime(addDays(now, 3)),
      teacher: '刘老师',
      description: '用思维导图工具梳理“实践与认识”的核心概念与关系。'
    }
  ]
}

function openDetail(a) {
  currentAssignment.value = a
  mode.value = 'detail'
  isEditing.value = false
}

function openGrade(row) {
  currentAssignment.value = row
  mode.value = 'grade'
  fetchGrades(row).catch(() => { gradeMembers.value = [] })
}

async function fetchGrades(row) {
  try {
    const assignmentId = row?.id || row?.assignmentId
    const key = assignmentId != null ? String(assignmentId) : ''
    const saved = key ? readJson(SUBMIT_STATE_KEY, {})[key] : null
    const submissionId = saved?.submissionId || assignmentId
    const token = localStorage.getItem('token')
    const resp = await axios.get(`${BASE_URL}/grading/group/${submissionId}`,
     { headers: { Authorization: `Bearer ${token}` } })
    const data = resp.data.data || resp.data
    console.log("获取成绩响应",resp.data)
    console.log("获取成绩",data)
    // 兼容多结构：[{name,role,score,level}] 或 {members:[...]} 或 studentScores
    const list = Array.isArray(data) ? data : (Array.isArray(data?.members) ? data.members : (Array.isArray(data?.studentScores) ? data.studentScores : []))
    gradeMembers.value = list.map(m => ({
      name: m.name || m.studentName || '-',
      level: (m.level || m.performanceLevel || m.gradeLevel || m.levelName || ''),
      score: (m.score != null ? Number(m.score) : (m.grade != null ? Number(m.grade) : null))
    }))
  } catch (e) {
    console.error('获取成绩失败', e)
    gradeMembers.value = []
  }
}

function backToList() {
  mode.value = 'list'
  isEditing.value = false
}
async function submitWork() {
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

  for (const f of submissionForm.value.files) {
    if (!validateSingleFile(f)) return
  }
  submitting.value = true

  try {
    // 构建FormData对象以适配后端接口
    const formData = new FormData()

    // 添加必要参数
    formData.append('assignmentId', currentAssignment.value?.id)
    console.log('assignmentId:', currentAssignment.value?.id)
    formData.append('groupId', groupId.value)
    formData.append('studentId', localStorage.getItem('userId'))
    if (submissionForm.value.title) {
      formData.append('title', submissionForm.value.title)
    }

    // 添加作业内容（注意：此处改为content以匹配后端参数名）
    if (submissionForm.value.description) {
      formData.append('content', submissionForm.value.description)
    }

    // 添加文件
    if (submissionForm.value.files && submissionForm.value.files.length > 0) {
      submissionForm.value.files.forEach(file => {
        formData.append('files', file.raw || file)
      })
    }
    console.log('提交上传的参数', formData)
    // 调用后端上传接口
    console.log('开始上传到:', `${BASE_URL}/submission/upload`)
    const response = await axios.post(`${BASE_URL}/submission/upload`, formData, {
      headers: { 
        Authorization: `Bearer ${localStorage.getItem('token')}` ,
        'Content-Type': 'multipart/form-data'
      }
    })
    console.log("提交作业响应",response.data)
    if (response.data.code === 200) {
      ElMessage.success('提交成功')
      const id = currentAssignment.value?.id || currentAssignment.value?.assignmentId
      const key = id != null ? String(id) : ''
      if (key) {
        const payload = {
          title: submissionForm.value.title,
          description: submissionForm.value.description,
          files: submissionForm.value.files
        }
        
        const newSubmissionId = response?.data?.data?.submissionId || response?.data?.data?.id || response?.data?.submissionId || null
        const state = readJson(SUBMIT_STATE_KEY, {})
        state[key] = {status: 'submitted', by: String(currentUserName.value || ''), at: Date.now(), lastPayload: payload, submissionId: newSubmissionId}
        writeJson(SUBMIT_STATE_KEY, state)
        submissionState.value = state
        window.dispatchEvent(new StorageEvent('storage', {key: SUBMIT_STATE_KEY}))
      }

      // 返回列表
      backToList()
      submissionForm.value = {title: '', description: '', files: []}

      try {
        const data = getWorkSidebarStatus()
        localStorage.setItem('work_sidebar_status', JSON.stringify(data || {}))
        window.dispatchEvent(new CustomEvent('work-sidebar-updated', {detail: data || {}}))
      } catch (e) {
        console.error(e)
      }
    } else {
      ElMessage.error(response.data.msg || '提交失败')
    }
    } catch (e) {
    console.error('提交接口错误', e?.response || e)
    const msg = e?.response?.data?.msg || e?.message || '未知错误'
    ElMessage.error('提交失败，请稍后重试: ' + msg)
    } finally {
      submitting.value = false
    }
}

function openEdit(row) {
  openDetail(row)
  isEditing.value = true
  try {
    const state = readJson(SUBMIT_STATE_KEY, {})
    const id = String(row?.id ?? row?.assignmentId ?? '')
    const last = id ? state[id]?.lastPayload : null
    if (last && typeof last === 'object') {
      submissionForm.value.title = last.title || ''
      submissionForm.value.description = last.description || ''
      submissionForm.value.files = Array.isArray(last.files) ? last.files : []
    }
  } catch {}
}

async function submitWorkUpdate() {
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

  for (const f of submissionForm.value.files) {
    if (!validateSingleFile(f)) return
  }
  submitting.value = true

  try {
    // 构建FormData对象以适配后端接口
    const formData = new FormData()

    // 添加必要参数
    formData.append('assignmentId', currentAssignment.value?.id)
    console.log('assignmentId:', currentAssignment.value?.id)
    formData.append('groupId', groupId.value)
    formData.append('studentId', localStorage.getItem('userId'))
    if (submissionForm.value.title) {
      formData.append('title', submissionForm.value.title)
    }

    // 添加作业内容（注意：此处改为content以匹配后端参数名）
    if (submissionForm.value.description) {
      formData.append('content', submissionForm.value.description)
    }

    // 添加文件
    if (submissionForm.value.files && submissionForm.value.files.length > 0) {
      submissionForm.value.files.forEach(file => {
        formData.append('files', file.raw || file)
      })
    }
    console.log('修改提交的参数', formData)
    // 调用后端上传接口
    const assignId = currentAssignment.value?.id
    const key = assignId != null ? String(assignId) : ''
    const saved = key ? readJson(SUBMIT_STATE_KEY, {})[key] : null
    const submissionId = saved?.submissionId || assignId
    console.log('用于修改的提交ID:', submissionId)
    console.log('开始上传到:', `${BASE_URL}/submission/${submissionId}`)
    const response = await axios.put(`${BASE_URL}/submission/${submissionId}`, formData, {
      headers: {
        Authorization: `Bearer ${localStorage.getItem('token')}` ,
        'Content-Type': 'multipart/form-data'
      }
    })
    console.log("提交修改",response.data)
    if (response.data.code === 200) {
      ElMessage.success('修改已提交')
      if (key) {
        const payload = {
          title: submissionForm.value.title,
          description: submissionForm.value.description,
          files: submissionForm.value.files
        }
        const state = readJson(SUBMIT_STATE_KEY, {})
        // 保留 submissionId（可能后端在修改时返回也可再次覆盖）
        const newSubmissionId = response?.data?.data?.submissionId || response?.data?.data?.id || response?.data?.submissionId || submissionId
        state[key] = {status: 'submitted', by: String(currentUserName.value || ''), at: Date.now(), lastPayload: payload, submissionId: newSubmissionId}
        writeJson(SUBMIT_STATE_KEY, state)
        submissionState.value = state
        window.dispatchEvent(new StorageEvent('storage', {key: SUBMIT_STATE_KEY}))
      }

      // 返回列表
      backToList()
      submissionForm.value = {title: '', description: '', files: []}

      try {
        const data = getWorkSidebarStatus()
        localStorage.setItem('work_sidebar_status', JSON.stringify(data || {}))
        window.dispatchEvent(new CustomEvent('work-sidebar-updated', {detail: data || {}}))
    } catch (e) {
        console.error(e)
      }
    } else {
      ElMessage.error(response.data.msg || '提交失败')
    }
  } catch (e) {
    console.error('提交接口错误', e?.response || e)
    const msg = e?.response?.data?.msg || e?.message || '未知错误'
    ElMessage.error('提交失败，请稍后重试: ' + msg)
    } finally {
      submitting.value = false
    }
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

.submission-form {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 22px;
  margin-bottom: 16px;
  box-shadow: 0 2px 6px rgba(0,0,0,.04);
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

.attachments {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
}

.attach-title {
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
}

.attachments-empty {
  color: #94a3b8;
  font-size: 13px;
  padding: 8px;
  border: 1px dashed #e5e7eb;
  border-radius: 8px;
}

</style>
