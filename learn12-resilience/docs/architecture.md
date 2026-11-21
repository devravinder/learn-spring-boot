# learn12-resilience architecture

## resilience1-spring-retry

```mermaid
flowchart LR
  c[curl] --> ctl[RetryController]
  ctl --> pc["PriceClient (proxy)<br/>@Retryable, @ConcurrencyLimit"]
  ctl --> pay["PaymentClient<br/>RetryTemplate + listener"]
  pc -->|"RestClient, 1 s read timeout"| fl[flaky-service :8131]
  pay -->|"POST /payments + Idempotency-Key"| fl
  chaos["POST /chaos"] -.-> fl
```

Lost reply, then a safe retry:

```mermaid
sequenceDiagram
  participant A as resilience1
  participant F as flaky-service
  A->>F: POST /payments (Idempotency-Key: K)
  Note over F: charge stored under K
  F--xA: 502 (reply lost)
  Note over A: wait 100 ms
  A->>F: POST /payments (Idempotency-Key: K)
  F-->>A: 200 stored result, Idempotent-Replayed: true
  Note over F: charges = 1
```

## resilience2-resilience4j

```mermaid
flowchart LR
  ctl[QuoteController] --> retry["@Retry prices<br/>fallback: cachedPrice"]
  retry --> cb["@CircuitBreaker prices"]
  cb --> call1[GET /prices]
  ctl --> rl["@RateLimiter search"] --> call2[GET /prices]
  ctl --> bh["@Bulkhead reports"] --> call3[GET /work]
  ctl --> tl["@TimeLimiter slow<br/>+ thread-pool bulkhead"] --> call4[GET /work]
  call1 & call2 & call3 & call4 --> fl[flaky-service]
  cb -.->|state, events| act["actuator: health, circuitbreakerevents, prometheus"]
```

## resilience3-cap-theorem

CP write during a partition:

```mermaid
sequenceDiagram
  participant C as client
  participant N3 as n3 (minority)
  participant N1 as n1 (majority)
  participant N2 as n2
  C->>N3: PUT x=minority
  Note over N3: reachable = {n3} < W=2
  N3-->>C: 503 (stay consistent)
  C->>N1: PUT x=v2
  N1->>N2: replicate (wait hopMs)
  N2-->>N1: ack
  N1-->>C: 200 acks=2
```

AP writes on both sides, then heal:

```mermaid
sequenceDiagram
  participant C as client
  participant N1 as n1
  participant N3 as n3
  C->>N3: PUT cart=book (clock 2, seen 1)
  N3-->>C: 200 acks=1
  C->>N1: PUT cart=pen (clock 3, seen 1)
  N1-->>C: 200
  Note over N1,N3: reads differ: pen vs book
  C->>N1: POST /heal
  Note over N1,N3: newest wins (clock 3 = pen)
  Note over N1,N3: book clock 2 > pen.seen 1, so it was concurrent and is reported lost
```

The quorum rule:

```mermaid
flowchart LR
  w["write set: W nodes"] --- overlap{"R + W > N ?"}
  r["read set: R nodes"] --- overlap
  overlap -->|yes| fresh["at least one node in both sets: read sees the last write"]
  overlap -->|no| stale["the sets can miss each other: stale reads"]
```
