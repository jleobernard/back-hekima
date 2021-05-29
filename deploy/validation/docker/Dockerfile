FROM openjdk:16-slim
VOLUME /tmp
VOLUME /logs
ADD back-hekima.jar back-hekima.jar
RUN sh -c 'touch /back-hekima.jar'
RUN sh -c 'apt-get update'
RUN sh -c 'apt-get install -y bash curl'
ENV JAVA_OPTS=""
ENV SPRING_PROFILE=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar -Dspring.profiles.active=$SPRING_PROFILE  /back-hekima.jar" ]