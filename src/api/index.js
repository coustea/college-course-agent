import axios from 'axios'

const api = axios.create({
    baseURL: process.env.VUE_APP_API_BASE_URL || 'http://localhost:3000/api',
    timeout: 10000
})

// 请求拦截器
api.interceptors.request.use(config => {
    const token = localStorage.getItem('token')
    if (token) {
        config.headers.Authorization = `Bearer ${token}`
    }
    return config
}, error => {
    return Promise.reject(error)
})

// 响应拦截器
api.interceptors.response.use(response => {
    return response.data
}, error => {
    if (error.response) {
        switch (error.response.status) {
            case 401:
                // 处理未授权
                break
            case 404:
                // 处理未找到
                break
            case 500:
                // 处理服务器错误
                break
        }
    }
    return Promise.reject(error)
})

export default {
    // 课程管理API
    getCourses() {
        return api.get('/courses')
    },
    createCourse(data) {
        return api.post('/courses', data)
    },
    updateCourse(id, data) {
        return api.put(`/courses/${id}`, data)
    },
    deleteCourse(id) {
        return api.delete(`/courses/${id}`)
    },

    // 学生管理API
    getStudents() {
        return api.get('/students')
    },
    importStudents(file) {
        const formData = new FormData()
        formData.append('file', file)
        return api.post('/students/import', formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        })
    },
    deleteStudent(id) {
        return api.delete(`/students/${id}`)
    },

    // 资源管理API
    getResources() {
        return api.get('/resources')
    },
    uploadResource(data) {
        const formData = new FormData()
        formData.append('name', data.name)
        formData.append('type', data.type)
        formData.append('file', data.file)
        return api.post('/resources', formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        })
    },
    deleteResource(id) {
        return api.delete(`/resources/${id}`)
    },

    // 智能助教API
    chatWithAssistant(data) {
        return api.post('/assistant/chat', data)
    }
}