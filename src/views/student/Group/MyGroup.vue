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
          <el-button type="primary" @click="reselectAfterRejected">更改小组信息</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const { proxy } = getCurrentInstance()
const BASE_URL = proxy?.$baseUrl
const router = useRouter()

const groupStatus = ref('none')
const createdGroup = ref(null)
// 保存当前小组成员的学号（含组长）
const currentGroupMemberSids = ref([])

onMounted(async () => { await refreshMyGroup() })

async function refreshMyGroup() {
  try {
    const userId = localStorage.getItem('userId')
    if (!userId) { setNone(); return }
    const token = localStorage.getItem('token')
    const headers = {Authorization: `Bearer ${token}`, 'Content-Type': 'multipart/form-data'}
    const fd1 = new FormData(); fd1.append('studentId', userId)
    const res = await axios.post(`${BASE_URL}/groupMember/getById`, fd1, {headers})
    if (!(res?.data?.code === 200) || !res?.data?.data?.groupId) { setNone(); return }

    const groupId = res.data.data.groupId
    const fd2 = new FormData(); fd2.append('groupId', groupId)
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
        groupName: group.groupName || '',
        leaderName,
        memberNames: restNames,
        taskDescription: group.groupDescription || ''
      }
    } else { setNone() }
  } catch { setNone() }
}

function setNone() {
  groupStatus.value = 'none'
  createdGroup.value = null
  currentGroupMemberSids.value = []
}

async function reselectAfterRejected() {
  try {
    const sids = Array.isArray(currentGroupMemberSids.value) ? currentGroupMemberSids.value : []
    const leaderSid = sids[0]
    const memberSids = sids.slice(1)

    // 标记“第一次创建的小组成员”（仅用于显示删除按钮的范围）
    try { localStorage.setItem('first_group_member_sids', JSON.stringify(memberSids)) } catch {}
    try { localStorage.setItem('first_group_leader_sid', String(leaderSid || '')) } catch {}

    // 清理覆盖与被删除名单（进入页面后由用户逐一删除）
    try { localStorage.removeItem('student_status_overrides') } catch {}
    try { localStorage.removeItem('rejected_group_member_sids') } catch {}

    // 进入“新建小组-选择队员”界面
    try { localStorage.setItem('student_groups_ui_state', JSON.stringify({ creating: true, selecting: true })) } catch {}
    router.push({ name: 'GroupBuild' })
  } catch (e) {
    alert(`跳转失败，请重试：${e?.message || e}`)
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
</style>


