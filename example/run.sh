#!/usr/bin/env bash

if [ "$1" = "start" ];
then
    echo "Your Diffy UI will be reached at http://localhost:8888" && \
    echo "You can run \"example/downstream.sh\" to send traffic to your Diffy instance." && \
    echo "Building Diffy" && \
    mvn package && \

    echo "Deploy Diffy" && \
    java -jar ./target/diffy.jar \
    --candidate='localhost:9000' \
    --master.primary='localhost:9100' \
    --master.secondary='localhost:9200' \
    --allowHttpSideEffects='true' \
    --responseMode='candidate' \
    --service.protocol='http' \
    --serviceName='ExampleService' \
    --proxy.port=8880 \
    --http.port=8888
else
    echo "Please make sure you run \"example/downstream.sh\" before running \"example/run.sh start\""
fi
