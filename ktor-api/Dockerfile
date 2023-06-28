FROM gradle:7-jdk19 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN mkdir lib

ADD https://github.com/FeatureIDE/FeatureIDE/releases/download/v3.9.3/de.ovgu.featureide.lib.fm-v3.9.2.jar lib/de.ovgu.featureide.lib.fm-v3.9.3_new.jar
ADD https://github.com/FeatureIDE/FeatureIDE/raw/v3.9.3/plugins/de.ovgu.featureide.fm.core/lib/antlr-3.4.jar lib/antlr-3.4.jar
ADD https://github.com/FeatureIDE/FeatureIDE/raw/v3.9.3/plugins/de.ovgu.featureide.fm.core/lib/org.sat4j.core.jar lib/org.sat4j.core.jar
ADD https://github.com/FeatureIDE/FeatureIDE/raw/v3.9.3/plugins/de.ovgu.featureide.fm.core/lib/uvl-parser.jar lib/uvl-parser.jar
ADD https://github.com/FeatureIDE/FeatureIDE/raw/v3.9.3/plugins/de.ovgu.featureide.fm.core/lib/SPLCAT.jar lib/SPLCAT.jar

RUN gradle shadowJar --no-daemon


FROM openjdk:19
EXPOSE 10000:10000
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/featureideservice.jar
ENTRYPOINT ["java","-jar","/app/featureideservice.jar"]