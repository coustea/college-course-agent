<template>
  <div class="survey-container">
    <div class="survey-header">
      <h1>思政课程问卷调查</h1>
      <p class="survey-description">亲爱的同学，为了更好地了解思政课程的学习效果，请您配合完成以下问卷调查。您的意见对我们非常重要！</p>
    </div>

    <div class="survey-content">
      <form @submit.prevent="submitSurvey" class="survey-form">
        <div class="question-block">
          <div class="question-title">
            <span class="question-number">1.</span>
            您对当前思政课程的教学内容满意度如何？
          </div>
          <div class="options">
            <label class="option-item" v-for="option in question1Options" :key="option.value">
              <input
                type="radio"
                name="question1"
                :value="option.value"
                v-model="formData.question1"
              >
              <span class="radio-custom"></span>
              <span class="option-text">{{ option.label }}</span>
            </label>
          </div>
        </div>

        <div class="question-block">
          <div class="question-title">
            <span class="question-number">2.</span>
            您认为思政课程对您的价值观培养有什么影响？
          </div>
          <div class="textarea-wrapper">
            <textarea
              v-model="formData.question2"
              placeholder="请详细描述您的感受和看法..."
              rows="6"
              class="survey-textarea"
            ></textarea>
            <div class="char-count">{{ formData.question2.length }}/500</div>
          </div>
        </div>

        <div class="form-actions">
          <button type="button" class="btn btn-secondary" @click="resetForm">重置</button>
          <button type="submit" class="btn btn-primary" :disabled="!isFormValid">提交问卷</button>
        </div>
      </form>
    </div>

    <div v-if="showSuccessMessage" class="success-overlay">
      <div class="success-modal">
        <div class="success-icon">
          <i class="fas fa-check-circle"></i>
        </div>
        <h3>提交成功！</h3>
        <p>感谢您的参与，您的意见对我们非常宝贵</p>
        <button class="btn btn-primary" @click="goToHome">确定</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
//目前都是假的后面要接接口，只有回到主页的逻辑不用改
const formData = ref({
  question1: '',
  question2: ''
})

const showSuccessMessage = ref(false)

const question1Options = [
  { value: 'very-satisfied', label: '非常满意' },
  { value: 'satisfied', label: '满意' },
  { value: 'neutral', label: '一般' },
  { value: 'dissatisfied', label: '不满意' },
  { value: 'very-dissatisfied', label: '非常不满意' }
]

const isFormValid = computed(() => {
  return formData.value.question1 && formData.value.question2.trim().length > 0
})

const submitSurvey = () => {
  if (!isFormValid.value) {
    alert('请完成所有必填项')
    return
  }

  console.log('问卷数据:', formData.value)
  showSuccessMessage.value = true
}

const resetForm = () => {
  formData.value = {
    question1: '',
    question2: ''
  }
}

const goToHome = () => {
  showSuccessMessage.value = false
  router.push('/')
}
</script>

<style scoped>
.survey-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.survey-header {
  text-align: center;
  margin-bottom: 40px;
  padding: 30px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  color: white;
}

.survey-header h1 {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 16px;
}

.survey-description {
  font-size: 16px;
  line-height: 1.6;
  opacity: 0.9;
}

.survey-content {
  background: white;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.question-block {
  margin-bottom: 40px;
}

.question-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
  display: flex;
  align-items: flex-start;
  line-height: 1.5;
}

.question-number {
  color: #667eea;
  margin-right: 8px;
  font-weight: 700;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.option-item {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 16px;
  border: 2px solid #e1e5e9;
  border-radius: 12px;
  transition: all 0.3s ease;
  background: #f8f9fa;
}

.option-item:hover {
  border-color: #667eea;
  background: #f0f2ff;
}

.option-item input[type="radio"] {
  display: none;
}

.radio-custom {
  width: 20px;
  height: 20px;
  border: 2px solid #ddd;
  border-radius: 50%;
  margin-right: 12px;
  position: relative;
  transition: all 0.3s ease;
}

.option-item input[type="radio"]:checked + .radio-custom {
  border-color: #667eea;
  background: #667eea;
}

.option-item input[type="radio"]:checked + .radio-custom::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: white;
}

.option-text {
  font-size: 16px;
  color: #333;
}

.textarea-wrapper {
  position: relative;
}

.survey-textarea {
  width: 100%;
  padding: 16px;
  border: 2px solid #e1e5e9;
  border-radius: 12px;
  font-size: 16px;
  line-height: 1.5;
  resize: vertical;
  min-height: 120px;
  font-family: inherit;
  transition: border-color 0.3s ease;
}

.survey-textarea:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.survey-textarea::placeholder {
  color: #999;
}

.char-count {
  position: absolute;
  bottom: 12px;
  right: 16px;
  font-size: 12px;
  color: #999;
  background: white;
  padding: 2px 6px;
  border-radius: 4px;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 40px;
  padding-top: 30px;
  border-top: 1px solid #e1e5e9;
}

.btn {
  padding: 14px 32px;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 120px;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.3);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-secondary {
  background: #f8f9fa;
  color: #666;
  border: 2px solid #e1e5e9;
}

.btn-secondary:hover {
  background: #e9ecef;
  border-color: #ddd;
}

/* 成功提示样式 */
.success-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.success-modal {
  background: white;
  padding: 40px;
  border-radius: 16px;
  text-align: center;
  max-width: 400px;
  width: 90%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.success-icon {
  font-size: 64px;
  color: #28a745;
  margin-bottom: 20px;
}

.success-modal h3 {
  font-size: 24px;
  color: #333;
  margin-bottom: 16px;
}

.success-modal p {
  color: #666;
  line-height: 1.6;
  margin-bottom: 30px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .survey-container {
    padding: 16px;
  }

  .survey-header {
    padding: 20px;
  }

  .survey-header h1 {
    font-size: 24px;
  }

  .survey-content {
    padding: 24px;
  }

  .question-title {
    font-size: 16px;
  }

  .form-actions {
    flex-direction: column;
    align-items: center;
  }

  .btn {
    width: 100%;
    max-width: 200px;
  }
}
</style>
