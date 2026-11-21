# learn13-rest-api decisions

## ADR-001: RFC 9457 problem details for every error

- Decision: `spring.mvc.problemdetails.enabled`, plus one
  `ResponseEntityExceptionHandler` subclass.
- Why: one error shape for clients, whether the failure was validation, a
  missing route, a bad media type or a domain rule.
- Rejected: a custom `{code, message}` envelope that every client has to learn.

## ADR-002: Field errors as an `errors` array extension

- Decision: `errors: [{field, message}]` on validation problems.
- Why: RFC 9457 allows extension members, and forms need one message per input.

## ADR-003: Validation groups instead of two request classes

- Decision: one `UserRequest` with `OnCreate` and `OnUpdate`.
- Rejected: `CreateUserRequest` and `UpdateUserRequest`, which duplicate most fields.
  That's still fine when the shapes really differ.

## ADR-004: `instance` filled by a `ResponseBodyAdvice`

- Context: Spring Framework 7 didn't set `instance` on any problem in our tests.
- Decision: `ProblemInstanceAdvice` sets it from the request path when it's empty.
- Rejected: setting it in every handler, which is easy to forget.

## ADR-005: Unexpected errors show only an id

- Decision: a 500 problem carries an `errorId`, and the stack trace is logged under that id.
- Why: exception messages leak internals. Our test exception holds a fake password.

## ADR-006: Header first, query parameter as a fallback

- Decision: `API-Version` header plus `?version=`.
- Why: URLs stay the same across versions (good for caches and links), and the
  query parameter keeps browser testing easy.
- Rejected: path segments (`/v2/...`). They're common, but every version
  becomes a new resource URL.

## ADR-007: A default version instead of `required`

- Decision: `default: 1.0`.
- Why: existing clients that never sent a version keep working. New APIs can
  use `required: true` from day one.

## ADR-008: Baselines (`1.0+`, `1.1+`) for unchanged endpoints

- Decision: only endpoints that changed get a new mapping.
- Rejected: copying every endpoint per version, which multiplies the code for no change.

## ADR-009: HTTP interface clients over hand-written `RestClient` calls

- Decision: `@HttpExchange` interfaces registered with `@ImportHttpServices`.
- Why: the contract reads like the server's controller, it's easy to mock in
  tests, and config is per group in properties.
- Rejected: OpenFeign, an extra dependency that Spring now covers itself.

## ADR-010: The upstream runs inside the same app

- Decision: `CatalogUpstream` under `/upstream/catalog` plays the remote service.
- Why: no outside requests, and nothing extra to start.
- Rejected: a public API, which breaks the "all local" rule and makes tests flaky.

## ADR-011: springdoc, code first

- Decision: `springdoc-openapi-starter-webmvc-ui:3.2.1`, the line for Boot 4.
- Why: the document follows the code, and validation rules show up in schemas automatically.
- Rejected: hand-written `openapi.yaml` (contract first), which is mentioned in the README for shared APIs.

## ADR-012: Document errors as `ProblemDetail`

- Decision: every error response points to the `ProblemDetail` schema with
  `application/problem+json`.
- Why: it matches rest1's error format, so clients can generate one error type.
