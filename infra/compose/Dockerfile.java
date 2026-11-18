FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace
COPY . .

ARG MODULE

RUN --mount=type=cache,target=/root/.m2 \
    chmod +x ./mvnw \
    && ./mvnw -B -pl services/${MODULE} -am -DskipTests package \
    && cp services/${MODULE}/target/*.jar /tmp/app.jar
    
FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /tmp/app.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
