<template>
  <div :class="['mistake-card', { mastered: mistake.mastered }]">
    <!-- Card Header -->
    <div class="card-header">
      <div class="course-info">
        <span class="course-tag">{{ mistake.courseName }}</span>
        <span class="chapter-tag">{{ mistake.chapter || '综合练习' }}</span>
      </div>
      <div class="meta-info">
        <span :class="['error-count', { 'is-high': mistake.errorCount > 2 }]">
          <i class="fas fa-exclamation-triangle" aria-hidden="true"></i>
          错误 {{ mistake.errorCount }} 次
        </span>
      </div>
    </div>

    <!-- Question Content -->
    <div class="question-section">
      <div class="question-label">题目</div>
      <p class="question-text">{{ mistake.question || '暂无题目内容' }}</p>

      <!-- Options (if choice question) -->
      <div v-if="mistake.type === 'choice' && mistake.options && mistake.options.length > 0" class="options-list">
        <div
          v-for="(option, index) in mistake.options"
          :key="index"
          :class="['option-item', {
            'is-correct': mistake.correctIndex === index,
            'is-wrong': getUserAnswerIndex() === index && getUserAnswerIndex() !== mistake.correctIndex
          }]"
        >
          <span class="option-label">{{ ['A', 'B', 'C', 'D'][index] || String.fromCharCode(65 + index) }}.</span>
          <span class="option-text">{{ option }}</span>
          <i v-if="mistake.correctIndex === index" class="fas fa-check option-icon correct-icon" aria-hidden="true"></i>
          <i v-else-if="getUserAnswerIndex() === index" class="fas fa-times option-icon wrong-icon" aria-hidden="true"></i>
        </div>
      </div>
    </div>

    <!-- Answer/Analysis Section (Conditional) -->
    <transition name="expand">
      <div v-if="showAnswer" class="analysis-section">
        <!-- User's Wrong Answer -->
        <div class="answer-block user-answer">
          <div class="answer-header">
            <i class="fas fa-times-circle" aria-hidden="true"></i>
            <span>你的答案</span>
          </div>
          <p class="answer-text">
            <template v-if="mistake.type === 'choice'">
              {{ formatUserAnswer() }}
            </template>
            <template v-else>
              {{ mistake.userAnswer || '未作答' }}
            </template>
          </p>
        </div>
        <!-- Correct Answer & Explanation -->
        <div class="answer-block correct-answer">
          <div class="answer-header">
            <i class="fas fa-check-circle" aria-hidden="true"></i>
            <span>正确答案</span>
          </div>
          <p class="answer-text">
            <template v-if="mistake.type === 'choice'">
              {{ ['A', 'B', 'C', 'D'][mistake.correctIndex] || mistake.correctAnswer }}
            </template>
            <template v-else>
              {{ mistake.correctAnswer }}
            </template>
          </p>
          <div v-if="mistake.explanation && mistake.explanation !== '暂无解析'" class="explanation">
            <div class="explanation-header">
              <i class="fas fa-lightbulb" aria-hidden="true"></i>
              <span>解析</span>
            </div>
            <p class="explanation-text">{{ mistake.explanation }}</p>
          </div>
        </div>
      </div>
    </transition>

    <!-- Note -->
    <div v-if="mistake.note" class="note-snippet" @click="$emit('add-note')">
      <i class="fas fa-pen" aria-hidden="true"></i>
      <p class="note-text">{{ mistake.note }}</p>
    </div>

    <!-- Footer Actions -->
    <div class="card-footer">
      <span class="date-text">
        <i class="far fa-calendar-alt" aria-hidden="true"></i> {{ formatDate(mistake.createTime) }}
      </span>
      <div class="actions">
        <button
          class="action-btn"
          @click="$emit('add-note')"
          title="编辑笔记"
          aria-label="编辑笔记"
        >
          <i class="fas fa-edit" aria-hidden="true"></i>
          <span class="btn-text">笔记</span>
        </button>
        <button
          :class="['action-btn status-btn', { active: mistake.mastered }]"
          @click="$emit('toggle-master')"
          :aria-label="mistake.mastered ? '取消掌握标记' : '标记为已掌握'"
          :aria-pressed="mistake.mastered"
        >
          <i :class="mistake.mastered ? 'fas fa-check-circle' : 'far fa-check-circle'" aria-hidden="true"></i>
          <span class="btn-text">{{ mistake.mastered ? '已掌握' : '掌握' }}</span>
        </button>
        <button
          class="action-btn primary"
          @click="$emit('toggle-answer')"
          :aria-label="showAnswer ? '隐藏解析' : '查看解析'"
          :aria-expanded="showAnswer"
        >
          <i :class="showAnswer ? 'fas fa-eye-slash' : 'fas fa-eye'" aria-hidden="true"></i>
          <span class="btn-text">{{ showAnswer ? '隐藏' : '解析' }}</span>
        </button>
        <el-popconfirm
          title="确定删除这道错题吗？"
          confirm-button-text="删除"
          cancel-button-text="取消"
          :icon="null"
          width="200"
          @confirm="$emit('delete')"
        >
          <template #reference>
            <button
              class="action-btn danger"
              title="删除"
              aria-label="删除错题"
            >
              <i class="fas fa-trash-alt" aria-hidden="true"></i>
            </button>
          </template>
        </el-popconfirm>
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  mistake: { type: Object, required: true },
  showAnswer: { type: Boolean, default: false }
});

defineEmits(['toggle-answer', 'toggle-master', 'add-note', 'delete']);

const formatDate = (date) => {
  if (!date) return '';
  return new Date(date).toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' });
};

// 获取用户答案的索引
const getUserAnswerIndex = () => {
  if (!props.mistake?.userAnswer) return -1;

  // 如果用户答案是单个字母 (A, B, C, D)
  if (typeof props.mistake.userAnswer === 'string' && /^[A-Da-d]$/.test(props.mistake.userAnswer)) {
    const letter = props.mistake.userAnswer.toUpperCase();
    const indexMap = { 'A': 0, 'B': 1, 'C': 2, 'D': 3 };
    return indexMap[letter] ?? -1;
  }

  // 如果用户答案是数字索引
  if (typeof props.mistake.userAnswer === 'number') {
    return props.mistake.userAnswer;
  }

  return -1;
};

// 格式化用户答案显示
const formatUserAnswer = () => {
  if (!props.mistake?.userAnswer) return '未作答';

  const userAnswer = props.mistake.userAnswer;
  const options = props.mistake.options || [];

  // 如果是字母答案 (A, B, C, D)
  if (typeof userAnswer === 'string' && /^[A-Da-d]$/.test(userAnswer)) {
    const index = ['A', 'B', 'C', 'D'].indexOf(userAnswer.toUpperCase());
    if (index >= 0 && options[index]) {
      return `${userAnswer.toUpperCase()}. ${options[index]}`;
    }
    return userAnswer.toUpperCase();
  }

  // 如果是数字索引
  if (typeof userAnswer === 'number' && options[userAnswer]) {
    return `${['A', 'B', 'C', 'D'][userAnswer] || userAnswer}. ${options[userAnswer]}`;
  }

  // 直接返回原值
  return userAnswer;
};
</script>

<style scoped>
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
  --color-success: #10B981;
  --color-success-bg: #D1FAE5;
  --color-error: #EF4444;
  --color-error-bg: #FEE2E2;
  --color-warning: #F59E0B;
  --color-warning-bg: #FEF3C7;

  /* Shadows */
  --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1);

  /* Transitions */
  --transition-fast: 150ms ease;
  --transition-base: 200ms ease;
}

.mistake-card {
  background: var(--color-bg);
  border-radius: 8px;
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  transition: box-shadow var(--transition-base), border-color var(--transition-base);
  position: relative;
  overflow: hidden;
}

.mistake-card:hover {
  box-shadow: var(--shadow-md);
  border-color: var(--color-border-light);
}

.mistake-card.mastered {
  background: var(--color-bg-secondary);
  opacity: 0.8;
}

.mistake-card.mastered .question-text {
  color: var(--color-text-secondary);
}

/* Card Header */
.card-header {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--color-border-light);
}

.course-info {
  display: flex;
  gap: 8px;
  align-items: center;
}

.course-tag, .chapter-tag {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
}

.error-count {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  background: var(--color-error-bg);
  color: #B91C1C;
  border: 1px solid #FECACA;
}

.error-count.is-high {
  background: #FEE2E2;
  color: #DC2626;
}

/* Question Section */
.question-section {
  padding: 20px 16px;
  flex-grow: 1;
}

.question-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}

.question-text {
  margin: 0 0 16px 0;
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text-primary);
  line-height: 1.6;
}

/* Options List */
.options-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: 6px;
  transition: all var(--transition-fast);
  position: relative;
}

.option-item.is-correct {
  background: var(--color-success-bg);
  border-color: #6EE7B7;
}

.option-item.is-wrong {
  background: var(--color-error-bg);
  border-color: #FCA5A5;
}

.option-label {
  font-weight: 600;
  font-size: 14px;
  color: var(--color-text-secondary);
  min-width: 24px;
}

.option-text {
  flex: 1;
  font-size: 14px;
  color: var(--color-text-primary);
  line-height: 1.5;
}

.option-icon {
  font-size: 16px;
}

.correct-icon {
  color: var(--color-success);
}

.wrong-icon {
  color: var(--color-error);
}

/* Analysis Section */
.analysis-section {
  padding: 0 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.answer-block {
  padding: 12px 16px;
  border-radius: 6px;
  font-size: 14px;
  border-left: 3px solid;
  background: var(--color-bg-secondary);
}

.answer-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.answer-text {
  margin: 0;
  color: var(--color-text-primary);
  line-height: 1.5;
  font-size: 14px;
}

.user-answer {
  border-left-color: var(--color-error);
  background: var(--color-error-bg);
}

.user-answer .answer-header {
  color: #DC2626;
}

.correct-answer {
  border-left-color: var(--color-success);
  background: var(--color-success-bg);
}

.correct-answer .answer-header {
  color: #059669;
}

.explanation {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border);
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.explanation-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  margin-bottom: 6px;
  color: var(--color-primary);
  font-size: 13px;
}

.explanation-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
}

/* Note Snippet */
.note-snippet {
  margin: 0 16px 12px;
  padding: 12px 16px;
  background: var(--color-warning-bg);
  border-radius: 6px;
  font-size: 13px;
  color: #92400E;
  display: flex;
  gap: 8px;
  cursor: pointer;
  border: 1px solid #FDE68A;
  transition: all var(--transition-fast);
}

.note-snippet:hover {
  border-color: #FCD34D;
}

.note-snippet p {
  margin: 0;
  line-height: 1.5;
}

/* Card Footer */
.card-footer {
  padding: 12px 16px;
  border-top: 1px solid var(--color-border-light);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--color-bg-secondary);
}

.date-text {
  font-size: 12px;
  color: var(--color-text-tertiary);
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}

/* Action Buttons */
.actions {
  display: flex;
  gap: 6px;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 32px;
  padding: 0 12px;
  border-radius: 6px;
  border: 1px solid var(--color-border);
  background: var(--color-bg);
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  white-space: nowrap;
}

.action-btn:hover:not(:disabled) {
  background: var(--color-bg-secondary);
  color: var(--color-text-primary);
  border-color: var(--color-border);
}

.action-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.action-btn.danger:hover:not(:disabled) {
  color: var(--color-error);
  background: var(--color-error-bg);
  border-color: #FCA5A5;
}

.action-btn.primary {
  background: var(--color-primary);
  color: white;
  border-color: transparent;
}

.action-btn.primary:hover:not(:disabled) {
  background: var(--color-primary-hover);
}

.action-btn.status-btn.active {
  color: var(--color-success);
  background: var(--color-success-bg);
  border-color: #6EE7B7;
}

.btn-text {
  display: inline;
}

@media (max-width: 640px) {
  .btn-text {
    display: none;
  }

  .action-btn {
    padding: 0 8px;
  }
}

/* Animation States */
.expand-enter-active,
.expand-leave-active {
  transition: all 200ms ease;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  max-height: 0;
  transform: translateY(-10px);
}

.expand-enter-to,
.expand-leave-from {
  opacity: 1;
  max-height: 600px;
  transform: translateY(0);
}

/* Respect user's motion preferences */
@media (prefers-reduced-motion: reduce) {
  .mistake-card,
  .action-btn,
  .note-snippet,
  .option-item {
    transition: none;
  }

  .expand-enter-active,
  .expand-leave-active {
    transition: none;
  }
}

/* Focus Visible for Keyboard Navigation */
.action-btn:focus-visible,
.note-snippet:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}
</style>
