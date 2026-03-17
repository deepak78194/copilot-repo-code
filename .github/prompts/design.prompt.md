---
mode: agent
agent: lead
description: "Design API contract, schema, or architecture only. Invokes: designer only."
---

# /design — API or Schema Design (1 agent: designer)

> **Routing: lead → designer**
> Use this when you need a concrete API contract, data model, or component design before any code is written. No planning or coding — pure design artifacts.

## What happens
1. `@lead` routes directly to `designer` (scope is clear enough to skip planner)
2. `designer` reads existing patterns in the codebase to match conventions
3. Produces: API endpoint definitions, request/response shapes, data model tables, component diagram
4. Includes design decision rationale so the coder doesn't re-decide

## Prompt
Design the following:

**What to design:** $design_target

Produce:
- API contract (method, path, request body, response, error codes) if applicable
- Data model (entities, fields, types, constraints, relationships) if applicable
- Component boundaries and data flow if applicable
- A design decisions table with rationale

Do NOT write any implementation code. Design artifacts only.
