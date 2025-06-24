# Etapa 1: build con Gradle + Java 21
FROM gradle:8-jdk21 AS builder          
WORKDIR /app

# Cachear dependencias
COPY build.gradle settings.gradle gradlew ./
COPY gradle gradle
RUN chmod +x gradlew \
    && ./gradlew --no-daemon dependencies  # prepara las dependencias

# Compilar el JAR
COPY src src
RUN ./gradlew --no-daemon bootJar         # genera build/libs/*.jar

# Etapa 2: runtime con OpenJDK 21 slim
FROM openjdk:21-jdk-slim AS runner       
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
