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
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from 'vue'
import axios from 'axios'

const { proxy } = getCurrentInstance()
const BASE_URL = proxy?.$baseUrl

const groupStatus = ref('none')
const createdGroup = ref(null)

onMounted(async () => { await refreshMyGroup() })

async function refreshMyGroup() {
  try {
    const userId = localStorage.getItem('userId')
    if (!userId) { setNone(); return }
    const token = localStorage.getItem('token')
    const headers = { Authorization: `Bearer ${token}`, 'Content-Type': 'multipart/form-data' }
    const fd1 = new FormData(); fd1.append('studentId', userId)
    const res = await axios.post(`${BASE_URL}/groupMember/getById`, fd1, { headers })
    console.log(res)
    if (!(res?.data?.code === 200) || !res?.data?.data?.groupId) { setNone(); return }
    const groupId = res.data.data.groupId
    const fd2 = new FormData(); fd2.append('groupId', groupId)
    const response = await axios.post(`${BASE_URL}/student-group/getByGroupId`, fd2, { headers })
    console.log(response)
    if (response?.data?.code === 200 && response?.data?.data) {
      const group = response.data.data
      const st = String(group?.approvalStatus || '').toLowerCase()
      groupStatus.value = (st === 'pending' || st === 'approval' || st === 'approved') ? (st === 'approval' ? 'approved' : st) : 'none'
      createdGroup.value = {
        groupName: group.groupName || '',
        leaderName: group.groupMemberList?.[0]?.studentName || group.leaderName || '',
        memberNames: Array.isArray(group.groupMemberList) ? group.groupMemberList.slice(1).map(m => m.studentName).filter(Boolean) : (group.memberNames || []),
        taskDescription: group.groupDescription || ''
      }
    } else { setNone() }
  } catch { setNone() }
}

function setNone() { groupStatus.value = 'none'; createdGroup.value = null }
</script>

<style scoped>
.block-section { margin-top: 14px; }
.group-summary-card { background:#f8fafc; border:1px solid #e5e7eb; border-radius:8px; padding:12px; }
.group-summary-header { display:flex; align-items:center; gap:12px; flex-wrap:wrap; }
.group-summary-label { font-weight:600; }
.status-pending { background:#ffeaa7; color:#d35400; padding:2px 8px; border-radius:999px; font-size:12px; font-weight:700; }
.status-approved { background:#e8f5e9; color:#2e7d32; padding:2px 8px; border-radius:999px; font-size:12px; font-weight:700; }
.member-chip { display:inline-flex; align-items:center; gap:6px; background:#f1f5f9; border:1px solid #e2e8f0; padding:6px 10px; border-radius:999px; font-size:12px; color:#1f2937; margin-right:6px; }
.leader-mark { background:#eef2ff; color:#2563eb; border:1px solid #c7d2fe; padding:2px 8px; border-radius:999px; font-size:12px; font-weight:700; }
.member-mark { background:#ecfeff; color:#0891b2; border:1px solid #a5f3fc; padding:2px 8px; border-radius:999px; font-size:12px; font-weight:700; }
</style>


