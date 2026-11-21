# How to run the MFA modules

Run each module from the repo root. The users are `alice / password` and
`bob / password`.

## mfa1-totp

```bash
./gradlew :learn8-security:mfa:mfa1-totp:bootRun
```

1. Open <http://localhost:8121> and sign in as `alice`.
2. You land on `/mfa/enroll`. Scan the QR code with any authenticator app,
   or type the key shown under it.
3. Enter the 6-digit code. Save the 8 recovery codes shown next.
4. Enter a fresh code on `/mfa/totp`. The home page lists `FACTOR_PASSWORD` and `FACTOR_TOTP`.

No phone? Generate codes on the command line with `oathtool`:

```bash
oathtool --totp -b <KEY-FROM-THE-PAGE>
```

Things to try:

| Try                                                    | Expected                                    |
| ------------------------------------------------------ | ------------------------------------------- |
| open `/` after only the password                       | redirect to `/mfa/totp`                     |
| the same code twice (second browser)                   | "That code was already used"                |
| 5 wrong codes                                          | "Too many attempts", even for a right code  |
| a recovery code like `1234-5678`                       | accepted once, then rejected                |
| tick "Trust this device", sign out, sign in again      | no code asked for 30 days                   |
| the `mfa_device` cookie in another user's browser      | ignored (bound to the user name)            |
| "Turn off two-step login"                              | next login starts enrollment again          |

The secrets are encrypted with `MFA_ENCRYPTION_KEY`, and the device cookie is
signed with `MFA_DEVICE_COOKIE_KEY`. Both have local-only defaults in
`application.yaml`.

## mfa2-email-ott

Start MailHog once (it's used by the other modules too):

```bash
docker compose -f learn8-security/mfa/infra/docker-compose.yaml up -d
./gradlew :learn8-security:mfa:mfa2-email-ott:bootRun
```

1. Open <http://localhost:8122> and sign in as `alice / password`.
2. Spring shows the one-time token form with `alice` already filled in. Click "Send Token".
3. Open the inbox at <http://localhost:8025>, then open the link in the same browser.
4. Click "Sign in". The home page lists `FACTOR_PASSWORD` and `FACTOR_OTT`.

| Try                                              | Expected                                 |
| ------------------------------------------------ | ---------------------------------------- |
| use the same link twice                          | `/login?error`                           |
| change `username` to `bob` in the form (dev tools) | the link still goes to alice           |
| restart with `--mfa.ott-ttl=10s` and wait 10 s   | the link is rejected                     |
| request a link first, then sign in with the password | both factors, in the other order     |

The MailHog JSON API is handy for scripts:
`curl -s 'localhost:8025/api/v2/search?kind=to&query=alice@example.test'`.

## mfa3-passkeys

```bash
./gradlew :learn8-security:mfa:mfa3-passkeys:bootRun
```

Use Chrome, Edge, Safari or Firefox on <http://localhost:8123>. WebAuthn
needs a secure context, and `localhost` counts as one even over http.

1. Sign in as `alice / password`. The home page shows only `FACTOR_PASSWORD`.
2. Click "Manage passkeys", type a label, then click "Register". Windows Hello,
   Touch ID or your phone creates the passkey.
3. You're asked for the passkey right away. MFA is now on for alice.
4. Sign out and in again: password, then "Sign in with a passkey". Both factors are listed.

No passkey hardware? In Chrome DevTools, open `More tools → WebAuthn`, enable
the virtual authenticator environment and add a `ctap2 / internal` authenticator
with resident keys and user verification.

| Try                                            | Expected                                   |
| ---------------------------------------------- | ------------------------------------------ |
| sign in as `bob` (no passkey)                  | password only                              |
| "Sign in with a passkey" first                 | then asked for the password, username pre-filled |
| alice's password in a browser without her passkey | stuck on the passkey step               |
| delete the passkey on the register page        | alice is back to password only             |

## mfa4-step-up

Use a short window so you don't have to wait two minutes:

```bash
./gradlew :learn8-security:mfa:mfa4-step-up:bootRun --args='--mfa.step-up-window=20s'
```

1. Sign in at <http://localhost:8124> as `alice / password`.
2. "Send money" works right away.
3. Wait 20 seconds. The statement still works, but "Send money" asks for the password again.
4. After you enter it, you're back on the transfer form, and the time on the statement has changed.
