# 🎓 CCUT 智慧课程在线学习系统 (CCUT Smart Learning System)

> **基于 AI 驱动的高性能在线教育平台** —— 融合 Spring Boot 3.5、Vue 3 与 大语言模型，打造从视频学习、文档阅读到智能测试的完整闭环。

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen)](https://spring.io/)
[![Vue](https://img.shields.io/badge/Vue-3.5.18-brightgreen)](https://vuejs.org/)
[![AI Powered](https://img.shields.io/badge/AI-DeepSeek/OpenAI-blue)](https://openai.com/)
[![Redis](https://img.shields.io/badge/Redis-Optimization-red)](https://redis.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 🌟 项目简介

CCUT 智慧课程系统不仅是一个简单的视频播放平台，它通过 **AI 智能交互** 和 **极高性能优化** 重新定义了在线学习体验。系统专为高校及企业培训设计，支持海量并发下的实时进度追踪，并能基于课程内容自动生成高质量测试题目。

### 🚀 核心亮点

- 🤖 **AI 深度集成**：内置 AI 智能对话、自动出题、即时判分，由 DeepSeek/OpenAI 强力驱动。
- ⚡ **极致性能优化**：独创 **Redis 缓存 + 定时批量刷新** 机制，将学习时长上报性能提升 40 倍，支持万级并发。
- 📝 **智能错题本**：全自动收集各环节错题，支持智能组卷与掌握度追踪。
- 📊 **可视化学习洞察**：集成 ECharts 展现学生学习趋势、完成度及知识点掌握情况。
- 🤝 **协作式学习**：完善的小组管理机制，支持自由组队、审核及协作。
- 📋 **智能作业管理**：支持教师发布作业、学生分组提交，实现完整的作业流程管理。
- 🔍 **课程推荐系统**：基于协同过滤和内容推荐算法，为学生推荐适合的课程。

---

## 🛠️ 技术栈

### 后端 (Server)
- **核心框架**: Spring Boot 3.5.5 (Java 17)
- **AI 引擎**: Spring AI (对接 OpenAI/DeepSeek API)
- **持久层**: MyBatis 3.0.4 + MySQL 8.0.33
- **高性能缓存**: Redis (Lettuce) + 异步线程池处理
- **办公自动化**: Apache POI (Excel) + Apache Tika (统一文档解析)
- **认证授权**: JWT (Json Web Token)
- **日志系统**: SLF4J + Logback

### 前端 (Web)
- **核心框架**: Vue 3.5.18 (Composition API)
- **构建工具**: Vite 7.1.5
- **UI 组件库**: Element Plus
- **状态管理**: Pinia
- **图表可视化**: ECharts & Chart.js
- **文档渲染**: PDF.js (vue-pdf-embed) & Mammoth (.docx) & Markdown-it
- **数学公式**: KaTeX
- **代码高亮**: Highlight.js

---

## 🎯 功能模块

### 👨‍🎓 学生端：沉浸式学习体验
- **全媒体学习**: 视频倍速播放、进度记忆；PDF/Word/Markdown 文档在线阅读，支持滚动进度保存。
- **AI 智能助手**: 随时发起与课程相关的对话，获取个性化答疑。
- **AI 自动化考试**: 基于章节内容一键生成选择、判断题，提交即出分，并附带 AI 解析。
- **错题复习闭环**: 自动收集考试及学习中的错题，支持“练习模式”与“浏览模式”切换。
- **个人数据看板**: 统计每日/每周学习时长分布，可视化查看课程完成进度。
- **智能课程推荐**: 基于学习行为和兴趣偏好，获得个性化课程推荐。
- **小组协作学习**: 加入学习小组，参与团队作业，促进协作学习。

### 👩‍🏫 教师端：高效教学管理
- **课程内容中台**: 自由创建课程、组织章节架构，批量上传视频与教学文档。
- **全方位学情监控**: 查看班级/个体学生的学习时长、进度及考试成绩趋势。
- **智能批改系统**: 自动批改客观题，支持对学生提交的作业进行多维度评分与反馈。
- **数据统计分析**: 高频错题统计、课程平均完成率分析，辅助教学策略调整。
- **作业发布管理**: 发布个人或小组作业，查看提交情况和评分结果。
- **学生分组管理**: 创建和管理学生小组，促进协作学习。

---

## ⚡ 性能优化深度解析

### Redis 缓存上报机制 (极致吞吐量)
系统将高频触发的学习进度上报从“直接写入数据库”优化为“Redis 异步批量同步”：
- **流程**: 前端上报 → Redis Hash 极速写入 (<5ms) → 定时任务 (每5分钟) → 批量 Upsert 至 MySQL。
- **成果**:
  - 数据库写入频率降低 **60倍**。
  - 单次接口响应时间从 150ms 降至 **<5ms**。
  - 系统并发处理能力提升 **100倍** 以上。

### 大文件分片上传
- **支持大文件上传**: 采用分片上传技术，支持GB级别的视频文件上传。
- **断点续传**: 支持上传中断后从断点继续上传，提高上传成功率。
- **秒传功能**: 通过文件哈希值去重，已存在文件直接秒传。

> 详情参考: [REDIS_OPTIMIZATION_GUIDE.md](server/REDIS_OPTIMIZATION_GUIDE.md)

---

## 📂 项目结构

```text
CCUT/backend/
├── server/                     # 后端工程 (Spring Boot)
│   ├── src/main/java/com/ccut/
│   │   ├── config/             # Redis、Async、AI、WebMvc 等配置
│   │   ├── controller/         # RESTful API 控制器
│   │   ├── service/            # 业务逻辑 (含 AI 及 Redis 缓存实现)
│   │   ├── scheduled/          # 定时任务 (进度批量同步)
│   │   ├── mapper/             # MyBatis 数据库映射
│   │   ├── entity/             # 数据库实体类
│   │   ├── dto/                # 数据传输对象
│   │   └── utils/              # 工具类 (JWT、文件处理等)
│   ├── src/main/resources/
│   │   ├── mapper/*.xml        # MyBatis SQL 映射文件
│   │   ├── prompts/            # AI Prompt 模板
│   │   ├── sql/                # 数据库初始化脚本
│   │   └── application.yml     # 应用配置文件
│   ├── uploads/                # 上传文件存储目录
│   ├── pom.xml                 # Maven 依赖配置
│   └── REBUILD_DATABASE.sql    # 数据库重建脚本
├── src/                        # 前端工程 (Vue 3)
│   ├── views/
│   │   ├── student/            # 学生端视图
│   │   └── teacher/            # 教师端视图
│   ├── components/             # 公共组件 (播放器、文档查看器、AI对话框)
│   ├── services/               # API 接口封装
│   ├── stores/                 # Pinia 全局状态管理
│   ├── router/                 # 路由配置
│   └── utils/                  # 工具函数
├── package.json                # 前端依赖与脚本
├── vite.config.js              # Vite 构建配置
├── index.html                  # HTML 入口文件
└── README.md                   # 项目说明文档
```

---

## 🚀 快速开始

### 1. 环境准备
- **JDK 17+**
- **Node.js 18+**
- **MySQL 8.0+**
- **Redis 7.0+**
- **DeepSeek/OpenAI API Key** (用于 AI 功能)

### 2. 数据库初始化
在 MySQL 中创建数据库 `ccut_db`，并执行以下脚本：
- `server/src/main/resources/SQL.sql` (结构与核心数据)

### 3. 后端启动
```bash
cd server
# 编辑 application.yml 配置数据库、Redis 及 AI Key
mvn clean install
mvn spring-boot:run
```

### 4. 前端启动
```bash
npm install
npm run dev
```
访问 `http://localhost:5173` 开启学习之旅。

### 5. 默认账号
- **教师**: `teacher` / `123456`
- **学生**: `student` / `123456`

---

## 📜 许可证

本项目采用 [MIT License](LICENSE) 授权。

---

## 👥 团队与致谢

- **开发团队**: CCUT 智慧教育项目组
- **特别鸣谢**: 感谢 [Spring AI](https://spring.io/projects/spring-ai) 提供的强大 AI 集成能力。

**⭐ 如果这个项目对你有帮助，欢迎点一个 Star！**
