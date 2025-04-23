<template>
  <v-app>
    <v-container class="teacher-dashboard">

      <!-- 顶部导航栏 -->
      <v-app-bar app color="#9d2c2a" dark flat class="custom-navbar">
        <v-app-bar-title class="text-center">教师端</v-app-bar-title>
        <v-spacer />
        <div class="nav-btn-container">
          <v-btn
              text
              class="nav-btn"
              @click="navigate('add')"
              :style="{ backgroundColor: hoverAdd ? '#d4a664' : '' }"
              @mouseover="hoverAdd = true"
              @mouseleave="hoverAdd = false"
          >
            主页
          </v-btn>
          <v-btn
              text
              class="nav-btn"
              @click="navigate('political')"
              :style="{ backgroundColor: hoverPolitical ? '#d4a664' : '' }"
              @mouseover="hoverPolitical = true"
              @mouseleave="hoverPolitical = false"
          >
            课程添加
          </v-btn>
          <v-btn
              text
              class="nav-btn"
              @click="navigate('analysis')"
              :style="{ backgroundColor: hoverAnalysis ? '#d4a664' : '' }"
              @mouseover="hoverAnalysis = true"
              @mouseleave="hoverAnalysis = false"
          >
            学生管理
          </v-btn>
        </div>
        <v-spacer />
      </v-app-bar>

      <!-- 关键字搜索框 -->
      <v-container>
        <v-text-field
            v-model="searchQuery"
            label="搜索"
            prepend-icon="mdi-magnify"
            outlined
        ></v-text-field>
      </v-container>

      <!-- 翻滚切换的图片 -->
      <v-carousel>
        <v-carousel-item v-for="(image, i) in images" :key="i">
          <v-img :src="image.src" height="500px" contain></v-img>
        </v-carousel-item>
      </v-carousel>

      <!-- 内容网格 -->
      <v-container>
        <v-row>
          <v-col v-for="(item, index) in items" :key="index" cols="12" sm="6" md="4">
            <v-card>
              <v-card-title>{{ item.title }}</v-card-title>
              <v-card-text>{{ item.description }}</v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </v-container>

  </v-app>
</template>

<script>
// 直接使用 import 引入图片
import ccutImage from '@/assets/ccut.jpg';
import sizhengImage from '@/assets/sizheng.png';
import sizheng2Image from '@/assets/sizheng2.png';

export default {
  data() {
    return {
      searchQuery: '',
      hoverAdd: false,
      hoverPolitical: false,
      hoverAnalysis: false,

      images: [
        { src: ccutImage },
        { src: sizhengImage },
        { src: sizheng2Image },
      ],
      items: [
        { title: '案例1：', description: '金融' },
        { title: '案例2：', description: '医疗' },
        { title: '案例3', description: '军工' },
      ],
    };
  },
  methods: {
    navigate(page) {
      this.currentPage = page;
    }
  }
};
</script>

<style scoped>
.teacher-dashboard {
  padding: 60px;
}

.nav-btn-container {
  display: flex;
  justify-content: center;
  gap: 20px; /* 按钮之间的间距 */
}

.nav-btn {
  color: white;
  font-weight: bold;
  font-size: 16px;
  transition: background-color 0.3s ease;
}

.custom-navbar{
  height: 60px; /* 设置导航栏高度 */
  padding: 0 10px; /* 调整导航栏内边距 */
}

.category-btn {
  font-size: 18px;
  text-transform: none;
}

.progress-bar {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
}

.video-preview {
  margin-bottom: 10px;
}

.progress-box {
  display: flex;
  justify-content: space-between;
  width: 100%;
  margin-bottom: 5px;
}

.progress-square {
  width: 10%;
  height: 10px;
  background-color: #e0e0e0;
  margin: 0 1px;
  transition: background-color 0.3s;
}

.progress-square.filled {
  background-color: #4caf50;
}
</style>
