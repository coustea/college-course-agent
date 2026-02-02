<template>
  <el-dialog
      v-model="visible"
      :title="title"
      width="600px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      append-to-body
      class="question-dialog"
  >
    <div class="q-body">
      <!-- 题干 -->
      <div class="q-stem">
        <span class="q-tag">单选题</span>
        <span v-html="renderedStem"></span>
      </div>

      <!-- 选项列表 -->
      <div class="q-options">
        <div
          v-for="(opt, idx) in normalizedOptions"
          :key="idx"
          class="option-item"
          :class="getOptionClass(idx)"
          @click="handleOptionClick(idx)"
        >
          <div class="option-prefix">{{ ['A', 'B', 'C', 'D'][idx] }}</div>
          <div class="option-content" v-html="renderMath(opt)"></div>

          <!-- 结果图标 -->
          <div class="option-status" v-if="resultShown">
            <i v-if="isCorrectIndex(idx)" class="fas fa-check-circle text-success"></i>
            <i v-else-if="isWrongSelection(idx)" class="fas fa-times-circle text-danger"></i>
          </div>
        </div>
      </div>

      <!-- 结果解析区域 -->
      <transition name="fade-slide">
        <div v-if="resultShown" class="analysis-panel" :class="isCorrect ? 'panel-success' : 'panel-error'">
          <div class="result-header">
            <div class="result-badge">
              <i class="fas" :class="isCorrect ? 'fa-smile' : 'fa-frown'"></i>
              {{ isCorrect ? '回答正确' : '回答错误' }}
            </div>
            <div class="correct-ref" v-if="!isCorrect">
              正确答案：<strong>{{ correctLetter }}</strong>
            </div>
          </div>

          <div class="analysis-content" v-if="analysis">
            <div class="analysis-label"><i class="fas fa-lightbulb"></i> 解析：</div>
            <div class="analysis-text" v-html="renderedAnalysis"></div>
          </div>
        </div>
      </transition>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button
            @click="onPrimaryClick"
            type="primary"
            size="large"
            :disabled="!resultShown && answerIndex===null"
            :loading="submitting"
            class="submit-btn"
        >
          {{ btnText }}
          <i class="fas" :class="resultShown ? 'fa-arrow-right' : 'fa-check'" style="margin-left: 6px;"></i>
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import {ref, watch, computed} from 'vue'
import katex from 'katex'
import 'katex/dist/katex.min.css'

const props = defineProps({
  modelValue: {type: Boolean, default: false},
  title: {type: String, default: '知识点测试'},
  stem: {type: String, default: ''},
  options: {type: Array, default: () => []},
  correctIndex: {type: Number, default: 0},
  analysis: {type: String, default: ''},
  nextText: {type: String, default: '继续学习'}
})
const emit = defineEmits(['update:modelValue', 'submit'])

const visible = ref(false)
watch(() => props.modelValue, v => { visible.value = v }, {immediate: true})
watch(visible, v => {
  emit('update:modelValue', v)
  if (v) {
     answerIndex.value = null
     resultShown.value = false
  }
})

const answerIndex = ref(null)
const submitting = ref(false)
const resultShown = ref(false)

// 确保选项始终为数组且有内容，防止索引越界
const normalizedOptions = computed(() => {
  const opts = props.options || []
  // 补齐4个选项以防万一，或者只展示实际传入的
  return opts.length > 0 ? opts : ['选项A', '选项B', '选项C', '选项D']
})

const isCorrect = computed(() => Number(answerIndex.value) === Number(props.correctIndex))
const correctLetter = computed(() => ['A','B','C','D'][Math.max(0, Math.min(3, Number(props.correctIndex)||0))])
const btnText = computed(() => resultShown.value ? (props.nextText || '继续学习') : '提交答案')

// 数学公式渲染函数
function renderMath(text) {
  if (!text) return ''
  // 匹配 $...$ 和 $$...$$ 格式的 LaTeX 公式
  return text.replace(/\$\$([\s\S]+?)\$\$/g, (match, formula) => {
    try {
      return katex.renderToString(formula.trim(), { displayMode: true, throwOnError: false })
    } catch (e) {
      console.warn('KaTeX render error:', e)
      return match
    }
  }).replace(/\$([^$]+?)\$/g, (match, formula) => {
    try {
      return katex.renderToString(formula.trim(), { displayMode: false, throwOnError: false })
    } catch (e) {
      console.warn('KaTeX render error:', e)
      return match
    }
  })
}

// 渲染后的题干和解析
const renderedStem = computed(() => renderMath(props.stem))
const renderedAnalysis = computed(() => renderMath(props.analysis))

// 选项样式计算
function getOptionClass(idx) {
  const isSelected = answerIndex.value === idx
  const isTargetCorrect = Number(props.correctIndex) === idx

  if (!resultShown.value) {
    return { 'is-selected': isSelected }
  }

  // 结果展示模式
  if (isTargetCorrect) return 'is-correct' // 正确答案总是绿色
  if (isSelected && !isTargetCorrect) return 'is-wrong' // 选错的显示红色
  return 'is-disabled' // 其他非正确选项变淡
}

// 辅助判断函数
function isCorrectIndex(idx) {
  return Number(props.correctIndex) === idx
}
function isWrongSelection(idx) {
  return answerIndex.value === idx && Number(props.correctIndex) !== idx
}

function handleOptionClick(idx) {
  if (resultShown.value) return
  answerIndex.value = idx
}

async function onSubmit() {
  if (submitting.value) return
  submitting.value = true
  try {
    // 模拟一点延迟感，或者直接显示
    resultShown.value = true
    emit('submit', {correct: isCorrect.value, answerIndex: answerIndex.value})
  } finally {
    submitting.value = false
  }
}

function onPrimaryClick() {
  if (!resultShown.value) {
    onSubmit()
  } else {
    visible.value = false
  }
}
</script>

<style scoped>
.question-dialog :deep(.el-dialog__body) {
  padding: 24px 32px;
}

.q-body {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 题干样式 */
.q-stem {
  font-size: 18px;
  font-weight: 600;
  line-height: 1.6;
  color: #1f2937;
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.q-tag {
  background: #eef2ff;
  color: #4f46e5;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  white-space: nowrap;
  margin-top: 4px;
  font-weight: 600;
}

/* 选项列表 */
.q-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
  background: #fff;
}

.option-item:hover:not(.is-disabled):not(.is-correct):not(.is-wrong) {
  border-color: #d1d5db;
  background: #f9fafb;
}

.option-item.is-selected {
  border-color: #4f46e5;
  background: #eef2ff;
  box-shadow: 0 0 0 1px #4f46e5;
}

.option-item.is-correct {
  border-color: #10b981;
  background: #ecfdf5;
  color: #065f46;
}

.option-item.is-wrong {
  border-color: #ef4444;
  background: #fef2f2;
  color: #991b1b;
}

.option-item.is-disabled {
  opacity: 0.6;
  cursor: default;
  border-color: #f3f4f6;
}

.option-prefix {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  border-radius: 50%;
  font-weight: 700;
  color: #6b7280;
  margin-right: 16px;
  flex-shrink: 0;
  transition: all 0.2s;
}

.option-item.is-selected .option-prefix {
  background: #4f46e5;
  color: #fff;
}
.option-item.is-correct .option-prefix {
  background: #10b981;
  color: #fff;
}
.option-item.is-wrong .option-prefix {
  background: #ef4444;
  color: #fff;
}

.option-content {
  flex: 1;
  font-size: 15px;
  line-height: 1.5;
  font-weight: 500;
}

.option-status {
  margin-left: 12px;
  font-size: 20px;
}
.text-success { color: #10b981; }
.text-danger { color: #ef4444; }

/* 结果解析面板 */
.analysis-panel {
  background: #f8fafc;
  border-radius: 12px;
  padding: 20px;
  margin-top: 8px;
  border: 1px solid #e2e8f0;
}

.panel-success {
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.panel-error {
  background: #fef2f2;
  border-color: #fecaca;
}

.result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.result-badge {
  font-size: 16px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
}
.panel-success .result-badge { color: #166534; }
.panel-error .result-badge { color: #991b1b; }

.correct-ref {
  font-size: 14px;
  color: #ef4444;
  font-weight: 500;
}

.analysis-content {
  padding-top: 12px;
  border-top: 1px solid rgba(0,0,0,0.05);
}

.analysis-label {
  font-size: 13px;
  font-weight: 700;
  color: #4b5563;
  margin-bottom: 4px;
}

.analysis-text {
  font-size: 14px;
  color: #374151;
  line-height: 1.6;
}

/* 动画 */
.fade-slide-enter-active {
  transition: all 0.3s ease-out;
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(-10px);
}

.submit-btn {
  width: 100%;
  font-weight: 600;
  letter-spacing: 1px;
}
</style>