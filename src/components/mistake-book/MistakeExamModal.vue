<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    title="错题组卷练习"
    width="800px"
    class="exam-dialog"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    destroy-on-close
  >
    <div v-if="examQuestions.length === 0" class="exam-empty">
      <i class="fas fa-check-circle empty-icon"></i>
      <h3 class="empty-title">太棒了!</h3>
      <p class="empty-text">当前没有待复习的错题可以练习。</p>
      <el-button type="primary" round @click="$emit('update:visible', false)">返回错题本</el-button>
    </div>

    <div v-else class="exam-container">
      <!-- Header: Progress and Score -->
      <div class="exam-header">
        <div class="exam-progress">
          <i class="fas fa-list-ol"></i>
          <span>进度：<strong>{{ currentExamIndex + 1 }}</strong> / {{ examQuestions.length }}</span>
        </div>
        <transition name="score-fade">
          <div v-if="examSubmitted" class="exam-score">
            <i class="fas fa-graduation-cap"></i>
            <span>得分：<strong class="score-value">{{ examScore }}</strong> / 100</span>
          </div>
        </transition>
      </div>

      <!-- Question Card -->
      <div class="exam-question-card">
        <p class="q-title">
          <span class="q-index">{{ currentExamIndex + 1 }}.</span>
          {{ currentQuestion.question }}
        </p>

        <!-- Choices -->
        <div class="choice-list exam-mode">
          <div
            v-for="(option, index) in currentQuestion.options"
            :key="index"
            :class="['choice-item', {
              'selected': examAnswers[currentQuestion.id] === index,
              'correct': examSubmitted && index === currentQuestion.correctIndex,
              'wrong': examSubmitted && examAnswers[currentQuestion.id] === index && index !== currentQuestion.correctIndex,
              'disabled': examSubmitted
            }]"
            @click="selectAnswer(currentQuestion.id, index)"
          >
            <span class="choice-circle">{{ ['A', 'B', 'C', 'D'][index] }}</span>
            <span class="choice-text">{{ option }}</span>
             <i v-if="examSubmitted && index === currentQuestion.correctIndex" class="fas fa-check status-icon correct"></i>
             <i v-else-if="examSubmitted && examAnswers[currentQuestion.id] === index && index !== currentQuestion.correctIndex" class="fas fa-times status-icon wrong"></i>
          </div>
        </div>

        <!-- Analysis (shown after submission) -->
        <transition name="expand">
          <div v-if="examSubmitted" class="exam-analysis">
            <div class="analysis-title"><i class="fas fa-lightbulb"></i> 解析</div>
            <div class="analysis-text">{{ currentQuestion.explanation }}</div>
            <div class="analysis-actions" v-if="!currentQuestion.mastered">
              <el-button size="small" type="success" plain round @click="markAsMastered(currentQuestion)">
                <i class="fas fa-check-circle"></i> 我已掌握
              </el-button>
            </div>
          </div>
        </transition>
      </div>
    </div>

    <!-- Footer: Navigation -->
    <template #footer>
      <div class="exam-footer">
        <el-button @click="prevQuestion" :disabled="currentExamIndex === 0">
          <i class="fas fa-chevron-left"></i>&nbsp; 上一题
        </el-button>

        <el-button
          v-if="!isLastQuestion"
          type="primary"
          @click="nextQuestion"
        >
          下一题 &nbsp;<i class="fas fa-chevron-right"></i>
        </el-button>

        <el-button
          v-if="isLastQuestion && !examSubmitted"
          type="success"
          @click="submit"
          :disabled="!allAnswered"
        >
          <i class="fas fa-check"></i> 提交试卷
        </el-button>

        <el-button
          v-if="examSubmitted"
          type="primary"
          @click="$emit('update:visible', false)"
        >
          完成练习
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { ElMessage } from 'element-plus';

const props = defineProps({
  visible: { type: Boolean, default: false },
  examQuestions: { type: Array, default: () => [] }
});

const emit = defineEmits(['update:visible', 'mark-mastered']);

// Internal State
const currentExamIndex = ref(0);
const examAnswers = ref({});
const examSubmitted = ref(false);
const examScore = ref(0);

// Reset state when modal opens
watch(() => props.visible, (newVal) => {
  if (newVal && props.examQuestions.length > 0) {
    currentExamIndex.value = 0;
    examAnswers.value = {};
    examSubmitted.value = false;
    examScore.value = 0;
  }
});

// Computed
const currentQuestion = computed(() => props.examQuestions[currentExamIndex.value]);
const isLastQuestion = computed(() => currentExamIndex.value === props.examQuestions.length - 1);
const allAnswered = computed(() => Object.keys(examAnswers.value).length === props.examQuestions.length);

// Methods
const selectAnswer = (mistakeId, index) => {
  if (examSubmitted.value) return;
  examAnswers.value[mistakeId] = index;
};

const nextQuestion = () => {
  if (!isLastQuestion.value) currentExamIndex.value++;
};

const prevQuestion = () => {
  if (currentExamIndex.value > 0) currentExamIndex.value--;
};

const submit = () => {
  let correctCount = 0;
  props.examQuestions.forEach(q => {
    if (examAnswers.value[q.id] === q.correctIndex) {
      correctCount++;
    }
  });

  examScore.value = Math.round((correctCount / props.examQuestions.length) * 100);
  examSubmitted.value = true;

  if (examScore.value === 100) ElMessage.success({ message: `得分 ${examScore.value} - 完美！`, type: 'success' });
  else if (examScore.value >= 60) ElMessage.info(`得分 ${examScore.value}，继续加油！`);
  else ElMessage.warning(`得分 ${examScore.value}，还需多加练习哦。`);
};

const markAsMastered = (mistake) => {
  emit('mark-mastered', mistake);
};
</script>

<style scoped>
/* Clean White Design System */
:root {
  --color-bg: #FFFFFF;
  --color-bg-secondary: #F9FAFB;
  --color-text-primary: #111827;
  --color-text-secondary: #6B7280;
  --color-border: #E5E7EB;
  --color-primary: #3B82F6;
  --color-primary-hover: #2563EB;
  --color-primary-light: #EFF6FF;
  --color-success: #10B981;
  --color-success-bg: #D1FAE5;
  --color-error: #EF4444;
  --color-error-bg: #FEE2E2;

  --transition-fast: 150ms ease;
  --transition-base: 200ms ease;
}

.exam-dialog :deep(.el-dialog__body) {
  padding: 0;
  background-color: var(--color-bg-secondary);
}

.exam-dialog :deep(.el-dialog__header) {
  font-weight: 600;
}

.exam-dialog :deep(.el-dialog__title) {
  font-weight: 600;
  color: var(--color-text-primary);
}

.exam-container {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-height: 450px;
}

.exam-empty {
  padding: 48px 32px;
  text-align: center;
}

.empty-icon {
  font-size: 64px;
  color: var(--color-success);
  margin-bottom: 16px;
  opacity: 0.8;
}

.empty-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 8px 0;
}

.empty-text {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 8px 0 32px 0;
}

/* Exam Header */
.exam-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--color-bg);
  padding: 16px 20px;
  border-radius: 8px;
  border: 1px solid var(--color-border);
}

.exam-progress,
.exam-score {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.score-value {
  color: var(--color-primary);
  font-size: 24px;
  font-weight: 700;
}

/* Question Card */
.exam-question-card {
  background: var(--color-bg);
  border-radius: 8px;
  padding: 24px;
  border: 1px solid var(--color-border);
  flex: 1;
}

.q-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 20px 0;
  line-height: 1.6;
  color: var(--color-text-primary);
}

.q-index {
  color: var(--color-primary);
  margin-right: 8px;
  font-weight: 700;
}

/* Analysis Section */
.exam-analysis {
  margin-top: 20px;
  padding: 16px;
  background: var(--color-success-bg);
  border-radius: 8px;
  border: 1px solid #6EE7B7;
}

.analysis-title {
  font-weight: 600;
  color: #065F46;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.analysis-text {
  color: var(--color-text-primary);
  line-height: 1.6;
  font-size: 14px;
}

.analysis-actions {
  margin-top: 16px;
  text-align: right;
}

/* Footer */
.exam-footer {
  width: 100%;
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

/* Animations */
.score-fade-enter-active {
  transition: all 0.3s ease;
}

.score-fade-enter-from {
  opacity: 0;
  transform: translateY(-10px);
}

.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease;
}

.expand-enter-from,
.expand-leave-to {
  max-height: 0;
  opacity: 0;
  margin-top: 0;
}

.expand-enter-to,
.expand-leave-from {
  max-height: 500px;
  opacity: 1;
}
</style>

<style>
/* Global styles for choice items in the modal */
.choice-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.choice-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 8px;
  border: 1px solid #E5E7EB;
  background: #F9FAFB;
  transition: all 0.15s ease;
  cursor: pointer;
}

.choice-item:hover:not(.disabled) {
  border-color: #3B82F6;
  background: #EFF6FF;
}

.choice-item.selected {
  border-color: #3B82F6;
  background: #EFF6FF;
}

.choice-item.disabled {
  pointer-events: none;
  opacity: 0.8;
}

.choice-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #E5E7EB;
  color: #6B7280;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
  transition: all 0.15s ease;
}

.choice-item.selected .choice-circle {
  background: #3B82F6;
  color: white;
}

.choice-text {
  font-size: 14px;
  color: #111827;
  flex: 1;
  line-height: 1.5;
}

.choice-item.correct {
  background-color: #D1FAE5;
  border-color: #10B981;
}

.choice-item.correct .choice-circle {
  background-color: #10B981;
  color: white;
}

.choice-item.wrong {
  background-color: #FEE2E2;
  border-color: #EF4444;
}

.choice-item.wrong .choice-circle {
  background-color: #EF4444;
  color: white;
}

.status-icon {
  font-size: 16px;
  margin-left: auto;
}

.status-icon.correct {
  color: #10B981;
}

.status-icon.wrong {
  color: #EF4444;
}
</style>
