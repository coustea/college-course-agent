<template>
  <div class="course-materials-page">
    <!-- 顶部导航 -->
    <div class="page-header">
      <div class="header-left">
        <el-button link class="back-btn" @click="goBack">
          <i class="fas fa-arrow-left"></i> 返回列表
        </el-button>
        <el-divider direction="vertical" />
        <div class="title-group">
          <h1 class="page-title">课程内容管理</h1>
          <!-- 优先显示课程名称 -->
          <el-tag type="info" effect="plain" class="course-tag">{{ course.courseName || '未命名课程' }}</el-tag>
        </div>
      </div>
      <div class="header-actions">
        <el-button @click="loadChapterTree" :icon="Refresh" circle title="刷新目录" />
        <el-button type="primary" @click="saveAllChanges" :loading="saving">
          <i class="fas fa-save"></i> 保存所有更改
        </el-button>
      </div>
    </div>

    <!-- 主布局 -->
    <div class="layout-container">
      <!-- 左侧：章节目录树 -->
      <div class="sidebar-panel">
        <div class="sidebar-header">
          <h3>章节目录</h3>
          <el-tooltip content="添加一个新的大章" placement="top">
            <el-button type="primary" size="small" icon="Plus" circle @click="addChapter" />
          </el-tooltip>
        </div>

        <div class="tree-container" v-loading="loadingChapters">
          <el-tree
            ref="chapterTreeRef"
            :data="chapterTree"
            :props="treeProps"
            :allow-drop="allowDrop"
            @node-drop="handleNodeDrop"
            draggable
            node-key="chapterId"
            :expand-on-click-node="false"
            :default-expanded-keys="expandedKeys"
            highlight-current
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <div class="custom-tree-node" :class="{ 'is-section': data.chapterType === 'section' }">
                <div class="node-main">
                  <i v-if="data.chapterType === 'chapter'" class="fas fa-folder chapter-icon"></i>
                  <i v-else class="fas fa-file-alt section-icon"></i>
                  <span class="node-label" :title="data.title">{{ data.title }}</span>
                  
                  <!-- 内容标识徽章 -->
                  <div class="badges" v-if="data.video || data.document">
                    <el-tag v-if="data.video" size="small" type="warning" effect="dark" round>视频</el-tag>
                    <el-tag v-if="data.document" size="small" type="success" effect="dark" round>文档</el-tag>
                  </div>
                </div>

                <div class="node-actions-hover">
                  <el-button
                    v-if="data.chapterType === 'chapter'"
                    link
                    type="primary"
                    size="small"
                    @click.stop="addSection(data)"
                    title="添加小节"
                  >
                    <i class="fas fa-plus"></i>
                  </el-button>
                  <el-button link type="primary" size="small" @click.stop="editNode(data)" title="重命名">
                    <i class="fas fa-edit"></i>
                  </el-button>
                  <el-button link type="danger" size="small" @click.stop="deleteNode(data)" title="删除">
                    <i class="fas fa-trash"></i>
                  </el-button>
                </div>
              </div>
            </template>
          </el-tree>
          
          <el-empty 
            v-if="!chapterTree.length" 
            :image-size="80" 
            description="暂无章节，请添加"
            class="sidebar-empty"
          />
        </div>
      </div>

      <!-- 右侧：动态编辑区 -->
      <div class="content-panel">
        
        <!-- 场景1：未选择任何章节 -> 显示课程基本信息编辑 (默认展示已有信息) -->
        <transition name="fade" mode="out-in">
          <div v-if="!selectedChapter" class="panel-card course-info-card" key="course-info">
            <div class="card-header">
              <h3><i class="fas fa-sliders-h"></i> 课程基本设置</h3>
              <span class="header-tip">在此修改课程名称、封面和简介</span>
            </div>
            <div class="card-body">
              <div class="info-layout">
                <!-- 封面上传区 -->
                <div class="cover-section">
                  <div class="cover-uploader" @click="$refs.coverInput.click()">
                    <!-- 优先显示新选择的预览图，其次显示已有的资源图 -->
                    <img v-if="imagePreview || course.resourceUrl" :src="imagePreview || course.resourceUrl" class="cover-image" />
                    <div v-else class="upload-placeholder">
                      <i class="fas fa-cloud-upload-alt"></i>
                      <span>点击上传封面</span>
                    </div>
                    <div class="cover-mask">
                      <i class="fas fa-camera"></i> 更换封面
                    </div>
                    <input ref="coverInput" type="file" accept="image/*" @change="onCourseImageChange" hidden />
                  </div>
                  <div class="cover-tips">建议尺寸 16:9，支持 JPG/PNG</div>
                </div>

                <!-- 表单编辑区 -->
                <div class="form-section">
                  <div class="form-group">
                    <label>课程名称</label>
                    <el-input v-model="course.courseName" placeholder="请输入精彩的课程名称" size="large" />
                  </div>
                  <div class="form-group">
                    <label>简介描述</label>
                    <el-input 
                      v-model="course.description" 
                      type="textarea" 
                      :rows="6" 
                      placeholder="介绍一下这门课程的主要内容..." 
                      resize="none"
                    />
                  </div>
                  <div class="form-group">
                    <label>排序权重</label>
                    <el-input-number v-model="course.vindex" :min="0" controls-position="right" />
                    <span class="field-tip">数字越大显示越靠前</span>
                  </div>
                  <div class="form-actions">
                     <el-button type="primary" @click="updateCourseInfo" :loading="savingInfo">保存基本信息</el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 场景2：选中章节 -> 显示章节编辑 -->
          <div v-else class="panel-card chapter-edit-card" :key="selectedChapter.chapterId">
            <div class="card-header with-breadcrumb">
              <div class="breadcrumb">
                <span class="root">课程</span>
                <i class="fas fa-chevron-right separator"></i>
                <span class="parent" v-if="selectedChapter.chapterType === 'section' && parentChapter">
                  {{ parentChapter.title }}
                  <i class="fas fa-chevron-right separator"></i>
                </span>
                <span class="current">{{ selectedChapter.title }}</span>
              </div>
              <div class="header-right">
                <el-tag :type="selectedChapter.chapterType === 'chapter' ? 'primary' : 'success'">
                  {{ selectedChapter.chapterType === 'chapter' ? '章' : '节' }}
                </el-tag>
              </div>
            </div>

            <div class="card-body">
              <!-- 标题编辑 -->
              <div class="quick-edit-title">
                <el-input 
                  v-model="selectedChapter.title" 
                  size="large" 
                  class="title-input"
                  @change="confirmEdit(false)"
                >
                  <template #prepend>标题</template>
                  <template #append>
                    <el-button @click="confirmEdit(false)"><i class="fas fa-check"></i></el-button>
                  </template>
                </el-input>
              </div>

              <el-divider />

              <!-- 章的视图：显示子节列表概览 -->
              <div v-if="selectedChapter.chapterType === 'chapter'" class="chapter-overview">
                <div class="overview-header">
                  <h4>包含小节 ({{ selectedChapter.children ? selectedChapter.children.length : 0 }})</h4>
                  <el-button type="primary" size="small" plain @click="addSection(selectedChapter)">
                    <i class="fas fa-plus"></i> 添加新节
                  </el-button>
                </div>
                <div class="section-list">
                  <div 
                    v-for="child in selectedChapter.children" 
                    :key="child.chapterId"
                    class="section-item"
                    @click="handleNodeClick(child)"
                  >
                    <i class="fas fa-file-alt"></i>
                    <span>{{ child.title }}</span>
                    <i class="fas fa-chevron-right arrow"></i>
                  </div>
                  <div v-if="!selectedChapter.children || selectedChapter.children.length === 0" class="empty-tip">
                    暂无小节，请点击上方按钮添加
                  </div>
                </div>
              </div>

              <!-- 节的视图：内容管理（核心） -->
              <div v-else class="section-content-manager">
                <!-- 状态1：已有内容 -->
                <div v-if="selectedChapter.video || selectedChapter.document" class="current-content-view">
                  <!-- 视频卡片 -->
                  <div v-if="selectedChapter.video" class="content-display-card video-card">
                    <div class="icon-wrapper">
                      <i class="fas fa-play-circle"></i>
                    </div>
                    <div class="info-wrapper">
                      <h5>{{ selectedChapter.video.videoTitle }}</h5>
                      <p>
                        <el-tag size="small" type="warning">视频</el-tag> 
                        <span class="meta">{{ formatDuration(selectedChapter.video.duration) }}</span>
                      </p>
                    </div>
                    <div class="actions-wrapper">
                      <el-button type="primary" circle @click="previewVideo(selectedChapter.video)" title="预览">
                        <i class="fas fa-play"></i>
                      </el-button>
                      <el-button type="danger" circle @click="removeContent('video')" title="移除">
                        <i class="fas fa-trash-alt"></i>
                      </el-button>
                    </div>
                  </div>

                  <!-- 文档卡片 -->
                  <div v-if="selectedChapter.document" class="content-display-card doc-card">
                    <div class="icon-wrapper">
                      <i class="fas fa-file-pdf"></i>
                    </div>
                    <div class="info-wrapper">
                      <h5>{{ selectedChapter.document.docTitle }}</h5>
                      <p>
                        <el-tag size="small" type="success">文档</el-tag>
                        <span class="meta">{{ formatDate(selectedChapter.document.uploadDate) }}</span>
                      </p>
                    </div>
                    <div class="actions-wrapper">
                      <el-button type="primary" circle @click="previewDocument(selectedChapter.document)" title="查看">
                        <i class="fas fa-eye"></i>
                      </el-button>
                      <el-button type="danger" circle @click="removeContent('document')" title="移除">
                        <i class="fas fa-trash-alt"></i>
                      </el-button>
                    </div>
                  </div>
                  
                  <el-alert title="如需更换内容，请先移除当前内容" type="info" show-icon :closable="false" style="margin-top: 15px;" />
                </div>

                <!-- 状态2：无内容，选择上传类型 -->
                <div v-else class="empty-content-selector">
                  <h4>当前小节暂无内容</h4>
                  <p class="sub-text">请选择一种内容类型进行上传</p>
                  
                  <div class="upload-options">
                    <!-- 视频上传区 -->
                    <div class="upload-card" :class="{ active: uploadType === 'video' }" @click="uploadType = 'video'">
                      <div class="card-icon video"><i class="fas fa-video"></i></div>
                      <div class="card-title">上传视频</div>
                      <div class="card-desc">支持 MP4, MOV 等格式</div>
                    </div>
                    
                    <!-- 文档上传区 -->
                    <div class="upload-card" :class="{ active: uploadType === 'doc' }" @click="uploadType = 'doc'">
                      <div class="card-icon doc"><i class="fas fa-file-alt"></i></div>
                      <div class="card-title">上传文档</div>
                      <div class="card-desc">支持 PDF, PPT, Word 等</div>
                    </div>
                  </div>

                  <!-- 具体上传组件区域 -->
                  <div v-if="uploadType" class="upload-action-area">
                    <el-divider content-position="center">
                      {{ uploadType === 'video' ? '配置视频信息' : '配置文档信息' }}
                    </el-divider>

                    <div v-if="uploadType === 'video'" class="upload-form">
                       <el-input v-model="videoForm.videoTitle" placeholder="视频标题（默认为文件名）" class="mb-2">
                         <template #prepend>标题</template>
                       </el-input>
                       <el-upload
                         class="upload-dragger"
                         drag
                         action="#"
                         :auto-upload="false"
                         :on-change="handleVideoChange"
                         :show-file-list="true"
                         :limit="1"
                         accept=".mp4,.mov,.webm"
                       >
                         <i class="fas fa-cloud-upload-alt upload-icon"></i>
                         <div class="el-upload__text">拖拽视频文件到此处，或 <em>点击上传</em></div>
                       </el-upload>
                       <div class="action-btn-row">
                          <el-button type="primary" @click="saveAllChanges" :loading="saving">开始上传并保存</el-button>
                          <el-button @click="uploadType = null">取消</el-button>
                       </div>
                    </div>

                    <div v-if="uploadType === 'doc'" class="upload-form">
                       <el-input v-model="documentForm.docTitle" placeholder="文档标题（默认为文件名）" class="mb-2">
                         <template #prepend>标题</template>
                       </el-input>
                       <el-upload
                         class="upload-dragger"
                         drag
                         action="#"
                         :auto-upload="false"
                         :on-change="handleDocumentChange"
                         :show-file-list="true"
                         :limit="1"
                         accept=".pdf,.doc,.docx,.ppt,.pptx"
                       >
                         <i class="fas fa-file-upload upload-icon"></i>
                         <div class="el-upload__text">拖拽文档文件到此处，或 <em>点击上传</em></div>
                       </el-upload>
                       <div class="action-btn-row">
                          <el-button type="primary" @click="saveAllChanges" :loading="saving">开始上传并保存</el-button>
                          <el-button @click="uploadType = null">取消</el-button>
                       </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </transition>
      </div>
    </div>

    <!-- 弹窗：重命名 (用于树形菜单上的快速操作) -->
    <el-dialog v-model="editDialogVisible" title="重命名章节" width="400px">
      <el-input v-model="editChapter.title" placeholder="请输入新标题" @keyup.enter="confirmEdit(true)" />
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmEdit(true)">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()
const courseId = ref(Number(route.params.id))

// --- 数据状态 ---
// 尝试从 history.state 获取初始数据（支持从列表页跳转时的秒开体验）
const getInitialData = () => {
  const state = history.state
  if (state && state.initialCourse) {
    return state.initialCourse
  }
  return {}
}
const initialData = getInitialData()

const course = ref({
  courseId: courseId.value,
  // 映射字段：优先使用传递过来的名称，支持多种字段名
  courseName: initialData.courseName || initialData.title || initialData.name || '',
  description: initialData.description || '',
  // 映射资源URL
  resourceUrl: initialData.resourceUrl || initialData.image || initialData.cover || '',
  vindex: initialData.vindex || 0
})

const chapterTree = ref([])
const selectedChapter = ref(null)
const expandedKeys = ref([]) // 默认展开的节点

const loadingChapters = ref(false)
const saving = ref(false)
const savingInfo = ref(false)

// 图片预览
const imagePreview = ref('')
const imageFile = ref(null)

// 上传交互状态
const uploadType = ref(null) // 'video' | 'doc' | null
const videoForm = ref({ videoTitle: '', file: null })
const documentForm = ref({ docTitle: '', file: null })

// 编辑状态
const editDialogVisible = ref(false)
const editChapter = ref({ chapterId: null, title: '', chapterType: '' })

// Tree Config
const chapterTreeRef = ref(null)
const treeProps = { children: 'children', label: 'title' }

// --- 计算属性 ---
const parentChapter = computed(() => {
  if (!selectedChapter.value || selectedChapter.value.chapterType !== 'section') return null
  // 简单查找父节点（假设两层结构）
  return chapterTree.value.find(ch => ch.children && ch.children.some(sec => sec.chapterId === selectedChapter.value.chapterId))
})

// --- 核心方法 ---

const goBack = () => router.back()

// 加载课程详情（覆盖初始数据，确保数据最新）
const loadCourse = async () => {
  try {
    const res = await request.get(`/api/course/detail`, { params: { courseId: courseId.value } })
    if (res.data.code === 200 && res.data.data) {
      const data = res.data.data
      // 智能合并：如果后端返回了有效值则更新，否则保持现有值（避免覆盖为空）
      course.value.courseName = data.courseName || data.title || data.name || course.value.courseName
      course.value.description = data.description || course.value.description
      course.value.resourceUrl = data.resourceUrl || data.image || data.cover || course.value.resourceUrl
      if (data.vindex !== undefined) course.value.vindex = data.vindex
    }
  } catch (error) {
    console.error('加载课程信息失败', error)
  }
}

const loadChapterTree = async () => {
  loadingChapters.value = true
  try {
    const res = await request.get(`/api/chapter/tree`, { params: { courseId: courseId.value } })
    if (res.data.code === 200) {
      chapterTree.value = res.data.data || []
      // 如果当前有选中的章节，尝试刷新选中状态的数据
      if (selectedChapter.value) {
        refreshSelectedNode()
      }
    }
  } catch (error) { console.error(error) } 
  finally { loadingChapters.value = false }
}

// 刷新选中节点的数据（用于保存后更新视图）
const refreshSelectedNode = () => {
  const findNode = (nodes) => {
    for (const node of nodes) {
      if (node.chapterId === selectedChapter.value.chapterId) return node
      if (node.children) {
        const found = findNode(node.children)
        if (found) return found
      }
    }
    return null
  }
  const freshNode = findNode(chapterTree.value)
  if (freshNode) {
    selectedChapter.value = freshNode
    // 保持内容类型同步
    if (freshNode.video) uploadType.value = null
    if (freshNode.document) uploadType.value = null
  } else {
    selectedChapter.value = null // 节点可能已被删除
  }
}

// 树节点点击
const handleNodeClick = (data) => {
  selectedChapter.value = data
  uploadType.value = null // 重置上传面板
  videoForm.value = { videoTitle: '', file: null }
  documentForm.value = { docTitle: '', file: null }
}

// 添加章
const addChapter = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入章标题', '添加章', {
      confirmButtonText: '创建',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：第一章 基础入门'
    })
    if (!value) return

    const res = await request.post(`/api/chapter/create`, null, {
      params: { courseId: courseId.value, title: value }
    })
    if (res.data.code === 200) {
      ElMessage.success('创建成功')
      await loadChapterTree()
    }
  } catch (e) { /* cancel */ }
}

// 添加节
const addSection = async (chapter) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入小节标题', `在“${chapter.title}”下添加节`, {
      confirmButtonText: '创建',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：1.1 环境搭建'
    })
    if (!value) return

    const res = await request.post(`/api/chapter/section/create`, null, {
      params: { parentId: chapter.chapterId, title: value }
    })
    if (res.data.code === 200) {
      ElMessage.success('创建成功')
      expandedKeys.value.push(chapter.chapterId) // 自动展开父节点
      await loadChapterTree()
    }
  } catch (e) { /* cancel */ }
}

// 编辑/重命名节点
const editNode = (node) => {
  editChapter.value = { ...node }
  editDialogVisible.value = true
}

const confirmEdit = async (isDialog = true) => {
  const targetId = isDialog ? editChapter.value.chapterId : selectedChapter.value.chapterId
  const targetTitle = isDialog ? editChapter.value.title : selectedChapter.value.title

  if (!targetTitle.trim()) {
    ElMessage.warning('标题不能为空')
    return
  }

  try {
    const res = await request.put(`/api/chapter/update`, null, {
      params: { chapterId: targetId, title: targetTitle }
    })
    if (res.data.code === 200) {
      ElMessage.success('更新成功')
      if (isDialog) editDialogVisible.value = false
      loadChapterTree()
    }
  } catch (e) { ElMessage.error('更新失败') }
}

// 删除节点
const deleteNode = async (node) => {
  try {
    await ElMessageBox.confirm(
      node.chapterType === 'chapter' ? '删除章将同时删除其下所有内容，确认删除？' : '确认删除该小节？',
      '危险操作',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
    const res = await request.delete(`/api/chapter/delete`, { params: { chapterId: node.chapterId } })
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      if (selectedChapter.value?.chapterId === node.chapterId) selectedChapter.value = null
      loadChapterTree()
    }
  } catch (e) { /* cancel */ }
}

// 拖拽控制
const allowDrop = (draggingNode, dropNode, type) => {
  return draggingNode.data.chapterType === dropNode.data.chapterType && type !== 'inner'
}
const handleNodeDrop = () => {
  ElMessage.success('排序已更新')
  // 这里可以添加实际的排序API调用
}

// --- 内容上传逻辑 ---

const handleVideoChange = (file) => {
  videoForm.value.file = file.raw
  if (!videoForm.value.videoTitle) videoForm.value.videoTitle = file.name.replace(/\.[^/.]+$/, '')
}

const handleDocumentChange = (file) => {
  documentForm.value.file = file.raw
  if (!documentForm.value.docTitle) documentForm.value.docTitle = file.name.replace(/\.[^/.]+$/, '')
}

const onCourseImageChange = (e) => {
  const file = e.target.files?.[0]
  if (file) {
    imageFile.value = file
    const reader = new FileReader()
    reader.onload = (e) => (imagePreview.value = e.target.result)
    reader.readAsDataURL(file)
  }
}

// 统一保存入口
const saveAllChanges = async () => {
  saving.value = true
  try {
    // 1. 如果是在上传模式，优先处理上传
    if (selectedChapter.value && uploadType.value) {
      if (uploadType.value === 'video') await uploadVideo()
      else if (uploadType.value === 'doc') await uploadDoc()
    } else {
      // 2. 否则只保存基本信息
      await updateCourseInfo(false) // false means don't show specific success msg if implied
    }
    
    // 刷新全页数据
    await Promise.all([loadChapterTree(), loadCourse()])
  } catch (e) {
    console.error(e)
  } finally {
    saving.value = false
  }
}

// 独立更新课程信息
const updateCourseInfo = async (showMsg = true) => {
  savingInfo.value = true
  const formData = new FormData()
  formData.append('courseId', course.value.courseId)
  formData.append('courseName', course.value.courseName || '')
  formData.append('description', course.value.description || '')
  if (imageFile.value) formData.append('image', imageFile.value)

  try {
    const res = await request.put(`/api/course/update`, formData, { headers: { 'Content-Type': 'multipart/form-data' } })
    if (res.data.code === 200) {
      if (showMsg) ElMessage.success('课程基本信息已保存')
      imageFile.value = null
    } else {
      ElMessage.error(res.data.message)
    }
  } finally {
    savingInfo.value = false
  }
}

const uploadVideo = async () => {
  if (!videoForm.value.file) return ElMessage.warning('请选择视频文件')
  
  const formData = new FormData()
  formData.append('courseId', courseId.value)
  formData.append('videoTitle', videoForm.value.videoTitle)
  formData.append('file', videoForm.value.file)

  const uploadRes = await request.post(`/api/course/video/insert`, formData)
  if (uploadRes.data.code === 200) {
    const attachRes = await request.post(`/api/chapter/attach/video`, null, {
      params: { chapterId: selectedChapter.value.chapterId, videoId: uploadRes.data.data.videoId }
    })
    if (attachRes.data.code === 200) {
      ElMessage.success('视频上传成功')
      uploadType.value = null
    }
  }
}

const uploadDoc = async () => {
  if (!documentForm.value.file) return ElMessage.warning('请选择文档文件')

  const formData = new FormData()
  formData.append('courseId', courseId.value)
  formData.append('docTitle', documentForm.value.docTitle)
  formData.append('file', documentForm.value.file)

  const uploadRes = await request.post(`/api/course/document/insert`, formData)
  if (uploadRes.data.code === 200) {
    const attachRes = await request.post(`/api/chapter/attach/document`, null, {
      params: { chapterId: selectedChapter.value.chapterId, documentId: uploadRes.data.data.documentId }
    })
    if (attachRes.data.code === 200) {
      ElMessage.success('文档上传成功')
      uploadType.value = null
    }
  }
}

const removeContent = async (type) => {
  try {
    await ElMessageBox.confirm('确定要移除当前内容吗？', '提示', { type: 'warning' })
    const apiPath = type === 'video' ? '/api/course/video/delete' : '/api/course/document/delete'
    const paramKey = type === 'video' ? 'videoId' : 'documentId'
    const id = type === 'video' ? selectedChapter.value.video.videoId : selectedChapter.value.document.documentId
    
    const res = await request.delete(`${apiPath}`, { params: { [paramKey]: id } })
    if (res.data.code === 200) {
      ElMessage.success('移除成功')
      loadChapterTree()
    }
  } catch (e) { /* cancel */ }
}

const previewVideo = (v) => v.videoUrl && window.open(v.videoUrl, '_blank')
const previewDocument = (d) => d.docUrl && window.open(d.docUrl, '_blank')

const formatDuration = (s) => {
  if (!s) return '00:00'
  const m = Math.floor(s / 60).toString().padStart(2,'0')
  const sec = (s % 60).toString().padStart(2,'0')
  return `${m}:${sec}`
}
const formatDate = (d) => d ? new Date(d).toLocaleDateString() : '-'

onMounted(() => {
  loadCourse()
  loadChapterTree()
})
</script>

<style scoped>
.course-materials-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f0f2f5;
  overflow: hidden;
}

/* 顶部栏 */
.page-header {
  flex-shrink: 0;
  height: 60px;
  background: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0,21,41,0.08);
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.title-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

/* 主布局 */
.layout-container {
  flex: 1;
  display: flex;
  overflow: hidden;
  padding: 16px 24px 24px;
  gap: 20px;
}

/* 左侧 Sidebar */
.sidebar-panel {
  width: 320px;
  background: white;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.tree-container {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

/* 自定义树节点 */
.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 8px;
  overflow: hidden;
}

.node-main {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
}

.node-label {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 140px;
}

.chapter-icon { color: #f59e0b; }
.section-icon { color: #8c8c8c; }
.is-section .section-icon { color: #10b981; }

.node-actions-hover {
  display: none;
}

.custom-tree-node:hover .node-actions-hover {
  display: flex;
  align-items: center;
}

/* 右侧内容区 */
.content-panel {
  flex: 1;
  overflow-y: auto;
  position: relative;
}

/* 卡片通用样式 */
.panel-card {
  background: white;
  border-radius: 8px;
  min-height: 100%;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
}

.card-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 { margin: 0; font-size: 18px; color: #333; }
.header-tip { font-size: 13px; color: #999; }

.card-body {
  padding: 24px;
  flex: 1;
}

/* 课程信息编辑 */
.info-layout {
  display: flex;
  gap: 32px;
}

.cover-section {
  width: 280px;
  flex-shrink: 0;
}

.cover-uploader {
  width: 100%;
  aspect-ratio: 16/9;
  background: #fafafa;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.3s;
}

.cover-uploader:hover { border-color: #409eff; }
.cover-image { width: 100%; height: 100%; object-fit: cover; }

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #999;
  gap: 8px;
}
.upload-placeholder i { font-size: 24px; }

.cover-mask {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}
.cover-uploader:hover .cover-mask { opacity: 1; }

.cover-tips { margin-top: 8px; font-size: 12px; color: #999; text-align: center; }

.form-section { flex: 1; max-width: 600px; display: flex; flex-direction: column; gap: 20px; }
.form-group label { display: block; margin-bottom: 8px; font-weight: 500; color: #333; }
.field-tip { font-size: 12px; color: #999; margin-left: 10px; }
.form-actions { margin-top: 20px; }

/* 面包屑导航 */
.breadcrumb {
  display: flex;
  align-items: center;
  font-size: 15px;
  color: #666;
}
.breadcrumb .root { font-weight: bold; color: #333; }
.breadcrumb .separator { margin: 0 8px; font-size: 12px; color: #ccc; }
.breadcrumb .current { color: #409eff; font-weight: 500; }

/* 章节概览 */
.chapter-overview { margin-top: 24px; }
.overview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.section-list {
  border: 1px solid #eee;
  border-radius: 6px;
}
.section-item {
  padding: 12px 16px;
  border-bottom: 1px solid #eee;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: background 0.2s;
}
.section-item:last-child { border-bottom: none; }
.section-item:hover { background: #f9f9f9; }
.section-item .arrow { margin-left: auto; color: #ccc; }
.empty-tip { padding: 30px; text-align: center; color: #999; }

/* 内容卡片 */
.content-display-card {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 20px;
  transition: transform 0.2s;
}
.video-card { background: #fff7e6; border: 1px solid #ffd591; }
.doc-card { background: #f0f5ff; border: 1px solid #adc6ff; }

.icon-wrapper {
  font-size: 32px;
  margin-right: 20px;
}
.video-card .icon-wrapper { color: #fa8c16; }
.doc-card .icon-wrapper { color: #2f54eb; }

.info-wrapper { flex: 1; }
.info-wrapper h5 { margin: 0 0 6px 0; font-size: 16px; }
.info-wrapper p { margin: 0; display: flex; align-items: center; gap: 10px; }
.meta { font-size: 13px; color: #666; }

/* 上传选择器 */
.empty-content-selector {
  text-align: center;
  padding: 40px 0;
}
.sub-text { color: #999; margin-bottom: 30px; }

.upload-options {
  display: flex;
  justify-content: center;
  gap: 30px;
  margin-bottom: 30px;
}

.upload-card {
  width: 200px;
  padding: 30px;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
}
.upload-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 16px rgba(0,0,0,0.1);
}
.upload-card.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.card-icon { font-size: 40px; margin-bottom: 16px; }
.card-icon.video { color: #fa8c16; }
.card-icon.doc { color: #2f54eb; }
.card-title { font-size: 16px; font-weight: 600; color: #333; margin-bottom: 8px; }
.card-desc { font-size: 12px; color: #999; }

.upload-action-area {
  max-width: 600px;
  margin: 0 auto;
  text-align: left;
  background: #f9f9f9;
  padding: 20px;
  border-radius: 8px;
}

.upload-dragger { width: 100%; }
.upload-dragger :deep(.el-upload-dragger) { width: 100%; }
.upload-icon { font-size: 48px; color: #c0c4cc; margin: 20px 0 10px; }

.action-btn-row {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
.mb-2 { margin-bottom: 12px; }
</style>