<template>
  <component :is="layoutComponent">
    <router-view v-if="hasLayout" />
  </component>
  <router-view v-if="!hasLayout" />
</template>

<script setup>
import { computed} from 'vue'
import { useRoute } from 'vue-router'
import StudentLayout from './components/layout/StudentLayout.vue'
import TeacherLayout from './components/layout/TeacherLayout.vue'

const route = useRoute()

// 判断哪些页面不需要布局 - 只包含登录和404页面
const noLayoutPaths = ['/', '/login', '/404', '/not-found']

const hasLayout = computed(() => {
  return !noLayoutPaths.includes(route.path)
})

const layoutComponent = computed(() => {
  if (!hasLayout.value) return null
  return route.path.startsWith('/teacher') ? TeacherLayout : StudentLayout
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