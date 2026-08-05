# Use an official Maven image with Java 21 to build the app
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Build the application (skipping tests for faster deployment)
RUN mvn clean package -DskipTests

# Use a lightweight JRE image for the final run stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Copy the JAR from the build stage
COPY --from=build /app/target/invisible-maze-server-0.0.1-SNAPSHOT.jar app.jar

# Render will supply a PORT environment variable, our application.properties handles it, but expose 8085 as a default
EXPOSE 8085

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
