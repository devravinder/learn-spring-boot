# Tasks

One commit per task, made once its checks pass.

| #   | Task                                                        | Status  |
| --- | ----------------------------------------------------------- | ------- |
| 0   | Upgrade the repo to Spring Boot 4.2.0 + Java 21             | done    |
| 1   | `learn11-actuator/actuator1-custom-metrics`                 | done    |
| 2   | `learn11-actuator/actuator2-advanced-config`                | done    |
| 3   | `learn10-observability/observe3-otel-lgtm` (Micrometer + OTLP) | done    |
| 4   | `learn10-observability/observe4-otel-javaagent`             | planned |
| 5   | `learn8-security/mfa/mfa1-totp`                             | planned |
| 6   | `learn8-security/mfa/mfa2-email-ott`                        | planned |
| 7   | `learn8-security/mfa/mfa3-passkeys`                         | planned |
| 8   | `learn8-security/mfa/mfa4-step-up`                          | planned |
| 9   | `learn8-security/mfa/mfa5-keycloak-otp`                     | planned |
| 10  | `learn12-resilience`: flaky service + Spring core retry     | planned |
| 11  | `learn12-resilience/resilience2-resilience4j`               | planned |
| 12  | `learn12-resilience/resilience3-cap-theorem`                | planned |
| 13  | `learn13-rest-api/rest1-validation-problem-details`         | planned |
| 14  | `learn13-rest-api/rest2-api-versioning`                     | planned |
| 15  | `learn13-rest-api/rest3-http-interface-clients`             | planned |
| 16  | `learn13-rest-api/rest4-openapi`                            | planned |
| 17  | `learn14-data-jpa/data1-jpa-essentials` (incl. cursor pagination) | planned |
| 18  | Docs with Mermaid diagrams for the existing projects        | planned |

## Task 0: Boot 4 upgrade

- [x] Boot 4.2.0, Security 7.2.0, Java 21 toolchain, Gradle 9.9.2
- [x] Starter renames, Jackson 3, Testcontainers 2, Security 7 auth server
- [x] Renamed properties fixed (properties migrator run on every app)
- [x] `observe1` tracing via Brave + Zipkin, `learn1` TimeSeries via raw commands
- [x] `clean build` green; runtime smoke checks on security, DB and OAuth2 modules
- [ ] Runtime check of `learn1` TimeSeries against a `redis-stack` image

Details: [UPGRADE-BOOT-4.md](UPGRADE-BOOT-4.md). Decisions: [DECISIONS.md](DECISIONS.md).

## Task 1: custom metrics

- [x] Counter, DistributionSummary with SLOs, Timer with percentiles, Gauge, FunctionCounter, LongTaskTimer
- [x] `@Timed`, `@Counted`, `@Observed` through `spring-boot-starter-aspectj`
- [x] `MeterBinder`, common tags, `MeterFilter` deny, rename and tag cap
- [x] 18 runtime checks against `/actuator/prometheus` and `/actuator/metrics`

Docs: [learn11-actuator](../learn11-actuator/README.md).

## Task 2: actuator advanced configuration

- [x] Separate management port and base path, exposure, read-only access by default
- [x] `EndpointRequest` security chain, health details by role, CORS
- [x] Custom `HealthIndicator`, readiness group, probes on the app port, maintenance mode
- [x] Build, env, java, os and custom info; custom `@Endpoint` with read, write and delete
- [x] Runtime log levels, startup timeline, HTTP exchanges, masked secrets, shutdown
- [x] 30 runtime checks; no renamed or deprecated keys reported by the properties migrator

## Task 3: OTLP to otel-lgtm

- [x] `spring-boot-starter-opentelemetry`: metrics, traces and logs over OTLP HTTP
- [x] Trace across two services, baggage as header, span tag, MDC and log attribute
- [x] Logs in Loki with `trace_id`; error logs; manual `Observation` span + timer
- [x] Provisioned dashboard; every panel query checked against Prometheus
- [x] 17 end-to-end checks against Prometheus, Tempo and Loki
