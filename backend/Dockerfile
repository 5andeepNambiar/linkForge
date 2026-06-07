FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY common common
COPY url-service url-service
COPY redirect-service redirect-service
COPY analytics-service analytics-service
ARG MODULE
RUN mvn -q -pl ${MODULE} -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
ARG MODULE
COPY --from=build /workspace/${MODULE}/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
