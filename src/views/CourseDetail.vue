<template>
  <v-container>
    <v-btn
        @click="$router.go(-1)"
        color="primary"
        class="mb-4"
    >
      返回
    </v-btn>

    <v-card>
      <v-img
          :src="course.image"
          height="400"
          cover
      ></v-img>

      <v-card-title class="text-h3 mt-4">{{ course.title }}</v-card-title>
      <v-card-subtitle class="text-h5">{{ course.teacher }}</v-card-subtitle>

      <v-card-text>
        <v-chip color="primary" class="mr-2">{{ course.students }}人学习</v-chip>
        <v-chip>{{ course.category }}</v-chip>

        <div class="mt-6">
          <h3 class="text-h5 mb-2">课程简介</h3>
          <p>{{ course.description }}</p>
        </div>

        <v-divider class="my-6"></v-divider>

        <h3 class="text-h5 mb-4">课程章节</h3>
        <v-list>
          <v-list-item
              v-for="(chapter, index) in course.chapters"
              :key="index"
          >
            <v-list-item-content>
              <v-list-item-title>{{ chapter.title }}</v-list-item-title>
              <v-list-item-subtitle>{{ chapter.duration }}</v-list-item-subtitle>
            </v-list-item-content>
            <v-list-item-action>
              <v-btn icon>
                <v-icon>mdi-play-circle-outline</v-icon>
              </v-btn>
            </v-list-item-action>
          </v-list-item>
        </v-list>
      </v-card-text>

      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn color="primary">编辑课程</v-btn>
        <v-btn color="secondary">管理学生</v-btn>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
export default {
  props: {
    id: {
      type: [String, Number],
      required: true
    }
  },
  data() {
    return {
      course: {
        id: 1,
        title: '习近平新时代中国特色社会主义思想',
        teacher: '张教授',
        students: 1250,
        category: '政治理论',
        image: require('@/assets/sizheng.png'),
        description: '本课程系统讲解习近平新时代中国特色社会主义思想的核心要义、精神实质、丰富内涵和实践要求...',
        chapters: [
          { title: '第一章 习近平新时代中国特色社会主义思想概述', duration: '2小时' },
          { title: '第二章 新时代坚持和发展中国特色社会主义', duration: '3小时' },
          { title: '第三章 实现中华民族伟大复兴的中国梦', duration: '2.5小时' },
          { title: '第四章 全面深化改革', duration: '3小时' },
          { title: '第五章 全面依法治国', duration: '2小时' }
        ]
      }
    }
  },
  created() {
    // 这里应该从API获取课程详情
    // this.fetchCourseDetail()
  },
  methods: {
    fetchCourseDetail() {
      // 实际项目中应该调用API
      // axios.get(`/api/courses/${this.id}`)
      //   .then(response => {
      //     this.course = response.data
      //   })
      //   .catch(error => {
      //     console.error('获取课程详情失败:', error)
      //   })
    }
  }
}
</script>

<style scoped>
.text-h3 {
  color: #9d2c2a;
}
</style>