import axios from 'axios'
import { getAuthHeaders } from './auth'

const API_BASE = import.meta?.env?.VITE_API_BASE_URL || '/api'

// ======================== 会话管理 ========================

/** 创建新会话 */
export async function createConversation(username, title = '新对话') {
  const res = await axios.post(`${API_BASE}/ai/conversation/create`, { username, title })
  return res.data
}

/** 获取用户所有会话列表 */
export async function getUserConversations(username) {
  const res = await axios.get(`${API_BASE}/ai/conversation/list/${username}`)
  return res.data
}

/** 获取会话历史消息 */
export async function getConversationMessages(conversationId) {
  const res = await axios.get(`${API_BASE}/ai/conversation/${conversationId}/messages`)
  return res.data
}

/** 删除会话 */
export async function deleteConversation(conversationId) {
  const res = await axios.delete(`${API_BASE}/ai/conversation/${conversationId}`)
  return res.data
}

/** 更新会话标题 */
export async function updateConversationTitle(conversationId, title) {
  const res = await axios.put(`${API_BASE}/ai/conversation/${conversationId}/title`, { title })
  return res.data
}

// ======================== 聊天 ========================

/** 获取聊天历史 */
export async function getChatHistory(username) {
  const res = await axios.get(`${API_BASE}/ai/chat/history`, { params: { username } })
  return res.data
}

/** 删除聊天记录 */
export async function deleteChat(username) {
  const res = await axios.delete(`${API_BASE}/ai/chat/delete`, { params: { username } })
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
 * @returns {AbortController} 用于外部中断流式请求
 */
export function sendChatStream(conversationId, message, files, onChunk, onError, onDone) {
  const controller = new AbortController()

  const fd = new FormData()
  fd.append('conversationId', conversationId)
  fd.append('message', message || '请分析上传的文件')
  if (files && files.length > 0) {
    files.forEach((f) => { if (f.file) fd.append('files', f.file) })
  }

  const headers = { ...getAuthHeaders(), 'Accept': 'text/event-stream' }

  console.log('[ChatStream] 发送流式请求:', { conversationId, message, fileCount: files?.length || 0 })

  fetch(`${API_BASE}/ai/chat/stream`, {
    method: 'POST',
    headers,
    body: fd,
    signal: controller.signal,
  })
    .then(async (response) => {
      console.log('[ChatStream] 响应状态:', response.status)

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
            console.log('[ChatStream] 流式接收完成，共接收', chunkCount, '个chunk')
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
        console.log('[ChatStream] 请求被中止')
      } else {
        console.error('[ChatStream] 网络错误:', err)
        onError(err.message || '网络错误')
      }
    })

  return controller
}