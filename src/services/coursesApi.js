import request from '@/utils/request'

// Re-export api for backward compatibility with homeCoursesApi.js
export const api = request

// Courses
export const listAllCourses = () => api.get('/course/list')
export const deleteCourseById = (courseId) => api.delete('/course/delete', { params: { courseId } })

// 课程发布管理
export const publishCourse = (courseId, teacherId) =>
  api.post(`/course/${courseId}/publish`, null, { params: { teacherId } })

export const unpublishCourse = (courseId, teacherId) =>
  api.post(`/course/${courseId}/unpublish`, null, { params: { teacherId } })

export const getTeacherCourses = (teacherId) =>
  api.get(`/course/teacher/${teacherId}/all`)

export const getPublishedCourses = () =>
  api.get('/course/published')

export const getCourseStatistics = (teacherId) =>
  api.get('/course/stats/all', { params: { teacherId } })

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
