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
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.78), rgba(15, 23, 42, 0.94));
}

.ideology-panel.compact {
  padding: 14px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.panel-eyebrow {
  margin: 0 0 4px;
  color: #fbbf24;
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.panel-head h3 {
  margin: 0;
  color: #fff;
  font-size: 18px;
}

.refresh-btn {
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 10px;
  color: #f8fafc;
  background: rgba(255, 255, 255, 0.08);
  cursor: pointer;
}

.spinning {
  animation: spin 0.8s linear infinite;
}

.panel-state {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 0;
  color: rgba(226, 232, 240, 0.72);
  font-size: 13px;
}

.panel-state.empty {
  color: rgba(148, 163, 184, 0.86);
}

.panel-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.resource-tile {
  padding: 14px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.05);
}

.resource-tile.featured {
  border-color: rgba(251, 191, 36, 0.35);
  background: linear-gradient(180deg, rgba(251, 191, 36, 0.1), rgba(255, 255, 255, 0.05));
}

.tile-top,
.meta-row,
.keyword-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.type-chip,
.reason-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 11px;
}

.type-chip {
  color: #fde68a;
  background: rgba(245, 158, 11, 0.14);
}

.reason-chip {
  color: #bfdbfe;
  background: rgba(37, 99, 235, 0.18);
}

.resource-tile h4 {
  margin: 10px 0 8px;
  color: #fff;
  font-size: 15px;
  line-height: 1.45;
}

.resource-tile p {
  margin: 0;
  color: rgba(226, 232, 240, 0.78);
  font-size: 13px;
  line-height: 1.7;
}

.meta-row {
  margin-top: 10px;
}

.meta-row span {
  color: #cbd5e1;
  font-size: 12px;
}

.meta-row i {
  margin-right: 5px;
  color: #fbbf24;
}

.keyword-row {
  margin-top: 10px;
}

.keyword-row span {
  color: #93c5fd;
  font-size: 12px;
}

.open-btn {
  margin-top: 12px;
  width: 100%;
  height: 34px;
  border: 0;
  border-radius: 12px;
  color: #0f172a;
  background: linear-gradient(135deg, #fde68a, #f59e0b);
  cursor: pointer;
  font-weight: 700;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
