<template>
  <div class="assignments-check">
    <div class="check-header-white">
      <div class="header-content">
        <div class="header-top">
          <el-button @click="$router.push('/teacher/assignments/list')" link class="back-link">
            <i class="fas fa-arrow-left"></i> 返回列表
          </el-button>
        </div>
        <div class="header-main">
          <div class="header-info">
            <h2 class="page-title">{{ assignmentTitle }}</h2>
            <div class="meta-tags">
              <el-tag effect="plain" type="info" class="meta-tag"><i class="fas fa-users"></i> {{ courseName }}</el-tag>
              <el-tag effect="plain" type="warning" class="meta-tag"><i class="far fa-clock"></i> 截止：{{ formatDate(deadline) }}</el-tag>
              <el-tag effect="plain" type="success" class="meta-tag" v-if="submittedCount === totalGroups"><i class="fas fa-check-circle"></i> 全部收齐</el-tag>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="check-content-premium">
      <div class="stats-overview-premium">
        <el-row :gutter="24">
          <el-col :span="6">
            <el-card shadow="hover" class="stat-card-premium submitted-card">
              <div class="stat-icon-wrapper">
                <i class="fas fa-file-alt"></i>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ submittedCount }}</div>
                <div class="stat-label">已提交小组</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" class="stat-card-premium pending-card">
              <div class="stat-icon-wrapper">
                <i class="fas fa-spinner"></i>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ totalGroups - submittedCount }}</div>
                <div class="stat-label">未提交小组</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" class="stat-card-premium graded-card">
              <div class="stat-icon-wrapper">
                <i class="fas fa-check-double"></i>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ completedCount }}</div>
                <div class="stat-label">已完成检查</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" class="stat-card-premium progress-card">
              <div class="stat-icon-wrapper">
                <el-progress
                  type="circle"
                  :percentage="Math.round(((submittedCount / totalGroups) || 0) * 100)"
                  :width="40"
                  :stroke-width="5"
                  :show-text="false"
                  :color="customColors"
                />
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ Math.round(((submittedCount / totalGroups) || 0) * 100) }}%</div>
                <div class="stat-label">整体提交率</div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <div class="groups-list">
        <h3>小组检查情况</h3>
        <el-table :data="groups" style="width: 100%" stripe>
          <el-table-column prop="groupName" label="小组名称" min-width="160">
            <template #default="scope">
              <div class="group-name-cell">
                <div class="group-avatar" :style="{ backgroundColor: getAvatarColor(scope.row.groupName) }">
                  {{ scope.row.groupName.charAt(0) }}
                </div>
                <span class="group-name-text">{{ scope.row.groupName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="leaderName" label="组长" width="140" align="center">
             <template #default="scope">
                <div class="leader-cell">
                  <span>{{ scope.row.leaderName }}</span>
                  <i v-if="scope.row.leaderName" class="fas fa-crown leader-icon" title="组长"></i>
                </div>
             </template>
          </el-table-column>
          <el-table-column prop="members" label="组员" width="200" align="center">
            <template #default="scope">
              <el-tag
                v-for="member in scope.row.members"
                :key="member"
                size="small"
                style="margin: 2px;"
              >
                {{ member }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="submitTime" label="提交时间" width="160" align="center">
            <template #default="scope">
              {{ scope.row.submitTime ? formatDateTime(scope.row.submitTime) : '未提交' }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="提交状态" width="100" align="center">
            <template #default="scope">
              <el-tag :type="getSubmitStatusType(scope.row.status)" size="small">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="checkStatus" label="检查状态" width="100" align="center">
            <template #default="scope">
              <el-tag :type="getCheckStatusType(scope.row.hasGrades ? '已检查' : scope.row.checkStatus)" size="small">
                {{ scope.row.hasGrades ? '已检查' : scope.row.checkStatus }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="score" label="评分" width="100" align="center">
            <template #default="scope">
              {{ scope.row.hasGrades ? '已评分' : '未评分' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="300" align="center">
            <template #default="scope">
              <el-button
                size="small"
                @click="viewGroupDetails(scope.row)"
                :disabled="!scope.row.submitTime"
              >
                查看详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>


      <el-dialog
        v-model="detailDialogVisible"
        :title="`${selectedGroup?.groupName} - 检查详情`"
        width="80%"
        top="50px"
        class="centered-dialog"
        append-to-body
      >
        <div v-if="selectedGroup">
          <el-descriptions title="基本信息" border>
            <el-descriptions-item label="小组名称">{{ selectedGroup.groupName }}</el-descriptions-item>
            <el-descriptions-item label="组长">{{ selectedGroup.leaderName }}</el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ formatDateTime(selectedGroup.submitTime) }}</el-descriptions-item>
            <el-descriptions-item label="检查状态">
              <el-tag :type="getCheckStatusType(selectedGroup.checkStatus)">
                {{ selectedGroup.checkStatus }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <div class="submission-content" style="margin-top: 20px;">
            <h4>提交内容：</h4>
            <div class="content-preview">
              <p v-if="!selectedGroup.content">该小组未提交文本内容</p>
              <p v-else>{{ selectedGroup.content }}</p>
            </div>

            <h4>附件：</h4>
            <div class="attachments">
              <div v-for="(file, index) in selectedGroup.attachments" :key="index" class="attachment-item">
                <i class="fas fa-file"></i>
                <span>{{ file.name }}</span>
                <el-button link type="primary" @click="previewFile(file)">预览</el-button>
                <el-button link type="primary" @click="downloadFile(file)">下载</el-button>
              </div>
              <p v-if="selectedGroup.attachments.length === 0">该小组未提交附件</p>
            </div>
          </div>

          <div class="grading-form" style="margin-top: 20px;">
            <h4>成员评分</h4>
            <el-table :data="gradingMembers" style="width: 100%" size="small">
              <el-table-column prop="studentName" label="成员" width="160"/>
              <el-table-column label="分数" width="160">
                <template #default="scope">
                  <el-input-number v-model="scope.row.score" :min="0" :max="100"/>
                </template>
              </el-table-column>
              <el-table-column label="等级" width="160">
                <template #default="scope">
                  <el-select v-model="scope.row.level" placeholder="等级" style="width: 120px;">
                    <el-option label="优秀" value="优秀"/>
                    <el-option label="中等" value="中等"/>
                    <el-option label="合格" value="合格"/>
                    <el-option label="不及格" value="不及格"/>
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="评语">
                <template #default="scope">
                  <el-input v-model="scope.row.feedback" placeholder="评语"/>
                </template>
              </el-table-column>
            </el-table>

            <h4 style="margin-top: 20px;">小组整体评语</h4>
                <el-input
              v-model="gradingForm.groupComment"
                  type="textarea"
                  :rows="4"
              :maxlength="500"
              show-word-limit
              placeholder="请输入对整个小组的评语（最多500字）"
              style="width: 100%;"
            />
          </div>
        </div>

        <template #footer>
          <el-button @click="detailDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitCheck">提交检查</el-button>
        </template>
      </el-dialog>

      <el-dialog
          v-model="personalDialogVisible"
          :title="`个人提交详情 - 学生ID: ${selectedPersonal?.studentId || ''}`"
          width="60%"
          top="80px"
          append-to-body
      >
        <div v-if="selectedPersonal">
          <el-descriptions title="基本信息" border>
            <el-descriptions-item label="学生ID">{{ selectedPersonal.studentId }}</el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ formatDateTime(selectedPersonal.submittedAt) }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="selectedPersonal.status === 'submitted' ? 'success' : (selectedPersonal.status === 'graded' ? 'primary' : 'info')">
                {{ selectedPersonal.status || '—' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="评分">{{ selectedPersonal.score != null ? selectedPersonal.score : '未评分' }}</el-descriptions-item>
          </el-descriptions>

          <div class="content-preview" style="margin-top: 20px;">
            <h4>提交内容：</h4>
            <div class="content-preview">
              <p v-if="!selectedPersonal.submissionContent">无</p>
              <p v-else>{{ selectedPersonal.submissionContent }}</p>
            </div>
            <h4>附件：</h4>
            <div class="attachments">
              <div v-for="(file, index) in selectedPersonal.files" :key="index" class="attachment-item">
                <i class="fas fa-file"></i>
                <span>{{ file.name }}</span>
                <el-button link type="primary" @click="previewFile(file)">预览</el-button>
                <el-button link type="primary" @click="downloadFile(file)">下载</el-button>
              </div>
              <p v-if="!selectedPersonal.files || selectedPersonal.files.length === 0">无</p>
            </div>
          </div>

          <div class="grading-form" style="margin-top: 20px;">
            <h4>个人评分</h4>
            <el-form :model="personalGrading" label-width="80px">
              <el-form-item label="得分">
                <el-input-number v-model="personalGrading.score" :min="0" :max="100" placeholder="请输入得分" />
                <span class="score-total">/ 100</span>
              </el-form-item>
              <el-form-item label="评语">
                <el-input v-model="personalGrading.feedback" type="textarea" :rows="3" placeholder="请输入评语" />
              </el-form-item>
            </el-form>
          </div>
        </div>
        <template #footer>
          <el-button @click="personalDialogVisible = false">关闭</el-button>
          <el-button type="primary" @click="submitPersonalGrade">提交评分</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const assignmentId = route.params.id

// axios 实例
const API_BASE = (import.meta?.env?.VITE_API_BASE_URL || '/api')
const FILE_BASE = API_BASE.replace(/\/api$/, '')
const api = axios.create({ baseURL: API_BASE, timeout: 20000 })
api.interceptors.request.use((config) => {
  try {
    const token = localStorage.getItem('token') || localStorage.getItem('userToken')
    if (token) config.headers = { ...(config.headers || {}), Authorization: `Bearer ${token}` }
  } catch {}
  return config
})

// 作业信息（动态）
const assignmentTitle = ref('')
const courseName = ref('') // 用于展示班级名称
const deadline = ref('')
const submittedCount = ref(0)
const totalGroups = ref(0)
const currentCourseId = ref(null)

// 小组数据（动态）
const groups = ref([])

const selectedGroup = ref(null)
const highlightedMember = ref('')
const detailDialogVisible = ref(false)

// 评分表单（组）
const gradingForm = reactive({
  score: null,
  comment: '',
  result: '通过',
  groupComment: ''
})

// 成员评分表
const gradingMembers = ref([])

// 个人提交数据
const personalSubmissions = ref([])
const personalDialogVisible = ref(false)
const selectedPersonal = ref(null)
const personalGrading = reactive({ score: null, feedback: '' })

// 计算属性
const completedCount = computed(() => {
  return groups.value.filter(g => g.checkStatus === '已检查').length
})

// 方法
const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  // 处理时区问题：确保显示正确的本地时间
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

// 自动生成头像颜色
const getAvatarColor = (name) => {
  const colors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#6366f1']
  if (!name) return colors[0]
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  return colors[Math.abs(hash) % colors.length]
}

const customColors = [
  { color: '#f56c6c', percentage: 20 },
  { color: '#e6a23c', percentage: 40 },
  { color: '#5cb87a', percentage: 60 },
  { color: '#1989fa', percentage: 80 },
  { color: '#6f7ad3', percentage: 100 },
]

const formatDateTime = (dateTimeString) => {
  if (!dateTimeString) return '-'
  const date = new Date(dateTimeString)
  // 处理时区问题：确保显示正确的本地时间（中国时区 UTC+8）
  // 如果传入的是字符串，确保正确解析
  const timestamp = date.getTime()
  if (isNaN(timestamp)) return dateTimeString // 如果无法解析，返回原字符串

  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  })
}

const getSubmitStatusType = (status) => {
  const statusMap = {
    '已提交': 'success',
    '未提交': 'warning',
    '迟交': 'danger'
  }
  return statusMap[status] || 'info'
}

const getCheckStatusType = (status) => {
  const statusMap = {
    '已检查': 'success',
    '待检查': 'warning',
    '未提交': 'info'
  }
  return statusMap[status] || 'info'
}

const viewGroupDetails = async (group) => {
  selectedGroup.value = group
  // 初始化表单数据
  gradingForm.score = group.score
  gradingForm.comment = ''
  gradingForm.result = '通过'
  gradingForm.groupComment = ''
  // 初始化成员评分表
  const memberObjs = Array.isArray(group.memberObjects) ? group.memberObjects : []
  gradingMembers.value = memberObjs.map(m => ({
    studentId: Number(m.studentId),
    studentName: m.name || m.studentName || '',
    score: null,
    level: '',
    feedback: ''
  }))
  detailDialogVisible.value = true
  // 预填已有评分和小组评语（现在统一由 tryPrefillExistingGrades 处理）
  tryPrefillExistingGrades()
}

const checkGroupWork = (group) => {
  viewGroupDetails(group)
}

const downloadFile = async (file) => {
  try {
    const url = normalizeFileUrl(file.url || file)
    const filename = file.name || (url ? url.split('/').pop() : '') || 'download'
    // 优先使用 a[download]
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    a.target = '_blank'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
  } catch (e) {
    try {
      const url = normalizeFileUrl(file.url || file)
      const resp = await axios.get(url, { responseType: 'blob' })
      const blobUrl = window.URL.createObjectURL(resp.data)
      const a = document.createElement('a')
      a.href = blobUrl
      a.download = file.name || 'download'
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      window.URL.revokeObjectURL(blobUrl)
    } catch {}
  }
}

const previewFile = async (file) => {
  const url = normalizeFileUrl(file.url || file)
  const fileName = file.name || url.split('/').pop()
  const fileExt = fileName.toLowerCase().split('.').pop()

  // 图片预览：png, jpg, jpeg, gif, webp
  if (['png', 'jpg', 'jpeg', 'gif', 'webp'].includes(fileExt)) {
    ElMessageBox({
      title: fileName,
      message: h('img', { src: url, style: 'max-width: 100%; max-height: 500px;' }),
      showCancelButton: true,
      confirmButtonText: '下载',
      cancelButtonText: '关闭'
    }).then(() => {
      downloadFile(file)
    }).catch(() => {})
    return
  }

  // PDF 预览（直接打开 URL）
  if (fileExt === 'pdf') {
    window.open(url, '_blank')
    return
  }

  // 文本类：txt, js, css, html, json 等
  if (['txt', 'js', 'css', 'html', 'json', 'xml', 'md'].includes(fileExt)) {
    try {
      const res = await axios.get(url)
      ElMessageBox({
        title: fileName,
        message: h('pre', { style: 'white-space: pre-wrap; max-height: 500px; overflow-y: auto;' }, res.data),
        showConfirmButton: false,
        cancelButtonText: '关闭',
        showCancelButton: true
      })
    } catch {
      ElMessage.warning('无法加载文本内容')
    }
    return
  }

  // Office 文件（使用第三方服务或后端转换）
  if (['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx'].includes(fileExt)) {
    // 使用 Microsoft Online Viewer
    const officePreviewUrl = `https://view.officeapps.live.com/op/view.aspx?src=${encodeURIComponent(url)}`
    window.open(officePreviewUrl, '_blank')
    return
  }

  // 其他格式默认下载
  ElMessageBox.confirm(`不支持预览该文件类型（.${fileExt}），是否下载？`, '提示', {
    type: 'warning'
  }).then(() => {
    downloadFile(file)
  }).catch(() => {})
}

const submitCheck = async () => {
  try {
    // 组平均分（可选）
    if (gradingForm.score === null && gradingMembers.value.every(m => m.score == null)) {
      ElMessage.error('请先输入分数（至少成员中有一人有分数）')
      return
    }

    // 如果存在提交ID，优先提交成员评分
    const submissionId = selectedGroup.value && (selectedGroup.value.submissionId || selectedGroup.value.submission_id)
    if (submissionId) {
      const teacherId = Number(localStorage.getItem('teacherId') || localStorage.getItem('userId') || 0)
      let teacherName = ''
      try {
        const ui = JSON.parse(localStorage.getItem('userInfo') || '{}')
        teacherName = ui?.name || ui?.profile?.name || ''
      } catch {}
      const payload = {
        submissionId: Number(submissionId),
        teacherName: teacherName || undefined,
        members: gradingMembers.value
          .filter(m => m && Number.isFinite(Number(m.studentId)))
          .map(m => ({
            studentId: Number(m.studentId),
            score: m.score != null ? Number(m.score) : undefined,
            level: m.level || undefined,
            feedback: m.feedback || undefined
          }))
      }
      await api.post('/grading/group', payload)

      // 保存小组整体评语
      if (gradingForm.groupComment && gradingForm.groupComment.trim()) {
        try {
          await api.put(`/submission/${submissionId}/comment`, {
            groupComment: gradingForm.groupComment.trim()
          })
        } catch (error) {
          console.error('保存小组评语失败:', error)
          ElMessage.warning('评分成功，但小组评语保存失败')
        }
      }
    }

    // 更新小组检查状态与显示分数
  const group = groups.value.find(g => g.id === selectedGroup.value.id)
  if (group) {
      // 计算成员平均分（若存在）
      const validScores = gradingMembers.value.map(m => m.score).filter(s => s != null && !isNaN(Number(s)))
      if (validScores.length > 0) {
        const avg = Math.round(validScores.reduce((a, b) => a + Number(b), 0) / validScores.length)
        group.score = avg
        group.hasGrades = true
      } else if (gradingForm.score != null) {
        group.score = Number(gradingForm.score)
        group.hasGrades = true
      }
    group.checkStatus = '已检查'
    }
    ElMessage.success('检查完成')
    detailDialogVisible.value = false
  } catch (e) {
    console.error('提交评分失败:', e)
    ElMessage.error('提交评分失败')
  }
}

// 预填已有成员评分（同时加载小组评语）
const tryPrefillExistingGrades = async () => {
  try {
    const submissionId = selectedGroup.value && (selectedGroup.value.submissionId || selectedGroup.value.submission_id)


    if (!submissionId) {
      console.warn('[加载历史评分] submissionId 为空，无法加载历史评分')
      return
    }

    const resp = await api.get(`/grading/group/${submissionId}`)

    const raw = resp?.data
    const data = raw?.data || raw


    // 处理成员评分（新版本返回格式：{ memberScores: [...], groupComment: "..." }）
    const list = Array.isArray(data?.memberScores) ? data.memberScores : (Array.isArray(data) ? data : [])

    if (Array.isArray(list) && list.length > 0) {
      const map = new Map()
      list.forEach(it => map.set(Number(it.studentId || it.student_id), {
        score: it.score != null ? Number(it.score) : null,
        level: it.level || '',
        feedback: it.feedback || ''
      }))

      gradingMembers.value = gradingMembers.value.map(m => {
        const got = map.get(Number(m.studentId))
        const result = got ? { ...m, ...got } : m
        if (got) {
        }
        return result
      })

      // 若存在任一成员已有评分，则标为已评分
      const anyScored = gradingMembers.value.some(x => x.score != null && !isNaN(Number(x.score)))
      if (anyScored && selectedGroup.value) {
        const group = groups.value.find(g => g.id === selectedGroup.value.id)
        if (group) group.hasGrades = true
      }
    } else {
    }

    // 处理小组评语（新版本已包含在同一个响应中）
    if (data && data.groupComment) {
      gradingForm.groupComment = data.groupComment
    } else {
    }
  } catch (error) {
    console.error('[加载历史评分] 加载评分和评语失败:', error)
    ElMessage.warning('加载历史评分失败: ' + (error?.response?.data?.message || error?.message || '未知错误'))
  }
}

// 加载小组整体评语（现在由 tryPrefillExistingGrades 统一处理，保留此函数以防需要单独调用）
const loadGroupComment = async () => {
  // 评语现在由 tryPrefillExistingGrades 统一加载，此函数可以为空或移除
  // 保留空函数以避免调用错误
}

const exportReport = () => {
  ElMessage.success('导出检查报告成功')
  // 实现导出逻辑
}

const normalizeFileUrl = (u) => {
  if (!u) return ''
  const s = String(u)
  if (/^https?:\/\//i.test(s) || s.startsWith('data:') || s.startsWith('blob:')) return s
  // 统一转为 /uploads/... 再拼接后端文件域
  if (s.startsWith('/uploads/')) return `${FILE_BASE}${s}`
  if (s.startsWith('uploads/')) return `${FILE_BASE}/${s}`
  return s
}

// 将任意对象/字符串的附件条目规范为 { name, url }
const normalizeAttachmentEntry = (p) => {
  try {
    if (p == null) return null
    if (typeof p === 'string') {
      const url = normalizeFileUrl(p)
      const name = url ? url.split('/').pop() : '附件'
      return { name: String(name || '附件'), url }
    }
    if (typeof p === 'object') {
      let url = p.url || p.fileUrl || p.path || p.filePath || p.value || p.src
      if (url && typeof url === 'object') url = url.url || url.href || ''
      url = url ? normalizeFileUrl(String(url)) : ''
      let name = p.name || p.fileName || p.filename || p.originalName || p.originalFilename
      if (!name && url) name = url.split('/').pop()
      return { name: String(name || '附件'), url }
    }
  } catch {}
  return null
}

// 解析后端字段 submissionFiles，兼容字符串/数组/对象
const parseAttachments = (raw) => {
  try {
    if (!raw) return []
    let arr = []
    if (Array.isArray(raw)) arr = raw
    else if (typeof raw === 'string') {
      try {
        const json = JSON.parse(raw)
        if (Array.isArray(json)) arr = json
        else if (json) arr = [json]
      } catch {
        arr = [raw]
      }
    } else if (typeof raw === 'object') {
      arr = [raw]
    }
    return arr.map(normalizeAttachmentEntry).filter(x => x && x.url)
  } catch { return [] }
}

const viewPersonalDetails = (row) => {
  selectedPersonal.value = row
  personalGrading.score = row.score != null ? row.score : null
  personalGrading.feedback = ''
  personalDialogVisible.value = true
}

const submitPersonalGrade = async () => {
  try {
    if (!selectedPersonal.value) return
    if (personalGrading.score == null) { ElMessage.error('请先输入分数'); return }
    const teacherId = Number(localStorage.getItem('teacherId') || localStorage.getItem('userId') || 0)
    const form = new URLSearchParams()
    form.append('assignmentId', String(assignmentId))
    form.append('studentId', String(selectedPersonal.value.studentId))
    form.append('score', String(personalGrading.score))
    if (personalGrading.feedback) form.append('feedback', personalGrading.feedback)
    if (teacherId) form.append('gradedBy', String(teacherId))
    await api.post('/personal-submission/grade', form)
    // 更新本地
    selectedPersonal.value.score = Number(personalGrading.score)
    selectedPersonal.value.status = 'graded'
    const idx = personalSubmissions.value.findIndex(p => Number(p.studentId) === Number(selectedPersonal.value.studentId))
    if (idx >= 0) personalSubmissions.value[idx] = { ...personalSubmissions.value[idx], score: Number(personalGrading.score), status: 'graded' }
    ElMessage.success('评分成功')
    personalDialogVisible.value = false
  } catch (e) {
    ElMessage.error('评分失败')
  }
}

// 拉取作业详情
const fetchAssignment = async () => {
  try {
    const res = await api.get('/teacherAssignments')
    const raw = res?.data
    const list = Array.isArray(raw?.data) ? raw.data : []
    const found = list.find(a => Number(a.assignmentId) === Number(assignmentId))
    if (found) {
      assignmentTitle.value = found.assignmentName || '作品检查'
      deadline.value = found.dueDate || ''
      courseName.value = found.className || ''
      currentCourseId.value = found.courseId || null
    }
  } catch (e) {
    ElMessage.error('获取检查信息失败')
  }
}

// 拉取分组列表（按课程）
const fetchGroups = async () => {
  try {
    // 拉取全部分组（后端暂未提供按课程筛选）
    const resp = await api.get('/student-group')
    const raw = resp?.data
    const list = Array.isArray(raw?.data) ? raw.data : (Array.isArray(raw) ? raw : [])
    const normalized = list.map(g => {
      const id = g.id || g.groupId || g.group_id
      const name = g.name || g.groupName || `分组#${id ?? ''}`
      const gmRaw = Array.isArray(g.groupMemberList) ? g.groupMemberList : (Array.isArray(g.memberList) ? g.memberList : [])
      const memberObjects = (gmRaw.length > 0 ? gmRaw.map(m => ({
        studentId: m.studentId || m.student_id || m.id,
        name: m.studentName || m.name || m.username || ''
      })) : []).filter(x => x && x.name)
      const members = memberObjects.map(m => m.name)
      const leader = gmRaw.find(m => (m.role === 'leader' || m.role === 'LEADER'))
      const leaderName = g.leaderName || (leader?.name) || (leader?.studentName) || '未知'
      return {
        id,
        groupName: name,
        leaderName,
        members,
        hasGrades: false,
        memberObjects,
        submitTime: null,
        status: '未提交',
        checkStatus: '未提交',
        score: null,
        content: '',
        attachments: []
      }
    })
    groups.value = normalized
    totalGroups.value = normalized.length
    submittedCount.value = normalized.filter(g => g.submitTime).length
  } catch (e) {
    groups.value = []
    totalGroups.value = 0
    submittedCount.value = 0
  }
}

// 拉取小组提交并融合到 groups
const fetchGroupSubmissions = async () => {
  try {
    const resp = await api.get(`/submission/${assignmentId}`)
    const raw = resp?.data
    const list = Array.isArray(raw?.data) ? raw.data : []
    // 以 group_id 对齐
    const map = new Map()
    list.forEach(s => {
      const gid = s.groupId || s.group_id
      if (!gid) return
      map.set(Number(gid), s)
    })
    groups.value = (groups.value || []).map(g => {
      const s = map.get(Number(g.id))
      if (!s) return g
      const attachments = parseAttachments(s.submissionFiles)
      return {
        ...g,
        submissionId: s.submissionId || s.submission_id,
        submitTime: s.submittedAt || s.submitted_at || g.submitTime,
        status: '已提交',
        checkStatus: g.checkStatus === '已检查' ? '已检查' : '待检查',
        content: s.submissionContent || s.submission_content || g.content,
        attachments
      }
    })
    submittedCount.value = groups.value.filter(x => !!x.submitTime).length
  } catch (e) {
    // 忽略错误，保持现状
  }
}

onMounted(async () => {
  await fetchAssignment()
  await fetchGroups()
  // 结合后端小组提交列表，填充每个分组的提交状态
  await fetchGroupSubmissions()
  // 刷新：根据已存在的成员评分，标记"已检查/已评分"并计算均分
  await (async () => {
    try {
      const tasks = (groups.value || [])
        .filter(g => g && (g.submissionId || g.submission_id))
        .map(async (g) => {
          const sid = g.submissionId || g.submission_id
          try {
            const resp = await api.get(`/grading/group/${sid}`)
            const raw = resp?.data
            const data = raw?.data || raw
            // 新版本返回格式：{ memberScores: [...], groupComment: "..." }
            const list = Array.isArray(data?.memberScores) ? data.memberScores : (Array.isArray(data) ? data : [])
            if (!Array.isArray(list)) return
            const numericScores = list
              .map(it => it && it.score != null ? Number(it.score) : null)
              .filter(v => v != null && !isNaN(v))
            if (list.length > 0 || numericScores.length > 0) {
              g.hasGrades = true
              g.checkStatus = '已检查'
              if (numericScores.length > 0) {
                const avg = Math.round(numericScores.reduce((a, b) => a + b, 0) / numericScores.length)
                g.score = avg
              }
            }
          } catch {}
        })
      await Promise.allSettled(tasks)
    } catch {}
  })()
  // 拉取个人提交
  try {
    const res = await api.get('/personal-submission/by-assignment', { params: { assignmentId }, headers: {} })
    const raw = res?.data
    const list = Array.isArray(raw?.data) ? raw.data : []
    // 解析 JSON 数组字段 submissionFiles，映射为 {name,url}
    personalSubmissions.value = list.map(it => ({
      studentId: it.studentId,
      submittedAt: it.submittedAt,
      status: it.status,
      score: it.score,
      submissionContent: it.submissionContent,
      files: parseAttachments(it.submissionFiles)
    }))
  } catch (e) {
    // 尝试使用绝对后端基址作为降级
    try {
      const token = localStorage.getItem('token') || localStorage.getItem('userToken') || ''
      const res2 = await axios.get(`${API_BASE}/personal-submission/by-assignment`, { params: { assignmentId }, headers: token ? { Authorization: `Bearer ${token}` } : {} })
      const raw2 = res2?.data
      const list2 = Array.isArray(raw2?.data) ? raw2.data : []
      personalSubmissions.value = list2.map(it => ({
        studentId: it.studentId,
        submittedAt: it.submittedAt,
        status: it.status,
        score: it.score,
        submissionContent: it.submissionContent,
        files: parseAttachments(it.submissionFiles)
      }))
    } catch {
      personalSubmissions.value = []
    }
  }
  // 根据路由参数自动打开小组/成员详情
  const q = route.query || {}
  const gid = Number(q.groupId)
  const member = (q.member || q.studentName || '').toString().trim()
  if (member) highlightedMember.value = member
  const tryAutoOpen = () => {
    if (Number.isFinite(gid)) {
      const g = (groups.value || []).find(x => Number(x.id) === gid)
      if (g) viewGroupDetails(g)
    }
  }
  if ((groups.value || []).length > 0) tryAutoOpen()
  else {
    const stop = watch(groups, (nv) => {
      if (Array.isArray(nv) && nv.length > 0) { tryAutoOpen(); stop && stop() }
    }, { immediate: false })
  }
})
</script>

<style scoped>
.assignments-check {
  margin: -30px; /* Offset TeacherLayout's padding */
  width: calc(100% + 60px); /* Re-calculate width */
  padding: 0;
  background: #f3f4f6;
  min-height: calc(100vh - 60px); /* Adjust min-height */
  overflow-x: hidden;
}

/* Header White Styles */
.check-header-white {
  position: relative;
  background: white;
  padding: 24px 32px 24px 32px; /* 确保有足够的右侧和左侧padding */
  border-bottom: 1px solid #e5e7eb;
  margin-bottom: 24px;
  z-index: 1; /* 确保在侧边栏之上 */
}

.header-top {
  margin-bottom: 16px;
}

.back-link {
  color: #6b7280 !important;
  font-weight: 500;
  padding: 0;
}
.back-link:hover {
  color: #111827 !important;
}

.header-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 12px 0;
  color: #1f2937; /* Dark text */
}

.meta-tags {
  display: flex;
  gap: 12px;
}

.meta-tag {
  font-weight: 500;
}

/* Content Area */
.check-content-premium {
  padding: 0 32px 32px;
  margin-top: 0; /* Reset negative margin */
  position: relative;
  z-index: 10;
}

/* Stats Cards */
.stats-overview-premium {
  margin-bottom: 30px;
}

.stat-card-premium {
  height: 100px;
  background: white;
  border: none;
  border-radius: 12px;
  display: flex;
  align-items: center;
  transition: all 0.3s ease;
  overflow: hidden;
  position: relative;
}

.stat-card-premium:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
}

.stat-card-premium :deep(.el-card__body) {
  width: 100%;
  display: flex;
  align-items: center;
  padding: 20px !important;
}

.stat-icon-wrapper {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-right: 16px;
  flex-shrink: 0;
}

.submitted-card .stat-icon-wrapper { background: #eff6ff; color: #3b82f6; }
.pending-card .stat-icon-wrapper { background: #fef2f2; color: #ef4444; }
.graded-card .stat-icon-wrapper { background: #f0fdf4; color: #10b981; }
.progress-card .stat-icon-wrapper { background: #fdf2f8; color: #db2777; }

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 800;
  color: #111827;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
  margin-top: 4px;
}

/* Progress Card needs special styling due to different structure */
.progress-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}
.stat-value-small {
  font-weight: 700;
  color: #111827;
}

/* Table Area */
.groups-list {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06);
}

.groups-list h3 {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 20px;
  padding-left: 10px;
  border-left: 4px solid #3b82f6;
}

/* Group Avatar */
.group-name-cell {
  display: flex;
  align-items: center;
}
.group-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 14px;
  margin-right: 12px;
  flex-shrink: 0;
}
.group-name-text {
  font-weight: 500;
  color: #374151;
}

.leader-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.leader-icon {
  color: #f59e0b;
  font-size: 12px;
}

/* Dialog Stylings */
.content-preview {
  background: #f9fafb;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  margin-bottom: 24px;
  min-height: 80px;
}

.attachment-item {
  background: #fff;
  border: 1px solid #f3f4f6;
  border-radius: 6px;
  padding: 10px 16px;
  margin-bottom: 8px;
  transition: all 0.2s;
}
.attachment-item:hover {
  background: #f0f9ff;
  border-color: #bae6fd;
}
.attachment-item i {
  color: #3b82f6;
}

.grading-form {
  background: #ffffff;
  padding: 24px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

/* Custom Scrollbar for big lists */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}
::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 4px;
}
::-webkit-scrollbar-track {
  background: #f3f4f6;
}

:deep(.centered-dialog .el-dialog) {
  border-radius: 16px;
  overflow: hidden;
}
:deep(.el-dialog__header) {
  margin: 0;
  padding: 20px 24px;
  border-bottom: 1px solid #f3f4f6;
}
:deep(.el-dialog__footer) {
  padding: 20px 24px;
  background: #f9fafb;
  border-top: 1px solid #f3f4f6;
}

/* Responsive adjustment for small screens */
@media (max-width: 768px) {
  .check-header-premium {
    padding: 20px;
    padding-bottom: 40px;
  }
  .check-content-premium {
    padding: 0 16px 20px;
  }
}
</style>