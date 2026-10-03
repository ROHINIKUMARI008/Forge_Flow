# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY forgeflow-common forgeflow-common
COPY workflow-service workflow-service
COPY execution-worker execution-worker
RUN chmod +x mvnw && ./mvnw -pl execution-worker -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /src/execution-worker/target/app.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
