---
description: 'Fortalis Auth agent guide - Java 21 & Spring Boot 3'
applyTo: '**/*'
---

# Fortalis Auth Agent Guide

## Architecture

- Service is Spring Boot 3.3+ / Java 21; entry point `src/main/java/io/fortalis/fortalisauth/FortalisAuthApplication.java`.
- REST controllers in `src/main/java/io/fortalis/fortalisauth/controller` stay thin and delegate to services for validation/persistence.
- Services in `src/main/java/io/fortalis/fortalisauth/service` own transactions and rely on Spring Data repositories under `.../repo` for DB access.
- Follow hexagonal/clean architecture principles: isolate business logic from infrastructure concerns.
- Use constructor injection (preferred over field injection) for all dependencies.
## Auth & Tokens

- `AuthController` handles `/auth/**`; reuse `TokenService.issueTokens` and `TokenService.refresh` for JWT + refresh flows.
- `TokenService` signs JWTs through `JwtService` (Nimbus) and persists hashed refresh tokens; always rotate via `refresh` instead of mutating rows manually.
- `JwtService` builds RS256 tokens using PEMs loaded by `KeyProvider`; keep `auth.jwt.key-file-private/public` in sync with the PEMs in `keys/`.
- Token expiration times should be configurable and follow industry standards (access tokens: 15-30 min, refresh tokens: 7-30 days).
- Implement token revocation checks for critical operations.
## MFA

- `MfaService` manages TOTP setup, enable/disable, and verification; secrets are encrypted by `MfaCryptoService` when `crypto.mfa-encryption-key` (32-byte base64) is provided, otherwise passthrough for local dev.
- `TotpService` enforces RFC 6238 30-second windows and supports backup codes hashed via SHA-256; follow this flow when adding MFA entry points. (Backup codes are planned but not yet implemented in this repo.)
- Backup codes should be single-use and regenerable by the user.
- Rate limit MFA verification attempts to prevent brute force attacks.
## Data Model

- Entities live in `src/main/java/io/fortalis/fortalisauth/entity` and mirror `src/main/resources/db/migration/V1__initial_auth_schema.sql`; Postgres UUIDs come from `gen_random_uuid()` (requires `pgcrypto`).
- `Account` normalizes email addresses to lowercase in `@PrePersist/@PreUpdate`; preserve this behavior in registration changes.
- `RefreshToken` stores only token hashes; never persist or return raw refresh tokens from the database.
- Use `@Version` for optimistic locking on entities that may have concurrent updates.
- Implement soft deletes where data retention is required for audit trails.
- Use database constraints (unique, not null, check) to enforce data integrity at the database level.
## Error & Validation

- Throw `ApiException.badRequest/unauthorized` from services; `GlobalExceptionHandler` converts them to `ErrorResponse` records for JSON clients. If you decide to adopt RFC 7807 Problem Details, update the handler and DTOs consistently across the codebase; otherwise continue using `ErrorResponse`.
- DTOs under `src/main/java/io/fortalis/fortalisauth/dto` are Java records with `jakarta.validation` annotations; match this pattern for new request/response types.
- Prefer a consistent error shape; RFC 7807 is recommended for future adoption, but current implementation returns `ErrorResponse`.
- Never expose internal error details (stack traces, database errors) to clients in production.
- Log errors with appropriate severity levels; consider adding correlation IDs for tracing (not yet implemented here).
- Validation should happen at the controller level using `@Valid` and custom validators when needed.
## Rate Limiting

- `RateLimiterService` offers in-memory throttling (`checkAndConsume`/`clear`) for login/register; reuse it instead of ad-hoc throttling and note it is per-instance only.
- Consider distributed rate limiting (Redis-based) for production deployments with multiple instances.
- Implement different rate limits for different endpoints based on sensitivity.
- Current implementation throws `401 Unauthorized` when rate limits are exceeded; for production, prefer `429 Too Many Requests` and include a `Retry-After` header (update handlers and tests together when changing this behavior).
## Configuration

- Default settings live in `src/main/resources/application.yml`; override via env vars (`SPRING_DATASOURCE_*`, `AUTH_JWT_*`, `CRYPTO_*`) when scripting or deploying.
- `SecurityConfig` permits `/auth/**`, `/.well-known/**`, and `/actuator/health`; align new endpoints with this whitelist or secure them explicitly.
- Use `@ConfigurationProperties` with `@Validated` for type-safe configuration binding.
- Never commit secrets to version control; prefer environment variables or secret management services. Keys in `keys/` are for local dev only and should be git-ignored in real projects.
- Profile-specific configurations should be in separate files (e.g., `application-dev.yml`, `application-prod.yml`).

## Security Best Practices

- **Authentication**: Use Spring Security 6+ with JWT-based stateless authentication.
- **Authorization**: Implement method-level security with `@PreAuthorize` where fine-grained control is needed.
- **CORS**: Configure CORS explicitly and restrictively; avoid wildcard origins in production.
- **CSRF**: Disable for stateless REST APIs, but ensure all state-changing operations require authentication.
- **Headers**: Configure security headers (HSTS, X-Frame-Options, CSP, X-Content-Type-Options).
- **Passwords**: Use Argon2id via Spring Security (sane defaults) for password hashing.
- **Secrets Management**: Use Spring Cloud Config or HashiCorp Vault for production secrets.
- **Input Validation**: Validate all inputs at entry points; sanitize data before processing.
- **SQL Injection**: Use parameterized queries exclusively; Spring Data JPA does this by default.
- **Logging**: Never log sensitive data (passwords, tokens, PII) even in debug mode.

## Build & Tests

- Build with `./gradlew clean build`; run locally via `./gradlew bootRun` (needs local Postgres on `localhost:5433` and the PEM key pair in `keys/`).
- Tests use Spring Boot Testcontainers (`TestcontainersConfiguration` spins up Postgres); ensure Docker is running before `./gradlew test`.
- Add lightweight unit tests similar to `src/test/java/io/fortalis/fortalisauth/crypto/TotpServiceTest.java` for pure logic components.

### Testing Strategy

- **Unit Tests**: Test individual components in isolation using JUnit 5 and Mockito.
  - Mock external dependencies using `@Mock` and `@InjectMocks`.
  - Verify behavior, not implementation details.
  - Aim for 80%+ code coverage on business logic.
  
- **Integration Tests**: Test component interactions with `@SpringBootTest`.
  - Use Testcontainers for database and external service dependencies.
  - Test REST endpoints with `@WebMvcTest` or `TestRestTemplate`.
  - Use `@Sql` scripts to set up test data.
  
- **Security Tests**: Verify authentication and authorization rules.
  - Test with `@WithMockUser` and `@WithAnonymousUser`.
  - Verify proper HTTP status codes (401, 403).
  
- **Performance Tests**: Add benchmarks for critical paths.
  - Use JMH for microbenchmarks when needed.
  
- **Test Naming**: Use descriptive names: `should_ReturnUnauthorized_When_TokenIsExpired()`.
- **Test Data**: Use builders or fixtures for consistent test data creation.
- **Assertions**: Use AssertJ for fluent and readable assertions.

## Development Tips

- When adding persistence logic, prefer repository methods over manual EntityManager work; query derivation is the norm.
- Logging defaults to DEBUG for `io.fortalis.fortalisauth`; follow existing `Slf4j` usage (see `AuthController`) for structured context like emails/IPs. Avoid logging secrets.
- Responses should remain immutable and explicit; reuse `AuthResponse` for token exchanges and avoid exposing entity classes over HTTP.
- Use Spring Boot DevTools for faster development cycles with automatic restarts.
- Enable actuator endpoints for health checks and metrics in non-production environments.

## Java 21 Best Practices

### Modern Language Features

- **Records**: For classes primarily intended to store data (e.g., DTOs, immutable data structures), **Java Records should be used instead of traditional classes**.
  - Records are final and immutable by default.
  - Automatically generates constructors, getters, `equals()`, `hashCode()`, and `toString()`.
  - Use compact constructors for validation: `public record User(String name) { public User { Objects.requireNonNull(name); } }`
  
- **Sealed Classes**: Use sealed classes/interfaces to restrict inheritance and create exhaustive hierarchies.
  ```java
  public sealed interface Result permits Success, Failure {}
  public record Success(String data) implements Result {}
  public record Failure(String error) implements Result {}
  ```
  
- **Pattern Matching**: Utilize pattern matching for `instanceof` and `switch` expressions to simplify conditional logic and type casting.
  ```java
  // Pattern matching for instanceof
  if (obj instanceof String s && s.length() > 5) {
      return s.toUpperCase();
  }
  
  // Pattern matching in switch (Java 21)
  return switch (result) {
      case Success(var data) -> processSuccess(data);
      case Failure(var error) -> handleError(error);
  };
  ```
  
- **Type Inference**: Use `var` for local variable declarations to improve readability, but only when the type is explicitly clear from the right-hand side of the expression.
  ```java
  var users = repository.findAll();  // Clear: returns List<User>
  var count = users.size();          // Clear: int
  ```
  
- **Text Blocks**: Use text blocks for multi-line strings (SQL, JSON, HTML).
  ```java
  var sql = """
      SELECT u.id, u.name, u.email
      FROM users u
      WHERE u.active = true
      ORDER BY u.created_at DESC
      """;
  ```
  
- **Virtual Threads**: Leverage virtual threads (Project Loom) for high-concurrency scenarios.
  - Configure Spring Boot to use virtual threads: `spring.threads.virtual.enabled=true`
  - Use for I/O-bound operations, not CPU-bound tasks.
  - Combine with structured concurrency for safer concurrent programming.

### Immutability & Null Safety

- **Immutability**: Favor immutable objects. Make classes and fields `final` where possible.
  - Use collections from `List.of()`/`Map.of()` for fixed data.
  - Use `Stream.toList()` to create unmodifiable lists.
  - Prefer `Collections.unmodifiable*()` wrappers when returning collections.
  
- **Null Handling**: Avoid returning or accepting `null`.
  - Use `Optional<T>` for possibly-absent values in return types.
  - Use `Objects.requireNonNull()` to validate non-null parameters early.
  - Use `Objects.requireNonNullElse()` for default values.
  - Never use `Optional` for fields, method parameters, or collections.
  ```java
  // Good
  public Optional<User> findById(UUID id) {
      return repository.findById(id);
  }
  
  // Bad
  public User findById(UUID id) {
      return repository.findById(id).orElse(null);
  }
  ```

### Functional Programming

- **Streams and Lambdas**: Use the Streams API and lambda expressions for collection processing.
  - Employ method references (e.g., `stream.map(User::getName)`).
  - Prefer streams for declarative data transformations.
  - Avoid side effects in stream operations.
  ```java
  var activeEmails = users.stream()
      .filter(User::isActive)
      .map(User::getEmail)
      .collect(Collectors.toSet());
  ```
  
- **Functional Interfaces**: Use built-in functional interfaces (`Function`, `Predicate`, `Consumer`, `Supplier`).
- **Optional Operations**: Chain Optional operations for cleaner null handling.
  ```java
  return optionalUser
      .filter(User::isActive)
      .map(User::getEmail)
      .orElseThrow(() -> new ApiException.notFound("User not found"));
  ```

### Naming Conventions

Follow Google's Java Style Guide:

- **Classes & Interfaces**: `UpperCamelCase` (e.g., `UserService`, `AuthenticationProvider`)
- **Methods & Variables**: `lowerCamelCase` (e.g., `getUserById`, `isActive`)
- **Constants**: `UPPER_SNAKE_CASE` (e.g., `MAX_LOGIN_ATTEMPTS`, `DEFAULT_TIMEOUT`)
- **Packages**: `lowercase` (e.g., `io.fortalis.fortalisauth.service`)
- **Type Parameters**: Single uppercase letter (e.g., `T`, `E`, `K`, `V`)

Naming Guidelines:
- Use nouns for classes (`UserService`, `Account`)
- Use verbs for methods (`getUserById`, `validateToken`, `processRequest`)
- Boolean methods should start with `is`, `has`, `can`, or `should`
- Avoid abbreviations and Hungarian notation
- Be descriptive: prefer `userRepository` over `repo`

## Code Quality Rules

### Critical Bug Patterns

| Rule ID | Description | Example / Fix |
|---------|-------------|---------------|
| `S2095` | Resources should be closed | Use try-with-resources for `InputStream`, `OutputStream`, `Connection`, etc. |
| `S1698` | Use `.equals()` instead of `==` for objects | Always use `.equals()` for String and wrapper types comparison |
| `S1905` | Remove redundant casts | Clean up unnecessary type casts |
| `S3518` | Conditions should not always evaluate to the same value | Avoid `if (true)` or infinite loops |
| `S108` | Remove unreachable code | Code after `return`, `throw`, `break` is unreachable |
| `S2259` | Null pointer dereference | Check for null before calling methods |
| `S2583` | Conditions should not unconditionally evaluate to true/false | Review logic for dead code branches |
| `S1656` | Variables should not be self-assigned | Typo check: `this.name = this.name` is likely wrong |

### Code Smells

| Rule ID | Description | Recommendation |
|---------|-------------|----------------|
| `S107` | Too many parameters | Max 4-5 parameters; refactor to parameter object or builder |
| `S1121` | Duplicate code blocks | Extract to shared methods; follow DRY principle |
| `S138` | Methods should not be too long | Keep methods under 50 lines; extract helper methods |
| `S3776` | Reduce cognitive complexity | Simplify nested logic; max complexity of 15 |
| `S1192` | Don't duplicate string literals | Use constants or `enum` for repeated strings |
| `S1854` | Remove unused assignments | Dead code detection; remove or refactor |
| `S109` | Replace magic numbers with constants | Use named constants for clarity |
| `S1188` | Catch blocks should not be empty | Always log exceptions or rethrow |
| `S1118` | Utility classes should not have public constructors | Make constructor private |
| `S1125` | Boolean literals should not be redundant | `if (condition == true)` → `if (condition)` |

### Modern Java Patterns

- **Prefer Composition over Inheritance**: Use delegation and interfaces.
- **Single Responsibility Principle**: Each class should have one reason to change.
- **Dependency Injection**: Use Spring's DI; avoid manual instantiation.
- **Fail Fast**: Validate inputs early; throw exceptions immediately on invalid state.
- **Defensive Copying**: Make defensive copies of mutable inputs/outputs when needed.
- **Builder Pattern**: Use for objects with many optional parameters.
  ```java
  public record CreateUserRequest(
      String email,
      String password,
      String firstName,
      String lastName,
      Optional<String> phoneNumber
  ) {
      public static Builder builder() {
          return new Builder();
      }
      
      public static class Builder {
          // Builder implementation
      }
  }
  ```

## Modularity & Extensibility

- **Package Structure**: Organize by feature, not by layer.
  ```
  io.fortalis.fortalisauth
  ├── auth (feature module)
  │   ├── AuthController
  │   ├── AuthService
  │   ├── dto
  │   └── entity
  ├── user (feature module)
  ├── mfa (feature module)
  └── shared (common utilities)
  ```
  
- **Interfaces**: Define interfaces for external dependencies and complex services.
  - Enables easier testing and implementation swapping.
  - Use meaningful interface names (avoid `I` prefix).
  
- **Strategy Pattern**: Use for interchangeable algorithms (e.g., different MFA methods).
  - Provide default implementations in abstract classes.
- **Factory Pattern**: Use for complex object creation.
- **Event-Driven**: Consider Spring Events for decoupled component communication.
  ```java
  @Component
  public class AuthEventPublisher {
      private final ApplicationEventPublisher publisher;
      
      public void publishLoginSuccess(UUID userId) {
          publisher.publishEvent(new LoginSuccessEvent(userId));
      }
  }
  ```
  
- **Extension Points**: Design services with extensibility in mind.
  - Use protected methods for hook points.

## Observability & Monitoring

- **Logging**:
  - Use SLF4J with Logback.
  - Consider including correlation IDs for request tracing (MDC), especially in production.
  - Log at appropriate levels: ERROR (issues requiring immediate attention), WARN (potential issues), INFO (significant events), DEBUG (detailed flow).
  - Never log sensitive data (passwords, tokens, SSNs, credit cards).
  - Use structured logging (JSON format) in production.
  ```java
  @Slf4j
  public class AuthService {
      public void login(String email) {
          log.info("Login attempt for email: {}", email);
          // Process login
          log.debug("Token generated for user: {}", userId);
      }
  }
  ```
  
- **Metrics**:
  - Use Micrometer for application metrics.
  - Track custom metrics for business events (logins, registrations, MFA attempts).
  - Monitor JVM metrics (heap, GC, threads).
  
- **Health Checks**:
  - Implement custom health indicators for critical dependencies.
  - Use `@HealthIndicator` for database, external services.
  
- **Distributed Tracing**:
  - Integrate with Micrometer Tracing (OpenTelemetry).
  - Include trace IDs in logs and responses.

## API Design

- **RESTful Principles**: Follow REST conventions for resource naming and HTTP methods.
  - Use plural nouns for collections: `/users`, `/tokens`
  - Use HTTP methods semantically: GET (read), POST (create), PUT (update), PATCH (partial update), DELETE (delete)
  
- **Versioning**: Use URI versioning (`/api/v1/...`) for breaking changes.
- **Pagination**: Implement pagination for list endpoints.
  ```java
  @GetMapping("/users")
  public Page<UserDto> getUsers(Pageable pageable) {
      return userService.findAll(pageable);
  }
  ```
  
- **Response Format**: Keep responses explicit and immutable. This project returns plain DTOs (e.g., `AuthResponse`) rather than a `data/meta/errors` envelope. If you introduce envelopes, do so consistently across endpoints.
  
- **HATEOAS**: Consider hypermedia links for resource navigation (when appropriate).
- **Idempotency**: Ensure PUT and DELETE are idempotent; consider idempotency keys for POST.

## Performance Optimization

- **Database**:
  - Use appropriate indexes on frequently queried columns.
  - Fetch only needed columns with projections.
  - Use `@EntityGraph` or JOIN FETCH to avoid N+1 queries.
  - Enable query logging in development to identify slow queries.
  - Use connection pooling (HikariCP is default in Spring Boot).
  
- **Caching**:
  - Use Spring Cache abstraction with appropriate TTLs.
  - Cache read-heavy, infrequently changing data.
  - Use Redis for distributed caching in production.
  ```java
  @Cacheable(value = "users", key = "#id")
  public User getUserById(UUID id) {
      return repository.findById(id).orElseThrow();
  }
  ```
  
- **Async Processing**:
  - Use `@Async` for long-running operations.
  - Implement async endpoints for non-blocking I/O.
  - Configure appropriate thread pools.
  
- **Lazy Loading**: Be cautious with JPA lazy loading; prefer explicit fetching strategies.

## Documentation

- **Javadoc**: Write Javadoc for public APIs and complex methods.
  - Explain the "why," not just the "what."
  - Document parameters, return values, and exceptions.
  
- **OpenAPI/Swagger**: Generate API documentation with Springdoc OpenAPI.
  - Annotate controllers with `@Operation`, `@ApiResponse`.
  - Keep documentation synchronized with code.
  
- **README**: Maintain comprehensive README with setup instructions.
- **Architecture Decision Records (ADRs)**: Document significant design decisions.

## Build and Verification

- After adding or modifying code, verify the project continues to build successfully.
- The project uses Gradle; run `./gradlew build`.
- Ensure all tests pass as part of the build.
- Run `./gradlew check` to execute all verification tasks (tests, code quality checks).
- Use `./gradlew bootRun` for local development.
- Configure pre-commit hooks for automated checks (formatting, linting).

## Continuous Integration

- All commits should trigger CI pipeline.
- CI should include:
  - Compilation
  - Unit and integration tests
  - Code coverage analysis (minimum 80% for business logic)
  - Static code analysis (SonarQube, SpotBugs)
  - Security scanning (OWASP Dependency Check, Snyk)
  - Container image building and scanning
  
## Deployment

- Use containerization (Docker) for consistent deployments.
- Follow 12-factor app principles.
- Externalize configuration; use environment-specific values.
- Implement blue-green or rolling deployments for zero downtime.
- Use health checks and readiness probes in orchestration platforms.
- Implement graceful shutdown to finish in-flight requests.

---

## Quick Reference

### Common Tasks

**Adding a new REST endpoint:**
1. Create DTO record with validation annotations
2. Add method to service interface/class with business logic
3. Implement repository method if database access needed
4. Create controller method delegating to service
5. Add integration tests for the endpoint
6. Update OpenAPI documentation

**Adding a new configuration property:**
1. Define in `application.yml` with sensible default
2. Create `@ConfigurationProperties` record with `@Validated`
3. Document the property and its purpose
4. Add to environment-specific configuration files

**Adding a new database entity:**
1. Create entity class with appropriate annotations
2. Create migration script in `db/migration`
3. Create repository interface
4. Add service methods for business operations
5. Write unit and integration tests
6. Consider indexing strategy

**Handling a new exception type:**
1. Create specific exception class extending appropriate base
2. Add handler method in `GlobalExceptionHandler`
3. Return appropriate HTTP status and error response
4. Add tests for error scenarios

---

*This guide is a living document. Update it as the project evolves and new best practices emerge.*
