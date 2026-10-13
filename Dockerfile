FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package
FROM quay.io/wildfly/wildfly:39.0.1.Final-jdk21
COPY --from=build --chown=jboss:root /build/target/matricula.war /opt/jboss/wildfly/standalone/deployments/
