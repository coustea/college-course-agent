# 🚀 CCUT 智慧学习系统 - 后端服务 (Backend Server)

> **基于 Spring Boot 3.5.5 的高性能 AI 教育引擎** —— 集成大语言模型、Redis 缓存优化与异步任务处理。

---

## 🛠️ 技术架构 (Tech Stack)

### 核心框架
- **Spring Boot 3.5.5**: 提供基础的 IoC、AOP 及 Web 服务支持。
- **Java 17**: 利用其高性能、密封类及强化的类型推断特性。

### AI 与 智能化
- **Spring AI**: 统一集成 DeepSeek/OpenAI 接口。
- **DeepSeek-Chat**: 负责智能出题（选择、判断题）及学习答疑。
- **BeanOutputConverter**: 实现 AI 响应的结构化解析，确保 JSON 输出与 Java 实体无缝对接。

### 存储与缓存
- **MySQL 8.0.33**: 持久化核心业务数据（用户、课程、进度、成绩）。
- **MyBatis 3.0.4**: 轻量级 ORM 框架，支持复杂 SQL 编写。
- **Redis (Lettuce)**: 实现高频学习进度的秒级缓存与去重。
- **HikariCP**: 高性能数据库连接池。

### 辅助功能
- **Apache POI**: 处理 Excel 成绩导出、Word 文档内容提取。
- **Apache Tika**: 统一文档解析框架，支持 PDF、PPT、DOC 等多种格式。
- **MP4Parser**: 解析视频文件时长等元数据。
- **JJWT**: 基于 Token 的轻量级身份认证机制。
- **Hutool**: 丰富的 Java 工具集，简化文件操作与加密。

---

## 🎯 核心模块解析

### 1. AI 考试生成 (AiExamService)
- **原理**: 通过定制化的 Prompt 模板，将课程名称、出题数量及格式规范发送至 DeepSeek 引擎。
- **流程**: AI 生成题目 → `BeanOutputConverter` 解析为 Java 对象 → 批量存入 `ai_exam_questions` 表。
- **闭环**: 学生提交后，系统自动对比答案、计算得分，并同步触发 `WrongQuestionService` 将错题存入错题本。

### 2. 极致性能进度追踪 (ProgressCacheService)
- **优化点**: 针对每 10~30 秒一次的进度上报，使用 Redis Hash 进行缓存。
- **策略**: `ProgressFlushScheduler` 定时任务（每 5 分钟）从 Redis 批量读取数据，执行 `Upsert` 批量更新 MySQL，将数据库 IO 降低 60 倍以上。

### 3. 分段文件上传 (ChunkUpload)
- **支持**: 针对大型视频课程，支持分片上传、断点续传及文件合并，解决网络波动导致的上传失败问题。

### 4. 智能文档解析 (DocumentAnalysisService)
- **功能**: 支持多种文档格式（PDF、Word、PPT、Excel等）的文本内容提取。
- **应用**: 用于AI问答系统的知识库构建，提升学习体验。

---

## 📂 项目结构

```text
server/src/main/java/com/ccut/
├── config/             # 全局配置 (Async、Redis、AI、CORS、JWT)
├── controller/         # RESTful 接口 (AI 对话、考试、进度、小组管理)
├── dto/                # Data Transfer Objects (请求与响应参数封装)
├── entity/             # 数据库实体类
├── exception/          # 自定义异常处理
├── mapper/             # MyBatis Mapper 接口 (与 resources/mapper/*.xml 对应)
├── scheduled/          # 定时任务 (Redis 缓存持久化、统计任务)
├── service/            # 核心业务逻辑实现
│   ├── Impl/           # 具体实现类 (重点: AiExamServiceImpl, ProgressServiceImpl)
├── tools/              # 系统工具
├── utils/              # 常用工具类 (JWT、Result 包装类)
└── ServiceApplication.java # Spring Boot 启动类
```

Additional Resources:
```text
server/src/main/resources/
├── application.yml     # 应用配置文件
├── logback-spring.xml  # 日志配置
├── mapper/             # MyBatis XML 映射文件
├── prompts/            # AI Prompt 模板
├── sql/                # 数据库初始化脚本
├── static/             # 静态资源
└── upload/             # 文件上传目录
```

---

## 🚀 快速开始

### 环境要求
- **JDK 17+**: 推荐使用 OpenJDK 或 Oracle JDK 17 及以上版本
- **Maven 3.6+**: 项目构建工具
- **MySQL 8.0+**: 数据库存储
- **Redis 7.0+**: 缓存服务
- **DeepSeek/OpenAI API Key**: AI 功能所需

### 本地开发环境搭建

1. **克隆项目**
```bash
git clone <repository-url>
cd CCUT/backend/server
```

2. **配置数据库**
   - 创建名为 `ccut` 的 MySQL 数据库
   - 执行 `src/main/resources/sql/SQL.sql` 初始化表结构

3. **配置 application.yml**
   - 设置正确的数据库连接信息
   - 配置 Redis 连接（默认 localhost:6379）
   - 添加 DeepSeek 或 OpenAI API Key

4. **编译运行**
```bash
# 使用 Maven 直接运行（推荐开发阶段）
mvn spring-boot:run

# 或者打包后运行
mvn clean package -DskipTests
java -jar target/service-0.0.1-SNAPSHOT.jar
```

---

## ⚙️ 配置说明 (application.yml)

### 1. 数据库配置
```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/ccut?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&allowMultiQueries=true&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    hikari:
      maximum-pool-size: 30 # 最大连接数
      minimum-idle: 10 # 最小空闲连接数
      connection-timeout: 30000 # 获取连接超时时间（30秒）
      idle-timeout: 600000 # 空闲连接超时（10分钟）
      max-lifetime: 1800000 # 连接最大生命周期（30分钟）
      leak-detection-threshold: 60000 # 连接泄漏检测（1分钟）
```

### 2. AI 接口配置
```yaml
spring:
  ai:
    openai:
      api-key: sk-eb6f314b76cf4570a81f4e35df8b9874  # 替换为真实的 API Key
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-chat
          temperature: 0.7
```

### 3. Redis 缓存配置
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

### 4. 服务器配置
```yaml
server:
  port: 9999
  tomcat:
    threads:
      max: 100 # 最大处理线程数
      min-spare: 10 # 最小空闲线程数
    accept-count: 50 # 请求排队上限
    connection-timeout: 20000 # 连接超时时间20秒
```

---

## 🌐 API 端点

### 主要接口列表
- **认证**: `POST /api/auth/login` - 用户登录
- **课程**: `GET /api/courses` - 获取所有课程
- **AI 考试**: `POST /api/ai-exam/generate` - AI生成考试题目
- **AI 对话**: `POST /api/ai/chat` - AI学习助手对话
- **学习进度**: `POST /api/progress/update` - 更新学习进度
- **错题本**: `GET /api/wrong-question/list` - 获取错题列表
- **分组**: `POST /api/group/create` - 创建学习小组
- **文件上传**: `POST /api/upload/chunk` - 分片文件上传

### 默认账号
- 教师：`teacher` / `123456`
- 学生：`student` / `123456`

---

## 🛡️ 安全与优化

- **JWT 开关**: 在开发环境下可通过 `jwt.enabled: false` 绕过校验；生产环境请务必开启。
- **Tomcat 优化**: 配置文件中预设了 `threads.max: 100` 与 `accept-count: 50` 以适应中等并发。
- **日志分级**: `logging.level.com.ccut.mapper: debug` 开启 SQL 监控，便于性能调优。
- **文件上传限制**: 单个文件最大支持 1024MB (`multipart.max-file-size`)。

---

## 🧪 测试

### 运行单元测试
```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=YourTestClass
```

---

## 🔧 部署

### 生产环境部署
```bash
# 1. 打包应用
mvn clean package -DskipTests

# 2. 启动服务
java -jar target/service-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Docker 部署（可选）
```dockerfile
FROM openjdk:17-jdk-slim

COPY target/service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 9999

ENTRYPOINT ["java", "-jar", "/app.jar"]
```

---

**© CCUT 智慧教育技术组**
