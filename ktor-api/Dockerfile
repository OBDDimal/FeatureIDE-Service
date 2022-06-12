FROM gradle:7-jdk11 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle shadowJar --no-daemon

FROM openjdk:11
EXPOSE 8080:8080
RUN mkdir /app
COPY de.featureide.service.jar /app/ktor-docker.jar
ENTRYPOINT ["java","-jar","/app/ktor-docker.jar"]