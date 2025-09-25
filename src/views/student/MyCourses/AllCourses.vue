<template>
  <div class="all-courses">
    <el-alert title="我的全部课程" type="info" show-icon :closable="false" style="margin-bottom: 20px;" />

    <div class="courses-grid">
      <div class="course-card" v-for="course in courses" :key="course.id">
        <div class="course-image">
          <img :src="course.image" :alt="course.title" />
          <div class="course-type-badge" :class="course.type">
            <i :class="course.type === 'video' ? 'fas fa-video' : 'fas fa-file-alt'"></i>
            {{ course.type === 'video' ? '视频' : '文件' }}
          </div>
        </div>
        <div class="course-content">
          <h3 class="course-title">{{ course.title }}</h3>
          <p class="course-description">{{ course.courseDescription }}</p>
          <div class="course-category">
            <i class="fas fa-tag"></i>
            <span>{{ course.category }}</span>
          </div>
          <div class="course-meta">
            <span class="course-duration">
              <i class="fas fa-clock"></i>
              {{ course.duration }}
            </span>
          </div>
          <div class="course-progress-bar">
            <div class="progress-info">
              <span>学习进度: {{ course.progress }}%</span>
            </div>
            <div class="progress-bar">
              <div class="progress-fill" :style="{width: course.progress + '%'}"></div>
            </div>
          </div>
          <div class="course-actions">
            <el-button type="primary" size="small" @click="startLearning(course)">
              {{ course.progress > 0 ? '继续学习' : '开始学习' }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="courses.length === 0" class="empty-state">
      <i class="fas fa-book-open"></i>
      <p>暂无课程</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchUserCourses, updateDocumentProgress } from '@/services/student/coursesApi.js'

const rawCourses = ref([])
const courses = computed(() => rawCourses.value.flatMap(course =>
  course.documents.map(m => ({
    ...m,
    courseId: course.id,
    courseTitle: course.title,
    courseDescription: course.description,
    category: course.category
  }))
))

const startLearning = (course) => {
  ElMessage.success(`开始学习：${course.title}`)

  if (course.progress < 100) {
    const newProgress = Math.min(course.progress + 10, 100)
    updateDocumentProgress(course.id, newProgress).then(() => {
      for (const c of rawCourses.value) {
        const m = c.documents.find(x => x.id === course.id)
        if (m) { m.progress = newProgress; break }
      }
    })
    ElMessage.info(`学习进度更新为：${newProgress}%`)
  }

}

onMounted(async () => {
  rawCourses.value = await fetchUserCourses('20250001')
})
</script>

<style scoped>
.all-courses {
  padding: 20px 0;
}

.courses-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
  margin-top: 20px;
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

.course-type-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: white;
  display: flex;
  align-items: center;
  gap: 4px;
}

.course-type-badge.video {
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
}

.course-type-badge.file {
  background: linear-gradient(135deg, #4ecdc4, #44bd87);
}

.course-content {
  padding: 20px;
}

.course-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
  line-height: 1.4;
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

.course-category {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
  font-size: 12px;
  color: #1890ff;
  background: #f0f8ff;
  padding: 4px 8px;
  border-radius: 12px;
  width: fit-content;
}

.course-category i {
  font-size: 11px;
}

.course-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
  font-size: 12px;
  color: #888;
}

.course-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.course-progress-bar {
  margin-bottom: 16px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  font-size: 12px;
  color: #666;
}

.progress-bar {
  width: 100%;
  height: 6px;
  background-color: #f0f0f0;
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #4ecdc4, #44bd87);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.course-actions {
  display: flex;
  gap: 8px;
}

.course-actions .el-button {
  flex: 1;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.empty-state i {
  font-size: 64px;
  margin-bottom: 16px;
  color: #ddd;
}

.empty-state p {
  font-size: 16px;
  margin-bottom: 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .courses-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .course-card {
    margin: 0 10px;
  }

  .course-actions {
    flex-direction: column;
  }

  .course-actions .el-button {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .all-courses {
    padding: 10px 0;
  }

  .course-content {
    padding: 15px;
  }

  .course-title {
    font-size: 16px;
  }

  .course-description {
    font-size: 13px;
  }
}
</style>
