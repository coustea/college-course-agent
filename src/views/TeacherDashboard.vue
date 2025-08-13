<template>
  <v-app>
    <v-app-bar app color="primary" dark flat>
      <v-app-bar-title class="ml-4">教师端</v-app-bar-title>
      <v-spacer />

      <v-btn
          v-for="(item, index) in navItems"
          :key="index"
          :to="{ name: item.route }"
          :color="isActive(item.route) ? 'secondary' : ''"
          variant="text"
          class="mx-1 text-body-1"
      >
        {{ item.title }}
      </v-btn>

      <v-text-field
          v-model="searchQuery"
          placeholder="搜索课程、资源..."
          density="compact"
          variant="solo"
          hide-details
          prepend-inner-icon="mdi-magnify"
          class="ml-4 mr-2"
          style="max-width: 300px"
      />

      <v-btn icon class="mr-2">
        <v-icon>mdi-account</v-icon>
      </v-btn>
    </v-app-bar>

    <v-main class="main-content">
      <v-container fluid class="px-8 py-6">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </v-container>
    </v-main>
  </v-app>
</template>

<script>
export default {
  data: () => ({
    searchQuery: '',
    navItems: [
      { title: '首页', route: 'teacher-home' },
      { title: '课程管理', route: 'teacher-courses' },
      { title: '学生管理', route: 'teacher-students' },
      { title: '教学资源', route: 'teacher-resources' }
    ]
  }),
  methods: {
    isActive(routeName) {
      return this.$route.name === routeName
    }
  }
}
</script>

<style scoped>
.main-content {
  background-color: #f8f9fa;
  min-height: calc(100vh - 64px);
}

.v-btn {
  text-transform: none;
  letter-spacing: normal;
  font-weight: 500;
  font-size: 1rem;
}

.v-app-bar {
  padding-left: 16px;
  padding-right: 16px;
}
</style>