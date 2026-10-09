FROM eclipse-temurin:21-jdk@sha256:3e3c176ffed168beb42c607be9bc1639b466cf00261a0fb04425562c9d0c5c2b AS build
WORKDIR /app

# NOTE: unlike billing-service, no buf CLI is installed here. agent-adapter has no
# proto sources yet and its generateBufCode task is guarded off, so the build does
# not need buf. If gRPC/proto is introduced, install buf as billing-service does
# and COPY the buf.yaml / buf.gen.yaml files before the build step.

# Build scripts first so dependency resolution is cached independently of sources.
# The Gradle root is the outer directory; modules live under agent-service/ (see settings.gradle).
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY agent-service/agent-domain/build.gradle agent-service/agent-domain/
COPY agent-service/agent-application/build.gradle agent-service/agent-application/
COPY agent-service/agent-adapter/build.gradle agent-service/agent-adapter/
COPY agent-service/agent-bootstrap/build.gradle agent-service/agent-bootstrap/
RUN chmod +x gradlew

# Sources for every module.
COPY agent-service/agent-domain/src agent-service/agent-domain/src
COPY agent-service/agent-application/src agent-service/agent-application/src
COPY agent-service/agent-adapter/src agent-service/agent-adapter/src
COPY agent-service/agent-bootstrap/src agent-service/agent-bootstrap/src

RUN ./gradlew :agent-bootstrap:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine@sha256:51ab5e3302e7141ce665ca3ea85e8b5cd648eafbc3c0c90dd79d6537684e4555 AS runtime
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /app/agent-service/agent-bootstrap/build/libs/*.jar app.jar

USER appuser

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
