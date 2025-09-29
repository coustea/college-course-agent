<template>
  <div class="page-header">
    <h1 class="page-title">学习数据</h1>
  </div>
  <div class="charts-container">
    <!-- 课程进度图表 -->
    <div class="chart-card">
      <div class="chart-header">
        <h2>课程学习进度</h2>
        <div class="chart-filter">
          <div class="mini-legend" :class="{ disabled: selectedCourse !== '__all__' }">
              <span class="legend-item" :class="{ inactive: !selectedTypes.video }" @click="toggleType('video')">
                <i class="legend-dot" style="background:#3B82F6"></i> 视频类
              </span>
            <span class="legend-item" :class="{ inactive: !selectedTypes.document }" @click="toggleType('document')">
                <i class="legend-dot" style="background:#10B981"></i> 文档类
              </span>
          </div>
          <select v-model="selectedCourse" @change="updateCharts" class="select-center">
            <option value="__all__">全部课程</option>
            <option v-for="name in allCourses" :key="name" :value="name">{{ name }}</option>
          </select>
        </div>
      </div>
      <div class="chart-wrapper">
        <Bar :data="progressChartData" :options="progressChartOptions" />
      </div>
    </div>

    <!-- 学习时间分布图表 -->
    <div class="chart-card">
      <div class="chart-header">
        <h2>学习时间分布</h2>
        <div class="chart-filter">
          <select v-model="timeFilter" @change="updateCharts">
            <option value="week">最近一周</option>
            <option value="month">最近一月</option>
          </select>
        </div>
      </div>
      <div class="chart-wrapper">
        <Line :data="timeChartData" :options="timeChartOptions" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Line, Bar } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
  Filler
} from 'chart.js'

import { fetchHomeCourses } from '/src/services/homeCoursesApi'

const selectedCourse = ref('__all__')
const timeFilter = ref('week')
const selectedTypes = ref({ video: true, document: true })
function toggleType(key) {
  if (selectedCourse.value !== '__all__') return
  selectedTypes.value[key] = !selectedTypes.value[key]
}

// 与首页绑定：复用首页课程数据源
const homeCourses = ref([])
onMounted(async () => {
  try {
    const list = await fetchHomeCourses()
    homeCourses.value = Array.isArray(list) ? list : []
  } catch {
    homeCourses.value = []
  }
})

// 课程名称列表
const allCourses = computed(() => (homeCourses.value || [])
  .map(c => c.title || c.courseName || '')
  .filter(Boolean))

// 课程类型映射：name -> 'video' | 'document'
const courseTypes = computed(() => {
  const map = {}
  for (const c of (homeCourses.value || [])) {
    const name = c.title || c.courseName || ''
    if (!name) continue
    const t = c.type === 'video' ? 'video' : 'document'
    map[name] = t
  }
  return map
})

// 课程整体进度（0-100），若后端无值则默认为 0
const allCoursesProgress = computed(() => (homeCourses.value || []).map(c => {
  const v = c.progress ?? c.overall ?? c.overallProgress ?? c.completionRate
  const n = Number(v)
  if (Number.isFinite(n)) return Math.max(0, Math.min(100, n))
  return 0
}))

const singleCourseChapters = [
  '第一章', '第二章', '第三章', '第四章', '第五章',
  '第六章', '第七章', '第八章', '第九章', '第十章'
]
// 按课程自定义章节进度（示例），没有配置则按“单条”展示
// 提取并拍平成“章节进度”数组：支持多层 children；若无进度字段则默认 0
const courseChaptersMap = computed(() => {
  const map = {}
  const flatten = (nodes = [], acc = []) => {
    for (const node of nodes) {
      if (Array.isArray(node?.children) && node.children.length) {
        flatten(node.children, acc)
      } else {
        acc.push(node)
      }
    }
    return acc
  }
  for (const c of (homeCourses.value || [])) {
    const name = c.title || c.courseName || ''
    if (!name) continue
    const raw = Array.isArray(c.chapters) ? c.chapters : []
    const leafChapters = flatten(raw, [])
    if (leafChapters.length > 1) {
      const progresses = leafChapters.map(ch => {
        const v = ch?.progress ?? ch?.completionRate ?? ch?.overall ?? 0
        const n = Number(v)
        if (Number.isFinite(n)) return Math.max(0, Math.min(100, n))
        return 0
      })
      map[name] = progresses
    }
  }
  return map
})

const timeData = {
  week: {
    labels: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    videoTime: [2.5, 2.0, 3.5, 2.0, 2.5, 1.5, 1.0],
    documentTime: [1.5, 2.0, 1.0, 2.5, 1.5, 2.0, 0.5]
  },
  month: {
    labels: ['第1周', '第2周', '第3周', '第4周'],
    videoTime: [12.5, 14.0, 11.5, 13.2],
    documentTime: [8.5, 9.0, 7.5, 8.8]
  }
}

ChartJS.register(
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    BarElement,
    Title,
    Tooltip,
    Legend,
    Filler
)

const progressChartData = computed(() => {
  if (selectedCourse.value === '__all__') {
    const names = allCourses.value
    const types = courseTypes.value
    const progress = allCoursesProgress.value
    const visibleIdx = names
        .map((name, idx) => ({ name, idx, type: courseTypes[name] }))
        .filter(({ name, type }) => {
          const t = types[name] || type
          return (t === 'video' && selectedTypes.value.video) || (t === 'document' && selectedTypes.value.document) || (t !== 'video' && t !== 'document')
        })
        .map(({ idx }) => idx)

    return {
      labels: visibleIdx.map(i => {
        const course = names[i]
        return course.length > 12 ? course.substring(0, 12) + '...' : course
      }),
      datasets: [{
        label: '学习进度',
        data: visibleIdx.map(i => progress[i]),
        backgroundColor: visibleIdx.map(i => {
          const t = types[names[i]]
          return t === 'video' ? '#3b82f6' : t === 'document' ? '#10b981' : '#6b7280'
        }),
        borderColor: visibleIdx.map(i => {
          const t = types[names[i]]
          return t === 'video' ? '#2563eb' : t === 'document' ? '#059669' : '#4b5563'
        }),
        borderWidth: 1,
        borderRadius: 4,
        borderSkipped: false,
        barThickness: 18,
        maxBarThickness: 22,
        categoryPercentage: 0.7,
        barPercentage: 0.8,
      }]
    }
  }

  // 选中单课程：如果是单视频/单文档，仅保留该课程数据；若有章节则 y 轴为章节列表
  const name = selectedCourse.value
  const types = courseTypes.value
  const chapterMap = courseChaptersMap.value
  const type = types[name]
  const chapterProgress = chapterMap[name]
  if (Array.isArray(chapterProgress) && chapterProgress.length > 1) {
    // 多章节：按章节数量动态生成标签与进度
    const labels = Array.from({ length: chapterProgress.length }, (_, i) => `第${i + 1}章`)
    const baseColor = type === 'video' ? '#3b82f6' : '#10b981'
    const border = type === 'video' ? '#2563eb' : '#059669'
    return {
      labels,
      datasets: [{
        label: '章节进度',
        data: chapterProgress,
        backgroundColor: chapterProgress.map(() => baseColor),
        borderColor: chapterProgress.map(() => border),
        borderWidth: 1,
        borderRadius: 4,
        borderSkipped: false,
        barThickness: 18,
        maxBarThickness: 22,
        categoryPercentage: 0.72,
        barPercentage: 0.82,
      }]
    }
  }

  const idx = allCourses.value.indexOf(name)
  const value = idx >= 0 ? allCoursesProgress.value[idx] : 0
  return {
    labels: [name.length > 12 ? name.substring(0, 12) + '...' : name],
    datasets: [{
      label: '学习进度',
      data: [value],
      backgroundColor: [type === 'video' ? '#3b82f6' : '#10b981'],
      borderColor: [type === 'video' ? '#2563eb' : '#059669'],
      borderWidth: 1,
      borderRadius: 4,
      borderSkipped: false,
      barThickness: 18,
      maxBarThickness: 22,
      categoryPercentage: 0.7,
      barPercentage: 0.8,
    }]
  }
})

const timeChartData = computed(() => {
  const currentTimeData = timeData[timeFilter.value]
  return {
    labels: currentTimeData.labels,
    datasets: [
      {
        label: '视频学习',
        data: currentTimeData.videoTime,
        borderColor: '#3B82F6',
        backgroundColor: 'rgba(59, 130, 246, 0.1)',
        borderWidth: 3,
        fill: true,
        tension: 0.4,
        pointBackgroundColor: '#3B82F6',
        pointBorderColor: '#ffffff',
        pointBorderWidth: 2,
        pointRadius: 6
      },
      {
        label: '文档阅读',
        data: currentTimeData.documentTime,
        borderColor: '#10B981',
        backgroundColor: 'rgba(16, 185, 129, 0.1)',
        borderWidth: 3,
        fill: true,
        tension: 0.4,
        pointBackgroundColor: '#10B981',
        pointBorderColor: '#ffffff',
        pointBorderWidth: 2,
        pointRadius: 6
      }
    ]
  }
})

// 课程学习进度：条形图
const progressChartOptions = ref({
  responsive: true,
  maintainAspectRatio: false,
  indexAxis: 'y',
  plugins: {
    legend: {
      display: false
    },
    tooltip: {
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      titleColor: '#ffffff',
      bodyColor: '#ffffff',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      cornerRadius: 8,
      displayColors: true,
      callbacks: {
        label: function(context) {
          const isAll = selectedCourse.value === '__all__'
          const courseName = isAll
              ? allCourses[context.dataIndex]
              : (selectedCourse.value === '__chapters__' ? singleCourseChapters[context.dataIndex] : selectedCourse.value)
          return `${courseName}: ${context.parsed.x}%`
        }
      }
    }
  },
  scales: {
    x: {
      beginAtZero: true,
      max: 100,
      grid: {
        color: 'rgba(107, 114, 128, 0.1)'
      },
      ticks: {
        color: '#6b7280',
        font: {
          size: 12
        },
        callback: function(value) {
          return value + '%'
        }
      }
    },
    y: {
      grid: {
        display: false
      },
      ticks: {
        color: '#6b7280',
        font: {
          size: 12
        }
      }
    }
  }
})

// 学习时间折线图
const timeChartOptions = ref({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: true,
      position: 'top',
      align: 'end',
      labels: {
        usePointStyle: true,
        padding: 20,
        font: {
          size: 12
        }
      }
    },
    tooltip: {
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      titleColor: '#ffffff',
      bodyColor: '#ffffff',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      cornerRadius: 8,
      displayColors: true,
      callbacks: {
        label: function(context) {
          const val = context.parsed.y
          if (val == null || Number.isNaN(val)) return ''
          // 显示为 Xh Ym，更易读
          const totalMin = Math.round(val * 60)
          const h = Math.floor(totalMin / 60)
          const m = totalMin % 60
          const text = (h > 0 ? `${h}h` : '') + (m > 0 ? `${h > 0 ? ' ' : ''}${m}m` : (h === 0 ? '0m' : ''))
          return `${context.dataset.label}: ${text}`
        },
        title: function(items) {
          // 灵活标题：当周显示“周几”，当月显示“第N周”
          return items?.[0]?.label || ''
        }
      }
    }
  },
  scales: {
    x: {
      grid: {
        display: false
      },
      ticks: {
        color: '#6b7280',
        font: {
          size: 12
        }
      }
    },
    y: {
      beginAtZero: true,
      grid: {
        color: 'rgba(107, 114, 128, 0.1)'
      },
      ticks: {
        color: '#6b7280',
        font: {
          size: 12
        },
        callback: function(value) {
          return value + 'h'
        }
      }
    }
  },
  interaction: {
    intersect: false,
    mode: 'index'
  }
})

const updateCharts = () => {}
// 选中项变化时，图表已根据 selectedCourse 的计算属性联动；此处仅作为手动触发器
</script>

<style scoped>

.page-header {
  margin-bottom: 16px;
}

.page-title {
  font-size: 24px;
  font-weight: bold;
  color: #2c3e50;
}

.page-header p {
  font-size: 16px;
  color: #6b7280;
  margin: 0;
}

.charts-container {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}

.chart-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06);
  padding: 24px;
  transition: box-shadow 0.2s ease-in-out;
}

.chart-card:hover {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.chart-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

/* 课程选择改回右侧下拉，无需课程 pills 样式 */

.chart-filter select {
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: white;
  color: #374151;
  font-size: 14px;
  cursor: pointer;
  transition: border-color 0.2s;
}

.chart-filter .select-center {
  text-align: center;
  text-align-last: center;
}

.chart-filter select:hover {
  border-color: #3b82f6;
}

.chart-filter select:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.mini-legend {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-right: 10px;
}
.mini-legend.disabled { opacity: 0.6; pointer-events: none; }
.mini-legend .legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #64748b;
  cursor: pointer;
  user-select: none;
}
.mini-legend .legend-item.inactive { opacity: 0.35; }
.mini-legend .legend-dot { width: 10px; height: 10px; border-radius: 2px; display: inline-block; }

.chart-wrapper {
  height: 400px;
  position: relative;
}

@media (min-width: 1024px) {
  .charts-container {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {

  .chart-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .chart-wrapper {
    height: 300px;
  }
}
</style>
