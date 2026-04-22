/**
 * 思政资源相关API服务
 */
import request from '@/utils/request'

// ==================== 教师端 - 资源管理 ====================

/**
 * 获取思政资源列表
 */
export function getIdeologyResources(params) {
  return request({
    url: '/api/ideology/resources',
    method: 'get',
    params
  })
}

/**
 * 创建思政资源
 */
export function createIdeologyResource(data) {
  return request({
    url: '/api/ideology/resources',
    method: 'post',
    data
  })
}

/**
 * 更新思政资源
 */
export function updateIdeologyResource(resourceId, data) {
  return request({
    url: `/api/ideology/resources/${resourceId}`,
    method: 'put',
    data
  })
}

/**
 * 删除思政资源
 */
export function deleteIdeologyResource(resourceId) {
  return request({
    url: `/api/ideology/resources/${resourceId}`,
    method: 'delete'
  })
}

/**
 * 获取思政资源详情
 */
export function getIdeologyResourceDetail(resourceId) {
  return request({
    url: `/api/ideology/resources/${resourceId}`,
    method: 'get'
  })
}

/**
 * AI分析思政资源内容
 */
export function analyzeIdeologyContent(title, content) {
  return request({
    url: '/api/ideology/resources/analyze',
    method: 'post',
    data: { title, content }
  })
}

/**
 * 获取资源标签列表
 */
export function getResourceTags(resourceId) {
  return request({
    url: `/api/ideology/resources/${resourceId}/tags`,
    method: 'get'
  })
}

// ==================== 学生端 - 推荐展示 ====================

/**
 * 获取学生思政资源推荐
 */
export function getIdeologyRecommendations(studentId, courseId, limit = 10) {
  return request({
    url: '/api/ideology/resources/recommendations',
    method: 'get',
    params: { studentId, courseId, limit }
  })
}

/**
 * 刷新学生推荐
 */
export function refreshIdeologyRecommendations(studentId, courseId, limit = 10) {
  return request({
    url: '/api/ideology/resources/recommendations/refresh',
    method: 'post',
    params: { studentId, courseId, limit }
  })
}

/**
 * 标记推荐为已点击
 */
export function markRecommendationClicked(recommendationId) {
  return request({
    url: `/api/ideology/resources/recommendations/${recommendationId}/click`,
    method: 'post'
  })
}

// ==================== 教学评价 ====================

/**
 * 计算学生评价
 */
export function evaluateStudent(studentId, courseId, period) {
  return request({
    url: `/api/evaluation/student/${studentId}/course/${courseId}`,
    method: 'post',
    params: { period }
  })
}

/**
 * 获取课程评价汇总
 */
export function getCourseEvaluationSummary(courseId, period) {
  return request({
    url: `/api/evaluation/course/${courseId}/summary`,
    method: 'get',
    params: { period }
  })
}

/**
 * 获取学生评价历史
 */
export function getStudentEvaluationHistory(studentId, courseId) {
  return request({
    url: `/api/evaluation/student/${studentId}/history`,
    method: 'get',
    params: { courseId }
  })
}

/**
 * 获取课程预警学生
 */
export function getCourseWarnings(courseId) {
  return request({
    url: `/api/evaluation/course/${courseId}/warnings`,
    method: 'get'
  })
}

// ==================== 行为分析 ====================

/**
 * 分析学生行为
 */
export function analyzeStudentBehavior(studentId) {
  return request({
    url: `/api/evaluation/behavior/student/${studentId}`,
    method: 'get'
  })
}

/**
 * 获取学生学习路径
 */
export function getLearningPath(studentId, courseId, limit) {
  return request({
    url: `/api/evaluation/behavior/student/${studentId}/path`,
    method: 'get',
    params: { courseId, limit }
  })
}

/**
 * 记录学习行为
 */
export function recordLearningAction(data) {
  return request({
    url: '/api/evaluation/behavior/record',
    method: 'post',
    data
  })
}

/**
 * 获取学生行为画像
 */
export function getStudentProfile(studentId) {
  return request({
    url: `/api/evaluation/behavior/student/${studentId}/profile`,
    method: 'get'
  })
}

/**
 * 刷新学生行为画像
 */
export function refreshStudentProfile(studentId) {
  return request({
    url: `/api/evaluation/behavior/student/${studentId}/refresh`,
    method: 'post'
  })
}

// ==================== 情感分析 ====================

/**
 * 分析文本情感
 */
export function analyzeSentiment(text) {
  return request({
    url: '/api/analysis/sentiment',
    method: 'post',
    data: { text }
  })
}

/**
 * 获取学生情感趋势
 */
export function getStudentSentimentTrend(studentId, courseId, days = 30) {
  return request({
    url: `/api/analysis/sentiment/student/${studentId}`,
    method: 'get',
    params: { courseId, days }
  })
}

/**
 * 获取课程情感分布
 */
export function getCourseSentimentDistribution(courseId) {
  return request({
    url: `/api/analysis/sentiment/course/${courseId}/distribution`,
    method: 'get'
  })
}

/**
 * 分析思政情感
 */
export function analyzeIdeologySentiment(text, theme) {
  return request({
    url: '/api/analysis/sentiment/ideology',
    method: 'post',
    data: { text, theme }
  })
}

// ==================== 价值认同评估 ====================

/**
 * 评估学生价值认同
 */
export function assessStudentValue(studentId, courseId, period) {
  return request({
    url: `/api/analysis/value/student/${studentId}`,
    method: 'post',
    params: { courseId, period }
  })
}

/**
 * 获取学生价值认同趋势
 */
export function getValueTrend(studentId, courseId, months = 6) {
  return request({
    url: `/api/analysis/value/student/${studentId}/trend`,
    method: 'get',
    params: { courseId, months }
  })
}

/**
 * 批量评估课程学生
 */
export function batchAssessCourseStudents(courseId, period) {
  return request({
    url: `/api/analysis/value/course/${courseId}/batch`,
    method: 'post',
    params: { period }
  })
}

/**
 * 获取课程价值认同汇总
 */
export function getCourseValueSummary(courseId) {
  return request({
    url: `/api/analysis/value/course/${courseId}/summary`,
    method: 'get'
  })
}

/**
 * 预测价值认同趋势
 */
export function predictValueTrend(studentId, courseId) {
  return request({
    url: `/api/analysis/value/student/${studentId}/predict`,
    method: 'get',
    params: { courseId }
  })
}

// ==================== 教师授课风格 ====================

/**
 * 获取教师授课风格画像
 */
export function getTeacherStyleProfile(teacherId) {
  return request({
    url: `/api/analysis/style/teacher/${teacherId}`,
    method: 'get'
  })
}

/**
 * 分析教师授课风格
 */
export function analyzeTeachingStyle(teacherId) {
  return request({
    url: `/api/analysis/style/teacher/${teacherId}/analyze`,
    method: 'post'
  })
}