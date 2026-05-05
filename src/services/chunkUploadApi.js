import axios from 'axios'

const normalizeBaseURL = (value) => {
  const baseURL = value || ''
  return baseURL.replace(/\/api\/?$/, '')
}

const chunkRequest = axios.create({
  baseURL: normalizeBaseURL(import.meta.env.VITE_API_BASE_URL),
  timeout: 60000
})

chunkRequest.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export const initChunkUpload = ({ fileName, fileSize, totalChunks }) =>
  chunkRequest.post('/api/chunk/init', null, {
    params: { fileName, fileSize, totalChunks }
  })

export const uploadChunkPart = ({ uploadId, chunkIndex, chunk }) => {
  const formData = new FormData()
  formData.append('uploadId', uploadId)
  formData.append('chunkIndex', chunkIndex)
  formData.append('chunk', chunk)

  return chunkRequest.post('/api/chunk/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const mergeChunkUpload = ({ uploadId, courseId, videoTitle, duration }) =>
  chunkRequest.post('/api/chunk/merge', null, {
    params: { uploadId, courseId, videoTitle, duration }
  })

export const cancelChunkUpload = (uploadId) =>
  chunkRequest.delete('/api/chunk/cancel', {
    params: { uploadId }
  })
