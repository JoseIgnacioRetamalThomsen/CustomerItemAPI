# Base image
FROM eclipse-temurin:21-jdk-alpine

# Set working directory inside container
WORKDIR /app

# Copy the JAR built by Maven
COPY target/undertow-app-1.0.0-SNAPSHOT.jar app.jar

# Create directory for MapDB
RUN mkdir -p /app/db

# Expose the port the app listens on
EXPOSE 8080

# Run the JAR
ENTRYPOINT ["java", "-jar", "app.jar"]