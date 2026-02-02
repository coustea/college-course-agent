<template>
  <div v-if="modelValue" class="edv-mask" @click.self="close">
    <div class="edv-modal" ref="modalRef">
      <!-- 顶部工具栏 -->
      <div class="edv-header">
        <div class="edv-title">{{ headerTitle }}</div>
        <div class="edv-tools" v-if="docType === 'pdf'">
          <button class="edv-tool" @click="zoomOut" title="缩小"><i class="fas fa-search-minus"></i></button>
          <button class="edv-tool" @click="zoomIn" title="放大"><i class="fas fa-search-plus"></i></button>
          <span class="edv-page-info">{{ currentPage }} / {{ totalPages }}</span>
          <button class="edv-tool" @click="prevPage" :disabled="currentPage <= 1" title="上一页"><i class="fas fa-chevron-left"></i></button>
          <button class="edv-tool" @click="nextPage" :disabled="currentPage >= totalPages" title="下一页"><i class="fas fa-chevron-right"></i></button>
        </div>
        <div class="edv-tools" v-else-if="docType === 'docx' || docType === 'txt'">
          <button class="edv-tool" @click="zoomOut" title="缩小"><i class="fas fa-search-minus"></i></button>
          <button class="edv-tool" @click="zoomIn" title="放大"><i class="fas fa-search-plus"></i></button>
        </div>
        <button class="edv-tool" @click="toggleFullscreen" title="全屏"><i class="fas fa-expand-arrows-alt"></i></button>
        <button class="edv-close" @click="close" title="关闭"><i class="fas fa-times"></i></button>
      </div>

      <!-- 主内容区 -->
      <div class="edv-body" ref="bodyRef">
        <!-- 加载状态 -->
        <div v-if="loading" class="edv-loading">
          <i class="fas fa-spinner fa-spin"></i>
          <span>文档加载中...</span>
        </div>

        <!-- 错误状态 -->
        <div v-else-if="error" class="edv-error">
          <i class="fas fa-exclamation-triangle"></i>
          <p>{{ error }}</p>
          <button class="edv-btn" @click="downloadFile">下载文档</button>
        </div>

        <!-- PDF渲染 -->
        <div v-else-if="docType === 'pdf'" class="edv-content edv-pdf-container">
          <VuePdfEmbed
            v-if="pdfSource"
            :source="pdfSource"
            :page="currentPage"
            class="edv-pdf"
            :style="{ transform: `scale(${scale})` }"
          />
        </div>

        <!-- DOCX渲染 -->
        <div v-else-if="docType === 'docx'" class="edv-content edv-docx-container">
          <div
            class="edv-docx-content"
            v-html="docxHtml"
            :style="{ fontSize: `${baseFontSize * scale}px` }"
          ></div>
        </div>

        <!-- TXT渲染 -->
        <div v-else-if="docType === 'txt'" class="edv-content edv-txt-container">
          <pre
            class="edv-txt-content"
            :style="{ fontSize: `${baseFontSize * scale}px` }"
          >{{ txtContent }}</pre>
        </div>

        <!-- 不支持的格式 -->
        <div v-else class="edv-unsupported">
          <i class="fas fa-file"></i>
          <p>暂不支持预览此格式</p>
          <p class="edv-hint">支持的格式：PDF、DOCX、TXT</p>
          <button class="edv-btn" @click="downloadFile">下载文档</button>
        </div>
      </div>

      <!-- 底部信息 -->
      <div class="edv-footer">
        <div class="edv-info">
          <el-tag :type="docTypeTag">{{ docTypeUpper }}</el-tag>
          <span class="edv-filename">{{ fileName }}</span>
        </div>
        <div class="edv-actions">
          <button class="edv-btn edv-btn-secondary" @click="downloadFile">
            <i class="fas fa-download"></i> 下载
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, getCurrentInstance } from 'vue'
import { ElMessage } from 'element-plus'
import VuePdfEmbed from 'vue-pdf-embed'
import mammoth from 'mammoth'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '文档预览' },
  fileUrl: { type: String, default: '' },
  courseTitle: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue'])
const { proxy } = getCurrentInstance()
const BASE_URL = proxy?.$baseUrl || ''

// 状态
const loading = ref(true)
const error = ref('')
const docType = ref('')
const pdfSource = ref(null)
const docxHtml = ref('')
const txtContent = ref('')

// PDF相关
const currentPage = ref(1)
const totalPages = ref(0)
const scale = ref(1)

// 通用
const baseFontSize = ref(14)
const isFullscreen = ref(false)
const modalRef = ref(null)

const fileName = computed(() => {
  const url = props.fileUrl || ''
  const parts = url.split('/')
  return parts[parts.length - 1] || props.title
})

const headerTitle = computed(() => {
  return props.courseTitle ? `${props.courseTitle} - ${props.title}` : props.title
})

const docTypeUpper = computed(() => docType.value.toUpperCase())

const docTypeTag = computed(() => {
  const map = {
    pdf: 'danger',
    docx: 'warning',
    txt: 'success'
  }
  return map[docType.value] || 'info'
})

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
  loading.value = true
  error.value = ''

  const type = detectDocType(props.fileUrl)
  docType.value = type

  if (!type) {
    error.value = '不支持的文件格式'
    loading.value = false
    return
  }

  try {
    switch (type) {
      case 'pdf':
        await loadPdf()
        break
      case 'docx':
        await loadDocx()
        break
      case 'txt':
        await loadTxt()
        break
    }
  } catch (e) {
    console.error('加载文档失败:', e)
    error.value = `加载失败: ${e.message}`
  } finally {
    loading.value = false
  }
}

// 加载PDF
async function loadPdf() {
  try {
    pdfSource.value = props.fileUrl
    // PDF加载是异步的，总页数会在渲染后获取
    ElMessage.success('PDF加载成功')
  } catch (e) {
    throw new Error('PDF加载失败')
  }
}

// 加载DOCX
async function loadDocx() {
  try {
    const response = await fetch(props.fileUrl)
    if (!response.ok) throw new Error('文件下载失败')

    const arrayBuffer = await response.arrayBuffer()
    const result = await mammoth.convertToHtml({ arrayBuffer })

    if (result.messages.length > 0) {
      console.warn('DOCX转换警告:', result.messages)
    }

    docxHtml.value = result.value
    ElMessage.success('文档加载成功')
  } catch (e) {
    throw new Error('DOCX加载失败: ' + e.message)
  }
}

// 加载TXT
async function loadTxt() {
  try {
    const response = await fetch(props.fileUrl)
    if (!response.ok) throw new Error('文件下载失败')

    const text = await response.text()
    txtContent.value = text
    ElMessage.success('文档加载成功')
  } catch (e) {
    throw new Error('TXT加载失败: ' + e.message)
  }
}

// 下载文档
function downloadFile() {
  const link = document.createElement('a')
  link.href = props.fileUrl
  link.download = fileName.value
  link.target = '_blank'
  link.click()
}

// PDF控制
function zoomIn() {
  scale.value = Math.min(2, scale.value + 0.1)
}

function zoomOut() {
  scale.value = Math.max(0.5, scale.value - 0.1)
}

function prevPage() {
  if (currentPage.value > 1) {
    currentPage.value--
  }
}

function nextPage() {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
  }
}

// 全屏控制
function toggleFullscreen() {
  const el = modalRef.value
  if (!el) return

  if (!document.fullscreenElement) {
    el.requestFullscreen?.()
    isFullscreen.value = true
  } else {
    document.exitFullscreen?.()
    isFullscreen.value = false
  }
}

function handleFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement
}

// 关闭
function close() {
  emit('update:modelValue', false)
}

// 监听打开
watch(() => props.modelValue, (v) => {
  if (v) {
    loadDocument()
  } else {
    // 重置状态
    loading.value = true
    error.value = ''
    docType.value = ''
    pdfSource.value = null
    docxHtml.value = ''
    txtContent.value = ''
    currentPage.value = 1
    totalPages.value = 0
    scale.value = 1
  }
})

// 监听PDF总页数变化（vue-pdf-embed会在渲染后通过事件或ref获取）
// 这里需要等待PDF加载完成后再获取总页数
watch(pdfSource, async () => {
  if (pdfSource.value && docType.value === 'pdf') {
    // PDF.js会在加载后提供总页数，这里暂时设置为默认值
    // 实际使用中可能需要通过PDF.js的API获取
    totalPages.value = 999 // 占位值，需要实际获取
  }
})

onMounted(() => {
  document.addEventListener('fullscreenchange', handleFullscreenChange)
})

onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
})
</script>

<style scoped>
.edv-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  backdrop-filter: blur(4px);
}

.edv-modal {
  width: 90vw;
  max-width: 1400px;
  height: 90vh;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
}

.edv-modal:fullscreen {
  width: 100vw;
  height: 100vh;
  max-width: none;
  border-radius: 0;
}

/* 顶部工具栏 */
.edv-header {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  background: #f8f9fa;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
  gap: 16px;
}

.edv-title {
  flex: 1;
  font-weight: 600;
  font-size: 16px;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.edv-tools {
  display: flex;
  align-items: center;
  gap: 8px;
}

.edv-page-info {
  padding: 0 12px;
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
}

.edv-tool,
.edv-close {
  border: 1px solid #d1d5db;
  width: 36px;
  height: 36px;
  border-radius: 6px;
  background: #fff;
  color: #4b5563;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.edv-tool:hover:not(:disabled),
.edv-close:hover {
  background: #f3f4f6;
  border-color: #9ca3af;
}

.edv-tool:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.edv-close {
  background: #fef2f2;
  border-color: #fecaca;
  color: #dc2626;
}

.edv-close:hover {
  background: #fee2e2;
}

/* 主体内容区 */
.edv-body {
  flex: 1;
  overflow: auto;
  background: #5f6368;
  position: relative;
}

.edv-content {
  min-height: 100%;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 24px;
}

/* PDF容器 */
.edv-pdf-container {
  background: #fff;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.edv-pdf {
  width: 100%;
  max-width: 900px;
  margin: 0 auto;
  transform-origin: top center;
  transition: transform 0.3s ease;
}

/* DOCX容器 */
.edv-docx-container {
  background: #fff;
  width: 100%;
  max-width: 900px;
  margin: 0 auto;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.edv-docx-content {
  padding: 40px;
  line-height: 1.8;
  color: #1f2937;
}

.edv-docx-content :deep(h1),
.edv-docx-content :deep(h2),
.edv-docx-content :deep(h3) {
  margin-top: 1.5em;
  margin-bottom: 0.75em;
  font-weight: 600;
}

.edv-docx-content :deep(p) {
  margin-bottom: 1em;
}

.edv-docx-content :deep(img) {
  max-width: 100%;
  height: auto;
}

.edv-docx-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1em 0;
}

.edv-docx-content :deep(table td),
.edv-docx-content :deep(table th) {
  border: 1px solid #d1d5db;
  padding: 8px 12px;
}

/* TXT容器 */
.edv-txt-container {
  background: #fff;
  width: 100%;
  max-width: 900px;
  margin: 0 auto;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.edv-txt-content {
  padding: 24px;
  margin: 0;
  font-family: 'Courier New', Courier, monospace;
  white-space: pre-wrap;
  word-wrap: break-word;
  color: #1f2937;
  line-height: 1.6;
}

/* 加载、错误、不支持状态 */
.edv-loading,
.edv-error,
.edv-unsupported {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  height: 100%;
  color: #fff;
}

.edv-loading i,
.edv-error i,
.edv-unsupported i {
  font-size: 48px;
}

.edv-error p,
.edv-unsupported p {
  font-size: 16px;
  margin: 0;
}

.edv-hint {
  font-size: 14px;
  color: #d1d5db;
}

.edv-btn {
  padding: 10px 20px;
  background: #3b82f6;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.edv-btn:hover {
  background: #2563eb;
}

.edv-btn-secondary {
  background: #6b7280;
}

.edv-btn-secondary:hover {
  background: #4b5563;
}

/* 底部 */
.edv-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: #f8f9fa;
  border-top: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.edv-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.edv-filename {
  font-size: 14px;
  color: #6b7280;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 500px;
}

.edv-actions {
  display: flex;
  gap: 12px;
}
</style>
