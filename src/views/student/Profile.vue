<template>
  <div class="header">
    <h1 class="page-title">个人中心</h1>
    <div class="welcome">欢迎回来，{{ student.name || '同学' }}！</div>
  </div>
  <div class="profile-page">
    <div class="profile-container">
      <div class="profile-left">
        <div class="profile-card">
          <div class="profile-header">
            <div class="profile-avatar">{{ avatarText }}</div>
            <h2 class="profile-name">{{ student.name || '未命名' }}</h2>
            <div class="profile-role">{{ student.major || '未填写专业' }} ·
              {{ student.grade || student.enrollmentYear || '年级未填' }}</div>
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
              <span class="info-value">{{ student.studentNumber || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">班级</span>
              <span class="info-value">{{ student.className || student.class || student.clazzName || student.classroom || '-' }}</span>
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
            <h3 class="stat-card-title">最近作品提交</h3>
            <div class="header-actions">
              <div class="stat-card-icon">
                <i class="fas fa-upload"></i>
              </div>
            </div>
          </div>

          <ul class="course-list">
            <li class="course-item" v-for="(rec, index) in recentSubmissions" :key="rec.id || index">
              <span class="course-name">{{ rec.title }}</span>
            </li>
            <li v-if="recentSubmissions.length === 0" class="course-item">
              <span class="course-name">暂无作品提交</span>
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
              <span class="progress-label">作品提交截至时间</span>
              <span class="progress-value">{{ workDeadline }}</span>
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
import { getOverallProgress } from '@/services/progressApi.js'
import { fetchHomeCourses } from '@/services/homeCoursesApi.js'

const isExpanded = ref(false)
const student = ref({})
const avatarText = computed(() => (student.value.name ? student.value.name[student.value.name.length-1] : '学'))
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
  loadStudent()
  recentSubmissions.value = getRecentWorks()
  try { window.addEventListener('work-submitted', () => { recentSubmissions.value = getRecentWorks() }) } catch (e) { console.error(e) }
  try { window.addEventListener('work-deadline-updated', loadDeadline) } catch (e) { console.error(e) }
  loadDeadline()
  calcTotalTime()
})

onBeforeUnmount(() => {
  window.removeEventListener('learning-progress-updated', onProgressEvent)
  if (unlisten) unlisten()
})

async function loadCoursesFromApi() {
  try {
    const data = await fetchHomeCourses()
    myCourses.value = (data || []).map(c => {
      const localOverall = getOverallProgress(c.id)
      const progress = typeof c.progress === 'number'
          ? Math.round(c.progress)
          : (typeof localOverall === 'number' && localOverall > 0
              ? Math.round(localOverall * 100)
              : 0)
      return { ...c, progress }
    })
  } catch (e) {
  }
}

function loadStudent() {
  try {
    const saved = JSON.parse(localStorage.getItem('currentUser') || 'null') || {}
    student.value = saved && Object.keys(saved).length ? saved : {}
  } catch (e) {
    console.error(e)
    student.value = {}
  }
}

const recentSubmissions = ref([])
let unlisten = null

function getRecentWorks() {
  try {
    const raw = localStorage.getItem('work_submissions_v1') || '[]'
    const arr = JSON.parse(raw)
    if (Array.isArray(arr)) return arr.slice(0, 3)
  } catch {}
  return []
}
computed(() => {
  const secs = totalSeconds.value
  const hours = (secs / 3600).toFixed(1)
  return `${hours}小时`
})
const totalSeconds = ref(0)
function calcTotalTime() {
  totalSeconds.value = 0
  videoSeconds.value = 0
  docSeconds.value = 0
}

const videoSeconds = ref(0)
const docSeconds = ref(0)
const videoHours = computed(() => `${(videoSeconds.value / 3600).toFixed(1)}小时`)
const docHours = computed(() => `${(docSeconds.value / 3600).toFixed(1)}小时`)

// 作品截止时间
const workDeadline = ref('未设置')
function loadDeadline() {
  try {
    const v = localStorage.getItem('work_deadline_v1') || ''
    workDeadline.value = v ? v.replace('T', ' ') : '未设置'
  } catch { workDeadline.value = '未设置' }
}
</script>

<style scoped>
.profile-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 10px;
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

.action-btn i {
  width: 20px;
  color: #64748b;
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
}

@media (max-width: 768px) {
  .profile-page {
    padding: 20px;
  }
}
</style>
