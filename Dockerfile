# build image
FROM maven:3.8.6-openjdk-18 as builder
ENV HOME=/usr/local/src
RUN mkdir -p $HOME
WORKDIR $HOME
ADD maven_docker_cache.xml $HOME
RUN mvn verify -f maven_docker_cache.xml --fail-never
ADD pom.xml $HOME
RUN mvn verify --fail-never
ADD . $HOME
RUN mvn package
RUN ls
RUN mv target /target
RUN mv agent /agent

# production image
FROM maven:3.8.6-openjdk-18
COPY --from=builder /target/diffy.jar /diffy.jar
COPY --from=builder /agent/opentelemetry-javaagent.jar /opentelemetry-javaagent.jar
ENTRYPOINT ["java", "-javaagent:opentelemetry-javaagent.jar", "-jar", "diffy.jar"]
CMD []