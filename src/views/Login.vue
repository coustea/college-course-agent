<template>
  <div class="login-wrapper">
    <!-- 左侧：品牌与背景轮播区 -->
    <div class="login-banner">
      <div
        class="banner-bg"
        :style="{ backgroundImage: `url(${currentBackground})` }"
      ></div>
      <div class="banner-overlay">
        <div class="banner-content">
          <h1 class="system-title">课程思政学习平台</h1>
          <p class="system-desc">立德树人 · 润物无声 · 协同育人</p>
          <div class="decoration-line"></div>
        </div>
      </div>
    </div>

    <!-- 右侧：登录表单区 -->
    <div class="login-form-container">
      <div class="form-box">
        <div class="form-header">
          <div class="logo-area">
            <el-icon :size="32" color="#409eff"><Reading /></el-icon>
          </div>
          <h2>欢迎登录</h2>
          <p>请输入您的账号和密码开始学习</p>
        </div>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="rules"
          size="large"
          class="login-form"
          @submit.prevent
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="账号/学号/工号"
              :prefix-icon="User"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="密码"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item prop="role" class="role-form-item">
            <div class="role-buttons">
              <div
                class="role-btn"
                :class="{ active: loginForm.role === 'student' }"
                @click="loginForm.role = 'student'"
              >
                <el-icon><School /></el-icon>
                <span>学生</span>
              </div>
              <div
                class="role-btn"
                :class="{ active: loginForm.role === 'teacher' }"
                @click="loginForm.role = 'teacher'"
              >
                <el-icon><Monitor /></el-icon>
                <span>教师</span>
              </div>
            </div>
          </el-form-item>

          <el-button
            type="primary"
            class="submit-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登录系统
          </el-button>
        </el-form>
      </div>

      <div class="footer-copyright">
        &copy; {{ new Date().getFullYear() }} 课程思政教学管理系统 | All Rights Reserved
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, School, Monitor, Reading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { setAuthSession } from '../services/auth'

const router = useRouter()
const { proxy } = getCurrentInstance()
const BASE_URL = proxy.$baseUrl

// 表单数据
const loginForm = reactive({
  username: '',
  password: '',
  role: 'student' // 默认角色
})

const loading = ref(false)
const loginFormRef = ref(null)

// 验证规则
const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择登录角色', trigger: 'change' }
  ]
}

// 背景图片轮播
const backgrounds = [
  "https://images.unsplash.com/photo-1523240795612-9a054b0db644?auto=format&fit=crop&w=1920&q=80",
  "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?auto=format&fit=crop&w=1920&q=80",
  "https://images.unsplash.com/photo-1552664730-d307ca884978?auto=format&fit=crop&w=1920&q=80"
]
const currentBackground = ref(backgrounds[0])
let bgInterval = null

onMounted(() => {
  let index = 0
  bgInterval = setInterval(() => {
    index = (index + 1) % backgrounds.length
    currentBackground.value = backgrounds[index]
  }, 6000)
})

onBeforeUnmount(() => {
  if (bgInterval) clearInterval(bgInterval)
})

const handleLogin = async () => {
  if (!loginFormRef.value) return

  try {
    const valid = await loginFormRef.value.validate()
    if (!valid) return
  } catch {
    return
  }

  loading.value = true
  try {
    const res = await axios.post(`${BASE_URL}/auth/login`, {
      username: loginForm.username,
      password: loginForm.password,
      role: loginForm.role
    })

    if (res.data.code === 200) {
      const { userId, username, token, role, profile } = res.data.data

      // 使用统一的会话管理，自动设置 axios 全局 Authorization 头
      const payload = { userId, username, token, role, profile }
      if (profile?.id) {
        payload.teacherId = profile.id
        payload.studentId = profile.id
      }
      setAuthSession(payload)

      // 统一存储 userInfo
      const userInfo = {
        id: userId,
        username: username,
        role: role,
        ...profile
      }
      localStorage.setItem('userInfo', JSON.stringify(userInfo))

      ElMessage.success('登录成功')

      // 根据后端返回的 role 判断
      if (role === 'teacher') {
        // 教师登录：尝试拉取详细信息并缓存
        try {
           const infoResp = await axios.get(`${BASE_URL}/teacher/${userId}`, {
              headers: { Authorization: `Bearer ${token}` }
           })
           if (infoResp.data.data) {
             const updatedUserInfo = {
               ...userInfo,
               ...infoResp.data.data
             }
             localStorage.setItem('userInfo', JSON.stringify(updatedUserInfo))

             const tId = infoResp.data.data.id
             if (tId) localStorage.setItem('teacherId', String(tId))

             const currentUser = {
               id: tId || userId,
               username: username,
               name: infoResp.data.data.name || username,
               teacherId: tId || userId
             }
             localStorage.setItem('currentUser', JSON.stringify(currentUser))
           }
        } catch (e) {
           localStorage.setItem('teacherId', String(userId))
        }
        router.push('/teacher')
      } else if (role === 'student') {
        // 学生登录：尝试拉取详细信息并缓存
        try {
           const infoResp = await axios.get(`${BASE_URL}/student/${userId}`, {
              headers: { Authorization: `Bearer ${token}` }
           })
           if (infoResp.data.data) {
             const updatedUserInfo = {
               ...userInfo,
               ...infoResp.data.data
             }
             localStorage.setItem('userInfo', JSON.stringify(updatedUserInfo))

             const sId = infoResp.data.data.id
             if (sId) localStorage.setItem('studentId', String(sId))

             const currentUser = {
               id: sId || userId,
               username: username,
               name: infoResp.data.data.name || username,
               studentId: sId || userId
             }
             localStorage.setItem('currentUser', JSON.stringify(currentUser))
           }
        } catch (e) {
           console.error('获取学生详细信息失败:', e)
        }
        router.push('/student')
      }
    } else {
      ElMessage.error(res.data.msg || res.data.message || '登录失败，请检查账号密码')
    }
  } catch (error) {
    console.error(error)
    ElMessage.error(error.response?.data?.message || '登录服务异常，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrapper {
  display: flex;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background-color: #fff;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

/* === 左侧 Banner 区域 === */
.login-banner {
  flex: 1.2;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.banner-bg {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  background-size: cover;
  background-position: center;
  transition: background-image 1.5s ease-in-out;
  z-index: 1;
  animation: zoomEffect 20s infinite alternate;
}

@keyframes zoomEffect {
  from { transform: scale(1); }
  to { transform: scale(1.1); }
}

.banner-overlay {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  background: linear-gradient(135deg, rgba(20, 50, 140, 0.85), rgba(40, 80, 180, 0.85));
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
  backdrop-filter: blur(2px);
}

.banner-content {
  color: white;
  max-width: 600px;
  animation: fadeUp 1s ease-out;
}

.system-title {
  font-size: 3.2rem;
  font-weight: 800;
  margin-bottom: 24px;
  letter-spacing: 4px;
  line-height: 1.2;
  text-shadow: 0 4px 10px rgba(0,0,0,0.3);
}

.system-desc {
  font-size: 1.6rem;
  font-weight: 300;
  opacity: 0.95;
  letter-spacing: 6px;
  margin-bottom: 30px;
}

.decoration-line {
  width: 80px;
  height: 4px;
  background: #fff;
  border-radius: 2px;
  opacity: 0.8;
}

/* === 右侧表单区域 === */
.login-form-container {
  flex: 0 0 480px; /* 稍微调窄一点宽度 */
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px;
  background: #fff;
  position: relative;
  box-shadow: -10px 0 30px rgba(0,0,0,0.05);
  z-index: 10;
}

.form-box {
  width: 100%;
  max-width: 340px; /* 表单内容宽度调窄 */
}

.form-header {
  margin-bottom: 32px;
  text-align: center;
}

.logo-area {
  width: 56px;
  height: 56px;
  background: #ecf5ff;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.form-header h2 {
  font-size: 26px;
  color: #1e293b;
  margin-bottom: 8px;
  font-weight: 700;
}

.form-header p {
  color: #94a3b8;
  font-size: 14px;
}

/* 角色选择按钮 */
.role-form-item {
  margin-bottom: 24px;
}

.role-buttons {
  display: flex;
  gap: 15px;
  width: 100%;
}

.role-btn {
  flex: 1;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  color: #606266;
  background-color: #fff;
}

.role-btn:hover {
  color: #409eff;
  border-color: #c6e2ff;
  background-color: #ecf5ff;
}

.role-btn.active {
  color: #409eff;
  border-color: #409eff;
  background-color: #ecf5ff;
  font-weight: 600;
  box-shadow: 0 0 0 1px #409eff inset;
}

.role-btn .el-icon {
  font-size: 16px;
}

/* 按钮样式 */
.submit-btn {
  width: 100%;
  height: 44px; /* 稍微调小高度 */
  font-size: 15px;
  border-radius: 8px;
  font-weight: 600;
  letter-spacing: 1px;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
  transition: all 0.3s;
}

.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.4);
}

.footer-copyright {
  position: absolute;
  bottom: 20px;
  left: 0;
  width: 100%;
  text-align: center;
  color: #cbd5e1;
  font-size: 12px;
}

/* Element Plus 覆盖 */
.login-form :deep(.el-input__wrapper) {
  padding: 8px 12px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
  transition: all 0.2s;
  background-color: #f9f9f9;
  height: 44px; /* 统一高度 */
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #409eff inset !important;
  background-color: #fff;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

/* 动画 */
@keyframes fadeUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 响应式适配 */
@media (max-width: 900px) {
  .login-banner {
    display: none;
  }
  .login-form-container {
    width: 100%;
    flex: 1;
    box-shadow: none;
  }
}
</style>