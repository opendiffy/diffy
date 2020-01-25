# Diffy

[![Build status](https://img.shields.io/travis/opendiffy/diffy/master.svg)](https://travis-ci.org/opendiffy/diffy)
[![Coverage status](https://img.shields.io/codecov/c/github/opendiffy/diffy/master.svg)](https://codecov.io/github/opendiffy/diffy)
[![Project status](https://img.shields.io/badge/status-active-brightgreen.svg)](#status)
[![Gitter](https://img.shields.io/badge/gitter-join%20chat-green.svg)](https://gitter.im/opendiffy/diffy)
[![Docker](https://img.shields.io/docker/pulls/diffy/diffy)](https://hub.docker.com/r/diffy/diffy)
[![Downloads](https://img.shields.io/github/downloads/opendiffy/diffy/total.svg)](https://github.com/opendiffy/diffy/releases/latest)
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

## Getting started

If you are new to Diffy, please refer to our [Quickstart](/QUICKSTART.md) guide.

## Upgrade to Isotope
1. Login to [isotope](https://isotope.sn126.com).
2. Click on [services](http://isotope.sn126.com/services) tab to create the service you want to test.
3. Download the resulting `local.isotope` file.
4. Deploy Diffy with the `isotope.config` flag pointing to the location of `local.isotope`:
    ```bash
        java -jar ./target/scala-2.12/diffy-server.jar \
        -candidate='localhost:9200' \
        -master.primary='localhost:9000' \
        -master.secondary='localhost:9100' \
        -service.protocol='http' \
        -serviceName='ExampleService' \
        -summary.delay='1' \
        -summary.email='isotope@diffy.ai' \
        -proxy.port=:8880 \
        -admin.port=:8881 \
        -http.port=:8888 \
        -candidateApiRoot='api/v4/?param1=value1&param2=value2' \
        -primaryApiRoot='api/v3/' \
        -secondaryApiRoot='api/v3/' \
        -isotope.config='/path/to/local.isotope'
    ```
5. Send some traffic to your deployed Diffy instance.
6. Go the [analysis](http://isotope.sn126.com/analysis) tab to see your latest and historical results.

### Support
Please reach out to isotope@sn126.com for support. We look forward to hearing from you.


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