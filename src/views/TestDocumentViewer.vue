<template>
  <div class="test-page">
    <div class="test-header">
      <h1>文档查看器测试页面</h1>
      <p>支持 PDF、DOCX、TXT 格式</p>
    </div>

    <div class="test-section">
      <h2>测试文档列表</h2>
      <div class="test-grid">
        <!-- PDF测试 -->
        <div class="test-card" v-for="doc in testDocuments" :key="doc.id">
          <div class="card-icon" :class="doc.type">
            <i :class="doc.icon"></i>
          </div>
          <div class="card-info">
            <h3>{{ doc.title }}</h3>
            <p>{{ doc.description }}</p>
            <el-tag :type="doc.tagType">{{ doc.type.toUpperCase() }}</el-tag>
          </div>
          <button class="test-btn" @click="openViewer(doc)">
            <i class="fas fa-eye"></i> 预览
          </button>
        </div>

        <!-- 自定义URL测试 -->
        <div class="test-card custom-url">
          <div class="card-icon">
            <i class="fas fa-link"></i>
          </div>
          <div class="card-info">
            <h3>自定义文档URL</h3>
            <el-input
              v-model="customUrl"
              placeholder="输入文档URL"
              size="large"
            />
          </div>
          <button class="test-btn" @click="openCustomUrl">
            <i class="fas fa-eye"></i> 预览
          </button>
        </div>
      </div>
    </div>

    <!-- 文档查看器 -->
    <EnhancedDocumentViewer
      v-model="showViewer"
      :file-url="currentDoc?.url"
      :title="currentDoc?.title"
      course-title="测试课程"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import EnhancedDocumentViewer from '@/components/EnhancedDocumentViewer.vue'
import { ElMessage } from 'element-plus'

const showViewer = ref(false)
const currentDoc = ref(null)
const customUrl = ref('')

// 测试文档列表（请根据实际文档路径修改）
const testDocuments = ref([
  {
    id: 1,
    title: 'PDF测试文档',
    description: '示例PDF文档',
    type: 'pdf',
    tagType: 'danger',
    icon: 'fas fa-file-pdf',
    url: '/uploads/sample.pdf' // 替换为实际的PDF文档路径
  },
  {
    id: 2,
    title: 'Word测试文档',
    description: '示例Word文档',
    type: 'docx',
    tagType: 'warning',
    icon: 'fas fa-file-word',
    url: '/uploads/sample.docx' // 替换为实际的DOCX文档路径
  },
  {
    id: 3,
    title: '文本测试文档',
    description: '示例文本文件',
    type: 'txt',
    tagType: 'success',
    icon: 'fas fa-file-alt',
    url: '/uploads/sample.txt' // 替换为实际的TXT文档路径
  }
])

function openViewer(doc) {
  if (!doc.url) {
    ElMessage.warning('请先设置文档URL')
    return
  }
  currentDoc.value = doc
  showViewer.value = true
}

function openCustomUrl() {
  if (!customUrl.value) {
    ElMessage.warning('请输入文档URL')
    return
  }

  const ext = customUrl.value.split('.').pop().toLowerCase()
  const typeMap = {
    'pdf': { type: 'pdf', tagType: 'danger', icon: 'fas fa-file-pdf' },
    'doc': { type: 'docx', tagType: 'warning', icon: 'fas fa-file-word' },
    'docx': { type: 'docx', tagType: 'warning', icon: 'fas fa-file-word' },
    'txt': { type: 'txt', tagType: 'success', icon: 'fas fa-file-alt' },
    'md': { type: 'txt', tagType: 'success', icon: 'fas fa-file-alt' }
  }

  const config = typeMap[ext] || { type: 'unknown', tagType: 'info', icon: 'fas fa-file' }

  currentDoc.value = {
    url: customUrl.value,
    title: `自定义文档 (${ext.toUpperCase()})`,
    type: config.type,
    tagType: config.tagType,
    icon: config.icon
  }
  showViewer.value = true
}
</script>

<style scoped>
.test-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 40px 24px;
}

.test-header {
  text-align: center;
  margin-bottom: 48px;
}

.test-header h1 {
  font-size: 32px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 12px;
}

.test-header p {
  font-size: 16px;
  color: #6b7280;
}

.test-section h2 {
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 24px;
}

.test-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}

.test-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  transition: all 0.3s;
}

.test-card:hover {
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.card-icon {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
}

.card-icon.pdf {
  background: #fef2f2;
  color: #dc2626;
}

.card-icon.docx {
  background: #fffbeb;
  color: #f59e0b;
}

.card-icon.txt {
  background: #f0fdf4;
  color: #16a34a;
}

.card-icon:not(.pdf):not(.docx):not(.txt) {
  background: #f3f4f6;
  color: #6b7280;
}

.card-info {
  flex: 1;
}

.card-info h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
}

.card-info p {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 12px;
}

.test-btn {
  padding: 12px 24px;
  background: #3b82f6;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s;
}

.test-btn:hover {
  background: #2563eb;
}

.custom-url {
  grid-column: 1 / -1;
}
</style>
