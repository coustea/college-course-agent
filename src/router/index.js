import { createRouter, createWebHistory } from 'vue-router'

// 学生端路由
const studentRoutes = [
    {
        path: '/',
        name: 'Home',
        component: () => import('../views/student/Home.vue')
    },
    {
        path: '/data',
        name: 'LearningData',
        component: () => import('../views/student/LearningData.vue')
    },
    {
        path: '/group',
        name: 'LearningGroup',
        component: () => import('../views/student/Groups.vue')
    },
    {
        path: '/work',
        name: 'Work',
        component: () => import('../views/student/work.vue')
    },
    {
        path: '/profile',
        name: 'Profile',
        component: () => import('../views/student/Profile.vue')
    },

]

// 教师端路由
const teacherRoutes = [
  {
    path: '/teacher',
    name: 'TeacherHome',
    component: () => import('../views/teacher/Home.vue'),
    meta: { title: '教师工作台', requiresAuth: true, role: 'teacher' }
  },
  {
    path: '/teacher/courses',
    name: 'TeacherCourses',
    component: () => import('../views/teacher/Courses.vue'),
    meta: { requiresAuth: true, role: 'teacher', title: '课程管理' },
    children: [
      {
        path: '',
        redirect: 'list'
      },
      {
        path: 'list',
        name: 'TeacherCoursesList',
        component: () => import('../views/teacher/MyCourses/CoursesList.vue'),
        meta: { title: '课程列表' }
      },
      {
        path: 'create',
        name: 'TeacherCoursesCreate',
        component: () => import('../views/teacher/MyCourses/CoursesCreate.vue'),
        meta: { title: '创建课程' }
      },
      {
        path: 'categories',
        name: 'TeacherCoursesCategories',
        component: () => import('../views/teacher/MyCourses/CoursesCategories.vue'),
        meta: { title: '课程分类' }
      }
    ]
  },
  {
    path: '/teacher/students',
    name: 'TeacherStudents',
    component: () => import('../views/teacher/Students.vue'),
    meta: { requiresAuth: true, role: 'teacher', title: '学生管理' },
    children: [
      {
        path: '',
        redirect: 'list'
      },
      {
        path: 'list',
        name: 'TeacherStudentsList',
        component: () => import('../views/teacher/Students/StudentsList.vue'),
        meta: { title: '学生列表' }
      },
      {
        path: 'groups',
        name: 'TeacherStudentsGroups',
        component: () => import('../views/teacher/Students/StudentsGroups.vue'),
        meta: { title: '分组管理' }
      },
      {
        path: 'performance',
        name: 'TeacherStudentsPerformance',
        component: () => import('../views/teacher/Students/StudentsPerformance.vue'),
        meta: { title: '学习表现' }
      }
    ]
  },
  {
    path: '/teacher/assignments',
    name: 'TeacherAssignments',
    component: () => import('../views/teacher/Assignments.vue'),
    meta: { requiresAuth: true, role: 'teacher', title: '作业管理' },
    children: [
      {
        path: '',
        redirect: 'list'
      },
      {
        path: 'list',
        name: 'TeacherAssignmentsList',
        component: () => import('../views/teacher/Assignments/AssignmentsList.vue'),
        meta: { title: '作业列表' }
      },
      {
        path: 'create',
        name: 'TeacherAssignmentsCreate',
        component: () => import('../views/teacher/Assignments/AssignmentsCreate.vue'),
        meta: { title: '布置作业' }
      },
      {
        path: 'check/:id',
        name: 'TeacherAssignmentsCheck',
        component: () => import('../views/teacher/Assignments/AssignmentsCheck.vue'),
        meta: { title: '检查情况' }
      }
    ]
  },
  {
    path: '/teacher/analytics',
    name: 'TeacherAnalytics',
    component: () => import('../views/teacher/Analytics.vue'),
    meta: { requiresAuth: true, role: 'teacher', title: '数据分析' }
  },
  {
    path: '/teacher/profile',
    name: 'TeacherProfile',
    component: () => import('../views/teacher/Profile.vue'),
    meta: { requiresAuth: true, role: 'teacher', title: '个人资料' }
  }
]

// 公共路由（登录、注册等）
const publicRoutes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue'),
    meta: { title: '页面未找到', requiresAuth: false }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: [
    ...studentRoutes,
    ...teacherRoutes,
    ...publicRoutes
  ]
})

// // 路由守卫
// router.beforeEach(async (to, from, next) => {
//   // 设置页面标题
//   document.title = to.meta.title ? `${to.meta.title} - 课程思政平台` : '课程思政平台'
//
//   // 检查是否需要认证
//   if (to.meta.requiresAuth) {
//     const isAuthenticated = checkAuth()
//
//     if (!isAuthenticated) {
//       next('/login')
//       return
//     }
//
//     // 检查角色权限
//     const userRole = getUserRole()
//     if (to.meta.role && to.meta.role !== userRole) {
//       // 根据用户角色重定向到对应首页
//       if (userRole === 'teacher') {
//         next('/teacher')
//       } else {
//         next('/')
//       }
//       return
//     }
//   }
//
//   // 如果已登录且访问登录/注册页，重定向到首页
//   if ((to.name === 'Login' || to.name === 'Register') && checkAuth()) {
//     const userRole = getUserRole()
//     if (userRole === 'teacher') {
//       next('/teacher')
//     } else {
//       next('/')
//     }
//     return
//   }
//
//   next()
// })
//
// // 辅助函数 - 实际应用中需要替换为真实的认证检查
// function checkAuth() {
//   const token = localStorage.getItem('userToken')
//   return !!token
// }
//
// // 辅助函数 - 实际应用中需要替换为真实的角色获取
// function getUserRole() {
//   return localStorage.getItem('userRole') || 'student'
// }

export default router
