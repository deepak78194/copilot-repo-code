# Global Copilot Instructions

These instructions apply to all Copilot interactions in this repository.
The primary instructions file is `.github/copilot-instructions.md` (auto-applied by GitHub Copilot).
This file extends those instructions with additional detail.

## Repository Purpose

This is an AI workflow experimentation lab. It is NOT an application to be shipped.
All code generated here is for learning, evaluation, and workflow design purposes.

## General Behavior

- Always follow the **plan → implement → review** workflow.
- Do not implement anything that has not been planned first.
- Do not skip the review phase.
- Prefer **explicit, minimal, readable** code over clever or abbreviated code.
- When in doubt, ask a clarifying question rather than assuming.

## Language and Framework Conventions

- Java version: 21 — use records, sealed interfaces, pattern matching, and text blocks.
- Frameworks: Micronaut 4.x or Spring Boot 3.x (as specified per experiment)
- Build tool: Gradle (Kotlin DSL) preferred; Maven acceptable
- Testing: JUnit 5, AssertJ, Mockito, Testcontainers
- Database: PostgreSQL with Flyway migrations
- TypeScript: Node 20 LTS, strict mode, Hono, Zod, Drizzle, Vitest (see `typescript.skill.md`)

## Code Style

- Follow Google Java Style Guide.
- Class names: PascalCase. Method and variable names: camelCase. Constants: UPPER_SNAKE_CASE.
- Keep methods short (≤ 20 lines preferred). Extract helpers liberally.
- No raw types. No unchecked casts without a comment justifying the cast.
- All public API methods must have Javadoc.
- Never call `Optional.get()` — always use `orElseThrow()` with a domain exception.

## Testing Standards

- Every new feature must have at least one unit test and one integration test.
- Test method naming: `methodName_stateUnderTest_expectedBehavior`.
- Use `@DisplayName` for human-readable test descriptions.
- Avoid testing implementation details; test behaviour and contracts.
- Use Testcontainers with a PostgreSQL container for integration tests.

## REST API Standards

- Version APIs via path: `/api/v1/...`
- Use standard HTTP verbs (GET, POST, PUT, PATCH, DELETE) correctly.
- Return appropriate HTTP status codes (200, 201, 204, 400, 404, 409, 422, 500).
- All request/response bodies use JSON.
- API paths use kebab-case (e.g., `/user-profiles`).
- Validate all inputs; return structured error responses with `code` and `message` fields.
- Never return JPA entities from controllers — always map to a DTO.

## Security

- Never log sensitive data (passwords, tokens, PII).
- Validate and sanitise all inputs before use.
- Use parameterised queries; never concatenate SQL strings.
- Do not commit secrets, credentials, or API keys.
- Apply a deny-by-default authorisation policy.
- See `.copilot/skills/security.skill.md` for the full OWASP-aligned ruleset.

## Observability

- Use structured JSON logs (SLF4J + Logback Logstash encoder).
- Propagate `traceId` via MDC to every log line within a request.
- Expose `/health` (liveness) and `/ready` (readiness) endpoints.
- See `.copilot/skills/observability.skill.md` for full conventions.

## Agent Interaction

- When acting as the **planner**: produce a numbered task list with acceptance criteria.
- When acting as the **implementer**: implement exactly what the plan specifies; do not add unrequested features.
- When acting as the **reviewer**: check for correctness, test coverage, style, and security; produce a structured report.
- When acting as the **orchestrator**: enforce the workflow sequence; do not skip phases.
- When acting as the **debugger**: perform root-cause analysis; write a minimal failing test; apply the targeted fix.
- When acting as the **security agent**: run the OWASP Top 10 checklist; rate findings by CVSS severity; provide exploit scenarios.

## Prompt Engineering Guidelines

- Provide role context at the top of every prompt (e.g., "You are the implementer agent...").
- Include relevant skill files as context when asking for domain-specific output.
- Specify the expected output format explicitly.
- Use `## Constraints` sections to limit scope and prevent over-generation.
