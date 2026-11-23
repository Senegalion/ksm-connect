FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package \
    && java -Djarmode=tools -jar target/ksm-connect.jar extract --layers --launcher --destination target/extracted

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S ksm && adduser -S -G ksm -H -s /sbin/nologin ksm
WORKDIR /app
COPY --from=build --chown=ksm:ksm /workspace/target/extracted/dependencies/ ./
COPY --from=build --chown=ksm:ksm /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build --chown=ksm:ksm /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=ksm:ksm /workspace/target/extracted/application/ ./
USER ksm
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
