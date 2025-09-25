<template>
  <div class="content">
    <div class="header">
      <h1 class="page-title">首页</h1>
      <div class="welcome-text">欢迎回来，王小虎！</div>
    </div>

    <div class="search-container">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input
          type="text"
          placeholder="搜索课程名称、关键词..."
          v-model="searchQuery"
          @input="filterCourses"
        />
      </div>

      <div class="search-filters">
        <button
          class="filter-btn"
          :class="{active: activeFilter === 'all'}"
          @click="setFilter('all')"
        >
          全部课程
        </button>
        <button
          class="filter-btn"
          :class="{active: activeFilter === 'document'}"
          @click="setFilter('document')"
        >
          文档课程
        </button>
        <button
          class="filter-btn"
          :class="{active: activeFilter === 'video'}"
          @click="setFilter('video')"
        >
          视频课程
        </button>
      </div>
    </div>
    <div class="courses-container">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <div class="loading-spinner"></div>
        <p>正在加载课程数据...</p>
      </div>

      <!-- 课程卡片 -->
      <div
        v-for="course in filteredCourses"
        :key="course.id"
        class="course-card"
        @click="onCardClick(course)"
      >
        <div class="course-image">
          <img :src="course.image" :alt="course.title" />
          <div class="course-type-badge" :class="course.type === 'video' ? 'video' : 'file'">
            <i :class="course.type === 'video' ? 'fas fa-video' : 'fas fa-file-alt'"></i>
            {{ course.type === 'video' ? '视频课程' : '文档课程' }}
          </div>
        </div>
        <div class="course-content">
          <h3 class="course-title">{{ course.title }}</h3>
          <p class="course-description">{{ course.description }}</p>
          <div class="course-category">
            <i class="fas fa-tag"></i>
            <span>{{ course.category }}</span>
          </div>
          <div class="course-meta">
            <span>{{ course.date }}</span>
            <span>{{ course.duration }}</span>
          </div>
        </div>
      </div>

      <div v-if="filteredCourses.length === 0" class="no-results">
        <i class="fas fa-search"></i>
        <h3>没有找到相关课程</h3>
        <p>请尝试其他搜索关键词或筛选条件</p>
      </div>
    </div>

    <CoursePlayer
      v-if="activeCourse"
      v-model="playerVisible"
      :course-id="activeCourse.id"
      :title="activeCourse.title"
      :chapters="activeCourse.chapters || []"
      :fallback-src="activeCourse.videoUrl || ''"
      @progress="onOverallProgress"
    />

    <DocumentViewer
      v-model="docVisible"
      :id="activeDoc?.id"
      :title="activeDoc?.title || '文档课程'"
      :file-url="activeDoc?.url || ''"
      :html-content="activeDoc?.html || ''"
      :progress="0"
      :image="activeDoc?.image || ''"
      :duration="activeDoc?.duration || ''"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import CoursePlayer from '../../components/CoursePlayer.vue'
import DocumentViewer from '../../components/DocumentViewer.vue'
import { addOrUpdateRecord } from '../../services/student/historyService.js'
import { fetchHomeCourses, filterCoursesByType } from '../../services/student/homeCoursesApi.js'

const searchQuery = ref('')
const activeFilter = ref('all')
const courses = ref([])
const loading = ref(false)

const filteredCourses = computed(() => {
  // 只根据课程类型筛选（视频或文档）
  return filterCoursesByType(courses.value, activeFilter.value)
})

const setFilter = (filter) => {
  activeFilter.value = filter
}

// 加载课程数据
const loadCourses = async () => {
  loading.value = true
  try {
    courses.value = await fetchHomeCourses()
  } catch (error) {
    console.error('加载课程数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadCourses()
})

const playerVisible = ref(false)
const activeCourse = ref(null)
const openCourse = (course) => {
  if (course.type !== 'video') return
  activeCourse.value = course
  playerVisible.value = true
}

const onOverallProgress = (overall) => {}

const docVisible = ref(false)
const activeDoc = ref(null)
const openDoc = (course) => {
  activeDoc.value = course
  docVisible.value = true
}

const onCardClick = (course) => {
  if (course.type === 'video') {
    addOrUpdateRecord({ id: course.id, type: 'video', title: course.title, image: course.image, duration: course.duration, progress: 0 })
    openCourse(course)
  } else {
    addOrUpdateRecord({ id: course.id, type: 'document', title: course.title, image: course.image, duration: course.duration, progress: 0 })
    openDoc(course)
  }
}
</script>

<style scoped>
.content {
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.page-title {
  font-size: 24px;
  font-weight: bold;
  color: #2c3e50;
}

.welcome-text {
  color: #666;
  font-size: 16px;
}

.search-container {
  background: white;
  border-radius: 10px;
  padding: 25px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);
  margin-bottom: 30px;
}

.search-box {
  display: flex;
  align-items: center;
  background: #f0f5ff;
  border-radius: 8px;
  padding: 10px 15px;
  margin-bottom: 20px;
}

.search-box input {
  flex-grow: 1;
  border: none;
  background: transparent;
  padding: 10px;
  font-size: 16px;
  outline: none;
}

.search-box i {
  color: #2563eb;
  font-size: 20px;
  margin-right: 10px;
}

.search-filters {
  display: flex;
  gap: 15px;
}

.filter-btn {
  padding: 8px 16px;
  background: #f0f5ff;
  border: none;
  border-radius: 6px;
  color: #2563eb;
  cursor: pointer;
  transition: all 0.3s;
}

.filter-btn.active {
  background: #2563eb;
  color: white;
}

.filter-btn:hover {
  background: #dbeafe;
}

.filter-btn.active:hover {
  background: #2563eb;
}

.courses-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.course-card {
  background: white;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s;
}

.course-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

.course-image {
  height: 160px;
  overflow: hidden;
  position: relative;
}

.course-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s;
}

.course-card:hover .course-image img {
  transform: scale(1.05);
}

.course-type-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: white;
  display: flex;
  align-items: center;
  gap: 4px;
}

.course-type-badge.video {
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
}

.course-type-badge.file {
  background: linear-gradient(135deg, #4ecdc4, #44bd87);
}

.course-content {
  padding: 20px;
}

.course-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 10px;
  color: #2c3e50;
}

.course-description {
  color: #64748b;
  margin-bottom: 15px;
  font-size: 14px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.course-category {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
  font-size: 12px;
  color: #1890ff;
  background: #f0f8ff;
  padding: 4px 8px;
  border-radius: 12px;
  width: fit-content;
}

.course-meta {
  display: flex;
  justify-content: space-between;
  color: #94a3b8;
  font-size: 13px;
}

.no-results {
  text-align: center;
  padding: 40px;
  color: #94a3b8;
  grid-column: 1 / -1;
}

.no-results i {
  font-size: 48px;
  margin-bottom: 15px;
  display: block;
}

.no-results h3 {
  margin-bottom: 10px;
  color: #2c3e50;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #94a3b8;
  grid-column: 1 / -1;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f4f6;
  border-top: 4px solid #2563eb;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 15px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@media (max-width: 992px) {
  .courses-container {
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  }
}

@media (max-width: 768px) {
  .header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .search-filters {
    flex-wrap: wrap;
  }

  .courses-container {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 576px) {
  .content {
    padding: 20px;
  }

  .search-container {
    padding: 20px;
  }

  .search-box {
    padding: 8px 12px;
  }

  .search-box input {
    padding: 8px;
    font-size: 14px;
  }
}
</style>
