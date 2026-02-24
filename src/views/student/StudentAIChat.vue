<template>
  <div class="ai-chat-container">
    <div class="chat-main">
      <div class="chat-header">
        <div class="header-title">
          <el-icon class="title-icon"><ChatDotRound /></el-icon>
          <h2>AI 学习助手</h2>
        </div>
        </div>

      <div class="messages-container" ref="messagesContainer">
        <div v-if="messages.length === 0" class="welcome-screen">
          <div class="welcome-avatar">
            <div class="avatar-inner">
              <el-icon :size="48"><ChatDotRound /></el-icon>
            </div>
          </div>
          <h1 class="welcome-title">有什么我可以帮你的？</h1>
          <p class="welcome-subtitle">
            可以问我学习中遇到的问题，支持上传文档与图片进行分析
          </p>
          <div class="quick-prompts">
            <div
              class="prompt-card"
              @click="sendQuickPrompt('帮我解释一下这道题的解题思路')"
            >
              <el-icon class="prompt-icon"><QuestionFilled /></el-icon>
              <span>解题辅导</span>
            </div>
            <div
              class="prompt-card"
              @click="sendQuickPrompt('帮我制定一份本周的学习计划')"
            >
              <el-icon class="prompt-icon"><Document /></el-icon>
              <span>学习计划</span>
            </div>
            <div
              class="prompt-card"
              @click="sendQuickPrompt('帮我总结一下这个知识点')"
            >
              <el-icon class="prompt-icon"><DataAnalysis /></el-icon>
              <span>知识总结</span>
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
                <div v-if="msg.role === 'assistant'" class="ai-avatar">
                  <el-icon><ChatDotRound /></el-icon>
                </div>
                <el-avatar
                  v-if="msg.role === 'user'"
                  :size="36"
                  :icon="User"
                  class="user-avatar"
                />
              </div>
              <div class="message-content">
                <div class="message-header">
                  <span class="message-sender">{{
                    msg.role === "user" ? "我" : "AI 助手"
                  }}</span>
                  <span class="message-time">{{
                    formatTime(msg.timestamp)
                  }}</span>
                </div>

                <div class="message-body">
                  <el-button
                    v-if="msg.role === 'assistant' && msg.content"
                    class="copy-btn"
                    circle
                    size="small"
                    :icon="CopyDocument"
                    @click="copyMessage(msg.content)"
                    title="复制内容"
                  />

                  <div
                    v-if="msg.files && msg.files.length"
                    class="message-attachments"
                  >
                    <div
                      v-for="(file, fIndex) in msg.files"
                      :key="fIndex"
                      class="attachment-item"
                    >
                      <div v-if="file.isImage" class="attachment-image-wrap">
                        <img
                          :src="file.url"
                          :alt="file.name"
                          class="attachment-image"
                        />
                      </div>
                      <div v-else class="attachment-doc-card">
                        <div class="doc-icon-wrap">
                          <el-icon><Document /></el-icon>
                        </div>
                        <div class="doc-info">
                          <span class="doc-name" :title="file.name">{{
                            file.name
                          }}</span>
                          <span class="doc-size">{{
                            formatFileSize(file.size)
                          }}</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div
                    v-if="msg.content"
                    class="message-text"
                    v-html="renderMarkdown(msg.content)"
                  ></div>

                  <div v-else-if="msg.role === 'assistant'" class="typing-indicator">
                    <span></span><span></span><span></span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="input-area-container">
        <div class="input-container-inner" :class="{ 'drag-over': isDragOver }">
          <div v-if="uploadedFiles.length" class="staging-area">
            <div
              v-for="(file, index) in uploadedFiles"
              :key="index"
              class="staging-file"
            >
              <template v-if="file.isImage">
                <img :src="file.url" class="staging-image" />
              </template>
              <template v-else>
                <div class="staging-doc-pill">
                  <div class="staging-doc-icon">
                    <el-icon><Document /></el-icon>
                  </div>
                  <div class="staging-doc-info">
                    <span class="staging-doc-name" :title="file.name">{{ file.name }}</span>
                    <span class="staging-doc-size">{{ formatFileSize(file.size) }}</span>
                  </div>
                </div>
              </template>
              <button class="remove-file-btn" @click="removeFile(index)">
                <el-icon><Close /></el-icon>
              </button>
            </div>
          </div>

          <div
            class="input-wrapper"
            @dragover.prevent="handleDragOver"
            @dragleave.prevent="handleDragLeave"
            @drop.prevent="handleDrop"
          >
            <el-button
              v-if="messages.length > 0"
              :icon="Delete"
              circle
              class="toolbar-btn danger-hover"
              @click="clearChatHistory"
              title="清空对话"
            />

            <el-button
              :icon="Plus"
              circle
              class="toolbar-btn"
              @click="triggerFileUpload"
              title="上传文件或图片"
            />
            <input
              type="file"
              ref="fileInput"
              multiple
              @change="handleFileUpload"
              style="display: none"
              accept=".txt,.pdf,.doc,.docx,.xls,.xlsx,.png,.jpg,.jpeg,.gif,.webp"
            />

            <el-input
              v-model="inputMessage"
              type="textarea"
              :autosize="{ minRows: 1, maxRows: 8 }"
              placeholder="输入消息或提问，支持拖拽文件到此处..."
              @keydown="handleKeydown"
              :disabled="isLoading"
              class="message-input"
            />

            <el-button
              type="primary"
              :icon="Promotion"
              circle
              class="send-btn"
              :disabled="!canSend"
              @click="sendMessage"
              :class="{ 'can-send': canSend }"
            />
          </div>
        </div>
        <div class="input-tips">
          内容由 AI 生成，请注意甄别。支持上传 PDF, Word, Excel, 图片等文件。
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted } from "vue";
import {
  Plus, Delete, Promotion, Close, ChatDotRound,
  User, Document, DataAnalysis, QuestionFilled, CopyDocument,
} from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import MarkdownIt from "markdown-it";
import hljs from "highlight.js";
import "highlight.js/styles/github.css";
import {
  getUserConversations, createConversation,
  getConversationMessages, deleteChat, sendChatStream,
} from "@/services/chatApi";

// === 配置 ===
const getUsername = () => {
  try {
    const userName = localStorage.getItem("userName");
    if (userName) return userName;
    const u = JSON.parse(localStorage.getItem("userInfo") || "null");
    return u?.username || u?.name || "student";
  } catch { return "student"; }
};

// === 状态 ===
const messages = ref([]);
const inputMessage = ref("");
const isLoading = ref(false);
const uploadedFiles = ref([]);
const messagesContainer = ref(null);
const fileInput = ref(null);
const isDragOver = ref(false);
const conversationId = ref("");
const username = ref(getUsername());

const canSend = computed(() => (inputMessage.value.trim() || uploadedFiles.value.length) && !isLoading.value);

// === 工具函数 ===
const formatTime = (ts) => {
  if (!ts) return "";
  return new Date(ts).toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" });
};
const formatFileSize = (b) => {
  if (b < 1024) return b + " B";
  if (b < 1048576) return (b / 1024).toFixed(1) + " KB";
  return (b / 1048576).toFixed(1) + " MB";
};

// Markdown 渲染
const md = new MarkdownIt({
  html: true, linkify: true, typographer: true, breaks: true,
  highlight(str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try { return '<pre class="hljs"><code>' + hljs.highlight(str, { language: lang, ignoreIllegals: true }).value + "</code></pre>"; } catch {}
    }
    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + "</code></pre>";
  },
});
const renderMarkdown = (text) => text ? md.render(text) : "";

const scrollToBottom = () => nextTick(() => { if (messagesContainer.value) messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight; });

// === 初始化 ===
onMounted(async () => { await ensureConversation(); await loadHistory(); });

const ensureConversation = async () => {
  try {
    const res = await getUserConversations(username.value);
    if (res?.code === 200 && res.data?.length > 0) { conversationId.value = res.data[0].conversationId; return; }
  } catch {}
  try {
    const res = await createConversation("AI 学习助手");
    if (res?.code === 200) conversationId.value = res.data.conversationId;
  } catch (e) { console.error("创建会话失败", e); }
};

const loadHistory = async () => {
  try {
    if (!conversationId.value) {
      await ensureConversation();
    }
    const res = await getConversationMessages(conversationId.value);
    if (res?.code === 200 && res.data?.messages) {
      messages.value = res.data.messages.map((m) => {
        const msg = {
          role: m.role,
          content: m.content,
          timestamp: m.createdAt
        };

        // 解析文件信息
        if (m.files) {
          try {
            msg.files = JSON.parse(m.files);
          } catch (e) {
            console.error("解析文件信息失败", e);
            msg.files = [];
          }
        }

        return msg;
      });
      scrollToBottom();
    }
  } catch (e) { console.error("加载历史失败", e); }
};

// === 文件处理 ===
const processFiles = (list) => {
  list.forEach((file) => {
    if (file.size > 20 * 1024 * 1024) { ElMessage.warning(`文件 ${file.name} 超过 20MB`); return; }
    const isImage = file.type.startsWith("image/");
    uploadedFiles.value.push({ file, url: isImage ? URL.createObjectURL(file) : null, name: file.name, size: file.size, isImage });
  });
};
const triggerFileUpload = () => fileInput.value?.click();
const handleFileUpload = (e) => { processFiles(Array.from(e.target.files || [])); e.target.value = ""; };
const handleDragOver = () => (isDragOver.value = true);
const handleDragLeave = () => (isDragOver.value = false);
const handleDrop = (e) => { isDragOver.value = false; processFiles(Array.from(e.dataTransfer.files || [])); };
const removeFile = (i) => { const f = uploadedFiles.value[i]; if (f.url) URL.revokeObjectURL(f.url); uploadedFiles.value.splice(i, 1); };

// === 会话管理 ===
const clearChatHistory = async () => {
  try {
    await ElMessageBox.confirm("确定要清空所有聊天记录吗？此操作将无法恢复。", "清空对话",
      { confirmButtonText: "确定清空", cancelButtonText: "取消", type: "warning", confirmButtonClass: "el-button--danger" });
    isLoading.value = true;
    await deleteChat();
    messages.value = [];
    ElMessage.success("聊天记录已清空");
  } catch (err) { if (err !== "cancel") ElMessage.error("清空失败"); }
  finally { isLoading.value = false; }
};

const copyMessage = (c) => navigator.clipboard.writeText(c).then(() => ElMessage.success("已复制到剪贴板"));
const sendQuickPrompt = (p) => { inputMessage.value = p; sendMessage(); };
const handleKeydown = (e) => { if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); sendMessage(); } };

// === 发送消息 ===
const sendMessage = async () => {
  if (!canSend.value) return;
  const content = inputMessage.value.trim();
  const files = [...uploadedFiles.value];

  console.log('[StudentAIChat] 准备发送消息:', { content, files: files.length, conversationId: conversationId.value })

  if (!conversationId.value) {
    console.log('[StudentAIChat] 会话不存在，创建新会话...')
    await ensureConversation();
    if (!conversationId.value) {
      console.error('[StudentAIChat] 无法创建会话')
      ElMessage.error("无法创建会话");
      return;
    }
  }

  messages.value.push({ role: "user", content, files, timestamp: Date.now() });
  inputMessage.value = "";
  uploadedFiles.value = [];
  scrollToBottom();
  isLoading.value = true;

  messages.value.push({ role: "assistant", content: "", timestamp: Date.now() });
  const aiMsgIndex = messages.value.length - 1;

  sendChatStream(
    conversationId.value,
    content,
    files,
    (text) => {
      messages.value[aiMsgIndex].content += text;
      scrollToBottom();
    },
    (errMsg) => {
      messages.value[aiMsgIndex].content += `\n\n**错误**: ${errMsg}`;
      isLoading.value = false;
      scrollToBottom();
    },
    () => {
      if (!messages.value[aiMsgIndex].content) {
        messages.value[aiMsgIndex].content = "抱歉，AI 暂时无法回复，请稍后重试。";
      }
      isLoading.value = false;
      scrollToBottom();
    }
  );
};
</script>

<style scoped>
/* 全局容器背景 */
.ai-chat-container {
  display: flex;
  height: 100vh;
  background: #ffffff;
  box-sizing: border-box;
}

.chat-main {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  background: #ffffff;
  overflow: hidden;
}

/* ==================== 顶部导航条 ==================== */
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #f3f4f6;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  z-index: 10;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-title h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.title-icon {
  font-size: 22px;
  color: #4f9cf7;
}

/* ==================== 聊天记录区 ==================== */
.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px 10%;
  scroll-behavior: smooth;
}

.messages-container::-webkit-scrollbar { width: 6px; }
.messages-container::-webkit-scrollbar-thumb { background: #e5e7eb; border-radius: 4px; }

/* 欢迎页 */
.welcome-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  animation: fadeIn 0.5s ease-out;
}

.avatar-inner {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #4f9cf7 0%, #6366f1 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-bottom: 20px;
  box-shadow: 0 8px 20px rgba(79, 156, 247, 0.3);
}

.welcome-title { font-size: 28px; font-weight: 600; color: #111827; margin-bottom: 8px; }
.welcome-subtitle { color: #6b7280; margin-bottom: 30px; }

.quick-prompts { display: flex; gap: 16px; justify-content: center; }

.prompt-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  cursor: pointer;
  color: #374151;
  font-weight: 500;
  transition: all 0.2s;
}

.prompt-card:hover {
  background: #f3f4f6;
  border-color: #4f9cf7;
  color: #4f9cf7;
  transform: translateY(-2px);
}

.prompt-icon { font-size: 18px; }

/* 消息列表 */
.messages-list { display: flex; flex-direction: column; gap: 24px; }
.message-item { display: flex; animation: slideUp 0.3s ease-out; }
.message-wrapper { display: flex; gap: 16px; max-width: 85%; }
.message-item.user { justify-content: flex-end; }
.message-item.user .message-wrapper { flex-direction: row-reverse; }
.message-avatar { flex-shrink: 0; }

.ai-avatar {
  width: 36px; height: 36px;
  background: linear-gradient(135deg, #4f9cf7 0%, #6366f1 100%);
  color: white; border-radius: 10px;
  display: flex; align-items: center; justify-content: center; font-size: 20px;
}

.user-avatar { background: #e5e7eb; color: #4b5563; }

.message-content { display: flex; flex-direction: column; align-items: flex-start; gap: 6px; }
.message-item.user .message-content { align-items: flex-end; }
.message-header { display: flex; align-items: center; gap: 8px; font-size: 13px; margin-left: 4px; }
.message-sender { font-weight: 600; color: #374151; }
.message-time { color: #9ca3af; }

/* ========== 消息框气泡主体 ========== */
.message-body {
  position: relative; /* 为内部复制按钮绝对定位做准备 */
  background: #f3f4f6; color: #1f2937;
  padding: 14px 18px;
  /* 如果是 AI，右侧多留出一点 padding 以防复制按钮遮挡文字 */
  padding-right: 44px;
  border-radius: 18px; border-top-left-radius: 4px;
  font-size: 15px; line-height: 1.6;
  min-width: 60px;
}

.message-item.user .message-body {
  background: #e8f4fd; color: #111827;
  border-radius: 18px; border-top-right-radius: 4px;
  padding-right: 18px; /* 用户消息不需要给复制按钮留空间 */
}

/* ========== 复制按钮操作区 ========== */
.copy-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  opacity: 0; /* 默认隐藏 */
  transition: opacity 0.2s ease-in-out;
  border: none;
  background: rgba(255, 255, 255, 0.7);
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  color: #6b7280;
  z-index: 2;
}
.message-body:hover .copy-btn {
  opacity: 1; /* 鼠标悬浮气泡时显示 */
}
.copy-btn:hover {
  background: #ffffff;
  color: #4f9cf7;
}

/* 附件 */
.message-attachments { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 10px; }
.attachment-image { max-width: 250px; max-height: 250px; border-radius: 12px; object-fit: cover; border: 1px solid rgba(0,0,0,0.1); }
.attachment-doc-card {
  display: flex; align-items: center; gap: 12px;
  background: #ffffff; border: 1px solid rgba(0,0,0,0.1);
  padding: 10px 14px; border-radius: 12px; min-width: 200px;
}
.message-item.user .attachment-doc-card { background: rgba(255,255,255,0.7); border-color: rgba(0,0,0,0.05); }
.doc-icon-wrap {
  width: 36px; height: 36px; background: #ef4444; color: white;
  border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 18px;
}
.doc-info { display: flex; flex-direction: column; }
.doc-name { font-size: 14px; font-weight: 600; max-width: 150px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.doc-size { font-size: 12px; color: #6b7280; }

/* Markdown */
.message-text { word-break: break-word; }
.message-text :deep(p) { margin: 0 0 10px 0; padding: 0; }
.message-text :deep(p:last-child) { margin-bottom: 0; }
.message-text :deep(pre.hljs) {
  background: #ffffff; color: #1f2937; padding: 12px; border-radius: 8px;
  border: 1px solid #e5e7eb; overflow-x: auto; margin: 10px 0; font-family: monospace;
}
.message-text :deep(code) { background: rgba(0,0,0,0.05); padding: 2px 6px; border-radius: 4px; color: #ef4444; }
.message-text :deep(pre.hljs code) { background: transparent; color: inherit; padding: 0; }
/* 表格、引用样式增强 */
.message-text :deep(table) { width: 100%; border-collapse: collapse; margin: 12px 0; }
.message-text :deep(th), .message-text :deep(td) { border: 1px solid #d1d5db; padding: 8px 12px; }
.message-text :deep(th) { background-color: #e5e7eb; font-weight: 600; }
.message-text :deep(blockquote) { margin: 10px 0; padding: 8px 16px; color: #4b5563; background-color: #f8fafc; border-left: 4px solid #4f9cf7; border-radius: 0 8px 8px 0; }

/* ==================== 输入区域 ==================== */
.input-area-container { padding: 20px 10%; background: #ffffff; border-top: 1px solid #f3f4f6; }

.input-container-inner {
  background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 20px;
  transition: all 0.3s; position: relative;
}
.input-container-inner:focus-within { border-color: #4f9cf7; box-shadow: 0 0 0 3px rgba(79,156,247,0.1); background: #ffffff; }
.input-container-inner.drag-over { border-color: #4f9cf7; background: #e8f4fd; }

/* 上传暂存区 */
.staging-area { display: flex; flex-wrap: wrap; gap: 12px; padding: 16px 16px 0 16px; }
.staging-file {
  position: relative; height: 70px; border-radius: 12px;
  background: #ffffff; border: 1px solid #e5e7eb;
  display: flex; align-items: center; justify-content: center; overflow: hidden;
}
.staging-image { height: 100%; width: 100%; object-fit: cover; border-radius: 12px; }
.staging-doc-pill { display: flex; align-items: center; gap: 10px; padding: 0 12px; height: 100%; }
.staging-doc-icon {
  width: 32px; height: 32px; border-radius: 8px;
  background: #f3f4f6; display: flex; align-items: center; justify-content: center;
  color: #4f9cf7; font-size: 20px;
}
.staging-doc-info { display: flex; flex-direction: column; overflow: hidden; }
.staging-doc-name { font-size: 13px; font-weight: 600; color: #111827; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.staging-doc-size { font-size: 11px; color: #6b7280; margin-top: 2px; }

.remove-file-btn {
  position: absolute; top: -6px; right: -6px; width: 20px; height: 20px;
  border-radius: 50%; background: #374151; color: white; border: none;
  display: flex; align-items: center; justify-content: center; font-size: 12px;
  cursor: pointer; opacity: 0; transition: opacity 0.2s;
}
.staging-file:hover .remove-file-btn { opacity: 1; }

/* 输入框 */
.input-wrapper { display: flex; align-items: flex-end; padding: 12px 16px; gap: 10px; }
.toolbar-btn { background: #f3f4f6; border: none; color: #4b5563; margin-bottom: 2px; transition: all 0.2s; }
.toolbar-btn:hover { background: #e5e7eb; color: #111827; }
.toolbar-btn.danger-hover:hover { background: #fee2e2; color: #ef4444; } /* 删除按钮专属 hover 颜色 */

.message-input { flex: 1; }
.message-input :deep(.el-textarea__inner) {
  background: transparent; border: none; box-shadow: none;
  padding: 8px 0; font-size: 15px; line-height: 1.5; color: #111827; resize: none;
}

.send-btn { background: #e5e7eb; border: none; color: #9ca3af; margin-bottom: 2px; transition: all 0.3s; }
.send-btn.can-send { background: #111827; color: #ffffff; }
.send-btn.can-send:hover { transform: scale(1.05); }

.input-tips { text-align: center; font-size: 12px; color: #9ca3af; margin-top: 12px; }

/* 打字动画 */
.typing-indicator { display: flex; gap: 4px; padding: 4px 8px; }
.typing-indicator span { width: 6px; height: 6px; background: #6b7280; border-radius: 50%; animation: typing 1.4s infinite ease-in-out; }
.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing { 0%, 60%, 100% { transform: translateY(0); opacity: 0.4; } 30% { transform: translateY(-4px); opacity: 1; } }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes slideUp { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }

@media (max-width: 768px) {
  .messages-container, .input-area-container { padding-left: 16px; padding-right: 16px; }
  .message-wrapper { max-width: 95%; }
  .staging-file { width: 60px; height: 60px; }
  .attachment-image { max-width: 200px; }
  .copy-btn { opacity: 1; } /* 移动端由于没有hover事件，始终显示复制按钮 */
}
</style>