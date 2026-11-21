# observe3-otel-lgtm decisions

## ADR-001: `grafana/otel-lgtm` instead of four containers

- Decision: one container for collector, Prometheus, Tempo, Loki and Grafana,
  with the datasources and correlations already set up.
- Rejected: separate containers like `observe1`. Closer to production, but most
  of the effort goes into wiring, not into learning OTLP.
- Consequence: dev only. It has no persistence, and data is lost on `down`.

## ADR-002: Micrometer + OTLP through `spring-boot-starter-opentelemetry`

- Decision: keep the Micrometer and Observation API in code and export over OTLP.
- Rejected: the OpenTelemetry Java agent. It's covered in `observe4-otel-javaagent`
  so the two can be compared.

## ADR-003: Logs through the OTel Logback appender

- Decision: `OpenTelemetryAppender` in `logback-spring.xml`, installed into the
  Boot-managed SDK at startup.
- Why: log records carry `trace_id`, `span_id` and MDC attributes, with no regex
  parsing in Loki.
- Rejected: the Loki Logback appender (Loki-specific) and file tailing (needs an agent).

## ADR-004: Push metrics every 10 s

- Decision: `management.otlp.metrics.export.step: 10s`.
- Rejected: the default `1m`. It's cheaper, but the dashboard looks frozen while
  you test. Use `1m` or more in production.

## ADR-005: Expose Prometheus on 9090

- Decision: publish 9090 from the container so curl and scripts can query PromQL directly.
- Rejected: Grafana-only access, which is harder to debug when a panel is empty.
