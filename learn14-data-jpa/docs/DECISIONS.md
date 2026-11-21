# learn14-data-jpa decisions

## ADR-001: Show the SQL in every response

- Decision: a Hibernate `StatementInspector` (`SqlRecorder`) records statements
  per request, and the controller returns them.
- Why: "N+1" and "COUNT query" become numbers you can see.
- Rejected: `show-sql` in the log only, which is hard to tie to one request.

## ADR-002: `open-in-view: false`

- Decision: no session in the web layer, and services return DTOs.
- Why: lazy loading during JSON rendering hides queries and couples the API to the entities.

## ADR-003: Cursor paging with Spring Data `Window` and an opaque token

- Decision: `ScrollPosition` / `Window` keyset scrolling, with the last row's
  keys encoded as base64.
- Why: one indexed query per page, and stable results while rows are added.
  The token lets the server change the keys later without breaking clients.
- Rejected: exposing `?lastPrice=&lastId=` (it leaks the sort keys and is easy
  to misuse) and offset paging for feeds.

## ADR-004: A unique tie-breaker in every cursor sort

- Decision: sort by `price, id`, never by `price` alone.
- Why: prices repeat, and a non-unique key makes keyset paging skip or repeat rows.

## ADR-005: Three locking strategies side by side

- Decision: optimistic (`@Version`), pessimistic (`FOR UPDATE`) and an atomic `UPDATE`
  on the same race.
- Why: the trade-offs are only clear when the same 20 parallel requests hit each one.

## ADR-006: H2 in memory

- Decision: H2, seeded at startup.
- Why: no setup needed. The same queries run on PostgreSQL. In production, add
  an index on `(price, id)` for the cursor.
