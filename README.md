# CCUT Backend Service

高等院校课程管理平台后端服务，基于 Spring Boot 构建。提供课程管理、学生分组、作业提交、AI 智能助教等完整的教务管理功能，集成阿里云 DashScope AI 能力。

## 目录

- [技术栈](#技术栈)
- [核心功能](#核心功能)
- [项目架构](#项目架构)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [API 文档](#api-文档)
- [数据库设计](#数据库设计)
- [AI 会话管理系统](#ai-会话管理系统)

## 技术栈

### 后端框架
- **Java**: 17
- **Spring Boot**: 3.5.5
- **MyBatis**: 3.0.4 (ORM 框架)

### 数据存储
- **MySQL**: 8.0.33 (关系型数据库)
- **Redis**: 缓存层（Spring Data Redis + Lettuce 连接池）

### AI 集成
- **Spring AI Alibaba**: 1.1.0.0-RC2
- **DashScope**: 阿里云通义千问 (qwen-max)
- **ReactAgent**: 支持工具调用的 AI 代理

### 工具库
- **Hutool**: 5.8.22 (Java 工具集)
- **Apache POI**: 5.2.5 (Excel 处理)
- **MP4Parser**: 1.9.41 (视频时长解析)
- **JJWT**: 0.9.1 (JWT 认证)

### 构建工具
- **Maven**: 3.x

## 核心功能

### 1. 用户管理
- 学生/教师注册登录
- JWT 认证机制
- 角色权限管理

### 2. 课程管理
- 课程创建与发布
- 课程视频管理
- 课程文档管理
- 学生选课功能

### 3. 学习进度追踪
- 视频观看进度
- 文档阅读进度
- 学习时长统计
- 完成度计算

### 4. 学生分组
- 小组创建与管理
- 成员审核机制
- 组长分配
- 小组状态管理

### 5. 作业系统
- 教师发布作业
- 小组作业提交
- 教师评分与反馈
- 学生个人成绩记录

### 6. AI 智能助教
- **AI 试卷生成**: 基于课程内容自动生成测试题
- **AI 聊天助手**: 智能对话"学小微"，支持：
  - 多轮对话上下文记忆
  - 联网搜索实时信息
  - 会话管理与持久化
  - 流式响应支持

### 7. 文件管理
- 文件上传（支持大文件分片上传）
- 视频处理
- 文档管理

## 项目架构

```
backend/
├── src/main/java/com/ccut/
│   ├── controller/          # 控制器层（API 接口）
│   │   ├── AuthController.java           # 认证接口
│   │   ├── UserController.java           # 用户管理
│   │   ├── CourseController.java         # 课程管理
│   │   ├── StudentController.java        # 学生管理
│   │   ├── TeacherController.java        # 教师管理
│   │   ├── StudentGroupController.java   # 学生分组
│   │   ├── SubmissionController.java     # 作业提交
│   │   ├── AiController.java             # AI 试卷生成
│   │   ├── ConversationController.java   # AI 会话管理
│   │   ├── ChatController.java           # AI 聊天接口
│   │   └── ...                           # 其他控制器
│   ├── service/            # 服务层（业务逻辑）
│   │   ├── ConversationService.java      # 会话服务
│   │   ├── MessageService.java           # 消息服务
│   │   ├── ChatService.java              # 聊天服务
│   │   └── impl/                         # 服务实现
│   ├── mapper/             # 数据访问层
│   │   ├── ConversationMapper.java       # 会话 Mapper
│   │   ├── MessageMapper.java            # 消息 Mapper
│   │   └── ...                           # 其他 Mapper
│   ├── entity/             # 实体类
│   │   ├── Conversation.java             # 会话实体
│   │   ├── Message.java                  # 消息实体
│   │   └── ...                           # 其他实体
│   ├── dto/                # 数据传输对象
│   │   ├── ChatRequest.java              # 聊天请求
│   │   ├── ChatResponse.java             # 聊天响应
│   │   └── Result.java                   # 统一响应格式
│   ├── config/             # 配置类
│   │   ├── RedisConfig.java              # Redis 配置
│   │   ├── AsyncConfig.java              # 异步线程池配置
│   │   └── JwtInterceptor.java           # JWT 拦截器
│   └── tools/              # AI 工具
│       └── SearchTool.java               # 联网搜索工具
├── src/main/resources/
│   ├── application.yml     # 主配置文件
│   ├── SQL.sql             # 数据库初始化脚本
│   └── mapper/             # MyBatis XML 映射
│       ├── MessageMapper.xml
│       ├── ConversationMapper.xml
│       └── ...
└── pom.xml                # Maven 依赖配置
```

## 环境要求

- **JDK**: 17+
- **Maven**: 3.x
- **MySQL**: 8.0+
- **Redis**: 5.0+ (用于会话缓存)

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/coustea/college-course-agent.git
cd backend
```

### 2. 安装依赖

```bash
mvn clean install
```

### 3. 初始化数据库

创建数据库并执行初始化脚本：

```sql
CREATE DATABASE ccut CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ccut;

-- 执行 src/main/resources/SQL.sql 中的所有 SQL 语句
SOURCE /path/to/SQL.sql;
```

### 4. 配置应用

编辑 `src/main/resources/application.yml`，配置数据库、Redis 等信息（详见[配置说明](#配置说明)）。

### 5. 启动 Redis

```bash
# Linux/Mac
redis-server

# Windows
redis-server.exe
```

### 6. 运行应用

```bash
mvn spring-boot:run
```

或运行打包后的 JAR：

```bash
java -jar target/service-0.0.1-SNAPSHOT.jar
```

### 7. 访问应用

应用将在 `http://localhost:9999` 启动。

## 配置说明

### 数据库配置

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/ccut?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8
    username: root
    password: your_password  # 修改为你的 MySQL 密码
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

### Redis 配置

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 20
          max-wait: -1ms
          max-idle: 10
          min-idle: 5
```

### AI 服务配置

```yaml
spring:
  ai:
    dashscope:
      api-key: your-dashscope-api-key  # 替换为你的阿里云 DashScope API Key
```

### 服务器配置

```yaml
server:
  port: 9999  # 应用端口
  tomcat:
    threads:
      max: 100
      min-spare: 10
```

### JWT 认证配置

```yaml
jwt:
  enabled: false  # 生产环境设为 true，开发环境可设为 false
```

### 文件上传配置

```yaml
file:
  upload-dir: /path/to/upload/directory  # 修改为你的上传目录路径
```

## API 文档

### 统一响应格式

所有 API 返回统一的 JSON 格式：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 认证接口

#### 用户登录
```
POST /api/auth/login
Content-Type: application/json

{
  "username": "2021001",
  "password": "123456"
}
```

### 课程管理接口

#### 获取所有课程
```
GET /api/course/list
```

#### 学生选课
```
POST /api/course/enroll
{
  "studentId": 1,
  "courseId": 1
}
```

### AI 会话管理接口

#### 创建新会话
```
POST /api/ai/conversation/create
Content-Type: application/json

{
  "username": "student1",
  "title": "关于Java的问题"
}

Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "conversationId": "student1:1",
    "title": "关于Java的问题",
    "sequenceNum": 1,
    "createdAt": "2025-01-26T10:00:00"
  }
}
```

#### 获取用户会话列表
```
GET /api/ai/conversation/list/{username}

Response:
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "conversationId": "student1:1",
      "title": "关于Java的问题",
      "username": "student1",
      "sequenceNum": 1,
      "createdAt": "2025-01-26T10:00:00",
      "updatedAt": "2025-01-26T10:30:00"
    }
  ]
}
```

#### 获取会话历史消息
```
GET /api/ai/conversation/{conversationId}/messages

Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "conversationId": "student1:1",
    "title": "关于Java的问题",
    "messages": [
      {
        "id": 1,
        "role": "user",
        "content": "什么是Java？",
        "sequenceNum": 1,
        "createdAt": "2025-01-26T10:00:00"
      },
      {
        "id": 2,
        "role": "assistant",
        "content": "Java是一门...",
        "sequenceNum": 2,
        "createdAt": "2025-01-26T10:00:05"
      }
    ]
  }
}
```

#### 删除会话
```
DELETE /api/ai/conversation/{conversationId}
```

#### 更新会话标题
```
PUT /api/ai/conversation/{conversationId}/title
Content-Type: application/json

{
  "title": "新标题"
}
```

### AI 聊天接口

#### 同步聊天
```
POST /api/ai/chat/send
Content-Type: application/json

{
  "conversationId": "student1:1",
  "message": "Spring Boot怎么用？"
}

Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "conversationId": "student1:1",
    "userMessage": {
      "id": 10,
      "role": "user",
      "content": "Spring Boot怎么用？",
      "sequenceNum": 10,
      "createdAt": "2025-01-26T11:00:00"
    },
    "aiMessage": {
      "id": 11,
      "role": "assistant",
      "content": "Spring Boot 是...",
      "sequenceNum": 11,
      "createdAt": "2025-01-26T11:00:02"
    }
  }
}
```

#### 流式聊天（SSE）
```
POST /api/ai/chat/stream
Content-Type: application/json

{
  "conversationId": "student1:1",
  "message": "解释一下多线程"
}

Response (Server-Sent Events):
data: Spring
data:  Boot
data:  中
data: 的
...
```

**限制说明**:
- 单个会话最多支持 50 轮对话（100 条消息）
- 超过限制后需要创建新会话

## 数据库设计

### 核心表结构

#### 用户相关表
- `users`: 用户基础信息
- `students`: 学生详细信息
- `teachers`: 教师详细信息

#### 课程相关表
- `courses`: 课程信息
- `enrollments`: 选课记录
- `course_videos`: 课程视频
- `course_documents`: 课程文档

#### 学习进度表
- `learning_progress`: 学习进度汇总
- `video_progress`: 视频观看进度
- `document_progress`: 文档阅读进度

#### 分组与作业表
- `student_groups`: 学生分组
- `group_members`: 小组成员
- `teacher_assignments`: 教师发布作业
- `student_submissions`: 学生作业提交
- `student_member_scores`: 学生个人得分

#### AI 相关表
- `ai_exams`: AI 生成试卷
- `ai_exam_questions`: 试卷题目
- `ai_exam_attempts`: 试卷提交记录
- `ai_exam_answers`: 学生答题记录
- `conversations`: AI 对话会话表
- `messages`: AI 聊天消息表

完整的数据库表结构请参考 `src/main/resources/SQL.sql`。

## AI 会话管理系统

### 系统架构

AI 会话管理系统采用 **Redis 缓存 + MySQL 持久化** 双层架构：

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
┌──────▼──────────┐
│ ChatController  │
└──────┬──────────┘
       │
┌──────▼──────────┐     ┌─────────────┐
│  ChatService    │────▶│  ReactAgent │
│                 │     │  (DashScope)│
└──────┬──────────┘     └─────────────┘
       │
┌──────▼──────────┐     ┌──────────────┐
│ MessageService  │────▶│ Redis Cache  │
│                 │     │ (24h TTL)    │
└──────┬──────────┘     └──────────────┘
       │
┌──────▼──────────┐     ┌──────────────┐
│ MessageMapper   │────▶│   MySQL DB   │
└─────────────────┘     └──────────────┘
```

### 核心特性

#### 1. 会话隔离
- 每个 conversation 对应用户的一个独立对话会话
- 会话 ID 格式：`username:序号`（如 `zhangsan:1`）
- 支持多会话并行管理

#### 2. 消息持久化
- 所有消息实时保存到 MySQL（保证数据不丢失）
- 消息包含：role（user/assistant）、content、sequence_num、created_at

#### 3. 智能缓存策略
- **缓存格式**: Redis List，key 为 `chat:messages:{conversationId}`
- **加载策略**: Cache Aside（懒加载）
  - 先查 Redis，未命中则从 MySQL 加载
  - 加载后异步回填到 Redis
- **更新策略**: Write-Through 异步
  - 消息保存同步写 MySQL
  - 缓存更新异步执行（不阻塞响应）
- **TTL**: 24 小时

#### 4. 多轮对话上下文
- 自动加载历史消息（最多 20 条）作为 AI 上下文
- 支持 ReactAgent 工具调用（联网搜索）
- 流式响应支持（SSE）

#### 5. 异步处理
- 消息保存：同步写 MySQL，异步更新 Redis
- 缓存刷新：异步执行，使用独立线程池
- 线程池隔离：
  - `chatExecutor`: 聊天异步任务（core: 10, max: 20）
  - `cacheExecutor`: 缓存异步任务（core: 5, max: 10）

### 缓存 Key 设计

| Key 格式 | 类型 | TTL | 说明 |
|----------|------|-----|------|
| `chat:messages:{conversationId}` | List | 24h | 会话消息列表 |
| `chat:conversation:{conversationId}` | Hash | 24h | 会话元数据 |

### 数据一致性保障

1. **MySQL 为主**: 所有消息先写入 MySQL，确保持久化
2. **Redis 为辅**: 作为缓存层，提升读取性能
3. **异步更新**: 缓存更新失败不影响主流程
4. **最终一致性**: 对话结束后完整刷新缓存

### 性能优化

- ✅ Redis 缓存热点会话，减少 MySQL 查询
- ✅ 异步缓存刷新，不阻塞用户请求
- ✅ 线程池隔离，避免资源竞争
- ✅ 连接池管理（Hikari + Lettuce）

## 开发指南

### 添加新的 API

1. 在 `controller` 包创建 Controller 类
2. 在 `service` 包创建 Service 接口和实现
3. 在 `mapper` 包创建 Mapper 接口
4. 在 `resources/mapper` 创建 MyBatis XML 映射
5. 在 `entity` 包创建实体类

### 代码规范

- 遵循阿里巴巴 Java 开发手册
- 使用 Lombok 简化实体类
- 统一异常处理
- 统一日志格式（SLF4J + Logback）

## 常见问题

### Q: 如何修改 JWT 认证状态？

A: 在 `application.yml` 中修改 `jwt.enabled`:
```yaml
jwt:
  enabled: true  # 生产环境
  enabled: false # 开发环境
```

### Q: Redis 连接失败怎么办？

A: 检查以下几点：
1. Redis 服务是否启动
2. `application.yml` 中的 Redis 配置是否正确
3. 防火墙是否开放 6379 端口

### Q: 如何更换 AI 模型？

A: 在 `ChatServiceImpl.java` 中修改 `model` 参数：
```java
.model("qwen-max")  // 可改为 qwen-plus, qwen-turbo 等
```

### Q: 如何调整会话消息上限？

A: 在 `ChatController.java` 中修改验证逻辑：
```java
if (messageCount >= 100) {  // 修改这个值
    return Result.error(400, "当前会话已达到最大对话轮数");
}
```

## 许可证

[待添加]

## 联系方式

- 项目地址: [GitHub](https://github.com/coustea/college-course-agent)
- 问题反馈: [Issues](https://github.com/coustea/college-course-agent/issues)
