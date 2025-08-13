<template>
  <v-card class="pa-6 mx-auto" style="max-width: 1200px">
    <v-toolbar color="transparent" density="compact" class="mb-6">
      <v-btn color="primary" @click="openUploadDialog" prepend-icon="mdi-upload">
        上传资源
      </v-btn>

      <v-text-field
          v-model="searchQuery"
          placeholder="搜索资源"
          density="compact"
          variant="outlined"
          hide-details
          prepend-inner-icon="mdi-magnify"
          class="ml-4"
          style="max-width: 300px"
      />
    </v-toolbar>

    <v-data-table
        :headers="headers"
        :items="filteredResources"
        :items-per-page="10"
    >
      <template #item.type="{ item }">
        <v-chip :color="getTypeColor(item.type)" size="small">
          {{ getTypeName(item.type) }}
        </v-chip>
      </template>

      <template #item.size="{ item }">
        {{ formatFileSize(item.size) }}
      </template>

      <template #item.actions="{ item }">
        <v-btn
            icon="mdi-download"
            size="small"
            variant="text"
            color="primary"
            @click="downloadResource(item)"
            class="mr-2"
        />
        <v-btn
            icon="mdi-delete"
            size="small"
            variant="text"
            color="error"
            @click="confirmDelete(item)"
        />
      </template>
    </v-data-table>

    <!-- 上传对话框 -->
    <v-dialog v-model="uploadDialogVisible" max-width="600">
      <v-card>
        <v-card-title>上传资源</v-card-title>
        <v-card-text>
          <v-form ref="form">
            <v-text-field
                v-model="uploadForm.name"
                label="资源名称"
                :rules="[v => !!v || '必填项']"
                required
            />

            <v-select
                v-model="uploadForm.type"
                :items="resourceTypes"
                label="资源类型"
                :rules="[v => !!v || '必填项']"
                required
            />

            <v-file-input
                v-model="uploadForm.file"
                label="选择文件"
                :rules="[v => !!v || '请选择文件']"
                required
                prepend-icon="mdi-paperclip"
            />
          </v-form>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn @click="uploadDialogVisible = false">取消</v-btn>
          <v-btn color="primary" @click="submitUpload">上传</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-card>
</template>

<script>
import { ref, computed } from 'vue'
import { useConfirm } from 'vuetify-use-dialog'

export default {
  setup() {
    const confirm = useConfirm()
    const searchQuery = ref('')
    const uploadDialogVisible = ref(false)
    const form = ref(null)

    const uploadForm = ref({
      name: '',
      type: 'document',
      file: null
    })

    const resourceTypes = [
      { text: '文档', value: 'document' },
      { text: '视频', value: 'video' },
      { text: '音频', value: 'audio' },
      { text: '图片', value: 'image' },
      { text: '其他', value: 'other' }
    ]

    const resources = ref([
      {
        id: 1,
        name: 'JavaScript高级教程.pdf',
        type: 'document',
        size: 2457600,
        uploadTime: '2023-10-15 14:30',
        downloadCount: 12
      },
      {
        id: 2,
        name: 'Vue3教学视频.mp4',
        type: 'video',
        size: 102400000,
        uploadTime: '2023-10-16 09:15',
        downloadCount: 8
      }
    ])

    const headers = [
      { title: '资源名称', key: 'name' },
      { title: '类型', key: 'type', width: '120px' },
      { title: '大小', key: 'size', width: '120px' },
      { title: '上传时间', key: 'uploadTime', width: '180px' },
      { title: '下载次数', key: 'downloadCount', width: '120px' },
      { title: '操作', key: 'actions', width: '120px', sortable: false }
    ]

    const filteredResources = computed(() => {
      if (!searchQuery.value) return resources.value
      const query = searchQuery.value.toLowerCase()
      return resources.value.filter(resource =>
          resource.name.toLowerCase().includes(query)
      )
    })

    const getTypeName = (type) => {
      const typeNames = {
        document: '文档',
        video: '视频',
        audio: '音频',
        image: '图片',
        other: '其他'
      }
      return typeNames[type] || '未知'
    }

    const getTypeColor = (type) => {
      const typeColors = {
        document: 'blue',
        video: 'red',
        audio: 'orange',
        image: 'green',
        other: 'grey'
      }
      return typeColors[type] || 'grey'
    }

    const formatFileSize = (bytes) => {
      if (bytes === 0) return '0 Bytes'
      const k = 1024
      const sizes = ['Bytes', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    }

    const openUploadDialog = () => {
      uploadForm.value = {
        name: '',
        type: 'document',
        file: null
      }
      uploadDialogVisible.value = true
    }

    const submitUpload = async () => {
      const { valid } = await form.value.validate()
      if (!valid) return

      resources.value.push({
        id: Math.max(...resources.value.map(r => r.id)) + 1,
        name: uploadForm.value.name,
        type: uploadForm.value.type,
        size: uploadForm.value.file.size,
        uploadTime: new Date().toLocaleString(),
        downloadCount: 0
      })

      uploadDialogVisible.value = false
    }

    const downloadResource = (resource) => {
      console.log('下载资源:', resource)
      resource.downloadCount++
    }

    const confirmDelete = async (resource) => {
      const result = await confirm({
        title: '确认删除',
        text: `确定删除资源 "${resource.name}" 吗?`,
        confirmationText: '确定',
        cancellationText: '取消'
      })

      if (result) {
        deleteResource(resource.id)
      }
    }

    const deleteResource = (id) => {
      resources.value = resources.value.filter(resource => resource.id !== id)
    }

    return {
      searchQuery,
      headers,
      filteredResources,
      resourceTypes,
      uploadDialogVisible,
      uploadForm,
      form,
      getTypeName,
      getTypeColor,
      formatFileSize,
      openUploadDialog,
      submitUpload,
      downloadResource,
      confirmDelete
    }
  }
}
</script>