import axios from 'axios'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

/**
 * 获取学生统计数据
 * @param {number} studentId - 学生ID
 * @returns {Promise} 返回学生统计数据
 */
export const fetchStudentStatistics = async (studentId) => {
  const response = await axios.get(`${BASE_URL}/progress/student/statistics`, {
    params: { studentId },
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  })
  return response.data.data
}
