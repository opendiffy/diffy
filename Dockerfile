# build image
FROM mozilla/sbt:8u212_1.2.8 as builder

RUN apt-get update

ADD . /usr/local/src
WORKDIR /usr/local/src
RUN ./sbt assembly
RUN mv target/scala-2.12 /bin/diffy

# production image
FROM openjdk:8-jre-alpine
COPY --from=builder /bin/diffy /bin/diffy
ENTRYPOINT ["java", "-jar", "/bin/diffy/diffy-server.jar"]

CMD [ "-candidate=localhost:9992", \
      "-master.primary=localhost:9990", \
      "-master.secondary=localhost:9991", \
      "-service.protocol=http", \
      "-serviceName='Test-Service'", \
      "-proxy.port=:8880", \
      "-admin.port=:8881", \
      "-http.port=:8888", \
      "-rootUrl=localhost:8888" \
]

