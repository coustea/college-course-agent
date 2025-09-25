<template>
  <div class="header">
    <h1 class="page-title">作品提交</h1>
  </div>
  <div class="card">
    <div class="field">
      <label>作业标题</label>
      <input class="title-input" type="text" v-model="title" placeholder="请输入作业标题" />
    </div>
    <div class="field">
      <label>作品描述</label>
      <textarea v-model="description" placeholder="请输入作品描述"></textarea>
    </div>
    <div class="field">
      <label>上传作品（图片或文档）</label>
      <div class="upload" :class="{dragover: dragOver}" @dragenter.prevent="dragOver=true" @dragover.prevent @dragleave.prevent="dragOver=false" @drop.prevent="onDrop">
        <div class="upload-inner">
          <i class="fas fa-cloud-upload-alt"></i>
          <div>拖拽到此处或</div>
          <button class="btn" type="button" @click="chooseFiles">选择文件</button>
          <input ref="fileInput" type="file" class="hidden" multiple :accept="accepts" @change="onChoose" />
        </div>
      </div>
      <ul class="file-list" v-if="files.length">
        <li v-for="(f, i) in files" :key="i">
          <span class="name">{{ f.name }}</span>
          <button class="link" type="button" @click="remove(i)">删除</button>
        </li>
      </ul>
    </div>
    <div class="actions">
      <div class="deadline">截至时间：{{ deadlineLabel }}</div>
      <button class="btn primary" type="button" @click="submit">提交作品</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const title = ref('')
const description = ref('')
const files = ref([])
const dragOver = ref(false)
const fileInput = ref(null)
const accepts = '.png,.jpg,.jpeg,.gif,.bmp,.svg,.webp,.pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.txt'

const endTime = ref(formatDateTime(new Date(Date.now() + 7 * 24 * 3600 * 1000)))

const status = ref(localStorage.getItem('work_status') || 'none')

function chooseFiles() { fileInput.value?.click() }
function onChoose(e) { addFiles(e.target.files) }
function onDrop(e) { dragOver.value=false; addFiles(e.dataTransfer?.files) }
function addFiles(list){ if(!list) return; files.value = [...files.value, ...Array.from(list)] }
function remove(i){ files.value.splice(i,1) }

function submit(){
  // 保存提交记录（最多保留最近50条）
  const titleVal = computeTitle()
  const record = { id: Date.now(), title: titleVal, submittedAt: new Date().toISOString() }
  try {
    const raw = localStorage.getItem('work_submissions_v1') || '[]'
    const arr = Array.isArray(JSON.parse(raw)) ? JSON.parse(raw) : []
    arr.unshift(record)
    localStorage.setItem('work_submissions_v1', JSON.stringify(arr.slice(0, 50)))
  } catch {}

  try { localStorage.setItem('work_deadline_v1', endTime.value || '') } catch {}
  localStorage.setItem('work_status', 'submitted')
  status.value = 'submitted'
  try { window.dispatchEvent(new Event('work-status-updated')) } catch {}
  try { window.dispatchEvent(new Event('work-submitted')) } catch {}
  try { window.dispatchEvent(new Event('work-deadline-updated')) } catch {}
  alert('提交成功')
}

function computeTitle() {
  if (title.value.trim()) return title.value.trim().slice(0, 50)
  if (files.value.length > 0) {
    const n = files.value[0].name || ''
    const dot = n.lastIndexOf('.')
    return dot > 0 ? n.slice(0, dot) : n
  }
  const text = (description.value || '').trim()
  if (text) return text.slice(0, 20)
  return '未命名作品'
}

watch(endTime, (v) => {
  try { localStorage.setItem('work_deadline_v1', v || '') } catch {}
  try { window.dispatchEvent(new Event('work-deadline-updated')) } catch {}
})

function formatDateTime(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth()+1).padStart(2,'0')
  const d = String(date.getDate()).padStart(2,'0')
  const hh = String(date.getHours()).padStart(2,'0')
  const mm = String(date.getMinutes()).padStart(2,'0')
  const ss = String(date.getSeconds()).padStart(2,'0')
  return `${y}-${m}-${d}T${hh}:${mm}:${ss}`
}

const deadlineLabel = computed(() => {
  try { return (endTime.value || '').replace('T',' ') } catch { return '' }
})
</script>

<style scoped>
.content {
  max-width: 100%;
  margin: 0 auto;
}
.header {
  display:flex;
  align-items:center;
  justify-content:space-between;
  margin-bottom:18px;
}
.page-title {
  font-size:24px;
  font-weight:700;
  color:#2c3e50;
}
.status-chip {
  padding:6px 12px;
  border-radius:999px;
  font-weight:700;
  font-size:12px;
}
.card {
  background:#fff;
  border-radius:10px;
  box-shadow:0 4px 10px rgba(0,0,0,.05);
  padding:20px;
}
.grid-2 {
  display:grid;
  grid-template-columns: 1fr 1fr;
  gap:16px;
}
label {
  display:block;
  margin-bottom:6px;
  color:#2c3e50;
  font-weight:600;
}
input[type="datetime-local"], textarea {
  width:100%;
  border:1px solid #dcdfe6;
  border-radius:6px;
  padding:10px;
}
textarea {
  min-height:120px;
}
.field {
  margin-top:16px;
}
.title-input {
  width:100%;
  border:1px solid #dcdfe6;
  border-radius:6px;
  padding:10px;
}
.upload {
  border:2px dashed #cbd5e1;
  border-radius:8px;
  background:#fafafa;
}
.upload.dragover {
  background: rgba(37,99,235,.05);
  border-color:#2563eb;
}
.upload-inner {
  text-align:center;
  padding:28px;
  color:#64748b;
}
.upload .btn {
  margin-top:8px;
}
.file-list {
  margin-top:10px;
}
.file-list li {
  display:flex;
  justify-content:space-between;
  align-items:center;
  padding:8px 10px;
  background:#f5f7fa;
  border-radius:6px;
  margin-bottom:8px;
}
.file-list .name {
  font-size:14px;
}
.link {
  background:transparent;
  border:0; color:#f43f5e;
  cursor:pointer;
}
.actions {
  text-align:right;
  margin-top:16px;
}
.deadline {
  float:left;
  color:#64748b;
  line-height:36px;
}
.btn {
  padding:8px 14px;
  border-radius:6px;
  border:1px solid #cbd5e1;
  background:#f0f2f5;
  cursor:pointer;
}
.btn.primary {
  background:#2563eb;
  color:#fff;
  border-color:#2563eb;
}
.hidden {
  display:none;
}
@media (max-width:768px){
  .grid-2{
    grid-template-columns:1fr;
  }
}
</style>
