FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN ./mvnw -B -DskipTests package -DskipTests

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/target/assignment-0.0.1-SNAPSHOT.jar"]
