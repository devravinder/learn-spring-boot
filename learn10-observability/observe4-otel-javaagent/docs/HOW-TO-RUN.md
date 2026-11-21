# How to run observe4-otel-javaagent

## 1. Start the LGTM container

Stop `observe3` first if it's running, because they share the same ports.

```bash
docker compose -f learn10-observability/observe4-otel-javaagent/infra/docker-compose.yaml up -d
```

## 2. Start the services with the agent

`bootRun` downloads the agent (Gradle configuration `otelAgent`), copies it to
`build/otel/` and adds the `-javaagent` and `-Dotel.*` flags (see `agent.gradle`).

```bash
./gradlew :learn10-observability:observe4-otel-javaagent:product-service:bootRun
./gradlew :learn10-observability:observe4-otel-javaagent:order-service:bootRun
```

The first log line should be
`opentelemetry-javaagent - version: 2.34.0`.

To run the jar without Gradle:

```bash
./gradlew :learn10-observability:observe4-otel-javaagent:order-service:bootJar
java -javaagent:learn10-observability/observe4-otel-javaagent/order-service/build/otel/opentelemetry-javaagent.jar \
  -Dotel.service.name=order-service-agent \
  -Dotel.exporter.otlp.endpoint=http://localhost:4318 \
  -Dotel.metric.export.interval=10000 \
  -jar learn10-observability/observe4-otel-javaagent/order-service/build/libs/order-service-0.0.1-SNAPSHOT.jar
```

Every `-Dotel.x.y` flag can also be set as an `OTEL_X_Y` environment variable,
which is the usual choice in containers.

## 3. Send traffic

```bash
for p in p1 p2 slow broken; do
  curl -s -X POST localhost:8113/orders -H 'baggage: customer-id=c-42' \
    -H 'Content-Type: application/json' -d "{\"productId\":\"$p\",\"quantity\":2}"; echo
done
```

## 4. Look around

| Where                                       | What to see                                              |
| ------------------------------------------- | -------------------------------------------------------- |
| Dashboards → `Spring services (OTel Java agent)` | the same panels as observe3, built on semantic-convention names |
| Dashboards → `RED Metrics (classic histogram)` | built-in otel-lgtm dashboard; works as-is with agent metrics |
| Tempo → `{ resource.service.name = "order-service-agent" }` | server, client and `order.pricing` (`@WithSpan`) spans |
| Loki → `{service_name="product-service-agent"}` | logs captured by the agent, with `trace_id`          |
| Prometheus → `http_server_request_duration_seconds_count` | labels `http_route`, `http_response_status_code` |

## 5. Stop

```bash
docker compose -f learn10-observability/observe4-otel-javaagent/infra/docker-compose.yaml down
```
