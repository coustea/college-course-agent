import { ref, computed, nextTick, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import MarkdownIt from "markdown-it";
import hljs from "highlight.js";
import "highlight.js/styles/github.css";
import {
  getUserConversations,
  createConversation,
  getConversationMessages,
  deleteChat,
  sendChatStream,
} from "@/services/chatApi";

/**
 * Shared AI Chat composable
 * Extracts common chat logic used by both TeacherAIChat and StudentAIChat
 *
 * @param {Object} options - Configuration options
 * @param {string} options.conversationTitle - Title for new conversations
 * @param {string} options.defaultUsername - Default username fallback
 * @param {Array} options.quickPrompts - Quick prompt suggestions [{ label, icon, text }]
 * @returns {Object} Chat state and methods
 */
export function useChat({
  conversationTitle = "AI 助手",
  defaultUsername = "user",
  quickPrompts = [],
} = {}) {
  // ==================== Utilities ====================
  const getUsername = (fallback = "user") => {
    try {
      const userName = localStorage.getItem("userName");
      if (userName) return userName;
      const u = JSON.parse(localStorage.getItem("userInfo") || "null");
      return u?.username || u?.name || fallback;
    } catch {
      return fallback;
    }
  };

  const formatTime = (ts) => {
    if (!ts) return "";
    return new Date(ts).toLocaleTimeString("zh-CN", {
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const formatFileSize = (b) => {
    if (b < 1024) return b + " B";
    if (b < 1048576) return (b / 1024).toFixed(1) + " KB";
    return (b / 1048576).toFixed(1) + " MB";
  };

  // ==================== State ====================
  const messages = ref([]);
  const inputMessage = ref("");
  const isLoading = ref(false);
  const uploadedFiles = ref([]);
  const messagesContainer = ref(null);
  const fileInput = ref(null);
  const isDragOver = ref(false);
  const conversationId = ref("");
  const username = ref(getUsername(defaultUsername));

  // ==================== Computed ====================
  const canSend = computed(
    () => (inputMessage.value.trim() || uploadedFiles.value.length) && !isLoading.value
  );

  // ==================== Markdown Rendering ====================
  const md = new MarkdownIt({
    html: true,
    linkify: true,
    typographer: true,
    breaks: true,
    highlight(str, lang) {
      if (lang && hljs.getLanguage(lang)) {
        try {
          return (
            '<pre class="hljs"><code>' +
            hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
            "</code></pre>"
          );
        } catch {}
      }
      return (
        '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + "</code></pre>"
      );
    },
  });

  const renderMarkdown = (text) => {
    if (!text) return "";
    const cleaned = text.replace(/\[document:[^\]]+\]/g, "").replace(/\[entity:[^\]]+\]/g, "");
    return md.render(cleaned);
  };

  const parseDocumentLinks = (text) => {
    if (!text) return [];
    const regex = /\[document:([^\]]+)\]/g;
    const docs = [];
    let match;
    while ((match = regex.exec(text)) !== null) {
      const parts = match[1].split("|");
      if (parts.length >= 2) {
        docs.push({ url: parts[0], filename: parts[1], type: parts[2] || "" });
      }
    }
    return docs;
  };

  const getFileTypeColor = (type) => {
    const colors = {
      docx: "#2b5797", xlsx: "#217346", pdf: "#d32f2f",
      md: "#4a5568", txt: "#718096", xls: "#217346", doc: "#2b5797",
    };
    return colors[(type || "").toLowerCase()] || "#606266";
  };

  const getFileTypeBgColor = (type) => {
    const colors = {
      docx: "#e8f0fe", xlsx: "#e6f4ea", pdf: "#fce8e6",
      md: "#f0f2f5", txt: "#f0f2f5", xls: "#e6f4ea", doc: "#e8f0fe",
    };
    return colors[(type || "").toLowerCase()] || "#f5f7fa";
  };

  const getFileTypeLabel = (type) => {
    const labels = {
      docx: "Word", xlsx: "Excel", pdf: "PDF",
      md: "Markdown", txt: "文本", xls: "Excel", doc: "Word",
    };
    return labels[(type || "").toLowerCase()] || type || "文件";
  };

  const normalizeGeneratedFile = (file) => {
    if (!file) return null;
    const name =
      file.name || file.fileName || file.filename || file.originalFilename || "未命名文件";
    const url = file.url || file.fileUrl || file.path || "";
    if (!url) return null;
    const rawType =
      file.type || file.fileType || (name.includes(".") ? name.split(".").pop() : "");
    return {
      name,
      url,
      type: String(rawType || "").toLowerCase(),
      description: file.description || "",
      generated: file.generated !== false,
    };
  };

  const downloadFile = (url, filename) => {
    const link = document.createElement("a");
    link.href = url;
    link.download = filename || url.split("/").pop();
    link.target = "_blank";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const previewFile = (url, type) => {
    window.open(url, "_blank");
  };

  const parseEntityLinks = (text) => {
    if (!text) return [];
    const regex = /\[entity:([^\]]+)\]/g;
    const entities = [];
    let match;
    while ((match = regex.exec(text)) !== null) {
      const parts = match[1].split("|");
      if (parts.length >= 2) {
        if (parts[0] === "course") {
          entities.push({ type: "course", id: parts[1], name: parts[2], description: parts[3], teacher: parts[4], credits: parts[5] });
        }
      }
    }
    return entities;
  };

  const scrollToBottom = () =>
    nextTick(() => {
      if (messagesContainer.value)
        messagesContainer.value.scrollTop =
          messagesContainer.value.scrollHeight;
    });

  // ==================== Conversation Management ====================
  const ensureConversation = async () => {
    try {
      const res = await getUserConversations(username.value);
      if (res?.code === 200 && res.data?.length > 0) {
        conversationId.value = res.data[0].conversationId;
        return;
      }
    } catch {}
    try {
      const res = await createConversation(conversationTitle);
      if (res?.code === 200) conversationId.value = res.data.conversationId;
    } catch (e) {
      console.error("创建会话失败", e);
    }
  };

  const loadHistory = async () => {
    try {
      if (!conversationId.value) {
        await ensureConversation();
      }
      const res = await getConversationMessages(conversationId.value);
      if (res?.code === 200 && res.data?.messages) {
        messages.value = res.data.messages.map((m) => {
          let parsedFiles = [];
          let generatedFiles = [];
          if (m.files) {
            try {
              const backendFiles =
                typeof m.files === "string" ? JSON.parse(m.files) : m.files;
              backendFiles.forEach((f) => {
                if (f?.generated || f?.source === "generated") {
                  const generated = normalizeGeneratedFile(f);
                  if (generated) generatedFiles.push(generated);
                  return;
                }
                const getFileName =
                  f.fileName || f.filename || f.name || f.originalFilename || "未命名文件";
                const getFileUrl = f.url || f.fileUrl || f.path || "";
                const getFileSize = f.size || f.fileSize || f.length || 0;
                const isImg =
                  f.isImage !== undefined
                    ? f.isImage
                    : !!getFileName.match(/\.(jpeg|jpg|gif|png|webp)$/i);
                parsedFiles.push({
                  name: getFileName,
                  url: getFileUrl,
                  isImage: isImg,
                  size: getFileSize,
                });
              });
            } catch (e) {
              console.error("解析历史文件记录失败", e);
            }
          }
          return {
            role: m.role,
            content: m.content,
            files: parsedFiles,
            generatedFiles,
            timestamp: m.createdAt,
          };
        });
        scrollToBottom();
      }
    } catch (e) {
      console.error("加载历史失败", e);
    }
  };

  // ==================== File Handling ====================
  const processFiles = (list) => {
    list.forEach((file) => {
      if (file.size > 20 * 1024 * 1024) {
        ElMessage.warning(`文件 ${file.name} 超过 20MB`);
        return;
      }
      const isImage = file.type.startsWith("image/");
      uploadedFiles.value.push({
        file,
        url: isImage ? URL.createObjectURL(file) : null,
        name: file.name,
        size: file.size,
        isImage,
      });
    });
  };

  const triggerFileUpload = () => fileInput.value?.click();

  const handleFileUpload = (e) => {
    processFiles(Array.from(e.target.files || []));
    e.target.value = "";
  };

  const handleDragOver = () => (isDragOver.value = true);

  const handleDragLeave = () => (isDragOver.value = false);

  const handleDrop = (e) => {
    isDragOver.value = false;
    processFiles(Array.from(e.dataTransfer.files || []));
  };

  const removeFile = (i) => {
    const f = uploadedFiles.value[i];
    if (f.url) URL.revokeObjectURL(f.url);
    uploadedFiles.value.splice(i, 1);
  };

  // ==================== Message Actions ====================
  const clearChatHistory = async () => {
    try {
      await ElMessageBox.confirm(
        "确定要清空所有聊天记录吗？此操作将无法恢复。",
        "清空对话",
        {
          confirmButtonText: "确定清空",
          cancelButtonText: "取消",
          type: "warning",
          confirmButtonClass: "el-button--danger",
        }
      );
      isLoading.value = true;
      await deleteChat();
      messages.value = [];
      ElMessage.success("聊天记录已清空");
    } catch (err) {
      if (err !== "cancel") ElMessage.error("清空失败");
    } finally {
      isLoading.value = false;
    }
  };

  const copyMessage = (c) =>
    navigator.clipboard.writeText(c).then(() =>
      ElMessage.success("已复制到剪贴板")
    );

  const sendQuickPrompt = (prompt) => {
    inputMessage.value = prompt;
    sendMessage();
  };

  const handleKeydown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  // ==================== Send Message ====================
  const sendMessage = async () => {
    if (!canSend.value) return;
    const content = inputMessage.value.trim();
    const files = [...uploadedFiles.value];

    if (!conversationId.value) {
      await ensureConversation();
      if (!conversationId.value) {
        ElMessage.error("无法创建会话");
        return;
      }
    }

    messages.value.push({
      role: "user",
      content,
      files,
      timestamp: Date.now(),
    });
    inputMessage.value = "";
    uploadedFiles.value = [];
    scrollToBottom();
    isLoading.value = true;

    messages.value.push({ role: "assistant", content: "", generatedFiles: [], timestamp: Date.now() });
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
        if (
          !messages.value[aiMsgIndex].content &&
          !(messages.value[aiMsgIndex].generatedFiles || []).length
        ) {
          messages.value[aiMsgIndex].content =
            "抱歉，AI 暂时无法回复，请稍后重试。";
        }
        isLoading.value = false;
        scrollToBottom();
      },
      (filesMeta) => {
        messages.value[aiMsgIndex].generatedFiles = (filesMeta || [])
          .map(normalizeGeneratedFile)
          .filter(Boolean);
        scrollToBottom();
      }
    );
  };

  // ==================== Lifecycle ====================
  onMounted(async () => {
    await ensureConversation();
    await loadHistory();
  });

  // ==================== Return API ====================
  return {
    // State
    messages,
    inputMessage,
    isLoading,
    uploadedFiles,
    messagesContainer,
    fileInput,
    isDragOver,
    conversationId,
    username,
    canSend,
    quickPrompts,

    // Utilities
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
    scrollToBottom,

    // Conversation
    ensureConversation,
    loadHistory,

    // File handling
    processFiles,
    triggerFileUpload,
    handleFileUpload,
    handleDragOver,
    handleDragLeave,
    handleDrop,
    removeFile,

    // Message actions
    clearChatHistory,
    copyMessage,
    sendQuickPrompt,
    handleKeydown,
    sendMessage,
  };
}
