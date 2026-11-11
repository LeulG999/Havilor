# Havilor as a container image (Flow 24). Two stages: build with the full JDK, run on a small JRE.
#   docker build -t havilor .
# Tests are not run here (they need Docker for Testcontainers); CI runs them before building the image.

# ---- build: the project's own Maven wrapper on JDK 21 ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build
# Dependencies first: this layer is reused as long as pom.xml doesn't change.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY src/ src/
RUN ./mvnw -q -B -DskipTests package

# ---- run: JRE 21 only (no compiler, no Maven), as a non-root user ----
FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S havilor && adduser -S -G havilor havilor
WORKDIR /app
COPY --from=build --chown=havilor:havilor /build/target/Havilor-*.jar app.jar
USER havilor
EXPOSE 8080
# Docker marks the container unhealthy when /actuator/health isn't UP (wget comes with Alpine's busybox).
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -q -O /dev/null http://localhost:8080/actuator/health || exit 1
# Secrets (HAVILOR_JWT_SECRET, API keys) come from the environment at run time, never from the image.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
