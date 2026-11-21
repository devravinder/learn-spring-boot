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
