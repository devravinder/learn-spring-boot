# learn11-actuator architecture

## actuator1-custom-metrics

```mermaid
flowchart TB
  subgraph web[Controllers]
    oc[OrderController]
    ic[InventoryController]
    rc[ReportController]
    cc[CardinalityController]
  end
  subgraph domain[Services]
    os[OrderService]
    inv[Inventory]
    rs[ReportService]
    rec[RecommendationService]
  end
  oc --> os
  ic --> inv
  rc --> rs
  rc --> rec
  os -->|Counter, Summary, Timer| reg[(MeterRegistry)]
  rs -->|LongTaskTimer| reg
  rec -->|"@Observed"| obs[ObservationRegistry] --> reg
  inv -.->|read on scrape| binder[InventoryMetrics MeterBinder] --> reg
  cc -->|user tag| reg
  filters[MetricsConfig MeterFilters] -.-> reg
  reg --> prom["/actuator/prometheus"]
```

How a request becomes a metric:

```mermaid
sequenceDiagram
  participant C as Client
  participant OC as OrderController
  participant OS as OrderService (proxy)
  participant R as MeterRegistry
  participant P as Prometheus
  C->>OC: POST /orders
  OC->>OS: place(request)
  Note over OS: CountedAspect wraps the call
  OS->>R: Timer.Sample start
  OS->>R: shop.orders.placed +1, amount recorded
  OS->>R: sample.stop(shop.order.processing)
  OS-->>OC: Order
  P->>R: GET /actuator/prometheus (scrape)
  R-->>P: text exposition format
```

## actuator2-advanced-config

Two ports, two security chains:

```mermaid
flowchart LR
  user[Browser or API client] -->|":8102 /api/**"| app[App servlet context]
  lb[Load balancer / Kubernetes] -->|":8102 /livez /readyz"| app
  ops[Ops tooling] -->|":9102 /manage/**"| mgmt[Management context]
  subgraph security[SecurityFilterChains]
    c1["Order 1: EndpointRequest.toAnyEndpoint()<br/>health, info public<br/>others need ACTUATOR"]
    c2["Order 2: everything else, permitAll"]
  end
  mgmt --> c1
  app --> c2
```

Readiness vs liveness:

```mermaid
stateDiagram-v2
  [*] --> Accepting: app started
  Accepting --> Refusing: POST /api/maintenance/on
  Accepting --> Refusing: paymentGateway DOWN
  Refusing --> Accepting: maintenance off / gateway up
  note right of Refusing
    /readyz returns 503, so no new traffic is sent
    /livez stays 200, so the pod is not restarted
  end note
```

Custom endpoint operations map to HTTP methods:

```mermaid
flowchart LR
  r["@ReadOperation"] --> get["GET /manage/features[/name]"]
  w["@WriteOperation"] --> post["POST /manage/features/name"]
  d["@DeleteOperation"] --> del["DELETE /manage/features/name"]
```
