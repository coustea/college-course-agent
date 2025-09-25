<template>
  <div class="analytics-container">
    <div class="page-header">
      <h2>数据分析中心</h2>
      <div class="header-actions">
        <el-select v-model="timeRange" placeholder="选择时间范围" @change="fetchData" style="width: 120px; margin-right: 10px;">
          <el-option label="最近7天" value="7"></el-option>
          <el-option label="最近30天" value="30"></el-option>
          <el-option label="本学期" value="term"></el-option>
          <el-option label="自定义" value="custom"></el-option>
        </el-select>
        <el-date-picker
          v-if="timeRange === 'custom'"
          v-model="customDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          @change="fetchData"
          style="width: 240px; margin-right: 10px;"
        />
        <el-select v-model="selectedClass" placeholder="选择班级" @change="fetchData" style="width: 120px; margin-right: 10px;">
          <el-option label="全部班级" value="all"></el-option>
          <el-option v-for="classItem in classes" :key="classItem.id" :label="classItem.name" :value="classItem.id"></el-option>
        </el-select>
        <el-button type="primary" :icon="Download" @click="exportData">导出数据</el-button>
      </div>
    </div>

    <!-- 关键指标卡片 -->
    <div class="metrics-cards">
      <el-card class="metric-card">
        <div class="metric-content">
          <div class="metric-icon students">
            <i class="el-icon-user-solid"></i>
          </div>
          <div class="metric-info">
            <h3>{{ metrics.totalStudents }}</h3>
            <p>学生总数</p>
            <span class="metric-trend" :class="metrics.studentTrend >= 0 ? 'up' : 'down'">
              {{ metrics.studentTrend >= 0 ? '+' : '' }}{{ metrics.studentTrend }}%
            </span>
          </div>
        </div>
      </el-card>

      <el-card class="metric-card">
        <div class="metric-content">
          <div class="metric-icon courses">
            <i class="el-icon-notebook-2"></i>
          </div>
          <div class="metric-info">
            <h3>{{ metrics.totalCourses }}</h3>
            <p>已发布课程数量</p>
            <span class="metric-trend" :class="metrics.courseTrend >= 0 ? 'up' : 'down'">
              {{ metrics.courseTrend >= 0 ? '+' : '' }}{{ metrics.courseTrend }}%
            </span>
          </div>
        </div>
      </el-card>

      <el-card class="metric-card">
        <div class="metric-content">
          <div class="metric-icon assignments">
            <i class="el-icon-finished"></i>
          </div>
          <div class="metric-info">
            <h3>{{ metrics.avgCompletion }}%</h3>
            <p>课程学习完成率</p>
            <span class="metric-trend" :class="metrics.completionTrend >= 0 ? 'up' : 'down'">
              {{ metrics.completionTrend >= 0 ? '+' : '' }}{{ metrics.completionTrend }}%
            </span>
          </div>
        </div>
      </el-card>

      <el-card class="metric-card">
        <div class="metric-content">
          <div class="metric-icon rating">
            <i class="el-icon-star-on"></i>
          </div>
          <div class="metric-info">
            <h3>{{ metrics.avgScore }}</h3>
            <p>平均测验成绩</p>
            <span class="metric-trend" :class="metrics.scoreTrend >= 0 ? 'up' : 'down'">
              {{ metrics.scoreTrend >= 0 ? '+' : '' }}{{ metrics.scoreTrend }}%
            </span>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 图表区域 -->
    <div class="charts-section">
      <!-- 学生成绩分布 -->
      <el-card class="chart-card">
        <template #header>
          <div class="chart-header">
            <h3>测验成绩分布</h3>
            <el-select v-model="scoreCourse" placeholder="选择课程" size="small" @change="updateScoreChart" style="width: 150px;">
              <el-option
                v-for="course in courses"
                :key="course.id"
                :label="course.name"
                :value="course.id"
              />
            </el-select>
          </div>
        </template>
        <div class="chart-container">
          <div ref="scoreChartRef" style="width: 100%; height: 300px;"></div>
        </div>
      </el-card>

      <!-- 课程完整观看量 -->
      <el-card class="chart-card">
        <template #header>
          <div class="chart-header">
            <h3>教学视频完整观看量</h3>
            <el-select v-model="assignmentCourse" placeholder="选择课程" size="small" @change="updateAssignmentChart" style="width: 150px;">
              <el-option
                v-for="course in courses"
                :key="course.id"
                :label="course.name"
                :value="course.id"
              />
            </el-select>
          </div>
        </template>
        <div class="chart-container">
          <div ref="assignmentChartRef" style="width: 100%; height: 300px;"></div>
        </div>
      </el-card>

      <!-- 课程访问量 -->
      <el-card class="chart-card">
        <template #header>
          <div class="chart-header">
            <h3>教学视频访问量</h3>
            <el-select v-model="accessClass" placeholder="选择班级" size="small" @change="updateCourseAccessChart" style="width: 150px;">
              <el-option label="全部班级" value="all"></el-option>
              <el-option v-for="classItem in classes" :key="classItem.id" :label="classItem.name" :value="classItem.id"></el-option>
            </el-select>
          </div>
        </template>
        <div class="chart-container">
          <div ref="courseAccessChartRef" style="width: 100%; height: 300px;"></div>
        </div>
      </el-card>

      <!-- 学生活跃度 -->
      <el-card class="chart-card">
        <template #header>
          <div class="chart-header">
            <h3>学生活跃度分析</h3>
            <el-select v-model="activityClass" placeholder="选择班级" size="small" @change="updateActivityChart" style="width: 150px;">
              <el-option label="全部班级" value="all"></el-option>
              <el-option v-for="classItem in classes" :key="classItem.id" :label="classItem.name" :value="classItem.id"></el-option>
            </el-select>
          </div>
        </template>
        <div class="chart-container">
          <div ref="activityChartRef" style="width: 100%; height: 300px;"></div>
        </div>
      </el-card>
    </div>

    <!-- 详细数据表格 -->
    <el-card class="data-table-card">
      <template #header>
        <div class="table-header">
          <h3>各班课程详细数据</h3>
        </div>
      </template>
      <el-table :data="courseData" v-loading="tableLoading" style="width: 100%">
        <el-table-column prop="className" label="班级" min-width="120" />
        <el-table-column prop="courseName" label="教学视频名称" min-width="150" />
        <el-table-column prop="studentCount" label="学生数" width="100" />
        <el-table-column prop="assignmentCount" label="作品数" width="100" />
        <el-table-column prop="avgScore" label="平均分" width="100">
          <template #default="scope">
            {{ scope.row.avgScore.toFixed(1) }}
          </template>
        </el-table-column>
        <el-table-column prop="completionRate" label="完成率" width="100">
          <template #default="scope">
            {{ (scope.row.completionRate * 100).toFixed(1) }}%
          </template>
        </el-table-column>
        <el-table-column prop="accessCount" label="访问量" width="100" />
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button link type="primary" @click="viewCourseDetails(scope.row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 课程详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="selectedCourse ? selectedCourse.courseName + ' 详情' : '课程详情'"
      width="80%"
    >
      <div v-if="selectedCourse" class="course-details">
        <el-descriptions title="基本信息" :column="2" border>
          <el-descriptions-item label="班级">{{ selectedCourse.className }}</el-descriptions-item>
          <el-descriptions-item label="课程名称">{{ selectedCourse.courseName }}</el-descriptions-item>
          <el-descriptions-item label="学生人数">{{ selectedCourse.studentCount }}</el-descriptions-item>
          <el-descriptions-item label="作业数量">{{ selectedCourse.assignmentCount }}</el-descriptions-item>
          <el-descriptions-item label="平均成绩">{{ selectedCourse.avgScore.toFixed(1) }}</el-descriptions-item>
          <el-descriptions-item label="完成率">{{ (selectedCourse.completionRate * 100).toFixed(1) }}%</el-descriptions-item>
          <el-descriptions-item label="课程访问量">{{ selectedCourse.accessCount }}</el-descriptions-item>
        </el-descriptions>

        <h4 style="margin-top: 20px;">学生成绩分布</h4>
        <div ref="detailChartRef" style="width: 100%; height: 300px; margin-top: 10px;"></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage, ElLoading } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

// 模拟axios - 实际使用时替换为真实axios
const mockAxios = {
  get: (url) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        // 模拟API响应
        if (url.includes('/metrics')) {
          resolve({
            data: {
              totalStudents: 156,
              totalCourses: 5,
              avgCompletion: 87,
              avgScore: 82.5,
              studentTrend: 5,
              courseTrend: 0,
              completionTrend: 3,
              scoreTrend: -2
            }
          })
        } else if (url.includes('/course-data')) {
          resolve({
            data: {
              items: [
                {
                  id: 1,
                  className: '计算机1班',
                  courseName: 'Web前端开发',
                  studentCount: 42,
                  assignmentCount: 8,
                  avgScore: 85.2,
                  completionRate: 0.89,
                  accessCount: 1245
                },
                {
                  id: 2,
                  className: '计算机2班',
                  courseName: '数据库原理',
                  studentCount: 38,
                  assignmentCount: 6,
                  avgScore: 79.8,
                  completionRate: 0.82,
                  accessCount: 987
                },
                {
                  id: 3,
                  className: '软件工程1班',
                  courseName: '操作系统',
                  studentCount: 35,
                  assignmentCount: 7,
                  avgScore: 81.5,
                  completionRate: 0.85,
                  accessCount: 1123
                },
                {
                  id: 4,
                  className: '软件工程2班',
                  courseName: '计算机网络',
                  studentCount: 41,
                  assignmentCount: 5,
                  avgScore: 83.7,
                  completionRate: 0.91,
                  accessCount: 1356
                },
                {
                  id: 5,
                  className: '网络工程班',
                  courseName: '软件工程',
                  studentCount: 32,
                  assignmentCount: 4,
                  avgScore: 80.3,
                  completionRate: 0.78,
                  accessCount: 876
                }
              ],
              total: 5
            }
          })
        }
      }, 500)
    })
  }
}

// 数据过滤
const timeRange = ref('30')
const customDateRange = ref([])
const selectedClass = ref('all')
const accessClass = ref('all')
const activityClass = ref('all')

// 分页参数
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 指标数据
const metrics = reactive({
  totalStudents: 0,
  totalCourses: 0,
  avgCompletion: 0,
  avgScore: 0,
  studentTrend: 0,
  courseTrend: 0,
  completionTrend: 0,
  scoreTrend: 0
})

// 班级数据
const classes = ref([
  { id: 1, name: '计算机1班' },
  { id: 2, name: '计算机2班' },
  { id: 3, name: '软件工程1班' },
  { id: 4, name: '软件工程2班' },
  { id: 5, name: '网络工程班' }
])

// 课程数据
const courses = ref([
  { id: 1, name: 'Web前端开发' },
  { id: 2, name: '数据库原理' },
  { id: 3, name: '操作系统' },
  { id: 4, name: '计算机网络' },
  { id: 5, name: '软件工程' }
])

const scoreCourse = ref(1)
const assignmentCourse = ref(1)

// 表格数据
const courseData = ref([])

const tableLoading = ref(false)
const detailDialogVisible = ref(false)
const selectedCourse = ref(null)

// ECharts实例引用
const scoreChartRef = ref(null)
const assignmentChartRef = ref(null)
const courseAccessChartRef = ref(null)
const activityChartRef = ref(null)
const detailChartRef = ref(null)

// ECharts实例
let scoreChartInstance = null
let assignmentChartInstance = null
let courseAccessChartInstance = null
let activityChartInstance = null
let detailChartInstance = null

// 获取指标数据
const fetchMetrics = async () => {
  try {
    // 使用模拟数据 - 实际使用时替换为真实API调用
    const response = await mockAxios.get('/api/analytics/metrics')
    Object.assign(metrics, response.data)

    // 实际API调用示例（取消注释使用）：
    // const response = await axios.get(`${API_BASE_URL}/analytics/metrics`, {
    //   params: {
    //     timeRange: timeRange.value,
    //     customDateRange: customDateRange.value,
    //     classId: selectedClass.value === 'all' ? null : selectedClass.value
    //   }
    // })
    // Object.assign(metrics, response.data)
  } catch (error) {
    console.error('获取指标数据失败:', error)
    // 使用默认数据作为后备
    Object.assign(metrics, {
      totalStudents: 156,
      totalCourses: 5,
      avgCompletion: 87,
      avgScore: 82.5,
      studentTrend: 5,
      courseTrend: 0,
      completionTrend: 3,
      scoreTrend: -2
    })
    ElMessage.error('获取指标数据失败，使用模拟数据')
  }
}

// 获取课程数据
const fetchCourseData = async () => {
  tableLoading.value = true
  try {
    // 使用模拟数据 - 实际使用时替换为真实API调用
    const response = await mockAxios.get('/api/analytics/course-data')
    courseData.value = response.data.items
    total.value = response.data.total

    // 实际API调用示例（取消注释使用）：
    // const response = await axios.get(`${API_BASE_URL}/analytics/course-data`, {
    //   params: {
    //     timeRange: timeRange.value,
    //     customDateRange: customDateRange.value,
    //     classId: selectedClass.value === 'all' ? null : selectedClass.value,
    //     page: currentPage.value,
    //     pageSize: pageSize.value
    //   }
    // })
    // courseData.value = response.data.items
    // total.value = response.data.total
  } catch (error) {
    console.error('获取课程数据失败:', error)
    // 使用模拟数据作为后备
    courseData.value = [
      {
        id: 1,
        className: '计算机1班',
        courseName: 'Web前端开发',
        studentCount: 42,
        assignmentCount: 8,
        avgScore: 85.2,
        completionRate: 0.89,
        accessCount: 1245
      }
    ]
    total.value = 1
    ElMessage.error('获取课程数据失败，使用模拟数据')
  } finally {
    tableLoading.value = false
  }
}

// 分页大小改变
const handleSizeChange = (newSize) => {
  pageSize.value = newSize
  currentPage.value = 1
  fetchData()
}

// 当前页改变
const handleCurrentChange = (newPage) => {
  currentPage.value = newPage
  fetchData()
}

// 获取数据
const fetchData = () => {
  fetchMetrics()
  fetchCourseData()
  // 更新图表
  nextTick(() => {
    updateScoreChart()
    updateAssignmentChart()
    updateCourseAccessChart()
    updateActivityChart()
  })
}

// 初始化图表
const initCharts = () => {
  // 确保DOM已经渲染
  nextTick(() => {
    // 初始化成绩分布图表
    if (scoreChartRef.value) {
      scoreChartInstance = echarts.init(scoreChartRef.value)
      updateScoreChart()
    }

    // 初始化作业提交趋势图表
    if (assignmentChartRef.value) {
      assignmentChartInstance = echarts.init(assignmentChartRef.value)
      updateAssignmentChart()
    }

    // 初始化课程访问量图表
    if (courseAccessChartRef.value) {
      courseAccessChartInstance = echarts.init(courseAccessChartRef.value)
      updateCourseAccessChart()
    }

    // 初始化学生活跃度图表
    if (activityChartRef.value) {
      activityChartInstance = echarts.init(activityChartRef.value)
      updateActivityChart()
    }
  })
}

// 更新成绩分布图表
const updateScoreChart = () => {
  if (!scoreChartInstance) return

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    xAxis: {
      type: 'category',
      data: ['0-59', '60-69', '70-79', '80-89', '90-100'],
      name: '分数段'
    },
    yAxis: {
      type: 'value',
      name: '学生人数'
    },
    series: [{
      data: [3, 7, 12, 15, 5],
      type: 'bar',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#83bff6' },
          { offset: 0.5, color: '#188df0' },
          { offset: 1, color: '#188df0' }
        ])
      },
      emphasis: {
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#2378f7' },
            { offset: 0.7, color: '#2378f7' },
            { offset: 1, color: '#83bff6' }
          ])
        }
      }
    }]
  }
  scoreChartInstance.setOption(option)
}

// 更新作业提交趋势图表
const updateAssignmentChart = () => {
  if (!assignmentChartInstance) return

  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['完整观看人数', '未完整观看人数']
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
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '完整观看人数',
        type: 'line',
        stack: 'Total',
        data: [32, 35, 30, 38, 36, 34, 40],
        smooth: true,
        lineStyle: {
          width: 3
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(58,77,233,0.3)' },
            { offset: 1, color: 'rgba(58,77,233,0.1)' }
          ])
        }
      },
      {
        name: '未完整观看人数',
        type: 'line',
        stack: 'Total',
        data: [10, 7, 12, 4, 6, 8, 2],
        smooth: true,
        lineStyle: {
          width: 3
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255,86,86,0.3)' },
            { offset: 1, color: 'rgba(255,86,86,0.1)' }
          ])
        }
      }
    ]
  }
  assignmentChartInstance.setOption(option)
}

// 更新课程访问量图表
const updateCourseAccessChart = () => {
  if (!courseAccessChartInstance) return

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center'
    },
    series: [
      {
        name: '课程访问量',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 18,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: [
          { value: 1245, name: 'Web前端开发' },
          { value: 987, name: '数据库原理' },
          { value: 1123, name: '操作系统' },
          { value: 1356, name: '计算机网络' },
          { value: 876, name: '软件工程' }
        ]
      }
    ]
  }
  courseAccessChartInstance.setOption(option)
}

// 更新学生活跃度图表
const updateActivityChart = () => {
  if (!activityChartInstance) return

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: {
          backgroundColor: '#6a7985'
        }
      }
    },
    legend: {
      data: ['高活跃度', '中等活跃度', '低活跃度']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: [
      {
        type: 'category',
        boundaryGap: false,
        data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
      }
    ],
    yAxis: [
      {
        type: 'value'
      }
    ],
    series: [
      {
        name: '高活跃度',
        type: 'line',
        stack: 'Total',
        areaStyle: {},
        emphasis: {
          focus: 'series'
        },
        data: [12, 13, 10, 15, 17, 20, 18]
      },
      {
        name: '中等活跃度',
        type: 'line',
        stack: 'Total',
        areaStyle: {},
        emphasis: {
          focus: 'series'
        },
        data: [22, 18, 19, 23, 20, 25, 22]
      },
      {
        name: '低活跃度',
        type: 'line',
        stack: 'Total',
        areaStyle: {},
        emphasis: {
          focus: 'series'
        },
        data: [10, 12, 8, 9, 7, 5, 8]
      }
    ]
  }
  activityChartInstance.setOption(option)
}

// 查看课程详情
const viewCourseDetails = (course) => {
  selectedCourse.value = course
  detailDialogVisible.value = true

  // 初始化详情图表
  nextTick(() => {
    if (detailChartRef.value) {
      detailChartInstance = echarts.init(detailChartRef.value)

      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        series: [
          {
            name: '成绩分布',
            type: 'pie',
            radius: '50%',
            data: [
              { value: 3, name: '0-59' },
              { value: 7, name: '60-69' },
              { value: 12, name: '70-79' },
              { value: 15, name: '80-89' },
              { value: 5, name: '90-100' }
            ],
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }
        ]
      }

      detailChartInstance.setOption(option)
    }
  })
}

// 导出数据
const exportData = async () => {
  try {
    // 模拟导出
    ElMessage.success('数据导出成功')

    // 实际导出代码示例：
    // const response = await axios.get(`${API_BASE_URL}/analytics/export`, {
    //   params: {
    //     timeRange: timeRange.value,
    //     customDateRange: customDateRange.value,
    //     classId: selectedClass.value === 'all' ? null : selectedClass.value
    //   },
    //   responseType: 'blob'
    // })
    // const url = window.URL.createObjectURL(new Blob([response.data]))
    // const link = document.createElement('a')
    // link.href = url
    // link.setAttribute('download', `analytics_${new Date().getTime()}.xlsx`)
    // document.body.appendChild(link)
    // link.click()
    // link.remove()
    // window.URL.revokeObjectURL(url)
  } catch (error) {
    console.error('导出数据失败:', error)
    ElMessage.error('导出数据失败')
  }
}

// 响应式调整图表大小
const handleResize = () => {
  if (scoreChartInstance) scoreChartInstance.resize()
  if (assignmentChartInstance) assignmentChartInstance.resize()
  if (courseAccessChartInstance) courseAccessChartInstance.resize()
  if (activityChartInstance) activityChartInstance.resize()
  if (detailChartInstance) detailChartInstance.resize()
}

// 初始化
onMounted(() => {
  initCharts()
  fetchData() // 初始化时获取数据
  window.addEventListener('resize', handleResize)
})

// 清理
onUnmounted(() => {
  if (scoreChartInstance) echarts.dispose(scoreChartInstance)
  if (assignmentChartInstance) echarts.dispose(assignmentChartInstance)
  if (courseAccessChartInstance) echarts.dispose(courseAccessChartInstance)
  if (activityChartInstance) echarts.dispose(activityChartInstance)
  if (detailChartInstance) echarts.dispose(detailChartInstance)
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.analytics-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 40px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 16px 24px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.page-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
}

/* 指标卡片样式 */
.metrics-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.metric-card {
  border-radius: 8px;
}

.metric-content {
  display: flex;
  align-items: center;
  gap: 15px;
}

.metric-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.metric-icon.students {
  background-color: #10b981;
}

.metric-icon.courses {
  background-color: #3b82f6;
}

.metric-icon.assignments {
  background-color: #f59e0b;
}

.metric-icon.rating {
  background-color: #ef4444;
}

.metric-info h3 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
}

.metric-info p {
  margin: 5px 0 0;
  color: #6b7280;
  font-size: 14px;
}

.metric-trend {
  font-size: 12px;
  font-weight: 500;
  margin-top: 5px;
}

.metric-trend.up {
  color: #10b981;
}

.metric-trend.down {
  color: #ef4444;
}

/* 图表区域样式 */
.charts-section {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.chart-card {
  border-radius: 8px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container {
  padding: 10px 0;
}

/* 表格区域样式 */
.data-table-card {
  border-radius: 8px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.course-details {
  padding: 10px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .charts-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .analytics-container {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .header-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .metrics-cards {
    grid-template-columns: 1fr;
  }
}
</style>
