# Analysis — Agent Consistency Experiment

## Summary

Two runs of the same prompt produced structurally equivalent implementations with one notable
divergence in error handling.

## Results Overview

| Metric | Run 1 | Run 2 |
|--------|-------|-------|
| AC Score | 6/6 | 5.5/6 |
| Constraint violations | 0 | 1 |
| Reviewer cycles needed | 2 | 2 |
| Final verdict | APPROVED | APPROVED (after fix) |

## Consistent Behaviours (both runs)

✅ Layer separation: controller delegated to service; service delegated to repository  
✅ Constructor injection used everywhere  
✅ `UserResponse` record as DTO — entity not exposed from controller  
✅ `@Transactional` on write methods  
✅ Flyway migration followed `V{n}__{description}.sql` naming  
✅ Both unit and integration tests present  
✅ Structured logging at INFO level (userId only)

## Divergences

| Aspect | Run 1 | Run 2 | Impact |
|--------|-------|-------|--------|
| Exception handler annotation | `@RestControllerAdvice` | `@ControllerAdvice` | Low (functionally equivalent) |
| 409 error body | Structured `ProblemDetail` | Plain string initially | **High** (caught by Reviewer) |
| Extra edge-case test | No | Yes (empty name) | Positive |
| Optional usage | `orElseThrow()` from start | `get()` initially → fixed | Medium (caught by Reviewer) |

## Key Finding

**The Reviewer agent is essential.** Both runs had at least one issue that the Implementer
introduced. In both cases, the Reviewer caught it and the Implementer corrected it within the
same session. Without the Review phase, run-2 would have shipped a `409` response without a
structured `code` field — violating the API contract.

## Recommendations

1. **Add the structured error response requirement to the skill file** — the Implementer
   followed the skill file closely; adding a concrete `@RestControllerAdvice` example to
   `rest-api.skill.md` would eliminate the `@ControllerAdvice` divergence.

2. **Add `orElseThrow` as a constraint in `java.skill.md`** — both runs initially used `Optional.get()`.
   Adding a constraint "use `orElseThrow()` with a domain exception; never call `get()` on an Optional"
   would prevent this pattern upfront.

3. **Two runs are insufficient for statistical confidence** — run at least 5 runs before drawing
   conclusions about consistency. The current data suggests ~90% AC compliance on first attempt.

## Follow-Up Experiments

- [ ] Repeat with a different model (Claude 3.5 Sonnet) and compare results
- [ ] Test with a more ambiguous prompt (omit the error-case specifications) to measure hallucination
- [ ] Add the two recommended skill-file updates and measure whether run-1-style issues disappear
