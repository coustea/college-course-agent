<template>
  <div class="ideology-dashboard">
    <el-row :gutter="20">
      <!-- 课程选择 -->
      <el-col :span="24">
        <el-card class="filter-card">
          <el-select v-model="selectedCourseId" placeholder="选择课程" @change="loadData" style="width: 300px">
            <el-option v-for="course in courses" :key="course.courseId" :label="course.courseName" :value="course.courseId" />
          </el-select>
        </el-card>
      </el-col>
    </el-row>

    <!-- 核心指标卡片 -->
    <el-row :gutter="20" class="metric-cards">
      <el-col :xs="12" :sm="6">
        <el-card class="metric-card">
          <div class="metric-icon" style="background: #409eff;">
            <el-icon><User /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-value">{{ evaluationSummary?.totalStudents || 0 }}</div>
            <div class="metric-label">参与学生数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="metric-card">
          <div class="metric-icon" style="background: #67c23a;">
            <el-icon><TrendCharts /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-value">{{ (evaluationSummary?.overallScore || 0).toFixed(1) }}</div>
            <div class="metric-label">平均评分</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="metric-card">
          <div class="metric-icon" style="background: #e6a23c;">
            <el-icon><CollectionTag /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-value">{{ valueSummary?.avgTotalScore?.toFixed(1) || 0 }}</div>
            <div class="metric-label">价值认同度</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="metric-card">
          <div class="metric-icon" style="background: #f56c6c;">
            <el-icon><Warning /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-value">{{ warnings?.length || 0 }}</div>
            <div class="metric-label">预警学生</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20">
      <!-- 教学评价雷达图 -->
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <span>教学评价维度分析</span>
          </template>
          <div ref="radarChartRef" class="chart-container"></div>
        </el-card>
      </el-col>

      <!-- 评分分布饼图 -->
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <span>学生评价分布</span>
          </template>
          <div ref="pieChartRef" class="chart-container"></div>
        </el-card>
      </el-col>

      <!-- 情感分析趋势 -->
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <span>情感分析趋势</span>
          </template>
          <div ref="sentimentChartRef" class="chart-container"></div>
        </el-card>
      </el-col>

      <!-- 价值认同维度 -->
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <span>价值认同维度得分</span>
          </template>
          <div ref="valueChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 预警学生列表 -->
    <el-card class="warning-card" v-if="warnings && warnings.length > 0">
      <template #header>
        <span style="color: #f56c6c;">⚠️ 预警学生列表</span>
      </template>
      <el-table :data="warnings" stripe>
        <el-table-column prop="studentName" label="学生姓名" width="120" />
        <el-table-column prop="warningType" label="预警类型" width="150" />
        <el-table-column prop="currentScore" label="当前得分" width="100">
          <template #default="{ row }">
            <el-tag :type="row.currentScore < 60 ? 'danger' : 'warning'">{{ row.currentScore?.toFixed(1) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="suggestion" label="建议" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import { User, TrendCharts, CollectionTag, Warning } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  getCourseEvaluationSummary,
  getCourseWarnings,
  getCourseSentimentDistribution,
  getCourseValueSummary
} from '@/services/ideology'
import { getCourses } from '@/services/coursesApi'

const selectedCourseId = ref(null)
const courses = ref([])
const evaluationSummary = ref(null)
const valueSummary = ref(null)
const warnings = ref([])
const sentimentDistribution = ref(null)

// 图表引用
const radarChartRef = ref(null)
const pieChartRef = ref(null)
const sentimentChartRef = ref(null)
const valueChartRef = ref(null)

let radarChart = null
let pieChart = null
let sentimentChart = null
let valueChart = null

onMounted(() => {
  loadCourses()
  initCharts()
})

const loadCourses = async () => {
  try {
    const res = await getCourses()
    if (res.code === 200) {
      courses.value = res.data || []
      if (courses.value.length > 0) {
        selectedCourseId.value = courses.value[0].courseId
        loadData()
      }
    }
  } catch (e) {
    console.error('加载课程失败', e)
  }
}

const loadData = async () => {
  if (!selectedCourseId.value) return

  // 并行加载数据
  const [evalRes, warnRes, valueRes, sentimentRes] = await Promise.all([
    getCourseEvaluationSummary(selectedCourseId.value, 'monthly'),
    getCourseWarnings(selectedCourseId.value),
    getCourseValueSummary(selectedCourseId.value),
    getCourseSentimentDistribution(selectedCourseId.value)
  ])

  if (evalRes.code === 200) evaluationSummary.value = evalRes.data
  if (warnRes.code === 200) warnings.value = warnRes.data
  if (valueRes.code === 200) valueSummary.value = valueRes.data
  if (sentimentRes.code === 200) sentimentDistribution.value = sentimentRes.data

  nextTick(() => {
    updateCharts()
  })
}

const initCharts = () => {
  nextTick(() => {
    if (radarChartRef.value) radarChart = echarts.init(radarChartRef.value)
    if (pieChartRef.value) pieChart = echarts.init(pieChartRef.value)
    if (sentimentChartRef.value) sentimentChart = echarts.init(sentimentChartRef.value)
    if (valueChartRef.value) valueChart = echarts.init(valueChartRef.value)
  })
}

const updateCharts = () => {
  // 雷达图
  if (radarChart && evaluationSummary.value) {
    radarChart.setOption({
      tooltip: {},
      radar: {
        indicator: [
          { name: '内容融入度', max: 100 },
          { name: '师生互动', max: 100 },
          { name: '学生参与度', max: 100 },
          { name: '价值认同', max: 100 }
        ]
      },
      series: [{
        type: 'radar',
        data: [{
          value: [
            evaluationSummary.value.contentIntegrationAvg || 0,
            evaluationSummary.value.interactionAvg || 0,
            evaluationSummary.value.participationAvg || 0,
            evaluationSummary.value.valueRecognitionAvg || 0
          ],
          name: '课程评价',
          areaStyle: { color: 'rgba(64, 158, 255, 0.3)' }
        }]
      }]
    })
  }

  // 饼图
  if (pieChart && evaluationSummary.value?.scoreDistribution) {
    const dist = evaluationSummary.value.scoreDistribution
    pieChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        data: [
          { value: dist['优秀'] || 0, name: '优秀', itemStyle: { color: '#67c23a' } },
          { value: dist['良好'] || 0, name: '良好', itemStyle: { color: '#409eff' } },
          { value: dist['中等'] || 0, name: '中等', itemStyle: { color: '#e6a23c' } },
          { value: dist['待提升'] || 0, name: '待提升', itemStyle: { color: '#f56c6c' } }
        ]
      }]
    })
  }

  // 情感分析图
  if (sentimentChart && sentimentDistribution.value) {
    sentimentChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: ['积极', '中性', '消极'] },
      yAxis: { type: 'value', max: 1, axisLabel: { formatter: '{%}' } },
      series: [{
        type: 'bar',
        data: [
          { value: (sentimentDistribution.value.positiveRatio * 100).toFixed(1), itemStyle: { color: '#67c23a' } },
          { value: (sentimentDistribution.value.neutralRatio * 100).toFixed(1), itemStyle: { color: '#909399' } },
          { value: (sentimentDistribution.value.negativeRatio * 100).toFixed(1), itemStyle: { color: '#f56c6c' } }
        ],
        label: { show: true, position: 'top', formatter: '{c}%' }
      }]
    })
  }

  // 价值认同柱状图
  if (valueChart && valueSummary.value) {
    valueChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: ['爱国认同', '社会责任', '职业道德', '创新精神', '文化自信'],
        axisLabel: { interval: 0, rotate: 30 }
      },
      yAxis: { type: 'value', max: 100 },
      series: [{
        type: 'bar',
        data: [
          valueSummary.value.avgPatriotismScore || 0,
          valueSummary.value.avgSocialResponsibilityScore || 0,
          valueSummary.value.avgProfessionalEthicsScore || 0,
          valueSummary.value.avgInnovationScore || 0,
          valueSummary.value.avgCulturalConfidenceScore || 0
        ],
        itemStyle: { color: '#409eff' }
      }]
    })
  }
}
</script>

<style scoped>
.ideology-dashboard {
  padding: 20px;
}

.filter-card {
  margin-bottom: 20px;
}

.metric-cards {
  margin-bottom: 20px;
}

.metric-card {
  display: flex;
  align-items: center;
  padding: 20px;
}

.metric-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 20px;
}

.metric-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: white;
  margin-right: 16px;
}

.metric-content {
  flex: 1;
}

.metric-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.metric-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.chart-card {
  margin-bottom: 20px;
}

.chart-container {
  height: 300px;
}

.warning-card {
  margin-top: 20px;
}
</style>
