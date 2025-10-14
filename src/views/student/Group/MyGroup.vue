<template>
  <div class="group-mine">
    <div class="group-summary block-section">
      <div v-if="groupStatus==='none'" class="group-summary-card">你还未进入小组</div>
      <div v-else class="group-summary-card">
        <div class="group-summary-header">
          <span class="group-summary-label">我的小组名称：</span>
          <span>{{ createdGroup?.groupName || '—' }}</span>
          <span class="status-chip status-pending" v-if="groupStatus==='pending'">审批中</span>
          <span class="status-chip status-approved" v-if="groupStatus==='approved'">已组队</span>
          <span class="status-chip status-rejected" v-if="groupStatus==='rejected'">已驳回</span>
        </div>
        <div class="group-summary-members">
          <span class="group-summary-label">成员：</span>
          <span class="member-chip" v-if="createdGroup?.leaderName">
            <span class="member-name">{{ createdGroup.leaderName }}</span>
            <span class="leader-mark">组长</span>
          </span>
          <span class="member-chip" v-for="(n,i) in (createdGroup?.memberNames||[])" :key="i">
            <span class="member-name">{{ n }}</span>
            <span class="member-mark">组员</span>
          </span>
        </div>
        <div class="group-summary-tasks">
          <span class="group-summary-label">分工：</span>
          <span>{{ createdGroup?.taskDescription || '—' }}</span>
        </div>
        <div class="group-summary-actions" v-if="groupStatus==='rejected'">
          <el-button type="primary" @click="toggleEditPanel">更改小组信息</el-button>
        </div>
        <div v-if="groupStatus==='rejected' && openEditPanel" class="group-edit-panel">
          <div class="panel-row">
            <label class="row-label">小组名称</label>
            <el-input v-model="editGroupName" placeholder="请输入小组名称" />
          </div>
          <div class="panel-row">
            <label class="row-label">分工描述</label>
            <el-input type="textarea" v-model="editTaskDesc" :rows="3" placeholder="请输入分工描述" />
          </div>
          <div class="panel-row">
            <label class="row-label">小组成员</label>
            <div class="members-list">
              <span class="member-chip">
                <span class="member-name">{{ createdGroup?.leaderName }}</span>
                <span class="leader-mark">组长</span>
              </span>
              <span v-for="m in editableMembers" :key="m.sid" class="member-chip">
                <span class="member-name">{{ m.name }}</span>
                <el-button size="small" type="danger" link @click="removeMember(m)">删除</el-button>
              </span>
            </div>
          </div>
          <div class="panel-actions">
            <div class="left-actions">
              <el-button type="primary" @click="goSelectMembers">选择队员</el-button>
              <el-button @click="openEditPanel=false">收起</el-button>
            </div>
            <el-button type="success" @click="resubmitNow">再次提交</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance, watch } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const { proxy } = getCurrentInstance()
const BASE_URL = proxy?.$baseUrl
console.log(BASE_URL)
const router = useRouter()

const groupStatus = ref('none')
const createdGroup = ref(null)
const currentGroupId = ref(null)

const currentGroupMemberSids = ref([])

const openEditPanel = ref(false)
const editGroupName = ref('')
const editTaskDesc = ref('')
const editableMembers = ref([])
// 记录初始（加载时）的小组非组长成员，用于与当前列表比较，避免必须先删除旧成员
const originalMemberIds = ref([])

onMounted(async () => {
  await refreshMyGroup()
})

async function refreshMyGroup() {
  try {
    const userId = localStorage.getItem('userId')
    if (!userId) {
      setNone()
      return
    }
    const token = localStorage.getItem('token')
    const headers = {Authorization: `Bearer ${token}`, 'Content-Type': 'multipart/form-data'}
    const fd1 = new FormData()
    fd1.append('studentId', userId)
    const res = await axios.post(`${BASE_URL}/groupMember/getById`, fd1, {headers})
    if (!(res?.data?.code === 200) || !res?.data?.data?.groupId) {
      setNone()
      return
    }

    const groupId = res.data.data.groupId
    currentGroupId.value = groupId
    const fd2 = new FormData();
    fd2.append('groupId', groupId)
    const response = await axios.post(`${BASE_URL}/student-group/getByGroupId`, fd2, {headers})
    if (response?.data?.code === 200 && response?.data?.data) {
      const group = response.data.data
      const st = String(group?.approvalStatus || '').toLowerCase()
      groupStatus.value = st === 'approval' ? 'approved' : (st === 'pending' ? 'pending' : (st === 'rejected' ? 'rejected' : 'none'))
      // 成员信息与学号收集（基于角色判断）
      const members = Array.isArray(group.groupMemberList) ? group.groupMemberList : []
      const roleOf = (m) => String(m?.role || m?.memberRole || m?.position || '').toLowerCase()
      let leaderIdx = members.findIndex(m => m?.isLeader === true || m?.leader === true || roleOf(m) === 'leader')
      if (leaderIdx < 0) leaderIdx = 0
      const leaderItem = members[leaderIdx] || {}
      const leaderName = leaderItem?.studentName || group.leaderName || ''
      const leaderSid = String(leaderItem?.studentNumber || '')
      const rest = members.filter((_, i) => i !== leaderIdx)
      const restNames = rest.map(m => m.studentName).filter(Boolean)
      const restSids = rest.map(m => m.studentNumber).filter(Boolean)
      currentGroupMemberSids.value = [leaderSid, ...restSids].filter(Boolean)
      createdGroup.value = {
        groupName: group.groupName,
        leaderName,
        memberNames: restNames,
        taskDescription: group.groupDescription
      }
      // 初始化编辑面板草稿（优先使用本地草稿，避免返回后被覆盖）
      const draftName = localStorage.getItem('edit_group_name_draft')
      const draftTask = localStorage.getItem('edit_group_task_draft')
      editGroupName.value = (draftName !== null ? draftName : createdGroup.value.groupName)
      editTaskDesc.value = (draftTask !== null ? draftTask : createdGroup.value.taskDescription)
      editableMembers.value = rest.map(m => ({ groupId, name: m.studentName, studentId: m.studentId || m.id }))
      originalMemberIds.value = rest.map(m => m.studentId || m.id).filter(v => v != null)
      // 合并“新增选择”的临时成员（仅前端展示，未提交前不落库）
      try {
        const added = JSON.parse(localStorage.getItem('added_member_infos') || '[]')
        if (Array.isArray(added) && added.length) {
          const existSet = new Set((editableMembers.value || []).map(x => String(x.studentId)))
          const toAdd = added.filter(a => !existSet.has(String(a.studentId))).map(a => ({ groupId, name: a.name, studentId: a.studentId }))
          editableMembers.value = [...editableMembers.value, ...toAdd]
          localStorage.removeItem('added_member_infos')
        }
      } catch {}
      // 若从选择页返回，自动展开面板
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
  currentGroupMemberSids.value = []
}

function toggleEditPanel() {
  openEditPanel.value = !openEditPanel.value
}

console.log("该小组成员信息editableMembers:", editableMembers)

async function removeMember(m) {
  console.log("删除的成员信息m:", m)
  try {
    const groupId = currentGroupId.value
    const studentId = m.studentId || m.id
    console.log('删除这个学生的小组id:', groupId + "学生id:" + studentId)
   const res = await axios.delete(`${BASE_URL}/groupMember/${groupId}/${studentId}`, {
      headers: {
        Authorization: `Bearer ${localStorage.getItem('token')}`,
        'Content-Type': 'application/json'
      }
    })
    console.log("删除成员resp:", res)
    if (res.data.code === 200) {
      editableMembers.value = editableMembers.value.filter(x => String(x.studentId) !== String(studentId))
    }
  } catch (e) {
    alert(`删除失败，请重试：${e?.message || e}`)
  }
}

function goSelectMembers() {
  localStorage.setItem('edit_group_name_draft', editGroupName.value || '') 
  localStorage.setItem('edit_group_task_draft', editTaskDesc.value || '') 
  localStorage.setItem('from_group_edit', '1') 
  // 保存未删除成员的学生ID，供新建页作为基础选择
  localStorage.setItem('base_member_student_ids', JSON.stringify((editableMembers.value||[]).map(x=>x.studentId)))
  localStorage.setItem('student_groups_ui_state', JSON.stringify({ creating: true, selecting: true })) 
  router.push({ name: 'GroupBuild' })
}

// 实时保存草稿，避免点击跳转时丢失最新输入
watch(editGroupName, (v) => {
  localStorage.setItem('edit_group_name_draft', v ?? '') 
})
watch(editTaskDesc, (v) => {
  localStorage.setItem('edit_group_task_draft', v ?? '') 
})

async function resubmitNow() {
    try {
      const newLeaderId = Number(localStorage.getItem('userId')) || undefined
      // 总队员数量校验（不含组长）：对当前展示列表做限制
      const count = (editableMembers.value || []).length
      if (count < 2 || count > 5) {
        alert(`组员人数需为 2~5 人（不含组长），当前为 ${count} 人`)
        return
      }

      // 仅提交“新增”的成员：当前列表 - 初始列表
      const baseSet = new Set((originalMemberIds.value || []).map(Number))
      let addMemberIds = Array.from(new Set(
        (editableMembers.value || [])
          .map(m => m?.studentId)
          .filter(v => v != null)
          .filter(id => !baseSet.has(Number(id)))
      ))

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
      console.log("再次提交小组resp:", resp)
      console.log("再次提交小组返回:", resp.data)
      if (resp.data.code === 200) {
        localStorage.setItem('student_group_status', 'pending')
        // 根据后端返回状态更新显示
        const statusRaw = String(resp?.data?.data?.approvalStatus).toLowerCase()
        groupStatus.value = statusRaw === 'approval' ? 'approved' : (statusRaw === 'pending' ? 'pending' : (statusRaw === 'rejected' ? 'rejected' : 'pending'))
        
        openEditPanel.value = false
        alert('已提交小组审批')
        await refreshMyGroup()
      } else {
        alert(`提交失败：${resp?.data?.message ||  '未知错误'}`)
      }
    } catch (e) {
      alert(`提交失败，请稍后再试：${e?.message || e}`)
    }
}
</script>

<style scoped>
.block-section {
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

.status-pending {
  background: #ffeaa7;
  color: #d35400;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.status-approved {
  background: #e8f5e9;
  color: #2e7d32;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.status-rejected {
  background: #fee2e2;
  color: #b91c1c;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
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
  margin-right: 6px;
}

.leader-mark {
  background: #eef2ff;
  color: #2563eb;
  border: 1px solid #c7d2fe;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.member-mark {
  background: #ecfeff;
  color: #0891b2;
  border: 1px solid #a5f3fc;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.group-summary-actions {
  margin-top: 10px;
}

.group-edit-panel {
  margin-top: 12px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 14px;
}
.panel-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin: 10px 0;
}
.row-label {
  width: 88px;
  text-align: right;
  color: #374151;
  font-weight: 600;
  line-height: 32px;
}

.members-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.group-edit-panel .member-chip {
  background: #fff;
  border: 1px solid #e5e7eb;
  padding: 6px 10px;
  border-radius: 999px;
}

.panel-actions {
  margin-top: 12px;
  display: flex;
  gap: 10px;
  justify-content: space-between;
}
.panel-actions .left-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

</style>


