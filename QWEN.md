# CCUT Backend 项目上下文

## 项目概述

**CCUT 智慧课程在线学习系统** 是一个基于 AI 驱动的高性能在线教育平台，融合 Spring Boot 3.5、Vue 3 与大语言模型（DeepSeek/OpenAI），提供从视频学习、文档阅读到智能测试的完整闭环。

### 核心特性
- 🤖 **AI 深度集成**: 智能对话、自动出题、即时判分
- ⚡ **极致性能**: Redis 缓存 + 定时批量刷新，学习时长上报性能提升 40 倍
- 📝 **智能错题本**: 全自动收集错题，支持智能组卷
- 📊 **可视化学习洞察**: ECharts 展现学生学习趋势
- 🤝 **协作式学习**: 完善的小组管理机制

---

## 技术架构

### 后端 (Spring Boot)
| 技术 | 版本/用途 |
|------|-----------|
| **框架** | Spring Boot 3.5.5 |
| **语言** | Java 17 |
| **AI 引擎** | Spring AI (DeepSeek/OpenAI) |
| **持久层** | MyBatis 3.0.4 + MySQL 8.0.33 |
| **缓存** | Redis (Lettuce) + 异步线程池 |
| **认证** | JWT |
| **文件处理** | Apache POI (Excel), PDFBox (PDF) |

### 前端 (Vue 3)
| 技术 | 版本/用途 |
|------|-----------|
| **框架** | Vue 3.5.18 (Composition API) |
| **构建** | Vite 7.1.5 |
| **UI** | Element Plus |
| **状态管理** | Pinia |
| **图表** | ECharts, Chart.js |
| **文档** | PDF.js, Mammoth (.docx) |
| **公式** | KaTeX |

---

## 目录结构

```
CCUT/backend/
├── server/                     # 后端工程 (Spring Boot)
│   ├── src/main/java/com/ccut/
│   │   ├── config/             # 配置类 (Redis, Async, AI, JWT, CORS)
│   │   ├── controller/         # RESTful API 控制器
│   │   ├── service/            # 业务逻辑层 (含 AI 及 Redis 缓存实现)
│   │   ├── scheduled/          # 定时任务 (进度批量同步)
│   │   ├── mapper/             # MyBatis Mapper 接口
│   │   ├── entity/             # 数据库实体
│   │   ├── dto/                # 数据传输对象
│   │   └── utils/              # 工具类 (JWT, Result 等)
│   ├── src/main/resources/
│   │   ├── mapper/*.xml        # MyBatis SQL 映射
│   │   ├── prompts/            # AI Prompt 模板
│   │   ├── sql/                # 数据库脚本
│   │   └── application.yml     # 应用配置
│   ├── pom.xml                 # Maven 依赖
│   └── uploads/                # 上传文件存储
│
├── src/                        # 前端工程 (Vue 3)
│   ├── views/
│   │   ├── student/            # 学生端视图
│   │   ├── Teacher/            # 教师端视图
│   │   └── Login.vue           # 登录页
│   ├── components/             # 公共组件
│   ├── services/               # API 接口封装
│   ├── stores/                 # Pinia 状态管理
│   ├── router/                 # Vue Router 配置
│   └── main.js                 # 入口文件
│
├── package.json                # 前端依赖配置
├── vite.config.js              # Vite 构建配置
└── index.html                  # HTML 入口
```

---

## 构建与运行

### 环境要求
- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Redis 7.0+
- DeepSeek/OpenAI API Key

### 后端启动
```bash
cd server
# 编辑 application.yml 配置数据库、Redis 及 AI Key
mvn clean package -DskipTests
java -jar target/service-0.0.1-SNAPSHOT.jar
# 或
mvn spring-boot:run
```
- 默认端口：`9999`

### 前端启动
```bash
npm install
npm run dev
```
- 访问地址：`http://localhost:5173`
- API 代理：`/api` → `http://localhost:9999`

### 默认账号
- 教师：`teacher` / `123456`
- 学生：`student` / `123456`

---

## 核心模块说明

### 1. AI 考试生成 (AiExamService)
- 通过 Prompt 模板将课程信息发送至 DeepSeek
- `BeanOutputConverter` 解析 AI 响应为结构化 JSON
- 自动生成选择题、判断题并存储
- 学生提交后自动判分，错题同步存入错题本

### 2. 学习进度追踪 (ProgressCacheService)
**Redis 缓存优化方案**（详见 `server/REDIS_OPTIMIZATION_GUIDE.md`）:
- 前端上报 → Redis Hash 写入 (<5ms) → 定时任务 (每 5 分钟) → 批量 Upsert MySQL
- 数据库写入频率降低 **60 倍**
- 并发能力提升 **100 倍**

### 3. 分段文件上传
- 支持大型视频分片上传、断点续传
- 文件合并后存储于 `server/uploads/`

---

## API 端点前缀

| 模块 | 路径前缀 |
|------|----------|
| 认证 | `/api/auth/*` |
| 课程 | `/api/courses/*` |
| 进度 | `/api/progress/*` |
| AI 考试 | `/api/ai-exam/*` |
| AI 对话 | `/api/ai-chat/*` |
| 小组 | `/api/group/*` |
| 错题本 | `/api/mistake-book/*` |

---

## 开发约定

### 代码风格
- **后端**: 遵循 Spring Boot 官方规范，使用 Lombok 简化代码
- **前端**: Vue 3 Composition API + ESLint

### 命名规范
- 后端实体类：大驼峰 (如 `LearningProgress`)
- 前端组件：大驼峰 (如 `CourseCard.vue`)
- API 服务：小驼峰 (如 `coursesApi.js`)

### 数据库
- MyBatis XML 映射文件位于 `server/src/main/resources/mapper/`
- 开启 SQL 日志：`logging.level.com.ccut.mapper: debug`

### 配置管理
- 敏感配置 (数据库密码、API Key) 在 `application.yml` 中管理
- JWT 开关：`jwt.enabled` (开发环境设为 `false`)

---

## 关键配置摘要

### Redis 配置
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      lettuce:
        pool:
          max-active: 20
```

### 数据库连接池
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
      connection-timeout: 30000
```

### AI 配置
```yaml
spring:
  ai:
    openai:
      api-key: <your-api-key>
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-chat
          temperature: 0.7
```

---

## 常用命令

### 前端
```bash
npm run dev      # 启动开发服务器
npm run build    # 生产构建
npm run lint     # ESLint 检查
npm run preview  # 预览构建结果
```

### 后端
```bash
mvn clean install           # 清理并编译
mvn spring-boot:run         # 启动服务
mvn test                    # 运行测试
```

---

## 相关文档

- [Redis 优化指南](server/REDIS_OPTIMIZATION_GUIDE.md) - 学习时长上报性能优化详解
- [学习进度优化](server/LEARNING_TIME_OPTIMIZATION.md) - 学习时长计算优化方案
- [后端 README](server/README.md) - 后端服务详细说明

---

## 日志系统

### 日志配置
- **配置文件**: `server/src/main/resources/logback-spring.xml`
- **存储路径**: `logs/` (项目根目录下)
- **日志文件**:
  | 文件 | 说明 |
  |------|------|
  | `ccut-service.log` | 所有日志（按天滚动） |
  | `ccut-service.2024-01-15.log` | 历史日志文件 |

### 日志滚动策略
- 按天滚动：每天生成一个新文件
- 保留 30 天历史
- 总大小限制：10GB

### 日志清理
- **定时任务**: `LogCleanupScheduler`
- **执行频率**: 每 30 天执行一次（凌晨 2:00）
- **清理策略**: 删除 30 天前的日志文件

### 查看日志
```bash
# 实时查看日志
tail -f logs/ccut-service.log

# 查看指定日期日志
cat logs/ccut-service.2024-01-15.log

# 搜索特定关键词
grep "批量刷新" logs/ccut-service.log

# 查看今日日志
cat logs/ccut-service.$(date +%Y-%m-%d).log
```

---

## 故障排查

### Redis 连接失败
```bash
redis-cli ping  # 检查 Redis 状态
redis-server    # 启动 Redis
```

### 查看后端日志
```bash
tail -f logs/service.log | grep "批量刷新"  # 查看定时任务日志
tail -f logs/service.log | grep "ERROR"     # 查看错误日志
```

### 前端代理问题
检查 `vite.config.js` 中的代理配置是否正确指向后端端口 `9999`
