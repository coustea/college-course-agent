<template>
  <div class="courses-categories">
    <div class="page-header">
      <h2>课程分类管理</h2>
      <el-button type="primary" @click="openAddDialog">
        <el-icon><Plus /></el-icon>
        添加分类
      </el-button>
    </div>

    <div class="table-container">
      <el-table :data="categories" style="width: 100%" height="100%">
        <el-table-column prop="name" label="分类名称" />
        <el-table-column prop="courseCount" label="课程数量" width="120" :style="{ textAlign: 'center' }" />
        <el-table-column prop="createTime" label="创建时间" width="180" :style="{ textAlign: 'center' }" />
        <el-table-column label="操作" width="200" fixed="right" :style="{ textAlign: 'center' }">
          <template #default="scope">
            <el-button size="small" @click="editCategory(scope.row)">编辑</el-button>
            <el-button
              size="small"
              type="danger"
              @click="deleteCategory(scope.row)"
              style="margin-left: 8px;"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 添加/编辑分类对话框 -->
    <el-dialog
      :title="isEditing ? '编辑分类' : '添加分类'"
      v-model="showAddDialog"
      width="500px"
    >
      <el-form :model="categoryForm" label-width="80px">
        <el-form-item label="分类名称">
          <el-input v-model="categoryForm.name" placeholder="请输入分类名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="saveCategory">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import axios from 'axios'

export default {
  name: 'CoursesCategories',
  components: { Plus },
  setup() {
    const router = useRouter()
    const categories = ref([])
    const showAddDialog = ref(false)
    const isEditing = ref(false)
    const categoryForm = ref({ id: null, name: '' })
    const token = ref('')

    // 打开新增分类对话框
    const openAddDialog = () => {
      resetForm()
      showAddDialog.value = true
    }

    // 加载分类
    const loadCategories = async () => {
      try {
        const res = await axios.get('/api/categories', {
          headers: { Authorization: `Bearer ${token.value}` }
        })
        if (res.data.success) {
          categories.value = res.data.data
        } else {
          ElMessage.error(res.data.message || '加载分类失败')
        }
      } catch (error) {
        console.error('加载分类失败:', error)
        if (error.response?.status === 401) {
          ElMessage.error('登录已过期，请重新登录')
          router.push('/login')
        } else {
          ElMessage.error('加载分类失败，请稍后重试')
        }
      }
    }

    // 编辑分类
    const editCategory = (category) => {
      isEditing.value = true
      categoryForm.value = { ...category }
      showAddDialog.value = true
    }

    // 删除分类
    const deleteCategory = (category) => {
      ElMessageBox.confirm(
        `确定要删除分类 "${category.name}" 吗？`,
        '删除确认',
        { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
      ).then(async () => {
        try {
          const res = await axios.delete(`/api/categories/${category.id}`, {
            headers: { Authorization: `Bearer ${token.value}` }
          })
          if (res.data.success) {
            ElMessage.success('分类删除成功')
            loadCategories()
          } else {
            ElMessage.error(res.data.message || '删除失败')
          }
        } catch (error) {
          console.error('删除失败:', error)
          ElMessage.error('删除失败，请稍后重试')
        }
      })
    }

    // 保存分类（新增/编辑）
    const saveCategory = async () => {
      if (!categoryForm.value.name.trim()) {
        ElMessage.warning('请输入分类名称')
        return
      }

      try {
        let res
        if (isEditing.value) {
          // 编辑
          res = await axios.put(`/api/categories/${categoryForm.value.id}`, {
            name: categoryForm.value.name
          }, {
            headers: { Authorization: `Bearer ${token.value}` }
          })
        } else {
          // 新增
          res = await axios.post('/api/categories', {
            name: categoryForm.value.name
          }, {
            headers: { Authorization: `Bearer ${token.value}` }
          })
        }

        if (res.data.success) {
          ElMessage.success(isEditing.value ? '分类更新成功' : '分类添加成功')
          showAddDialog.value = false
          loadCategories()
          resetForm()
        } else {
          ElMessage.error(res.data.message || '保存失败')
        }
      } catch (error) {
        console.error('保存失败:', error)
        ElMessage.error('保存失败，请稍后重试')
      }
    }

    // 重置表单
    const resetForm = () => {
      categoryForm.value = { id: null, name: '' }
      isEditing.value = false
    }

    onMounted(() => {
      token.value = localStorage.getItem('token')
      if (!token.value) {
        ElMessage.error('用户未登录，请先登录')
        router.push('/login')
        return
      }
      loadCategories()
    })

    return {
      categories,
      showAddDialog,
      isEditing,
      categoryForm,
      openAddDialog,
      editCategory,
      deleteCategory,
      saveCategory
    }
  }
}
</script>

<style scoped>
.courses-categories {
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

.table-container {
  background: white;
  padding: 0 24px;
  height: calc(100vh - 200px);
  overflow: auto;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .courses-categories {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .table-container {
    padding: 0 16px;
    height: auto;
  }
}
</style>
