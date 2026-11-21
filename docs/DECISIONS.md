# Decisions (ADRs)

## ADR-001: Spring Boot 4.2.0, Java 21, Gradle 9.9.2 for the whole repo

- **Context:** the repo was on Boot 3.4.1 with a Java 23 toolchain, but the local
  JDK is 21. New projects (actuator, OTel LGTM, MFA) should use current APIs.
  Security 7 also adds features we want to learn.
- **Decision:** upgrade the single multi-module build to Boot 4.2.0 (the latest
  release), with a Java 21 toolchain and Gradle 9.9.2.
- **Rejected:**
  - Keep 3.4.1: we would learn APIs that are already replaced.
  - A separate Boot 4 build only for new projects: two versions to maintain, and
    the old modules would rot.
- **Consequences:** every module needed starter renames (see `UPGRADE-BOOT-4.md`).
  A demo that runs only on JRE 23 is now skipped.

## ADR-002: New Boot 4 starter names, no "classic" compatibility starters

- **Decision:** switch to the modular starters (`webmvc`, `webclient`,
  `rabbitmq`, `security-oauth2-*`, `flyway`, `liquibase`, `h2console`) and their
  test starters.
- **Rejected:** keeping the deprecated names. They still resolve, but they hide
  which module provides which auto-configuration, and that is part of what we
  want to learn.

## ADR-003: Migrate code to Jackson 3

- **Decision:** move imports to `tools.jackson.databind` and use the Jackson 3
  converters for RabbitMQ and Kafka.
- **Rejected:** adding Jackson 2 back for compatibility. That means two JSON
  stacks, and Boot 4 configures only Jackson 3.
- **Consequence:** `FAIL_ON_NULL_FOR_PRIMITIVES` is on, so DTOs with optional
  numbers use wrapper types.

## ADR-004: observe1 keeps Zipkin format, via Brave

- **Context:** Boot 4 no longer manages `opentelemetry-exporter-zipkin`.
- **Decision:** use `spring-boot-starter-zipkin` (Brave bridge), which keeps
  observe1 sending Zipkin spans to Tempo.
- **Rejected:**
  - Pinning the OTel Zipkin exporter by hand: an unmanaged version, and OTel
    no longer recommends it.
  - Switching observe1 to OTLP: that is the subject of `observe3-otel-lgtm`.

## ADR-005: learn1 sends raw RedisTimeSeries commands

- **Context:** `lettucemod` needs Lettuce 6, Spring Data Redis 4 needs Lettuce 7,
  and Lettuce 7 has no TimeSeries API.
- **Decision:** send `TS.*` commands through `RedisCallback` and map replies into a
  small `Sample` class.
- **Rejected:**
  - Pinning Lettuce 6: fails with `NoClassDefFoundError: io.lettuce.core.DriverInfo`.
  - Dropping the TimeSeries example.
- **Consequence:** we see the raw Redis protocol replies, which is a learning
  point of its own.

## ADR-006: Authorization server keeps the default PKCE requirement

- **Context:** Security 7 requires PKCE for the authorization-code grant by default.
- **Decision:** keep the default. The Spring OAuth2 client already sends
  `code_challenge`, and the docs show how to add it to manual curl flows.
- **Rejected:** setting `requireProofKey(false)`. It weakens the flow just to keep
  old curl commands working.
