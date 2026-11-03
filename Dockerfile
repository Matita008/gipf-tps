# image to run the server

FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

COPY src/ src/
RUN ./gradlew serverJar --no-daemon -x test

RUN $JAVA_HOME/bin/jlink \
    --add-modules java.base,java.logging \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=1 \
    --output /min-jre

FROM alpine:latest as runtime
WORKDIR /app

# TODO: check if they are needed
RUN apk add --no-cache libstdc++ gcompat

ENV JAVA_HOME=/opt/java/jre
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --from=builder /min-jre $JAVA_HOME

# don't run as root
RUN addgroup -S gipf-server && adduser -S gipf-server -G gipf-server
USER gipf-server

COPY --from=builder /app/build/libs/*.jar ./

ENTRYPOINT ["java", "-jar", "server.jar"]
