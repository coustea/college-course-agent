<template>
  <el-dialog
      v-model="visible"
      :title="title"
      width="560px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      append-to-body
  >
    <div class="q-body">
      <div class="q-stem">{{ stem }}</div>
      <div class="q-options">
        <el-radio-group v-model="answerIndex" :disabled="resultShown">
          <el-radio :label="0" class="q-option">
            <template #default>
              <div class="option-content">
                <span class="badge">A</span>
                <span class="option-text">{{ options[0] }}</span>
              </div>
            </template>
          </el-radio>
          <el-radio :label="1" class="q-option">
            <template #default>
              <div class="option-content">
                <span class="badge">B</span>
                <span class="option-text">{{ options[1] }}</span>
              </div>
            </template>
          </el-radio>
          <el-radio :label="2" class="q-option">
            <template #default>
              <div class="option-content">
                <span class="badge">C</span>
                <span class="option-text">{{ options[2] }}</span>
              </div>
            </template>
          </el-radio>
          <el-radio :label="3" class="q-option">
            <template #default>
              <div class="option-content">
                <span class="badge">D</span>
                <span class="option-text">{{ options[3] }}</span>
              </div>
            </template>
          </el-radio>
        </el-radio-group>
      </div>

      <div v-if="resultShown" class="q-ans">
        <div class="q-ans-row">正确答案：<b>{{ correctLetter }}</b><span v-if="correctText">. {{ correctText }}</span></div>
        <div v-if="analysis" class="q-ans-analysis">{{ analysis }}</div>
      </div>
      <div v-if="resultShown" class="q-result" :class="{ ok: isCorrect, bad: !isCorrect }">
        <span>{{ isCorrect ? '回答正确' : '回答错误' }}</span>
      </div>
    </div>
    <template #footer>
      <el-button
          @click="onPrimaryClick"
          type="primary"
          :disabled="!resultShown && answerIndex===null"
          :loading="submitting"
      >{{ btnText }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import {ref, watch, computed} from 'vue'

const props = defineProps({
  modelValue: {type: Boolean, default: false},
  title: {type: String, default: '选择题'},
  stem: {type: String, default: ''},
  options: {type: Array, default: () => ['选项A', '选项B', '选项C', '选项D']},
  correctIndex: {type: Number, default: 0},
  analysis: {type: String, default: ''},
  // 新增：结果展示后的主按钮文案（例如：下一题/完成）
  nextText: {type: String, default: '继续学习'}
})
const emit = defineEmits(['update:modelValue', 'submit'])

const visible = ref(false)
watch(() => props.modelValue, v => {
  visible.value = v
}, {immediate: true})
watch(visible, v => {
  emit('update:modelValue', v)
  if (v) {
    // 打开时重置作答与结果展示
     answerIndex.value = null 
     resultShown.value = false 
  }
})

const answerIndex = ref(null)
const submitting = ref(false)
const resultShown = ref(false)
const isCorrect = computed(() => Number(answerIndex.value) === Number(props.correctIndex))
const correctLetter = computed(() => ['A','B','C','D'][Math.max(0, Math.min(3, Number(props.correctIndex)||0))])
const correctText = computed(() => {
  const idx = Math.max(0, Math.min(3, Number(props.correctIndex)||0))
  return Array.isArray(props.options) ? String(props.options[idx] || '') : ''
})
const btnText = computed(() => resultShown.value ? (props.nextText || '继续学习') : '提交')

async function onSubmit() {
  if (submitting.value) return
  submitting.value = true
  try {
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
.q-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 100%;
  overflow: hidden;
}

.q-stem {
  font-size: 18px;
  font-weight: 600;
  line-height: 1.7;
  color: #111827;
}

.q-options {
  padding: 8px 0;
}

.q-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  width: 100%;
}

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #e5e7eb;
  color: #6b7280;
  font-weight: 700;
  font-size: 14px;
  flex-shrink: 0;
}

.q-result {
  margin-top: 8px;
  color: #10b981;
}

.q-result.bad {
  color: #ef4444;
}

.q-ans {
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  padding: 10px 12px;
  border-radius: 8px;
  margin-bottom: 8px;
}

.q-ans-row {
  font-size: 14px;
  margin-bottom: 4px;
  color: #111827;
}

.q-ans-analysis {
  font-size: 13px;
  color: #475569;
  line-height: 1.7;
}

.q-options :deep(.el-radio.is-checked .badge) {
  background: #10b981;
  color: #fff;
}

.q-options :deep(.el-radio.is-checked .option-text) {
  color: #111827;
  font-weight: 600;
}


.option-content {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  max-width: 100%;
  overflow: hidden;
  justify-content: space-between;
}

.option-text {
  flex: 1;
  font-size: 15px;
  line-height: 1.5;
  font-weight: 500;
  color: #374151;
  word-break: break-word;
  overflow-wrap: break-word;
  max-width: 100%;
  min-width: 0;
  text-align: left;
  padding-right: 12px;
}
</style>


