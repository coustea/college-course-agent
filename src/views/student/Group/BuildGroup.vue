<template>
  <div class="group-build">
    <div class="header">
      <div class="header-actions" style="margin-left:auto;">
        <span v-if="isSelecting" class="select-hint">请选择 2 ~ 5 名组员</span>
        <el-button v-if="!isCreating" type="primary" @click="startCreate">新建小组</el-button>
        <el-button v-else-if="isSelecting" type="primary" @click="finishSelecting">选择完成</el-button>
      </div>
    </div>

    <div v-if="!isCreating || isSelecting">
      <div class="filters-row block-section">
        <el-input v-model="keyword" placeholder="搜索学生姓名或学号..." clearable class="keyword-input" />
        <el-select v-model="statusFilter" clearable placeholder="组队状态" class="status-select">
          <el-option label="全部" :value="''" />
          <el-option label="未组队" value="available" />
          <el-option label="已组队" value="unavailable" />
        </el-select>
        <div class="actions-spacer"></div>
        <span class="selected-count">已选择 {{ selectedMembers.length }} 人</span>
      </div>

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
      <div class="members-chips">
        <span class="member-chip" v-for="m in membersForTasks" :key="m.id">
          <span class="member-name">{{ m.name }}</span>
        </span>
      </div>
      <div class="btn-row block-section">
        <button class="btn btn-blue" @click="enterSelecting">选择队员</button>
        <button class="btn btn-yellow" @click="reselect">重新选择</button>
        <button class="btn btn-blue" :disabled="!canSubmit" @click="submitGroup">提交申请 ({{ selectedMembers.length }}/5)</button>
        <button class="btn btn-yellow" @click="cancelCreate">取消新建</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { getStudentsByClassName, createStudentGroup } from '@/services/groupApi'

const allStudents = ref([])
const keyword = ref('')
const statusFilter = ref('')
const selectedIds = ref([])
const groupName = ref('')
const taskDescription = ref('')
const isCreating = ref(false)
const isSelecting = ref(false)

const { proxy } = getCurrentInstance()
const uiStateStorageKey = 'student_groups_ui_state'
const GROUP_STATUS_KEY = 'student_group_status'
const GROUP_INFO_KEY = 'student_group_info'

onMounted(async () => {
  try {
    // 恢复 UI 状态
    try {
      const ui = JSON.parse(localStorage.getItem(uiStateStorageKey) || 'null')
      if (ui && typeof ui === 'object') {
        isCreating.value = !!ui.creating
        isSelecting.value = !!ui.selecting
      }
    } catch {}

    const className = localStorage.getItem('className')
    const list = await getStudentsByClassName(className)
    console.log('学生分组的列表', list)
    allStudents.value = (list || []).map((s, i) => {
      const groupStatusRaw = String(s.groupStatus || s.status || '').toLowerCase()
      const mappedUnavailable = (groupStatusRaw === 'approval' || groupStatusRaw === 'approved' || s.grouped === true || s.status === 'grouped')
      return {
        id: s.id || s.studentId || s.sid || i + 1,
        name: s.name || s.studentName || '-',
        sid: s.sid || s.studentId || s.studentNumber || '-',
        status: mappedUnavailable ? 'unavailable' : 'available',
        phone: s.phone || '-',
        email: s.email || '-',
      }
    })
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
  try {
    const saved = JSON.parse(localStorage.getItem('currentUser') || 'null')
    const mySid = saved?.sid || saved?.studentNo || saved?.studentId || null
    if (mySid && stu?.sid) return String(stu.sid) === String(mySid)
  } catch {}
  return false
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
function resetSelection() { selectedIds.value = [] }

function startCreate() {
  isCreating.value = true
  isSelecting.value = false
  try { localStorage.setItem(uiStateStorageKey, JSON.stringify({ creating: true, selecting: false })) } catch {}
}
function enterSelecting() {
  isSelecting.value = true
  try { localStorage.setItem(uiStateStorageKey, JSON.stringify({ creating: true, selecting: true })) } catch {}
}
function finishSelecting() {
  isSelecting.value = false
  try { localStorage.setItem(uiStateStorageKey, JSON.stringify({ creating: true, selecting: false })) } catch {}
}
function reselect() {
  selectedIds.value = []
  isSelecting.value = true
  try { localStorage.setItem(uiStateStorageKey, JSON.stringify({ creating: true, selecting: true })) } catch {}
}
function cancelCreate() {
  isCreating.value = false
  isSelecting.value = false
  selectedIds.value = []
  try { localStorage.setItem(uiStateStorageKey, JSON.stringify({ creating: false, selecting: false })) } catch {}
}

async function submitGroup() {
  if (!canSubmit.value) return
  try {
    const leaderId = Number(localStorage.getItem('userId')) || undefined
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
.header { display:flex; align-items:center; justify-content:space-between; gap:12px; margin-bottom:16px; }
.page-title { font-size:24px; font-weight:700; color:#2c3e50; }
.header-actions { display:flex; align-items:center; gap:12px; }
.select-hint { color:#6b7280; font-size:14px; }
.filters-row { display:flex; align-items:center; gap:12px; margin-bottom:12px; }
.filters-row .keyword-input { max-width: 420px; width: 100%; }
.filters-row .status-select { width: 160px; }
.filters-row .actions-spacer { flex: 1; }
.selected-count { margin-left: 12px; color: #6b7280; font-size: 14px; white-space: nowrap; }
.full-width-table { width: 100%; }
.status-chip { padding: 2px 10px; border-radius: 999px; font-size: 12px; font-weight: 700; }
.chip-red { background: #fee2e2; color: #b91c1c; }
.chip-green { background: #dcfce7; color: #15803d; }
.task-inputs { background: #f8f9fa; border: 1px solid #e5e7eb; border-radius: 8px; padding: 22px; margin-top: 8px; position: relative; z-index: 10; }
.task-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 8px; }
.group-name-row { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.group-name-label { width: 160px; text-align: right; color: #2c3e50; font-weight: 600; }
.group-name-input { flex: 1; padding: 10px 12px; border: 1px solid #cbd5e1; border-radius: 6px; }
.task-title { display: block; width: 100%; text-align: center; font-size: 22px; font-weight: 800; color: #111827; margin: 4px 0 18px 0; }
.task-input-row { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.task-name { width: 160px; text-align: right; color: #2c3e50; font-weight: 600; white-space: nowrap; }
.task-text { flex: 1; padding: 10px 12px; border: 1px solid #d1d5db; border-radius: 6px; }
.members-chips { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 6px; justify-content: center; }
.member-chip { display: inline-flex; align-items: center; gap: 6px; background: #f1f5f9; border: 1px solid #e2e8f0; padding: 6px 10px; border-radius: 999px; font-size: 12px; color: #1f2937; }
.self-mark { display:inline-block; margin-left:6px; background:#fff7ed; color:#b45309; border:1px solid #fdba74; padding:2px 6px; border-radius:999px; font-size:12px; font-weight:700; line-height:1; }
.btn-row { display: flex; gap: 40px; justify-content: center; margin-top: 16px; }
.btn { padding: 10px 20px; border: none; border-radius: 8px; cursor: pointer; font-weight: bold; transition: all 0.3s; min-width: 140px; }
.btn-blue { background: #2563eb; color: #fff; }
.btn-blue:hover { background: #1d4ed8; }
.btn-yellow { background: #fbbf24; color: #1f2937; }
.btn-yellow:hover { background: #f59e0b; }
</style>


