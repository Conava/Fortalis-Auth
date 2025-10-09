# Fortalis Auth

A lightweight **authentication service** for the Fortalis game.
Built with **Spring Boot 3**, **Java 21**, **PostgreSQL**, **Flyway**, and **Nimbus JOSE/JWT**.
It issues **RS256-signed JWT access tokens**, rotates **refresh tokens**, supports **TOTP 2FA**, and exposes a **JWKS**
endpoint so region/game backends can verify tokens **without** calling Auth.

---

## ✨ Features

* **Accounts**
    * Email + password (Argon2id by default)
    * Optional **TOTP** multi-factor authentication
    * Extensible identity model for **Google/Apple Login** in the future

* **Tokens**
    * Short-lived **access tokens** (JWT, RS256)
    * Long-lived **refresh tokens** (hashed & revocable, rotation on refresh)
    * **JWKS** endpoint for public key discovery

* **Persistence**
    * **PostgreSQL** with **Flyway** migrations

* **Ops**
    * Health endpoint via Spring **Actuator**
    * Clean separation from region/game databases

---

## 🧱 Architecture

```
+-------------------+         Verify via JWKS
|  Region Backend   |  <------------------------------+
|  (per region)     |                                  |
+-------------------+                                  |
         ^                                             |
         |   sub = account_id (UUID)                   |
         |                                             v
+-------------------+       RS256 JWT        +--------------------+
|  Fortalis Auth    |  ------------------>   |  Clients (mobile,  |
|  (this service)   |  <------------------   |  web, tools)       |
|  - Accounts/MFA   |   Refresh rotation     +--------------------+
|  - JWT/JWKS       |
+-------------------+
       |
       +-- PostgreSQL (fortalis_auth): account, identity, mfa, refresh_token
```

---

## 📦 Requirements

* **Java 21**
* **Gradle**
* **Docker** + **Docker Compose** (for local Postgres/Redis)
* **IntelliJ IDEA Ultimate** recommended

---

## 🚀 Quick Start (Local Dev)

### 1) Bring up databases (two Postgres containers + Redis)

`docker-compose.yml`

* **Auth DB** on `localhost:5433` → database `fortalis_auth`
* **Game DB** on `localhost:5432` → database `fortalis` (used by game backend)
* **Redis** on `localhost:6379` (currently unused by Auth; required for game backend)

Start:

```bash
docker compose up -d
```

Sanity:

```bash
docker exec -it fortalis-pg-auth psql -U fortalis -d fortalis_auth -c '\dt'
```

### 2) Generate RSA keys (PEM files)

```bash
mkdir -p keys
openssl genrsa -out keys/tmp.key 2048
openssl pkcs8 -topk8 -inform PEM -outform PEM -in keys/tmp.key -out keys/fortalis_auth_private.pem -nocrypt
openssl rsa -in keys/tmp.key -pubout -out keys/fortalis_auth_public.pem
rm keys/tmp.key
```

### 3) Configure the app

Configure the App in `src/main/resources/application.properties`

### 4) Run the app

**IntelliJ Spring Boot run configuration**

* Main class: `io.fortalis.fortalisauth.FortalisAuthApplication`
* JRE: 21
* VM options (optional): `-Dspring.profiles.active=local`
* Environment:

    * `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/fortalis_auth`
    * `SPRING_DATASOURCE_USERNAME=fortalis`
    * `SPRING_DATASOURCE_PASSWORD=fortalis`

**Via Gradle:**

```bash
./gradlew bootRun
```

### 5) Verify it’s up

* Health: `GET http://localhost:8080/actuator/health` → `{"status":"UP"}`
* JWKS: `GET http://localhost:8080/.well-known/jwks.json`

---

## 🗃️ Database & Migrations

**Flyway migration files** (applied automatically on startup):

```
src/main/resources/db/migration/
  V1__initial_auth_schema.sql             -- tables: account, account_identity, account_mfa, account_settings, refresh_token
```

---

## 🔐 Security Model

* **Access token**: RS256 JWT with claims:

    * `iss` = `https://auth.fortalis.game`
    * `sub` = `account_id` (UUID)
    * `aud` = `fortalis-game`
    * `exp/iat`
    * `mfa` = boolean
* **Refresh token**:

    * Opaque random string (returned to client)
    * Stored **hashed** in DB, with expiry & revocation
    * **Rotated** on refresh

**Region backends** validate the access token by fetching **JWKS** from:
`GET /.well-known/jwks.json`

---

## 📚 API Reference (v0)

All endpoints are JSON over HTTPS.

### Auth

#### `POST /auth/register`

Create an account & immediately issue tokens.

Request:

```json
{
  "email": "player@example.com",
  "password": "Str0ngP@ss!",
  "displayName": "PlayerOne"
}
```

Response:

```json
{
  "accessToken": "<JWT>",
  "refreshToken": "<opaque>",
  "expiresInSeconds": 900
}
```

#### `POST /auth/login`

Login with email/password. If TOTP is enabled, include `mfaCode`.

Request:

```json
{
  "emailOrUsername": "player@example.com",
  "password": "Str0ngP@ss!",
  "mfaCode": "123456"
}
```

Response: same as register.

#### `POST /auth/refresh`

Rotate refresh token.

Request:

```json
{
  "refreshToken": "<opaque>"
}
```

Response: new pair of tokens.

#### `POST /auth/logout`

Revoke a refresh token.

Request:

```json
{
  "refreshToken": "<opaque>"
}
```

### MFA (TOTP)

> In a real flow you’d authenticate with the access token and infer `accountId` from `sub`.
> For simplicity in early dev, these endpoints accept `accountId` as a parameter.

#### `POST /auth/mfa/totp/setup?accountId=<uuid>`

Returns secret & otpauth URL for QR.

Response:

```json
{
  "secretBase32": "JBSWY3DPEHPK3PXP...",
  "otpauthUrl": "otpauth://totp/Fortalis:acct:<uuid>?secret=...&issuer=Fortalis"
}
```

#### `POST /auth/mfa/totp/enable?accountId=<uuid>&code=123456`

Enables TOTP after verifying code.

#### `POST /auth/mfa/totp/disable?accountId=<uuid>&code=123456`

Disables TOTP after verifying code.

### JWKS

#### `GET /.well-known/jwks.json`

Public keys (JWK set) for RS256 verification.

---

## 🧩 Sample cURL

Register:

```bash
curl -X POST http://localhost:8080/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@example.com","password":"Str0ngP@ss!","displayName":"User"}'
```

Login (without MFA):

```bash
curl -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"emailOrUsername":"user@example.com","password":"Str0ngP@ss!"}'
```

Refresh:

```bash
curl -X POST http://localhost:8080/auth/refresh \
  -H 'Content-Type: application/json' \
  -d '{"refreshToken":"<opaque>"}'
```

JWKS:

```bash
curl http://localhost:8080/.well-known/jwks.json
```

---

## 🧰 Build & Run

### IntelliJ (recommended)

* Use a **Spring Boot** run configuration for fast Run/Debug + DevTools restarts.
* Enable **Annotation Processing** (Lombok, config processor).
* Optional: Settings → Build → Compiler → **Build project automatically**; and enable automake while app running.

### Gradle (CI parity)

```bash
./gradlew clean build
./gradlew bootRun
```

Run tests:

```bash
./gradlew test
```

---

## 🌱 Environments

### Local

* Databases via Docker Compose
* Keys from local files (`./keys/*.pem`)
* `issuer` may remain `https://auth.fortalis.game` or use `http://localhost:8080` (ensure region verifiers match)

### Test/Staging

* Separate Postgres (compose or managed)
* Distinct key pair; publish the **test** JWKS URL to staging backends
* Consider enabling **CORS** for web clients

### Production

* Managed Postgres (RDS, Cloud SQL, …)
* Store keys in a **secret manager** or **KMS**
* Enforce TLS, lock down Actuator endpoints
* Structured authentication logs (no PII) to a central sink
* Plan **key rotation** (two JWKs with different `kid`s during rollout)

---

## 🧠 Implementation Notes

* **Password hashing**
    * Argon2id via Spring Security 6 (configurable parameters).
    * Store only the hash (no salt field; included in Argon2 hash format).
  
* **MFA**
    * TOTP implemented per RFC 6238 (HMAC-SHA1, 30s window, ±1 step tolerance).
    * Store secrets securely in production.

---

## 🔜 Roadmap

* Email verification & password reset flow
* OAuth provider token exchange (Google/Apple)
* Key rotation automation & KMS integration
* Rate limiting / brute-force protection
* Structured audit logging
