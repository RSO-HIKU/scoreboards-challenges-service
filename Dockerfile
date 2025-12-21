FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy JAR
COPY target/scoreboards-challenges-service-0.1.0.jar ./scoreboards-challenges-service.jar

# Copy configuration
COPY src/main/resources/config.yaml ./config.yaml

# Expose port as defined in config.yaml
EXPOSE 8089

# Run JAR with explicit config
CMD ["java", "-jar", "scoreboards-challenges-service.jar"]
