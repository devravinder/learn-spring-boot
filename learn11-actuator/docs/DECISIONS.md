# learn11-actuator decisions

## ADR-001: One shop domain carries every meter type

- Context: meter types are easy to mix up when they're shown in isolation.
- Decision: each meter is tied to one real action (place order, restock, run
  report), so the reason for the type is visible in the code.
- Rejected: a single "metrics demo" controller that increments everything.

## ADR-002: Histograms for amounts, client percentiles for one timer

- Context: Prometheus can't combine client-side percentiles across instances.
- Decision: `shop.order.amount` uses SLO buckets (histogram), and
  `shop.order.processing` uses `publishPercentiles`, so both are visible side by side.
- Rejected: percentiles everywhere, which is easy to read but wrong once
  there are multiple instances.

## ADR-003: Explicit `LongTaskTimer` instead of `@Timed(longTask = true)`

- Context: on an `@Async` method, the annotation produced a normal timer in our test.
- Decision: build the `LongTaskTimer` in code and wrap the work with `record`.
- Rejected: relying on annotation order between the async and metrics aspects.

## ADR-004: Cardinality guarded by `MeterFilter`, not by convention

- Decision: `maximumAllowableTags("shop.visits", "user", 20, deny())` keeps the
  anti-pattern from blowing up the number of series.
- Rejected: only documenting "don't tag with user ids". People forget.
