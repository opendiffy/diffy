# Diffy

[![Build status](https://img.shields.io/travis/opendiffy/diffy/master.svg)](https://travis-ci.org/opendiffy/diffy)
[![Coverage status](https://img.shields.io/codecov/c/github/opendiffy/diffy/master.svg)](https://codecov.io/github/opendiffy/diffy)
[![Project status](https://img.shields.io/badge/status-active-brightgreen.svg)](#status)
[![Gitter](https://img.shields.io/badge/gitter-join%20chat-green.svg)](https://gitter.im/opendiffy/diffy)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)
## Status

Diffy is used in production at:
* [Mixpanel](https://engineering.mixpanel.com/2019/07/24/safely-rewriting-mixpanels-highest-throughput-service-in-golang/)
* Airbnb [(Scalabity)](https://www.infoq.com/presentations/airbnb-services-scalability/) [(Migration)](https://www.infoq.com/presentations/airbnb-soa-migration/)
* [Twitter](https://blog.twitter.com/engineering/en_us/a/2015/diffy-testing-services-without-writing-tests.html)
* Baidu
* Bytedance

and blogged about by cloud infrastructure providers like:
* [Alibaba Cloud](https://www.alibabacloud.com/blog/traffic-management-with-istio-3-traffic-comparison-analysis-based-on-istio_594545)
* [Datawire](https://blog.getambassador.io/next-level-testing-with-an-api-gateway-and-continuous-delivery-9cbb9c4564b5)

If your organization is using Diffy, consider adding a link here and sending us a pull request!

Diffy is being actively developed and maintained by the engineering team at [Sn126](https://www.sn126.com).

Feel free to contact us via [linkedin](https://www.linkedin.com/company/diffy), [gitter](https://gitter.im/opendiffy/diffy) or [twitter](https://twitter.com/diffyproject).

## What is Diffy?

Diffy finds potential bugs in your service using running instances of your new code and your old
code side by side. Diffy behaves as a proxy and multicasts whatever requests it receives to each of
the running instances. It then compares the responses, and reports any regressions that may surface
from those comparisons. The premise for Diffy is that if two implementations of the service return
“similar” responses for a sufficiently large and diverse set of requests, then the two
implementations can be treated as equivalent and the newer implementation is regression-free.

## How does Diffy work?

Diffy acts as a proxy that accepts requests drawn from any source that you provide and multicasts
each of those requests to three different service instances:

1. A candidate instance running your new code
2. A primary instance running your last known-good code
3. A secondary instance running the same known-good code as the primary instance

As Diffy receives a request, it is multicast and sent to your candidate, primary, and secondary
instances. When those services send responses back, Diffy compares those responses and looks for two
things:

1. Raw differences observed between the candidate and primary instances.
2. Non-deterministic noise observed between the primary and secondary instances. Since both of these
   instances are running known-good code, you should expect responses to be in agreement. If not,
   your service may have non-deterministic behavior, which is to be expected.
![Diffy Topology](/images/diffy_topology.png)

Diffy measures how often primary and secondary disagree with each other vs. how often primary and
candidate disagree with each other. If these measurements are roughly the same, then Diffy
determines that there is nothing wrong and that the error can be ignored.

## How to get started?
# Running the example
The example.sh script included here builds and launches example servers as well as a diffy instance. Verify
that the following ports are available (9000, 9100, 9200, 8880, 8881, & 8888) and run `./example/run.sh start`.

Once your local Diffy instance is deployed, you send it a few requests by running `./example/traffic.sh`.

You can now go to your browser at
[http://localhost:8888](http://localhost:8888) to see what the differences across our example instances look like.

# Digging deeper
That was cool but now you want to compare old and new versions of your own service. Here’s how you can
start using Diffy to compare three instances of your service:

1. Deploy your old code to `localhost:9990`. This is your primary.
2. Deploy your old code to `localhost:9991`. This is your secondary.
3. Deploy your new code to `localhost:9992`. This is your candidate.
4. Download the latest Diffy binary from maven central or build your own from the code using `./sbt assembly`.
5. Run the Diffy jar with following command line arguments:

    ```
    java -jar diffy-server.jar \
    -candidate=localhost:9992 \
    -master.primary=localhost:9990 \
    -master.secondary=localhost:9991 \
    -service.protocol=http \
    -serviceName=My-Service \
    -proxy.port=:8880 \
    -admin.port=:8881 \
    -http.port=:8888 \
    -rootUrl="localhost:8888" \
    -summary.email="info@diffy.ai" \
    -summary.delay="5"
    ```

6. Send a few test requests to your Diffy instance on its proxy port:

    ```
    curl localhost:8880/your/application/route?with=queryparams
    ```

7. Watch the differences show up in your browser at [http://localhost:8888](http://localhost:8888).

8. Note that after ```summary.delay``` minutes, your Diffy instance will email a summary report to your ```summary.email``` address.
## Using Diffy with Docker

You can pull the [official docker image](https://hub.docker.com/r/diffy/) with `docker pull diffy/diffy`

And run it with
```
docker run -d --name diffy-01 \
  -p 8880:8880 -p 8881:8881 -p 8888:8888 \
  diffy/diffy \
    -candidate=localhost:9992 \
    -master.primary=localhost:9990 \
    -master.secondary=localhost:9991 \
    -service.protocol=http \
    -serviceName="Test-Service" \
    -proxy.port=:8880 \
    -admin.port=:8881 \
    -http.port=:8888 \
    -rootUrl=localhost:8888 \
    -summary.email="info@diffy.ai" \
    -summary.delay="5"
```

You should now be able to point to:
 - http://localhost:8888 to see the web interface
 - http://localhost:8881/admin for admin console
 - Use port 8880 to make the API requests

*NOTE*: You can  pull the [sample service](https://hub.docker.com/r/diffy/example-service/) and deploy the `production` (primary, secondary) and `candidate` tags to start playing with diffy right away.

You can always build the image from source with `docker build -t diffy .`


## FAQ's
   For safety reasons `POST`, `PUT`, ` DELETE ` are ignored by default . Add ` -allowHttpSideEffects=true ` to your command line arguments to enable these verbs.

## HTTPS
If you are trying to run Diffy over a HTTPS API, the config required is:

    -service.protocol=https

And in case of the HTTPS port be different than 443:

    -https.port=123

## License

    Copyright (C) 2019 Sn126, Inc.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program. If not, see https://www.gnu.org/licenses/.