<template>
  <section class="ideology-panel" :class="{ compact }">
    <div class="panel-head">
      <div>
        <p class="panel-eyebrow">Ideology Picks</p>
        <h3>思政资源</h3>
      </div>
      <button class="refresh-btn" @click="loadResources" :disabled="loading">
        <i class="fas fa-rotate-right" :class="{ spinning: loading }"></i>
      </button>
    </div>

    <div v-if="loading" class="panel-state">
      <i class="fas fa-spinner fa-spin"></i>
      <span>加载资源中...</span>
    </div>

    <div v-else-if="cards.length === 0" class="panel-state empty">
      <i class="fas fa-seedling"></i>
      <span>当前课程暂无思政资源</span>
    </div>

    <div v-else class="panel-list">
      <article v-for="card in cards" :key="card.key" class="resource-tile" :class="{ featured: card.featured }">
        <div class="tile-top">
          <span class="type-chip">{{ typeText(card.resourceType) }}</span>
          <span v-if="card.reason" class="reason-chip">{{ card.reason }}</span>
        </div>
        <h4>{{ card.title }}</h4>
        <p>{{ card.contentSummary || '暂无摘要，建议查看原始资源。' }}</p>
        <div class="meta-row">
          <span v-if="card.valueTheme"><i class="fas fa-bookmark"></i>{{ card.valueTheme }}</span>
          <span v-if="card.applicableScene"><i class="fas fa-location-dot"></i>{{ card.applicableScene }}</span>
        </div>
        <div class="keyword-row">
          <span v-for="keyword in splitKeywords(card.keywords)" :key="keyword">{{ keyword }}</span>
        </div>
        <button v-if="card.sourceUrl" class="open-btn" @click="openResource(card.sourceUrl)">
          查看资源
        </button>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { getIdeologyRecommendations, searchIdeologyResources } from '@/services/ideologyApi'

const props = defineProps({
  courseId: { type: [String, Number], default: null },
  videoId: { type: [String, Number], default: null },
  documentId: { type: [String, Number], default: null },
  compact: { type: Boolean, default: false }
})

const loading = ref(false)
const recommendations = ref([])
const resources = ref([])
const userRole = computed(() => localStorage.getItem('userRole'))
const studentId = computed(() => localStorage.getItem('userId') || localStorage.getItem('studentId'))

const cards = computed(() => {
  const base = userRole.value === 'student'
    ? recommendations.value.map(item => ({
        key: `rec-${item.id}`,
        resourceId: item.resource?.resourceId,
        title: item.resource?.title,
        contentSummary: item.resource?.contentSummary,
        resourceType: item.resource?.resourceType,
        valueTheme: item.resource?.valueTheme,
        applicableScene: item.resource?.applicableScene,
        keywords: item.resource?.keywords,
        sourceUrl: item.resource?.sourceUrl,
        videoId: item.resource?.videoId,
        documentId: item.resource?.documentId,
        reason: item.reason,
        featured: false
      }))
    : resources.value.map(item => ({
        key: `res-${item.resourceId}`,
        ...item,
        reason: '',
        featured: false
      }))

  return prioritize(base).slice(0, props.compact ? 4 : 6)
})

const prioritize = (items) => {
  const exact = []
  const related = []
  const seen = new Set()
  items.forEach(item => {
    if (!item || seen.has(item.key)) return
    seen.add(item.key)
    const videoMatch = props.videoId && String(item.videoId) === String(props.videoId)
    const documentMatch = props.documentId && String(item.documentId) === String(props.documentId)
    if (videoMatch || documentMatch) {
      exact.push({ ...item, featured: true })
    } else {
      related.push(item)
    }
  })
  return [...exact, ...related]
}

const loadResources = async () => {
  if (!props.courseId) return
  loading.value = true
  try {
    if (userRole.value === 'student' && studentId.value) {
      const res = await getIdeologyRecommendations({
        studentId: studentId.value,
        courseId: props.courseId,
        limit: 10
      })
      recommendations.value = res.data?.data || []
    } else {
      const res = await searchIdeologyResources({
        courseId: props.courseId,
        status: 'published',
        limit: 10
      })
      resources.value = res.data?.data || []
    }
  } catch (error) {
    console.error('加载思政资源区块失败:', error)
    recommendations.value = []
    resources.value = []
  } finally {
    loading.value = false
  }
}

const splitKeywords = (keywords) => {
  if (!keywords) return []
  return String(keywords).split(/[,，;；\s]+/).filter(Boolean).slice(0, 4)
}

const openResource = (url) => {
  if (!url) return
  window.open(url, '_blank', 'noopener')
}

const typeText = (type) => ({
  case: '案例',
  policy: '政策',
  video: '视频',
  document: '文档',
  activity: '活动'
}[type] || '资源')

watch(
  () => [props.courseId, props.videoId, props.documentId],
  () => {
    loadResources()
  },
  { immediate: true }
)
</script>

<style scoped>
.ideology-panel {
  padding: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  background: #0F172A;
  color: #F8FAFC;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

.ideology-panel.compact {
  padding: 16px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.panel-eyebrow {
  margin: 0 0 6px;
  color: #38BDF8;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.panel-head h3 {
  margin: 0;
  color: #FFFFFF;
  font-size: 18px;
  font-weight: 600;
}

.refresh-btn {
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 8px;
  color: #94A3B8;
  background: rgba(255, 255, 255, 0.05);
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.refresh-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.1);
  color: #F8FAFC;
}

.spinning {
  animation: spin 1s linear infinite;
}

.panel-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 32px 0;
  color: #94A3B8;
  font-size: 14px;
}

.panel-state i {
  font-size: 24px;
  color: #475569;
}

.panel-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.resource-tile {
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  background: rgba(30, 41, 59, 0.5);
  transition: all 0.3s ease;
}

.resource-tile:hover {
  background: rgba(30, 41, 59, 0.8);
  border-color: rgba(56, 189, 248, 0.3);
}

.resource-tile.featured {
  border-color: rgba(56, 189, 248, 0.4);
  background: linear-gradient(180deg, rgba(14, 165, 233, 0.1), rgba(30, 41, 59, 0.6));
}

.tile-top {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.type-chip,
.reason-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
}

.type-chip {
  color: #BAE6FD;
  background: rgba(2, 132, 199, 0.2);
  border: 1px solid rgba(2, 132, 199, 0.3);
}

.reason-chip {
  color: #FDE68A;
  background: rgba(217, 119, 6, 0.15);
  border: 1px solid rgba(217, 119, 6, 0.2);
}

.resource-tile h4 {
  margin: 0 0 8px;
  color: #F8FAFC;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
}

.resource-tile p {
  margin: 0 0 12px 0;
  color: #94A3B8;
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}

.meta-row span {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #CBD5E1;
  font-size: 12px;
}

.meta-row i {
  color: #38BDF8;
}

.keyword-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 16px;
}

.keyword-row span {
  color: #7DD3FC;
  font-size: 12px;
  background: rgba(14, 165, 233, 0.1);
  padding: 2px 8px;
  border-radius: 4px;
}

.open-btn {
  width: 100%;
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: #F8FAFC;
  background: rgba(255, 255, 255, 0.05);
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.open-btn:hover {
  background: #0284C7;
  border-color: #0284C7;
  color: #FFFFFF;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
