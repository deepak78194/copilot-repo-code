# Copilot Experimentation Lab

This repository is a structured learning and experimentation lab for GitHub Copilot — focused on custom agents, agent orchestration, prompt engineering, context engineering, and deterministic AI-assisted workflows.

---

## Purpose

Use this repository to:

- Design and evaluate **custom agents** with well-defined roles
- Experiment with **agent orchestration** (planner → implementer → reviewer)
- Engineer high-quality **prompts** and **context** for Copilot
- Automate **TDD workflows** with Java (Micronaut, Spring Boot) and JUnit 5
- Design and test **REST APIs** with deterministic, reviewable outcomes
- Practice **PostgreSQL migration scenarios** in a safe sandbox

---

## Repository Structure

```
.copilot/
  instructions.md          # Global Copilot behavior instructions
  agents/                  # Agent role definitions
    planner.agent.md
    implementer.agent.md
    reviewer.agent.md
    orchestrator.agent.md
  skills/                  # Reusable skill definitions
    java.skill.md
    testing.skill.md
    rest-api.skill.md
  prompt-library/          # Reusable prompt templates
    tdd-cycle.prompt.md
    rest-endpoint.prompt.md
    db-migration.prompt.md

playground/                # Example projects for hands-on experimentation
experiments/               # Agent evaluation and determinism testing scenarios
```

---

## How Agents Are Structured

Each agent is defined in `.copilot/agents/` as a Markdown file that specifies:

- **Role** — what the agent is responsible for
- **Inputs** — what context it needs
- **Outputs** — what it produces
- **Constraints** — what it must not do
- **Prompt template** — the structured instruction sent to Copilot

| Agent | Responsibility |
|---|---|
| `planner` | Breaks requirements into tasks and acceptance criteria |
| `implementer` | Writes production code from a plan |
| `reviewer` | Reviews code for correctness, style, and security |
| `orchestrator` | Sequences agents and manages handoffs |

---

## How Skills Are Reused

Skills in `.copilot/skills/` are modular knowledge files that any agent can reference. They encode domain-specific conventions, patterns, and constraints.

| Skill | Purpose |
|---|---|
| `java` | Java coding conventions, Micronaut/Spring Boot patterns |
| `testing` | JUnit 5 patterns, TDD cycle, test naming |
| `rest-api` | REST design rules, status codes, request/response shapes |

An agent references a skill by including it in its prompt context.

---

## How Orchestration Works

The `orchestrator` agent drives a deterministic three-phase workflow:

```
1. Plan   → planner.agent receives a requirement and produces a task list
2. Build  → implementer.agent receives the task list and writes code
3. Review → reviewer.agent receives the code and produces a review report
```

Each phase produces a documented output that feeds the next phase, making the workflow auditable and reproducible.

---

## Prompt Library

The `.copilot/prompt-library/` folder contains ready-to-use prompt templates for common scenarios:

- `tdd-cycle.prompt.md` — Red → Green → Refactor cycle for a Java feature
- `rest-endpoint.prompt.md` — Design, implement, and test a REST endpoint
- `db-migration.prompt.md` — Write and validate a PostgreSQL migration

---

## Best Practices for Copilot Experimentation

1. **Be explicit about roles.** Each agent should have a single, clear responsibility.
2. **Separate planning from implementation.** Never let the implementer define requirements.
3. **Use constraints.** Tell agents what they must NOT do — this reduces hallucination.
4. **Prefer deterministic workflows.** Plan → Implement → Review produces auditable results.
5. **Write tests first.** TDD gives Copilot a concrete target and validates its output.
6. **Version your prompts.** Treat prompt templates like code — track changes and evaluate regressions.
7. **Keep context tight.** Pass only the context each agent needs. Noise degrades output quality.
8. **Evaluate outputs.** Use the `experiments/` folder to measure consistency across runs.

---

## Getting Started

1. Read `.copilot/instructions.md` to understand global Copilot behavior for this repo.
2. Pick a scenario from `playground/` or design your own.
3. Run the orchestrator workflow: plan → implement → review.
4. Log results in `experiments/` for later analysis.
