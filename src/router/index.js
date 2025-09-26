import { createRouter, createWebHistory } from 'vue-router'

// 学生端路由
const studentRoutes = [
  { path: '/student', name: 'StudentHome', component: () => import('../views/student/Home.vue'), meta: { title: '首页' } },
  { path: '/home', name: 'StudentHomeAlt', component: () => import('../views/student/Home.vue'), meta: { title: '首页' } },
  { path: '/data', name: 'LearningData', component: () => import('../views/student/LearningData.vue'), meta: { title: '学习数据' } },
  { path: '/group', name: 'Groups', component: () => import('../views/student/Groups.vue'), meta: { title: '学习小组' } },
  { path: '/work', name: 'Work', component: () => import('../views/student/Work.vue'), meta: { title: '作品/作业' } },
  { path: '/courses', name: 'Courses', component: () => import('../views/student/Courses.vue'), meta: { title: '课程' } },
  { path: '/profile', name: 'Profile', component: () => import('../views/student/Profile.vue'), meta: { title: '个人中心' } },
]

// 教师端路由
const teacherRoutes = [
  { path: '/teacher', name: 'TeacherHome', component: () => import('../views/Teacher/Home.vue'), meta: { title: '教师工作台', requiresAuth: true, role: 'teacher' } },
  { path: '/teacher/courses', name: 'TeacherCourses', component: () => import('../views/Teacher/Courses.vue'), meta: { requiresAuth: true, role: 'teacher', title: '课程管理' },
    children: [
      { path: '', redirect: 'list' },
      { path: 'list', name: 'TeacherCoursesList', component: () => import('../views/Teacher/MyCourses/TeacherCoursesList.vue'), meta: { title: '课程列表' } },
      { path: 'create', name: 'TeacherCoursesCreate', component: () => import('../views/Teacher/MyCourses/CoursesCreate.vue'), meta: { title: '创建课程' } },
      { path: 'edit/:id', name: 'TeacherCourseEdit', component: () => import('../views/Teacher/MyCourses/CourseEdit.vue'), meta: { title: '编辑课程' } },
      { path: ':id/materials', name: 'TeacherCourseMaterials', component: () => import('../views/Teacher/MyCourses/CourseMaterials.vue'), meta: { title: '课程内容管理' } },
      { path: 'categories', name: 'TeacherCoursesCategories', component: () => import('../views/Teacher/MyCourses/CoursesCategories.vue'), meta: { title: '课程分类' } },
    ]
  },
  { path: '/teacher/assignments', name: 'TeacherAssignments', component: () => import('../views/Teacher/Assignments.vue'), meta: { requiresAuth: true, role: 'teacher', title: '作品检查' },
    children: [
      { path: '', redirect: 'list' },
      { path: 'list', name: 'TeacherAssignmentsList', component: () => import('../views/Teacher/Assignments/AssignmentsList.vue'), meta: { title: '检查列表' } },
      { path: 'create', name: 'TeacherAssignmentsCreate', component: () => import('../views/Teacher/Assignments/AssignmentsCreate.vue'), meta: { title: '下发检查' } },
      { path: 'check/:id', name: 'TeacherAssignmentsCheck', component: () => import('../views/Teacher/Assignments/AssignmentsCheck.vue'), meta: { title: '检查详情' } },
    ]
  },
  { path: '/teacher/students', name: 'TeacherStudents', component: () => import('../views/Teacher/Students.vue'), meta: { requiresAuth: true, role: 'teacher', title: '学生管理' },
    children: [
      { path: '', redirect: 'list' },
      { path: 'list', name: 'TeacherStudentsList', component: () => import('../views/Teacher/students/StudentsList.vue'), meta: { title: '学生列表' } },
      { path: 'groups', name: 'TeacherStudentsGroups', component: () => import('../views/Teacher/students/StudentsGroups.vue'), meta: { title: '分组管理' } },
      { path: 'performance', name: 'TeacherStudentsPerformance', component: () => import('../views/Teacher/students/StudentsPerformance.vue'), meta: { title: '学习表现' } },
    ]
  },
  { path: '/teacher/analytics', name: 'TeacherAnalytics', component: () => import('../views/Teacher/Analytics.vue'), meta: { requiresAuth: true, role: 'teacher', title: '教学分析' } },
  { path: '/teacher/profile', name: 'TeacherProfile', component: () => import('../views/Teacher/Profile.vue'), meta: { requiresAuth: true, role: 'teacher', title: '个人中心' } },
]

// 公共路由
const publicRoutes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { title: '登录', requiresAuth: false } },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('../views/NotFound.vue'), meta: { title: '页面未找到', requiresAuth: false } }
]

const router = createRouter({
  history: createWebHistory(),
  routes: [ ...studentRoutes, ...teacherRoutes, ...publicRoutes ]
})

export default router


