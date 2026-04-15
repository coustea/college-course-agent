# CCUT 智慧课程在线学习系统

> 基于 AI 驱动的新一代在线教育平台，融合 Spring Boot 3 + Vue 3 + DeepSeek 大模型，构建从课程学习、智能测评到协作管理的完整教学闭环。

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen)](https://spring.io/)
[![Vue](https://img.shields.io/badge/Vue-3.5.18-brightgreen)](https://vuejs.org/)
[![AI Powered](https://img.shields.io/badge/AI-DeepSeek-blue)](https://platform.deepseek.com/)
[![Redis](https://img.shields.io/badge/Redis-7.0-red)](https://redis.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 目录

- [项目简介](#项目简介)
- [核心亮点](#核心亮点)
- [技术栈](#技术栈)
- [系统架构](#系统架构)
- [功能模块](#功能模块)
- [数据库设计](#数据库设计)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [性能优化](#性能优化)
- [API 规范](#api-规范)
- [许可证](#许可证)

---

## 项目简介

CCUT 智慧课程在线学习系统是一款面向高校的 AI 驱动在线教育平台。系统围绕教师教学和学生学习的核心场景，提供了课程管理、多媒体学习、AI 智能对话、自动出题与判分、错题管理、学习数据分析、协作分组等完整功能。

系统采用前后端分离架构，后端基于 Spring Boot 3.5.5 + MyBatis + Redis 构建，前端使用 Vue 3 + Element Plus，AI 能力通过 Spring AI 框架对接 DeepSeek 大模型，实现原生 Function Calling 工具调用。

---

## 核心亮点

### AI 深度集成

- **智能对话助手"学小微"**：支持联网搜索、文件分析（PDF/Word/Excel/图片）、代码执行、数据库查询，具备 6 个社区技能包（文档生成、课程材料制作、闪卡创建等）
- **AI 自动出题与判分**：基于课程章节内容一键生成选择/判断题，提交即出分并附带 AI 解析
- **Spring AI Function Calling**：采用原生工具调用机制，非手动 ReAct 循环，支持同步和 SSE 流式响应

### 极致性能优化

- **Redis 缓存进度上报**：学习进度先写入 Redis Hash（<5ms），定时任务每 5 分钟批量刷新至 MySQL
- **60 倍写入降低**：数据库写入频率降低约 60 倍，单接口响应从 150ms 降至 <5ms
- **100 倍并发提升**：系统并发处理能力提升超过 100 倍

### 完整教学闭环

- **全媒体学习**：视频倍速播放 + 进度记忆；PDF/Word/Markdown 在线阅读 + 滚动进度保存
- **智能错题本**：自动收集考试与练习中的错题，支持练习模式与浏览模式
- **协作学习**：小组创建、成员审批、分组作业提交与评分
- **数据洞察**：ECharts 可视化学情分析，课程完成率、学习时长、知识点掌握度一目了然

---

## 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.5 | 核心框架 |
| Spring AI | 1.0.0 | AI 工具调用（Function Calling） |
| MyBatis | 3.0.4 | ORM 持久层 |
| MySQL | 8.0+ | 关系型数据库 |
| Redis + Lettuce | 7.0+ | 高性能缓存 |
| Apache POI | 5.2.5 | Excel/Word 文档生成 |
| Apache Tika | 3.2.3 | 统一文档解析 |
| JWT (jjwt) | 0.11.5 | 认证授权 |
| Hutool | 5.8.22 | Java 工具库 |
| mp4parser | 1.9.41 | 视频时长提取 |

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5.18 | 前端框架（Composition API） |
| Vite | 7.1.5 | 构建工具 |
| Element Plus | 2.11.2 | UI 组件库 |
| Pinia | 3.0.3 | 状态管理 |
| Vue Router | 4.5.1 | 路由管理 |
| ECharts | 6.0.0 | 数据可视化 |
| Chart.js | 4.5.0 | 数据可视化 |
| PDF.js (vue-pdf-embed) | 2.1.3 | PDF 在线阅读 |
| Mammoth | 1.11.0 | Word 文档渲染 |
| Markdown-it | 14.1.1 | Markdown 渲染 |
| KaTeX | 0.16.28 | 数学公式渲染 |
| Highlight.js | 11.11.1 | 代码高亮 |

---

## 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                    前端 (Vue 3 + Vite)                   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐  │
│  │ 学生端    │ │ 教师端    │ │ AI 对话   │ │ 数据可视化  │  │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └─────┬──────┘  │
│       └─────────────┴───────────┴──────────────┘         │
│                     Axios + JWT                          │
└─────────────────────────┬───────────────────────────────┘
                          │ HTTP / SSE
                          ▼
┌─────────────────────────────────────────────────────────┐
│               后端 (Spring Boot 3.5.5)                   │
│  ┌──────────────────────────────────────────────────┐   │
│  │  Controller 层 — REST API (/api/**)               │   │
│  └──────────────────────┬───────────────────────────┘   │
│  ┌──────────────────────┴───────────────────────────┐   │
│  │  Service 层 — 业务逻辑                            │   │
│  │  ┌───────────┐ ┌───────────┐ ┌────────────────┐  │   │
│  │  │ 课程管理   │ │ 进度追踪   │ │ AI 对话/出题    │  │   │
│  │  └───────────┘ └───────────┘ └────────────────┘  │   │
│  └──────────────────────┬───────────────────────────┘   │
│  ┌──────────────────────┴───────────────────────────┐   │
│  │  Mapper 层 — MyBatis (Java Interface + XML)       │   │
│  └──────────────────────┬───────────────────────────┘   │
│                                                          │
│  ┌───────────┐  ┌────────────┐  ┌──────────────────┐   │
│  │   MySQL    │  │   Redis    │  │   DeepSeek API   │   │
│  │  持久存储   │  │  进度缓存   │  │   AI 工具调用     │   │
│  └───────────┘  └────────────┘  └──────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### AI 系统架构

系统采用 **Spring AI 原生 Function Calling** 机制（非手动 ReAct 循环）：

- **双模型配置**：`chatModel`（deepseek-chat，通用对话）和 `reasoningChatModel`（deepseek-reasoner，深度推理）
- **工具注册**：`AgentToolsConfig` 注册通用工具（联网搜索、Excel/Word 生成），`AgentDbToolsConfig` 注册数据库查询工具（学生信息、学习进度、错题、考试记录等）
- **流式响应**：支持 SSE（Server-Sent Events）实时流式输出
- **文档分析**：通过 Apache Tika 解析上传文件，注入对话上下文

---

## 功能模块

### 学生端

| 模块 | 功能描述 |
|------|----------|
| 课程学习 | 视频倍速播放、进度记忆；PDF/Word/Markdown 在线阅读，滚动进度自动保存 |
| AI 学习助手 | 随时发起对话答疑，支持上传文件分析、联网搜索、个性化学习建议 |
| AI 智能考试 | 基于章节内容自动生成选择题/判断题，提交即出分并附带 AI 解析 |
| 错题本 | 自动收集考试及练习中的错题，支持练习模式（重做）与浏览模式（查看解析） |
| 学习数据看板 | 统计每日/每周学习时长、课程完成进度，ECharts 可视化呈现 |
| 课程推荐 | 基于学习行为和兴趣偏好的智能课程推荐 |
| 小组协作 | 创建/加入学习小组，参与团队作业 |

### 教师端

| 模块 | 功能描述 |
|------|----------|
| 工作台 | 课程总数、学生总数、平均完成率、学习小组数一览；待办事项管理；课程学情图表 |
| 课程管理 | 创建/编辑课程，组织章节架构，上传视频与教学文档，发布/取消发布 |
| 学生管理 | 学生列表、分组管理（创建/审批/分配）、学习表现分析 |
| 作业管理 | 发布个人/小组作业，查看提交情况，多维度评分与反馈 |
| 学情分析 | 课程完成率统计、高频错题分析、学生学习时长分布 |
| AI 教学助手 | 智能教学问答、文档生成（教案/大纲/试题）、学情数据查询 |

---

## 数据库设计

系统共 25 张数据表，按功能域划分：

### 用户与认证

| 表名 | 说明 |
|------|------|
| `users` | 用户账号（含 role: student/teacher），存储 JWT token |
| `students` | 学生详细信息（姓名、学号、班级、专业、年级） |
| `teachers` | 教师详细信息 |

### 课程内容

| 表名 | 说明 |
|------|------|
| `courses` | 课程信息（名称、描述、封面、教师 ID、发布状态） |
| `chapters` | 章节结构（支持层级嵌套） |
| `course_videos` | 视频资源（标题、URL、时长、排序） |
| `course_documents` | 文档资源（PDF/Word/PPT 等） |

### 学习追踪

| 表名 | 说明 |
|------|------|
| `enrollments` | 学生选课记录（status: active/completed） |
| `learning_progress` | 课程总体学习进度（completion_percentage） |
| `video_progress` | 视频观看进度（播放位置、是否完成） |
| `document_progress` | 文档阅读进度（滚动位置、阅读比例） |
| `weekly_study_time` | 每周学习时长统计 |

### 协作与作业

| 表名 | 说明 |
|------|------|
| `student_groups` | 学习小组 |
| `group_members` | 小组成员关系 |
| `teacher_assignments` | 教师发布的作业 |
| `student_submissions` | 学生作业提交 |
| `student_member_scores` | 小组内个人评分 |

### AI 功能

| 表名 | 说明 |
|------|------|
| `ai_exams` | AI 生成的考试 |
| `ai_exam_questions` | 考试题目（选择题/判断题 + 答案解析） |
| `ai_exam_attempts` | 学生考试记录（得分、用时） |
| `ai_exam_answers` | 学生逐题作答记录 |
| `conversations` | AI 对话会话 |
| `messages` | 对话消息（含 `files` 字段存储附件信息） |

### 其他

| 表名 | 说明 |
|------|------|
| `wrong_question` | 错题集 |
| `recommendation` | 课程推荐记录 |

---

## 项目结构

```
CCUT/
├── server/                          # 后端工程 (Spring Boot)
│   ├── src/main/java/com/ccut/
│   │   ├── config/                  # 配置类
│   │   │   ├── AiConfig.java            # AI 双模型配置（chat + reasoner）
│   │   │   ├── AgentToolsConfig.java    # 通用工具注册（搜索、文档生成）
│   │   │   ├── AgentDbToolsConfig.java  # 数据库查询工具注册
│   │   │   ├── AsyncConfig.java         # 异步线程池
│   │   │   ├── WebMvcConfig.java        # CORS、拦截器、静态资源
│   │   │   └── RedisConfig.java         # Redis 序列化配置
│   │   ├── controller/              # REST 控制器
│   │   ├── service/                 # 业务接口
│   │   │   └── Impl/                   # 业务实现
│   │   │       ├── ChatAgentServiceImpl.java    # AI 对话核心服务
│   │   │       ├── DocumentGeneratorServiceImpl.java  # 文档生成
│   │   │       ├── ProgressCacheServiceImpl.java      # Redis 进度缓存
│   │   │       └── AiExamService.java          # AI 考试服务
│   │   ├── mapper/                  # MyBatis 数据访问接口
│   │   ├── entity/                  # 数据库实体（Lombok @Data）
│   │   ├── dto/                     # 数据传输对象（Result<T> 统一响应）
│   │   ├── context/                 # UserContext 线程本地存储
│   │   ├── scheduled/               # 定时任务
│   │   │   ├── ProgressFlushScheduler.java     # 进度批量刷新（5 分钟）
│   │   │   └── LogCleanupScheduler.java        # 日志清理
│   │   ├── exception/               # BusinessException + GlobalExceptionHandler
│   │   └── utils/                   # JWTUtils、ReadFileUtils
│   ├── src/main/resources/
│   │   ├── mapper/*.xml             # MyBatis SQL 映射
│   │   ├── prompts/system-prompt.md # AI 系统提示词（"学小微"人设）
│   │   ├── SQL.sql                  # 数据库初始化脚本
│   │   └── application.yml          # 应用配置
│   └── pom.xml
│
├── src/                              # 前端工程 (Vue 3)
│   ├── views/
│   │   ├── student/                 # 学生端页面
│   │   │   ├── Home.vue                 # 学习首页（课程、统计）
│   │   │   ├── StudentAIChat.vue        # AI 对话（SSE 流式）
│   │   │   ├── Work.vue                 # 作业列表
│   │   │   ├── MistakeBook.vue          # 错题本
│   │   │   ├── CourseCenter.vue         # 课程中心
│   │   │   └── Data.vue                 # 学习数据看板
│   │   ├── teacher/                 # 教师端页面
│   │   │   ├── Home.vue                 # 工作台（统计、待办）
│   │   │   ├── AIChat.vue               # AI 教学助手
│   │   │   ├── Analytics.vue            # 学情分析
│   │   │   └── ...                      # 课程/学生/作业管理
│   │   └── Login.vue                # 登录页（角色识别）
│   ├── components/                  # 公共组件
│   │   ├── VideoPlayer.vue              # 视频播放器（倍速、进度记忆）
│   │   ├── DocumentViewer.vue           # 文档查看器（PDF/Word/Markdown）
│   │   └── layout/                      # 布局组件（StudentLayout/TeacherLayout）
│   ├── services/                    # API 接口封装（Axios）
│   ├── stores/                      # Pinia 状态管理
│   ├── router/                      # 路由配置（角色守卫）
│   ├── composables/                 # 组合式函数
│   └── utils/                       # 工具函数
│
├── package.json                      # 前端依赖
├── vite.config.js                    # Vite 配置（代理、别名）
└── README.md
```

---

## 快速开始

### 环境要求

| 依赖 | 版本要求 |
|------|----------|
| JDK | 17+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Redis | 7.0+ |

### 1. 克隆项目

```bash
git clone https://github.com/coustea/college-course-agent.git
cd college-course-agent
```

### 2. 数据库初始化

```bash
mysql -u root -p
```

```sql
CREATE DATABASE ccut DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE ccut;
SOURCE server/src/main/resources/SQL.sql;
```

### 3. 后端启动

```bash
cd server

# 编辑配置文件，填入数据库密码和 AI API Key
# vim src/main/resources/application.yml

mvn clean install
mvn spring-boot:run
```

后端启动在 `http://localhost:9999`。

### 4. 前端启动

```bash
# 回到项目根目录
npm install
npm run dev
```

前端启动在 `http://localhost:5173`，自动代理 `/api`、`/media`、`/uploads` 到后端。

### 5. 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 教师 | `teacher` | `123456` |
| 学生 | `student` | `123456` |

---

## 配置说明

### 后端配置 (`server/src/main/resources/application.yml`)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ccut?serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD:123456}        # 建议通过环境变量设置

  data:
    redis:
      host: localhost
      port: 6379

  ai:
    openai:
      api-key: ${AI_API_KEY:your-key}      # DeepSeek API Key
      base-url: https://api.deepseek.com

server:
  port: 9999

jwt:
  enabled: true                             # 开发环境可设为 false 跳过认证
  secret-key: ${JWT_SECRET_KEY:...}

file:
  upload-dir: /path/to/uploads              # 文件上传目录
```

### 前端配置 (`vite.config.js`)

```javascript
export default defineConfig({
  server: {
    proxy: {
      '/api': { target: 'http://localhost:9999', changeOrigin: true },
      '/media': { target: 'http://localhost:9999', changeOrigin: true },
      '/uploads': { target: 'http://localhost:9999', changeOrigin: true },
    },
  },
})
```

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `DB_PASSWORD` | MySQL 密码 | `123456` |
| `AI_API_KEY` | DeepSeek API Key | - |
| `JWT_SECRET_KEY` | JWT 签名密钥 | 内置默认值 |
| `SERP_API_KEY` | SerpAPI 搜索密钥 | - |
| `CORS_ORIGINS` | 允许的跨域来源 | `http://localhost:5173` |

---

## 性能优化

### Redis 缓存进度上报

学习进度上报是高频操作（视频进度、文档滚动位置），直接写 MySQL 会成为性能瓶颈。系统采用 **Redis Hash 缓存 + 定时批量刷新** 策略：

```
前端上报进度 → Redis Hash 写入 (<5ms)
                      ↓
         ProgressFlushScheduler (每 5 分钟)
                      ↓
         合并增量 → 批量 Upsert 至 MySQL
```

**优化效果**：

| 指标 | 优化前 | 优化后 |
|------|--------|--------|
| 单次写入延迟 | ~150ms | <5ms |
| 数据库写入频率 | 每次上报 | 每 5 分钟批量 |
| 写入量降低 | - | ~60 倍 |
| 并发能力 | ~100 QPS | 10,000+ QPS |

### Tomcat 线程池优化

```yaml
server:
  tomcat:
    threads:
      max: 100
      min-spare: 10
    accept-count: 50
```

### HikariCP 连接池

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
      connection-timeout: 30000
```

---

## API 规范

所有接口返回统一的 `Result<T>` 响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

- **成功**：`code = 200`
- **业务异常**：`code` 为对应错误码，由 `BusinessException` + `GlobalExceptionHandler` 统一处理
- **认证失败**：JWT 拦截器返回 401，前端 Axios 拦截器自动跳转登录页

### 主要 API 端点

| 模块 | 路径前缀 | 说明 |
|------|----------|------|
| 认证 | `/api/auth` | 登录、登出 |
| 课程 | `/api/course` | 课程 CRUD、发布、统计 |
| 章节 | `/api/chapter` | 章节管理 |
| 视频 | `/api/course-video` | 视频资源管理 |
| 文档 | `/api/course-document` | 文档资源管理 |
| 选课 | `/api/enrollment` | 学生选课 |
| 进度 | `/api/progress` | 学习进度上报与查询 |
| AI 对话 | `/api/chat` | AI 对话（支持 SSE 流式） |
| AI 考试 | `/api/ai-exam` | 自动出题、提交、判分 |
| 分组 | `/api/student-group` | 小组管理 |
| 作业 | `/api/assignment` | 作业发布与提交 |
| 推荐 | `/api/recommendation` | 课程推荐 |

---

## 许可证

本项目采用 [MIT License](LICENSE) 授权。
