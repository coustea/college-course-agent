// main.js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { useSettingsStore } from './stores/settings'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(ElementPlus)
app.use(router)

// === 全局 BASE_URL ===
app.config.globalProperties.$baseUrl = 'http://192.168.1.106:9999/api'

// 初始化全局设置
const settingsStore = useSettingsStore()
settingsStore.initializeSettings()

app.mount('#app')
