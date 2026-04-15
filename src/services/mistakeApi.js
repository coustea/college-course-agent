import request from '@/utils/request'

const api = request

// 根据学生ID获取错题列表
export const getWrongQuestionsByStudentId = (studentId, courseId = null) => {
  return api.get('/wrong-question/list', { params: { studentId, courseId } })
}

// 获取错题详情
export const getWrongQuestionDetail = (id) => {
  return api.get('/wrong-question/detail', { params: { id } })
}

// 复习错题
export const reviewWrongQuestion = (id, isCorrect) => {
  return api.post('/wrong-question/review', null, { params: { id, isCorrect } })
}

// 获取错题统计信息
export const getWrongQuestionStats = (studentId, courseId = null) => {
  return api.get('/wrong-question/statistics', { params: { studentId, courseId } })
}

// 标记为已掌握
export const markAsMastered = (id) => {
  return api.post('/wrong-question/mark-mastered', null, { params: { id } })
}

// 取消标记为已掌握
export const cancelMastered = (id) => {
  return api.post('/wrong-question/cancel-mastered', null, { params: { id } })
}

// 删除错题
export const deleteWrongQuestion = (id) => {
  return api.delete('/wrong-question/delete', { params: { id } })
}

// 批量标记为已掌握
export const batchMarkAsMastered = (ids) => {
  return api.post('/wrong-question/batch-mark-mastered', ids)
}

// 批量删除错题
export const batchDeleteWrongQuestions = (ids) => {
  return api.delete('/wrong-question/batch-delete', { data: ids })
}

// 更新错题笔记
export const updateNote = (id, note) => {
  return api.post('/wrong-question/update-note', note, { params: { id } })
}
