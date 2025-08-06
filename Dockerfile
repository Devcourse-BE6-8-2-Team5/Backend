FROM openjdk:21-jdk-slim
WORKDIR /app

# 전체 복사 후 한번에 빌드
COPY . .
RUN chmod +x gradlew
RUN ./gradlew bootJar -x test --no-daemon

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "build/libs/Backend-0.0.1-SNAPSHOT.jar"]

