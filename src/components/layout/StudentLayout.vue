<template>
  <div id="app">
    <div class="sidebar">
      <div class="sidebar-content">
        <div class="sidebar-header">
          <h1>课程思政学习平台</h1>
        </div>

        <ul class="menu">
          <li class="menu-item">
            <div class="menu-title" :class="{active: $route.path === '/'}"
                 @click="navigateTo('/')">
              <div>
                <i class="fas fa-home"></i>
                <span>首页</span>
              </div>
            </div>
          </li>

          <li class="menu-item">
            <div class="menu-title" :class="{active: $route.path === '/data'}"
                 @click="navigateTo('/data')">
              <div>
                <i class="fas fa-chart-line"></i>
                <span>学习数据</span>
              </div>
            </div>
          </li>

          <li class="menu-item">
            <div class="menu-title" :class="{active: $route.path === '/group'}"
                 @click="navigateTo('/group')">
              <div>
                <i class="fas fa-users"></i>
                <span>学习分组</span>
              </div>
            </div>
          </li>

          <li class="menu-item">
            <div class="menu-title" :class="{active: $route.path === '/work'}"
                 @click="navigateTo('/work')">
              <div>
                <i class="fas fa-file"></i>
                <span>作品提交</span>
              </div>
            </div>
          </li>

          <li class="menu-item">
            <div class="menu-title" :class="{active: $route.path === '/profile'}"
                 @click="navigateTo('/profile')">
              <div>
                <i class="fas fa-user"></i>
                <span>个人中心</span>
              </div>
            </div>
          </li>

        </ul>
      </div>
      <div class="user-panel">
        <div class="user-info" @click="toggleUserMenu">
          <div>
            <div class="user-avatar">{{ avatar }}</div>
            <span>{{ userName }}</span>
            <span class="work-chip" :class="workStatusClass">{{ workStatusLabel }}</span>
          </div>
          <i class="fas fa-chevron-up" :class="{active: showUserMenu}"></i>
        </div>

        <ul class="user-dropdown" :class="{show: showUserMenu}">
          <li class="dropdown-item" @click="logout">
            <i class="fas fa-sign-out-alt"></i>
            <span>退出登录</span>
          </li>
        </ul>
      </div>
    </div>
    <div class="main-content">
      <router-view></router-view>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const isSubMenuOpen = ref(false)
const showUserMenu = ref(false)
const userName = ref('未登录')
const avatar = computed(() => {
  const name = userName.value || ''
  return name ? name[name.length - 1] : '访'
})


const workStatus = ref('none')
const workStatusLabel = computed(() => workStatus.value === 'submitted' ? '作业已提交' : '有新的作业')
const workStatusClass = computed(() => workStatus.value === 'submitted' ? 'chip-ok' : 'chip-none')

function refreshWorkStatus() {
  try {
    const s = localStorage.getItem('work_status') || 'none'
    workStatus.value = s === 'submitted' ? 'submitted' : 'none'
  } catch { workStatus.value = 'none' }
}

function loadUserFromStorage() {
  try {
    const u = JSON.parse(localStorage.getItem('currentUser') || 'null')
    userName.value = u?.name || '未登录'
  } catch {
    userName.value = '未登录'
  }
}

onMounted(() => {
  loadUserFromStorage()
  refreshWorkStatus()
  try { window.addEventListener('storage', refreshWorkStatus) } catch {}
  try { window.addEventListener('work-status-updated', refreshWorkStatus) } catch {}
})
const toggleUserMenu = () => {
  showUserMenu.value = !showUserMenu.value
}
const logout = () => {
  showUserMenu.value = false
  localStorage.removeItem('userRole')
  localStorage.removeItem('currentUser')
  navigateTo('/login')
}
const navigateTo = (path) => {
  showUserMenu.value = false
  router.push(path)
  if (!path.startsWith('/courses')) {
    isSubMenuOpen.value = false
  }
  setTimeout(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }, 100)
}

watch(() => route.path, (newPath) => {
  isSubMenuOpen.value = newPath.startsWith('/courses')
}, { immediate: true })
</script>

<style>
body {
  overflow-x: hidden;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: 'Microsoft YaHei', sans-serif;
}

body {
  background-color: #f5f7fa;
  color: #333;
}

#app {
  display: flex;
  width: 100%;
  min-height: 100vh;
}

.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  width: 280px;
  background: linear-gradient(135deg, #0f172a, #1e293b);
  color: white;
  height: 100vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 0 15px rgba(0, 0, 0, 0.1);
  z-index: 1000;
}

.sidebar-content {
  flex: 1;
  overflow-y: auto;
}

.sidebar-header {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.sidebar-header h1 {
  font-size: 22px;
  font-weight: 600;
}

.menu {
  list-style: none;
  padding: 15px 0;
}

.menu-item {
  position: relative;
}

.menu-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 20px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 16px;
  line-height: 1.2;
}

.menu-title:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.menu-title.active {
  background-color: rgba(255, 255, 255, 0.2);
  border-left: 4px solid #fff;
}

.menu-title i {
  margin-right: 12px;
  font-size: 18px;
  display: inline-flex;
  align-items: center;
}

.menu-title  {
  transition: transform 0.3s ease;
}

.menu-title .active {
  transform: rotate(90deg);
}

.submenu-item i {
  margin-right: 8px;
  font-size: 14px;
}

.user-panel {
  position: relative;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  margin-top: auto;
}

.user-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
  cursor: pointer;
  transition: all 0.3s;
  line-height: 1.2;
}

.user-info:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.user-info > div {
  display: flex;
  align-items: center;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #86b8ff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 10px;
  font-weight: bold;
}

.work-chip {
  display:inline-block;
  margin-left:8px;
  padding:2px 8px;
  border-radius:999px;
  font-size:12px;
  font-weight:700;
}
.chip-ok { background:#e8f5e9; color:#2e7d32; }
.chip-none { background:#ffebee; color:#c62828; }

.user-info .fa-chevron-up {
  transition: transform 0.3s ease;
}

.user-info .fa-chevron-up.active {
  transform: rotate(180deg);
}

.user-dropdown {
  position: absolute;
  bottom: 100%;
  left: 0;
  right: 0;
  background-color: rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(10px);
  list-style: none;
  padding: 0;
  margin: 0;
  max-height: 0;
  overflow: hidden;
  transition: max-height 0.3s ease;
  border-radius: 8px 8px 0 0;
}

.user-dropdown.show {
  max-height: 120px;
}

.dropdown-item {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.dropdown-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.dropdown-item:last-child {
  border-bottom: none;
}

.dropdown-item i {
  margin-right: 10px;
  font-size: 14px;
  width: 16px;
}

.main-content {
  flex: 1;
  margin-left: 280px;
  padding: 30px;
  overflow-y: auto;
  min-height: 100vh;
  background-color: #f5f7fa;
  transition: margin-left 0.3s ease;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar {
    width: 70px;
  }

  .main-content {
    margin-left: 70px;
    padding: 20px;
  }

  .sidebar-header h1,
  .user-info span,
  .menu-title span,
  .submenu-item span {
    display: none;
  }

  .sidebar-header {
    padding: 15px 0;
  }

  .menu-title {
    justify-content: center;
    padding: 15px 0;
  }

  .menu-title i {
    margin-right: 0;
    font-size: 20px;
  }

  .submenu-item {
    padding-left: 25px;
    text-align: center;
    justify-content: center;
  }

  .submenu-item i {
    margin-right: 0;
  }
}

@media (max-width: 480px) {
  .main-content {
    margin-left: 70px;
    padding: 15px;
  }
}
</style>
