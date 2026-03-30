# Stage 1: Build the application using Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the built application
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/manageme-0.0.1-SNAPSHOT.jar manageme-v1.0.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "manageme-v1.0.jar"]