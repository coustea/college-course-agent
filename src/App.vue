<template>
  <v-app>
    <router-view />
    <FloatingAssistant />
  </v-app>
</template>

<script>
import FloatingAssistant from '@/components/FloatingAssistant.vue'

export default {
  name: 'App',
  components: {
    FloatingAssistant
  }
}
</script>

<style>
html, body, #app {
  height: 100%;
  margin: 0;
  padding: 0;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 悬浮球样式 */
.floating-assistant {
  position: fixed;
  bottom: 30px;
  right: 30px;
  z-index: 999;
  transition: all 0.3s ease;
}

.assistant-icon {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #1976D2, #2196F3);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  color: white;
  transition: transform 0.2s;
}

.assistant-icon:hover {
  transform: scale(1.1);
}
</style>