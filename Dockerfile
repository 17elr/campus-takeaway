FROM node:24-alpine AS web
WORKDIR /build/web
COPY web/package*.json ./
RUN npm ci --no-audit --no-fund
COPY web/ ./
RUN npm test && npm run build

FROM maven:3.9-eclipse-temurin-17 AS java
WORKDIR /build
COPY pom.xml ./
RUN mvn -B dependency:go-offline
COPY src/ src/
COPY --from=web /build/web/dist/ web/dist/
RUN mvn -B -Dmaven.test.skip=true package

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S takeaway && adduser -S takeaway -G takeaway
WORKDIR /app
COPY --from=java /build/target/campus-takeaway-2.0.0.jar app.jar
USER takeaway
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
