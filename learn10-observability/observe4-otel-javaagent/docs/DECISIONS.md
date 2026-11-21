# observe4-otel-javaagent decisions

## ADR-001: Agent pulled as a Gradle dependency

- Decision: a custom `otelAgent` configuration resolves
  `io.opentelemetry.javaagent:opentelemetry-javaagent:2.34.0`, and a copy
  task puts it in `build/otel/`.
- Why: the version is pinned in the build, and nobody downloads jars by hand.
- Rejected: committing the jar (about 20 MB in git) and download scripts.

## ADR-002: Config through `-Dotel.*` flags in `bootRun`

- Decision: the service name, endpoint and export interval are JVM flags in `agent.gradle`.
- Why: the agent starts before Spring and can't read `application.yaml`.
- Rejected: Spring properties, which the agent never sees.

## ADR-003: No Micrometer or OTel starter in these services

- Decision: only `opentelemetry-api` and the annotations artifact.
- Why: two SDKs in one JVM give duplicate spans and confusing metrics.
- Rejected: agent plus `spring-boot-starter-opentelemetry` together.

## ADR-004: Service names end in `-agent`

- Decision: `order-service-agent` and `product-service-agent`.
- Why: observe3 and observe4 can share a backend, and their series and traces
  stay apart.
