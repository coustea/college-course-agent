<template>
  <div id="app">
    <div class="sidebar">
      <div class="sidebar-content">
        <div class="sidebar-header">
          <h1>课程思政示范课程平台</h1>
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
            <div class="menu-title"
                 :class="{active: $route.path.startsWith('/courses')}"
                 @click="toggleSubMenu">
              <div>
                <i class="fas fa-book"></i>
                <span>我的课程</span>
              </div>
              <i class="fas fa-chevron-right arrow" :class="{active: isSubMenuOpen}"></i>
            </div>

            <ul class="submenu" :class="{open: isSubMenuOpen}">
              <li class="submenu-item"
                  :class="{active: $route.path === '/courses/all'}"
                  @click="navigateTo('/courses/all')">
                <i class="fas fa-th-list"></i>
                <span>全部课程</span>
              </li>
              <li class="submenu-item"
                  :class="{active: $route.path === '/courses/video'}"
                  @click="navigateTo('/courses/video')">
                <i class="fas fa-video"></i>
                <span>视频课程</span>
              </li>
              <li class="submenu-item"
                  :class="{active: $route.path === '/courses/file'}"
                  @click="navigateTo('/courses/file')">
                <i class="fas fa-file-alt"></i>
                <span>文档课程</span>
              </li>
            </ul>
          </li>

          <li class="menu-item">
            <div class="menu-title" :class="{active: $route.path === '/history'}"
                 @click="navigateTo('/history')">
              <div>
                <i class="fas fa-history"></i>
                <span>历史记录</span>
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

          <li class="menu-item">
            <div class="menu-title" :class="{active: $route.path === '/survey'}"
                 @click="navigateTo('/survey')">
              <div>
                <i class="fas fa-poll"></i>
                <span>问卷调查</span>
              </div>
            </div>
          </li>
        </ul>
      </div>

      <div class="user-panel">
        <div class="user-info" @click="toggleUserMenu">
          <div>
            <div class="user-avatar">学</div>
            <span>王小虎</span>
          </div>
          <i class="fas fa-chevron-up" :class="{active: showUserMenu}"></i>
        </div>

        <ul class="user-dropdown" :class="{show: showUserMenu}">
          <li class="dropdown-item" @click="navigateToContact">
            <i class="fas fa-headset"></i>
            <span>联系我们</span>
          </li>
          <li class="dropdown-item" @click="logout">
            <i class="fas fa-sign-out-alt"></i>
            <span>退出登录</span>
          </li>
        </ul>
      </div>
    </div>

    <div class="main-content">
      <slot></slot>
    </div>
  </div>
</template>

<script setup>
import {ref, watch} from 'vue'
import {useRouter, useRoute} from 'vue-router'

const router = useRouter()
const route = useRoute()

const isSubMenuOpen = ref(false)
const showUserMenu = ref(false)

const toggleSubMenu = () => {
  showUserMenu.value = false
  isSubMenuOpen.value = !isSubMenuOpen.value
}

const toggleUserMenu = () => {
  showUserMenu.value = !showUserMenu.value
}

const logout = () => {
  // 退出登录逻辑
  showUserMenu.value = false
  // 清除用户信息
  localStorage.removeItem('userToken')
  localStorage.removeItem('userRole')
  navigateTo('/login')
}

const navigateToContact = () => {
  showUserMenu.value = false
  navigateTo('/contact')
}

const navigateTo = (path) => {
  showUserMenu.value = false
  router.push(path)
  if (!path.startsWith('/courses')) {
    isSubMenuOpen.value = false
  }
  setTimeout(() => {
    window.scrollTo({top: 0, behavior: 'smooth'})
  }, 100)
}

watch(() => route.path, (newPath) => {
  isSubMenuOpen.value = newPath.startsWith('/courses')
}, {immediate: true})
</script>

<style scoped>
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

.menu-title .arrow {
  transition: transform 0.3s ease;
}

.menu-title .arrow.active {
  transform: rotate(90deg);
}

.submenu {
  list-style: none;
  max-height: 0;
  overflow: hidden;
  transition: max-height 0.4s ease;
  background-color: rgba(0, 0, 0, 0.1);
}

.submenu.open {
  max-height: 250px;
}

.submenu-item {
  padding: 14px 20px 14px 50px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  display: flex;
  align-items: center;
  line-height: 1.2;
}

.submenu-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.submenu-item.active {
  background-color: rgba(255, 255, 255, 0.2);
  border-left: 4px solid #fff;
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
