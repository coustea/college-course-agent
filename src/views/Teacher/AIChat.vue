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
            <div class="message-avatar">
              <el-avatar v-if="msg.role === 'user'" :size="36" :icon="User" />
              <el-avatar v-else :size="36" class="ai-avatar">
                <el-icon><ChatDotRound /></el-icon>
              </el-avatar>
            </div>
            <div class="message-content">
              <div class="message-header">
                <span class="message-role">{{ msg.role === 'user' ? '我' : 'AI 助手' }}</span>
                <span class="message-time">{{ formatTime(msg.timestamp) }}</span>
              </div>
              <div class="message-text" v-html="renderMarkdown(msg.content)"></div>
              <!-- 附件显示 -->
              <div v-if="msg.files && msg.files.length" class="message-files">
                <div v-for="file in msg.files" :key="file.name" class="file-tag">
                  <el-icon><Document /></el-icon>
                  {{ file.name }}
                </div>
              </div>
            </div>
          </div>
          <!-- 加载中指示器 -->
          <div v-if="isLoading" class="message-item assistant">
            <div class="message-avatar">
              <el-avatar :size="36" class="ai-avatar">
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
import { ref, computed, nextTick, onMounted } from 'vue'
import { 
  Plus, Delete, Fold, Expand, Upload, Promotion, Close,
  ChatDotRound, User, Document, DataAnalysis, QuestionFilled 
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

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
    messages.value.push({
      role: 'assistant',
      content: '抱歉，发生了错误，请稍后重试。',
      timestamp: Date.now()
    })
  } finally {
    isLoading.value = false
    scrollToBottom()
    saveChatHistory()
  }
}

// 流式响应模拟（实际项目中替换为真实 API）
const streamResponse = async (content, files) => {
  // 创建 AI 消息占位
  const aiMessage = {
    role: 'assistant',
    content: '',
    timestamp: Date.now()
  }
  messages.value.push(aiMessage)
  
  // 模拟流式输出（实际项目中使用 SSE 或 WebSocket）
  const mockResponse = generateMockResponse(content, files)
  
  for (let i = 0; i < mockResponse.length; i++) {
    await new Promise(resolve => setTimeout(resolve, 20))
    aiMessage.content += mockResponse[i]
    scrollToBottom()
  }
}

// 模拟 AI 响应（实际项目中替换为真实 API 调用）
const generateMockResponse = (content, files) => {
  const hasFiles = files && files.length > 0
  
  if (hasFiles) {
    return `我已收到您上传的 ${files.length} 个文件。

**文件列表：**
${files.map(f => `- ${f.name}`).join('\n')}

我会分析这些文件内容，请问您需要我做什么？

> 提示：在实际部署时，请配置后端 AI 服务（如 OpenAI、Gemini 等）来处理文件分析。`
  }
  
  if (content.includes('学习情况') || content.includes('分析')) {
    return `## 学生学习情况分析

根据当前数据，我为您整理了以下分析报告：

### 整体概况
- **课程完成率**：平均 68%
- **活跃学生比例**：85%
- **作业提交率**：92%

### 需要关注的问题
1. 部分学生视频观看进度较慢
2. 第三章测验正确率偏低（平均 65%）
3. 有 5 名学生超过一周未登录

### 建议措施
- 对进度落后的学生进行一对一辅导
- 针对第三章内容安排答疑课
- 通过系统发送学习提醒

> 如需更详细的分析，请上传具体的学生数据文件。`
  }
  
  if (content.includes('教案')) {
    return `## 课程教案模板

### 一、教学目标
1. 知识目标：掌握本章核心概念
2. 能力目标：能够独立完成相关练习
3. 情感目标：培养学习兴趣

### 二、教学重难点
- **重点**：核心知识点讲解
- **难点**：实际应用场景

### 三、教学过程
1. **导入**（5分钟）：回顾上节内容
2. **新课讲授**（30分钟）：讲解新知识
3. **练习巩固**（10分钟）：课堂练习
4. **总结**（5分钟）：归纳要点

### 四、作业布置
- 完成课后习题 1-5
- 预习下一章内容

> 请告诉我具体的课程主题，我可以生成更详细的教案。`
  }
  
  return `您好！我是 AI 教学助手，很高兴为您服务。

您的问题是：**${content}**

我可以帮助您：
- 📊 分析学生学习数据
- 📝 生成课程教案和教学材料
- 💡 提供教学方法建议
- 📁 分析上传的文档内容

请告诉我您具体需要什么帮助？

> 提示：您可以上传文件让我进行分析，支持 PDF、Word、Excel、图片等格式。`
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
  gap: 20px;
  max-width: 900px;
  margin: 0 auto;
}

.message-item {
  display: flex;
  gap: 12px;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.ai-avatar {
  background: linear-gradient(135deg, #409eff, #67c23a);
}

.message-content {
  max-width: 70%;
  background: #fff;
  border-radius: 12px;
  padding: 12px 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.message-item.user .message-content {
  background: #409eff;
  color: #fff;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 12px;
}

.message-role {
  font-weight: 500;
}

.message-time {
  color: #909399;
}

.message-item.user .message-time {
  color: rgba(255, 255, 255, 0.7);
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
  
  .message-content {
    max-width: 85%;
  }
  
  .quick-prompts {
    flex-direction: column;
  }
}
</style>
