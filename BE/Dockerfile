FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/bakery3d-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
EXPOSE 8080
