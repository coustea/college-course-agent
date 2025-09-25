<template>
  <div class="assignments-check">
    <div class="check-header">
      <div class="header-left">
        <h2>检查情况：{{ assignmentTitle }}</h2>
        <p>班级：{{ courseName }} | 截止日期：{{ formatDate(deadline) }}</p>
        <p>完成进度：{{ submittedCount }}/{{ totalGroups }} 个小组</p>
      </div>
      <div class="header-right">
        <el-button @click="$router.push('/teacher/assignments/list')">
          <i class="fas fa-arrow-left"></i> 返回列表
        </el-button>
      </div>
    </div>

    <div class="check-content">
      <!-- 统计概览 -->
      <div class="stats-overview">
        <el-row :gutter="20">
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-value">{{ submittedCount }}</div>
              <div class="stat-label">已提交</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-value">{{ totalGroups - submittedCount }}</div>
              <div class="stat-label">未提交</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-value">{{ completedCount }}</div>
              <div class="stat-label">已完成检查</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-value">{{ Math.round((submittedCount / totalGroups) * 100) }}%</div>
              <div class="stat-label">提交率</div>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 小组列表 -->
      <div class="groups-list">
        <h3>小组检查情况</h3>
        <el-table :data="groups" style="width: 100%" stripe>
          <el-table-column prop="groupName" label="小组名称" width="150" align="center" />
          <el-table-column prop="leaderName" label="组长" width="120" align="center" />
          <el-table-column prop="members" label="组员" width="200" align="center">
            <template #default="scope">
              <el-tag
                v-for="member in scope.row.members"
                :key="member"
                size="small"
                style="margin: 2px;"
              >
                {{ member }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="submitTime" label="提交时间" width="160" align="center">
            <template #default="scope">
              {{ scope.row.submitTime ? formatDateTime(scope.row.submitTime) : '未提交' }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="提交状态" width="100" align="center">
            <template #default="scope">
              <el-tag :type="getSubmitStatusType(scope.row.status)" size="small">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="checkStatus" label="检查状态" width="100" align="center">
            <template #default="scope">
              <el-tag :type="getCheckStatusType(scope.row.checkStatus)" size="small">
                {{ scope.row.checkStatus }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="score" label="评分" width="100" align="center">
            <template #default="scope">
              {{ scope.row.score !== null ? scope.row.score : '未评分' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="300" align="center">
            <template #default="scope">
              <el-button
                size="small"
                @click="viewGroupDetails(scope.row)"
                :disabled="!scope.row.submitTime"
              >
                查看详情
              </el-button>

            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 检查详情对话框 -->
      <el-dialog
        v-model="detailDialogVisible"
        :title="`${selectedGroup?.groupName} - 检查详情`"
        width="80%"
        top="50px"
        class="centered-dialog"
      >
        <div v-if="selectedGroup">
          <el-descriptions title="基本信息" border>
            <el-descriptions-item label="小组名称">{{ selectedGroup.groupName }}</el-descriptions-item>
            <el-descriptions-item label="组长">{{ selectedGroup.leaderName }}</el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ formatDateTime(selectedGroup.submitTime) }}</el-descriptions-item>
            <el-descriptions-item label="检查状态">
              <el-tag :type="getCheckStatusType(selectedGroup.checkStatus)">
                {{ selectedGroup.checkStatus }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <div class="submission-content" style="margin-top: 20px;">
            <h4>提交内容：</h4>
            <div class="content-preview">
              <p v-if="!selectedGroup.content">该小组未提交文本内容</p>
              <p v-else>{{ selectedGroup.content }}</p>
            </div>

            <h4>附件：</h4>
            <div class="attachments">
              <div v-for="(file, index) in selectedGroup.attachments" :key="index" class="attachment-item">
                <i class="fas fa-file"></i>
                <span>{{ file.name }}</span>
                <el-button link type="primary" @click="downloadFile(file)">下载</el-button>
              </div>
              <p v-if="selectedGroup.attachments.length === 0">该小组未提交附件</p>
            </div>
          </div>

          <div class="grading-form" style="margin-top: 20px;">
            <h4>检查评分</h4>
            <el-form :model="gradingForm" label-width="80px">
              <el-form-item label="得分">
                <el-input-number
                  v-model="gradingForm.score"
                  :min="0"
                  :max="100"
                  placeholder="请输入得分"
                />
                <span class="score-total">/ 100</span>
              </el-form-item>
              <el-form-item label="评语">
                <el-input
                  v-model="gradingForm.comment"
                  type="textarea"
                  :rows="4"
                  placeholder="请输入评语"
                />
              </el-form-item>
              <el-form-item label="检查结果">
                <el-radio-group v-model="gradingForm.result">
                  <el-radio label="通过">通过</el-radio>
                  <el-radio label="需修改">需修改</el-radio>
                  <el-radio label="不通过">不通过</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-form>
          </div>
        </div>

        <template #footer>
          <el-button @click="detailDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitCheck">提交检查</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const assignmentId = route.params.id

// 作业信息
const assignmentTitle = ref('期中作品进度检查')
const courseName = ref('Web前端开发班')
const deadline = ref('2023-12-15')
const submittedCount = ref(8)
const totalGroups = ref(10)

// 小组数据
const groups = ref([
  {
    id: 1,
    groupName: '第一组',
    leaderName: '张三',
    members: ['张三', '李四', '王五'],
    submitTime: '2023-12-10 14:30:25',
    status: '已提交',
    checkStatus: '已检查',
    score: 85,
    content: '我们小组已经完成了项目的基本框架搭建，主要功能模块开发完成80%。',
    attachments: [
      { name: '项目进度报告.pdf', url: '#' },
      { name: '源代码.zip', url: '#' }
    ]
  },
  {
    id: 2,
    groupName: '第二组',
    leaderName: '赵六',
    members: ['赵六', '钱七', '孙八'],
    submitTime: '2023-12-12 09:15:47',
    status: '已提交',
    checkStatus: '待检查',
    score: null,
    content: '项目完成度较高，主要功能已实现，正在进行细节优化。',
    attachments: [
      { name: '项目文档.docx', url: '#' }
    ]
  },
  {
    id: 3,
    groupName: '第三组',
    leaderName: '周九',
    members: ['周九', '吴十'],
    submitTime: null,
    status: '未提交',
    checkStatus: '未提交',
    score: null,
    content: '',
    attachments: []
  }
])

const selectedGroup = ref(null)
const detailDialogVisible = ref(false)

// 评分表单
const gradingForm = reactive({
  score: null,
  comment: '',
  result: '通过'
})

// 计算属性
const completedCount = computed(() => {
  return groups.value.filter(g => g.checkStatus === '已检查').length
})

// 方法
const formatDate = (dateString) => {
  return new Date(dateString).toLocaleDateString('zh-CN')
}

const formatDateTime = (dateTimeString) => {
  return new Date(dateTimeString).toLocaleString('zh-CN')
}

const getSubmitStatusType = (status) => {
  const statusMap = {
    '已提交': 'success',
    '未提交': 'warning',
    '迟交': 'danger'
  }
  return statusMap[status] || 'info'
}

const getCheckStatusType = (status) => {
  const statusMap = {
    '已检查': 'success',
    '待检查': 'warning',
    '未提交': 'info'
  }
  return statusMap[status] || 'info'
}

const viewGroupDetails = (group) => {
  selectedGroup.value = group
  // 初始化表单数据
  gradingForm.score = group.score
  gradingForm.comment = ''
  gradingForm.result = '通过'
  detailDialogVisible.value = true
}

const checkGroupWork = (group) => {
  viewGroupDetails(group)
}

const downloadFile = (file) => {
  ElMessage.info(`下载文件: ${file.name}`)
  // 实现文件下载逻辑
}

const submitCheck = () => {
  if (gradingForm.score === null) {
    ElMessage.error('请先输入分数')
    return
  }

  // 更新小组检查状态
  const group = groups.value.find(g => g.id === selectedGroup.value.id)
  if (group) {
    group.score = gradingForm.score
    group.checkStatus = '已检查'
    ElMessage.success('检查完成')
    detailDialogVisible.value = false
  }
}

const exportReport = () => {
  ElMessage.success('导出检查报告成功')
  // 实现导出逻辑
}

onMounted(() => {
  // 可以在这里根据assignmentId获取作业详情和小组提交情况
  console.log('检查项目ID:', assignmentId)
})
</script>

<style scoped>
.assignments-check {
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.check-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eaeaea;
}

.header-left h2 {
  margin-bottom: 8px;
  color: #1f2937;
}

.header-left p {
  color: #6b7280;
  font-size: 14px;
  margin: 4px 0;
}

.stats-overview {
  margin-bottom: 30px;
}

.stat-card {
  background: #f8fafc;
  padding: 20px;
  border-radius: 8px;
  text-align: center;
  border-left: 4px solid #3b82f6;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #1f2937;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
}

.groups-list h3 {
  margin-bottom: 15px;
  color: #1f2937;
}

.content-preview {
  background: #f9fafb;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
  min-height: 60px;
}

.attachment-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f3f4f6;
}

.attachment-item i {
  margin-right: 10px;
  color: #6b7280;
}

.attachment-item span {
  flex: 1;
}

.grading-form {
  background: #f9fafb;
  padding: 20px;
  border-radius: 8px;
}

.grading-form h4 {
  margin-bottom: 15px;
  color: #374151;
}

.score-total {
  margin-left: 10px;
  color: #6b7280;
}

:deep(.centered-dialog .el-dialog) {
  margin: 0 auto !important;
  max-height: calc(100vh - 100px);
  display: flex;
  flex-direction: column;
}

:deep(.centered-dialog .el-dialog__body) {
  flex: 1;
  overflow-y: auto;
}
</style>
