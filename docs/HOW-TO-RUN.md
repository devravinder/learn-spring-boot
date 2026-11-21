# How to run

## Prerequisites

- JDK 21 (the Gradle toolchain is set to 21)
- Docker, for the Testcontainers tests and each topic's `docker-compose.yaml`
- Nothing else: use the Gradle wrapper (`gradlew` / `gradlew.bat`, Gradle 9.9.2)

## Build and test everything

```bash
./gradlew clean build          # Linux / macOS
gradlew.bat clean build        # Windows
```

Expected: `BUILD SUCCESSFUL`. Some tests are skipped on purpose (OS- and
JRE-specific demos in `testing3`).

## Run one module

```bash
./gradlew :learn8-security:jwt:jwt1-basic:bootRun
```

The module path is the folder path with `:` instead of `/`. See `settings.gradle`.

## Tips

- Port taken: pass another port, for example
  `./gradlew :learn9-sse:bootRun --args='--server.port=8090'`.
- Postgres says `invalid value for parameter "TimeZone"`: some JVMs still
  report the old zone alias `Asia/Calcutta`. Run with
  `-Duser.timezone=Asia/Kolkata`.
- `Renamed property?` Add
  `runtimeOnly 'org.springframework.boot:spring-boot-properties-migrator'`
  temporarily. At startup it prints every renamed key and its replacement.

## OAuth2 code flow with curl (PKCE)

Security 7 requires PKCE, so a manual code flow needs a verifier and a challenge:

```bash
VERIFIER=$(openssl rand -base64 32 | tr '+/' '-_' | tr -d '=')
CHALLENGE=$(printf %s "$VERIFIER" | openssl dgst -sha256 -binary | openssl base64 | tr '+/' '-_' | tr -d '=')

# 1. open in a browser, log in as user/password, approve, copy ?code=...
echo "http://localhost:7000/oauth2/authorize?response_type=code&client_id=login-client&scope=openid%20profile&redirect_uri=http://127.0.0.1:8080/authorized&code_challenge=$CHALLENGE&code_challenge_method=S256"

# 2. exchange the code
curl -u login-client:openid-connect \
  -d grant_type=authorization_code -d code=CODE \
  -d redirect_uri=http://127.0.0.1:8080/authorized \
  -d code_verifier=$VERIFIER \
  http://localhost:7000/oauth2/token
```

The `auth-client` apps do this for you.
