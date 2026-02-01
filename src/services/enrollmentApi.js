import axios from 'axios'

const fallbackBase = 'http://localhost:9999/api'

export const api = axios.create({
  baseURL: import.meta?.env?.VITE_API_BASE_URL || fallbackBase,
  timeout: 15000
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token') || localStorage.getItem('userToken')
  if (token) config.headers = { ...(config.headers || {}), Authorization: `Bearer ${token}` }
  return config
})

// 学生选课
export const enrollCourse = (studentId, courseId) =>
  api.post('/enrollment/enroll', { studentId, courseId })

// 学生退课
export const dropCourse = (studentId, courseId) =>
  api.post('/enrollment/drop', { studentId, courseId })

// 教师批量添加学生到课程
export const batchEnrollStudents = (courseId, teacherId, studentIds) =>
  api.post('/enrollment/batch-enroll', { courseId, teacherId, studentIds })

// 查询学生的已选课程列表
export const getStudentEnrollments = (studentId) =>
  api.get(`/enrollment/student/${studentId}`)

// 查询课程的所有选课学生
export const getCourseEnrollments = (courseId) =>
  api.get(`/enrollment/course/${courseId}/students`)

// 检查学生是否已选某课程
export const checkEnrollment = (studentId, courseId) =>
  api.get('/enrollment/check', { params: { studentId, courseId } })
