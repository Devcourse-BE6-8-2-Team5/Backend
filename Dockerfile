# Java 기반 이미지 사용
FROM openjdk:23-jdk-slim

# 작업 디렉토리 설정
WORKDIR /app

# Gradle wrapper와 소스코드 복사
COPY . .

# 실행 권한 부여 및 빌드
RUN chmod +x ./gradlew
RUN ./gradlew build -x test

# 8080 포트 노출
EXPOSE 8080

# Spring Boot 앱 실행
ENTRYPOINT ["java", "-jar", "build/libs/*.jar"]
