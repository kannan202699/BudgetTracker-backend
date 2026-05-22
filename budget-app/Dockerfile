FROM eclipse-temurin:21-jdk

LABEL authors="Kannan"

WORKDIR /app

COPY target/budget-app-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 1010

ENTRYPOINT ["java", "-jar", "app.jar"]