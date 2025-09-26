<template>
  <div class="students-groups">
    <div class="page-header">
      <h2>分组管理</h2>
    </div>

    <div class="filter-bar">
      <el-select v-model="courseFilter" placeholder="按课程筛选" clearable
                 style="width: 150px; margin-right: 12px;">
        <el-option
          v-for="course in courses"
          :key="course.id"
          :label="course.name"
          :value="course.id"
        />
      </el-select>
      <el-button type="primary" :icon="Search" @click="fetchGroups">
        搜索
      </el-button>
    </div>

    <!-- 添加申请状态筛选 -->
    <div class="application-status-filter">
      <el-radio-group v-model="applicationStatusFilter" @change="filterGroups">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="pending">未审批</el-radio-button>
        <el-radio-button label="approved">已同意</el-radio-button>
        <el-radio-button label="rejected">已驳回</el-radio-button>
      </el-radio-group>
    </div>

    <div class="table-container">
      <el-table :data="paginatedGroups" style="width: 100%" v-loading="loading" height="100%">
        <el-table-column type="index" label="序号" width="80" align="center"/>
        <el-table-column prop="courseName" label="所属课程" width="200" align="center"/>
        <el-table-column prop="leaderName" label="组长" width="150" align="center"/>
        <el-table-column prop="members" label="组员" width="200" align="center">
          <template #default="scope">
            <span>{{ scope.row.members }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="memberCount" label="人数" width="80" align="center"/>
        <el-table-column prop="status" label="申请状态" width="120" align="center">
          <template #default="scope">
            <el-tag :type="getStatusTagType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="scope">
            <el-button size="small" @click="viewGroupDetails(scope.row)">查看</el-button>
            <el-button
              v-if="scope.row.status === 'pending'"
              size="small"
              type="success"
              @click="approveGroup(scope.row)"
            >
              同意
            </el-button>
            <el-button
              v-if="scope.row.status === 'pending'"
              size="small"
              type="danger"
              @click="rejectGroup(scope.row)"
            >
              驳回
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-pagination
      v-if="filteredGroups.length > 0"
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :page-sizes="[10, 20, 50, 100]"
      :total="filteredGroups.length"
      layout="total, sizes, prev, pager, next, jumper"
      background
      class="pagination"
    />

    <!-- 查看分组详情对话框 -->
    <el-dialog
      title="分组详情"
      v-model="showDetailsDialog"
      width="600px"
    >
      <div class="group-details-content">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="所属课程">{{
              currentGroupDetails?.courseName
            }}
          </el-descriptions-item>
          <el-descriptions-item label="组长">{{
              currentGroupDetails?.leaderName
            }}
          </el-descriptions-item>
          <el-descriptions-item label="组员">
            <div class="member-list">
              <el-tag
                v-for="member in currentGroupDetails?.memberList"
                :key="member.id"
                class="member-tag"
              >
                {{ member.name }}
              </el-tag>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="人数">{{
              currentGroupDetails?.memberCount
            }}
          </el-descriptions-item>
          <el-descriptions-item label="分工描述">
            <div class="description-content">
              {{ currentGroupDetails?.description || '暂无分工描述' }}
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="申请状态">
            <el-tag :type="getStatusTagType(currentGroupDetails?.status)">
              {{ getStatusText(currentGroupDetails?.status) }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="showDetailsDialog = false">关闭</el-button>
        <el-button
          v-if="currentGroupDetails?.status === 'pending'"
          type="success"
          @click="approveGroup(currentGroupDetails)"
        >
          同意
        </el-button>
        <el-button
          v-if="currentGroupDetails?.status === 'pending'"
          type="danger"
          @click="rejectGroup(currentGroupDetails)"
        >
          驳回
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import {ref, computed, onMounted} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {Search} from '@element-plus/icons-vue'
import axios from 'axios'
// 动态后端基址 + Token 拦截
const API_BASE = (import.meta?.env?.VITE_API_BASE_URL || (window?.location?.port === '4173' ? 'http://localhost:9999/api' : '/api'))
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
    Search
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
    const students = ref([])

    // 获取状态标签类型
    const getStatusTagType = (status) => {
      switch (status) {
        case 'pending':
          return 'warning'
        case 'approved':
          return 'success'
        case 'rejected':
          return 'danger'
        default:
          return 'info'
      }
    }

    // 获取状态文本
    const getStatusText = (status) => {
      switch (status) {
        case 'pending':
          return '未审批'
        case 'approved':
          return '已同意'
        case 'rejected':
          return '已驳回'
        default:
          return '未知'
      }
    }

    // 计算未审批分组数量
    const pendingGroupsCount = computed(() => {
      return groups.value.filter(group => group.status === 'pending').length
    })

    // 更新本地存储的未审批数量
    const updatePendingCountStorage = () => {
      localStorage.setItem('pendingGroupsCount', pendingGroupsCount.value.toString())
      // 触发自定义事件，通知首页更新
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

      // 根据申请状态筛选
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

    // 获取分组数据（接后端 /api/student-group/approvalStatus）
    const requestApprovalStatus = async () => {
      const params = {}
      if (courseFilter.value) params.courseId = courseFilter.value
      if (applicationStatusFilter.value) params.approvalStatus = applicationStatusFilter.value
      // 优先尝试 GET ?params
      try {
        return await api.get('/student-group/approvalStatus', { params })
      } catch (e) {
        // 若后端不允许 GET（405），尝试 POST JSON
        if (e?.response?.status === 405) {
          return await api.post('/student-group/approvalStatus', params)
        }
        throw e
      }
    }

    const fetchGroups = async () => {
      loading.value = true
      try {
        const res = await requestApprovalStatus()
        const raw = res?.data
        const list = Array.isArray(raw?.data) ? raw.data : (Array.isArray(raw) ? raw : [])
        const groupsWithDetails = list.map((g) => {
          const id = g.id || g.groupId || g.group_id
          const courseIdVal = g.courseId || g.course_id || g.course?.id
          const courseName = g.courseName || g.course_name || g.course?.courseName || g.course?.name || '未知课程'
          const leader = g.leader || g.leaderInfo || null
          const leaderName = g.leaderName || leader?.name || leader?.username || '未知'
          const memberListRaw = g.members || g.memberList || g.students || []
          const memberList = Array.isArray(memberListRaw)
            ? memberListRaw.map(m => (typeof m === 'string' ? { name: m } : m))
            : String(memberListRaw || '').split(',').filter(Boolean).map(n => ({ name: n.trim() }))
          const members = memberList.map(m => m.name || m.username || '').filter(Boolean).join(', ')
          const memberCount = memberList.length
          const status = g.status || g.approvalStatus || g.state || ''
          return {
            id,
            name: g.name || g.groupName || `分组#${id ?? ''}`,
            courseId: courseIdVal,
            courseName,
            leaderId: leader?.id,
            leaderName,
            description: g.description || g.remark || '',
            members,
            memberList,
            memberCount,
            status,
          }
        })

        groups.value = groupsWithDetails
        // 从分组聚合课程下拉
        const uniqueCourses = new Map()
        for (const g of groupsWithDetails) {
          if (!uniqueCourses.has(g.courseId)) uniqueCourses.set(g.courseId, { id: g.courseId, name: g.courseName })
        }
        courses.value = Array.from(uniqueCourses.values()).filter(c => c.id != null)

        updatePendingCountStorage()
      } catch (error) {
        console.error('获取数据失败:', error)
        const msg = error?.response?.data?.message || error?.message || '获取数据失败'
        ElMessage.error(msg)
      } finally {
        loading.value = false
      }
    }

    // 查看分组详情
    const viewGroupDetails = (group) => {
      currentGroupDetails.value = group
      showDetailsDialog.value = true
    }

    // 筛选分组
    const filterGroups = () => {
      currentPage.value = 1 // 重置到第一页
    }

    // 同意分组申请
    const approveGroup = async (group) => {
      try {
        await ElMessageBox.confirm(
          `确定要同意"${group.name}"的分组申请吗？`,
          '确认操作',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
          }
        )

        // 模拟API调用
        // await api.put(`/groups/${group.id}/approve`)

        // 更新本地状态
        group.status = 'approved'
        ElMessage.success('分组申请已同意')

        // 更新未审批数量
        updatePendingCountStorage()

        // 如果当前在查看详情，关闭对话框
        if (currentGroupDetails.value && currentGroupDetails.value.id === group.id) {
          showDetailsDialog.value = false
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('操作失败:', error)
          ElMessage.error('操作失败')
        }
      }
    }

    // 驳回分组申请
    const rejectGroup = async (group) => {
      try {
        await ElMessageBox.confirm(
          `确定要驳回"${group.name}"的分组申请吗？`,
          '确认操作',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
          }
        )

        // 模拟API调用
        // await api.put(`/groups/${group.id}/reject`)

        // 更新本地状态
        group.status = 'rejected'
        ElMessage.success('分组申请已驳回')

        // 更新未审批数量
        updatePendingCountStorage()

        // 如果当前在查看详情，关闭对话框
        if (currentGroupDetails.value && currentGroupDetails.value.id === group.id) {
          showDetailsDialog.value = false
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('操作失败:', error)
          ElMessage.error('操作失败')
        }
      }
    }

    // 初始化数据
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
      students,
      filteredGroups,
      paginatedGroups,
      fetchGroups,
      viewGroupDetails,
      filterGroups,
      approveGroup,
      rejectGroup,
      getStatusTagType,
      getStatusText
    }
  }
}
</script>

<style scoped>
.students-groups {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 40px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 16px 24px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.page-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0;
}

.filter-bar {
  display: flex;
  align-items: center;
  margin-bottom: 0;
  padding: 16px 24px;
  background: white;
  border-radius: 8px 8px 0 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.application-status-filter {
  padding: 16px 24px;
  background: white;
  border-top: 1px solid #eee;
}

.table-container {
  background: white;
  padding: 0 24px;
  height: calc(100vh - 380px);
  overflow: auto;
}

.pagination {
  margin-top: 0;
  padding: 16px;
  background: white;
  border-radius: 0 0 8px 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.group-details-content {
  padding: 20px 0;
}

.member-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.member-tag {
  margin-right: 0;
}

.description-content {
  white-space: pre-wrap;
  min-height: 60px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .filter-bar {
    flex-direction: column;
    gap: 12px;
  }

  .filter-bar .el-input {
    width: 100% !important;
    margin-right: 0 !important;
  }

  .filter-bar .el-select {
    width: 100% !important;
    margin-right: 0 !important;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .table-container {
    padding: 0 16px;
    height: auto;
  }
}
</style>
