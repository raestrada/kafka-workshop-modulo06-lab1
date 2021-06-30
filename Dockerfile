FROM openjdk:16-jdk-slim as build

ARG MODULE
ARG SERVICE

WORKDIR /app

COPY ./${MODULE}/. ./

RUN JAVA_TOOL_OPTIONS="--illegal-access=permit" ./mvnw package

RUN  cp $SERVICE/target/*.jar $SERVICE/target/app.jar

FROM openjdk:16-jdk-slim as production

ARG SERVICE

RUN useradd -u 1001 app

WORKDIR /app

COPY --from=build /app/$SERVICE/target/app.jar /app/app.jar

USER app

ENTRYPOINT ["java","-jar","/app/app.jar"]
