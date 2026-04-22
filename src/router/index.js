import { createRouter, createWebHistory } from 'vue-router'

// 学生端路由
const studentRoutes = [
    {
        path: '/student',
        name: 'StudentHome',
        component: () => import('@/views/student/Home.vue'),
        meta: {title: '首页'}
    },
    {
        path: '/student/course-center',
        name: 'CourseCenter',
        component: () => import('@/views/student/CourseCenter.vue'),
        meta: {title: '选课中心'}
    },
    {
        path: '/data',
        name: 'LearningData',
        component: () => import('@/views/student/LearningData.vue'),
        meta: {title: '学习数据'}
    },
    {
        path: '/group',
        name: 'Groups',
        component: () => import('@/views/student/Groups.vue'),
        meta: {title: '学习小组'},
        children: [
          { path: '', redirect: 'build' },
          { path: 'build', name: 'GroupBuild', component: () => import('@/views/student/Group/BuildGroup.vue'), meta: { title: '新建小组' } },
          { path: 'mine', name: 'GroupMine', component: () => import('@/views/student/Group/MyGroup.vue'), meta: { title: '我的小组' } },
        ]
    },
    {
        path: '/work',
        name: 'Work',
        component: () => import('../views/student/Work.vue'),
        meta: {title: '作品/作业'}},
    {
        path: '/profile',
        name: 'Profile',
        component: () => import('../views/student/Profile.vue'),
        meta: {title: '个人中心'}
    },
    {
        path: '/student/mistake-book',
        name: 'MistakeBook',
        component: () => import('../views/student/MistakeBook.vue'),
        meta: {title: '错题本'}
    },
    {
        path: '/student/ai-chat',
        name: 'StudentAIChat',
        component: () => import('@/views/student/StudentAIChat.vue'),
        meta: { title: 'AI 学习助手' }
    },
    {
        path: '/student/ideology',
        name: 'IdeologyRecommend',
        component: () => import('@/views/student/IdeologyRecommend.vue'),
        meta: { title: '思政学习' }
    },
    {
        path: '/student/ideology-recommendations',
        name: 'IdeologyRecommendations',
        component: () => import('@/views/student/IdeologyRecommendations.vue'),
        meta: { title: '思政资源推荐' }
    },
]

// 教师端路由
const teacherRoutes = [
  { path: '/teacher', name: 'TeacherHome', component: () => import('@/views/Teacher/Home.vue'), meta: { title: '教师工作台', requiresAuth: true, role: 'teacher' } },
  { path: '/teacher/courses', name: 'TeacherCourses', component: () => import('../views/Teacher/Courses.vue'), meta: { requiresAuth: true, role: 'teacher', title: '课程管理' },
    children: [
      { path: '', redirect: 'list' },
      { path: 'list', name: 'TeacherCoursesList', component: () => import('@/views/Teacher/MyCourses/TeacherCoursesList.vue'), meta: { title: '课程列表' } },
      { path: 'create', name: 'TeacherCoursesCreate', component: () => import('@/views/Teacher/MyCourses/CoursesCreate.vue'), meta: { title: '创建课程' } },
      { path: ':id/materials', name: 'TeacherCourseMaterials', component: () => import('@/views/Teacher/MyCourses/CourseMaterials.vue'), meta: { title: '课程内容管理' } },
      { path: ':id/progress', name: 'TeacherCourseProgress', component: () => import('@/views/Teacher/MyCourses/CourseProgress.vue'), meta: { title: '课程进度管理' } },
      { path: ':id/grades', name: 'TeacherCourseGrades', component: () => import('@/views/Teacher/MyCourses/GradesManagement.vue'), meta: { title: '成绩管理' } },
      { path: 'categories', name: 'TeacherCoursesCategories', component: () => import('@/views/Teacher/MyCourses/CoursesCategories.vue'), meta: { title: '课程分类' } },
    ]
  },
  { path: '/teacher/assignments', name: 'TeacherAssignments', component: () => import('@/views/Teacher/Assignments.vue'), meta: { requiresAuth: true, role: 'teacher', title: '作品检查' },
    children: [
      { path: '', redirect: 'list' },
      { path: 'list', name: 'TeacherAssignmentsList', component: () => import('@/views/Teacher/Assignments/AssignmentsList.vue'), meta: { title: '检查列表' } },
      { path: 'create', name: 'TeacherAssignmentsCreate', component: () => import('@/views/Teacher/Assignments/AssignmentsCreate.vue'), meta: { title: '下发检查' } },
      { path: 'check/:id', name: 'TeacherAssignmentsCheck', component: () => import('@/views/Teacher/Assignments/AssignmentsCheck.vue'), meta: { title: '检查详情' } },
    ]
  },
  { path: '/teacher/students', name: 'TeacherStudents', component: () => import('@/views/Teacher/students.vue'), meta: { requiresAuth: true, role: 'teacher', title: '学生管理' },
    children: [
      { path: '', redirect: 'list' },
      { path: 'list', name: 'TeacherStudentsList', component: () => import('@/views/Teacher/students/StudentsList.vue'), meta: { title: '学生列表' } },
      { path: 'groups', name: 'TeacherStudentsGroups', component: () => import('@/views/Teacher/students/StudentsGroups.vue'), meta: { title: '分组管理' } },
      { path: 'performance', name: 'TeacherStudentsPerformance', component: () => import('@/views/Teacher/students/StudentsPerformance.vue'), meta: { title: '学习表现' } },
    ]
  },
  { path: '/teacher/analytics', name: 'TeacherAnalytics', component: () => import('@/views/Teacher/Analytics.vue'), meta: { requiresAuth: true, role: 'teacher', title: '教学分析' } },
  { path: '/teacher/study-time', name: 'TeacherStudyTime', component: () => import('@/views/Teacher/StudentStudyTime.vue'), meta: { requiresAuth: true, role: 'teacher', title: '学生学习时间' } },
  { path: '/teacher/profile', name: 'TeacherProfile', component: () => import('@/views/Teacher/Profile.vue'), meta: { requiresAuth: true, role: 'teacher', title: '个人中心' } },
  { path: '/teacher/ai-chat', name: 'TeacherAIChat', component: () => import('@/views/Teacher/AIChat.vue'), meta: { requiresAuth: true, role: 'teacher', title: 'AI 助手' } },
  { path: '/teacher/ideology', name: 'TeacherIdeologyResources', component: () => import('@/views/Teacher/Ideology/IdeologyResources.vue'), meta: { requiresAuth: true, role: 'teacher', title: '思政资源管理' } },
  { path: '/teacher/ideology-dashboard', name: 'TeacherIdeologyDashboard', component: () => import('@/views/Teacher/Ideology/IdeologyDashboard.vue'), meta: { requiresAuth: true, role: 'teacher', title: '思政数据分析' } },
]

// 公共路由
const publicRoutes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { title: '登录', requiresAuth: false } },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/NotFound.vue'), meta: { title: '页面未找到', requiresAuth: false } }
]

const router = createRouter({
  history: createWebHistory(),
  routes: [ ...studentRoutes, ...teacherRoutes, ...publicRoutes ]
})

// Public routes that don't require authentication
const publicRoutePaths = ['/', '/login', '/404', '/not-found']

// Router authentication guard
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userRole = localStorage.getItem('userRole')

  // Allow public routes without auth check
  if (publicRoutePaths.includes(to.path)) {
    // If user is already logged in and tries to access login page, redirect based on role
    if (to.path === '/login' && token) {
      if (userRole === 'teacher') {
        next('/teacher')
      } else {
        next('/student')
      }
    } else {
      next()
    }
    return
  }

  // For all other routes, check if token exists
  if (!token) {
    // No token, redirect to login
    next('/login')
    return
  }

  // Token exists, perform role-based access control
  const isTeacherPath = to.path.startsWith('/teacher')
  const isStudentPath = to.path.startsWith('/student') ||
                        to.path.startsWith('/group') ||
                        to.path.startsWith('/data') ||
                        to.path.startsWith('/work') ||
                        to.path.startsWith('/profile')

  // Check role permissions
  if (isTeacherPath && userRole !== 'teacher') {
    // Non-teacher trying to access teacher routes
    next('/student')
    return
  }

  if (isStudentPath && userRole === 'teacher') {
    // Teacher trying to access student routes
    next('/teacher')
    return
  }

  // Allow navigation
  next()
})

export default router
