import { ref, computed, onMounted, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  getWrongQuestionsByStudentId,
  deleteWrongQuestion,
  markAsMastered,
  cancelMastered,
} from '@/services/mistakeApi';

// This composable now focuses on managing the list of mistakes and user interactions with the list.
// Exam logic is mostly delegated to the MistakeExamModal component.
export function useMistakeBook() {
  // State Management
  const mistakeList = ref([]);
  const searchQuery = ref('');
  const activeFilter = ref('all');
  const sortBy = ref('newest');
  const showAnswer = ref({});
  const noteDialogVisible = ref(false);
  const currentMistake = ref(null);
  const currentNote = ref('');
  const currentPage = ref(1);
  const pageSize = 5;
  const isLoading = ref(true);

  // Exam-related State (only what's needed to launch it)
  const examDialogVisible = ref(false);
  const examQuestions = ref([]);

  // Helper to get student ID
  const getStudentIdFromToken = () => {
    const userId = localStorage.getItem('userId');
    if (userId) return userId;
    console.error('User ID not found.');
    return null;
  };

  const studentId = getStudentIdFromToken();

  // Static data
  const filters = [
    { key: 'all', label: '全部', icon: 'fas fa-th-list' },
    { key: 'unmastered', label: '待掌握', icon: 'fas fa-times-circle' },
    { key: 'mastered', label: '已掌握', icon: 'fas fa-check-circle' },
    { key: 'noted', label: '带笔记', icon: 'fas fa-sticky-note' }
  ];

  // API Calls
  const fetchMistakes = async () => {
    if (!studentId) {
      ElMessage.error('无法获取用户信息，请重新登录');
      isLoading.value = false;
      return;
    }
    isLoading.value = true;
    try {
      const response = await getWrongQuestionsByStudentId(studentId);
      const data = response.data.data || [];
      mistakeList.value = data.map(item => ({
        ...item,
        id: item.id,
        question: item.question?.content || '',
        type: item.question?.type?.toLowerCase() || 'text',
        options: (item.question?.type === 'CHOICE' && item.question?.options)
          ? (() => {
              try {
                const parsed = JSON.parse(item.question.options);
                return Array.isArray(parsed) ? parsed : [];
              } catch (e) {
                return typeof item.question.options === 'string'
                  ? item.question.options.split(';').map(opt => opt.trim())
                  : [];
              }
            })()
          : [],
        correctIndex: (item.question?.type === 'CHOICE' && item.question?.answer)
          ? ['A', 'B', 'C', 'D'].indexOf(item.question.answer)
          : -1,
        correctAnswer: item.question?.answer || item.correctAnswer || '',
        explanation: item.question?.analysis || '暂无解析',
        userAnswer: item.wrongAnswer || '',
        courseName: item.courseName || '课程 ' + item.courseId,
        chapter: item.chapter || '综合',
        mastered: item.isMastered || false,
        errorCount: item.wrongCount || 1,
        createTime: new Date(item.createTime),
        note: item.note || ''
      }));
    } catch (error) {
      ElMessage.error('错题列表加载失败: ' + (error.response?.data?.message || error.message));
    } finally {
      isLoading.value = false;
    }
  };

  // Computed Properties
  const masteredCount = computed(() => mistakeList.value.filter(m => m.mastered).length);
  const reviewCount = computed(() => mistakeList.value.filter(m => !m.mastered).length);
  const totalCount = computed(() => mistakeList.value.length);

  const masteryRate = computed(() => {
    return totalCount.value > 0
      ? Math.round((masteredCount.value / totalCount.value) * 100)
      : 0;
  });

  const subjectStats = computed(() => {
    const stats = {};
    mistakeList.value.forEach(item => {
      stats[item.courseName] = (stats[item.courseName] || 0) + 1;
    });
    return Object.entries(stats)
      .sort(([, a], [, b]) => b - a)
      .slice(0, 5);
  });

  const filteredMistakes = computed(() => {
    let list = [...mistakeList.value];
    if (activeFilter.value === 'unmastered') list = list.filter(m => !m.mastered);
    else if (activeFilter.value === 'mastered') list = list.filter(m => m.mastered);
    else if (activeFilter.value === 'noted') list = list.filter(m => m.note && m.note.trim());

    if (searchQuery.value) {
      const query = searchQuery.value.toLowerCase();
      list = list.filter(m => m.question.toLowerCase().includes(query) || m.courseName.toLowerCase().includes(query));
    }
    
    list.sort((a, b) => {
      if (sortBy.value === 'oldest') return a.createTime - b.createTime;
      if (sortBy.value === 'errorCount') return b.errorCount - a.errorCount;
      return b.createTime - a.createTime;
    });
    return list;
  });

  const paginatedMistakes = computed(() => {
    const start = (currentPage.value - 1) * pageSize;
    return filteredMistakes.value.slice(start, start + pageSize);
  });

  const totalPages = computed(() => Math.ceil(filteredMistakes.value.length / pageSize));

  // Methods
  const toggleAnswer = (id) => {
    showAnswer.value[id] = !showAnswer.value[id];
  };

  const toggleMaster = async (mistake) => {
    const originalMastered = mistake.mastered;
    mistake.mastered = !mistake.mastered;
    try {
      if (mistake.mastered) {
        await markAsMastered(mistake.id);
        ElMessage.success({ message: '太棒了！已标记为掌握', type: 'success' });
      } else {
        await cancelMastered(mistake.id);
        ElMessage.info('已取消标记');
      }
    } catch (error) {
      mistake.mastered = originalMastered;
      ElMessage.error('操作失败，请重试');
    }
  };

  const addNote = (mistake) => {
    currentMistake.value = mistake;
    currentNote.value = mistake.note || '';
    noteDialogVisible.value = true;
  };

  const saveNote = () => {
    if (currentMistake.value) {
      // API call to save the note would go here
      currentMistake.value.note = currentNote.value;
      ElMessage.success('笔记已保存');
      noteDialogVisible.value = false;
    }
  };

  const deleteSingleMistake = async (id) => {
    try {
      await ElMessageBox.confirm('删除后无法恢复，确定要移除这道错题吗？', '确认删除', { type: 'warning' });
      await deleteWrongQuestion(id);
      mistakeList.value = mistakeList.value.filter(m => m.id !== id);
      if (paginatedMistakes.value.length === 0 && currentPage.value > 1) {
        currentPage.value--;
      }
      ElMessage.success('删除成功');
    } catch (error) {
      if (error !== 'cancel') ElMessage.error('删除失败');
    }
  };

  const changePage = (page) => {
    if (page > 0 && page <= totalPages.value) {
      currentPage.value = page;
    }
  };

  const startExamMode = () => {
    const EXAM_SIZE = 5;
    const candidates = mistakeList.value.filter(m => !m.mastered && m.type === 'choice');
    const shuffled = candidates.sort(() => 0.5 - Math.random());
    examQuestions.value = shuffled.slice(0, Math.min(EXAM_SIZE, shuffled.length));
    examDialogVisible.value = true;
  };
  
  // Handler for event emitted from modal
  const handleMarkMastered = async (mistake) => {
    const itemInList = mistakeList.value.find(m => m.id === mistake.id);
    if (itemInList && !itemInList.mastered) {
      await toggleMaster(itemInList);
    }
  };

  // Lifecycle & Watchers
  onMounted(fetchMistakes);
  watch([searchQuery, activeFilter, sortBy], () => { currentPage.value = 1; });

  return {
    // State
    searchQuery, activeFilter, sortBy, showAnswer, noteDialogVisible, currentNote,
    currentPage, pageSize, isLoading, filters,
    // Computed
    masteredCount, reviewCount, totalCount, masteryRate, subjectStats,
    paginatedMistakes, totalPages,
    // Methods
    toggleAnswer, addNote, saveNote, changePage, fetchMistakes,
    // Actions passed to children
    handleDelete: deleteSingleMistake,
    handleToggleMaster: toggleMaster,
    // Exam
    examDialogVisible, examQuestions, startExamMode, handleMarkMastered,
  };
}