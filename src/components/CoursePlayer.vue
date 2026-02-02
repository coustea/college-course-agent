<template>
  <div v-if="visible" class="modal" @click.self="close" @wheel="forwardBackgroundScroll">
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
            <span class="overall-badge" :title="'本课程整体学习进度，原始值: ' + overallProgress">
              {{ Math.round(overallProgress * 100) }}%
            </span>
          </div>
          <div class="directory-summary">
            <span v-if="episodeCount > 0">共 {{ episodeCount }} 节</span>
            <span v-else>暂无目录 · 单视频课程</span>
          </div>
          <div v-if="episodeCount > 0" class="directory-container">
            <div
                v-for="n in episodeCount"
                :key="n"
                class="directory-item"
            >
              <div
                  class="item-content"
                  :class="{ 'active': (n - 1) === currentIndex }"
                  :style="{ paddingLeft: '12px' }"
                  @click="selectEpisode(n - 1)"
              >
                <div class="item-inner">
                  <i class="fas fa-play-circle video-icon"></i>
                  <span class="item-title">第{{ n }}节</span>
                </div>
              </div>
            </div>
          </div>
        </aside>
        <section class="course-main">
          <div class="video-container" ref="videoContainer" :class="{ theatre: isTheatre }">
            <video ref="player" class="video-player" playsinline preload="metadata" :src="currentSrc"
                   @click="togglePlay" @timeupdate="onTimeUpdate(); syncPlayState()"
                   @loadedmetadata="onLoaded" @play="syncPlayState" @pause="syncPlayState" @ended="onEnded" @seeked="onSeeked"
                   @error="onVideoError">
              您的浏览器不支持HTML5视频播放
            </video>
            <div class="time-hud">{{ hudCurrentLabel }} / {{ hudDurationLabel }}</div>
            <div class="mini-progress">
              <div class="mini-progress-fill" :style="{ width: Math.round(currentProgress * 100) + '%' }"></div>
            </div>
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
                <button class="icon-btn" :disabled="currentIndex >= totalCount - 1"
                        :title="'下一节'" @click="next" aria-label="next">
                  <i class="fas fa-chevron-right"></i>
                </button>
              </div>
              <div class="overlay-progress" aria-label="progress">
                <div class="overlay-progress-track" ref="overlayTrack"
                     @mousedown="onTrackDown($event, 'overlay')" @click="onTrackClick($event, 'overlay')">
                  <div class="overlay-progress-fill"
                       :style="{ width: Math.round(currentProgress * 100) + '%' }"></div>
                  <div class="overlay-track-dot"
                       :style="[progressDotStyle, { left: bubbleLeft + '%' }]"
                       :title="Math.round(currentProgress * 100) + '%'
                       " @mousedown.stop="onTrackDown($event, 'overlay')"></div>
                </div>
              </div>
              <div class="right-actions">
                <div class="volume">
                  <i class="fas fa-volume-up" title="音量"></i>
                  <input type="range" min="0" max="100" step="1" v-model.number="volumePercent" @input="applyVolume" />
                </div>
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
              <button class="icon-btn" :disabled="currentIndex >= totalCount - 1"
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
                  @mousedown="onTrackDown($event, 'controls')"
                  @click="onTrackClick($event, 'controls')"
              >
                <div class="controls-progress-fill"
                     :style="{ width: Math.round(currentProgress * 100) + '%' }">
                </div>
                <div class="controls-track-dot"
                     :style="{ left: bubbleLeft + '%' }"
                     @mousedown.stop="onTrackDown($event, 'controls')"></div>
                <div class="controls-progress-time"
                     :style="{ left: bubbleLeft + '%' }">{{ currentTimeLabel }} / {{ durationLabel }}
                </div>
                <div v-show="hoverTimeVisible" class="hover-time"
                     :style="{ left: hoverLeft + '%' }">{{ hoverTimeLabel }}
                </div>
              </div>
            </div>
            <div class="right-actions">
              <div class="volume">
                <i class="fas fa-volume-up" title="音量"></i>
                <input type="range" min="0" max="100" step="1" v-model.number="volumePercent" @input="applyVolume" />
              </div>
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
  <Question
      v-if="visible && enableQuestions"
      v-model="questionVisible"
      :title="questionTitle"
      :stem="questionStem"
      :options="questionOptions"
      :correct-index="questionCorrectIndex"
      :analysis="questionAnalysis"
      @submit="onQuestionSubmit"
  />
</template>

<script setup>
import { ref, watch, computed, onMounted, onBeforeUnmount, getCurrentInstance } from 'vue'
import axios from "axios"
import Question from '/src/components/Question.vue'

const { proxy } = getCurrentInstance()
const BASE_URL = proxy.$baseUrl

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  courseId: { type: [String, Number], required: true },
  title: { type: String, required: true },
  chapters: { type: Array, default: () => [] },
  fallbackSrc: { type: String, default: '' },
  startIndex: { type: Number, default: 0 },
  episodeTotal: { type: Number, default: 0 },
  videoCount: { type: Number, default: 0 },
  docCount: { type: Number, default: 0 },
  enableQuestions: { type: Boolean, default: true }
})
const emit = defineEmits(['update:modelValue', 'progress'])

const visible = ref(false)
watch(() => props.modelValue, v => { visible.value = v })
async function close() {
  if (isTheatre.value) {
    isTheatre.value = false
  }
  stopWatchTimerAndAccumulate()
  await reportAndReset()
  lockScroll(false)
  emit('update:modelValue', false)
}

const player = ref(null)
const videoContainer = ref(null)
const currentIndex = ref(0)
const flatChapters = computed(() => {
  const result = []
  const process = (items) => {
    items.forEach(item => {
      const hasUrl = !!(item && item.videoUrl && String(item.videoUrl).trim())
      if (hasUrl) {
        result.push(item)
      } else if (item && Array.isArray(item.children) && item.children.length) {
        process(item.children)
      }
    })
  }
  process(props.chapters)
  return result
})

// 计算总集数
const episodeCount = computed(() => {
  if (props.episodeTotal && props.episodeTotal > 0) return props.episodeTotal
  const sum = (props.videoCount || 0) + (props.docCount || 0)
  if (sum > 0) return sum
  return flatChapters.value.length
})

const currentSrc = computed(() => {
  const currentChapter = flatChapters.value[currentIndex.value]
  return currentChapter?.videoUrl || props.fallbackSrc
})

// 错误切换候选源，尽量自愈播放路径
const triedSources = ref(new Set())
// 预取试题键集合，避免重复预取
const prefetchedKeys = new Set()
const UPLOADS_ORIGIN = ( 'http://localhost:9999' || import.meta?.env?.VITE_BACKEND_ORIGIN || 'http://localhost:9999')
function buildAltSources(src) {
  const list = []
  const s = String(src || '')
  const add = (u) => { if (u && !list.includes(u)) list.push(u) }
  // 当前章节与回退
  const chapter = flatChapters.value[currentIndex.value]
  add(chapter?.videoUrl)
  add(props.fallbackSrc)
  // /uploads <-> /media/uploads 互换
  if (s.startsWith('/uploads/')) add(`/media${s}`)
  if (s.startsWith('/media/uploads/')) add(s.replace(/^\/media/, ''))
  // 绝对地址 host -> 同源 /uploads/**
  const m = s.match(/^https?:\/\/[^/]+(:\d+)?\/(uploads\/.*)$/i)
  if (m) add(`/${m[2]}`)
  // 绝对地址 host -> 同源 /media/uploads/**
  const m2 = s.match(/^https?:\/\/[^/]+(:\d+)?\/(media\/uploads\/.*)$/i)
  if (m2) add(`/${m2[2]}`)
  // 同源 /uploads/** -> 绝对后端 ORIGIN
  const mu = s.match(/^\/(?:media\/)?(uploads\/.*)$/i)
  if (mu) add(`${UPLOADS_ORIGIN}/${mu[1]}`)
  return list.filter(Boolean)
}

async function isReachable(url) {
  if (!url) return false
    const res = await fetch(url, { method: 'GET', headers: { Range: 'bytes=0-1' }, cache: 'no-store' })
    if (res.ok) return true
  try {
    const res2 = await fetch(url, { method: 'HEAD', cache: 'no-store' })
    return res2.ok
  } catch(e) { console.error(e) }
  return false
}

async function choosePlayableAndLoad(preferred) {
  const el = player.value
  if (!el) return false
  const start = String(preferred || '')
  const candidates = [start, ...buildAltSources(start)].filter(Boolean)
  for (const u of candidates) {
    if (triedSources.value.has(u)) continue
    // eslint-disable-next-line no-console
    console.debug('[CoursePlayer] 尝试播放源:', u)
    const ok = await isReachable(u)
    if (ok) {
        el.src = u
        el.load()
        await el.play?.()
        triedSources.value.add(u)
        return true
    }
    triedSources.value.add(u)
  }
  return false
}

function onVideoError(e) {
  console.warn('视频加载失败，回退到 fallbackSrc', e)
  const el = player.value
  if (!el) return
  const cur = el.currentSrc || el.src || ''
  if (!triedSources.value) triedSources.value = new Set()
  triedSources.value.add(cur)
  // 依次检测候选源，找到可访问的立即切换
  choosePlayableAndLoad(cur)
}

const hasChapters = computed(() => flatChapters.value.length > 0)

// 统一计数：有真实目录则用目录数，否则用后端总集数
const totalCount = computed(() => {
  return hasChapters.value ? flatChapters.value.length : episodeCount.value
})

function selectEpisode(i) {
  if (hasChapters.value) {
    playChapter(i)
  } else {
    currentIndex.value = Math.max(0, Math.min(i, Math.max(0, totalCount.value - 1)))
    const t = `第${currentIndex.value + 1}集`
    showEnterTip(t)
    nextTickSeekSaved()
  }
}

const currentProgress = ref(0)
const overallProgress = ref(0)
// 后端允许的最大可快进比例（当前视频），0~1
const backendSeekMax = ref(0)
function allowedSeekRatio() {
  const cur = Number(currentProgress.value || 0)
  const srv = Number(backendSeekMax.value || 0)
  // 允许回退任意，但不允许超过已观看或后端给出的最大比例
  return Math.max(cur, srv)
}

// 获取课程整体进度
async function fetchOverallProgress() {
  try {
    const studentId = localStorage.getItem('userId')
    const courseId = props.courseId || flatChapters.value?.[0]?.courseId
    if (!studentId || !courseId) {
      console.log('[CoursePlayer] 缺少必要参数，studentId:', studentId, 'courseId:', courseId)
      return
    }

    const token = localStorage.getItem('token')
    console.log('[CoursePlayer] 请求课程进度，studentId:', studentId, 'courseId:', courseId)
    const res = await axios.get(`${BASE_URL}/progress/course/all`, {
      params: { studentId, courseId },
      headers: { Authorization: `Bearer ${token}` }
    })

    console.log('[CoursePlayer] 获取课程进度结果:', res.data)
    if (res?.data?.code === 200) {
      const data = res?.data?.data
      console.log('[CoursePlayer] data内容:', data)

      // 获取当前视频的 videoId
      const ch = flatChapters.value?.[currentIndex.value]
      const currentVideoId = ch?.videoId ?? ch?.id ?? ch?.videoIndex ?? (currentIndex.value + 1)
      console.log('[CoursePlayer] 当前视频ID:', currentVideoId, '当前索引:', currentIndex.value)

      // 从 videos 数组中查找当前视频的进度
      let percentage = 0
      if (Array.isArray(data?.videos)) {
        const currentVideo = data.videos.find(v =>
          v.videoId === currentVideoId ||
          v.id === currentVideoId ||
          v.courseId === currentVideoId
        )
        console.log('[CoursePlayer] 找到的视频数据:', currentVideo)

        if (currentVideo && typeof currentVideo.percentage === 'number') {
          percentage = currentVideo.percentage
          console.log('[CoursePlayer] 当前视频进度:', percentage + '%')
        } else {
          console.log('[CoursePlayer] 未找到当前视频进度，使用默认值0')
        }
      }

      overallProgress.value = percentage / 100
      console.log('[CoursePlayer] 设置显示进度:', percentage + '%', '转换后:', overallProgress.value)
      // 更新当前视频允许的最大可快进比例
      backendSeekMax.value = Math.max(0, Math.min(1, percentage / 100))
      // 将后端进度写入本地恢复键，确保每次进入课程只用后端进度
      try {
        const pct = Math.max(0, Math.min(100, Number(percentage || 0))) / 100
        const key = `video_resume_${props.courseId}_${currentIndex.value}`
        localStorage.setItem(key, JSON.stringify({ p: pct, t: Date.now() }))
      } catch (e) { console.error(e) }
    }
  } catch (e) {
    console.error('[CoursePlayer] 获取课程进度失败:', e)
  }
}

// 观看时长统计与上报
const lastPlayRealStartMs = ref(0)
const unreportedWatchedSec = ref(0)
async function reportCourseProgress(deltaSec) {
  const sec = Math.max(0, Math.floor(Number(deltaSec)))
  if (sec <= 0) return
  const studentId = localStorage.getItem('userId')
  const ch = flatChapters.value?.[currentIndex.value]
  const courseId = props.courseId || ch?.courseId || flatChapters.value?.[0]?.courseId
  const videoId = ch?.videoId ?? ch?.id ?? ch?.videoIndex ?? ch?.index ?? (currentIndex.value + 1)
  if (!studentId || !courseId || !videoId) return
  try {
    console.log('[CoursePlayer] 观看时长上报开始(展示所需参数)', studentId, courseId, videoId, sec)
    const token = localStorage.getItem('token')
    const res = await axios.post(`${BASE_URL}/progress/report`,null, {
      params: { studentId, courseId, videoId, deltaSec: sec },
      headers:  { Authorization: `Bearer ${token}` }
    })
    console.log('[CoursePlayer] 观看时长上报结果', res.data)
    if (res?.data?.code === 200) {
      console.log('观看时长上报成功')
      // 更新整体进度
      const percentage = res?.data?.data?.percentage ?? res?.data?.percentage
      if (typeof percentage === 'number') {
        overallProgress.value = percentage / 100 // 将百分比转换为 0-1 范围
        console.log('[CoursePlayer] 更新整体进度:', percentage + '%')
      }
    }
  } catch (e) {
    console.error('观看时长上报失败', e)
  }
}

function startWatchTimerIfNeeded() {
  if (!lastPlayRealStartMs.value) lastPlayRealStartMs.value = Date.now()
}

function stopWatchTimerAndAccumulate() {
  if (lastPlayRealStartMs.value) {
    const delta = Math.floor((Date.now() - lastPlayRealStartMs.value) / 1000)
    if (delta > 0) unreportedWatchedSec.value += delta
    lastPlayRealStartMs.value = 0
  }
}

async function reportAndReset() {
  const sec = unreportedWatchedSec.value
  unreportedWatchedSec.value = 0
  if (sec > 0)
    await reportCourseProgress(sec)
}


const isTheatre = ref(false)
const isPlaying = ref(false)
const overlayVisible = ref(true)
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
      document.documentElement.style.overflow = 'auto'
      document.body.style.overflow = 'auto'
      if (wheelHandler) {
        window.removeEventListener('wheel', wheelHandler)
        wheelHandler = null
      }
    }
  } catch (e) { console.error(e) }
}
function toggleTheatre() {
  isTheatre.value = !isTheatre.value
  lockScroll(isTheatre.value)
}

function forwardBackgroundScroll(e) {
  try {
    if (!visible.value || isTheatre.value) return
    const sc = document.querySelector('.main-content') || document.scrollingElement || document.documentElement
    if (sc) {
      e.preventDefault()
      sc.scrollTop += e.deltaY
    }
  } catch (e) { console.error(e) }
}

function handleKeydown(e) {
  try {
    const tag = (e.target && e.target.tagName) ? String(e.target.tagName).toLowerCase() : ''
    if (tag === 'input' || tag === 'textarea' || (e.target && e.target.isContentEditable)) return
    const el = player.value
    if (!visible.value || !el) return
    if (e.key === 'Escape' && isTheatre.value) {
      isTheatre.value = false
      lockScroll(false)
      return
    }
    if (e.key === ' ' || e.code === 'Space') {
      e.preventDefault()
      togglePlay()
      return
    }
    if (e.key === 'ArrowLeft') {
      e.preventDefault()
      seekBy(-5)
      return
    }
    if (e.key === 'ArrowRight') {
      e.preventDefault()
      seekBy(5)
    }
  } catch (e) { console.error(e) }
}

function seekBy(deltaSec) {
  const el = player.value
  if (!el || !el.duration) return
  const now = el.currentTime || 0
  let target = now + deltaSec
  // 禁止快进超过允许比例
  const maxPos = allowedSeekRatio() * el.duration
  if (deltaSec > 0 && target > maxPos) target = maxPos
  el.currentTime = Math.max(0, Math.min(el.duration, target))
}

let timeTicker = null
const wasPlayingBeforeQuestion = ref(false)
async function togglePlay() {
  const el = player.value
  if (!el) return
  if (el.paused) {
    if (!el.currentSrc || !el.src || el.readyState < 2) {
      await choosePlayableAndLoad(currentSrc.value)
    }
    el.play()
    isPlaying.value = true
    startWatchTimerIfNeeded()
    // 首次播放预取本节试题：后台生成并保存，后续 40%/80% 时直接取用
    const key = `${props.courseId}-${currentIndex.value}`
    if (prefetchedKeys.has(key)) return
    prefetchedKeys.add(key)
  } else {
    el.pause()
    isPlaying.value = false
    stopWatchTimerAndAccumulate()
    await reportAndReset()
  }
}
  const hudNow = ref(0) // 触发计算属性刷新
let hudTicker = null
const currentTimeLabel = computed(() => {
  hudNow.value
  const el = player.value
  if (!el) return '0:00'
  const t = Math.floor(el.currentTime || 0)
  const m = Math.floor(t / 60)
  const s = String(t % 60).padStart(2, '0')
  return `${m}:${s}`
})

// 用于展示的时长（优先采用后端提供的每集时长，其次回退到真实时长）
const displayDurationSec = computed(() => {
  const backend = backendDurationSec.value
  if (Number.isFinite(backend) && backend > 0) return Math.floor(backend)
  const el = player.value
  return Math.floor(el?.duration || 0)
})

const durationLabel = computed(() => {
  const d = displayDurationSec.value
  const m = Math.floor(d / 60)
  const s = String(d % 60).padStart(2, '0')
  return `${m}:${s}`
})

// 解析后端时长为“秒”：支持 '1050' / '1050秒' / '17:29' / { durationSec } / { length }
function parseDurationSec(obj) {
  if (!obj) return 0
  const keys = ['durationSec', 'length', 'seconds', 'duration']
  for (const k of keys) {
    if (obj[k] != null) {
      const v = String(obj[k])
      // mm:ss -> 转秒
      if (/^\d{1,3}:\d{2}$/.test(v)) {
        const [m, s] = v.split(':').map(n => Number(n))
        if (Number.isFinite(m) && Number.isFinite(s)) return m * 60 + s
      }
      // 纯数字或带“秒”
      const n = Number(v.replace(/[^\d]/g, ''))
      if (Number.isFinite(n)) return n
    }
  }
  return 0
}

const backendDurationsSec = computed(() => {
  return (flatChapters.value || []).map(ch => parseDurationSec(ch))
})

const backendDurationSec = computed(() => {
  return backendDurationsSec.value?.[currentIndex.value] || 0
})
const hudDurationLabel = computed(() => {
  const base = displayDurationSec.value
  const m = Math.floor(base / 60)
  const s = String(base % 60).padStart(2, '0')
  return `${m}:${s}`
})
const hudCurrentLabel = computed(() => {
  hudNow.value
  const el = player.value
  const cur = Math.floor(el?.currentTime || 0)
  const m = Math.floor(cur / 60)
  const s = String(cur % 60).padStart(2, '0')
  return `${m}:${s}`
})

  const progressTrack = ref(null)
const overlayTrack = ref(null)
const hoverTimeVisible = ref(false)
const hoverLeft = ref(0)
const hoverTimeLabel = ref('0:00')
const volumePercent = ref(100)
const isDragging = ref(false)
const draggingWhich = ref('')
function onDragMove(e) {
  if (!isDragging.value) return
  const video = player.value
  const el = draggingWhich.value === 'overlay' ? overlayTrack.value : progressTrack.value
  if (!el || !video || !video.duration) return
  const rect = el.getBoundingClientRect()
  const x = Math.min(Math.max(e.clientX - rect.left, 0), rect.width)
  let ratio = x / rect.width
  const maxR = allowedSeekRatio()
  if (ratio > maxR) ratio = maxR
  video.currentTime = ratio * video.duration
  currentProgress.value = ratio
}

function stopDragging() {
  isDragging.value = false
  draggingWhich.value = ''
  try {
    document.removeEventListener('mousemove', onDragMove)
    document.removeEventListener('mouseup', stopDragging)
  } catch (e) { console.error(e) }
}
function startDragging(which) {
  isDragging.value = true
  draggingWhich.value = which
  try {
    document.addEventListener('mousemove', onDragMove)
    document.addEventListener('mouseup', stopDragging)
  } catch (e) { console.error(e) }
}

function onTrackEnter() {}
function onTrackLeave() {}
function onTrackMove() {}

function onTrackClick(e, which) {
  const el = which === 'overlay' ? overlayTrack.value : progressTrack.value
  const video = player.value
  if (!el || !video || !video.duration) return
  const rect = el.getBoundingClientRect()
  const x = Math.min(Math.max(e.clientX - rect.left, 0), rect.width)
  let ratio = x / rect.width
  const maxR = allowedSeekRatio()
  if (ratio > maxR) ratio = maxR
  video.currentTime = ratio * video.duration
}

function onTrackDown(e, which) {
  onTrackClick(e, which)
  startDragging(which)
}

function applyVolume() {
  const el = player.value
  if (!el) return
  const v = Math.max(0, Math.min(100, Number(volumePercent.value) || 0))
  el.muted = v === 0
  el.volume = v / 100
}

function formatTime(sec) {
  const t = Math.max(0, Math.floor(sec))
  const m = Math.floor(t / 60)
  const s = String(t % 60).padStart(2, '0')
  return `${m}:${s}`
}

function playChapter(i) {
  currentIndex.value = i
  nextTickSeekSaved()
  const t = flatChapters.value[i]?.title || `第${i + 1}集`
  showEnterTip(t)
}

function prev() {
  if (currentIndex.value > 0) playChapter(currentIndex.value - 1)
}

async function next() {
  if (currentIndex.value < totalCount.value - 1) {
    // 切换前先上报当前小节的观看进度
    stopWatchTimerAndAccumulate()
    await reportAndReset()
    if (hasChapters.value) {
      playChapter(currentIndex.value + 1)
    } else {
      selectEpisode(currentIndex.value + 1)
    }
    // 进入下一集后开始计时
    startWatchTimerIfNeeded()
  }
}

function nextTickSeekSaved() {
  requestAnimationFrame(() => {
    try {
      const key = `video_resume_${props.courseId}_${currentIndex.value}`
      const raw = localStorage.getItem(key)
      if (raw && player.value?.duration) {
        const saved = JSON.parse(raw)
        const pct = Number(saved?.p)
        if (Number.isFinite(pct) && pct > 0 && pct < 1) {
          player.value.currentTime = pct * player.value.duration
        }
      }
    } catch (e) { console.error(e) }
  })
}

onMounted(() => {
  visible.value = props.modelValue
  if (!hasChapters.value) {
    showEnterTip(props.title)
  }
  try { window.addEventListener('keydown', handleKeydown) } catch (e) { console.error(e) }
  // 进入播放器时，如果自动播放或用户立即播放，会开始计时
  // 题目生成：按课程维度只取一次
  prefetchQuestions()
})
watch(currentIndex, (v) => {
  if (v != null && totalCount.value > 0) {
    const t = hasChapters.value ? (flatChapters.value[v]?.title || `第${v + 1}集`) : `第${v + 1}集`
    showEnterTip(t)
    // 切换视频时重新获取进度
    fetchOverallProgress()
  }
}, { immediate: true })

watch(visible, (v) => {
  if (!v) {
    lockScroll(false)
  }
  // 每次打开弹窗时预取一次（保障刷新后立即可用）
  if (v) {
    // 获取课程整体进度
    fetchOverallProgress()
    // 题目生成：按课程维度只取一次
    prefetchQuestions()
  }
})

onBeforeUnmount(() => {
  lockScroll(false)
  stopTickerAndFlush()

  stopWatchTimerAndAccumulate()
  reportAndReset()

  try { window.removeEventListener('keydown', handleKeydown) } catch (e) { console.error(e) }
  stopDragging()
  try { if (hudTicker) { clearInterval(hudTicker); hudTicker = null } } catch (e) { console.error(e) }
})

watch(() => props.startIndex, (v) => {
  if (typeof v === 'number' && v >= 0 && v < totalCount.value) {
    if (hasChapters.value) {
      currentIndex.value = v
      nextTickSeekSaved()
    } else {
      selectEpisode(v)
    }
  }
}, { immediate: true })

// 还原缺失的播放状态与时间相关函数
function startHeartbeatTicker() {}

function stopTickerAndFlush() {
  if (timeTicker) {
    clearInterval(timeTicker)
    timeTicker = null
  }
}

function syncPlayState() {
  const el = player.value
  if (!el) return
  const nowPlaying = !el.paused
  if (nowPlaying && !timeTicker) {
    startHeartbeatTicker()
  } else if (!nowPlaying && timeTicker) {
    stopTickerAndFlush()
  }
  isPlaying.value = nowPlaying
}

function onLoaded() {
  try {
    const key = `video_resume_${props.courseId}_${currentIndex.value}`
    const raw = localStorage.getItem(key)
    if (raw) {
      const saved = JSON.parse(raw)
      const pct = Number(saved?.p)
      if (Number.isFinite(pct) && pct > 0 && pct < 1 && player.value?.duration) {
        player.value.currentTime = pct * player.value.duration
      }
    }
  } catch (e) { console.error(e) }
  try {
    if (!hudTicker) {
      hudTicker = setInterval(() => { hudNow.value = Date.now() }, 200)
    }
  } catch (e) { console.error(e) }
}

function onEnded() {
  isPlaying.value = false
  stopTickerAndFlush()
  stopWatchTimerAndAccumulate()
  reportAndReset()
}

function onSeeked() {
  stopTickerAndFlush()
  startHeartbeatTicker()
  try { lastPlayRealStartMs.value = Date.now() } catch (e) { console.error(e) }
}

function onTimeUpdate() {
  if (!player.value?.duration) return
  const progress = player.value.currentTime / player.value.duration
  currentProgress.value = progress
  // 进度触发 40% / 80% 弹题
  maybeAskByProgress(progress)
  try {
    const key = `video_resume_${props.courseId}_${currentIndex.value}`
    localStorage.setItem(key, JSON.stringify({ p: progress, t: Date.now() }))
  } catch (e) { console.error(e) }
  try { emit('progress', overallProgress.value) } catch {}
}

// 缺失的进度相关计算属性
const bubbleLeft = computed(() => {
  const pct = Math.round((currentProgress.value || 0) * 100)
  return Math.min(98, Math.max(2, pct))
})

const progressDotStyle = computed(() => {
  const pct = Math.max(0, Math.min(1, currentProgress.value || 0))
  const deg = Math.round(pct * 360)
  return { background: `conic-gradient(#10b981 ${deg}deg, rgba(255,255,255,0.18) 0)` }
})

// ===== 题目弹窗逻辑 =====
const questionVisible = ref(false)
const questionTitle = ref('选择题')
const questionStem = ref('以下哪个选项是正确的？')
const questionOptions = ref(['选项A', '选项B', '选项C', '选项D'])
const questionCorrectIndex = ref(0)
const questionAnalysis = ref('')
const examId = ref(null)
const currentQuestionId = ref(null)
const answersSoFar = ref([])
const asked40 = ref(false)
const asked80 = ref(false)
const prefetchedExam = ref(null)
const prefetchedDict = ref({})
const prefetchingKeys = new Set()

function maybeAskByProgress(p) {
  if (!props.enableQuestions) return
  try {
    const pct = Number(p)
    if (!Number.isFinite(pct) || pct <= 0) return
    if (!prefetchedExam.value) prefetchQuestions()
    if (pct >= 0.4 && !asked40.value) {
      asked40.value = true
      if (!prefetchedExam.value) {
        prefetchQuestions().finally(() => { showQuestionFromPool(0)})
      } else {
        showQuestionFromPool(0)
      }
    } else if (pct >= 0.8 && !asked80.value) {
      asked80.value = true
      if (!prefetchedExam.value) {
        prefetchQuestions().finally(() => {showQuestionFromPool(1) })
      } else {
        showQuestionFromPool(1)
      }
    }
  } catch (e) { console.error(e) }
}

async function prefetchQuestions() {
  try {
    const studentId = localStorage.getItem('userId')
    const courseId = props.courseId
    if (!studentId || !courseId) return
    const key = `${courseId}-${currentIndex.value}`
    if (prefetchedDict.value[key] || prefetchingKeys.has(key)) return
    prefetchingKeys.add(key)
    const token = localStorage.getItem('token') || ''
    // 获取当前视频的 videoId
    const ch = props.chapters[currentIndex.value]
    const videoId = ch?.videoId ?? ch?.id ?? ch?.videoIndex ?? null
    const body = { courseId, studentId, choiceCount: 2, judgeCount: 0, videoId, documentId: null }
    const res = await axios.post(`${BASE_URL}/aiexam/generate`, body, {
      headers: {
        Authorization: `Bearer ${token}`, 'Content-Type': 'application/json'
      }
    })
    console.log('获取题目结果:', res.data)
    if (res.data.code === 200) {
      const data = res.data.data
      prefetchedDict.value[key] = data
      // 试卷ID优先取 data.exam.id，其次顶层 id，最后取题目上的 examId
      examId.value = (data?.exam && (data.exam.id || data.exam.examId)) || data.id || (Array.isArray(data.questions) ? data.questions[0]?.examId : null) || null
      const curKey = `${props.courseId}-${currentIndex.value}`
      if (key === curKey)
        prefetchedExam.value = data
    }
  } catch (e) {
    console.error('获取题目失败', e)
  } finally {
     prefetchingKeys.delete(`${props.courseId}-${currentIndex.value}`)
  }
}

function showQuestionFromPool(idx) {
  const list = Array.isArray(prefetchedExam.value?.questions)
      ? prefetchedExam.value.questions
      : (Array.isArray(prefetchedExam.value?.choices) ? prefetchedExam.value.choices : [])
  const q = list[idx] || null
  if (q) {
    const stem = q.content
    const opts = normalizeOptions(q)
    const correct = (typeof q.correctIndex === 'number') ? q.correctIndex : (['A', 'B', 'C', 'D'].indexOf(String(q.answer || '').toUpperCase()))
    const qid = q.id || q.questionId || null
    showQuestion('选择题', stem, opts, Number.isFinite(correct) ? correct : 0, q.analysis || '', qid)
  } else {

  }
}

function showQuestion(title, stem, options, correctIndex, analysis, qid) {
  try {
    questionTitle.value = title
    questionStem.value = stem
    questionOptions.value = (Array.isArray(options) && options.length === 4) ? options : ['选项A', '选项B', '选项C', '选项D']
    questionCorrectIndex.value = Number.isFinite(correctIndex) ? correctIndex : 0
    questionAnalysis.value = String(analysis || '')
    currentQuestionId.value = qid
    const el = player.value
    if (el && !el.paused) {
      el.pause()
      isPlaying.value = false
      wasPlayingBeforeQuestion.value = true
    } else {
      wasPlayingBeforeQuestion.value = false
    }
    questionVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

// 解析后端选项，兼容数组/字符串/分隔形式，并去掉前缀“A./A、/A ”
function normalizeOptions(q) {
  try {
    const stripLabel = (s) => String(s || '')
      .replace(/^\s*[A-Da-d][\.|、\s]\s*/, '')
      .trim()
    let arr = []
    if (Array.isArray(q?.options)) {
      arr = q.options
    } else if (typeof q?.options === 'string') {
      let s = q.options.trim()
      // 尝试 JSON 化：把单引号转双引号
      if (/^\[.*\]$/.test(s)) {
        try { arr = JSON.parse(s.replace(/'/g, '"')) } catch {}
      }
      if (!Array.isArray(arr) || arr.length === 0) {
        // 退化：按逗号/顿号/分号拆分
        arr = s.split(/[，,；;\n]/).map(x => x.trim()).filter(Boolean)
      }
    } else {
      arr = [q?.a, q?.b, q?.c, q?.d].filter(Boolean)
    }
    // 仅保留前四项，并去标签
    const cleaned = (arr || []).slice(0, 4).map(stripLabel)
    if (cleaned.length === 4) return cleaned
  } catch (e) { console.error('解析选项失败', e) }
  // 兜底
  return ['是', '否', '不确定', '无法判断']
}

function onQuestionSubmit(payload) {
  try {
    // 保持弹窗开启，先展示正确答案与解析；仅在用户点击“继续学习”关闭
    // 记录答案并上报
    const idx = Number(payload?.answerIndex)
    const letter = ['A','B','C','D'][Math.max(0, Math.min(3, Number.isFinite(idx) ? idx : 0))]
    const qid = currentQuestionId.value
    if (qid) {
      const existing = (answersSoFar.value).findIndex(a => a.questionId === qid)
      if (existing >= 0) answersSoFar.value.splice(existing, 1, { questionId: qid, answer: letter })
      else answersSoFar.value.push({ questionId: qid, answer: letter })
      submitAnswers()
    }
  } catch (e) { console.error(e) }
}

async function submitAnswers() {
  try {
    const studentId = localStorage.getItem('userId')
    const examId2 = examId.value
    if (!studentId || !examId2) return
    const token = localStorage.getItem('token')
    const body = {
      examId: examId2,
      studentId: studentId,
      answers: (answersSoFar.value).map(a => ({ questionId: a.questionId, answer: a.answer }))
    }
    const res = await axios.post(`${BASE_URL}/aiexam/submit`, body, {headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json'
    }})
    if (res.data.code === 200) {
      console.log('提交答案成功')
    }
  } catch (e) {
    console.error('提交答案失败', e)
  }
}
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

.item-inner {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.video-icon {
  color: #10b981;
  font-size: 14px;
}

.item-title {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
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

.time-hud {
  position: absolute;
  left: 8px;
  bottom: 42px;
  color: #fff;
  font-size: 13px;
  text-shadow: 0 1px 2px rgba(0,0,0,0.6);
  background: rgba(0,0,0,0.35);
  padding: 2px 6px;
  border-radius: 4px;
  pointer-events: none;
}

.mini-progress {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 4px;
  background: rgba(255,255,255,0.25);
  pointer-events: none;
}
.mini-progress-fill {
  height: 100%;
  background: #10b981;
  transition: width 0.2s ease;
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

.controls-track-dot {
  position: absolute;
  top: 50%;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 2px 6px rgba(0,0,0,0.35);
  transform: translate(-50%, -50%);
  cursor: pointer;
}

.overlay-track-dot {
  position: absolute;
  top: 50%;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 2px 6px rgba(0,0,0,0.45);
  transform: translate(-50%, -50%);
  cursor: pointer;
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

.right-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.volume {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(255,255,255,0.85);
  padding: 4px 8px;
  border-radius: 8px;
}
.volume input[type='range'] {
  width: 120px;
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
  pointer-events: none;
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

</style>

