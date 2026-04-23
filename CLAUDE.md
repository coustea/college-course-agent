# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

CCUT Smart Learning System (智慧课程在线学习系统) — an AI-powered online education platform. Spring Boot 3.5.5 (Java 25) backend in `server/`, Vue 3 frontend at project root. AI features (chat, exams, document generation) powered by DeepSeek via Spring AI. Redis-cached progress tracking for high throughput.

**Key Innovation**: Redis-cached progress tracking with batch flushing reduces database write load by ~60x, enabling high-throughput learning progress reporting.

## Build & Run

### Prerequisites
- JDK 25+, Node.js 18+, MySQL 8.0+, Redis 7.0+

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
```bash
# Create database and run initialization script
mysql -u root -p
CREATE DATABASE ccut;
USE ccut;
SOURCE server/src/main/resources/SQL.sql;
```

**Default credentials**: teacher/123456, student/123456

**Important**: This project uses Java 25 (not Java 17).

There are currently no tests in this project.

## Architecture

### Repository Layout
```
├── server/                 # Spring Boot backend (Java 25)
│   └── src/main/java/com/ccut/
│       ├── controller/     # REST endpoints under /api/ (recently refactored, ~1700 lines removed)
│       ├── service/Impl/   # Business logic
│       ├── mapper/         # MyBatis interfaces + XML mappings in resources/mapper/
│       ├── entity/         # Lombok @Data DB entities
│       ├── dto/            # Result<T> wrapper, request/response DTOs
│       ├── config/         # WebMvc, JWT, AI (AiConfig, AgentToolsConfig), Redis, Async
│       ├── scheduled/      # ProgressFlushScheduler (5min), LogCleanupScheduler
│       ├── context/        # UserContext for thread-local user data
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

- **Backend**: Controller → Service interface → ServiceImpl → MyBatis Mapper (Java + XML). All APIs return `Result<T>` (`{code, message, data}`). Errors via `BusinessException` + `GlobalExceptionHandler`. Recently refactored to eliminate controller boilerplate.
- **AI**: Native Spring AI Function Calling (not manual ReAct). Two model beans: `chatModel` (deepseek-chat) and `reasoningChatModel` (deepseek-reasoner). Tools registered in `AgentToolsConfig` (webSearch, generateExcel, generateWord) and `AgentDbToolsConfig` (database queries for student data). System prompt in `server/src/main/resources/prompts/system-prompt.md`.
- **Redis caching**: Learning progress → Redis Hash (<5ms) → batch flush to MySQL every 5min via `ProgressFlushScheduler`. ~60x fewer DB writes, 100x+ concurrent capacity improvement.
- **Auth**: JWT in `users` table (revocation on re-login). `JwtInterceptor` protects `/api/**` except login/init. User context stored in thread-local `UserContext`. Disabling: `jwt.enabled: false` in application.yml.
- **Frontend**: Vue 3 Composition API, Element Plus, Pinia. Dynamic layout switching (StudentLayout/TeacherLayout) via route meta. Axios interceptor handles JWT injection and 401 redirect.

### Configuration

- `server/src/main/resources/application.yml` — DB (localhost:3306/ccut), Redis (localhost:6379), AI API, file upload dir, MyBatis settings
- `vite.config.js` — API proxy, path aliases
- MyBatis mapper XML files: `server/src/main/resources/mapper/*.xml` (underscore-to-camel-case enabled)

### Notable Libraries
- Apache POI + Tika — document generation and parsing
- mp4parser — video duration extraction
- Hutool — Java utility library
- PDF.js, Mammoth, Markdown-it, KaTeX, Highlight.js — frontend document/math rendering
- ECharts, Chart.js — data visualization

## Common Development Tasks

When working on this codebase:

**Backend changes**: Focus on `server/` directory. Follow Controller → Service → Mapper pattern. All APIs must return `Result<T>`. Use `BusinessException` for errors with `ErrorCode`.

**Frontend changes**: Work in root `src/` directory. Use Composition API with `<script setup>`. API calls go through services in `src/services/`. State management via Pinia stores.

**AI-related changes**: Modify `server/src/main/resources/prompts/system-prompt.md` for persona changes. Register new tools in `AgentToolsConfig` or `AgentDbToolsConfig`. Implement tools in `service/Impl/`.

**Database changes**: Update `server/src/main/resources/SQL.sql` for schema changes. Create corresponding entity classes, mapper interfaces, and XML mappings.

**Performance considerations**: Progress reporting uses Redis caching — avoid bypassing `ProgressCacheService` for progress updates. Batch operations are preferred over individual queries.