# Etapa 1: compilar el microservicio con Maven y Java 25
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src
# Las pruebas se omiten porque necesitan la base de datos encendida
RUN chmod +x mvnw && ./mvnw -q package -DskipTests

# Etapa 2: imagen final, solo con Java 25 (JRE) y el .jar
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/autenticacion-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
