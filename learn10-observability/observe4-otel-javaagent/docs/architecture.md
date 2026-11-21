# observe4-otel-javaagent architecture

## How the agent hooks in

```mermaid
sequenceDiagram
  participant JVM
  participant A as OTel agent (premain)
  participant App as Spring Boot app
  participant L as otel-lgtm
  JVM->>A: load -javaagent before main()
  A->>A: install bytecode transformers, start SDK
  JVM->>App: main()
  Note over App: Tomcat, DispatcherServlet, RestClient, Logback classes are transformed when they load
  App->>App: handle POST /orders
  Note over App: injected code opens spans, records metrics, copies logs
  App-)L: OTLP traces, metrics, logs
```

## Code using the OTel API

```mermaid
flowchart LR
  ctrl[OrderController] -->|"Span.current()"| api[opentelemetry-api in the app]
  ctrl -->|"LongCounter.add()"| api
  pricing["PricingService @WithSpan"] --> api
  api -.->|agent replaces the no-op implementation| sdk[Agent SDK]
  sdk --> out((OTLP))
```

Without the agent, the same jar still runs. `GlobalOpenTelemetry` falls back
to a no-op implementation, so the calls cost nothing and export nothing.
