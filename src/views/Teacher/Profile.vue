<template>
  <div class="teacher-profile">
    <div class="page-header">
      <h2>个人中心</h2>
      <div class="welcome-text">管理您的个人信息和账户设置</div>
    </div>

    <div class="profile-content">
      <!-- 个人信息卡片 -->
      <el-card class="profile-card">
        <template #header>
          <div class="card-header">
            <h3>个人信息</h3>
            <el-button type="primary" :icon="Edit" @click="editDialogVisible = true">
              编辑信息
            </el-button>
          </div>
        </template>

        <div class="profile-info">
          <div class="avatar-section">
            <el-avatar :size="100" :src="teacherInfo.avatar" class="profile-avatar">
              {{ teacherInfo.name ? teacherInfo.name.charAt(0) : '' }}
            </el-avatar>
            <div class="avatar-actions">
              <el-upload action="#" :show-file-list="false" :before-upload="beforeAvatarUpload"
                         :http-request="handleAvatarUpload">
                <el-button type="primary" link>更换头像</el-button>
              </el-upload>
            </div>
          </div>

          <div class="info-details">
            <div class="info-grid">
              <div class="info-item">
                <label>姓名</label>
                <span>{{ teacherInfo.name || '未设置' }}</span>
              </div>
              <div class="info-item">
                <label>工号</label>
                <span>{{ teacherInfo.employee_number || teacherInfo.employeeNumber || '未设置' }}</span>
              </div>
              <div class="info-item">
                <label>学院</label>
                <span>{{ teacherInfo.department || '未设置' }}</span>
              </div>
              <div class="info-item">
                <label>职称</label>
                <span>{{ teacherInfo.title || '未设置' }}</span>
              </div>
              <div class="info-item">
                <label>邮箱</label>
                <span>{{ teacherInfo.email || '未设置' }}</span>
              </div>
              <div class="info-item">
                <label>手机号</label>
                <span>{{ teacherInfo.phone || '未设置' }}</span>
              </div>
            </div>

            <div class="info-row">
              <div class="info-item full-width">
                <label>个人简介</label>
                <p class="bio">{{ teacherInfo.bio || '暂无个人简介' }}</p>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 账户安全 -->
      <el-card class="security-card">
        <template #header>
          <h3>账户安全</h3>
        </template>

        <div class="security-items">
          <div class="security-item">
            <div class="security-info">
              <i class="el-icon-lock"></i>
              <div>
                <h4>登录密码</h4>
                <p>定期更改密码可以提高账户安全性</p>
              </div>
            </div>
            <el-button @click="showPasswordDialog">修改密码</el-button>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 编辑信息对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑个人信息" width="600px" :before-close="handleCloseEditDialog">
      <el-form :model="editForm" label-width="80px" ref="editFormRef">
        <el-form-item label="姓名" prop="name" :rules="[{ required: true, message: '请输入姓名', trigger: 'blur' }]">
          <el-input v-model="editForm.name" />
        </el-form-item>

        <el-form-item label="学院" prop="department" :rules="[{ required: true, message: '请输入学院', trigger: 'blur' }]">
          <el-input v-model="editForm.department" />
        </el-form-item>

        <el-form-item label="职称" prop="title" :rules="[{ required: true, message: '请选择职称', trigger: 'change' }]">
          <el-select v-model="editForm.title" placeholder="请选择职称">
            <el-option label="院长" value="院长" />
            <el-option label="副院长" value="副院长" />
            <el-option label="教授" value="教授" />
            <el-option label="副教授" value="副教授" />
            <el-option label="讲师" value="讲师" />
            <el-option label="助教" value="助教" />
          </el-select>
        </el-form-item>

        <el-form-item label="邮箱" prop="email" :rules="[
          { required: true, message: '请输入邮箱', trigger: 'blur' },
          { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
        ]">
          <el-input v-model="editForm.email" type="email" />
        </el-form-item>

        <el-form-item label="手机号" prop="phone" :rules="[
          { required: true, message: '请输入手机号', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }
        ]">
          <el-input v-model="editForm.phone" />
        </el-form-item>

        <el-form-item label="个人简介">
          <el-input v-model="editForm.bio" type="textarea" :rows="4" placeholder="请输入个人简介" />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="cancelEdit">取消</el-button>
          <el-button type="primary" @click="saveProfile" :loading="saving">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="500px" :before-close="handleClosePasswordDialog">
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px">
        <el-form-item label="当前密码" prop="currentPassword">
          <el-input v-model="passwordForm.currentPassword" type="password" show-password />
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="cancelPasswordChange">取消</el-button>
          <el-button type="primary" @click="changePassword" :loading="changingPassword">确认修改</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit } from '@element-plus/icons-vue'
import axios from 'axios'

// 动态后端基址
const API_BASE = (import.meta?.env?.VITE_API_BASE_URL || (window?.location?.port === '4173' ? 'http://localhost:9999/api' : '/api'))
// axios 实例（附加 token）
const api = axios.create({ baseURL: API_BASE, timeout: 20000 })
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token') || localStorage.getItem('userToken')
  if (token) config.headers = { ...(config.headers || {}), Authorization: `Bearer ${token}` }
  return config
})

// 教师信息
const teacherInfo = ref({})

// 编辑对话框
const editDialogVisible = ref(false)
const editForm = reactive({})
const editFormRef = ref()
const saving = ref(false)

// 密码对话框
const passwordDialogVisible = ref(false)
const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const passwordFormRef = ref()
const changingPassword = ref(false)

// 密码验证规则
const passwordRules = {
  currentPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 获取教师ID（优先登录信息）
const teacherId = computed(() => {
  try { const u = JSON.parse(localStorage.getItem('userInfo') || 'null'); if (u?.id) return Number(u.id) } catch (e) { console.error(e) }
  const id = localStorage.getItem('teacherId')
  return id ? Number(id) : null
})

// 获取教师信息（强匹配后端：优先 id -> username -> employeeNumber；命中后覆盖本地）
const fetchTeacherData = async () => {
  try {
    // 读取本地登录信息用于匹配键
    let local = null
    try { local = JSON.parse(localStorage.getItem('userInfo') || 'null') } catch (e) { console.error(e) }
    const localId = Number(local?.id) || null
    const localUsername = local?.username || null
    const localEmpNo = local?.employeeNumber || local?.employee_number || null

    const res = await api.get('/teacher/list/teachers')
    const list = Array.isArray(res?.data?.data) ? res.data.data : []

    // 多重匹配：id -> username -> employeeNumber -> teacherId 计算值 -> 首个
    let found = null
    if (localId) found = list.find(t => Number(t?.id) === localId) || null
    if (!found && localUsername) found = list.find(t => (t?.username || '').toString() === localUsername) || null
    if (!found && localEmpNo) found = list.find(t => (t?.employeeNumber || t?.employee_number || '').toString() === localEmpNo) || null
    if (!found && teacherId.value) found = list.find(t => Number(t?.id) === Number(teacherId.value)) || null
    if (!found && list.length) found = list[0]

    if (found) {
      const mapped = { ...found }
      if (mapped.employee_number && !mapped.employeeNumber) mapped.employeeNumber = mapped.employee_number
      teacherInfo.value = mapped
      try { localStorage.setItem('userInfo', JSON.stringify(mapped)) } catch (e) { console.error(e) }
      return
    }

    // 后端未返回任何教师时，回退到本地
    if (local && typeof local === 'object') {
      const mapped = { ...local }
      if (mapped.employee_number && !mapped.employeeNumber) mapped.employeeNumber = mapped.employee_number
      teacherInfo.value = mapped
    }
  } catch (error) {
    console.error('获取教师数据失败:', error)
  }
}

// 初始化编辑表单
const initEditForm = () => {
  Object.keys(teacherInfo.value).forEach(key => {
    editForm[key] = teacherInfo.value[key]
  })
}

// 保存个人信息（调用后端现有接口）
const saveProfile = async () => {
  try {
    saving.value = true
    await editFormRef.value.validate()
    if (!teacherId.value) { ElMessage.error('未获取到教师ID'); return }
    await api.put(`/teacher/update/teacher`, editForm, { params: { id: teacherId.value } })
    teacherInfo.value = { ...teacherInfo.value, ...editForm }
    try { localStorage.setItem('userInfo', JSON.stringify(teacherInfo.value)) } catch (e) { console.error(e) }
    editDialogVisible.value = false
    ElMessage.success('个人信息更新成功')
  } catch (error) {
    console.error('更新个人信息失败:', error)
    ElMessage.error('更新个人信息失败')
  } finally {
    saving.value = false
  }
}

// 取消编辑
const cancelEdit = () => {
  editDialogVisible.value = false
  initEditForm()
}

// 关闭编辑对话框前的处理
const handleCloseEditDialog = (done) => {
  cancelEdit()
  done()
}

// 显示密码对话框
const showPasswordDialog = () => {
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordDialogVisible.value = true
}

// 修改密码（若后端暂无对应接口，则暂不调用，仅前端校验）
const changePassword = async () => {
  try {
    changingPassword.value = true
    await passwordFormRef.value.validate()
    ElMessage.success('已校验表单（后端密码修改接口未接入）')
    passwordDialogVisible.value = false
  } catch (error) {
    console.error('修改密码失败:', error)
    ElMessage.error('请检查密码填写是否正确')
  } finally {
    changingPassword.value = false
  }
}

// 取消密码修改
const cancelPasswordChange = () => {
  passwordDialogVisible.value = false
  passwordFormRef.value.resetFields()
}

// 关闭密码对话框前的处理
const handleClosePasswordDialog = (done) => {
  cancelPasswordChange()
  done()
}

// 头像上传前检查
const beforeAvatarUpload = (file) => {
  const isJPGOrPNG = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPGOrPNG) {
    ElMessage.error('头像只能是 JPG 或 PNG 格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('头像大小不能超过 2MB!')
    return false
  }
  return true
}

// 处理头像上传（后端暂无接口，先提示占位）
const handleAvatarUpload = async () => {
  ElMessage.info('头像上传功能暂未接入后端')
}

// 初始化
onMounted(() => {
  fetchTeacherData().then(() => initEditForm())
})
</script>

<style scoped>
.teacher-profile {
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

.welcome-text {
  color: #6b7280;
  font-size: 16px;
}

.profile-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid #f3f4f6;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.profile-info {
  display: flex;
  gap: 30px;
  padding: 16px 0;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.profile-avatar {
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  color: white;
  font-size: 36px;
  font-weight: bold;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.info-details {
  flex: 1;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
}

.info-item label {
  font-weight: 500;
  color: #6b7280;
  margin-bottom: 6px;
  font-size: 14px;
}

.info-item span {
  font-size: 16px;
  color: #1f2937;
  font-weight: 500;
}

.info-item.full-width {
  grid-column: 1 / -1;
}

.bio {
  color: #4b5563;
  line-height: 1.6;
  margin: 0;
  padding: 12px;
  background-color: #f9fafb;
  border-radius: 6px;
  border-left: 4px solid #3b82f6;
}

.security-items {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.security-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f3f4f6;
}

.security-item:last-child {
  border-bottom: none;
}

.security-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.security-info .el-icon-lock {
  font-size: 20px;
  color: #3b82f6;
  background: #eff6ff;
  padding: 10px;
  border-radius: 8px;
}

.security-info h4 {
  margin: 0;
  font-size: 16px;
  color: #1f2937;
}

.security-info p {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .teacher-profile {
    padding: 16px;
  }

  .profile-info {
    flex-direction: column;
    align-items: center;
    text-align: center;
    gap: 20px;
  }

  .info-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .security-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .security-item .el-button {
    align-self: flex-end;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
}
</style>
