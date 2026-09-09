FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /build/target/veritaspath.jar ./veritaspath.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "veritaspath.jar"]
