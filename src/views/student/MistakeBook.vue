<template>
  <div class="mistake-book">
    <!-- 动态背景 -->
    <div class="background-decoration"></div>

    <!-- 页面头部 (简化版) -->
    <div class="page-header">
      <div class="header-content">
        <button class="back-btn" @click="goBack">
          <i class="fas fa-arrow-left"></i>
        </button>
        <div class="title-section">
          <h1 class="page-title">我的错题本</h1>
          <p class="page-subtitle">记录每一次错误，成就更好的自己</p>
        </div>
      </div>
    </div>

    <!-- 主要布局容器 -->
    <div class="main-layout">

      <!-- 左侧：主内容区 -->
      <div class="content-column">
        <!-- 筛选和搜索栏 -->
        <div class="filter-bar">
          <div class="filter-tabs">
            <button
              v-for="filter in filters"
              :key="filter.key"
              :class="['filter-tab', { active: activeFilter === filter.key }]"
              @click="activeFilter = filter.key"
            >
              <i :class="filter.icon"></i>
              {{ filter.label }}
            </button>
          </div>

          <div class="search-actions">
            <div class="search-box">
              <i class="fas fa-search"></i>
              <input v-model="searchQuery" placeholder="搜索题目..." />
            </div>
            <div class="sort-select">
              <el-select v-model="sortBy" placeholder="排序" size="default" style="width: 110px">
                <el-option label="最新" value="newest" />
                <el-option label="最早" value="oldest" />
                <el-option label="易错" value="errorCount" />
              </el-select>
            </div>
          </div>
        </div>

        <!-- 错题列表 -->
        <div class="mistake-list">
          <!-- 空状态 -->
          <div v-if="paginatedMistakes.length === 0" class="empty-state">
            <div class="empty-icon">
              <i class="fas fa-clipboard-list"></i>
            </div>
            <h3>{{ searchQuery ? '未找到相关错题' : '太棒了，暂无错题记录' }}</h3>
            <p>{{ searchQuery ? '换个关键词试试吧' : '继续保持，认真学习哦~' }}</p>
            <el-button v-if="!searchQuery" type="primary" round class="empty-btn" @click="goBack">
              <i class="fas fa-book-open"></i> 去学习课程
            </el-button>
          </div>

          <!-- 错题卡片 -->
          <transition-group name="list" tag="div" class="mistake-grid">
            <div
              v-for="mistake in paginatedMistakes"
              :key="mistake.id"
              :id="`mistake-${mistake.id}`"
              :class="['mistake-card', { mastered: mistake.mastered }]"
            >
              <!-- 高频易错标签 -->
              <div v-if="mistake.errorCount >= 3" class="hot-badge">
                <i class="fas fa-fire"></i> 高频易错
              </div>

              <!-- 卡片头部 -->
              <div class="card-header">
                <div class="course-info">
                  <span class="course-tag">{{ mistake.courseName }}</span>
                  <span class="chapter-tag">{{ mistake.chapter }}</span>
                </div>
                <div class="meta-info">
                  <span class="date-text">{{ formatDate(mistake.createTime) }}</span>
                  <span :class="['error-text', { 'text-danger': mistake.errorCount > 2 }]">
                    已错 {{ mistake.errorCount }} 次
                  </span>
                </div>
              </div>

              <!-- 问题内容 -->
              <div class="question-content">
                <h4 class="question-title">{{ mistake.question }}</h4>

                <!-- 选择题 -->
                <div v-if="mistake.type === 'choice'" class="choice-list">
                  <div
                    v-for="(option, index) in mistake.options"
                    :key="index"
                    :class="['choice-item', {
                      'correct': index === mistake.correctIndex && showAnswer[mistake.id],
                      'wrong': mistake.userAnswer === index && !mistake.mastered,
                      'user-selected': mistake.userAnswer === index && !showAnswer[mistake.id]
                    }]"
                  >
                    <span class="choice-circle">{{ ['A', 'B', 'C', 'D'][index] }}</span>
                    <span class="choice-text">{{ option }}</span>
                    <i v-if="index === mistake.correctIndex && showAnswer[mistake.id]" class="fas fa-check status-icon correct"></i>
                    <i v-else-if="mistake.userAnswer === index && !mistake.mastered" class="fas fa-times status-icon wrong"></i>
                  </div>
                </div>

                <!-- 问答题 -->
                <div v-else class="answer-preview">
                  <p class="user-answer-label">你的回答：</p>
                  <p class="user-answer-text">{{ mistake.userAnswer }}</p>
                </div>
              </div>

              <!-- 正确答案与解析（可展开） -->
              <transition name="expand">
                <div v-if="showAnswer[mistake.id]" class="analysis-panel">
                  <div class="analysis-header">
                    <i class="fas fa-lightbulb"></i>
                    <span>解析报告</span>
                  </div>
                  <div class="analysis-content">
                    <p class="correct-answer-row">
                      <span class="label">正确答案</span>
                      <strong class="value">{{ mistake.type === 'choice' ? ['A', 'B', 'C', 'D'][mistake.correctIndex] : mistake.correctAnswer }}</strong>
                    </p>
                    <div class="explanation-box">
                      {{ mistake.explanation }}
                    </div>
                  </div>
                </div>
              </transition>

              <!-- 笔记摘要 -->
              <div v-if="mistake.note" class="note-snippet">
                <i class="fas fa-pen"></i>
                <span>{{ mistake.note }}</span>
              </div>

              <!-- 卡片底部操作 -->
              <div class="card-footer">
                <div class="action-left">
                  <button
                    :class="['status-btn', { active: mistake.mastered }]"
                    @click="toggleMaster(mistake)"
                  >
                    <i :class="mistake.mastered ? 'fas fa-check-circle' : 'far fa-circle'"></i>
                    {{ mistake.mastered ? '已掌握' : '标记掌握' }}
                  </button>
                  <button class="icon-btn" @click="addNote(mistake)" title="编辑笔记">
                    <i class="fas fa-edit"></i>
                  </button>
                </div>
                <div class="action-right">
                  <button
                    :class="['toggle-btn', { active: showAnswer[mistake.id] }]"
                    @click="toggleAnswer(mistake.id)"
                  >
                    <i :class="showAnswer[mistake.id] ? 'fas fa-eye-slash' : 'fas fa-eye'"></i>
                    {{ showAnswer[mistake.id] ? '隐藏解析' : '查看解析' }}
                  </button>
                  <div class="divider"></div>
                  <button class="icon-btn danger" @click="deleteMistake(mistake.id)" title="删除">
                    <i class="fas fa-trash-alt"></i>
                  </button>
                </div>
              </div>
            </div>
          </transition-group>

          <!-- 分页控制器 -->
          <div v-if="totalPages > 1" class="pagination-container">
            <button
              class="page-btn"
              :disabled="currentPage === 1"
              @click="changePage(currentPage - 1)"
            >
              <i class="fas fa-chevron-left"></i>
            </button>
            <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
            <button
              class="page-btn"
              :disabled="currentPage === totalPages"
              @click="changePage(currentPage + 1)"
            >
              <i class="fas fa-chevron-right"></i>
            </button>
          </div>
        </div>
      </div>

      <!-- 右侧：侧边功能栏 (新增) -->
      <div class="sidebar-column">
        <div class="sticky-wrapper">

          <!-- 核心统计卡片 -->
          <div class="sidebar-card stats-card">
            <h3 class="sidebar-title">学习概况</h3>
            <div class="progress-section">
              <div class="progress-ring-container">
                <svg class="progress-ring" width="80" height="80">
                  <circle class="progress-ring__circle-bg" stroke="#f1f5f9" stroke-width="6" fill="transparent" r="34" cx="40" cy="40"/>
                  <circle
                    class="progress-ring__circle"
                    stroke="currentColor"
                    stroke-width="6"
                    fill="transparent"
                    r="34"
                    cx="40"
                    cy="40"
                    :style="{ strokeDashoffset: circumference - (masteryRate / 100) * circumference }"
                  />
                </svg>
                <div class="progress-text-group">
                  <span class="progress-value">{{ masteryRate }}%</span>
                  <span class="progress-label">掌握率</span>
                </div>
              </div>
              <div class="stats-grid">
                <div class="stat-item">
                  <span class="val">{{ masteredCount }}</span>
                  <span class="lbl">已掌握</span>
                </div>
                <div class="stat-item">
                  <span class="val text-warn">{{ reviewCount }}</span>
                  <span class="lbl">待复习</span>
                </div>
              </div>
            </div>
            <button class="random-btn full-width" @click="startRandomReview">
              <i class="fas fa-play-circle"></i> 随机温习错题
            </button>
          </div>

          <!-- 科目分布卡片 -->
          <div class="sidebar-card">
            <h3 class="sidebar-title">科目分布</h3>
            <div class="subject-list">
              <div v-for="(count, subject) in subjectStats" :key="subject" class="subject-item">
                <span class="subject-name">{{ subject }}</span>
                <div class="subject-bar-container">
                  <div class="subject-bar" :style="{ width: `${(count / mistakeList.length) * 100}%` }"></div>
                </div>
                <span class="subject-count">{{ count }}</span>
              </div>
            </div>
          </div>

          <!-- 每日一句/打卡 (装饰性) -->
          <div class="sidebar-card daily-card">
            <div class="daily-icon"><i class="fas fa-calendar-check"></i></div>
            <p class="daily-quote">"温故而知新，可以为师矣。"</p>
            <span class="daily-date">{{ new Date().toLocaleDateString() }}</span>
          </div>

        </div>
      </div>
    </div>

    <!-- 笔记弹窗 -->
    <el-dialog v-model="noteDialogVisible" title="编辑笔记" width="480px" class="custom-dialog">
      <el-input
        v-model="currentNote"
        type="textarea"
        :rows="5"
        placeholder="记录核心考点、记忆口诀或易错原因..."
        class="note-input"
      />
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="noteDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveNote">保存笔记</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  getWrongQuestionsByStudentId,
  deleteWrongQuestion,
  markAsMastered,
  cancelMastered,
} from '@/services/mistakeApi';
import { jwtDecode } from "jwt-decode";


const router = useRouter();

// 状态管理
const searchQuery = ref('');
const activeFilter = ref('all');
const sortBy = ref('newest');
const showAnswer = ref({});
const noteDialogVisible = ref(false);
const currentMistake = ref(null);
const currentNote = ref('');
const currentPage = ref(1);
const pageSize = 5;
const isLoading = ref(true);

// 从 Token 中获取学生 ID
const getStudentIdFromToken = () => {
  const token = localStorage.getItem('userToken');
  if (token) {
    try {
      const decoded = jwtDecode(token);
      return decoded.id; // 根据你的 Token 结构调整
    } catch (error) {
      console.error('Token 解析失败:', error);
      return null;
    }
  }
  return null;
};

const studentId = getStudentIdFromToken();

// 筛选选项
const filters = [
  { key: 'all', label: '全部', icon: 'fas fa-th-list' },
  { key: 'unmastered', label: '待掌握', icon: 'fas fa-times-circle' },
  { key: 'mastered', label: '已掌握', icon: 'fas fa-check-circle' },
  { key: 'noted', label: '带笔记', icon: 'fas fa-sticky-note' }
];

const mistakeList = ref([]);

// 获取错题数据
const fetchMistakes = async () => {
  if (!studentId) {
    ElMessage.error('无法获取用户信息，请重新登录');
    isLoading.value = false;
    return;
  }
  isLoading.value = true;
  try {
    const response = await getWrongQuestionsByStudentId(studentId);
    mistakeList.value = response.data.map(item => ({
      ...item,
      id: item.questionId, // 映射后端返回的 questionId 到 id
      createTime: new Date(item.createTime),
      mastered: item.mastered || false, // 确保 mastered 有默认值
      options: (item.questionType === 'choice' && item.options) ? item.options.split(';') : [], // TODO: 确认选项格式
      correctIndex: (item.questionType === 'choice' && item.correctAnswer) ? ['A', 'B', 'C', 'D'].indexOf(item.correctAnswer) : -1, // TODO: 确认正确答案格式
    }));
  } catch (error) {
    ElMessage.error('错题列表加载失败');
    console.error(error);
  } finally {
    isLoading.value = false;
  }
};

onMounted(() => {
  fetchMistakes();
});

// 监听筛选变化重置页码
watch([searchQuery, activeFilter, sortBy], () => {
  currentPage.value = 1;
});

// 计算属性
const masteredCount = computed(() => mistakeList.value.filter(m => m.mastered).length);
const reviewCount = computed(() => mistakeList.value.filter(m => !m.mastered).length);

// 掌握率计算
const masteryRate = computed(() => {
  return mistakeList.value.length
    ? Math.round((masteredCount.value / mistakeList.value.length) * 100)
    : 0;
});

// 圆环周长 r=34 -> 2*PI*34 ≈ 213.6
const circumference = 2 * Math.PI * 34;

// 科目统计
const subjectStats = computed(() => {
  const stats = {};
  mistakeList.value.forEach(item => {
    stats[item.courseName] = (stats[item.courseName] || 0) + 1;
  });
  return stats;
});

const filteredMistakes = computed(() => {
  let list = [...mistakeList.value];

  // 筛选
  if (activeFilter.value === 'unmastered') {
    list = list.filter(m => !m.mastered);
  } else if (activeFilter.value === 'mastered') {
    list = list.filter(m => m.mastered);
  } else if (activeFilter.value === 'noted') {
    list = list.filter(m => m.note && m.note.trim());
  }

  // 搜索
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase();
    list = list.filter(m =>
      m.question.toLowerCase().includes(query) ||
      m.courseName.toLowerCase().includes(query) ||
      (m.note && m.note.toLowerCase().includes(query))
    );
  }

  // 排序
  list.sort((a, b) => {
    if (sortBy.value === 'oldest') {
      return new Date(a.createTime) - new Date(b.createTime);
    } else if (sortBy.value === 'errorCount') {
      return b.errorCount - a.errorCount;
    } else {
      return new Date(b.createTime) - new Date(a.createTime);
    }
  });

  return list;
});

// 分页数据
const paginatedMistakes = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredMistakes.value.slice(start, start + pageSize);
});

const totalPages = computed(() => Math.ceil(filteredMistakes.value.length / pageSize));

// 方法
const goBack = () => {
  router.push('/home');
};

const formatDate = (date) => {
  return new Date(date).toLocaleDateString();
};

const toggleAnswer = (id) => {
  showAnswer.value[id] = !showAnswer.value[id];
};

const toggleMaster = async (mistake) => {
  const originalMastered = mistake.mastered;
  mistake.mastered = !mistake.mastered; // Optimistic update

  try {
    if (mistake.mastered) {
      await markAsMastered(mistake.id);
      ElMessage.success({
        message: '太棒了！已标记为掌握',
        type: 'success',
        plain: true,
      });
    } else {
      await cancelMastered(mistake.id);
      ElMessage.info('已取消标记');
    }
  } catch (error) {
    mistake.mastered = originalMastered; // Revert on error
    ElMessage.error('操作失败，请重试');
    console.error(error);
  }
};


const addNote = (mistake) => {
  currentMistake.value = mistake;
  currentNote.value = mistake.note || '';
  noteDialogVisible.value = true;
};

const saveNote = () => {
  // TODO: Add API call to save note
  if (currentMistake.value) {
    currentMistake.value.note = currentNote.value;
    ElMessage.success('笔记已保存');
    noteDialogVisible.value = false;
  }
};

const deleteMistake = async (id) => {
  try {
    await ElMessageBox.confirm('删除后无法恢复，确定要移除这道错题吗？', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    });
    
    await deleteWrongQuestion(id);

    mistakeList.value = mistakeList.value.filter(m => m.id !== id);
    if (paginatedMistakes.value.length === 0 && currentPage.value > 1) {
      currentPage.value--;
    }
    ElMessage.success('删除成功');
  } catch (error) {
    if (error !== 'cancel') {
        ElMessage.error('删除失败，请重试');
        console.error(error);
    }
  }
};

const changePage = (page) => {
  currentPage.value = page;
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

const startRandomReview = () => {
  const unmastered = mistakeList.value.filter(m => !m.mastered);
  if (unmastered.length === 0) {
    ElMessage.success('恭喜！所有错题都已掌握！');
    return;
  }

  // 随机逻辑
  const random = unmastered[Math.floor(Math.random() * unmastered.length)];

  // 简单定位：如果不在当前视图，重置筛选器以显示
  activeFilter.value = 'all';
  searchQuery.value = '';

  setTimeout(() => {
    // 计算所在页码
    const index = filteredMistakes.value.findIndex(m => m.id === random.id);
    if (index !== -1) {
      currentPage.value = Math.floor(index / pageSize) + 1;

      // 滚动并高亮
      setTimeout(() => {
        const el = document.getElementById(`mistake-${random.id}`);
        if (el) {
          el.scrollIntoView({ behavior: 'smooth', block: 'center' });
          el.classList.add('highlight-pulse');
          setTimeout(() => el.classList.remove('highlight-pulse'), 2000);
        }
      }, 300);
    }
  }, 100);
};

onMounted(() => {
  fetchMistakes();
  if (mistakeList.value.length > 0) {
    showAnswer.value[mistakeList.value[0].id] = true;
  }
});
</script>

<style scoped>
/* 基础变量 */
.mistake-book {
  --primary-color: #6366f1;
  --secondary-color: #8b5cf6;
  --success-color: #10b981;
  --danger-color: #ef4444;
  --bg-color: #f8fafc;
  --card-bg: #ffffff;
  --text-primary: #1e293b;
  --text-secondary: #64748b;
  --sidebar-width: 300px;

  /* 增加整体最大宽度 */
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px;
  min-height: 100vh;
  font-family: 'Segoe UI', system-ui, sans-serif;
  color: var(--text-primary);
  position: relative;
  overflow-x: hidden;
}

/* 动态背景 */
.background-decoration {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100vh;
  background:
    radial-gradient(circle at 10% 20%, rgba(99, 102, 241, 0.05) 0%, transparent 20%),
    radial-gradient(circle at 90% 80%, rgba(139, 92, 246, 0.05) 0%, transparent 20%);
  z-index: -1;
  background-color: var(--bg-color);
}

/* 页面头部：简化版 */
.page-header {
  margin-bottom: 24px;
  padding: 0 12px;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.back-btn {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: white;
  border: 1px solid #e2e8f0;
  color: var(--text-secondary);
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-btn:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
  transform: translateX(-2px);
}

.page-title {
  font-size: 24px;
  font-weight: 800;
  margin: 0;
  background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.page-subtitle {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 4px 0 0 0;
  font-weight: 500;
}

/* 双栏布局 */
.main-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.content-column {
  flex: 1;
  min-width: 0; /* 防止子元素溢出 */
}

.sidebar-column {
  width: var(--sidebar-width);
  flex-shrink: 0;
}

.sticky-wrapper {
  position: sticky;
  top: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 侧边栏卡片通用样式 */
.sidebar-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.02);
  border: 1px solid #f1f5f9;
}

.sidebar-title {
  font-size: 15px;
  font-weight: 700;
  margin: 0 0 16px 0;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 核心统计卡片 */
.progress-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
}

.progress-ring-container {
  position: relative;
  width: 80px;
  height: 80px;
  margin-bottom: 16px;
}

.progress-ring {
  transform: rotate(-90deg);
}

.progress-ring__circle {
  transition: stroke-dashoffset 0.8s ease-in-out;
  color: var(--primary-color);
}

.progress-text-group {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.progress-value {
  display: block;
  font-size: 18px;
  font-weight: 800;
  color: var(--primary-color);
  line-height: 1;
}

.progress-label {
  font-size: 10px;
  color: var(--text-secondary);
  transform: scale(0.9);
  display: block;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  width: 100%;
  gap: 12px;
  text-align: center;
}

.stat-item {
  background: #f8fafc;
  padding: 10px;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
}

.stat-item .val {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-item .val.text-warn {
  color: #f59e0b;
}

.stat-item .lbl {
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.random-btn {
  background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
  color: white;
  border: none;
  padding: 12px;
  border-radius: 12px;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.random-btn.full-width {
  width: 100%;
}

.random-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.4);
}

/* 科目分布 */
.subject-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.subject-item {
  display: flex;
  align-items: center;
  font-size: 13px;
  gap: 10px;
}

.subject-name {
  width: 80px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.subject-bar-container {
  flex: 1;
  height: 6px;
  background: #f1f5f9;
  border-radius: 3px;
  overflow: hidden;
}

.subject-bar {
  height: 100%;
  background: var(--primary-color);
  border-radius: 3px;
}

.subject-count {
  width: 24px;
  text-align: right;
  font-weight: 600;
  color: var(--text-primary);
}

/* 每日一句 */
.daily-card {
  background: linear-gradient(135deg, #a5b4fc 0%, #818cf8 100%);
  color: white;
  border: none;
  text-align: center;
}

.daily-icon {
  font-size: 24px;
  margin-bottom: 8px;
  opacity: 0.8;
}

.daily-quote {
  font-size: 13px;
  font-style: italic;
  margin: 0 0 8px 0;
  line-height: 1.5;
  font-weight: 500;
}

.daily-date {
  font-size: 11px;
  opacity: 0.8;
  background: rgba(255,255,255,0.2);
  padding: 2px 8px;
  border-radius: 10px;
}

/* 筛选栏 (在主内容区) */
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-tabs {
  background: rgba(255,255,255,0.8);
  backdrop-filter: blur(8px);
  padding: 4px;
  border-radius: 12px;
  display: flex;
  gap: 4px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
}

.filter-tab {
  padding: 8px 16px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.filter-tab:hover {
  color: var(--text-primary);
  background: #f1f5f9;
}

.filter-tab.active {
  background: white;
  color: var(--primary-color);
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  font-weight: 600;
}

.search-actions {
  display: flex;
  gap: 12px;
  flex: 1;
  justify-content: flex-end;
}

.search-box {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  height: 40px;
  flex: 1; /* 让搜索框占据剩余空间 */
  min-width: 150px;
  transition: all 0.2s;
}

.search-box:focus-within {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.search-box i {
  color: #94a3b8;
  margin-right: 10px;
}

.search-box input {
  border: none;
  outline: none;
  font-size: 14px;
  width: 100%;
  color: var(--text-primary);
}

/* 错题卡片 */
.mistake-grid {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.mistake-card {
  background: white;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.02);
  border: 1px solid #f1f5f9;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.mistake-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 24px rgba(0,0,0,0.06);
  border-color: #e2e8f0;
}

.mistake-card.mastered {
  opacity: 0.85;
  background: #fcfdfd;
  border: 1px solid #e2e8f0;
}

.mistake-card.highlight-pulse {
  animation: pulse-ring 2s infinite;
  border-color: var(--primary-color);
}

@keyframes pulse-ring {
  0% { box-shadow: 0 0 0 0 rgba(99, 102, 241, 0.4); }
  70% { box-shadow: 0 0 0 10px rgba(99, 102, 241, 0); }
  100% { box-shadow: 0 0 0 0 rgba(99, 102, 241, 0); }
}

.hot-badge {
  position: absolute;
  top: 0;
  right: 0;
  background: #ff7675;
  color: white;
  font-size: 11px;
  font-weight: 700;
  padding: 4px 10px;
  border-bottom-left-radius: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  z-index: 1;
}

/* 卡片内容 */
.card-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}

.course-info {
  display: flex;
  gap: 8px;
}

.course-tag {
  background: #e0e7ff;
  color: var(--primary-color);
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.chapter-tag {
  background: #f1f5f9;
  color: var(--text-secondary);
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 11px;
}

.meta-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.date-text {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 600;
}

.error-text {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 500;
}

.error-text.text-danger {
  color: var(--danger-color);
}

.question-title {
  font-size: 17px;
  font-weight: 600;
  line-height: 1.6;
  margin: 0 0 20px 0;
  color: var(--text-primary);
}

/* 选项样式 */
.choice-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 12px;
}

.choice-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  background: white;
  transition: all 0.2s;
}

.choice-circle {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #f1f5f9;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.choice-text {
  font-size: 14px;
  color: var(--text-primary);
  flex: 1;
}

/* 选项状态 */
.choice-item.correct {
  background: #ecfdf5;
  border-color: #10b981;
}
.choice-item.correct .choice-circle {
  background: #10b981;
  color: white;
}

.choice-item.wrong {
  background: #fef2f2;
  border-color: #ef4444;
}
.choice-item.wrong .choice-circle {
  background: #ef4444;
  color: white;
}

.choice-item.user-selected {
  border-color: var(--primary-color);
  background: #eef2ff;
}
.choice-item.user-selected .choice-circle {
  background: var(--primary-color);
  color: white;
}

.status-icon {
  font-size: 16px;
}
.status-icon.correct { color: #10b981; }
.status-icon.wrong { color: #ef4444; }

/* 问答题 */
.answer-preview {
  background: #f8fafc;
  padding: 16px;
  border-radius: 12px;
  border: 1px dashed #cbd5e1;
}
.user-answer-label {
  font-size: 12px;
  color: #94a3b8;
  margin: 0 0 4px 0;
  font-weight: 600;
}
.user-answer-text {
  margin: 0;
  font-size: 14px;
  color: var(--text-primary);
  font-style: italic;
}

/* 解析面板 */
.analysis-panel {
  margin-top: 20px;
  background: #f8fafc;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
}

.analysis-header {
  background: #f1f5f9;
  padding: 10px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.analysis-content {
  padding: 16px;
}

.correct-answer-row {
  margin: 0 0 12px 0;
  font-size: 14px;
}

.correct-answer-row .label {
  background: #dcfce7;
  color: #166534;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 700;
  margin-right: 8px;
}

.explanation-box {
  font-size: 14px;
  line-height: 1.6;
  color: #475569;
  padding-left: 12px;
  border-left: 3px solid #cbd5e1;
}

/* 笔记摘要 */
.note-snippet {
  margin-top: 16px;
  background: #fffbeb;
  border: 1px solid #fcd34d;
  padding: 10px 14px;
  border-radius: 8px;
  display: flex;
  gap: 10px;
  font-size: 13px;
  color: #92400e;
  align-items: flex-start;
}

/* 底部操作 */
.card-footer {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-left, .action-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: white;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}

.status-btn:hover {
  background: #f8fafc;
  color: #10b981;
  border-color: #10b981;
}

.status-btn.active {
  background: #10b981;
  color: white;
  border-color: #10b981;
}

.toggle-btn {
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px solid transparent;
  background: #eef2ff;
  color: var(--primary-color);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}

.toggle-btn:hover {
  background: #e0e7ff;
}

.toggle-btn.active {
  background: #334155;
  color: white;
}

.icon-btn {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.icon-btn:hover {
  background: #f1f5f9;
  color: var(--primary-color);
}

.icon-btn.danger:hover {
  background: #fef2f2;
  color: var(--danger-color);
}

.divider {
  width: 1px;
  height: 20px;
  background: #e2e8f0;
}

/* 分页 */
.pagination-container {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 32px;
  gap: 16px;
}

.page-btn {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: white;
  border: 1px solid #e2e8f0;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.page-btn:hover:not(:disabled) {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 0;
  background: white;
  border-radius: 24px;
  border: 2px dashed #e2e8f0;
}

.empty-icon {
  font-size: 48px;
  color: #cbd5e1;
  margin-bottom: 24px;
}

.empty-btn {
  margin-top: 24px;
  padding: 12px 24px;
}

/* 过渡动画 */
.list-move,
.list-enter-active,
.list-leave-active {
  transition: all 0.5s cubic-bezier(0.55, 0, 0.1, 1);
}

.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

.list-leave-active {
  position: absolute;
  width: 100%;
}

.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease;
  max-height: 500px;
  opacity: 1;
}

.expand-enter-from,
.expand-leave-to {
  max-height: 0;
  opacity: 0;
}

/* 响应式调整 */
@media (max-width: 900px) {
  .main-layout {
    flex-direction: column;
  }

  .sidebar-column {
    width: 100%;
    order: -1; /* 小屏幕时侧边栏显示在上方，或者可以改成 footer */
  }

  /* 调整侧边栏内容为水平排列 */
  .sticky-wrapper {
    position: static;
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  }
}

@media (max-width: 768px) {
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-actions {
    flex-direction: column;
  }

  .choice-list {
    grid-template-columns: 1fr;
  }

  .card-footer {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }

  .action-left, .action-right {
    justify-content: space-between;
  }

  .divider {
    display: none;
  }

  .sidebar-column {
    display: none; /* 移动端可以考虑隐藏侧边栏或者折叠 */
  }
}
</style>