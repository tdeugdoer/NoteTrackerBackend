# syntax=docker/dockerfile:1

# ---------- build ----------
FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY settings.gradle build.gradle ./
COPY note-service/build.gradle ./note-service/build.gradle
COPY history-service/build.gradle ./history-service/build.gradle

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

COPY history-service/src ./history-service/src

RUN ./gradlew :history-service:bootJar --no-daemon --console=plain

# ---------- runtime ----------
FROM eclipse-temurin:25-jre

RUN groupadd --system app && useradd --system --gid app app

WORKDIR /app
COPY --from=build /workspace/history-service/build/libs/history-service-*.jar app.jar

USER app
EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
