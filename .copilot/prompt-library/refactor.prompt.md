# Refactor Prompt Template

Use this prompt to safely refactor existing code using the Plan → Implement → Review workflow,
with a mandatory test-safety gate before any structural change.

## When to Use

- Extracting a large method into smaller, focused methods.
- Renaming a class, method, or field across the codebase.
- Replacing an `if-else` chain with a strategy pattern or sealed interface.
- Migrating from one library or framework version to another.
- Eliminating code duplication (DRY cleanup).

---

## Refactoring Safety Rules

Before ANY refactoring begins, the following gate must be passed:

1. **Test coverage verified** — All public methods of the target class are covered by at least
   one passing test. If coverage is insufficient, add tests first (as a separate PR).
2. **Behaviour preserved** — The refactoring must not change observable behaviour.
3. **Incremental steps** — Each refactoring step is a separate, reviewable commit.
4. **No mixed concerns** — A refactoring commit must not contain bug fixes or feature additions.

---

## Prompt

```
You are the Orchestrator agent. Run the Plan → Implement → Review workflow for the following
refactoring task.

## Refactoring Goal
{describe_what_needs_to_be_changed_and_why}

## Target Files
{comma_separated_file_paths}

## Current Behaviour (must be preserved)
{describe_what_the_code_currently_does_correctly}

## Motivation
{technical_debt_duplication_readability_performance}

## Phase 1 — Plan
Invoke the Planner agent to produce a task list. Each task must be:
- A single, atomic refactoring step.
- Independently testable.
- Safe to roll back individually.

## Phase 2 — Implement
For each task, invoke the Implementer agent:
- Apply the refactoring step.
- Run all existing tests after each step — zero regressions allowed.
- Do not add new behaviour.

## Phase 3 — Review
Invoke the Reviewer agent on each changed file:
- Verify behaviour is preserved.
- Confirm tests still pass (all green).
- Check that the refactored code is cleaner and follows repository conventions.
- Confirm no accidental feature additions or bug fixes are mixed in.

## Constraints
- Do not fix bugs during the refactoring. Log them and address separately.
- Do not add new public methods unless required by the refactoring pattern.
- Do not change the public API surface without explicit approval.
- See `.copilot/instructions.md` for code-quality conventions.

## Output Format
### Phase 1 — Refactoring Plan
(Planner output: ordered list of atomic steps)

### Phase 2 — Implementation: Step {n}
(Implementer output: code change + confirmation tests pass)

### Phase 3 — Review: Step {n}
(Reviewer output: APPROVED or CHANGES REQUIRED)

### Final Status
COMPLETE / BLOCKED — (reason if blocked)
```

---

## Example

**Goal:** Replace the `if-else` chain in `OrderStatusService.transition()` with a sealed interface
and pattern matching, improving extensibility and readability.

**Target files:** `src/main/java/com/example/order/service/OrderStatusService.java`

**Current behaviour:** Given an order in state `PENDING` and the event `PAYMENT_RECEIVED`, the
method transitions to `CONFIRMED`. All existing state transitions must behave identically after
the refactoring.

**Motivation:** The `if-else` chain has grown to 14 branches and is difficult to extend without
risking regression. A sealed `OrderEvent` interface with pattern-matched dispatch is cleaner and
exhaustiveness-checked by the compiler.

**Refactoring steps (from Planner):**
1. Add a `sealed interface OrderEvent permits PaymentReceived, OrderCancelled, ...` with one
   record per event — no changes to `OrderStatusService` yet.
2. Replace `if-else` in `transition()` with a `switch` expression over `OrderEvent`.
3. Delete the old `if-else` chain once the switch expression passes all tests.
