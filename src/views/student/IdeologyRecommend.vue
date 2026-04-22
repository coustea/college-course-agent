<template>
  <div class="ideology-recommend">
    <el-card class="header-card">
      <div class="header">
        <h2>思政学习</h2>
        <el-button type="primary" @click="handleRefresh" :loading="refreshing">
          <el-icon><Refresh /></el-icon>
          刷新推荐
        </el-button>
      </div>
      <p class="subtitle">为您推荐的课程思政学习资源</p>
    </el-card>

    <!-- 课程筛选 -->
    <el-card class="filter-card">
      <el-select v-model="selectedCourseId" placeholder="选择课程筛选推荐" clearable @change="loadRecommendations">
        <el-option v-for="course in courses" :key="course.courseId" :label="course.courseName" :value="course.courseId" />
      </el-select>
    </el-card>

    <!-- 推荐列表 -->
    <div class="recommendations" v-loading="loading">
      <el-empty v-if="!loading && recommendations.length === 0" description="暂无推荐，请稍后刷新" />

      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="item in recommendations" :key="item.id">
          <el-card class="recommend-card" shadow="hover" @click="viewResource(item)">
            <div class="card-header">
              <el-tag :type="getTypeTag(item.resource?.resourceType)" size="small">
                {{ getTypeName(item.resource?.resourceType) }}
              </el-tag>
              <span class="score">匹配度: {{ Math.round(item.score * 100) }}%</span>
            </div>
            <h3 class="title">{{ item.resource?.title || '未知资源' }}</h3>
            <p class="summary">{{ item.resource?.contentSummary || '暂无摘要' }}</p>
            <div class="meta">
              <span class="theme">
                <el-icon><CollectionTag /></el-icon>
                {{ item.resource?.valueTheme || '综合' }}
              </span>
            </div>
            <div class="reason">
              <el-icon><InfoFilled /></el-icon>
              {{ item.reason || '基于您的学习画像推荐' }}
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 资源详情对话框 -->
    <el-dialog v-model="detailVisible" title="资源详情" width="600px">
      <el-descriptions :column="2" border v-if="currentResource">
        <el-descriptions-item label="资源标题" :span="2">
          {{ currentResource.title }}
        </el-descriptions-item>
        <el-descriptions-item label="资源类型">
          {{ getTypeName(currentResource.resourceType) }}
        </el-descriptions-item>
        <el-descriptions-item label="价值主题">
          {{ currentResource.valueTheme || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="内容摘要" :span="2">
          {{ currentResource.contentSummary || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="适用场景">
          {{ currentResource.applicableScene || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="关键词">
          {{ currentResource.keywords || '-' }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="currentResource?.sourceUrl" type="primary" @click="openResource">
          查看资源
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, CollectionTag, InfoFilled } from '@element-plus/icons-vue'
import {
  getIdeologyRecommendations,
  refreshIdeologyRecommendations,
  markRecommendationClicked
} from '@/services/ideology'

const studentId = computed(() => localStorage.getItem('userId') || localStorage.getItem('studentId'))
const loading = ref(false)
const refreshing = ref(false)
const recommendations = ref([])
const courses = ref([])
const selectedCourseId = ref(null)
const detailVisible = ref(false)
const currentResource = ref(null)
const currentRecommendationId = ref(null)

onMounted(() => {
  loadRecommendations()
})

const loadRecommendations = async () => {
  loading.value = true
  try {
    const sid = studentId.value
    if (!sid) {
      ElMessage.warning('请先登录')
      return
    }

    const res = await getIdeologyRecommendations(sid, selectedCourseId.value, 20)
    if (res.code === 200) {
      recommendations.value = res.data || []
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
    if (res.code === 200) {
      recommendations.value = res.data || []
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

  // 标记为已点击
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
  const map = { case: '案例', policy: '政策', video: '视频', document: '文档', activity: '活动' }
  return map[type] || '资源'
}

const getTypeTag = (type) => {
  const map = { case: 'success', policy: 'warning', video: 'danger', document: '', activity: 'info' }
  return map[type] || ''
}
</script>

<style scoped>
.ideology-recommend {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.header-card {
  margin-bottom: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header h2 {
  margin: 0;
  font-size: 22px;
  color: #303133;
}

.subtitle {
  margin: 8px 0 0;
  color: #909399;
  font-size: 14px;
}

.filter-card {
  margin-bottom: 20px;
}

.recommendations {
  min-height: 300px;
}

.recommend-card {
  margin-bottom: 20px;
  cursor: pointer;
  transition: transform 0.2s;
}

.recommend-card:hover {
  transform: translateY(-4px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.score {
  font-size: 12px;
  color: #67c23a;
  font-weight: 500;
}

.title {
  margin: 0 0 8px;
  font-size: 16px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary {
  margin: 0 0 12px;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
  height: 40px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.meta {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.theme {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.reason {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #409eff;
  padding-top: 8px;
  border-top: 1px solid #ebeef5;
}
</style>