# How to run learn12-resilience

Every module uses `flaky-service`, so start it first:

```bash
./gradlew :learn12-resilience:flaky-service:bootRun
```

Break it at runtime:

```bash
# fail the next 2 calls with 503
curl -s -X POST localhost:8131/chaos -H 'Content-Type: application/json' \
  -d '{"failRate":0,"failNext":2,"latencyMs":0,"status":503}'
# 50 % random failures and 300 ms latency
curl -s -X POST localhost:8131/chaos -H 'Content-Type: application/json' \
  -d '{"failRate":0.5,"failNext":0,"latencyMs":300,"status":503}'
curl -s -X POST localhost:8131/chaos/reset
curl -s localhost:8131/chaos/stats   # calls, failures, maxInFlight, charges
```

## resilience1-spring-retry

```bash
./gradlew :learn12-resilience:resilience1-spring-retry:bootRun
```

| Chaos                         | Request                         | Expected                                     |
| ----------------------------- | ------------------------------- | -------------------------------------------- |
| reset                         | `GET :8132/prices/p1`           | `attempts: 1`                                |
| `failNext: 2`                 | `GET :8132/prices/p1`           | 200, `attempts: 3`, takes about 300 ms       |
| `failRate: 1`                 | `GET :8132/prices/p1`           | 503, `attempts: 4` (1 + 3 retries)           |
| reset                         | `GET :8132/prices/missing`      | 404, `attempts: 1` (never retried)           |
| `latencyMs: 1500`             | `GET :8132/prices/p1`           | 503 after 4 read timeouts                    |
| `failNext: 2, status: 502`    | `POST :8132/payments?amount=500` | 200, `retries: 1`; stats show `charges: 1`  |
| reset                         | `GET :8132/bulkhead?n=6`        | all ok; stats `maxInFlight: 2`               |
| reset                         | `GET :8132/bulkhead?n=5&mode=reject` | most rejected; `maxInFlight: 1`         |

## resilience2-resilience4j

```bash
./gradlew :learn12-resilience:resilience2-resilience4j:bootRun
```

| Step                                             | Request                           | Expected                                  |
| ------------------------------------------------ | --------------------------------- | ----------------------------------------- |
| reset chaos                                      | `GET :8133/prices/p1`             | `source: live`; `GET :8133/breaker` is `CLOSED` |
| chaos `failRate: 1`                              | `GET :8133/prices/p1` twice       | `source: cache`; breaker `OPEN`           |
| keep failing                                     | `GET :8133/prices/p1` × 5         | `reason: circuit open`; `/chaos/stats` calls don't change |
| reset chaos, wait 3 s                            | `GET :8133/breaker`               | `HALF_OPEN`; 2 more price calls → `CLOSED` |
| chaos `failNext: 2`                              | `GET :8133/prices/p3`             | `source: live` (healed by retry)          |
| reset                                            | `GET :8133/search?q=p1` × 10 fast | 5 × 200, 5 × 429                          |
| reset                                            | `GET :8133/reports?n=5`           | `ok: 2, full: 3`                          |
| reset                                            | `GET :8133/slow?ms=1500`          | fallback after about 500 ms               |

Actuator views:

```bash
curl -s localhost:8133/actuator/health | jq .components.circuitBreakers
curl -s localhost:8133/actuator/circuitbreakerevents/prices
curl -s localhost:8133/actuator/prometheus | grep resilience4j_circuitbreaker_state
```
