<template>
  <div class="student-dashboard">
    <!-- 顶部欢迎区 -->
    <div class="dashboard-header">
      <div class="header-content">
        <div class="welcome-section">
          <h1 class="page-title">思政推荐</h1>
          <p class="welcome-text">基于您的学习进度、课程上下文和错题情况，精选的专业思政拓展资源。</p>
        </div>
        <div class="action-section">
          <el-button :loading="refreshing" class="ai-btn" type="primary" round @click="refreshRecommendations">
            <i class="fas fa-magic" style="margin-right: 6px;"></i> 智能刷新
          </el-button>
        </div>
      </div>
    </div>

    <!-- 筛选工具栏 -->
    <div class="section-toolbar">
      <div class="toolbar-left">
        <h3 class="section-title"><i class="fas fa-compass"></i> 当前画像：<span class="highlight">{{ profileText }}</span></h3>
      </div>
      <div class="toolbar-actions">
        <div class="filter-group">
          <label>按课程聚焦：</label>
          <el-select v-model="selectedCourseId" placeholder="全部课程" clearable filterable @change="loadRecommendations" class="course-select">
            <el-option v-for="course in enrolledCourses" :key="course.courseId" :label="course.courseName" :value="course.courseId" />
          </el-select>
        </div>
      </div>
    </div>

    <!-- 推荐资源网格 -->
    <div class="courses-grid" v-loading="loading">
      <div v-if="!loading && recommendations.length === 0" class="empty-state">
        <img src="https://gw.alipayobjects.com/zos/antfincdn/ZHrcdLPrvN/empty.svg" alt="Empty">
        <p>暂无推荐资源，请尝试刷新或选择其他课程</p>
      </div>

      <div
        v-else
        v-for="item in recommendations"
        :key="item.id"
        class="course-card"
        @click="openResource(item)"
      >
        <div class="card-cover">
          <div class="cover-bg" :class="item.resource?.resourceType || 'default'">
            <i :class="getIconClass(item.resource?.resourceType)"></i>
          </div>
          <span class="type-tag" :class="item.resource?.resourceType">
            {{ resourceTypeText(item.resource?.resourceType) }}
          </span>
          <div class="match-score">匹配度 {{ Math.round((item.score || 0) * 100) }}%</div>
        </div>
        
        <div class="card-body">
          <h4 class="course-name" :title="item.resource?.title">{{ item.resource?.title || '未命名资源' }}</h4>
          
          <div class="reason-bar">
            <i class="fas fa-lightbulb text-orange"></i>
            <span>{{ item.reason || '系统综合推荐' }}</span>
          </div>
          
          <p class="resource-summary">{{ item.resource?.contentSummary || '暂无摘要，建议进入资源查看详情。' }}</p>
          
          <div class="meta-tags">
            <span v-if="item.resource?.valueTheme"><i class="fas fa-seedling"></i> {{ item.resource.valueTheme }}</span>
            <span v-if="item.resource?.applicableScene"><i class="fas fa-map-marker-alt"></i> {{ item.resource.applicableScene }}</span>
          </div>

          <div class="card-footer">
            <el-button type="primary" link @click.stop="markClicked(item)">
              <i class="fas fa-check" style="margin-right: 4px;"></i> 标记已学
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 资源详情弹窗 -->
    <el-dialog v-model="detailVisible" width="600px" class="custom-dialog" destroy-on-close>
      <template #header>
        <div class="dialog-header">
          <span class="dialog-badge">{{ activeResource?.resourceType ? resourceTypeText(activeResource.resourceType) : '思政资源' }}</span>
          <h3>{{ activeResource?.title }}</h3>
        </div>
      </template>
      <div class="dialog-content" v-if="activeResource">
        <div class="meta-row">
          <span class="meta-item"><i class="fas fa-seedling"></i> {{ activeResource.valueTheme || '价值引导' }}</span>
          <span class="meta-item"><i class="fas fa-map-marker-alt"></i> {{ activeResource.applicableScene || '课后拓展' }}</span>
          <span class="meta-item"><i class="fas fa-layer-group"></i> {{ difficultyText(activeResource.difficulty) }}</span>
        </div>
        <div class="content-section">
          <h4><i class="fas fa-align-left"></i> 内容摘要</h4>
          <p>{{ activeResource.contentSummary || '暂无内容摘要' }}</p>
        </div>
        <div class="content-section" v-if="splitKeywords(activeResource.keywords).length">
          <h4><i class="fas fa-tags"></i> 关键词</h4>
          <div class="tags-wrapper">
            <span class="kw-tag" v-for="kw in splitKeywords(activeResource.keywords)" :key="kw">#{{ kw }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="hasResourceLink(activeResource)" type="primary" @click="openResourceLink(activeResource)">
          前往学习 <i class="fas fa-arrow-right" style="margin-left: 4px;"></i>
        </el-button>
        <el-button v-else disabled type="info" plain>资源暂未开放</el-button>
      </template>
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
    return '进阶探索型'
  }
  return selectedCourseId.value ? '课程聚焦型' : '综合拓展型'
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

const openResourceLink = (resource) => {
  if (resource?.sourceUrl) {
    window.open(resource.sourceUrl, '_blank', 'noopener')
  }
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
  video: '视频资料',
  document: '拓展文档',
  activity: '互动活动'
}[type] || '学习资源')

const getIconClass = (type) => {
  const map = { case: 'fas fa-book', policy: 'fas fa-file-contract', video: 'fas fa-play-circle', document: 'fas fa-file-alt', activity: 'fas fa-users' }
  return map[type] || 'fas fa-bookmark'
}

const hasResourceLink = (resource) => Boolean(resource?.sourceUrl)

onMounted(async () => {
  await loadCourses()
  await loadRecommendations()
})
</script>

<style scoped>
/* 继承全局样式架构 */
.student-dashboard {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  min-height: 100vh;
  background-color: #f5f7fa;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: #1f2937;
}

/* 顶部 Header */
.dashboard-header {
  background: white;
  padding: 20px 24px;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  margin-bottom: 24px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 6px 0;
  color: #111827;
}

.welcome-text {
  color: #6b7280;
  font-size: 14px;
  margin: 0;
}

.action-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.ai-btn {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border: none;
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.3);
}

/* 工具栏 */
.section-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  background: white;
  padding: 16px 24px;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  border: 1px solid rgba(229, 231, 235, 0.5);
  margin-bottom: 24px;
}

.section-title { 
  font-size: 16px; 
  font-weight: 600; 
  margin: 0; 
  display: flex; 
  align-items: center; 
  gap: 8px; 
  color: #111827; 
}

.highlight {
  color: #3b82f6;
}

.toolbar-actions { 
  display: flex; 
  gap: 16px; 
  flex-wrap: wrap; 
  align-items: center; 
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-group label {
  font-size: 14px;
  color: #475569;
  font-weight: 500;
}

.course-select {
  width: 240px;
}

/* 课程网格 */
.courses-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.course-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
}

.course-card:hover { 
  transform: translateY(-4px); 
  box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1); 
}

.card-cover {
  height: 120px;
  position: relative;
  background: #f8fafc;
  overflow: hidden;
}

.cover-bg {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  opacity: 0.8;
  transition: transform 0.5s;
}

.course-card:hover .cover-bg {
  transform: scale(1.1);
}

.cover-bg.case { background: linear-gradient(135deg, #dcfce7, #a7f3d0); color: #10b981; }
.cover-bg.policy { background: linear-gradient(135deg, #fef3c7, #fde68a); color: #f59e0b; }
.cover-bg.video { background: linear-gradient(135deg, #dbeafe, #bfdbfe); color: #3b82f6; }
.cover-bg.document { background: linear-gradient(135deg, #f3f4f6, #e2e8f0); color: #64748b; }
.cover-bg.activity { background: linear-gradient(135deg, #f3e8ff, #ddd6fe); color: #8b5cf6; }

.type-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  color: white;
  background: rgba(0,0,0,0.5);
  backdrop-filter: blur(4px);
}

.match-score {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  color: #10b981;
  background: rgba(255,255,255,0.95);
  box-shadow: 0 2px 6px rgba(0,0,0,0.08);
}

.card-body {
  padding: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.course-name {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 12px 0;
  color: #111827;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
}

.reason-bar {
  background: #fffbeb;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 12px;
  color: #92400e;
  display: flex;
  align-items: flex-start;
  gap: 6px;
  line-height: 1.4;
  margin-bottom: 12px;
  border: 1px solid #fef3c7;
}

.text-orange { color: #d97706; margin-top: 2px; }

.resource-summary {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 16px 0;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.meta-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.meta-tags span {
  font-size: 12px;
  color: #475569;
  background: #f1f5f9;
  padding: 4px 8px;
  border-radius: 4px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.meta-tags i {
  color: #3b82f6;
}

.card-footer {
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  justify-content: flex-end;
}

.empty-state { 
  text-align: center; 
  padding: 60px 0; 
  color: #6b7280; 
  grid-column: 1 / -1;
}
.empty-state img { width: 120px; margin-bottom: 16px; opacity: 0.5; }

/* 弹窗样式 */
.dialog-header h3 { margin: 8px 0 0 0; font-size: 20px; color: #111827; }
.dialog-badge { display: inline-block; padding: 4px 8px; border-radius: 4px; background: #e0e7ff; color: #2563eb; font-size: 12px; font-weight: 600; }
.meta-row { display: flex; gap: 16px; margin-bottom: 24px; padding-bottom: 16px; border-bottom: 1px solid #f1f5f9; }
.meta-item { font-size: 14px; color: #475569; display: flex; align-items: center; gap: 6px; }
.meta-item i { color: #10b981; }
.content-section { margin-bottom: 20px; }
.content-section h4 { font-size: 15px; color: #1e293b; margin: 0 0 8px 0; display: flex; align-items: center; gap: 6px; }
.content-section h4 i { color: #94a3b8; font-size: 14px; }
.content-section p { font-size: 14px; color: #475569; line-height: 1.6; margin: 0; background: #f8fafc; padding: 12px; border-radius: 8px; border: 1px solid #f1f5f9;}
.tags-wrapper { display: flex; flex-wrap: wrap; gap: 8px; }
.kw-tag { font-size: 12px; color: #3b82f6; background: #eff6ff; padding: 4px 10px; border-radius: 6px; }

@media (max-width: 768px) {
  .header-content { flex-direction: column; align-items: flex-start; gap: 16px; }
  .section-toolbar { flex-direction: column; align-items: flex-start; }
  .course-select { width: 100%; }
}
</style>
