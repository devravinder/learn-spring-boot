# Tasks

One commit per task, made once its checks pass.

| #   | Task                                                        | Status  |
| --- | ----------------------------------------------------------- | ------- |
| 0   | Upgrade the repo to Spring Boot 4.2.0 + Java 21             | done    |
| 1   | `learn11-actuator/actuator1-custom-metrics`                 | done    |
| 2   | `learn11-actuator/actuator2-advanced-config`                | done    |
| 3   | `learn10-observability/observe3-otel-lgtm` (Micrometer + OTLP) | done    |
| 4   | `learn10-observability/observe4-otel-javaagent`             | done    |
| 5   | `learn8-security/mfa/mfa1-totp`                             | done    |
| 6   | `learn8-security/mfa/mfa2-email-ott`                        | done    |
| 7   | `learn8-security/mfa/mfa3-passkeys`                         | done    |
| 8   | `learn8-security/mfa/mfa4-step-up`                          | done    |
| 9   | `learn8-security/mfa/mfa5-keycloak-otp`                     | done    |
| 10  | `learn12-resilience`: flaky service + Spring core retry     | done    |
| 11  | `learn12-resilience/resilience2-resilience4j`               | done    |
| 12  | `learn12-resilience/resilience3-cap-theorem`                | done    |
| 13  | `learn13-rest-api/rest1-validation-problem-details`         | done    |
| 14  | `learn13-rest-api/rest2-api-versioning`                     | done    |
| 15  | `learn13-rest-api/rest3-http-interface-clients`             | done    |
| 16  | `learn13-rest-api/rest4-openapi`                            | done    |
| 17  | `learn14-data-jpa/data1-jpa-essentials` (incl. cursor pagination) | done    |
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

## Task 4: OpenTelemetry Java agent

- [x] Agent 2.34.0 resolved by Gradle and attached in `bootRun` with `-Dotel.*` flags
- [x] Zero-code traces, semantic-convention metrics and logs; `@WithSpan` and the OTel metrics API
- [x] Dashboard on semantic-convention names; comparison table against observe3
- [x] 17 end-to-end checks; observe3 rechecked with the regenerated dashboard

## Task 5: TOTP second factor

- [x] RFC 6238 TOTP and Base32 by hand; QR code drawn locally
- [x] `FACTOR_TOTP` as a Security 7 factor; `multiFactor()` rule; missing-factor entry point
- [x] Enrollment, encrypted secret, recovery codes, replay protection, lockout, trusted device, session rotation
- [x] 25 end-to-end checks with real CSRF form posts and computed codes

## Task 6: email one-time link

- [x] `@EnableMultiFactorAuthentication` with `FACTOR_PASSWORD` + `FACTOR_OTT`
- [x] `oneTimeTokenLogin()` with a mail sender to MailHog; resolver bound to the signed-in user
- [x] Single use, expiry, either factor order, link alone is not enough
- [x] 13 end-to-end checks reading the MailHog API

## Task 7: passkeys

- [x] `webAuthn()` with rpId, origin; opt-in MFA with `when = WEBAUTHN_REGISTERED`
- [x] Shared in-memory passkey repositories; either factor order; signature counter
- [x] 13 browser checks in Chrome with a CDP virtual authenticator

## Task 8: step-up authentication

- [x] `RequiredFactor.validDuration` on `/transfer/**`; `issuedAt` shown to the user
- [x] Expired factor redirect, re-auth returns to the saved request, other pages unaffected
- [x] 10 end-to-end checks with a 5 s window

## Task 9: MFA through Keycloak

- [x] Realm as code: client with PKCE, OTP policy, brute-force protection, `CONFIGURE_TOTP`
- [x] `oauth2Login()` from `issuer-uri`; RP-initiated logout
- [x] 13 browser checks: OTP setup, OTP login, wrong code, code reuse, logout

## Task 10: Spring core resilience

- [x] `flaky-service` with runtime chaos switches and downstream stats
- [x] Timeouts, `@Retryable` with backoff and jitter, 4xx not retried, `RetryTemplate` + listener
- [x] Idempotency key for a lost reply; `@ConcurrencyLimit` BLOCK and REJECT
- [x] 12 end-to-end checks against the downstream stats

## Task 11: Resilience4j

- [x] Circuit breaker states with half-open recovery, ignored 4xx, slow-call threshold
- [x] Retry with backoff and the fallback on the outer aspect; cached fallback
- [x] Rate limiter, semaphore bulkhead, time limiter with a thread-pool bulkhead
- [x] Actuator health, events and Prometheus metrics
- [x] 23 end-to-end checks against the downstream stats

## Task 12: CAP theorem

- [x] Three replicas, partitions and heal; CP quorum vs AP local writes
- [x] Lamport versions, last-write-wins with concurrent losers reported
- [x] Replication lag (eventual consistency), `R + W > N`, `W = N`, PACELC latency
- [x] 20 end-to-end checks

## Task 13: validation and problem details

- [x] Groups, custom and class-level constraints, nested `@Valid`, method validation, i18n messages
- [x] RFC 9457 for validation, domain, framework (400, 404, 405, 415) and unexpected (500) errors
- [x] `instance` filled by one advice; no internals leaked
- [x] 20 end-to-end checks

## Task 14: API versioning

- [x] Header and query strategies, supported list, default version
- [x] Exact (`2.0`) and baseline (`1.0+`, `1.1+`) mappings
- [x] Deprecation, Sunset and Link headers; 400 for unsupported versions
- [x] 13 end-to-end checks

## Task 15: HTTP interface clients

- [x] `@HttpExchange` GET, POST, DELETE, path, query, header, `ResponseEntity`; `@ImportHttpServices` group
- [x] Group base URL, timeouts, default header, API version insertion; status handler mapping 404
- [x] 11 end-to-end checks against a local upstream

## Task 16: OpenAPI

- [x] Info, tags, operations, problem+json responses, schemas from records and Bean Validation
- [x] Bearer security scheme per operation, `@Hidden`, public and admin groups, YAML, local Swagger UI
- [x] 20 checks against the generated document

## Task 17: JPA essentials and cursor pagination

- [x] N+1 (21 statements) vs JOIN FETCH and `@EntityGraph` (1); interface, DTO and dynamic projections
- [x] Offset `Page` (rows + count), `Slice` (no count), cursor `Window` (keyset, opaque token, `price, id` tie-breaker)
- [x] Insert while paging: offset repeats a row, cursor does not
- [x] Auditing, `@Version`; optimistic vs pessimistic vs atomic under 20 parallel requests
- [x] Rollback rules, `REQUIRES_NEW`, the checked-exception trap
- [x] 27 end-to-end checks with recorded SQL
