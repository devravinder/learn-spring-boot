# MFA decisions

## ADR-001: Everything local, no third-party MFA service

- Decision: TOTP is computed in the app, QR codes are drawn with ZXing, email
  goes to MailHog, and passkeys use the browser's built-in authenticator.
- Rejected:
  - SMS: needs a paid gateway and is weak against SIM swaps.
  - Hosted QR APIs: they would see the TOTP secret.

## ADR-002: TOTP written by hand instead of a library

- Decision: `Totp.java` implements RFC 6238 and Base32 in about 80 lines.
- Why: the point is to see HMAC, the time step and dynamic truncation.
- Rejected: `googleauth` or `dev.samstevens.totp`, fine for production but they hide the mechanism.

## ADR-003: TOTP as a Security 7 factor authority

- Decision: a custom filter and provider add `FACTOR_TOTP`, and rules use
  `AuthorizationManagerFactories.multiFactor()`.
- Rejected: a "pending MFA" role and a custom success handler (the Security 6
  pattern). It works, but it duplicates what Security 7 now models directly.

## ADR-004: Merge factors explicitly in the TOTP filter

- Context: `setMfaEnabled(true)` did not merge our custom token. The session
  kept only `FACTOR_TOTP` and lost `FACTOR_PASSWORD`.
- Decision: the filter builds the result from the current authorities plus the
  new factor.
- Consequence: the merge is visible in our own code instead of hidden in the framework.

## ADR-005: Replay protection and lockout in the service

- Decision: store the last used time step per user, and lock for 5 minutes after 5 failures.
- Why: a 6-digit code has 1,000,000 values, and the ±1 step window accepts 3 of
  them, so an unlimited guesser would get in.

## ADR-006: Trusted device as a signed cookie

- Decision: an HMAC-SHA256 cookie with `user|expiry`, HttpOnly, SameSite=Lax, 30 days.
- Rejected:
  - Skipping MFA by IP address: shared networks and VPNs make that unsafe.
  - A server-side device table: better for "sign out all devices", but more
    code than the lesson needs.

## ADR-007: Email link via the built-in OTT login

- Decision: `@EnableMultiFactorAuthentication` + `oneTimeTokenLogin()` with a mail sender.
- Why: shows the framework path with no custom security code, next to mfa1's custom factor.
- Rejected: a hand-written email code flow. It would repeat mfa1.

## ADR-008: Token request resolved from the signed-in user

- Context: the default resolver trusts the `username` form field.
- Decision: once a user has a session, use that name and ignore the field.
- Why: otherwise a signed-in user could trigger links to other inboxes, which is spam at best.

## ADR-009: MailHog for email

- Decision: `mailhog/mailhog` on 1025 (SMTP) and 8025 (web and API).
- Rejected: a real SMTP provider. That breaks the "nothing leaves the machine" rule.

## ADR-010: Passkeys as opt-in MFA (`when = WEBAUTHN_REGISTERED`)

- Decision: MFA is required only once a user has a passkey.
- Why: that's how most sites roll passkeys out, and nobody gets locked out before enrolling.
- Rejected: mandatory for everyone, which needs an enrollment path for users
  with no device, as mfa1 does for TOTP.

## ADR-011: Passkey repositories as beans

- Context: the `WEBAUTHN_REGISTERED` condition failed at startup with
  "No qualifying bean of type PublicKeyCredentialUserEntityRepository".
- Decision: declare `MapPublicKeyCredentialUserEntityRepository` and
  `MapUserCredentialRepository` beans that both the filters and the condition use.
- Consequence: swap them for JDBC versions to keep passkeys across restarts.

## ADR-012: Browser test with a CDP virtual authenticator

- Decision: test in Chrome with `WebAuthn.addVirtualAuthenticator`
  (ctap2, internal, resident key, user verified).
- Rejected: faking the WebAuthn JSON with HTTP calls, which skips the browser
  checks this module is about.
