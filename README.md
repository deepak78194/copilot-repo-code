# Copilot Experimentation Lab

A structured lab for designing, evaluating, and improving GitHub Copilot agent definitions,
prompt templates, and orchestration patterns for production Java and TypeScript backends.

> **This is not a shipped application.** All generated code is for evaluation purposes.

---

## Quick Start

1. Read `.github/copilot-instructions.md` — global rules applied to every Copilot interaction.
2. Explore `.copilot/agents/` to understand the six available agent roles.
3. Pick a prompt from `.copilot/prompt-library/` that matches your task.
4. Run the Orchestrator workflow: **Plan → Implement → Review**.
5. Save outputs in `experiments/` and record what you learned.

---

## Repository Structure

```
.github/
  copilot-instructions.md  # Primary Copilot instructions (auto-applied by GitHub)

.copilot/
  instructions.md          # Extended global instructions and conventions
  agents/                  # Agent role definitions
    orchestrator.agent.md  # Sequences phases; manages handoffs
    planner.agent.md       # Decomposes requirements into tasks
    implementer.agent.md   # Writes production code from a plan
    reviewer.agent.md      # Reviews code for correctness and security
    debug.agent.md         # Root-cause analysis and targeted bug fixes  ← NEW
    security.agent.md      # OWASP-aligned security audit                ← NEW
  skills/                  # Modular, reusable domain knowledge
    java.skill.md          # Java 21, Spring Boot 3, Micronaut 4
    rest-api.skill.md      # REST design rules and status codes
    testing.skill.md       # JUnit 5, AssertJ, Mockito, Testcontainers
    typescript.skill.md    # TypeScript 5, Hono, Zod, Drizzle, Vitest    ← NEW
    security.skill.md      # OWASP Top 10, secure-coding patterns         ← NEW
    observability.skill.md # Structured logging, OTel tracing, Micrometer ← NEW
  prompt-library/          # Ready-to-use prompt templates
    tdd-cycle.prompt.md
    rest-endpoint.prompt.md
    db-migration.prompt.md
    bug-fix.prompt.md       # Debug → Fix → Review workflow               ← NEW
    refactor.prompt.md      # Safe, test-protected refactoring workflow   ← NEW
    security-review.prompt.md # OWASP-aligned security audit              ← NEW

playground/
  user-service/            # End-to-end Spring Boot example (CRUD + tests) ← NEW

experiments/
  agent-consistency/       # Measured consistency experiment with analysis  ← NEW
```

---

## Agents

| Agent | File | Responsibility |
|-------|------|----------------|
| Orchestrator | `orchestrator.agent.md` | Sequences phases; enforces workflow rules |
| Planner | `planner.agent.md` | Decomposes requirements into tasks with acceptance criteria |
| Implementer | `implementer.agent.md` | Writes production code from a plan |
| Reviewer | `reviewer.agent.md` | Reviews for correctness, style, and security |
| Debugger | `debug.agent.md` | Root-cause analysis → minimal failing test → targeted fix |
| Security | `security.agent.md` | OWASP Top 10 audit with exploit scenarios and remediation |

Each agent definition includes:
- **Role** and **Inputs/Outputs**
- **Constraints** (what it must NOT do)
- **Prompt template** (copy-paste ready)
- **Concrete example**

---

## Skills

Skills are modular knowledge files that agents reference. They encode conventions, patterns, and
constraints for a specific domain.

| Skill | Language | Key Topics |
|-------|----------|-----------|
| `java` | Java 21 | Records, sealed types, Spring Boot 3, Micronaut 4, JPA |
| `rest-api` | — | HTTP verbs, status codes, URL design, pagination, versioning |
| `testing` | Java | JUnit 5, AssertJ, Mockito, Testcontainers, TDD cycle |
| `typescript` | TypeScript 5 | Hono, Zod, Drizzle ORM, Vitest, strict mode |
| `security` | Java / TS | OWASP Top 10, injection, auth, crypto, dependency scanning |
| `observability` | Java | Structured JSON logs, OTel tracing, Micrometer metrics, health endpoints |

---

## How Orchestration Works

```
Requirement
    │
    ▼
┌─────────┐   task list    ┌─────────────┐   code     ┌──────────┐
│ Planner │ ─────────────► │ Implementer │ ─────────► │ Reviewer │
└─────────┘                └─────────────┘            └──────────┘
                                  ▲                        │
                                  │    CHANGES REQUIRED    │
                                  └────────────────────────┘
                                           │ APPROVED
                                           ▼
                                        COMPLETE
```

A task is only `COMPLETE` when the Reviewer returns `APPROVED`. If the Reviewer returns
`CHANGES REQUIRED` more than twice for the same task, the Orchestrator escalates to the user.

---

## Prompt Library

| Template | Workflow | When to Use |
|----------|---------|-------------|
| `rest-endpoint.prompt.md` | Plan → Implement → Review | Add a new REST endpoint end-to-end |
| `tdd-cycle.prompt.md` | Red → Green → Refactor | Implement a method using TDD |
| `db-migration.prompt.md` | Plan → Implement → Review | Add or modify database tables |
| `bug-fix.prompt.md` | Debug → Fix → Review | Investigate and fix a defect |
| `refactor.prompt.md` | Plan → Implement → Review | Safely restructure existing code |
| `security-review.prompt.md` | Audit → Remediate → Re-audit | OWASP security scan before merge |

---

## Playground: `user-service`

A complete, production-quality Spring Boot 3 + PostgreSQL CRUD service built using this lab's
workflow. Use it as a reference implementation.

**Endpoints:** `GET/POST /api/v1/users`, `GET/PATCH/DELETE /api/v1/users/{id}`  
**Stack:** Java 21 · Spring Boot 3 · JPA + Flyway · PostgreSQL · JUnit 5 + Testcontainers  
**Patterns:** Constructor injection · DTOs · Structured errors · `@Transactional` service layer

See `playground/user-service/README.md` for the full walkthrough.

---

## Experiments: `agent-consistency`

A controlled experiment measuring how consistently the agent pipeline produces code that meets the
same acceptance criteria across multiple independent runs.

**Finding:** The Reviewer agent is essential — both runs had at least one issue the Implementer
introduced; both were caught and corrected within the session.

See `experiments/agent-consistency/analysis.md` for the full results and recommendations.

---

## Best Practices

1. **Be explicit about roles.** Each agent has a single responsibility.
2. **Encode conventions in skill files, not in prompts.** Skills are reusable; prompts are throwaway.
3. **Use constraints aggressively.** Telling agents what NOT to do reduces hallucination.
4. **Separate planning from implementation.** Never let the Implementer define requirements.
5. **Write tests before implementation.** TDD gives Copilot a concrete target.
6. **Version your prompts.** Track prompt changes in git and measure regressions.
7. **Keep context tight.** Pass only the context each agent needs — noise degrades output.
8. **Run the Security agent before every merge.** OWASP findings are cheaper to fix early.
