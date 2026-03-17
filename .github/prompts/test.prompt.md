---
mode: agent
agent: lead
description: "Write and run tests only. Invokes: tester only."
---

# /test — Write and Run Tests (1 agent: tester)

> **Routing: lead → tester**
> Use this when code already exists and you need test coverage added or improved.

## What happens
1. `@lead` routes directly to `tester`
2. `tester` reads the target production files to understand behavior
3. Writes unit + integration tests following existing project test patterns
4. Runs tests and reports real pass/fail results

## Prompt
Write tests for the following code:

**Target:** $target

Cover:
- Happy path for each public method
- Boundary conditions (null, empty, max values)
- Expected error/exception cases
- Integration test if the method touches DB or external services

Follow the project's existing test style and directory conventions.
Do NOT modify any production source files.
