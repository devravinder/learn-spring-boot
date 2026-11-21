# How to run learn11-actuator

No containers are needed. Run from the repo root.

## actuator1-custom-metrics

```bash
./gradlew :learn11-actuator:actuator1-custom-metrics:bootRun
```

Then send the requests in [../http/actuator1-metrics.http](../http/actuator1-metrics.http), or:

```bash
curl -s -X POST localhost:8101/orders -H 'Content-Type: application/json' \
  -d '{"product":"book","paymentMethod":"upi","amount":300}'
curl -s -X POST 'localhost:8101/reports?seconds=30'
curl -s localhost:8101/actuator/prometheus | grep '^shop_'
```

What you should see:

| Check                                     | Expected                                          |
| ----------------------------------------- | ------------------------------------------------- |
| `shop_orders_placed_total`                | one series per `payment_method`                   |
| `shop_order_processing_seconds{quantile}` | p50 / p95 / p99                                   |
| `shop_order_amount_inr_bucket{le="1000.0"}` | orders up to 1000                               |
| `shop_report_generation_seconds_count`    | `1` while the report runs, `0` after it finishes  |
| `POST /visits/u1` .. `/visits/u25`        | `series` stops at `20`                            |
| `tomcat_sessions_*`                       | missing (denied by a filter)                      |

Drill down with the metrics endpoint:

```bash
curl -s 'localhost:8101/actuator/metrics/shop.orders.placed?tag=payment.method:upi'
```
