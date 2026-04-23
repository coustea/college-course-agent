# ========================================
# 多阶段构建：Docker 内编译 + 运行
# 直接 docker compose up -d --build 即可
# ========================================

# 阶段1: Maven 编译
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# 阶段2: JRE 运行
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd -r appuser && useradd -r -g appuser -d /app appuser

COPY --from=builder /build/target/service-0.0.1-SNAPSHOT.jar app.jar

RUN mkdir -p /app/logs /app/uploads && chown -R appuser:appuser /app

USER appuser

EXPOSE 9999

VOLUME ["/app/logs", "/app/uploads"]

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/heapdump.hprof"
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]