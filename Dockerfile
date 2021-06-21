FROM openjdk:16-jdk-slim as build

WORKDIR /app

COPY . ./

run ./mvnw package

run  cp target/*.jar target/app.jar

FROM openjdk:16-jdk-slim as production

RUN useradd -u 1001 app

WORKDIR /app

COPY --from=build /app/target/app.jar /app/app.jar

USER app

ENTRYPOINT ["java","-jar","/app/app.jar"]
