<template>
  <div class="students-management">
    <div class="page-header">
      <h2>学生列表</h2>
      <el-button type="primary" @click="showImportDialog = true"><el-icon><Upload/></el-icon>导入学生</el-button>
    </div>

    <div class="filter-bar">
      <el-input v-model="searchQuery" placeholder="搜索学生姓名或学号..." :prefix-icon="Search" style="width: 400px; margin-right: 16px;" clearable />
      <el-select v-model="classFilter" placeholder="按班级筛选" clearable style="width: 150px; margin-right: 12px;">
        <el-option v-for="classItem in classes" :key="classItem" :label="classItem" :value="classItem" />
      </el-select>
      <el-select v-model="statusFilter" placeholder="按状态筛选" clearable style="width: 150px; margin-right: 12px;">
        <el-option label="在校" value="IN_SCHOOL" />
        <el-option label="校外实习" value="OFF_CAMPUS_INTERNSHIP" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="fetchStudents">搜索</el-button>
    </div>

    <div class="table-container">
      <el-table :data="paginatedStudents" style="width: 100%" v-loading="loading" height="100%">
        <el-table-column type="index" label="序号" width="60" :style="{ textAlign: 'center' }" />
        <el-table-column prop="studentNumber" label="学号" width="140" :style="{ textAlign: 'center' }" />
        <el-table-column prop="name" label="姓名" width="120" :style="{ textAlign: 'center' }" />
        <el-table-column prop="className" label="班级" width="120" :style="{ textAlign: 'center' }" />
        <el-table-column prop="phone" label="手机号" width="150" :style="{ textAlign: 'center' }" />
        <el-table-column prop="email" label="邮箱" width="200" :style="{ textAlign: 'center' }" />
        <el-table-column prop="status" label="状态" width="120" :style="{ textAlign: 'center' }">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'IN_SCHOOL' ? 'success' : 'warning'">
              {{ scope.row.status === 'IN_SCHOOL' ? '在校' : scope.row.status === 'OFF_CAMPUS_INTERNSHIP' ? '校外实习' : scope.row.status || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="courseCount" label="学习课程" width="100" :style="{ textAlign: 'center' }" />
        <el-table-column label="操作" width="240" fixed="right" :style="{ textAlign: 'center' }">
          <template #default="scope">
            <el-button size="small" @click="viewLearningProgress(scope.row)">进度</el-button>
            <el-button size="small" @click="editStudent(scope.row)" style="margin-left: 8px;">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteStudent(scope.row)" style="margin-left: 8px;">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-pagination v-if="filteredStudents.length > 0" v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[10, 20, 50, 100]" :total="filteredStudents.length" layout="total, sizes, prev, pager, next, jumper" background class="pagination" />

    <!-- 导入学生对话框 -->
    <el-dialog title="导入学生" v-model="showImportDialog" width="500px">
      <el-upload class="upload-demo" :http-request="uploadStudentFile" :before-upload="beforeUpload" :on-success="handleUploadSuccess" :on-error="handleUploadError" :file-list="fileList" :headers="uploadHeaders" accept=".xlsx,.xls">
        <el-button type="primary">选择文件</el-button>
        <template #tip><div class="el-upload__tip">请上传Excel文件（.xlsx, .xls），包含学号、姓名、班级等信息</div></template>
      </el-upload>
      <template #footer><el-button @click="showImportDialog = false">取消</el-button></template>
    </el-dialog>

    <!-- 编辑学生对话框 -->
    <el-dialog :title="isEditing ? '编辑学生' : '添加学生'" v-model="showEditDialog" width="500px">
      <el-form :model="studentForm" label-width="80px" :rules="rules" ref="formRef">
        <el-form-item label="学号" prop="studentNumber"><el-input v-model="studentForm.studentNumber" placeholder="请输入学号" /></el-form-item>
        <el-form-item label="姓名" prop="name"><el-input v-model="studentForm.name" placeholder="请输入姓名" /></el-form-item>
        <el-form-item label="班级" prop="className">
          <el-select v-model="studentForm.className" placeholder="请选择班级">
            <el-option v-for="classItem in classes" :key="classItem" :label="classItem" :value="classItem" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号" prop="phone"><el-input v-model="studentForm.phone" placeholder="请输入手机号" /></el-form-item>
        <el-form-item label="邮箱" prop="email"><el-input v-model="studentForm.email" placeholder="请输入邮箱" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showEditDialog = false">取消</el-button><el-button type="primary" @click="saveStudent">保存</el-button></template>
    </el-dialog>

    <!-- 学习进度弹窗 -->
    <el-dialog title="学生学习进度" v-model="showProgressDialog" width="80%" top="5vh" center>
      <div class="progress-content" v-if="selectedStudent">
        <div class="student-info"><h3>{{ selectedStudent.name }} 的学习进度</h3><p>学号: {{ selectedStudent.studentNumber || selectedStudent.studentId }} | 班级: {{ selectedStudent.className }}</p></div>
        <div class="table-container">
          <el-table :data="learningProgress" style="width: 100%" height="400">
            <el-table-column prop="courseName" label="课程名称" width="200" :style="{ textAlign: 'center' }" />
            <el-table-column prop="progress" label="学习进度" width="120" :style="{ textAlign: 'center' }">
              <template #default="scope"><el-progress :percentage="scope.row.progress" :color="progressColor(scope.row.progress)" /></template>
            </el-table-column>
            <el-table-column prop="lastStudyTime" label="最后学习时间" width="180" :style="{ textAlign: 'center' }" />
            <el-table-column prop="score" label="成绩" width="100" :style="{ textAlign: 'center' }"><template #default="scope"><span :class="{ 'excellent': scope.row.score >= 90, 'good': scope.row.score >= 80 && scope.row.score < 90 }">{{ scope.row.score || '暂无' }}</span></template></el-table-column>
            <el-table-column prop="status" label="状态" width="100" :style="{ textAlign: 'center' }"><template #default="scope"><el-tag :type="scope.row.status === 'completed' ? 'success' : 'primary'">{{ scope.row.status === 'completed' ? '已完成' : '进行中' }}</el-tag></template></el-table-column>
          </el-table>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Upload } from '@element-plus/icons-vue'
import axios from 'axios'

// === 全局统一后端地址 ===
const BASE_URL = (import.meta?.env?.VITE_API_BASE_URL || '/api')

// === 状态管理 ===
const loading = ref(false)
const searchQuery = ref('')
const classFilter = ref('')
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

const showImportDialog = ref(false)
const showEditDialog = ref(false)
const showProgressDialog = ref(false)
const isEditing = ref(false)

const fileList = ref([])
const formRef = ref()
const selectedStudent = ref(null)
const learningProgress = ref([])
const students = ref([])

const studentForm = ref({
  id: null,
  studentNumber: '',
  name: '',
  className: '',
  phone: '',
  email: ''
})

// === 表单校验规则 ===
const rules = {
  studentNumber: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  className: [{ required: true, message: '请选择班级', trigger: 'change' }]
}

// === 上传配置 ===
const uploadHeaders = ref({
  Authorization: `Bearer ${localStorage.getItem('token') || ''}`
})

const uploadStudentFile = async (options) => {
  const formData = new FormData();
  formData.append('file', options.file);

  try {
    const res = await axios.post(`${BASE_URL}/user/excel`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        'Authorization': `Bearer ${localStorage.getItem('token') || ''}`
      }
    });
    options.onSuccess(res.data); // 调用 el-upload 的成功回调
  } catch (error) {
    options.onError(error); // 调用 el-upload 的失败回调
  }
}

// === 计算属性 ===
const classes = computed(() => [
  ...new Set(students.value.map(s => s.className))
])

const filteredStudents = computed(() => {
  let result = students.value

  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(s => {
      const nm = (s.name || '').toLowerCase()
      const sn = (s.studentNumber || s.studentId || '').toLowerCase()
      return nm.includes(query) || sn.includes(query)
    })
  }

  if (classFilter.value)
    result = result.filter(s => s.className === classFilter.value)

  if (statusFilter.value)
    result = result.filter(s => s.status === statusFilter.value)

  return result
})

const paginatedStudents = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredStudents.value.slice(start, end)
})

const completedCourses = computed(() =>
  learningProgress.value.filter(c => c.status === 'completed').length
)

const averageProgress = computed(() => {
  if (!learningProgress.value.length) return 0
  const total = learningProgress.value.reduce(
    (sum, c) => sum + c.progress,
    0
  )
  return Math.round(total / learningProgress.value.length)
})

const averageScore = computed(() => {
  const arr = learningProgress.value.filter(c => c.score != null)
  if (!arr.length) return null
  const total = arr.reduce((sum, c) => sum + c.score, 0)
  return Math.round(total / arr.length)
})

const progressColor = p => {
  if (p >= 90) return '#67c23a'
  if (p >= 70) return '#409eff'
  if (p >= 50) return '#e6a23c'
  return '#f56c6c'
}

// === axios 实例 ===
const api = axios.create({
  baseURL: BASE_URL,
  timeout: 20000
})

api.interceptors.request.use(config => {
  try {
    const token =
      localStorage.getItem('token') || localStorage.getItem('userToken')
    if (token)
      config.headers = {
        ...(config.headers || {}),
        Authorization: `Bearer ${token}`
      }
  } catch {}
  return config
})

const insertStudent = payload => api.post('/teacher/insert/students', payload)
const updateStudent = (id, payload) =>
  api.put('/teacher/update/student', payload, { params: { id } })
const deleteStudentById = id =>
  api.delete('/teacher/delete/student', { params: { id } })
const listCoursesByStudent = studentId =>
  api.get('/teacher/enrollments/courses', { params: { studentId } })
const getCourseProgress = (studentId, courseId) =>
  api.get('/progress/course', { params: { studentId, courseId } })

// === 获取学生列表 ===
const fetchStudents = async () => {
  loading.value = true
  try {
    const res = await api.get('/teacher/list/students')
    console.log('学生列表', res.data)
    const body = res?.data

    students.value =
      body && Number(body.code) === 200 && Array.isArray(body.data)
        ? body.data.map(student => ({
            ...student,
            // 确保字段名正确映射
            phone: student.phone || student.phoneNumber || student.tel || '',
            email: student.email || student.mail || '',
            status: student.status || 'IN_SCHOOL' // 默认值设为在校
          }))
        : []

    try {
      await updateCourseCountsForStudents()
    } catch (e) {
      console.error(e)
    }
  } catch (e) {
    console.error('获取学生列表失败:', e)
    ElMessage.error('获取学生列表失败')
  } finally {
    loading.value = false
  }
}

// === 查看学习进度 ===
const viewLearningProgress = async student => {
  loading.value = true
  selectedStudent.value = student
  try {
    const coursesRes = await listCoursesByStudent(student.id)
    const coursesBody = coursesRes?.data
    const courses =
      coursesBody && Number(coursesBody.code) === 200 && Array.isArray(coursesBody.data)
        ? coursesBody.data
        : []

    const list = []
    for (const c of courses.slice(0, 50)) {
      try {
        const pr = await getCourseProgress(student.id, c.courseId ?? c.id)
        const pb = pr?.data
        let pct = 0
        if (pb && Number(pb.code) === 200 && pb.data) {
          pct = Number(
            pb.data.completionPercentage ?? pb.data.completion_percentage ?? 0
          )
          if (!Number.isFinite(pct)) pct = 0
          if (pct >= 0 && pct <= 1) pct *= 100
        }

        list.push({
          courseName: c.courseName ?? c.title ?? '课程',
          progress: Math.round(Math.max(0, Math.min(100, pct))),
          lastStudyTime: '',
          score: null,
          status: pct >= 100 ? 'completed' : 'learning'
        })
      } catch (e) {
        console.error(e)
      }
    }

    learningProgress.value = list
    showProgressDialog.value = true
  } catch (e) {
    console.error('获取学习进度失败:', e)
    ElMessage.error('获取学习进度失败')
  } finally {
    loading.value = false
  }
}

// === 编辑学生 ===
const editStudent = student => {
  isEditing.value = true
  studentForm.value = { ...student }
  showEditDialog.value = true
}

// === 删除学生 ===
const deleteStudent = async student => {
  try {
    await ElMessageBox.confirm(
      `确定要删除学生 "${student.name}" 吗？`,
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deleteStudentById(student.id)
    const index = students.value.findIndex(s => s.id === student.id)
    if (index !== -1) students.value.splice(index, 1)
    ElMessage.success('学生删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除学生失败:', error)
      ElMessage.error('删除学生失败')
    }
  }
}

// === 启用/锁定学生 ===
const toggleStudentStatus = async student => {
  try {
    const action = student.status === 'active' ? '锁定' : '解锁'
    await ElMessageBox.confirm(
      `确定要${action}学生 "${student.name}" 吗？`,
      `${action}确认`,
      {
        confirmButtonText: action,
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const newStatus = student.status === 'active' ? 'locked' : 'active'
    student.status = newStatus
    ElMessage.success(`学生${action}成功`)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('切换学生状态失败:', error)
      ElMessage.error('切换学生状态失败')
    }
  }
}

// === 保存学生 ===
const saveStudent = async () => {
  try {
    await formRef.value.validate()
    const payload = { ...studentForm.value }

    if (!payload.studentId && payload.studentNumber)
      payload.studentId = payload.studentNumber

    if (isEditing.value) {
      await updateStudent(studentForm.value.id, payload)
      const updatedStudent = { ...payload, id: studentForm.value.id }
      const index = students.value.findIndex(s => s.id === updatedStudent.id)
      if (index !== -1)
        students.value[index] = {
          ...students.value[index],
          ...updatedStudent
        }
      ElMessage.success('学生信息更新成功')
    } else {
      await insertStudent(payload)
      const newStudent = { ...payload, id: Date.now() }
      students.value.push(newStudent)
      ElMessage.success('学生添加成功')
    }

    showEditDialog.value = false
    resetForm()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('保存学生失败:', error)
      ElMessage.error('保存学生失败')
    }
  }
}

// === 重置表单 ===
const resetForm = () => {
  studentForm.value = {
    id: null,
    studentNumber: '',
    name: '',
    className: '',
    phone: '',
    email: ''
  }
  isEditing.value = false
}

// === 上传相关 ===
const beforeUpload = file => {
  const isExcel =
    file.type ===
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' ||
    file.type === 'application/vnd.ms-excel'
  if (!isExcel) {
    ElMessage.error('只能上传.xls或.xlsx文件!')
    return false
  }
  return true
}

const handleUploadSuccess = response => {
  if (response && Number(response.code) === 200) {
    ElMessage.success('学生导入成功')
    showImportDialog.value = false
    fileList.value = []
    fetchStudents()
  } else {
    ElMessage.error(response?.message || '学生导入失败')
  }
}

const handleUploadError = error => {
  console.error('上传失败:', error)
  ElMessage.error('学生导入失败')
}

// === 更新课程数量 ===
const updateCourseCountsForStudents = async () => {
  const limit = 5
  let idx = 0
  const workers = []

  for (let i = 0; i < Math.min(limit, students.value.length); i++) {
    workers.push(
      (async () => {
        while (true) {
          const current = idx++
          if (current >= students.value.length) break
          const s = students.value[current]
          if (!s || !s.id) continue

          try {
            const res = await listCoursesByStudent(s.id)
            const body = res?.data
            const count =
              body && Number(body.code) === 200 && Array.isArray(body.data)
                ? body.data.length
                : 0
            students.value[current] = { ...students.value[current], courseCount: count }
          } catch {
            students.value[current] = { ...students.value[current], courseCount: 0 }
          }
        }
      })()
    )
  }

  await Promise.all(workers)
}

// === 生命周期 ===
onMounted(() => {
  fetchStudents()
})
</script>



<style scoped>
.students-management { padding: 20px; background-color: #f5f7fa; min-height: calc(100vh - 40px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; padding: 16px 24px; background: white; border-radius: 8px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1); }
.page-header h2 { font-size: 24px; font-weight: 600; color: #2c3e50; margin: 0; }
.filter-bar { display: flex; align-items: center; margin-bottom: 0; padding: 16px 24px; background: white; border-radius: 8px 8px 0 0; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1); }
.table-container { background: white; padding: 0 24px; height: calc(100vh - 320px); overflow: auto; }
.pagination { margin-top: 0; padding: 16px; background: white; border-radius: 0 0 8px 8px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1); }
.upload-demo { text-align: center; }
.el-upload__tip { margin-top: 10px; color: #666; font-size: 12px; }
.progress-content { padding: 0 10px; }
.student-info { text-align: center; margin-bottom: 20px; }
.student-info h3 { margin: 0 0 8px 0; color: #2c3e50; }
.student-info p { margin: 0; color: #606266; }
.excellent { color: #67c23a; font-weight: bold; }
.good { color: #409eff; font-weight: bold; }
@media (max-width: 768px) { .filter-bar { flex-direction: column; gap: 12px; } .filter-bar .el-input { width: 100% !important; margin-right: 0 !important; } .filter-bar .el-select { width: 100% !important; margin-right: 0 !important; } .page-header { flex-direction: column; align-items: flex-start; gap: 16px; } .table-container { padding: 0 16px; height: auto; } }
</style>

