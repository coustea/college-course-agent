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
- **MySQL 8.0**: 持久化核心业务数据（用户、课程、进度、成绩）。
- **MyBatis 3.0.4**: 轻量级 ORM 框架，支持复杂 SQL 编写。
- **Redis (Lettuce)**: 实现高频学习进度的秒级缓存与去重。
- **HikariCP**: 高性能数据库连接池。

### 辅助功能
- **Apache POI / PDFBox**: 处理 Excel 成绩导出、视频时长解析及 PDF 文档内容提取。
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

---

## 📂 项目结构

```text
server/src/main/java/com/ccut/
├── config/             # 全局配置 (Async、Redis、AI、CORS、JWT)
├── controller/         # RESTful 接口 (AI 对话、考试、进度、小组管理)
├── dto/                # Data Transfer Objects (请求与响应参数封装)
├── entity/             # 数据库实体类
├── mapper/             # MyBatis Mapper 接口 (与 resources/mapper/*.xml 对应)
├── service/            # 核心业务逻辑实现
│   ├── Impl/           # 具体实现类 (重点: AiExamServiceImpl, ProgressServiceImpl)
├── scheduled/          # 定时任务 (Redis 缓存持久化、统计任务)
├── tools/              # 系统工具
└── utils/              # 常用工具类 (JWT、Result 包装类)
```

---

## ⚙️ 关键配置 (application.yml)

### 1. 数据库配置
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ccut?serverTimezone=Asia/Shanghai...
    username: root
    password: 123456
    hikari:
      maximum-pool-size: 30  # 优化后的连接池大小
```

### 2. AI 接口配置
```yaml
spring:
  ai:
    openai:
      api-key: your-api-key
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-chat
```

### 3. Redis 缓存池
```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 20
```

---

## 🚀 启动与部署

### 1. 编译打包
```bash
# 在 server 目录下执行
mvn clean package -DskipTests
```

### 2. 运行服务
```bash
java -jar target/service-0.0.1-SNAPSHOT.jar
```

### 3. API 端点
- 默认端口: `9999`
- 接口前缀: `/api` (根据 Controller 配置)
- 示例: `POST http://localhost:9999/api/ai-exam/generate`

---

## 🛡️ 安全与优化

- **JWT 开关**: 在开发环境下可通过 `jwt.enabled: false` 绕过校验；生产环境请务必开启。
- **Tomcat 优化**: 配置文件中预设了 `threads.max: 100` 与 `accept-count: 50` 以适应中等并发。
- **日志分级**: `logging.level.com.ccut.mapper: debug` 开启 SQL 监控，便于性能调优。

---

**© CCUT 智慧教育技术组**
