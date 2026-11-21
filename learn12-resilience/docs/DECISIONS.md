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

## ADR-005: Fallback on `@Retry`, not on `@CircuitBreaker`

- Context: in the first run, the fallback sat on the breaker. Retry never saw
  an exception, so it made one attempt per request.
- Decision: the fallback goes on the outermost aspect, and the breaker has none.

## ADR-006: Count-based window with a minimum number of calls

- Decision: a window of 10 with `minimum-number-of-calls: 5`.
- Why: it's easy to reason about in a demo, and one failed call after a restart
  can't open the breaker.
- Rejected: a time-based window. Better for steady production traffic, but
  harder to see in a short test.

## ADR-007: Rate limiter with zero wait

- Decision: `timeout-duration: 0`, then 429 with `Retry-After: 1`.
- Rejected: waiting for a permit, which hides overload as latency and holds threads.
