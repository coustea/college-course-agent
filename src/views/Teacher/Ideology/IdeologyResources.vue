<template>
  <div class="ideology-resources">
    <el-card class="header-card">
      <div class="header">
        <h2>思政资源管理</h2>
        <el-button type="primary" @click="showCreateDialog">
          <el-icon><Plus /></el-icon>
          添加资源
        </el-button>
      </div>
    </el-card>

    <el-row :gutter="16" class="stats-row">
      <el-col :xs="12" :sm="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value">{{ resourceStats.totalResources }}</div>
          <div class="stat-label">资源总数</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value">{{ resourceStats.publishedResources }}</div>
          <div class="stat-label">已发布</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value">{{ resourceStats.videoResources }}</div>
          <div class="stat-label">视频资源</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value">{{ resourceStats.pendingSourceResources }}</div>
          <div class="stat-label">待补链接</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="goal-card">
      <div class="goal-content">
        <div>
          <div class="goal-title">项目资源库目标进度</div>
          <div class="goal-subtitle">
            已上线 {{ resourceStats.totalResources }} / {{ resourceStats.targetResources }} 条，已发布率 {{ resourceStats.publishedRate }}%
          </div>
        </div>
        <el-progress class="goal-progress" :percentage="resourceStats.completionRate" :stroke-width="10" />
      </div>
    </el-card>

    <!-- 搜索筛选 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="搜索标题/关键词" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="课程">
          <el-select v-model="searchForm.courseId" placeholder="选择课程" clearable>
            <el-option v-for="course in courses" :key="course.courseId" :label="course.courseName" :value="course.courseId" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="选择状态" clearable>
            <el-option label="草稿" value="draft" />
            <el-option label="已发布" value="published" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 资源列表 -->
    <el-card class="table-card">
      <el-table :data="resources" v-loading="loading" stripe>
        <el-table-column prop="title" label="资源标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="resourceType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.resourceType)">{{ getTypeName(row.resourceType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="valueTheme" label="价值主题" width="120" show-overflow-tooltip />
        <el-table-column prop="keywords" label="关键词" min-width="150" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'published' ? 'success' : 'info'">
              {{ row.status === 'published' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showEditDialog(row)">编辑</el-button>
            <el-button link type="primary" @click="viewDetail(row)">查看</el-button>
            <el-popconfirm title="确定删除该资源吗？" @confirm="handleDelete(row.resourceId)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadResources"
          @current-change="loadResources"
        />
      </div>
    </el-card>

    <!-- 创建/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑资源' : '添加资源'" width="700px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="资源标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入资源标题" maxlength="100" />
        </el-form-item>
        <el-form-item label="资源类型" prop="resourceType">
          <el-select v-model="form.resourceType" placeholder="选择资源类型">
            <el-option label="案例" value="case" />
            <el-option label="政策文件" value="policy" />
            <el-option label="视频" value="video" />
            <el-option label="文档" value="document" />
            <el-option label="活动" value="activity" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容摘要" prop="contentSummary">
          <el-input v-model="form.contentSummary" type="textarea" :rows="3" placeholder="请输入内容摘要" maxlength="500" />
        </el-form-item>
        <el-form-item label="关联课程">
          <el-select v-model="form.courseId" placeholder="选择关联课程" clearable>
            <el-option v-for="course in courses" :key="course.courseId" :label="course.courseName" :value="course.courseId" />
          </el-select>
        </el-form-item>
        <el-form-item label="价值主题">
          <el-input v-model="form.valueTheme" placeholder="如：家国情怀、工程伦理、创新精神" />
        </el-form-item>
        <el-form-item label="适用场景">
          <el-input v-model="form.applicableScene" placeholder="如：课程导入、课后拓展" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="form.keywords" placeholder="多个关键词用逗号分隔" />
        </el-form-item>
        <el-form-item label="资源链接">
          <el-input v-model="form.sourceUrl" placeholder="资源URL地址" />
        </el-form-item>
        <el-form-item label="难度">
          <el-radio-group v-model="form.difficulty">
            <el-radio value="easy">简单</el-radio>
            <el-radio value="medium">中等</el-radio>
            <el-radio value="hard">困难</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="draft">草稿</el-radio>
            <el-radio value="published">发布</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="form.autoAnalyze">AI辅助分析（自动提取标签和主题）</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="资源详情" width="600px">
      <el-descriptions :column="2" border v-if="currentResource">
        <el-descriptions-item label="资源标题" :span="2">{{ currentResource.title }}</el-descriptions-item>
        <el-descriptions-item label="资源类型">{{ getTypeName(currentResource.resourceType) }}</el-descriptions-item>
        <el-descriptions-item label="价值主题">{{ currentResource.valueTheme || '-' }}</el-descriptions-item>
        <el-descriptions-item label="内容摘要" :span="2">{{ currentResource.contentSummary || '-' }}</el-descriptions-item>
        <el-descriptions-item label="适用场景">{{ currentResource.applicableScene || '-' }}</el-descriptions-item>
        <el-descriptions-item label="关键词">{{ currentResource.keywords || '-' }}</el-descriptions-item>
        <el-descriptions-item label="难度">{{ getDifficultyName(currentResource.difficulty) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentResource.status === 'published' ? 'success' : 'info'">
            {{ currentResource.status === 'published' ? '已发布' : '草稿' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getIdeologyResources,
  getIdeologyResourceStats,
  createIdeologyResource,
  updateIdeologyResource,
  deleteIdeologyResource
} from '@/services/ideology'
import { getCourses } from '@/services/coursesApi'

const loading = ref(false)
const resourceStatsLoading = ref(false)
const submitting = ref(false)
const resources = ref([])
const courses = ref([])
const resourceStats = reactive({
  targetResources: 100,
  totalResources: 0,
  publishedResources: 0,
  videoResources: 0,
  pendingSourceResources: 0,
  courseCoverageCount: 0,
  completionRate: 0,
  publishedRate: 0
})
const dialogVisible = ref(false)
const detailVisible = ref(false)
const isEdit = ref(false)
const currentResource = ref(null)
const formRef = ref(null)

const searchForm = reactive({
  keyword: '',
  courseId: null,
  status: null
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const form = reactive({
  resourceId: null,
  title: '',
  resourceType: 'document',
  contentSummary: '',
  courseId: null,
  valueTheme: '',
  applicableScene: '',
  keywords: '',
  sourceUrl: '',
  difficulty: 'medium',
  status: 'draft',
  autoAnalyze: true
})

const rules = {
  title: [{ required: true, message: '请输入资源标题', trigger: 'blur' }],
  resourceType: [{ required: true, message: '请选择资源类型', trigger: 'change' }]
}

const normalizeResponse = (res) => {
  if (res && typeof res === 'object' && 'status' in res && res.data) {
    return res.data
  }
  return res
}

const buildResourceStats = (list = []) => {
  const items = Array.isArray(list) ? list : []
  const publishedCount = items.filter(item => item.status === 'published').length
  return {
    targetResources: 100,
    totalResources: items.length,
    publishedResources: publishedCount,
    videoResources: items.filter(item => item.resourceType === 'video').length,
    pendingSourceResources: items.filter(item => item.resourceType === 'video' && !item.sourceUrl).length,
    courseCoverageCount: new Set(items.map(item => item.courseId).filter(Boolean)).size,
    completionRate: clampPercentage(items.length),
    publishedRate: items.length > 0 ? clampPercentage((publishedCount * 100) / items.length) : 0
  }
}

const applyResourceStats = (payload) => {
  if (Array.isArray(payload)) {
    Object.assign(resourceStats, buildResourceStats(payload))
    return
  }
  if (payload && typeof payload === 'object') {
    const resourceTypeCounts = payload.resourceTypeCounts || {}
    const totalResources = Number(payload.totalResources ?? payload.totalCount ?? payload.total ?? 0)
    const publishedResources = Number(payload.publishedResources ?? payload.publishedCount ?? 0)
    Object.assign(resourceStats, {
      targetResources: Number(payload.targetResources ?? payload.targetCount ?? 100),
      totalResources,
      publishedResources,
      videoResources: Number(payload.videoResources ?? payload.videoCount ?? resourceTypeCounts.video ?? 0),
      pendingSourceResources: Number(payload.pendingSourceResources ?? payload.pendingSourceCount ?? payload.pendingCount ?? 0),
      courseCoverageCount: Number(payload.courseCoverageCount ?? 0),
      completionRate: clampPercentage(payload.completionRate ?? ((totalResources * 100) / Number(payload.targetResources ?? payload.targetCount ?? 100))),
      publishedRate: clampPercentage(payload.publishedRate ?? (totalResources > 0 ? (publishedResources * 100) / totalResources : 0))
    })
  }
}

const clampPercentage = (value) => {
  const numberValue = Number(value ?? 0)
  if (Number.isNaN(numberValue)) {
    return 0
  }
  return Math.max(0, Math.min(100, Math.round(numberValue)))
}

const refreshResources = async () => {
  await loadResources()
  await loadResourceStats()
}

onMounted(async () => {
  await loadCourses()
  await refreshResources()
})

const loadCourses = async () => {
  try {
    const res = await getCourses()
    const body = normalizeResponse(res)
    if (body?.code === 200) {
      courses.value = body.data || []
    }
  } catch (e) {
    console.error('加载课程失败', e)
  }
}

const loadResources = async () => {
  loading.value = true
  try {
    const res = await getIdeologyResources({
      keyword: searchForm.keyword || undefined,
      courseId: searchForm.courseId || undefined,
      status: searchForm.status || undefined,
      limit: pagination.pageSize
    })
    const body = normalizeResponse(res)
    if (body?.code === 200) {
      resources.value = body.data || []
      pagination.total = resources.value.length
      if (!resourceStatsLoading.value) {
        Object.assign(resourceStats, buildResourceStats(resources.value))
      }
    }
  } catch (e) {
    console.error('加载资源失败', e)
  } finally {
    loading.value = false
  }
}

const loadResourceStats = async () => {
  resourceStatsLoading.value = true
  try {
    const res = await getIdeologyResourceStats({
      courseId: searchForm.courseId || undefined
    })
    const body = normalizeResponse(res)
    if (body?.code === 200) {
      applyResourceStats(body.data)
      return
    }
  } catch (e) {
    console.warn('加载资源统计失败，使用列表数据回退', e)
  } finally {
    resourceStatsLoading.value = false
  }

  applyResourceStats(resources.value)
}

const handleSearch = () => {
  pagination.page = 1
  refreshResources()
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.courseId = null
  searchForm.status = null
  handleSearch()
}

const showCreateDialog = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const showEditDialog = (row) => {
  isEdit.value = true
  Object.assign(form, {
    resourceId: row.resourceId,
    title: row.title,
    resourceType: row.resourceType,
    contentSummary: row.contentSummary,
    courseId: row.courseId,
    valueTheme: row.valueTheme,
    applicableScene: row.applicableScene,
    keywords: row.keywords,
    sourceUrl: row.sourceUrl,
    difficulty: row.difficulty,
    status: row.status,
    autoAnalyze: false
  })
  dialogVisible.value = true
}

const resetForm = () => {
  Object.assign(form, {
    resourceId: null,
    title: '',
    resourceType: 'document',
    contentSummary: '',
    courseId: null,
    valueTheme: '',
    applicableScene: '',
    keywords: '',
    sourceUrl: '',
    difficulty: 'medium',
    status: 'draft',
    autoAnalyze: true
  })
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    let res
    if (isEdit.value) {
      res = await updateIdeologyResource(form.resourceId, form)
    } else {
      res = await createIdeologyResource(form)
    }

    const body = normalizeResponse(res)
    if (body?.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      refreshResources()
    } else {
      ElMessage.error(body?.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (resourceId) => {
  try {
    const res = await deleteIdeologyResource(resourceId)
    const body = normalizeResponse(res)
    if (body?.code === 200) {
      ElMessage.success('删除成功')
      refreshResources()
    } else {
      ElMessage.error(body?.message || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const viewDetail = (row) => {
  currentResource.value = row
  detailVisible.value = true
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const getTypeName = (type) => {
  const map = { case: '案例', policy: '政策', video: '视频', document: '文档', activity: '活动' }
  return map[type] || type
}

const getTypeTag = (type) => {
  const map = { case: 'success', policy: 'warning', video: 'danger', document: '', activity: 'info' }
  return map[type] || ''
}

const getDifficultyName = (difficulty) => {
  const map = { easy: '简单', medium: '中等', hard: '困难' }
  return map[difficulty] || difficulty
}
</script>

<style scoped>
.ideology-resources {
  padding: 20px;
}

.header-card {
  margin-bottom: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.goal-card {
  margin-bottom: 20px;
}

.goal-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.goal-title {
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.goal-subtitle {
  margin-top: 6px;
  color: #606266;
  font-size: 13px;
}

.goal-progress {
  width: min(420px, 45%);
}

.stat-card {
  border-radius: 8px;
}

.stat-value {
  color: #303133;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
}

.stat-label {
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header h2 {
  margin: 0;
  font-size: 20px;
}

.filter-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .goal-content {
    align-items: stretch;
    flex-direction: column;
  }

  .goal-progress {
    width: 100%;
  }
}
</style>
