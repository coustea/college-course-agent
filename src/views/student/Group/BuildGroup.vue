<template>
  <div class="group-build">
    <div class="header">
      <div class="header-filters" v-if="!isCreating || isSelecting">
        <el-input v-model="keyword" placeholder="搜索学生姓名或学号..." clearable class="keyword-input" />
        <el-select v-model="statusFilter" clearable placeholder="组队状态" class="status-select">
          <el-option label="全部" :value="''" />
          <el-option label="未组队" value="available" />
          <el-option label="已组队" value="unavailable" />
        </el-select>
      </div>
      <div class="header-actions">
        <span v-if="isSelecting" class="select-hint">请选择 2 ~ 5 名组员</span>
        <span v-if="!isCreating || isSelecting" class="choose-hint">可选择 2~5 人为组员</span>
        <span v-if="!isCreating || isSelecting" class="selected-count">已选择 {{ selectedMembers.length }} 人</span>
        <el-button v-if="!isCreating" type="primary" @click="startCreate">新建小组</el-button>
        <el-button v-else-if="isSelecting" type="primary" @click="finishSelecting">选择完成</el-button>
      </div>
    </div>

    <div v-if="!isCreating || isSelecting">

      <div class="block-section">
        <el-table :data="filteredStudents" stripe border class="full-width-table" @row-click="onRowClick" :row-class-name="rowClassName" empty-text="暂无学生">
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
                <span v-if="isSelf(row)" class="self-mark"><i class="fa-solid fa-user fa-icon"></i>本人</span>
              </span>
            </template>
          </el-table-column>
          <el-table-column label="组队状态" width="160">
            <template #default="{ row }">
              <span class="status-chip" :class="row.status === 'available' ? 'chip-red' : 'chip-green'">
                <i :class="['fa-solid', row.status === 'available' ? 'fa-user' : 'fa-circle-check', 'fa-icon']"></i>
                {{ row.status === 'available' ? '未组队' : '已组队' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="手机号" min-width="160">
            <template #default="{ row }">
              <i class="fa-solid fa-phone fa-icon"></i>
              <span class="col-phone">{{ row.phone || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="邮箱" min-width="220">
            <template #default="{ row }">
              <i class="fa-solid fa-envelope fa-icon"></i>
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
    </div>

    <div class="task-inputs block-section" v-if="isCreating && !isSelecting">
      <div class="task-header">
        <h4 class="task-title">任务分工</h4>
      </div>
      <div class="group-name-row">
        <label class="group-name-label">我的小组名称：</label>
        <input class="group-name-input" type="text" v-model="groupName" placeholder="请输入小组名称" />
      </div>
      <div class="task-input-row">
        <span class="task-name">小组任务描述：</span>
        <textarea class="task-text" v-model="taskDescription" placeholder="请描述分工（示例：张三-资料收集，李四-文档整理，王五-PPT与汇报）" rows="3"></textarea>
      </div>
      <div class="role-summary">
        <div class="role-row">
          <span class="role-label"><i class="fa-solid fa-medal fa-icon"></i>组长：</span>
          <span class="role-name">{{ leaderName }}</span>
        </div>
        <div class="role-row">
          <span class="role-label"><i class="fa-solid fa-user-group fa-icon"></i>组员：</span>
          <div class="role-members">
            <span class="member-chip" v-for="m in membersForTasks" :key="m.id">
              <span class="member-name">{{ m.name }}</span>
            </span>
            <span v-if="membersForTasks.length === 0" class="members-empty">未选择</span>
          </div>
        </div>
      </div>
      <div class="btn-row block-section">
        <button class="btn btn-blue" @click="enterSelecting">选择队员</button>
        <button class="btn btn-yellow" @click="reselect">重新选择</button>
        <el-tooltip content="可选择 2~5 人为组员" placement="top">
          <span class="tooltip-wrapper">
            <button class="btn btn-blue" :disabled="!canSubmit" @click="submitGroup">提交申请 ({{ selectedMembers.length }}/5)</button>
          </span>
        </el-tooltip>
        <button class="btn btn-yellow" @click="cancelCreate">取消新建</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getStudentsByClassName, createStudentGroup } from '@/services/groupApi'

const allStudents = ref([])
const keyword = ref('')
const statusFilter = ref('')
const selectedIds = ref([])
const groupName = ref('')
const taskDescription = ref('')

const isCreating = ref(false)
const isSelecting = ref(false)

const myId = ref(null)
const myFallbackName = ref('我')
const uiStateStorageKey = 'student_groups_ui_state'
const GROUP_STATUS_KEY = 'student_group_status'
// 被驳回小组成员（含组长）的学号缓存键
const REJECTED_SIDS_KEY = 'rejected_group_member_sids'
onMounted(async () => {
  try {
    // 恢复 UI 状态
      const ui = JSON.parse(localStorage.getItem(uiStateStorageKey) || 'null')
      if (ui && typeof ui === 'object') {
        isCreating.value = !!ui.creating
        isSelecting.value = !!ui.selecting
      }

    const className = localStorage.getItem('className')
    const list = await getStudentsByClassName(className)
    console.log('学生分组的列表', list)
    // 读取被驳回的小组成员学号列表（一次性使用）
    let rejectedSids = []
    try { rejectedSids = JSON.parse(localStorage.getItem(REJECTED_SIDS_KEY) || '[]') } catch {}
    const rejectedSet = new Set((rejectedSids || []).map(x => String(x)))

    allStudents.value = list.map((s, i) => {
      const groupStatusRaw = String(s.groupStatus || s.status || '').toLowerCase()
      let mappedUnavailable = (groupStatusRaw === 'approval')
      // 若该学生在被驳回小组名单中，则强制视为可选（未组队）
      const sidStr = String(s.studentNumber || s.sid || '')
      if (sidStr && rejectedSet.has(sidStr)) mappedUnavailable = false
      return {
        id: s.id|| i + 1,
        name: s.name || '-',
        sid: s.studentNumber || '-',
        status: mappedUnavailable ? 'unavailable' : 'available',
        phone: s.phone || '-',
        email: s.email || '-',
      }
    })
    // 清理一次性缓存
    try { localStorage.removeItem(REJECTED_SIDS_KEY) } catch {}
  } catch (e) { alert(`加载学生列表失败：${e?.message || e}`) }
})

const filteredStudents = computed(() => {
  const k = keyword.value.trim()
  return (allStudents.value || [])
    .filter(s => {
      if (statusFilter.value === 'available') return s.status === 'available'
      if (statusFilter.value === 'unavailable') return s.status !== 'available'
      return true
    })
    .filter(s => !k || s.name.includes(k) || String(s.sid).includes(k))
})

const selectedMembers = computed(() => allStudents.value.filter(s => selectedIds.value.includes(s.id)))
const membersForTasks = computed(() => selectedMembers.value)
const canSubmit = computed(() => {
  const count = selectedMembers.value.length
  return count >= 2 && count <= 5
})

const leaderStudent = computed(() => allStudents.value.find(s => String(s.id) === String(myId.value)))
const leaderName = computed(() => leaderStudent.value?.name || myFallbackName.value)

function onRowClick(row) {
  if (!isSelecting.value) return
  if (row?.status !== 'available') return
  toggleSelect(row)
}
function rowClassName({ row }) {
  if (row.status !== 'available') return 'disabled'
  if (selectedIds.value.includes(row.id)) return 'selected'
  return ''
}
function isSelf(stu) {
  return !!(myId.value != null && stu && String(stu.id) === String(myId.value))
}
function isSelected(id) { return selectedIds.value.includes(id) }
function toggleSelect(stu) {
  if (stu.status !== 'available') return
  if (isSelf(stu)) return
  const idx = selectedIds.value.indexOf(stu.id)
  if (idx >= 0) {
    selectedIds.value.splice(idx, 1)
  } else {
    if (selectedIds.value.length >= 5) return
    selectedIds.value.push(stu.id)
  }
}
function startCreate() {
  isCreating.value = true
  isSelecting.value = false
  localStorage.setItem(uiStateStorageKey, JSON.stringify({creating: true, selecting: false}))
}
function enterSelecting() {
  isSelecting.value = true
  localStorage.setItem(uiStateStorageKey, JSON.stringify({creating: true, selecting: true}))
}
function finishSelecting() {
  isSelecting.value = false
  localStorage.setItem(uiStateStorageKey, JSON.stringify({creating: true, selecting: false}))
}
function reselect() {
  selectedIds.value = []
  isSelecting.value = true
  localStorage.setItem(uiStateStorageKey, JSON.stringify({creating: true, selecting: true}))
}
function cancelCreate() {
  isCreating.value = false
  isSelecting.value = false
  selectedIds.value = []
  localStorage.setItem(uiStateStorageKey, JSON.stringify({creating: false, selecting: false}))
}

async function submitGroup() {
  if (!canSubmit.value) return
  try {
    const leaderId = Number(myId.value) || Number(localStorage.getItem('userId')) || undefined
    const members = selectedMembers.value
    const payload = {
      groupName: groupName.value,
      groupLeaderId: leaderId,
      memberIds: members.map(m => m.id),
      teacherId: (() => { const v = localStorage.getItem('userId'); return v ? Number(v) : undefined })(),
      groupDescription: taskDescription.value
    }
    console.log('提交小组信息', payload)
    const res = await createStudentGroup(payload)
    const code = Number(res?.code ?? res?.status ?? 0)
    if (code === 200) {
      try { localStorage.setItem(GROUP_STATUS_KEY, 'pending') } catch {}
      alert('已提交小组审批')
      // 回到初始态
      isCreating.value = false
      isSelecting.value = false
      selectedIds.value = []
      try { localStorage.setItem(uiStateStorageKey, JSON.stringify({ creating: false, selecting: false })) } catch {}
    } else {
      alert(`提交失败：${res?.message || code || '未知错误'}`)
    }
  } catch (e) { alert('提交失败，请稍后再试') }
}
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.header-filters {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
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

.filters-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.filters-row .keyword-input {
  max-width: 420px;
  width: 100%;
}

.filters-row .status-select {
  width: 160px;
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

.status-chip {
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.chip-red {
  background: #fee2e2;
  color: #b91c1c;
}

.chip-green {
  background: #dcfce7;
  color: #15803d;
}

.task-inputs {
  background: #f8f9fa;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 22px;
  margin-top: 8px;
  position: relative;
  z-index: 10;
}

.task-header {
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

.task-title {
  display: block;
  width: 100%;
  text-align: center;
  font-size: 22px;
  font-weight: 800;
  color: #111827;
  margin: 4px 0 18px 0;
}

.task-input-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.task-name {
  width: 160px;
  text-align: right;
  color: #2c3e50;
  font-weight: 600;
  white-space: nowrap;
}

.task-text {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
}

.role-summary {
  margin-top: 8px;
}

.role-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 6px 0;
}

.role-label {
  width: 160px;
  text-align: right;
  color: #2c3e50;
  font-weight: 600;
}

.role-name {
  color: #111827;
  font-weight: 700;
  font-size: 16px;
}

.role-members {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.members-empty {
  color: #9ca3af;
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

.btn-row {
  display: flex;
  gap: 110px;
  justify-content: center;
  margin-top: 16px;
}

.tooltip-wrapper { display: inline-block; }

.btn {
  padding: 12px 26px;
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

.btn-yellow {
  background: #fbbf24;
  color: #1f2937;
}

.btn-yellow:hover {
  background: #f59e0b;
}
.fa-icon { margin-right: 6px; }

/* 额外：缩小头部左侧两个搜索框尺寸 + 提示文案样式 */
.header-filters .keyword-input { width: 320px; }
.header-filters .status-select { width: 140px; }
.choose-hint { color: #9ca3af; font-size: 13px; white-space: nowrap; }

/* 仅在任务分工的“组员”区覆盖为与组长一致的字体样式 */
.role-members .member-chip {
  background: transparent;
  border: none;
  padding: 0;
  border-radius: 0;
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  margin-right: 16px;
}
</style>


