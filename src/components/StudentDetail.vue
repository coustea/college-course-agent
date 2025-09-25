<template>
  <div class="student-detail" v-if="student">
    <!-- 基本信息 -->
    <el-card class="info-card">
      <template #header>
        <div class="card-header">
          <span>基本信息</span>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="学号">{{ student.studentId }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ student.name }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ student.className }}</el-descriptions-item>
        <el-descriptions-item label="课程">{{ student.courseName }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ student.email || '暂无' }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ student.phone || '暂无' }}</el-descriptions-item>
        <el-descriptions-item label="最后登录">{{ student.lastLogin }}</el-descriptions-item>
        <el-descriptions-item label="学习状态">
          <el-tag :type="getStatusType(student)">
            {{ getStatusText(student) }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 学习统计 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value">{{ student.studyTime }}h</div>
            <div class="stat-label">总学习时长</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value">{{ student.completionRate }}%</div>
            <div class="stat-label">课程完成率</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value">{{ student.avgScore }}</div>
            <div class="stat-label">平均成绩</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value">{{ getPerformanceText(student.performanceLevel) }}</div>
            <div class="stat-label">表现等级</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 学习趋势 -->
    <el-card class="trend-card">
      <template #header>
        <div class="card-header">
          <span>学习趋势分析</span>
        </div>
      </template>
      <div ref="trendChart" style="height: 300px;"></div>
    </el-card>
  </div>
  <div v-else class="loading-container">
    <el-empty description="学生信息加载中..." />
  </div>
</template>

<script>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

export default {
  name: 'StudentDetail',
  props: {
    student: Object
  },
  setup(props) {
    const trendChart = ref(null)
    let trendChartInstance = null

    // 获取状态类型和文本
    const getStatusType = (student) => {
      if (student.avgScore >= 90 && student.completionRate >= 90) return 'success'
      if (student.avgScore < 70 || student.completionRate < 60) return 'danger'
      return 'warning'
    }

    const getStatusText = (student) => {
      if (student.avgScore >= 90 && student.completionRate >= 90) return '优秀'
      if (student.avgScore < 70 || student.completionRate < 60) return '需关注'
      return '良好'
    }

    const getPerformanceText = (level) => {
      const texts = {
        excellent: '优秀',
        good: '良好',
        average: '一般',
        concern: '需关注'
      }
      return texts[level] || '未知'
    }

    // 初始化趋势图表
    const initTrendChart = () => {
      if (!trendChart.value) return

      trendChartInstance = echarts.init(trendChart.value)

      const option = {
        tooltip: {
          trigger: 'axis'
        },
        legend: {
          data: ['成绩趋势', '学习时长']
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: ['第1周', '第2周', '第3周', '第4周', '第5周', '第6周', '第7周']
        },
        yAxis: [
          {
            type: 'value',
            name: '成绩',
            max: 100
          },
          {
            type: 'value',
            name: '时长(h)'
          }
        ],
        series: [
          {
            name: '成绩趋势',
            type: 'line',
            yAxisIndex: 0,
            data: [65, 70, 75, 80, 82, 85, 88],
            smooth: true,
            lineStyle: {
              width: 3,
              color: '#5470c6'
            }
          },
          {
            name: '学习时长',
            type: 'line',
            yAxisIndex: 1,
            data: [8, 10, 12, 14, 13, 15, 16],
            smooth: true,
            lineStyle: {
              width: 3,
              color: '#91cc75'
            }
          }
        ]
      }

      trendChartInstance.setOption(option)
    }

    onMounted(() => {
      if (props.student) {
        nextTick(() => {
          setTimeout(() => {
            initTrendChart()
          }, 300)
        })
      }
    })

    return {
      trendChart,
      getStatusType,
      getStatusText,
      getPerformanceText
    }
  }
}
</script>

<style scoped>

.info-card {
  margin-bottom: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
  padding: 20px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 8px;
}

.stat-label {
  color: #909399;
  font-size: 14px;
}

.trend-card {
  margin-bottom: 20px;
}

.card-header {
  font-weight: 600;
  font-size: 16px;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}
</style>