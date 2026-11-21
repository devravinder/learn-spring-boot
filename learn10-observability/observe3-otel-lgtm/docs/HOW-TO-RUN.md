# How to run observe3-otel-lgtm

## 1. Start the LGTM container

```bash
docker compose -f learn10-observability/observe3-otel-lgtm/infra/docker-compose.yaml up -d
```

Wait until <http://localhost:3000> opens (admin / admin). It uses the same
Grafana port as `observe1`, so run only one of them at a time.

## 2. Start the services (repo root, two terminals)

```bash
./gradlew :learn10-observability:observe3-otel-lgtm:product-service:bootRun
./gradlew :learn10-observability:observe3-otel-lgtm:order-service:bootRun
```

## 3. Send traffic

```bash
for p in p1 p2 slow broken; do
  curl -s -X POST localhost:8111/orders -H 'customer-id: c-42' \
    -H 'Content-Type: application/json' -d "{\"productId\":\"$p\",\"quantity\":2}"; echo
done
```

Each response includes a `traceId`. Metrics are pushed every 10 s.

## 4. Look around in Grafana

| Where                                      | What to try                                                  |
| ------------------------------------------ | ------------------------------------------------------------ |
| Dashboards → `Spring services (OTLP)`      | rate, p95 latency, 5xx, orders per product, heap, error logs |
| Explore → Tempo → TraceQL `{ trace:id = "<traceId>" }` | `order-service` → `product-service` spans, plus `order.pricing` |
| Explore → Tempo → `{ span.customer-id = "c-42" }` | traces found by baggage                              |
| Explore → Loki → `{service_name="product-service"} \| trace_id="<traceId>"` | product logs for one order, with `customer_id` |
| Explore → Prometheus → `orders_created_total` | one series per product                                   |
| Dashboards → `JVM Metrics`, `RED Metrics`  | built-in otel-lgtm dashboards                                |

From a log line, open the trace with the `trace_id` link. From a trace span,
use "Logs for this span".

## 5. Stop

Stop the apps with Ctrl+C, then:

```bash
docker compose -f learn10-observability/observe3-otel-lgtm/infra/docker-compose.yaml down
```
