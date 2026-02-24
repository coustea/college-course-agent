<template>
  <div class="course-recommendation">
    <div class="header">
      <h3>
        <i class="fas fa-magic"></i>
        智能推荐
      </h3>
      <el-button
        type="primary"
        :icon="RefreshRight"
        @click="refreshRecommendations"
        :loading="refreshing"
        size="small"
      >
        换一批
      </el-button>
    </div>

    <div v-loading="loading" class="recommendation-list">
      <div v-if="recommendations.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无推荐课程" />
      </div>

      <div
        v-for="item in recommendations"
        :key="item.id"
        class="recommendation-item"
        @click="handleCourseClick(item)"
      >
        <div class="recommendation-badge" :class="getBadgeClass(item.recommendationType)">
          {{ getTypeText(item.recommendationType) }}
        </div>

        <div class="course-info">
          <h4 class="course-title">{{ getCourseTitle(item.courseId) }}</h4>
          <p class="recommendation-reason">
            <i class="fas fa-lightbulb"></i>
            {{ item.reason }}
          </p>
          <div class="course-meta">
            <el-tag size="small" type="info">推荐指数 {{ (item.score * 100).toFixed(0) }}%</el-tag>
          </div>
        </div>

        <div class="action-btn">
          <el-button type="primary" size="small" @click.stop="viewCourse(item.courseId)">
            查看课程
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { RefreshRight } from '@element-plus/icons-vue'
import axios from 'axios'

const props = defineProps({
  limit: {
    type: Number,
    default: 6
  }
})

const emit = defineEmits(['course-click'])

const loading = ref(false)
const refreshing = ref(false)
const recommendations = ref([])
const courseCache = ref(new Map()) // 缓存课程信息

// 获取推荐列表
const fetchRecommendations = async (showLoading = true) => {
  if (showLoading) {
    loading.value = true
  }

  try {
    const token = localStorage.getItem('token')
    const response = await axios.get('/api/recommendation/list', {
      params: { limit: props.limit },
      headers: { Authorization: `Bearer ${token}` }
    })

    if (response.data.code === 200) {
      recommendations.value = response.data.data || []

      // 预加载课程信息
      for (const item of recommendations.value) {
        if (!courseCache.value.has(item.courseId)) {
          await loadCourseInfo(item.courseId)
        }
      }
    }
  } catch (error) {
    console.error('获取推荐失败:', error)
    if (showLoading) {
      ElMessage.error('获取推荐失败')
    }
  } finally {
    if (showLoading) {
      loading.value = false
    }
  }
}

// 加载课程信息
const loadCourseInfo = async (courseId) => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get(`/api/course/${courseId}`, {
      headers: { Authorization: `Bearer ${token}` }
    })

    if (response.data.code === 200) {
      courseCache.value.set(courseId, response.data.data)
    }
  } catch (error) {
    console.error('加载课程信息失败:', error)
  }
}

// 获取课程标题
const getCourseTitle = (courseId) => {
  const course = courseCache.value.get(courseId)
  return course ? course.courseName || course.title : '加载中...'
}

// 获取推荐类型徽章样式
const getBadgeClass = (type) => {
  const classMap = {
    'CONTENT_BASED': 'content-badge',
    'COLLABORATIVE': 'collaborative-badge',
    'POPULAR': 'popular-badge'
  }
  return classMap[type] || 'default-badge'
}

// 获取推荐类型文本
const getTypeText = (type) => {
  const textMap = {
    'CONTENT_BASED': '内容匹配',
    'COLLABORATIVE': '相似推荐',
    'POPULAR': '热门推荐'
  }
  return textMap[type] || '智能推荐'
}

// 刷新推荐
const refreshRecommendations = async () => {
  refreshing.value = true
  try {
    const token = localStorage.getItem('token')
    await axios.post('/api/recommendation/refresh', {}, {
      headers: { Authorization: `Bearer ${token}` }
    })

    await fetchRecommendations(false)
    ElMessage.success('推荐已刷新')
  } catch (error) {
    console.error('刷新推荐失败:', error)
    ElMessage.error('刷新推荐失败')
  } finally {
    refreshing.value = false
  }
}

// 处理课程点击
const handleCourseClick = async (item) => {
  try {
    const token = localStorage.getItem('token')
    await axios.post(`/api/recommendation/${item.id}/click`, {}, {
      headers: { Authorization: `Bearer ${token}` }
    })
  } catch (error) {
    console.error('标记点击失败:', error)
  }

  emit('course-click', item.courseId)
}

// 查看课程
const viewCourse = (courseId) => {
  emit('course-click', courseId)
}

onMounted(() => {
  fetchRecommendations()
})

// 暴露刷新方法给父组件
defineExpose({
  refresh: refreshRecommendations
})
</script>

<style scoped>
.course-recommendation {
  background: white;
  border-radius: 8px;
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
  display: flex;
  align-items: center;
  gap: 8px;
}

.header h3 i {
  color: #f39c12;
}

.recommendation-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.recommendation-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  cursor: pointer;
  transition: all 0.3s ease;
}

.recommendation-item:hover {
  background: #e9ecef;
  border-color: #dee2e6;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.recommendation-badge {
  position: absolute;
  top: -8px;
  right: 12px;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.content-badge {
  background: #e3f2fd;
  color: #1976d2;
}

.collaborative-badge {
  background: #f3e5f5;
  color: #7b1fa2;
}

.popular-badge {
  background: #fff3e0;
  color: #e65100;
}

.course-info {
  flex: 1;
}

.course-title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

.recommendation-reason {
  margin: 0 0 8px;
  font-size: 14px;
  color: #6c757d;
  display: flex;
  align-items: center;
  gap: 6px;
}

.recommendation-reason i {
  color: #f39c12;
}

.course-meta {
  display: flex;
  gap: 8px;
  align-items: center;
}

.action-btn {
  flex-shrink: 0;
}

.empty-state {
  padding: 40px 20px;
  text-align: center;
}
</style>
