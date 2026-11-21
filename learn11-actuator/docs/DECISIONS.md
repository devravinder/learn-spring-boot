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

## ADR-005: Actuator on a separate management port

- Decision: `management.server.port: 9102`, and probes copied to the app port
  with `add-additional-paths`.
- Why: the public load balancer only routes 8102, so `/manage/**` is never
  public. Kubernetes still gets `/livez` and `/readyz` on the app port, which
  checks that the app connector itself is accepting requests.
- Rejected: one port with a path-based firewall rule. That's easy to get wrong.

## ADR-006: Read-only by default, writes opted in per endpoint

- Decision: `management.endpoints.access.default: read-only`, with
  `unrestricted` only for `loggers`, `features` and `shutdown`.
- Rejected: `include: "*"` with full access, where one leaked credential could
  shut the app down or change config.

## ADR-007: `EndpointRequest` matchers instead of path strings

- Decision: a dedicated `SecurityFilterChain` matched with
  `EndpointRequest.toAnyEndpoint()`.
- Why: it follows `base-path` and port changes automatically.
- Rejected: `requestMatchers("/manage/**")`, which breaks silently when the base path changes.

## ADR-008: Secrets masked twice

- Decision: `show-values: when-authorized`, plus a `SanitizingFunction` that
  always masks keys containing `token`.
- Rejected: `show-values: always`, which is convenient but leaks secrets to anyone
  with actuator access.
