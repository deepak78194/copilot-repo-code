# Run 2 — Evaluation

**Date:** 2025-01-15  
**Model:** GPT-4o  
**Session:** Fresh session, no prior context (same day as run-1)

## Acceptance Criteria Scorecard

| # | Criterion | Result | Notes |
|---|-----------|--------|-------|
| 1 | POST returns 201 with Location header | PASS | Location header present |
| 2 | POST returns 400 for blank/invalid email | PASS | Bean Validation on DTO |
| 3 | POST returns 409 for duplicate email | PARTIAL | 409 returned but body missing `code` field |
| 4 | Response contains id, email, name, createdAt | PASS | UserResponse record |
| 5 | No JPA entity returned from controller | PASS | Returns UserResponse DTO |
| 6 | Unit test + integration test present | PASS | Both present |

**AC Score: 5.5/6**

## Constraint Compliance

| Constraint | Result | Notes |
|-----------|--------|-------|
| No field injection | COMPLIANT | Constructor injection used |
| No raw types | COMPLIANT | All generics parameterised |
| No println | COMPLIANT | SLF4J used |
| Structured error responses | VIOLATION | `DuplicateEmailException` handler returned `ResponseEntity<String>` not a structured DTO |
| @Transactional on write methods | COMPLIANT | createUser, deleteUser annotated |

## Observations

- The Reviewer correctly identified the missing `code` field in the 409 response body as a
  `major` issue and flagged it as CHANGES REQUIRED.
- The Implementer applied the fix in the second cycle, adding `GlobalExceptionHandler` with a
  proper `ProblemDetail` response.
- Interesting divergence: this run used `@ControllerAdvice` whereas run-1 used `@RestControllerAdvice`.
  Functionally equivalent but inconsistent. Worth noting in the analysis.
- The unit test in this run included an additional edge case (empty name) not present in run-1.

## Raw Output

See `output.md` for the complete Orchestrator session transcript.
