FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

COPY pom.xml ./
COPY common/pom.xml common/
COPY tx-simulator/pom.xml tx-simulator/
COPY feature-service/pom.xml feature-service/
COPY decision-service/pom.xml decision-service/
COPY dashboard/pom.xml dashboard/

COPY common/src common/src
COPY tx-simulator/src tx-simulator/src

RUN mvn clean package -pl tx-simulator -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/tx-simulator/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
