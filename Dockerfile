# build image
FROM maven:3.8-eclipse-temurin-17-focal as builder
ENV HOME=/usr/local/src
RUN mkdir -p $HOME
WORKDIR $HOME
# install cached version of pom.xml
ADD maven_docker_cache.xml $HOME
RUN mvn verify -f maven_docker_cache.xml --fail-never

# install node v16.14.0 and yarn v1.22.19
RUN mvn com.github.eirslett:frontend-maven-plugin:install-node-and-yarn -DnodeVersion=v16.14.0 -DyarnVersion=v1.22.19 -f maven_docker_cache.xml

# install dependencies in frontend/package.json
RUN mkdir -p $HOME/frontend
ADD frontend/package.json $HOME/frontend
RUN mvn com.github.eirslett:frontend-maven-plugin:yarn -f maven_docker_cache.xml

# install dependencies in pom.xml
ADD pom.xml $HOME
RUN mvn verify --fail-never

# finally, copy, compile, bundle, and package everything
ADD . $HOME
RUN mvn package
RUN ls
RUN mv target /target
RUN mv agent /agent

# production image
FROM maven:3.8-eclipse-temurin-17-focal
COPY --from=builder /target/diffy.jar /diffy.jar
ENTRYPOINT ["java", "-jar", "diffy.jar"]
CMD []
