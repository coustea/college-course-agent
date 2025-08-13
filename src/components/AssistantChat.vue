<template>
  <div class="app-container">
    <!-- 左侧历史记录 -->
    <aside class="history-sidebar">
      <div class="sidebar-header">
        <button class="new-chat-btn" @click="startNewChat">+ 新对话</button>
      </div>
      <div class="history-list">
        <div
            v-for="(history, index) in chatHistory"
            :key="index"
            class="history-item"
            :class="{ active: currentChatId === history.id }"
            @click="loadChatHistory(history.id)"
        >
          <span class="history-title">{{ getHistoryTitle(history.messages) }}</span>
          <button class="delete-btn" @click.stop="deleteHistory(history.id)">×</button>
        </div>
      </div>
    </aside>

    <!-- 主聊天区域 -->
    <div class="chat-container">

      <!-- 聊天区域 -->
      <main class="chat-main" ref="chatMain">
        <div v-if="messages.length === 1" class="welcome-container">
          <div class="assistant-icon">
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z" fill="#10A37F"/>
              <path d="M12 6c-3.31 0-6 2.69-6 6s2.69 6 6 6 6-2.69 6-6-2.69-6-6-6zm0 10c-2.21 0-4-1.79-4-4s1.79-4 4-4 4 1.79 4 4-1.79 4-4 4z" fill="#10A37F"/>
              <path d="M12 10c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z" fill="#10A37F"/>
            </svg>
          </div>
          <h2 class="welcome-title">我是您的智能助教</h2>
          <p class="welcome-subtitle">有什么可以帮您解答的吗？</p>
        </div>

        <div
            v-for="(msg, index) in messages"
            :key="index"
            :class="['chat-message', msg.role === 'user' ? 'user' : 'assistant']"
        >
          <div class="message-avatar" v-if="msg.role === 'assistant'">
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z" fill="#10A37F"/>
              <path d="M12 6c-3.31 0-6 2.69-6 6s2.69 6 6 6 6-2.69 6-6-2.69-6-6-6zm0 10c-2.21 0-4-1.79-4-4s1.79-4 4-4 4 1.79 4 4-1.79 4-4 4z" fill="#10A37F"/>
              <path d="M12 10c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z" fill="#10A37F"/>
            </svg>
          </div>
          <div class="message-content">{{ msg.content }}</div>
          <div class="message-avatar user-avatar" v-if="msg.role === 'user'">
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99 4-3.08 6-3.08 1.99 0 5.97 1.09 6 3.08-1.29 1.94-3.5 3.22-6 3.22z" fill="#007AFF"/>
            </svg>
          </div>
        </div>

        <div v-if="loading" class="chat-message assistant">
          <div class="message-avatar">
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z" fill="#10A37F"/>
              <path d="M12 6c-3.31 0-6 2.69-6 6s2.69 6 6 6 6-2.69 6-6-2.69-6-6-6zm0 10c-2.21 0-4-1.79-4-4s1.79-4 4-4 4 1.79 4 4-1.79 4-4 4z" fill="#10A37F"/>
              <path d="M12 10c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z" fill="#10A37F"/>
            </svg>
          </div>
          <div class="message-content">
            <div class="typing-indicator">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        </div>
      </main>

      <!-- 底部输入框 -->
      <footer class="chat-footer">
        <form @submit.prevent="sendMessage" class="input-area">
          <textarea
              v-model="input"
              placeholder="请输入内容..."
              class="chat-input"
              rows="1"
              @keydown.enter.exact.prevent="sendMessage"
              @keydown.shift.enter.stop
              @input="adjustTextareaHeight"
          ></textarea>
          <button class="send-button" :disabled="loading || !input.trim()">
            <svg v-if="!loading" width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path d="M22 2L11 13" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M22 2L15 22L11 13L2 9L22 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span v-else class="loading-dots">
              <span>.</span><span>.</span><span>.</span>
            </span>
          </button>
        </form>
        <p class="footer-note">智能助教可能会犯错，请核实重要信息</p>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'

const input = ref('')
const loading = ref(false)
const messages = ref([
  { role: 'assistant', content: '您好，我是您的专属智能助教，有什么可以帮您？' }
])
const chatMain = ref(null)
const chatHistory = ref([])
const currentChatId = ref(null)

// 初始化时加载历史记录
onMounted(() => {
  loadChatHistoryFromLocalStorage()
  currentChatId.value = generateChatId()
})

// 生成唯一的聊天ID
const generateChatId = () => {
  return Date.now().toString()
}

// 从本地存储加载历史记录
const loadChatHistoryFromLocalStorage = () => {
  const history = localStorage.getItem('chatHistory')
  if (history) {
    chatHistory.value = JSON.parse(history)
  }
}

// 保存历史记录到本地存储
const saveChatHistoryToLocalStorage = () => {
  localStorage.setItem('chatHistory', JSON.stringify(chatHistory.value))
}

// 获取历史记录标题（第一条用户消息或默认）
const getHistoryTitle = (messages) => {
  const userMessage = messages.find(msg => msg.role === 'user')
  return userMessage ? userMessage.content.slice(0, 20) + (userMessage.content.length > 20 ? '...' : '') : '新对话'
}

// 开始新对话
const startNewChat = () => {
  messages.value = [{ role: 'assistant', content: '您好，我是您的专属智能助教，有什么可以帮您？' }]
  currentChatId.value = generateChatId()
}

// 加载历史对话
const loadChatHistory = (id) => {
  const history = chatHistory.value.find(item => item.id === id)
  if (history) {
    messages.value = history.messages
    currentChatId.value = id
  }
}

// 删除历史记录
const deleteHistory = (id) => {
  chatHistory.value = chatHistory.value.filter(item => item.id !== id)
  saveChatHistoryToLocalStorage()
  if (currentChatId.value === id) {
    startNewChat()
  }
}

// 调整输入框高度
const adjustTextareaHeight = (event) => {
  const textarea = event.target
  textarea.style.height = 'auto'
  textarea.style.height = textarea.scrollHeight + 'px'
}

const sendMessage = async () => {
  if (!input.value.trim()) return

  // 添加用户消息
  const userMessage = {
    role: 'user',
    content: input.value.trim()
  }
  messages.value.push(userMessage)

  const question = input.value.trim()
  input.value = ''
  loading.value = true

  await nextTick()
  scrollToBottom()

  try {
    // 模拟 AI 回复，可替换为真实接口
    const reply = await mockAIReply(question)
    messages.value.push({ role: 'assistant', content: reply })

    // 保存到历史记录
    saveCurrentChat()
  } catch (e) {
    messages.value.push({
      role: 'assistant',
      content: '出错了，请稍后再试'
    })
  } finally {
    loading.value = false
    await nextTick()
    scrollToBottom()
  }
}

// 保存当前对话到历史记录
const saveCurrentChat = () => {
  // 过滤掉欢迎消息
  const chatMessages = messages.value.filter(msg =>
      !(msg.role === 'assistant' && msg.content === '您好，我是您的专属智能助教，有什么可以帮您？')
  )

  if (chatMessages.length === 0) return

  // 检查是否已存在当前聊天记录
  const existingIndex = chatHistory.value.findIndex(item => item.id === currentChatId.value)

  if (existingIndex !== -1) {
    // 更新现有记录
    chatHistory.value[existingIndex].messages = chatMessages
  } else {
    // 添加新记录
    chatHistory.value.unshift({
      id: currentChatId.value,
      messages: chatMessages
    })
  }

  // 限制历史记录数量
  if (chatHistory.value.length > 20) {
    chatHistory.value = chatHistory.value.slice(0, 20)
  }

  saveChatHistoryToLocalStorage()
}

const mockAIReply = (question) =>
    new Promise((resolve) => {
      setTimeout(() => {
        resolve(`关于"${question}"，这是一个模拟回复。在实际应用中，这里会连接到AI服务获取真实回答。`)
      }, 1000)
    })

const scrollToBottom = () => {
  const el = chatMain.value
  if (el) el.scrollTop = el.scrollHeight
}
</script>

<style scoped>
.app-container {
  display: flex;
  height: 100vh;
  font-family: "Segoe UI", -apple-system, BlinkMacSystemFont, sans-serif;
  background-color: #ffffff;
}

.history-sidebar {
  width: 260px;
  background-color: #f7f7f8;
  border-right: 1px solid #e5e5e6;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.sidebar-header {
  padding: 12px;
  border-bottom: 1px solid #e5e5e6;
}

.new-chat-btn {
  width: 100%;
  padding: 10px 12px;
  background-color: #ffffff;
  border: 1px solid #e5e5e6;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  text-align: left;
  transition: all 0.2s;
}

.new-chat-btn:hover {
  background-color: #f0f0f0;
}

.history-list {
  flex: 1;
  overflow-y: auto;
}

.history-item {
  padding: 12px;
  margin: 4px 8px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.history-item:hover {
  background-color: #e5e5e6;
}

.history-item.active {
  background-color: #e5e5e6;
  font-weight: 500;
}

.history-title {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.delete-btn {
  background: none;
  border: none;
  color: #6e6e80;
  font-size: 18px;
  cursor: pointer;
  padding: 0 4px;
  margin-left: 8px;
}

.delete-btn:hover {
  color: #000;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #ffffff;
}

.chat-header {
  padding: 16px;
  background-color: #ffffff;
  border-bottom: 1px solid #e5e5e6;
  text-align: center;
  font-size: 16px;
  font-weight: bold;
}

.chat-main {
  flex: 1;
  padding: 24px 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  background-color: #f7f7f8;
}

.welcome-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  max-width: 800px;
  text-align: center;
  padding: 0 20px;
  margin-bottom: 100px;
}

.assistant-icon {
  width: 64px;
  height: 64px;
  margin-bottom: 20px;
  background-color: #ffffff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.welcome-title {
  font-size: 32px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #333;
}

.welcome-subtitle {
  font-size: 16px;
  color: #666;
  max-width: 500px;
  line-height: 1.5;
}

.chat-message {
  display: flex;
  gap: 16px;
  max-width: 800px;
  width: 100%;
  padding: 20px 0;
  margin-bottom: 4px;
}

.chat-message.user {
  background-color: #f7f7f8;
}

.chat-message.assistant {
  background-color: #f7f7f8;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-left: 20px;
}

.user-avatar {
  margin-left: 0;
  margin-right: 20px;
}

.message-content {
  flex: 1;
  padding-right: 20px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-message.user .message-content {
  padding-left: 20px;
  padding-right: 0;
}

.chat-footer {
  padding: 12px 16px;
  background-color: #ffffff;
  border-top: 1px solid #e5e5e6;
}

.input-area {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  max-width: 800px;
  margin: 0 auto;
  position: relative;
}

.chat-input {
  flex: 1;
  resize: none;
  padding: 12px 50px 12px 16px;
  border-radius: 8px;
  border: 1px solid #e5e5e6;
  font-size: 16px;
  outline: none;
  max-height: 200px;
  background-color: #f7f7f8;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

.chat-input:focus {
  border-color: #10A37F;
  box-shadow: 0 0 0 2px rgba(16, 163, 127, 0.2);
}

.send-button {
  position: absolute;
  right: 8px;
  bottom: 8px;
  background-color: #10A37F;
  color: white;
  border: none;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
  transition: background-color 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.send-button:hover:not(:disabled) {
  background-color: #0d8a6d;
}

.send-button:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.footer-note {
  text-align: center;
  font-size: 12px;
  color: #6e6e80;
  margin-top: 8px;
}

.typing-indicator {
  display: flex;
  align-items: center;
  height: 20px;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  margin: 0 2px;
  background-color: #888;
  border-radius: 50%;
  display: inline-block;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(1) {
  animation-delay: 0s;
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
  }
  30% {
    transform: translateY(-5px);
  }
}

.loading-dots span {
  animation: blink 1.4s infinite both;
}

.loading-dots span:nth-child(2) {
  animation-delay: 0.2s;
}

.loading-dots span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes blink {
  0%, 100% {
    opacity: 0.2;
  }
  20% {
    opacity: 1;
  }
}
</style>