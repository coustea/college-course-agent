<template>
  <div class="mistake-header-bar">
    <!-- Left Section: Title & Back Button -->
    <div class="header-left">
      <button class="back-btn" @click="$emit('back')" title="返回">
        <i class="fas fa-arrow-left"></i>
      </button>
      <h1 class="page-title">我的错题本</h1>
      <div class="header-stats">
        <span title="待复习">
          <i class="fas fa-hourglass-half"></i> {{ reviewCount }}
        </span>
        <span title="已掌握">
          <i class="fas fa-check-circle"></i> {{ masteredCount }}
        </span>
        <span title="总计">
          <i class="fas fa-list-ul"></i> {{ totalCount }}
        </span>
      </div>
    </div>

    <!-- Right Section: Filters & Actions -->
    <div class="header-right">
       <div class="filter-tabs" role="tablist" aria-label="错题筛选">
        <button
          v-for="filter in filters"
          :key="filter.key"
          :class="['filter-tab', { active: activeFilter === filter.key }]"
          @click="$emit('update:activeFilter', filter.key)"
          :title="filter.label"
          :aria-label="filter.label"
          :aria-selected="activeFilter === filter.key"
          :aria-pressed="activeFilter === filter.key"
          role="tab"
        >
          <i :class="filter.icon" aria-hidden="true"></i>
          <span class="filter-label-text">{{ filter.label }}</span>
        </button>
      </div>
      <div class="search-box">
        <i class="fas fa-search" aria-hidden="true"></i>
        <input
          :value="searchQuery"
          @input="$emit('update:searchQuery', $event.target.value)"
          placeholder="搜索题目、课程..."
          aria-label="搜索错题"
          type="search"
        />
      </div>
      <el-select
        :model-value="sortBy"
        @update:model-value="$emit('update:sortBy', $event)"
        placeholder="排序"
        style="width: 110px;"
        aria-label="排序方式"
      >
        <el-option label="最新" value="newest" />
        <el-option label="最早" value="oldest" />
        <el-option label="易错" value="errorCount" />
      </el-select>
      <el-button
        type="primary"
        class="generate-exam-btn"
        @click="$emit('start-exam')"
        aria-label="生成练习卷"
      >
        <i class="fas fa-file-alt" aria-hidden="true"></i>&nbsp; 组卷练习
      </el-button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  masteredCount: { type: Number, default: 0 },
  reviewCount: { type: Number, default: 0 },
  totalCount: { type: Number, default: 0 },
  filters: { type: Array, required: true },
  activeFilter: { type: String, required: true },
  searchQuery: { type: String, required: true },
  sortBy: { type: String, required: true },
});

defineEmits([
  'back', 
  'start-exam', 
  'update:activeFilter', 
  'update:searchQuery', 
  'update:sortBy'
]);
</script>

<style scoped>
/* Clean White Design System */
:root {
  --font-family-sans: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;

  /* Minimal Color Palette */
  --color-bg: #FFFFFF;
  --color-bg-secondary: #F9FAFB;
  --color-text-primary: #111827;
  --color-text-secondary: #6B7280;
  --color-text-tertiary: #9CA3AF;
  --color-border: #E5E7EB;
  --color-border-light: #F3F4F6;

  /* Semantic Colors */
  --color-primary: #3B82F6;
  --color-primary-hover: #2563EB;
  --color-primary-light: #EFF6FF;
  --color-success: #10B981;
  --color-warning: #F59E0B;
  --color-error: #EF4444;

  /* Transitions */
  --transition-fast: 150ms ease;
  --transition-base: 200ms ease;
}

.mistake-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: var(--color-bg);
  border-radius: 8px;
  border: 1px solid var(--color-border);
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.header-left, .header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

/* Back Button */
.back-btn {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  color: var(--color-text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-btn:hover {
  background: var(--color-bg-secondary);
  color: var(--color-text-primary);
  border-color: var(--color-border);
}

.back-btn:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

/* Page Title */
.page-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
  color: var(--color-text-primary);
}

/* Header Stats */
.header-stats {
  display: flex;
  gap: 16px;
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
  padding-left: 16px;
  border-left: 1px solid var(--color-border);
}

.header-stats span {
  display: flex;
  align-items: center;
  gap: 6px;
}

.header-stats i {
  font-size: 12px;
}

/* Filter Tabs */
.filter-tabs {
  background: var(--color-bg-secondary);
  padding: 4px;
  border-radius: 6px;
  display: flex;
  border: 1px solid var(--color-border);
}

.filter-tab {
  padding: 6px 12px;
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
  border-radius: 4px;
  cursor: pointer;
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  gap: 6px;
}

.filter-tab:hover:not(.active) {
  background: var(--color-bg);
  color: var(--color-text-primary);
}

.filter-tab.active {
  background: var(--color-bg);
  color: var(--color-primary);
  font-weight: 600;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
}

.filter-tab i {
  font-size: 12px;
}

.filter-tab:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

/* Search Box */
.search-box {
  display: flex;
  align-items: center;
  position: relative;
}

.search-box i {
  position: absolute;
  left: 10px;
  color: var(--color-text-tertiary);
  font-size: 13px;
}

.search-box input {
  border: 1px solid var(--color-border);
  border-radius: 6px;
  height: 36px;
  padding-left: 32px;
  padding-right: 12px;
  font-size: 13px;
  width: 180px;
  transition: all var(--transition-fast);
  background: var(--color-bg);
  color: var(--color-text-primary);
}

.search-box input:hover {
  border-color: var(--color-border);
}

.search-box input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-light);
  width: 220px;
}

.search-box input:focus + i {
  color: var(--color-primary);
}

/* Generate Exam Button */
.generate-exam-btn {
  font-weight: 600;
}

/* Responsive Design */
@media (max-width: 1200px) {
  .header-left, .header-right {
    flex-grow: 1;
    justify-content: space-between;
  }

  .mistake-header-bar {
    padding: 12px 16px;
  }
}

@media (max-width: 768px) {
  .header-stats {
    display: none;
  }

  .filter-label-text {
    display: none;
  }

  .filter-tab {
    padding: 6px 10px;
  }

  .search-box input {
    width: 140px;
  }

  .search-box input:focus {
    width: 180px;
  }

  .page-title {
    font-size: 18px;
  }

  .mistake-header-bar {
    gap: 12px;
    padding: 12px;
  }
}

/* Respect user's motion preferences */
@media (prefers-reduced-motion: reduce) {
  .mistake-header-bar,
  .back-btn,
  .filter-tab,
  .search-box input {
    transition: none;
  }

  .search-box input:focus {
    width: 180px;
  }
}

/* Focus Visible for Keyboard Navigation */
.back-btn:focus-visible,
.filter-tab:focus-visible,
.search-box input:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
  border-radius: 4px;
}
</style>