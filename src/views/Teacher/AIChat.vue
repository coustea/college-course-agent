<template>
  <div class="ai-chat-container">
    <DocumentViewer v-if="showDocViewer" v-model="showDocViewer" :fileUrl="docPreviewUrl" :title="docPreviewTitle" />
    <div class="chat-main">
      <div class="chat-header">
        <div class="header-title">
          <el-icon class="title-icon"><ChatDotRound /></el-icon>
          <h2>AI 教学助手</h2>
        </div>
        <el-button
          v-if="messages.length > 0"
          type="danger"
          plain
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
          <h1 class="welcome-title">有什么我可以帮您的？</h1>
          <p class="welcome-subtitle">
            支持上传文档与图片进行深度分析、教案生成等教学辅助工作
          </p>
          <div class="quick-prompts">
            <div
              class="prompt-card"
              @click="sendQuickPrompt('帮我分析一下这批学生的学习数据')"
            >
              <el-icon class="prompt-icon"><DataAnalysis /></el-icon>
              <span>数据分析</span>
            </div>
            <div
              class="prompt-card"
              @click="sendQuickPrompt('生成一份45分钟的公开课教案')"
            >
              <el-icon class="prompt-icon"><Document /></el-icon>
              <span>生成教案</span>
            </div>
            <div
              class="prompt-card"
              @click="sendQuickPrompt('如何提升课堂互动率？')"
            >
              <el-icon class="prompt-icon"><QuestionFilled /></el-icon>
              <span>教学建议</span>
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
                    msg.role === "user" ? "您" : "AI 助手"
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
                        <el-button size="small" type="primary" @click="window.open(`/course/${entity.id}`, '_blank')">查看详情</el-button>
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
              :icon="Plus"
              circle
              class="upload-btn"
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
import { Plus, Delete, Promotion, Close, ChatDotRound, User, Document, DataAnalysis, QuestionFilled, CopyDocument, Reading } from "@element-plus/icons-vue";
import { useChat } from "@/composables/useChat";
import { ref } from "vue";
import DocumentViewer from "@/components/DocumentViewer.vue";

const showDocViewer = ref(false);
const docPreviewUrl = ref("");
const docPreviewTitle = ref("");

const openDocumentViewer = (url, name) => {
  docPreviewUrl.value = url;
  docPreviewTitle.value = name || "文档预览";
  showDocViewer.value = true;
};

// Use the shared chat composable with teacher-specific configuration
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
  conversationTitle: "AI 教学助手",
  defaultUsername: "teacher",
});
</script>

<style scoped>
.doc-preview-cards { margin-top: 12px; display: flex; flex-direction: column; gap: 8px; }
.doc-card { display: flex; align-items: center; gap: 12px; padding: 12px 16px; border-radius: 10px; border: 1px solid; transition: transform 0.2s, box-shadow 0.2s; }
.doc-card:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
.doc-card-icon { width: 40px; height: 40px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.doc-card-info { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.doc-card-name { font-weight: 600; font-size: 14px; color: #303133; }
.doc-card-type { font-size: 12px; font-weight: 500; }
.doc-card-desc { font-size: 12px; color: #606266; line-height: 1.4; }
.doc-card-actions { display: flex; gap: 8px; flex-shrink: 0; }

.entity-preview-cards { margin-top: 12px; display: flex; flex-direction: column; gap: 8px; }
.entity-card { display: flex; align-items: flex-start; gap: 16px; padding: 16px; background: #fff; border-radius: 12px; border: 1px solid #e4e7ed; box-shadow: 0 2px 8px rgba(0,0,0,0.04); transition: transform 0.2s; }
.entity-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
.entity-card-icon { width: 48px; height: 48px; background: #ecf5ff; color: #409eff; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.entity-card-info { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.entity-card-name { font-weight: 600; font-size: 16px; color: #303133; }
.entity-card-desc { font-size: 13px; color: #606266; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.entity-card-meta { display: flex; gap: 8px; margin-top: 4px; }
.entity-card-actions { display: flex; flex-direction: column; gap: 8px; flex-shrink: 0; }

.ai-chat-container { display: flex; margin: -30px; height: 100vh; background: #ffffff; box-sizing: border-box; }
.chat-main { display: flex; flex-direction: column; width: 100%; height: 100%; background: #ffffff; overflow: hidden; }
.chat-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 24px; border-bottom: 1px solid #f3f4f6; background: rgba(255, 255, 255, 0.9); backdrop-filter: blur(10px); z-index: 10; }
.header-title { display: flex; align-items: center; gap: 10px; }
.header-title h2 { margin: 0; font-size: 18px; font-weight: 600; color: #111827; }
.title-icon { font-size: 22px; color: #667eea; }
.clear-btn { border-radius: 8px; font-weight: 500; }
.messages-container { flex: 1; overflow-y: auto; padding: 24px 10%; scroll-behavior: smooth; }
.messages-container::-webkit-scrollbar { width: 6px; }
.messages-container::-webkit-scrollbar-thumb { background: #e5e7eb; border-radius: 4px; }
.welcome-screen { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; text-align: center; animation: fadeIn 0.5s ease-out; }
.avatar-inner { width: 80px; height: 80px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; margin-bottom: 20px; box-shadow: 0 8px 20px rgba(102, 126, 234, 0.3); }
.welcome-title { font-size: 28px; font-weight: 600; color: #111827; margin-bottom: 8px; }
.welcome-subtitle { color: #6b7280; margin-bottom: 30px; }
.quick-prompts { display: flex; gap: 16px; justify-content: center; }
.prompt-card { display: flex; align-items: center; gap: 10px; padding: 12px 20px; background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 12px; cursor: pointer; color: #374151; font-weight: 500; transition: all 0.2s; }
.prompt-card:hover { background: #f3f4f6; border-color: #667eea; color: #667eea; transform: translateY(-2px); }
.prompt-icon { font-size: 18px; }
.messages-list { display: flex; flex-direction: column; gap: 24px; }
.message-item { display: flex; animation: slideUp 0.3s ease-out; }

/* 聊天气泡布局优化（防拉扁拉宽） */
.message-wrapper {
  display: flex;
  gap: 16px;
  max-width: 50%;
}
@media (min-width: 1024px) {
  .message-wrapper {
    max-width: 50%;
  }
}

.message-item.user { justify-content: flex-end; }
.message-item.user .message-wrapper { flex-direction: row-reverse; }
.message-item.assistant .message-wrapper { flex-direction: row; }
.message-avatar { flex-shrink: 0; }
.ai-avatar { width: 36px; height: 36px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 20px; }
.user-avatar { background: #e5e7eb; color: #4b5563; }
.message-content { display: flex; flex-direction: column; align-items: flex-start; gap: 4px; min-width: 0; flex: 1; }
.message-item.user .message-content { align-items: flex-end; }
.message-header { display: flex; align-items: center; gap: 8px; font-size: 13px; margin-left: 4px; }
.message-sender { font-weight: 600; color: #374151; }
.message-time { color: #9ca3af; }

/* ========== 气泡美化 ========== */
.message-body {
  position: relative;
  background: #f3f4f6;
  color: #1f2937;
  padding: 12px 16px;
  padding-right: 44px;
  border-radius: 16px;
  border-top-left-radius: 2px;
  font-size: 15px;
  line-height: 1.6;
  min-width: 60px;
  word-break: break-word;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.message-item.user .message-body {
  background: #ebf5ff;
  color: #111827;
  border-radius: 16px;
  border-top-right-radius: 2px;
  padding-right: 16px;
}

/* ========== 复制按钮操作区 ========== */
.copy-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  opacity: 0;
  transition: opacity 0.2s ease-in-out;
  border: none;
  background: rgba(255, 255, 255, 0.7);
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  color: #6b7280;
  z-index: 2;
}
.message-body:hover .copy-btn {
  opacity: 1;
}
.copy-btn:hover {
  background: #ffffff;
  color: #667eea;
}

.message-attachments { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 10px; }
.attachment-image { max-width: 250px; max-height: 250px; border-radius: 12px; object-fit: cover; border: 1px solid rgba(0, 0, 0, 0.1); }
.attachment-doc-card { display: flex; align-items: center; gap: 12px; background: #ffffff; border: 1px solid rgba(0, 0, 0, 0.1); padding: 10px 14px; border-radius: 12px; min-width: 200px; }
.message-item.user .attachment-doc-card { background: rgba(255, 255, 255, 0.7); border-color: rgba(0, 0, 0, 0.05); }
.doc-icon-wrap { width: 36px; height: 36px; background: #ef4444; color: white; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 18px; }
.doc-info { display: flex; flex-direction: column; }
.doc-name { font-size: 14px; font-weight: 600; max-width: 150px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.doc-size { font-size: 12px; color: #6b7280; }

/* Markdown 渲染样式增强 */
.message-text { word-break: break-word; }
.message-text :deep(p) { margin: 0 0 10px 0; padding: 0; }
.message-text :deep(p:last-child) { margin-bottom: 0; }
.message-text :deep(pre.hljs) { background: #ffffff; color: #1f2937; padding: 12px; border-radius: 8px; border: 1px solid #e5e7eb; overflow-x: auto; margin: 10px 0; font-family: monospace; }
.message-text :deep(code) { background: rgba(0, 0, 0, 0.05); padding: 2px 6px; border-radius: 4px; color: #ef4444; }
.message-text :deep(pre.hljs code) { background: transparent; color: inherit; padding: 0; }
.message-text :deep(table) { width: 100%; border-collapse: collapse; margin: 12px 0; }
.message-text :deep(th), .message-text :deep(td) { border: 1px solid #d1d5db; padding: 8px 12px; }
.message-text :deep(th) { background-color: #e5e7eb; font-weight: 600; }
.message-text :deep(blockquote) { margin: 10px 0; padding: 8px 16px; color: #4b5563; background-color: #f8fafc; border-left: 4px solid #667eea; border-radius: 0 8px 8px 0; }

.input-area-container { padding: 20px 10%; background: #ffffff; border-top: 1px solid #f3f4f6; }
.input-container-inner { background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 20px; transition: all 0.3s; position: relative; }
.input-container-inner:focus-within { border-color: #667eea; box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1); background: #ffffff; }
.input-container-inner.drag-over { border-color: #667eea; background: #ebf5ff; }
.staging-area { display: flex; flex-wrap: wrap; gap: 12px; padding: 16px 16px 0 16px; }
.staging-file { position: relative; height: 70px; border-radius: 12px; background: #ffffff; border: 1px solid #e5e7eb; display: flex; align-items: center; justify-content: center; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05); }
.staging-image { width: 70px; height: 70px; object-fit: cover; border-radius: 12px; }
.staging-doc-pill { display: flex; align-items: center; gap: 10px; padding: 0 16px 0 10px; min-width: 140px; max-width: 250px; height: 100%; }
.staging-doc-icon { width: 40px; height: 40px; background: #f3f4f6; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: #667eea; font-size: 20px; }
.staging-doc-info { display: flex; flex-direction: column; overflow: hidden; }
.staging-doc-name { font-size: 13px; font-weight: 600; color: #111827; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.staging-doc-size { font-size: 11px; color: #6b7280; margin-top: 2px; }
.remove-file-btn { position: absolute; top: -6px; right: -6px; width: 20px; height: 20px; border-radius: 50%; background: #374151; color: white; border: none; display: flex; align-items: center; justify-content: center; font-size: 12px; cursor: pointer; opacity: 0; transition: opacity 0.2s; }
.staging-file:hover .remove-file-btn { opacity: 1; }
.input-wrapper { display: flex; align-items: flex-end; padding: 12px 16px; gap: 12px; }
.upload-btn { background: #f3f4f6; border: none; color: #4b5563; margin-bottom: 2px; }
.upload-btn:hover { background: #e5e7eb; color: #111827; }
.message-input { flex: 1; }
.message-input :deep(.el-textarea__inner) { background: transparent; border: none; box-shadow: none; padding: 8px 0; font-size: 15px; line-height: 1.5; color: #111827; resize: none; }
.send-btn { background: #e5e7eb; border: none; color: #9ca3af; margin-bottom: 2px; transition: all 0.3s; }
.send-btn.can-send { background: #111827; color: #ffffff; }
.send-btn.can-send:hover { transform: scale(1.05); }
.input-tips { text-align: center; font-size: 12px; color: #9ca3af; margin-top: 12px; }
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
  .copy-btn { opacity: 1; }
}
</style>
