<template>
  <div class="learning-analytics-dashboard">
    <!-- 顶部统计卡片 -->
    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon primary">
          <i class="fas fa-book-open"></i>
        </div>
        <div class="stat-content">
          <h3>{{ analytics.totalCourses }}</h3>
          <p>已学课程</p>
          <span class="stat-trend" :class="{ up: analytics.courseTrend > 0 }">
            {{ analytics.courseTrend > 0 ? '+' : '' }}{{ analytics.courseTrend }}
          </span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon success">
          <i class="fas fa-clock"></i>
        </div>
        <div class="stat-content">
          <h3>{{ analytics.totalHours }}h</h3>
          <p>学习时长</p>
          <span class="stat-trend up">
            +{{ analytics.hoursTrend }}%
          </span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon warning">
          <i class="fas fa-chart-line"></i>
        </div>
        <div class="stat-content">
          <h3>{{ analytics.avgScore }}%</h3>
          <p>平均完成度</p>
          <span class="stat-trend up">
            +{{ analytics.completionTrend }}%
          </span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon danger">
          <i class="fas fa-trophy"></i>
        </div>
        <div class="stat-content">
          <h3>{{ analytics.rank }}</h3>
          <p>班级排名</p>
          <span class="stat-trend">
            {{ analytics.rankTrend > 0 ? '↑' : '↓' }}
          </span>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-section">
      <!-- 学习趋势图 -->
      <div class="chart-card">
        <div class="chart-header">
          <h4>学习趋势</h4>
          <el-radio-group v-model="trendPeriod" size="small" @change="updateTrendChart">
            <el-radio-button label="week">周</el-radio-button>
            <el-radio-button label="month">月</el-radio-button>
            <el-radio-button label="semester">学期</el-radio-button>
          </el-radio-group>
        </div>
        <div ref="trendChart" class="chart-container"></div>
      </div>

      <!-- 课程完成度 -->
      <div class="chart-card">
        <div class="chart-header">
          <h4>课程完成度</h4>
        </div>
        <div ref="completionChart" class="chart-container"></div>
      </div>

      <!-- 学习时长分布 -->
      <div class="chart-card">
        <div class="chart-header">
          <h4>每日学习时长</h4>
        </div>
        <div ref="timeChart" class="chart-container"></div>
      </div>

      <!-- 知识点雷达图 -->
      <div class="chart-card">
        <div class="chart-header">
          <h4>能力雷达图</h4>
        </div>
        <div ref="radarChart" class="chart-container"></div>
      </div>
    </div>

    <!-- 学习轨迹时间线 -->
    <div class="timeline-section">
      <div class="section-header">
        <h3>学习轨迹</h3>
        <el-button type="primary" size="small" @click="exportReport">导出报告</el-button>
      </div>
      <el-timeline>
        <el-timeline-item
          v-for="event in learningEvents"
          :key="event.id"
          :timestamp="event.timestamp"
          :type="event.type"
        >
          <div class="event-content">
            <h4>{{ event.title }}</h4>
            <p>{{ event.description }}</p>
            <el-tag v-if="event.score" size="small" :type="event.score >= 80 ? 'success' : 'warning'">
              得分: {{ event.score }}
            </el-tag>
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import axios from 'axios'

const analytics = ref({
  totalCourses: 0,
  totalHours: 0,
  avgScore: 0,
  rank: 0,
  courseTrend: 0,
  hoursTrend: 0,
  completionTrend: 0,
  rankTrend: 0
})

const trendPeriod = ref('week')
const learningEvents = ref([])

let trendChartInstance = null
let completionChartInstance = null
let timeChartInstance = null
let radarChartInstance = null

const trendChart = ref(null)
const completionChart = ref(null)
const timeChart = ref(null)
const radarChart = ref(null)

// 加载分析数据
const loadAnalytics = async () => {
  try {
    const token = localStorage.getItem('token')
    const userId = localStorage.getItem('userId')

    // 获取基础统计数据
    const [progressRes, eventsRes] = await Promise.all([
      axios.get('/api/progress/course/all', {
        params: { studentId: userId },
        headers: { Authorization: `Bearer ${token}` }
      }),
      axios.get('/api/progress/learning-events', {
        params: { studentId: userId, limit: 10 },
        headers: { Authorization: `Bearer ${token}` }
      })
    ])

    if (progressRes.data.code === 200) {
      const courses = progressRes.data.data?.courses || []
      analytics.value.totalCourses = courses.length
      analytics.value.avgScore = courses.length > 0
        ? Math.round(courses.reduce((sum, c) => sum + (c.percentage || 0), 0) / courses.length)
        : 0

      // 计算总学习时长（假设从进度数据中获取）
      analytics.value.totalHours = 0 // 需要从实际数据计算
    }

    if (eventsRes.data.code === 200) {
      learningEvents.value = eventsRes.data.data || []
    }

    // 初始化图表
    await nextTick()
    initCharts()
  } catch (error) {
    console.error('加载分析数据失败:', error)
  }
}

// 初始化图表
const initCharts = () => {
  initTrendChart()
  initCompletionChart()
  initTimeChart()
  initRadarChart()
}

// 学习趋势图
const initTrendChart = () => {
  if (!trendChart.value) return

  trendChartInstance = echarts.init(trendChart.value)

  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['学习时长', '完成课程'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: [
      { type: 'value', name: '时长(h)', position: 'left' },
      { type: 'value', name: '课程数', position: 'right' }
    ],
    series: [
      {
        name: '学习时长',
        type: 'bar',
        data: [2, 3, 2.5, 4, 3, 5, 4],
        itemStyle: { color: '#409EFF' }
      },
      {
        name: '完成课程',
        type: 'line',
        yAxisIndex: 1,
        data: [0, 1, 0, 2, 1, 3, 2],
        itemStyle: { color: '#67C23A' }
      }
    ]
  }

  trendChartInstance.setOption(option)
}

// 课程完成度
const initCompletionChart = () => {
  if (!completionChart.value) return

  completionChartInstance = echarts.init(completionChart.value)

  const option = {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data: [
        { value: 3, name: '已完成' },
        { value: 2, name: '进行中' },
        { value: 5, name: '未开始' }
      ],
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2
      }
    }]
  }

  completionChartInstance.setOption(option)
}

// 每日学习时长
const initTimeChart = () => {
  if (!timeChart.value) return

  timeChartInstance = echarts.init(timeChart.value)

  const option = {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: ['0-2h', '2-4h', '4-6h', '6-8h', '8h+']
    },
    yAxis: { type: 'value' },
    series: [{
      type: 'line',
      data: [5, 12, 8, 4, 2],
      smooth: true,
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
          { offset: 1, color: 'rgba(64, 158, 255, 0.1)' }
        ])
      }
    }]
  }

  timeChartInstance.setOption(option)
}

// 能力雷达图
const initRadarChart = () => {
  if (!radarChart.value) return

  radarChartInstance = echarts.init(radarChart.value)

  const option = {
    tooltip: {},
    radar: {
      indicator: [
        { name: '理论基础', max: 100 },
        { name: '实践能力', max: 100 },
        { name: '创新能力', max: 100 },
        { name: '团队协作', max: 100 },
        { name: '学习效率', max: 100 }
      ]
    },
    series: [{
      type: 'radar',
      data: [{
        value: [85, 75, 70, 90, 80],
        name: '能力评估',
        areaStyle: { color: 'rgba(103, 194, 58, 0.3)' }
      }]
    }]
  }

  radarChartInstance.setOption(option)
}

// 更新趋势图
const updateTrendChart = () => {
  // 根据选择的时期更新图表数据
  console.log('更新趋势图:', trendPeriod.value)
}

// 导出报告
const exportReport = () => {
  ElMessage.success('报告导出功能开发中...')
}

onMounted(() => {
  loadAnalytics()

  // 响应式调整
  window.addEventListener('resize', () => {
    trendChartInstance?.resize()
    completionChartInstance?.resize()
    timeChartInstance?.resize()
    radarChartInstance?.resize()
  })
})

onBeforeUnmount(() => {
  trendChartInstance?.dispose()
  completionChartInstance?.dispose()
  timeChartInstance?.dispose()
  radarChartInstance?.dispose()
})
</script>

<style scoped>
.learning-analytics-dashboard {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.stat-icon.primary { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.stat-icon.success { background: linear-gradient(135deg, #84fab0 0%, #8fd3f4 100%); }
.stat-icon.warning { background: linear-gradient(135deg, #fccb90 0%, #d57eeb 100%); }
.stat-icon.danger { background: linear-gradient(135deg, #e0c3fc 0%, #8ec5fc 100%); }

.stat-content h3 {
  margin: 0 0 4px;
  font-size: 28px;
  font-weight: 700;
  color: #2c3e50;
}

.stat-content p {
  margin: 0 0 8px;
  color: #6c757d;
  font-size: 14px;
}

.stat-trend {
  font-size: 12px;
  font-weight: 600;
  color: #dc3545;
}

.stat-trend.up {
  color: #28a745;
}

.charts-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.chart-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.chart-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

.chart-container {
  height: 300px;
}

.timeline-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
}

.event-content h4 {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
}

.event-content p {
  margin: 0 0 8px;
  color: #6c757d;
  font-size: 14px;
}

@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: 1fr;
  }

  .charts-section {
    grid-template-columns: 1fr;
  }
}
</style>
