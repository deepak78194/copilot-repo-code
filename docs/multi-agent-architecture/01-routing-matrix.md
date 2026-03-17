# Routing Matrix — Quick Reference

> Use this to understand which agents @lead will activate for a given task.

---

## Dispatch Matrix

| Task Signal | Agent Chain | Agent Count | Notes |
|---|---|---|---|
| `plan` / `break down` / `outline` | `planner` | 1 | Returns ordered task table |
| `review` / `audit` / `security check` | `reviewer` | 1 | OWASP checklist included |
| `write tests` / `add tests` / `test coverage` | `tester` | 1 | Writes + runs tests |
| `debug` / `fix` / `troubleshoot` | `coder` | 1 | Targeted fix, no planning needed |
| `design API` / `design schema` / `data model` | `designer` | 1 | Skips planner if scope is clear |
| `deploy` / `Dockerfile` / `CI/CD` / `infrastructure` | `devops` | 1 | No src changes |
| `design architecture` / `system design` | `planner → designer` | 2 | Planner scopes, Designer produces artifacts |
| `implement` / `build` / `create` / `code` | `planner → coder` | 2 | Plan first, then implement |
| `refactor` | `reviewer → coder` | 2 | Understand what exists before changing |
| `implement and test` | `planner → coder → tester` | 3 | Standard feature chain |
| `build and deploy` | `planner → coder → tester → devops` | 4 | Full service ship chain |
| `full feature` / `end-to-end` / `complete cycle` | `planner → designer → coder → tester → reviewer` | 5 | Full engineering cycle |

---

## Parallel vs Sequential Invocation

### Sequential (default)
Use when agent B needs output from agent A:
```
planner output ──► coder ──► tester ──► reviewer
```

### Parallel (when outputs are independent)
Use when two agents can work simultaneously:
```
planner ──► designer   (both can run independently if planner scope is fixed
         └─► researcher   and researcher doesn't depend on designer)
```

Lead announces parallel invocations explicitly before starting them.

---

## Context Budget Per Handoff

| From | To | What Is Passed | Token Budget |
|------|----|----------------|-------------|
| planner | coder | Task table + file targets + acceptance criteria | ≤300 tokens |
| planner | designer | Feature scope + constraints + component list | ≤250 tokens |
| designer | coder | API contract + schema + constraints for impl | ≤400 tokens |
| coder | tester | Files modified + key functions + edge case hints | ≤200 tokens |
| coder | reviewer | Files modified + change summary | ≤150 tokens |
| tester | reviewer | Test coverage summary + any failures | ≤150 tokens |
| any | devops | Service name + language + port + dependencies | ≤150 tokens |

**Lead never passes raw file contents or full agent transcripts between agents.**

---

## Agent Capability Matrix

| Capability | lead | planner | designer | coder | tester | reviewer | devops |
|---|---|---|---|---|---|---|---|
| Read files | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Edit source files | ❌ | ❌ | ❌ | ✅ | Partial* | ❌ | ❌ |
| Edit test files | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ |
| Edit infra files | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| Run commands | ❌ | ❌ | ❌ | ✅ | ✅ | ❌ | ✅ |
| Web search | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ |
| Spawn sub-agents | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Ask user questions | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |

*Tester can only create/edit files in test directories.

---

## Adding a New Sub-Agent

When you need a new specialized agent (e.g., `documenter`, `migrator`, `data-analyst`):

1. Create `.github/agents/<name>.agent.md` with `user-invocable: false`
2. Choose a model from [available-models.md](../available-models.md) matching the task type
3. Restrict tools to only what the agent's role needs
4. Define `<rules>`, `<workflow>`, and `<output_contract>` sections
5. Add the agent name to the `agents:` list in `lead.agent.md`
6. Add the new task signal → agent mapping to `<dispatch_matrix>` in `lead.agent.md`
7. Update the routing matrix table in this file

**Template for a new sub-agent:**

```yaml
---
name: <name>
description: |
  [What this agent does in 2-3 sentences.]
  [When Lead should invoke it.]
model: <model-from-available-models.md>
user-invocable: false
tools:
  - read
  - search
  # add: edit, execute, web only if the role requires them
agents: []
---

## Identity
[Who I am, what I do, what model I use and why]

<rules>
- [What I NEVER do]
- [What I ALWAYS do]
- [Quality/safety guardrails]
</rules>

<workflow>
## Step 1 — [Phase name]
...
## Step N — Report
[Output format with markdown template]
</workflow>

<output_contract>
[What Lead expects: structure, token budget, specific fields]
</output_contract>
```
