FROM gradle:7-jdk19 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src

RUN gradle shadowJar --no-daemon


FROM openjdk:19
EXPOSE 10000:10000
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/featureideservice.jar
ENTRYPOINT ["java","-jar","/app/featureideservice.jar"]