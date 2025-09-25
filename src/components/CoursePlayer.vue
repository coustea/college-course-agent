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
            <span class="overall-badge" :title="'本课程整体学习进度'">
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
            <video ref="player" class="video-player" playsinline :src="currentSrc" crossorigin="anonymous"
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
      v-model="questionVisible"
      :questions="questionList"
      title="知识检查"
      :closable="false"
      :requireAll="true"
      @submit="onQuestionSubmit"
  />
</template>

<script setup>
import { ref, watch, computed, onMounted, onBeforeUnmount } from 'vue'
import { getVideoProgress, setVideoProgress, getOverallProgress, reportLearningHeartbeat } from '../services/progressApi'
import { fetchQuestions, submitExamAnswers, hasQuestionShown, markQuestionShown } from '@/services/questionApi'
import Question from '@/components/Question.vue'


const props = defineProps({
  modelValue: { type: Boolean, default: false },
  courseId: { type: [String, Number], required: true },
  title: { type: String, required: true },
  chapters: { type: Array, default: () => [] },
  fallbackSrc: { type: String, default: '' },
  startIndex: { type: Number, default: 0 },
  episodeTotal: { type: Number, default: 0 },
  videoCount: { type: Number, default: 0 },
  docCount: { type: Number, default: 0 }
})
const emit = defineEmits(['update:modelValue', 'progress'])

const visible = ref(false)
watch(() => props.modelValue, v => { visible.value = v })
function close() {
  if (isTheatre.value) {
    isTheatre.value = false
  }
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
      if (item.videoUrl || item.type === 'video' || item.type === 'chapter') {
        result.push(item)
      } else if (item.children && Array.isArray(item.children)) {
        process(item.children)
      }
    })
  }
  process(props.chapters || [])
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

function onVideoError(e) {
  console.warn('视频加载失败，回退到 fallbackSrc', e)
  const el = player.value
  if (!el) return
  if (props.fallbackSrc && el.src !== props.fallbackSrc) {
    el.src = props.fallbackSrc
    el.load()
    el.play?.()
  }
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
const overallProgress = computed(() => {
  const v = Number(getOverallProgress(props.courseId))
  if (!Number.isFinite(v)) return 0
  return Math.max(0, Math.min(1, v))
})


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
  } catch {}
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
  } catch {}
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
      seekBy(5) // 受限：不允许超过当前进度
      return
    }
  } catch {}
}

function seekBy(deltaSec) {
  const el = player.value
  if (!el || !el.duration) return
  const now = el.currentTime || 0
  const target = now + deltaSec
  el.currentTime = Math.max(0, Math.min(el.duration, target))
}

let timeTicker = null
let lastTick = 0
let accumulatedHeartbeatSec = 0
const questionVisible = ref(false)
const questionList = ref([])
const questionNodeKey = ref('')
const wasPlayingBeforeQuestion = ref(false)

function pauseForQuestion() {
  const el = player.value
  if (!el) return
  wasPlayingBeforeQuestion.value = !el.paused
  if (!el.paused) {
    el.pause()
    isPlaying.value = false
  }
}

function resumeAfterQuestion() {
  const el = player.value
  if (!el) return
  if (wasPlayingBeforeQuestion.value) {
    el.play()
    isPlaying.value = true
  }
}
function togglePlay() {
  const el = player.value
  if (!el) return
  if (el.paused) {
    el.play()
    isPlaying.value = true
  } else {
    el.pause()
    isPlaying.value = false
  }

}

function startHeartbeatTicker() {
  lastTick = Date.now()
  if (!timeTicker) {
    timeTicker = setInterval(() => {
      const now = Date.now()
      const delta = Math.floor((now - lastTick) / 1000)
      if (delta > 0) {
        accumulatedHeartbeatSec += delta
        lastTick = now
        if (accumulatedHeartbeatSec >= 5) {
          try { reportLearningHeartbeat({ courseId: props.courseId, deltaSec: accumulatedHeartbeatSec, eventType: 'heartbeat', videoIndex: currentIndex.value, currentTimeSec: player.value?.currentTime, durationSec: player.value?.duration }) } catch {}
          accumulatedHeartbeatSec = 0
        }
      }
    }, 1000)
  }
}

function flushHeartbeat(eventType) {
  const now = Date.now()
  const delta = Math.floor((now - lastTick) / 1000)
  if (delta > 0) {
    accumulatedHeartbeatSec += delta
    lastTick = now
  }
  if (accumulatedHeartbeatSec > 0) {
    try {
      reportLearningHeartbeat({
        courseId: props.courseId,
        deltaSec: accumulatedHeartbeatSec,
        eventType: eventType || 'pause',
        videoIndex: currentIndex.value,
        currentTimeSec: player.value?.currentTime,
        durationSec: player.value?.duration
      }) } catch {}
    accumulatedHeartbeatSec = 0
  }
}

function stopTickerAndFlush(eventType) {
  if (timeTicker) {
    clearInterval(timeTicker)
    timeTicker = null
  }
  flushHeartbeat(eventType)
}

function syncPlayState() {
  const el = player.value
  if (!el) return
  const nowPlaying = !el.paused
  if (nowPlaying && !timeTicker) {
    startHeartbeatTicker()
  } else if (!nowPlaying && timeTicker) {
    stopTickerAndFlush('pause')
  }
  isPlaying.value = nowPlaying
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

const durationLabel = computed(() => {
  const el = player.value
  const d = Math.floor(el?.duration || 0)
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

// 每个视频单独判断展示
const backendDurationSec = computed(() => {
  const currentChapter = flatChapters.value[currentIndex.value]
  return parseDurationSec(currentChapter)
})
const hudDurationLabel = computed(() => {
  const el = player.value
  const run = Math.floor(el?.duration || 0)
  const base = Math.max(run, backendDurationSec.value)
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

const bubbleLeft = computed(() => {
  const pct = Math.round((currentProgress.value || 0) * 100)
  return Math.min(98, Math.max(2, pct))
})

const progressDotStyle = computed(() => {
  const pct = Math.max(0, Math.min(1, currentProgress.value || 0))
  const deg = Math.round(pct * 360)
  return {
    background: `conic-gradient(#10b981 ${deg}deg, rgba(255,255,255,0.18) 0)`
  }
})

const progressTrack = ref(null)
const overlayTrack = ref(null)
const hoverTimeVisible = ref(false)
const hoverLeft = ref(0)
const hoverTimeLabel = ref('0:00')
const volumePercent = ref(100)
const isDragging = ref(false)
const draggingWhich = ref('')

function startDragging(which) {
  isDragging.value = true
  draggingWhich.value = which
  try {
    document.addEventListener('mousemove', onDragMove)
    document.addEventListener('mouseup', stopDragging)
  } catch {}
}

function onDragMove(e) {
  if (!isDragging.value) return
  const video = player.value
  const el = draggingWhich.value === 'overlay' ? overlayTrack.value : progressTrack.value
  if (!el || !video || !video.duration) return
  const rect = el.getBoundingClientRect()
  const x = Math.min(Math.max(e.clientX - rect.left, 0), rect.width)
  const ratio = x / rect.width
  video.currentTime = ratio * video.duration
  currentProgress.value = ratio
}

function stopDragging() {
  isDragging.value = false
  draggingWhich.value = ''
  try {
    document.removeEventListener('mousemove', onDragMove)
    document.removeEventListener('mouseup', stopDragging)
  } catch {}
}

function formatTime(sec) {
  const t = Math.max(0, Math.floor(sec))
  const m = Math.floor(t / 60)
  const s = String(t % 60).padStart(2, '0')
  return `${m}:${s}`
}

function applyVolume() {
  const el = player.value
  if (!el) return
  const v = Math.max(0, Math.min(100, Number(volumePercent.value) || 0))
  el.muted = v === 0
  el.volume = v / 100
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

function onTrackClick(e, which) {
  const el = which === 'overlay' ? overlayTrack.value : progressTrack.value
  const video = player.value
  if (!el || !video || !video.duration) return
  const rect = el.getBoundingClientRect()
  const x = Math.min(Math.max(e.clientX - rect.left, 0), rect.width)
  const ratio = x / rect.width
  video.currentTime = ratio * video.duration
}

function onTrackDown(e, which) {
  onTrackClick(e, which)
  startDragging(which)
}

function onLoaded() {
  const p = getVideoProgress(props.courseId, currentIndex.value)
  if (p > 0 && player.value?.duration) {
    player.value.currentTime = p * player.value.duration
  } else {
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
    } catch {}
  }
  try {
    if (!hudTicker) {
      hudTicker = setInterval(() => { hudNow.value = Date.now() }, 100)
    }
  } catch {}
}

function onEnded() {
  isPlaying.value = false
  stopTickerAndFlush('ended')
}

function onSeeked() {
  stopTickerAndFlush('seek')
  startHeartbeatTicker()
}

async function onTimeUpdate() {
  if (!player.value?.duration) return
  const progress = player.value.currentTime / player.value.duration
  currentProgress.value = progress
  setVideoProgress(props.courseId, currentIndex.value, progress)
  // 本地持久化，保证退出回来继续播放
  try {
    const key = `video_resume_${props.courseId}_${currentIndex.value}`
    localStorage.setItem(key, JSON.stringify({ p: progress, t: Date.now() }))
  } catch {}
  emit('progress', overallProgress.value)
  try { window.dispatchEvent(new CustomEvent('learning-progress-updated', { detail: { courseId: props.courseId } })) } catch {}
  // 题目触发：视频在 40% 与 80% 位置各触发一次；避免同时弹多个
  try {
    if (questionVisible.value) return
    const isSingle = !hasChapters.value || flatChapters.value.length === 0
    const baseKey = isSingle ? 'single' : `idx-${currentIndex.value}`
    const key40 = `${baseKey}-p40`
    const key80 = `${baseKey}-p80`

    let targetKey = ''
    if (progress >= 0.4 && !hasQuestionShown(props.courseId, key40)) {
      targetKey = key40
    } else if (progress >= 0.8 && !hasQuestionShown(props.courseId, key80)) {
      targetKey = key80
    }
    if (targetKey) {
      markQuestionShown(props.courseId, targetKey)
      const qs = await fetchQuestions(props.courseId, targetKey)
      if (Array.isArray(qs) && qs.length) {
        questionNodeKey.value = targetKey
        questionList.value = qs
        pauseForQuestion()
        questionVisible.value = true
      }
    }
  } catch {}
}

async function onQuestionSubmit(payload) {
  try {
    const answers = payload?.answers || {}
    const nodeKey = questionNodeKey.value
    const questions = questionList.value || []
    await submitExamAnswers(props.courseId, nodeKey, questions, answers)
  } catch {}
  questionVisible.value = false
  resumeAfterQuestion()
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

function next() {
  if (currentIndex.value < totalCount.value - 1) {
    if (hasChapters.value) {
      playChapter(currentIndex.value + 1)
    } else {
      selectEpisode(currentIndex.value + 1)
    }
  }
}

function nextTickSeekSaved() {
  requestAnimationFrame(() => {
    const p = getVideoProgress(props.courseId, currentIndex.value)
    if (player.value?.duration && p > 0) {
      player.value.currentTime = p * player.value.duration
    } else {
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
      } catch {}
    }
  })
}

onMounted(() => {
  visible.value = props.modelValue
  if (!hasChapters.value) {
    showEnterTip(props.title)
  }
  try { window.addEventListener('keydown', handleKeydown) } catch {}
})
watch(currentIndex, (v) => {
  if (v != null && totalCount.value > 0) {
    const t = hasChapters.value ? (flatChapters.value[v]?.title || `第${v + 1}集`) : `第${v + 1}集`
    showEnterTip(t)
  }
}, { immediate: true })

watch(visible, (v) => {
  if (!v) {
    lockScroll(false)
  }
})

onBeforeUnmount(() => {
  lockScroll(false)
  stopTickerAndFlush('ended')
  try { window.removeEventListener('keydown', handleKeydown) } catch {}
  stopDragging()
  try { if (hudTicker) { clearInterval(hudTicker); hudTicker = null } } catch {}
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
}

.mini-progress {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 4px;
  background: rgba(255,255,255,0.25);
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

