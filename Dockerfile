FROM node:24-alpine AS frontend
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-25 AS agent
ARG APPLICATION_INSIGHTS_AGENT_VERSION=3.7.10
ARG APPLICATION_INSIGHTS_AGENT_SHA256=93a70c8f5d364c7e777f6c4d1b235dba91aef8448bd3fa94359f1d7f3e2eb0ec
WORKDIR /agent
RUN curl --fail --silent --show-error --location --proto '=https' --proto-redir '=https' --tlsv1.2 \
      --output applicationinsights-agent.jar \
      "https://github.com/microsoft/ApplicationInsights-Java/releases/download/${APPLICATION_INSIGHTS_AGENT_VERSION}/applicationinsights-agent-${APPLICATION_INSIGHTS_AGENT_VERSION}.jar" \
    && echo "${APPLICATION_INSIGHTS_AGENT_SHA256}  applicationinsights-agent.jar" | sha256sum -c -

FROM maven:3.9-eclipse-temurin-25 AS backend
WORKDIR /workspace
COPY backend/pom.xml .
RUN mvn -B -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /frontend/dist ./src/main/resources/static
RUN mvn -B -q -DskipTests package \
    && java -Djarmode=tools -jar target/ksm-connect.jar extract --layers --launcher --destination target/extracted

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S ksm && adduser -S -G ksm -H -s /sbin/nologin ksm
WORKDIR /app
COPY --from=agent --chown=ksm:ksm /agent/applicationinsights-agent.jar /app/agent/applicationinsights-agent.jar
COPY --from=backend --chown=ksm:ksm /workspace/target/extracted/dependencies/ ./
COPY --from=backend --chown=ksm:ksm /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=backend --chown=ksm:ksm /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=backend --chown=ksm:ksm /workspace/target/extracted/application/ ./
USER ksm
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
