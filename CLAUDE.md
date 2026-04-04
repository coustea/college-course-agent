# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

CCUT Smart Learning System (智慧课程在线学习系统) — an AI-powered online education platform. Spring Boot 3.5.5 (Java 17) backend in `server/`, Vue 3 frontend at project root. AI features (chat, exams, document generation) powered by DeepSeek via Spring AI. Redis-cached progress tracking for high throughput.

## Build & Run

### Prerequisites
- JDK 17+, Node.js 18+, MySQL 8.0+, Redis 7.0+

### Backend (`server/`)
```bash
cd server
mvn clean install           # Build
mvn spring-boot:run         # Run on port 9999
```

### Frontend (root)
```bash
npm install                 # Install dependencies
npm run dev                 # Dev server on port 5173 (proxies /api, /media, /uploads to :9999)
npm run build               # Production build
npm run lint                # ESLint with --fix
```

### Database
- Create database `ccut`, run `server/src/main/resources/SQL.sql`
- Default credentials: teacher/123456, student/123456

There are currently no tests in this project.

## Architecture

### Repository Layout
```
├── server/                 # Spring Boot backend (Java 17)
│   └── src/main/java/com/ccut/
│       ├── controller/     # REST endpoints under /api/
│       ├── service/Impl/   # Business logic
│       ├── mapper/         # MyBatis interfaces + XML mappings in resources/mapper/
│       ├── entity/         # Lombok @Data DB entities
│       ├── dto/            # Result<T> wrapper, request/response DTOs
│       ├── config/         # WebMvc, JWT, AI (AiConfig, AgentToolsConfig), Redis, Async
│       ├── scheduled/      # ProgressFlushScheduler (5min), LogCleanupScheduler
│       └── utils/          # JWTUtils, ReadFileUtils
├── src/                    # Vue 3 frontend (Composition API + <script setup>)
│   ├── views/student/      # Student-facing pages
│   ├── views/teacher/      # Teacher-facing pages
│   ├── services/           # Axios API calls
│   ├── stores/             # Pinia state management
│   ├── router/             # Role-based route guards
│   └── components/         # Reusable components (player, doc viewer, AI chat)
├── server/CLAUDE.md        # Detailed backend-specific guidance (read this for server work)
└── README.md               # Full project documentation (Chinese)
```

### Key Patterns

- **Backend**: Controller → Service interface → ServiceImpl → MyBatis Mapper (Java + XML). All APIs return `Result<T>` (`{code, message, data}`). Errors via `BusinessException` + `GlobalExceptionHandler`.
- **AI**: Native Spring AI Function Calling (not manual ReAct). Two model beans: `chatModel` (deepseek-chat) and `reasoningChatModel` (deepseek-reasoner). Tools registered in `AgentToolsConfig` (webSearch, generateExcel, generateWord). System prompt in `server/src/main/resources/prompts/system-prompt.md`.
- **Redis caching**: Learning progress → Redis Hash (<5ms) → batch flush to MySQL every 5min via `ProgressFlushScheduler`. ~60x fewer DB writes.
- **Auth**: JWT in `users` table (revocation on re-login). `JwtInterceptor` protects `/api/**` except login/init. Disabling: `jwt.enabled: false` in application.yml.
- **Frontend**: Vue 3 Composition API, Element Plus, Pinia. Dynamic layout switching (StudentLayout/TeacherLayout) via route meta. Axios interceptor handles JWT injection and 401 redirect.

### Configuration

- `server/src/main/resources/application.yml` — DB (localhost:3306/ccut), Redis (localhost:6379), AI API, file upload dir, MyBatis settings
- `vite.config.js` — API proxy, path aliases
- MyBatis mapper XML files: `server/src/main/resources/mapper/*.xml` (underscore-to-camel-case enabled)

### Notable Libraries
- Apache POI + Tika — document generation and parsing
- mp4parser — video duration extraction
- PDF.js, Mammoth, Markdown-it, KaTeX, Highlight.js — frontend document/math rendering
- ECharts, Chart.js — data visualization
