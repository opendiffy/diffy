#!/usr/bin/env bash
echo "Build primary, secondary, and candidate servers" && \
javac -d example src/test/scala/ai/diffy/examples/http/ExampleServers.java && \

echo "Deploy primary, secondary, and candidate servers" && \
java -cp example ai.diffy.examples.http.ExampleServers 9100 9200 9000
