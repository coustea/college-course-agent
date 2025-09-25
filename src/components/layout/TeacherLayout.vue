<template>
  <div id="app">
    <div class="sidebar teacher-sidebar">
      <div class="sidebar-content">
        <div class="sidebar-header">
          <h1>教师管理平台</h1>
        </div>

        <ul class="menu">
          <li class="menu-item">
            <div class="menu-title" :class="{active: $route.path === '/teacher'}"
                 @click="navigateTo('/teacher')">
              <div>
                <i class="fas fa-chalkboard-teacher"></i>
                <span>教师工作台</span>
              </div>
            </div>
          </li>

          <li class="menu-item">
            <div class="menu-title"
                 :class="{active: $route.path.startsWith('/teacher/courses')}"
                 @click="toggleSubMenu('courses')">
              <div>
                <i class="fas fa-book"></i>
                <span>课程管理</span>
              </div>
              <i class="fas fa-chevron-right arrow"
                 :class="{active: activeSubMenu === 'courses'}"></i>
            </div>

            <ul class="submenu" :class="{open: activeSubMenu === 'courses'}">
              <li class="submenu-item"
                  :class="{active: $route.path === '/teacher/courses/list'}"
                  @click="navigateTo('/teacher/courses/list')">
                <i class="fas fa-list"></i>
                <span>课程列表</span>
              </li>
              <li class="submenu-item"
                  :class="{active: $route.path === '/teacher/courses/create'}"
                  @click="navigateTo('/teacher/courses/create')">
                <i class="fas fa-plus-circle"></i>
                <span>创建课程</span>
              </li>
              <li class="submenu-item"
                  :class="{active: $route.path === '/teacher/courses/categories'}"
                  @click="navigateTo('/teacher/courses/categories')">
                <i class="fas fa-folder"></i>
                <span>课程分类</span>
              </li>
            </ul>
          </li>

          <li class="menu-item">
            <div class="menu-title"
                 :class="{active: $route.path.startsWith('/teacher/students')}"
                 @click="toggleSubMenu('students')">
              <div>
                <i class="fas fa-user-graduate"></i>
                <span>学生管理</span>
              </div>
              <i class="fas fa-chevron-right arrow"
                 :class="{active: activeSubMenu === 'students'}"></i>
            </div>

            <ul class="submenu" :class="{open: activeSubMenu === 'students'}">
              <li class="submenu-item"
                  :class="{active: $route.path === '/teacher/students/list'}"
                  @click="navigateTo('/teacher/students/list')">
                <i class="fas fa-users"></i>
                <span>学生列表</span>
              </li>
              <li class="submenu-item"
                  :class="{active: $route.path === '/teacher/students/groups'}"
                  @click="navigateTo('/teacher/students/groups')">
                <i class="fas fa-layer-group"></i>
                <span>分组管理</span>
              </li>
              <li class="submenu-item"
                  :class="{active: $route.path === '/teacher/students/performance'}"
                  @click="navigateTo('/teacher/students/performance')">
                <i class="fas fa-chart-line"></i>
                <span>学习表现</span>
              </li>
            </ul>
          </li>

          <li class="menu-item">
            <div class="menu-title"
                 :class="{active: $route.path.startsWith('/teacher/assignments')}"
                 @click="toggleSubMenu('assignments')">
              <div>
                <i class="fas fa-tasks"></i>
                <span>作品管理</span>
              </div>
              <i class="fas fa-chevron-right arrow"
                 :class="{active: activeSubMenu === 'assignments'}"></i>
            </div>

            <ul class="submenu" :class="{open: activeSubMenu === 'assignments'}">
              <li class="submenu-item"
                  :class="{active: $route.path === '/teacher/assignments/list'}"
                  @click="navigateTo('/teacher/assignments/list')">
                <i class="fas fa-clipboard-list"></i>
                <span>作品列表</span>
              </li>
              <li class="submenu-item"
                  :class="{active: $route.path === '/teacher/assignments/create'}"
                  @click="navigateTo('/teacher/assignments/create')">
                <i class="fas fa-plus-square"></i>
                <span>布置作品</span>
              </li>
<!--              <li class="submenu-item"-->
<!--                  :class="{active: $route.path === '/teacher/assignments/grading'}"-->
<!--                  @click="navigateTo('/teacher/assignments/grading')">-->
<!--                <i class="fas fa-check-circle"></i>-->
<!--                <span>批改作品</span>-->
<!--              </li>-->
            </ul>
          </li>

          <li class="menu-item">
            <div class="menu-title" :class="{active: $route.path === '/teacher/analytics'}"
                 @click="navigateTo('/teacher/analytics')">
              <div>
                <i class="fas fa-chart-bar"></i>
                <span>数据分析</span>
              </div>
            </div>
          </li>

          <li class="menu-item">
            <div class="menu-title" :class="{active: $route.path === '/teacher/profile'}"
                 @click="navigateTo('/teacher/profile')">
              <div>
                <i class="fas fa-user"></i>
                <span>个人资料</span>
              </div>
            </div>
          </li>
        </ul>
      </div>

      <div class="user-panel">
        <div class="user-info" @click="toggleUserMenu">
          <div>
            <div class="user-avatar">教</div>
            <span>王老师</span>
          </div>
          <i class="fas fa-chevron-up" :class="{active: showUserMenu}"></i>
        </div>

        <ul class="user-dropdown" :class="{show: showUserMenu}">
          <li class="dropdown-item" @click="navigateToProfile">
            <i class="fas fa-user"></i>
            <span>个人资料</span>
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

const activeSubMenu = ref('')
const showUserMenu = ref(false)

// 仅切换子菜单，不导航
const toggleSubMenuOnly = (menu) => {
  showUserMenu.value = false
  activeSubMenu.value = activeSubMenu.value === menu ? '' : menu
}

// 切换子菜单并导航到默认页面
const toggleSubMenu = (menu) => {
  showUserMenu.value = false

  // 如果当前子菜单已经展开，则关闭
  if (activeSubMenu.value === menu) {
    activeSubMenu.value = ''
    return
  }

  // 否则展开子菜单
  activeSubMenu.value = menu

  // 根据菜单类型导航到默认页面
  if (menu === 'courses') {
    navigateTo('/teacher/courses/list')
  } else if (menu === 'students') {
    navigateTo('/teacher/students/list')
  } else if (menu === 'assignments') {
    navigateTo('/teacher/assignments/list')
  }
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
  navigateTo('/')
}

const navigateToProfile = () => {
  showUserMenu.value = false
  navigateTo('/teacher/profile')
}


const navigateTo = (path) => {
  showUserMenu.value = false
  router.push(path)
  setTimeout(() => {
    window.scrollTo({top: 0, behavior: 'smooth'})
  }, 100)
}

// 根据当前路由自动展开对应的子菜单
watch(() => route.path, (newPath) => {
  if (newPath.startsWith('/teacher/courses')) {
    activeSubMenu.value = 'courses'
  } else if (newPath.startsWith('/teacher/students')) {
    activeSubMenu.value = 'students'
  } else if (newPath.startsWith('/teacher/assignments')) {
    activeSubMenu.value = 'assignments'
  } else {
    activeSubMenu.value = ''
  }
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
  background: linear-gradient(135deg, #1e40af, #3730a3);
  color: white;
  height: 100vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 0 15px rgba(0, 0, 0, 0.1);
  z-index: 1000;
}

.teacher-sidebar {
  background: linear-gradient(135deg, #0f172a, #1e293b);
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
  max-height: 300px;
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
  background-color: #8b5cf6;
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
  max-height: 150px;
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
  background-color: #f8fafc;
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
