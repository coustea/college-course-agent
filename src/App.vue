<template>
  <component :is="layoutComponent">
    <router-view v-if="hasLayout" />
  </component>
  <router-view v-if="!hasLayout" />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { watch } from 'vue'
import StudentLayout from './components/layout/StudentLayout.vue'
import TeacherLayout from './components/layout/TeacherLayout.vue'

const route = useRoute()
const router = useRouter()

// 判断哪些页面不需要布局 - 只包含登录和404页面
const noLayoutPaths = ['/', '/login', '/404', '/not-found']

const hasLayout = computed(() => {
  return !noLayoutPaths.includes(route.path)
})

const layoutComponent = computed(() => {
  if (!hasLayout.value) return null

  // 获取用户角色
  const userRole = localStorage.getItem('userRole')

  // 根据路径判断应该使用的布局
  const isTeacherPath = route.path.startsWith('/teacher')
  const isStudentPath = route.path.startsWith('/student') ||
                        route.path.startsWith('/group') ||
                        route.path.startsWith('/home') ||
                        route.path.startsWith('/data') ||
                        route.path.startsWith('/work') ||
                        route.path.startsWith('/profile')

  // 角色验证和权限检查
  if (isTeacherPath) {
    if (userRole === 'teacher') {
      return TeacherLayout
    } else {
      // 学生尝试访问教师端页面，重定向到学生首页
      console.warn('[权限验证] 学生不能访问教师端页面，重定向到学生首页')
      router.push('/student')
      return StudentLayout
    }
  }

  // 默认使用学生布局
  return StudentLayout
})

// 路由变化时验证权限
watch(() => route.path, (newPath) => {
  const userRole = localStorage.getItem('userRole')

  // 如果访问教师端页面但不是教师角色
  if (newPath.startsWith('/teacher') && userRole !== 'teacher') {
    console.warn('[权限验证] 非教师用户访问教师端页面，重定向')
    router.push('/student')
  }

  // 如果访问学生端页面但不是学生角色（虽然当前只有学生和教师两种角色）
  if ((newPath.startsWith('/student') || newPath.startsWith('/group')) && userRole === 'teacher') {
    console.warn('[权限验证] 教师访问学生端页面，重定向')
    router.push('/teacher')
  }
})
</script>

<style>
:root {
  --primary-color: #409EFF;
  --primary-light: #79bbff;
  --bg-color: #f5f7fa;
  --text-color: #303133;
  --text-secondary: #909399;
  --card-bg: #ffffff;
  --border-color: #e4e7ed;
  --hover-bg: #f5f7fa;
}

html {
  font-size: 14px;
}

body {
  margin: 0;
  padding: 0;
  background-color: var(--bg-color);
  color: var(--text-color);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen',
  'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue',
  sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  transition: background-color 0.3s ease, color 0.3s ease;
}
</style>