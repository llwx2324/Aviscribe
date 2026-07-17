FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ffmpeg python3-pip \
    && pip3 install --no-cache-dir yt-dlp \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home aviscribe
WORKDIR /app
COPY --from=build /workspace/target/aviscribe-backend-*.jar /app/aviscribe-backend.jar
RUN mkdir -p /var/lib/aviscribe/files && chown -R aviscribe:aviscribe /app /var/lib/aviscribe
USER aviscribe
EXPOSE 8082
ENV AVISCRIBE_UPLOAD_PATH=/var/lib/aviscribe/files/
VOLUME ["/var/lib/aviscribe/files"]
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl --fail --silent http://localhost:8082/api/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/aviscribe-backend.jar"]
