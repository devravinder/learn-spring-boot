# learn11-actuator

Spring Boot Actuator in depth: custom business metrics, then the production
configuration around them.

| Module                     | Port         | Learn                                                                       |
| -------------------------- | ------------ | --------------------------------------------------------------------------- |
| `actuator1-custom-metrics` | 8101         | every Micrometer meter type, annotations, `MeterFilter`, cardinality limits |
| `actuator2-advanced-config` | 8102 / 9102 | exposure, security, health groups, probes, custom endpoints, runtime log levels |

- How to run: [docs/HOW-TO-RUN.md](docs/HOW-TO-RUN.md)
- Architecture: [docs/architecture.md](docs/architecture.md)
- Decisions: [docs/DECISIONS.md](docs/DECISIONS.md)
- Requests: [http/](http/)

```mermaid
flowchart LR
  code[Business code] -->|Counter, Timer, Gauge| registry[(MeterRegistry)]
  ann["@Timed / @Counted / @Observed"] --> registry
  binder[MeterBinder] --> registry
  filter[MeterFilter] -.->|deny, rename, cap tags| registry
  registry --> prom["/actuator/prometheus"]
  registry --> metrics["/actuator/metrics"]
  prom --> scraper[Prometheus or Grafana]
```

## actuator1-custom-metrics

A small shop. Every meter type is attached to a real action.

| Meter                    | Type                         | Where                   | Why this type                                  |
| ------------------------ | ---------------------------- | ----------------------- | ---------------------------------------------- |
| `shop.orders.placed`     | `Counter` with `payment` tag | `OrderService.place`    | counts that only go up                         |
| `shop.orders.failed`     | `@Counted(recordFailuresOnly)` | `OrderService.place`  | failures, tagged with the exception            |
| `shop.order.amount`      | `DistributionSummary` + SLO  | `OrderService.place`    | sizes, not times; buckets for 500 / 1000 / 5000 |
| `shop.order.processing`  | `Timer` + client percentiles | `OrderService.place`    | latency with p50 / p95 / p99                   |
| `shop.order.lookup`      | `@Timed`                     | `OrderService.find`     | latency without touching the code              |
| `shop.inventory.stock`   | `Gauge` per product          | `InventoryMetrics`      | a current value that goes up and down          |
| `shop.inventory.restocks` | `FunctionCounter`           | `InventoryMetrics`      | reads a counter the domain already keeps       |
| `shop.report.generation` | `LongTaskTimer`              | `ReportService`         | work still running (active count, duration)    |
| `shop.recommendations`   | `@Observed`                  | `RecommendationService` | one Observation, a timer now, a span later     |
| `shop.visits`            | `Counter` with `user` tag    | `CardinalityController` | anti-pattern, capped by a `MeterFilter`        |

Things to notice:

- `management.metrics.tags.*` adds `application` and `region` to every meter.
- `MeterFilter.renameTag` turns `payment` into `payment.method` without
  changing the code that records it.
- `MeterFilter.maximumAllowableTags` stops `shop.visits` at 20 series. A user id
  as a tag is the classic way to overload Prometheus.
- Client-side percentiles (`publishPercentiles`) can't be combined across
  instances. Histograms (`serviceLevelObjectives`, `percentiles-histogram`) can,
  using `histogram_quantile` in PromQL.
- In Prometheus output, a `LongTaskTimer` shows `_count` as the number of active
  tasks and `_sum` as their running time.

## actuator2-advanced-config

Actuator configured the way you would in production.

| Topic             | Setting / code                                                        | Effect                                               |
| ----------------- | --------------------------------------------------------------------- | ---------------------------------------------------- |
| Separate port     | `management.server.port: 9102`                                        | `/manage/**` is not reachable on the app port 8102   |
| Base path         | `management.endpoints.web.base-path: /manage`                         | `/manage/health` instead of `/actuator/health`       |
| Exposure          | `include: "*"`, `exclude: heapdump`                                   | all endpoints except the heap dump                   |
| Access levels     | `endpoints.access.default: read-only`, opt-in `unrestricted`          | only `loggers`, `features` and `shutdown` accept writes |
| Security          | `SecurityConfig` with `EndpointRequest`                               | `health` and `info` public; the rest need role `ACTUATOR` |
| Health details    | `show-details: when-authorized`                                       | `ops` sees components, anonymous users see only `UP` |
| Custom health     | `PaymentGatewayHealthIndicator`                                       | `paymentGateway` component                           |
| Probes            | `probes.enabled`, `add-additional-paths`                              | `/livez` and `/readyz` on the app port               |
| Readiness group   | `group.readiness.include: readinessState,paymentGateway`              | a down dependency takes the pod out of the load balancer |
| Availability      | `AvailabilityChangeEvent` in `OpsController`                          | maintenance mode: readiness 503, liveness 200        |
| Info              | `buildInfo()`, `info.env`, `info.java`, `InfoContributor`             | version, team, JVM, OS and feature counts            |
| Custom endpoint   | `FeaturesEndpoint` with read, write and delete operations             | feature flags at `/manage/features`                  |
| Runtime log level | `POST /manage/loggers/com.paravar.ops`                                | DEBUG lines appear with no restart                   |
| Secrets           | `show-values: when-authorized` + `SanitizingFunction`                 | `*token*` keys are always masked                     |
| Startup           | `BufferingApplicationStartup`                                         | `/manage/startup` lists the slow startup steps       |
| HTTP exchanges    | `InMemoryHttpExchangeRepository`                                      | last 50 requests at `/manage/httpexchanges`          |
| Response cache    | `endpoint.beans.cache.time-to-live: 10s`                              | repeated `/beans` calls are served from cache        |
| CORS              | `endpoints.web.cors.*`                                                | a dashboard on `localhost:3000` can call it          |
| Shutdown          | `endpoint.shutdown.access: unrestricted`                              | `POST /manage/shutdown` as `ops` stops the app       |

Users: `ops / ops` (role `ACTUATOR`) and `dev / dev` (role `USER`).
