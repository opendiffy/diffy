#!/usr/bin/env bash
echo "Sending some traffic to your Diffy instance"
for i in {1..1}
do
    sleep 0.1
    curl -s -i -X POST -d 'Microsoft' -H "Content-Type:application/text;Canonical-Resource:text" http://localhost:8880/text > /dev/null
    sleep 0.1
    curl -s -i -X POST -d '{"name":"Microsoft"}' -H "Content-Type:application/json;Canonical-Resource:json" http://localhost:8880/json > /dev/null
    sleep 0.1
    curl -s -i -X POST -d 'Twitter' -H "Content-Type:application/text;Canonical-Resource:text" http://localhost:8880/text > /dev/null
    sleep 0.1
    curl -s -i -X POST -d '{"name":"Twitter"}' -H "Content-Type:application/json;Canonical-Resource:json" http://localhost:8880/json > /dev/null
    sleep 0.1
    curl -s -i -X POST -d 'Airbnb' -H "Content-Type:application/text;Canonical-Resource:text" http://localhost:8880/text > /dev/null
    sleep 0.1
    curl -s -i -X POST -d '{"name":"Airbnb"}' -H "Content-Type:application/json;Canonical-Resource:json" http://localhost:8880/json > /dev/null
    sleep 0.1
    curl -s -i -X POST -d 'Mixpanel' -H "Content-Type:application/text;Canonical-Resource:text" http://localhost:8880/text > /dev/null
    sleep 0.1
    curl -s -i -X POST -d '{"name":"Mixpanel"}' -H "Content-Type:application/json;Canonical-Resource:json" http://localhost:8880/json > /dev/null
    sleep 0.1
    curl -s -i -X POST -d 'Cigna' -H "Content-Type:application/text;Canonical-Resource:text" http://localhost:8880/text > /dev/null
    sleep 0.1
    curl -s -i -X POST -d '{"name":"Cigna"}' -H "Content-Type:application/json;Canonical-Resource:json" http://localhost:8880/json > /dev/null
done
