# learn12-resilience

How a service keeps working when its dependencies don't, and what
"consistent" means once data lives on more than one node.

| Module                     | Port | Learn                                                        |
| -------------------------- | ---- | ------------------------------------------------------------ |
| `flaky-service`            | 8131 | a downstream you can break at runtime (`POST /chaos`)        |
| `resilience1-spring-retry` | 8132 | timeouts, `@Retryable`, `RetryTemplate`, idempotency keys, `@ConcurrencyLimit` (Spring Framework 7, no library) |
| `resilience2-resilience4j` | 8133 | circuit breaker, retry, rate limiter, bulkhead, time limiter, fallbacks, metrics |
| `resilience3-cap-theorem`  | 8134 | 3 replicas, network partitions, CP vs AP, quorums, last-write-wins |

- How to run: [docs/HOW-TO-RUN.md](docs/HOW-TO-RUN.md)
- Architecture: [docs/architecture.md](docs/architecture.md)
- Decisions: [docs/DECISIONS.md](docs/DECISIONS.md)
- Requests: [http/](http/)

## The patterns in one table

| Pattern          | Problem it solves                        | Here                                         |
| ---------------- | ---------------------------------------- | -------------------------------------------- |
| Timeout          | a slow call holds a thread forever       | `setReadTimeout(1s)` on the `RestClient`     |
| Retry + backoff  | short glitches (a 503, a dropped connection) | `@Retryable(delay=100, multiplier=2)`    |
| Jitter           | all clients retry at the same moment     | `jitter = 50`                                |
| Retry only what heals | a 404 never gets better             | `includes = {5xx, timeouts}`                 |
| Idempotency key  | a retried POST charges twice             | `Idempotency-Key` header, made once per payment |
| Bulkhead         | one slow dependency uses all threads     | `@ConcurrencyLimit(2)`, BLOCK or REJECT      |
| Circuit breaker  | hammering a dependency that is down      | resilience2                                  |
| Rate limiter     | callers above the agreed rate            | resilience2                                  |
| Fallback         | something is better than an error        | resilience2                                  |

```mermaid
flowchart LR
  req[call] --> to{"timeout?"}
  to -->|ok| done[result]
  to -->|"5xx or timeout"| retry{"retries left?"}
  to -->|4xx| fail[fail fast]
  retry -->|"yes: wait 100, 200, 400 ms + jitter"| req
  retry -->|no| giveup["503 or fallback"]
```

## resilience1-spring-retry

Spring Framework 7 has resilience built in:

| API                                   | What it does                                          |
| ------------------------------------- | ----------------------------------------------------- |
| `@EnableResilientMethods`             | turns on the annotations below                        |
| `@Retryable(includes, maxRetries, delay, multiplier, jitter, maxDelay)` | declarative retry with exponential backoff |
| `RetryTemplate` + `RetryPolicy.builder()` | the same, in code; `RetryListener` sees every retry |
| `@ConcurrencyLimit(n)`                | at most n calls at once; `BLOCK` queues, `REJECT` throws `InvocationRejectedException` |

The lost-reply case is the one people forget. The payment is charged, but
the reply never arrives (here, a 502). The client can't tell "not charged" from
"charged, reply lost", so it retries. The same `Idempotency-Key` makes the
server return the stored result instead of charging again.

A proxy pitfall to remember: `@Retryable` beans are CGLIB proxies. Reading a
field through the proxy gives `null`, so expose state through methods
(`PriceClient.attempts()`).
