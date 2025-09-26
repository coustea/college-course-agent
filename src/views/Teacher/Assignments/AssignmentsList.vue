<template>
  <div class="assignments-list">
    <!-- 统一页面头部样式 -->
    <div class="page-header">
      <h2>作品定期检查列表</h2>
      <el-button type="primary" @click="$router.push('/teacher/assignments/create')">
        <el-icon>
          <Plus />
        </el-icon>
        下发新检查
      </el-button>
    </div>

    <!-- 搜索和筛选区域 -->
    <div class="filter-bar">
      <el-input
        v-model="searchQuery"
        placeholder="搜索检查标题或班级..."
        :prefix-icon="Search"
        style="width: 400px; margin-right: 16px;"
        clearable
        @input="filterAssignments"
      />
      <el-select
        v-model="statusFilter"
        placeholder="按状态筛选"
        clearable
        style="width: 150px; margin-right: 12px;"
        @change="filterAssignments"
      >
        <el-option label="进行中" value="进行中"/>
        <el-option label="已截止" value="已截止"/>
        <el-option label="已完成" value="已完成"/>
      </el-select>
      <el-button type="primary" :icon="Search" @click="fetchAssignments">
        搜索
      </el-button>
    </div>

    <!-- 检查项目表格 -->
    <div class="table-container">
      <el-table
        :data="paginatedAssignments"
        style="width: 100%"
        v-loading="loading"
        height="100%"
        stripe
      >
        <el-table-column prop="title" label="检查标题" min-width="200" align="center">
        </el-table-column>
        <el-table-column prop="courseName" label="课程" min-width="150" align="center">
        </el-table-column>
        <el-table-column prop="deadline" label="截止日期" width="120" align="center">
          <template #default="scope">
            {{ formatDate(scope.row.deadline) }}
          </template>
        </el-table-column>
        <el-table-column prop="submittedCount" label="完成情况" width="140" align="center">
          <template #default="scope">
            <span>{{ Number(scope.row.submittedCount) || 0 }}/{{ Number(scope.row.totalGroups) || 0 }}</span>
            <el-progress
              :percentage="safePct(scope.row.submittedCount, scope.row.totalGroups)"
              :show-text="false"
              style="margin-top: 5px;"
            />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="下发时间" width="120" align="center">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="scope">
            <el-button
              size="small"
              type="primary"
              @click="checkAssignment(scope.row)"
              :disabled="scope.row.submittedCount === 0"
            >
              检查情况
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页组件 -->
    <el-pagination
      v-if="filteredAssignments.length > 0"
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :page-sizes="[10, 20, 50, 100]"
      :total="filteredAssignments.length"
      layout="total, sizes, prev, pager, next, jumper"
      background
      class="pagination"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import axios from 'axios'

// 动态后端基址 + token 拦截
const API_BASE = (import.meta?.env?.VITE_API_BASE_URL || (window?.location?.port === '4173' ? 'http://localhost:9999/api' : '/api'))
const api = axios.create({ baseURL: API_BASE, timeout: 20000 })
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token') || localStorage.getItem('userToken')
  if (token) config.headers = { ...(config.headers || {}), Authorization: `Bearer ${token}` }
  return config
})

export default {
  name: 'AssignmentsList',
  components: {
    Search,
    Plus
  },
  setup() {
    const router = useRouter()

    // 响应式数据
    const loading = ref(false)
    const searchQuery = ref('')
    const statusFilter = ref('')
    const currentPage = ref(1)
    const pageSize = ref(10)

    // 检查项目数据
    const assignments = ref([])
    const safePct = (done, total) => {
      const d = Number(done) || 0
      const t = Number(total) || 0
      if (t <= 0) return 0
      const p = Math.round((d / t) * 100)
      return Math.max(0, Math.min(100, p))
    }

    // 模拟数据 - 用于调试
    const mockAssignments = [
      {
        id: 1,
        title: '期中作品进度检查',
        courseId: 1,
        courseName: 'Web前端开发班',
        deadline: '2023-12-15',
        submittedCount: 8,
        totalGroups: 10,
        status: '进行中',
        createdAt: '2023-11-20',
        description: '检查各小组期中作品完成进度'
      },
      {
        id: 2,
        title: '期末项目代码规范检查',
        courseId: 1,
        courseName: 'Web前端开发班',
        deadline: '2023-11-30',
        submittedCount: 10,
        totalGroups: 10,
        status: '已完成',
        createdAt: '2023-11-10',
        description: '检查期末项目的代码规范和质量'
      },
      {
        id: 3,
        title: '数据库设计文档检查',
        courseId: 2,
        courseName: '数据库原理班',
        deadline: '2023-12-20',
        submittedCount: 5,
        totalGroups: 8,
        status: '进行中',
        createdAt: '2023-11-25',
        description: '检查数据库设计文档的完整性和规范性'
      },
      {
        id: 4,
        title: '操作系统实验报告检查',
        courseId: 3,
        courseName: '操作系统班',
        deadline: '2023-11-28',
        submittedCount: 12,
        totalGroups: 12,
        status: '已截止',
        createdAt: '2023-11-05',
        description: '检查操作系统实验报告的完成情况'
      },
      {
        id: 5,
        title: '项目演示准备检查',
        courseId: 1,
        courseName: 'Web前端开发班',
        deadline: '2023-12-10',
        submittedCount: 3,
        totalGroups: 10,
        status: '进行中',
        createdAt: '2023-11-28',
        description: '检查项目演示的准备工作'
      }
    ]

    // 计算属性
    const filteredAssignments = computed(() => {
      let result = assignments.value

      // 按搜索词过滤
      if (searchQuery.value) {
        const query = searchQuery.value.toLowerCase()
        result = result.filter(a =>
          String(a.title || '').toLowerCase().includes(query) ||
          String(a.courseName || '').toLowerCase().includes(query)
        )
      }

      // 按状态过滤
      if (statusFilter.value) {
        result = result.filter(a => a.status === statusFilter.value)
      }

      return result
    })

    // 分页后的检查项目
    const paginatedAssignments = computed(() => {
      const start = (currentPage.value - 1) * pageSize.value
      const end = start + pageSize.value
      return filteredAssignments.value.slice(start, end)
    })

    // 方法
    const filterAssignments = () => {
      currentPage.value = 1
    }

    const formatDate = (dateString) => {
      if (!dateString) return '-'
      return new Date(dateString).toLocaleDateString('zh-CN')
    }

    const getStatusType = (status) => {
      const statusMap = {
        '进行中': 'primary',
        '已截止': 'warning',
        '已完成': 'success'
      }
      return statusMap[status] || 'info'
    }

    const checkAssignment = (assignment) => {
      router.push(`/teacher/assignments/check/${assignment.id}`)
    }

    const viewDetails = (assignment) => {
      router.push(`/teacher/assignments/detail/${assignment.id}`)
    }

    const deleteAssignment = async (id) => {
      try {
        await ElMessageBox.confirm(
          '确定要删除这个检查项目吗？',
          '删除确认',
          {
            confirmButtonText: '删除',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )

        // 实际API调用
        // await api.delete(`/assignments/${id}`)

        // 模拟删除
        const index = assignments.value.findIndex(a => a.id === id)
        if (index !== -1) {
          assignments.value.splice(index, 1)
          ElMessage.success('检查项目删除成功')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除检查项目失败:', error)
          ElMessage.error('删除检查项目失败')
        }
      }
    }

    const handleSizeChange = (val) => {
      pageSize.value = val
    }

    const handleCurrentChange = (val) => {
      currentPage.value = val
    }

    // 解析截止时间为时间戳（兼容 yyyy-MM-dd HH:mm）
    const parseDeadlineTs = (deadline) => {
      if (!deadline) return null
      const s = String(deadline).trim()
      // 提取本地年月日时分秒，忽略 T/Z 等，以本地时间构造
      const re = /(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2})(?::(\d{2}))?/
      const m = s.match(re)
      if (m) {
        const y = Number(m[1]); const mo = Number(m[2]); const d = Number(m[3])
        const hh = Number(m[4]); const mm = Number(m[5]); const ss = Number(m[6] || 0)
        // 约定：若后端时间为当天 00:00:00，视为当日 23:59:59 截止
        const hh2 = (hh === 0 && mm === 0 && ss === 0) ? 23 : hh
        const mm2 = (hh === 0 && mm === 0 && ss === 0) ? 59 : mm
        const ss2 = (hh === 0 && mm === 0 && ss === 0) ? 59 : ss
        const ts = new Date(y, mo - 1, d, hh2, mm2, ss2).getTime()
        return Number.isFinite(ts) ? ts : null
      }
      // 仅有日期（到日） -> 视为该天 23:59:59 截止
      const m2 = s.match(/^(\d{4})-(\d{2})-(\d{2})$/)
      if (m2) {
        const y = Number(m2[1]); const mo = Number(m2[2]); const d = Number(m2[3])
        const ts = new Date(y, mo - 1, d, 23, 59, 59).getTime()
        return Number.isFinite(ts) ? ts : null
      }
      const ts = new Date(s).getTime()
      return Number.isFinite(ts) ? ts : null
    }

    let statusTimer = null
    const updateStatuses = () => {
      const now = Date.now()
      assignments.value = (assignments.value || []).map(a => {
        const ts = parseDeadlineTs(a.deadline)
        const status = ts != null && ts < now ? '已截止' : '进行中'
        return { ...a, status }
      })
    }

    // 获取课程名映射
    const buildCourseNameMap = async () => {
      try {
        const res = await api.get('/course/list')
        const body = res?.data
        const arr = (body && Number(body.code) === 200 && Array.isArray(body.data)) ? body.data : []
        const m = new Map()
        for (const c of arr) {
          const id = Number(c.courseId || c.id)
          if (Number.isFinite(id)) m.set(id, c.courseName || c.title || '')
        }
        return m
      } catch {
        return new Map()
      }
    }

    // 获取检测项目列表：GET /api/teacherAssignments/{teacherId}
    const fetchAssignments = async () => {
      loading.value = true
      try {
        // 获取教师ID（userInfo.id -> teacherId 本地 -> 后端列表首个）
        let teacherId = null
        try { const u = JSON.parse(localStorage.getItem('userInfo') || 'null'); if (u?.id) teacherId = Number(u.id) } catch {}
        if (!teacherId) { const tid = localStorage.getItem('teacherId'); if (tid) teacherId = Number(tid) }
        if (!teacherId) {
          try {
            const tRes = await api.get('/teacher/list/teachers')
            const tRaw = tRes?.data
            const tList = (tRaw && Number(tRaw.code) === 200 && Array.isArray(tRaw.data)) ? tRaw.data : []
            if (tList.length > 0) teacherId = Number(tList[0].id)
          } catch {}
        }
        if (!teacherId) { ElMessage.error('未获取到教师ID'); assignments.value = []; return }

        const res = await api.get(`/teacherAssignments/${teacherId}`)
        const raw = res?.data
        const list = (raw && Number(raw.code) === 200 && Array.isArray(raw.data)) ? raw.data : []
        // 映射为表格需要的字段
        assignments.value = list.map((it) => {
          const deadline = it.dueDate || ''
          return {
            id: it.assignmentId,
            title: it.assignmentName || '未命名检测',
            courseId: it.courseId || it.course_id,
            courseName: it.courseName || it.course_name || (it.courseId ? `课程#${it.courseId}` : '—'),
            deadline: deadline,
            submittedCount: 0,
            totalGroups: 0,
            status: '进行中',
            createdAt: ''
          }
        })
        // 课程名增强：从课程列表补全
        try {
          const courseMap = await buildCourseNameMap()
          assignments.value = assignments.value.map(a => {
            const name = courseMap.get(Number(a.courseId))
            return { ...a, courseName: name || a.courseName || (a.courseId ? `课程#${a.courseId}` : '—') }
          })
        } catch {}
        // 如果后端给的是 LocalDateTime（不带秒），补零再解析
        assignments.value = assignments.value.map(a => {
          let s = String(a.deadline || '').trim()
          if (s && /^\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}$/.test(s)) s = s + ':00'
          return { ...a, deadline: s }
        })
        updateStatuses()
        if (!statusTimer) statusTimer = setInterval(updateStatuses, 60000)
        if (!assignments.value.length) {
          ElMessage.info('当前教师暂无检测记录')
        }
      } catch (error) {
        console.error('获取检测列表失败:', error)
        ElMessage.error('获取检测列表失败')
        assignments.value = []
      } finally {
        loading.value = false
      }
    }

    onUnmounted(() => { if (statusTimer) { clearInterval(statusTimer); statusTimer = null } })

    // 初始化数据
    onMounted(() => {
      fetchAssignments()
    })

    return {
      loading,
      searchQuery,
      statusFilter,
      currentPage,
      pageSize,
      assignments,
      filteredAssignments,
      paginatedAssignments,
      filterAssignments,
      formatDate,
      getStatusType,
      checkAssignment,
      viewDetails,
      deleteAssignment,
      handleSizeChange,
      handleCurrentChange,
      fetchAssignments,
      safePct
    }
  }
}
</script>

<style scoped>
.assignments-list {
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

.table-container {
  background: white;
  padding: 0 24px;
  height: calc(100vh - 320px);
  overflow: auto;
}

.pagination {
  margin-top: 0;
  padding: 16px;
  background: white;
  border-radius: 0 0 8px 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
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
