<template>
  <div class="header">
    <h1 class="page-title">学习分组</h1>
    <div class="header-right">
      <span
          class="status-chip"
          :class="{
                'status-not-formed': groupStatus === 'none',
                'status-pending': groupStatus === 'pending',
                'status-approved': groupStatus === 'approved'
              }"
      >
              我的组队状态：{{ groupStatus === 'none' ? '未组队' : (groupStatus === 'pending' ? '待审批' : '已组队') }}
            </span>
      <span v-if="isSelecting && groupStatus === 'none'" class="selected-count">已选择 {{ selectedMembers.length }} 人</span>
    </div>
  </div>


  <div class="filters-row block-section" v-if="(groupStatus === 'none') && (!isCreating || isSelecting)">
    <el-input
        v-model="keyword"
        placeholder="搜索学生姓名或学号..."
        clearable
        prefix-icon="el-icon-search"
        class="keyword-input"
    />
    <el-select v-model="statusFilter" clearable placeholder="组队状态" class="status-select">
      <el-option label="全部" :value="''" />
      <el-option label="未组队" value="available" />
      <el-option label="已组队" value="unavailable" />
    </el-select>
    <div class="actions-spacer"></div>
    <div v-if="groupStatus === 'none' && (!isCreating || isSelecting)" class="header-actions">
      <span v-if="isCreating && isSelecting" class="select-hint">请选择 2 ~ 4 名组员</span>
      <el-button
          type="primary"
          @click="headerPrimaryAction"
      >
        {{ !isCreating ? '新建小组' : '选择完成' }}
      </el-button>
    </div>
  </div>

  <div class="block-section" v-if="(groupStatus === 'none') && (!isCreating || isSelecting)">
    <el-table
        :data="filteredStudents"
        stripe
        border
        class="full-width-table"
        @row-click="onRowClick"
        :row-class-name="rowClassName"
        empty-text="暂无学生"
    >
      <el-table-column type="index" label="序号" width="64" align="left" />
      <el-table-column label="学号" width="160">
        <template #default="{ row }">
          <span class="col-sid">{{ row.sid || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="姓名" width="140">
        <template #default="{ row }">
          <span class="col-name">
            {{ row.name }}
            <span v-if="isSelf(row)" class="self-mark">本人</span>
          </span>
        </template>
      </el-table-column>
      <el-table-column label="组队状态" width="160">
        <template #default="{ row }">
            <span class="status-chip" :class="row.status === 'available' ? 'chip-red' : 'chip-green'">
              {{ row.status === 'available' ? '未组队' : '已组队' }}
            </span>
        </template>
      </el-table-column>
      <el-table-column label="手机号" min-width="160">
        <template #default="{ row }">
          <span class="col-phone">{{ row.phone || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="邮箱" min-width="220">
        <template #default="{ row }">
          <span class="col-email">{{ row.email || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="isCreating && isSelecting" label="操作" width="160" align="center">
        <template #default="{ row }">
          <el-button v-if="row.status !== 'available'" size="small" disabled>已组队</el-button>
          <el-button v-else-if="isSelected(row.id)" size="small" type="danger" plain @click.stop="toggleSelect(row)">取消</el-button>
          <el-button v-else size="small" type="primary" plain @click.stop="toggleSelect(row)">选择</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <div class="task-inputs block-section" v-if="isCreating && !isSelecting">
    <div class="task-header">
      <h4 class="task-title">任务分工</h4>
    </div>
    <div class="group-name-row">
      <label class="group-name-label">我的小组名称：</label>
      <input
          class="group-name-input"
          type="text"
          v-model="groupName"
          placeholder="请输入小组名称"
          ref="groupNameInputRef"
      />
    </div>
    <div class="task-input-row">
      <span class="task-name">小组任务描述：</span>
      <textarea
          class="task-text"
          v-model="taskDescription"
          placeholder="请用一段话描述本小组的任务分工与目标（示例：张三-资料收集，李四-文档整理，王五-PPT与汇报）"
          rows="3"
      ></textarea>
    </div>

    <div class="members-chips">
      <span class="member-chip" v-for="m in membersForTasks" :key="m.id">
        <span class="member-name">{{ m.name }}</span>
        <span v-if="m.id === currentUserId" class="leader-mark">组长</span>
        <span v-else class="member-mark">组员</span>
      </span>
    </div>
    <div class="btn-row block-section" v-if="isCreating && !isSelecting">
      <button class="btn btn-yellow" data-tip="清空并重新选择组员" @click="resetSelection">重新选择</button>
      <button class="btn btn-yellow" data-tip="继续挑选更多组员" @click="againSelect">选择队员</button>
      <div class="tooltip" data-tip="组员最多4人，最少2人">
        <button class="btn btn-blue" :disabled="!canSubmit || !isCreating" @click="submitGroup">
          提交小组申请 ({{ selectedMembers.length }}/4)
        </button>
      </div>
      <button class="btn btn-yellow" @click="resetAllAndExit">重置</button>
    </div>
  </div>

  <div v-if="groupStatus !== 'none'" class="group-summary block-section">
    <!-- <h4 class="group-summary-title">我的小组</h4> -->
    <div class="group-summary-card">
      <div class="group-summary-header">
        <span class="group-summary-label">我的小组名称：</span>
        <span>{{ createdGroup?.groupName || groupName || '—' }}</span>
        <span class="status-chip status-pending" v-if="groupStatus==='pending'">审批中</span>
        <span class="status-chip status-approved" v-else>已组队</span>
      </div>
      <div class="group-summary-members">
        <span class="group-summary-label">成员：</span>
        <span>{{ [createdGroup?.leaderName, ...(createdGroup?.memberNames||[])] .filter(Boolean).join('、') }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, nextTick } from 'vue'
import { getStudentsByGrade, createStudentGroup } from '@/services/groupApi'
import { fetchHomeCourses } from '@/services/homeCoursesApi'

const allStudents = ref([])
const isCreating = ref(false)
const isSelecting = ref(false)
const uiStateStorageKey = 'student_groups_ui_state'
const GROUP_STATUS_KEY = 'student_group_status'
const GROUP_INFO_KEY = 'student_group_info'
onMounted(async () => {
  try {
    const ui = JSON.parse(localStorage.getItem(uiStateStorageKey) || 'null')
    if (ui && typeof ui === 'object') {
      isCreating.value = !!ui.creating
      if (ui.creating) {
        isSelecting.value = false
        localStorage.setItem(uiStateStorageKey, JSON.stringify({ creating: true, selecting: false }))
      } else {
        isSelecting.value = !!ui.selecting
      }
    }
  } catch {}
  // 恢复小组状态与信息（若存在）
  try {
    const st = localStorage.getItem(GROUP_STATUS_KEY)
    if (st === 'pending' || st === 'approved') {
      groupStatus.value = st
      const info = JSON.parse(localStorage.getItem(GROUP_INFO_KEY) || 'null')
      if (info && typeof info === 'object') {
        createdGroup.value = info
        groupName.value = info.groupName || groupName.value
      }
    }
  } catch (e) { console.error(e) }
  await Promise.all([loadStudents(), cacheTeacherIdFromHome()])
})

async function loadStudents() {
  try {
    const className =  localStorage.getItem("className")
    const list = await getStudentsByGrade(className)
    console.log('list', list)
    allStudents.value = (list || []).map((s, i) => ({
      id: s.id || s.studentId || s.sid || i + 1,
      name: s.name ||'-',
      sid: s.sid || s.studentId || '-',
      status: (s.grouped === true || s.status === 'grouped') ? 'unavailable' : 'available'
    }))
  } catch (e) {
    console.error(e)
    allStudents.value = []
  }
}

// 从首页课程接口获取教师ID，缓存在本地，供分组提交使用
async function cacheTeacherIdFromHome() {
  try {
    const list = await fetchHomeCourses()
    const first = Array.isArray(list) ? list.find(Boolean) : null
    const tid = first?.teacherId || null
    if (tid != null) {
      localStorage.setItem('teacherId', String(tid))
    }
  } catch (e) { console.error(e) }
}

const keyword = ref('')
const groupStatus = ref('none')
const selectedIds = ref([])
const statusFilter = ref('')
const tasks = ref({})
const taskDescription = ref('')
const groupName = ref('')

const currentUserName = ref('')
try {
  const saved = JSON.parse(localStorage.getItem('currentUser') || 'null')
  currentUserName.value = saved?.name || ''
} catch { currentUserName.value = '' }

const filteredStudents = computed(() => {
  const k = keyword.value.trim()
  return (allStudents.value || [])
      .filter(s => {
        if (statusFilter.value === 'available') return s.status === 'available'
        if (statusFilter.value === 'unavailable') return s.status !== 'available'
        return true
      })
      .filter(s => {
        if (!k) return true
        return s.name.includes(k) || s.sid.includes(k)
      })
})

const selectedMembers = computed(() =>
    allStudents.value.filter(s => selectedIds.value.includes(s.id))
)

const currentUserObj = computed(() => {
  const found = (allStudents.value || []).find(s => s.name === (currentUserName.value || ''))
  if (found) return found
  if (currentUserName.value) {
    return { id: '__self__', name: currentUserName.value, sid: '-', status: 'available' }
  }
  return null
})

const currentUserId = computed(() => currentUserObj.value?.id)

const membersForTasks = computed(() => {
  const leader = currentUserObj.value ? [currentUserObj.value] : []
  if (selectedMembers.value.length === 0) return leader
  const others = selectedMembers.value.filter(m => m.id !== currentUserId.value)
  return [...leader, ...others]
})

const canSubmit = computed(() => {
  const count = selectedMembers.value.length
  return count >= 2 && count <= 4
})

// 顶部主按钮逻辑：新建小组 -> 选择组员 -> 选择完成
function headerPrimaryAction() {
  if (!isCreating.value) {
    // 第一次点击：进入创建流程，展示任务分工，隐藏学生列表
    isCreating.value = true
    isSelecting.value = false
    localStorage.setItem(uiStateStorageKey, JSON.stringify({ creating: true, selecting: false }))
    nextTick(() => {
      document.querySelector('.task-inputs')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
      if (groupNameInputRef.value && groupNameInputRef.value.focus) {
        groupNameInputRef.value.focus()
      }
    })
    return
  }
  if (isCreating.value && !isSelecting.value) {
    // 第二次点击：进入选择组员模式，展示可选学生列表
    isSelecting.value = true
    localStorage.setItem(uiStateStorageKey, JSON.stringify({ creating: true, selecting: true }))
    return
  }
  if (isCreating.value && isSelecting.value) {
    // 第三次点击：完成选择，返回任务分工
    isSelecting.value = false
    // 固化需求：完成后仍停留在任务分工，之后跳转回来也维持任务分工
    localStorage.setItem(uiStateStorageKey, JSON.stringify({ creating: true, selecting: false }))
    nextTick(() => {
      document.querySelector('.task-inputs')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    })
  }
}

function toggleSelect(stu) {
  if (stu.status !== 'available') return
  if (isSelf(stu)) return
  const idx = selectedIds.value.indexOf(stu.id)
  if (idx >= 0) {
    selectedIds.value.splice(idx, 1)
    const copy = { ...tasks.value }
    delete copy[stu.id]
    tasks.value = copy
  } else {
    if (selectedIds.value.length >= 4) return
    selectedIds.value.push(stu.id)
    if (!tasks.value[stu.id]) {
      tasks.value = { ...tasks.value, [stu.id]: '' }
    }
  }
}

function isSelected(id) {
  return selectedIds.value.includes(id)
}

function isSelf(stu) {
  // 以学号优先比对，其次比对 id
  try {
    const saved = JSON.parse(localStorage.getItem('currentUser') || 'null')
    const mySid = saved?.sid || saved?.studentNo || saved?.studentId || null
    if (mySid && stu?.sid) return String(stu.sid) === String(mySid)
  } catch (e) { console.error(e) }
  return stu?.id === currentUserId.value
}

function onRowClick(row) {
  if (!isCreating.value || !isSelecting.value) return
  toggleSelect(row)
}

function rowClassName({ row }) {
  if (row.status !== 'available') return 'disabled'
  if (selectedIds.value.includes(row.id)) return 'selected'
  return ''
}

function resetSelection() {
  selectedIds.value = []
  const leaderKey = currentUserId.value
  const leaderVal = tasks.value[leaderKey] || ''
  tasks.value = leaderKey != null ? { [leaderKey]: leaderVal } : {}
  // 直接进入选择模式
  if (isCreating.value) {
    isSelecting.value = true
    localStorage.setItem(uiStateStorageKey, JSON.stringify({ creating: true, selecting: true }))
  }
}

// 选择队员：保留当前已选，直接进入选择模式
function againSelect() {
  if (!isCreating.value) return
  isSelecting.value = true
  localStorage.setItem(uiStateStorageKey, JSON.stringify({ creating: true, selecting: true }))
}

async function submitGroup() {
  if (!canSubmit.value) return
  try {
    const leader = currentUserObj.value
    const members = selectedMembers.value
    const payload = {
      groupName: groupName.value || `${leader?.name || '我的'}小组`,
      groupLeaderId: leader?.id,
      memberIds: members.map(m => m.id),
      teacherId: (() => {
        const v = localStorage.getItem('teacherId')
        return v ? Number(v) : undefined
      })(),
      taskDescription: taskDescription.value
    }
    const res = await createStudentGroup(payload)
    const code = Number(res?.code ?? res?.status ?? 0)
    if (code === 200) {
      groupStatus.value = 'pending'
      createdGroup.value = {
        groupName: payload.groupName,
        leaderName: leader?.name,
        memberNames: members.map(m => m.name)
      }
      // 持久化当前状态与信息
      try {
        localStorage.setItem(GROUP_STATUS_KEY, 'pending')
        localStorage.setItem(GROUP_INFO_KEY, JSON.stringify(createdGroup.value))
      } catch (e) { console.error(e) }
      isCreating.value = false
      isSelecting.value = false
      localStorage.setItem(uiStateStorageKey, JSON.stringify({ creating: false, selecting: false }))
      // 固定当前页面状态，无进一步跳转
    } else {
      alert(`提交失败：${res?.message || code || '未知错误'}`)
    }
  } catch (e) {
    alert('提交失败，请稍后再试')
  }
}

if (currentUserId.value != null && !tasks.value[currentUserId.value]) {
  tasks.value = { ...tasks.value, [currentUserId.value]: '' }
}

function createNewGroup() {
  selectedIds.value = []
  groupName.value = ''
  isCreating.value = true
  isSelecting.value = false
  localStorage.setItem(uiStateStorageKey, JSON.stringify({ creating: true, selecting: false }))
}

function resetAllAndExit() {
  selectedIds.value = []
  tasks.value = {}
  groupName.value = ''
  isCreating.value = false
  isSelecting.value = false
  localStorage.setItem(uiStateStorageKey, JSON.stringify({ creating: false, selecting: false }))
}

// 提交成功后用于展示的本地小组信息
const createdGroup = ref(null)
const groupNameInputRef = ref(null)
</script>

<style scoped>

.groups-page
.groups-page  {
  max-width: 1200px;
  margin: 0 auto;
}

.welcome {
  font-size: 18px;
  font-weight: bold;
  color: #2c3e50;
}

.groups-content {
  position: relative;
}
.block-section {
  background: transparent;
  margin-bottom: 12px;
}

.page-title {
  font-size: 24px;
  font-weight: bold;
  color: #2c3e50;
  margin-bottom: 0;
}

.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
  position: sticky;
  top: 0;
  z-index: 20;
  background: #ffffff;
  padding: 8px 0;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.select-hint {
  color: #6b7280;
  font-size: 14px;
}

.status-chip {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.search-box input {
  flex-grow: 1;
  border: none;
  background: transparent;
  padding: 10px;
  font-size: 16px;
  outline: none;
}

.search-box {
  display: flex;
  align-items: center;
  background: #e6efff;
  border: 1px solid #c7d2fe;
  border-radius: 10px;
  padding: 12px 16px;
  margin-bottom: 20px;
}
.search-box i {
  color: #2563eb;
  font-size: 20px;
  margin-right: 10px;
}

.filter-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: bold;
  color: #2c3e50;
}
.filter-group select, .filter-group input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.status-pending {
  background: #ffeaa7;
  color: #d35400;
}
.status-approved {
  background: #e8f5e9;
  color: #2e7d32;
}
.status-not-formed {
  background: #ffebee;
  color: #c62828;
}

.student-list {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin: 20px 0;
}

.student-table {
  margin: 20px 0;
}
.student-table table { width: 100%; border-collapse: separate; border-spacing: 0; background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden; }
.student-table thead th { text-align: left; background: #f3f4f6; color: #6b7280; padding: 14px 16px; font-weight: 600; border-bottom: 1px solid #e5e7eb; }
.student-table tbody td { padding: 14px 16px; border-bottom: 1px solid #e5e7eb; color: #4b5563; }
.student-table tbody td.col-index { color:#9ca3af; }
.student-table tbody td.col-sid { color:#1f2937; font-weight:600; }
.student-table tbody td.col-name { color:#1f2937; font-weight:600; }
.student-table tbody td.col-status { color:#4b5563; }
.student-table tbody td.col-phone { color:#374151; }
.student-table tbody td.col-email { color:#2563eb; }
.student-table tbody tr:hover { background: #f9fafb; cursor: pointer; }
.student-table tbody tr.selected { background: #eef2ff; }
.student-table tbody tr.disabled { color: #9ca3af; cursor: not-allowed; }
.status-chip { padding: 2px 10px; border-radius: 999px; font-size: 12px; font-weight: 700; }
.chip-red { background: #fee2e2; color: #b91c1c; }
.chip-green { background: #dcfce7; color: #15803d; }
.empty-cell { text-align: center; color: #9ca3af; padding: 20px 0; }

.task-inputs {
  background: #f8f9fa;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 22px;
  margin-top: 8px;
  position: relative;
  z-index: 10;
}
.task-inputs .task-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}
.group-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.group-name-label {
  width: 160px;
  text-align: right;
  color: #2c3e50;
  font-weight: 600;
}
.group-name-input {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
}
.task-inputs .task-title {
  display: block;
  width: 100%;
  text-align: center;
  font-size: 26px;
  line-height: 1.4;
  font-weight: 800;
  color: #111827;
  letter-spacing: 0.5px;
  margin: 4px 0 18px 0;
}
.task-inputs .task-title::after {
  content: '';
  display: block;
  width: 88px;
  height: 4px;
  border-radius: 999px;
  background: linear-gradient(90deg, #60a5fa 0%, #4f46e5 100%);
  margin: 8px auto 0;
}
.task-input-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.task-input-row .task-text::placeholder {
  color: #9ca3af;
}
.task-input-row:last-child {
  margin-bottom: 0;
}
.task-input-row textarea.task-text {
  width: 100%;
  min-height: 84px;
  resize: vertical;
}
.task-empty {
  text-align: center;
  color: #6b7280;
  padding: 8px 0;
}
.task-name {
  width: 160px;
  text-align: right;
  color: #2c3e50;
  font-weight: 600;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}
.task-text {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
}

.members-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 6px;
  justify-content: center;
}
.member-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: #1f2937;
}
.member-chip .leader-mark, .member-chip .member-mark {
  margin-left: 0;
}

.leader-mark {
  display: inline-block;
  background: #eef2ff;
  color: #2563eb;
  border: 1px solid #c7d2fe;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}
.member-mark {
  display: inline-block;
  background: #ecfeff;
  color: #0891b2;
  border: 1px solid #a5f3fc;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.self-mark {
  display: inline-block;
  margin-left: 6px;
  background: #fff7ed;
  color: #b45309;
  border: 1px solid #fdba74;
  padding: 2px 6px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.student-card {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 140px;
  background: #fff;
}

.student-card:hover {
  box-shadow: 0 5px 15px rgba(0,0,0,0.1);
}
.student-card.selected {
  border-color: #3498db;
  background: #ebf5fb;
}
.student-card.unavailable {
  opacity: 0.6;
  background: #f5f5f5;
  cursor: not-allowed;
}
.student-name {
  font-weight: bold;
  color: #2c3e50;
  margin-bottom: 5px;
}
.student-id {
  color: #7f8c8d;
  font-size: 14px;
}
.student-status {
  font-size: 13px;
  padding: 3px 8px;
  border-radius: 4px;
  display: inline-block;
  margin-top: 8px;
}
.status-available {
  background: #e8f5e9;
  color: #2e7d32;
}
.status-unavailable {
  background: #ffebee;
  color: #c62828;
}

.btn-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-top: 20px;
  flex-wrap: nowrap;
  padding: 0 8px;
}
.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: bold;
  transition: all 0.3s;
  min-width: 160px;
}
.btn-blue {
  background: #2563eb;
  color: #fff;
}
.btn-blue:hover {
  background: #1d4ed8;
}
.btn-blue:disabled {
  background: #93c5fd;
  cursor: not-allowed;
}

.btn-yellow {
  background: #fbbf24;
  color: #1f2937;
}
.btn-yellow:hover {
  background: #f59e0b;
}

.tooltip {
  position: relative;
  display: inline-block;
}
.tooltip::after {
  content: attr(data-tip);
  position: absolute;
  right: 0;
  bottom: calc(100% + 8px);
  background: rgba(0,0,0,0.8);
  color: #fff;
  padding: 6px 10px;
  border-radius: 6px;
  font-size: 12px;
  white-space: nowrap;
  opacity: 0;
  pointer-events: none;
  transform: translateY(4px);
  transition: opacity .2s ease, transform .2s ease;
}
.tooltip:hover::after {
  opacity: 1;
  transform: translateY(0);
}

.filters-row {
  display:flex;
  align-items:center;
  gap:12px;
  margin-bottom:12px;
}
.filters-row .keyword-input {
  max-width: 420px;
  width: 100%;
}
.filters-row .status-select {
  width: 160px;
}
.filters-row > .el-input {
  margin-right: 8px;
}
.filters-row > .el-select {
  margin-right: 8px;
}
.filters-row .actions-spacer {
  flex: 1;
}

.selected-count {
  margin-left: 12px;
  color: #6b7280;
  font-size: 14px;
  white-space: nowrap;
}

.full-width-table {
  width: 100%;
}

.group-summary {
  margin-top: 14px;
}
.group-summary-title {
  margin: 0 0 8px 0;
  color: #2c3e50;
}
.group-summary-card {
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
}
.group-summary-header {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.group-summary-label {
  font-weight: 600;
}
.group-summary-members {
  margin-top: 8px;
  color: #374151;
}

@media (max-width: 1200px) {
  .student-list {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media (max-width: 900px) {
  .student-list {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 600px) {
  .student-list {
    grid-template-columns: 1fr;
  }
}
</style>


