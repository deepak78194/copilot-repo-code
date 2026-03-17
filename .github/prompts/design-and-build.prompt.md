---
mode: agent
agent: lead
description: "Design architecture then implement — no tests. Invokes: planner → designer → coder (3 agents)."
---

# /design-and-build — Design Then Implement (3 agents: planner → designer → coder)

> **Routing: lead → planner → designer → coder**
> Use this when you need architecture/API design decisions made before coding starts. Good for new services, new APIs, or anything with non-obvious structure.

## What happens
1. `@lead` invokes `planner` to scope the feature and list components
2. `planner` output is passed to `designer` to produce API contracts, data models, component boundaries
3. `designer` artifacts (contracts + constraints) are passed to `coder` for implementation
4. `coder` implements and verifies the build

## Prompt
Design and implement the following:

**What to build:** $feature
**Service / module:** $service

Steps:
1. Planner: define scope, component list, constraints
2. Designer: produce API contract, data model, component diagram, design decisions table
3. Coder: implement based on the design artifacts — do NOT deviate from the API contract

Stop after implementation. Do NOT write tests.
