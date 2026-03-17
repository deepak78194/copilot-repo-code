---
mode: agent
agent: lead
description: "Plan then implement — no tests, no review. Invokes: planner → coder (2 agents)."
---

# /implement — Plan + Implement (2 agents: planner → coder)

> **Routing: lead → planner → coder**
> Use this when you want to go from a feature description straight to working code, with a planning step to decompose first. Stops before testing or review.

## What happens
1. `@lead` invokes `planner` to decompose the feature into an ordered task list
2. `planner` output (task table + file targets) is passed to `coder`
3. `coder` implements every task from the plan, verifies the build, reports all changed files

## Prompt
Implement the following feature:

**Feature:** $feature
**Service / module:** $service

Steps:
1. Planner: decompose into ordered tasks with file targets and acceptance criteria
2. Coder: implement all tasks from the plan — match existing code style, verify build passes

Stop after implementation. Do NOT write tests or run a code review.
