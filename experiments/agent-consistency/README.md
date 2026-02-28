# Experiment: Agent Consistency — POST /users Endpoint

## Objective

Measure how consistently the Orchestrator + Planner + Implementer + Reviewer agent pipeline produces
code that satisfies the same acceptance criteria across multiple independent runs.

## Hypothesis

Given identical prompts and skill files, the agents should produce structurally equivalent
implementations (same layers, same error codes, same test coverage) even though the exact code may
differ in minor stylistic ways.

## Setup

### Prompt Used

See `prompt.md` for the exact prompt sent to the Orchestrator in each run.

### Acceptance Criteria (fixed across all runs)

1. `POST /api/v1/users` returns `201 Created` with a `Location` header on success.
2. `POST /api/v1/users` returns `400 Bad Request` when the email is blank or malformed.
3. `POST /api/v1/users` returns `409 Conflict` when the email is already taken.
4. The response body contains `id`, `email`, `name`, and `createdAt` fields.
5. No JPA entity is returned directly from the controller.
6. At least one unit test and one integration test cover the endpoint.

### Evaluation Criteria

| Criterion | Description | Weight |
|-----------|-------------|--------|
| **Acceptance coverage** | All 6 ACs satisfied | High |
| **Layer separation** | Controller has no business logic | High |
| **Error handling** | Correct status codes + structured error body | High |
| **Test coverage** | Unit + integration tests present | High |
| **Constraint adherence** | No field injection, no raw types, no `println` | Medium |
| **Convention compliance** | Naming, Javadoc, package structure | Low |

## Methodology

1. Send the identical prompt (see `prompt.md`) to Copilot three times in separate sessions.
2. Save the complete output of each session to `run-1/`, `run-2/` respectively.
3. Evaluate each output against the acceptance criteria using the scoring rubric below.
4. Document differences and regressions in `analysis.md`.

## Scoring Rubric

For each acceptance criterion, assign:
- **PASS** — criterion is fully met
- **PARTIAL** — criterion is partially met (note what is missing)
- **FAIL** — criterion is not met

For each constraint, assign:
- **COMPLIANT** — constraint is respected
- **VIOLATION** — constraint is violated (cite the line)

## Runs

| Run | Date | Model | Notes |
|-----|------|-------|-------|
| run-1 | 2025-01-15 | GPT-4o | Baseline |
| run-2 | 2025-01-15 | GPT-4o | Same session, re-run |

## File Structure

```
experiments/agent-consistency/
  README.md        ← this file
  prompt.md        ← exact prompt used in all runs
  run-1/
    output.md      ← raw Orchestrator output
    evaluation.md  ← scored against acceptance criteria
  run-2/
    output.md
    evaluation.md
  analysis.md      ← comparison, findings, recommendations
```
