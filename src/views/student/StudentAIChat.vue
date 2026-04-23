<template>
  <div class="ai-chat-container">
    <div class="chat-main">
      <div class="chat-header">
        <div class="header-title">
          <el-icon class="title-icon"><ChatDotRound /></el-icon>
          <h2>AI 学习助手</h2>
        </div>
        <el-button
          v-if="messages.length > 0"
          type="danger"
          plain
          size="small"
          class="clear-btn"
          :icon="Delete"
          @click="clearChatHistory"
        >
          清空对话
        </el-button>
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
            <div class="prompt-card" @click="sendQuickPrompt('帮我解释一下这道题的解题思路')">
              <el-icon class="prompt-icon"><QuestionFilled /></el-icon>
              <span>解题辅导</span>
            </div>
            <div class="prompt-card" @click="sendQuickPrompt('帮我制定一份本周的学习计划')">
              <el-icon class="prompt-icon"><Document /></el-icon>
              <span>学习计划</span>
            </div>
            <div class="prompt-card" @click="sendQuickPrompt('帮我总结一下这个知识点')">
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
                  <span class="message-sender">{{ msg.role === "user" ? "我" : "AI 助手" }}</span>
                  <span class="message-time">{{ formatTime(msg.timestamp) }}</span>
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

                  <div v-if="msg.files && msg.files.length" class="message-attachments">
                    <div v-for="(file, fIndex) in msg.files" :key="fIndex" class="attachment-item">
                      <div v-if="file.isImage" class="attachment-image-wrap">
                        <img :src="file.url" :alt="file.name" class="attachment-image" />
                      </div>
                      <div v-else class="attachment-doc-card">
                        <div class="doc-icon-wrap">
                          <el-icon><Document /></el-icon>
                        </div>
                        <div class="doc-info">
                          <span class="doc-name" :title="file.name">{{ file.name }}</span>
                          <span class="doc-size">{{ formatFileSize(file.size) }}</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div v-if="msg.content" class="message-text" v-html="renderMarkdown(msg.content)"></div>

                  <div v-if="msg.generatedFiles && msg.generatedFiles.length" class="doc-preview-cards">
                    <div v-for="doc in msg.generatedFiles" :key="doc.url" class="doc-card" :style="{ borderColor: getFileTypeColor(doc.type), background: getFileTypeBgColor(doc.type) }">
                      <div class="doc-card-icon" :style="{ background: getFileTypeColor(doc.type) }">
                        <el-icon :size="24" color="#fff"><Document /></el-icon>
                      </div>
                      <div class="doc-card-info">
                        <span class="doc-card-name">{{ doc.name }}</span>
                        <span class="doc-card-type" :style="{ color: getFileTypeColor(doc.type) }">{{ getFileTypeLabel(doc.type) }} 文件</span>
                        <span v-if="doc.description" class="doc-card-desc">{{ doc.description }}</span>
                      </div>
                      <div class="doc-card-actions">
                        <el-button size="small" type="primary" @click="previewFile(doc.url, doc.type)">预览</el-button>
                        <el-button size="small" @click="downloadFile(doc.url, doc.name)">下载</el-button>
                      </div>
                    </div>
                  </div>

                  <div v-else-if="msg.content && parseDocumentLinks(msg.content).length" class="doc-preview-cards">
                    <div v-for="doc in parseDocumentLinks(msg.content)" :key="doc.url" class="doc-card" :style="{ borderColor: getFileTypeColor(doc.type), background: getFileTypeBgColor(doc.type) }">
                      <div class="doc-card-icon" :style="{ background: getFileTypeColor(doc.type) }">
                        <el-icon :size="24" color="#fff"><Document /></el-icon>
                      </div>
                      <div class="doc-card-info">
                        <span class="doc-card-name">{{ doc.filename }}</span>
                        <span class="doc-card-type" :style="{ color: getFileTypeColor(doc.type) }">{{ getFileTypeLabel(doc.type) }} 文件</span>
                      </div>
                      <div class="doc-card-actions">
                        <el-button size="small" type="primary" @click="previewFile(doc.url, doc.type)">预览</el-button>
                        <el-button size="small" @click="downloadFile(doc.url, doc.filename)">下载</el-button>
                      </div>
                    </div>
                  </div>

                  <div v-if="msg.content && parseEntityLinks(msg.content).length" class="entity-preview-cards">
                    <div v-for="entity in parseEntityLinks(msg.content)" :key="entity.id" class="entity-card">
                      <div class="entity-card-icon">
                        <el-icon :size="32"><Reading /></el-icon>
                      </div>
                      <div class="entity-card-info">
                        <span class="entity-card-name">{{ entity.name }}</span>
                        <span class="entity-card-desc">{{ entity.description }}</span>
                        <div class="entity-card-meta">
                          <el-tag size="small" type="info">教师: {{ entity.teacher }}</el-tag>
                          <el-tag size="small" type="success">学分: {{ entity.credits }}</el-tag>
                        </div>
                      </div>
                      <div class="entity-card-actions">
                        <el-button size="small" type="primary" @click="window.open(`/course/${entity.id}`, '_blank')">查看课程</el-button>
                      </div>
                    </div>
                  </div>

                  <div v-if="!msg.content && msg.role === 'assistant'" class="typing-indicator">
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
            <div v-for="(file, index) in uploadedFiles" :key="index" class="staging-file">
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
            <el-button :icon="Plus" circle class="toolbar-btn" @click="triggerFileUpload" title="上传文件或图片" />
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
        <div class="input-tips">内容由 AI 生成，请注意甄别。支持上传 PDF, Word, Excel, 图片等文件。</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Plus, Delete, Promotion, Close, ChatDotRound, User, Document, DataAnalysis, QuestionFilled, CopyDocument, Reading } from "@element-plus/icons-vue";
import { useChat } from "@/composables/useChat";

// Use the shared chat composable with student-specific configuration
const {
  messages,
  inputMessage,
  isLoading,
  uploadedFiles,
  messagesContainer,
  fileInput,
  isDragOver,
  canSend,
  formatTime,
  formatFileSize,
  renderMarkdown,
  parseDocumentLinks,
  parseEntityLinks,
  getFileTypeColor,
  getFileTypeBgColor,
  getFileTypeLabel,
  downloadFile,
  previewFile,
  clearChatHistory,
  copyMessage,
  sendQuickPrompt,
  handleDragOver,
  handleDragLeave,
  handleDrop,
  removeFile,
  triggerFileUpload,
  handleFileUpload,
  handleKeydown,
  sendMessage,
} = useChat({
  conversationTitle: "AI 学习助手",
  defaultUsername: "student",
});
</script>

<style scoped>
/* 基础容器 */
.ai-chat-container { 
  display: flex; 
  height: calc(100vh - 48px); /* 适应外层的 24px padding */
  background: transparent; 
  box-sizing: border-box;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  color: #1f2937;
}

.chat-main { 
  display: flex; 
  flex-direction: column; 
  width: 100%; 
  height: 100%; 
  background: #ffffff; 
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05), 0 10px 25px -5px rgba(0,0,0,0.05);
  overflow: hidden; 
}

/* 顶部导航 */
.chat-header { 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  padding: 20px 24px; 
  border-bottom: 1px solid #f1f5f9; 
  background: #ffffff; 
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
  font-weight: 700; 
  color: #111827; 
}

.title-icon { 
  font-size: 22px; 
  color: #3b82f6; 
}

.clear-btn { 
  border-radius: 8px; 
  font-weight: 500; 
  transition: all 0.2s;
}

/* 消息流区域 */
.messages-container { 
  flex: 1; 
  overflow-y: auto; 
  padding: 32px 10%; 
  scroll-behavior: smooth; 
  background: #f8fafc;
}

.messages-container::-webkit-scrollbar { width: 6px; }
.messages-container::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
.messages-container::-webkit-scrollbar-thumb:hover { background: #94a3b8; }

/* 欢迎引导屏 */
.welcome-screen { 
  display: flex; 
  flex-direction: column; 
  align-items: center; 
  justify-content: center; 
  height: 100%; 
  text-align: center; 
  animation: fadeIn 0.6s cubic-bezier(0.16, 1, 0.3, 1); 
}

.avatar-inner { 
  width: 80px; 
  height: 80px; 
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%); 
  border-radius: 20px; 
  display: flex; 
  align-items: center; 
  justify-content: center; 
  color: white; 
  margin-bottom: 24px; 
  box-shadow: 0 10px 25px -5px rgba(59, 130, 246, 0.4); 
  transform: rotate(-5deg);
}

.welcome-title { 
  font-size: 28px; 
  font-weight: 700; 
  color: #111827; 
  margin-bottom: 12px; 
  letter-spacing: -0.02em;
}

.welcome-subtitle { 
  color: #64748b; 
  margin-bottom: 32px; 
  font-size: 15px;
  max-width: 480px;
  line-height: 1.6;
}

.quick-prompts { 
  display: flex; 
  gap: 16px; 
  justify-content: center; 
  flex-wrap: wrap;
}

.prompt-card { 
  display: flex; 
  align-items: center; 
  gap: 10px; 
  padding: 14px 24px; 
  background: #ffffff; 
  border: 1px solid #e2e8f0; 
  border-radius: 12px; 
  cursor: pointer; 
  color: #475569; 
  font-weight: 500; 
  font-size: 14px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); 
  box-shadow: 0 2px 4px rgba(0,0,0,0.02);
}

.prompt-card:hover { 
  border-color: #bfdbfe; 
  color: #2563eb; 
  transform: translateY(-2px); 
  box-shadow: 0 10px 15px -3px rgba(59, 130, 246, 0.1);
}

.prompt-icon { font-size: 18px; color: #3b82f6; }

/* 聊天气泡列表 */
.messages-list { 
  display: flex; 
  flex-direction: column; 
  gap: 32px; 
  padding-bottom: 24px;
}

.message-item { 
  display: flex; 
  animation: slideUp 0.4s cubic-bezier(0.16, 1, 0.3, 1); 
}

.message-wrapper {
  display: flex;
  gap: 16px;
  max-width: 85%;
}

@media (min-width: 1024px) {
  .message-wrapper { max-width: 75%; }
}

.message-item.user { justify-content: flex-end; }
.message-item.user .message-wrapper { flex-direction: row-reverse; }

.message-avatar { flex-shrink: 0; margin-top: 4px; }
.ai-avatar { 
  width: 38px; 
  height: 38px; 
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%); 
  color: white; 
  border-radius: 10px; 
  display: flex; 
  align-items: center; 
  justify-content: center; 
  font-size: 20px; 
  box-shadow: 0 4px 6px rgba(59, 130, 246, 0.2);
}

.user-avatar { 
  background: #e2e8f0; 
  color: #475569; 
  border: 2px solid #ffffff;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.message-content { 
  display: flex; 
  flex-direction: column; 
  align-items: flex-start; 
  gap: 6px; 
  min-width: 0; 
  flex: 1; 
}

.message-item.user .message-content { align-items: flex-end; }

.message-header { 
  display: flex; 
  align-items: center; 
  gap: 8px; 
  font-size: 13px; 
  margin: 0 4px; 
}

.message-sender { font-weight: 600; color: #475569; }
.message-time { color: #94a3b8; }

/* 气泡美化 */
.message-body {
  position: relative;
  background: #ffffff;
  color: #334155;
  padding: 16px 20px;
  border-radius: 16px;
  border-top-left-radius: 4px;
  font-size: 15px;
  line-height: 1.6;
  min-width: 60px;
  word-break: break-word;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
  border: 1px solid rgba(226, 232, 240, 0.8);
}

.message-item.assistant .message-body {
  padding-right: 48px;
}

.message-item.user .message-body {
  background: #ebf5ff; /* 淡蓝底色 */
  color: #0f172a;
  border-radius: 16px;
  border-top-right-radius: 4px;
  border: 1px solid #bfdbfe;
  padding-right: 20px;
}

.copy-btn { 
  position: absolute; 
  top: 12px; 
  right: 12px; 
  opacity: 0; 
  transition: all 0.2s ease-in-out; 
  border: none; 
  background: #f8fafc; 
  color: #64748b; 
  z-index: 2; 
}

.message-body:hover .copy-btn { opacity: 1; }
.copy-btn:hover { background: #eff6ff; color: #3b82f6; transform: scale(1.1); }

/* 附件区域 */
.message-attachments { display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 12px; }
.attachment-image { max-width: 280px; max-height: 280px; border-radius: 12px; object-fit: cover; border: 1px solid #e2e8f0; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }

.attachment-doc-card { 
  display: flex; 
  align-items: center; 
  gap: 12px; 
  background: #f8fafc; 
  border: 1px solid #e2e8f0; 
  padding: 12px 16px; 
  border-radius: 12px; 
  min-width: 220px; 
}

.message-item.user .attachment-doc-card { 
  background: rgba(255,255,255,0.6); 
  border-color: #bfdbfe; 
}

.doc-icon-wrap { width: 36px; height: 36px; background: #ef4444; color: white; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 18px; }
.doc-info { display: flex; flex-direction: column; gap: 2px; }
.doc-name { font-size: 14px; font-weight: 600; max-width: 160px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; color: #1e293b; }
.doc-size { font-size: 12px; color: #64748b; }

/* Markdown 样式增强 */
.message-text { word-break: break-word; color: #334155; }
.message-text :deep(p) { margin: 0 0 12px 0; padding: 0; line-height: 1.7; }
.message-text :deep(p:last-child) { margin-bottom: 0; }
.message-text :deep(h1), .message-text :deep(h2), .message-text :deep(h3) { margin: 16px 0 12px; color: #0f172a; font-weight: 600; }
.message-text :deep(ul), .message-text :deep(ol) { margin: 0 0 12px 0; padding-left: 24px; }
.message-text :deep(li) { margin-bottom: 6px; }
.message-text :deep(pre.hljs) { background: #f8fafc; color: #1f2937; padding: 16px; border-radius: 12px; border: 1px solid #e2e8f0; overflow-x: auto; margin: 12px 0; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size: 13px; line-height: 1.5; }
.message-text :deep(code) { background: #f1f5f9; padding: 2px 6px; border-radius: 6px; color: #ef4444; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size: 0.9em; }
.message-text :deep(pre.hljs code) { background: transparent; color: inherit; padding: 0; border-radius: 0; }
.message-text :deep(table) { width: 100%; border-collapse: collapse; margin: 16px 0; border-radius: 8px; overflow: hidden; border: 1px solid #e2e8f0; }
.message-text :deep(th), .message-text :deep(td) { border-bottom: 1px solid #e2e8f0; padding: 10px 16px; text-align: left; }
.message-text :deep(th) { background-color: #f8fafc; font-weight: 600; color: #475569; border-bottom: 2px solid #e2e8f0; }
.message-text :deep(blockquote) { margin: 12px 0; padding: 12px 20px; color: #475569; background-color: #f8fafc; border-left: 4px solid #3b82f6; border-radius: 0 8px 8px 0; }

/* 预览卡片组 */
.doc-preview-cards { margin-top: 16px; display: flex; flex-direction: column; gap: 12px; }
.doc-card { display: flex; align-items: center; gap: 16px; padding: 16px; border-radius: 12px; background: #ffffff; box-shadow: 0 1px 3px rgba(0,0,0,0.05); transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); border: 1px solid #e2e8f0; width: 100%; max-width: 480px; }
.doc-card:hover { transform: translateY(-2px); box-shadow: 0 10px 15px -3px rgba(0,0,0,0.05); border-color: #cbd5e1; }
.doc-card-icon { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.doc-card-info { flex: 1; display: flex; flex-direction: column; gap: 4px; overflow: hidden; }
.doc-card-name { font-weight: 600; font-size: 15px; color: #1e293b; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.doc-card-type { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; }
.doc-card-desc { font-size: 13px; color: #64748b; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.doc-card-actions { display: flex; flex-direction: column; gap: 8px; flex-shrink: 0; width: 80px; }
.doc-card-actions .el-button { margin-left: 0 !important; width: 100%; }

.entity-preview-cards { margin-top: 16px; display: flex; flex-direction: column; gap: 12px; }
.entity-card { display: flex; align-items: flex-start; gap: 16px; padding: 16px; background: #ffffff; border-radius: 12px; border: 1px solid #e2e8f0; box-shadow: 0 1px 3px rgba(0,0,0,0.05); transition: all 0.2s; }
.entity-card:hover { transform: translateY(-2px); box-shadow: 0 10px 15px -3px rgba(0,0,0,0.05); border-color: #bfdbfe; }
.entity-card-icon { width: 44px; height: 44px; background: #eff6ff; color: #3b82f6; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.entity-card-info { flex: 1; display: flex; flex-direction: column; gap: 6px; }
.entity-card-name { font-weight: 600; font-size: 16px; color: #1e293b; }
.entity-card-desc { font-size: 13px; color: #64748b; line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.entity-card-meta { display: flex; gap: 8px; margin-top: 2px; }
.entity-card-actions { display: flex; flex-direction: column; gap: 8px; flex-shrink: 0; }

/* 输入区重构：嵌入式设计 (Inset style) */
.input-area-container { 
  padding: 0 10% 24px; 
  background: #f8fafc; 
}

.input-container-inner { 
  background: #ffffff; 
  border: 1px solid #e2e8f0; 
  border-radius: 16px; 
  transition: all 0.3s; 
  position: relative;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
  display: flex;
  flex-direction: column;
}

.input-container-inner:focus-within { 
  border-color: #3b82f6; 
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.05), 0 0 0 3px rgba(59, 130, 246, 0.1); 
}

.input-container-inner.drag-over { 
  border-color: #3b82f6; 
  background: #f0f9ff; 
  border-style: dashed;
}

.staging-area { 
  display: flex; 
  flex-wrap: wrap; 
  gap: 12px; 
  padding: 16px 16px 0 16px; 
}

.staging-file { 
  position: relative; 
  height: 64px; 
  border-radius: 10px; 
  background: #f8fafc; 
  border: 1px solid #e2e8f0; 
  display: flex; 
  align-items: center; 
  justify-content: center; 
  overflow: hidden; 
}

.staging-image { height: 100%; width: 100%; object-fit: cover; }

.staging-doc-pill { 
  display: flex; 
  align-items: center; 
  gap: 10px; 
  padding: 0 12px; 
  height: 100%; 
}

.staging-doc-icon { 
  width: 32px; 
  height: 32px; 
  border-radius: 8px; 
  background: #eff6ff; 
  display: flex; 
  align-items: center; 
  justify-content: center; 
  color: #3b82f6; 
  font-size: 18px; 
}

.staging-doc-info { display: flex; flex-direction: column; overflow: hidden; }
.staging-doc-name { font-size: 13px; font-weight: 600; color: #1e293b; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 120px;}
.staging-doc-size { font-size: 11px; color: #64748b; margin-top: 2px; }

.remove-file-btn { 
  position: absolute; 
  top: -8px; 
  right: -8px; 
  width: 22px; 
  height: 22px; 
  border-radius: 50%; 
  background: #ef4444; 
  color: white; 
  border: 2px solid #ffffff; 
  display: flex; 
  align-items: center; 
  justify-content: center; 
  font-size: 12px; 
  cursor: pointer; 
  opacity: 0; 
  transition: all 0.2s; 
}

.staging-file:hover .remove-file-btn { opacity: 1; top: -4px; right: -4px; }

.input-wrapper { 
  display: flex; 
  align-items: flex-end; 
  padding: 12px 16px; 
  gap: 12px; 
}

.toolbar-btn { 
  background: #f1f5f9; 
  border: none; 
  color: #64748b; 
  margin-bottom: 2px; 
  transition: all 0.2s; 
}

.toolbar-btn:hover { background: #e2e8f0; color: #1e293b; }

.message-input { flex: 1; }
.message-input :deep(.el-textarea__inner) { 
  background: transparent; 
  border: none; 
  box-shadow: none; 
  padding: 8px 0; 
  font-size: 15px; 
  line-height: 1.6; 
  color: #1e293b; 
  resize: none; 
}
.message-input :deep(.el-textarea__inner::placeholder) { color: #94a3b8; }

.send-btn { 
  background: #f1f5f9; 
  border: none; 
  color: #94a3b8; 
  margin-bottom: 2px; 
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); 
}

.send-btn.can-send { background: #1e293b; color: #ffffff; }
.send-btn.can-send:hover { transform: scale(1.05); background: #3b82f6; }

.input-tips { 
  text-align: center; 
  font-size: 12px; 
  color: #94a3b8; 
  margin-top: 12px; 
}

/* Typing 动画 */
.typing-indicator { display: flex; gap: 5px; padding: 4px 8px; }
.typing-indicator span { 
  width: 6px; 
  height: 6px; 
  background: #94a3b8; 
  border-radius: 50%; 
  animation: typing 1.4s infinite ease-in-out both; 
}
.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }
@keyframes typing { 0%, 80%, 100% { transform: scale(0); opacity: 0.4; } 40% { transform: scale(1); opacity: 1; } }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
@keyframes slideUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }

@media (max-width: 768px) {
  .ai-chat-container { height: 100vh; }
  .chat-main { border-radius: 0; }
  .messages-container, .input-area-container { padding-left: 16px; padding-right: 16px; }
  .message-wrapper { max-width: 95%; }
  .staging-file { width: 60px; height: 60px; }
  .attachment-image { max-width: 200px; }
  .copy-btn { opacity: 1; }
}
</style>
