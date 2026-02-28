# User Service — Playground Example

This is a complete, end-to-end example of using the Copilot experimentation lab workflow to build a
production-quality Java service from scratch. It demonstrates the **Orchestrator → Planner →
Implementer → Reviewer** cycle applied to a real CRUD API.

---

## Objective

Build a `user-service` — a Spring Boot 3 REST API that manages user accounts backed by PostgreSQL,
with full CRUD operations, input validation, structured error handling, and test coverage.

---

## What Was Built

| Layer | File | Description |
|-------|------|-------------|
| Model | `User.java` | JPA entity |
| Model | `CreateUserRequest.java` | POST request DTO |
| Model | `UpdateUserRequest.java` | PATCH request DTO |
| Model | `UserResponse.java` | Response DTO |
| Repository | `UserRepository.java` | Spring Data JPA interface |
| Service | `UserService.java` | Business logic |
| Controller | `UserController.java` | REST endpoints |
| Exception | `UserNotFoundException.java` | 404 domain error |
| Exception | `DuplicateEmailException.java` | 409 domain error |
| Exception | `GlobalExceptionHandler.java` | Maps exceptions to HTTP |
| Migration | `V1__create_users_table.sql` | Flyway schema |
| Test | `UserServiceTest.java` | Unit tests (Mockito) |
| Test | `UserControllerTest.java` | Integration tests (Testcontainers) |

---

## API Endpoints

| Method | Path | Status | Description |
|--------|------|--------|-------------|
| `GET` | `/api/v1/users` | 200 | List all users (paginated) |
| `GET` | `/api/v1/users/{id}` | 200 / 404 | Get a user by ID |
| `POST` | `/api/v1/users` | 201 / 400 / 409 | Create a new user |
| `PATCH` | `/api/v1/users/{id}` | 200 / 400 / 404 | Update a user's name |
| `DELETE` | `/api/v1/users/{id}` | 204 / 404 | Delete a user |

---

## How This Was Built

### Step 1 — Plan

Prompt sent to Orchestrator (see `../.copilot/prompt-library/rest-endpoint.prompt.md`):

```
HTTP Method: POST
Path: /api/v1/users
Description: Create a new user account
Request Body: { "email": "string (required, valid email)", "name": "string (required, max 100 chars)" }
Response Body: { "id": "long", "email": "string", "name": "string", "createdAt": "ISO-8601" }
Success Status: 201
Error Cases: 400 (validation), 409 (duplicate email)
Framework: Spring Boot 3.x
```

### Step 2 — Implement

The Implementer agent produced all source files following `.copilot/skills/java.skill.md` and
`.copilot/skills/rest-api.skill.md` conventions:
- Constructor injection throughout
- No JPA entity returned from controller
- Structured error responses (`code` + `message`)
- Flyway migration for schema

### Step 3 — Review

The Reviewer agent checked:
- ✅ Acceptance criteria met
- ✅ Input validation on DTO
- ✅ 409 on duplicate email (not 500)
- ✅ Unit test for service, integration test for controller
- ✅ No stack traces in API responses

---

## Running Locally

```bash
# Prerequisites: Docker Desktop running (for Testcontainers)

# Run tests
./gradlew test

# Start the service (requires PostgreSQL on localhost:5432)
./gradlew bootRun

# Or use Docker Compose
docker compose up
```

---

## What to Learn From This Example

1. **Agent prompts produce consistent results** when skill files encode explicit conventions.
2. **Test-first constraints** (the Reviewer blocks approval without tests) prevent shipping untested code.
3. **Structured error handling** is only reliable when encoded in the skill + checked by the Reviewer.
4. **Flyway migrations** live alongside the code and are tested via Testcontainers.
