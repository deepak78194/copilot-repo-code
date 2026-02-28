# GitHub Copilot Instructions

These instructions are automatically applied to every Copilot interaction in this repository.

## Repository Overview

This is a structured AI-workflow experimentation lab. Its purpose is to design, evaluate, and improve
GitHub Copilot agent definitions, prompt templates, and orchestration patterns for production Java
and TypeScript backends.

**This repository is NOT a shipped application.** All generated code is for evaluation purposes.

---

## Workflow Mandate

Every non-trivial change follows the three-phase workflow enforced by the Orchestrator agent:

```
Plan  →  Implement  →  Review
```

- **Never** write implementation code before a plan exists.
- **Never** skip the Review phase.
- A task is **done** only when the Reviewer returns `APPROVED`.

Agent definitions live in `.copilot/agents/`. Skills live in `.copilot/skills/`. Prompt templates
live in `.copilot/prompt-library/`.

---

## Language & Runtime Conventions

### Java (primary)
- Java **21** — use records, sealed interfaces, pattern matching, text blocks, and `var` freely.
- Frameworks: **Micronaut 4.x** or **Spring Boot 3.x** as specified per experiment.
- Build: **Gradle** (Kotlin DSL). Maven acceptable.
- Dependencies declared in `libs.versions.toml` (version catalog).

### TypeScript (secondary)
- **Node 20 LTS**, strict mode (`"strict": true`), ES modules.
- Runtime: **Bun** preferred; Node acceptable.
- Framework: **Hono** for HTTP services; **Vitest** for testing.
- See `.copilot/skills/typescript.skill.md` for full conventions.

---

## Code-Quality Non-Negotiables

| Rule | Detail |
|------|--------|
| No field injection | Use constructor injection everywhere |
| No raw types | Every generic must be parameterised |
| No `println` / `console.log` in prod code | Use a structured logger |
| No hardcoded secrets or config | Externalise to env vars / config files |
| No stack traces in API responses | Map to structured error DTOs |
| No SQL string concatenation | Parameterised queries only |

---

## Testing Requirements

- Every public method → at least one unit test.
- Every REST endpoint → at least one integration test.
- Naming: `methodName_stateUnderTest_expectedBehavior`.
- Use `@DisplayName` for human-readable descriptions.
- Java: JUnit 5 + AssertJ + Mockito + Testcontainers.
- TypeScript: Vitest + supertest (or Hono test client).

---

## REST API Conventions

- **Versioned paths**: `/api/v1/...`
- **Nouns, plural, kebab-case**: `/user-profiles`, `/order-items`
- **Correct status codes**: 201 for creates, 204 for deletes, 422 for business-rule violations
- **Structured errors**:
  ```json
  { "code": "USER_NOT_FOUND", "message": "No user with id 42 exists." }
  ```
- Never return JPA entities — always map to a response DTO.

---

## Security Baseline

- Validate **all** inputs at the HTTP boundary.
- Sanitise before persistence; parameterise all queries.
- Never log PII, passwords, or tokens.
- Apply least-privilege to every service account or IAM role.
- See `.copilot/skills/security.skill.md` for full OWASP-aligned rules.

---

## Observability Baseline

- Use **structured JSON logs** in production.
- Emit a trace ID on every inbound request; propagate via headers.
- Define health (`/health`) and readiness (`/ready`) endpoints.
- See `.copilot/skills/observability.skill.md` for full conventions.

---

## Agent Roles (summary)

| Agent | File | Responsibility |
|-------|------|----------------|
| Orchestrator | `orchestrator.agent.md` | Sequences phases; manages handoffs |
| Planner | `planner.agent.md` | Decomposes requirements into tasks |
| Implementer | `implementer.agent.md` | Writes production code from a plan |
| Reviewer | `reviewer.agent.md` | Reviews code for correctness and security |
| Debugger | `debug.agent.md` | Root-cause analysis and targeted bug fixes |
| Security | `security.agent.md` | OWASP-aligned security audit |
