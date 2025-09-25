<template>
  <div class="teacher-courses">
    <div class="page-header">
      <h2>课程管理</h2>
      <el-button type="primary" @click="navigateToCreate">
        <el-icon>
          <Plus/>
        </el-icon>
        创建新课程
      </el-button>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="searchQuery"
        placeholder="搜索课程名称..."
        :prefix-icon="Search"
        style="width: 300px; margin-right: 16px;"
        clearable
      />
      <el-select v-model="categoryFilter" placeholder="按分类筛选" clearable>
        <el-option
          v-for="category in categories"
          :key="category.id"
          :label="category.name"
          :value="category.id"
        />
      </el-select>
      <el-select v-model="statusFilter" placeholder="按状态筛选" clearable
                 style="margin-left: 12px;">
        <el-option label="已发布" value="published"/>
        <el-option label="草稿" value="draft"/>
      </el-select>
    </div>

    <div class="courses-grid">
      <div class="course-card" v-for="course in paginatedCourses" :key="course.id">
        <div class="course-image">
          <img :src="course.image || 'https://via.placeholder.com/300x180?text=课程封面'"
               :alt="course.title"/>
          <div class="course-status-badge" :class="course.status">
            {{ course.status === 'published' ? '已发布' : '草稿' }}
          </div>
          <div class="course-stats-overlay">
            <div class="stat-item">
              <el-icon>
                <User/>
              </el-icon>
              <span>{{ course.studentCount || 0 }} 学生</span>
            </div>
            <div class="stat-item">
              <el-icon>
                <VideoPlay/>
              </el-icon>
              <span>{{ course.videoCount || 0 }} 视频</span>
            </div>
            <div class="stat-item">
              <el-icon>
                <Document/>
              </el-icon>
              <span>{{ course.documentCount || 0 }} 文档</span>
            </div>
          </div>
        </div>

        <div class="course-content">
          <div class="course-header">
            <h3 class="course-title">{{ course.title }}</h3>
            <div class="course-actions">
              <el-dropdown trigger="click">
                <el-button link>
                  <el-icon>
                    <More/>
                  </el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="editCourse(course)">
                      <el-icon>
                        <Edit/>
                      </el-icon>
                      编辑课程
                    </el-dropdown-item>
                    <el-dropdown-item @click="manageMaterials(course)">
                      <el-icon>
                        <Setting/>
                      </el-icon>
                      管理内容
                    </el-dropdown-item>
                    <el-dropdown-item
                      v-if="course.status === 'draft'"
                      @click="publishCourse(course)"
                    >
                      <el-icon>
                        <Upload/>
                      </el-icon>
                      发布课程
                    </el-dropdown-item>
                    <el-dropdown-item
                      v-else
                      @click="unpublishCourse(course)"
                    >
                      <el-icon>
                        <Download/>
                      </el-icon>
                      下架课程
                    </el-dropdown-item>
                    <el-dropdown-item divided @click="deleteCourse(course)">
                      <el-icon color="#f56c6c">
                        <Delete/>
                      </el-icon>
                      <span style="color: #f56c6c;">删除课程</span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>

          <p class="course-description">{{ course.description }}</p>

          <div class="course-meta">
            <div class="meta-item">
              <el-icon>
                <PriceTag/>
              </el-icon>
              <span>{{ getCategoryName(course.categoryId) }}</span>
            </div>
            <div class="meta-item">
              <el-icon>
                <Calendar/>
              </el-icon>
              <span>{{ formatDate(course.createTime) }}</span>
            </div>
          </div>

          <div class="course-progress" v-if="course.completionRate !== undefined">
            <div class="progress-info">
              <span>平均完成率: {{ course.completionRate || 0 }}%</span>
            </div>
            <el-progress
              :percentage="course.completionRate || 0"
              :color="progressColor(course.completionRate || 0)"
              :show-text="false"
            />
          </div>

          <div class="teacher-actions">
            <el-button size="small" @click="viewAnalytics(course)">
              <el-icon>
                <DataAnalysis/>
              </el-icon>
              数据分析
            </el-button>
            <el-button size="small" type="primary" @click="viewStudents(course)">
              <el-icon>
                <User/>
              </el-icon>
              学生管理
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading">
        <Loading/>
      </el-icon>
      <p>加载中...</p>
    </div>

    <div v-else-if="filteredCourses.length === 0" class="empty-state">
      <el-icon>
        <Notebook/>
      </el-icon>
      <p>暂无课程</p>
      <el-button type="primary" @click="navigateToCreate">
        创建第一个课程
      </el-button>
    </div>

    <!-- 分页组件 -->
    <div class="pagination" v-if="filteredCourses.length > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[12, 24, 36, 48]"
        :total="filteredCourses.length"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @update:current-page="handleCurrentPageChange"
        @update:page-size="handlePageSizeChange"
      />
    </div>
  </div>
</template>

<script>
import {ref, computed, onMounted} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage, ElMessageBox} from 'element-plus'
import {
  Plus,
  Search,
  User,
  VideoPlay,
  Document,
  More,
  Edit,
  Setting,
  Upload,
  Download,
  Delete,
  PriceTag,
  Calendar,
  DataAnalysis,
  Notebook,
  Loading
} from '@element-plus/icons-vue'
import axios from 'axios'

export default {
  name: 'CoursesList',
  components: {
    Plus,
    Search,
    User,
    VideoPlay,
    Document,
    More,
    Edit,
    Setting,
    Upload,
    Download,
    Delete,
    PriceTag,
    Calendar,
    DataAnalysis,
    Notebook,
    Loading
  },
  setup() {
    const router = useRouter()
    const token = ref(localStorage.getItem('token') || '')

    // 课程数据
    const courses = ref([])
    const categories = ref([])
    const loading = ref(false)

    const searchQuery = ref('')
    const categoryFilter = ref('')
    const statusFilter = ref('')
    const currentPage = ref(1)
    const pageSize = ref(12)

    // 加载课程数据
    const loadCourses = async () => {
      loading.value = true
      try {
        const response = await axios.get('/api/teacher/courses', {
          headers: {Authorization: `Bearer ${token.value}`}
        })

        if (response.data.success) {
          courses.value = response.data.data
        } else {
          ElMessage.error(response.data.message || '加载课程失败')
        }
      } catch (error) {
        console.error('加载课程失败:', error)
        if (error.response?.status === 401) {
          ElMessage.error('登录已过期，请重新登录')
          router.push('/login')
        } else {
          ElMessage.error('加载课程失败，请稍后重试')
        }
      } finally {
        loading.value = false
      }
    }

    // 加载分类数据
    const loadCategories = async () => {
      try {
        const response = await axios.get('/api/categories', {
          headers: {Authorization: `Bearer ${token.value}`}
        })

        if (response.data.success) {
          categories.value = response.data.data
        } else {
          ElMessage.error(response.data.message || '加载分类失败')
        }
      } catch (error) {
        console.error('加载分类失败:', error)
        if (error.response?.status === 401) {
          ElMessage.error('登录已过期，请重新登录')
          router.push('/login')
        }
      }
    }

    // 获取分类名称
    const getCategoryName = (categoryId) => {
      const category = categories.value.find(c => c.id === categoryId)
      return category ? category.name : '未知分类'
    }

    // 格式化日期
    const formatDate = (dateString) => {
      if (!dateString) return ''
      const date = new Date(dateString)
      return date.toLocaleDateString('zh-CN')
    }

    // 过滤课程
    const filteredCourses = computed(() => {
      let result = courses.value

      if (searchQuery.value) {
        const query = searchQuery.value.toLowerCase()
        result = result.filter(course =>
          course.title.toLowerCase().includes(query) ||
          course.description.toLowerCase().includes(query)
        )
      }

      if (categoryFilter.value) {
        result = result.filter(course => course.categoryId == categoryFilter.value)
      }

      if (statusFilter.value) {
        result = result.filter(course => course.status === statusFilter.value)
      }

      return result
    })

    // 分页后的课程
    const paginatedCourses = computed(() => {
      const start = (currentPage.value - 1) * pageSize.value
      const end = start + pageSize.value
      return filteredCourses.value.slice(start, end)
    })

    // 进度条颜色
    const progressColor = (percentage) => {
      if (percentage >= 80) return '#67c23a'
      if (percentage >= 60) return '#e6a23c'
      if (percentage >= 40) return '#f56c6c'
      return '#909399'
    }

    // 导航到创建课程页面
    const navigateToCreate = () => {
      router.push('/teacher/courses/create')
    }

    // 编辑课程
    const editCourse = (course) => {
      router.push(`/teacher/courses/edit/${course.id}`)
    }

    // 管理课程内容
    const manageMaterials = (course) => {
      router.push(`/teacher/courses/${course.id}/materials`)
    }

    // 发布课程
    const publishCourse = async (course) => {
      try {
        ElMessageBox.confirm(
          `确定要发布课程 "${course.title}" 吗？发布后学生将可以看到此课程。`,
          '发布确认',
          {
            confirmButtonText: '发布',
            cancelButtonText: '取消',
            type: 'warning'
          }
        ).then(async () => {
          const response = await axios.put(`/api/courses/${course.id}/status`, {
            status: 'published'
          }, {
            headers: {Authorization: `Bearer ${token.value}`}
          })

          if (response.data.success) {
            ElMessage.success('课程发布成功')
            // 重新加载课程列表
            loadCourses()
          } else {
            ElMessage.error(response.data.message || '发布失败')
          }
        }).catch(() => {
        })
      } catch (error) {
        console.error('发布课程失败:', error)
        ElMessage.error('发布失败，请稍后重试')
      }
    }

    // 下架课程
    const unpublishCourse = async (course) => {
      try {
        ElMessageBox.confirm(
          `确定要下架课程 "${course.title}" 吗？下架后学生将无法看到此课程。`,
          '下架确认',
          {
            confirmButtonText: '下架',
            cancelButtonText: '取消',
            type: 'warning'
          }
        ).then(async () => {
          const response = await axios.put(`/api/courses/${course.id}/status`, {
            status: 'draft'
          }, {
            headers: {Authorization: `Bearer ${token.value}`}
          })

          if (response.data.success) {
            ElMessage.warning('课程已下架')
            // 重新加载课程列表
            loadCourses()
          } else {
            ElMessage.error(response.data.message || '下架失败')
          }
        }).catch(() => {
        })
      } catch (error) {
        console.error('下架课程失败:', error)
        ElMessage.error('下架失败，请稍后重试')
      }
    }

    // 删除课程
    const deleteCourse = async (course) => {
      try {
        ElMessageBox.confirm(
          `确定要删除课程 "${course.title}" 吗？此操作不可恢复。`,
          '删除确认',
          {
            confirmButtonText: '删除',
            cancelButtonText: '取消',
            type: 'error',
            confirmButtonClass: 'el-button--danger'
          }
        ).then(async () => {
          const response = await axios.delete(`/api/courses/${course.id}`, {
            headers: {Authorization: `Bearer ${token.value}`}
          })

          if (response.data.success) {
            ElMessage.success('课程删除成功')
            // 重新加载课程列表
            loadCourses()
          } else {
            ElMessage.error(response.data.message || '删除失败')
          }
        }).catch(() => {
        })
      } catch (error) {
        console.error('删除课程失败:', error)
        ElMessage.error('删除失败，请稍后重试')
      }
    }

    // 查看数据分析
    const viewAnalytics = (course) => {
      router.push(`/teacher/courses/${course.id}/analytics`)
    }

    // 查看学生管理
    const viewStudents = (course) => {
      router.push(`/teacher/courses/${course.id}/students`)
    }

    // 分页变化处理
    const handleCurrentPageChange = (page) => {
      currentPage.value = page
    }

    const handlePageSizeChange = (size) => {
      pageSize.value = size
      currentPage.value = 1
    }

    // 初始化加载数据
    onMounted(() => {
      if (!token.value) {
        ElMessage.error('用户未登录，请先登录')
        router.push('/login')
        return
      }

      loadCourses()
      loadCategories()
    })

    return {
      courses,
      categories,
      loading,
      searchQuery,
      categoryFilter,
      statusFilter,
      currentPage,
      pageSize,
      filteredCourses,
      paginatedCourses,
      progressColor,
      navigateToCreate,
      editCourse,
      manageMaterials,
      publishCourse,
      unpublishCourse,
      deleteCourse,
      viewAnalytics,
      viewStudents,
      handleCurrentPageChange,
      handlePageSizeChange,
      getCategoryName,
      formatDate
    }
  }
}
</script>

<style scoped>
.teacher-courses {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 40px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 16px 24px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.page-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0;
}

.filter-bar {
  display: flex;
  align-items: center;
  margin-bottom: 0;
  padding: 16px 24px;
  background: white;
  border-radius: 8px 8px 0 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.courses-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 24px;
  margin-bottom: 24px;
}

.course-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  transition: all 0.3s ease;
  border: 1px solid #e8e8e8;
}

.course-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

.course-image {
  position: relative;
  height: 180px;
  overflow: hidden;
}

.course-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.course-card:hover .course-image img {
  transform: scale(1.05);
}

.course-status-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: white;
}

.course-status-badge.published {
  background: linear-gradient(135deg, #4ecdc4, #44bd87);
}

.course-status-badge.draft {
  background: linear-gradient(135deg, #ffd666, #ffc53d);
}

.course-stats-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.8));
  padding: 12px;
  display: flex;
  justify-content: space-around;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  color: white;
  font-size: 12px;
}

.stat-item i {
  font-size: 11px;
}

.course-content {
  padding: 20px;
}

.course-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.course-title {
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0;
  line-height: 1.4;
  flex: 1;
  margin-right: 12px;
}

.course-actions {
  flex-shrink: 0;
}

.course-description {
  color: #666;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.course-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #888;
}

.meta-item i {
  font-size: 11px;
}

.course-progress {
  margin-bottom: 16px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 12px;
  color: #666;
}

.teacher-actions {
  display: flex;
  gap: 8px;
}

.teacher-actions .el-button {
  flex: 1;
}

.loading-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.loading-state .el-icon {
  font-size: 48px;
  margin-bottom: 16px;
  color: #409EFF;
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.empty-state .el-icon {
  font-size: 64px;
  margin-bottom: 16px;
  color: #ddd;
}

.empty-state p {
  font-size: 16px;
  margin-bottom: 20px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .courses-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .filter-bar {
    flex-direction: column;
    gap: 12px;
  }

  .filter-bar .el-input {
    width: 100% !important;
    margin-right: 0 !important;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
}
</style>
