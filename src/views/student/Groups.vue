<template>
  <div class="groups-wrapper">
  <div class="header">
    <h1 class="page-title">学习分组</h1>
<!--      <div class="tabs">-->
<!--        <router-link class="tab" :class="{active: $route.path === '/group/build'}" to="/group/build">新建小组</router-link>-->
<!--        <router-link class="tab" :class="{active: $route.path === '/group/mine'}" to="/group/mine">我的小组</router-link>-->
<!--      </div>-->
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
.tabs {
  display: flex;
  gap: 10px;
}
.tab {
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  color: #111827;
  text-decoration: none;
}
.tab.active {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
}
</style>


