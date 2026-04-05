# =========================
# --- Build Stage ---
# =========================

# Java 17 JDK가 포함된 이미지 사용 (빌드용)
FROM eclipse-temurin:17-jdk AS build

# 작업 디렉토리 설정
WORKDIR /app

# Gradle wrapper 관련 파일 먼저 복사 (의존성 캐시 활용 목적)
COPY gradlew .
COPY gradle gradle

# Gradle 설정 파일 복사
COPY build.gradle .
COPY settings.gradle .

# Gradle build 실행 (테스트 제외)
# || true → 실패해도 다음 단계 진행 (캐시 목적)
RUN ./gradlew build -x test --no-daemon || true

# 실제 애플리케이션 소스 코드 복사
COPY src src

# 최종 실행 가능한 Spring Boot jar 생성
RUN ./gradlew clean bootJar -x test --no-daemon


# =========================
# --- Run Stage ---
# =========================

# JRE만 포함된 경량 이미지 사용 (실행용)
FROM eclipse-temurin:17-jre-alpine

# 작업 디렉토리 설정
WORKDIR /app

# 보안을 위해 일반 사용자 생성 (root 대신 실행)
RUN addgroup -S spring && adduser -S spring -G spring

# 이후 명령을 spring 사용자로 실행
USER spring:spring

# build stage에서 생성된 jar 파일을 복사
COPY --from=build /app/build/libs/*jar app.jar

# JVM 옵션 설정
# -Xms256M : 초기 힙 메모리
# -Xmx256M : 최대 힙 메모리
# -XX:+UseG1GC : G1 Garbage Collector 사용
# -XX:MaxRAMPercentage=75.0 : 컨테이너 메모리 기준 힙 자동 설정
# -Duser.timezone=Asia/Seoul : 타임존 설정
ENV JAVA_TOOL_OPTIONS="-Xms256M -Xmx256M -XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Duser.timezone=Asia/Seoul"

# 컨테이너 시작 시 실행될 명령어 (Spring Boot 실행)
ENTRYPOINT ["java", "-jar", "app.jar"]