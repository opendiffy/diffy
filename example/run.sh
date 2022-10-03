#!/usr/bin/env bash

if [ "$1" = "start" ];
then

    echo "Build primary, secondary, and candidate servers" && \
    javac -d example src/test/scala/ai/diffy/examples/http/ExampleServers.java && \

    echo "Deploy primary, secondary, and candidate servers" && \
    java -cp example ai.diffy.examples.http.ExampleServers 9000 9100 9200 & \

    echo "Build Diffy" && \
    mvn package && \

    echo "Deploy Diffy" && \
    java -javaagent:./agent/opentelemetry-javaagent.jar -jar ./target/diffy.jar \
    --candidate='localhost:9200' \
    --master.primary='localhost:9000' \
    --master.secondary='localhost:9100' \
    --responseMode='candidate' \
    --service.protocol='http' \
    --serviceName='ExampleService' \
    --proxy.port=8880 \
    --http.port=8888 & \

    echo "Your Diffy UI can be reached at http://localhost:8888"

else
    echo "Please make sure ports 9000, 9100, 9200, 8880, & 8888 are available before running \"example/run.sh start\""
fi
