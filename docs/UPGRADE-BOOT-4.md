# Upgrade: Spring Boot 3.4.1 → 4.2.0

What changed when this repo moved to Spring Boot 4.2.0, Spring Security 7.2,
Java 21 and Gradle 9.9.2, and why each change was needed.

## Versions

| Item                 | Before | After  |
| -------------------- | ------ | ------ |
| Spring Boot          | 3.4.1  | 4.2.0  |
| Spring Security      | 6.4    | 7.2.0  |
| Java toolchain       | 23     | 21     |
| Gradle wrapper       | 9.1.0  | 9.9.2  |
| Jackson              | 2.x    | 3.x    |
| Testcontainers       | 1.20   | 2.0.5  |
| Liquibase            | 4.30.0 | 5.x    |
| Lettuce (Redis)      | 6.x    | 7.8.0  |

## 1. Boot 4 is modular: use the starter, not the bare library

Boot 4 split auto-configuration into one module per technology. A bare library
on the classpath no longer switches anything on.

| Before (Boot 3)                                    | After (Boot 4)                                       | Why                                         |
| -------------------------------------------------- | ---------------------------------------------------- | ------------------------------------------- |
| `spring-boot-starter-web`                          | `spring-boot-starter-webmvc`                         | renamed; the old name is deprecated         |
| `spring-webflux` (for `WebClient`)                 | `spring-boot-starter-webclient`                      | `WebClient.Builder` bean + trace propagation |
| `spring-boot-starter-amqp`                         | `spring-boot-starter-rabbitmq`                       | `amqp` no longer brings `spring-rabbit`     |
| `spring-kafka`                                     | `spring-boot-starter-kafka`                          | Kafka auto-config moved to `spring-boot-kafka` |
| `flyway-core` / `liquibase-core`                   | `spring-boot-starter-flyway` / `-liquibase`          | migrations would silently not run           |
| `spring-boot-starter-oauth2-client`                | `spring-boot-starter-security-oauth2-client`         | renamed                                     |
| `spring-boot-starter-oauth2-resource-server`       | `spring-boot-starter-security-oauth2-resource-server` | renamed                                    |
| `spring-security-oauth2-authorization-server`      | `spring-boot-starter-security-oauth2-authorization-server` | the server now lives in Security 7     |
| H2 console via `spring.h2.console.*` only          | add `spring-boot-h2console`                          | console auto-config is its own module       |
| `spring-rabbit-test` / `spring-kafka-test`         | `spring-boot-starter-rabbitmq-test` / `-kafka-test`  | matching test starters                      |
| MockMvc / `@DataJpaTest` from `starter-test`       | `spring-boot-starter-webmvc-test` / `-data-jpa-test` | test slices are per module too              |

Moved classes:

| Class                         | Old package                                          | New package                                               |
| ----------------------------- | ---------------------------------------------------- | --------------------------------------------------------- |
| `JpaProperties`               | `org.springframework.boot.autoconfigure.orm.jpa`     | `org.springframework.boot.jpa.autoconfigure`              |
| `EntityManagerFactoryBuilder` | `org.springframework.boot.orm.jpa`                   | `org.springframework.boot.jpa`                            |
| `AutoConfigureMockMvc`        | `org.springframework.boot.test.autoconfigure.web.servlet` | `org.springframework.boot.webmvc.test.autoconfigure` |
| `DataJpaTest`                 | `org.springframework.boot.test.autoconfigure.orm.jpa` | `org.springframework.boot.data.jpa.test.autoconfigure`   |

`EntityManagerFactoryBuilder` now takes a `Function<DataSource, Map>` for JPA
properties instead of a `Map` (`learn5-multiple-dbs`).

## 2. Jackson 3

Boot 4 uses Jackson 3 by default.

- Package `com.fasterxml.jackson.databind` → `tools.jackson.databind`
  (`ObjectMapper`, `JsonNode`). Annotations stay in `com.fasterxml.jackson.annotation`.
- `FAIL_ON_NULL_FOR_PRIMITIVES` is on by default. A JSON body without
  `followers` can no longer bind to `int followers`. We switched to `Integer` /
  `Boolean` in the WireMock and MockServer `GitHubUser` (`testing8`, `testing9`).
- RabbitMQ: `Jackson2JsonMessageConverter` → `JacksonJsonMessageConverter`, which
  takes a Jackson 3 `JsonMapper`.
- Kafka: `JsonSerializer` / `JsonDeserializer` → `JacksonJsonSerializer` /
  `JacksonJsonDeserializer`. An app without the web starter must add
  `spring-boot-starter-jackson` (`testing6`).

## 3. Spring Security 7

- The authorization server is now part of Spring Security:
  `org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer`.
- The static `OAuth2AuthorizationServerConfigurer.authorizationServer()` was
  removed. Use `new OAuth2AuthorizationServerConfigurer()` or
  `http.oauth2AuthorizationServer(...)`.
- **PKCE is required by default** for the authorization-code grant. A request
  without `code_challenge` is rejected with `invalid_request`. The Spring OAuth2
  client (`auth-client`) sends `code_challenge` + `S256` automatically. Plain
  curl tests must add it (see `HOW-TO-RUN.md`).

## 4. Renamed properties

Found by running every app with `spring-boot-properties-migrator`.

| Old key                              | New key                                        | Module   |
| ------------------------------------ | ---------------------------------------------- | -------- |
| `management.tracing.enabled`         | `management.tracing.export.enabled`            | observe1 |
| `management.zipkin.tracing.endpoint` | `management.tracing.export.zipkin.endpoint`    | observe1 |
| `server.servlet.encoding.*`          | `spring.servlet.encoding.*`                    | jwt6     |

## 5. Libraries that had to change

- **observe1 tracing:** Boot 4 no longer manages `opentelemetry-exporter-zipkin`.
  We use `spring-boot-starter-zipkin` (Brave bridge) and still send Zipkin spans
  to Tempo. OTLP gets its own project (`observe3-otel-lgtm`).
- **learn1 Redis TimeSeries:** `lettucemod` is built on Lettuce 6 and crashes on
  Lettuce 7 (`bfInfo` clash). Spring Data Redis 4 needs Lettuce 7, so we can't pin
  Lettuce 6 either. `RedisTimeSeriesService` now sends raw `TS.CREATE`, `TS.ADD`,
  `TS.RANGE` and `TS.CREATERULE` commands through `RedisCallback`.
- **Testcontainers 2:** artifacts are `testcontainers-<module>`, and container
  classes moved to `org.testcontainers.<module>` (for example
  `org.testcontainers.postgresql.PostgreSQLContainer`, no longer generic). The
  old Testcontainers could not talk to the current Docker Desktop API, so
  `testing5`, `testing6`, `testing7` and `testing9` now pass where they failed before.
- **rest-assured** is no longer managed by Boot, so it's pinned to `6.3.0`.

## 6. Fixed along the way (not caused by the upgrade)

- `learn5`: `HsqlDbConfig.PACKAGE_NAME` was `com.paravar.employees`, but the package
  is `com.paravar.employee`, so `EmployeeRepository` was never found.
- `testing3`: test classes run in parallel and shared a `HashMap`, which randomly
  threw `ConcurrentModificationException`. It's a `ConcurrentHashMap` now.

## How it was verified

- `./gradlew clean build`: everything compiles. 44 tests pass, 4 conditional
  demos are skipped, and none fail.
- Every app was started once with the properties migrator. 47 of 57 start
  without infrastructure. The rest need Postgres, Kafka or HSQLDB, or are
  test-only modules.
- Runtime smoke tests: 39 HTTP checks, plus three manual runs (the last three items below).
  - basic2/5/6 (form, basic, custom user).
  - jwt1/3/5/7/8 (tokens, scopes, roles, hierarchy, JSON errors).
  - auth3 (discovery, client credentials, resource server).
  - auth5 (full code + PKCE + consent + refresh).
  - auth-client sends PKCE.
  - learn6 Liquibase 5 on Postgres.
  - learn5 writing to both Postgres and HSQLDB.
- Not verified at runtime: learn1's TimeSeries endpoint. It needs a
  `redis-stack` image, which wasn't available locally.
