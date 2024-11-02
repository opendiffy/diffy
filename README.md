# Diffy
[![Project status](https://img.shields.io/badge/status-active-brightgreen.svg)](#status)
[![Docker](https://img.shields.io/docker/pulls/diffy/diffy)](https://hub.docker.com/r/diffy/diffy)
[![Downloads](https://img.shields.io/github/downloads/opendiffy/diffy/total.svg)](https://github.com/opendiffy/diffy/releases/latest)
[![License: CC](https://img.shields.io/badge/License-CC%20BY%20NC%20ND-blue.svg)](https://creativecommons.org/licenses/by-nc-nd/4.0/legalcode)

[![Build status](https://github.com/opendiffy/diffy/actions/workflows/maven_macos_13.yml/badge.svg
)](https://github.com/opendiffy/diffy/actions/workflows/maven_macos_13.yml)
[![Build status](https://github.com/opendiffy/diffy/actions/workflows/maven_macos_12.yml/badge.svg
)](https://github.com/opendiffy/diffy/actions/workflows/maven_macos_12.yml)
[![Build status](https://github.com/opendiffy/diffy/actions/workflows/maven_macos_latest.yml/badge.svg
)](https://github.com/opendiffy/diffy/actions/workflows/maven_macos_latest.yml)

[![Build status](https://github.com/opendiffy/diffy/actions/workflows/maven_windows_latest.yml/badge.svg
)](https://github.com/opendiffy/diffy/actions/workflows/maven_windows_latest.yml)
[![Build status](https://github.com/opendiffy/diffy/actions/workflows/maven_windows_2022.yml/badge.svg
)](https://github.com/opendiffy/diffy/actions/workflows/maven_windows_2022.yml)
[![Build status](https://github.com/opendiffy/diffy/actions/workflows/maven_windows_2019.yml/badge.svg
)](https://github.com/opendiffy/diffy/actions/workflows/maven_windows_2019.yml)

[![Build status](https://github.com/opendiffy/diffy/actions/workflows/maven_ubuntu_latest.yml/badge.svg
)](https://github.com/opendiffy/diffy/actions/workflows/maven_ubuntu_latest.yml)

[!["Buy Me A Coffee"](https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png)](https://www.buymeacoffee.com/diffy)
## Status

Diffy is used in production at:
* [Mixpanel](https://engineering.mixpanel.com/safely-rewriting-mixpanels-highest-throughput-service-in-golang-mixpanel-engineering-62cd69b5ebdb)
* Airbnb [(Scalabity)](https://www.infoq.com/presentations/airbnb-services-scalability/) [(Migration)](https://www.infoq.com/presentations/airbnb-soa-migration/)
* [Twitter](https://blog.twitter.com/engineering/en_us/a/2015/diffy-testing-services-without-writing-tests.html)
* Baidu
* Bytedance

and blogged about by cloud infrastructure providers like:
* [Microsoft](https://microsoft.github.io/code-with-engineering-playbook/automated-testing/shadow-testing/)
* [Google](https://cloud.google.com/architecture/application-deployment-and-testing-strategies#shadow_test_pattern)
* [Alibaba Cloud](https://www.alibabacloud.com/blog/traffic-management-with-istio-3-traffic-comparison-analysis-based-on-istio_594545)
* [KrakenD](https://www.krakend.io/blog/migrate-aws-api-gateway-to-krakend/)
* [Datawire](https://blog.getambassador.io/next-level-testing-with-an-api-gateway-and-continuous-delivery-9cbb9c4564b5)

Visit our [engineering blog](https://content.sn126.com/blog) to keep up with the latest developments in Diffy.

Diffy is being actively developed and maintained by the engineering team at [Sn126](https://www.sn126.com).

Feel free to contact us via [discord](https://discord.gg/QEJRxgVfD8), [linkedin](https://www.linkedin.com/company/diffy), or [twitter](https://twitter.com/diffyproject).

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

## Documentation

Detailed Diffy Documentation is available [here](https://content.sn126.com/docs/diffy).

### Support
Please reach out to isotope@sn126.com for support. We look forward to hearing from you.

### Code of Conduct
1. Bug reports are welcome even if submitted anonymously via fresh github accounts.
2. Anonymous feature and support requests will be ignored.

## License

    Copyright (C) 2019 Sn126, Inc.

    This license allows reusers to copy and distribute the material in 
    any medium or format in unadapted form only, for noncommercial purposes 
    only, and only so long as attribution is given to the creator. 

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International Public License
    for more details.

    You should have received a copy of the Creative Commons Attribution-NonCommercial-NoDerivatives 4.0
    International Public License along with this program. If not, see 
    https://creativecommons.org/licenses/by-nc-nd/4.0/.
