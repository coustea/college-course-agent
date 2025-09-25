<template>
  <component :is="layoutComponent">
    <router-view v-if="hasLayout" />
  </component>
  <router-view v-if="!hasLayout" />
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import StudentLayout from './components/layout/StudentLayout.vue'
import TeacherLayout from './components/layout/TeacherLayout.vue'
import { useSettingsStore } from './stores/settings'

const route = useRoute()
const settingsStore = useSettingsStore()

// 判断哪些页面不需要布局
const noLayoutPaths = ['/login', '/404', '/not-found']

const hasLayout = computed(() => {
  return !noLayoutPaths.some(path => route.path.startsWith(path))
})

const layoutComponent = computed(() => {
  if (!hasLayout.value) return null

  // 根据路径判断：以/teacher开头的路径使用教师布局
  return route.path.startsWith('/teacher') ? TeacherLayout : StudentLayout
})

// 初始化设置
onMounted(() => {
  settingsStore.initializeSettings()
})
</script>

<style>
/* 全局CSS变量 - 这些会被JavaScript动态覆盖 */
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

/* 应用全局字体大小 */
html {
  font-size: 14px; /* 默认值，会被JavaScript覆盖 */
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

#app {
  min-height: 100vh;
  background-color: var(--bg-color);
}

/* 确保所有元素使用CSS变量 */
.el-button {
  background-color: var(--primary-color);
  border-color: var(--primary-color);
  color: white;
}

.el-button:hover {
  background-color: var(--primary-light);
  border-color: var(--primary-light);
}

.el-button:focus {
  background-color: var(--primary-color);
  border-color: var(--primary-color);
}

.el-card {
  background-color: var(--card-bg);
  border-color: var(--border-color);
  color: var(--text-color);
}

.el-input__inner {
  background-color: var(--card-bg);
  border-color: var(--border-color);
  color: var(--text-color);
}

.el-input__inner:focus {
  border-color: var(--primary-color);
}

.el-select-dropdown {
  background-color: var(--card-bg);
  border-color: var(--border-color);
}

.el-select-dropdown__item {
  color: var(--text-color);
}

.el-select-dropdown__item.hover {
  background-color: var(--hover-bg);
}

.el-dialog {
  background-color: var(--card-bg);
  border-color: var(--border-color);
}

.el-dialog__title {
  color: var(--text-color);
}

.el-form-item__label {
  color: var(--text-color);
}

.el-switch__label {
  color: var(--text-color);
}

.el-checkbox__label {
  color: var(--text-color);
}

.el-slider__button {
  border-color: var(--primary-color);
}

.el-slider__bar {
  background-color: var(--primary-color);
}

.el-slider__button-wrapper {
  background-color: var(--primary-color);
}

/* 紧凑模式样式 */
.compact-mode .el-card {
  margin-bottom: 16px;
}

.compact-mode .el-card__body {
  padding: 16px;
}

.compact-mode .el-form-item {
  margin-bottom: 16px;
}

.compact-mode .el-button {
  padding: 8px 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .compact-mode .el-card__body {
    padding: 12px;
  }

  .compact-mode .el-form-item {
    margin-bottom: 12px;
  }
}
</style>
