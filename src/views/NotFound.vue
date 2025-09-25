<template>
  <div class="not-found-container">
    <div class="not-found-content">
      <h1>404</h1>
      <p>抱歉，您访问的页面不存在</p>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  const userRole = localStorage.getItem('userRole') || 'student'
  if (userRole === 'teacher') {
    router.push('/teacher')
  } else {
    router.push('/')
  }
}
</script>

<style scoped>
.not-found-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f5f7fa;
}

.not-found-content {
  text-align: center;
}

.not-found-content h1 {
  font-size: 120px;
  color: #409EFF;
  margin: 0;
}

.not-found-content p {
  font-size: 18px;
  color: #606266;
  margin: 20px 0;
}
</style>
