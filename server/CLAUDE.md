# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

CCUT Smart Learning System (智慧课程在线学习系统) — an AI-powered online education platform with a Spring Boot backend and Vue 3 frontend. The backend (this directory) is a Java 17 / Spring Boot 3.5.5 application using MyBatis for persistence, Redis for caching, and Spring AI (via OpenAI-compatible API) for AI features powered by DeepSeek.

## Build & Run Commands

```bash
# Backend (from this server/ directory)
mvn clean install              # Build
mvn spring-boot:run            # Run (port 9999)

# Frontend (from parent directory)
npm install                    # Install dependencies
npm run dev                    # Dev server (port 5173, proxies /api /media /uploads to :9999)
npm run build                  # Production build
npm run lint                   # ESLint with --fix
```

There are currently no tests in this project.

## Architecture

### Backend Layers (`com.ccut`)

Standard Spring Boot layered architecture with interface-impl separation:

- **controller/** — REST API endpoints, all under `/api/`
- **service/** + **service/Impl/** — Business logic interfaces and implementations
- **mapper/** + **resources/mapper/*.xml** — MyBatis data access (Java interfaces + XML SQL mappings)
- **entity/** — Database entity classes (Lombok `@Data`)
- **dto/** — Request/response objects including the universal `Result<T>` wrapper
- **config/** — Configuration classes (WebMvc, JWT interceptor, AI, Redis, Async, AgentTools)
- **scheduled/** — `ProgressFlushScheduler` (every 5 min) and `LogCleanupScheduler`
- **utils/** — `JWTUtils`, `ReadFileUtils`
- **exception/** — `BusinessException`, `ErrorCode`, and `GlobalExceptionHandler`

### AI Integration

The AI system uses **native Spring AI Function Calling** (not a manual ReAct loop). Key components:

- **`AiConfig`** — Creates two model beans via OpenAI-compatible DeepSeek API: `chatModel` (deepseek-chat) and `reasoningChatModel` (deepseek-reasoner). Includes custom SSL config.
- **`AgentToolsConfig`** — Registers three Spring AI tool functions: `webSearch`, `generateExcel`, `generateWord`
- **`ChatAgentServiceImpl`** — Main chat agent using `ChatClient` with automatic tool dispatch. Supports both sync and SSE streaming (`Flux<String>`). Handles attachment context injection (document/image analysis via Tika).
- **`DocumentGeneratorServiceImpl`** — Generates Excel (Apache POI) and Word (.docx) files with styled headers, chapter structure, and organized output directories.
- **`WebSearchService`** — Internet search tool.
- **`system-prompt.md`** — External prompt file defining the AI persona "学小微" (Xiaowei), including tool usage rules, response style, and data accuracy requirements.
- **`AiExamService`** — AI-powered exam generation and grading.

### Redis Caching Pattern

The key performance optimization: learning progress reports are written to Redis first, then batch-flushed to MySQL by `ProgressFlushScheduler` every 5 minutes:
- `ProgressCacheService` / `ProgressCacheServiceImpl` — Redis Hash-based caching
- `ProgressFlushScheduler.getAllCachedProgress()` → groups by student → merges deltas → upserts to `video_progress`, `document_progress`, `learning_progress`, `weekly_study_time`

### Authentication

- `JwtInterceptor` intercepts all `/api/**` requests (except `/api/auth/login` and `/api/user/init`)
- JWT validation checks token format AND verifies the token matches what's stored in the `users` table (revocation on re-login)
- Can be disabled via `jwt.enabled: false` in application.yml

### API Conventions

- All APIs return `Result<T>` with `code`, `message`, `data` fields (200 = success)
- Errors use `BusinessException` with `ErrorCode` codes, caught by `GlobalExceptionHandler`
- Static uploads served at `/uploads/**` from configurable `file.upload-dir`

### Key Database Tables

Core schema in `resources/SQL.sql`:
- `users` (with role: student/teacher), `students`, `teachers`
- `courses`, `chapters`, `course_videos`, `course_documents`
- `enrollments`, `learning_progress`, `video_progress`, `document_progress`, `weekly_study_time`
- `conversations`, `messages` (AI chat history with `files` field for attachments)
- `ai_exams`, `ai_exam_questions`, `ai_exam_attempts`, `ai_exam_answers`
- `wrong_question`, `recommendation`, `student_groups`, `group_members`, `teacher_assignments`, `student_submissions`

### Configuration

`application.yml` key settings:
- MySQL on `localhost:3306/ccut`, Redis on `localhost:6379`
- AI via DeepSeek API (`spring.ai.openai.base-url`)
- File uploads configured via `file.upload-dir`
- MyBatis mapper-locations: `classpath:mapper/*.xml`, underscore-to-camel-case enabled
- SQL logging enabled at `com.ccut.mapper: debug`

### Frontend (in parent directory `src/`)

Vue 3 + Vite + Element Plus + Pinia. API services in `src/services/`, router in `src/router/`, views split into `student/` and `teacher/` directories. Vite dev server proxies `/api`, `/media`, `/uploads` to `http://localhost:9999`.
