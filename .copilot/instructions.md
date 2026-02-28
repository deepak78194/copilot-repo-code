# Global Copilot Instructions

These instructions apply to all Copilot interactions in this repository.

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

- Java version: 21
- Frameworks: Micronaut 4.x or Spring Boot 3.x (as specified per experiment)
- Build tool: Gradle (Kotlin DSL) preferred; Maven acceptable
- Testing: JUnit 5, AssertJ, Mockito
- Database: PostgreSQL with Flyway migrations

## Code Style

- Follow Google Java Style Guide.
- Class names: PascalCase. Method and variable names: camelCase. Constants: UPPER_SNAKE_CASE.
- Keep methods short (≤ 20 lines preferred). Extract helpers liberally.
- No raw types. No unchecked casts without a comment justifying the cast.
- All public API methods must have Javadoc.

## Testing Standards

- Every new feature must have at least one unit test and one integration test.
- Test method naming: `methodName_stateUnderTest_expectedBehavior`.
- Use `@DisplayName` for human-readable test descriptions.
- Avoid testing implementation details; test behavior and contracts.

## REST API Standards

- Use standard HTTP verbs (GET, POST, PUT, PATCH, DELETE) correctly.
- Return appropriate HTTP status codes (200, 201, 204, 400, 404, 409, 500).
- All request/response bodies use JSON.
- API paths use kebab-case (e.g., `/user-profiles`).
- Validate all inputs; return structured error responses with `message` and `code` fields.

## Security

- Never log sensitive data (passwords, tokens, PII).
- Validate and sanitize all inputs before use.
- Use parameterized queries; never concatenate SQL strings.
- Do not commit secrets, credentials, or API keys.

## Agent Interaction

- When acting as the **planner**: produce a numbered task list with acceptance criteria.
- When acting as the **implementer**: implement exactly what the plan specifies; do not add unrequested features.
- When acting as the **reviewer**: check for correctness, test coverage, style, and security; produce a structured report.
- When acting as the **orchestrator**: enforce the workflow sequence; do not skip phases.

## Prompt Engineering Guidelines

- Provide role context at the top of every prompt (e.g., "You are the implementer agent...").
- Include relevant skill files as context when asking for domain-specific output.
- Specify the expected output format explicitly.
- Use `## Constraints` sections to limit scope and prevent over-generation.
