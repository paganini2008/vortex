# syntax=docker/dockerfile:1.7
# The jar is the same on every platform: build it once, natively, on the build machine
# ($BUILDPLATFORM), so a multi-platform build runs Maven without CPU emulation
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Dependencies first, so editing the code does not download them again
RUN --mount=type=cache,id=vortex-m2,target=/root/.m2 mvn -q -B -U dependency:go-offline
COPY src src
RUN --mount=type=cache,id=vortex-m2,target=/root/.m2 mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/vortex-tsdb.jar vortex-tsdb.jar
# 30080 HTTP. The cluster port (22000) and work ports only need to be reachable between nodes
EXPOSE 30080
# The snapshot goes to /data: mount a volume there to keep it across containers
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75" \
    VORTEX_SERVER_PORT=30080 \
    VORTEX_DATA_DIR=/data
# Traefik routes to a container only once it reports healthy, so a node that is starting (or
# restarting after a crash) gets no traffic before it can answer
HEALTHCHECK --interval=2s --timeout=2s --start-period=90s --retries=3 \
  CMD curl -fsS -o /dev/null http://127.0.0.1:30080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/vortex-tsdb.jar"]
