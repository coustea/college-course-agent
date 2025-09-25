<template>
  <div v-if="visible" class="modal" @click.self="close">
    <div class="modal-content">
      <div class="modal-header">
        <div class="modal-title">{{ title }}</div>
        <button class="modal-close"
        @click="close"><i class="fas fa-times"></i></button>
      </div>
      <div class="modal-body">
        <aside class="course-sidebar">
          <div class="sidebar-title">
            课程目录
            <span class="overall-badge" :title="'本课程整体学习进度'">
              {{ Math.round(overallProgress * 100) }}%
            </span>
          </div>
          <div v-if="!hasChapters" class="no-chapters-tip">暂无目录 · 单视频课程</div>
          <div v-else class="directory-container">
            <div
              v-for="(item, index) in chapters"
              :key="index"
              class="directory-item"
            >
              <div
                class="item-content"
                :class="{
                  'active': isItemActive(item),
                  'is-folder': !isItemVideo(item),
                  'is-video': isItemVideo(item)
                }"
                :style="{ paddingLeft: '12px' }"
                @click="handleItemClick(item, 0)"
              >
                <div class="item-inner">
                  <i
                    v-if="hasItemChildren(item)"
                    class="fas expand-icon fa-chevron-down"
                  ></i>
                  <i
                    v-else-if="isItemVideo(item)"
                    class="fas fa-play-circle video-icon"
                  ></i>
                  <i
                    v-else
                    class="fas fa-folder folder-icon"
                  ></i>
                  <span class="item-title">{{ item.title }}</span>
                  <span v-if="isItemVideo(item) && item.duration" class="item-duration">{{ item.duration }}</span>
                </div>
              </div>

              <div v-if="hasItemChildren(item)" class="item-children">
                <div
                  v-for="(child, childIndex) in item.children"
                  :key="childIndex"
                  class="directory-item"
                >
                  <div
                    class="item-content"
                    :class="{
                      'active': isItemActive(child),
                      'is-folder': !isItemVideo(child),
                      'is-video': isItemVideo(child)
                    }"
                    :style="{ paddingLeft: '32px' }"
                    @click="handleItemClick(child, 1)"
                  >
                    <div class="item-inner">
                      <i
                        v-if="hasItemChildren(child)"
                        class="fas expand-icon fa-chevron-down"
                      ></i>
                      <i
                        v-else-if="isItemVideo(child)"
                        class="fas fa-play-circle video-icon"
                      ></i>
                      <i
                        v-else
                        class="fas fa-folder folder-icon"
                      ></i>
                      <span class="item-title">{{ child.title }}</span>
                      <span v-if="isItemVideo(child) && child.duration" class="item-duration">{{ child.duration }}</span>
                    </div>
                  </div>

                  <div v-if="hasItemChildren(child)" class="item-children">
                    <div
                      v-for="(grandChild, grandChildIndex) in child.children"
                      :key="grandChildIndex"
                      class="directory-item"
                    >
                      <div
                        class="item-content"
                        :class="{
                          'active': isItemActive(grandChild),
                          'is-video': isItemVideo(grandChild)
                        }"
                        :style="{ paddingLeft: '52px' }"
                        @click="handleItemClick(grandChild, 2)"
                      >
                        <div class="item-inner">
                          <i
                            v-if="isItemVideo(grandChild)"
                            class="fas fa-play-circle video-icon"
                          ></i>
                          <span class="item-title">{{ grandChild.title }}</span>
                          <span v-if="isItemVideo(grandChild) && grandChild.duration" class="item-duration">{{ grandChild.duration }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </aside>
        <section class="course-main">
          <div class="video-container" ref="videoContainer" :class="{ theatre: isTheatre }">
            <video ref="player" class="video-player" playsinline
            @click="togglePlay" @timeupdate="onTimeUpdate; syncPlayState"
            @loadedmetadata="onLoaded" @play="syncPlayState" @pause="syncPlayState">
              <source :src="currentSrc" type="video/mp4" />
              您的浏览器不支持HTML5视频播放
            </video>
            <div v-show="enterTipVisible" class="enter-tip">{{ enterTipText }}</div>
            <div v-show="isTheatre && overlayVisible" class="overlay-controls">
              <div class="left-actions">
                <button class="icon-btn" :disabled="currentIndex === 0"
                  :title="'上一节'" @click="prev" aria-label="prev">
                  <i class="fas fa-chevron-left"></i>
                </button>
                <button class="icon-btn" :title="isPlaying ? '暂停' : '播放'"
                @click="togglePlay" aria-label="play-pause">
                  <i class="fas" :class="isPlaying ? 'fa-pause' : 'fa-play'"></i>
                </button>
                <button class="icon-btn" :disabled="currentIndex >= flatChapters.length - 1"
                  :title="'下一节'" @click="next" aria-label="next">
                  <i class="fas fa-chevron-right"></i>
                </button>
              </div>
              <div class="overlay-progress" aria-label="progress">
                <div class="overlay-progress-track">
                  <div class="overlay-progress-fill"
                    :style="{ width: Math.round(currentProgress * 100) + '%' }"></div>
                </div>
              </div>
              <div class="right-actions">
                <button class="icon-btn" :title="'退出全屏(Esc)'"
                  @click="toggleTheatre" aria-label="immersive-exit">
                  <i class="fas fa-compress"></i>
                </button>
              </div>
            </div>
            <button class="corner-exit left" v-show="isTheatre && overlayVisible"
              :title="'退出全屏(Esc)'"
              @click="toggleTheatre" aria-label="corner-exit-left">
              <i class="fas fa-arrow-left"></i></button>
            <button class="corner-exit top-right"
              v-show="isTheatre && overlayVisible"
              :title="'退出全屏(Esc)'"
              @click="toggleTheatre" aria-label="corner-exit-right">
              <i class="fas fa-times"></i></button>
          </div>
          <div class="controls" v-if="!isTheatre">
            <div class="left-actions">
              <button class="icon-btn" :disabled="currentIndex === 0"
                :title="'上一节'" @click="prev" aria-label="prev">
                <i class="fas fa-chevron-left"></i>
              </button>
              <button class="icon-btn" :title="isPlaying ? '暂停' : '播放'"
                @click="togglePlay" aria-label="play-pause">
                <i class="fas" :class="isPlaying ? 'fa-pause' : 'fa-play'"></i>
              </button>
              <button class="icon-btn" :disabled="currentIndex >= flatChapters.length - 1"
                :title="'下一节'" @click="next" aria-label="next">
                <i class="fas fa-chevron-right"></i>
              </button>
            </div>
            <div class="controls-progress" aria-label="progress">
              <div
                class="controls-progress-track"
                ref="progressTrack"
                @mousemove="onTrackMove"
                @mouseenter="onTrackEnter"
                @mouseleave="onTrackLeave"
              >
                <div class="controls-progress-fill"
                  :style="{ width: Math.round(currentProgress * 100) + '%' }">
                </div>
                <div class="controls-progress-time"
                  :style="{ left: bubbleLeft + '%' }">{{ currentTimeLabel }} / {{ durationLabel }}
                </div>
                <div v-show="hoverTimeVisible" class="hover-time"
                  :style="{ left: hoverLeft + '%' }">{{ hoverTimeLabel }}
                </div>
              </div>
            </div>
            <div class="right-actions">
              <button class="icon-btn" :title="isTheatre ? '退出全屏(Esc)' : '全屏模式'"
                @click="toggleTheatre" aria-label="immersive">
                <i class="fas" :class="isTheatre ? 'fa-compress' : 'fa-expand'"></i>
              </button>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted, onBeforeUnmount } from 'vue'
import { getVideoProgress, setVideoProgress, getOverallProgress } from '../services/student/learningProgress.js'
import { addOrUpdateRecord, updateProgress, addTime } from '../services/student/historyService.js'


const props = defineProps({
  modelValue: { type: Boolean, default: false },
  courseId: { type: [String, Number], required: true },
  title: { type: String, required: true },
  chapters: { type: Array, default: () => [] },
  fallbackSrc: { type: String, default: '' },
  startIndex: { type: Number, default: 0 }
})
const emit = defineEmits(['update:modelValue', 'progress'])

const visible = ref(false)
watch(() => props.modelValue, v => { visible.value = v })
function close() {
  if (isTheatre.value) {
    isTheatre.value = false
    lockScroll(false)
  }
  emit('update:modelValue', false)
}

const player = ref(null)
const videoContainer = ref(null)
const currentIndex = ref(0)
// 扁平化目录结构，支持多级目录
const flatChapters = computed(() => {
  const result = []
  const process = (items) => {
    items.forEach(item => {
      if (item.videoUrl || item.type === 'video' || item.type === 'chapter') {
        // 这是一个视频章节
        result.push(item)
      } else if (item.children && Array.isArray(item.children)) {
        // 这是一个目录分组，递归处理子项
        process(item.children)
      }
    })
  }
  process(props.chapters || [])
  return result
})

const currentSrc = computed(() => {
  const currentChapter = flatChapters.value[currentIndex.value]
  return currentChapter?.videoUrl || props.fallbackSrc
})

const hasChapters = computed(() => flatChapters.value.length > 0)

// 辅助函数
const isItemVideo = (item) => {
  return item.videoUrl || item.type === 'video' || item.type === 'chapter'
}

const hasItemChildren = (item) => {
  return item.children && Array.isArray(item.children) && item.children.length > 0
}

const isItemActive = (item) => {
  if (!isItemVideo(item)) return false
  const flatIndex = flatChapters.value.findIndex(ch => ch === item)
  return flatIndex === currentIndex.value
}

const handleItemClick = (item, level) => {
  if (isItemVideo(item)) {
    const flatIndex = flatChapters.value.findIndex(ch => ch === item)
    if (flatIndex >= 0) {
      playChapter(flatIndex)
    }
  }
  // 对于文件夹，可以在这里添加展开/收起逻辑
}

const currentProgress = ref(0)
const overallProgress = computed(() => getOverallProgress(props.courseId) || 0)


const isTheatre = ref(false)
const isPlaying = ref(false)
const overlayVisible = ref(true)
let overlayTimer = null

const enterTipVisible = ref(false)
const enterTipText = ref('')
let enterTipTimer = null
function showEnterTip(title) {
  enterTipText.value = `已进入：${title}`
  enterTipVisible.value = true
  if (enterTipTimer) clearTimeout(enterTipTimer)
  enterTipTimer = setTimeout(() => { enterTipVisible.value = false }, 1500)
}

let wheelHandler = null
function lockScroll(enable) {
  try {
    if (enable) {
      document.documentElement.style.overflow = 'hidden'
      document.body.style.overflow = 'hidden'
      if (!wheelHandler) {
        wheelHandler = (e) => { e.preventDefault() }
        window.addEventListener('wheel', wheelHandler, { passive: false })
      }
    } else {
      document.documentElement.style.overflow = ''
      document.body.style.overflow = ''
      if (wheelHandler) {
        window.removeEventListener('wheel', wheelHandler)
        wheelHandler = null
      }
    }
  } catch {}
}

function onMouseMove() {
  if (!isTheatre.value) return
  overlayVisible.value = true
  if (overlayTimer) clearTimeout(overlayTimer)
  overlayTimer = setTimeout(() => { overlayVisible.value = false }, 2000)
}

function toggleTheatre() {
  isTheatre.value = !isTheatre.value
  lockScroll(isTheatre.value)
}

document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape' && isTheatre.value) {
    isTheatre.value = false
    lockScroll(false)
  }
})

let timeTicker = null
let lastTick = 0
function togglePlay() {
  const el = player.value
  if (!el) return
  if (el.paused) {
    el.play()
    isPlaying.value = true
    lastTick = Date.now()
    if (!timeTicker) {
      timeTicker = setInterval(() => {
        const now = Date.now()
        const delta = Math.floor((now - lastTick) / 1000)
        if (delta > 0) { addTime(props.courseId, delta); lastTick = now }
      }, 1000)
    }
  } else {
    el.pause()
    isPlaying.value = false
    if (timeTicker) {
      const now = Date.now(); const delta = Math.floor((now - lastTick) / 1000)
      if (delta > 0) addTime(props.courseId, delta)
      clearInterval(timeTicker); timeTicker = null
    }
  }

  addOrUpdateRecord({
    id: props.courseId,
    type: (props.chapters?.length || 0) > 0 ? 'video' : 'video',
    title: props.title, image: '', duration: '',
    progress: Math.round(currentProgress.value * 100) })
}

function syncPlayState() {
  const el = player.value
  if (!el) return
  isPlaying.value = !el.paused
}

const currentTimeLabel = computed(() => {
  const el = player.value
  if (!el) return '0:00'
  const t = Math.floor(el.currentTime || 0)
  const m = Math.floor(t / 60)
  const s = String(t % 60).padStart(2, '0')
  return `${m}:${s}`
})

const durationLabel = computed(() => {
  const el = player.value
  const d = Math.floor(el?.duration || 0)
  const m = Math.floor(d / 60)
  const s = String(d % 60).padStart(2, '0')
  return `${m}:${s}`
})

const bubbleLeft = computed(() => {
  const pct = Math.round((currentProgress.value || 0) * 100)
  return Math.min(98, Math.max(2, pct))
})

const progressTrack = ref(null)
const hoverTimeVisible = ref(false)
const hoverLeft = ref(0)
const hoverTimeLabel = ref('0:00')

function formatTime(sec) {
  const t = Math.max(0, Math.floor(sec))
  const m = Math.floor(t / 60)
  const s = String(t % 60).padStart(2, '0')
  return `${m}:${s}`
}

function onTrackEnter() {
  hoverTimeVisible.value = true
}

function onTrackLeave() {
  hoverTimeVisible.value = false
}

function onTrackMove(e) {
  const el = progressTrack.value
  const video = player.value
  if (!el || !video || !video.duration) return
  const rect = el.getBoundingClientRect()
  const x = Math.min(Math.max(e.clientX - rect.left, 0), rect.width)
  const ratio = x / rect.width
  hoverLeft.value = Math.min(98, Math.max(2, Math.round(ratio * 100)))
  hoverTimeLabel.value = formatTime(video.duration * ratio)
}

function onLoaded() {
  const p = getVideoProgress(props.courseId, currentIndex.value)
  if (p > 0 && player.value?.duration) {
    player.value.currentTime = p * player.value.duration
  }
}

function onTimeUpdate() {
  if (!player.value?.duration) return
  const progress = player.value.currentTime / player.value.duration
  currentProgress.value = progress
  setVideoProgress(props.courseId, currentIndex.value, progress)
  emit('progress', getOverallProgress(props.courseId))
  try { window.dispatchEvent(new CustomEvent('learning-progress-updated', { detail: { courseId: props.courseId } })) } catch {}
  updateProgress(props.courseId, Math.round(progress * 100))

}


function playChapter(i) {
  currentIndex.value = i
  nextTickSeekSaved()
  const t = flatChapters.value[i]?.title || `第${i + 1}节`
  showEnterTip(t)
}

function prev() {
  if (currentIndex.value > 0) playChapter(currentIndex.value - 1)
}

function next() {
  if (currentIndex.value < flatChapters.value.length - 1) playChapter(currentIndex.value + 1)
}

function nextTickSeekSaved() {
  requestAnimationFrame(() => {
    const p = getVideoProgress(props.courseId, currentIndex.value)
    if (player.value?.duration && p > 0) {
      player.value.currentTime = p * player.value.duration
    }
  })
}

onMounted(() => {
  visible.value = props.modelValue
  if (!hasChapters.value) {
    showEnterTip(props.title)
  }
})
watch(currentIndex, (v) => {
  if (v != null && flatChapters.value.length > 0) {
    const t = flatChapters.value[v]?.title || `第${v + 1}节`
    showEnterTip(t)
  }
}, { immediate: true })

onBeforeUnmount(() => {
  lockScroll(false)
  if (timeTicker) {
    clearInterval(timeTicker)
    timeTicker = null
  }
})

watch(() => props.startIndex, (v) => {
  if (typeof v === 'number' && v >= 0 && v < flatChapters.value.length) {
    currentIndex.value = v
    nextTickSeekSaved()
  }
}, { immediate: true })
</script>

<style scoped>
.modal {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0,0,0,.6);
  z-index: 1000;
}

.modal-content {
  width: 90%;
  max-width: 1000px;
  max-height: 90vh;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: linear-gradient(135deg, #1a56db 0%, #0d3b9e 100%);
  color: #fff;
}

.modal-title {
  font-weight: 700;
}

.modal-close {
  border: 0;
  width: 36px;
  height: 36px;
  border-radius: 18px;
  background: rgba(255,255,255,.2);
  color: #fff;
  cursor: pointer;
}

.modal-body {
  display: flex;
  height: 70vh;
}

.course-sidebar {
  width: 280px;
  background: #f8f9fa;
  padding: 16px;
  border-right: 1px solid #e9ecef;
  overflow: auto;
}

.sidebar-title {
  font-weight: 700;
  color: #1a56db;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.overall-badge {
  background: #e6f4ff;
  color: #1677ff;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.directory-container {
  max-height: calc(70vh - 100px);
  overflow-y: auto;
}

.directory-item {
  margin: 0;
}

.item-content {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 6px;
  margin-bottom: 2px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.item-content:hover {
  background: #f8f9fa;
}

.item-content.active {
  background: #e1ebff;
  color: #1a56db;
  font-weight: 600;
}

.item-content.is-folder {
  font-weight: 500;
  color: #374151;
}

.item-content.is-video {
  color: #1f2937;
}

.item-inner {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.expand-icon {
  width: 12px;
  height: 12px;
  font-size: 10px;
  color: #666;
  transition: transform 0.2s ease;
}

.video-icon {
  color: #10b981;
  font-size: 14px;
}

.folder-icon {
  color: #f59e0b;
  font-size: 14px;
}

.item-title {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
}

.item-duration {
  color: #666;
  font-size: 12px;
  white-space: nowrap;
}

.item-children {
  margin-left: 0;
}

.no-chapters-tip {
  background: #fff7ed;
  color: #d97706;
  border: 1px solid #fde68a;
  border-radius: 6px;
  padding: 8px 10px;
  margin-bottom: 8px;
  font-size: 12px;
}

.course-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.video-container {
  flex: 1;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  pointer-events: auto;
}

.video-player {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: #f8f9fa;
}

.left-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.controls-progress {
  flex: 1;
  padding: 0 12px;
}

.controls-progress-track {
  width: 100%; height: 6px;
  background: #e2e8f0;
  border-radius: 3px;
  overflow: hidden;
  position: relative;
}

.controls-progress-fill {
  height: 100%;
  background: #10b981;
  border-radius: 3px;
  transition: width 0.2s;
}

.controls-progress-time {
  position: absolute;
  top: -28px;
  right: 0; transform: translate(-50%, 0);
  font-size: 12px;
  color: #fff; background: rgba(0,0,0,0.7);
  padding: 2px 6px;
  border-radius: 4px;
  white-space: nowrap;
}

.hover-time {
  position: absolute;
  top: -52px;
  transform: translate(-50%, 0);
  font-size: 12px;
  color: #fff;
  background: rgba(0,0,0,0.6);
  padding: 2px 6px;
  border-radius: 4px;
  white-space: nowrap;
}

.btn {
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid #e1e8ef;
  background: #fff;
  cursor: pointer;
}

.right-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-btn {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 8px;
  background: rgba(255,255,255,0.85);
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0,0,0,0.18);
}
.icon-btn:hover { box-shadow: 0 6px 16px rgba(0,0,0,0.24); }


.btn.btn-primary {
  border: none;
  background: linear-gradient(135deg, #1a56db 0%, #0d3b9e 100%);
  color: #fff;
}

.video-container.theatre {
  position: fixed;
  inset: 0;
  z-index: 2000;
  overflow: hidden;
}

.overlay-controls {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0.35) 65%);
}

.enter-tip {
  position: absolute;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0,0,0,0.6);
  color: #fff;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 14px;
}

.overlay-progress {
  flex: 1;
  padding: 0 12px;
}

.overlay-progress-track {
  width: 100%;
  height: 6px;
  background: rgba(255,255,255,0.35);
  border-radius: 3px;
  overflow: hidden;
}

.overlay-progress-fill {
  height: 100%;
  background: #10b981;
  border-radius: 3px;
  transition: width 0.2s;
}

.corner-exit {
  position: absolute;
  top: 12px;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: none;
  background: rgba(0,0,0,0.5);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.corner-exit.left {
  left: 12px;
}

.corner-exit.top-right {
  right: 12px;
  top: 12px;
}
.corner-exit { z-index: 2010; }

.no-chapters-tip {
  font-size: 14px;
  color: #666;
  text-align: center;
  padding: 10px 0;
  background: #f0f0f0;
  border-radius: 6px;
  margin-bottom: 12px;
}
</style>

