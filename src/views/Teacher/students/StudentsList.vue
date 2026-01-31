<template>
  <div class="students-management">
    <!-- Page Header -->
    <div class="page-header">
      <div class="header-content">
        <div class="header-left">
          <h2>
            <el-icon><User /></el-icon>
            学生管理
          </h2>
          <p class="subtitle">管理和查看所有学生信息与学习进度</p>
        </div>
        <div class="header-right">
          <el-button type="primary" @click="openAddStudent" size="large">
            <el-icon><Plus /></el-icon>
            添加学生
          </el-button>
          <el-button @click="showImportDialog = true" size="large">
            <el-icon><Upload /></el-icon>
            批量导入
          </el-button>
        </div>
      </div>
    </div>

    <!-- Statistics Cards -->
    <div class="stats-container">
      <div class="stat-card">
        <div class="stat-icon stat-icon-primary">
          <el-icon><User /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ students.length }}</div>
          <div class="stat-label">总学生数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon-success">
          <el-icon><Reading /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ inSchoolCount }}</div>
          <div class="stat-label">在校生</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon-warning">
          <el-icon><Briefcase /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ internshipCount }}</div>
          <div class="stat-label">实习生</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon-info">
          <el-icon><DataAnalysis /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ classes.length }}</div>
          <div class="stat-label">班级数</div>
        </div>
      </div>
    </div>

    <!-- Filter Bar -->
    <div class="filter-bar">
      <div class="filter-left">
        <el-input
          v-model="searchQuery"
          placeholder="搜索学生姓名或学号..."
          clearable
          size="large"
          style="width: 320px;"
          @keyup.enter="fetchStudents"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select
          v-model="classFilter"
          placeholder="班级筛选"
          clearable
          size="large"
          style="width: 180px; margin-left: 12px;"
          @change="handleFilterChange"
        >
          <el-option v-for="classItem in classes" :key="classItem" :label="classItem" :value="classItem">
            <span>{{ classItem }}</span>
            <el-tag size="small" type="info" style="margin-left: 8px;">
              {{ getStudentCountByClass(classItem) }}
            </el-tag>
          </el-option>
        </el-select>
        <el-select
          v-model="statusFilter"
          placeholder="状态筛选"
          clearable
          size="large"
          style="width: 140px; margin-left: 12px;"
          @change="handleFilterChange"
        >
          <el-option label="全部" value="">
            <div style="display: flex; align-items: center; gap: 8px;">
              <span>全部</span>
              <el-tag size="small" type="info">{{ students.length }}</el-tag>
            </div>
          </el-option>
          <el-option label="在校" value="IN_SCHOOL">
            <div style="display: flex; align-items: center; gap: 8px;">
              <span>在校</span>
              <el-tag size="small" type="success">{{ inSchoolCount }}</el-tag>
            </div>
          </el-option>
          <el-option label="校外实习" value="OFF_CAMPUS_INTERNSHIP">
            <div style="display: flex; align-items: center; gap: 8px;">
              <span>校外实习</span>
              <el-tag size="small" type="warning">{{ internshipCount }}</el-tag>
            </div>
          </el-option>
        </el-select>
      </div>
      <div class="filter-right">
        <el-button type="primary" @click="fetchStudents" size="large">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button @click="resetFilters" size="large">
          <el-icon><RefreshLeft /></el-icon>
          重置
        </el-button>
      </div>
    </div>

    <!-- Table Container -->
    <div class="table-container">
      <el-table
        :data="paginatedStudents"
        style="width: 100%"
        v-loading="loading"
        height="100%"
        :header-cell-style="{
          background: '#fafbfc',
          color: '#5c6b77',
          fontWeight: '600',
          fontSize: '14px'
        }"
        :row-style="{ height: '64px' }"
        :row-class-name="tableRowClassName"
        stripe
        @sort-change="handleSortChange"
      >
        <el-table-column type="index" label="序号" width="80" align="center" fixed>
          <template #default="{ $index }">
            <span class="index-number">
              {{ (currentPage - 1) * pageSize + $index + 1 }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="studentNumber" label="学号" min-width="120" align="center" sortable show-overflow-tooltip>
          <template #default="{ row }">
            <span class="table-text">{{ row.studentNumber || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="name" label="姓名" min-width="120" align="center" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="table-text">{{ row.name }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="className" label="班级" min-width="120" align="center" sortable show-overflow-tooltip>
          <template #default="{ row }">
            <span class="table-text">{{ row.className || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="phone" label="联系方式" min-width="160" align="center" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="contact-info-simple">
              <div v-if="row.phone">{{ row.phone }}</div>
              <div v-if="row.email" class="email-simple">{{ row.email }}</div>
              <span v-if="!row.phone && !row.email" style="color: #c0c4cc;">-</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" min-width="100" align="center" sortable>
          <template #default="{ row }">
            <el-tag
              :type="row.status === 'IN_SCHOOL' ? 'success' : 'warning'"
              effect="plain"
              size="default"
            >
              {{ row.status === 'IN_SCHOOL' ? '在校' : '校外实习' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" link size="small" @click="openDetailDialog(row)">
                详情
              </el-button>
              <el-button type="success" link size="small" @click="openProgressDialog(row)">
                进度
              </el-button>
              <el-button type="warning" link size="small" @click="editStudent(row)">
                编辑
              </el-button>
              <el-button type="danger" link size="small" @click="deleteStudent(row)">
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Pagination -->
    <div class="pagination-container" v-if="filteredStudents.length > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="filteredStudents.length"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- Empty State -->
    <div v-else class="empty-state">
      <el-empty description="暂无学生数据" :image-size="200">
        <el-button type="primary" @click="openAddStudent">添加第一个学生</el-button>
      </el-empty>
    </div>

    <!-- Import Dialog -->
    <el-dialog
      v-model="showImportDialog"
      title="批量导入学生"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="import-content">
        <el-alert
          title="导入说明"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 20px;"
        >
          <template #default>
            <ul style="margin: 8px 0; padding-left: 20px; line-height: 1.8;">
              <li>支持上传 <strong>.xlsx</strong> 或 <strong>.xls</strong> 格式的 Excel 文件</li>
              <li>文件需包含学号、姓名、班级等必要信息</li>
              <li>学号不能重复，重复的学号将被跳过</li>
              <li>单个文件大小不超过 <strong>10MB</strong></li>
            </ul>
          </template>
        </el-alert>

        <el-upload
          class="upload-demo"
          drag
          :http-request="uploadStudentFile"
          :before-upload="beforeUpload"
          :on-success="handleUploadSuccess"
          :on-error="handleUploadError"
          :file-list="fileList"
          :headers="uploadHeaders"
          accept=".xlsx,.xls"
          :auto-upload="false"
          ref="uploadRef"
          :limit="1"
          :on-exceed="handleExceed"
        >
          <el-icon class="el-icon--upload" style="font-size: 67px; color: #409eff;"><UploadFilled /></el-icon>
          <div class="el-upload__text" style="margin-top: 16px;">
            将文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              只能上传 .xlsx 或 .xls 文件，且不超过 10MB
            </div>
          </template>
        </el-upload>
      </div>
      <template #footer>
        <el-button @click="showImportDialog = false">取消</el-button>
        <el-button type="primary" @click="submitUpload" :loading="uploading" :disabled="fileList.length === 0">
          <el-icon v-if="!uploading"><Upload /></el-icon>
          {{ uploading ? '导入中...' : '开始导入' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- Edit/Add Student Dialog -->
    <el-dialog
      v-model="showEditDialog"
      :title="isEditing ? '编辑学生信息' : '添加学生'"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form
        :model="studentForm"
        label-width="100px"
        :rules="rules"
        ref="formRef"
        label-position="right"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="学号" prop="studentNumber">
              <el-input
                v-model="studentForm.studentNumber"
                placeholder="请输入学号"
                :prefix-icon="Postcard"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input
                v-model="studentForm.name"
                placeholder="请输入姓名"
                :prefix-icon="User"
                clearable
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="班级" prop="className">
              <el-select
                v-model="studentForm.className"
                placeholder="请选择班级"
                style="width: 100%"
                filterable
                allow-create
                clearable
              >
                <el-option
                  v-for="classItem in classes"
                  :key="classItem"
                  :label="classItem"
                  :value="classItem"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="studentForm.status" placeholder="请选择状态" style="width: 100%" clearable>
                <el-option label="在校" value="IN_SCHOOL">
                  <div style="display: flex; align-items: center; gap: 8px;">
                    <el-tag type="success" effect="dark" size="small">在校</el-tag>
                    <el-icon color="#67c23a"><School /></el-icon>
                  </div>
                </el-option>
                <el-option label="校外实习" value="OFF_CAMPUS_INTERNSHIP">
                  <div style="display: flex; align-items: center; gap: 8px;">
                    <el-tag type="warning" effect="dark" size="small">实习</el-tag>
                    <el-icon color="#e6a23c"><Briefcase /></el-icon>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input
                v-model="studentForm.phone"
                placeholder="请输入手机号"
                :prefix-icon="Phone"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input
                v-model="studentForm.email"
                placeholder="请输入邮箱"
                :prefix-icon="Message"
                clearable
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="saveStudent" :loading="saving">
          <el-icon v-if="!saving"><Check /></el-icon>
          {{ saving ? '保存中...' : '保存' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- Learning Progress Dialog -->
    <el-dialog
      v-model="showProgressDialog"
      title="学生学习进度详情"
      width="90%"
      top="5vh"
      :close-on-click-modal="false"
    >
      <div class="progress-content" v-if="selectedStudent">
        <div class="student-profile">
          <el-avatar :size="72" :icon="UserFilled" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); flex-shrink: 0;" />
          <div class="profile-info">
            <h3>{{ selectedStudent.name }} 的学习进度</h3>
            <p class="profile-meta">
              <el-tag type="info" effect="plain">{{ selectedStudent.studentNumber }}</el-tag>
              <el-tag type="primary" effect="plain">{{ selectedStudent.className }}</el-tag>
            </p>
          </div>
        </div>

        <el-divider />

        <div class="course-selector-section">
          <div class="selector-title">
            <el-icon><Reading /></el-icon>
            选择课程查看进度
          </div>
          <el-select
            v-model="currentCourseId"
            placeholder="请选择课程查看详细进度"
            style="min-width: 320px;"
            size="large"
            @change="onCourseChange"
            clearable
          >
            <el-option
              v-for="c in courseOptions"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>

          <div v-if="currentCourseId" class="progress-overview">
            <div class="overview-item">
              <span class="overview-label">总体进度</span>
              <el-progress
                v-if="overallProgressPct >= 0"
                :percentage="overallProgressPct"
                :color="getProgressColor(overallProgressPct)"
                :stroke-width="18"
                style="width: 240px;"
              />
              <el-tag v-else type="info">暂无数据</el-tag>
            </div>

            <div class="overview-item">
              <span class="overview-label">答题正确率</span>
              <el-tag
                v-if="examAccuracy >= 0"
                :type="examAccuracy >= 80 ? 'success' : (examAccuracy >= 60 ? 'warning' : 'danger')"
                size="large"
                effect="dark"
              >
                {{ examAccuracy.toFixed(1) }}%
              </el-tag>
              <el-tag v-else type="info" size="large">暂无数据</el-tag>
            </div>
          </div>
        </div>

        <div v-if="currentCourseId" class="progress-tables">
          <el-card class="progress-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <el-icon color="#409eff" style="font-size: 20px;"><VideoPlay /></el-icon>
                <span>视频学习进度</span>
                <el-tag type="info" effect="plain" size="small">
                  {{ videoProgressList.length }} 个视频
                </el-tag>
              </div>
            </template>
            <el-table
              :data="videoProgressList"
              style="width: 100%"
              max-height="320"
              :empty-text="'暂无视频数据'"
            >
              <el-table-column type="index" label="#" width="60" align="center" />
              <el-table-column prop="title" label="视频标题" min-width="280" />
              <el-table-column label="观看进度" width="220" align="center">
                <template #default="{ row }">
                  <el-progress
                    :percentage="Math.round(row.percentage || 0)"
                    :color="getProgressColor(row.percentage || 0)"
                    :stroke-width="12"
                  />
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.completed ? 'success' : 'info'" effect="dark">
                    {{ row.completed ? '已完成' : '未完成' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="updatedAt" label="最后学习时间" width="180" align="center" />
            </el-table>
          </el-card>

          <el-card class="progress-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <el-icon color="#67c23a" style="font-size: 20px;"><Document /></el-icon>
                <span>文档阅读进度</span>
                <el-tag type="info" effect="plain" size="small">
                  {{ documentProgressList.length }} 个文档
                </el-tag>
              </div>
            </template>
            <el-table
              :data="documentProgressList"
              style="width: 100%"
              max-height="320"
              :empty-text="'暂无文档数据'"
            >
              <el-table-column type="index" label="#" width="60" align="center" />
              <el-table-column prop="title" label="文档标题" min-width="280" />
              <el-table-column label="阅读状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.completed ? 'success' : 'info'" effect="dark">
                    {{ row.completed ? '已读完' : '未读完' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="updatedAt" label="最后阅读时间" width="180" align="center" />
            </el-table>
          </el-card>
        </div>

        <el-empty v-else description="请选择课程查看学习进度" :image-size="200" />
      </div>
    </el-dialog>

    <!-- Student Detail Dialog -->
    <el-dialog
      v-model="showDetailDialog"
      title="学生详细信息"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="detail-content" v-if="selectedStudent">
        <div class="detail-header">
          <el-avatar :size="80" :icon="UserFilled" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);" />
          <div class="detail-info">
            <h2>{{ selectedStudent.name }}</h2>
            <el-tag type="info">{{ selectedStudent.studentNumber }}</el-tag>
            <el-tag type="primary" style="margin-left: 8px;">{{ selectedStudent.className }}</el-tag>
          </div>
        </div>
        <el-divider />
        <el-descriptions :column="1" border>
          <el-descriptions-item label="学号">{{ selectedStudent.studentNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ selectedStudent.name }}</el-descriptions-item>
          <el-descriptions-item label="班级">{{ selectedStudent.className || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ selectedStudent.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ selectedStudent.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="selectedStudent.status === 'IN_SCHOOL' ? 'success' : 'warning'" effect="dark">
              {{ selectedStudent.status === 'IN_SCHOOL' ? '在校' : '校外实习' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="showDetailDialog = false">关闭</el-button>
        <el-button type="primary" @click="showDetailDialog = false; editStudent(selectedStudent)">
          <el-icon><Edit /></el-icon>
          编辑信息
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Upload,
  User,
  Plus,
  UserFilled,
  Phone,
  Message,
  Edit,
  Delete,
  RefreshLeft,
  TrendCharts,
  Postcard,
  Check,
  Reading,
  Briefcase,
  DataAnalysis,
  School,
  VideoPlay,
  Document,
  UploadFilled,
  View
} from '@element-plus/icons-vue'
import axios from 'axios'

// API Configuration
const BASE_URL = (import.meta?.env?.VITE_API_BASE_URL || '/api')

// State
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const searchQuery = ref('')
const classFilter = ref('')
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const students = ref([])
const showImportDialog = ref(false)
const showEditDialog = ref(false)
const showProgressDialog = ref(false)
const showDetailDialog = ref(false)
const isEditing = ref(false)
const fileList = ref([])
const formRef = ref()
const uploadRef = ref()
const selectedStudent = ref(null)
const courseOptions = ref([])
const currentCourseId = ref(null)
const overallProgressPct = ref(-1)
const examAccuracy = ref(-1)
const videoProgressList = ref([])
const documentProgressList = ref([])

const studentForm = ref({
  id: null,
  studentNumber: '',
  name: '',
  className: '',
  phone: '',
  email: '',
  status: 'IN_SCHOOL'
})

const rules = {
  studentNumber: [
    { required: true, message: '请输入学号', trigger: 'blur' },
    { min: 3, max: 20, message: '学号长度应在 3-20 个字符', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 50, message: '姓名长度应在 2-50 个字符', trigger: 'blur' }
  ],
  className: [
    { required: true, message: '请选择班级', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

const uploadHeaders = ref({
  Authorization: `Bearer ${localStorage.getItem('token') || ''}`
})

// Computed Properties
const classes = computed(() => [
  ...new Set(students.value.map(s => s.className).filter(Boolean))
])

const inSchoolCount = computed(() =>
  students.value.filter(s => s.status === 'IN_SCHOOL').length
)

const internshipCount = computed(() =>
  students.value.filter(s => s.status === 'OFF_CAMPUS_INTERNSHIP').length
)

const filteredStudents = computed(() => {
  let result = students.value

  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(s => {
      const nm = (s.name || '').toLowerCase()
      const sn = (s.studentNumber || '').toLowerCase()
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

// API Configuration
const api = axios.create({
  baseURL: BASE_URL,
  timeout: 20000
})

api.interceptors.request.use(config => {
  try {
    const token = localStorage.getItem('token') || localStorage.getItem('userToken')
    if (token)
      config.headers = {
        ...(config.headers || {}),
        Authorization: `Bearer ${token}`
      }
  } catch {}
  return config
})

// Methods
const getStudentCountByClass = (className) => {
  return students.value.filter(s => s.className === className).length
}

const resetFilters = () => {
  searchQuery.value = ''
  classFilter.value = ''
  statusFilter.value = ''
  currentPage.value = 1
  fetchStudents()
}

const handleFilterChange = () => {
  currentPage.value = 1
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
}

const handleCurrentChange = (val) => {
  currentPage.value = val
}

const handleSortChange = ({ prop, order }) => {
  // Implement sorting logic if needed
  console.log('Sort changed:', prop, order)
}

const tableRowClassName = ({ rowIndex }) => {
  return rowIndex % 2 === 0 ? 'even-row' : 'odd-row'
}

const openAddStudent = () => {
  isEditing.value = false
  studentForm.value = {
    id: null,
    studentNumber: '',
    name: '',
    className: '',
    phone: '',
    email: '',
    status: 'IN_SCHOOL'
  }
  showEditDialog.value = true
}

const openDetailDialog = (student) => {
  selectedStudent.value = student
  showDetailDialog.value = true
}

const fetchStudents = async () => {
  loading.value = true
  try {
    const res = await api.get('/teacher/list/students')
    const body = res?.data

    if (body && Number(body.code) === 200 && Array.isArray(body.data)) {
      students.value = body.data.map(student => ({
        ...student,
        phone: student.phone || '',
        email: student.email || '',
        status: student.status || 'IN_SCHOOL'
      }))
      ElMessage.success('学生列表加载成功')
    } else {
      students.value = []
      ElMessage.warning('暂无学生数据')
    }
  } catch (e) {
    console.error('获取学生列表失败:', e)
    ElMessage.error('获取学生列表失败')
  } finally {
    loading.value = false
  }
}

const editStudent = student => {
  isEditing.value = true
  studentForm.value = { ...student }
  showEditDialog.value = true
}

const deleteStudent = async student => {
  try {
    await ElMessageBox.confirm(
      `确定要删除学生 "${student.name}" 吗？此操作不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )

    loading.value = true
    const res = await api.delete('/teacher/delete/student', {
      params: { id: student.id }
    })

    if (res.data && Number(res.data.code) === 200) {
      ElMessage.success('学生删除成功')
      await fetchStudents()
    } else {
      throw new Error(res.data?.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除学生失败:', error)
      ElMessage.error(error.message || '删除学生失败')
    }
  } finally {
    loading.value = false
  }
}

const saveStudent = async () => {
  try {
    await formRef.value.validate()
    saving.value = true

    const payload = { ...studentForm.value }

    // Ensure studentId is set if studentNumber exists
    if (!payload.studentId && payload.studentNumber) {
      payload.studentId = payload.studentNumber
    }

    let res
    if (isEditing.value) {
      res = await api.put('/teacher/update/student', payload, {
        params: { id: studentForm.value.id }
      })
    } else {
      res = await api.post('/teacher/insert/students', payload)
    }

    if (res.data && Number(res.data.code) === 200) {
      ElMessage.success(isEditing.value ? '学生信息更新成功' : '学生添加成功')
      showEditDialog.value = false
      await fetchStudents()
    } else {
      throw new Error(res.data?.message || '保存失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('保存学生失败:', error)
      ElMessage.error(error.message || '保存学生失败')
    }
  } finally {
    saving.value = false
  }
}

const beforeUpload = file => {
  const isExcel = file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' ||
    file.type === 'application/vnd.ms-excel'
  if (!isExcel) {
    ElMessage.error('只能上传 .xls 或 .xlsx 文件!')
    return false
  }
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB!')
    return false
  }
  return true
}

const uploadStudentFile = async (options) => {
  const formData = new FormData()
  formData.append('file', options.file)

  try {
    uploading.value = true
    const res = await axios.post(`${BASE_URL}/user/excel`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        'Authorization': `Bearer ${localStorage.getItem('token') || ''}`
      }
    })
    options.onSuccess(res.data)
  } catch (error) {
    options.onError(error)
  } finally {
    uploading.value = false
  }
}

const submitUpload = () => {
  uploadRef.value?.submit()
}

const handleExceed = () => {
  ElMessage.warning('只能上传一个文件')
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

const getProgressColor = p => {
  if (p >= 90) return '#67c23a'
  if (p >= 70) return '#409eff'
  if (p >= 50) return '#e6a23c'
  return '#f56c6c'
}

const openProgressDialog = async (student) => {
  loading.value = true
  selectedStudent.value = student
  try {
    const coursesRes = await api.get('/teacher/enrollments/courses', {
      params: { studentId: student.id }
    })
    const coursesBody = coursesRes?.data

    if (coursesBody && Number(coursesBody.code) === 200 && Array.isArray(coursesBody.data)) {
      courseOptions.value = coursesBody.data.map(c => ({
        id: c.courseId ?? c.id,
        name: c.courseName ?? c.title ?? `课程 ${c.courseId ?? c.id}`
      }))

      // Auto-select first course if available
      if (courseOptions.value.length > 0) {
        currentCourseId.value = courseOptions.value[0].id
        await refreshCourseProgress()
      }
    }

    showProgressDialog.value = true
  } catch (e) {
    console.error('获取学习进度失败:', e)
    ElMessage.error('获取学习进度失败')
  } finally {
    loading.value = false
  }
}

const onCourseChange = async () => {
  await refreshCourseProgress()
}

const refreshCourseProgress = async () => {
  videoProgressList.value = []
  documentProgressList.value = []
  overallProgressPct.value = -1
  examAccuracy.value = -1

  const sid = selectedStudent.value?.id
  const cid = currentCourseId.value
  if (!sid || !cid) return

  try {
    // Fetch all data in parallel for better performance
    const [allRes, vidsRes, docsRes, accRes] = await Promise.all([
      api.get('/progress/course/all', { params: { studentId: sid, courseId: cid } }),
      api.get('/course/video/list', { params: { courseId: cid } }),
      api.get('/course/document/list', { params: { courseId: cid } }),
      api.get('/aiexam/accuracy', { params: { studentId: sid, courseId: cid } })
    ])

    // Process overall progress
    const body = allRes?.data
    if (body && Number(body.code) === 200 && body.data) {
      const d = body.data
      const pct = Number(d?.coursePercent ?? d?.completionPercentage ?? 0)
      overallProgressPct.value = Math.round(Math.max(0, Math.min(100, Number.isFinite(pct) ? pct : 0)))

      const progressVideos = Array.isArray(d?.videos) ? d.videos : []
      const progressDocs = Array.isArray(d?.documents) ? d.documents : []

      const allVideos = vidsRes?.data?.code === 200 ? vidsRes.data.data : []
      const allDocs = docsRes?.data?.code === 200 ? docsRes.data.data : []

      // Merge video progress
      const vpMap = new Map(progressVideos.map(v => [(v.videoId ?? v.video_id ?? v.id), v]))
      videoProgressList.value = (allVideos || []).map((v, i) => {
        const id = v.videoId ?? v.id
        const pv = vpMap.get(id)
        const fallback = `第${i + 1}节`
        if (pv) return { ...pv, title: (pv.videoTitle || v.videoTitle || v.title || fallback) }
        return {
          videoId: id,
          title: (v.videoTitle || v.title || fallback),
          percentage: 0,
          completed: false,
          updatedAt: '-'
        }
      })

      // Merge document progress
      const dpMap = new Map(progressDocs.map(v => [(v.documentId ?? v.document_id ?? v.id), v]))
      documentProgressList.value = (allDocs || []).map((d0, i) => {
        const id = d0.documentId ?? d0.id
        const pd = dpMap.get(id)
        const fallback = `第${i + 1}节`
        if (pd) return { ...pd, title: (pd.docTitle || d0.docTitle || d0.title || fallback) }
        return {
          documentId: id,
          title: (d0.docTitle || d0.title || fallback),
          percentage: 0,
          completed: false,
          updatedAt: '-'
        }
      })
    }

    // Process exam accuracy
    const accBody = accRes?.data
    if (accBody && Number(accBody.code) === 200 && accBody.data) {
      let pct = 0
      if (accBody.data.percentage != null) {
        pct = Number(accBody.data.percentage)
      } else if (accBody.data.accuracy != null) {
        pct = Number(accBody.data.accuracy) * 100
      }
      if (Number.isFinite(pct)) {
        examAccuracy.value = Math.max(0, Math.min(100, pct))
      }
    }
  } catch (e) {
    console.error('获取课程进度失败:', e)
    ElMessage.error('获取课程进度失败')
  }
}

// Lifecycle
onMounted(() => {
  fetchStudents()
})
</script>

<style scoped>
.students-management {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 48px);
}

/* Page Header - Simplified */
.page-header {
  margin-bottom: 20px;
}

.header-content {
  background: white;
  border-radius: 8px;
  padding: 20px 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left h2 {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 6px 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-left .subtitle {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

.header-right {
  display: flex;
  gap: 8px;
}

/* Statistics Cards - Simplified */
.stats-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.2s ease;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  flex-shrink: 0;
}

.stat-icon-primary {
  background: #409eff;
}

.stat-icon-success {
  background: #67c23a;
}

.stat-icon-warning {
  background: #e6a23c;
}

.stat-icon-info {
  background: #909399;
}

.stat-content {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  font-weight: 400;
}

/* Filter Bar - Simplified */
.filter-bar {
  background: white;
  border-radius: 8px;
  padding: 16px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.filter-left {
  display: flex;
  align-items: center;
  flex: 1;
  gap: 12px;
}

.filter-right {
  display: flex;
  gap: 8px;
}

/* Table Container */
.table-container {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  height: calc(100vh - 480px);
  overflow: auto;
}

/* Enhanced Table Styles - Simplified */
:deep(.el-table) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.el-table__body-wrapper) {
  border-radius: 0 0 8px 8px;
}

:deep(.el-table tr) {
  transition: background-color 0.2s ease;
}

:deep(.el-table__row) {
  cursor: pointer;
}

:deep(.el-table__row:hover) {
  background-color: #f5f7fa !important;
}

:deep(.el-table__row.even-row) {
  background-color: #ffffff;
}

:deep(.el-table__row.odd-row) {
  background-color: #fafbfc;
}

:deep(.el-table td) {
  border-bottom: 1px solid #ebeef5;
}

:deep(.el-table th) {
  border-bottom: 2px solid #e4e7ed;
}

.index-number {
  font-weight: 600;
  color: #606266;
  font-size: 14px;
}

.table-text {
  color: #606266;
  font-size: 14px;
}

.contact-info-simple {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.contact-info-simple > div {
  margin-bottom: 2px;
}

.email-simple {
  font-size: 12px;
  color: #909399;
}

/* Action Buttons - Simplified */
.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.action-buttons .el-button {
  padding: 4px 8px;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.action-buttons .el-button:hover {
  opacity: 0.8;
}

/* Pagination - Simplified */
.pagination-container {
  background: white;
  border-radius: 8px;
  padding: 16px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

/* Empty State - Simplified */
.empty-state {
  background: white;
  border-radius: 8px;
  padding: 40px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-top: 16px;
}

/* Import Dialog */
.import-content {
  padding: 0 10px;
}

/* Progress Content */
.progress-content {
  padding: 0 10px;
}

.student-profile {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 24px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 16px;
  margin-bottom: 24px;
}

.profile-info h3 {
  margin: 0 0 8px 0;
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
}

.profile-meta {
  margin: 0;
  display: flex;
  gap: 8px;
}

.course-selector-section {
  padding: 20px 24px;
  background: #f8f9fa;
  border-radius: 12px;
  margin-bottom: 24px;
}

.selector-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 15px;
  margin-bottom: 16px;
}

.progress-overview {
  display: flex;
  align-items: center;
  gap: 32px;
  margin-top: 16px;
}

.overview-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.overview-label {
  font-weight: 600;
  color: #475569;
  font-size: 14px;
}

.progress-tables {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
}

.progress-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  font-size: 16px;
  color: #1a1a1a;
}

/* Detail Dialog */
.detail-header {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 12px;
  margin-bottom: 24px;
}

.detail-info h2 {
  margin: 0 0 12px 0;
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
}

.detail-meta {
  margin: 0;
  display: flex;
  gap: 8px;
}

/* Responsive Design */
@media (max-width: 1200px) {
  .stats-container {
    grid-template-columns: repeat(2, 1fr);
  }

  .filter-bar {
    flex-direction: column;
    gap: 16px;
  }

  .filter-left {
    width: 100%;
    flex-wrap: wrap;
  }

  .filter-right {
    width: 100%;
    margin-left: 0;
  }

  .filter-left .el-input,
  .filter-left .el-select {
    flex: 1;
    min-width: 200px;
    margin-left: 0 !important;
  }
}

@media (max-width: 768px) {
  .students-management {
    padding: 16px;
  }

  .header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 20px;
    padding: 24px;
  }

  .header-right {
    width: 100%;
    flex-direction: column;
  }

  .header-right .el-button {
    width: 100%;
  }

  .stats-container {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .stat-card {
    padding: 20px;
  }

  .table-container {
    padding: 16px;
    height: auto;
  }

  .filter-bar {
    padding: 16px;
  }

  .course-selector-section {
    padding: 16px;
  }

  .progress-overview {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .student-profile {
    flex-direction: column;
    text-align: center;
  }
}
</style>
