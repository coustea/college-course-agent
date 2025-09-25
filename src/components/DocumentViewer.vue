<template>
  <div v-if="modelValue" class="dv-mask" @click.self="close">
    <div class="dv-modal">
      <div class="dv-header">
        <div class="dv-title">{{ title }}</div>
        <button class="dv-close" @click="close"><i class="fas fa-times"></i></button>
      </div>
      <div class="dv-body">
        <template v-if="isIframe">
          <iframe class="dv-iframe" :src="fileUrl" title="document" referrerpolicy="no-referrer" />
        </template>
        <template v-else>
          <div class="dv-content" v-html="safeHtml"></div>
        </template>
      </div>
      <div class="dv-footer">
        <div class="dv-progress">阅读进度：{{ progress }}%</div>
        <div class="dv-actions">
          <button class="dv-btn" @click="emit('next')">下一节</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, watch, onBeforeUnmount } from 'vue'
import { addOrUpdateRecord, addTime } from '@/services/student/historyService.js'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '文档课程' },
  fileUrl: { type: String, default: '' },
  htmlContent: { type: String, default: '' },
  progress: { type: Number, default: 0 },
  id: { type: [String, Number], default: null },
  image: { type: String, default: '' },
  duration: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue', 'next'])

const isIframe = computed(() => !!props.fileUrl)
const safeHtml = computed(() => props.htmlContent || '<p style="color:#666">暂无内容</p>')

function close() { emit('update:modelValue', false) }

let timer = null
let last = 0
watch(() => props.modelValue, (v) => {
  if (v) {
    addOrUpdateRecord({ id: props.id || props.title, type: 'static', title: props.title, image: props.image, duration: props.duration, progress: props.progress || 0 })
    last = Date.now()
    if (!timer) {
      timer = setInterval(() => {
        const now = Date.now(); const delta = Math.floor((now - last) / 1000)
        if (delta > 0) { addTime(props.id || props.title, delta); last = now }
      }, 1000)
    }
  } else {
    if (timer) { const now = Date.now(); const delta = Math.floor((now - last) / 1000); if (delta > 0) addTime(props.id || props.title, delta); clearInterval(timer); timer = null }
  }
})

onBeforeUnmount(() => { if (timer) { clearInterval(timer); timer = null } })
</script>

<style scoped>
.dv-mask {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1200;
}
.dv-modal {
  width: 90%;
  max-width: 1000px;
  max-height: 90vh;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.dv-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: linear-gradient(135deg, #1a56db 0%, #0d3b9e 100%);
  color: #fff;
}
.dv-title {
  font-weight: 700;
}
.dv-close {
  border: 0;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: rgba(255,255,255,0.2);
  color: #fff;
  cursor: pointer;
}
.dv-body {
  padding: 0;
  height: 70vh;
  background: #fafafa;
}
.dv-iframe {
  width: 100%;
  height: 100%;
  border: 0;
}
.dv-content {
  padding: 16px;
  color: #333;
  line-height: 1.7;
}
.dv-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: #f6f8fb;
}
.dv-progress {
  color: #64748b;
  font-size: 14px;
}
.dv-btn {
  padding: 8px 12px;
  background: #2563eb;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}
.dv-btn:hover { background: #1d4ed8; }
</style>
