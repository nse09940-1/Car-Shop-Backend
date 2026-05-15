FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace
COPY . .

RUN sed -i 's/\r$//' gradlew \
    && chmod +x gradlew \
    && ./gradlew --no-daemon :storage-service:bootJar -x test \
    && JAR_FILE="$(find storage-service/build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | head -n 1)" \
    && test -n "$JAR_FILE" \
    && cp "$JAR_FILE" /tmp/app.jar

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=builder /tmp/app.jar /app/app.jar

EXPOSE 8082 9090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
