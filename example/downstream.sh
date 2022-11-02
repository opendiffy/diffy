#!/usr/bin/env bash
echo "Build primary, secondary, and candidate servers" && \
mvn package -f example/pom.xml && \

echo "Deploy primary, secondary, and candidate servers" && \
java -jar target/example.jar 9100 9200 9000
