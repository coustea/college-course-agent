<template>
  <div class="history-page">
    <div class="header">
      <h1 class="page-title">历史记录</h1>
      <button class="clear-history-btn" :disabled="history.length === 0" @click="clearHistory">
        <i class="fas fa-trash"></i>
        清空历史记录
      </button>
    </div>

    <div class="search-area">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索你的历史记录"
        />
      </div>
      <div class="filter-controls">
        <span class="filter-label">筛选:</span>
        <select v-model="filterType" class="filter-select">
          <option value="all">全部类型</option>
          <option value="static">文件课程</option>
          <option value="video">视频课程</option>
        </select>
        <select v-model="sortOrder" class="filter-select">
          <option value="newest">最近学习</option>
          <option value="oldest">最早学习</option>
        </select>
      </div>
    </div>

    <div class="courses-container">
      <template v-if="displayedRecords.length > 0">
        <div
          class="course-card"
          v-for="record in displayedRecords"
          :key="record.id"
          @click.stop.prevent="handleCardClick(record)"
        >
          <div class="course-image">
            <img :src="record.image" :alt="record.title" />
            <div class="course-type-badge" :class="record.type === 'video' ? 'video' : 'file'">
              <i :class="record.type === 'video' ? 'fas fa-video' : 'fas fa-file-alt'"></i>
              {{ record.type === 'video' ? '视频课程' : '文档课程' }}
            </div>
            <span class="watched-time" :title="formatDate(record.watchedAt)">{{ relativeTime(record.watchedAt) }}</span>
          </div>
          <div class="course-content">
            <h3 class="course-title">{{ record.title }}</h3>
            <p class="course-description">{{ record.description }}</p>
            <div class="course-meta">
              <span>{{ formatDate(record.watchedAt) }}</span>
              <span>{{ record.duration }}</span>
            </div>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: (record.progress || 0) + '%' }"></div>
            </div>
            <div class="course-action">
              <span class="progress-indicator">已学习 {{ record.progress || 0 }}%</span>
            </div>
          </div>
        </div>
      </template>

      <div v-else class="no-results">
        <i class="fas fa-history"></i>
        <h3>暂无观看历史</h3>
        <p>您可以通过上方搜索或筛选快速定位想要回看的课程</p>
      </div>
    </div>

    <CoursePlayer
      v-if="playerVisible && activeCourse"
      v-model="playerVisible"
      :course-id="activeCourse.id"
      :title="activeCourse.title"
      :chapters="activeChapters"
      :start-index="startIndex"
      :fallback-src="fallbackSrcMap[activeCourse.id] || ''"
    />

    <DocumentViewer
      v-model="docVisible"
      :id="activeDoc?.id"
      :title="activeDoc?.title || '文档课程'"
      :file-url="activeDoc?.url || ''"
      :html-content="activeDoc?.html || ''"
      :progress="activeDoc?.progress || 0"
      :image="activeDoc?.image || ''"
      :duration="activeDoc?.duration || ''"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import CoursePlayer from '../../components/CoursePlayer.vue'
import DocumentViewer from '../../components/DocumentViewer.vue'
import { getOverallProgress } from '../../services/student/learningProgress.js'
import { getHistory, clearHistory as hsClear, onHistoryChanged } from '../../services/student/historyService.js'

const searchQuery = ref('')
const filterType = ref('all')
const sortOrder = ref('newest')

// 示例课程（真实项目可改由接口获取）
const allCourses = ref([
  { id: 1, title: '思想道德修养与法律基础', description: '本课程主要讲解马克思主义基本原理和中国特色社会主义理论体系。', type: 'static', image: 'https://images.unsplash.com/photo-1501504905252-473c47e087f8?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=800&q=80', date: '2023-09-15', duration: '12章', required: true },
  { id: 2, title: '中国近现代史纲要', description: '全面介绍中国从1840年至今的历史发展进程和重大历史事件。', type: 'video', image: 'https://images.unsplash.com/photo-1532094349884-543bc11b234d?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=800&q=80', date: '2023-09-10', duration: '8章', required: true },
  { id: 3, title: '马克思主义基本原理', description: '深入学习马克思主义哲学、政治经济学和科学社会主义的基本原理。', type: 'static', image: 'https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=800&q=80', date: '2023-09-05', duration: '10章', required: true },
  { id: 4, title: '毛泽东思想和中国特色社会主义理论体系概论', description: '系统学习毛泽东思想和中国特色社会主义理论体系的形成与发展。', type: 'video', image: 'https://images.unsplash.com/photo-1562813733-b31f71025d54?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=800&q=80', date: '2023-09-01', duration: '12章', required: false },
  { id: 5, title: '形势与政策', description: '分析当前国际国内形势，解读党和国家重大方针政策。', type: 'static', image: 'https://images.unsplash.com/photo-1450101499163-c8848c66ca85?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=800&q=80', date: '2023-08-28', duration: '6章', required: true },
  { id: 6, title: '习近平新时代中国特色社会主义思想', description: '深入学习习近平新时代中国特色社会主义思想的核心要义和实践要求。', type: 'video', image: 'https://images.unsplash.com/photo-1589652717521-10c0d092dea9?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=800&q=80', date: '2023-08-25', duration: '8章', required: false }
])

const history = ref([])
onMounted(() => { history.value = getHistory() })
onHistoryChanged(() => { history.value = getHistory() })

const joined = computed(() => {
  return history.value
    .map(h => {
      const c = allCourses.value.find(cc => cc.id === h.id)
      if (!c) return null
      const base = { ...c, watchedAt: h.watchedAt }
      if (c.type === 'video') {
        const p = Math.round((getOverallProgress(c.id) || 0) * 100)
        return { ...base, progress: p }
      }
      return { ...base, progress: 100 }
    })
    .filter(Boolean)
})

const displayedRecords = computed(() => {
  let list = [...joined.value]
  if (filterType.value !== 'all') {
    list = list.filter(i => i.type === filterType.value)
  }
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.trim().toLowerCase()
    list = list.filter(i => i.title.toLowerCase().includes(q) || i.description.toLowerCase().includes(q))
  }
  list.sort((a, b) => {
    const da = new Date(a.watchedAt)
    const db = new Date(b.watchedAt)
    return sortOrder.value === 'newest' ? db - da : da - db
  })
  return list
})

function formatDate(dateStr) {
  const d = new Date(dateStr)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${hh}:${mm}`
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
  const years = Math.floor(months / 12)
  return `${years}年前`
}

function clearHistory() {
  if (history.value.length === 0) return
  if (confirm('确定要清空所有观看历史记录吗？此操作不可撤销')) {
    hsClear()
    history.value = []
  }
}

// 播放器/文档查看器集成
const playerVisible = ref(false)
const activeCourse = ref(null)
const activeChapters = ref([])
const startIndex = ref(0)

function openRecord(record) {
  activeCourse.value = { id: record.id, title: record.title }
  activeChapters.value = chaptersMap[record.id] || []
  startIndex.value = 0
  playerVisible.value = true
}

const docVisible = ref(false)
const activeDoc = ref(null)
function openDoc(record) {
  activeDoc.value = record
  docVisible.value = true
}

function handleCardClick(record) {
  if (record && record.type === 'video') {
    openRecord(record)
  } else {
    openDoc(record)
  }
}

// 示例视频章节和备用视频
const chaptersMap = {
  2: [
    { title: '第一章：近代开端', duration: '15:00', videoUrl: 'https://example.com/video4.mp4' },
    { title: '第二章：辛亥革命', duration: '15:00', videoUrl: 'https://example.com/video5.mp4' },
    { title: '第三章：新民主主义', duration: '15:00', videoUrl: 'https://example.com/video6.mp4' }
  ],
  4: []
}
const fallbackSrcMap = { 4: 'https://example.com/long-video.mp4' }
</script>

<style scoped>
.history-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 30px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-title {
  font-weight: bold;
  font-size: 24px;
  color: #2c3e50;
}

.clear-history-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  background: #ef4444;
  color: #fff;
  cursor: pointer;
  transition: all 0.3s;
}

.clear-history-btn:hover {
  background: #dc2626;
}

.clear-history-btn:disabled {
  background: #94a3b8;
  cursor: not-allowed;
}

.search-area {
  margin-bottom: 20px;
}

.search-box {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  padding: 10px 15px;
  border-radius: 8px;
  background: #e0e7ff;
}

.search-box i {
  margin-right: 10px;
  color: #2563eb;
}

.search-box input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 16px;
}

.filter-controls {
  display: flex;
  align-items: center;
  gap: 15px;
}

.filter-label {
  font-size: 14px;
  color: #64748b;
}

.filter-select {
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #fff;
  color: #334155;
}

.courses-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.course-card {
  position: relative;
  overflow: hidden;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s;
}

.course-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

.course-image {
  position: relative;
  overflow: hidden;
  height: 160px;
}

.course-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  background: linear-gradient(120deg, #e0e7ff, #c7d2fe);
  transition: transform 0.5s;
}

.course-card:hover .course-image img {
  transform: scale(1.05);
}

.course-type-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
}

.course-type-badge.video {
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
}

.course-type-badge.file {
  background: linear-gradient(135deg, #4ecdc4, #44bd87);
}

.watched-time {
  position: absolute;
  bottom: 10px;
  left: 10px;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #fff;
  background: rgba(0, 0, 0, 0.7);
}

.course-content {
  padding: 20px;
}

.course-title {
  margin-bottom: 10px;
  font-weight: 600;
  font-size: 18px;
  color: #2c3e50;
}

.course-description {
  margin-bottom: 15px;
  font-size: 14px;
  line-height: 1.5;
  color: #64748b;
}

.course-meta {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
  font-size: 13px;
  color: #94a3b8;
}

.course-action {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.watch-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  background: #2563eb;
  color: #fff;
  cursor: pointer;
  transition: all 0.3s;
}

.watch-btn:hover {
  background: #1d4ed8;
}

.watch-btn.watched {
  background: #10b981;
}

.progress-indicator {
  font-size: 13px;
  color: #64748b;
}

.progress-bar {
  height: 6px;
  margin-top: 10px;
  overflow: hidden;
  border-radius: 3px;
  background: #e2e8f0;
}

.progress-fill {
  height: 100%;
  border-radius: 3px;
  background: #10b981;
  transition: width 0.3s;
}

.no-results {
  grid-column: 1 / -1;
  padding: 60px 40px;
  text-align: center;
  color: #94a3b8;
}

.no-results i {
  margin-bottom: 20px;
  font-size: 64px;
  opacity: 0.5;
}

.no-results h3 {
  margin-bottom: 10px;
  font-size: 20px;
  color: #64748b;
}

.no-results p {
  margin-bottom: 20px;
  font-size: 15px;
}

@media (max-width: 992px) {
  .courses-container {
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  }
}

@media (max-width: 768px) {
  .history-page {
    padding: 20px;
  }

  .filter-controls {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 576px) {
  .courses-container {
    grid-template-columns: 1fr;
  }
}
</style>

