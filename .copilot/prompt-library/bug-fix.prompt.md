# Bug-Fix Prompt Template

Use this prompt to investigate a defect and apply a targeted fix using the Debug agent, followed
by a Reviewer sign-off.

## When to Use

- A test is failing and you need to find the root cause.
- A bug has been reported in a specific endpoint or service method.
- A regression was introduced and needs to be isolated and fixed.

---

## Prompt

```
You are the Orchestrator agent. Run the Debug → Fix → Review workflow for the following bug.

## Bug Report
{describe_observed_vs_expected_behaviour}

## Reproduction Steps
{exact_steps_to_reproduce}

## Relevant Files
{comma_separated_file_paths}

## Error Output
{stack_trace_or_test_failure_message}

## Phase 1 — Debug
Invoke the Debug agent:
- Perform root-cause analysis.
- Write a minimal failing JUnit 5 test that captures the exact failure.

## Phase 2 — Fix
Invoke the Implementer agent:
- Apply the targeted fix identified by the Debug agent.
- Re-run the reproduction test — it must now pass.
- Run all existing tests — none must regress.

## Phase 3 — Review
Invoke the Reviewer agent on the fix diff:
- Verify the fix is correct and minimal.
- Confirm no regressions were introduced.
- Check the fix does not introduce new security issues.

## Constraints
- Fix only what is broken. Do not refactor unrelated code.
- Do not add features while fixing a bug.
- The reproduction test must be committed alongside the fix.
- See `.copilot/agents/debug.agent.md` and `.copilot/agents/reviewer.agent.md`.

## Output Format
### Phase 1 — Root Cause
(Debug agent output: root-cause analysis + failing test)

### Phase 2 — Fix
(Implementer output: code change + confirmation tests pass)

### Phase 3 — Review
(Reviewer output: APPROVED or CHANGES REQUIRED)

### Final Status
FIXED / BLOCKED — (reason if blocked)
```

---

## Example

**Bug:** `GET /api/v1/users/{id}` returns `500` instead of `404` when the user does not exist.

**Reproduction steps:**
1. Start the service with an empty database.
2. `curl -X GET http://localhost:8080/api/v1/users/9999`
3. Response: `500 Internal Server Error` with body `{"message": "No value present"}`

**Expected:** `404 Not Found` with body `{"code": "USER_NOT_FOUND", "message": "No user with id 9999 exists."}`

**Root cause:** `UserService.findById()` calls `Optional.get()` without checking `isPresent()`, so
a `NoSuchElementException` bubbles up unhandled. The global exception handler does not map
`NoSuchElementException` to `404` — it falls through to the `500` catch-all.

**Fix:** Replace `Optional.get()` with `orElseThrow(() -> new UserNotFoundException(id))`.
Verify that the global handler maps `UserNotFoundException` → `404`.
