<template>
  <div v-if="modelValue" class="dv-mask" @click.self="close" @wheel="forwardBackgroundScroll">
    <div class="dv-modal" ref="modalRef">
      <div class="dv-header">
        <div class="dv-title">{{ headerTitle }}</div>
        <div class="dv-tools">
          <button v-if="!isIframe" class="dv-tool" @click="zoomOut" title="缩小"><i class="fas fa-search-minus"></i></button>
          <button v-if="!isIframe" class="dv-tool" @click="zoomIn" title="放大"><i class="fas fa-search-plus"></i></button>
          <button v-if="!isIframe" class="dv-tool" @click="decreaseText" title="减小文字">A-</button>
          <button v-if="!isIframe" class="dv-tool" @click="increaseText" title="增大文字">A+</button>
          <button class="dv-tool" @click="toggleFullscreen" title="全屏"><i class="fas fa-expand"></i></button>
        </div>
        <button class="dv-close" @click="close"><i class="fas fa-times"></i></button>
      </div>
      <div class="dv-body" ref="bodyRef">
        <div class="dv-main">
          <aside v-if="!isFullscreen && flatChapters.length" class="dv-aside">
            <div class="dv-aside-title">课程目录</div>
            <div class="dv-aside-list">
              <div
                  v-for="(ch, i) in flatChapters"
                  :key="i"
                  class="dv-aside-item"
                  :class="{ active: i === currentIndex }"
                  @click="selectChapter(i)"
              >
                <i class="fas fa-file-alt"></i>
                <span class="dv-aside-text">{{ ch.title || (`第${i + 1}章`) }}</span>
              </div>
            </div>
          </aside>
          <div class="dv-view">
            <template v-if="isIframe">
              <iframe class="dv-iframe" :src="viewerSrc" title="document" referrerpolicy="no-referrer" />
            </template>
            <template v-else>
              <div class="dv-content" :style="contentStyle" v-html="safeHtml"></div>
            </template>
          </div>
        </div>
      </div>
      <div class="dv-footer">
        <button class="dv-pill" @click="$emit('progressClick')">阅读进度：{{ progressDisplay }}%</button>
        <div class="dv-actions">
          <button class="dv-btn dv-btn-secondary" @click="onPrev">上一章</button>
          <button class="dv-btn" @click="onNext">下一章</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onBeforeUnmount, onMounted } from 'vue'
import { fetchQuestions, hasQuestionShown, markQuestionShown, submitExamAnswers } from '@/services/questionApi'


const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '预览' },
  fileUrl: { type: String, default: '' },
  htmlContent: { type: String, default: '' },
  progress: { type: Number, default: 0 },
  id: { type: [String, Number], default: null },
  image: { type: String, default: '' },
  duration: { type: String, default: '' },
  chapterIndex: { type: Number, default: 1 },
  courseTitle: { type: String, default: '' },
  chapters: { type: Array, default: () => [] }
})

const backendHost = (() => { try { const p = window?.location?.port; if (p === '4173' || p === '5173') return 'http://localhost:9999'; } catch {} return '' })()
function toUrl(u) {
  if (!u) return ''
  const s = String(u)
  if (/^(https?:|data:|blob:)/.test(s)) return s
  if (s.startsWith('/uploads/')) return (backendHost || '') + s
  return s
}

const emit = defineEmits(['update:modelValue', 'next', 'prev', 'progressClick'])

// 目录
const flatChapters = computed(() => {
  const list = []
  const walk = (nodes) => {
    if (!Array.isArray(nodes)) return
    for (const n of nodes) {
      if (!n) continue
      const title = n.title || n.name || n.docTitle || ''
      const fileUrl = n.docUrl || n.fileUrl || n.url || n.resourceUrl || ''
      const html = n.html || n.content || ''
      if (title || fileUrl || html) list.push({ title, fileUrl, html })
      if (Array.isArray(n.children)) walk(n.children)
    }
  }
  walk(props.chapters)
  if (list.length === 0) {
    list.push({
      title: props.title || '文档',
      fileUrl: props.fileUrl,
      html: props.htmlContent
    })
  }
  return list
})
const currentIndex = ref( Math.max(0, (props.chapterIndex || 1) - 1) )
watch(() => props.modelValue, (v) => { if (v) currentIndex.value = Math.max(0, (props.chapterIndex || 1) - 1) })
const currentChapter = computed(() => flatChapters.value[currentIndex.value] || { title: props.title, fileUrl: props.fileUrl, html: props.htmlContent })

const effectiveFileUrl = computed(() => currentChapter.value?.fileUrl || '')
const effectiveHtml = computed(() => currentChapter.value?.html || '')
const readProgress = ref(0)
const progressDisplay = computed(() => {
  const p = Math.round((readProgress.value || 0) * 100)
  return Number.isFinite(p) ? p : (props.progress || 0)
})
const safeHtml = computed(() => {
  if (effectiveHtml.value) return effectiveHtml.value
  if (!viewerSrc.value && normalizedFileUrl.value) {
    const name = (props.title || '文档')
    const href = normalizedFileUrl.value
    return `<div style="padding:16px;color:#666;">无法在线预览此文档（可能为内网/本地地址或格式不支持）。<a href="${href}" target="_blank" rel="noopener" style="color:#1a56db;">点击下载查看 (${name})</a></div>`
  }
  return '<p style="color:#666">暂无内容</p>'
})

const headerTitle = computed(() => {
  const course = props.courseTitle || props.title || '课程'
  const index = (currentIndex.value || 0) + 1
  const prefix = flatChapters.value.length > 1 ? `第${index}章` : ''
  return prefix ? `${course} · ${prefix}` : course
})

const normalizedFileUrl = computed(() => {
  const url = effectiveFileUrl.value || ''
  return toUrl(url)
})

const fileExt = computed(() => {
  const u = (normalizedFileUrl.value || '').split('?')[0]
  const idx = u.lastIndexOf('.')
  return idx >= 0 ? u.slice(idx + 1).toLowerCase() : ''
})

function isPrivateUrl(u) {
  try {
    const loc = new URL(u)
    const host = loc.hostname
    if (host === 'localhost' || host === '127.0.0.1') return true
    if (/^(10\.|192\.168\.|172\.(1[6-9]|2\d|3[0-1])\.)/.test(host)) return true
  } catch {}
  return false
}
const viewerSrc = computed(() => {
  const url = normalizedFileUrl.value
  if (!url) return ''
  const isOffice = ["doc","docx","ppt","pptx","xls","xlsx"].includes(fileExt.value)
  if (isOffice) {
    if (isPrivateUrl(url)) return ''
    return `https://view.officeapps.live.com/op/view.aspx?ui=en-US&src=${encodeURIComponent(url)}`
  }
  return url
})

const isIframe = computed(() => !!viewerSrc.value)

// 按阅读进度在 40% 与 80% 触发问题
async function maybeAskByProgress(pct) {
  try {
    if (questionVisible.value) return
    const courseId = props.id || props.title
    const baseKey = `document-${props.chapterIndex || 1}`
    const key40 = `${baseKey}-p40`
    const key80 = `${baseKey}-p80`
    const progress = (pct > 1) ? (pct / 100) : pct
    let targetKey = ''
    if (progress >= 0.4 && !hasQuestionShown(courseId, key40)) {
      targetKey = key40
    } else if (progress >= 0.8 && !hasQuestionShown(courseId, key80)) {
      targetKey = key80
    }
    if (targetKey) {
      markQuestionShown(courseId, targetKey)
      const qs = await fetchQuestions(courseId, targetKey)
      if (Array.isArray(qs) && qs.length) {
        pendingNodeKey.value = targetKey
        questionList.value = qs
        questionVisible.value = true
        return new Promise((resolve) => { resolver.value = resolve })
      }
    }
  } catch {}
}

async function close() {
  emit('update:modelValue', false)
}

async function onNext() {
  if (currentIndex.value < flatChapters.value.length - 1) currentIndex.value += 1
  emit('next')
}

const questionVisible = ref(false)
const questionList = ref([])
const pendingNodeKey = ref('')
const resolver = ref(null)

async function onQuestionSubmit(payload) {
  try {
    const courseId = props.id || props.title
    const nodeKey = pendingNodeKey.value || `document-${props.chapterIndex || 1}`
    const answers = payload?.answers || {}
    const questions = questionList.value || []
    await submitExamAnswers(courseId, nodeKey, questions, answers)
  } catch {}
  questionVisible.value = false
  if (typeof resolver.value === 'function') {
    try { resolver.value(true) } catch {}
    resolver.value = null
  }
}

let timer = null
let last = 0
watch(() => props.modelValue, (v) => {
  if (v) {
    last = Date.now()
    if (!timer) {
      timer = setInterval(() => {
        const now = Date.now()
        const delta = Math.floor((now - last) / 1000)
        if (delta > 0) { last = now }
      }, 1000)
    }
  } else {
    if (timer) {
      const now = Date.now()
      Math.floor((now - last) / 1000);
      clearInterval(timer); timer = null
    }
  }
})

// 监听阅读进度，按 40% / 80% 触发一次
watch(() => props.progress, (p) => {
  const val = Number(p)
  if (!Number.isFinite(val)) return
  maybeAskByProgress(val)
}, { immediate: true })

onBeforeUnmount(() => { if (timer) { clearInterval(timer); timer = null } })

const fontScale = ref(1)
const modalRef = ref(null)
const bodyRef = ref(null)

const contentStyle = computed(() => ({
  fontSize: `${Math.round(16 * fontScale.value)}px`,
  lineHeight: 1.6 * fontScale.value
}))

function zoomIn() { increaseText() }
function zoomOut() { decreaseText() }
function increaseText() { fontScale.value = Math.min(2, +(fontScale.value + 0.1).toFixed(2)) }
function decreaseText() { fontScale.value = Math.max(0.6, +(fontScale.value - 0.1).toFixed(2)) }

function toggleFullscreen() {
  const el = modalRef.value
  if (!el) return
  const d = document
  if (!d.fullscreenElement) el.requestFullscreen?.()
  else d.exitFullscreen?.()
}

function forwardBackgroundScroll(e) {
  try {
    const d = document
    if (d.fullscreenElement) return
    const sc = document.querySelector('.main-content') || document.scrollingElement || document.documentElement
    if (sc) {
      e.preventDefault()
      sc.scrollTop += e.deltaY
    }
  } catch {}
}

function onPrev() { if (currentIndex.value > 0) currentIndex.value -= 1; emit('prev') }

function selectChapter(i) {
  if (i >= 0 && i < flatChapters.value.length) currentIndex.value = i
}

// 全屏状态跟踪（Esc 退出也能更新）
const isFullscreen = ref(false)
function handleFullscreenChange() {
  try { isFullscreen.value = !!document.fullscreenElement } catch { isFullscreen.value = false }
}
onMounted(() => {
  try { document.addEventListener('fullscreenchange', handleFullscreenChange) } catch {}
})
onBeforeUnmount(() => { try { document.removeEventListener('fullscreenchange', handleFullscreenChange) } catch {} })

// 本地滚动进度计算（非 iframe 文档有效）
function updateReadProgress() {
  const el = bodyRef.value
  if (!el) return
  const total = Math.max(1, el.scrollHeight - el.clientHeight)
  const ratio = Math.max(0, Math.min(1, el.scrollTop / total))
  readProgress.value = ratio
}

// Ctrl + 滚轮：仅对 HTML 内容调整字号
function handleWheel(e) {
  // Ctrl + 滚轮：缩放（仅 HTML）
  if (e.ctrlKey && !isIframe.value) {
    e.preventDefault()
    e.deltaY > 0 ? decreaseText() : increaseText()
    return
  }
  // 普通滚轮：在 iframe(PDF/Office) 无法读取内部滚动时，合成阅读进度
  if (isIframe.value && !e.ctrlKey) {
    const delta = Math.max(-1, Math.min(1, e.deltaY / 200))
    const next = Math.max(0, Math.min(1, (readProgress.value || 0) + delta * 0.05))
    readProgress.value = next
  }
}
watch(() => props.modelValue, (v) => {
  if (v) setTimeout(() => bodyRef.value?.addEventListener('wheel', handleWheel, { passive: false }), 0)
  else bodyRef.value?.removeEventListener('wheel', handleWheel)
})

watch(() => props.modelValue, (v) => {
  if (v) {
    setTimeout(() => {
      const el = bodyRef.value
      if (!el) return
      el.addEventListener('scroll', updateReadProgress)
      updateReadProgress()
    }, 0)
  } else {
    const el = bodyRef.value
    try { el?.removeEventListener('scroll', updateReadProgress) } catch {}
  }
})

// 监听本地进度，触发 40% / 80% 问题弹窗
watch(readProgress, (r) => {
  if (!Number.isFinite(r)) return
  maybeAskByProgress(r)
})
</script>

<Question
    v-model="questionVisible"
    :questions="questionList"
    title="知识检查"
    :closable="false"
    :requireAll="true"
    @submit="onQuestionSubmit"
/>

<style scoped>
.dv-mask {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1200;
}
.dv-modal {
  width: 90%;
  max-width: 1000px;
  max-height: 90vh;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.dv-modal:fullscreen {
  width: 100vw;
  height: 100vh;
  max-width: none;
  max-height: none;
  border-radius: 0;
}
.dv-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: linear-gradient(135deg, #1a56db 0%, #0d3b9e 100%);
  color: #fff;
}
.dv-title {
  font-weight: 700;
}
.dv-tools {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
  margin-right: 8px;
}
.dv-tool {
  border: 0;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: rgba(255,255,255,0.2);
  color: #fff;
  cursor: pointer;
}
.dv-close {
  border: 0;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: rgba(255,255,255,0.2);
  color: #fff;
  cursor: pointer;
}
.dv-body {
  padding: 0;
  height: 70vh;
  background: #fafafa;
}
.dv-main { display: flex; height: 100%; }
.dv-aside {
  width: 260px;
  background: #f8f9fa;
  padding: 12px;
  border-right: 1px solid #e9ecef;
  overflow: auto;
}
.dv-aside-title { font-weight: 700; color: #1a56db; margin-bottom: 10px; }
.dv-aside-list { max-height: calc(70vh - 60px); overflow-y: auto; }
.dv-aside-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px; border-radius: 6px; cursor: pointer; margin-bottom: 4px;
}
.dv-aside-item i { color: #10b981; font-size: 14px; }
.dv-aside-item:hover { background: #f0f3f7; }
.dv-aside-item.active { background: #e1ebff; color: #1a56db; font-weight: 600; }
.dv-aside-text { flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-size: 14px; }
.dv-view { flex: 1; }
/* 仅全屏时让内容自适应剩余空间，确保底部在最下方 */
.dv-modal:fullscreen .dv-body {
  height: auto;
  flex: 1;
  min-height: 0;
}
.dv-iframe {
  width: 100%;
  height: 100%;
  border: 0;
}
.dv-content {
  padding: 16px;
  color: #333;
  line-height: 1.7;
}
.dv-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px; /* 增大底部内边距，提高按钮间距 */
  background: #f6f8fb;
  gap: 16px; /* pill 与 actions 之间的间隔 */
}
/* 全屏时保持底部工具栏固定在底部，尺寸不改变 */
.dv-modal:fullscreen .dv-footer {
  padding: 14px 18px;
}
.dv-progress {
  color: #64748b;
  font-size: 14px;
}
.dv-pill {
  padding: 6px 12px;
  background: #eef2ff;
  color: #334155;
  border: 1px solid #c7d2fe;
  border-radius: 999px;
  cursor: pointer;
  font-size: 13px;
}
.dv-actions { display: flex; align-items: center; gap: 12px; }
.dv-btn {
  padding: 10px 16px; /* 放大按钮尺寸 */
  background: #2563eb;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}
.dv-btn-secondary {
  background: #0ea5e9;
}
.dv-btn:hover {
  background: #1d4ed8;
}
</style>
