FROM eclipse-termurin:21-jre
WORKDIR /app
COPY target/manageme-0.0.1-SNAPSHOT.jar manageme-v1.0.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "manageme-v1.0.jar"]