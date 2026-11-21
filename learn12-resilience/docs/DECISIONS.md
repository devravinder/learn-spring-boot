# learn12-resilience decisions

## ADR-001: One chaos-controlled downstream for every module

- Decision: `flaky-service` with `failRate`, `failNext`, `latencyMs` and
  `status`, changed at runtime, plus stats (`calls`, `maxInFlight`, `charges`).
- Why: each pattern can be shown with an exact, repeatable failure.
- Rejected: random failures inside each app. They're not repeatable and can't
  show concurrency downstream.

## ADR-002: Spring Framework 7 core resilience before Resilience4j

- Decision: resilience1 uses only `@Retryable`, `RetryTemplate` and `@ConcurrencyLimit`.
- Why: they're now in the framework. Many apps need nothing more.
- Consequence: there's no circuit breaker in resilience1. That's what resilience2 adds.

## ADR-003: Retry only failures that can heal

- Decision: `includes = {HttpServerErrorException, ResourceAccessException}`.
- Rejected: retrying every exception, which turns a 404 or 400 into four slow failures.

## ADR-004: Idempotency key made outside the retry loop

- Decision: `PaymentClient.pay` creates the key once, and every attempt sends it.
- Rejected: a new key per attempt. That defeats the point and charges twice.
