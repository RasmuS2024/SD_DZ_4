FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl

COPY build/libs/file-storing-service-*.jar app.jar

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]