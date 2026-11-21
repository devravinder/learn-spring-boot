# Tasks

One commit per task, made once its checks pass.

| #   | Task                                                 | Status  |
| --- | ---------------------------------------------------- | ------- |
| 0   | Upgrade the repo to Spring Boot 4.2.0 + Java 21      | done    |
| 1   | `learn11-actuator`: custom metrics                   | planned |
| 2   | `learn11-actuator`: advanced configuration           | planned |
| 3   | `observe3-otel-lgtm`: Micrometer + OTLP to otel-lgtm | planned |
| 4   | `observe3-otel-lgtm`: OpenTelemetry Java agent       | planned |
| 5   | MFA series under `learn8-security` (all local)       | planned |

## Task 0: Boot 4 upgrade

- [x] Boot 4.2.0, Security 7.2.0, Java 21 toolchain, Gradle 9.9.2
- [x] Starter renames, Jackson 3, Testcontainers 2, Security 7 auth server
- [x] Renamed properties fixed (properties migrator run on every app)
- [x] `observe1` tracing via Brave + Zipkin, `learn1` TimeSeries via raw commands
- [x] `clean build` green; runtime smoke checks on security, DB and OAuth2 modules
- [ ] Runtime check of `learn1` TimeSeries against `redis-stack` (image not available yet)

Details: [UPGRADE-BOOT-4.md](UPGRADE-BOOT-4.md). Decisions: [DECISIONS.md](DECISIONS.md).
