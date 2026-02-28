# TDD Cycle Prompt Template

Use this prompt to drive a complete Red → Green → Refactor cycle for a Java feature using the Implementer agent.

## When to Use

- When you want to implement a specific method or feature using TDD.
- When you want Copilot to write the test first, then the implementation.

---

## Prompt

```
You are the Implementer agent following a strict TDD cycle.

## Feature
{feature_description}

## Skills
- See `.copilot/skills/java.skill.md`
- See `.copilot/skills/testing.skill.md`

## Phase 1 — Red (Write the failing test)
Write a JUnit 5 test that:
- Describes the expected behavior of `{class_name}.{method_name}`.
- Follows the naming convention: `methodName_stateUnderTest_expectedBehavior`.
- Uses AssertJ for assertions.
- Should FAIL because the implementation does not exist yet.

## Phase 2 — Green (Write the minimal implementation)
Write the minimal Java code to make the test pass.
- Do not add any functionality not required by the test.
- Do not refactor yet.

## Phase 3 — Refactor
Improve the code without changing behavior:
- Extract methods if any block exceeds 10 lines.
- Rename variables/methods for clarity.
- Add Javadoc to the public method.
- Ensure the test still passes after refactoring.

## Constraints
- Do not skip any phase.
- Do not write implementation before the test exists.
- Do not add untested code.

## Output Format
### Phase 1 — Failing Test
(Test class with failing test)

### Phase 2 — Minimal Implementation
(Class with minimal implementation)

### Phase 3 — Refactored Code
(Cleaned-up implementation + test, side by side)
```

---

## Example

**Feature:** Parse a comma-separated string into a sorted, deduplicated list of integers.

**Class:** `NumberParser`  
**Method:** `parseSorted(String input)`

Fill in the template and send to the Implementer agent.
