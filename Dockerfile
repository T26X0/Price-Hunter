# syntax=docker/dockerfile:1

# Собирает неизменяемый Spring Boot JAR отдельно от минимального runtime-образа.
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml ./
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

# Запускает приложение от непривилегированного пользователя и не публикует порт напрямую наружу.
FROM eclipse-temurin:21-jre-jammy
RUN groupadd --system pricehunter && useradd --system --gid pricehunter --create-home pricehunter
WORKDIR /app

COPY --from=build /build/target/price-hunter-0.0.1-SNAPSHOT.jar /app/app.jar
USER pricehunter
EXPOSE 8080

ENTRYPOINT ["java", "-XX:InitialRAMPercentage=20", "-XX:MaxRAMPercentage=65", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]
