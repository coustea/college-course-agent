<template>
  <div class="student-layout">
    <div class="sidebar">
      <div class="sidebar-content">
        <div class="sidebar-header">
          <h1>课程思政学习平台</h1>
        </div>

        <ul class="menu">
          <li class="menu-item">
            <div class="menu-title" :class="{active: $route.path === '/home'}"
                 @click="navigateTo('/home')">
              <div>
                <i class="fas fa-home"></i>
                <span>首页</span>
              </div>
            </div>
          </li>

<!--          <li class="menu-item">-->
<!--            <div class="menu-title" :class="{active: $route.path === '/data'}"-->
<!--                 @click="navigateTo('/data')">-->
<!--              <div>-->
<!--                <i class="fas fa-chart-line"></i>-->
<!--                <span>学习数据</span>-->
<!--              </div>-->
<!--            </div>-->
<!--          </li>目前不用-->

          <li class="menu-item">
            <div class="menu-title" :class="{active: $route.path === '/group'}"
                 @click="navigateTo('/group')">
              <div>
                <i class="fas fa-users"></i>
                <span>学习分组</span>
              </div>
            </div>
          </li>

          <li class="menu-item">
            <div class="menu-title" :class="{active: $route.path === '/work'}"
                 @click="navigateTo('/work')">
              <div>
                <i class="fas fa-file"></i>
                <span>作品提交</span>
              </div>
            </div>
          </li>

<!--          <li class="menu-item">-->
<!--            <div class="menu-title" :class="{active: $route.path === '/profile'}"-->
<!--                 @click="navigateTo('/profile')">-->
<!--              <div>-->
<!--                <i class="fas fa-user"></i>-->
<!--                <span>个人中心</span>-->
<!--              </div>-->
<!--            </div>-->
<!--          </li>-->
        </ul>
      </div>
      <div class="user-panel">
        <div class="user-info" @click="toggleUserMenu">
          <div>
            <div class="user-avatar">{{ avatar }}</div>
            <span>{{ userName }}</span>
          </div>
          <i class="fas fa-chevron-up" :class="{active: showUserMenu}"></i>
        </div>

        <ul class="user-dropdown" :class="{show: showUserMenu}">
          <li class="dropdown-item" @click="openProfileDialog">
            <i class="fas fa-user"></i>
            <span>个人资料</span>
          </li>
          <li class="dropdown-item" @click="openPasswordDialog">
            <i class="fas fa-key"></i>
            <span>修改密码</span>
          </li>
          <li class="dropdown-item" @click="logout">
            <i class="fas fa-sign-out-alt"></i>
            <span>退出登录</span>
          </li>

        </ul>
      </div>
    </div>
    <div class="main-content">
      <router-view></router-view>
    </div>
    <!-- 修改密码 -->
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

    <!-- 个人资料 -->
    <el-dialog v-model="profileDialogVisible" title="个人资料" width="620px" :before-close="handleCloseProfileDialog">
      <el-descriptions :column="2" border style="margin-bottom: 12px;">
        <el-descriptions-item label="姓名">{{ profileForm.name || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="学号">{{ profileForm.studentNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="班级" :span="2">{{ profileForm.className || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-form :model="profileForm" :rules="profileRules" ref="profileFormRef" label-width="90px">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="cancelProfileEdit">取消</el-button>
          <el-button type="primary" :loading="savingProfile" @click="updateStudentContact(profileForm.phone, profileForm.email)">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted,getCurrentInstance,onActivated, provide} from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter, useRoute,onBeforeRouteUpdate} from 'vue-router'
import axios from "axios"

const router = useRouter()
const route = useRoute()

const {proxy} = getCurrentInstance()
const BASE_URL = proxy.$baseUrl

const isSubMenuOpen = ref(false)
const showUserMenu = ref(false)
const userName = ref('未登录')
const avatar = computed(() => {
  const name = userName.value || ''
  return name ? name[name.length - 1] : '访'
})

const profileDialogVisible = ref(false)
const profileFormRef = ref()
const profileForm = ref({ name: '', className: '', studentNumber: '', phone: '', email: '' })
const savingProfile = ref(false)

const getStudentById = async () => {
  console.log('getStudentById called')
  try {
    const userId = localStorage.getItem('userId')
    console.log(userId)
    const res = await axios.post(`${BASE_URL}/student/by-id`, { userId }, {
      headers: {
        'content-type': 'multipart/form-data',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    console.log(res.data)
    if (res.data.code === 200) {
      console.log(res.data.data)
      userName.value = res.data.data.name
      localStorage.setItem('studentName', res.data.data.name)
      localStorage.setItem('className', res.data.data.className)
      localStorage.setItem('studentNumber', res.data.data.studentNumber)
    }
  } catch (error) {
    console.error('获取学生信息失败:', error)
  }
}


function loadUserFromStorage() {
  try {
    userName.value = localStorage.getItem('studentName')
  } catch {
    userName.value = '未登录'
  }
}

onMounted(() => {
  loadUserFromStorage()
  getStudentById()
})

onActivated(() => {
  getStudentById()
})

onBeforeRouteUpdate(() => {
  getStudentById()
})


const toggleUserMenu = () => {
  showUserMenu.value = !showUserMenu.value
}

const logout = async () => {
  showUserMenu.value = false

  try {
    const token = localStorage.getItem('token')

    if (token) {
      const res = await axios.post(`${BASE_URL}/auth/logout`, {}, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })
      if (res.data.code === 200) {
        ElMessage.success('退出登录成功')
      } else {
        ElMessage.error('退出登录失败')
      }
    }
  } catch (error) {
    console.error('退出登录请求失败:', error)
  } finally {
    try {
      localStorage.removeItem('student_group_status')
      localStorage.removeItem('student_group_info')
    } catch (e) { console.error(e) }
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    await router.push('/')
  }
}


const navigateTo = (path) => {
  showUserMenu.value = false
  router.push(path)
  if (!path.startsWith('/courses')) {
    isSubMenuOpen.value = false
  }
  setTimeout(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }, 100)
}

watch(() => route.path, (newPath) => {
  isSubMenuOpen.value = newPath.startsWith('/courses')
}, { immediate: true })


const passwordDialogVisible = ref(false)
const changingPassword = ref(false)
const passwordFormRef = ref()
const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) return callback(new Error('请再次输入新密码'))
  if (value !== passwordForm.value.newPassword) {
    return callback(new Error('两次输入的密码不一致'))
  }
  callback()
}

const passwordRules = {
  currentPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码长度至少为6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const resetPasswordForm = () => {
  passwordForm.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
  if (passwordFormRef.value) passwordFormRef.value.clearValidate()
}

const openPasswordDialog = () => {
  showUserMenu.value = false
  resetPasswordForm()
  passwordDialogVisible.value = true
}

const handleClosePasswordDialog = (done) => {
  if (changingPassword.value) return
  resetPasswordForm()
  done()
}

const cancelPasswordChange = () => {
  handleClosePasswordDialog(() => {
    passwordDialogVisible.value = false
  })
}

const changePassword = async () => {
  if (!passwordFormRef.value) return

  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) return

    if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
      ElMessage.error('两次输入的密码不一致')
      return
    }

    try {
      changingPassword.value = true

      const userId = localStorage.getItem('userId')
      if (!userId) {
        ElMessage.error('未找到用户 ID，请重新登录')
        return
      }

      const payload = {
        password: passwordForm.value.newPassword
      }

      const res = await axios.put(`${BASE_URL}/user/${userId}`, payload, {
        headers: {
          Authorization: `Bearer ${localStorage.getItem('token')}`
        }
      })

      if (res.data.code === 200) {
        ElMessage.success('密码修改成功')
        passwordDialogVisible.value = false
        resetPasswordForm()
      } else {
        ElMessage.error(res.data.message || '密码修改失败')
      }
    } catch (e) {
      console.error(e)
      ElMessage.error('密码修改失败，请稍后重试')
    } finally {
      changingPassword.value = false
    }
  })
}

const profileRules = {
  phone: [
    {
      validator: (rule, value, callback) => {
        const v = String(value || '').trim()
        if (!v) return callback()
        return /^\d{6,20}$/.test(v) ? callback() : callback(new Error('手机号格式不正确'))
      },
      trigger: 'blur'
    }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

function openProfileDialog() {
  showUserMenu.value = false
  loadProfileIntoForm()
  profileDialogVisible.value = true
}

function handleCloseProfileDialog(done) {
  done()
}

function cancelProfileEdit() {
  profileDialogVisible.value = false
}

function loadProfileIntoForm() {
  try {
    const saved = JSON.parse(localStorage.getItem('currentUser') || 'null') || {}
    profileForm.value.name = localStorage.getItem('studentName') || saved.name || ''
    profileForm.value.className = localStorage.getItem('className') || saved.className || ''
    profileForm.value.studentNumber = localStorage.getItem('studentNumber') || saved.studentNumber || ''
    profileForm.value.phone = localStorage.getItem('studentPhone') || saved.phone || ''
    profileForm.value.email = localStorage.getItem('studentEmail') || saved.email || ''
  } catch (e) { console.error(e) }
}

async function updateStudentContact(phone, email) {
  try {
    if (profileFormRef.value) await profileFormRef.value.validate()
    savingProfile.value = true

    const userId = localStorage.getItem('userId')
    if (!userId) { ElMessage.error('未找到用户 ID，请重新登录'); return false }

    const normalizedPhone = String((phone ?? profileForm.value.phone ?? '')).trim()
    const normalizedEmail = String((email ?? profileForm.value.email ?? '')).trim()

    const token = localStorage.getItem('token')
    console.log('学生更改信息', normalizedPhone, normalizedEmail)
    const res = await axios.put(
      `${BASE_URL}/teacher/update/student?id=${userId}`,
      { phone: normalizedPhone, email: normalizedEmail },
      { headers: { Authorization: `Bearer ${token}` } }
    )
    console.log('学生更改信息结果', res.data)
    if (res?.data?.code !== 200) { ElMessage.error('保存失败'); return false }

    try {
      const saved = JSON.parse(localStorage.getItem('currentUser') || 'null') || {}
      const merged = { ...saved, phone: normalizedPhone, email: normalizedEmail }
      localStorage.setItem('currentUser', JSON.stringify(merged))
      localStorage.setItem('studentPhone', normalizedPhone)
      localStorage.setItem('studentEmail', normalizedEmail)
    } catch (e) { console.error(e) }

    ElMessage.success('个人资料已更新')
    profileDialogVisible.value = false
    return true
  } catch (e) {
    console.error(e)
    ElMessage.error('保存失败')
    return false
  } finally {
    savingProfile.value = false
  }
}

provide('updateStudentContact', updateStudentContact)

</script>

<style scoped>
.student-layout {
  display: flex;
  width: 100%;
  min-height: 100vh;
}

.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  width: 280px;
  background: linear-gradient(135deg, #0f172a, #1e293b);
  color: white;
  height: 100vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 0 15px rgba(0, 0, 0, 0.1);
  z-index: 1000;
}

.sidebar-content {
  flex: 1;
  overflow-y: auto;
}

.sidebar-header {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.sidebar-header h1 {
  font-size: 22px;
  font-weight: 600;
}

.menu {
  list-style: none;
  padding: 15px 0;
}

.menu-item {
  position: relative;
}

.menu-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 20px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 16px;
  line-height: 1.2;
}

.menu-title:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.menu-title.active {
  background-color: rgba(255, 255, 255, 0.2);
  border-left: 4px solid #fff;
}

.menu-title i {
  margin-right: 12px;
  font-size: 18px;
  display: inline-flex;
  align-items: center;
}

.user-panel {
  position: relative;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  margin-top: auto;
}

.user-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
  cursor: pointer;
  transition: all 0.3s;
  line-height: 1.2;
}

.user-info:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.user-info > div {
  display: flex;
  align-items: center;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #86b8ff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 10px;
  font-weight: bold;
}

.user-info .fa-chevron-up {
  transition: transform 0.3s ease;
}

.user-info .fa-chevron-up.active {
  transform: rotate(180deg);
}

.user-dropdown {
  position: absolute;
  bottom: 100%;
  left: 0;
  right: 0;
  background-color: rgba(15, 23, 42, 0.9);
  backdrop-filter: blur(10px);
  list-style: none;
  padding: 0;
  margin: 0;
  max-height: 0;
  overflow: hidden;
  transition: max-height 0.3s ease;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.35);
}

.user-dropdown.show {
  max-height: 200px;
}

.dropdown-item {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.15);
}

.dropdown-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.dropdown-item:last-child {
  border-bottom: none;
}

.dropdown-item i {
  margin-right: 10px;
  font-size: 14px;
  width: 16px;
}

.main-content {
  flex: 1;
  margin-left: 280px;
  padding: 30px;
  overflow-y: auto;
  min-height: 100vh;
  background-color: #f5f7fa;
  transition: margin-left 0.3s ease;
}

@media (max-width: 768px) {
  .sidebar {
    width: 70px;
  }

  .main-content {
    margin-left: 70px;
    padding: 20px;
  }

  .sidebar-header h1,
  .user-info span,
  .menu-title span {
    display: none;
  }

  .sidebar-header {
    padding: 15px 0;
  }

  .menu-title {
    justify-content: center;
    padding: 15px 0;
  }

  .menu-title i {
    margin-right: 0;
    font-size: 20px;
  }
}

@media (max-width: 480px) {
  .main-content {
    margin-left: 70px;
    padding: 15px;
  }
}
</style>