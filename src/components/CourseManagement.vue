<template>
  <v-card class="pa-6 mx-auto" style="max-width: 1200px">
    <v-toolbar color="transparent" density="compact" class="mb-6">
      <v-btn color="primary" @click="openCreateDialog" prepend-icon="mdi-plus">
        新增课程
      </v-btn>

      <v-text-field
          v-model="searchQuery"
          placeholder="搜索课程"
          density="compact"
          variant="outlined"
          hide-details
          prepend-inner-icon="mdi-magnify"
          class="ml-4"
          style="max-width: 300px"
      />
    </v-toolbar>

    <!-- 图表部分 -->
    <v-row class="mb-6">
      <v-col cols="12" md="6">
        <v-card>
          <v-card-title>学生学习进度</v-card-title>
          <v-card-text>
            <div ref="progressChart" style="height: 300px;"></div>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" md="6">
        <v-card>
          <v-card-title>课程参与度分析</v-card-title>
          <v-card-text>
            <div ref="engagementChart" style="height: 300px;"></div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-data-table
        :headers="headers"
        :items="filteredCourses"
        :items-per-page="10"
        :loading="loading"
        loading-text="加载中...请稍候"
    >
      <template #item.status="{ item }">
        <v-chip :color="getStatusColor(item.status)" size="small">
          {{ item.status }}
        </v-chip>
      </template>

      <!-- 行内编辑模板 -->
      <template #item="{ item, index }">
        <tr v-if="editingIndex === index" class="editing-row">
          <td>{{ item.id }}</td>
          <td>
            <v-text-field
                v-model="editForm.name"
                :rules="[v => !!v || '必填项']"
                density="compact"
                hide-details
            />
          </td>
          <td>
            <v-text-field
                v-model="editForm.code"
                :rules="[v => !!v || '必填项']"
                density="compact"
                hide-details
            />
          </td>
          <td>
            <v-text-field
                v-model="editForm.credit"
                type="number"
                :rules="[v => !!v || '必填项']"
                density="compact"
                hide-details
            />
          </td>
          <td>{{ item.teacherName }}</td>
          <td>{{ item.studentCount }}</td>
          <td>
            <v-select
                v-model="editForm.status"
                :items="statusOptions"
                density="compact"
                hide-details
            />
          </td>
          <td>
            <v-btn
                icon="mdi-check"
                size="small"
                variant="text"
                color="success"
                @click="saveEdit(item)"
                class="mr-2"
            />
            <v-btn
                icon="mdi-close"
                size="small"
                variant="text"
                color="error"
                @click="cancelEdit"
            />
          </td>
        </tr>
        <tr v-else>
          <td>{{ item.id }}</td>
          <td>{{ item.name }}</td>
          <td>{{ item.code }}</td>
          <td>{{ item.credit }}</td>
          <td>{{ item.teacherName }}</td>
          <td>{{ item.studentCount }}</td>
          <td>
            <v-chip :color="getStatusColor(item.status)" size="small">
              {{ item.status }}
            </v-chip>
          </td>
          <td>
            <v-btn
                icon="mdi-pencil"
                size="small"
                variant="text"
                color="primary"
                @click="editCourse(item, index)"
                class="mr-2"
            />
            <v-btn
                icon="mdi-delete"
                size="small"
                variant="text"
                color="error"
                @click="confirmDelete(item)"
            />
          </td>
        </tr>
      </template>
    </v-data-table>

    <!-- 新增课程对话框 -->
    <v-dialog v-model="createDialogVisible" max-width="600">
      <v-card>
        <v-card-title class="text-h5">新增课程</v-card-title>

        <v-card-text>
          <v-form ref="createForm">
            <v-text-field
                v-model="createForm.name"
                label="课程名称"
                :rules="[v => !!v || '必填项']"
                required
            />

            <v-text-field
                v-model="createForm.code"
                label="课程代码"
                :rules="[v => !!v || '必填项']"
                required
            />

            <v-text-field
                v-model="createForm.credit"
                label="学分"
                type="number"
                :rules="[v => !!v || '必填项']"
                required
            />

            <v-textarea
                v-model="createForm.description"
                label="课程描述"
                rows="3"
            />
          </v-form>
        </v-card-text>

        <v-card-actions>
          <v-spacer />
          <v-btn @click="createDialogVisible = false">取消</v-btn>
          <v-btn color="primary" @click="saveNewCourse" :loading="saving">保存</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-snackbar v-model="snackbar.visible" :color="snackbar.color">
      {{ snackbar.message }}
    </v-snackbar>
  </v-card>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import * as echarts from 'echarts'
import axios from 'axios'

export default {
  setup() {
    // API基础配置
    const API_BASE_URL = 'http://47.95.222.6:8080/api'
    const token = ref(localStorage.getItem('token') || '')

    // 状态管理
    const searchQuery = ref('')
    const createDialogVisible = ref(false)
    const editingIndex = ref(-1)
    const progressChart = ref(null)
    const engagementChart = ref(null)
    const loading = ref(false)
    const saving = ref(false)

    // 表单数据
    const createForm = ref({
      name: '',
      code: '',
      credit: 2,
      description: ''
    })

    const editForm = ref({
      name: '',
      code: '',
      credit: 2,
      status: '未开始'
    })

    // 课程数据
    const courses = ref([])
    const statusOptions = ['未开始', '进行中', '已结束']

    // 表格列配置
    const headers = [
      { title: '课程ID', key: 'id', width: '100px' },
      { title: '课程名称', key: 'name' },
      { title: '课程代码', key: 'code', width: '120px' },
      { title: '学分', key: 'credit', width: '80px' },
      { title: '授课教师', key: 'teacherName', width: '120px' },
      { title: '学生人数', key: 'studentCount', width: '100px' },
      { title: '状态', key: 'status', width: '100px' },
      { title: '操作', key: 'actions', width: '150px', sortable: false }
    ]

    // 通知管理
    const snackbar = ref({
      visible: false,
      message: '',
      color: 'success'
    })

    // 计算属性 - 过滤课程
    const filteredCourses = computed(() => {
      if (!searchQuery.value) return courses.value
      const query = searchQuery.value.toLowerCase()
      return courses.value.filter(course =>
          course.name.toLowerCase().includes(query) ||
          course.code.toLowerCase().includes(query))
    })

    // 工具函数 - 获取状态颜色
    const getStatusColor = (status) => {
      const statusColors = {
        '进行中': 'success',
        '已结束': 'info',
        '未开始': 'warning'
      }
      return statusColors[status] || 'grey'
    }

    // 显示通知
    const showSnackbar = (message, type = 'success') => {
      snackbar.value = {
        visible: true,
        message,
        color: type
      }
    }

    // 初始化图表
    const initCharts = () => {
      if (courses.value.length === 0) return

      // 学生学习进度图表 (折线图)
      const progressChartInstance = echarts.init(progressChart.value)
      progressChartInstance.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: courses.value.map(c => c.name) },
        xAxis: {
          type: 'category',
          data: ['第一章', '第二章', '第三章', '第四章']
        },
        yAxis: {
          type: 'value',
          axisLabel: { formatter: '{value}%' },
          max: 100
        },
        series: courses.value.map(course => ({
          name: course.name,
          type: 'line',
          data: course.progressData || [20, 50, 80, 100],
          smooth: true,
          lineStyle: { width: 3 }
        }))
      })

      // 课程参与度图表 (柱状图)
      const engagementChartInstance = echarts.init(engagementChart.value)
      engagementChartInstance.setOption({
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' }
        },
        legend: { data: ['参与度'] },
        xAxis: {
          type: 'category',
          data: courses.value.map(c => c.name)
        },
        yAxis: {
          type: 'value',
          axisLabel: { formatter: '{value}%' },
          max: 100
        },
        series: [{
          name: '参与度',
          type: 'bar',
          data: courses.value.map(c => {
            const data = c.engagementData || [85, 78, 92, 65]
            return data.reduce((a, b) => a + b, 0) / data.length
          }),
          itemStyle: {
            color: function(params) {
              const colorList = ['#5470C6', '#91CC75', '#EE6666']
              return colorList[params.dataIndex % colorList.length]
            }
          }
        }]
      })

      // 响应式调整图表大小
      window.addEventListener('resize', () => {
        progressChartInstance.resize()
        engagementChartInstance.resize()
      })
    }

    // API操作 - 获取课程列表
    const fetchCourses = async () => {
      try {
        loading.value = true
        const response = await axios.get(API_BASE_URL + '/courses/', {
          headers: {
            'Authorization': `Token ${token.value}`
          }
        })

        if (Array.isArray(response.data)) {
          courses.value = response.data.map(course => ({
            id: course.id,
            name: course.name,
            code: course.code,
            credit: course.credit,
            teacherName: course.teacher_name || '当前教师',
            studentCount: course.student_count || 0,
            status: course.status || '未开始',
            progressData: course.progress_data || [20, 50, 80, 100],
            engagementData: course.engagement_data || [85, 78, 92, 65]
          }))
        } else {
          throw new Error('返回数据格式不正确')
        }

        initCharts()
      } catch (error) {
        console.error('获取课程列表失败:', error)
        showSnackbar('获取课程列表失败', 'error')
      } finally {
        loading.value = false
      }
    }

    // API操作 - 创建课程
    const createCourse = async (courseData) => {
      try {
        saving.value = true
        const formData = new FormData()
        formData.append('name', courseData.name)
        formData.append('code', courseData.code)
        formData.append('credit', courseData.credit)
        if (courseData.description) formData.append('description', courseData.description)

        const response = await axios.post(
            API_BASE_URL + '/courses/',
            formData,
            {
              headers: {
                'Authorization': `Token ${token.value}`,
                'Content-Type': 'multipart/form-data'
              }
            }
        )

        courses.value.push({
          id: response.data.id,
          name: response.data.name,
          code: response.data.code,
          credit: response.data.credit,
          teacherName: response.data.teacher_name || '当前教师',
          studentCount: response.data.student_count || 0,
          status: response.data.status || '未开始',
          progressData: [10, 30, 60, 80],
          engagementData: [70, 75, 80, 85]
        })

        showSnackbar('课程创建成功')
        return true
      } catch (error) {
        console.error('创建课程失败:', error)
        showSnackbar('创建课程失败', 'error')
        return false
      } finally {
        saving.value = false
      }
    }

    // API操作 - 更新课程
    const updateCourse = async (id, courseData) => {
      try {
        saving.value = true
        const formData = new FormData()
        formData.append('name', courseData.name)
        formData.append('code', courseData.code)
        formData.append('credit', courseData.credit)
        formData.append('status', courseData.status)
        if (courseData.description) formData.append('description', courseData.description)

        const response = await axios.put(
            API_BASE_URL + `/courses/${id}/`,
            formData,
            {
              headers: {
                'Authorization': `Token ${token.value}`,
                'Content-Type': 'multipart/form-data'
              }
            }
        )

        const index = courses.value.findIndex(c => c.id === id)
        if (index !== -1) {
          courses.value[index] = {
            id: response.data.id,
            name: response.data.name,
            code: response.data.code,
            credit: response.data.credit,
            teacherName: response.data.teacher_name || courses.value[index].teacherName,
            studentCount: response.data.student_count || courses.value[index].studentCount,
            status: response.data.status || courses.value[index].status,
            progressData: courses.value[index].progressData,
            engagementData: courses.value[index].engagementData
          }
        }

        showSnackbar('课程更新成功')
        return true
      } catch (error) {
        console.error('更新课程失败:', error)
        showSnackbar('更新课程失败', 'error')
        return false
      } finally {
        saving.value = false
      }
    }

    // API操作 - 删除课程
    const deleteCourse = async (id) => {
      try {
        await axios.delete(
            API_BASE_URL + `/courses/${id}/`,
            {
              headers: {
                'Authorization': `Token ${token.value}`
              }
            }
        )
        courses.value = courses.value.filter(course => course.id !== id)
        showSnackbar('课程删除成功')
      } catch (error) {
        console.error('删除课程失败:', error)
        showSnackbar('删除课程失败', 'error')
      }
    }

    // 组件方法 - 打开新增对话框
    const openCreateDialog = () => {
      createForm.value = {
        name: '',
        code: '',
        credit: 2,
        description: ''
      }
      createDialogVisible.value = true
    }

    // 组件方法 - 编辑课程
    const editCourse = (course, index) => {
      editForm.value = {
        name: course.name,
        code: course.code,
        credit: course.credit,
        status: course.status
      }
      editingIndex.value = index
    }

    // 组件方法 - 保存编辑
    const saveEdit = async (course) => {
      try {
        const success = await updateCourse(course.id, editForm.value)
        if (success) {
          editingIndex.value = -1
        }
      } catch (error) {
        console.error('保存编辑失败:', error)
      }
    }

    // 组件方法 - 取消编辑
    const cancelEdit = () => {
      editingIndex.value = -1
    }

    // 组件方法 - 确认删除
    const confirmDelete = async (course) => {
      if (confirm(`确定删除课程 "${course.name}" 吗?`)) {
        await deleteCourse(course.id)
      }
    }

    // 组件方法 - 保存新课程
    const saveNewCourse = async () => {
      const { valid } = await createForm.value.validate()
      if (!valid) return

      const success = await createCourse(createForm.value)
      if (success) {
        createDialogVisible.value = false
        initCharts()
      }
    }

    // 初始化
    onMounted(() => {
      fetchCourses()
    })

    return {
      searchQuery,
      createDialogVisible,
      editingIndex,
      createForm,
      editForm,
      statusOptions,
      headers,
      filteredCourses,
      progressChart,
      engagementChart,
      loading,
      saving,
      snackbar,
      getStatusColor,
      openCreateDialog,
      editCourse,
      saveEdit,
      cancelEdit,
      confirmDelete,
      saveNewCourse
    }
  }
}
</script>

<style scoped>
.editing-row {
  background-color: rgba(0, 0, 0, 0.04);
}
</style>