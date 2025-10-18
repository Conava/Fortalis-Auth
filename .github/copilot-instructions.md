---
description: 'Fortalis Auth agent guide'
applyTo: '**/*'
---

# Fortalis Auth Agent Guide
## Architecture
- Service is Spring Boot 3 / Java 21; entry point `src/main/java/io/fortalis/fortalisauth/FortalisAuthApplication.java`.
- REST controllers in `src/main/java/io/fortalis/fortalisauth/controller` stay thin and delegate to services for validation/persistence.
- Services in `src/main/java/io/fortalis/fortalisauth/service` own transactions and rely on Spring Data repositories under `.../repo` for DB access.
## Auth & Tokens
- `AuthController` handles `/auth/**`; reuse `TokenService.issueTokens` and `TokenService.refresh` for JWT + refresh flows.
- `TokenService` signs JWTs through `JwtService` (Nimbus) and persists hashed refresh tokens; always rotate via `refresh` instead of mutating rows manually.
- `JwtService` builds RS256 tokens using PEMs loaded by `KeyProvider`; keep `auth.jwt.key-file-private/public` in sync with the PEMs in `keys/`.
## MFA
- `MfaService` manages TOTP setup, enable/disable, and verification; secrets are encrypted by `MfaCryptoService` when `crypto.mfa-encryption-key` (32-byte base64) is provided, otherwise passthrough for local dev.
- `TotpService` enforces RFC 6238 30-second windows and supports backup codes hashed via SHA-256; follow this flow when adding MFA entry points.
## Data Model
- Entities live in `src/main/java/io/fortalis/fortalisauth/entity` and mirror `src/main/resources/db/migration/V1__initial_auth_schema.sql`; Postgres UUIDs come from `gen_random_uuid()` (requires `pgcrypto`).
- `Account` normalizes email addresses to lowercase in `@PrePersist/@PreUpdate`; preserve this behavior in registration changes.
- `RefreshToken` stores only token hashes; never persist or return raw refresh tokens from the database.
## Error & Validation
- Throw `ApiException.badRequest/unauthorized` from services; `GlobalExceptionHandler` converts them to `ErrorResponse` records for JSON clients.
- DTOs under `src/main/java/io/fortalis/fortalisauth/dto` are Java records with `jakarta.validation` annotations; match this pattern for new request/response types.
## Rate Limiting
- `RateLimiterService` offers in-memory throttling (`checkAndConsume`/`clear`) for login/register; reuse it instead of ad-hoc throttling and note it is per-instance only.
## Configuration
- Default settings live in `src/main/resources/application.yml`; override via env vars (`SPRING_DATASOURCE_*`, `AUTH_JWT_*`, `CRYPTO_*`) when scripting or deploying.
- `SecurityConfig` permits `/auth/**`, `/.well-known/**`, and `/actuator/health`; align new endpoints with this whitelist or secure them explicitly.
## Build & Tests
- Build with `./gradlew clean build`; run locally via `./gradlew bootRun` (needs local Postgres on `localhost:5433` and the PEM key pair in `keys/`).
- Tests use Spring Boot Testcontainers (`TestcontainersConfiguration` spins up Postgres); ensure Docker is running before `./gradlew test`.
- Add lightweight unit tests similar to `src/test/java/io/fortalis/fortalisauth/crypto/TotpServiceTest.java` for pure logic components.
## Development Tips
- When adding persistence logic, prefer repository methods over manual EntityManager work; query derivation is the norm.
- Logging defaults to DEBUG for `io.fortalis.fortalisauth`; follow existing `Slf4j` usage (see `AuthController`) for structured context like emails/IPs.
- Responses should remain immutable and explicit; reuse `AuthResponse` for token exchanges and avoid exposing entity classes over HTTP.

## Best practices

- **Records**: For classes primarily intended to store data (e.g., DTOs, immutable data structures), **Java Records should be used instead of traditional classes**.
- **Pattern Matching**: Utilize pattern matching for `instanceof` and `switch` expression to simplify conditional logic and type casting.
- **Type Inference**: Use `var` for local variable declarations to improve readability, but only when the type is explicitly clear from the right-hand side of the expression.
- **Immutability**: Favor immutable objects. Make classes and fields `final` where possible. Use collections from `List.of()`/`Map.of()` for fixed data. Use `Stream.toList()` to create immutable lists.
- **Streams and Lambdas**: Use the Streams API and lambda expressions for collection processing. Employ method references (e.g., `stream.map(Foo::toBar)`).
- **Null Handling**: Avoid returning or accepting `null`. Use `Optional<T>` for possibly-absent values and `Objects` utility methods like `equals()` and `requireNonNull()`.

### Naming Conventions

- Follow Google's Java style guide:
  - `UpperCamelCase` for class and interface names.
  - `lowerCamelCase` for method and variable names.
  - `UPPER_SNAKE_CASE` for constants.
  - `lowercase` for package names.
- Use nouns for classes (`UserService`) and verbs for methods (`getUserById`).
- Avoid abbreviations and Hungarian notation.

### Bug Patterns

| Rule ID | Description                                                 | Example / Notes                                                                                  |
| ------- | ----------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| `S2095` | Resources should be closed                                  | Use try-with-resources when working with streams, files, sockets, etc.                           |
| `S1698` | Objects should be compared with `.equals()` instead of `==` | Especially important for Strings and boxed primitives.                                           |
| `S1905` | Redundant casts should be removed                           | Clean up unnecessary or unsafe casts.                                                            |
| `S3518` | Conditions should not always evaluate to true or false      | Watch for infinite loops or if-conditions that never change.                                     |
| `S108`  | Unreachable code should be removed                          | Code after `return`, `throw`, etc., must be cleaned up.                                          |

## Code Smells

| Rule ID | Description                                            | Example / Notes                                                               |
| ------- | ------------------------------------------------------ | ----------------------------------------------------------------------------- |
| `S107`  | Methods should not have too many parameters            | Refactor into helper classes or use builder pattern.                          |
| `S121`  | Duplicated blocks of code should be removed            | Consolidate logic into shared methods.                                        |
| `S138`  | Methods should not be too long                         | Break complex logic into smaller, testable units.                             |
| `S3776` | Cognitive complexity should be reduced                 | Simplify nested logic, extract methods, avoid deep `if` trees.                |
| `S1192` | String literals should not be duplicated               | Replace with constants or enums.                                              |
| `S1854` | Unused assignments should be removed                   | Avoid dead variables—remove or refactor.                                      |
| `S109`  | Magic numbers should be replaced with constants        | Improves readability and maintainability.                                     |
| `S1188` | Catch blocks should not be empty                       | Always log or handle exceptions meaningfully.                                 |

## Build and Verification

- After adding or modifying code, verify the project continues to build successfully.
- The project uses Gradle, run `./gradlew build`.
- Ensure all tests pass as part of the build.