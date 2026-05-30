FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar -x test

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

# Seed files for read_file smoke tests.
# Note: M6 sink server has NO path validation — these are baseline targets;
# arbitrary paths (e.g., /etc/passwd) are also reachable.
RUN mkdir -p /data && \
    printf 'hello from fs-server. M6 smoke target file.\n' > /data/welcome.txt && \
    printf 'Menu:\n- pizza\n- salad\n- pasta\n' > /data/menu.txt

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
