<template>
  <div class="profile-page">
    <div class="header">
      <h1 class="page-title">个人中心</h1>
      <div class="welcome">欢迎回来，王小虎！</div>
    </div>

    <div class="profile-container">
      <div class="profile-left">
        <div class="profile-card">
          <div class="profile-header">
            <div class="profile-avatar">虎</div>
            <h2 class="profile-name">王小虎</h2>
            <div class="profile-role">计算机科学与技术 · 研究生</div>
          </div>

          <div class="profile-stats">
            <div class="stat-item">
              <div class="stat-value">0</div>
              <div class="stat-label">已修课程</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ averageProgress }}%</div>
              <div class="stat-label">平均进度</div>
            </div>
          </div>

          <div class="profile-info">
            <div class="info-item">
              <span class="info-label">学号</span>
              <span class="info-value">20250001</span>
            </div>
            <div class="info-item">
              <span class="info-label">学院</span>
              <span class="info-value">计算机科学与工程学院</span>
            </div>
            <div class="info-item">
              <span class="info-label">年级</span>
              <span class="info-value">2025级</span>
            </div>
            <div class="info-item">
              <span class="info-label">手机</span>
              <span class="info-value">138****1234</span>
            </div>
          </div>
        </div>
      </div>

      <div class="profile-middle">
        <div class="stat-card top-card">
          <div class="stat-card-header">
            <h3 class="stat-card-title">学习进度</h3>
            <div class="header-actions">
              <button
                v-if="canToggle"
                class="expand-btn"
                @click="toggleExpanded"
              >{{ toggleText }}</button>
              <div class="stat-card-icon">
                <i class="fas fa-chart-line"></i>
              </div>
            </div>
          </div>

          <div v-if="displayedCourses.length === 0" class="empty-state">
            <div class="empty-text">暂无任何学习进度</div>
          </div>
          <div
            v-for="course in displayedCourses"
            :key="course.id"
            class="progress-container"
          >
            <div class="progress-header">
              <span class="progress-label">{{ course.title }}</span>
              <span class="progress-value">{{ course.progress }}%</span>
            </div>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: course.progress + '%' }"></div>
            </div>
          </div>
        </div>

        <div class="stat-card bottom-card">
          <div class="stat-card-header">
            <h3 class="stat-card-title">最近学习</h3>
            <div class="header-actions">

              <div class="stat-card-icon">
                <i class="fas fa-history"></i>
              </div>
            </div>
          </div>

          <ul class="course-list">
            <li class="course-item" v-for="(rec, index) in recentHistory"
              :key="`${rec.id}-${index}`">
              <span class="course-name">{{ rec.title }}</span>
              <span class="course-progress">{{ relativeTime(rec.watchedAt) }}</span>
            </li>
          </ul>
        </div>
      </div>

      <div class="profile-right">
        <div class="stat-card top-card">
          <div class="stat-card-header">
            <h3 class="stat-card-title">我的课程</h3>
            <div class="stat-card-icon">
              <i class="fas fa-book"></i>
            </div>
          </div>

          <div class="progress-container">
            <div class="progress-header">
              <span class="progress-label">已完成课程</span>
              <span class="progress-value">{{ completedCount }}/{{ totalCoursesCount }}</span>
            </div>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: completedPercent + '%' }"></div>
            </div>
          </div>

          <div class="progress-container">
            <div class="progress-header">
              <span class="progress-label">视频</span>
              <span class="progress-value">{{ videoCount }}</span>
            </div>
          </div>

          <div class="progress-container">
            <div class="progress-header">
              <span class="progress-label">文档</span>
              <span class="progress-value">{{ fileCount }}</span>
            </div>
          </div>
        </div>

        <div class="stat-card bottom-card">
          <div class="stat-card-header">
            <h3 class="stat-card-title">学习时间</h3>
            <div class="stat-card-icon">
              <i class="fas fa-clock"></i>
            </div>
          </div>

          <div class="progress-container">
            <div class="progress-header">
              <span class="progress-label">累计学习（视频+文档）</span>
              <span class="progress-value">{{ totalHours }}</span>
            </div>
          </div>
          <div class="progress-container">
            <div class="progress-header">
              <span class="progress-label">视频课程</span>
              <span class="progress-value">{{ videoHours }}</span>
            </div>
          </div>
          <div class="progress-container">
            <div class="progress-header">
              <span class="progress-label">文档课程</span>
              <span class="progress-value">{{ docHours }}</span>
            </div>
          </div>

        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { getOverallProgress, getAllCoursesSummary } from '../../services/student/learningProgress.js'
import { fetchUserCourses } from '../../services/student/coursesApi.js'
import { getHistory, onHistoryChanged, clearHistory as clearHistoryService } from '../../services/student/historyService.js'

const isExpanded = ref(false)
const myCourses = ref([])

function refreshProgress() {
  myCourses.value = myCourses.value.map(c => {
    const p = getOverallProgress(c.id)
    if (typeof p === 'number' && p > 0) {
      return { ...c, progress: Math.round(p * 100) }
    }
    return c
  })
}

const displayedCourses = computed(() => {
  return isExpanded.value ? myCourses.value : myCourses.value.slice(0, 3)
})

const averageProgress = computed(() => {
  if (myCourses.value.length === 0) return 0
  const sum = myCourses.value.reduce((acc, c) => acc + (c.progress || 0), 0)
  return Math.round(sum / myCourses.value.length)
})

const totalCoursesCount = computed(() => myCourses.value.length)
const videoCount = computed(() => myCourses.value.filter(c => c.type === 'video').length)
const fileCount = computed(() => myCourses.value.filter(c => c.type !== 'video').length)
const completedCount = computed(() => myCourses.value.filter(c => (c.progress || 0) >= 100).length)
const completedPercent = computed(() => {
  if (totalCoursesCount.value === 0) return 0
  return Math.round((completedCount.value / totalCoursesCount.value) * 100)
})

const canToggle = computed(() => myCourses.value.length > 3)
const toggleText = computed(() => (isExpanded.value ? '收起' : '展开全部'))
const toggleExpanded = () => {
  isExpanded.value = !isExpanded.value
}

function onProgressEvent() {
  refreshProgress()
}

onMounted(() => {
  refreshProgress()
  window.addEventListener('learning-progress-updated', onProgressEvent)
  loadCoursesFromApi()
  recentHistory.value = getUniqueHistory().slice(0, 3)
  unlisten = onHistoryChanged(() => {
    recentHistory.value = getUniqueHistory().slice(0, 3)
    calcTotalTime()
  })
  calcTotalTime()
})

onBeforeUnmount(() => {
  window.removeEventListener('learning-progress-updated', onProgressEvent)
  if (unlisten) unlisten()
})

async function loadCoursesFromApi() {
  try {
    const data = await fetchUserCourses('20250001')
    // 期望结构：[{ id, title, description, category, materials: [{ id, type: 'video'|'document'|'static', progress }] }]
    const mapped = (data || []).map(c => {
      const hasVideo = (c.materials || []).some(m => m.type === 'video')
      const localOverall = getOverallProgress(c.id)
      let progress = 0
      if (typeof c.progress === 'number') progress = Math.round(c.progress)
      else if (typeof localOverall === 'number' && localOverall > 0) progress = Math.round(localOverall * 100)
      else if ((c.materials || []).length) {
        const vals = (c.materials || []).map(m => Math.max(0, Math.min(100, m.progress || 0)))
        progress = Math.round(vals.reduce((a, b) => a + b, 0) / vals.length)
      }
      return { id: c.id, title: c.title, type: hasVideo ? 'video' : 'static', progress }
    })
    // 合并本地所有课程记录（即使接口未返回，也能显示最近学习记录）
    const local = getAllCoursesSummary()
    const localOnlyIds = local.filter(l => !mapped.some(m => m.id === l.courseId)).map(l => l.courseId)
    const localAsCourses = localOnlyIds.map(id => ({ id, title: `课程 ${id}`, type: 'video', progress: Math.round(getOverallProgress(id) * 100) }))
    myCourses.value = [...mapped, ...localAsCourses]
  } catch (e) {
  }
}

const recentHistory = ref([])
let unlisten = null

function getUniqueHistory() {
  return getHistory()
}

function relativeTime(dateStr) {
  const d = new Date(dateStr)
  const diff = Math.floor((Date.now() - d.getTime()) / 1000)
  if (diff < 60) return '刚刚'
  const mins = Math.floor(diff / 60)
  if (mins < 60) return `${mins}分钟前`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days}天前`
  const months = Math.floor(days / 30)
  if (months < 12) return `${months}个月前`
  // const years = Math.floor(months / 12)
  // return `${years}年前`      目前想法是只记录展示一个学期的历史
}

const totalHours = computed(() => {
  const secs = totalSeconds.value
  const hours = (secs / 3600).toFixed(1)
  return `${hours}小时`
})
const totalSeconds = ref(0)
function calcTotalTime() {
  const list = getHistory()
  totalSeconds.value = list.reduce((acc, r) => acc + (r.timeSpent || 0), 0)
  videoSeconds.value = list.filter(r => r.type === 'video').reduce((a, b) => a + (b.timeSpent || 0), 0)
  docSeconds.value = list.filter(r => r.type !== 'video').reduce((a, b) => a + (b.timeSpent || 0), 0)
}

const videoSeconds = ref(0)
const docSeconds = ref(0)
const videoHours = computed(() => `${(videoSeconds.value / 3600).toFixed(1)}小时`)
const docHours = computed(() => `${(docSeconds.value / 3600).toFixed(1)}小时`)
</script>

<style scoped>
.profile-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 30px;
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

.profile-container {
  display: grid;
  grid-template-columns: 0.95fr 1.2fr 0.95fr;
  gap: 20px;
  align-items: start;
}

.profile-middle, .profile-left, .profile-right {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-card {
  background: white;
  border-radius: 10px;
  padding: 25px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);
  min-height: 560px;
  margin-bottom: 0;
}

.profile-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-bottom: 25px;
}

.profile-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(45deg, #3498db, #2ecc71);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: white;
  margin: 0 auto 15px;
}

.profile-name {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 5px;
  text-align: center;
}

.profile-role {
  color: #64748b;
  font-size: 14px;
  text-align: center;
}

.profile-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
  margin-bottom: 25px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 15px;
  background: #f8fafc;
  border-radius: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #2563eb;
  margin-bottom: 5px;
}

.stat-label {
  color: #64748b;
  font-size: 13px;
}

.profile-info {
  margin-bottom: 25px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
}

.info-item:last-child {
  border-bottom: none;
}

.info-label {
  color: #64748b;
}

.info-value {
  font-weight: 500;
}

.profile-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.action-btn {
  padding: 12px;
  background: #f1f5f9;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 10px;
  text-align: left;
}

.action-btn:hover {
  background: #e2e8f0;
}

.action-btn i {
  width: 20px;
  color: #64748b;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);
}

.stat-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 13px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-card-title {
  font-weight: 600;
  color: #2c3e50;
}

.stat-card-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #e0f2fe;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0ea5e9;
}

.profile-right .progress-container {
  margin-bottom: 20px;
}

.profile-right .progress-header {
  margin-bottom: 12px;
  line-height: 1.8;
}

.expand-btn {
  padding: 6px 10px;
  border: none;
  background-color: #2563eb;
  color: #fff;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
}

.expand-btn:hover {
  background-color: #1d4ed8;
}

.clear-btn {
  background: #ff4757;
  color: white;
  border: none;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.3s ease;
}

.clear-btn:hover {
  background: #ff3742;
}

.top-card {
  min-height: 280px;
}

.bottom-card {
  min-height: 260px;
}

.progress-container {
  margin-bottom: 15px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.progress-label {
  color: #64748b;
  font-size: 14px;
}

.progress-value {
  color: #2c3e50;
  font-weight: 600;
}

.progress-bar {
  height: 8px;
  background: #e2e8f0;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #10b981;
  border-radius: 4px;
}

.course-list {
  list-style: none;
}

.course-item {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
}

.course-item:last-child {
  border-bottom: none;
}

.course-name {
  color: #2c3e50;
}

.course-progress {
  color: #64748b;
  font-size: 14px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #94a3b8;
}

.empty-text {
  font-size: 16px;
  text-align: center;
}

@media (max-width: 992px) {
  .profile-container {
    grid-template-columns: 1fr;
  }
  .stats-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .profile-page {
    padding: 20px;
  }
}
</style>
