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
