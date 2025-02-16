FROM maven:3.8.4-openjdk-17-slim AS builder
WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests -pl rest -am

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /build/rest/target/rest-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
CMD ["java", "-jar", "app.jar"]