#!/usr/bin/env bash

if [ "$1" = "start" ];
then

    echo "Build Diffy" && \
   ./sbt assembly

    echo "Build primary, secondary, and candidate servers" && \
    javac -d example src/test/scala/ai/diffy/examples/http/ExampleServers.java && \

    echo "Deploy primary, secondary, and candidate servers" && \
    java -cp example ai.diffy.examples.http.ExampleServers 9000 9100 9200 & \

    echo "Deploy Diffy" && \
    java -jar ./target/scala-2.12/diffy-server.jar \
    -candidate='localhost:9200' \
    -master.primary='localhost:9000' \
    -master.secondary='localhost:9100' \
    -service.protocol='http' \
    -serviceName='My Service' \
    -proxy.port=:8880 \
    -admin.port=:8881 \
    -http.port=:8888 \
    -rootUrl='localhost:8888' & \

    sleep 3
    echo "Wait for server to deploy"
    sleep 2

    echo "Send some traffic to your Diffy instance"
    for i in {1..20}
    do
        sleep 0.1
        curl -s -i -H "Canonical-Resource : json" http://localhost:8880/json?Mixpanel > /dev/null
        sleep 0.1
        curl -s -i -H "Canonical-Resource : json" http://localhost:8880/json?Twitter > /dev/null
        sleep 0.1
        curl -s -i -H "Canonical-Resource : json" http://localhost:8880/json?Airbnb > /dev/null
        sleep 0.1
        curl -s -i -H "Canonical-Resource : json" http://localhost:8880/json?Paytm > /dev/null
        sleep 0.1
        curl -s -i -H "Canonical-Resource : json" http://localhost:8880/json?Baidu > /dev/null
    done

    echo "Your Diffy UI can be reached at http://localhost:8888"

else
    echo "Please make sure ports 9000, 9100, 9200, 8880, 8881, & 8888 are available before running \"example/run.sh start\""
fi

