import axios from 'axios'

const fallbackBase = (() => {
  try { if (window?.location?.port === '4173') return 'http://localhost:9999/api' } catch {}
  return '/api'
})()

export const api = axios.create({ baseURL: import.meta?.env?.VITE_API_BASE_URL || fallbackBase, timeout: 15000 })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token') || localStorage.getItem('userToken')
  if (token) config.headers = { ...(config.headers || {}), Authorization: `Bearer ${token}` }
  return config
})

// Courses
export const listAllCourses = () => api.get('/course/list')
export const deleteCourseById = (courseId) => api.delete('/course/delete', { params: { courseId } })

// Videos & Documents
export const listVideos = (courseId) => api.get('/course/video/list', { params: { courseId } })
export const listDocuments = (courseId) => api.get('/course/document/list', { params: { courseId } })

// Enrollments & Progress
export const listCoursesByStudent = (studentId) => api.get('/teacher/enrollments/courses', { params: { studentId } })
export const getCourseProgress = (studentId, courseId) => api.get('/progress/course', { params: { studentId, courseId } })

// Students
export const listStudents = () => api.get('/teacher/list/students')
export const insertStudent = (payload) => api.post('/teacher/insert/students', payload)
export const updateStudent = (id, payload) => api.put('/teacher/update/student', payload, { params: { id } })
export const deleteStudentById = (id) => api.delete('/teacher/delete/student', { params: { id } })
export const importStudents = (file) => { const form = new FormData(); form.append('file', file); return api.post('/teacher/import/students', form) }


