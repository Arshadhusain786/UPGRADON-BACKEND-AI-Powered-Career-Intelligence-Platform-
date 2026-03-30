# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# Copy jar from builder
COPY --from=builder /app/target/nexpath-backend-1.0.0.jar app.jar

# Create non-root user
RUN useradd -m -u 1000 nexpath && chown -R nexpath:nexpath /app

USER nexpath

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD java -cp app.jar org.springframework.boot.loader.JarLauncher --actuator.endpoints.web.base-path=/api/actuator | grep -q '"status":"UP"' || exit 1

# Expose port
EXPOSE 8080

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
