# CCUT - 高等院校课程管理平台

<div align="center">

一个功能完整的高校教务管理系统，集成了现代化 AI 技术

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.5.18-brightgreen)](https://vuejs.org/)
[![Java](https://img.shields.io/badge/Java-17-orange)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

</div>

## 项目简介

CCUT（高等院校课程管理平台）是一个基于 **Spring Boot + Vue 3** 构建的现代化高校教务管理系统，集成了阿里云通义千问大模型，提供从课程管理到 AI 智能助教的全方位教务解决方案。

### 核心特性

- 双角色设计：学生端 / 教师端
- 课程管理与选课系统
- 学习进度实时追踪
- 学生分组与协作学习
- 作业下发与评分管理
- **AI 智能助教**（学小微）- 支持流式对话
- **AI 试卷生成** - 根据课程内容自动生成测试题
- **AI 智能评分** - 自动批改和评分系统
- 文件管理（支持大文件分片上传）
- 数据可视化分析

---

## 技术栈

### 后端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 编程语言 |
| Spring Boot | 3.5.5 | 应用框架 |
| MyBatis | 3.0.4 | ORM 持久层框架 |
| MySQL | 8.0.33 | 关系型数据库 |
| Redis | 5.0+ | 缓存数据库 |
| Spring AI Alibaba | 1.1.0.0-RC2 | AI 集成框架 |
| DashScope | - | 阿里云通义千问大模型 |
| Hutool | 5.8.22 | Java 工具集 |
| Apache POI | 5.2.5 | Excel 处理 |
| JJWT | 0.9.1 | JWT 认证 |

### 前端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue.js | 3.5.18 | 渐进式 JavaScript 框架 |
| Vite | 7.1.5 | 前端构建工具 |
| Element Plus | 2.11.2 | UI 组件库 |
| Vue Router | 4.5.1 | 路由管理 |
| Pinia | 3.0.3 | 状态管理 |
| ECharts | 6.0.0 | 数据可视化 |
| Chart.js | 4.5.0 | 图表库 |
| Axios | 1.11.0 | HTTP 客户端 |

---

## 项目结构

```
CCUT/
├── backend/                      # 后端项目根目录
│   ├── server/                   # Spring Boot 后端
│   │   ├── src/main/java/com/ccut/
│   │   │   ├── ServiceApplication.java  # 应用启动类
│   │   │   ├── controller/      # REST API 控制器层
│   │   │   ├── service/         # 业务逻辑层
│   │   │   ├── mapper/          # MyBatis 数据访问层
│   │   │   ├── entity/          # 数据库实体类
│   │   │   ├── dto/             # 数据传输对象
│   │   │   ├── config/          # 配置类
│   │   │   ├── exception/       # 异常处理
│   │   │   └── tools/           # 工具类
│   │   ├── src/main/resources/
│   │   │   ├── application.yml  # 主配置文件
│   │   │   ├── SQL.sql          # 数据库初始化脚本
│   │   │   └── mapper/          # MyBatis XML 映射
│   │   ├── pom.xml              # Maven 依赖配置
│   │   └── uploads/             # 文件上传目录
│   └── src/                     # Vue.js 前端
│       ├── views/               # 页面组件
│       │   ├── student/         # 学生端页面
│       │   └── Teacher/         # 教师端页面
│       ├── components/          # 公共组件
│       ├── router/              # 路由配置
│       ├── services/            # API 服务
│       ├── stores/              # Pinia 状态管理
│       ├── package.json         # 前端依赖配置
│       └── vite.config.js       # Vite 配置
└── README.md                    # 项目文档
```

---

## 功能模块

### 学生端功能

| 模块 | 功能 | 说明 |
|------|------|------|
| 首页 | 课程概览 | 展示已选课程、学习进度、最新作业 |
| 学习数据 | 数据统计 | 学习时长、完成度、可视化图表展示 |
| 学习小组 | 分组协作 | 创建小组、加入小组、成员管理 |
| 作品/作业 | 作业提交 | 查看作业、小组提交、查看评分 |
| 个人中心 | 信息管理 | 查看和编辑个人信息 |

### 教师端功能

| 模块 | 功能 | 说明 |
|------|------|------|
| 教师工作台 | 数据概览 | 课程统计、学生统计、快捷操作 |
| 课程管理 | 课程CRUD | 创建课程、上传资源、进度管理、成绩管理 |
| 作品检查 | 作业评分 | 下发作业、查看提交、成员评分、添加评语 |
| 学生管理 | 学生信息 | 学生列表、分组管理、学习表现分析 |
| 教学分析 | 数据分析 | 课程数据分析、教学效果评估 |
| AI 助手 | 智能对话 | 学小微智能对话、流式响应、会话管理 |
| 个人中心 | 信息管理 | 查看和编辑个人信息 |

### AI 功能特性

- **AI 智能对话**："学小微"智能助教，支持多轮对话和上下文记忆
- **AI 试卷生成**：根据课程内容自动生成选择/判断题
- **AI 智能评分**：自动批改试卷并给出得分和解析
- **流式响应**：基于 Server-Sent Events 的实时流式输出

---

## 快速开始

### 环境要求

- **JDK**: 17+
- **Maven**: 3.x
- **Node.js**: 16+
- **MySQL**: 8.0+
- **Redis**: 5.0+

### 安装步骤

#### 1. 克隆项目

```bash
git clone https://github.com/coustea/college-course-agent.git
cd CCUT/backend
```

#### 2. 数据库初始化

创建数据库并导入初始化脚本：

```bash
mysql -u root -p
CREATE DATABASE ccut CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ccut;
SOURCE server/src/main/resources/SQL.sql;
```

#### 3. 后端配置

编辑 `server/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ccut
    username: root
    password: your_password

  data:
    redis:
      host: localhost
      port: 6379

  ai:
    dashscope:
      api-key: your_dashscope_api_key

file:
  upload-dir: /path/to/upload/directory

jwt:
  enabled: false  # 开发环境设为 false
```

#### 4. 启动后端服务

```bash
cd server
mvn clean install
mvn spring-boot:run
```

后端服务将在 http://localhost:9999 启动

#### 5. 安装前端依赖

```bash
cd ../src
npm install
```

#### 6. 配置前端 API 地址

编辑 `src/main.js`，确认后端 API 地址：

```javascript
app.config.globalProperties.$baseUrl = 'http://localhost:9999/api'
```

#### 7. 启动前端服务

```bash
npm run dev
```

前端应用将在 http://localhost:5173 启动

#### 8. 访问应用

打开浏览器访问 http://localhost:5173

**测试账号**：
- 学生：`2021001` / `123456`
- 教师：`T001` / `123456`

---

## API 接口文档

### 基础信息

- **Base URL**: `http://localhost:9999/api`
- **认证方式**: JWT Token（Header: `Authorization: Bearer <token>`）
- **响应格式**: JSON

### 主要接口

#### 认证接口

```http
# 用户登录
POST /api/auth/login
Content-Type: application/json

{
  "username": "2021001",
  "password": "123456"
}

# 用户登出
POST /api/auth/logout
```

#### 课程管理接口

```http
# 获取课程列表
GET /api/course/list

# 获取课程详情
GET /api/course/{id}

# 学生选课
POST /api/course/enroll
```

#### AI 功能接口

```http
# 同步聊天
POST /api/ai/chat/send

# 流式聊天（SSE）
POST /api/ai/chat/stream

# 创建会话
POST /api/ai/conversation/create

# 获取会话列表
GET /api/ai/conversation/list?username=user

# 生成试卷
POST /api/ai/exam/generate

# 提交答卷
POST /api/ai/exam/submit
```

#### 学习进度接口

```http
# 获取视频进度
GET /api/progress/video?courseId=1

# 获取文档进度
GET /api/progress/document?courseId=1

# 获取学习概览
GET /api/progress/overview?courseId=1
```

#### 分组管理接口

```http
# 创建小组
POST /api/group/create

# 我的小组
GET /api/group/my-groups

# 加入小组
POST /api/group/join

# 审核成员
PUT /api/group/approve
```

#### 文件上传接口

```http
# 上传视频
POST /api/upload/video

# 上传文档
POST /api/upload/document

# 分片上传
POST /api/upload/chunk
```

---

## 数据库设计

### 核心表结构

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| users | 用户基础信息 | id, username, password, role, token |
| students | 学生详细信息 | id, student_number, name, class_name, major, grade |
| teachers | 教师详细信息 | id, name, department, title, position |
| courses | 课程基本信息 | course_id, course_code, course_name, teacher_id |
| enrollments | 选课记录 | enrollment_id, student_id, course_id, status |
| course_videos | 课程视频 | video_id, course_id, video_url, duration |
| course_documents | 课程文档 | document_id, course_id, document_url |
| learning_progress | 学习进度汇总 | progress_id, student_id, course_id, completion_percentage |
| video_progress | 视频观看进度 | id, student_id, video_id, watched_seconds |
| document_progress | 文档阅读进度 | id, student_id, document_id, max_scroll_pct |
| student_groups | 学生分组 | group_id, group_name, group_leader_id, status |
| group_members | 小组成员 | id, group_id, student_id, role, join_status |
| teacher_assignments | 教师作业 | assignment_id, course_id, assignment_name, due_date |
| student_submissions | 学生提交 | submission_id, assignment_id, group_id, submission_files |
| student_member_scores | 个人得分 | id, submission_id, student_id, score, feedback |
| conversations | AI 对话会话 | id, conversation_id, username, title |
| messages | AI 聊天消息 | id, conversation_id, role, content, sequence_num |
| ai_exams | AI 生成试卷 | id, course_id, topic, question_count, total_score |
| ai_exam_questions | 试卷题目 | id, exam_id, type, content, options, answer |
| ai_exam_attempts | 试卷提交记录 | id, exam_id, student_id, score |
| ai_exam_answers | 答题记录 | id, attempt_id, question_id, student_answer, correct |

---

## 配置说明

### 后端配置（application.yml）

```yaml
# 服务器配置
server:
  port: 9999

# 数据库配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ccut
    username: root
    password: 123456
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10

  # Redis 配置
  data:
    redis:
      host: localhost
      port: 6379
      lettuce:
        pool:
          max-active: 20
          max-idle: 10

  # 文件上传配置
  servlet:
    multipart:
      max-file-size: 1024MB
      max-request-size: 1024MB

  # AI 服务配置
  ai:
    dashscope:
      api-key: your_dashscope_api_key

# JWT 认证开关
jwt:
  enabled: false

# 文件上传目录
file:
  upload-dir: /path/to/upload/directory
```

### 前端配置（main.js）

```javascript
// 后端 API 地址
app.config.globalProperties.$baseUrl = 'http://localhost:9999/api'
```

---

## 开发指南

### 后端开发规范

1. **Controller 层**：处理 HTTP 请求，参数验证，调用 Service 层
2. **Service 层**：业务逻辑处理，事务管理
3. **Mapper 层**：数据库操作，SQL 执行
4. **Entity 层**：数据库实体映射
5. **DTO 层**：数据传输对象

异常处理：
```java
throw new BusinessException(ErrorCode.USER_NOT_FOUND);
```

统一响应：
```java
return Result.success(data);
return Result.error("操作失败");
```

### 前端开发规范

1. **组件命名**：使用 PascalCase（如 `UserProfile.vue`）
2. **文件组织**：按功能模块组织代码
3. **API 调用**：统一使用 `services/` 下的封装方法
4. **状态管理**：使用 Pinia 管理全局状态

使用 Composition API：
```vue
<script setup>
import { ref, computed } from 'vue'

const count = ref(0)
const doubled = computed(() => count.value * 2)
</script>
```

---

## 部署指南

### 后端部署

```bash
cd server
mvn clean package
java -jar target/service-0.0.1-SNAPSHOT.jar
```

### 前端部署

```bash
cd src
npm run build
```

将 `dist/` 目录部署到 Nginx：

```nginx
server {
    listen 80;
    server_name your-domain.com;
    root /path/to/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:9999/api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### Docker 部署（可选）

后端 Dockerfile：
```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 9999
ENTRYPOINT ["java", "-jar", "app.jar"]
```

前端 Dockerfile：
```dockerfile
FROM node:16-alpine as builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

---

## 测试

### 运行后端测试

```bash
cd server
mvn test
```

### API 接口测试

项目提供了 API 测试脚本：

```bash
# 快速测试
./test_api.sh

# 完整测试
./test_api_full.sh
```

---

## 常见问题

### Q: 启动时报数据库连接错误？
**A**: 检查 MySQL 服务是否启动，配置文件中的数据库连接信息是否正确。

### Q: Redis 连接失败？
**A**: 确保 Redis 服务正在运行，检查 Redis 配置是否正确。

### Q: 文件上传失败？
**A**: 检查文件上传目录是否存在，是否有写入权限，文件大小是否超限。

### Q: AI 功能无法使用？
**A**: 检查 DashScope API Key 是否配置正确，是否有足够的 API 调用额度。

### Q: 前端路由刷新 404？
**A**: 确保 Nginx 配置了 `try_files $uri $uri/ /index.html;` 支持 Vue Router history 模式。

---

## 项目截图

### 学生端
- 首页课程列表
- 学习数据可视化
- 小组协作管理
- 作业提交与查看

### 教师端
- 教师工作台
- 课程资源管理
- 作业评分界面
- AI 智能助手

---

## 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

### 代码规范

- 后端遵循阿里巴巴 Java 开发手册
- 前端遵循 Vue 风格指南
- 提交前确保代码通过 ESLint 检查

---

## 版本历史

- **v1.0.0** (2024-01)
  - 初始版本发布
  - 完成学生端和教师端核心功能
  - 集成 AI 智能助教功能
  - 实现试卷生成和自动评分

---

## 许可证

本项目采用 [MIT License](LICENSE) 开源协议。

---

## 致谢

感谢以下开源项目：

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Vue.js](https://vuejs.org/)
- [Element Plus](https://element-plus.org/)
- [MyBatis](https://mybatis.org/)
- [ECharts](https://echarts.apache.org/)

---

## 联系方式

- 项目地址：[GitHub Repository]
- 问题反馈：[Issues]
- 邮箱：[maintainer@example.com]

---

<div align="center">

**如果这个项目对你有帮助，请给一个 ⭐️ Star 支持一下！**

Made with ❤️ by CCUT Team

</div>
