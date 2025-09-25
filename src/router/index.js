import { createRouter, createWebHistory } from 'vue-router'

// 学生端路由
const studentRoutes = [
  {
    path: '/home',
    name: 'Home',
    component: () => import('../views/student/Home.vue'),
    meta: { title: '首页', requiresAuth: true, role: 'student' }
  },
  {
    path: '/data',
    name: 'LearningData',
    component: () => import('../views/student/LearningData.vue'),
    meta: { title: '学习数据', requiresAuth: true, role: 'student' }
  },
  {
    path: '/group',
    name: 'LearningGroup',
    component: () => import('../views/student/Groups.vue'),
    meta: { title: '学习分组', requiresAuth: true, role: 'student' }
  },
  {
    path: '/work',
    name: 'Work',
    component: () => import('../views/student/work.vue'),
    meta: { title: '作品提交', requiresAuth: true, role: 'student' }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/student/Profile.vue'),
    meta: { title: '个人中心', requiresAuth: true, role: 'student' }
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

// 公共路由（登录、注册等）- 不使用布局
const publicRoutes = [
  {
    path: '/',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录', requiresAuth: false, noLayout: true }
  },
  {
    path: '/login',
    name: 'LoginRedirect',
    redirect: '/'
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue'),
    meta: { title: '页面未找到', requiresAuth: false, noLayout: true }
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


export default router
