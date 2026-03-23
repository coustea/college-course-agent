<template>
  <div class="ai-chat-container">
    <!-- 侧边栏 - 历史会话 -->
    <div class="chat-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <h3 v-if="!sidebarCollapsed">历史会话</h3>
        <el-button 
          :icon="sidebarCollapsed ? Expand : Fold" 
          circle 
          size="small"
          @click="sidebarCollapsed = !sidebarCollapsed"
        />
      </div>
      <div class="sidebar-content" v-if="!sidebarCollapsed">
        <el-button type="primary" class="new-chat-btn" @click="startNewChat">
          <el-icon><Plus /></el-icon>
          新建对话
        </el-button>
        <div class="history-list">
          <div 
            v-for="chat in chatHistory" 
            :key="chat.id"
            class="history-item"
            :class="{ active: currentChatId === chat.id }"
            @click="loadChat(chat.id)"
          >
            <el-icon><ChatDotRound /></el-icon>
            <span class="history-title">{{ chat.title }}</span>
            <el-button 
              :icon="Delete" 
              circle 
              size="small" 
              class="delete-btn"
              @click.stop="deleteChat(chat.id)"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 主聊天区域 -->
    <div class="chat-main">
      <!-- 消息列表 -->
      <div class="messages-container" ref="messagesContainer">
        <div v-if="messages.length === 0" class="welcome-screen">
          <div class="welcome-icon">
            <el-icon :size="64"><ChatDotRound /></el-icon>
          </div>
          <h2>AI 教学助手</h2>
          <p>我可以帮助您解答教学问题、分析学生数据、生成教案等</p>
          <div class="quick-prompts">
            <div class="prompt-card" @click="sendQuickPrompt('帮我分析一下学生的学习情况')">
              <el-icon><DataAnalysis /></el-icon>
              <span>分析学生学习情况</span>
            </div>
            <div class="prompt-card" @click="sendQuickPrompt('帮我生成一份课程教案')">
              <el-icon><Document /></el-icon>
              <span>生成课程教案</span>
            </div>
            <div class="prompt-card" @click="sendQuickPrompt('如何提高学生的学习积极性？')">
              <el-icon><QuestionFilled /></el-icon>
              <span>教学方法建议</span>
            </div>
          </div>
        </div>

        <div v-else class="messages-list">
          <div
            v-for="(msg, index) in messages"
            :key="index"
            class="message-item"
            :class="msg.role"
          >
            <div class="message-wrapper">
              <div class="message-avatar">
                <span v-if="msg.role === 'assistant'" class="avatar-label">AI 助手</span>
                <el-avatar v-if="msg.role === 'user'" :size="32" :icon="User" />
                <el-avatar v-else :size="32" class="ai-avatar">
                  <el-icon><ChatDotRound /></el-icon>
                </el-avatar>
              </div>
              <div class="message-content">
                <!-- 思考中动画 -->
                <div v-if="msg.role === 'assistant' && !msg.content" class="thinking-animation">
                  <div class="thinking-dots">
                    <span class="dot"></span>
                    <span class="dot"></span>
                    <span class="dot"></span>
                  </div>
                  <div class="thinking-text">AI正在思考中...</div>
                </div>
                <!-- 消息内容 -->
                <div v-else class="message-text" v-html="renderMarkdown(msg.content)"></div>
                <!-- 附件显示 -->
                <div v-if="msg.files && msg.files.length" class="message-files">
                  <div v-for="file in msg.files" :key="file.name" class="file-tag">
                    <el-icon><Document /></el-icon>
                    {{ file.name }}
                  </div>
                </div>
                <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
              </div>
            </div>
          </div>
          <!-- 加载中指示器 - 仅在非流式输出时显示 -->
          <div v-if="isLoading && messages.length === 0" class="message-item assistant">
            <div class="message-wrapper">
              <div class="message-avatar">
                <span class="avatar-label">AI 助手</span>
                <el-avatar :size="32" class="ai-avatar">
                  <el-icon><ChatDotRound /></el-icon>
                </el-avatar>
              </div>
              <div class="message-content">
                <div class="typing-indicator">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-area">
        <!-- 文件预览 -->
        <div v-if="uploadedFiles.length" class="uploaded-files">
          <div v-for="(file, index) in uploadedFiles" :key="index" class="file-preview">
            <el-icon><Document /></el-icon>
            <span>{{ file.name }}</span>
            <el-button :icon="Close" circle size="small" @click="removeFile(index)" />
          </div>
        </div>
        
        <div class="input-wrapper">
          <el-button 
            :icon="Upload" 
            circle 
            class="upload-btn"
            @click="triggerFileUpload"
          />
          <input 
            type="file" 
            ref="fileInput" 
            multiple 
            @change="handleFileUpload"
            style="display: none"
            accept=".txt,.pdf,.doc,.docx,.xls,.xlsx,.png,.jpg,.jpeg"
          />
          <el-input
            v-model="inputMessage"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 6 }"
            placeholder="输入消息，按 Enter 发送，Shift+Enter 换行..."
            @keydown="handleKeydown"
            :disabled="isLoading"
          />
          <el-button 
            type="primary" 
            :icon="Promotion" 
            circle 
            class="send-btn"
            :disabled="!canSend"
            @click="sendMessage"
          />
        </div>
        <div class="input-tips">
          支持上传 txt、pdf、doc、docx、xls、xlsx、png、jpg 等文件
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted, reactive } from 'vue'
import {
  Plus, Delete, Fold, Expand, Upload, Promotion, Close,
  ChatDotRound, User, Document, DataAnalysis, QuestionFilled
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { sendChatStream } from '@/services/chatApi'

// 获取当前用户ID，用于隔离不同用户的聊天记录
const getUserId = () => {
  return localStorage.getItem('userId') || localStorage.getItem('teacherId') || 'default'
}

const getStorageKey = () => {
  return `aiChatHistory_${getUserId()}`
}

// 状态
const sidebarCollapsed = ref(false)
const messages = ref([])
const inputMessage = ref('')
const isLoading = ref(false)
const uploadedFiles = ref([])
const chatHistory = ref([])
const currentChatId = ref(null)
const messagesContainer = ref(null)
const fileInput = ref(null)
const cancelStream = ref(null) // 用于取消正在进行的流式请求

// 计算属性
const canSend = computed(() => {
  return (inputMessage.value.trim() || uploadedFiles.value.length) && !isLoading.value
})

// 格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 简单的 Markdown 渲染
const renderMarkdown = (text) => {
  if (!text) return ''
  return text
    .replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code class="language-$1">$2</code></pre>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>')
    .replace(/\n/g, '<br>')
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// 文件上传
const triggerFileUpload = () => {
  fileInput.value?.click()
}

const handleFileUpload = (event) => {
  const files = Array.from(event.target.files || [])
  const maxSize = 10 * 1024 * 1024 // 10MB
  
  files.forEach(file => {
    if (file.size > maxSize) {
      ElMessage.warning(`文件 ${file.name} 超过10MB限制`)
      return
    }
    uploadedFiles.value.push(file)
  })
  
  event.target.value = ''
}

const removeFile = (index) => {
  uploadedFiles.value.splice(index, 1)
}

// 发送消息
const sendMessage = async () => {
  if (!canSend.value) return

  const content = inputMessage.value.trim()
  const files = [...uploadedFiles.value]

  // 检查是否有token
  const token = localStorage.getItem('token') || localStorage.getItem('userToken')
  if (!token) {
    ElMessage.error('您尚未登录，请先登录后再使用AI聊天功能')
    return
  }

  console.log('发送消息:', content, '文件数:', files.length, 'token:', token ? 'exists' : 'missing')

  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: content || '(已上传文件)',
    files: files.map(f => ({ name: f.name, size: f.size })),
    timestamp: Date.now()
  })

  inputMessage.value = ''
  uploadedFiles.value = []
  scrollToBottom()

  // 开始流式响应
  isLoading.value = true

  try {
    await streamResponse(content, files)
  } catch (error) {
    console.error('AI 响应错误:', error)
    ElMessage.error('AI 响应失败: ' + (error.message || '未知错误'))
    messages.value.push({
      role: 'assistant',
      content: '抱歉，发生了错误：' + (error.message || '未知错误') + '\n\n请检查：\n1. 是否已登录\n2. 后端服务是否正常运行\n3. 浏览器控制台是否有详细错误信息',
      timestamp: Date.now()
    })
  } finally {
    isLoading.value = false
    scrollToBottom()
    saveChatHistory()
  }
}

// 流式响应 - 使用真实的后端API
const streamResponse = async (content, files) => {
  // 创建 AI 消息占位
  const aiMessage = reactive({
    role: 'assistant',
    content: '',
    timestamp: Date.now()
  })
  messages.value.push(aiMessage)

  console.log('开始流式响应, conversationId:', currentChatId.value)

  // 调用后端流式API
  cancelStream.value = sendChatStream(
    {
      message: content,
      files: files,
      conversationId: currentChatId.value
    },
    {
      onMessage: (data) => {
        console.log('收到流式消息:', data)
        // 处理流式消息 - 直接修改响应式对象
        if (data.content) {
          aiMessage.content += data.content
          // 强制更新视图
          nextTick(() => {
            scrollToBottom()
          })
        }
      },
      onError: (error) => {
        console.error('AI 响应错误:', error)
        ElMessage.error('AI 响应失败: ' + (error.message || '未知错误'))
        aiMessage.content = '抱歉，发生了错误：' + (error.message || '未知错误')
      },
      onComplete: () => {
        console.log('Stream completed')
        cancelStream.value = null
        isLoading.value = false
        saveChatHistory()
      }
    }
  )
}

// 快捷提示
const sendQuickPrompt = (prompt) => {
  inputMessage.value = prompt
  sendMessage()
}

// 键盘事件
const handleKeydown = (event) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

// 聊天历史管理
const startNewChat = () => {
  if (messages.value.length > 0) {
    saveChatHistory()
  }
  currentChatId.value = Date.now().toString()
  messages.value = []
}

const saveChatHistory = () => {
  if (messages.value.length === 0) return
  
  const chatId = currentChatId.value || Date.now().toString()
  const title = messages.value[0]?.content?.slice(0, 20) || '新对话'
  
  const existingIndex = chatHistory.value.findIndex(c => c.id === chatId)
  const chatData = {
    id: chatId,
    title: title + (title.length >= 20 ? '...' : ''),
    messages: messages.value,
    updatedAt: Date.now()
  }
  
  if (existingIndex >= 0) {
    chatHistory.value[existingIndex] = chatData
  } else {
    chatHistory.value.unshift(chatData)
  }
  
  currentChatId.value = chatId
  localStorage.setItem(getStorageKey(), JSON.stringify(chatHistory.value))
}

const loadChat = (chatId) => {
  const chat = chatHistory.value.find(c => c.id === chatId)
  if (chat) {
    currentChatId.value = chatId
    messages.value = [...chat.messages]
    scrollToBottom()
  }
}

const deleteChat = (chatId) => {
  chatHistory.value = chatHistory.value.filter(c => c.id !== chatId)
  localStorage.setItem(getStorageKey(), JSON.stringify(chatHistory.value))
  
  if (currentChatId.value === chatId) {
    currentChatId.value = null
    messages.value = []
  }
}

// 初始化
onMounted(() => {
  const saved = localStorage.getItem(getStorageKey())
  if (saved) {
    try {
      chatHistory.value = JSON.parse(saved)
    } catch {}
  }
})

// 组件卸载时取消正在进行的请求
onUnmounted(() => {
  if (cancelStream.value) {
    cancelStream.value()
    cancelStream.value = null
  }
})
</script>

<style scoped>
.ai-chat-container {
  display: flex;
  height: calc(100vh - 60px);
  background: #f5f7fa;
}

/* 侧边栏 */
.chat-sidebar {
  width: 260px;
  background: #fff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
}

.chat-sidebar.collapsed {
  width: 50px;
}

.sidebar-header {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e4e7ed;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.sidebar-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.new-chat-btn {
  width: 100%;
  margin-bottom: 12px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.history-item:hover {
  background: #f5f7fa;
}

.history-item.active {
  background: #ecf5ff;
  color: #409eff;
}

.history-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.delete-btn {
  opacity: 0;
  transition: opacity 0.2s;
}

.history-item:hover .delete-btn {
  opacity: 1;
}

/* 主聊天区域 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

/* 欢迎界面 */
.welcome-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  color: #606266;
}

.welcome-icon {
  color: #409eff;
  margin-bottom: 20px;
}

.welcome-screen h2 {
  margin: 0 0 12px;
  font-size: 24px;
  color: #303133;
}

.welcome-screen p {
  margin: 0 0 30px;
  font-size: 14px;
}

.quick-prompts {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  justify-content: center;
}

.prompt-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.prompt-card:hover {
  border-color: #409eff;
  color: #409eff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1);
}

/* 消息列表 */
.messages-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-width: 900px;
  margin: 0 auto;
}

.message-item {
  display: flex;
}

.message-item.user {
  justify-content: flex-end;
}

.message-wrapper {
  display: flex;
  flex-direction: column;
  max-width: 70%;
}

.message-item.user .message-wrapper {
  align-items: flex-end;
}

.message-avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 8px;
}

.message-item.user .message-avatar {
  align-items: flex-end;
}

.message-item.assistant .message-avatar {
  align-items: flex-start;
}

.avatar-label {
  font-size: 12px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 4px;
}

.ai-avatar {
  background: linear-gradient(135deg, #409eff, #67c23a);
}

.message-content {
  background: #fff;
  border-radius: 12px;
  padding: 12px 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.message-item.user .message-content {
  background: #409eff;
  color: #fff;
}

.message-time {
  font-size: 11px;
  color: #909399;
  margin-top: 8px;
}

.message-item.user .message-time {
  color: rgba(255, 255, 255, 0.7);
  text-align: right;
}

.message-text {
  line-height: 1.6;
  word-break: break-word;
}

.message-text :deep(pre) {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 8px 0;
}

.message-item.user .message-text :deep(pre) {
  background: rgba(255, 255, 255, 0.1);
}

.message-text :deep(code) {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
}

.message-item.user .message-text :deep(code) {
  background: rgba(255, 255, 255, 0.2);
}

.message-files {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.file-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
}

.message-item.user .file-tag {
  background: rgba(255, 255, 255, 0.2);
}

/* 加载动画 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 8px 0;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #409eff;
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  30% {
    transform: translateY(-8px);
    opacity: 1;
  }
}

/* 思考中动画 */
.thinking-animation {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 0;
}

.thinking-dots {
  display: flex;
  gap: 6px;
}

.thinking-dots .dot {
  width: 10px;
  height: 10px;
  background: #409eff;
  border-radius: 50%;
  animation: thinking-pulse 1.4s infinite ease-in-out;
}

.thinking-dots .dot:nth-child(1) {
  animation-delay: 0s;
}

.thinking-dots .dot:nth-child(2) {
  animation-delay: 0.2s;
}

.thinking-dots .dot:nth-child(3) {
  animation-delay: 0.4s;
}

.thinking-text {
  color: #606266;
  font-size: 14px;
  animation: thinking-blink 1.5s infinite;
}

@keyframes thinking-pulse {
  0%, 60%, 100% {
    transform: scale(0.8);
    opacity: 0.4;
  }
  30% {
    transform: scale(1.2);
    opacity: 1;
  }
}

@keyframes thinking-blink {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

/* 输入区域 */
.input-area {
  padding: 16px 20px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
}

.uploaded-files {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.file-preview {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 13px;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  max-width: 900px;
  margin: 0 auto;
}

.input-wrapper :deep(.el-textarea__inner) {
  border-radius: 12px;
  padding: 12px 16px;
  resize: none;
}

.upload-btn, .send-btn {
  flex-shrink: 0;
}

.input-tips {
  text-align: center;
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

/* 响应式 */
@media (max-width: 768px) {
  .chat-sidebar {
    display: none;
  }
  
  .message-wrapper {
    max-width: 85%;
  }
  
  .quick-prompts {
    flex-direction: column;
  }
}
</style>
