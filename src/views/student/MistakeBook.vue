<template>
  <div class="mistake-book-page">
    <MistakeHeader
      :mastered-count="masteredCount"
      :review-count="reviewCount"
      :total-count="totalCount"
      :filters="filters"
      v-model:activeFilter="activeFilter"
      v-model:searchQuery="searchQuery"
      v-model:sortBy="sortBy"
      @back="goBack"
      @start-exam="startExamMode"
    />

    <main class="mistake-book-main" role="main" aria-label="错题本内容">
      <!-- Loading State -->
      <div v-if="isLoading" class="loading-state" role="status" aria-live="polite" aria-label="正在加载错题">
        <div v-for="i in 6" :key="i" class="skeleton-card" :aria-label="'加载占位符 ' + i"></div>
      </div>

      <!-- Empty State or Grid -->
      <div v-else class="mistake-grid-container">
        <div v-if="paginatedMistakes.length === 0" class="empty-state" role="status" aria-live="polite">
          <div class="empty-icon-wrapper" aria-hidden="true"><i class="fas fa-magic"></i></div>
          <h3>{{ searchQuery ? '未找到相关错题' : '太棒了，暂无错题记录！' }}</h3>
          <p>{{ searchQuery ? '试试更换关键词吧' : '继续保持，成就更好的自己。' }}</p>
          <el-button v-if="!searchQuery" type="primary" round @click="goBack">
            <i class="fas fa-book-open" aria-hidden="true"></i> 返回课程学习
          </el-button>
        </div>

        <!-- Mistake List -->
        <transition-group v-else name="list" tag="div" class="mistake-list" role="list" aria-label="错题列表">
          <MistakeCard
            v-for="mistake in paginatedMistakes"
            :key="mistake.id"
            :mistake="mistake"
            :show-answer="showAnswer[mistake.id] || false"
            @toggle-answer="() => toggleAnswer(mistake.id)"
            @toggle-master="() => handleToggleMaster(mistake)"
            @add-note="() => addNote(mistake)"
            @delete="() => handleDelete(mistake.id)"
          />
        </transition-group>
      </div>

      <!-- Pagination -->
      <nav v-if="totalPages > 1" class="pagination-container" aria-label="分页导航">
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="currentPage"
          :page-size="pageSize"
          :total="filteredMistakes.length"
          @current-change="changePage"
        />
      </nav>
    </main>

    <!-- Note Dialog -->
    <el-dialog
      v-model="noteDialogVisible"
      title="编辑笔记"
      width="480px"
      aria-label="编辑错题笔记"
      role="dialog"
    >
      <el-input
        v-model="currentNote"
        type="textarea"
        :rows="6"
        placeholder="记录核心考点、记忆口诀或易错原因..."
        aria-label="笔记内容"
      />
      <template #footer>
        <el-button @click="noteDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveNote">保存笔记</el-button>
      </template>
    </el-dialog>

    <!-- Exam Modal -->
    <MistakeExamModal
      v-model:visible="examDialogVisible"
      :exam-questions="examQuestions"
      @mark-mastered="handleMarkMastered"
    />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';
import MistakeHeader from '@/components/mistake-book/MistakeHeader.vue';
import MistakeCard from '@/components/mistake-book/MistakeCard.vue';
import MistakeExamModal from '@/components/mistake-book/MistakeExamModal.vue';
import { useMistakeBook } from '@/composables/useMistakeBook';

const router = useRouter();
const goBack = () => router.push('/student');

const {
  // State
  searchQuery, activeFilter, sortBy, showAnswer, noteDialogVisible, currentNote,
  currentPage, pageSize, isLoading, filters,
  // Computed
  masteredCount, reviewCount, totalCount, masteryRate, paginatedMistakes, totalPages,
  filteredMistakes,
  // Methods
  toggleAnswer, addNote, saveNote, changePage,
  // Actions
  handleDelete, handleToggleMaster,
  // Exam
  examDialogVisible, examQuestions, startExamMode, handleMarkMastered,
} = useMistakeBook();
</script>

<style>
/* Clean White Design System */
:root {
  --font-family-sans: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;

  /* Minimal Color Palette */
  --color-bg: #FFFFFF;
  --color-bg-secondary: #F9FAFB;
  --color-text-primary: #111827;
  --color-text-secondary: #6B7280;
  --color-text-tertiary: #9CA3AF;
  --color-border: #E5E7EB;
  --color-border-light: #F3F4F6;

  /* Semantic Colors */
  --color-primary: #3B82F6;
  --color-primary-hover: #2563EB;
  --color-primary-light: #EFF6FF;
  --color-success: #10B981;
  --color-success-bg: #D1FAE5;
  --color-error: #EF4444;
  --color-error-bg: #FEE2E2;
  --color-warning: #F59E0B;
  --color-warning-bg: #FEF3C7;

  /* Shadows */
  --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.1);

  /* Transitions */
  --transition-fast: 150ms ease;
  --transition-base: 200ms ease;
  --transition-slow: 300ms ease;
}

.mistake-book-page {
  background: var(--color-bg-secondary);
  min-height: 100vh;
  padding: 32px 24px;
  font-family: var(--font-family-sans);
}

.mistake-book-main {
  max-width: 1400px;
  margin: 0 auto;
}

/* Grid Layout */
.mistake-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
  align-items: start;
}

/* States */
.loading-state {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
}

.skeleton-card {
  height: 280px;
  border-radius: 8px;
  background: linear-gradient(90deg,
    var(--color-bg) 0%,
    var(--color-bg-secondary) 50%,
    var(--color-bg) 100%);
  background-size: 200% 100%;
  border: 1px solid var(--color-border);
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.empty-state {
  text-align: center;
  padding: 64px 32px;
  background: var(--color-bg);
  border-radius: 8px;
  border: 2px dashed var(--color-border);
  grid-column: 1 / -1;
}

.empty-icon-wrapper {
  font-size: 48px;
  color: var(--color-text-tertiary);
  opacity: 0.6;
  margin-bottom: 24px;
}

.empty-state h3 {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 8px;
}

.empty-state p {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 8px 0 32px;
}

/* Pagination */
.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 40px;
  padding: 16px;
  background: var(--color-bg);
  border-radius: 8px;
  border: 1px solid var(--color-border);
  max-width: fit-content;
  margin-left: auto;
  margin-right: auto;
}

/* Element Plus Overrides */
.el-button--primary {
  --el-button-bg-color: var(--color-primary);
  --el-button-hover-bg-color: var(--color-primary-hover);
  --el-button-border-color: var(--color-primary);
  --el-button-hover-border-color: var(--color-primary-hover);
  font-weight: 500;
}

.el-button--success {
  --el-button-bg-color: var(--color-success);
  --el-button-hover-bg-color: #059669;
  --el-button-border-color: var(--color-success);
  --el-button-hover-border-color: #059669;
}

.el-pagination.is-background .el-pager li:not(.is-disabled).is-active {
  background-color: var(--color-primary);
  border-color: var(--color-primary);
  font-weight: 600;
}

.el-pagination.is-background .el-pager li {
  border-radius: 6px;
  font-weight: 500;
  transition: all var(--transition-fast);
}

.el-pagination.is-background .el-pager li:hover {
  background-color: var(--color-primary-light);
  color: var(--color-primary);
}

.el-dialog {
  border-radius: 8px !important;
  border: 1px solid var(--color-border) !important;
}

.el-dialog__title {
  font-weight: 600;
  color: var(--color-text-primary);
}

.el-dialog__header {
  padding: 20px 24px 16px;
  border-bottom: 1px solid var(--color-border-light);
}

.el-dialog__body {
  padding: 24px;
}

.el-dialog__footer {
  padding: 16px 24px 20px;
  border-top: 1px solid var(--color-border-light);
}

.el-textarea__inner {
  border-radius: 6px !important;
  border: 1px solid var(--color-border) !important;
  transition: all var(--transition-base) !important;
  font-family: var(--font-family-sans) !important;
  font-size: 14px !important;
}

.el-textarea__inner:hover {
  border-color: var(--color-border) !important;
}

.el-textarea__inner:focus {
  border-color: var(--color-primary) !important;
  box-shadow: 0 0 0 3px var(--color-primary-light) !important;
}

.el-select .el-input__wrapper {
  border-radius: 6px !important;
  box-shadow: 0 0 0 1px var(--color-border) !important;
  transition: all var(--transition-base) !important;
  font-weight: 500;
}

.el-select .el-input.is-focus .el-input__wrapper {
  box-shadow: 0 0 0 3px var(--color-primary-light) !important;
  border-color: var(--color-primary) !important;
}

/* List Transition */
.list-move,
.list-enter-active,
.list-leave-active {
  transition: all var(--transition-slow) ease;
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

/* Responsive Design */
@media (max-width: 1200px) {
  .mistake-list {
    grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
    gap: 16px;
  }

  .loading-state {
    grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  }
}

@media (max-width: 768px) {
  .mistake-book-page {
    padding: 16px;
  }

  .mistake-list {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .loading-state {
    grid-template-columns: 1fr;
  }

  .empty-state {
    padding: 48px 24px;
  }

  .empty-icon-wrapper {
    font-size: 40px;
  }

  .empty-state h3 {
    font-size: 18px;
  }

  .empty-state p {
    font-size: 13px;
  }
}

/* Respect user's motion preferences */
@media (prefers-reduced-motion: reduce) {
  .skeleton-card {
    animation: none;
  }

  .list-move,
  .list-enter-active,
  .list-leave-active {
    transition: none;
  }
}
</style>

