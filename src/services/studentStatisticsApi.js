import request from '@/utils/request'

/**
 * 获取学生统计数据
 * @param {number} studentId - 学生ID
 * @returns {Promise} 返回学生统计数据
 */
export const fetchStudentStatistics = async (studentId) => {
  const response = await request.get('/progress/student/statistics', {
    params: { studentId }
  })
  return response.data.data
}
