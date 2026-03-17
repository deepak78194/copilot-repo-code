---
mode: agent
agent: lead
description: "Full engineering cycle — plan, design, implement, test, review. Invokes: planner → designer → coder → tester → reviewer (5 agents)."
---

# /full-feature — Complete Engineering Cycle (5 agents: planner → designer → coder → tester → reviewer)

> **Routing: lead → planner → designer → coder → tester → reviewer**
> The full end-to-end cycle. Use for new features that need architecture decisions, solid implementation, test coverage, and a security + quality gate before merging.

## What happens
1. `planner` decomposes the feature and scopes component boundaries
2. `designer` produces API contracts, data models, and the design decisions table
3. `coder` implements strictly within the design contract and verifies the build
4. `tester` writes + runs unit and integration tests on the implementation
5. `reviewer` audits the final code: OWASP Top 10 + quality checks, returns severity-tiered issue list

## Prompt
Complete engineering cycle for the following feature:

**Feature:** $feature
**Service / module:** $service

Expectations:
1. Planner: scope + ordered task list
2. Designer: API contract, data model, design decisions — coder must match exactly
3. Coder: implement all tasks, no deviations from design contract, build must pass
4. Tester: happy path + boundary + error cases, real test run results
5. Reviewer: full OWASP audit + quality review — flag CRITICAL issues prominently

After reviewer completes: present a final summary table of what was built, tested, and any issues that need follow-up before merging.
