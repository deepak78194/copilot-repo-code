# 00 — Architectural Overview: Agentic Workflow for Enterprise API Development

> **Purpose:** Define the layered building blocks of an enterprise agentic workflow and show how they compose into a scalable, reusable system for GitHub Copilot–driven API development.

---

## 1. The Five Building Blocks

An agentic workflow is not a single monolithic system. It is an assembly of five
distinct building blocks, each owning a specific slice of the problem:

| # | Building Block | One-Line Definition | Owns |
|---|---------------|---------------------|------|
| 1 | **Knowledge Base** | Raw reference material the system can consult | Facts, schemas, API docs, codebase snapshots |
| 2 | **Skill** | A curated instruction set for a domain or framework | Conventions, patterns, anti-patterns, examples |
| 3 | **Agent** | An autonomous reasoning unit with identity and constraints | *How to think* about a category of work |
| 4 | **Orchestration** | A coordination layer that routes, sequences, and escalates | *When* and *who* does the work |
| 5 | **Prompt** | A parameterized task trigger that initiates agent action | *What* to do right now, with what context |

These are listed bottom-up: lower layers are more stable and change less frequently;
upper layers are more dynamic and change per task.

---

## 2. The Layered Architecture Model

```
┌─────────────────────────────────────────────────────────────┐
│                         PROMPT                              │
│   User message  +  Prompt template  +  copilot-instructions │
│   "Generate integration tests for POST /api/v1/orders"      │
├─────────────────────────────────────────────────────────────┤
│                      ORCHESTRATION                          │
│   Route to agent  ·  Select skills  ·  Manage phases        │
│   Plan → Implement → Review  ·  Escalation policies         │
├─────────────────────────────────────────────────────────────┤
│                         AGENT                               │
│   Identity (role, reasoning style, constraints)             │
│   "I am a Test Agent. I reason about coverage, isolation,   │
│    assertion quality, and test naming."                      │
├─────────────────────────────────────────────────────────────┤
│                         SKILL                               │
│   Domain/framework conventions loaded at runtime            │
│   junit5.skill.md  ·  cypress.skill.md  ·  cucumber.skill   │
├─────────────────────────────────────────────────────────────┤
│                     KNOWLEDGE BASE                          │
│   Reference material: API specs, framework docs, codebase   │
│   patterns, schema definitions, migration history           │
└─────────────────────────────────────────────────────────────┘
```

### Information flows upward, control flows downward

- **Upward (information):** Knowledge bases feed facts into skills. Skills feed
  conventions into agents. Agents feed outputs into orchestration. Orchestration
  feeds results back to the prompt layer (and ultimately the user).

- **Downward (control):** A prompt triggers orchestration. Orchestration selects
  an agent and attaches skills. The agent consults skills and knowledge bases to
  produce output.

This separation means **no layer needs to understand the internals of layers below it**.
An Orchestrator does not know JUnit 5 conventions — it only knows that a Test Agent
exists and can be given a `junit5.skill.md`. The Test Agent does not know project
lifecycle — it only knows how to reason about test quality using whatever skills it
receives.

---

## 3. How the Layers Map to the File System

In a GitHub Copilot enterprise setup, the building blocks map to a conventional
directory structure:

```
.copilot/
├── agents/                          # Agent identity definitions
│   ├── orchestrator.agent.md        # Orchestration layer (composite agent)
│   ├── planner.agent.md             # Core agent: decompose requirements
│   ├── implementer.agent.md         # Core agent: write production code
│   ├── reviewer.agent.md            # Core agent: review for correctness
│   ├── test.agent.md                # Core agent: generic test reasoning
│   ├── debugger.agent.md            # Core agent: root-cause analysis
│   └── security.agent.md            # Domain agent: OWASP-aligned audit
│
├── skills/                          # Composable knowledge modules
│   ├── java.skill.md                # Language: Java 21 conventions
│   ├── typescript.skill.md          # Language: TypeScript/Node conventions
│   ├── spring-boot.skill.md         # Framework: Spring Boot 3.x patterns
│   ├── micronaut.skill.md           # Framework: Micronaut 4.x patterns
│   ├── junit5.skill.md              # Testing: JUnit 5 + AssertJ + Mockito
│   ├── vitest.skill.md              # Testing: Vitest + supertest
│   ├── cypress.skill.md             # Testing: Cypress E2E patterns
│   ├── cucumber.skill.md            # Testing: BDD/Cucumber/Gherkin
│   ├── rest-api.skill.md            # Domain: REST conventions
│   ├── security.skill.md            # Domain: security constraints
│   └── observability.skill.md       # Domain: logging, tracing, health
│
├── knowledge/                       # Reference material (not instructions)
│   ├── api-specs/                   # OpenAPI/Swagger definitions
│   ├── schema/                      # Database schemas, ERDs
│   └── codebase-patterns/           # Extracted patterns from existing code
│
├── prompt-library/                  # Reusable prompt templates
│   ├── tdd-cycle.prompt.md
│   ├── rest-endpoint.prompt.md
│   ├── db-migration.prompt.md
│   └── security-review.prompt.md
│
└── instructions.md                  # Cross-cutting instructions (auto-loaded)

.github/
└── copilot-instructions.md          # Always-on baseline (every interaction)
```

### Key Structural Principle

Each directory corresponds to exactly one building block. Files within a directory
are **composable units** — an agent can be given zero, one, or many skills; a prompt
template can reference zero, one, or many agents. The file system enforces separation
of concerns at the organizational level.

---

## 4. The Composition Model

The power of this architecture lies in **runtime composition**. The same core agent
combines with different skills to produce radically different behavior:

```
Test Agent  +  junit5.skill  +  java.skill  +  spring-boot.skill
  → Generates JUnit 5 integration tests for a Spring Boot controller

Test Agent  +  cypress.skill  +  typescript.skill
  → Generates Cypress E2E tests for a React frontend

Test Agent  +  cucumber.skill  +  java.skill
  → Generates Cucumber feature files with Java step definitions
```

The Test Agent's **identity** (reasoning about coverage, isolation, naming,
assertion quality) remains constant. The **skills** provide framework-specific
knowledge. The **orchestration** decided which skills to attach based on project
context. The **prompt** specified what to test.

No layer was modified to achieve this variation. This is the core design goal.

---

## 5. Design Principles

### 5.1 — Separation of Reasoning from Knowledge

An agent's definition file should contain *how to think*, not *what to know about
a specific framework*. If you find yourself writing "use `@ExtendWith(MockitoExtension.class)`"
inside an agent definition, that knowledge belongs in a skill file.

**Test:** Can you swap `junit5.skill.md` for `vitest.skill.md` without editing the
agent file? If yes, reasoning and knowledge are properly separated.

### 5.2 — Composability over Specialization

Prefer one generic Test Agent + N skill files over N specialized test agents
(JUnitTestAgent, CypressTestAgent, CucumberTestAgent). Specialized agents are
justified only when the *reasoning strategy* fundamentally differs — not just the
framework conventions.

**Test:** Would a new testing framework require a new agent, or just a new skill
file? If a new skill suffices, composability is working.

### 5.3 — Orchestration as Pure Coordination

The Orchestrator should not contain domain logic. It selects agents, attaches
skills, manages phase transitions, and handles escalation. If the Orchestrator
needs to understand JUnit 5 to do its job, the boundary is violated.

**Test:** Can a non-engineer understand the Orchestrator's flow diagram? If yes,
domain logic is properly delegated.

### 5.4 — Prompts as Parameters, Not Programs

A prompt template should specify *what to do* and *what constraints apply*, not
teach the agent how to reason. If the same information appears in both a prompt
and a skill file, it should live in the skill file only, with the prompt referencing it.

**Test:** If you remove all skill references from a prompt, does the prompt still
make sense as a task description? If yes, the prompt is properly parameterized.

### 5.5 — Knowledge Bases as Ground Truth

Knowledge bases contain facts that are verifiable, not opinions or conventions.
"The `users` table has columns `id`, `email`, `name`" is knowledge.
"Always validate email at the boundary" is a skill convention.

**Test:** Can the content be verified by reading source code or documentation? If
yes, it's knowledge. If it requires judgment, it's a skill.

---

## 6. Reading Guide

This document is the first of eight. The remaining documents provide deep dives
into each architectural concern:

| Document | Focus |
|----------|-------|
| [01 — Responsibility Boundaries](01-responsibility-boundaries.md) | Where each type of behavior lives; the Substitution Principle |
| [02 — Agent Taxonomy](02-agent-taxonomy.md) | Core vs. Domain vs. Composite agents; when to split |
| [03 — Skill & Knowledge Design](03-skill-and-knowledge-design.md) | Skill contracts, composition, granularity, freshness |
| [04 — Prompt Design](04-prompt-design.md) | Prompt anatomy, layering, template vs. one-shot |
| [05 — Orchestration Patterns](05-orchestration-patterns.md) | Routing, sequencing, escalation, sub-agent delegation |
| [06 — Context Detection & Adaptation](06-context-detection-and-adaptation.md) | Environment sensing, signal taxonomy, confirmation strategies |
| [07 — Freshness & Bounded Autonomy](07-freshness-and-bounded-autonomy.md) | Guardrails, drift detection, governance, innovation budgets |

Each document is self-contained but cross-references related sections in other
documents. The **Test Agent** example threads through documents 02, 03, 05, and 06
as a consistent worked example.

---

## Key Takeaways

1. **Five building blocks** — Knowledge, Skills, Agents, Orchestration, Prompts —
   each own a distinct responsibility. Mixing responsibilities creates brittle,
   non-reusable systems.

2. **Information flows upward, control flows downward.** No layer needs to understand
   the internals of layers below it.

3. **Runtime composition** is the core mechanism for reuse. One generic agent +
   swappable skills > many specialized agents.

4. **The file system enforces separation.** Each directory = one building block.
   Files within are composable units.

5. **Five design tests** (swap skill, new framework, non-engineer readability,
   remove skills from prompt, verify vs. source) validate that boundaries are clean.
