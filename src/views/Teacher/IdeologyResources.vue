<template>
  <div class="ideology-page">
    <section class="hero-panel">
      <div>
        <p class="eyebrow">Course Ideology Library</p>
        <h1>思政资源管理</h1>
        <p class="hero-desc">沉淀案例、政策、视频与互动材料，让每门课程都能找到合适的价值引导切入点。</p>
      </div>
      <div class="hero-actions">
        <el-button class="ghost-btn" @click="loadResources">
          <i class="fas fa-rotate-right"></i>
          刷新
        </el-button>
        <el-button type="primary" class="primary-btn" @click="openCreateDialog">
          <i class="fas fa-plus"></i>
          新建资源
        </el-button>
      </div>
    </section>

    <section class="metric-grid">
      <div class="metric-card dark">
        <span>资源总量</span>
        <strong>{{ resources.length }}</strong>
        <small>当前筛选结果</small>
      </div>
      <div class="metric-card">
        <span>已发布</span>
        <strong>{{ publishedCount }}</strong>
        <small>可进入学生推荐池</small>
      </div>
      <div class="metric-card">
        <span>价值主题</span>
        <strong>{{ themeCount }}</strong>
        <small>覆盖不同育人方向</small>
      </div>
      <div class="metric-card warm">
        <span>AI 分析</span>
        <strong>可用</strong>
        <small>摘要、标签、场景自动生成</small>
      </div>
    </section>

    <section class="toolbar-card">
      <div class="search-control">
        <i class="fas fa-search"></i>
        <input v-model="filters.keyword" placeholder="搜索标题、摘要、关键词、价值主题" @keyup.enter="loadResources" />
      </div>
      <el-select v-model="filters.courseId" placeholder="按课程筛选" clearable filterable class="filter-select">
        <el-option v-for="course in courseOptions" :key="course.value" :label="course.label" :value="course.value" />
      </el-select>
      <el-select v-model="filters.status" placeholder="状态" clearable class="status-select">
        <el-option label="草稿" value="draft" />
        <el-option label="已发布" value="published" />
      </el-select>
      <el-button type="primary" @click="loadResources">筛选</el-button>
    </section>

    <section v-loading="loading" class="resource-board">
      <el-empty v-if="!loading && resources.length === 0" description="暂无思政资源，先创建一条资源吧" />
      <article v-for="item in resources" v-else :key="item.resourceId" class="resource-card">
        <div class="card-topline">
          <span class="resource-type">{{ typeText(item.resourceType) }}</span>
          <el-tag :type="item.status === 'published' ? 'success' : 'info'" effect="light">
            {{ item.status === 'published' ? '已发布' : '草稿' }}
          </el-tag>
        </div>
        <h3>{{ item.title }}</h3>
        <p class="summary">{{ item.contentSummary || '暂无摘要，可点击编辑并启用 AI 分析生成结构化信息。' }}</p>
        <div class="tag-row">
          <el-tag v-if="item.valueTheme" type="warning" effect="plain">{{ item.valueTheme }}</el-tag>
          <el-tag v-if="item.applicableScene" type="primary" effect="plain">{{ item.applicableScene }}</el-tag>
          <el-tag v-if="item.difficulty" effect="plain">{{ difficultyText(item.difficulty) }}</el-tag>
        </div>
        <div class="keywords">
          <span v-for="keyword in splitKeywords(item.keywords)" :key="keyword"># {{ keyword }}</span>
        </div>
        <div class="card-footer">
          <span>课程：{{ courseName(item.courseId) }}</span>
          <div class="actions">
            <button @click="openEditDialog(item)">编辑</button>
            <button class="danger" @click="confirmDelete(item)">删除</button>
          </div>
        </div>
      </article>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="editingResource ? '编辑思政资源' : '新建思政资源'"
      width="760px"
      class="ideology-dialog"
      destroy-on-close
    >
      <el-form :model="form" label-width="96px" class="resource-form">
        <el-form-item label="资源标题" required>
          <el-input v-model="form.title" placeholder="例如：人工智能伦理中的责任意识案例" />
        </el-form-item>
        <el-form-item label="关联课程">
          <el-select v-model="form.courseId" placeholder="选择课程" clearable filterable style="width: 100%;">
            <el-option v-for="course in courseOptions" :key="course.value" :label="course.label" :value="course.value" />
          </el-select>
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="资源类型">
            <el-select v-model="form.resourceType" style="width: 100%;">
              <el-option label="教学案例" value="case" />
              <el-option label="政策文件" value="policy" />
              <el-option label="视频资源" value="video" />
              <el-option label="文档材料" value="document" />
              <el-option label="互动活动" value="activity" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status" style="width: 100%;">
              <el-option label="草稿" value="draft" />
              <el-option label="发布" value="published" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="内容摘要">
          <el-input v-model="form.contentSummary" type="textarea" :rows="4" placeholder="输入资源内容、教学用途或素材摘要，可用于 AI 自动分析" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="价值主题">
            <el-input v-model="form.valueTheme" placeholder="家国情怀 / 工程伦理 / 创新精神" />
          </el-form-item>
          <el-form-item label="适用场景">
            <el-input v-model="form.applicableScene" placeholder="课程导入 / 课堂讨论 / 课后拓展" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="关键词">
            <el-input v-model="form.keywords" placeholder="多个关键词用逗号分隔" />
          </el-form-item>
          <el-form-item label="难度">
            <el-select v-model="form.difficulty" style="width: 100%;">
              <el-option label="入门" value="easy" />
              <el-option label="进阶" value="medium" />
              <el-option label="挑战" value="hard" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="资源链接">
          <el-input v-model="form.sourceUrl" placeholder="可填写外部链接或 /uploads/... 文件地址" />
        </el-form-item>
        <el-form-item label="AI 分析">
          <div class="ai-row">
            <el-switch v-model="form.autoAnalyze" active-text="保存时自动分析" />
            <el-button :loading="analyzing" @click="previewAnalysis">立即预览分析</el-button>
          </div>
        </el-form-item>
      </el-form>

      <div v-if="analysisPreview" class="analysis-preview">
        <div>
          <span>价值主题</span>
          <strong>{{ analysisPreview.valueTheme || '-' }}</strong>
        </div>
        <div>
          <span>适用场景</span>
          <strong>{{ analysisPreview.applicableScene || '-' }}</strong>
        </div>
        <div class="wide">
          <span>摘要</span>
          <p>{{ analysisPreview.summary || '-' }}</p>
        </div>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveResource">保存资源</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAllCourses } from '@/services/coursesApi'
import {
  analyzeIdeologyResource,
  createIdeologyResource,
  deleteIdeologyResource,
  searchIdeologyResources,
  updateIdeologyResource
} from '@/services/ideologyApi'

const loading = ref(false)
const saving = ref(false)
const analyzing = ref(false)
const dialogVisible = ref(false)
const editingResource = ref(null)
const analysisPreview = ref(null)
const resources = ref([])
const courses = ref([])

const filters = reactive({
  keyword: '',
  courseId: null,
  status: ''
})

const form = reactive({
  courseId: null,
  title: '',
  resourceType: 'document',
  contentSummary: '',
  sourceUrl: '',
  valueTheme: '',
  applicableScene: '',
  keywords: '',
  difficulty: 'medium',
  status: 'draft',
  createdBy: null,
  autoAnalyze: true
})

const courseOptions = computed(() => courses.value.map(course => ({
  value: course.courseId || course.id,
  label: course.courseName || course.title || `课程 ${course.courseId || course.id}`
})))

const publishedCount = computed(() => resources.value.filter(item => item.status === 'published').length)
const themeCount = computed(() => new Set(resources.value.map(item => item.valueTheme).filter(Boolean)).size)

const loadCourses = async () => {
  try {
    const res = await listAllCourses()
    if (res.data.code === 200) {
      courses.value = res.data.data || []
    }
  } catch (error) {
    console.error('加载课程失败:', error)
  }
}

const loadResources = async () => {
  loading.value = true
  try {
    const res = await searchIdeologyResources({
      keyword: filters.keyword || undefined,
      courseId: filters.courseId || undefined,
      status: filters.status || undefined,
      limit: 100
    })
    if (res.data.code === 200) {
      resources.value = res.data.data || []
    }
  } catch (error) {
    console.error('加载思政资源失败:', error)
    ElMessage.error('加载思政资源失败')
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  editingResource.value = null
  analysisPreview.value = null
  Object.assign(form, {
    courseId: null,
    title: '',
    resourceType: 'document',
    contentSummary: '',
    sourceUrl: '',
    valueTheme: '',
    applicableScene: '',
    keywords: '',
    difficulty: 'medium',
    status: 'draft',
    createdBy: localStorage.getItem('userId'),
    autoAnalyze: true
  })
  dialogVisible.value = true
}

const openEditDialog = (item) => {
  editingResource.value = item
  analysisPreview.value = null
  Object.assign(form, {
    courseId: item.courseId,
    title: item.title,
    resourceType: item.resourceType || 'document',
    contentSummary: item.contentSummary || '',
    sourceUrl: item.sourceUrl || '',
    valueTheme: item.valueTheme || '',
    applicableScene: item.applicableScene || '',
    keywords: item.keywords || '',
    difficulty: item.difficulty || 'medium',
    status: item.status || 'draft',
    createdBy: item.createdBy || localStorage.getItem('userId'),
    autoAnalyze: false
  })
  dialogVisible.value = true
}

const previewAnalysis = async () => {
  if (!form.title && !form.contentSummary) {
    ElMessage.warning('请先填写标题或摘要')
    return
  }
  analyzing.value = true
  try {
    const res = await analyzeIdeologyResource({
      title: form.title,
      content: form.contentSummary
    })
    if (res.data.code === 200) {
      analysisPreview.value = res.data.data
      applyAnalysisToForm(res.data.data)
      ElMessage.success('AI 分析完成')
    }
  } catch (error) {
    console.error('AI 分析失败:', error)
    ElMessage.error('AI 分析失败')
  } finally {
    analyzing.value = false
  }
}

const applyAnalysisToForm = (analysis) => {
  if (!analysis) return
  form.contentSummary = form.contentSummary || analysis.summary || ''
  form.valueTheme = analysis.valueTheme || form.valueTheme
  form.applicableScene = analysis.applicableScene || form.applicableScene
  form.keywords = Array.isArray(analysis.keywords) ? analysis.keywords.join('，') : form.keywords
  form.difficulty = analysis.difficulty || form.difficulty
}

const saveResource = async () => {
  if (!form.title.trim()) {
    ElMessage.warning('请填写资源标题')
    return
  }
  saving.value = true
  try {
    const payload = { ...form, createdBy: form.createdBy || localStorage.getItem('userId') }
    const res = editingResource.value
      ? await updateIdeologyResource(editingResource.value.resourceId, payload)
      : await createIdeologyResource(payload)
    if (res.data.code === 200) {
      ElMessage.success('保存成功')
      dialogVisible.value = false
      await loadResources()
    }
  } catch (error) {
    console.error('保存思政资源失败:', error)
    ElMessage.error(error.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const confirmDelete = async (item) => {
  try {
    await ElMessageBox.confirm(`确定删除「${item.title}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteIdeologyResource(item.resourceId)
    ElMessage.success('删除成功')
    await loadResources()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const courseName = (courseId) => {
  const course = courses.value.find(item => (item.courseId || item.id) === courseId)
  return course?.courseName || course?.title || '未关联课程'
}

const splitKeywords = (keywords) => {
  if (!keywords) return []
  return keywords.split(/[,，;；\s]+/).filter(Boolean).slice(0, 5)
}

const typeText = (type) => ({
  case: '教学案例',
  policy: '政策文件',
  video: '视频资源',
  document: '文档材料',
  activity: '互动活动'
}[type] || '资源')

const difficultyText = (difficulty) => ({
  easy: '入门',
  medium: '进阶',
  hard: '挑战'
}[difficulty] || difficulty)

onMounted(async () => {
  await loadCourses()
  await loadResources()
})
</script>

<style scoped>
.ideology-page {
  min-height: 100vh;
  padding: 28px;
  background:
    radial-gradient(circle at top left, rgba(245, 158, 11, 0.16), transparent 32%),
    linear-gradient(180deg, #f8fafc 0%, #eef2f7 100%);
}

.hero-panel {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 30px;
  border-radius: 26px;
  color: #fff;
  background: linear-gradient(135deg, #0f172a 0%, #1e3a5f 52%, #b45309 140%);
  box-shadow: 0 24px 55px rgba(15, 23, 42, 0.22);
}

.eyebrow {
  margin: 0 0 8px;
  color: #facc15;
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.hero-panel h1 {
  margin: 0;
  font-size: 34px;
  letter-spacing: -0.03em;
}

.hero-desc {
  max-width: 640px;
  margin: 12px 0 0;
  color: rgba(255, 255, 255, 0.78);
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.primary-btn {
  border: none;
  background: linear-gradient(135deg, #f59e0b, #ea580c);
}

.ghost-btn {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.35);
  background: rgba(255, 255, 255, 0.08);
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin: 22px 0;
}

.metric-card {
  padding: 20px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.06);
}

.metric-card.dark {
  color: #fff;
  background: #111827;
}

.metric-card.warm {
  background: linear-gradient(135deg, #fff7ed, #fffbeb);
}

.metric-card span,
.metric-card small {
  display: block;
  color: #64748b;
}

.metric-card.dark span,
.metric-card.dark small {
  color: rgba(255, 255, 255, 0.66);
}

.metric-card strong {
  display: block;
  margin: 8px 0 4px;
  color: #0f172a;
  font-size: 28px;
}

.metric-card.dark strong {
  color: #fff;
}

.toolbar-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.06);
}

.search-control {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 260px;
  padding: 0 16px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #f8fafc;
}

.search-control input {
  width: 100%;
  height: 40px;
  border: 0;
  outline: none;
  background: transparent;
}

.filter-select {
  width: 240px;
}

.status-select {
  width: 130px;
}

.resource-board {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(330px, 1fr));
  gap: 18px;
  margin-top: 20px;
}

.resource-card {
  display: flex;
  flex-direction: column;
  min-height: 280px;
  padding: 22px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.07);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.resource-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 24px 52px rgba(15, 23, 42, 0.12);
}

.card-topline,
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.resource-type {
  color: #b45309;
  font-size: 13px;
  font-weight: 700;
}

.resource-card h3 {
  margin: 14px 0 10px;
  color: #0f172a;
  font-size: 20px;
}

.summary {
  flex: 1;
  color: #64748b;
  line-height: 1.7;
}

.tag-row,
.keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.keywords span {
  color: #475569;
  font-size: 13px;
}

.card-footer {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
  color: #64748b;
  font-size: 13px;
}

.actions {
  display: flex;
  gap: 10px;
}

.actions button {
  border: 0;
  color: #2563eb;
  background: transparent;
  cursor: pointer;
}

.actions .danger {
  color: #dc2626;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.ai-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.analysis-preview {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding: 16px;
  border-radius: 18px;
  background: #f8fafc;
}

.analysis-preview .wide {
  grid-column: span 2;
}

.analysis-preview span {
  display: block;
  color: #64748b;
  font-size: 12px;
  margin-bottom: 4px;
}

.analysis-preview strong,
.analysis-preview p {
  margin: 0;
  color: #0f172a;
}

@media (max-width: 900px) {
  .hero-panel,
  .toolbar-card {
    flex-direction: column;
    align-items: stretch;
  }

  .metric-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .filter-select,
  .status-select {
    width: 100%;
  }
}
</style>
