<template>
  <div class="students-performance">
    <div class="page-header">
      <h2>学习表现分析</h2>
    </div>

    <div class="filter-bar">
      <el-select v-model="courseFilter" placeholder="选择课程" clearable style="width: 200px;">
        <el-option
          v-for="course in courses"
          :key="course.id"
          :label="course.name"
          :value="course.id"
        />
      </el-select>
      <el-select v-model="classFilter" placeholder="选择班级" clearable style="width: 200px; margin-left: 12px;">
        <el-option
          v-for="classItem in classes"
          :key="classItem"
          :label="classItem"
          :value="classItem"
        />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        style="margin-left: 12px; width: 350px;"
      />
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-card">
              <div class="stat-icon" style="color: #409EFF;">
                <el-icon><User /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-value">{{ stats.totalStudents }}</div>
                <div class="stat-label">总学生数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-card">
              <div class="stat-icon" style="color: #67C23A;">
                <el-icon><Clock /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-value">{{ stats.avgStudyTime }}h</div>
                <div class="stat-label">平均学习时长</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-card">
              <div class="stat-icon" style="color: #E6A23C;">
                <el-icon><Document /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-value">{{ stats.avgScore }}</div>
                <div class="stat-label">平均成绩</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-card">
              <div class="stat-icon" style="color: #F56C6C;">
                <el-icon><Warning /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-value">{{ stats.lowPerformance }}</div>
                <div class="stat-label">需关注学生</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 图表区域 -->
    <div class="charts-section">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <div class="chart-header">
                <span>测验成绩分布</span>
              </div>
            </template>
            <div ref="scoreChart" style="height: 300px;"></div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <div class="chart-header">
                <span>学习时长分布</span>
              </div>
            </template>
            <div ref="timeChart" style="height: 300px;"></div>
          </el-card>
        </el-col>
      </el-row>

    </div>

    <!-- 学生表现表格 -->
    <div class="performance-table">
      <el-card shadow="hover">
        <template #header>
          <div class="table-header">
            <span>学生表现详情</span>
            <el-button type="primary" :icon="Download" @click="exportData">导出数据</el-button>
          </div>
        </template>
        <el-table :data="performanceData" style="width: 100%" v-loading="loading">
          <el-table-column prop="studentId" label="学号" width="100" />
          <el-table-column prop="name" label="姓名" width="100" />
          <el-table-column prop="className" label="班级" width="120" />
          <el-table-column prop="courseName" label="课程" width="150" />
          <el-table-column prop="studyTime" label="学习时长(h)" width="120" sortable />
          <el-table-column prop="completedLessons" label="已完成课时" width="120" sortable />
          <el-table-column prop="totalLessons" label="总课时" width="100" />
          <el-table-column prop="completionRate" label="完成率" width="100" sortable>
            <template #default="scope">
              <el-tag :type="getCompletionType(scope.row.completionRate)">
                {{ scope.row.completionRate }}%
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="avgScore" label="平均成绩" width="100" sortable>
            <template #default="scope">
              <span :class="{'high-score': scope.row.avgScore >= 90, 'low-score': scope.row.avgScore < 60}">
                {{ scope.row.avgScore }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="scope">
              <el-button link type="primary" @click="viewDetails(scope.row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination" v-if="performanceData.length > 0">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="performanceData.length"
            layout="total, sizes, prev, pager, next, jumper"
            background
          />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, User, Clock, Document, Warning } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

export default {
  name: 'StudentsPerformance',
  components: {
    Download,
    User,
    Clock,
    Document,
    Warning
  },
  setup() {
    const loading = ref(false)
    const courseFilter = ref('')
    const classFilter = ref('')
    const dateRange = ref('')
    const currentPage = ref(1)
    const pageSize = ref(10)
    const scoreChart = ref(null)
    const timeChart = ref(null)
    const completionChart = ref(null)

    // 模拟数据
    const courses = ref([
      { id: 1, name: 'Web前端开发' },
      { id: 2, name: '数据结构' },
      { id: 3, name: '数据库原理' }
    ])

    const classes = ref(['计算机科学与技术1班', '软件工程2班', '网络工程1班'])

    const stats = ref({
      totalStudents: 156,
      avgStudyTime: 12.5,
      avgScore: 78.6,
      lowPerformance: 18
    })

    const performanceData = ref([
      {
        id: 1,
        studentId: '20230001',
        name: '张三',
        className: '计算机科学与技术1班',
        courseName: 'Web前端开发',
        studyTime: 15.5,
        completedLessons: 12,
        totalLessons: 15,
        completionRate: 80,
        avgScore: 85
      },
      {
        id: 2,
        studentId: '20230002',
        name: '李四',
        className: '软件工程2班',
        courseName: '数据结构',
        studyTime: 20.2,
        completedLessons: 10,
        totalLessons: 12,
        completionRate: 83,
        avgScore: 92
      },
      {
        id: 3,
        studentId: '20230003',
        name: '王五',
        className: '计算机科学与技术1班',
        courseName: '数据库原理',
        studyTime: 8.7,
        completedLessons: 6,
        totalLessons: 10,
        completionRate: 60,
        avgScore: 65
      },
      {
        id: 4,
        studentId: '20230004',
        name: '赵六',
        className: '软件工程2班',
        courseName: 'Web前端开发',
        studyTime: 18.3,
        completedLessons: 14,
        totalLessons: 15,
        completionRate: 93,
        avgScore: 88
      },
      {
        id: 5,
        studentId: '20230005',
        name: '钱七',
        className: '网络工程1班',
        courseName: '数据结构',
        studyTime: 9.2,
        completedLessons: 7,
        totalLessons: 12,
        completionRate: 58,
        avgScore: 52
      }
    ])

    // 过滤后的数据
    const filteredData = computed(() => {
      let result = performanceData.value

      if (courseFilter.value) {
        const course = courses.value.find(c => c.id == courseFilter.value)
        if (course) {
          result = result.filter(item => item.courseName === course.name)
        }
      }

      if (classFilter.value) {
        result = result.filter(item => item.className === classFilter.value)
      }

      // 这里可以添加日期范围过滤逻辑

      return result
    })

    // 分页数据
    const paginatedData = computed(() => {
      const start = (currentPage.value - 1) * pageSize.value
      const end = start + pageSize.value
      return filteredData.value.slice(start, end)
    })

    // 获取完成率标签类型
    const getCompletionType = (rate) => {
      if (rate >= 80) return 'success'
      if (rate >= 60) return 'warning'
      return 'danger'
    }

    // 查看详情
    const viewDetails = (student) => {
      ElMessage.info(`查看 ${student.name} 的详细学习表现`)
      // 实际项目中这里可以跳转到详细页面或打开对话框
    }

    // 导出数据
    const exportData = () => {
      ElMessage.success('数据导出功能待实现')
    }

    // 初始化图表
    const initCharts = () => {
      nextTick(() => {
        // 成绩分布图表
        const scoreChartInstance = echarts.init(scoreChart.value)
        scoreChartInstance.setOption({
          tooltip: {
            trigger: 'item'
          },
          legend: {
            orient: 'vertical',
            right: 10,
            top: 'center'
          },
          series: [
            {
              name: '成绩分布',
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
                  fontSize: '18',
                  fontWeight: 'bold'
                }
              },
              labelLine: {
                show: false
              },
              data: [
                { value: 35, name: '优秀(90-100)' },
                { value: 45, name: '良好(80-89)' },
                { value: 50, name: '中等(70-79)' },
                { value: 20, name: '及格(60-69)' },
                { value: 6, name: '不及格(<60)' }
              ]
            }
          ]
        })

        // 学习时长分布图表
        const timeChartInstance = echarts.init(timeChart.value)
        timeChartInstance.setOption({
          tooltip: {
            trigger: 'axis',
            axisPointer: {
              type: 'shadow'
            }
          },
          xAxis: {
            type: 'category',
            data: ['0-5h', '5-10h', '10-15h', '15-20h', '20+h']
          },
          yAxis: {
            type: 'value'
          },
          series: [
            {
              data: [15, 32, 45, 28, 10],
              type: 'bar',
              itemStyle: {
                color: '#5470c6'
              }
            }
          ]
        })

        // 课程完成率图表
        const completionChartInstance = echarts.init(completionChart.value)
        completionChartInstance.setOption({
          tooltip: {
            trigger: 'axis'
          },
          legend: {
            data: ['Web前端开发', '数据结构', '数据库原理']
          },
          grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            containLabel: true
          },
          xAxis: {
            type: 'category',
            data: ['第1周', '第2周', '第3周', '第4周', '第5周', '第6周', '第7周']
          },
          yAxis: {
            type: 'value',
            max: 100
          },
          series: [
            {
              name: 'Web前端开发',
              type: 'line',
              data: [15, 25, 40, 55, 70, 80, 90]
            },
            {
              name: '数据结构',
              type: 'line',
              data: [10, 20, 35, 50, 65, 75, 85]
            },
            {
              name: '数据库原理',
              type: 'line',
              data: [5, 15, 30, 45, 60, 70, 80]
            }
          ]
        })

        // 响应窗口大小变化
        window.addEventListener('resize', () => {
          scoreChartInstance.resize()
          timeChartInstance.resize()
          completionChartInstance.resize()
        })
      })
    }

    onMounted(() => {
      initCharts()
    })

    return {
      loading,
      courseFilter,
      classFilter,
      dateRange,
      currentPage,
      pageSize,
      scoreChart,
      timeChart,
      completionChart,
      courses,
      classes,
      stats,
      performanceData: paginatedData,
      getCompletionType,
      viewDetails,
      exportData
    }
  }
}
</script>

<style scoped>
.students-performance {
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

.filter-bar {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  padding: 16px 24px;
  background: white;
  border-radius: 8px 8px 0 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.stats-cards {
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
}

.stat-icon {
  font-size: 40px;
  margin-right: 15px;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 5px;
}

.stat-label {
  color: #999;
  font-size: 14px;
}

.charts-section {
  margin-bottom: 24px;
}

.chart-header {
  font-weight: 600;
  font-size: 16px;
}

.performance-table {
  margin-bottom: 24px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.high-score {
  color: #67c23a;
  font-weight: bold;
}

.low-score {
  color: #f56c6c;
  font-weight: bold;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .filter-bar {
    flex-wrap: wrap;
    gap: 12px;
  }
}

@media (max-width: 768px) {
  .stats-cards .el-col {
    margin-bottom: 15px;
  }

  .charts-section .el-col {
    margin-bottom: 20px;
  }

  .filter-bar {
    flex-direction: column;
    gap: 12px;
  }

  .filter-bar .el-select,
  .filter-bar .el-date-editor {
    width: 100% !important;
    margin-left: 0 !important;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
}
</style>
