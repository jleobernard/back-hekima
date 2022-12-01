FROM maven:3.8.6-eclipse-temurin-19-alpine AS build
WORKDIR /code
COPY src /code/src
COPY pom.xml /code/pom.xml
RUN mvn clean package -DskipTests -f /code/pom.xml

FROM eclipse-temurin:19.0.1_10-jre-alpine
WORKDIR /code
VOLUME /tmp
VOLUME /logs
VOLUME /kosubs
COPY --from=build /code/target/notes.jar /code/notes.jar
ENV JAVA_OPTS=""
ENV SPRING_PROFILE=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar -Dspring.profiles.active=$SPRING_PROFILE  /code/notes.jar" ]
