// main.js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { useSettingsStore } from './stores/settings'
import request from './utils/request'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(ElementPlus)
app.use(router)

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.config.globalProperties.$baseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
app.config.globalProperties.$request = request

// 初始化全局设置
const settingsStore = useSettingsStore()
settingsStore.initializeSettings()

app.mount('#app')
