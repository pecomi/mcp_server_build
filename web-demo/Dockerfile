# Stage 1 — Build React frontend
FROM node:20-alpine AS frontend-build
WORKDIR /frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# Stage 2 — Build Spring Boot backend (with frontend dist copied in as static)
FROM eclipse-temurin:17-jdk AS backend-build
WORKDIR /backend
COPY backend/ ./
COPY --from=frontend-build /frontend/dist ./src/main/resources/static
RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar -x test

# Stage 3 — Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=backend-build /backend/build/libs/*.jar app.jar

EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
