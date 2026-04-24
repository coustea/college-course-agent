<template>
  <div class="groups-page">
    <!-- 顶部导航 -->
    <div class="page-header-container">
      <el-page-header @back="goBack" title="返回学生管理">
        <template #content>
          <span class="header-title">分组管理</span>
        </template>
        <template #extra>
          <el-button
            type="primary"
            :icon="Refresh"
            circle
            @click="fetchGroups"
            :loading="loading"
            title="刷新数据"
          />
        </template>
      </el-page-header>
    </div>

    <!-- 统计卡片区 -->
    <el-row :gutter="20" class="stats-container">
      <el-col :xs="24" :sm="6">
        <el-card shadow="hover" class="stat-card blue-theme">
          <div class="stat-body">
            <div class="stat-info">
              <div class="stat-value">{{ groups.length }}</div>
              <div class="stat-label">分组总数</div>
            </div>
            <el-icon class="stat-icon"><DataLine /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="6">
        <el-card shadow="hover" class="stat-card orange-theme">
          <div class="stat-body">
            <div class="stat-info">
              <div class="stat-value">{{ pendingGroupsCount }}</div>
              <div class="stat-label">待审批</div>
            </div>
            <el-icon class="stat-icon"><Timer /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="6">
        <el-card shadow="hover" class="stat-card green-theme">
          <div class="stat-body">
            <div class="stat-info">
              <div class="stat-value">{{ approvedCount }}</div>
              <div class="stat-label">已通过</div>
            </div>
            <el-icon class="stat-icon"><Check /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="6">
        <el-card shadow="hover" class="stat-card red-theme">
          <div class="stat-body">
            <div class="stat-info">
              <div class="stat-value">{{ rejectedCount }}</div>
              <div class="stat-label">已驳回</div>
            </div>
            <el-icon class="stat-icon"><Close /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 主内容卡片 -->
    <el-card class="main-card" shadow="never">
      <!-- 筛选工具栏 -->
      <div class="filter-toolbar">
        <div class="left-filters">
          <span class="filter-label">筛选状态：</span>
          <el-radio-group v-model="applicationStatusFilter" @change="filterGroups" size="small">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="pending">待审批</el-radio-button>
            <el-radio-button label="approval">已通过</el-radio-button>
            <el-radio-button label="rejected">已驳回</el-radio-button>
          </el-radio-group>
        </div>
        <div class="right-filters">
           <!-- 如果需要更多操作可以放在这里 -->
        </div>
      </div>

      <!-- 表格内容 -->
      <el-table
        :data="paginatedGroups"
        v-loading="loading"
        style="width: 100%"
        stripe
        highlight-current-row
        header-cell-class-name="table-header-gray"
        row-key="id"
      >
          <el-table-column type="index" label="序号" width="60" align="center"/>

          <el-table-column prop="name" label="队伍名称" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="group-name-text">{{ row.name }}</span>
            </template>
          </el-table-column>

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
      <!-- 分页组件 -->
      <div class="pagination-container">
        <el-pagination
          v-if="filteredGroups.length > 0"
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="filteredGroups.length"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="currentPage = $event"
          @size-change="pageSize = $event; currentPage = 1"
          background
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="showDetailsDialog"
      :title="`${currentGroupDetails?.name || '分组'} 的详情`"
      width="750px"
      destroy-on-close
      class="detail-dialog"
    >
      <div v-if="currentGroupDetails" class="detail-content">
        <!-- 分组基本信息卡片 (采用渐变风格) -->
        <el-card class="info-card" shadow="hover">
          <el-descriptions :column="2" border size="large" class="custom-desc">
            <el-descriptions-item label="分组名称">
              <span class="info-text">{{ currentGroupDetails.name }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
               <el-tag :type="getStatusTagType(currentGroupDetails.status)" size="small" effect="dark">
                {{ getStatusText(currentGroupDetails.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="组长">
              <span class="info-text">{{ currentGroupDetails.leaderName }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="当前人数">
              <span class="info-text">{{ currentGroupDetails.memberCount }} 人</span>
            </el-descriptions-item>
            <el-descriptions-item label="分工描述" :span="2">
              <div class="desc-text">{{ currentGroupDetails.description || '暂无描述' }}</div>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <div class="section-title mt-4">
          <span class="title-text">成员列表 ({{ currentGroupDetails.memberList?.length || 0 }})</span>
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
import { useRouter } from 'vue-router'
import {ElMessage, ElMessageBox} from 'element-plus'
import {Search, DataLine, Timer, Check, Close, Refresh} from '@element-plus/icons-vue'
import axios from 'axios'

const API_BASE = (import.meta?.env?.VITE_API_BASE_URL || '/api')
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
    Search, DataLine, Timer, Check, Close
  },
  setup() {
    const router = useRouter()
    const loading = ref(false)
    const applicationStatusFilter = ref('')
    const currentPage = ref(1)
    const pageSize = ref(10)
    const showDetailsDialog = ref(false)
    const currentGroupDetails = ref(null)

    // 数据
    const groups = ref([])

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
      applicationStatusFilter,
      currentPage,
      pageSize,
      showDetailsDialog,
      currentGroupDetails,
      groups,
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
      goBack: () => router.back(),
      Search,
      Refresh
    }
  }
}
</script>

<style scoped>
.groups-page {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

/* 顶部 Header */
.page-header-container {
  background: #fff;
  padding: 16px 24px;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.header-title {
  font-weight: 600;
  font-size: 18px;
  color: #303133;
}

/* 统计卡片 */
.stats-container {
  margin-bottom: 20px;
}

.stat-card {
  border: none;
  border-radius: 8px;
  transition: transform 0.2s, box-shadow 0.2s;
  height: 100%;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.stat-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 6px;
}

.stat-icon {
  font-size: 40px;
  padding: 10px;
  border-radius: 12px;
  opacity: 0.8;
}

/* 统计卡片主题色 */
.blue-theme .stat-icon {
  background-color: #ecf5ff;
  color: #409eff;
}
.orange-theme .stat-icon {
  background-color: #fdf6ec;
  color: #e6a23c;
}
.green-theme .stat-icon {
  background-color: #f0f9eb;
  color: #67c23a;
}
.red-theme .stat-icon {
  background-color: #fef0f0;
  color: #f56c6c;
}

/* 主内容卡片 */
.main-card {
  border-radius: 8px;
}

/* 筛选工具栏 */
.filter-toolbar {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 16px;
}

.left-filters {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.right-filters {
  display: flex;
  gap: 12px;
}

/* 表格样式 */
.group-name-text {
  font-weight: 600;
  color: #303133;
}

.leader-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.leader-name {
  font-weight: 500;
  color: #303133;
}

.members-preview {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.member-tag-item {
  background-color: #f0f2f5;
  border-color: #dcdfe6;
  color: #606266;
}

.status-tag-rounded {
  border-radius: 12px;
}

:deep(.table-header-gray) {
  background-color: #f5f7fa !important;
  color: #606266;
  font-weight: 600;
}

/* 分页 */
.pagination-container {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

/* 详情弹窗 */
.detail-content {
  padding: 0 10px;
}

.info-card {
  margin-bottom: 20px;
  border-radius: 8px;
  background: linear-gradient(135deg, #fdfbfb 0%, #ebedee 100%);
  border: none;
}

.info-text {
  font-weight: 500;
  color: #303133;
}

.desc-text {
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
  font-size: 14px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.members-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
}

.member-card-mini {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
}

.member-card-mini.is-leader {
  border-color: #409eff;
  background-color: #ecf5ff;
}

.member-info .m-name {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.member-info .m-role {
  font-size: 11px;
  color: #409eff;
}

.member-info .text-gray {
  color: #909399;
}

.mt-4 {
  margin-top: 24px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 响应式 */
@media (max-width: 768px) {
  .filter-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .right-filters {
    flex-direction: column;
  }
}
</style>