# AGENTS.md - Development Guidelines for CCUT Backend

This document provides guidelines for AI agents working on the CCUT backend project. It covers build commands, testing, code style, and conventions.

## Project Overview

- **Language**: Java 17
- **Framework**: Spring Boot 3.5.5
- **Build Tool**: Maven
- **Database**: MySQL 8.0.33 with MyBatis
- **Key Dependencies**: Lombok, Hutool, Apache POI, JJWT, Spring AI Alibaba DashScope

## Build Commands

### Standard Maven Lifecycle Commands

```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package application (creates JAR in target/)
mvn clean package

# Install to local Maven repository
mvn clean install

# Run Spring Boot application
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Running a Single Test

```bash
# Run a specific test class
mvn test -Dtest=ServiceApplicationTests

# Run a specific test method
mvn test -Dtest=ServiceApplicationTests#contextLoads

# Run tests matching a pattern
mvn test -Dtest="*ControllerTest"
```

### Development Workflow Commands

```bash
# Check for compilation errors
mvn compile

# Run tests without compiling
mvn test -DskipTests=false

# Skip tests during build
mvn clean package -DskipTests

# Generate dependency tree
mvn dependency:tree
```

## Code Style Guidelines

### Package Structure

- Root package: `com.ccut`
- Sub-packages:
  - `controller`: REST API controllers
  - `service`: Business logic interfaces
  - `service.Impl`: Service implementations (note: capital I in "Impl")
  - `mapper`: MyBatis data access interfaces
  - `entity`: Data transfer objects and database entities
  - `config`: Configuration classes
  - `utils`: Utility classes

### Naming Conventions

- **Classes**: PascalCase (e.g., `CourseController`, `UserService`)
- **Interfaces**: PascalCase with descriptive names (e.g., `CourseService`, `UserMapper`)
- **Methods**: camelCase (e.g., `insert`, `deleteById`, `selectAll`)
- **Variables**: camelCase (e.g., `courseService`, `uploadDir`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_UPLOAD_SIZE`)
- **Database fields**: snake_case (mapped via MyBatis)

### Imports Organization

Follow this import order in Java files:

1. Package imports (same package - usually none)
2. Third-party library imports (org.springframework, org.apache, etc.)
3. Project imports (com.ccut.*)
4. Static imports (if any)

Example from `CourseController.java`:
```java
import com.ccut.entity.Course;
import com.ccut.entity.Result;
import com.ccut.service.CourseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
```

### Lombok Usage

- Use `@Data` for entity classes with getters, setters, toString, equals, hashCode
- Use `@AllArgsConstructor` and `@NoArgsConstructor` for entities
- Use `@Slf4j` for logging in controllers and services
- Avoid manual getter/setter generation when Lombok can handle it

Example entity:
```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Course {
    private Long courseId;
    private String courseCode;
    // ... other fields
}
```

### Spring Annotations

- **Controllers**: `@RestController`, `@RequestMapping("/api/[resource]")`, `@Slf4j`
- **Services**: `@Service` on implementation classes
- **Dependency Injection**: Use `@Autowired` field injection (as seen in existing code)
- **Configuration**: `@Value` for property injection, `@Configuration` for config classes
- **MyBatis Mappers**: `@Mapper` on interface

### Error Handling Pattern

- All controller methods should wrap business logic in try-catch blocks
- Return `Result<T>` wrapper with appropriate HTTP-like status codes:
  - `Result.success(data)` for 200 responses
  - `Result.error(code, message)` for errors (code >= 400)
- Log exceptions with `log.error("description: {}", e.getMessage(), e)`

Example:
```java
try {
    int n = courseService.insert(course);
    if (n > 0) {
        return Result.success(course);
    }
    return Result.error(500, "添加失败");
} catch (Exception e) {
    log.error("课程上传失败: {}", e.getMessage(), e);
    return Result.error(500, e.getMessage());
}
```

### REST API Conventions

- Use HTTP methods appropriately: GET (read), POST (create), PUT (update), DELETE (delete)
- Endpoint paths: `/api/[resource]/[action]` (e.g., `/api/course/insert`)
- Request parameters: Use `@RequestParam` for simple values, `@RequestPart` for file uploads
- JSON bodies: Use `@RequestBody` for complex objects
- File uploads: Consume `MediaType.MULTIPART_FORM_DATA_VALUE`

### Database/MyBatis Patterns

- Mapper interfaces define CRUD operations with descriptive names
- Use `@Param` annotation for mapper method parameters with multiple arguments
- Entity classes mirror database tables with appropriate field types
- Use `Long` for ID fields, not `long` (to allow null checks)

### Logging

- Use SLF4J via Lombok's `@Slf4j` annotation
- Log at appropriate levels: `log.debug()`, `log.info()`, `log.warn()`, `log.error()`
- Include context in log messages (e.g., "课程上传失败: {}")

## Testing Guidelines

- Test classes reside in `src/test/java/com/ccut/`
- Use JUnit 5 (`@Test` from `org.junit.jupiter.api.Test`)
- Use `@SpringBootTest` for integration tests
- Follow naming: `[ClassName]Tests` (e.g., `ServiceApplicationTests`)
- Currently minimal test coverage; add tests for new functionality

## Linting and Formatting

- No automated linting/formatting tools configured (no checkstyle, spotless)
- Follow existing code style conventions
- Maintain consistent indentation (4 spaces, not tabs)
- Line length: Keep under 120 characters where possible

## Git Practices

- Commit messages: Use descriptive present-tense messages
- Branch naming: feature/..., bugfix/..., hotfix/...
- Never commit secrets, credentials, or local configuration
- Update `.gitignore` as needed for IDE files, build artifacts

## IDE Configuration

- The project includes `.idea` directory for IntelliJ IDEA
- Use Java 17 SDK
- Enable annotation processing for Lombok
- Import as Maven project

## Additional Notes

- **Cursor/Copilot Rules**: No `.cursorrules` or `.github/copilot-instructions.md` found
- **Environment Variables**: Configured via `application.yml`
- **File Uploads**: Path configured in `file.upload-dir` property
- **AI Integration**: Spring AI Alibaba DashScope for AI features

## When Making Changes

1. Analyze existing patterns in similar files
2. Follow the established naming and structure conventions
3. Write tests for new functionality when possible
4. Run `mvn test` before committing
5. Ensure the application starts with `mvn spring-boot:run`

---
*Last updated: January 26, 2025*