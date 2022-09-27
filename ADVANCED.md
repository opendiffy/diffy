# Advanced Deployment Configurations
## Observability + Diffy
The data produced by Diffy can provide valuable insights and make them easily accessible within your existing
observability infrastructure. [OpenTelemetry](https://opentelemetry.io) provides a standardized mechanism for Diffy to leverage your
observability stack to provide useful logs, metrics, and traces.

Diffy's bundled docker-compose script gives you a taste of these capabilities by leveraging:
 - [Grafana](https://grafana.com/) for visualization
 - [Loki](https://grafana.com/oss/loki/) for logs
 - [Prometheus](https://prometheus.io/) for metrics
 - [Tempo](https://grafana.com/oss/tempo/) and [Jaeger](https://www.jaegertracing.io/) for tracing (you only need one but this example shows you both)

Run ```docker-compose up``` with this [docker-compose.yml](/docker-compose.yml) configuration and then send some traffic to your deployed topology:
```
curl -s -i -H Canonical-Resource : endpoint-test http://localhost:8880/success?value=happy-tester
curl -s -i -H Canonical-Resource : endpoint-test http://localhost:8880/noise?value=happy-tester
curl -s -i -H Canonical-Resource : endpoint-test http://localhost:8880/regression?value=happy-tester
curl -s -i -H Canonical-Resource : endpoint-test http://localhost:8880/noisy_regression?value=happy-tester
```

You can now access live results as follows:
 - [Diffy](http://localhost:8888)
 - [Grafana Dashboards](http://localhost:3000/explore)
   - Loki (logs)
   - Tempo (tracing)
   - Prometheus (metrics)
 
You can also access [Jaeger](http://localhost:16686/search) and [Prometheus](http://localhost:9090/graph) directly.

### Credit
The above integration is largely based on examples shared on the [OpenTelemetry Java Instrumentation](https://opentelemetry.io/docs/instrumentation/java/examples/) website.