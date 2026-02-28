# Security Review Prompt Template

Use this prompt to run an OWASP-aligned security audit on a feature branch or a set of changed
files before merging to the main branch.

## When to Use

- Before merging a PR that touches authentication, authorisation, or session management.
- Before merging a PR that introduces new user inputs, file uploads, or external calls.
- Before any release candidate is promoted to production.
- Periodically (at least once per sprint) on the full codebase.

---

## Prompt

```
You are the Orchestrator agent. Run the Security Review workflow for the following code change.

## Change Description
{describe_what_changed_and_why}

## Changed Files
{comma_separated_file_paths_or_diff}

## Technology Stack
- Language: {java_21_or_typescript}
- Framework: {micronaut_or_spring_or_hono}
- Database: {postgresql}
- Auth mechanism: {jwt_or_oauth2_or_session}

## Phase 1 — Security Audit
Invoke the Security agent:
- Run the full OWASP Top 10 checklist against the changed files.
- Identify all findings with severity, exploit scenario, and remediation.

## Phase 2 — Remediation (if findings exist)
Invoke the Implementer agent for each CRITICAL or HIGH finding:
- Apply the remediation described by the Security agent.
- Do not change unrelated code.

## Phase 3 — Re-audit
Invoke the Security agent again on the remediated code:
- Confirm all CRITICAL and HIGH findings are resolved.
- Confirm no new findings were introduced.

## Constraints
- CRITICAL and HIGH findings must be resolved before the PR can merge.
- MEDIUM findings must be tracked (create issues) but do not block merge.
- LOW and INFO findings are noted but require no immediate action.
- See `.copilot/agents/security.agent.md` and `.copilot/skills/security.skill.md`.

## Output Format
### Phase 1 — Initial Audit
(Security agent output: OWASP checklist + findings)

### Phase 2 — Remediation: FINDING-{n}
(Implementer output: code change for each critical/high finding)

### Phase 3 — Re-audit
(Security agent output: updated checklist + verdict)

### Final Verdict
CLEAR / FINDINGS REMAINING — (list any unresolved medium/low findings as tracked issues)
```

---

## Example

**Change:** Added a `POST /api/v1/password-reset` endpoint that accepts an email address, looks up
the user, and sends a password-reset link.

**Security findings:**
1. **[HIGH] A07 — Auth Failure**: The reset token is generated with `Math.random()` (predictable).
   **Remediation:** Replace with `SecureRandom.generateSeed(32)` and Base64-encode.
2. **[MEDIUM] A09 — Logging Failure**: The email is logged at INFO level during the reset flow,
   exposing PII.
   **Remediation:** Replace `log.info("Reset requested for {}", email)` with
   `log.info("Password reset requested userId={}", userId)`.
3. **[LOW] A05 — Security Misconfiguration**: The endpoint does not enforce rate limiting.
   **Remediation:** Add a `@RateLimited(requests=5, per=Duration.ofMinutes(1))` annotation or
   configure a gateway-level rate limit.

**Verdict after remediation:** CLEAR (HIGH resolved; MEDIUM resolved; LOW tracked as issue #47).
