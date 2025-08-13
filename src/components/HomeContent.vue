<template>
  <div>
    <!-- 轮播图 - 完全对齐并添加轮播点 -->
    <div class="carousel-container">
      <v-carousel
          ref="carousel"
          cycle
          height="400"
          hide-delimiters
          show-arrows-on-hover
          :show-arrows="false"
      >
        <v-carousel-item
            v-for="(image, i) in images"
            :key="i"
            :src="image.src"
            cover
        >
          <!-- 自定义轮播点 -->
          <div class="custom-dots">
            <button
                v-for="(dot, index) in images"
                :key="index"
                :class="{ active: i === index }"
                @click="$refs.carousel.goToPage(index)"
            />
          </div>
        </v-carousel-item>
      </v-carousel>
    </div>

    <!-- 主内容区域 -->
    <v-container class="mt-8" style="max-width: 1200px">
      <v-row>
        <!-- 左侧边栏 -->
        <v-col cols="12" md="3" class="sidebar">
          <v-card class="mb-4" elevation="2">
            <v-tabs v-model="activeTab" direction="vertical" color="primary">
              <v-tab value="knowledgeGraph" prepend-icon="mdi-graph-outline">
                知识图谱
              </v-tab>
              <v-tab value="cases" prepend-icon="mdi-file-document-multiple">
                案例展示
              </v-tab>
              <v-tab value="ideology" prepend-icon="mdi-school">
                课程思政
              </v-tab>
              <v-tab value="policies" prepend-icon="mdi-file-document">
                政策文件
              </v-tab>
            </v-tabs>
          </v-card>
        </v-col>

        <!-- 右侧内容区域 -->
        <v-col cols="12" md="9">
          <v-window v-model="activeTab">
            <!-- 知识图谱 -->
            <v-window-item value="knowledgeGraph">
              <v-card elevation="2">
                <v-card-title class="text-h5 primary--text">
                  案例教学知识图谱
                </v-card-title>
                <v-card-text>
                  <div class="graph-controls mb-4">
                    <v-btn small @click="resetGraph" class="mr-2">
                      <v-icon small left>mdi-autorenew</v-icon>重置视图
                    </v-btn>
                    <v-btn small @click="zoomIn" class="mr-2">
                      <v-icon small left>mdi-magnify-plus</v-icon>放大
                    </v-btn>
                    <v-btn small @click="zoomOut">
                      <v-icon small left>mdi-magnify-minus</v-icon>缩小
                    </v-btn>
                  </div>
                  <div id="knowledge-graph-container" class="knowledge-graph"></div>
                  <div class="graph-legend mt-4">
                    <div v-for="(item, index) in legendItems" :key="index" class="legend-item">
                      <span class="legend-color" :style="{backgroundColor: item.color}"></span>
                      <span class="legend-text">{{ item.text }}</span>
                    </div>
                  </div>
                </v-card-text>
              </v-card>
            </v-window-item>

            <!-- 案例展示 -->
            <v-window-item value="cases">
              <v-card elevation="2">
                <v-card-title class="text-h5 primary--text">
                  案例展示
                </v-card-title>
                <v-card-text>
                  <v-tabs v-model="caseTab" grow color="primary">
                    <v-tab value="medical">医疗软件案例</v-tab>
                    <v-tab value="finance">金融软件案例</v-tab>
                  </v-tabs>

                  <v-window v-model="caseTab" class="mt-4">
                    <v-window-item value="medical">
                      <h3 class="text-h6 mb-4 primary--text">软件工程在医疗中的应用</h3>
                      <v-stepper :items="developmentSteps" :model-value="developmentSteps.length" class="elevation-0">
                        <!-- 需求分析 -->
                        <template #item.1>
                          <div class="stepper-content">
                            <v-card flat class="mb-4">
                              <v-card-title class="primary--text">{{ developmentSteps[0] }}</v-card-title>
                              <v-card-text>
                                <div v-if="caseDetails.medical[0]">
                                  <p class="font-weight-bold">教学知识点:</p>
                                  <ul class="pl-4">
                                    <li v-for="(point, i) in caseDetails.medical[0]" :key="i">{{ point }}</li>
                                  </ul>
                                </div>
                              </v-card-text>
                            </v-card>
                            <div class="video-container mb-6">
                              <v-card outlined>
                                <v-card-title class="text-subtitle-1">教学视频</v-card-title>
                                <div class="video-wrapper">
                                  <iframe
                                      width="100%"
                                      height="315"
                                      src="https://www.youtube.com/embed/示例视频ID"
                                      frameborder="0"
                                      allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                                      allowfullscreen
                                  ></iframe>
                                </div>
                                <v-card-actions>
                                  <v-btn color="primary" text>下载资料</v-btn>
                                  <v-btn color="primary" text>扩展阅读</v-btn>
                                </v-card-actions>
                              </v-card>
                            </div>
                          </div>
                        </template>

                        <!-- 概要设计 -->
                        <template #item.2>
                          <div class="stepper-content">
                            <v-card flat class="mb-4">
                              <v-card-title class="primary--text">{{ developmentSteps[1] }}</v-card-title>
                              <v-card-text>
                                <div v-if="caseDetails.medical[1]">
                                  <p class="font-weight-bold">教学知识点:</p>
                                  <ul class="pl-4">
                                    <li v-for="(point, i) in caseDetails.medical[1]" :key="i">{{ point }}</li>
                                  </ul>
                                </div>
                              </v-card-text>
                            </v-card>
                            <div class="video-container mb-6">
                              <v-card outlined>
                                <v-card-title class="text-subtitle-1">教学视频</v-card-title>
                                <div class="video-wrapper">
                                  <iframe
                                      width="100%"
                                      height="315"
                                      src="https://www.youtube.com/embed/示例视频ID"
                                      frameborder="0"
                                      allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                                      allowfullscreen
                                  ></iframe>
                                </div>
                                <v-card-actions>
                                  <v-btn color="primary" text>下载资料</v-btn>
                                  <v-btn color="primary" text>扩展阅读</v-btn>
                                </v-card-actions>
                              </v-card>
                            </div>
                          </div>
                        </template>

                        <!-- 详细设计 -->
                        <template #item.3>
                          <div class="stepper-content">
                            <v-card flat class="mb-4">
                              <v-card-title class="primary--text">{{ developmentSteps[2] }}</v-card-title>
                              <v-card-text>
                                <div v-if="caseDetails.medical[2]">
                                  <p class="font-weight-bold">教学知识点:</p>
                                  <ul class="pl-4">
                                    <li v-for="(point, i) in caseDetails.medical[2]" :key="i">{{ point }}</li>
                                  </ul>
                                </div>
                              </v-card-text>
                            </v-card>
                            <div class="video-container mb-6">
                              <v-card outlined>
                                <v-card-title class="text-subtitle-1">教学视频</v-card-title>
                                <div class="video-wrapper">
                                  <iframe
                                      width="100%"
                                      height="315"
                                      src="https://www.youtube.com/embed/示例视频ID"
                                      frameborder="0"
                                      allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                                      allowfullscreen
                                  ></iframe>
                                </div>
                                <v-card-actions>
                                  <v-btn color="primary" text>下载资料</v-btn>
                                  <v-btn color="primary" text>扩展阅读</v-btn>
                                </v-card-actions>
                              </v-card>
                            </div>
                          </div>
                        </template>

                        <!-- 测试部署 -->
                        <template #item.4>
                          <div class="stepper-content">
                            <v-card flat class="mb-4">
                              <v-card-title class="primary--text">{{ developmentSteps[3] }}</v-card-title>
                              <v-card-text>
                                <div v-if="caseDetails.medical[3]">
                                  <p class="font-weight-bold">教学知识点:</p>
                                  <ul class="pl-4">
                                    <li v-for="(point, i) in caseDetails.medical[3]" :key="i">{{ point }}</li>
                                  </ul>
                                </div>
                              </v-card-text>
                            </v-card>
                            <div class="video-container mb-6">
                              <v-card outlined>
                                <v-card-title class="text-subtitle-1">教学视频</v-card-title>
                                <div class="video-wrapper">
                                  <iframe
                                      width="100%"
                                      height="315"
                                      src="https://www.youtube.com/embed/示例视频ID"
                                      frameborder="0"
                                      allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                                      allowfullscreen
                                  ></iframe>
                                </div>
                                <v-card-actions>
                                  <v-btn color="primary" text>下载资料</v-btn>
                                  <v-btn color="primary" text>扩展阅读</v-btn>
                                </v-card-actions>
                              </v-card>
                            </div>
                          </div>
                        </template>
                      </v-stepper>
                    </v-window-item>

                    <v-window-item value="finance">
                      <h3 class="text-h6 mb-4 primary--text">软件工程在金融中的应用</h3>
                      <v-stepper :items="developmentSteps" :model-value="developmentSteps.length" class="elevation-0">
                        <!-- 需求分析 -->
                        <template #item.1>
                          <div class="stepper-content">
                            <v-card flat class="mb-4">
                              <v-card-title class="primary--text">{{ developmentSteps[0] }}</v-card-title>
                              <v-card-text>
                                <div v-if="caseDetails.finance[0]">
                                  <p class="font-weight-bold">教学知识点:</p>
                                  <ul class="pl-4">
                                    <li v-for="(point, i) in caseDetails.finance[0]" :key="i">{{ point }}</li>
                                  </ul>
                                </div>
                              </v-card-text>
                            </v-card>
                            <div class="video-container mb-6">
                              <v-card outlined>
                                <v-card-title class="text-subtitle-1">教学视频</v-card-title>
                                <div class="video-wrapper">
                                  <iframe
                                      width="100%"
                                      height="315"
                                      src="https://www.youtube.com/embed/示例视频ID"
                                      frameborder="0"
                                      allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                                      allowfullscreen
                                  ></iframe>
                                </div>
                                <v-card-actions>
                                  <v-btn color="primary" text>下载资料</v-btn>
                                  <v-btn color="primary" text>扩展阅读</v-btn>
                                </v-card-actions>
                              </v-card>
                            </div>
                          </div>
                        </template>

                        <!-- 概要设计 -->
                        <template #item.2>
                          <div class="stepper-content">
                            <v-card flat class="mb-4">
                              <v-card-title class="primary--text">{{ developmentSteps[1] }}</v-card-title>
                              <v-card-text>
                                <div v-if="caseDetails.finance[1]">
                                  <p class="font-weight-bold">教学知识点:</p>
                                  <ul class="pl-4">
                                    <li v-for="(point, i) in caseDetails.finance[1]" :key="i">{{ point }}</li>
                                  </ul>
                                </div>
                              </v-card-text>
                            </v-card>
                            <div class="video-container mb-6">
                              <v-card outlined>
                                <v-card-title class="text-subtitle-1">教学视频</v-card-title>
                                <div class="video-wrapper">
                                  <iframe
                                      width="100%"
                                      height="315"
                                      src="https://www.youtube.com/embed/示例视频ID"
                                      frameborder="0"
                                      allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                                      allowfullscreen
                                  ></iframe>
                                </div>
                                <v-card-actions>
                                  <v-btn color="primary" text>下载资料</v-btn>
                                  <v-btn color="primary" text>扩展阅读</v-btn>
                                </v-card-actions>
                              </v-card>
                            </div>
                          </div>
                        </template>

                        <!-- 详细设计 -->
                        <template #item.3>
                          <div class="stepper-content">
                            <v-card flat class="mb-4">
                              <v-card-title class="primary--text">{{ developmentSteps[2] }}</v-card-title>
                              <v-card-text>
                                <div v-if="caseDetails.finance[2]">
                                  <p class="font-weight-bold">教学知识点:</p>
                                  <ul class="pl-4">
                                    <li v-for="(point, i) in caseDetails.finance[2]" :key="i">{{ point }}</li>
                                  </ul>
                                </div>
                              </v-card-text>
                            </v-card>
                            <div class="video-container mb-6">
                              <v-card outlined>
                                <v-card-title class="text-subtitle-1">教学视频</v-card-title>
                                <div class="video-wrapper">
                                  <iframe
                                      width="100%"
                                      height="315"
                                      src="https://www.youtube.com/embed/示例视频ID"
                                      frameborder="0"
                                      allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                                      allowfullscreen
                                  ></iframe>
                                </div>
                                <v-card-actions>
                                  <v-btn color="primary" text>下载资料</v-btn>
                                  <v-btn color="primary" text>扩展阅读</v-btn>
                                </v-card-actions>
                              </v-card>
                            </div>
                          </div>
                        </template>

                        <!-- 测试部署 -->
                        <template #item.4>
                          <div class="stepper-content">
                            <v-card flat class="mb-4">
                              <v-card-title class="primary--text">{{ developmentSteps[3] }}</v-card-title>
                              <v-card-text>
                                <div v-if="caseDetails.finance[3]">
                                  <p class="font-weight-bold">教学知识点:</p>
                                  <ul class="pl-4">
                                    <li v-for="(point, i) in caseDetails.finance[3]" :key="i">{{ point }}</li>
                                  </ul>
                                </div>
                              </v-card-text>
                            </v-card>
                            <div class="video-container mb-6">
                              <v-card outlined>
                                <v-card-title class="text-subtitle-1">教学视频</v-card-title>
                                <div class="video-wrapper">
                                  <iframe
                                      width="100%"
                                      height="315"
                                      src="https://www.youtube.com/embed/示例视频ID"
                                      frameborder="0"
                                      allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                                      allowfullscreen
                                  ></iframe>
                                </div>
                                <v-card-actions>
                                  <v-btn color="primary" text>下载资料</v-btn>
                                  <v-btn color="primary" text>扩展阅读</v-btn>
                                </v-card-actions>
                              </v-card>
                            </div>
                          </div>
                        </template>
                      </v-stepper>
                    </v-window-item>
                  </v-window>
                </v-card-text>
              </v-card>
            </v-window-item>

            <!-- 课程思政 -->
            <v-window-item value="ideology">
              <v-card elevation="2">
                <v-card-title class="text-h5 primary--text">课程思政</v-card-title>
                <v-card-text>
                  <v-tabs v-model="ideologyTab" grow color="primary">
                    <v-tab value="medical">医疗案例思政分析</v-tab>
                    <v-tab value="finance">金融案例思政分析</v-tab>
                  </v-tabs>

                  <v-window v-model="ideologyTab" class="mt-4">
                    <v-window-item value="medical">
                      <h3 class="text-h6 mb-4 primary--text">医疗软件案例的思政元素</h3>
                      <v-list class="elevation-1 rounded-lg">
                        <v-list-item
                            v-for="(item, index) in ideologyElements.medical"
                            :key="index"
                            class="mb-2"
                        >
                          <template v-slot:prepend>
                            <v-icon color="primary" class="mr-4">mdi-check-circle</v-icon>
                          </template>
                          <v-list-item-title class="font-weight-bold text-subtitle-1">{{ item.element }}</v-list-item-title>
                          <v-list-item-subtitle class="text-body-1">{{ item.description }}</v-list-item-subtitle>
                        </v-list-item>
                      </v-list>
                    </v-window-item>

                    <v-window-item value="finance">
                      <h3 class="text-h6 mb-4 primary--text">金融软件案例的思政元素</h3>
                      <v-list class="elevation-1 rounded-lg">
                        <v-list-item
                            v-for="(item, index) in ideologyElements.finance"
                            :key="index"
                            class="mb-2"
                        >
                          <template v-slot:prepend>
                            <v-icon color="primary" class="mr-4">mdi-check-circle</v-icon>
                          </template>
                          <v-list-item-title class="font-weight-bold text-subtitle-1">{{ item.element }}</v-list-item-title>
                          <v-list-item-subtitle class="text-body-1">{{ item.description }}</v-list-item-subtitle>
                        </v-list-item>
                      </v-list>
                    </v-window-item>
                  </v-window>
                </v-card-text>
              </v-card>
            </v-window-item>

            <!-- 政策文件 -->
            <v-window-item value="policies">
              <v-card elevation="2">
                <v-card-title class="text-h5 primary--text">政策文件</v-card-title>
                <v-card-text>
                  <v-timeline side="end" align="start" class="policy-timeline">
                    <v-timeline-item
                        v-for="(policy, index) in policies"
                        :key="index"
                        dot-color="primary"
                        size="small"
                    >
                      <template v-slot:opposite>
                        <span class="text-caption">{{ policy.date }}</span>
                      </template>
                      <v-card class="elevation-2 policy-card" hover>
                        <v-card-title class="text-subtitle-1 font-weight-bold">
                          {{ policy.title }}
                        </v-card-title>
                        <v-card-subtitle class="text-caption">
                          <v-icon small class="mr-1">mdi-account-tie</v-icon>
                          {{ policy.issuer }}
                        </v-card-subtitle>
                        <v-card-actions>
                          <v-btn
                              variant="text"
                              color="primary"
                              size="small"
                              :href="policy.link"
                              target="_blank"
                          >
                            查看全文
                            <v-icon right small>mdi-open-in-new</v-icon>
                          </v-btn>
                        </v-card-actions>
                      </v-card>
                    </v-timeline-item>
                  </v-timeline>
                </v-card-text>
              </v-card>
            </v-window-item>
          </v-window>
        </v-col>
      </v-row>
    </v-container>

    <!-- 课程列表 -->
    <v-container class="mt-8 mb-12" style="max-width: 1200px">
      <v-row>
        <v-col cols="12" class="px-4">
          <div class="d-flex align-center mb-4">
            <h2 class="text-h4 primary--text font-weight-bold">热门课程</h2>
            <v-spacer />
            <v-btn variant="text" color="primary" class="text-capitalize">查看更多 <v-icon right>mdi-chevron-right</v-icon></v-btn>
          </div>
          <v-divider thickness="2" class="mb-6" />
        </v-col>

        <v-col
            v-for="(course, index) in hotCourses"
            :key="index"
            cols="12"
            sm="6"
            md="4"
            lg="3"
            class="px-4"
        >
          <v-card
              hover
              @click="viewCourse(course.id)"
              class="h-100 course-card"
              elevation="2"
          >
            <v-img
                :src="course.image"
                height="160"
                cover
                gradient="to bottom, rgba(0,0,0,0.1), rgba(0,0,0,0.5)"
                class="course-image"
            >
              <v-chip
                  v-if="course.tag"
                  color="primary"
                  small
                  class="ma-2"
              >
                {{ course.tag }}
              </v-chip>
              <v-card-title class="white--text text-subtitle-1 font-weight-bold">
                {{ course.title }}
              </v-card-title>
            </v-img>

            <v-card-subtitle class="px-4 pt-4">
              <v-icon small class="mr-1">mdi-account</v-icon>
              {{ course.teacher }}
            </v-card-subtitle>

            <v-card-text class="px-4 pb-4">
              <div class="d-flex align-center">
                <v-rating
                    :value="course.rating"
                    color="amber"
                    dense
                    half-increments
                    readonly
                    size="16"
                    class="mr-2"
                />
                <span class="text-caption grey--text">
                  {{ course.rating }} ({{ course.students }}人)
                </span>
              </div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </v-container>

    <!-- 导入新案例对话框 -->
    <v-dialog v-model="showAddCaseDialog" max-width="600">
      <v-card>
        <v-card-title class="primary--text">导入新案例</v-card-title>
        <v-divider></v-divider>
        <v-card-text>
          <v-form class="mt-4">
            <v-select
                label="案例类型"
                :items="['医疗', '金融', '教育', '其他']"
                v-model="newCase.type"
                outlined
                required
            ></v-select>

            <v-text-field
                label="案例名称"
                v-model="newCase.name"
                outlined
                required
            ></v-text-field>

            <v-file-input
                label="上传案例文件"
                v-model="newCase.file"
                accept=".pdf,.doc,.docx"
                outlined
                prepend-icon="mdi-paperclip"
            ></v-file-input>

            <v-textarea
                label="案例描述"
                v-model="newCase.description"
                rows="3"
                outlined
            ></v-textarea>
          </v-form>
        </v-card-text>
        <v-divider></v-divider>
        <v-card-actions class="px-4 py-3">
          <v-spacer></v-spacer>
          <v-btn color="grey" text @click="showAddCaseDialog = false">取消</v-btn>
          <v-btn color="primary" depressed @click="addNewCase">提交</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script>
import * as d3 from 'd3'
import ccutImage from '@/assets/ccut.jpg'
import sizhengImage from '@/assets/sizheng.png'
import sizheng2Image from '@/assets/sizheng2.png'

export default {
  data() {
    return {
      images: [
        { src: sizhengImage },
        { src: sizheng2Image },
        { src: ccutImage }
      ],
      hotCourses: [
        {
          id: 1,
          title: '习近平新时代中国特色社会主义思想概论',
          teacher: '张教授',
          students: 1250,
          rating: 4.8,
          tag: '国家级',
          image: sizhengImage,
        },
        {
          id: 2,
          title: '马克思主义基本原理',
          teacher: '王教授',
          students: 980,
          rating: 4.7,
          tag: '省级',
          image: sizheng2Image,
        },
        {
          id: 3,
          title: '中国近现代史纲要',
          teacher: '李教授',
          students: 876,
          rating: 4.6,
          tag: '校级',
          image: ccutImage,
        },
        {
          id: 4,
          title: '思想道德与法治',
          teacher: '赵教授',
          students: 765,
          rating: 4.5,
          image: sizhengImage,
        }
      ],
      activeTab: 'cases',
      caseTab: 'medical',
      ideologyTab: 'medical',
      developmentSteps: ['需求分析', '概要设计', '详细设计', '测试部署'],
      caseDetails: {
        medical: [
          [
            '医疗行业需求特殊性分析',
            '患者隐私保护需求',
            '医疗数据安全要求'
          ],
          [
            '医疗系统架构设计',
            '数据流图设计',
            '数据库ER图设计'
          ],
          [
            '医疗业务流程实现',
            'HIPAA合规实现',
            '电子病历系统设计'
          ],
          [
            '医疗系统测试规范',
            '医疗数据迁移方案',
            '系统上线流程'
          ]
        ],
        finance: [
          [
            '金融业务需求分析',
            '金融风险控制需求',
            '交易安全需求'
          ],
          [
            '金融系统架构设计',
            '微服务拆分',
            '高并发处理方案'
          ],
          [
            '交易系统核心实现',
            '金融数据加密',
            '风控系统实现'
          ],
          [
            '金融系统压力测试',
            '安全渗透测试',
            '灾备方案设计'
          ]
        ]
      },
      ideologyElements: {
        medical: [
          {
            element: '医者仁心',
            description: '通过医疗软件开发培养学生关爱生命、尊重医德的人文精神'
          },
          {
            element: '社会责任',
            description: '强调医疗软件对社会的重大责任，培养社会责任感'
          },
          {
            element: '伦理道德',
            description: '讨论医疗数据隐私保护的伦理道德问题'
          }
        ],
        finance: [
          {
            element: '诚信教育',
            description: '通过金融软件开发强调诚信的重要性'
          },
          {
            element: '风险意识',
            description: '培养金融风险防范意识，树立正确的金钱观'
          },
          {
            element: '法治观念',
            description: '强调金融合规，培养法治观念'
          }
        ]
      },
      policies: [
        {
          title: '高等学校课程思政建设指导纲要',
          issuer: '教育部',
          date: '2020-05-28',
          link: 'http://www.moe.gov.cn/srcsite/A08/s7056/202006/t20200603_462437.html'
        },
        {
          title: '关于深化新时代学校思想政治理论课改革创新的若干意见',
          issuer: '中共中央办公厅、国务院办公厅',
          date: '2019-08-14',
          link: 'http://www.gov.cn/zhengce/2019-08/14/content_5421252.htm'
        },
        {
          title: '新时代高校思想政治理论课教学工作基本要求',
          issuer: '教育部',
          date: '2018-04-12',
          link: 'http://www.moe.gov.cn/srcsite/A13/moe_772/201804/t20180424_334099.html'
        }
      ],
      showAddCaseDialog: false,
      newCase: {
        type: '',
        name: '',
        file: null,
        description: ''
      },
      // 知识图谱相关数据
      legendItems: [
        { color: '#4e79a7', text: '核心概念' },
        { color: '#f28e2b', text: '教学方法' },
        { color: '#e15759', text: '案例领域' },
        { color: '#76b7b2', text: '开发阶段' },
        { color: '#59a14f', text: '思政元素' },
        { color: '#edc948', text: '技术工具' },
        { color: '#b07aa1', text: '具体案例' }
      ],
      graphData: {
        nodes: [
          // 核心概念节点
          { id: 1, name: "软件工程", type: "core", size: 28 },
          { id: 2, name: "需求分析", type: "core", size: 24 },
          { id: 3, name: "系统设计", type: "core", size: 24 },
          { id: 4, name: "编码实现", type: "core", size: 24 },
          { id: 5, name: "测试部署", type: "core", size: 24 },
          { id: 6, name: "项目管理", type: "core", size: 22 },

          // 教学方法节点
          { id: 7, name: "案例教学", type: "method", size: 26 },
          { id: 8, name: "项目驱动", type: "method", size: 22 },
          { id: 9, name: "翻转课堂", type: "method", size: 20 },
          { id: 10, name: "小组协作", type: "method", size: 20 },
          { id: 11, name: "情景模拟", type: "method", size: 20 },

          // 案例领域节点
          { id: 12, name: "医疗软件", type: "domain", size: 24 },
          { id: 13, name: "金融系统", type: "domain", size: 24 },
          { id: 14, name: "教育平台", type: "domain", size: 22 },
          { id: 15, name: "电子商务", type: "domain", size: 22 },

          // 开发阶段节点
          { id: 16, name: "需求收集", type: "phase", size: 20 },
          { id: 17, name: "原型设计", type: "phase", size: 20 },
          { id: 18, name: "架构设计", type: "phase", size: 20 },
          { id: 19, name: "数据库设计", type: "phase", size: 20 },
          { id: 20, name: "接口设计", type: "phase", size: 20 },
          { id: 21, name: "单元测试", type: "phase", size: 20 },
          { id: 22, name: "集成测试", type: "phase", size: 20 },

          // 思政元素节点
          { id: 23, name: "社会责任", type: "ideology", size: 22 },
          { id: 24, name: "职业道德", type: "ideology", size: 22 },
          { id: 25, name: "团队协作", type: "ideology", size: 20 },
          { id: 26, name: "创新精神", type: "ideology", size: 20 },
          { id: 27, name: "工匠精神", type: "ideology", size: 20 },

          // 技术工具节点
          { id: 28, name: "UML建模", type: "tool", size: 20 },
          { id: 29, name: "Git版本控制", type: "tool", size: 20 },
          { id: 30, name: "JIRA管理", type: "tool", size: 18 },
          { id: 31, name: "Jenkins", type: "tool", size: 18 },
          { id: 32, name: "Docker", type: "tool", size: 18 },

          // 具体案例节点
          { id: 33, name: "电子病历系统", type: "case", size: 22 },
          { id: 34, name: "医保结算系统", type: "case", size: 22 },
          { id: 35, name: "在线交易平台", type: "case", size: 22 },
          { id: 36, name: "风险控制系统", type: "case", size: 22 }
        ],
        links: [
          // 核心概念关联
          { source: 1, target: 2, value: 5 }, { source: 1, target: 3, value: 5 },
          { source: 1, target: 4, value: 5 }, { source: 1, target: 5, value: 5 },
          { source: 1, target: 6, value: 4 },

          // 教学方法关联
          { source: 7, target: 8, value: 4 }, { source: 7, target: 9, value: 3 },
          { source: 7, target: 10, value: 4 }, { source: 7, target: 11, value: 3 },

          // 案例领域关联
          { source: 7, target: 12, value: 5 }, { source: 7, target: 13, value: 5 },
          { source: 7, target: 14, value: 4 }, { source: 7, target: 15, value: 4 },

          // 开发阶段关联
          { source: 2, target: 16, value: 5 }, { source: 2, target: 17, value: 4 },
          { source: 3, target: 18, value: 5 }, { source: 3, target: 19, value: 5 },
          { source: 3, target: 20, value: 4 }, { source: 4, target: 21, value: 4 },
          { source: 5, target: 22, value: 5 },

          // 思政元素关联
          { source: 12, target: 23, value: 4 }, { source: 12, target: 24, value: 5 },
          { source: 13, target: 23, value: 5 }, { source: 13, target: 25, value: 4 },
          { source: 14, target: 26, value: 4 }, { source: 15, target: 27, value: 4 },

          // 技术工具关联
          { source: 3, target: 28, value: 5 }, { source: 4, target: 29, value: 5 },
          { source: 6, target: 30, value: 4 }, { source: 5, target: 31, value: 4 },
          { source: 5, target: 32, value: 4 },

          // 具体案例关联
          { source: 12, target: 33, value: 5 }, { source: 12, target: 34, value: 5 },
          { source: 13, target: 35, value: 5 }, { source: 13, target: 36, value: 5 },
          { source: 33, target: 16, value: 4 }, { source: 33, target: 19, value: 5 },
          { source: 35, target: 18, value: 5 }, { source: 35, target: 20, value: 5 }
        ]
      },
      simulation: null,
      svg: null,
      zoom: null
    }
  },
  methods: {
    viewCourse(id) {
      this.$router.push({
        name: 'course-detail',
        params: { id }
      })
    },
    addNewCase() {
      console.log('添加新案例:', this.newCase);
      this.showAddCaseDialog = false;
      this.newCase = {
        type: '',
        name: '',
        file: null,
        description: ''
      };
    },
    // 初始化知识图谱
    initKnowledgeGraph() {
      const container = document.getElementById('knowledge-graph-container');
      if (!container) return;

      const width = container.clientWidth;
      const height = Math.max(600, window.innerHeight * 0.7);

      // 清除现有内容
      d3.select('#knowledge-graph-container').selectAll('*').remove();

      // 创建SVG
      this.svg = d3.select('#knowledge-graph-container')
          .append('svg')
          .attr('width', width)
          .attr('height', height)
          .attr('viewBox', [0, 0, width, height])
          .attr('style', 'max-width: 100%; height: auto;');

      // 创建缩放行为
      this.zoom = d3.zoom()
          .scaleExtent([0.1, 8])
          .on('zoom', (event) => {
            this.svg.selectAll('g').attr('transform', event.transform);
          });

      this.svg.call(this.zoom);

      // 创建主图形组
      const g = this.svg.append('g');

      // 定义箭头
      g.append('defs').selectAll('marker')
          .data(['end'])
          .join('marker')
          .attr('id', d => d)
          .attr('viewBox', '0 -5 10 10')
          .attr('refX', 25)
          .attr('refY', 0)
          .attr('markerWidth', 6)
          .attr('markerHeight', 6)
          .attr('orient', 'auto')
          .append('path')
          .attr('d', 'M0,-5L10,0L0,5')
          .attr('fill', '#999');

      // 创建力导向图模拟
      this.simulation = d3.forceSimulation(this.graphData.nodes)
          .force('link', d3.forceLink(this.graphData.links).id(d => d.id).distance(100))
          .force('charge', d3.forceManyBody().strength(-500))
          .force('x', d3.forceX(width / 2).strength(0.05))
          .force('y', d3.forceY(height / 2).strength(0.05))
          .force('collision', d3.forceCollide().radius(d => d.size + 5));

      // 创建连线
      const link = g.append('g')
          .selectAll('line')
          .data(this.graphData.links)
          .join('line')
          .attr('stroke', '#999')
          .attr('stroke-opacity', 0.6)
          .attr('stroke-width', d => Math.sqrt(d.value))
          .attr('marker-end', 'url(#end)');

      // 创建节点组
      const node = g.append('g')
          .selectAll('g')
          .data(this.graphData.nodes)
          .join('g')
          .call(this.drag(this.simulation));

      // 添加节点圆圈
      node.append('circle')
          .attr('r', d => d.size)
          .attr('fill', d => {
            switch(d.type) {
              case 'core': return '#4e79a7';
              case 'method': return '#f28e2b';
              case 'domain': return '#e15759';
              case 'phase': return '#76b7b2';
              case 'ideology': return '#59a14f';
              case 'tool': return '#edc948';
              case 'case': return '#b07aa1';
              default: return '#9c755f';
            }
          })
          .attr('stroke', '#fff')
          .attr('stroke-width', 1.5)
          .on('mouseover', function(event, d) {
            // 高亮当前节点和关联节点
            node.select('circle').attr('opacity', 0.2);
            d3.select(this).attr('opacity', 1);

            link.attr('stroke-opacity', l => (l.source === d || l.target === d) ? 1 : 0.1);

            // 显示工具提示
            tooltip.style('visibility', 'visible')
                .html(`<strong>${d.name}</strong><br>类型: ${this.getTypeName(d.type)}`);
          }.bind(this))
          .on('mousemove', function(event) {
            tooltip.style('top', (event.pageY - 10) + 'px')
                .style('left', (event.pageX + 10) + 'px');
          })
          .on('mouseout', function() {
            // 恢复默认状态
            node.select('circle').attr('opacity', 1);
            link.attr('stroke-opacity', 0.6);
            tooltip.style('visibility', 'hidden');
          });

      // 添加节点文本
      node.append('text')
          .attr('dy', 4)
          .attr('text-anchor', 'middle')
          .text(d => d.name)
          .attr('fill', '#fff')
          .attr('font-size', d => Math.max(10, d.size / 2))
          .attr('pointer-events', 'none');

      // 创建工具提示
      const tooltip = d3.select('#knowledge-graph-container')
          .append('div')
          .attr('class', 'graph-tooltip')
          .style('position', 'absolute')
          .style('visibility', 'hidden')
          .style('background', 'rgba(0,0,0,0.8)')
          .style('color', '#fff')
          .style('padding', '8px')
          .style('border-radius', '4px')
          .style('font-size', '14px')
          .style('z-index', '10');

      // 更新模拟
      this.simulation.on('tick', () => {
        link
            .attr('x1', d => d.source.x)
            .attr('y1', d => d.source.y)
            .attr('x2', d => d.target.x)
            .attr('y2', d => d.target.y);

        node.attr('transform', d => `translate(${d.x},${d.y})`);
      });
    },

    // 拖拽行为
    drag(simulation) {
      const dragstarted = (event) => {
        if (!event.active) simulation.alphaTarget(0.3).restart();
        event.subject.fx = event.subject.x;
        event.subject.fy = event.subject.y;
      };

      const dragged = (event) => {
        event.subject.fx = event.x;
        event.subject.fy = event.y;
      };

      const dragended = (event) => {
        if (!event.active) simulation.alphaTarget(0);
        event.subject.fx = null;
        event.subject.fy = null;
      };

      return d3.drag()
          .on('start', dragstarted)
          .on('drag', dragged)
          .on('end', dragended);
    },

    // 获取类型名称
    getTypeName(type) {
      const types = {
        'core': '核心概念',
        'method': '教学方法',
        'domain': '案例领域',
        'phase': '开发阶段',
        'ideology': '思政元素',
        'tool': '技术工具',
        'case': '具体案例'
      };
      return types[type] || type;
    },

    // 重置视图
    resetGraph() {
      if (!this.svg || !this.zoom) return;
      this.svg.transition()
          .duration(750)
          .call(this.zoom.transform, d3.zoomIdentity);
    },

    // 放大
    zoomIn() {
      if (!this.svg || !this.zoom) return;
      this.svg.transition()
          .call(this.zoom.scaleBy, 1.2);
    },

    // 缩小
    zoomOut() {
      if (!this.svg || !this.zoom) return;
      this.svg.transition()
          .call(this.zoom.scaleBy, 0.8);
    }
  },
  mounted() {
    this.initKnowledgeGraph();
    window.addEventListener('resize', this.initKnowledgeGraph);
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.initKnowledgeGraph);
    if (this.simulation) {
      this.simulation.stop();
    }
  },
  watch: {
    activeTab(newVal) {
      if (newVal === 'knowledgeGraph') {
        this.$nextTick(() => {
          this.initKnowledgeGraph();
        });
      }
    }
  }
}
</script>

<style scoped>
/* 轮播图容器 - 完全对齐 */
.carousel-container {
  width: 100vw;
  position: relative;
  left: 50%;
  right: 50%;
  margin-left: -50vw;
  margin-right: -50vw;
  margin-top: -25px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 自定义轮播点样式 */
.custom-dots {
  position: absolute;
  bottom: 20px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  gap: 8px;
  z-index: 1;
}

.custom-dots button {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  padding: 0;
  transition: all 0.3s ease;
}

.custom-dots button.active {
  background: #fff;
  transform: scale(1.2);
}

/* 侧边栏样式 */
.sidebar {
  border-right: 1px solid rgba(0, 0, 0, 0.12);
}

/* 案例步骤样式 */
.stepper-content {
  padding: 16px;
  background-color: #f9f9f9;
  border-radius: 8px;
  margin-bottom: 16px;
}

.video-container {
  margin-top: 24px;
}

.video-wrapper {
  position: relative;
  padding-bottom: 56.25%; /* 16:9 比例 */
  height: 0;
  overflow: hidden;
}

.video-wrapper iframe {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

/* 课程卡片样式 */
.course-card {
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  border-radius: 8px !important;
  overflow: hidden;
}

.course-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.15) !important;
}

.course-image .v-card__title {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.7), transparent);
}

/* 政策时间线样式 */
.policy-timeline {
  padding: 0 16px;
}

.policy-card {
  border-left: 4px;
  transition: all 0.3s ease;
}

.policy-card:hover {
  transform: translateX(4px);
}

/* 知识图谱容器样式 */
.knowledge-graph {
  width: 100%;
  height: 600px;
  border: 1px solid #eee;
  border-radius: 8px;
  background-color: #f9f9f9;
  position: relative;
  overflow: hidden;
}

/* 图例样式 */
.graph-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 12px;
  background-color: #f5f5f5;
  border-radius: 6px;
}

.legend-item {
  display: flex;
  align-items: center;
  margin-right: 12px;
}

.legend-color {
  display: inline-block;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  margin-right: 6px;
  border: 1px solid #ddd;
}

.legend-text {
  font-size: 14px;
  color: #555;
}

/* 控制按钮样式 */
.graph-controls {
  display: flex;
  gap: 8px;
}

/* 工具提示样式 */
.graph-tooltip {
  position: absolute;
  visibility: hidden;
  background: rgba(0, 0, 0, 0.8);
  color: #fff;
  padding: 8px;
  border-radius: 4px;
  font-size: 14px;
  z-index: 10;
  pointer-events: none;
}

/* 全局调整 */
.v-card {
  border-radius: 8px !important;
}

.v-tab {
  font-weight: 500;
}

.v-stepper {
  background: transparent !important;
}

.v-stepper__header {
  box-shadow: none !important;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12);
}

.v-stepper__step {
  padding: 12px !important;
}

.v-list-item {
  border-radius: 6px;
  transition: all 0.2s ease;
}

.v-list-item:hover {
  background-color: #f5f5f5;
}
</style>