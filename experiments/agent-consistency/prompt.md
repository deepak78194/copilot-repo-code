# Experiment Prompt — Agent Consistency

This is the **exact prompt** used in every run of the agent-consistency experiment.
Do not modify this file between runs — it is the controlled variable.

---

```
You are the Orchestrator agent. Run the Plan → Implement → Review workflow for the following
REST endpoint.

## Endpoint Specification
- HTTP Method: POST
- Path: /api/v1/users
- Description: Create a new user account
- Request Body: { "email": "string (required, valid email format)", "name": "string (required, max 100 chars)" }
- Response Body: { "id": "long", "email": "string", "name": "string", "createdAt": "ISO-8601 datetime" }
- Success Status: 201 Created (with Location header)
- Error Cases:
    - 400 Bad Request — email is blank or not a valid email format
    - 400 Bad Request — name is blank
    - 409 Conflict — a user with the same email address already exists

## Framework
Spring Boot 3.x with JPA and PostgreSQL

## Skills
- See .copilot/skills/java.skill.md
- See .copilot/skills/rest-api.skill.md
- See .copilot/skills/testing.skill.md

## Phase 1 — Plan
Invoke the Planner agent. Produce tasks for:
  1. JPA entity (User)
  2. Request DTO (CreateUserRequest) with validation annotations
  3. Response DTO (UserResponse)
  4. Domain exceptions (UserNotFoundException, DuplicateEmailException)
  5. Global exception handler
  6. Repository (UserRepository)
  7. Service (UserService)
  8. Controller (UserController)
  9. Flyway migration (V1__create_users_table.sql)
  10. Unit tests for UserService
  11. Integration tests for UserController

## Phase 2 — Implement
For each task from Phase 1, invoke the Implementer agent.
Produce complete, compilable Java files.

## Phase 3 — Review
Invoke the Reviewer agent on all produced code.
Check: acceptance criteria, REST conventions, test coverage, error handling, security.

## Constraints
- Follow .copilot/instructions.md and the referenced skill files.
- Never return JPA entities from the controller — use DTOs.
- Return 409 (not 500) for duplicate email.
- Return 201 with a Location header on success.
- Include at least one unit test and one integration test.
- Use constructor injection; no field injection.
- Use @Transactional on service write methods.
- Log user creation at INFO level (userId only, not email).

## Output Format
Follow the Orchestrator workflow log format from orchestrator.agent.md.
```
