<template>
  <div
      class="floating-assistant"
      @click="goToAssistantPage"
      ref="floatingBall"
      :style="{
      left: position.x + 'px',
      top: position.y + 'px'
    }"
  >
    <div class="assistant-icon">
      <v-icon size="32">mdi-robot-happy</v-icon>
    </div>
  </div>
</template>

<script>
import { useRouter } from 'vue-router'
import { ref, onMounted } from 'vue'

export default {
  setup() {
    const router = useRouter()
    const floatingBall = ref(null)
    const position = ref({
      x: window.innerWidth - 80,
      y: window.innerHeight - 80
    })
    const isDragging = ref(false)
    const offset = ref({ x: 0, y: 0 })

    const goToAssistantPage = () => {
      if (!isDragging.value) {
        router.push({ name: 'teacher-assistant' })
      }
    }

    const startDrag = (e) => {
      isDragging.value = true
      offset.value = {
        x: e.clientX - position.value.x,
        y: e.clientY - position.value.y
      }
      document.addEventListener('mousemove', drag)
      document.addEventListener('mouseup', stopDrag)
    }

    const drag = (e) => {
      if (isDragging.value) {
        position.value = {
          x: e.clientX - offset.value.x,
          y: e.clientY - offset.value.y
        }
      }
    }

    const stopDrag = () => {
      isDragging.value = false
      document.removeEventListener('mousemove', drag)
      document.removeEventListener('mouseup', stopDrag)
      savePosition()
    }

    const savePosition = () => {
      localStorage.setItem('assistantPosition', JSON.stringify(position.value))
    }

    const loadPosition = () => {
      const savedPos = localStorage.getItem('assistantPosition')
      if (savedPos) {
        position.value = JSON.parse(savedPos)
      }
    }

    onMounted(() => {
      loadPosition()
      floatingBall.value.addEventListener('mousedown', startDrag)
    })

    return {
      goToAssistantPage,
      floatingBall,
      position
    }
  }
}
</script>

<style scoped>
.floating-assistant {
  position: fixed;
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #1976D2, #2196F3);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  z-index: 999;
  transition: transform 0.2s, box-shadow 0.2s;
}

.floating-assistant:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.3);
}

.assistant-icon {
  color: white;
}
</style>