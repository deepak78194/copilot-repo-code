---
mode: agent
agent: lead
description: "Plan, implement, and test. Invokes: planner → coder → tester (3 agents)."
---

# /implement-and-test — Plan + Implement + Test (3 agents: planner → coder → tester)

> **Routing: lead → planner → coder → tester**
> The standard feature development workflow. Decomposes, builds, and verifies with tests. Stops before code review.

## What happens
1. `@lead` invokes `planner` to break the feature into ordered tasks
2. `coder` implements every task, verifies the build
3. `tester` receives the list of modified files and writes unit + integration tests, then runs them and reports pass/fail

## Prompt
Implement and test the following feature:

**Feature:** $feature
**Service / module:** $service

Steps:
1. Planner: ordered task list with file targets and acceptance criteria
2. Coder: implement all tasks, verify build passes, report files changed
3. Tester: write tests covering happy path, boundary conditions, error cases — run them and report real results

Stop after tests. Do NOT run a code review.
