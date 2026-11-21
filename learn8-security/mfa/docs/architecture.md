# MFA architecture

## mfa1-totp filter chain

```mermaid
flowchart TB
  req[Request] --> upf["UsernamePasswordAuthenticationFilter<br/>POST /login adds FACTOR_PASSWORD"]
  upf --> tf["TotpAuthenticationFilter<br/>POST /mfa/totp adds FACTOR_TOTP"]
  tf --> tdf["TrustedDeviceFilter<br/>valid device cookie adds FACTOR_TOTP"]
  tdf --> az{"AuthorizationFilter"}
  az -->|"/login"| ok[permitAll]
  az -->|"/mfa/**"| pw[needs authentication only]
  az -->|"everything else"| both["needs FACTOR_PASSWORD + FACTOR_TOTP"]
  both -->|missing FACTOR_TOTP| ep["entry point /mfa/totp"]
  both -->|missing FACTOR_PASSWORD| login["/login"]
```

## Login with TOTP

```mermaid
sequenceDiagram
  participant B as Browser
  participant S as Security filters
  participant P as TotpAuthenticationProvider
  participant T as TotpService
  B->>S: POST /login (alice, password)
  S-->>B: 302 / (session: ROLE_USER, FACTOR_PASSWORD)
  B->>S: GET /
  S-->>B: 302 /mfa/totp (missing FACTOR_TOTP)
  B->>S: POST /mfa/totp code=492039
  S->>P: TotpAuthenticationToken(alice, 492039)
  P->>T: verify
  Note over T: locked? decrypt secret, check steps -1..+1, replay, recovery codes
  T-->>P: OK
  P-->>S: FACTOR_TOTP
  Note over S: merge with FACTOR_PASSWORD, new session id
  S-->>B: 302 / (saved request)
```

## Enrollment states

```mermaid
stateDiagram-v2
  [*] --> NotEnrolled
  NotEnrolled --> Pending: GET /mfa/enroll (secret in session)
  Pending --> Pending: wrong code
  Pending --> Enrolled: first valid code (secret encrypted, recovery codes made)
  Enrolled --> Locked: 5 wrong codes
  Locked --> Enrolled: after 5 minutes
  Enrolled --> NotEnrolled: turn off two-step login
```

## mfa2-email-ott

```mermaid
sequenceDiagram
  participant B as Browser
  participant A as App (Security 7)
  participant M as MailHog
  B->>A: POST /login (alice, password)
  A-->>B: 302 / then 302 /login?factor.type=ott&factor.reason=missing
  B->>A: POST /ott/generate
  Note over A: resolver uses the signed-in user, ttl 5 min
  A->>M: SMTP: link /login/ott?token=...
  A-->>B: 302 /ott/sent
  B->>M: read mail (http://localhost:8025)
  B->>A: GET /login/ott?token=... then POST /login/ott
  Note over A: token consumed once, FACTOR_OTT added to FACTOR_PASSWORD
  A-->>B: 302 / (both factors)
```

## mfa3-passkeys

Registration:

```mermaid
sequenceDiagram
  participant B as Browser JS
  participant Au as Authenticator (device)
  participant S as App
  B->>S: POST /webauthn/register/options
  S-->>B: challenge, rp.id=localhost, user handle
  B->>Au: navigator.credentials.create(options)
  Note over Au: user verification (PIN, fingerprint), new key pair
  Au-->>B: public key + attestation, signed over the challenge
  B->>S: POST /webauthn/register
  Note over S: check challenge + origin, store public key and counter
```

Sign-in as the second factor:

```mermaid
sequenceDiagram
  participant B as Browser JS
  participant Au as Authenticator
  participant S as App
  B->>S: POST /webauthn/authenticate/options
  S-->>B: challenge
  B->>Au: navigator.credentials.get(challenge)
  Au-->>B: signature with the private key (never leaves the device)
  B->>S: POST /login/webauthn
  Note over S: verify with the stored public key, counter must increase
  S-->>B: FACTOR_WEBAUTHN added to FACTOR_PASSWORD
```

## mfa4-step-up

```mermaid
stateDiagram-v2
  [*] --> Fresh: password entered (issuedAt = now)
  Fresh --> Stale: window passes
  Fresh --> Fresh: GET /, GET/POST /transfer
  Stale --> Stale: GET / still allowed
  Stale --> Reauth: GET or POST /transfer
  Reauth --> Fresh: password again (new issuedAt)
  note right of Stale
    /login?factor.type=password&factor.reason=expired
  end note
```

## mfa5-keycloak-otp

```mermaid
sequenceDiagram
  participant B as Browser
  participant A as Spring app (:8125)
  participant K as Keycloak (:8180)
  B->>A: GET /
  A-->>B: 302 to K /auth?code_challenge=...&nonce=...
  B->>K: username + password
  K-->>B: OTP page (or QR setup on first login)
  B->>K: 6-digit code
  K-->>B: 302 to A /login/oauth2/code/keycloak?code=...
  A->>K: POST /token (code + code_verifier + client secret)
  K-->>A: ID token (acr), access token
  Note over A: OidcUser in the session, no password or OTP seen by the app
  A-->>B: 302 /
```
