# Fortalis Auth

A lightweight **authentication service** for the Fortalis game.
Built with **Spring Boot 4**, **Java 21**, **PostgreSQL**, **Flyway**, and **Nimbus JOSE/JWT**.
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

Configure the app in `src/main/resources/application.yml` (or via environment variables). See defaults in that file.

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

## ⚠️ Error Handling

All errors follow **RFC 7807 Problem Details** format with `Content-Type: application/problem+json`.

### Error Response Structure

Every error response includes:

* **`type`**: URI identifying the error type (e.g., `https://auth.fortalis.game/errors/invalid-credentials`)
* **`title`**: Human-readable short summary (e.g., `Unauthorized`)
* **`status`**: HTTP status code (e.g., `401`)
* **`detail`**: Human-readable explanation specific to this occurrence
* **`instance`**: URI of the request that caused the error (e.g., `/auth/login`)

### Common Error Types

| HTTP Status | Type | Detail | When It Occurs |
|-------------|------|--------|----------------|
| `400` | `validation-error` | Field validation failed | Invalid request body (missing fields, wrong format) |
| `400` | `invalid_request` | Request parameters invalid | Generic bad request |
| `401` | `invalid-credentials` | Bad credentials | Wrong email/password |
| `401` | `invalid_refresh` | Invalid refresh token | Refresh token not found, revoked, or expired |
| `401` | `expired_refresh` | Refresh token expired | Refresh token has expired |
| `401` | `mfa_invalid` | Invalid MFA code | TOTP code verification failed |
| `401` | `mfa_challenge_invalid` | Login ticket invalid/expired | MFA challenge token expired or not found |
| `400` | `mfa_code_required` | TOTP code required | MFA enabled but no code provided |
| `400` | `mfa_factor_not_allowed` | Selected factor not allowed | Tried to use unsupported MFA method |
| `400` | `mfa_factor_unsupported` | Unsupported MFA factor | Unrecognized MFA type |
| `404` | `account_missing` | Account not found | Account ID doesn't exist |
| `409` | `email_taken` | Email already registered | Registration with existing email |
| `429` | `rate-limit-exceeded` | Too many requests | Rate limit hit; includes `Retry-After` header |
| `500` | `internal-server-error` | Unexpected error | Server-side issue |

### Example Error Responses

**Validation Error (400)**

```json
{
  "type": "https://auth.fortalis.game/errors/validation-error",
  "title": "Validation Failed",
  "status": 400,
  "detail": "email: must be a well-formed email address; password: size must be between 8 and 100",
  "instance": "/auth/register"
}
```

**Invalid Credentials (401)**

```json
{
  "type": "https://auth.fortalis.game/errors/invalid-credentials",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Bad credentials",
  "instance": "/auth/login"
}
```

**Rate Limit Exceeded (429)**

```json
{
  "type": "https://auth.fortalis.game/errors/rate-limit-exceeded",
  "title": "Too Many Requests",
  "status": 429,
  "detail": "Too many requests. Please try again later.",
  "instance": "/auth/login",
  "retryAfter": 60
}
```

**Response Headers:**
```
Retry-After: 60
Content-Type: application/problem+json
```

**MFA Required (but returned as login ticket)**

When MFA is enabled, `/auth/login` returns a login ticket instead of an error:

```json
{
  "loginTicket": "...",
  "allowedFactors": ["TOTP"]
}
```

Use this ticket with `/auth/login/complete`.

### Rate Limiting

Current in-memory rate limits:

* **IP-based**: 20 requests per 60 seconds
* **User-based** (login): 5 attempts per 15 minutes (900 seconds)
* **MFA verification**: 10 attempts per 15 minutes per ticket

When exceeded, returns `429 Too Many Requests` with `Retry-After` header indicating seconds until reset.

---

## 📚 API Reference (v0)

All endpoints are JSON over HTTPS.

### Auth

#### `POST /auth/register`

Create an account & immediately issue tokens.

**Request:**

```json
{
  "email": "player@example.com",
  "password": "Str0ngP@ss!",
  "displayName": "PlayerOne"
}
```

**Success Response (200 OK):**

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6Ii4uLiJ9...",
  "refreshToken": "YXNkZmFzZGZhc2RmYXNkZmFzZGY",
  "expiresInSeconds": 900,
  "displayName": "PlayerOne",
  "mfaEnabled": false
}
```

**Error Responses:**
* `400 validation-error` - Invalid email format or password too short
* `409 email_taken` - Email already registered
* `429 rate-limit-exceeded` - Too many registration attempts

---

#### `POST /auth/login`

Login with email/password. Returns tokens if no MFA, or login ticket if MFA is enabled.

**Request:**

```json
{
  "emailOrUsername": "player@example.com",
  "password": "Str0ngP@ss!",
  "mfaCode": "123456"
}
```

**Success Response - No MFA (200 OK):**

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6Ii4uLiJ9...",
  "refreshToken": "YXNkZmFzZGZhc2RmYXNkZmFzZGY",
  "expiresInSeconds": 900,
  "displayName": "PlayerOne",
  "mfaEnabled": false
}
```

**Success Response - MFA Enabled (200 OK):**

```json
{
  "loginTicket": "temp_ticket_xyz123",
  "allowedFactors": ["TOTP"]
}
```

**Error Responses:**
* `400 validation-error` - Missing required fields
* `401 invalid-credentials` - Wrong email/password
* `401 mfa_invalid` - Invalid MFA code (if provided)
* `429 rate-limit-exceeded` - Too many login attempts

---

#### `POST /auth/login/start`

Two-step login: start phase. Validates credentials and returns login ticket if MFA required.

**Request:**

```json
{
  "emailOrUsername": "player@example.com",
  "password": "Str0ngP@ss!"
}
```

**Success Response - No MFA (200 OK):**

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "expiresInSeconds": 900,
  "displayName": "PlayerOne",
  "mfaEnabled": false
}
```

**Success Response - MFA Required (200 OK):**

```json
{
  "loginTicket": "temp_ticket_xyz123",
  "allowedFactors": ["TOTP"]
}
```

**Error Responses:**
* `401 invalid-credentials` - Wrong email/password
* `429 rate-limit-exceeded` - Too many attempts

---

#### `POST /auth/login/complete`

Two-step login: complete phase with MFA verification.

**Request:**

```json
{
  "loginTicket": "temp_ticket_xyz123",
  "factor": "TOTP",
  "code": "123456"
}
```

**Success Response (200 OK):**

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "expiresInSeconds": 900,
  "displayName": "PlayerOne",
  "mfaEnabled": true
}
```

**Error Responses:**
* `400 mfa_code_required` - No code provided
* `400 mfa_factor_not_allowed` - Factor not in allowedFactors
* `400 mfa_factor_unsupported` - Unrecognized factor type
* `401 mfa_challenge_invalid` - Login ticket expired or invalid
* `401 mfa_invalid` - Wrong TOTP code
* `429 rate-limit-exceeded` - Too many MFA attempts

---

#### `POST /auth/refresh`

Rotate refresh token. Returns new access + refresh tokens.

**Request:**

```json
{
  "refreshToken": "YXNkZmFzZGZhc2RmYXNkZmFzZGY"
}
```

**Success Response (200 OK):**

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "expiresInSeconds": 900,
  "displayName": "PlayerOne",
  "mfaEnabled": false
}
```

**Error Responses:**
* `400 validation-error` - Missing refresh token
* `401 invalid_refresh` - Token not found or revoked
* `401 expired_refresh` - Token expired
* `401 account_missing` - Account deleted

---

#### `POST /auth/logout`

Revoke a refresh token. Always succeeds (idempotent).

**Request:**

```json
{
  "refreshToken": "YXNkZmFzZGZhc2RmYXNkZmFzZGY"
}
```

**Success Response (200 OK):**

No body (HTTP 200).

**Error Responses:**

None - always succeeds even if token doesn't exist.

---

### MFA (TOTP)

**Authentication Required**: All MFA endpoints require a valid JWT Bearer token. The `accountId` is extracted from the token's `sub` claim.

---

#### `POST /auth/mfa/totp/setup`

Generate TOTP secret and QR code URL for authenticator app setup.

**Headers:**
```
Authorization: Bearer <access_token>
```

**Request:** (No body)

**Success Response (200 OK):**

```json
{
  "secretBase32": "JBSWY3DPEHPK3PXP...",
  "otpauthUrl": "otpauth://totp/Fortalis:acct:uuid?secret=JBSWY3DPEHPK3PXP&issuer=Fortalis",
  "backupCodes": []
}
```

Use `otpauthUrl` to generate a QR code for the user to scan with Google Authenticator, Authy, etc.

**Error Responses:**
* `401` - No/invalid Bearer token

---

#### `POST /auth/mfa/totp/enable?code=123456`

Enable TOTP MFA after verifying the code from the authenticator app.

**Headers:**
```
Authorization: Bearer <access_token>
```

**Query Parameters:**
* `code` (required) - 6-digit TOTP code from authenticator app

**Request:** (No body)

**Success Response (200 OK):**

No body (HTTP 200).

**Error Responses:**
* `400 validation-error` - Missing code parameter
* `401 mfa_invalid` - Invalid TOTP code
* `401` - No/invalid Bearer token

---

#### `POST /auth/mfa/totp/disable?code=123456`

Disable TOTP MFA. Requires current valid TOTP code to confirm.

**Headers:**
```
Authorization: Bearer <access_token>
```

**Query Parameters:**
* `code` (required) - Current 6-digit TOTP code

**Request:** (No body)

**Success Response (200 OK):**

No body (HTTP 200).

**Error Responses:**
* `400 validation-error` - Missing code parameter
* `401 mfa_invalid` - Invalid TOTP code
* `401` - No/invalid Bearer token
* `404` - MFA not enabled for this account

---

### JWKS

#### `GET /.well-known/jwks.json`

Public keys (JWK set) for RS256 verification.

**Request:** (No parameters)

**Success Response (200 OK):**

```json
{
  "keys": [
    {
      "kty": "RSA",
      "use": "sig",
      "alg": "RS256",
      "kid": "abc123...",
      "n": "...",
      "e": "AQAB"
    }
  ]
}
```

Game servers use this to verify JWT signatures without calling Auth service.

**Error Responses:**

None - publicly accessible.

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
