<template>
  <div class="login-container" :style="{ backgroundImage: `url(${currentBackground})` }">
    <div class="overlay"></div>
    <div class="login-box">
      <h2 class="title">课程思政学习平台</h2>
      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <input
              type="text"
              v-model="username"
              placeholder="请输入账号"
              required
              class="form-input"
          />
        </div>
        <div class="form-group">
          <input
              type="password"
              v-model="password"
              placeholder="请输入密码"
              required
              class="form-input"
          />
        </div>
        <div class="form-group">
          <select v-model="role" required class="form-input">
            <option value="">请选择角色</option>
            <option value="student">学生</option>
            <option value="teacher">教师</option>
          </select>
        </div>
        <button type="submit" class="login-btn">登录</button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()

const username = ref('')
const password = ref('')
const role = ref('')

// 背景图片数组
const backgrounds = [
  "https://images.unsplash.com/photo-1523240795612-9a054b0db644?auto=format&fit=crop&w=1920&q=80",
  "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?auto=format&fit=crop&w=1920&q=80",
  "https://images.unsplash.com/photo-1552664730-d307ca884978?auto=format&fit=crop&w=1920&q=80"
]
const currentBackground = ref(backgrounds[0])
let index = 0
let intervalId = null

onMounted(() => {
  intervalId = setInterval(() => {
    index = (index + 1) % backgrounds.length
    currentBackground.value = backgrounds[index]
  }, 5000)
})

onBeforeUnmount(() => {
  if (intervalId) clearInterval(intervalId)
})

// 登录方法：必须账号/密码/角色与后端一致才跳转；不再依赖 token
const handleLogin = async () => {
  try {
    console.log('登录请求')
    console.log('username', username.value)
    console.log('password', password.value)
    const res = await axios.post('http://192.168.52.75:9999/api/user/login', {
      username: username.value,
      password: password.value,
      role: role.value
    })
    console.log('登录信息', res.data)
    // console.log('登录状态', res.status)
    const ok = res.data.code === 200
    if (ok) {
      try {
        const user = res?.data?.data
        localStorage.setItem('currentUser', JSON.stringify(user))
        console.log('当前用户', localStorage.getItem('currentUser'))
        localStorage.setItem('className', user.className)
        console.log('className', localStorage.getItem('className'))
      } catch (e) { console.error(e) }
      try { localStorage.setItem('userRole', role.value) } catch (e) { console.error(e) }
      await router.push(role.value === 'student' ? '/home' : '/teacher')
      return
    }
    alert('登录失败，请检查账号/密码/角色')
  } catch (error) {
    const status = error?.response?.status
    if (status) {
      switch (status) {
        case 400:
          alert('登录失败，请检查账号/密码/角色')
          break
        case 401:
          alert('登录失败，请检查账号/密码/角色')
          break
        case 403:
          alert('登录失败，请检查账号/密码/角色')
          break
        case 404:
          alert('登录失败，请检查账号/密码/角色')
          break
        case 500:
          alert('登录失败，请检查账号/密码/角色')
          break
        default:
          alert(`登录失败(${status})：请稍后再试`)
      }
    } else if (error?.request) {
      alert('网络异常：无法连接到服务器')
    } else {
      alert('登录失败：发生未知错误')
    }
  }
}
</script>

<style scoped>
.login-container {
  width: 100%;
  height: 100vh;
  background-size: cover;
  background-position: center;
  position: relative;
  transition: background-image 1s ease-in-out;
}

.overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0,0,0,0.5);
  backdrop-filter: blur(3px);
}

.login-box {
  position: relative;
  z-index: 2;
  width: 380px;
  margin: 0 auto;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(255, 255, 255, 0.15);
  padding: 45px 35px;
  border-radius: 16px;
  backdrop-filter: blur(12px);
  color: white;
  text-align: center;
  box-shadow: 0 8px 32px rgba(0,0,0,0.25);
  animation: fadeIn 1.5s ease;
}

.title {
  margin-bottom: 25px;
  font-size: 24px;
  font-weight: bold;
  letter-spacing: 1px;
  color: #fff;
}

.login-form {
  display: flex;
  flex-direction: column;
}

.form-group {
  margin-bottom: 20px;
}

.form-input {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 8px;
  outline: none;
  font-size: 14px;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  box-sizing: border-box;
}

input::placeholder {
  color: rgba(255, 255, 255, 0.7);
}

select {
  color: #fff;
}

option {
  color: #000;
}

.login-btn {
  background: linear-gradient(135deg, #1a5fb4, #1c71d8);
  color: white;
  padding: 12px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
  width: 100%;
}

.login-btn:hover {
  background: linear-gradient(135deg, #1558a5, #185ec2);
  transform: scale(1.05);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-60%);
  }
  to {
    opacity: 1;
    transform: translateY(-50%);
  }
}
</style>