<template>
  <div class="student-dashboard ideology-recommend-page">
    <div class="dashboard-header">
      <div class="header-content">
        <div class="welcome-section">
          <h1 class="page-title">思政推荐</h1>
          <p class="welcome-text">基于学习画像为您精选的课程思政拓展资源</p>
        </div>
        <div class="action-section">
          <el-select v-model="selectedCourseId" placeholder="选择课程筛选" clearable @change="loadRecommendations" class="course-select">
            <el-option v-for="course in courses" :key="course.courseId" :label="course.courseName" :value="course.courseId" />
          </el-select>
          <el-button type="primary" class="ai-btn" @click="handleRefresh" :loading="refreshing" round>
            <i class="fas fa-magic" style="margin-right: 6px;"></i>智能刷新
          </el-button>
        </div>
      </div>
    </div>

    <div class="main-column" v-loading="loading">
      <div v-if="!loading && recommendations.length === 0" class="empty-state">
        <img src="https://gw.alipayobjects.com/zos/antfincdn/ZHrcdLPrvN/empty.svg" alt="Empty">
        <p>暂无推荐，请尝试切换课程或刷新</p>
      </div>

      <div v-else class="courses-grid">
        <div v-for="item in recommendations" :key="item.id" class="course-card ideology-card" @click="viewResource(item)">
          <div class="card-cover">
            <div class="cover-bg" :class="item.resource?.resourceType || 'default'">
              <i :class="getIconClass(item.resource?.resourceType)"></i>
            </div>
            <span class="type-tag" :class="item.resource?.resourceType">
              {{ getTypeName(item.resource?.resourceType) }}
            </span>
            <div class="match-score">匹配度 {{ Math.round(item.score * 100) }}%</div>
          </div>
          <div class="card-body">
            <h4 class="course-name" :title="item.resource?.title || '未知资源'">{{ item.resource?.title || '未知资源' }}</h4>
            <p class="resource-summary">{{ item.resource?.contentSummary || '暂无内容摘要' }}</p>
            <div class="course-info meta-footer">
              <span class="theme-tag" v-if="item.resource?.valueTheme">
                <i class="fas fa-seedling"></i> {{ item.resource.valueTheme }}
              </span>
            </div>
            <div class="reason-bar">
              <i class="fas fa-lightbulb text-orange"></i>
              <span>{{ item.reason || '系统基于您的学习画像综合推荐' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 资源详情对话框 -->
    <el-dialog v-model="detailVisible" width="600px" class="custom-dialog" destroy-on-close>
      <template #header>
        <div class="dialog-header">
          <span class="dialog-badge">{{ getTypeName(currentResource?.resourceType) }}</span>
          <h3>{{ currentResource?.title }}</h3>
        </div>
      </template>
      <div class="dialog-content" v-if="currentResource">
        <div class="meta-row">
          <span class="meta-item"><i class="fas fa-seedling"></i> {{ currentResource.valueTheme || '综合主题' }}</span>
          <span class="meta-item"><i class="fas fa-map-marker-alt"></i> {{ currentResource.applicableScene || '通用场景' }}</span>
        </div>
        <div class="content-section">
          <h4><i class="fas fa-align-left"></i> 内容摘要</h4>
          <p>{{ currentResource.contentSummary || '暂无内容摘要' }}</p>
        </div>
        <div class="content-section" v-if="currentResource.keywords">
          <h4><i class="fas fa-tags"></i> 关键词</h4>
          <div class="tags-wrapper">
            <span class="kw-tag" v-for="kw in currentResource.keywords.split(',')" :key="kw">#{{ kw.trim() }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="currentResource?.sourceUrl" type="primary" @click="openResource">
          前往学习 <i class="fas fa-arrow-right" style="margin-left: 4px;"></i>
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getIdeologyRecommendations,
  refreshIdeologyRecommendations,
  markRecommendationClicked
} from '@/services/ideology'
import { listCoursesByStudent } from '@/services/coursesApi'

const studentId = computed(() => localStorage.getItem('userId') || localStorage.getItem('studentId'))
const loading = ref(false)
const refreshing = ref(false)
const recommendations = ref([])
const courses = ref([])
const selectedCourseId = ref(null)
const detailVisible = ref(false)
const currentResource = ref(null)
const currentRecommendationId = ref(null)

onMounted(async () => {
  await loadCourses()
  await loadRecommendations()
})

const loadCourses = async () => {
  if (!studentId.value) return
  try {
    const res = await listCoursesByStudent(studentId.value)
    const body = normalizeResponse(res)
    if (body?.code === 200) {
      courses.value = body.data || []
    }
  } catch (error) {
    console.error('加载课程失败:', error)
  }
}

const normalizeResponse = (res) => {
  if (res && typeof res === 'object' && 'status' in res && res.data) {
    return res.data
  }
  return res
}

const loadRecommendations = async () => {
  loading.value = true
  try {
    const sid = studentId.value
    if (!sid) {
      ElMessage.warning('请先登录')
      return
    }
    const res = await getIdeologyRecommendations(sid, selectedCourseId.value, 20)
    const body = normalizeResponse(res)
    if (body?.code === 200) {
      recommendations.value = body.data || []
    }
  } catch (e) {
    console.error('加载推荐失败', e)
  } finally {
    loading.value = false
  }
}

const handleRefresh = async () => {
  refreshing.value = true
  try {
    const sid = studentId.value
    if (!sid) return
    const res = await refreshIdeologyRecommendations(sid, selectedCourseId.value, 20)
    const body = normalizeResponse(res)
    if (body?.code === 200) {
      recommendations.value = body.data || []
      ElMessage.success('推荐已刷新')
    }
  } catch (e) {
    ElMessage.error('刷新失败')
  } finally {
    refreshing.value = false
  }
}

const viewResource = async (item) => {
  currentResource.value = item.resource
  currentRecommendationId.value = item.id
  detailVisible.value = true
  try {
    await markRecommendationClicked(item.id)
    item.hasClicked = true
  } catch (e) {
    console.error('标记点击失败', e)
  }
}

const openResource = () => {
  if (currentResource.value?.sourceUrl) {
    window.open(currentResource.value.sourceUrl, '_blank')
  }
}

const getTypeName = (type) => {
  const map = { case: '教学案例', policy: '政策文件', video: '视频资料', document: '拓展文档', activity: '互动活动' }
  return map[type] || '学习资源'
}

const getIconClass = (type) => {
  const map = { case: 'fas fa-book', policy: 'fas fa-file-contract', video: 'fas fa-play-circle', document: 'fas fa-file-alt', activity: 'fas fa-users' }
  return map[type] || 'fas fa-bookmark'
}
</script>

<style scoped>
.ideology-recommend-page {
  /* 使用系统的背景色 */
  padding: 0;
}

.dashboard-header {
  background: white;
  padding: 24px 30px;
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

.course-select {
  width: 220px;
}

.ai-btn {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border: none;
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.3);
}

.courses-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.course-card {
  background: white;
  border-radius: 16px;
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
  height: 140px;
  position: relative;
  background: #f8fafc;
}

.cover-bg {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  opacity: 0.8;
}

.cover-bg.case { background: linear-gradient(135deg, #dcfce7, #a7f3d0); color: #10b981; }
.cover-bg.policy { background: linear-gradient(135deg, #fef3c7, #fde68a); color: #f59e0b; }
.cover-bg.video { background: linear-gradient(135deg, #dbeafe, #bfdbfe); color: #3b82f6; }
.cover-bg.document { background: linear-gradient(135deg, #f3f4f6, #e2e8f0); color: #64748b; }
.cover-bg.activity { background: linear-gradient(135deg, #f3e8ff, #ddd6fe); color: #8b5cf6; }

.type-tag {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  color: white;
  background: rgba(0,0,0,0.4);
  backdrop-filter: blur(4px);
}

.match-score {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
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
  margin: 0 0 8px 0;
  color: #111827;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
}

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

.meta-footer {
  margin-bottom: 12px;
}

.theme-tag {
  font-size: 12px;
  color: #d97706;
  background: #fef3c7;
  padding: 4px 8px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.reason-bar {
  background: #f8fafc;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 12px;
  color: #475569;
  display: flex;
  align-items: flex-start;
  gap: 6px;
  line-height: 1.4;
  border: 1px solid #f1f5f9;
}

.text-orange { color: #f59e0b; margin-top: 2px; }

.empty-state { text-align: center; padding: 60px 0; color: #6b7280; }
.empty-state img { width: 120px; margin-bottom: 16px; opacity: 0.5; }

/* Dialog Styles */
.dialog-header h3 { margin: 8px 0 0 0; font-size: 20px; color: #111827; }
.dialog-badge { display: inline-block; padding: 4px 8px; border-radius: 4px; background: #e0e7ff; color: #2563eb; font-size: 12px; font-weight: 600; }
.meta-row { display: flex; gap: 16px; margin-bottom: 24px; padding-bottom: 16px; border-bottom: 1px solid #f1f5f9; }
.meta-item { font-size: 14px; color: #475569; display: flex; align-items: center; gap: 6px; }
.meta-item i { color: #10b981; }
.content-section { margin-bottom: 20px; }
.content-section h4 { font-size: 15px; color: #1e293b; margin: 0 0 8px 0; display: flex; align-items: center; gap: 6px; }
.content-section h4 i { color: #94a3b8; font-size: 14px; }
.content-section p { font-size: 14px; color: #475569; line-height: 1.6; margin: 0; }
.tags-wrapper { display: flex; flex-wrap: wrap; gap: 8px; }
.kw-tag { font-size: 12px; color: #3b82f6; background: #eff6ff; padding: 4px 10px; border-radius: 6px; }

@media (max-width: 768px) {
  .header-content { flex-direction: column; align-items: flex-start; gap: 16px; }
  .action-section { width: 100%; justify-content: space-between; }
  .course-select { width: 100%; max-width: 200px; }
}
</style>
