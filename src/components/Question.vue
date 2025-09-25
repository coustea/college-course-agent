<template>
  <div v-if="modelValue" class="q-mask" @click.self="handleClose">
    <div class="q-modal">
      <div class="q-header">
        <div class="q-title">{{ title }}</div>
        <button class="q-close" :class="{ disabled: !canExit }" :disabled="!canExit" @click="handleClose" aria-label="close"><i class="fas fa-times"></i></button>
      </div>
      <div class="q-body">
        <template v-if="hasQuestions">
          <div class="q-step" v-if="questions.length > 1">题目 {{ currentIndex + 1 }} / {{ questions.length }}</div>

          <div class="q-stem" v-html="currentQuestion.stem || '请回答以下问题'" />

          <div class="q-options">
            <template v-if="currentQuestion.type === 'multiple'">
              <label v-for="opt in currentQuestion.options || []" :key="opt.value" class="q-option"
                     :class="submitted ? optionClass(currentQuestion, opt.value) : ''">
                <input type="checkbox"
                       :value="opt.value"
                       :checked="isChecked(currentQuestion.id, opt.value)"
                       @change="toggleMultiple(currentQuestion.id, opt.value)"
                       :disabled="submitted" />
                <span class="q-option-label" v-html="opt.label"></span>
              </label>
            </template>
            <template v-else>
              <label v-for="opt in currentQuestion.options || []" :key="opt.value" class="q-option"
                     :class="submitted ? optionClass(currentQuestion, opt.value) : ''">
                <input type="radio"
                       :name="`q-${currentQuestion.id}`"
                       :value="opt.value"
                       :checked="isSelected(currentQuestion.id, opt.value)"
                       @change="selectSingle(currentQuestion.id, opt.value)"
                       :disabled="submitted" />
                <span class="q-option-label" v-html="opt.label"></span>
              </label>
            </template>
          </div>

          <div class="q-result" v-if="submitted">
            <span :class="isQuestionCorrect(currentQuestion) ? 'ok' : 'bad'">
              {{ isQuestionCorrect(currentQuestion) ? '回答正确' : '回答错误' }}
            </span>
          </div>

          <div class="q-actions">
            <button class="q-btn" v-if="questions.length > 1" :disabled="currentIndex === 0" @click="prev">上一题</button>
            <div class="q-actions-spacer"></div>
            <button class="q-btn" v-if="questions.length > 1 && currentIndex < questions.length - 1" @click="next">下一题</button>
            <button class="q-btn q-primary" :disabled="!canSubmit" @click="submit">提交并查看结果</button>
          </div>
        </template>
        <template v-else>
          <div class="q-empty">暂无题目</div>
        </template>
      </div>
    </div>
  </div>

</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  questions: { type: Array, default: () => [] },
  title: { type: String, default: '知识检查' },
  closable: { type: Boolean, default: true },
  requireAll: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue', 'submit'])

const hasQuestions = computed(() => Array.isArray(props.questions) && props.questions.length > 0)
const currentIndex = ref(0)

watch(() => props.modelValue, (v) => {
  if (v) currentIndex.value = 0
})

const currentQuestion = computed(() => props.questions?.[currentIndex.value] || {})

const answers = ref({})
const submitted = ref(false)

function selectSingle(qid, val) {
  answers.value = { ...answers.value, [qid]: val }
}

function isSelected(qid, val) {
  return answers.value?.[qid] === val
}

function toggleMultiple(qid, val) {
  const prev = Array.isArray(answers.value?.[qid]) ? answers.value[qid].slice() : []
  const idx = prev.indexOf(val)
  if (idx >= 0) prev.splice(idx, 1); else prev.push(val)
  answers.value = { ...answers.value, [qid]: prev }
}

function isChecked(qid, val) {
  return Array.isArray(answers.value?.[qid]) && answers.value[qid].includes(val)
}

const totalRequired = computed(() => props.questions?.length || 0)

const isCurrentAnswered = computed(() => {
  const q = currentQuestion.value
  if (!q?.id) return false
  const val = answers.value?.[q.id]
  if (q.type === 'multiple') return Array.isArray(val) && val.length > 0
  return val !== undefined && val !== null && val !== ''
})

const canSubmit = computed(() => {
  if (!hasQuestions.value) return false
  if (props.requireAll) return Object.keys(answers.value || {}).length >= totalRequired.value && isCurrentAnswered.value
  return Object.keys(answers.value || {}).length > 0 && isCurrentAnswered.value
})

function prev() {
  if (currentIndex.value > 0) currentIndex.value -= 1
}

function next() {
  if (!isCurrentAnswered.value) return
  if (currentIndex.value < (props.questions.length - 1)) currentIndex.value += 1
}

function submit() {
  submitted.value = true
  const results = {}
  for (const q of (props.questions || [])) {
    results[q.id] = isQuestionCorrect(q)
  }
  emit('submit', { answers: { ...answers.value }, questions: props.questions, results })
}

function handleClose() {
  if (!canExit.value) return
  emit('update:modelValue', false)
}

const canExit = computed(() => {
  // 只有题目解答完（全部题已作答并已提交）才可以退出
  const allAnswered = Object.keys(answers.value || {}).length >= (props.questions?.length || 0)
  return submitted.value && allAnswered
})

function isQuestionCorrect(q) {
  if (!q) return false
  const val = answers.value?.[q.id]
  if (q.type === 'multiple') {
    const ans = Array.isArray(q.answer) ? q.answer.slice().sort() : []
    const got = Array.isArray(val) ? val.slice().sort() : []
    return JSON.stringify(ans) === JSON.stringify(got)
  }
  return q.answer !== undefined && q.answer === val
}

function optionClass(q, value) {
  const correct = q.type === 'multiple'
      ? Array.isArray(q.answer) && q.answer.includes(value)
      : q.answer === value
  const chosen = q.type === 'multiple'
      ? Array.isArray(answers.value?.[q.id]) && answers.value[q.id].includes(value)
      : answers.value?.[q.id] === value
  return correct ? 'opt-correct' : (chosen ? 'opt-wrong' : '')
}
</script>

<style scoped>
.q-mask {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2200;
}
.q-modal {
  width: 92%;
  max-width: 720px;
  max-height: 86vh;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.q-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: linear-gradient(135deg, #1a56db 0%, #0d3b9e 100%);
  color: #fff;
}
.q-title { font-weight: 700; }
.q-close {
  border: 0;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: rgba(255,255,255,0.2);
  color: #fff;
  cursor: pointer;
}
.q-body {
  padding: 16px;
  overflow: auto;
}
.q-step {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 8px;
}
.q-stem {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}
.q-options { display: flex; flex-direction: column; gap: 8px; }
.q-option { display: flex; gap: 10px; align-items: flex-start; }
.q-option input { margin-top: 3px; }
.q-option-label { line-height: 1.6; }

.q-option.opt-correct { background: #ecfdf5; border-radius: 6px; padding: 6px 8px; }
.q-option.opt-wrong { background: #fff1f2; border-radius: 6px; padding: 6px 8px; }

.q-result { margin-top: 10px; font-size: 14px; }
.q-result .ok { color: #10b981; }
.q-result .bad { color: #ef4444; }

.q-actions { display: flex; align-items: center; margin-top: 16px; }
.q-actions-spacer { flex: 1; }
.q-btn {
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid #e1e8ef;
  background: #fff;
  cursor: pointer;
}
.q-btn:disabled { opacity: .6; cursor: not-allowed; }
.q-primary { background: #2563eb; color: #fff; border: none; }
.q-primary:hover { background: #1d4ed8; }

.q-empty { color: #64748b; text-align: center; padding: 24px 0; }

.q-close.disabled { opacity: .5; cursor: not-allowed; }
</style>
