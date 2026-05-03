<template>
  <div class="group-build">
    <div class="page-header">
      <div class="header-left">
        <h2>
          <el-icon class="header-icon"><UserFilled /></el-icon>
          新建学习小组
        </h2>
        <p class="subtitle">选择2-5名成员组建学习小组，明确任务分工</p>
      </div>
      <div class="header-right">
        <div class="info-tag">
          <span class="label">当前班级</span>
          <span class="value">{{ className }}</span>
        </div>
        <el-divider direction="vertical" />
        <div class="info-tag">
          <span class="label">班级总人数</span>
          <span class="value">{{ totalStudentCount }}人</span>
        </div>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-item">
        <div class="stat-icon-wrapper bg-blue-subtle">
          <el-icon class="text-blue"><User /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ totalStudentCount }}</div>
          <div class="stat-desc">班级总人数</div>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon-wrapper bg-green-subtle">
          <el-icon class="text-green"><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ availableCount }}</div>
          <div class="stat-desc">可组队人数</div>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon-wrapper bg-orange-subtle">
          <el-icon class="text-orange"><UserFilled /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ groupedCount }}</div>
          <div class="stat-desc">已组队人数</div>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon-wrapper bg-purple-subtle">
          <el-icon class="text-purple"><DataAnalysis /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ completionRate }}%</div>
          <div class="stat-desc">组队完成率</div>
        </div>
      </div>
    </div>

    <div class="main-layout">

      <div class="main-workspace">

        <div v-if="!isCreating || isSelecting" class="workspace-view">
          <el-card shadow="never" class="main-card">
            <div class="filter-header">
              <div class="filter-inputs">
                <el-input
                  v-model="keyword"
                  placeholder="搜索姓名或学号"
                  clearable
                  style="width: 260px;"
                >
                  <template v-slot:prefix><el-icon><Search /></el-icon></template>
                </el-input>
                <el-radio-group v-model="statusFilter" size="default">
                  <el-radio-button label="">全部</el-radio-button>
                  <el-radio-button label="available">未组队</el-radio-button>
                  <el-radio-button label="unavailable">已组队</el-radio-button>
                </el-radio-group>
              </div>
              <div class="filter-actions">
                 <el-button
                   v-if="isCreating"
                   @click="finishSelecting"
                   type="primary"
                   plain
                 >
                   完成选择
                 </el-button>
                 <el-button
                  v-else
                  type="primary"
                  @click="startCreate"
                  class="create-btn"
                >
                  <el-icon style="margin-right: 4px"><Plus /></el-icon>
                  新建小组
                </el-button>
              </div>
            </div>

            <el-table
              :data="paginatedStudents"
              style="width: 100%"
              :header-cell-style="{ background: '#f8fafc', color: '#64748b', fontWeight: '600' }"
              :row-style="{ height: '52px' }"
              :row-class-name="tableRowClassName"
              highlight-current-row
              @row-click="onRowClick"
            >
              <el-table-column label="序号" width="60" align="center">
                <template v-slot:default="{ $index }">
                  {{ (currentPage - 1) * pageSize + $index + 1 }}
                </template>
              </el-table-column>

              <el-table-column prop="sid" label="学号" min-width="120" sortable />
              <el-table-column prop="name" label="姓名" min-width="100">
                <template v-slot:default="{ row }">
                  <span class="font-medium">{{ row.name }}</span>
                  <el-tag v-if="isSelf(row)" type="warning" size="small" effect="plain" class="ml-2">我</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100" align="center">
                <template v-slot:default="{ row }">
                  <div class="status-indicator" :class="row.status === 'available' ? 'is-active' : 'is-disabled'">
                    <span class="dot"></span>
                    {{ row.status === 'available' ? '未组队' : '已组队' }}
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="phone" label="联系方式" min-width="140" show-overflow-tooltip />

              <el-table-column v-if="isCreating" label="操作" width="100" align="center" fixed="right">
                <template v-slot:default="{ row }">
                  <el-button
                    v-if="isSelected(row.id)"
                    type="danger"
                    link
                    size="small"
                    @click.stop="toggleSelect(row)"
                  >
                    取消
                  </el-button>
                  <el-button
                    v-else-if="row.status === 'available' && !isSelf(row)"
                    type="primary"
                    link
                    size="small"
                    @click.stop="toggleSelect(row)"
                    :disabled="selectedMembers.length >= 5"
                  >
                    选择
                  </el-button>
                  <span v-else class="text-gray-300">-</span>
                </template>
              </el-table-column>
            </el-table>

            <div class="table-footer">
              <el-pagination
                v-model:current-page="currentPage"
                v-model:page-size="pageSize"
                :page-sizes="[10, 20, 50, 100]"
                layout="total, sizes, prev, pager, next, jumper"
                :total="totalFilteredCount"
                background
                small
              />
            </div>
          </el-card>
        </div>

        <div v-else class="workspace-view">
          <el-card shadow="never" class="main-card form-mode">
            <template v-slot:header>
              <div class="card-title">
                <el-icon><EditPen /></el-icon>
                <span>填写小组信息</span>
              </div>
            </template>

            <el-form :model="groupForm" label-position="top" class="group-form">
              <el-form-item label="小组名称" required>
                <el-input
                  v-model="groupName"
                  placeholder="给你们的小组起个响亮的名字"
                  size="large"
                  maxlength="30"
                  show-word-limit
                />
              </el-form-item>

              <el-form-item label="任务分工描述" required>
                <el-input
                  v-model="taskDescription"
                  type="textarea"
                  :rows="6"
                  placeholder="请详细描述分工（例如：张三负责前端开发，李四负责后端API，王五负责文档撰写...）"
                  maxlength="500"
                  show-word-limit
                />
              </el-form-item>

              <div class="form-preview-section">
                <h4 class="section-title">成员概览</h4>
                <div class="preview-members">
                  <div class="preview-card leader">
                    <div class="role-badge">组长</div>
                    <el-avatar :size="40" class="member-avatar leader-avatar">{{ leaderName?.charAt(0) }}</el-avatar>
                    <div class="member-details">
                      <span class="name">{{ leaderName }}</span>
                      <span class="sid">本人</span>
                    </div>
                  </div>
                  <div v-for="m in selectedMembers" :key="m.id" class="preview-card member">
                    <el-button class="remove-btn" circle size="small" type="danger" @click="toggleSelect(m)"><el-icon><Close /></el-icon></el-button>
                    <div class="role-badge member-badge">组员</div>
                    <el-avatar :size="40" class="member-avatar">{{ m.name?.charAt(0) }}</el-avatar>
                    <div class="member-details">
                      <span class="name">{{ m.name }}</span>
                      <span class="sid">{{ m.sid }}</span>
                    </div>
                  </div>
                  <div class="preview-card add-placeholder" @click="enterSelecting" v-if="selectedMembers.length < 5">
                    <el-icon><Plus /></el-icon>
                    <span>添加</span>
                  </div>
                </div>
              </div>

              <div class="form-actions">
                <el-button @click="cancelCreate" size="large">取消</el-button>
                <el-button type="primary" size="large" @click="submitGroup" :disabled="!canSubmit">
                  {{ isUpdate ? '提交修改申请' : '立即创建小组' }}
                </el-button>
              </div>
            </el-form>
          </el-card>

          <div class="tips-section">
             <el-alert
               title="组建须知"
               type="info"
               show-icon
               :closable="false"
               description="请确保所有成员已确认加入。提交后需要等待老师审核，审核通过后小组即正式成立。"
             />
          </div>
        </div>
      </div>

      <div class="sidebar">
        <div class="sticky-wrapper">

          <template v-if="isCreating">
             <el-card shadow="never" class="sidebar-card progress-card">
               <div class="progress-header">
                 <span class="title">组队进度</span>
                 <span class="count">{{ selectedMembers.length + 1 }}/6</span>
               </div>
               <el-progress
                 :percentage="Math.min(((selectedMembers.length + 1) / 6) * 100, 100)"
                 :show-text="false"
                 :status="selectedMembers.length >= 2 ? 'success' : ''"
                 stroke-width="10"
               />
               <div class="progress-status-text">
                 <template v-if="selectedMembers.length < 2">
                   <el-icon color="#e6a23c"><Warning /></el-icon>
                   <span>还需至少选择 {{ 2 - selectedMembers.length }} 名组员</span>
                 </template>
                 <template v-else>
                   <el-icon color="#67c23a"><CircleCheck /></el-icon>
                   <span class="text-green-600">已满足组建条件 ({{selectedMembers.length + 1}}人)</span>
                 </template>
               </div>
             </el-card>

             <el-card shadow="never" class="sidebar-card selected-list-card">
               <template v-slot:header>
                 <div class="card-header-row">
                   <span>已选名单 ({{ selectedMembers.length }})</span>
                   <el-button v-if="!isSelecting" type="primary" link @click="enterSelecting">继续添加</el-button>
                 </div>
               </template>

               <div v-if="selectedMembers.length === 0" class="empty-select-state">
                  <el-empty description="暂无组员" :image-size="60" />
               </div>

               <div v-else class="selected-list-scroll">
                 <div class="selected-item" v-for="m in selectedMembers" :key="m.id">
                   <div class="item-left">
                     <el-avatar :size="32" class="bg-gradient">{{ m.name?.charAt(0) }}</el-avatar>
                     <div class="item-info">
                       <div class="name">{{ m.name }}</div>
                       <div class="sid">{{ m.sid }}</div>
                     </div>
                   </div>
                   <el-button link type="danger" @click="toggleSelect(m)"><el-icon><Close /></el-icon></el-button>
                 </div>
               </div>
             </el-card>

             <div class="sidebar-actions">
               <el-button v-if="isSelecting" type="primary" size="large" class="w-full" @click="finishSelecting" :disabled="selectedMembers.length < 2">
                  下一步：填写分工
               </el-button>
               <el-button v-if="isSelecting" size="large" class="w-full" @click="cancelCreate">
                  取消
               </el-button>
             </div>
          </template>

          <template v-else>
             <el-card shadow="never" class="sidebar-card welcome-card">
               <div class="welcome-content">
                 <h3>准备好组建团队了吗？</h3>
                 <p>你可以发起组建申请，邀请同学加入；或者等待其他同学邀请。</p>
                 <el-button type="primary" class="w-full" @click="startCreate">
                   <el-icon class="mr-1"><Plus /></el-icon>
                   立即发起组建
                 </el-button>
               </div>
             </el-card>

             <el-card shadow="never" class="sidebar-card simple-list-card">
               <template v-slot:header>
                 <span class="font-bold">最新可组队同学</span>
               </template>
               <div class="mini-list">
                 <div v-for="s in availableStudents.slice(0, 5)" :key="s.id" class="mini-item" @click="onRowClick(s)">
                   <el-avatar :size="28" style="background: #e0e7ff; color: #4f46e5; font-size: 12px;">{{ s.name?.charAt(0) }}</el-avatar>
                   <span class="mini-name">{{ s.name }}</span>
                   <el-tag size="small" type="success" effect="plain" class="ml-auto">闲置</el-tag>
                 </div>
               </div>
             </el-card>
          </template>

        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Search, Plus, User, UserFilled, Check, Close, RefreshLeft, EditPen,
  CircleCheck, Select, DataAnalysis, InfoFilled, List, Lightning, Filter, Warning
} from '@element-plus/icons-vue'
import { getStudentsByClassName, createStudentGroup, updateStudentGroup } from '@/services/groupApi'

const allStudents = ref([])
const keyword = ref('')
const statusFilter = ref('')
const selectedIds = ref([])
const groupName = ref('')
const taskDescription = ref('')
const className = ref('')

// Pagination State
const currentPage = ref(1)
const pageSize = ref(10)

const groupForm = ref({})
const isCreating = ref(false)
const isSelecting = ref(false)
const isUpdate = ref(false)

const router = useRouter()
const myId = ref(null)
const mySid = ref('')
const myFallbackName = localStorage.getItem('studentName')
const uiStateStorageKey = 'student_groups_ui_state'
const GROUP_STATUS_KEY = 'student_group_status'
const REJECTED_SIDS_KEY = 'rejected_group_member_sids'

onMounted(async () => {
  try {
    className.value = localStorage.getItem('className') || ''
    const ui = JSON.parse(localStorage.getItem(uiStateStorageKey) || 'null')
    if (ui && typeof ui === 'object') {
      isCreating.value = !!ui.creating
      isSelecting.value = !!ui.selecting
    }

    const idStr = localStorage.getItem('userId')
    if (idStr) myId.value = idStr
    const saved = JSON.parse(localStorage.getItem('currentUser') || 'null')
    mySid.value = String(localStorage.getItem('studentNumber') || saved?.studentNumber || saved?.sid || '')

    // 检查是否是编辑模式
    const isEditMode = localStorage.getItem('from_group_edit') === '1'
    const currentLeaderId = localStorage.getItem('current_leader_id')


    const list = await getStudentsByClassName(className.value)



    try {
      const baseIds = JSON.parse(localStorage.getItem('base_member_student_ids') || '[]')
      if (Array.isArray(baseIds) && baseIds.length) {
        // 过滤掉组长ID
        const filteredBaseIds = baseIds.filter(id => {
          const idNum = Number(id)
          const leaderIdNum = Number(currentLeaderId || myId.value)
          return idNum !== leaderIdNum
        })


        const idSet = new Set((list || []).filter(s => filteredBaseIds.includes(Number(s.id) || s.id)).map(s => s.id))
        selectedIds.value = Array.from(idSet)
      }
    } catch {}

    const rejectedSids = JSON.parse(localStorage.getItem(REJECTED_SIDS_KEY) || '[]')
    const rejectedSet = new Set((rejectedSids || []).map(x => String(x)))
    const overrides = JSON.parse(localStorage.getItem('student_status_overrides') || '{}')
    isUpdate.value = (Array.isArray(rejectedSids) && rejectedSids.length > 0) || (overrides && Object.keys(overrides).length > 0)

    allStudents.value = list.map((s, i) => {
      const groupStatusRaw = String(s.groupStatus || s.status || '').toLowerCase()
      let mappedUnavailable = (groupStatusRaw === 'approval' || groupStatusRaw === 'approved' || s.grouped === true || s.status === 'grouped')

      // 在编辑模式下，标记当前用户（组长）为不可选
      const isCurrentUser = isEditMode && (String(s.id) === String(myId.value) || String(s.id) === String(currentLeaderId))
      if (isCurrentUser) {
        mappedUnavailable = true
      }

      const sidStr = String(s.studentNumber || s.sid || '')
      if (sidStr && (rejectedSet.has(sidStr) || overrides[sidStr] === 'available')) mappedUnavailable = false
      return {
        id: s.id || i + 1,
        name: s.name || '-',
        sid: s.studentNumber || '-',
        status: mappedUnavailable ? 'unavailable' : 'available',
        phone: s.phone || '-',
      }
    })
  } catch (e) {
    ElMessage.error(`加载学生列表失败：${e?.message || e}`)
  }
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

// Pagination Logic
const paginatedStudents = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredStudents.value.slice(start, end)
})

// Computed properties to avoid template syntax errors with dot notation
const totalStudentCount = computed(() => allStudents.value.length)
const totalFilteredCount = computed(() => filteredStudents.value.length)
const completionRate = computed(() => {
  const total = allStudents.value.length
  if (!total) return 0
  return Math.round((groupedCount.value / total) * 100) || 0
})

// Reset pagination when filter changes
watch([keyword, statusFilter], () => {
  currentPage.value = 1
})

const availableStudents = computed(() => allStudents.value.filter(s => s.status === 'available'))
const selectedMembers = computed(() => allStudents.value.filter(s => selectedIds.value.includes(s.id)))
const availableCount = computed(() => availableStudents.value.length)
const groupedCount = computed(() => allStudents.value.filter(s => s.status !== 'available').length)
const leaderStudent = computed(() => allStudents.value.find(s => String(s.id) === String(myId.value)))
const leaderName = computed(() => leaderStudent.value?.name || myFallbackName)

const canSubmit = computed(() => {
  const count = selectedMembers.value.length
  return count >= 2 && count <= 5 && groupName.value.trim() && taskDescription.value.trim()
})

function tableRowClassName({ row }) {
  if (isSelected(row.id)) return 'selected-row'
  if (row.status !== 'available') return 'disabled-row'
  return ''
}

function onRowClick(row) {
  if (!isCreating) return
  if (!isSelecting && isCreating) return
  toggleSelect(row)
}

function isSelf(stu) {
  if (!stu) return false
  if (mySid.value) return String(stu.sid) === String(mySid.value)
  return !!(myId.value != null && String(stu.id) === String(myId.value))
}

function isSelected(id) {
  return selectedIds.value.includes(id)
}

function toggleSelect(stu) {
  if (stu.status !== 'available' && !isSelected(stu.id)) return
  if (isSelf(stu)) return

  const idx = selectedIds.value.indexOf(stu.id)
  if (idx >= 0) {
    selectedIds.value.splice(idx, 1)
  } else {
    if (selectedIds.value.length >= 5) {
      ElMessage.warning('最多只能选择5名组员')
      return
    }
    selectedIds.value.push(stu.id)
  }
}

function startCreate() {
  isCreating.value = true
  isSelecting.value = true
  statusFilter.value = 'available'
  localStorage.setItem(uiStateStorageKey, JSON.stringify({creating: true, selecting: true}))
}

function enterSelecting() {
  isSelecting.value = true
  localStorage.setItem(uiStateStorageKey, JSON.stringify({creating: true, selecting: true}))
}

function finishSelecting() {
  if (selectedMembers.value.length < 2) {
    ElMessage.warning('请至少选择2名组员')
    return
  }
  isSelecting.value = false
  localStorage.setItem(uiStateStorageKey, JSON.stringify({creating: true, selecting: false}))

  // 如果是编辑模式，保存选中的成员并返回编辑页面
  const isEditMode = localStorage.getItem('from_group_edit') === '1'
  if (isEditMode) {
    // 保存选中的成员信息
    const selectedMembersInfo = selectedMembers.value.map(m => ({
      groupId: null, // 会在 MyGroup.vue 中填充
      name: m.name,
      studentId: m.id
    }))
    localStorage.setItem('added_member_infos', JSON.stringify(selectedMembersInfo))

    // 清理临时标记
    localStorage.removeItem('current_leader_id')
    localStorage.removeItem('from_group_edit')

    // 返回编辑页面
    router.push({ name: 'GroupMine' })
  }
}

function cancelCreate() {
  isCreating.value = false
  isSelecting.value = false
  selectedIds.value = []
  statusFilter.value = ''
  localStorage.setItem(uiStateStorageKey, JSON.stringify({creating: false, selecting: false}))

  // 如果是编辑模式，返回编辑页面并清理临时数据
  const isEditMode = localStorage.getItem('from_group_edit') === '1'
  if (isEditMode) {
    localStorage.removeItem('current_leader_id')
    localStorage.removeItem('from_group_edit')
    localStorage.removeItem('base_member_student_ids')
    router.push({ name: 'GroupMine' })
  }
}

async function submitGroup() {
  if (!canSubmit.value) return

  try {
    // 调试日志

    let leaderId = Number(myId.value)

    // 如果 Number(myId.value) 是 NaN 或 0，尝试从 localStorage 获取
    if (!leaderId || leaderId === 0 || isNaN(leaderId)) {
      const localStorageUserId = localStorage.getItem('userId')
      leaderId = Number(localStorageUserId)
    }


    // 验证 leaderId 是否有效
    if (!leaderId || leaderId === 0 || isNaN(leaderId)) {
      ElMessage.error('无法获取用户ID，请重新登录')
      console.error('[创建小组] leaderId 无效:', leaderId)
      return
    }

    const members = selectedMembers.value
    const payload = {
      groupName: groupName.value,
      groupLeaderId: leaderId,
      groupDescription: taskDescription.value,
      memberIds: members.map(m => m.id)
    }


    const res = isUpdate.value ? await updateStudentGroup(payload) : await createStudentGroup(payload)
    if (Number(res?.code ?? res?.status) === 200) {
      localStorage.setItem(GROUP_STATUS_KEY, 'pending')
      ElMessage.success('提交成功，等待审核')

      // 清理编辑模式的临时数据
      const isEditMode = localStorage.getItem('from_group_edit') === '1'
      if (isEditMode) {
        localStorage.removeItem('current_leader_id')
        localStorage.removeItem('from_group_edit')
        localStorage.removeItem('base_member_student_ids')
        localStorage.removeItem('added_member_infos')
      }

      // 重置状态
      isCreating.value = false
      isSelecting.value = false
      selectedIds.value = []
      statusFilter.value = ''
      localStorage.setItem(uiStateStorageKey, JSON.stringify({creating: false, selecting: false}))

      router.push('/group/mine')
    } else {
      ElMessage.error(`提交失败：${res?.message || '未知错误'}`)
    }
  } catch (e) {
    ElMessage.error('系统繁忙，请重试')
  }
}
</script>

<style scoped>
.group-build {
  background-color: #f1f5f9;
  min-height: 100vh;
  padding: 24px;
  box-sizing: border-box;
}

/* Header */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-left h2 {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 6px 0;
  display: flex;
  align-items: center;
  gap: 10px;
}
.header-icon { color: #3b82f6; }
.subtitle {
  color: #64748b;
  font-size: 14px;
  margin: 0;
}
.header-right {
  display: flex;
  align-items: center;
  background: white;
  padding: 8px 16px;
  border-radius: 99px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  gap: 12px;
}
.info-tag {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}
.info-tag .label { font-size: 11px; color: #94a3b8; text-transform: uppercase; font-weight: 600; }
.info-tag .value { font-size: 14px; color: #334155; font-weight: 600; }

/* Stats Row */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.stat-item {
  background: white;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  transition: transform 0.2s;
}
.stat-item:hover { transform: translateY(-2px); box-shadow: 0 4px 6px rgba(0,0,0,0.05); }
.stat-icon-wrapper {
  width: 48px; height: 48px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px;
}
.stat-info { display: flex; flex-direction: column; }
.stat-num { font-size: 20px; font-weight: 700; color: #0f172a; line-height: 1.2; }
.stat-desc { font-size: 13px; color: #64748b; }

.bg-blue-subtle { background: #eff6ff; } .text-blue { color: #3b82f6; }
.bg-green-subtle { background: #f0fdf4; } .text-green { color: #22c55e; }
.bg-orange-subtle { background: #fff7ed; } .text-orange { color: #f97316; }
.bg-purple-subtle { background: #faf5ff; } .text-purple { color: #a855f7; }

/* Main Layout */
.main-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}
.main-workspace {
  flex: 1;
  min-width: 0;
}
.sidebar {
  width: 340px;
  flex-shrink: 0;
}
.sticky-wrapper {
  position: sticky;
  top: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Workspace View (Table) */
.main-card {
  border-radius: 12px;
  border: none;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
}
.filter-inputs { display: flex; gap: 12px; align-items: center; }
.create-btn { padding-left: 20px; padding-right: 20px; font-weight: 600; }

.status-indicator {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 13px; font-weight: 500;
}
.status-indicator .dot { width: 6px; height: 6px; border-radius: 50%; }
.status-indicator.is-active { color: #059669; }
.status-indicator.is-active .dot { background: #059669; }
.status-indicator.is-disabled { color: #94a3b8; }
.status-indicator.is-disabled .dot { background: #cbd5e1; }

.table-footer {
  padding-top: 16px;
  text-align: center;
  display: flex;
  justify-content: center;
}

:deep(.selected-row) { background-color: #f0f9ff !important; }
:deep(.disabled-row) { opacity: 0.6; background-color: #f8fafc; }
.ml-2 { margin-left: 8px; }

/* Workspace View (Form) */
.form-mode :deep(.el-card__header) {
  border-bottom: 1px solid #f1f5f9;
  padding: 16px 20px;
}
.card-title { font-size: 16px; font-weight: 600; display: flex; align-items: center; gap: 8px; }
.form-preview-section {
  background: #f8fafc;
  border-radius: 8px;
  padding: 16px;
  margin-top: 8px;
  margin-bottom: 24px;
}
.section-title { margin: 0 0 12px 0; font-size: 14px; color: #475569; }
.preview-members {
  display: flex; flex-wrap: wrap; gap: 12px;
}
.preview-card {
  position: relative;
  background: white; border: 1px solid #e2e8f0;
  border-radius: 8px; width: 140px; padding: 16px 10px;
  display: flex; flex-direction: column; align-items: center;
  transition: all 0.2s;
}
.preview-card.leader { border-color: #bfdbfe; background: #eff6ff; }
.role-badge {
  position: absolute; top: 8px; right: 8px;
  font-size: 10px; padding: 2px 6px; border-radius: 4px; font-weight: 700;
}
.leader .role-badge { background: #dbeafe; color: #2563eb; }
.member-badge { background: #f1f5f9; color: #64748b; }
.member-avatar { margin-bottom: 8px; background: #6366f1; font-size: 16px; }
.leader-avatar { background: #3b82f6; }
.member-details { text-align: center; }
.member-details .name { display: block; font-size: 14px; font-weight: 600; color: #334155; margin-bottom: 2px; }
.member-details .sid { display: block; font-size: 11px; color: #94a3b8; }
.remove-btn { position: absolute; top: -6px; left: -6px; opacity: 0; transition: opacity 0.2s; z-index: 2; transform: scale(0.8); }
.preview-card:hover .remove-btn { opacity: 1; }
.add-placeholder {
  border-style: dashed; cursor: pointer; color: #94a3b8; justify-content: center; gap: 4px;
}
.add-placeholder:hover { border-color: #3b82f6; color: #3b82f6; }

.form-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 16px; }

/* Sidebar */
.sidebar-card { border-radius: 12px; border: none; box-shadow: 0 1px 3px rgba(0,0,0,0.04); margin-bottom: 16px; }
.sidebar-card :deep(.el-card__header) { padding: 14px 16px; border-bottom: 1px solid #f1f5f9; }

/* Progress Card */
.progress-header { display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 14px; font-weight: 600; color: #334155; }
.progress-status-text { margin-top: 12px; font-size: 12px; display: flex; align-items: center; gap: 6px; }

/* Selected List Card */
.card-header-row { display: flex; justify-content: space-between; align-items: center; font-weight: 600; font-size: 14px; }
.selected-list-scroll { max-height: 300px; overflow-y: auto; padding: 0 4px; }
.selected-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 0; border-bottom: 1px solid #f8fafc;
}
.selected-item:last-child { border-bottom: none; }
.item-left { display: flex; align-items: center; gap: 10px; }
.bg-gradient { background: linear-gradient(135deg, #818cf8 0%, #6366f1 100%); font-size: 12px; }
.item-info .name { font-size: 13px; font-weight: 500; color: #334155; }
.item-info .sid { font-size: 11px; color: #94a3b8; }

/* Welcome / Mini List */
.welcome-content { text-align: center; padding: 10px 0; }
.welcome-content h3 { margin: 0 0 8px 0; font-size: 16px; color: #1e293b; }
.welcome-content p { font-size: 13px; color: #64748b; margin-bottom: 16px; line-height: 1.5; }

.mini-item { display: flex; align-items: center; gap: 10px; padding: 10px 0; border-bottom: 1px solid #f1f5f9; cursor: pointer; transition: background 0.2s; }
.mini-item:hover { background: #f8fafc; padding-left: 4px; padding-right: 4px; border-radius: 6px; }
.mini-name { font-size: 13px; font-weight: 500; color: #475569; }

.mt-2 { margin-top: 8px; }
.w-full { width: 100%; }
.ml-auto { margin-left: auto; }

/* Fix Sidebar Actions Alignment */
.sidebar-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.sidebar-actions .el-button {
  margin-left: 0 !important; /* Force override Element Plus default spacing */
  width: 100%;
}
</style>