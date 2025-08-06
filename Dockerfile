FROM openjdk:23-jdk-slim

WORKDIR /app

# Gradle wrapper 및 설정 먼저 복사 (캐시 최적화)
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

# 나머지 소스 복사 후 빌드
COPY . .
RUN ./gradlew bootJar -x test --no-daemon \
    && ls -l build/libs/*.jar || (echo "❌ JAR 파일이 생성되지 않았습니다." && exit 1)

EXPOSE 8080

# 실행
ENTRYPOINT ["sh", "-c", "java -jar build/libs/*.jar"]