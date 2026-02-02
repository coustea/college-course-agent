<template>
  <transition name="fade">
    <div v-if="visible" class="course-player-fullscreen" @mousemove="onMouseMove" @mouseleave="onMouseLeave">
      <!-- 顶部导航栏 (鼠标悬停或暂停时显示) -->
      <transition name="slide-down">
        <div v-show="controlsVisible || !isPlaying" class="cp-header">
          <div class="cp-back" @click="close" title="退出播放">
            <i class="fas fa-arrow-left"></i>
            <span class="cp-title">{{ title }}</span>
          </div>
          <div class="cp-header-actions">
            <!-- 可以在这里添加更多顶部操作 -->
          </div>
        </div>
      </transition>

      <div class="cp-body">
        <!-- 视频主区域 -->
        <div class="cp-main" :class="{ 'sidebar-collapsed': isSidebarCollapsed }">
          <div class="video-wrapper" ref="videoContainer" @click="togglePlay" @dblclick="toggleTheatre">
            <video
              ref="player"
              class="video-element"
              playsinline
              preload="metadata"
              :src="currentSrc"
              @timeupdate="onTimeUpdate(); syncPlayState()"
              @loadedmetadata="onLoaded"
              @play="syncPlayState"
              @pause="syncPlayState"
              @ended="onEnded"
              @seeked="onSeeked"
              @error="onVideoError"
              @waiting="isBuffering = true"
              @canplay="isBuffering = false"
            >
              您的浏览器不支持HTML5视频播放
            </video>

            <!-- 加载中提示 -->
            <div v-if="isBuffering" class="cp-loading">
              <i class="fas fa-spinner fa-spin"></i>
            </div>

            <!-- 播放/暂停 大图标覆盖 -->
            <div v-if="!isPlaying && !isBuffering" class="cp-play-overlay">
              <div class="play-btn-big"><i class="fas fa-play"></i></div>
            </div>
            
            <!-- 提示信息 -->
            <transition name="fade">
              <div v-show="enterTipVisible" class="cp-toast">{{ enterTipText }}</div>
            </transition>

            <!-- 底部控制栏 -->
            <transition name="slide-up">
              <div v-show="controlsVisible || !isPlaying" class="cp-controls" @click.stop>
                <!-- 进度条 -->
                <div 
                  class="cp-progress-container"
                  ref="progressTrack"
                  @mousemove="onTrackMove"
                  @mousedown="onTrackDown($event, 'controls')"
                  @click="onTrackClick($event, 'controls')"
                >
                  <div class="cp-progress-rail"></div>
                  <div class="cp-progress-loaded" :style="{ width: (bufferedRatio * 100) + '%' }"></div>
                  <div class="cp-progress-fill" :style="{ width: (currentProgress * 100) + '%' }"></div>
                  <div class="cp-progress-handle" :style="{ left: (currentProgress * 100) + '%' }"></div>
                  <!-- 悬浮时间提示 -->
                  <div v-show="hoverTimeVisible" class="cp-hover-time" :style="{ left: hoverLeft + '%' }">
                    {{ hoverTimeLabel }}
                  </div>
                </div>

                <div class="cp-controls-row">
                  <div class="cp-controls-left">
                    <button class="cp-btn" @click="togglePlay" :title="isPlaying ? '暂停' : '播放'">
                      <i class="fas" :class="isPlaying ? 'fa-pause' : 'fa-play'"></i>
                    </button>
                    <button class="cp-btn" @click="next" :disabled="currentIndex >= totalCount - 1" title="下一集">
                      <i class="fas fa-step-forward"></i>
                    </button>
                    <div class="cp-time">
                      {{ currentTimeLabel }} / {{ durationLabel }}
                    </div>
                  </div>

                  <div class="cp-controls-right">
                    <!-- 下一步/完成 -->
                    <button
                      v-if="currentIndex === totalCount - 1 && currentProgress > 0.9"
                      class="cp-btn cp-btn-primary"
                      @click="close"
                    >
                      完成学习
                    </button>

                    <!-- 音量 -->
                    <div class="cp-volume-wrap">
                      <button class="cp-btn" @click="toggleMute">
                        <i class="fas" :class="volumeIcon"></i>
                      </button>
                      <div class="cp-volume-slider">
                        <input type="range" min="0" max="100" v-model.number="volumePercent" @input="applyVolume" />
                      </div>
                    </div>

                    <!-- 侧边栏开关 -->
                    <button class="cp-btn" @click="toggleSidebar" :title="isSidebarCollapsed ? '展开目录' : '收起目录'" :class="{ active: !isSidebarCollapsed }">
                      <i class="fas fa-list-ul"></i>
                    </button>
                  </div>
                </div>
              </div>
            </transition>
          </div>
        </div>

        <!-- 右侧侧边栏 (目录) -->
        <div class="cp-sidebar" :class="{ 'collapsed': isSidebarCollapsed }">
          <div class="cp-sidebar-header">
            <h3 class="cp-sidebar-title">课程目录</h3>
            <span class="cp-progress-badge">{{ Math.round(overallProgress * 100) }}% 已学</span>
          </div>
          
          <div class="cp-chapter-list">
            <div
              v-for="(item, idx) in flatChapters"
              :key="idx"
              class="cp-chapter-item"
              :class="{ 'active': idx === currentIndex, 'played': isChapterPlayed(idx), 'completed': isChapterCompleted(idx) }"
              @click="selectEpisode(idx)"
            >
              <div class="cp-chapter-idx">
                <span v-if="isChapterCompleted(idx)" class="completed-badge"><i class="fas fa-check"></i></span>
                <span v-else>{{ String(idx + 1).padStart(2, '0') }}</span>
              </div>
              <div class="cp-chapter-info">
                <div class="cp-chapter-title" :title="item.title">
                  {{ item.title || `第 ${idx + 1} 节` }}
                  <i v-if="isChapterCompleted(idx)" class="fas fa-check-circle completed-icon" title="已完成"></i>
                </div>
                <div class="cp-chapter-meta">
                  <span class="cp-chapter-duration">{{ formatTime(parseDurationSec(item)) }}</span>
                  <i v-if="idx === currentIndex" class="fas fa-chart-bar anim-playing"></i>
                  <span v-if="isChapterCompleted(idx)" class="completed-text">已完成</span>
                </div>
              </div>
            </div>

            <div v-if="flatChapters.length === 0" class="cp-empty-chapter">
              暂无目录
            </div>
          </div>
        </div>
      </div>
    </div>
  </transition>

  <!-- 答题弹窗 -->
  <Question
      v-if="visible && enableQuestions"
      v-model="questionVisible"
      :title="questionTitle"
      :stem="questionStem"
      :options="questionOptions"
      :correct-index="questionCorrectIndex"
      :analysis="questionAnalysis"
      :next-text="questionNextText"
      @submit="onQuestionSubmit"
  />
</template>

<script setup>
import { ref, watch, computed, onMounted, onBeforeUnmount, getCurrentInstance, nextTick } from 'vue'
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
const isSidebarCollapsed = ref(false)
const controlsVisible = ref(true)
let controlsTimer = null
const isBuffering = ref(false)

watch(() => props.modelValue, v => { visible.value = v })

function toggleSidebar() {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
}

function onMouseMove() {
  controlsVisible.value = true
  if (controlsTimer) clearTimeout(controlsTimer)
  if (isPlaying.value) {
    controlsTimer = setTimeout(() => {
      controlsVisible.value = false
    }, 3000)
  }
}

function onMouseLeave() {
  if (isPlaying.value) {
    controlsVisible.value = false
  }
}

async function close() {
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
// 已完成的视频ID集合
const completedVideos = ref(new Set())
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

// 检查章节是否已完成
function isChapterCompleted(idx) {
  const ch = flatChapters.value?.[idx]
  if (!ch) return false
  const videoId = ch?.videoId ?? ch?.id ?? ch?.videoIndex ?? ch?.index ?? (idx + 1)
  return completedVideos.value.has(videoId)
}

// 检查章节是否已播放过（可能有进度但未完成）
function isChapterPlayed(idx) {
  const ch = flatChapters.value?.[idx]
  if (!ch) return false
  const videoId = ch?.videoId ?? ch?.id ?? ch?.videoIndex ?? ch?.index ?? (idx + 1)
  // 已完成肯定是已播放
  if (completedVideos.value.has(videoId)) return true
  // 可以从localStorage或其他状态检查是否有观看记录（使用 videoId 作为键）
  const key = `video_resume_${props.courseId}_${videoId}`
  try {
    const raw = localStorage.getItem(key)
    return raw && JSON.parse(raw)?.p > 0.05 // 观看超过5%视为已播放
  } catch {
    return false
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

      // 从后端数据中更新已完成视频集合
      if (Array.isArray(data?.videos)) {
        completedVideos.value.clear()
        data.videos.forEach(v => {
          if (v.completed === true || v.completed === 'true') {
            const vid = v.videoId ?? v.id ?? v.videoIndex
            if (vid) {
              completedVideos.value.add(vid)
              console.log('[CoursePlayer] 已完成的视频:', vid)
            }
          }
        })
      }

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

      // 将后端进度写入本地恢复键（使用 videoId 而不是 currentIndex）
      try {
        const pct = Math.max(0, Math.min(100, Number(percentage || 0))) / 100
        const ch = flatChapters.value?.[currentIndex.value]
        const videoId = ch?.videoId ?? ch?.id ?? ch?.videoIndex ?? currentIndex.value
        const key = `video_resume_${props.courseId}_${videoId}`
        localStorage.setItem(key, JSON.stringify({ p: pct, t: Date.now() }))
        console.log('[CoursePlayer] 保存进度到 localStorage:', key, '进度:', pct)

        // 如果视频已经加载，立即恢复进度
        if (player.value?.duration && pct > 0 && pct < 1) {
          const targetTime = pct * player.value.duration
          // 只有当当前时间与目标时间相差较大时才跳转（避免重复跳转）
          if (Math.abs(player.value.currentTime - targetTime) > 2) {
            console.log('[CoursePlayer] 从后端恢复视频位置:', targetTime, '秒')
            player.value.currentTime = targetTime
          }
        }
      } catch (e) { console.error(e) }
    }
  } catch (e) {
    console.error('[CoursePlayer] 获取课程进度失败:', e)
  }
}

// 观看时长统计与上报
const lastPlayRealStartMs = ref(0)
const unreportedWatchedSec = ref(0)
async function reportCourseProgress(deltaSec, completed = false) {
  const sec = Math.max(0, Math.floor(Number(deltaSec)))
  if (sec <= 0 && !completed) return

  // 检查用户角色，教师不上报进度
  const userRole = localStorage.getItem('userRole')
  if (userRole === 'teacher') {
    console.log('[CoursePlayer] 当前用户是教师，跳过进度上报')
    return
  }

  const studentId = localStorage.getItem('userId')
  const ch = flatChapters.value?.[currentIndex.value]
  const courseId = props.courseId || ch?.courseId || flatChapters.value?.[0]?.courseId
  const videoId = ch?.videoId ?? ch?.id ?? ch?.videoIndex ?? ch?.index ?? (currentIndex.value + 1)
  if (!studentId || !courseId || !videoId) return
  try {
    console.log('[CoursePlayer] 观看时长上报开始(展示所需参数)', studentId, courseId, videoId, sec, 'completed:', completed)
    const token = localStorage.getItem('token')
    const res = await axios.post(`${BASE_URL}/progress/report`,null, {
      params: { studentId, courseId, videoId, deltaSec: sec, completed: completed },
      headers:  { Authorization: `Bearer ${token}` }
    })
    console.log('[CoursePlayer] 观看时长上报结果', res.data)
    if (res?.data?.code === 200) {
      console.log('观看时长上报成功')
      // 如果标记为完成，添加到已完成集合
      if (completed) {
        completedVideos.value.add(videoId)
        console.log('[CoursePlayer] 视频已完成并标记:', videoId)
      }
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

// 缓冲状态
const bufferedRatio = ref(0)


const isTheatre = ref(false)
const isPlaying = ref(false)
const overlayVisible = ref(true)
const enterTipVisible = ref(false)
const enterTipText = ref('')
let enterTipTimer = null
function showEnterTip(title) {
  enterTipText.value = `正在播放：${title}`
  enterTipVisible.value = true
  if (enterTipTimer) clearTimeout(enterTipTimer)
  enterTipTimer = setTimeout(() => { enterTipVisible.value = false }, 2000)
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

// 音量图标
const volumeIcon = computed(() => {
  const v = volumePercent.value
  if (v === 0) return 'fa-volume-mute'
  if (v < 50) return 'fa-volume-down'
  return 'fa-volume-up'
})

// 切换静音
let lastVolume = 100
function toggleMute() {
  if (volumePercent.value > 0) {
    lastVolume = volumePercent.value
    volumePercent.value = 0
  } else {
    volumePercent.value = lastVolume || 100
  }
  applyVolume()
}

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
      // 使用 videoId 而不是 currentIndex 作为键
      const ch = flatChapters.value?.[currentIndex.value]
      const videoId = ch?.videoId ?? ch?.id ?? ch?.videoIndex ?? currentIndex.value
      const key = `video_resume_${props.courseId}_${videoId}`
      const raw = localStorage.getItem(key)
      if (raw && player.value?.duration) {
        const saved = JSON.parse(raw)
        const pct = Number(saved?.p)
        if (Number.isFinite(pct) && pct > 0 && pct < 1) {
          player.value.currentTime = pct * player.value.duration
          console.log('[CoursePlayer] 从 localStorage 恢复视频位置:', videoId, '进度:', pct)
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
    // 预取新视频的题目（内部会尝试恢复状态，如果无状态则会重置）
    prefetchQuestions()
  }
})

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
    // Don't restore progress here - let fetchOverallProgress handle it
    // This prevents race conditions where localStorage might have stale data
    console.log('[CoursePlayer] Video loaded, waiting for backend progress data')
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
  // 视频播放完成，上报完成状态
  reportAndReset()
  // 标记当前视频为已完成
  const ch = flatChapters.value?.[currentIndex.value]
  const videoId = ch?.videoId ?? ch?.id ?? ch?.videoIndex ?? ch?.index ?? (currentIndex.value + 1)
  console.log('[CoursePlayer] 视频播放完成，标记为已完成:', videoId)
  // 上报完成状态
  reportCourseProgress(0, true)
}

function onSeeked() {
  stopTickerAndFlush()
  startHeartbeatTicker()
  try { lastPlayRealStartMs.value = Date.now() } catch (e) { console.error(e) }
}

function onTimeUpdate() {
  if (!player.value?.duration) return
  const cur = player.value.currentTime
  const dur = player.value.duration
  const progress = cur / dur
  currentProgress.value = progress

  // 更新缓冲进度
  if (player.value.buffered.length > 0) {
    for (let i = 0; i < player.value.buffered.length; i++) {
      if (player.value.buffered.start(i) <= cur && player.value.buffered.end(i) >= cur) {
        bufferedRatio.value = player.value.buffered.end(i) / dur
        break
      }
    }
  }
  // 进度触发 40% / 80% 弹题
  maybeAskByProgress(progress)
  // 保存进度到 localStorage（使用 videoId 而不是 currentIndex）
  try {
    const ch = flatChapters.value?.[currentIndex.value]
    const videoId = ch?.videoId ?? ch?.id ?? ch?.videoIndex ?? currentIndex.value
    const key = `video_resume_${props.courseId}_${videoId}`
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
const questionNextText = ref('继续学习')
const examId = ref(null)
const currentQuestionId = ref(null)
const answersSoFar = ref([])
// 每个视频需要回答的题目总数
const REQUIRED_QUESTIONS_PER_VIDEO = 3
// 当前视频已回答的题目数量
const answeredCount = ref(0)
// 当前视频已弹出的题目索引集合
const askedQuestionIndexes = ref(new Set())
// 随机触发的时间点（进度百分比 0-1）
const randomTriggerPoints = ref([])
const prefetchedExam = ref(null)
const prefetchedDict = ref({})
const prefetchingKeys = new Set()
// 当前显示的题目索引
const currentQuestionIndex = ref(0)

function openQuestions() {
  // 打开试题列表或当前试题
  startQuiz()
}

// 开始答题（获取题目）
async function startQuiz() {
  try {
    const courseId = props.courseId || flatChapters.value?.[0]?.courseId
    const videoId = flatChapters.value?.[currentIndex.value]?.videoId ?? (currentIndex.value + 1)
    
    // 如果已经预取了，直接显示
    const key = `${props.courseId}-${currentIndex.value}`
    if (prefetchedExam.value) {
       showQuestionFromPool(0)
       questionVisible.value = true
       return
    }
    
    // 否则尝试获取
    await prefetchQuestions()
    if (prefetchedExam.value) {
       showQuestionFromPool(0)
    }
    questionVisible.value = true
  } catch (e) {
    console.error('开始答题失败:', e)
    questionVisible.value = true
  }
}

function maybeAskByProgress(p) {
  if (!props.enableQuestions) return
  try {
    const pct = Number(p)
    if (!Number.isFinite(pct) || pct <= 0 || pct >= 0.95) return

    // 初始化随机触发点（只执行一次）
    if (randomTriggerPoints.value.length === 0) {
      generateRandomTriggerPoints()
    }

    // 检查是否到达触发点
    for (let i = 0; i < randomTriggerPoints.value.length; i++) {
      const triggerPoint = randomTriggerPoints.value[i]
      if (pct >= triggerPoint && !askedQuestionIndexes.value.has(i)) {
        askedQuestionIndexes.value.add(i)
        if (!prefetchedExam.value) {
          prefetchQuestions().finally(() => { showNextQuestion() })
        } else {
          showNextQuestion()
        }
        break
      }
    }
  } catch (e) { console.error(e) }
}

// 生成随机触发点（在15%-75%之间随机分布3个点）
function generateRandomTriggerPoints() {
  const points = []
  // 在 15%, 40%, 65% 附近随机生成
  const basePoints = [0.15, 0.40, 0.65]
  for (const base of basePoints) {
    // 在基准点附近 ±5% 的范围内随机
    const offset = (Math.random() - 0.5) * 0.10
    const point = Math.max(0.10, Math.min(0.80, base + offset))
    points.push(point)
  }
  // 排序确保按顺序触发
  points.sort((a, b) => a - b)
  randomTriggerPoints.value = points
  console.log('[CoursePlayer] 生成随机题目触发点:', points.map(p => Math.round(p * 100) + '%'))
}

// 显示下一道题目
function showNextQuestion() {
  const list = Array.isArray(prefetchedExam.value?.questions)
      ? prefetchedExam.value.questions
      : (Array.isArray(prefetchedExam.value?.choices) ? prefetchedExam.value.choices : [])

  if (!list || list.length === 0) {
    console.warn('[CoursePlayer] 没有可用的题目')
    return
  }

  // 找到还没显示的题目
  let nextIdx = -1
  for (let i = 0; i < Math.min(REQUIRED_QUESTIONS_PER_VIDEO, list.length); i++) {
    if (!askedQuestionIndexes.value.has(`q_${i}`)) {
      nextIdx = i
      break
    }
  }

  // 如果所有题目都已显示，使用第一题
  if (nextIdx === -1 && askedQuestionIndexes.value.size < REQUIRED_QUESTIONS_PER_VIDEO) {
    nextIdx = askedQuestionIndexes.value.size % list.length
  }

  if (nextIdx >= 0) {
    currentQuestionIndex.value = nextIdx
    showQuestionFromPool(nextIdx)
  }
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

    // 尝试恢复答题状态（如果存在且未过期）
    let hasRestoredState = false
    if (prefetchedExam.value) {
      hasRestoredState = restoreQuestionState()
      if (hasRestoredState) {
        console.log('[CoursePlayer] 已恢复答题状态，无需重新生成题目')
        return
      }
    }

    // 如果没有恢复到状态，重置为初始状态
    if (!hasRestoredState) {
      resetQuestionState()
    }

    // 随机选择题目方案
    const questionSchemes = [
      { choiceCount: 3, judgeCount: 0, description: '3道选择题' },
      { choiceCount: 2, judgeCount: 1, description: '2道选择题+1道判断题' }
    ]
    const selectedScheme = questionSchemes[Math.floor(Math.random() * questionSchemes.length)]
    console.log('[CoursePlayer] 本次题目方案:', selectedScheme.description)

    const body = {
      courseId,
      studentId,
      choiceCount: selectedScheme.choiceCount,
      judgeCount: selectedScheme.judgeCount,
      videoId,
      documentId: null
    }

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

      // 生成题目后，再次尝试恢复状态（防止题目重新生成时丢失状态）
      restoreQuestionState()
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

    // 计算答题进度
    const totalRequired = Math.min(REQUIRED_QUESTIONS_PER_VIDEO, list.length)
    const answered = askedQuestionIndexes.value.size
    const title = `选择题 (${answered}/${totalRequired})`

    // 检查是否已完成所有题目
    const isAllCompleted = answered >= totalRequired
    const nextText = isAllCompleted ? '继续学习' : '下一题'

    showQuestion(title, stem, opts, Number.isFinite(correct) ? correct : 0, q.analysis || '', qid, nextText)
  } else {
    console.warn('[CoursePlayer] 未找到题目，索引:', idx)
  }
}

function showQuestion(title, stem, options, correctIndex, analysis, qid, nextText = '继续学习') {
  try {
    // 先关闭弹窗（如果已经打开），以便重置组件状态
    if (questionVisible.value) {
      questionVisible.value = false
    }

    // 使用 nextTick 确保弹窗完全关闭后再显示新题目
    nextTick(() => {
      questionTitle.value = title
      questionStem.value = stem
      questionOptions.value = (Array.isArray(options) && options.length === 4) ? options : ['选项A', '选项B', '选项C', '选项D']
      questionCorrectIndex.value = Number.isFinite(correctIndex) ? correctIndex : 0
      questionAnalysis.value = String(analysis || '')
      questionNextText.value = nextText
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
    })
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
    // 记录答案
    const idx = Number(payload?.answerIndex)
    const letter = ['A','B','C','D'][Math.max(0, Math.min(3, Number.isFinite(idx) ? idx : 0))]
    const qid = currentQuestionId.value

    // 判断是否答错
    const isCorrect = idx === questionCorrectIndex.value

    if (qid) {
      const existing = (answersSoFar.value).findIndex(a => a.questionId === qid)
      if (existing >= 0) answersSoFar.value.splice(existing, 1, { questionId: qid, answer: letter })
      else answersSoFar.value.push({ questionId: qid, answer: letter })

      // 标记当前题目已回答（使用触发点的索引，不是题目索引）
      const triggerIndex = randomTriggerPoints.value.findIndex((_, i) => !askedQuestionIndexes.value.has(i))
      if (triggerIndex >= 0) {
        askedQuestionIndexes.value.add(triggerIndex)
      }
      answeredCount.value++

      // 如果答错了，添加到错题本
      if (!isCorrect) {
        addToWrongQuestionBook(qid, letter, ['A','B','C','D'][questionCorrectIndex.value])
      }

      // 上传答案到后端
      submitAnswers()

      console.log(`[CoursePlayer] 答题进度: ${answeredCount.value}/${REQUIRED_QUESTIONS_PER_VIDEO}, 答题${isCorrect ? '正确' : '错误'}`)

      // 检查是否已完成所有题目
      const allCompleted = answeredCount.value >= REQUIRED_QUESTIONS_PER_VIDEO
      if (allCompleted) {
        console.log('[CoursePlayer] 所有题目已完成，清除答题状态')
        clearQuestionState()
      } else {
        // 保存答题状态到 localStorage
        saveQuestionState()
      }

      // 答完当前题后，关闭弹窗继续观看视频
      // 等待下一个随机触发点再弹出下一题
      setTimeout(() => {
        questionVisible.value = false
        // 恢复视频播放（如果之前在播放）
        if (wasPlayingBeforeQuestion.value) {
          const el = player.value
          if (el) {
            el.play()
            isPlaying.value = true
            startWatchTimerIfNeeded()
          }
        }
      }, 2000) // 2秒后关闭弹窗，让学生看完解析
    }
  } catch (e) { console.error(e) }
}

// 添加错题到错题本
async function addToWrongQuestionBook(questionId, wrongAnswer, correctAnswer) {
  try {
    const studentId = localStorage.getItem('userId')
    const courseId = props.courseId
    const examIdValue = examId.value

    if (!studentId || !courseId || !questionId || !examIdValue) {
      console.warn('[CoursePlayer] 缺少必要参数，跳过添加错题本', { studentId, courseId, questionId, examId: examIdValue })
      return
    }

    const token = localStorage.getItem('token')
    console.log('[CoursePlayer] 添加错题到错题本:', { questionId, wrongAnswer, correctAnswer })

    const res = await axios.post(`${BASE_URL}/wrong-question/add`, null, {
      params: {
        studentId,
        questionId,
        examId: examIdValue,
        courseId,
        wrongAnswer,
        correctAnswer
      },
      headers: {
        Authorization: `Bearer ${token}`
      }
    })

    if (res.data.code === 200) {
      console.log('[CoursePlayer] 错题添加成功')
    } else {
      console.warn('[CoursePlayer] 错题添加失败:', res.data.message)
    }
  } catch (e) {
    console.error('[CoursePlayer] 添加错题到错题本失败:', e)
  }
}

// 查找下一道未回答的题目
function findNextUnansweredQuestion() {
  const list = Array.isArray(prefetchedExam.value?.questions)
      ? prefetchedExam.value.questions
      : (Array.isArray(prefetchedExam.value?.choices) ? prefetchedExam.value.choices : [])

  if (!list || list.length === 0) return -1

  for (let i = 0; i < Math.min(REQUIRED_QUESTIONS_PER_VIDEO, list.length); i++) {
    if (!askedQuestionIndexes.value.has(`q_${i}`)) {
      return i
    }
  }
  return -1
}

// 重置题目状态（切换视频时调用）
function resetQuestionState() {
  answeredCount.value = 0
  askedQuestionIndexes.value = new Set()
  randomTriggerPoints.value = []
  currentQuestionIndex.value = 0
  questionTitle.value = '选择题'
  questionVisible.value = false
  prefetchedExam.value = null
  console.log('[CoursePlayer] 重置题目状态')
}

// 保存答题状态到 localStorage
function saveQuestionState() {
  try {
    const ch = flatChapters.value?.[currentIndex.value]
    const videoId = ch?.videoId ?? ch?.id ?? ch?.videoIndex ?? currentIndex.value
    const key = `question_state_${props.courseId}_${videoId}`
    const state = {
      answeredCount: answeredCount.value,
      askedQuestionIndexes: Array.from(askedQuestionIndexes.value),
      randomTriggerPoints: randomTriggerPoints.value,
      answersSoFar: answersSoFar.value,
      timestamp: Date.now()
    }
    localStorage.setItem(key, JSON.stringify(state))
    console.log('[CoursePlayer] 保存答题状态:', key, state)
  } catch (e) {
    console.error('[CoursePlayer] 保存答题状态失败:', e)
  }
}

// 从 localStorage 恢复答题状态
function restoreQuestionState() {
  try {
    const ch = flatChapters.value?.[currentIndex.value]
    const videoId = ch?.videoId ?? ch?.id ?? ch?.videoIndex ?? currentIndex.value
    const key = `question_state_${props.courseId}_${videoId}`
    const raw = localStorage.getItem(key)
    if (raw) {
      const state = JSON.parse(raw)
      // 只恢复30分钟内的状态（避免太旧的数据）
      const age = Date.now() - (state.timestamp || 0)
      if (age < 30 * 60 * 1000) {
        answeredCount.value = state.answeredCount || 0
        askedQuestionIndexes.value = new Set(state.askedQuestionIndexes || [])
        randomTriggerPoints.value = state.randomTriggerPoints || []
        answersSoFar.value = state.answersSoFar || []
        console.log('[CoursePlayer] 恢复答题状态:', key, state)
        console.log('[CoursePlayer] 已回答题目数:', answeredCount.value)
        return true
      } else {
        console.log('[CoursePlayer] 答题状态已过期，忽略')
        localStorage.removeItem(key)
      }
    }
    return false
  } catch (e) {
    console.error('[CoursePlayer] 恢复答题状态失败:', e)
    return false
  }
}

// 清除答题状态
function clearQuestionState() {
  try {
    const ch = flatChapters.value?.[currentIndex.value]
    const videoId = ch?.videoId ?? ch?.id ?? ch?.videoIndex ?? currentIndex.value
    const key = `question_state_${props.courseId}_${videoId}`
    localStorage.removeItem(key)
    console.log('[CoursePlayer] 清除答题状态:', key)
  } catch (e) {
    console.error('[CoursePlayer] 清除答题状态失败:', e)
  }
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
/* 全屏播放器样式 */
.course-player-fullscreen {
  position: fixed;
  inset: 0;
  background: #000;
  z-index: 2000;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  color: #fff;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.slide-down-enter-active, .slide-down-leave-active { transition: transform 0.3s ease; }
.slide-down-enter-from, .slide-down-leave-to { transform: translateY(-100%); }

.slide-up-enter-active, .slide-up-leave-active { transition: transform 0.3s ease; }
.slide-up-enter-from, .slide-up-leave-to { transform: translateY(100%); }

/* 顶部导航 */
.cp-header {
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 60px;
  background: linear-gradient(180deg, rgba(0,0,0,0.8) 0%, rgba(0,0,0,0) 100%);
  display: flex;
  align-items: center;
  padding: 0 24px;
  z-index: 2010;
  pointer-events: none;
}
.cp-header > * { pointer-events: auto; }

.cp-back {
  display: flex;
  align-items: center;
  cursor: pointer;
  font-size: 18px;
  opacity: 0.9;
  transition: opacity 0.2s;
}
.cp-back:hover { opacity: 1; }
.cp-back i { margin-right: 12px; }
.cp-title { font-weight: 500; font-size: 16px; }

/* 主体布局 */
.cp-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  position: relative;
}

.cp-main {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;
  transition: margin-right 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  margin-right: 320px; /* 侧边栏宽度 */
}
.cp-main.sidebar-collapsed { margin-right: 0; }

.video-wrapper {
  width: 100%;
  height: 100%;
  position: relative;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-element {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.cp-loading, .cp-play-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}
.cp-loading { background: rgba(0,0,0,0.5); z-index: 10; font-size: 48px; }
.play-btn-big {
  width: 64px; height: 64px;
  border-radius: 50%;
  background: rgba(0,0,0,0.6);
  backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
  font-size: 24px; color: #fff;
  border: 2px solid rgba(255,255,255,0.2);
}

.cp-toast {
  position: absolute;
  left: 24px; top: 80px;
  background: rgba(0,0,0,0.7);
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  z-index: 20;
}

/* 底部控制栏 */
.cp-controls {
  position: absolute;
  bottom: 0; left: 0; right: 0;
  background: linear-gradient(0deg, rgba(0,0,0,0.85) 0%, rgba(0,0,0,0) 100%);
  padding: 0 24px 24px;
  z-index: 2010;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cp-progress-container {
  height: 12px;
  position: relative;
  cursor: pointer;
  display: flex;
  align-items: center;
}
.cp-progress-rail {
  position: absolute;
  left: 0; right: 0; top: 5px; height: 2px;
  background: rgba(255,255,255,0.2);
  border-radius: 2px;
  transition: height 0.1s;
}
.cp-progress-container:hover .cp-progress-rail { height: 4px; top: 4px; }
.cp-progress-loaded {
  position: absolute;
  left: 0; top: 5px; height: 2px;
  background: rgba(255,255,255,0.4);
  border-radius: 2px;
  transition: height 0.1s;
}
.cp-progress-container:hover .cp-progress-loaded { height: 4px; top: 4px; }
.cp-progress-fill {
  position: absolute;
  left: 0; top: 5px; height: 2px;
  background: #10b981;
  border-radius: 2px;
  transition: height 0.1s;
}
.cp-progress-container:hover .cp-progress-fill { height: 4px; top: 4px; }
.cp-progress-handle {
  position: absolute;
  width: 12px; height: 12px;
  background: #fff;
  border-radius: 50%;
  top: 0;
  transform: translateX(-50%);
  opacity: 0;
  transition: opacity 0.1s;
}
.cp-progress-container:hover .cp-progress-handle { opacity: 1; }
.cp-hover-time {
  position: absolute;
  bottom: 20px;
  transform: translateX(-50%);
  background: rgba(0,0,0,0.8);
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
}

.cp-controls-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.cp-controls-left, .cp-controls-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.cp-btn {
  background: none; border: none;
  color: #fff; font-size: 18px;
  cursor: pointer;
  opacity: 0.8;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
  width: 32px; height: 32px;
  border-radius: 4px;
}
.cp-btn:hover { opacity: 1; background: rgba(255,255,255,0.1); }
.cp-btn:disabled { opacity: 0.3; cursor: not-allowed; }
.cp-btn.active { color: #10b981; opacity: 1; }

.cp-btn-text {
  width: auto; padding: 0 12px;
  font-size: 14px; gap: 6px;
}
.cp-btn-primary {
  width: auto; padding: 0 16px;
  background: #10b981;
  color: #fff;
  font-size: 14px;
  opacity: 1;
}
.cp-btn-primary:hover { background: #059669; }

.cp-time { font-size: 13px; font-variant-numeric: tabular-nums; opacity: 0.9; }

/* 音量条 */
.cp-volume-wrap {
  display: flex; align-items: center;
  position: relative;
}
.cp-volume-slider {
  width: 0;
  overflow: hidden;
  transition: width 0.2s;
  display: flex; align-items: center;
}
.cp-volume-wrap:hover .cp-volume-slider { width: 80px; }
.cp-volume-slider input {
  width: 70px; margin-left: 8px;
  accent-color: #10b981;
}

/* 侧边栏 */
.cp-sidebar {
  position: absolute;
  top: 0; right: 0; bottom: 0;
  width: 320px;
  background: #1f2937;
  border-left: 1px solid #374151;
  display: flex;
  flex-direction: column;
  transform: translateX(0);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 2005;
}
.cp-sidebar.collapsed { transform: translateX(100%); }

.cp-sidebar-header {
  height: 60px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #374151;
  flex-shrink: 0;
}
.cp-sidebar-title { margin: 0; font-size: 16px; font-weight: 600; }
.cp-progress-badge {
  font-size: 12px; color: #10b981;
  background: rgba(16, 185, 129, 0.1);
  padding: 2px 8px; border-radius: 4px;
}

.cp-chapter-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px 0;
}
.cp-chapter-list::-webkit-scrollbar { width: 6px; }
.cp-chapter-list::-webkit-scrollbar-thumb { background: #4b5563; border-radius: 3px; }

.cp-chapter-item {
  display: flex;
  align-items: flex-start;
  padding: 12px 20px;
  cursor: pointer;
  transition: background 0.2s;
  border-left: 3px solid transparent;
}
.cp-chapter-item:hover { background: rgba(255,255,255,0.05); }
.cp-chapter-item.active {
  background: rgba(16, 185, 129, 0.1);
  border-left-color: #10b981;
}
.cp-chapter-item.played { opacity: 0.7; }
.cp-chapter-item.active { opacity: 1; }

.cp-chapter-idx {
  width: 24px;
  font-size: 14px;
  color: #9ca3af;
  margin-top: 2px;
  font-family: monospace;
}
.cp-chapter-info { flex: 1; min-width: 0; }
.cp-chapter-title {
  font-size: 14px; line-height: 1.4;
  margin-bottom: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.cp-chapter-meta {
  display: flex; align-items: center; justify-content: space-between;
  font-size: 12px; color: #6b7280;
}
.active .cp-chapter-title { color: #10b981; font-weight: 500; }
.anim-playing { color: #10b981; animation: pulse 1.5s infinite; }

.cp-empty-chapter {
  text-align: center;
  padding: 40px;
  color: #6b7280;
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.5; }
  100% { opacity: 1; }
}

/* 旧的 modal 样式保留或注释掉 (这里我们主要覆盖为 fullscreen 样式，所以不需要旧样式) */
/* .modal { display: none; } */


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

/* 已完成章节样式 */
.cp-chapter-item.completed {
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
  border-left: 3px solid #22c55e;
}

.cp-chapter-item.completed .cp-chapter-title {
  color: #16a34a;
  font-weight: 600;
}

.completed-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: #22c55e;
  color: #fff;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 600;
}

.completed-icon {
  margin-left: 6px;
  color: #22c55e;
  font-size: 16px;
}

.completed-text {
  margin-left: auto;
  padding: 2px 8px;
  background: #22c55e;
  color: #fff;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.cp-chapter-item.played:not(.completed) {
  background: #f8f9fa;
  border-left: 3px solid #94a3b8;
}

</style>

