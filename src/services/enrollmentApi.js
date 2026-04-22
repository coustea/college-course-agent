import request from '@/utils/request'

const api = request

// 学生选课
export const enrollCourse = (studentId, courseId) =>
  api.post('/api/enrollment/enroll', { studentId, courseId })

// 学生退课
export const dropCourse = (studentId, courseId) =>
  api.post('/api/enrollment/drop', { studentId, courseId })

// 教师批量添加学生到课程
export const batchEnrollStudents = (courseId, teacherId, studentIds) =>
  api.post('/api/enrollment/batch-enroll', { courseId, teacherId, studentIds })

// 查询学生的已选课程列表
export const getStudentEnrollments = (studentId) =>
  api.get(`/api/enrollment/student/${studentId}`)

// 查询课程的所有选课学生
export const getCourseEnrollments = (courseId) =>
  api.get(`/api/enrollment/course/${courseId}/students`)

// 检查学生是否已选某课程
export const checkEnrollment = (studentId, courseId) =>
  api.get('/api/enrollment/check', { params: { studentId, courseId } })
