import request from '@/utils/request'

// request 的 baseURL 已修改为 http://localhost:9999（不含 /api）
// 所以所有 axios 请求都需要加 /api 前缀
const CHAT_PATH = '/api/ai/chat'
const CONV_PATH = '/api/ai/conversation'
// 原生 fetch 需要 /api 前缀（不走 axios baseURL）
const FETCH_API_BASE = '/api'

// ======================== 会话管理 ========================

/** 创建新会话 */
export async function createConversation(title = '新对话') {
  const res = await request.post(`${CONV_PATH}/create`, { title })
  return res.data
}

/** 获取用户所有会话列表 */
export async function getUserConversations(username) {
  const res = await request.get(`${CONV_PATH}/list/${username}`)
  return res.data
}

/** 获取会话历史消息 */
export async function getConversationMessages(conversationId) {
  const res = await request.get(`${CONV_PATH}/${conversationId}/messages`)
  return res.data
}

/** 删除会话 */
export async function deleteConversation(conversationId) {
  const res = await request.delete(`${CONV_PATH}/${conversationId}`)
  return res.data
}

/** 更新会话标题 */
export async function updateConversationTitle(conversationId, title) {
  const res = await request.put(`${CONV_PATH}/${conversationId}/title`, { title })
  return res.data
}

// ======================== 聊天 ========================

/** 获取聊天历史 */
export async function getChatHistory() {
  const res = await request.get(`${CHAT_PATH}/history`)
  return res.data
}

/** 删除聊天记录 */
export async function deleteChat() {
  const res = await request.delete(`${CHAT_PATH}/delete`)
  return res.data
}

/**
 * SSE 流式聊天（携带 JWT Token）
 * @param {string} conversationId - 会话ID
 * @param {string} message - 用户消息
 * @param {Array} files - 上传文件列表 [{file: File, ...}]
 * @param {Function} onChunk - 每收到一个文本片段时的回调 (text: string) => void
 * @param {Function} onError - 出错回调 (error: string) => void
 * @param {Function} onDone - 完成回调 () => void
 * @param {Function} onFiles - 文件元数据回调 (files: Array) => void
 * @returns {AbortController} 用于外部中断流式请求
 */
export function sendChatStream(conversationId, message, files, onChunk, onError, onDone, onFiles) {
  const controller = new AbortController()

  const fd = new FormData()
  fd.append('conversationId', conversationId)
  fd.append('message', message || '请分析上传的文件')
  if (files && files.length > 0) {
    files.forEach((f) => { if (f.file) fd.append('files', f.file) })
  }

  const token = localStorage.getItem('token') || localStorage.getItem('userToken')
  const headers = { 'Accept': 'text/event-stream' }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }


  fetch(`${FETCH_API_BASE}/ai/chat/stream`, {
    method: 'POST',
    headers,
    body: fd,
    signal: controller.signal,
  })
    .then(async (response) => {

      if (!response.ok) {
        const errorText = await response.text()
        console.error('[ChatStream] HTTP错误:', response.status, errorText)
        onError(`HTTP ${response.status}: ${errorText}`)
        return
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''
      let chunkCount = 0

      const processLine = (line) => {
        let jsonStr = null
        if (line.startsWith('data:')) {
          jsonStr = line.substring(5).trim()
        } else if (line.startsWith('{') && line.endsWith('}')) {
          jsonStr = line // 容错：有些流可能会漏掉 data: 前缀
        }

        if (jsonStr && jsonStr !== '[DONE]') {
          try {
            const obj = JSON.parse(jsonStr)
            if (obj.code && obj.code !== 200) {
              console.error('[ChatStream] 服务器错误:', obj)
              onError(obj.message || '服务器错误')
            } else if (Array.isArray(obj.files)) {
              onFiles?.(obj.files)
            } else if (obj.content !== undefined && obj.content !== null) {
              chunkCount++
              onChunk(String(obj.content))
            }
          } catch (e) {
            // 忽略非 JSON 数据残余
          }
        }
      }

      try {
        while (true) {
          const { done, value } = await reader.read()

          if (value) {
            buffer += decoder.decode(value, { stream: true })
            const lines = buffer.split('\n')
            buffer = lines.pop() || ''

            for (const line of lines) {
              if (line.trim()) processLine(line.trim())
            }
          }

          if (done) {
            if (buffer.trim()) processLine(buffer.trim())
            onDone()
            break
          }
        }
      } catch (readError) {
        console.error('[ChatStream] 读取流数据失败:', readError)
        onError(readError.message || '读取响应失败')
      }
    })
    .catch((err) => {
      if (err.name === 'AbortError') {
      } else {
        console.error('[ChatStream] 网络错误:', err)
        onError(err.message || '网络错误')
      }
    })

  return controller
}
