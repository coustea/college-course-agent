<template>
  <div v-if="modelValue" class="dv-mask" @click.self="close" @wheel="forwardBackgroundScroll">
    <div class="dv-modal" ref="modalRef">
      <div class="dv-header">
        <div class="dv-title">{{ headerTitle }}</div>
        <div class="dv-tools">
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
          <div class="dv-view" ref="viewRef">
            <template v-if="isIframe">
              <iframe 
                class="dv-iframe" 
                :src="viewerSrc" 
                title="document" 
                allowfullscreen
              />
            </template>
            <template v-else>
              <div class="dv-content" :style="contentStyle" v-html="safeHtml"></div>
            </template>
          </div>
        </div>
      </div>
      <div class="dv-footer">
        <button class="dv-pill" :class="isCompleted ? 'done' : 'todo'" @click="$emit('progressClick')">{{ isCompleted ? '已看完' : '未看完' }}</button>
        <div class="dv-actions">
          <button class="dv-btn dv-btn-quiz" @click="startQuiz" :disabled="quizStarted || isCompleted">
            {{ quizStarted ? (allQuestionsAnswered ? '已答完' : `答题中 ${answersSoFar.length}/5`) : '观看完毕开始答题' }}
          </button>
          <button class="dv-btn dv-btn-secondary" @click="onPrev">上一章</button>
          <button class="dv-btn" @click="onNext">下一章</button>
          <button v-if="allQuestionsAnswered" class="dv-btn dv-btn-finish" @click="finishDocument">结束</button>
        </div>
      </div>
    </div>
  </div>

<!-- 题目弹窗复用视频题目组件 -->
<Question
    v-if="modelValue && questionVisible"
    v-model="questionVisible"
    :title="'选择题'"
    :stem="currentStem"
    :options="currentOptions"
    :correct-index="currentCorrect"
    :analysis="currentAnalysis"
    :next-text="questionNextText"
    @submit="onQuestionSubmit"
/>

<!-- 答题总结弹窗 -->
<el-dialog
    v-model="summaryVisible"
    title="答题总结"
    width="680px"
    append-to-body
>
  <div>
    <div style="margin-bottom:10px;color:#374151;">
      共 {{ questionList.length }} 题，已作答 {{ answersSoFar.length }} 题
    </div>
    <div v-for="(q, i) in questionList" :key="q.id || i" style="display:flex;align-items:flex-start;gap:10px;padding:8px 0;border-bottom:1px dashed #e5e7eb;">
      <div :style="{color: isAnswerCorrect(q) ? '#16a34a' : '#ef4444', fontWeight: 700, minWidth: '52px'}">
        {{ isAnswerCorrect(q) ? '正确' : '错误' }}
      </div>
      <div style="flex:1;">
        <div style="font-weight:700;color:#111827;line-height:1.6;">第{{ i+1 }}题：{{ q.stem }}</div>
        <div style="margin-top:6px;color:#374151;">
          你的答案：<b>{{ userAnswerLetter(q) || '-' }}</b>
          <span style="margin-left:12px;">正确答案：<b>{{ correctLetterOf(q) }}</b></span>
        </div>
        <div v-if="q.analysis" style="margin-top:6px;color:#6b7280;">解析：{{ q.analysis }}</div>
      </div>
    </div>
  </div>
  <template #footer>
    <el-button type="primary" @click="summaryVisible = false">知道了</el-button>
  </template>
</el-dialog>
</template>
<script setup>
import Question from '/src/components/Question.vue'
import { ref, computed, watch, onBeforeUnmount, onMounted, getCurrentInstance, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const props = defineProps({
  modelValue: {type: Boolean, default: false},
  title: {type: String, default: '预览'},
  fileUrl: {type: String, default: ''},
  htmlContent: {type: String, default: ''},
  progress: {type: Number, default: 0},
  id: {type: [String, Number], default: null},
  image: {type: String, default: ''},
  duration: {type: String, default: ''},
  chapterIndex: {type: Number, default: 1},
  courseTitle: {type: String, default: ''},
  chapters: {type: Array, default: () => []}
})

function toUrl(u) {
  if (!u) return ''
  const s = String(u)
  // 已是绝对地址，直接返回
  if (/^(https?:|data:|blob:)/.test(s)) return s
  // 相对路径统一返回，让 Vite proxy 或 Nginx 处理
  if (s.startsWith('/')) return s
  // 其他情况，补上 /
  return '/' + s.replace(/^\//, '')
}

const emit = defineEmits(['update:modelValue', 'next', 'prev', 'progressClick'])
const { proxy } = getCurrentInstance()
const BASE_URL = proxy?.$baseUrl || ''

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
const currentIndex = ref(Math.max(0, (props.chapterIndex || 1) - 1))
watch(() => props.modelValue, (v) => {
  if (v) {
    currentIndex.value = Math.max(0, (props.chapterIndex || 1) - 1)
    // 立即同步进度展示
    console.log('[DocumentViewer]文档打开，立即同步进度')
    syncDocumentProgressFromCourse(false).catch(e => console.error(e))
  }
})
// 切换章节时重置答题状态
watch(currentIndex, () => {
  fetchedForChapter = false
  remainingIndex.value = -1
  answersSoFar.value = []
  quizStarted.value = false
  isClosing.value = false  // 重置关闭标志
  console.log('[DocumentViewer]切换章节，重置答题状态')
  // 同步进度时不重置答题状态（仅读取后端进度，不清空本地答题记录）
  try { syncDocumentProgressFromCourse(false) } catch (e) { console.error(e) }
})
const currentChapter = computed(() => flatChapters.value[currentIndex.value] || {
  title: props.title,
  fileUrl: props.fileUrl,
  html: props.htmlContent
})

const effectiveFileUrl = computed(() => currentChapter.value?.fileUrl || '')
const effectiveHtml = computed(() => currentChapter.value?.html || '')
const readProgress = ref(0)
const progressDisplay = computed(() => {
  const p = Math.round((readProgress.value || 0) * 100)
  return Number.isFinite(p) ? p : (props.progress || 0)
})
// 完成状态：容差 95%
const isCompleted = computed(() => {
  const p = Number(progressDisplay.value)
  return Number.isFinite(p) && p >= 95
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

function isAbsoluteUrl(u) {
  return /^https?:\/\//.test(u)
}

const viewerSrc = computed(() => {
  const url = normalizedFileUrl.value
  if (!url) return ''
  // const isOffice = ["doc","docx","ppt","pptx","xls","xlsx"].includes(fileExt.value)
  
  // if (isOffice) {
  //   // 对于 Office 文件，只有绝对 URL 才能使用在线预览
  //   if (isAbsoluteUrl(url)) {
  //     // 只有公网地址才能使用 Microsoft Office Apps 预览
  //     if (!isPrivateUrl(url)) {
  //       return `https://view.officeapps.live.com/op/view.aspx?ui=en-US&src=${encodeURIComponent(url)}`
  //     }
  //   }
  //
  //   return ''
  // }
  
  return url
})

const isIframe = computed(() => !!viewerSrc.value)


async function close() {
  isClosing.value = true
  if (questionVisible.value) {
    questionVisible.value = false
    console.log('[DocumentViewer]关闭文档前先关闭题目弹窗')
  }
  emit('update:modelValue', false)
}

async function syncDocumentProgressFromCourse(resetQuizState = false) {
  try {
    const studentId = localStorage.getItem('userId')
    const courseId = props.id
    if (!studentId || !courseId) return
    const token = localStorage.getItem('token')
    const res = await axios.get(`${BASE_URL}/progress/course/all`, {
      params: { studentId, courseId },
      headers: { Authorization: `Bearer ${token}` }
    })
    console.log('[DocumentViewer]同步文档进度结果:', res.data)
    const data = res.data.data
    if (!data) return
    const docs =  data.documents
    const docId = (Number(currentIndex.value) || 0) + 1
    const cur = docs.find(d => String(d.documentId) === String(docId))
    if (cur) {
      const completed = cur.completed === true 
      readProgress.value = completed ? 1 : (readProgress.value || 0)
      console.log('[DocumentViewer]同步进度 - 后端完成状态:', completed, '当前进度:', readProgress.value)
      // 如果后端已完成，且不需要重置答题状态，则保留已答题记录
      if (completed && !resetQuizState) {
        console.log('[DocumentViewer]后端已完成，保留答题状态不重置')
      }
    }
  } catch (e) { console.error('[DocumentViewer]同步文档进度失败', e) }
}

async function onNext() {
  if (currentIndex.value < flatChapters.value.length - 1) currentIndex.value += 1
  emit('next')
}

// 开始答题
async function startQuiz() {
  try {
    quizStarted.value = true
    console.log('[DocumentViewer]开始答题')
    
    const courseId = props.id || props.title
    const qs = await fetchQuestionsOnce(courseId)
    
    if (!Array.isArray(qs) || qs.length === 0) {
      ElMessage.error('获取题目失败，请重试')
      quizStarted.value = false
      return
    }
    
    // 显示第一题
    remainingIndex.value = 0
    const q = qs[0]
    if (q) {
      currentQuestionId.value = q.id
      currentStem.value = q.stem
      currentOptions.value = q.options
      currentCorrect.value = q.correct
      currentAnalysis.value = q.analysis
      questionVisible.value = true
    }
  } catch (e) {
    console.error('[DocumentViewer]开始答题失败:', e)
    ElMessage.error('开始答题失败')
    quizStarted.value = false
  }
}

// 结束文档学习（提交答案并标记完成）
async function finishDocument() {
  console.log('[DocumentViewer]点击结束按钮，提交答案并标记完成')
  await submitDocumentAnswersAndProgress()
}

const questionVisible = ref(false)
const questionList = ref([])
const answersSoFar = ref([])
const examId = ref(null)
const currentQuestionId = ref(null)
const currentStem = ref('')
const currentOptions = ref(['A','B','C','D'])
const currentCorrect = ref(0)
const currentAnalysis = ref('')
const pendingNodeKey = ref('')
const resolver = ref(null)
const isClosing = ref(false)  
const quizStarted = ref(false) 


const allQuestionsAnswered = computed(() => {
  return quizStarted.value && answersSoFar.value.length >= 5
})


const questionNextText = computed(() => {
  try {
    const total = Array.isArray(questionList.value) ? questionList.value.length : 0
    const answered = Math.max(0, Math.min(total, answersSoFar.value.length))
    return answered >= total - 1 ? '完成' : '下一题'
  } catch {
    return '下一题'
  }
})

const summaryVisible = ref(false)
function findUserAnswer(q) {
  try {
    const id = q?.id
    const item = answersSoFar.value.find(a => a.questionId === id)
    return item?.answer
  } catch { return '' }
}
function userAnswerLetter(q) {
  return findUserAnswer(q)
}
function correctLetterOf(q) {
  const idx = Number(q?.correct) || 0
  return ['A','B','C','D'][Math.max(0, Math.min(3, idx))]
}
function isAnswerCorrect(q) {
  const u = findUserAnswer(q)
  return u && u.toUpperCase() === correctLetterOf(q)
}

async function onQuestionSubmit(payload) {
  try {
    const idx = Number(payload?.answerIndex)
    const letter = ['A','B','C','D'][Math.max(0, Math.min(3, Number.isFinite(idx) ? idx : 0))]
    const qid = currentQuestionId.value
    if (qid) {
      const existing = answersSoFar.value.findIndex(a => a.questionId === qid)
      if (existing >= 0) answersSoFar.value.splice(existing, 1, { questionId: qid, answer: letter })
      else answersSoFar.value.push({ questionId: qid, answer: letter })
    }
  } catch (e) { console.error(e) }
  questionVisible.value = false
  if (typeof resolver.value === 'function') {
    resolver.value(true) 
    resolver.value = null
  }
}

// 一次性取题：每次打开文档时调用一次
let fetchedForChapter = false
async function fetchQuestionsOnce(courseId) {
  try {
    if (fetchedForChapter && questionList.value?.length > 0) {
      console.log('[fetchQuestionsOnce]已取过题，返回缓存')
      return questionList.value
    }
    
    const studentId = localStorage.getItem('userId')
    const token = localStorage.getItem('token')
    const body = { courseId, studentId, choiceCount: 5, judgeCount: 0 }
    console.log('[DocumentViewer]请求参数:', body)
    const res = await axios.post(`${BASE_URL}/aiexam/generate`, body, {
      headers: { 
        'Content-Type': 'application/json', 
        Authorization: `Bearer ${token}` 
      }
    })
    console.log('[DocumentViewer]获取题目结果:', res.data)
    if (res.data.code === 200) {
      const data = res.data.data
      // 保存 examId
      examId.value = (data?.exam && (data.exam.id || data.exam.examId)) || data.id || (Array.isArray(data.questions) ? data.questions[0]?.examId : null) || null
      const list = Array.isArray(data?.questions) ? data.questions : (Array.isArray(data?.choices) ? data.choices : [])
      questionList.value = list.slice(0, 5).map(q => normalizeQuestion(q))
      fetchedForChapter = true
      console.log('[fetchQuestionsOnce]取题成功，examId:', examId.value, '题目数量:', questionList.value.length)
      return questionList.value
    } else {
      console.log('[fetchQuestionsOnce]响应code不是200:', res.data)
    }
  } catch (e) { 
    console.error('[fetchQuestionsOnce]取题失败:', e)
    return [] 
  }
}

function normalizeQuestion(q) {
  const stripLabel = (s) => String(s || '').replace(/^\s*[A-Da-d][\.、\s]\s*/, '').trim()
  // 选项
  let opts = []
  if (Array.isArray(q?.options)) opts = q.options
  else if (typeof q?.options === 'string') {
    const s = q.options.trim()
    try { if (/^\[.*\]$/.test(s)) opts = JSON.parse(s.replace(/'/g, '"')) } catch {}
    if (!Array.isArray(opts) || !opts.length) opts = s.split(/[，,；;\n]/).map(x=>x.trim()).filter(Boolean)
  } else opts = [q?.a, q?.b, q?.c, q?.d].filter(Boolean)
  opts = opts.slice(0,4).map(stripLabel)
  const correctIndex = (typeof q.correctIndex === 'number') ? q.correctIndex : ['A','B','C','D'].indexOf(String(q.answer||'').toUpperCase())
  return {
    id: q.id || q.questionId,
    stem: q.content || q.stem || q.title || '',
    options: opts.length===4?opts:['选项A','选项B','选项C','选项D'],
    correct: Number.isFinite(correctIndex)?correctIndex:0,
    analysis: q.analysis || ''
  }
}

// 记录某章是否已经触发过题目
// 已移除 hasQuestionShown 和 markQuestionShown 函数（不再需要）

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

// 已移除自动监听阅读进度弹题的逻辑

onBeforeUnmount(() => { if (timer) { clearInterval(timer); timer = null } })

const fontScale = ref(1)
const modalRef = ref(null)
const bodyRef = ref(null)

const contentStyle = computed(() => ({
  fontSize: `${Math.round(16 * fontScale.value)}px`,
  lineHeight: 1.6 * fontScale.value
}))

function zoomIn() {
  increaseText()
}

function zoomOut() {
  decreaseText()
}

function increaseText() {
  fontScale.value = Math.min(2, +(fontScale.value + 0.1).toFixed(2))
}

function decreaseText() {
  fontScale.value = Math.max(0.6, +(fontScale.value - 0.1).toFixed(2))
}

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
  } catch (e) {
    console.error(e)
  }
}

function onPrev() {
  if (currentIndex.value > 0) currentIndex.value -= 1;
  emit('prev')
}

function selectChapter(i) {
  if (i >= 0 && i < flatChapters.value.length) currentIndex.value = i
}

// 全屏状态跟踪（Esc 退出也能更新）
const isFullscreen = ref(false)
function handleFullscreenChange() {
  try {
    isFullscreen.value = !!document.fullscreenElement
  } catch {
    isFullscreen.value = false
  }
}

onMounted(() => {
  try {
    document.addEventListener('fullscreenchange', handleFullscreenChange)
  } catch (e) {
    console.error(e)
  }
  // 打开时同步一次课程进度，并据此设置当前文档进度（不重置答题状态）
  try {
    syncDocumentProgressFromCourse(false)
  } catch (e) { console.error(e) }
})
onBeforeUnmount(() => {
  try {
    document.removeEventListener('fullscreenchange', handleFullscreenChange)
  } catch (e) {
    console.error(e)
  }
})

// 本地滚动进度计算（非 iframe 文档有效）
function updateReadProgress() {
  const el = bodyRef.value
  if (!el) return
  const total = Math.max(1, el.scrollHeight - el.clientHeight)
  const ratio = Math.max(0, Math.min(1, el.scrollTop / total))
  readProgress.value = ratio
  console.log(`[DocumentViewer]滚动进度: ${(ratio * 100).toFixed(1)}%, scrollTop: ${el.scrollTop}, total: ${total}`)
}

// Ctrl + 滚轮：仅对 HTML 内容调整字号
function handleWheel(e) {
  // 检查是否在文档查看器区域内
  if (!props.modelValue || !bodyRef.value) {
    return
  }
  
  const targetElement = e.target
  const isInViewer = bodyRef.value.contains(targetElement)
  
  console.log(`[handleWheel]被调用, isIframe: ${isIframe.value}, isInViewer: ${isInViewer}, ctrlKey: ${e.ctrlKey}, deltaY: ${e.deltaY}`)
  
  if (!isInViewer) {
    return // 不在文档查看器区域，忽略
  }
  
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
    console.log(`[handleWheel]iframe滚轮合成进度: ${(next * 100).toFixed(1)}%`)
  }
}

// 滚轮事件现在直接在模板中绑定到 .dv-body 上

// 键盘快捷键：按 End 键直接跳到 100% 进度（测试用）
function handleKeydown(e) {
  if (e.key === 'End') {
    console.log('[handleKeydown]End键按下，设置进度到100%')
    readProgress.value = 1
    // 已移除自动弹题调用
  } else if (e.key === 'Home') {
    console.log('[handleKeydown]Home键按下，重置进度到0%')
    readProgress.value = 0
  }
}


watch(() => props.modelValue, (v) => {
  if (v) {
    document.addEventListener('keydown', handleKeydown)
    // 使用捕获阶段监听滚轮事件，可以捕获 iframe 上的滚轮
    document.addEventListener('wheel', handleWheel, { capture: true, passive: true })
    // 每次打开文档时重置所有状态
    fetchedForChapter = false
    remainingIndex.value = -1
    answersSoFar.value = []
    quizStarted.value = false
    //isClosing.value = false  // 重置关闭标志
    console.log('[DocumentViewer]文档打开，重置所有答题状态')
  } else {
    document.removeEventListener('keydown', handleKeydown)
    document.removeEventListener('wheel', handleWheel, { capture: true })
    console.log('[DocumentViewer]文档关闭')
  }
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
    try {
      el?.removeEventListener('scroll', updateReadProgress)
    } catch (e) {
      console.error(e)
    }
  }
})

// 已移除自动监听滚动进度弹题的逻辑

// 题目弹窗：逐题展示，提交后显示答案与解析，点击继续展示下一题；全部答完后调用"已看完"逻辑
watch(questionVisible, async (v) => {
  if (!v) {
      // 文档正在关闭时不弹出下一题
      if (isClosing.value) {
        console.log('[DocumentViewer]文档查看器正在关闭，不弹出下一题')
        return
      }

      const total = Array.isArray(questionList.value) ? questionList.value.length : 0
      if (total <= 0) return

      const nextIndex = remainingIndex.value + 1
      if (nextIndex < total) {
        const q = questionList.value[nextIndex]
        if (q) {
          remainingIndex.value = nextIndex
          // 等待对话框完全关闭再打开下一题，避免动画导致的渲染竞态
          await nextTick()
          currentQuestionId.value = q.id
          currentStem.value = q.stem
          currentOptions.value = q.options
          currentCorrect.value = q.correct
          currentAnalysis.value = q.analysis
          questionVisible.value = true
        }
      } else {
        console.log('[DocumentViewer]题目已答完，展示答题总结')
        summaryVisible.value = true
      }
  }
})

const remainingIndex = ref(-1)
watch(questionVisible, (v, ov) => {
  if (v && ov === false) {
    if (remainingIndex.value < 0 && Array.isArray(questionList.value)) {
      remainingIndex.value = 0
      const q = questionList.value[0]
      if (q) {
        currentQuestionId.value = q.id
        currentStem.value = q.stem
        currentOptions.value = q.options
        currentCorrect.value = q.correct
        currentAnalysis.value = q.analysis
      }
    }
  }
})

// 提交答案并上报文档已看完（仅当5题全部答完时调用）
async function submitDocumentAnswersAndProgress() {
  try {
    const studentId = localStorage.getItem('userId')
    const eid = examId.value
    const token = localStorage.getItem('token')
    
    // 确保已经答完5题
    const expectedQuestions = 5
    if (answersSoFar.value.length < expectedQuestions) {
      console.log(`[DocumentViewer]题目未答完，已答${answersSoFar.value.length}/${expectedQuestions}题，不上报完成状态`)
      return
    }
    
    // 提交答案
    if (eid && studentId && answersSoFar.value.length > 0) {
      const body = {
        examId: eid,
        studentId: studentId,
        answers: answersSoFar.value.map(a => ({ questionId: a.questionId, answer: a.answer }))
      }
      console.log('[DocumentViewer]提交答案（5题全部完成）:', body)
      const res = await axios.post(`${BASE_URL}/aiexam/submit`, body, {
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
      })
      if (res.data.code === 200) {
        console.log('[DocumentViewer]提交答案成功')
      }
    }
    // 上报文档进度为已完成（所有题目答完后）
    if (studentId && props.id) {
      const docId = (Number(currentIndex.value) || 0) + 1
      const params = {
        studentId: studentId,
        courseId: props.id,
        documentId: docId,
        completed: true
      }
      console.log('[DocumentViewer]上报文档已看完（5题全部答完）:', params)
      const res2 = await axios.post(`${BASE_URL}/progress/report`, null, {
        params: params,
        headers: { Authorization: `Bearer ${token}` }
      })
      if (res2.data.code === 200) {
        console.log('[DocumentViewer]上报文档完成成功')
        // 标记已看完（使用后端返回的进度，不再被本地滚动进度覆盖）
        readProgress.value = 1
        // 同步后端进度，确保后续不被本地状态覆盖
        await syncDocumentProgressFromCourse(false)
      }
    }
  } catch (e) {
    console.error('[DocumentViewer]提交答案或进度失败', e)
  }
}
</script>
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
  overflow-y: auto; /* 只允许纵向滚动 */
  overflow-x: hidden; /* 禁止横向滚动，避免出现水平滚动条 */
}
.dv-main { display: flex; height: 100%; overflow: hidden; }
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
.dv-view { flex: 1; min-width: 0; overflow-x: hidden; }
.dv-modal:fullscreen .dv-body {
  height: auto;
  flex: 1;
  min-height: 0;
}
.dv-iframe {
  width: 100%;
  height: 100%;
  border: 0;
  display: block;
}
.dv-content {
  padding: 16px;
  color: #333;
  line-height: 1.7;
  max-width: 100%;
  overflow-x: hidden; /* HTML 内容禁止横向滚动 */
  word-wrap: break-word;
}
.dv-content img,
.dv-content video,
.dv-content canvas,
.dv-content table {
  max-width: 100%;
  height: auto;
}
.dv-content pre {
  white-space: pre-wrap; /* 长代码换行，避免撑出横向滚动 */
  word-break: break-word;
}
.dv-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  background: #f6f8fb;
  gap: 16px;
}

.dv-modal:fullscreen .dv-footer {
  padding: 14px 18px;
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
.dv-pill.done {
  background: #dcfce7;
  border-color: #86efac;
  color: #065f46;
}
.dv-pill.todo {
  background: #fee2e2;
  border-color: #fecaca;
  color: #7f1d1d;
}
.dv-actions { display: flex; align-items: center; gap: 12px; }
.dv-btn {
  padding: 10px 16px; /* 放大按钮尺寸 */
  background: #2563eb;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}
.dv-btn-secondary {
  background: #0ea5e9;
}
.dv-btn-quiz {
  background: #10b981;
}
.dv-btn-quiz:hover {
  background: #059669;
}
.dv-btn-quiz:disabled {
  background: #6ee7b7;
  cursor: not-allowed;
  opacity: 0.6;
}
.dv-btn-finish {
  background: #f59e0b;
}
.dv-btn-finish:hover {
  background: #d97706;
}
.dv-btn:hover {
  background: #1d4ed8;
}
.dv-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>

