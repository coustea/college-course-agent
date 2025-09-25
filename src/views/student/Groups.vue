<template>
  <div class="welcome">
    <div class="title-row" v-show="!taskOnlyMode">
      <h1 class="page-title">学习分组</h1>
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
    </div>
    <div class="filters-row" v-show="!taskOnlyMode">
      <el-input
          v-model="keyword"
          placeholder="搜索学生姓名或学号..."
          clearable
          prefix-icon="el-icon-search"
          style="max-width: 420px; width: 100%"
      />
      <el-select v-model="statusFilter" clearable placeholder="组队状态" style="width: 160px">
        <el-option label="全部" :value="''" />
        <el-option label="未组队" value="available" />
        <el-option label="已组队" value="unavailable" />
      </el-select>
      <div class="actions-spacer"></div>
      <el-button type="primary" @click="createNewGroup">新建小组</el-button>
    </div>

    <el-table
        v-show="!taskOnlyMode"
        :data="filteredStudents"
        stripe
        border
        style="width: 100%"
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
          <span class="col-name">{{ row.name }}</span>
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
      <el-table-column label="操作" width="160" align="center">
        <template #default="{ row }">
          <el-button v-if="row.status !== 'available'" size="small" disabled>已组队</el-button>
          <el-button v-else-if="isSelected(row.id)" size="small" type="danger" plain @click.stop="toggleSelect(row)">取消</el-button>
          <el-button v-else size="small" type="primary" plain @click.stop="toggleSelect(row)">选择</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="task-inputs" v-if="membersForTasks.length">
      <div v-if="taskOnlyMode" style="display:flex; justify-content:flex-end; margin-bottom:8px;">
        <el-button type="primary" @click="exitTaskOnlyMode">选择队员</el-button>
      </div>
      <h4 class="task-title">任务分工</h4>
      <div class="group-name-row">
        <label class="group-name-label">我的小组名称：</label>
        <input
            class="group-name-input"
            type="text"
            v-model="groupName"
            placeholder="请输入小组名称"
        />
      </div>
      <div class="task-input-row" v-for="m in membersForTasks" :key="m.id">
          <span class="task-name">{{ m.name }}
            <span v-if="m.id === currentUserId" class="leader-mark">组长</span>
            <span v-else class="member-mark">组员</span>
            ：
          </span>
        <input
            class="task-text"
            type="text"
            v-model="tasks[m.id]"
            :placeholder="`请输入${m.name}的任务内容`"
        />
      </div>
    </div>

    <div class="btn-row" v-if="false"></div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { getStudentsByGrade, createStudentGroup } from '@/services/group'

const allStudents = ref([])
const grade = ref('')
onMounted(async () => {
  try {
    // 自动从当前用户或本地获取班级，若无则使用示例
    const saved = JSON.parse(localStorage.getItem('currentUser') || 'null')
    grade.value = saved?.grade || '24040301'
  } catch { grade.value = '24040301' }
  await loadStudents()
})

async function loadStudents() {
  try {
    const list = await getStudentsByGrade(grade.value)
    // 规范化字段
    allStudents.value = (list || []).map((s, i) => ({
      id: s.id || s.studentId || s.sid || i + 1,
      name: s.name || s.studentName || s.realName || '-',
      sid: s.sid || s.studentNo || s.studentId || '-',
      status: (s.grouped === true || s.status === 'grouped') ? 'unavailable' : 'available'
    }))
  } catch {
    allStudents.value = []
  }
}

const keyword = ref('')
const groupStatus = ref('none')
const selectedIds = ref([])
const statusFilter = ref('')
const tasks = ref({})
const groupName = ref('')
const taskOnlyMode = ref(false)

// 读取左侧栏当前用户作为默认组长
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

function toggleSelect(stu) {
  if (stu.status !== 'available') return
  if (stu.id === currentUserId.value) return
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

function onRowClick(row) {
  if (taskOnlyMode.value) return
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
}

async function submitGroup() {
  if (!canSubmit.value) return
  try {
    const leader = currentUserObj.value
    const members = selectedMembers.value
    const payload = {
      groupName: groupName.value || `${leader?.name || '我的'}小组`,
      leaderId: leader?.id,
      memberIds: members.map(m => m.id),
      grade: grade.value
    }
    const res = await createStudentGroup(payload)
    groupStatus.value = 'pending'
    alert('已提交小组申请，等待审核！')
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
  taskOnlyMode.value = true
}

function exitTaskOnlyMode() {
  taskOnlyMode.value = false
}
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
  padding: 16px;
  margin-top: 4px;
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
.task-title {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 10px;
}
.task-input-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.task-input-row:last-child {
  margin-bottom: 0;
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
  margin-top: 20px;
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
.filters-row > .el-input {
  margin-right: 8px;
}
.filters-row > .el-select {
  margin-right: 8px;
}
.filters-row .actions-spacer {
  flex: 1;
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


