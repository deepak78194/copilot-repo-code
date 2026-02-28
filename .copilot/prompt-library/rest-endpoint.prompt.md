# REST Endpoint Prompt Template

Use this prompt to design, implement, and test a REST endpoint end-to-end using the full Plan → Implement → Review workflow.

## When to Use

- When adding a new REST endpoint to a Java backend.
- When you want a complete, tested, reviewed endpoint in a single workflow run.

---

## Prompt

```
You are the Orchestrator agent. Run the Plan → Implement → Review workflow for the following REST endpoint.

## Endpoint Specification
- HTTP Method: {method}
- Path: {path}
- Description: {description}
- Request Body: {request_body_description_or_none}
- Response Body: {response_body_description}
- Success Status: {success_status_code}
- Error Cases: {error_cases}

## Framework
{micronaut_or_spring_boot}

## Skills
- See `.copilot/skills/java.skill.md`
- See `.copilot/skills/rest-api.skill.md`
- See `.copilot/skills/testing.skill.md`

## Phase 1 — Plan
Invoke the Planner agent to produce a task list for this endpoint.
Include tasks for: entity/model, repository, service, controller, unit tests, integration tests.

## Phase 2 — Implement
For each task from Phase 1, invoke the Implementer agent.
Produce complete, compilable Java files.

## Phase 3 — Review
Invoke the Reviewer agent on all produced code.
Check: acceptance criteria, REST conventions, test coverage, error handling, security.

## Constraints
- Follow `.copilot/instructions.md` and the referenced skill files.
- Never return JPA entities from the controller — use DTOs.
- Return correct HTTP status codes as specified.
- Include at least one unit test and one integration test.

## Output Format
(Follow the Orchestrator workflow log format from `orchestrator.agent.md`)
```

---

## Example

**Method:** POST  
**Path:** `/api/v1/users`  
**Description:** Create a new user account  
**Request Body:** `{ "email": "string (required)", "name": "string (required)" }`  
**Response Body:** `{ "id": "long", "email": "string", "name": "string" }`  
**Success Status:** 201  
**Error Cases:** 400 (validation), 409 (duplicate email)
