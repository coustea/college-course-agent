<template>
  <div v-if="modelValue" class="dv-mask" @click.self="close">
    <div class="dv-modal" ref="modalRef">
      <div class="dv-header">
        <div class="dv-title">{{ headerTitle }}</div>
        <div class="dv-tools">
          <button class="dv-tool" @click="zoomOut" title="缩小"><i class="fas fa-search-minus"></i></button>
          <button class="dv-tool" @click="zoomIn" title="放大"><i class="fas fa-search-plus"></i></button>
          <button class="dv-tool" @click="toggleFullscreen" title="全屏"><i class="fas fa-expand-arrows-alt"></i></button>
        </div>
        <button class="dv-close" @click="close" title="关闭"><i class="fas fa-times"></i></button>
      </div>
      <div class="dv-body" ref="bodyRef">
        <div class="dv-main">
          <aside v-if="!isFullscreen && flatChapters.length > 1" class="dv-aside">
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
            <IdeologyResourcePanel
              v-if="props.id || currentChapter?.documentId"
              :course-id="courseIdForIdeology"
              :document-id="currentChapter?.documentId || props.id"
              compact
            />
          </aside>
          <div class="dv-view" ref="viewRef" @scroll="updateReadProgress" @wheel="handleWheel">
            <!-- 加载状态 -->
            <div v-if="docLoading" class="dv-loading">
              <i class="fas fa-spinner fa-spin"></i>
              <span>文档加载中...</span>
            </div>

            <!-- 错误状态 -->
            <div v-else-if="docError" class="dv-error">
              <i class="fas fa-exclamation-triangle"></i>
              <p>{{ docError }}</p>
              <button class="dv-btn" @click="downloadFile">下载文档</button>
            </div>

            <!-- PDF 原生渲染 -->
            <template v-else-if="docType === 'pdf'">
              <div class="dv-pdf-container">
                <div class="dv-pdf-toolbar">
                  <span class="dv-page-info">{{ currentPdfPage }} / {{ totalPdfPages || '?' }}</span>
                  <button class="dv-tool-sm" @click="prevPdfPage" :disabled="currentPdfPage <= 1" title="上一页"><i class="fas fa-chevron-left"></i></button>
                  <button class="dv-tool-sm" @click="nextPdfPage" :disabled="currentPdfPage >= totalPdfPages" title="下一页"><i class="fas fa-chevron-right"></i></button>
                </div>
                <VuePdfEmbed
                  v-if="pdfSource"
                  :source="pdfSource"
                  :page="currentPdfPage"
                  class="dv-pdf"
                  :style="{ transform: `scale(${fontScale})` }"
                  @rendered="onPdfRendered"
                  @loaded="onPdfLoaded"
                />
              </div>
            </template>

            <!-- DOCX 渲染 -->
            <template v-else-if="docType === 'docx'">
              <div class="dv-content dv-docx-content" :style="contentStyle" v-html="docxHtml"></div>
            </template>

            <!-- TXT 渲染 -->
            <template v-else-if="docType === 'txt'">
              <pre class="dv-content dv-txt-content" :style="contentStyle">{{ txtContent }}</pre>
            </template>

            <!-- HTML 内容渲染（原有逻辑） -->
            <template v-else-if="effectiveHtml">
              <div class="dv-content" :style="contentStyle" v-html="safeHtml"></div>
            </template>

            <!-- 不支持的格式 -->
            <div v-else class="dv-unsupported">
              <i class="fas fa-file"></i>
              <p>暂不支持预览此格式</p>
              <p class="dv-hint">支持的格式：PDF、DOCX、TXT</p>
              <button class="dv-btn" @click="downloadFile">下载文档</button>
            </div>
          </div>
        </div>
      </div>
      <div class="dv-footer">
        <button class="dv-pill" :class="isCompleted ? 'done' : 'todo'" @click="$emit('progressClick')">{{ isCompleted ? '已完成' : '未完成' }}</button>
        <div class="dv-actions">
          <button class="dv-btn dv-btn-secondary" @click="onPrev" :disabled="currentIndex === 0">上一章</button>
          <button class="dv-btn" @click="onNext" :disabled="currentIndex >= flatChapters.length - 1">下一章</button>
          <button class="dv-btn dv-btn-quiz" @click="startQuiz" :disabled="!isCompleted || (quizStarted && !allQuestionsAnswered)">
            {{ quizStarted ? (allQuestionsAnswered ? '查看总结' : `答题中 ${answersSoFar.length}/${questionList.length}`) : '开始答题' }}
          </button>
          <button v-if="allQuestionsAnswered" class="dv-btn dv-btn-finish" @click="finishDocument">完成学习</button>
        </div>
      </div>
    </div>
  </div>

<!-- 文档多题答题弹窗 -->
<el-dialog
    v-if="modelValue && quizStarted"
    v-model="documentQuizVisible"
    title="文档测试题"
    width="800px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    append-to-body
    class="document-quiz-dialog"
>
  <div class="document-quiz-body">
    <div class="quiz-header-info">
      <span class="quiz-count">共 {{ questionList.length }} 道题</span>
      <span class="quiz-progress">已作答 {{ answersSoFar.length }} 题</span>
    </div>

    <div class="quiz-questions-list">
      <div
        v-for="(q, idx) in questionList"
        :key="q.id || idx"
        class="quiz-question-item"
        :class="{ 'answered': isQuestionAnswered(q.id) }"
      >
        <!-- 题目序号和状态 -->
        <div class="question-header">
          <span class="question-number">第 {{ idx + 1 }} 题</span>
          <span v-if="isQuestionAnswered(q.id)" class="question-status answered-badge">已作答</span>
          <span v-else class="question-status pending-badge">未作答</span>
        </div>

        <!-- 题干 -->
        <div class="question-stem">{{ q.stem }}</div>

        <!-- 选项 -->
        <div class="question-options">
          <div
            v-for="(opt, optIdx) in q.options"
            :key="optIdx"
            class="quiz-option-item"
            :class="{ 'selected': isOptionSelected(q.id, optIdx) }"
            @click="selectOption(q.id, optIdx)"
          >
            <span class="option-label">{{ ['A', 'B', 'C', 'D'][optIdx] }}</span>
            <span class="option-text">{{ opt }}</span>
          </div>
        </div>

        <!-- 答题结果显示（仅在答题后显示） -->
        <div v-if="showQuizResults" class="quiz-result-panel" :class="{ 'correct': isQuestionCorrect(q.id), 'wrong': !isQuestionCorrect(q.id) }">
          <div class="result-title">
            <i class="fas" :class="isQuestionCorrect(q.id) ? 'fa-check-circle' : 'fa-times-circle'"></i>
            {{ isQuestionCorrect(q.id) ? '回答正确' : '回答错误' }}
          </div>
          <div v-if="!isQuestionCorrect(q.id)" class="correct-answer">
            正确答案：{{ ['A', 'B', 'C', 'D'][q.correct] }}
          </div>
          <div v-if="q.analysis" class="quiz-analysis">
            <strong>解析：</strong>{{ q.analysis }}
          </div>
        </div>
      </div>
    </div>
  </div>

  <template #footer>
    <div class="dialog-footer">
      <el-button v-if="!showQuizResults" @click="documentQuizVisible = false" size="large">暂时关闭</el-button>
      <el-button
          v-if="!showQuizResults"
          type="primary"
          @click="submitDocumentQuiz"
          size="large"
          :disabled="answersSoFar.length < questionList.length"
      >
        提交答案（{{ answersSoFar.length }}/{{ questionList.length }}）
      </el-button>
      <el-button
          v-if="showQuizResults"
          type="primary"
          @click="closeDocumentQuiz"
          size="large"
      >
        完成
      </el-button>
    </div>
  </template>
</el-dialog>

<!-- 题目弹窗复用视频题目组件（仅视频使用） -->
<Question
    v-if="modelValue && questionVisible && !quizStarted"
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
import VuePdfEmbed from 'vue-pdf-embed'
import mammoth from 'mammoth'
import IdeologyResourcePanel from '/src/components/IdeologyResourcePanel.vue'

const props = defineProps({
  modelValue: {type: Boolean, default: false},
  title: {type: String, default: '预览'},
  fileUrl: {type: String, default: ''},
  htmlContent: {type: String, default: ''},
  courseId: {type: [String, Number], default: null},
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
const BASE_URL = proxy?.$baseUrl || '/api'

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
      const documentId = n.documentId ?? n.id ?? null
      if (title || fileUrl || html) list.push({ title, fileUrl, html, documentId })
      if (Array.isArray(n.children)) walk(n.children)
    }
  }
  walk(props.chapters)
  if (list.length === 0) {
    list.push({
      title: props.title || '文档',
      fileUrl: props.fileUrl,
      html: props.htmlContent,
      documentId: props.id
    })
  }
  return list
})
const currentIndex = ref(Math.max(0, (props.chapterIndex || 1) - 1))
watch(() => props.modelValue, (v) => {
  if (v) {
    currentIndex.value = Math.max(0, (props.chapterIndex || 1) - 1)
    isBackendCompleted.value = false  // 重置后端完成状态
    // 立即同步进度展示
    syncDocumentProgressFromCourse(false).catch(e => console.error(e))
    // 加载文档内容（原生渲染）
    loadDocument()
  } else {
    // 重置文档渲染状态
    docType.value = ''
    docError.value = ''
    pdfSource.value = null
    docxHtml.value = ''
    txtContent.value = ''
    currentPdfPage.value = 1
    totalPdfPages.value = 0
    isBackendCompleted.value = false  // 重置后端完成状态
  }
})
// 切换章节时重置答题状态和阅读时间
watch(currentIndex, async () => {
  fetchedForChapter = false
  remainingIndex.value = -1
  answersSoFar.value = []
  quizStarted.value = false
  isClosing.value = false  // 重置关闭标志
  isBackendCompleted.value = false  // 重置后端完成状态
  // 不要立即重置readProgress，等待后端同步完成
  actualReadTime.value = 0 // 重置阅读时间
  readTimeAccumulator = 0  // 重置累加器
  // 同步进度时等待异步完成，确保后端状态正确加载
  try {
    await syncDocumentProgressFromCourse(false)
  } catch (e) {
    console.error('[DocumentViewer]同步进度失败', e)
    // 如果同步失败，重置为0
    readProgress.value = 0
  }
  // 重新加载文档内容（原生渲染）
  loadDocument()
})
const currentChapter = computed(() => flatChapters.value[currentIndex.value] || {
  title: props.title,
  fileUrl: props.fileUrl,
  html: props.htmlContent
})

const courseIdForIdeology = computed(() => {
  if (props.courseId) return props.courseId
  const direct = props.chapters?.find?.(item => item?.courseId)?.courseId
  if (direct) return direct
  const nested = props.chapters?.flatMap?.(item => Array.isArray(item?.children) ? item.children : [])?.find?.(item => item?.courseId)?.courseId
  return nested || null
})

const effectiveFileUrl = computed(() => currentChapter.value?.fileUrl || '')
const effectiveHtml = computed(() => currentChapter.value?.html || '')
const readProgress = ref(0)
const isBackendCompleted = ref(false) // 标记后端是否已完成
const progressDisplay = computed(() => {
  const p = Math.round((readProgress.value || 0) * 100)
  return Number.isFinite(p) ? p : (props.progress || 0)
})

// 计算文档所需阅读时间（动态）
const requiredReadTime = computed(() => {
  const html = effectiveHtml.value
  if (!html) return 30 // 默认最少30秒

  // 提取纯文本字数
  const text = html.replace(/<[^>]+>/g, '') // 移除HTML标签
                   .replace(/&nbsp;/g, ' ')
                   .replace(/&lt;/g, '<')
                   .replace(/&gt;/g, '>')
                   .replace(/&amp;/g, '&')
                   .trim()

  const wordCount = text.length // 中文字符计数
  // 计算公式：字数/10 + 30，最少30秒，最多5分钟
  const calculatedTime = Math.floor(wordCount / 10) + 30
  return Math.min(300, Math.max(30, calculatedTime))
})

// 实际阅读时间（秒）
const actualReadTime = ref(0)
const readTimer = ref(null)
const isReading = ref(false)

// 完成状态：后端已完成 或（阅读时间达标 且 滚动进度>=50%）
const isCompleted = computed(() => {
  // 如果后端已经标记为完成，直接返回true
  if (isBackendCompleted.value) {
    return true
  }

  // 否则根据本地状态判断
  const scrollPercent = Number(progressDisplay.value)
  const timeMet = actualReadTime.value >= requiredReadTime.value
  const scrollMet = Number.isFinite(scrollPercent) && scrollPercent >= 50
  return timeMet && scrollMet
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
  return url
})

// 移除 isIframe，改用原生渲染
// const isIframe = computed(() => !!viewerSrc.value)

// ========== 新增：原生文档渲染相关变量 ==========
const docType = ref('')
const docLoading = ref(false)
const docError = ref('')
const pdfSource = ref(null)
const docxHtml = ref('')
const txtContent = ref('')
const currentPdfPage = ref(1)
const totalPdfPages = ref(0)

// 检测文档类型
function detectDocType(url) {
  if (!url) return ''
  const ext = url.split('.').pop().toLowerCase().split('?')[0]
  const typeMap = {
    'pdf': 'pdf',
    'doc': 'docx',
    'docx': 'docx',
    'txt': 'txt',
    'md': 'txt'
  }
  return typeMap[ext] || ''
}

// 加载文档
async function loadDocument() {
  const url = normalizedFileUrl.value
  if (!url) {
    docType.value = ''
    return
  }

  docLoading.value = true
  docError.value = ''
  
  const type = detectDocType(url)
  docType.value = type

  if (!type) {
    // 如果有 HTML 内容，使用 HTML 渲染
    if (effectiveHtml.value) {
      docLoading.value = false
      return
    }
    docError.value = '不支持的文件格式'
    docLoading.value = false
    return
  }

  try {
    switch (type) {
      case 'pdf':
        await loadPdf(url)
        break
      case 'docx':
        await loadDocx(url)
        break
      case 'txt':
        await loadTxt(url)
        break
    }
  } catch (e) {
    console.error('[DocumentViewer]加载文档失败:', e)
    docError.value = `加载失败: ${e.message}`
  } finally {
    docLoading.value = false
  }
}

// 加载 PDF
async function loadPdf(url) {
  try {
    pdfSource.value = url
    currentPdfPage.value = 1
  } catch (e) {
    throw new Error('PDF加载失败')
  }
}

// 加载 DOCX
async function loadDocx(url) {
  try {
    const response = await fetch(url)
    if (!response.ok) throw new Error('文件下载失败')

    const arrayBuffer = await response.arrayBuffer()
    const result = await mammoth.convertToHtml({ arrayBuffer })

    if (result.messages.length > 0) {
      console.warn('[DocumentViewer]DOCX转换警告:', result.messages)
    }

    docxHtml.value = result.value
  } catch (e) {
    throw new Error('DOCX加载失败: ' + e.message)
  }
}

// 加载 TXT
async function loadTxt(url) {
  try {
    const response = await fetch(url)
    if (!response.ok) throw new Error('文件下载失败')

    const text = await response.text()
    txtContent.value = text
  } catch (e) {
    throw new Error('TXT加载失败: ' + e.message)
  }
}

// PDF 页面控制
function prevPdfPage() {
  if (currentPdfPage.value > 1) {
    currentPdfPage.value--
    updateReadProgress() // 更新阅读进度
  }
}

function nextPdfPage() {
  if (currentPdfPage.value < totalPdfPages.value) {
    currentPdfPage.value++
    updateReadProgress() // 更新阅读进度
  }
}

function onPdfLoaded(info) {
  if (info && info.numPages) {
    totalPdfPages.value = info.numPages
    updateReadProgress() // 初始化阅读进度
  }
}

function onPdfRendered() {
}

// 下载文档
function downloadFile() {
  const link = document.createElement('a')
  link.href = normalizedFileUrl.value
  link.download = props.title || '文档'
  link.target = '_blank'
  link.click()
}

async function close() {
  isClosing.value = true
  if (questionVisible.value) {
    questionVisible.value = false
  }
  emit('update:modelValue', false)
}

async function syncDocumentProgressFromCourse(resetQuizState = false) {
  try {
    const userRole = localStorage.getItem('userRole')
    if (userRole === 'teacher') {
      return
    }

    const studentId = localStorage.getItem('userId')
    const courseId = props.id
    if (!studentId || !courseId) return
    const token = localStorage.getItem('token')
    const res = await axios.get(`${BASE_URL}/progress/course/all`, {
      params: { studentId, courseId },
      headers: { Authorization: `Bearer ${token}` }
    })
    const data = res.data.data
    if (!data) return
    const docs =  data.documents
    // 使用当前章节的真实 documentId
    const currentChapter = flatChapters.value[currentIndex.value]
    const docId = currentChapter?.documentId
    if (!docId) {
      return
    }
    const cur = docs.find(d => String(d.documentId) === String(docId))
    if (cur) {
      const completed = cur.completed === true
      isBackendCompleted.value = completed  // 设置后端完成状态标志
      // 根据后端状态设置进度：已完成=1，未完成=后端百分比或0
      readProgress.value = completed ? 1 : (cur.percentage || cur.maxScrollPct || 0)
      // 如果后端已完成，且不需要重置答题状态，则保留已答题记录
      if (completed && !resetQuizState) {
      }
    } else {
      readProgress.value = 0  // 未找到记录，重置为0
      isBackendCompleted.value = false
    }
  } catch (e) {
    console.error('[DocumentViewer]同步文档进度失败', e)
    isBackendCompleted.value = false
  }
}

async function onNext() {
  if (currentIndex.value < flatChapters.value.length - 1) currentIndex.value += 1
  emit('next')
}

// 开始答题
async function startQuiz() {
  if (allQuestionsAnswered.value) {
    summaryVisible.value = true
    return
  }
  try {
    quizStarted.value = true

    const courseId = props.id || props.title
    const qs = await fetchQuestionsOnce(courseId)

    if (!Array.isArray(qs) || qs.length === 0) {
      ElMessage.error('获取题目失败，请重试')
      quizStarted.value = false
      return
    }

    // 打开多题答题弹窗
    showQuizResults.value = false
    documentQuizVisible.value = true
  } catch (e) {
    console.error('[DocumentViewer]开始答题失败:', e)
    ElMessage.error('开始答题失败')
    quizStarted.value = false
  }
}

// 判断题目是否已作答
function isQuestionAnswered(questionId) {
  return answersSoFar.value.some(a => a.questionId === questionId)
}

// 判断题目是否正确
function isQuestionCorrect(questionId) {
  const answer = answersSoFar.value.find(a => a.questionId === questionId)
  if (!answer) return false
  const question = questionList.value.find(q => q.id === questionId)
  if (!question) return false
  return answer.answer === ['A', 'B', 'C', 'D'][question.correct]
}

// 判断选项是否被选中
function isOptionSelected(questionId, optionIndex) {
  const answer = answersSoFar.value.find(a => a.questionId === questionId)
  if (!answer) return false
  return answer.answer === ['A', 'B', 'C', 'D'][optionIndex]
}

// 选择选项
function selectOption(questionId, optionIndex) {
  if (showQuizResults.value) return // 已提交后不允许修改

  const letter = ['A', 'B', 'C', 'D'][optionIndex]
  const existingIndex = answersSoFar.value.findIndex(a => a.questionId === questionId)

  if (existingIndex >= 0) {
    // 已有答案，更新
    answersSoFar.value[existingIndex] = { questionId, answer: letter }
  } else {
    // 新增答案
    answersSoFar.value.push({ questionId, answer: letter })
  }
}

// 提交文档测试答案
async function submitDocumentQuiz() {
  if (answersSoFar.value.length < questionList.value.length) {
    ElMessage.warning('请先完成所有题目')
    return
  }

  try {
    // 先判断并保存错题（在提交答案之前）
    for (const answer of answersSoFar.value) {
      const question = questionList.value.find(q => q.id === answer.questionId)
      if (!question) continue

      const correctLetter = ['A', 'B', 'C', 'D'][question.correct]
      const isCorrect = (answer.answer === correctLetter)

      // 如果答错了，添加到错题本
      if (!isCorrect) {
        await addToWrongQuestionBook(answer.questionId, answer.answer, correctLetter)
      } else {
      }
    }

    // 提交答案到后端
    await submitDocumentAnswersAndProgress()

    // 显示结果
    showQuizResults.value = true
    ElMessage.success('答案已提交')
  } catch (e) {
    console.error('[DocumentViewer]提交答案失败:', e)
    ElMessage.error('提交答案失败')
  }
}

// 关闭文档答题弹窗（只关闭dialog，不关闭整个DocumentViewer）
function closeDocumentQuiz() {
  documentQuizVisible.value = false
}

// 结束文档学习（提交答案并标记完成）
async function finishDocument() {
  await submitDocumentAnswersAndProgress()
  ElMessage.success('恭喜！文档学习已完成')
  // 延迟关闭，让用户看到成功消息
  setTimeout(() => {
    close()
  }, 500)
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

// 文档多题答题相关
const documentQuizVisible = ref(false)
const showQuizResults = ref(false)
const isClosing = ref(false)  
const quizStarted = ref(false)

const allQuestionsAnswered = computed(() => {
  return quizStarted.value && answersSoFar.value.length >= questionList.value.length && questionList.value.length > 0
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

    // 获取当前题目的正确答案
    const currentQuestion = questionList.value.find(q => q.id === qid)
    const correctIndex = currentQuestion?.correct ?? 0
    const correctLetter = ['A','B','C','D'][correctIndex]

    // 判断是否答错
    const isCorrect = (idx === correctIndex)

    if (qid) {
      const existing = answersSoFar.value.findIndex(a => a.questionId === qid)
      if (existing >= 0) answersSoFar.value.splice(existing, 1, { questionId: qid, answer: letter })
      else answersSoFar.value.push({ questionId: qid, answer: letter })

      // 如果答错了，立即添加到错题本
      if (!isCorrect) {
        await addToWrongQuestionBook(qid, letter, correctLetter)
      }

    }
  } catch (e) {
    console.error('[DocumentViewer] 答题处理失败:', e)
  }
  questionVisible.value = false
  if (typeof resolver.value === 'function') {
    resolver.value(true)
    resolver.value = null
  }
}

// 添加错题到错题本
async function addToWrongQuestionBook(questionId, wrongAnswer, correctAnswer) {
  try {
    const studentId = localStorage.getItem('userId')
    const courseId = props.id
    const examIdValue = examId.value

    if (!studentId || !courseId || !questionId || !examIdValue) {
      console.warn('[DocumentViewer] 缺少必要参数，跳过添加错题本', { studentId, courseId, questionId, examId: examIdValue })
      return
    }

    const token = localStorage.getItem('token')

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
    } else {
      console.warn('[DocumentViewer] 错题添加失败:', res.data.message)
    }
  } catch (e) {
    console.error('[DocumentViewer] 添加错题到错题本失败:', e)
  }
}

// 一次性取题：每次打开文档时调用一次
let fetchedForChapter = false
async function fetchQuestionsOnce(courseId) {
  try {
    if (fetchedForChapter && questionList.value?.length > 0) {
      return questionList.value
    }
    
    const studentId = localStorage.getItem('userId')
    const token = localStorage.getItem('token')
    // 获取当前文档的真实 documentId
    const currentChapter = flatChapters.value[currentIndex.value]
    const docId = currentChapter?.documentId
    const body = { courseId, studentId, choiceCount: 5, judgeCount: 0, videoId: null, documentId: docId }
    const res = await axios.post(`${BASE_URL}/aiexam/generate`, body, {
      headers: { 
        'Content-Type': 'application/json', 
        Authorization: `Bearer ${token}` 
      }
    })
    if (res.data.code === 200) {
      const data = res.data.data
      // 保存 examId
      examId.value = (data?.exam && (data.exam.id || data.exam.examId)) || data.id || (Array.isArray(data.questions) ? data.questions[0]?.examId : null) || null
      const list = Array.isArray(data?.questions) ? data.questions : (Array.isArray(data?.choices) ? data.choices : [])
      questionList.value = list.slice(0, 5).map(q => normalizeQuestion(q))
      fetchedForChapter = true
      return questionList.value
    } else {
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

// 文档打开时的计时器
let readTimerInterval = null
let readTimeAccumulator = 0
let lastReadTimeCheck = 0

watch(() => props.modelValue, (v) => {
  if (v) {
    // 文档打开，启动阅读计时器
    lastReadTimeCheck = Date.now()
    readTimeAccumulator = 0
    isReading.value = true

    if (!readTimerInterval) {
      readTimerInterval = setInterval(() => {
        const now = Date.now()
        const delta = Math.floor((now - lastReadTimeCheck) / 1000)
        if (delta > 0) {
          lastReadTimeCheck = now
          readTimeAccumulator += delta
          actualReadTime.value = readTimeAccumulator

          // 每5秒上报一次进度
          if (readTimeAccumulator % 5 === 0) {
            reportReadProgress(delta)
          }

          // 检查是否完成，完成则上报
          if (isCompleted.value) {
            reportReadProgress(delta, true)
          }
        }
      }, 1000)
    }

  } else {
    // 文档关闭，停止计时器
    if (readTimerInterval) {
      clearInterval(readTimerInterval)
      readTimerInterval = null
    }
    isReading.value = false

    // 上报最终进度
    if (readTimeAccumulator > 0) {
      reportReadProgress(readTimeAccumulator, isCompleted.value)
    }

  }
})

// 上报阅读进度
async function reportReadProgress(deltaSec, forceCompleted = false) {
  try {
    const userRole = localStorage.getItem('userRole')
    if (userRole === 'teacher') {
      return
    }

    const studentId = localStorage.getItem('userId')
    const courseId = props.id
    if (!studentId || !courseId) return

    const currentChapter = flatChapters.value[currentIndex.value]
    const docId = currentChapter?.documentId
    if (!docId) {
      return
    }

    const token = localStorage.getItem('token')
    const scrollPct = Number(progressDisplay.value) / 100
    const completed = forceCompleted || isCompleted.value


    const res = await axios.post(`${BASE_URL}/progress/report`, null, {
      params: {
        studentId,
        courseId,
        documentId: docId,
        deltaSec: deltaSec,
        scrollPct: scrollPct,
        completed: completed
      },
      headers: { Authorization: `Bearer ${token}` }
    })

    if (res.data.code === 200) {
    }
  } catch (e) {
    console.error('[DocumentViewer]上报进度失败:', e)
  }
}

onBeforeUnmount(() => {
  if (readTimerInterval) {
    clearInterval(readTimerInterval)
    readTimerInterval = null
  }
})

const fontScale = ref(1)
const modalRef = ref(null)
const bodyRef = ref(null)
const viewRef = ref(null)

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

function updateReadProgress() {
  // 如果后端已经完成，不再更新进度
  if (isBackendCompleted.value) {
    return
  }

  // 对于 PDF，使用页面进度
  if (docType.value === 'pdf' && totalPdfPages.value > 0) {
    const ratio = currentPdfPage.value / totalPdfPages.value
    readProgress.value = Math.max(0, Math.min(1, ratio))
    return
  }

  // 对于其他类型，使用滚动进度
  const el = viewRef.value
  if (!el) return
  const total = Math.max(1, el.scrollHeight - el.clientHeight)
  const ratio = Math.max(0, Math.min(1, el.scrollTop / total))
  readProgress.value = ratio
}

// Ctrl + 滚轮：调整字号
function handleWheel(e) {
  // 检查是否在文档查看器区域内
  if (!props.modelValue || !viewRef.value) {
    return
  }
  
  const targetElement = e.target
  const isInViewer = viewRef.value.contains(targetElement)

  if (!isInViewer) {
    return // 不在文档查看器区域，忽略
  }
  
  // Ctrl + 滚轮：缩放
  if (e.ctrlKey) {
    e.preventDefault()
    e.deltaY > 0 ? decreaseText() : increaseText()
    return
  }
  
  // 普通滚轮：滚动时更新阅读进度
  // 延迟更新进度，让滚动事件先完成
  setTimeout(() => {
    updateReadProgress()
  }, 100)
}

// 滚轮事件现在直接在模板中绑定到 .dv-body 上

// 键盘快捷键：按 End 键直接跳到 100% 进度（测试用）
function handleKeydown(e) {
  if (e.key === 'End') {
    readProgress.value = 1
    // 已移除自动弹题调用
  } else if (e.key === 'Home') {
    readProgress.value = 0
  }
}


watch(() => props.modelValue, (v) => {
  if (v) {
    document.addEventListener('keydown', handleKeydown)
    // 每次打开文档时重置所有状态
    fetchedForChapter = false
    remainingIndex.value = -1
    answersSoFar.value = []
    quizStarted.value = false
    //isClosing.value = false  // 重置关闭标志
    setTimeout(() => {
      updateReadProgress()
    }, 0)
  } else {
    document.removeEventListener('keydown', handleKeydown)
  }
})

// 已移除自动监听滚动进度弹题的逻辑

// 题目弹窗：逐题展示，提交后显示答案与解析，点击继续展示下一题；全部答完后调用"已看完"逻辑
watch(questionVisible, async (v) => {
  if (!v) {
      // 文档正在关闭时不弹出下一题
      if (isClosing.value) {
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
    const userRole = localStorage.getItem('userRole')
    if (userRole === 'teacher') {
      return
    }

    const studentId = localStorage.getItem('userId')
    const eid = examId.value
    const token = localStorage.getItem('token')
    
    // 确保已经答完所有题目
    if (!allQuestionsAnswered.value) {
      return
    }
    
    // 提交答案
    if (eid && studentId && answersSoFar.value.length > 0) {
      const body = {
        examId: eid,
        studentId: studentId,
        answers: answersSoFar.value.map(a => ({ questionId: a.questionId, answer: a.answer }))
      }
      const res = await axios.post(`${BASE_URL}/aiexam/submit`, body, {
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
      })
      if (res.data.code === 200) {
      }
    }
    // 上报文档进度为已完成（所有题目答完后）
    if (studentId && props.id) {
      // 使用当前章节的真实 documentId
      const currentChapter = flatChapters.value[currentIndex.value]
      const docId = currentChapter?.documentId
      if (!docId) {
        return
      }
      const params = {
        studentId: studentId,
        courseId: props.id,
        documentId: docId,
        completed: true
      }
      const res2 = await axios.post(`${BASE_URL}/progress/report`, null, {
        params: params,
        headers: { Authorization: `Bearer ${token}` }
      })
      if (res2.data.code === 200) {
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
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1200;
  backdrop-filter: blur(4px);
}
.dv-modal {
  width: 90vw;
  max-width: 1200px;
  height: 90vh;
  background: #f8f9fa;
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.1);
  transition: all 0.3s ease;
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
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  color: #111827;
  flex-shrink: 0;
}
.dv-title {
  font-weight: 600;
  font-size: 1.1rem;
}
.dv-tools {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
  margin-right: 16px;
}
.dv-tool {
  border: 1px solid #d1d5db;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #fff;
  color: #4b5563;
  cursor: pointer;
  transition: all 0.2s;
}
.dv-tool:hover {
  background: #f3f4f6;
  border-color: #9ca3af;
}
.dv-close {
  border: 0;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #f3f4f6;
  color: #4b5563;
  cursor: pointer;
  transition: all 0.2s;
}
.dv-close:hover {
  background: #e5e7eb;
}
.dv-body {
  flex: 1;
  min-height: 0;
  background: #f8f9fa;
  overflow-y: hidden;
}
.dv-main { display: flex; height: 100%; }
.dv-aside {
  width: 280px;
  flex-shrink: 0;
  background: #fff;
  padding: 16px;
  border-right: 1px solid #e5e7eb;
  overflow-y: auto;
}
.dv-aside-title { 
  font-weight: 600; 
  color: #374151; 
  margin-bottom: 12px; 
  font-size: 1rem;
}
.dv-aside-list { max-height: calc(100% - 40px); overflow-y: auto; }
.dv-aside-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px; border-radius: 8px; cursor: pointer; margin-bottom: 4px;
  transition: all 0.2s;
}
.dv-aside-item i { color: #6b7280; font-size: 16px; }
.dv-aside-item:hover { background: #f3f4f6; }
.dv-aside-item.active { background: #eef2ff; color: #312e81; font-weight: 600; }
.dv-aside-item.active i { color: #4338ca; }
.dv-aside-text { 
  flex: 1; 
  white-space: nowrap; 
  overflow: hidden; 
  text-overflow: ellipsis; 
  font-size: 0.9rem;
  line-height: 1.5;
}
.dv-view { flex: 1; min-width: 0; overflow: auto; background-color: #f8f9fa; }
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
  background: #fff;
}
.dv-content {
  margin: 24px auto;
  padding: 32px;
  background: #fff;
  border-radius: 8px;
  max-width: 800px;
  width: 90%;
  color: #374151;
  line-height: 1.8;
  box-shadow: 0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1);
}
.dv-content :deep(img),
.dv-content :deep(video),
.dv-content :deep(canvas),
.dv-content :deep(table) {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
}
.dv-content :deep(pre) {
  white-space: pre-wrap;
  word-break: break-all;
  background: #1f2937;
  color: #f3f4f6;
  padding: 16px;
  border-radius: 8px;
}
.dv-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: #fff;
  border-top: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.dv-pill {
  padding: 8px 16px;
  background: #f3f4f6;
  color: #4b5563;
  border: 1px solid #d1d5db;
  border-radius: 999px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
}
.dv-pill.done {
  background: #ecfdf5;
  border-color: #a7f3d0;
  color: #047857;
}
.dv-pill.todo {
  background: #fef2f2;
  border-color: #fecaca;
  color: #b91c1c;
}
.dv-actions { display: flex; align-items: center; gap: 12px; }
.dv-btn {
  padding: 10px 20px;
  font-weight: 500;
  background: #2563eb;
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.dv-btn-secondary {
  background: #f9fafb;
  color: #374151;
  border: 1px solid #d1d5db;
}
.dv-btn-secondary:hover {
  background: #f3f4f6;
}
.dv-btn-quiz {
  background: #10b981;
}
.dv-btn-quiz:hover {
  background: #059669;
}
.dv-btn-quiz:disabled {
  background: #a7f3d0;
  color: #065f46;
  cursor: not-allowed;
  opacity: 0.8;
}
.dv-btn-finish {
  background: #f59e0b;
}
.dv-btn-finish:hover {
  background: #d97706;
}
.dv-btn:hover {
  filter: brightness(1.1);
}
.dv-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  filter: none;
}

/* ========== 新增：PDF/DOCX/TXT 原生渲染样式 ========== */

/* 加载状态 */
.dv-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  height: 100%;
  color: #6b7280;
  font-size: 16px;
}

.dv-loading i {
  font-size: 48px;
  color: #3b82f6;
}

/* 错误状态 */
.dv-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  height: 100%;
  color: #ef4444;
}

.dv-error i {
  font-size: 48px;
}

.dv-error p {
  color: #6b7280;
  margin: 0;
}

/* 不支持的格式 */
.dv-unsupported {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  height: 100%;
  color: #6b7280;
}

.dv-unsupported i {
  font-size: 64px;
  color: #d1d5db;
}

.dv-unsupported p {
  margin: 0;
  font-size: 16px;
}

.dv-hint {
  font-size: 14px;
  color: #9ca3af;
}

/* PDF 容器 */
.dv-pdf-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px;
  min-height: 100%;
  background: #5f6368;
}

.dv-pdf-toolbar {
  position: sticky;
  top: 0;
  z-index: 10;
  background: rgba(255, 255, 255, 0.95);
  padding: 8px 16px;
  border-radius: 8px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.dv-page-info {
  font-size: 14px;
  color: #374151;
  font-weight: 500;
  min-width: 60px;
  text-align: center;
}

.dv-tool-sm {
  width: 28px;
  height: 28px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #fff;
  color: #4b5563;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.dv-tool-sm:hover:not(:disabled) {
  background: #f3f4f6;
  border-color: #9ca3af;
}

.dv-tool-sm:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.dv-pdf {
  background: #fff;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  max-width: 900px;
  width: 100%;
  transform-origin: top center;
  transition: transform 0.3s ease;
}

/* DOCX 内容样式 */
.dv-docx-content {
  line-height: 1.8;
}

.dv-docx-content :deep(h1),
.dv-docx-content :deep(h2),
.dv-docx-content :deep(h3) {
  margin-top: 1.5em;
  margin-bottom: 0.75em;
  font-weight: 600;
  color: #1f2937;
}

.dv-docx-content :deep(p) {
  margin-bottom: 1em;
}

.dv-docx-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
}

.dv-docx-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1em 0;
}

.dv-docx-content :deep(table td),
.dv-docx-content :deep(table th) {
  border: 1px solid #d1d5db;
  padding: 8px 12px;
}

/* TXT 内容样式 */
.dv-txt-content {
  font-family: 'Courier New', Courier, monospace;
  white-space: pre-wrap;
  word-wrap: break-word;
  line-height: 1.6;
  margin: 0;
}

/* 文档多题答题界面样式 */
.document-quiz-dialog .quiz-header-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f3f4f6;
  border-radius: 8px;
  margin-bottom: 16px;
  font-size: 14px;
  font-weight: 500;
}

.quiz-questions-list {
  max-height: 500px;
  overflow-y: auto;
  padding-right: 8px;
}

.quiz-question-item {
  padding: 16px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  margin-bottom: 16px;
  transition: all 0.2s;
}

.quiz-question-item.answered {
  background: #ecfdf5;
  border-color: #a7f3d0;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.question-number {
  font-weight: 600;
  font-size: 16px;
  color: #1f2937;
}

.question-status {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}

.answered-badge {
  background: #d1fae5;
  color: #065f46;
}

.pending-badge {
  background: #fef3c7;
  color: #92400e;
}

.question-stem {
  font-size: 15px;
  line-height: 1.6;
  color: #374151;
  margin-bottom: 12px;
  font-weight: 500;
}

.question-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quiz-option-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: #fff;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.quiz-option-item:hover {
  border-color: #3b82f6;
  background: #eff6ff;
}

.quiz-option-item.selected {
  border-color: #3b82f6;
  background: #dbeafe;
}

.option-label {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  border-radius: 50%;
  font-weight: 600;
  font-size: 13px;
  color: #4b5563;
}

.quiz-option-item.selected .option-label {
  background: #3b82f6;
  color: #fff;
}

.option-text {
  flex: 1;
  font-size: 14px;
  color: #374151;
}

.quiz-result-panel {
  margin-top: 12px;
  padding: 12px;
  border-radius: 6px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

.quiz-result-panel.correct {
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
}

.quiz-result-panel.wrong {
  background: #fef2f2;
  border: 1px solid #fecaca;
}

.result-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 8px;
}

.quiz-result-panel.correct .result-title {
  color: #047857;
}

.quiz-result-panel.wrong .result-title {
  color: #b91c1c;
}

.correct-answer {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 8px;
}

.quiz-analysis {
  font-size: 13px;
  color: #4b5563;
  line-height: 1.5;
}

/* 自定义滚动条 */
.quiz-questions-list::-webkit-scrollbar {
  width: 6px;
}

.quiz-questions-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.quiz-questions-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.quiz-questions-list::-webkit-scrollbar-thumb:hover {
  background: #a1a1a1;
}
</style>
