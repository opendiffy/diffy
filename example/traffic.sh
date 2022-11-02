#!/usr/bin/env bash
echo "Sending some traffic to your Diffy instance"
for i in {1..20}
do
    sleep 0.1
    curl -s -i -X POST -d 'Microsoft' -H "Canonical-Resource:text" http://localhost:8880/text > /dev/null
    sleep 0.1
    curl -s -i -X POST -d '{"name":"Microsoft"}' -H "Canonical-Resource:json" http://localhost:8880/json > /dev/null
    sleep 0.1
    curl -s -i -X POST -d 'Twitter' -H "Canonical-Resource:text" http://localhost:8880/text > /dev/null
    sleep 0.1
    curl -s -i -X POST -d '{"name":"Twitter"}' -H "Canonical-Resource:json" http://localhost:8880/json > /dev/null
    sleep 0.1
    curl -s -i -X POST -d 'Airbnb' -H "Canonical-Resource:text" http://localhost:8880/text > /dev/null
    sleep 0.1
    curl -s -i -X POST -d '{"name":"Airbnb"}' -H "Canonical-Resource:json" http://localhost:8880/json > /dev/null
    sleep 0.1
    curl -s -i -X POST -d 'Mixpanel' -H "Canonical-Resource:text" http://localhost:8880/text > /dev/null
    sleep 0.1
    curl -s -i -X POST -d '{"name":"Mixpanel"}' -H "Canonical-Resource:json" http://localhost:8880/json > /dev/null
    sleep 0.1
    curl -s -i -X POST -d 'Cigna' -H "Canonical-Resource:text" http://localhost:8880/text > /dev/null
    sleep 0.1
    curl -s -i -X POST -d '{"name":"Cigna"}' -H "Canonical-Resource:json" http://localhost:8880/json > /dev/null
done
