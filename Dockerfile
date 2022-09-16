# build image
FROM maven:3.8.6-openjdk-18 as builder
ENV HOME=/usr/local/src
RUN mkdir -p $HOME
WORKDIR $HOME
ADD pom.xml $HOME
RUN mvn verify --fail-never
ADD . $HOME
RUN mvn package
RUN ls
RUN mv target /target

# production image
FROM openjdk:oracle
COPY --from=builder /target/diffy.jar /diffy.jar
ENTRYPOINT ["java", "-jar", "diffy.jar"]
CMD [ "--candidate.port=9992", \
      "--master.primary.port=9990", \
      "--master.secondary.port=9991", \
      "--service.protocol=http", \
      "--service.name='Sample-Service'", \
      "--proxy.port=8880", \
      "--server.port=8888", \
      "--rootUrl=localhost:8888" \
]