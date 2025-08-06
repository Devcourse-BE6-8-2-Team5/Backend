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

RUN ./gradlew bootJar -x test --no-daemon && \
    echo "빌드 완료, jar 파일:" && ls -l build/libs

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "build/libs/Backend-0.0.1-SNAPSHOT.jar"]

