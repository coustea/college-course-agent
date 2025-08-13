import { createRouter, createWebHistory } from 'vue-router'

const routes = [
    {
        path: '/',
        redirect: '/teacher'
    },
    {
        path: '/teacher',
        component: () => import('@/views/TeacherDashboard.vue'),
        children: [
            {
                path: '',
                name: 'teacher-home',
                component: () => import('@/components/HomeContent.vue')
            },
            {
                path: 'courses',
                name: 'teacher-courses',
                component: () => import('@/components/CourseManagement.vue')
            },
            {
                path: 'students',
                name: 'teacher-students',
                component: () => import('@/components/StudentManagement.vue')
            },
            {
                path: 'resources',
                name: 'teacher-resources',
                component: () => import('@/components/ResourceManagement.vue')
            },
            {
                path: 'course/:id',
                name: 'course-detail',
                component: () => import('@/views/CourseDetail.vue'),
                props: true
            },
            {
                path: 'assistant',
                name: 'teacher-assistant',
                component: () => import('@/components/AssistantChat.vue')
            }
        ]
    },
    {
        path: '/:pathMatch(.*)*',
        redirect: '/teacher'
    }
]

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes
})

export default router