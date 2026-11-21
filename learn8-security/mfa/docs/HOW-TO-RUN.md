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
