# AGENTS.md — CCUT Smart Learning System

> For agentic coding agents operating in this repository.

## Project Overview

CCUT 智慧课程在线学习系统 — AI-powered education platform. Spring Boot 3.5.5 (Java 17) backend in `server/`, Vue 3 frontend at root. No tests exist in this project.

---

## Build / Lint / Test Commands

### Backend (`server/`)
```bash
cd server
mvn clean install              # Full build
mvn spring-boot:run            # Dev server on port 9999
mvn clean package -DskipTests  # Package JAR
```

### Frontend (project root)
```bash
npm install                    # Install deps
npm run dev                    # Vite dev server on :5173 (proxies /api, /media, /uploads → :9999)
npm run build                  # Production build
npm run preview                # Preview production build
npm run lint                   # ESLint --fix (flat config, eslint 9.x)
```

### Database
- Create MySQL database `ccut`, run `server/src/main/resources/SQL.sql`
- Default accounts: `teacher`/`123456`, `student`/`123456`

### Running a Single Test
There are **no tests** in this project. The `spring-boot-starter-test` dependency exists but no test classes or files have been written. If adding tests, place them under `server/src/test/java/com/ccut/` and run:
```bash
mvn test -Dtest=YourTestClass    # Single test class
mvn test -Dtest=YourTestClass#methodName  # Single test method
```

---

## Backend Code Style (Java / Spring Boot)

### Architecture Pattern
- **Controller → Service Interface → ServiceImpl → MyBatis Mapper**
- Controllers live in `com.ccut.controller`, service interfaces in `com.ccut.service`, implementations in `com.ccut.service.Impl` (note: `Impl` capital I).
- Mappers in `com.ccut.mapper` with XML in `server/src/main/resources/mapper/*.xml`.

### Imports & Formatting
- No `.editorconfig`, `.prettierrc`, or checkstyle config exists. Follow existing file patterns.
- Standard Java import ordering: `java.*` / `javax.*` last, grouped by package.
- Use `@Slf4j` (Lombok) for logging — `log.info()`, `log.warn()`, `log.error()`.

### Naming Conventions
- **Controllers**: `XxxController.java` (e.g., `AuthController`, `CourseController`)
- **Services**: `XxxService.java` (interface) → `XxxServiceImpl.java` (implementation)
- **Entities**: PascalCase matching table names (e.g., `User`, `Course`, `VideoProgress`)
- **DTOs**: PascalCase with `Request`/`Response` suffix where applicable
- **Mappers**: `XxxMapper.java` interface + `XxxMapper.xml`

### Response Pattern
- All APIs return `Result<T>` with `{code, message, data}` fields.
- Success: `Result.success(data)` or `Result.success()`
- Error: `Result.error(code, message)`
- Controllers use try/catch with specific exception handling per method.

### Entity Conventions
- Use Lombok `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`
- Fields use camelCase; MyBatis has underscore-to-camel-case enabled
- Inline Chinese comments on fields (e.g., `private Long id;// 用户ID`)
- Manual constructors are common alongside Lombok annotations

### Dependency Injection
- `@Autowired` on fields (not constructor injection)
- `@Service`, `@RestController`, `@RequestMapping("/api/...")`

### Error Handling
- `BusinessException` + `ErrorCode` for domain errors
- `GlobalExceptionHandler` catches exceptions globally
- Controllers also use inline try/catch returning `Result.error()`
- Use `IllegalArgumentException` for validation, `RuntimeException` for business logic errors

### AI Integration
- Two model beans: `chatModel` (deepseek-chat) and `reasoningChatModel` (deepseek-reasoner)
- Spring AI Function Calling (not manual ReAct)
- System prompt in `server/src/main/resources/prompts/system-prompt.md`
- Tools: `webSearch`, `generateExcel`, `generateWord` via `AgentToolsConfig`

### Redis Caching Pattern
- Learning progress → Redis Hash (<5ms) → batch flush to MySQL every 5min via `ProgressFlushScheduler`
- Never bypass this pattern for progress-related writes

---

## Frontend Code Style (Vue 3 / Vite)

### Component Conventions
- **Composition API with `<script setup>`** — no Options API
- Single `.vue` files: `<template>` → `<script setup>` → `<style scoped>`
- Path alias `@` maps to `src/` (e.g., `@/views/student/Home.vue`)
- Component imports use absolute paths: `import CoursePlayer from '/src/components/CoursePlayer.vue'`

### Naming Conventions
- **Views**: PascalCase (e.g., `Home.vue`, `CourseCenter.vue`)
- **Student views**: `src/views/student/` (lowercase dir, PascalCase files)
- **Teacher views**: `src/views/Teacher/` (capital T dir, PascalCase files)
- **Services**: camelCase with `Api` suffix (e.g., `coursesApi.js`, `progressApi.js`)
- **Stores**: camelCase (e.g., `settings.js`)

### Service / API Pattern
- Axios instances created with `axios.create({ baseURL, timeout })`
- Request interceptors inject JWT from `localStorage.getItem('token')`
- Response interceptors handle 401/403/404/500 with `ElMessage.error()`
- Export named functions: `export const listAllCourses = () => api.get('/course/list')`
- Some services use `request` utility (`src/utils/request.js`), others create their own axios instance

### State Management (Pinia)
- `defineStore('name', () => { ... })` setup function style
- `ref()` for primitives, `reactive()` for objects
- Return object at the end exposing state and methods
- localStorage for persistent settings

### Router
- Role-based routes: `studentRoutes[]`, `teacherRoutes[]`, `publicRoutes[]`
- Lazy loading: `component: () => import('@/views/...')`
- Meta fields: `{ title: '...', requiresAuth: true/false, role: 'teacher' }`
- Nested routes with redirect for default child

### Error Handling
- `ElMessage.error()` / `ElMessage.success()` from Element Plus for user feedback
- `console.error()` for dev debugging
- try/catch around async operations with graceful fallbacks

### Styling
- `<style scoped>` in `.vue` components
- BEM-ish class naming (e.g., `.login-wrapper`, `.login-banner`, `.banner-overlay`)
- CSS custom properties for theming (`--primary-color`, `--bg-color`, etc.)
- Element Plus components with custom CSS overrides

---

## Key Constraints

- **Never** suppress Java type errors with `as any` or `@ts-ignore`
- **Never** commit code unless explicitly requested
- **Match existing patterns** — this codebase has mixed conventions; follow the file's own style
- **Bugfix rule**: Fix minimally. Do NOT refactor while fixing.
- **No tests exist** — don't expect to run or write tests unless asked
