<template>
  <div class="assignments-container">
    <router-view></router-view>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'

onMounted(() => {
  console.log('作品管理页面加载完成')
})
</script>

<style scoped>
.assignments-container {
  padding: 20px;
}


.assignments-header h2 {
  color: #1f2937;
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 8px;
}

.assignments-header p {
  color: #6b7280;
  font-size: 14px;
}
</style>
