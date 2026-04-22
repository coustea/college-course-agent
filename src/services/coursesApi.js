import request from '@/utils/request'

// Re-export api for backward compatibility with homeCoursesApi.js
export const api = request

// Courses
export const listAllCourses = () => api.get('/api/course/list')
export const getCourses = () => api.get('/api/course/list')
export const deleteCourseById = (courseId) => api.delete('/api/course/delete', { params: { courseId } })

// 课程发布管理
export const publishCourse = (courseId, teacherId) =>
  api.post(`/api/course/${courseId}/publish`, null, { params: { teacherId } })

export const unpublishCourse = (courseId, teacherId) =>
  api.post(`/api/course/${courseId}/unpublish`, null, { params: { teacherId } })

export const getTeacherCourses = (teacherId) =>
  api.get(`/api/course/teacher/${teacherId}/all`)

export const getPublishedCourses = () =>
  api.get('/api/course/published')

export const getCourseStatistics = (teacherId) =>
  api.get('/api/course/stats/all', { params: { teacherId } })

// Videos & Documents
export const listVideos = (courseId) => api.get('/api/course/video/list', { params: { courseId } })
export const listDocuments = (courseId) => api.get('/api/course/document/list', { params: { courseId } })

// Enrollments & Progress
export const listCoursesByStudent = (studentId) => api.get('/api/teacher/enrollments/courses', { params: { studentId } })
export const getCourseProgress = (studentId, courseId) => api.get('/api/progress/course', { params: { studentId, courseId } })

// Students
export const listStudents = () => api.get('/api/teacher/list/students')
export const insertStudent = (payload) => api.post('/api/teacher/insert/students', payload)
export const updateStudent = (id, payload) => api.put('/api/teacher/update/student', payload, { params: { id } })
export const deleteStudentById = (id) => api.delete('/api/teacher/delete/student', { params: { id } })
export const importStudents = (file) => { const form = new FormData(); form.append('file', file); return api.post('/api/teacher/import/students', form) }
