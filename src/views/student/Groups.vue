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
      <span v-if="isCreating && isSelecting" class="select-hint">请选择 2 ~ 5 名组员</span>
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
      <div class="tooltip" data-tip="组员最多5人，最少2人">
        <button class="btn btn-blue" :disabled="!canSubmit || !isCreating" @click="submitGroup">
          提交小组申请 ({{ selectedMembers.length }}/5)
        </button>
      </div>
      <button class="btn btn-yellow" @click="resetAllAndExit">重置</button>
    </div>
  </div>

  <div v-if="groupStatus !== 'none'" class="group-summary block-section">
    <div class="group-summary-card">
      <div class="group-summary-header">
        <span class="group-summary-label">我的小组名称：</span>
        <span>{{ createdGroup?.groupName || groupName || '—' }}</span>
        <span class="status-chip status-pending" v-if="groupStatus==='pending'">审批中</span>
        <span class="status-chip status-approved" v-if="groupStatus==='approved'">已组队</span>
        <span class="status-chip status-none" v-if="groupStatus==='rejected'">未组队</span>
      </div>
      <div class="group-summary-members">
        <span class="group-summary-label">成员：</span>
        <span class="member-chip" v-if="createdGroup?.leaderName">
          <span class="member-name">{{ createdGroup.leaderName }}</span>
          <span class="leader-mark">组长</span>
        </span>
        <span
          class="member-chip"
          v-for="(n, i) in (createdGroup?.memberNames || [])"
          :key="i"
        >
          <span class="member-name">{{ n }}</span>
          <span class="member-mark">组员</span>
        </span>
      </div>
      <div class="group-summary-tasks">
        <span class="group-summary-label">分工：</span>
        <span>{{ createdGroup?.taskDescription || taskDescription || '—' }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted, nextTick,getCurrentInstance,onActivated } from 'vue'
import { getStudentsByClassName, createStudentGroup} from '@/services/groupApi'
import { fetchHomeCourses } from '@/services/homeCoursesApi'
import {onBeforeRouteUpdate} from "vue-router"
import axios from "axios"

const {proxy} = getCurrentInstance()
const BASE_URL = proxy.$baseUrl
console.log(BASE_URL)

const allStudents = ref([])
const isCreating = ref(false)
const isSelecting = ref(false)
const uiStateStorageKey = 'student_groups_ui_state'
const GROUP_STATUS_KEY = 'student_group_status'
const GROUP_INFO_KEY = 'student_group_info'
const StudentGroup = ref()
onMounted(async () => {
  try {
    const ui = JSON.parse(localStorage.getItem(uiStateStorageKey) || 'null')
    console.log(ui)
    if (ui && typeof ui === 'object') {
      isCreating.value = !!ui.creating
      if (ui.creating) {
        isSelecting.value = false
        localStorage.setItem(uiStateStorageKey, JSON.stringify({ creating: true, selecting: false }))
      } else {
        isSelecting.value = !!ui.selecting
      }
      // 若处于新建流程但尚未提交（非 pending/approved），刷新时将状态复原为全部 false
      try {
        const st = String(localStorage.getItem(GROUP_STATUS_KEY) || 'none').toLowerCase()
        const unsubmitted = !(st === 'pending' || st === 'approved')
        if (ui.creating === true && ui.selecting === false && unsubmitted) {
          isCreating.value = false
          isSelecting.value = false
          localStorage.setItem(uiStateStorageKey, JSON.stringify({ creating: false, selecting: false }))
        }
      } catch (e) {
        console.error('恢复UI状态时写入本地失败', e)
      }
    }
  } catch (e) {
    alert(`读取分组界面UI状态失败：${e?.message || e}`)
  }
  // 刷新/关闭前也进行一次保护性复原
  const resetUiIfUnsubmitted = () => {
    try {
      const ui = JSON.parse(localStorage.getItem(uiStateStorageKey) || 'null')
      const st = String(localStorage.getItem(GROUP_STATUS_KEY) || 'none').toLowerCase()
      const unsubmitted = !(st === 'pending' || st === 'approved')
      if (ui && ui.creating === true && ui.selecting === false && unsubmitted) {
        localStorage.setItem(uiStateStorageKey, JSON.stringify({ creating: false, selecting: false }))
      }
    } catch (e) {
      console.error('进入页面时复原UI失败', e)
    }
  }
  window.addEventListener('beforeunload', resetUiIfUnsubmitted)
  // 恢复小组状态与信息（若存在）
  try {
    const group = await getStudentGroup()
    console.log("获取小组审核状态:",group)
    if (!group) {
      try {
        localStorage.removeItem(GROUP_STATUS_KEY)
        localStorage.removeItem(GROUP_INFO_KEY)
      } catch (e) { console.error(e) }
      groupStatus.value = 'none'
    }else {
      const st = String(group.approvalStatus || '').toLowerCase()
      if (st === 'pending' || st === 'approved') {
        groupStatus.value = st
        const info = buildCreatedGroupFromApi(group)
        createdGroup.value = info
        if (info.groupName) groupName.value = info.groupName
        try {
          localStorage.setItem(GROUP_STATUS_KEY, st)
          localStorage.setItem(GROUP_INFO_KEY, JSON.stringify(info))
        } catch (e) { console.error(e) }
      } else {
        groupStatus.value = 'none'
      }
    }
  } catch (e) { alert(`恢复小组信息失败：${e?.message || e}`) }
  await Promise.all([loadStudents(), cacheTeacherIdFromHome()])
})

// onUnmounted(() => {
//   try {
//   } catch (e) { console.error(e) }
// })    兜底刷新使用

onActivated(async () => {
  const g = await getStudentGroup()
  const st = String(g?.approvalStatus || '').toLowerCase()
  if (st === 'pending' || st === 'approved') {
    groupStatus.value = st
  } else {
    groupStatus.value = 'none'
    createdGroup.value = null
    try {
      localStorage.setItem(GROUP_STATUS_KEY, 'none')
      localStorage.removeItem(GROUP_INFO_KEY)
    } catch (e) {
      console.error('复原UI失败', e)
    }
  }
})

onBeforeRouteUpdate(async () => {
  const g = await getStudentGroup()
  const st = String(g?.approvalStatus || '').toLowerCase()
  if (st === 'pending' || st === 'approved') {
    groupStatus.value = st
  } else {
    groupStatus.value = 'none'
    createdGroup.value = null
    try {
      localStorage.setItem(GROUP_STATUS_KEY, 'none')
      localStorage.removeItem(GROUP_INFO_KEY)
    } catch (e) {
      console.error('清理本地组队信息失败', e)
    }
  }
})

// 将后端的小组数据映射为前端展示结构（groupMemberList 的第一个为组长）
function buildCreatedGroupFromApi(group) {
  try {
    let cachedInfo = null
    try { cachedInfo = JSON.parse(localStorage.getItem(GROUP_INFO_KEY) || 'null') } catch (e) { cachedInfo = null }
    const list = Array.isArray(group?.groupMemberList) ? group.groupMemberList : []
    let leaderName = ''
    let memberNames = []
    if (list.length > 0) {
      leaderName = list[0]?.studentName || list[0]?.name || ''
      memberNames = list.slice(1).map(m => m?.studentName || m?.name).filter(Boolean)
    } else {
      leaderName = group?.leaderName || ''
      memberNames = Array.isArray(group?.memberNames) ? group.memberNames : []
    }
    return {
      groupName: group?.groupName || '',
      leaderName,
      memberNames,
      taskDescription: group?.groupDescription || cachedInfo?.taskDescription || ''
    }
  } catch (e) {
    return {
      groupName: group?.groupName || '',
      leaderName: group?.leaderName || '',
      memberNames: Array.isArray(group?.memberNames) ? group.memberNames : [],
      taskDescription: group?.groupDescription || ''
    }
  }
}
const getStudentGroup = async () => {
  try {
    // 获取 userId
    const userId = localStorage.getItem('userId')
    if (!userId) {
      console.error('未找到 userId')
      StudentGroup.value = null
      return null
    }

    // 将 userId 转换为数字（Long 类型）
    const studentId = Number(userId);
    if (isNaN(studentId)) {
      console.error('无效的 studentId:', userId)
      StudentGroup.value = null
      return null
    }

    // 第一次请求: 获取学生所在小组信息
    const res = await axios.post(
        `${BASE_URL}/groupMember/getById`,
        { studentId },  // 传递对象格式，确保 studentId 是数字类型
        {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
            'Content-Type': 'multipart/form-data',
          },
        }
    )

    console.log("根据 userId 查询所在小组信息:", res)

    // 检查返回的结果
    if (!(res.data.code === 200)) {
      console.error('未找到小组信息，groupId 不存在')
      StudentGroup.value = null
      return null
    }

    // 获取 groupId
    const groupId = res.data.data.groupId
    console.log("小组ID:", groupId)

    // 检查 groupId 是否有效
    if (!groupId) {
      console.error('无效的 groupId:', groupId)
      StudentGroup.value = null
      return null
    }

    // 第二次请求: 根据 groupId 获取小组详细信息
    const response = await axios.post(
        `${BASE_URL}/student-group/getByGroupId`,
        { groupId },  // 传递对象格式，确保 groupId 是数字类型
        {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
            'Content-Type': 'multipart/form-data',
          },
        }
    )

    console.log("小组审核信息:", response)

    // 如果返回成功且包含数据
    if (response?.data?.code === 200 && response?.data?.data) {
      StudentGroup.value = response.data.data
      return response.data.data
    }

    // 如果没有找到数据
    console.error('未找到小组详细信息')
    StudentGroup.value = null;
    return null;
  } catch (error) {
    console.error('获取学生分组信息失败:', error.response ? error.response.data : error)
    StudentGroup.value = null
    return null
  }
}


async function loadStudents() {
  try {
    const className =  localStorage.getItem("className")
    const list = await getStudentsByClassName(className)
    // 若处于待审批阶段，进入页面时根据后端返回的当前用户审核状态同步本地分组状态
    try {
      await checkMyGroupAudit(list)
    } catch (e) {
      alert('审核状态检查失败')
    }
    console.log('学生分组的列表', list)
    allStudents.value = (list || []).map((s, i) => ({
      id: s.id|| i + 1,
      name: s.name || '-',
      sid: s.studentNumber || '-',
      status: (s.groupStatus === 'approved') ? 'unavailable' : 'available',
      phone: s.phone || '-',
      email: s.email || '-',
    }))
  } catch (e) {
    alert(`加载学生列表失败：${e?.message || e}`)
    allStudents.value = []
  }
}
// 获取并本地持久化小组详情（审批通过时调用）
async function fetchAndPersistGroupInfo() {
  try {
    const group = await getStudentGroup()
    if (group && typeof group === 'object') {
      const info = buildCreatedGroupFromApi(group)
      createdGroup.value = info
      if (info.groupName) groupName.value = info.groupName
      try {
        localStorage.setItem(GROUP_STATUS_KEY, 'approved')
        localStorage.setItem(GROUP_INFO_KEY, JSON.stringify(info))
      } catch (e) { console.error(e) }
    }
  } catch (e) { console.warn('获取小组详情失败:', e) }
}

// 审核状态同步：仅以 /student-group/getByGroupId 返回的 approvalStatus 为准
async function checkMyGroupAudit() {
  try {
    const group = await getStudentGroup()
    const st = String(group?.approvalStatus || '').toLowerCase()
    const prev = groupStatus.value
    if (st === 'approved') {
      if (prev !== 'approved') {
        groupStatus.value = 'approved'
        try { localStorage.setItem(GROUP_STATUS_KEY, 'approved') } catch (e) { console.error('写入状态approved失败', e) }
        await fetchAndPersistGroupInfo()
      }
    } else if (st === 'pending') {
      if (prev !== 'pending') {
        groupStatus.value = 'pending'
        try { localStorage.setItem(GROUP_STATUS_KEY, 'pending') } catch (e) { console.error('写入状态pending失败', e) }
      }
    } else {
      if (prev !== 'none') {
        groupStatus.value = 'none'
        createdGroup.value = null
        groupName.value = ''
        try {
          localStorage.setItem(GROUP_STATUS_KEY, 'none')
          localStorage.removeItem(GROUP_INFO_KEY)
        } catch (e) { console.error('写入/清理本地状态失败', e) }
      }
    }
  } catch (e) { console.warn(e) }
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
  } catch (e) { alert(`缓存教师ID失败：${e?.message || e}`) }
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
  currentUserName.value = localStorage.getItem('studentName')
} catch (e) { alert(`读取学生姓名失败：${e?.message || e}`); currentUserName.value = '' }

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
  return count >= 2 && count <= 5
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
    if (selectedIds.value.length >= 5) return
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
  } catch (e) { alert(`读取本地用户信息失败：${e?.message || e}`) }
  return stu?.id === currentUserId.value
}

function onRowClick(row) {
  if (row?.status !== 'available') return
  if (!isCreating.value || !isSelecting.value) {
    isCreating.value = true
    isSelecting.value = true
    try { localStorage.setItem(uiStateStorageKey, JSON.stringify({ creating: true, selecting: true })) } catch (e) { alert(`保存界面状态失败：${e?.message || e}`) }
  }
  toggleSelect(row)
}

function rowClassName({ row }) {
  if (row.status !== 'available') return 'disabled'
  if (selectedIds.value?.includes(row.id)) return 'selected'
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
      groupName: groupName.value,
      groupLeaderId: leader?.id,
      memberIds: members.map(m => m?.id ),
      teacherId: (() => {
        const v = localStorage.getItem('userId')
        return v ? Number(v) : undefined
      })(),
      groupDescription: taskDescription.value
    }

    console.log('提交小组信息', payload)
    const res = await createStudentGroup(payload)
    const code = Number(res?.code ?? res?.status ?? 0)
    if (code === 200) {
      groupStatus.value = 'pending'
      createdGroup.value = {
        groupName: payload.groupName,
        leaderName: leader?.name,
        memberNames: members.map(m => m.name),
        taskDescription: taskDescription.value
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

// function createNewGroup() {
//   selectedIds.value = []
//   groupName.value = ''
//   isCreating.value = true
//   isSelecting.value = false
//   localStorage.setItem(uiStateStorageKey, JSON.stringify({ creating: true, selecting: false }))
// }

function resetAllAndExit() {
  selectedIds.value = []
  tasks.value = {}
  groupName.value = ''
  isCreating.value = false
  isSelecting.value = false
  localStorage.setItem(uiStateStorageKey, JSON.stringify({ creating: false, selecting: false }))
}

const createdGroup = ref(null)
const groupNameInputRef = ref(null)
</script>

<style scoped>

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

.student-table table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}
.student-table thead th {
  text-align: left;
  background: #f3f4f6;
  color: #6b7280;
  padding: 14px 16px;
  font-weight: 600;
  border-bottom: 1px solid #e5e7eb;
}
.student-table tbody td {
  padding: 14px 16px;
  border-bottom: 1px solid #e5e7eb;
  color: #4b5563;
}

.student-table tbody tr:hover {
  background: #f9fafb;
  cursor: pointer;
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
  letter-spacing: 1px;
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
}
@media (max-width: 900px) {
}
@media (max-width: 600px) {
}
</style>


