---
mode: agent
agent: lead
description: "Plan only — decompose a feature into an ordered task list. Invokes: planner only."
---

# /plan — Task Decomposition (1 agent: planner)

> **Routing: lead → planner**
> Use this when you have a feature idea or requirement and need it broken down into implementation tasks before touching any code.

## What happens
1. `@lead` reads your request and routes directly to `planner`
2. `planner` searches the codebase to understand existing structure
3. Returns a prioritized task table with file targets and acceptance criteria

## Prompt
Break down this feature into an implementation plan:

**Feature:** $feature

Include:
- Ordered task list with file targets
- Dependencies between tasks
- Acceptance criteria per task
- Explicit out-of-scope items

Do NOT write any code. Plan only.
