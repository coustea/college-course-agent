<template>
  <div class="group-mine-container">
    <!-- 空状态：未加入小组 -->
    <div v-if="groupStatus === 'none'" class="empty-state-wrapper">
      <el-empty description="你还没有加入任何学习小组" :image-size="200">
        <template #description>
          <p class="empty-text">加入小组可以与同学协作完成任务，快去组建或加入吧！</p>
        </template>
        <el-button type="primary" size="large" @click="$router.push({ name: 'GroupBuild' })">
          去组建小组
        </el-button>
      </el-empty>
    </div>

    <!-- 已加入小组：显示详情 -->
    <div v-else class="group-detail-content">
      <!-- 顶部横幅：小组名称与状态 -->
      <div class="group-header-card">
        <div class="header-left">
          <div class="group-icon">
            <el-icon><collection /></el-icon>
          </div>
          <div class="group-title-section">
            <h1 class="group-name">{{ createdGroup?.groupName || '未命名小组' }}</h1>
            <div class="group-meta">
              <span class="meta-item">
                <el-icon><user /></el-icon> {{ (createdGroup?.allMembers || []).length }} 人
              </span>
              <span class="meta-item divider">|</span>
              <span class="meta-item">ID: {{ currentGroupId }}</span>
              <span class="meta-item divider">|</span>
              <span class="meta-item">创建时间: {{ formatDate(new Date()) }}</span>
            </div>
          </div>
        </div>
        <div class="header-right">
          <div class="status-badge" :class="statusClass">
            <span class="dot"></span>
            {{ statusText }}
          </div>
          <el-button 
            v-if="groupStatus === 'rejected'" 
            type="warning" 
            plain 
            icon="Edit" 
            @click="toggleEditPanel"
          >
            修改小组信息
          </el-button>
        </div>
      </div>

      <!-- 编辑面板 (仅被驳回时显示) -->
      <transition name="el-zoom-in-top">
        <div v-if="groupStatus === 'rejected' && openEditPanel" class="edit-panel-card">
          <div class="panel-header">
            <h3><el-icon><EditPen /></el-icon> 修改申请信息</h3>
            <el-button link @click="openEditPanel = false"><el-icon><Close /></el-icon></el-button>
          </div>
          <div class="panel-body">
            <div class="form-row">
              <label>小组名称</label>
              <el-input v-model="editGroupName" placeholder="请输入小组名称" />
            </div>
            <div class="form-row">
              <label>任务分工</label>
              <el-input type="textarea" v-model="editTaskDesc" :rows="3" placeholder="请详细描述分工" />
            </div>
            <div class="form-row">
              <label>成员管理</label>
              <div class="edit-members-area">
                <el-tag 
                  v-for="m in editableMembers" 
                  :key="m.studentId" 
                  closable 
                  @close="removeMember(m)"
                  type="info"
                  effect="plain"
                  class="edit-member-tag"
                >
                  {{ m.name }}
                </el-tag>
                <el-button size="small" type="primary" plain icon="Plus" @click="goSelectMembers">添加/调整队员</el-button>
              </div>
            </div>
            <div class="form-actions">
              <el-button @click="openEditPanel = false">取消</el-button>
              <el-button type="primary" @click="resubmitNow">提交修改</el-button>
            </div>
          </div>
        </div>
      </transition>

      <div class="content-stack">
        <!-- 任务分工区域 -->
        <el-card shadow="never" class="info-card task-section">
          <template #header>
            <div class="card-header">
              <div class="header-title">
                <el-icon class="header-icon"><List /></el-icon>
                <span>任务分工与描述</span>
              </div>
            </div>
          </template>
          <div class="task-content">
            <p v-if="createdGroup?.taskDescription">{{ createdGroup.taskDescription }}</p>
            <div v-else class="empty-desc">
              <el-icon><EditPen /></el-icon>
              <span>暂无详细分工描述</span>
            </div>
          </div>
        </el-card>

        <!-- 成员列表区域 -->
        <el-card shadow="never" class="info-card members-section">
          <template #header>
            <div class="card-header">
              <div class="header-title">
                <el-icon class="header-icon"><UserFilled /></el-icon>
                <span>小组成员</span>
                <span class="member-count">({{ (createdGroup?.allMembers || []).length }})</span>
              </div>
            </div>
          </template>
          
          <div class="members-grid-container">
            <div 
              v-for="member in createdGroup?.allMembers" 
              :key="member.id" 
              class="member-profile-card"
              :class="{ 'is-leader': member.isLeader }"
            >
              <div class="profile-left">
                <div class="avatar-wrapper" :style="{ backgroundColor: getAvatarColor(member.name) }">
                  {{ member.name?.charAt(0) }}
                </div>
              </div>
              <div class="profile-right">
                <div class="name-row">
                  <span class="member-name">{{ member.name }}</span>
                  <span v-if="member.isLeader" class="role-badge leader">组长</span>
                  <span v-else class="role-badge member">组员</span>
                </div>
                <div class="info-row">
                  <span class="label">学号:</span> {{ member.studentNumber || '-' }}
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>


<script setup>
import { ref, onMounted, getCurrentInstance, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { 
  Collection, User, List, UserFilled, EditPen, Close, Plus 
} from '@element-plus/icons-vue'

const { proxy } = getCurrentInstance()
const BASE_URL = proxy?.$baseUrl
const router = useRouter()

const groupStatus = ref('none')
const createdGroup = ref(null)
const currentGroupId = ref(null)

const openEditPanel = ref(false)
const editGroupName = ref('')
const editTaskDesc = ref('')
const editableMembers = ref([])
const originalMemberIds = ref([])



// 状态显示的计算属性
const statusText = computed(() => {
  const map = {
    'pending': '审核中',
    'approved': '已组建',
    'rejected': '已驳回',
    'none': ''
  }
  return map[groupStatus.value] || groupStatus.value
})

const statusClass = computed(() => {
  return `status-${groupStatus.value}`
})

onMounted(async () => {
  await refreshMyGroup()
})

function formatDate(date) {
  const d = new Date(date)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}



async function refreshMyGroup() {
  try {
    const userId = localStorage.getItem('userId')
    if (!userId) {
      setNone()
      return
    }
    const token = localStorage.getItem('token')
    const headers = {Authorization: `Bearer ${token}`, 'Content-Type': 'multipart/form-data'}
    
    // 1. 获取用户所属小组ID
    const fd1 = new FormData()
    fd1.append('studentId', userId)
    const res = await axios.post(`${BASE_URL}/groupMember/getById`, fd1, {headers})
    
    if (!(res?.data?.code === 200) || !res?.data?.data?.groupId) {
      setNone()
      return
    }

    const groupId = res.data.data.groupId
    currentGroupId.value = groupId

    // 2. 获取小组详细信息
    const fd2 = new FormData();
    fd2.append('groupId', groupId)
    const response = await axios.post(`${BASE_URL}/student-group/getByGroupId`, fd2, {headers})
    
    if (response?.data?.code === 200 && response?.data?.data) {
      const group = response.data.data
      const st = String(group?.approvalStatus || '').toLowerCase()
      groupStatus.value = st === 'approval' ? 'approved' : (st === 'pending' ? 'pending' : (st === 'rejected' ? 'rejected' : 'none'))
      
      // 处理成员数据
      const membersRaw = Array.isArray(group.groupMemberList) ? group.groupMemberList : []
      const leaderId = group.groupLeaderId || membersRaw.find(m => m.role === 'leader' || m.memberRole === 'leader')?.studentId
      
      const allMembersFormatted = membersRaw.map(m => {
        const isLeader = String(m.studentId) === String(leaderId) || m.role === 'leader' || m.isLeader === true
        return {
          name: m.studentName,
          sid: m.studentNumber,
          studentNumber: m.studentNumber, // 添加 studentNumber 字段
          studentId: m.studentId || m.id,
          isLeader: isLeader,
          role: isLeader ? 'leader' : 'member'
        }
      }).sort((a, b) => (b.isLeader ? 1 : 0) - (a.isLeader ? 1 : 0)) 

      createdGroup.value = {
        groupName: group.groupName,
        taskDescription: group.groupDescription,
        allMembers: allMembersFormatted,
        leaderName: allMembersFormatted.find(m => m.isLeader)?.name || '',
        memberNames: allMembersFormatted.filter(m => !m.isLeader).map(m => m.name)
      }



      // 初始化编辑数据
      const draftName = localStorage.getItem('edit_group_name_draft')
      const draftTask = localStorage.getItem('edit_group_task_draft')
      editGroupName.value = (draftName !== null ? draftName : createdGroup.value.groupName)
      editTaskDesc.value = (draftTask !== null ? draftTask : createdGroup.value.taskDescription)
      
      editableMembers.value = allMembersFormatted.filter(m => !m.isLeader).map(m => ({ 
        groupId, 
        name: m.name, 
        studentId: m.studentId 
      }))
      originalMemberIds.value = editableMembers.value.map(m => m.studentId)

      // 合并新增数据
      try {
        const added = JSON.parse(localStorage.getItem('added_member_infos') || '[]')
        if (Array.isArray(added) && added.length) {
          const existSet = new Set(editableMembers.value.map(x => String(x.studentId)))
          const toAdd = added.filter(a => !existSet.has(String(a.studentId))).map(a => ({ 
            groupId, 
            name: a.name, 
            studentId: a.studentId 
          }))
          editableMembers.value = [...editableMembers.value, ...toAdd]
          localStorage.removeItem('added_member_infos')
        }
      } catch {}

      if (localStorage.getItem('group_edit_auto_open') === '1') {
        openEditPanel.value = true
        localStorage.removeItem('group_edit_auto_open')
      }
      
    } else {
      setNone()
    }
  } catch {
    setNone()
  }
}

function setNone() {
  groupStatus.value = 'none'
  createdGroup.value = null
}

function toggleEditPanel() {
  openEditPanel.value = !openEditPanel.value
}

function getAvatarColor(name) {
  const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#7356f1']
  if (!name) return colors[0]
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  return colors[Math.abs(hash) % colors.length]
}

async function removeMember(m) {
  try {
    const groupId = currentGroupId.value
    const studentId = m.studentId
    if (!originalMemberIds.value.includes(studentId)) {
       editableMembers.value = editableMembers.value.filter(x => String(x.studentId) !== String(studentId))
       return
    }
    const res = await axios.delete(`${BASE_URL}/groupMember/${groupId}/${studentId}`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}`, 'Content-Type': 'application/json' }
    })
    if (res.data.code === 200) {
      editableMembers.value = editableMembers.value.filter(x => String(x.studentId) !== String(studentId))
    }
  } catch (e) {
    alert(`删除失败：${e?.message || e}`)
  }
}

function goSelectMembers() {
  localStorage.setItem('edit_group_name_draft', editGroupName.value || '') 
  localStorage.setItem('edit_group_task_draft', editTaskDesc.value || '') 
  localStorage.setItem('from_group_edit', '1') 
  const currentLeaderId = (createdGroup.value?.allMembers || []).find(m => m.isLeader)?.studentId
  const memberIds = editableMembers.value.map(x => x.studentId)
  const allIds = currentLeaderId ? [currentLeaderId, ...memberIds] : memberIds
  localStorage.setItem('base_member_student_ids', JSON.stringify(allIds))
  localStorage.setItem('student_groups_ui_state', JSON.stringify({ creating: true, selecting: true })) 
  router.push({ name: 'GroupBuild' })
}

watch(editGroupName, (v) => localStorage.setItem('edit_group_name_draft', v ?? ''))
watch(editTaskDesc, (v) => localStorage.setItem('edit_group_task_draft', v ?? ''))

async function resubmitNow() {
    try {
      const newLeaderId = Number(localStorage.getItem('userId'))
      const count = editableMembers.value.length
      if (count < 1 || count > 4) {
        alert(`组员人数需为 1~4 人（不含组长），当前为 ${count} 人`)
        return
      }
      const baseSet = new Set(originalMemberIds.value.map(Number))
      let addMemberIds = editableMembers.value.map(m => m.studentId).filter(id => !baseSet.has(Number(id)))
      const payload = {
        groupName: editGroupName.value,
        groupDescription: editTaskDesc.value,
        groupLeaderId: newLeaderId,
        addMemberIds,
        approvalStatus: 'pending'
      }
      const headers = { Authorization: `Bearer ${localStorage.getItem('token')}`, 'Content-Type': 'application/json' }
      const gid = currentGroupId.value
      const resp = await axios.put(`${BASE_URL}/student-group/${gid}/full-update`, payload, { headers })
      
      if (resp.data.code === 200) {
        localStorage.setItem('student_group_status', 'pending')
        groupStatus.value = 'pending'
        openEditPanel.value = false
        alert('已提交修改')
        await refreshMyGroup()
      } else {
        alert(`提交失败：${resp?.data?.message || '未知错误'}`)
      }
    } catch (e) {
      alert(`提交失败：${e?.message || e}`)
    }
}
</script>

<style scoped>
.group-mine-container {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
}

/* Empty State */
.empty-state-wrapper {
  background: white;
  border-radius: 12px;
  padding: 60px 0;
  min-height: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}
.empty-text {
  color: #909399;
  margin-bottom: 20px;
}

/* Header Card */
.group-header-card {
  background: white;
  border-radius: 12px;
  padding: 24px 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 4px 16px rgba(0,0,0,0.04);
  margin-bottom: 24px;
  border-left: 6px solid #409eff;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.group-icon {
  width: 56px;
  height: 56px;
  background: #ecf5ff;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #409eff;
  font-size: 28px;
}

.group-name {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.group-meta {
  color: #909399;
  font-size: 14px;
  display: flex;
  gap: 8px;
  align-items: center;
}
.meta-item { display: flex; align-items: center; gap: 4px; }
.divider { color: #e4e7ed; margin: 0 4px; }

.header-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}

/* Status Badge */
.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
}
.status-badge .dot { width: 8px; height: 8px; border-radius: 50%; margin-right: 8px; }
.status-pending { background: #fff7e6; color: #fa8c16; } .status-pending .dot { background: #fa8c16; }
.status-approved { background: #f6ffed; color: #52c41a; } .status-approved .dot { background: #52c41a; }
.status-rejected { background: #fff1f0; color: #f5222d; } .status-rejected .dot { background: #f5222d; }


/* Content Stack */
.content-stack {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.info-card {
  border-radius: 12px;
  border: none;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.header-icon { color: #409eff; font-size: 18px; }
.member-count { color: #909399; font-size: 14px; font-weight: normal; margin-left: 4px; }

/* Task Content */
.task-content {
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
  min-height: 100px;
  font-size: 14px;
}
.empty-desc { 
  color: #c0c4cc; 
  font-style: italic; 
  display: flex; 
  flex-direction: column; 
  align-items: center; 
  justify-content: center; 
  height: 100%; 
  gap: 8px;
  padding: 20px 0;
}

/* Members Grid */
.members-grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
}

.member-profile-card {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #ebeef5;
  box-shadow: 0 1px 3px rgba(0,0,0,0.02);
  transition: all 0.3s;
}
.member-profile-card:hover { 
  box-shadow: 0 4px 12px rgba(0,0,0,0.08); 
  transform: translateY(-2px); 
  border-color: #dcdfe6;
}
.member-profile-card.is-leader { 
  background: linear-gradient(to right bottom, #ecf5ff, #fff); 
  border-color: #b3d8ff; 
}

.profile-left { margin-right: 16px; }
.avatar-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
  font-weight: bold;
  box-shadow: 0 2px 6px rgba(0,0,0,0.1);
}

.profile-right { flex: 1; overflow: hidden; }
.name-row { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.member-name { font-weight: 600; color: #303133; font-size: 16px; }
.role-badge { font-size: 11px; padding: 2px 8px; border-radius: 10px; font-weight: 600; }
.role-badge.leader { background: #e6f7ff; color: #1890ff; border: 1px solid #91d5ff; }
.role-badge.member { background: #f5f5f5; color: #909399; border: 1px solid #e8e8e8; }
.info-row { font-size: 13px; color: #606266; }
.label { color: #909399; margin-right: 4px; }

/* Edit Panel */
.edit-panel-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  border: 1px solid #e4e7ed;
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 12px;
}
.panel-header h3 { margin: 0; color: #303133; display: flex; align-items: center; gap: 8px; font-size: 18px;}
.form-row { margin-bottom: 20px; }
.form-row label { display: block; margin-bottom: 8px; font-weight: 600; color: #606266; }
.edit-members-area { display: flex; flex-wrap: wrap; gap: 8px; padding: 12px; background: #f9fafe; border-radius: 8px; border: 1px dashed #dcdfe6; }
.edit-member-tag { font-size: 13px; padding: 6px 12px; height: auto; }
.form-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 24px; }

/* Responsive */
@media (max-width: 768px) {
  .group-header-card { flex-direction: column; align-items: flex-start; gap: 16px; }
  .header-right { align-items: flex-start; width: 100%; flex-direction: row; justify-content: space-between; }
  .members-grid-container { grid-template-columns: 1fr; }
}
</style>