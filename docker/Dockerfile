FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

# Copier le JAR généré par Spring Boot
COPY api/target/*.jar app.jar

# Utilisateur non-root (sécurité)
RUN addgroup -S timemanager && adduser -S timemanager -G timemanager
USER timemanager

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]
