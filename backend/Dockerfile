# --- Build stage: full JDK + Maven wrapper ---
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy only the build definition first so the dependency layer is cached
# and re-downloaded only when pom.xml changes.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src src
# Tests run in CI (they need Docker for Testcontainers, which is not available inside a build).
RUN ./mvnw -B package -DskipTests

# --- Runtime stage: JRE only, smaller image, non-root user ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /app/target/roomledger.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
