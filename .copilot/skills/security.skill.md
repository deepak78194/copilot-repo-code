# Security Skill

This skill defines secure-coding rules, patterns, and verification checklists for this repository.
Reference it when implementing any feature that handles user input, authentication, authorisation,
or sensitive data.

## OWASP Alignment

All rules are derived from the **OWASP Top 10 (2021)** and the **OWASP Java Cheat Sheet Series**.
When in doubt, apply the most restrictive interpretation.

---

## Input Validation

**Rule:** Validate ALL inputs at the HTTP boundary before any processing.

```java
// Good — Bean Validation on the request DTO
public record CreateUserRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(max = 100) String name
) {}

// Bad — validating inside service logic, too late
public User createUser(CreateUserRequest req) {
    if (req.email() == null) throw new IllegalArgumentException(...);
}
```

**Rule:** Validate on BOTH the DTO level (Bean Validation) AND the domain level (business rules).

---

## Injection Prevention

### SQL / JPQL
- **Never** concatenate user input into a query string.
- Use named parameters in all JPQL/Criteria queries.
- Use parameterised native queries if raw SQL is unavoidable.

```java
// Good
@Query("SELECT u FROM User u WHERE u.email = :email")
Optional<User> findByEmail(@Param("email") String email);

// Bad — JPQL injection possible
String jpql = "SELECT u FROM User u WHERE u.email = '" + email + "'";
```

### Command Injection
- Never pass user input to `Runtime.exec()`, `ProcessBuilder`, or shell commands.
- If a subprocess is required, use an allowlist for arguments.

### Log Injection
- Sanitise user-controlled strings before logging (strip `\n`, `\r`, ANSI codes).
- Use a structured logger (SLF4J + Logback) with parameterised messages, never string concatenation.

```java
// Good
log.info("User login attempt for email={}", email);

// Bad — log injection via crafted email
log.info("User login attempt for email=" + email);
```

---

## Authentication & Session Management

- Tokens must be **short-lived**: access tokens ≤ 15 min, refresh tokens ≤ 7 days.
- Validate token signature, expiry, `iss`, and `aud` claims on every request.
- Use `HttpOnly`, `Secure`, and `SameSite=Strict` cookies if storing tokens in cookies.
- Invalidate sessions on logout and on password change.
- Implement rate limiting on login and token refresh endpoints.

```java
// JWT validation (Spring Security / jjwt)
Jwts.parserBuilder()
    .setSigningKey(signingKey)
    .requireIssuer("https://auth.example.com")
    .requireAudience("user-service")
    .build()
    .parseClaimsJws(token);
```

---

## Authorisation

- Apply a **deny-by-default** policy: every endpoint requires explicit permission.
- Check authorisation at the **service layer**, not just the controller.
- Use attribute-based access control (ABAC) for resource ownership checks.

```java
// Good — service checks ownership
public OrderResponse getOrder(Long orderId, Long requestingUserId) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new OrderNotFoundException(orderId));
    if (!order.getUserId().equals(requestingUserId)) {
        throw new AccessDeniedException("Not the order owner");
    }
    return mapper.toResponse(order);
}
```

---

## Sensitive Data Handling

| Data Type | Rule |
|-----------|------|
| Passwords | Hash with **bcrypt** (cost ≥ 12) or **Argon2id**. Never store plaintext or MD5/SHA hashes. |
| PII (email, phone, SSN) | Encrypt at rest with AES-256-GCM. Mask in logs. |
| API keys / secrets | Never commit. Inject via environment variables or a secrets manager. |
| Financial data | Encrypt at rest. Log only the last 4 digits of card numbers. |

---

## Cryptography

- Use **TLS 1.2+** for all network communication.
- Use **AES-256-GCM** for symmetric encryption.
- Use **RSA-2048** or **ECDSA-P256** for asymmetric operations.
- Never use **MD5**, **SHA-1**, **DES**, or **ECB** mode.
- Generate secrets with `SecureRandom`, never `Random`.

```java
// Good
SecureRandom random = new SecureRandom();
byte[] salt = new byte[16];
random.nextBytes(salt);

// Bad
byte[] salt = new byte[16];
new Random().nextBytes(salt);
```

---

## Dependency Management

- Run `./gradlew dependencyCheckAnalyze` (OWASP Dependency-Check) before every release.
- Fail the build on **CVSS ≥ 7.0** vulnerabilities.
- Pin dependency versions; do not use `+` or `latest` version ranges.
- Review transitive dependencies when adding a new direct dependency.

---

## Security Checklist (pre-merge)

- [ ] All inputs validated with Bean Validation + domain rules
- [ ] No raw SQL or JPQL string concatenation
- [ ] No sensitive data in logs (email, token, password, card number)
- [ ] Authorisation checked at service layer for every data-access operation
- [ ] Secrets injected from environment; not in source or config files
- [ ] Token validation includes signature, expiry, issuer, and audience
- [ ] OWASP Dependency-Check passes with no HIGH/CRITICAL CVEs
- [ ] Error responses contain `code` and `message` only — no stack traces

---

## Constraints

- Never bypass security checks in test helpers — write test-specific users/tokens instead.
- Never disable CSRF protection or CORS wildcard origins in production configuration.
- Never store credentials in `application.yml` — use environment variables or a vault.
- Never expose internal IDs (database PKs) in URLs if they are sequential — use UUIDs.
