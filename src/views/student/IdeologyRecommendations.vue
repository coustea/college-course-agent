<template>
  <div class="ideology-student-page">
    <section class="student-hero">
      <div>
        <p class="eyebrow">Personalized Ideology Path</p>
        <h1>思政资源推荐</h1>
        <p>根据你的学习进度、课程上下文和错题情况，推荐更适合当前阶段的案例、政策与拓展材料。</p>
      </div>
      <el-button :loading="refreshing" class="refresh-btn" @click="refreshRecommendations">
        <i class="fas fa-wand-magic-sparkles"></i>
        重新生成推荐
      </el-button>
    </section>

    <section class="focus-panel">
      <div class="focus-card">
        <span>当前画像</span>
        <strong>{{ profileText }}</strong>
        <small>由学习进度与错题状态推断</small>
      </div>
      <div class="course-filter">
        <label>按课程聚焦</label>
        <el-select v-model="selectedCourseId" placeholder="全部课程" clearable filterable @change="loadRecommendations">
          <el-option v-for="course in enrolledCourses" :key="course.courseId" :label="course.courseName" :value="course.courseId" />
        </el-select>
      </div>
    </section>

    <section v-loading="loading" class="recommendation-grid">
      <el-empty v-if="!loading && recommendations.length === 0" description="暂无推荐资源，请先刷新推荐或选择其他课程" />

      <article v-for="item in recommendations" v-else :key="item.id" class="recommendation-card">
        <div class="glow"></div>
        <div class="card-head">
          <span class="score">{{ Math.round((item.score || 0) * 100) }}%</span>
          <el-tag :type="item.recommendationType === 'TAG_MATCH' ? 'warning' : 'info'" effect="light">
            {{ recommendationTypeText(item.recommendationType) }}
          </el-tag>
        </div>
        <h2>{{ item.resource?.title || '未命名资源' }}</h2>
        <p class="reason">{{ item.reason || '系统根据当前课程学习阶段推荐' }}</p>
        <p class="summary">{{ item.resource?.contentSummary || '暂无摘要，建议进入资源查看详情。' }}</p>

        <div class="meta-strip">
          <span><i class="fas fa-seedling"></i>{{ item.resource?.valueTheme || '价值引导' }}</span>
          <span><i class="fas fa-location-dot"></i>{{ item.resource?.applicableScene || '课后拓展' }}</span>
          <span><i class="fas fa-layer-group"></i>{{ difficultyText(item.resource?.difficulty) }}</span>
        </div>

        <div class="keyword-row">
          <span v-for="keyword in splitKeywords(item.resource?.keywords)" :key="keyword">{{ keyword }}</span>
        </div>

        <div class="card-actions">
          <el-button text @click="openResource(item)">查看资源</el-button>
          <el-button type="primary" plain @click="markClicked(item)">我已学习</el-button>
        </div>
      </article>
    </section>

    <el-dialog v-model="detailVisible" width="680px" class="resource-detail-dialog">
      <template #header>
        <div class="detail-title">
          <span>{{ activeResource?.resourceType ? resourceTypeText(activeResource.resourceType) : '思政资源' }}</span>
          <h3>{{ activeResource?.title }}</h3>
        </div>
      </template>
      <div class="detail-body">
        <p>{{ activeResource?.contentSummary || '暂无详细摘要。' }}</p>
        <div class="detail-metas">
          <el-tag type="warning">{{ activeResource?.valueTheme || '价值引导' }}</el-tag>
          <el-tag type="primary">{{ activeResource?.applicableScene || '课后拓展' }}</el-tag>
          <el-tag>{{ difficultyText(activeResource?.difficulty) }}</el-tag>
        </div>
        <div class="detail-keywords">
          <span v-for="keyword in splitKeywords(activeResource?.keywords)" :key="keyword">#{{ keyword }}</span>
        </div>
        <a v-if="activeResource?.sourceUrl" class="resource-link" :href="activeResource.sourceUrl" target="_blank" rel="noopener">
          打开资源链接
        </a>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listCoursesByStudent } from '@/services/coursesApi'
import {
  getIdeologyRecommendations,
  markIdeologyRecommendationClicked,
  refreshIdeologyRecommendations
} from '@/services/ideologyApi'

const loading = ref(false)
const refreshing = ref(false)
const recommendations = ref([])
const enrolledCourses = ref([])
const selectedCourseId = ref(null)
const detailVisible = ref(false)
const activeResource = ref(null)

const studentId = computed(() => localStorage.getItem('userId') || localStorage.getItem('studentId'))
const profileText = computed(() => {
  if (recommendations.value.some(item => item.recommendationType === 'TAG_MATCH')) {
    return '标签匹配型学习者'
  }
  return selectedCourseId.value ? '课程聚焦型学习者' : '综合拓展型学习者'
})

const loadCourses = async () => {
  if (!studentId.value) return
  try {
    const res = await listCoursesByStudent(studentId.value)
    if (res.data.code === 200) {
      enrolledCourses.value = res.data.data || []
    }
  } catch (error) {
    console.error('加载已选课程失败:', error)
  }
}

const loadRecommendations = async () => {
  if (!studentId.value) {
    ElMessage.warning('请先登录')
    return
  }
  loading.value = true
  try {
    const res = await getIdeologyRecommendations({
      studentId: studentId.value,
      courseId: selectedCourseId.value || undefined,
      limit: 12
    })
    if (res.data.code === 200) {
      recommendations.value = res.data.data || []
    }
  } catch (error) {
    console.error('加载思政推荐失败:', error)
    ElMessage.error('加载思政推荐失败')
  } finally {
    loading.value = false
  }
}

const refreshRecommendations = async () => {
  if (!studentId.value) return
  refreshing.value = true
  try {
    const res = await refreshIdeologyRecommendations({
      studentId: studentId.value,
      courseId: selectedCourseId.value || undefined,
      limit: 12
    })
    if (res.data.code === 200) {
      recommendations.value = res.data.data || []
      ElMessage.success('推荐已更新')
    }
  } catch (error) {
    console.error('刷新推荐失败:', error)
    ElMessage.error('刷新推荐失败')
  } finally {
    refreshing.value = false
  }
}

const openResource = (item) => {
  activeResource.value = item.resource
  detailVisible.value = true
}

const markClicked = async (item) => {
  try {
    await markIdeologyRecommendationClicked(item.id)
    item.hasClicked = true
    ElMessage.success('已记录学习状态')
  } catch (error) {
    console.error('标记推荐失败:', error)
    ElMessage.error('操作失败')
  }
}

const splitKeywords = (keywords) => {
  if (!keywords) return []
  return keywords.split(/[,，;；\s]+/).filter(Boolean).slice(0, 6)
}

const recommendationTypeText = (type) => ({
  TAG_MATCH: '画像匹配',
  PROGRESS_SCENE: '进度场景',
  POPULAR: '通用推荐'
}[type] || '推荐')

const difficultyText = (difficulty) => ({
  easy: '入门',
  medium: '进阶',
  hard: '挑战'
}[difficulty] || '进阶')

const resourceTypeText = (type) => ({
  case: '教学案例',
  policy: '政策文件',
  video: '视频资源',
  document: '文档材料',
  activity: '互动活动'
}[type] || '思政资源')

onMounted(async () => {
  await loadCourses()
  await loadRecommendations()
})
</script>

<style scoped>
.ideology-student-page {
  min-height: 100vh;
  padding: 28px;
  background:
    radial-gradient(circle at 12% 8%, rgba(34, 197, 94, 0.14), transparent 30%),
    radial-gradient(circle at 88% 0%, rgba(251, 191, 36, 0.18), transparent 26%),
    #f6f8fb;
}

.student-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 32px;
  border-radius: 28px;
  color: #fff;
  background: linear-gradient(135deg, #064e3b 0%, #0f766e 48%, #f59e0b 135%);
  box-shadow: 0 24px 50px rgba(6, 78, 59, 0.2);
}

.eyebrow {
  margin: 0 0 8px;
  color: #fde68a;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.student-hero h1 {
  margin: 0;
  font-size: 34px;
}

.student-hero p {
  max-width: 670px;
  color: rgba(255, 255, 255, 0.78);
  line-height: 1.8;
}

.refresh-btn {
  color: #064e3b;
  border: 0;
  background: #fef3c7;
  font-weight: 700;
}

.focus-panel {
  display: grid;
  grid-template-columns: minmax(260px, 360px) 1fr;
  gap: 16px;
  margin: 22px 0;
}

.focus-card,
.course-filter {
  padding: 20px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.07);
}

.focus-card span,
.focus-card small,
.course-filter label {
  display: block;
  color: #64748b;
}

.focus-card strong {
  display: block;
  margin: 8px 0;
  color: #0f172a;
  font-size: 24px;
}

.course-filter {
  display: flex;
  align-items: center;
  gap: 18px;
}

.course-filter .el-select {
  flex: 1;
}

.recommendation-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 18px;
}

.recommendation-card {
  position: relative;
  overflow: hidden;
  min-height: 330px;
  padding: 24px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.08);
}

.glow {
  position: absolute;
  inset: -60px auto auto -60px;
  width: 150px;
  height: 150px;
  border-radius: 50%;
  background: rgba(245, 158, 11, 0.18);
}

.card-head {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.score {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 58px;
  height: 58px;
  border-radius: 18px;
  color: #0f766e;
  background: #ccfbf1;
  font-weight: 800;
}

.recommendation-card h2 {
  position: relative;
  margin: 18px 0 10px;
  color: #0f172a;
  font-size: 22px;
}

.reason {
  color: #b45309;
  font-weight: 700;
}

.summary {
  color: #64748b;
  line-height: 1.75;
}

.meta-strip,
.keyword-row,
.card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.meta-strip span {
  padding: 8px 10px;
  border-radius: 999px;
  color: #334155;
  background: #f1f5f9;
  font-size: 13px;
}

.meta-strip i {
  margin-right: 6px;
  color: #0f766e;
}

.keyword-row span {
  color: #0f766e;
  font-size: 13px;
}

.card-actions {
  justify-content: flex-end;
  padding-top: 12px;
}

.detail-title span {
  color: #0f766e;
  font-weight: 700;
}

.detail-title h3 {
  margin: 6px 0 0;
  color: #0f172a;
}

.detail-body p {
  color: #475569;
  line-height: 1.8;
}

.detail-metas,
.detail-keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.detail-keywords span {
  color: #64748b;
}

.resource-link {
  display: inline-flex;
  margin-top: 22px;
  color: #0f766e;
  font-weight: 700;
  text-decoration: none;
}

@media (max-width: 820px) {
  .student-hero,
  .course-filter {
    flex-direction: column;
    align-items: stretch;
  }

  .focus-panel {
    grid-template-columns: 1fr;
  }
}
</style>
