<template>
  <div class="assignments-check">
    <div class="check-header">
      <div class="header-left">
        <h2>检查情况：{{ assignmentTitle }}</h2>
        <p>班级：{{ courseName }} | 截止日期：{{ formatDate(deadline) }}</p>
        <p>完成进度：{{ submittedCount }}/{{ totalGroups }} 个小组</p>
      </div>
      <div class="header-right">
        <el-button @click="$router.push('/teacher/assignments/list')">
          <i class="fas fa-arrow-left"></i> 返回列表
        </el-button>
      </div>
    </div>

    <div class="check-content">
      <!-- 统计概览 -->
      <div class="stats-overview">
        <el-row :gutter="20">
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-value">{{ submittedCount }}</div>
              <div class="stat-label">已提交</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-value">{{ totalGroups - submittedCount }}</div>
              <div class="stat-label">未提交</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-value">{{ completedCount }}</div>
              <div class="stat-label">已完成检查</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-value">{{ Math.round((submittedCount / totalGroups) * 100) }}%</div>
              <div class="stat-label">提交率</div>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 小组列表 -->
      <div class="groups-list">
        <h3>小组检查情况</h3>
        <el-table :data="groups" style="width: 100%" stripe>
          <el-table-column prop="groupName" label="小组名称" width="150" align="center" />
          <el-table-column prop="leaderName" label="组长" width="120" align="center" />
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
              <el-tag :type="getCheckStatusType(scope.row.checkStatus)" size="small">
                {{ scope.row.checkStatus }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="score" label="评分" width="100" align="center">
            <template #default="scope">
              {{ scope.row.score !== null ? scope.row.score : '未评分' }}
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

      <!-- 个人提交列表 -->
      <div class="groups-list" style="margin-top:24px;">
        <h3>个人提交情况</h3>
        <el-table :data="personalSubmissions" style="width: 100%" stripe>
          <el-table-column prop="studentId" label="学生ID" width="120" align="center" />
          <el-table-column prop="submittedAt" label="提交时间" width="180" align="center">
            <template #default="scope">
              {{ scope.row.submittedAt ? formatDateTime(scope.row.submittedAt) : '未提交' }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="提交状态" width="120" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.status === 'submitted' ? 'success' : (scope.row.status === 'graded' ? 'primary' : 'info')">
                {{ scope.row.status || '—' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="score" label="评分" width="100" align="center">
            <template #default="scope">
              {{ scope.row.score != null ? scope.row.score : '未评分' }}
            </template>
          </el-table-column>
          <el-table-column label="附件" min-width="240" align="left">
            <template #default="scope">
              <div style="display:flex;flex-wrap:wrap;gap:8px;">
                <el-tag v-for="(f,idx) in scope.row.files" :key="idx" size="small" type="info">
                  <a :href="normalizeFileUrl(f.url)" target="_blank" style="text-decoration:none;color:inherit;">{{ f.name }}</a>
                </el-tag>
                <span v-if="!scope.row.files || scope.row.files.length === 0">—</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" align="center">
            <template #default="scope">
              <el-button size="small" @click="viewPersonalDetails(scope.row)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 检查详情对话框 -->
      <el-dialog
        v-model="detailDialogVisible"
        :title="`${selectedGroup?.groupName} - 检查详情`"
        width="80%"
        top="50px"
        class="centered-dialog"
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
                <el-button link type="primary" @click="downloadFile(file)">下载</el-button>
              </div>
              <p v-if="selectedGroup.attachments.length === 0">该小组未提交附件</p>
            </div>
          </div>

          <div class="grading-form" style="margin-top: 20px;">
            <h4>检查评分</h4>
            <el-form :model="gradingForm" label-width="80px">
              <el-form-item label="得分">
                <el-input-number
                  v-model="gradingForm.score"
                  :min="0"
                  :max="100"
                  placeholder="请输入得分"
                />
                <span class="score-total">/ 100</span>
              </el-form-item>
              <el-form-item label="评语">
                <el-input
                  v-model="gradingForm.comment"
                  type="textarea"
                  :rows="4"
                  placeholder="请输入评语"
                />
              </el-form-item>
              <el-form-item label="检查结果">
                <el-radio-group v-model="gradingForm.result">
                  <el-radio label="通过">通过</el-radio>
                  <el-radio label="需修改">需修改</el-radio>
                  <el-radio label="不通过">不通过</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-form>
          </div>
        </div>

        <template #footer>
          <el-button @click="detailDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitCheck">提交检查</el-button>
        </template>
      </el-dialog>

      <!-- 个人提交详情对话框 -->
      <el-dialog
        v-model="personalDialogVisible"
        :title="`个人提交详情 - 学生ID: ${selectedPersonal?.studentId || ''}`"
        width="60%"
        top="80px"
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
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const assignmentId = route.params.id

// axios 实例
const API_BASE = (import.meta?.env?.VITE_API_BASE_URL || 'http://localhost:9999/api')
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

// 评分表单
const gradingForm = reactive({
  score: null,
  comment: '',
  result: '通过'
})

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
  return new Date(dateString).toLocaleDateString('zh-CN')
}

const formatDateTime = (dateTimeString) => {
  return new Date(dateTimeString).toLocaleString('zh-CN')
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

const viewGroupDetails = (group) => {
  selectedGroup.value = group
  // 初始化表单数据
  gradingForm.score = group.score
  gradingForm.comment = ''
  gradingForm.result = '通过'
  detailDialogVisible.value = true
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

const submitCheck = () => {
  if (gradingForm.score === null) {
    ElMessage.error('请先输入分数')
    return
  }

  // 更新小组检查状态
  const group = groups.value.find(g => g.id === selectedGroup.value.id)
  if (group) {
    group.score = gradingForm.score
    group.checkStatus = '已检查'
    ElMessage.success('检查完成')
    detailDialogVisible.value = false
  }
}

const exportReport = () => {
  ElMessage.success('导出检查报告成功')
  // 实现导出逻辑
}

const normalizeFileUrl = (u) => {
  if (!u) return ''
  const s = String(u)
  if (/^https?:\/\//i.test(s) || s.startsWith('data:') || s.startsWith('blob:')) return s
  if (s.startsWith('/uploads/')) return s
  if (s.startsWith('uploads/')) return `/${s}`
  return s
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
  if (!currentCourseId.value) { groups.value = []; totalGroups.value = 0; submittedCount.value = 0; return }
  try {
    const params = { courseId: currentCourseId.value }
    // 后端该接口为 POST，直接使用 POST 以避免 405
    const resp = await api.post('/student-group/approvalStatus', params)
    const raw = resp?.data
    const list = Array.isArray(raw?.data) ? raw.data : (Array.isArray(raw) ? raw : [])
    const normalized = list.map(g => {
      const id = g.id || g.groupId || g.group_id
      const name = g.name || g.groupName || `分组#${id ?? ''}`
      const gmRaw = Array.isArray(g.groupMemberList) ? g.groupMemberList : (Array.isArray(g.memberList) ? g.memberList : [])
      const members = (gmRaw.length > 0 ? gmRaw.map(m => m.studentName || m.name || m.username || '') : []).filter(Boolean)
      const leader = gmRaw.find(m => m.role === 'leader')
      const leaderName = g.leaderName || (leader?.name) || (leader?.studentName) || '未知'
      return {
        id,
        groupName: name,
        leaderName,
        members,
        submitTime: null,      // 尚未接入分组提交接口，先置空
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

onMounted(async () => {
  await fetchAssignment()
  await fetchGroups()
  // 拉取个人提交
  try {
    const res = await api.get('/personal-submission/by-assignment', { params: { assignmentId }, headers: {} })
    const raw = res?.data
    const list = Array.isArray(raw?.data) ? raw.data : []
    // 解析 JSON 数组字段 submissionFiles，映射为 {name,url}
    personalSubmissions.value = list.map(it => {
      let files = []
      try {
        if (it.submissionFiles) {
          const arr = JSON.parse(it.submissionFiles)
          if (Array.isArray(arr)) files = arr.map((p) => ({ name: p.split('/').pop(), url: p }))
        }
      } catch {}
      return {
        studentId: it.studentId,
        submittedAt: it.submittedAt,
        status: it.status,
        score: it.score,
        submissionContent: it.submissionContent,
        files
      }
    })
  } catch (e) {
    // 尝试使用绝对后端基址作为降级
    try {
      const fallbackBase = (window?.location?.port === '5173' || window?.location?.port === '4173') ? 'http://localhost:9999/api' : API_BASE
      const token = localStorage.getItem('token') || localStorage.getItem('userToken') || ''
      const res2 = await axios.get(`${fallbackBase}/personal-submission/by-assignment`, { params: { assignmentId }, headers: token ? { Authorization: `Bearer ${token}` } : {} })
      const raw2 = res2?.data
      const list2 = Array.isArray(raw2?.data) ? raw2.data : []
      personalSubmissions.value = list2.map(it => {
        let files = []
        try {
          if (it.submissionFiles) {
            const arr = JSON.parse(it.submissionFiles)
            if (Array.isArray(arr)) files = arr.map((p) => ({ name: p.split('/').pop(), url: p }))
          }
        } catch {}
        return {
          studentId: it.studentId,
          submittedAt: it.submittedAt,
          status: it.status,
          score: it.score,
          submissionContent: it.submissionContent,
          files
        }
      })
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
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.check-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eaeaea;
}

.header-left h2 {
  margin-bottom: 8px;
  color: #1f2937;
}

.header-left p {
  color: #6b7280;
  font-size: 14px;
  margin: 4px 0;
}

.stats-overview {
  margin-bottom: 30px;
}

.stat-card {
  background: #f8fafc;
  padding: 20px;
  border-radius: 8px;
  text-align: center;
  border-left: 4px solid #3b82f6;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #1f2937;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
}

.groups-list h3 {
  margin-bottom: 15px;
  color: #1f2937;
}

.content-preview {
  background: #f9fafb;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
  min-height: 60px;
}

.attachment-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f3f4f6;
}

.attachment-item i {
  margin-right: 10px;
  color: #6b7280;
}

.attachment-item span {
  flex: 1;
}

.grading-form {
  background: #f9fafb;
  padding: 20px;
  border-radius: 8px;
}

.grading-form h4 {
  margin-bottom: 15px;
  color: #374151;
}

.score-total {
  margin-left: 10px;
  color: #6b7280;
}

:deep(.centered-dialog .el-dialog) {
  margin: 0 auto !important;
  max-height: calc(100vh - 100px);
  display: flex;
  flex-direction: column;
}

:deep(.centered-dialog .el-dialog__body) {
  flex: 1;
  overflow-y: auto;
}
</style>
