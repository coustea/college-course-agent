<template>
  <div class="students-groups-container">
    <!-- 顶部统计卡片 -->
    <div class="stats-header">
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat-card total">
            <div class="stat-icon-bg">
              <el-icon><DataLine /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">分组总数</div>
              <div class="stat-value">{{ groups.length }}</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card pending">
            <div class="stat-icon-bg">
              <el-icon><Timer /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">待审批</div>
              <div class="stat-value">{{ pendingGroupsCount }}</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card approved">
            <div class="stat-icon-bg">
              <el-icon><Check /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">已通过</div>
              <div class="stat-value">{{ approvedCount }}</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card rejected">
            <div class="stat-icon-bg">
              <el-icon><Close /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">已驳回</div>
              <div class="stat-value">{{ rejectedCount }}</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 主内容区 -->
    <div class="main-content-card">
      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="left-tools">
          <h3 class="card-title">分组列表</h3>
          <el-radio-group v-model="applicationStatusFilter" @change="filterGroups" class="status-filter">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="pending">待审批</el-radio-button>
            <el-radio-button label="approval">已通过</el-radio-button>
            <el-radio-button label="rejected">已驳回</el-radio-button>
          </el-radio-group>
        </div>
        <div class="right-tools">
          <el-select
            v-model="courseFilter"
            placeholder="按课程筛选"
            clearable
            style="width: 180px"
            @change="filterGroups"
          >
            <el-option
                v-for="course in courses"
                :key="course.id"
                :label="course.name"
                :value="course.id"
            />
          </el-select>
          <el-button type="primary" :icon="Search" circle @click="fetchGroups" title="刷新数据" />
        </div>
      </div>

      <!-- 表格区域 -->
      <div class="table-wrapper">
        <el-table
          :data="paginatedGroups"
          style="width: 100%"
          v-loading="loading"
          :header-cell-style="{ background: '#f8fafc', color: '#64748b', fontWeight: '600' }"
          row-key="id"
        >
          <el-table-column type="index" label="序号" width="60" align="center"/>

          <el-table-column prop="name" label="队伍名称" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="group-name-text">{{ row.name }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="courseName" label="所属课程" min-width="140" show-overflow-tooltip />

          <el-table-column label="组长" width="130">
            <template #default="{ row }">
              <div class="leader-info">
                <el-avatar :size="24" class="leader-avatar" :style="{background: getAvatarColor(row.leaderName)}">
                  {{ row.leaderName?.charAt(0) }}
                </el-avatar>
                <span class="leader-name">{{ row.leaderName }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="组员" min-width="220">
            <template #default="{ row }">
              <div class="members-preview">
                <el-tag
                  v-for="m in (row.memberList || []).filter(x => x.role !== 'leader').slice(0, 3)"
                  :key="m.id"
                  size="small"
                  effect="plain"
                  class="member-tag-item"
                >
                  {{ m.name }}
                </el-tag>
                <el-tag v-if="(row.memberList || []).filter(x => x.role !== 'leader').length > 3" size="small" type="info" effect="plain" class="member-tag-more">
                  +{{ (row.memberList || []).filter(x => x.role !== 'leader').length - 3 }}
                </el-tag>
                <span v-if="(row.memberList || []).filter(x => x.role !== 'leader').length === 0" class="no-members">-</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="memberCount" label="人数" width="80" align="center" />

          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.status)" effect="dark" size="small" class="status-tag-rounded">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="180" fixed="right" align="center">
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button link type="primary" @click="viewGroupDetails(row)">详情</el-button>
                <template v-if="row.status === 'pending'">
                  <el-divider direction="vertical" />
                  <el-button link type="success" @click="approveGroup(row)">同意</el-button>
                  <el-divider direction="vertical" />
                  <el-button link type="danger" @click="rejectGroup(row)">驳回</el-button>
                </template>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页 -->
      <div class="pagination-footer">
        <el-pagination
            v-if="filteredGroups.length > 0"
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="filteredGroups.length"
            layout="total, sizes, prev, pager, next, jumper"
            background
        />
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog
        v-model="showDetailsDialog"
        title="分组详情"
        width="600px"
        class="group-detail-dialog"
        destroy-on-close
        align-center
    >
      <div v-if="currentGroupDetails" class="detail-container">
        <!-- 弹窗顶部信息 -->
        <div class="detail-header">
          <div class="dh-left">
            <div class="dh-avatar" :style="{background: getAvatarColor(currentGroupDetails.name)}">
              {{ currentGroupDetails.name?.charAt(0) }}
            </div>
            <div class="dh-info">
              <div class="dh-name">{{ currentGroupDetails.name }}</div>
              <div class="dh-course">
                <el-icon><Collection /></el-icon> {{ currentGroupDetails.courseName }}
              </div>
            </div>
          </div>
          <div class="dh-right">
            <el-tag :type="getStatusTagType(currentGroupDetails.status)" size="large" effect="dark">
              {{ getStatusText(currentGroupDetails.status) }}
            </el-tag>
          </div>
        </div>

        <el-divider style="margin: 16px 0;" />

        <el-descriptions :column="2" border class="custom-desc">
          <el-descriptions-item label="组长" :span="1">
            <span class="desc-leader">{{ currentGroupDetails.leaderName }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="总人数" :span="1">{{ currentGroupDetails.memberCount }} 人</el-descriptions-item>
          <el-descriptions-item label="分工描述" :span="2">
            <div class="desc-text">{{ currentGroupDetails.description || '暂无分工描述' }}</div>
          </el-descriptions-item>
        </el-descriptions>

        <div class="section-title mt-4">
          <span class="title-text">成员列表</span>
          <span class="title-count">({{ currentGroupDetails.memberList?.length || 0 }})</span>
        </div>

        <div class="members-grid">
          <div v-for="m in currentGroupDetails.memberList" :key="m.id" class="member-card-mini" :class="{'is-leader': m.role === 'leader'}">
            <el-avatar :size="32" class="member-avatar" :style="{background: getAvatarColor(m.name)}">{{ m.name?.charAt(0) }}</el-avatar>
            <div class="member-info">
              <div class="m-name">{{ m.name }}</div>
              <div class="m-role" v-if="m.role === 'leader'">组长</div>
              <div class="m-role text-gray" v-else>组员</div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showDetailsDialog = false">关闭</el-button>
          <template v-if="currentGroupDetails?.status === 'pending'">
            <el-button type="danger" plain @click="rejectGroup(currentGroupDetails)">驳回申请</el-button>
            <el-button type="success" @click="approveGroup(currentGroupDetails)">同意申请</el-button>
          </template>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import {ref, computed, onMounted} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {Search, DataLine, Timer, Check, Close, Collection} from '@element-plus/icons-vue'
import axios from 'axios'

const API_BASE = (import.meta?.env?.VITE_API_BASE_URL || (window?.location?.port === '4173' ? 'http://192.168.52.75:9999/api' : '/api'))
const api = axios.create({ baseURL: API_BASE, timeout: 20000 })
api.interceptors.request.use((config) => {
  try {
    const token = localStorage.getItem('token') || localStorage.getItem('userToken')
    if (token) config.headers = { ...(config.headers || {}), Authorization: `Bearer ${token}` }
  } catch {}
  return config
})

export default {
  name: 'StudentsGroups',
  components: {
    Search, DataLine, Timer, Check, Close, Collection
  },
  setup() {
    const loading = ref(false)
    const courseFilter = ref('')
    const applicationStatusFilter = ref('')
    const currentPage = ref(1)
    const pageSize = ref(10)
    const showDetailsDialog = ref(false)
    const currentGroupDetails = ref(null)

    // 数据
    const groups = ref([])
    const courses = ref([])

    // 获取状态标签类型
    const getStatusTagType = (status) => {
      switch (status) {
        case 'pending': return 'warning'
        case 'approval':
        case 'approved': return 'success'
        case 'rejected': return 'danger'
        default: return 'info'
      }
    }

    // 获取状态文本
    const getStatusText = (status) => {
      switch (status) {
        case 'pending': return '待审批'
        case 'approval':
        case 'approved': return '已通过'
        case 'rejected': return '已驳回'
        default: return '未知'
      }
    }

    // 随机头像颜色
    const getAvatarColor = (name) => {
      const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#7356f1']
      if (!name) return colors[0]
      let hash = 0
      for (let i = 0; i < name.length; i++) {
        hash = name.charCodeAt(i) + ((hash << 5) - hash)
      }
      return colors[Math.abs(hash) % colors.length]
    }

    // 统计数据
    const pendingGroupsCount = computed(() => groups.value.filter(g => g.status === 'pending').length)
    const approvedCount = computed(() => groups.value.filter(g => g.status === 'approval' || g.status === 'approved').length)
    const rejectedCount = computed(() => groups.value.filter(g => g.status === 'rejected').length)

    // 更新本地存储的未审批数量
    const updatePendingCountStorage = () => {
      localStorage.setItem('pendingGroupsCount', pendingGroupsCount.value.toString())
      window.dispatchEvent(new CustomEvent('pendingGroupsCountUpdated', {
        detail: {count: pendingGroupsCount.value}
      }))
    }

    // 过滤分组
    const filteredGroups = computed(() => {
      let result = groups.value
      if (courseFilter.value) {
        result = result.filter(group => group.courseId == courseFilter.value)
      }
      if (applicationStatusFilter.value) {
        result = result.filter(group => group.status === applicationStatusFilter.value)
      }
      return result
    })

    // 分页后的分组
    const paginatedGroups = computed(() => {
      const start = (currentPage.value - 1) * pageSize.value
      const end = start + pageSize.value
      return filteredGroups.value.slice(start, end)
    })

    const fetchGroups = async () => {
      loading.value = true
      try {
        const params = {}
        // 注意：后端可能需要 approvalStatus 参数来过滤，这里先获取全部再前端过滤，或者根据API调整
        // 假设这里获取全部状态
        const result = await api.post('/student-group/approvalStatus', null, { params })

        const raw = result?.data
        const list = Array.isArray(raw?.data) ? raw.data : (Array.isArray(raw) ? raw : [])

        const groupsWithDetails = list.map((g) => {
          const id = g.id || g.groupId || g.group_id
          const courseIdVal = g.courseId || g.course_id || g.course?.id
          const courseName = g.courseName || g.course_name || g.course?.courseName || g.course?.name || '未知课程'

          const gmRaw = Array.isArray(g.groupMemberList) ? g.groupMemberList : (Array.isArray(g.memberList) ? g.memberList : [])
          const memberListFromGroup = gmRaw.map(m => ({
            id: m.studentId != null ? m.studentId : (m.id != null ? m.id : undefined),
            name: m.studentName || m.name || m.username || '',
            role: m.role,
            joinStatus: m.joinStatus
          }))

          const fallbackMemberRaw = g.members || g.memberList || g.students || []
          const fallbackMemberList = Array.isArray(fallbackMemberRaw)
              ? fallbackMemberRaw.map(m => (typeof m === 'string' ? { name: m } : m))
              : String(fallbackMemberRaw || '').split(',').filter(Boolean).map(n => ({ name: n.trim() }))

          const normalizedMemberList = memberListFromGroup.length > 0 ? memberListFromGroup : fallbackMemberList

          const leaderFromMembers = normalizedMemberList.find(m => m.role === 'leader')
          const leaderIdVal = g.groupLeaderId || g.leaderId || leaderFromMembers?.id
          const leaderName = g.leaderName || leaderFromMembers?.name || '未知'

          const membersOnly = normalizedMemberList.filter(m => m.role !== 'leader')
          const members = membersOnly.map(m => m.name || m.username || '').filter(Boolean).join(', ')

          const memberCount = normalizedMemberList.length
          const approvalStatus = g.approvalStatus || g.groupApprovalStatus || g.approval_status || g.approvalstatus || ''
          const status = approvalStatus || g.state || ''

          return {
            id,
            name: g.name || g.groupName || `分组#${id ?? ''}`,
            courseId: courseIdVal,
            courseName,
            leaderId: leaderIdVal,
            leaderName,
            description: g.description || g.groupDescription || g.remark || '',
            members,
            memberList: normalizedMemberList,
            memberCount,
            status,
          }
        })

        groups.value = groupsWithDetails

        const uniqueCourses = new Map()
        for (const g of groupsWithDetails) {
          if (!uniqueCourses.has(g.courseId)) uniqueCourses.set(g.courseId, { id: g.courseId, name: g.courseName })
        }
        courses.value = Array.from(uniqueCourses.values()).filter(c => c.id != null)

        updatePendingCountStorage()
      } catch (error) {
        console.error('获取数据失败:', error)
        ElMessage.error('获取数据失败')
      } finally {
        loading.value = false
      }
    }

    const viewGroupDetails = (group) => {
      currentGroupDetails.value = group
      showDetailsDialog.value = true
    }

    const filterGroups = () => {
      currentPage.value = 1
    }

    const approveGroup = async (group) => {
      try {
        const gid = group?.id || group?.groupId || group?.group_id
        if (!gid) { ElMessage.error('缺少分组ID'); return }

        await ElMessageBox.confirm(
            `确定要同意"${group.name}"的分组申请吗？`, '确认操作',
            { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
        )

        const payload = {
          groupId: gid,
          groupName: group.name,
          groupLeaderId: group.leaderId,
          groupDescription: group.description,
          approvalStatus: 'approval',
          status: 'active'
        }

        await api.put(`/student-group/${gid}`, payload)

        // 乐观更新
        const idx = groups.value.findIndex(g => g.id === gid)
        if (idx !== -1) groups.value[idx].status = 'approval'

        ElMessage.success('分组申请已同意')
        updatePendingCountStorage()
        if (currentGroupDetails.value && currentGroupDetails.value.id === group.id) {
          showDetailsDialog.value = false
        }
        await fetchGroups()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error(error?.response?.data?.message || '操作失败')
        }
      }
    }

    const rejectGroup = async (group) => {
      try {
        const gid = group?.id || group?.groupId || group?.group_id
        if (!gid) { ElMessage.error('缺少分组ID'); return }

        await ElMessageBox.confirm(
            `确定要驳回"${group.name}"的分组申请吗？`, '确认操作',
            { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
        )

        const payload = {
          groupId: gid,
          groupName: group.name,
          groupLeaderId: group.leaderId,
          groupDescription: group.description,
          approvalStatus: 'rejected',
          status: 'active'
        }

        await api.put(`/student-group/${gid}`, payload)

        // 乐观更新
        const idx = groups.value.findIndex(g => g.id === gid)
        if (idx !== -1) groups.value[idx].status = 'rejected'

        ElMessage.success('分组申请已驳回')
        updatePendingCountStorage()
        if (currentGroupDetails.value && currentGroupDetails.value.id === group.id) {
          showDetailsDialog.value = false
        }
        await fetchGroups()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error(error?.response?.data?.message || '操作失败')
        }
      }
    }

    onMounted(() => {
      fetchGroups()
    })

    return {
      loading,
      courseFilter,
      applicationStatusFilter,
      currentPage,
      pageSize,
      showDetailsDialog,
      currentGroupDetails,
      groups,
      courses,
      filteredGroups,
      paginatedGroups,
      pendingGroupsCount,
      approvedCount,
      rejectedCount,
      fetchGroups,
      viewGroupDetails,
      filterGroups,
      approveGroup,
      rejectGroup,
      getStatusTagType,
      getStatusText,
      getAvatarColor,
      Search
    }
  }
}
</script>

<style scoped>
.students-groups-container {
  padding: 24px;
  background-color: #f1f5f9;
  min-height: calc(100vh - 60px);
}

/* 顶部统计卡片 */
.stats-header {
  margin-bottom: 24px;
}
.stat-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  transition: transform 0.2s;
}
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 4px 6px rgba(0,0,0,0.08); }

.stat-icon-bg {
  width: 48px; height: 48px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 24px;
}
.total .stat-icon-bg { background: #eff6ff; color: #3b82f6; }
.pending .stat-icon-bg { background: #fff7ed; color: #f97316; }
.approved .stat-icon-bg { background: #f0fdf4; color: #22c55e; }
.rejected .stat-icon-bg { background: #fef2f2; color: #ef4444; }

.stat-info { display: flex; flex-direction: column; }
.stat-label { font-size: 13px; color: #64748b; margin-bottom: 4px; }
.stat-value { font-size: 24px; font-weight: 700; color: #1e293b; line-height: 1; }

/* 主内容卡片 */
.main-content-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  min-height: 500px;
  display: flex;
  flex-direction: column;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 16px;
}
.left-tools { display: flex; align-items: center; gap: 24px; }
.card-title { font-size: 18px; font-weight: 600; color: #1e293b; margin: 0; }
.right-tools { display: flex; align-items: center; gap: 12px; }

.table-wrapper { flex: 1; margin-bottom: 16px; }
.group-name-text { font-weight: 600; color: #334155; }

.leader-info { display: flex; align-items: center; gap: 8px; }
.leader-avatar { font-size: 12px; color: white; background: #3b82f6; }
.leader-name { font-weight: 500; color: #334155; font-size: 13px; }

.members-preview { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.member-tag-item { border: none; background: #f1f5f9; color: #475569; }
.member-tag-more { background: #f8fafc; color: #94a3b8; }
.no-members { color: #cbd5e1; font-size: 12px; }

.status-tag-rounded { border-radius: 12px; padding: 0 12px; height: 24px; line-height: 24px; border: none; }

.pagination-footer { display: flex; justify-content: flex-end; margin-top: auto; }

/* 详情弹窗样式 */
.detail-container { padding: 0 10px; }
.detail-header {
  display: flex; justify-content: space-between; align-items: center;
  background: #f8fafc; padding: 20px; border-radius: 12px;
}
.dh-left { display: flex; align-items: center; gap: 16px; }
.dh-avatar {
  width: 56px; height: 56px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  color: white; font-size: 24px; font-weight: bold;
}
.dh-info .dh-name { font-size: 20px; font-weight: 700; color: #1e293b; margin-bottom: 4px; }
.dh-info .dh-course { color: #64748b; font-size: 13px; display: flex; align-items: center; gap: 4px; }

.desc-text { color: #475569; line-height: 1.6; white-space: pre-wrap; font-size: 14px; }
.desc-leader { font-weight: 600; color: #3b82f6; }

.section-title {
  font-size: 15px; font-weight: 600; color: #1e293b; margin-bottom: 12px; display: flex; align-items: center; gap: 6px;
}
.title-count { color: #94a3b8; font-weight: normal; }

.members-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 12px;
}
.member-card-mini {
  display: flex; align-items: center; gap: 10px; padding: 10px;
  border: 1px solid #e2e8f0; border-radius: 8px;
  background: white;
}
.member-card-mini.is-leader { border-color: #bfdbfe; background: #eff6ff; }
.member-info .m-name { font-size: 13px; font-weight: 600; color: #334155; }
.member-info .m-role { font-size: 11px; color: #3b82f6; }
.member-info .text-gray { color: #94a3b8; }

.mt-4 { margin-top: 24px; }
.dialog-footer { display: flex; justify-content: flex-end; gap: 12px; }

/* 响应式 */
@media (max-width: 768px) {
  .toolbar { flex-direction: column; align-items: flex-start; }
  .right-tools { width: 100%; justify-content: space-between; }
  .stats-header .el-col { width: 50%; margin-bottom: 12px; }
}
</style>