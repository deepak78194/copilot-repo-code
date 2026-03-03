# Plan: New REST Endpoint — user-service

## Feature
Add a new REST endpoint to the `user-service`.

## Requirements
- Follow versioned path convention: `/api/v1/...`
- Use plural, kebab-case nouns (e.g. `/user-profiles`, `/order-items`)
- Return response DTOs — never expose JPA entities directly
- Use constructor injection (no field injection)
- Map business-rule violations to HTTP 422 with structured errors:
  ```json
  { "code": "USER_NOT_FOUND", "message": "No user with id 42 exists." }
  ```
- Validate all inputs at the HTTP boundary
- No hardcoded config or secrets

## Testing Scope
- **Unit tests** — every public method in service and controller layers
- **Integration tests** — at least one test per endpoint using Testcontainers
- **Contract tests** — verify request/response shapes match the agreed contract

## Naming Conventions
- Test methods: `methodName_stateUnderTest_expectedBehavior`
- Use `@DisplayName` for human-readable descriptions

## Steps
1. Define the request and response DTOs
2. Add repository method(s) if new queries are needed
3. Implement service logic
4. Implement controller with proper status codes (201 create, 204 delete, 422 business errors)
5. Add Flyway migration if schema changes are required
6. Write unit tests (JUnit 5 + AssertJ + Mockito)
7. Write integration tests (Testcontainers)
8. Write contract tests
9. Review phase — Reviewer must return `APPROVED` before task is considered done
