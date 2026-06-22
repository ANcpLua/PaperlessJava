FROM maven:3.8.5-openjdk-17-slim@sha256:502e781d39f0b40fbd02eb23f5b7663618b76ba52034da218c64e92f6c5647be AS builder
WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests -pl rest -am

FROM eclipse-temurin:17.0.19_10-jre-jammy@sha256:47c73dc23524b031bed0a5030410c722af6a8b49d4b25898ea8f4615895065f0
WORKDIR /app
COPY --from=builder /build/rest/target/rest-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
CMD ["java", "-jar", "app.jar"]