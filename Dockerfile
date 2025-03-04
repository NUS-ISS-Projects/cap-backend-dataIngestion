FROM openjdk:17-jdk
WORKDIR /app
COPY target/data-ingestion-service-0.0.1-SNAPSHOT.jar /app/data-ingestion-service.jar
ENTRYPOINT ["java", "-jar", "data-ingestion-service.jar"]
