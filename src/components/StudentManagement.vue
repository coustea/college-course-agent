<template>
  <v-card class="pa-6 mx-auto" style="max-width: 1200px">
    <v-toolbar color="transparent" density="compact" class="mb-6">
      <v-btn color="primary" @click="openImportDialog" prepend-icon="mdi-upload">
        导入学生
      </v-btn>

      <v-text-field
          v-model="searchQuery"
          placeholder="搜索学生"
          density="compact"
          variant="outlined"
          hide-details
          prepend-inner-icon="mdi-magnify"
          class="ml-4"
          style="max-width: 300px"
      />
    </v-toolbar>

    <v-data-table
        :headers="headers"
        :items="filteredStudents"
        :items-per-page="10"
        :loading="loading"
        loading-text="加载中...请稍候"
    >
      <template #item.gender="{ item }">
        <v-icon :color="item.gender === 'male' ? 'blue' : 'pink'">
          {{ item.gender === 'male' ? 'mdi-gender-male' : 'mdi-gender-female' }}
        </v-icon>
        {{ item.gender === 'male' ? '男' : '女' }}
      </template>

      <template #item.actions="{ item }">
        <v-btn
            icon="mdi-eye"
            size="small"
            variant="text"
            color="primary"
            @click="viewStudent(item)"
            class="mr-2"
        />
        <v-btn
            icon="mdi-delete"
            size="small"
            variant="text"
            color="error"
            @click="confirmDelete(item)"
        />
      </template>
    </v-data-table>

    <!-- 导入对话框 -->
    <v-dialog v-model="importDialogVisible" max-width="500">
      <v-card>
        <v-card-title>导入学生</v-card-title>
        <v-card-text>
          <v-file-input
              v-model="importFile"
              accept=".xlsx,.xls"
              label="选择Excel文件"
              prepend-icon="mdi-file-excel"
              outlined
              dense
          />
          <v-btn
              block
              color="primary"
              class="mt-2"
              :loading="importing"
              @click="handleImport"
          >
            开始导入
          </v-btn>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn
              color="primary"
              variant="text"
              @click="downloadTemplate"
          >
            下载模板
          </v-btn>
          <v-btn @click="importDialogVisible = false">关闭</v-btn>
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
import { useConfirm } from 'vuetify-use-dialog'
import axios from 'axios'

export default {
  setup() {
    const confirm = useConfirm()

    // API基础配置
    const API_BASE_URL = 'http://47.95.222.6:8080/api'
    const token = ref(localStorage.getItem('token') || '')

    // 状态管理
    const searchQuery = ref('')
    const importDialogVisible = ref(false)
    const importFile = ref(null)
    const importing = ref(false)
    const loading = ref(false)

    // 学生数据
    const students = ref([])

    // 表格列配置
    const headers = [
      { title: '学号', key: 'id', width: '120px' },
      { title: '姓名', key: 'name', width: '120px' },
      { title: '性别', key: 'gender', width: '100px' },
      { title: '班级', key: 'className', width: '150px' },
      { title: '联系电话', key: 'phone', width: '150px' },
      { title: '选课数', key: 'courseCount', width: '100px' },
      { title: '操作', key: 'actions', width: '120px', sortable: false }
    ]

    // 通知管理
    const snackbar = ref({
      visible: false,
      message: '',
      color: 'success'
    })

    // 计算属性 - 过滤学生
    const filteredStudents = computed(() => {
      if (!searchQuery.value) return students.value
      const query = searchQuery.value.toLowerCase()
      return students.value.filter(student =>
          student.name.toLowerCase().includes(query) ||
          student.id.toLowerCase().includes(query) ||
          student.className.toLowerCase().includes(query)
      )
    })

    // 显示通知
    const showSnackbar = (message, type = 'success') => {
      snackbar.value = {
        visible: true,
        message,
        color: type
      }
    }

    // API操作 - 获取学生列表
    const fetchStudents = async () => {
      try {
        loading.value = true
        const response = await axios.get(API_BASE_URL + '/students/', {
          headers: {
            'Authorization': `Token ${token.value}`
          }
        })

        if (Array.isArray(response.data)) {
          students.value = response.data.map(student => ({
            id: student.id,
            name: student.name,
            gender: student.gender || 'male',
            className: student.class_name || '未分配班级',
            phone: student.phone || '未填写',
            courseCount: student.course_count || 0
          }))
        } else {
          throw new Error('返回数据格式不正确')
        }
      } catch (error) {
        console.error('获取学生列表失败:', error)
        showSnackbar('获取学生列表失败', 'error')
      } finally {
        loading.value = false
      }
    }

    // API操作 - 删除学生
    const deleteStudent = async (id) => {
      try {
        await axios.delete(
            API_BASE_URL + `/students/${id}/`,
            {
              headers: {
                'Authorization': `Token ${token.value}`
              }
            }
        )
        students.value = students.value.filter(student => student.id !== id)
        showSnackbar('学生删除成功')
      } catch (error) {
        console.error('删除学生失败:', error)
        showSnackbar('删除学生失败', 'error')
      }
    }

    // API操作 - 导入学生
    const importStudents = async (file) => {
      try {
        importing.value = true
        const formData = new FormData()
        formData.append('file', file)

        const response = await axios.post(
            API_BASE_URL + '/students/import/',
            formData,
            {
              headers: {
                'Authorization': `Token ${token.value}`,
                'Content-Type': 'multipart/form-data'
              }
            }
        )

        if (response.data.success) {
          showSnackbar('学生导入成功')
          await fetchStudents() // 重新获取学生列表
          return true
        } else {
          throw new Error(response.data.message || '导入失败')
        }
      } catch (error) {
        console.error('导入学生失败:', error)
        showSnackbar(`导入学生失败: ${error.message}`, 'error')
        return false
      } finally {
        importing.value = false
      }
    }

    // 组件方法 - 打开导入对话框
    const openImportDialog = () => {
      importFile.value = null
      importDialogVisible.value = true
    }

    // 组件方法 - 查看学生
    const viewStudent = (student) => {
      console.log('查看学生:', student)
      // 这里可以添加查看学生详情的逻辑
    }

    // 组件方法 - 确认删除
    const confirmDelete = async (student) => {
      const result = await confirm({
        title: '确认删除',
        text: `确定删除学生 "${student.name}" 吗?`,
        confirmationText: '确定',
        cancellationText: '取消'
      })

      if (result) {
        await deleteStudent(student.id)
      }
    }

    // 组件方法 - 处理导入
    const handleImport = async () => {
      if (!importFile.value) {
        showSnackbar('请选择文件', 'error')
        return
      }

      const success = await importStudents(importFile.value)
      if (success) {
        importDialogVisible.value = false
      }
    }

    // 组件方法 - 下载模板
    const downloadTemplate = () => {
      window.open(API_BASE_URL + '/students/template/', '_blank')
    }

    // 初始化
    onMounted(() => {
      fetchStudents()
    })

    return {
      searchQuery,
      headers,
      filteredStudents,
      importDialogVisible,
      importFile,
      importing,
      loading,
      snackbar,
      openImportDialog,
      viewStudent,
      confirmDelete,
      handleImport,
      downloadTemplate
    }
  }
}
</script>