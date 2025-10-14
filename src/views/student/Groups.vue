<template>
  <div class="groups-wrapper">
  <div class="header">
    <h1 class="page-title">学习分组</h1>
    </div>
    <router-view />
  </div>
</template>

<script setup>
</script>

<style scoped>
.groups-wrapper {
  width: 100%;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}
.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #2c3e50;
  margin: 0;
}
</style>


