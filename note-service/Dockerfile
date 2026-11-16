FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY settings.gradle build.gradle ./
COPY note-service/build.gradle ./note-service/build.gradle

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

COPY note-service/src ./note-service/src

RUN ./gradlew :note-service:bootJar --no-daemon --console=plain

FROM eclipse-temurin:25-jre

RUN groupadd --system app && useradd --system --gid app app

WORKDIR /app
COPY --from=build /workspace/note-service/build/libs/note-service-*.jar app.jar

USER app
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
