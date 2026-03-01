# 01 — Responsibility Boundaries: Where Behavior Lives

> **Purpose:** Define precisely which type of behavior belongs in which architectural layer, establish the Substitution Principle as the correctness test, and catalog common boundary violations.

---

## 1. The Core Question

> *"Should specialization be driven by prompts, by agent identity, by skill files,
> or by orchestration logic?"*

The answer is: **all four, but at different abstraction levels.** The problem is
never *which* layer should own behavior — it's that teams conflate abstraction
levels and let behavior leak across boundaries.

This document defines a **Responsibility Matrix** that makes the boundaries
unambiguous.

---

## 2. The Responsibility Matrix

| Behavior Type | Responsible Layer | Example | Anti-Pattern if Misplaced |
|--------------|-------------------|---------|---------------------------|
| **What to do** (task scope) | Prompt | "Generate tests for `UserController`" | Embedding task scope in agent identity |
| **How to think** (reasoning strategy) | Agent Identity | "Reason about test isolation, coverage gaps, and assertion quality" | Putting reasoning heuristics in skill files |
| **What conventions to follow** (domain knowledge) | Skill | "Use `@ExtendWith(MockitoExtension.class)` for unit tests" | Hardcoding framework conventions in agent definitions |
| **What exists** (facts, schemas, APIs) | Knowledge Base | "The `users` table has columns `id`, `email`, `name`, `created_at`" | Encoding project facts in skill files (they go stale) |
| **When to act and who acts** (lifecycle) | Orchestration | "After Planner completes, route to Implementer with `java.skill`" | Agents deciding their own execution order |
| **What not to do** (guardrails) | Agent Identity + Orchestration | "Never return JPA entities from REST endpoints" | Scattering constraints across prompts (they get lost) |
| **What format to produce** (output shape) | Prompt + Agent Identity | "Return a structured task list as Markdown" | Skills dictating output format |

### Reading the Matrix

Each row has exactly one responsible layer. When you find behavior that seems to
belong in two layers, it's usually because the behavior is actually two concerns
fused together. Split them:

- "Use JUnit 5 with `@DisplayName`" is **one convention** → Skill
- "Every public method should have at least one test" is **a reasoning heuristic** → Agent
- "Generate tests for `OrderService`" is **a task** → Prompt
- "Route to Test Agent after Implementer finishes" is **lifecycle** → Orchestration

---

## 3. The Substitution Principle

The Substitution Principle is the architectural correctness test for boundary
separation:

> **Swapping one component at any layer should change behavior at that layer's
> abstraction level, without requiring changes to other layers.**

### 3.1 — Skill Substitution

**Swap:** Replace `junit5.skill.md` with `vitest.skill.md`

**Expected effect:** Agent produces Vitest tests instead of JUnit 5 tests.

**Must NOT require:** Changes to the Test Agent definition, Orchestrator logic,
or prompt templates.

**If it fails:** The agent definition contains framework-specific knowledge that
should be in the skill file.

### 3.2 — Agent Substitution

**Swap:** Replace the standard Reviewer agent with a Security-focused Reviewer.

**Expected effect:** Reviews emphasize OWASP concerns instead of general
correctness.

**Must NOT require:** Changes to skill files, orchestration phase sequence, or
prompt templates.

**If it fails:** Skill files contain reasoning instructions that should be in the
agent's identity.

### 3.3 — Prompt Substitution

**Swap:** Replace a `tdd-cycle.prompt.md` invocation with a `rest-endpoint.prompt.md`
invocation.

**Expected effect:** The workflow targets a different task type, but uses the same
agents and skills.

**Must NOT require:** Changes to agent definitions or skill files.

**If it fails:** The prompt is teaching the agent domain knowledge instead of
parameterizing a task.

### 3.4 — Orchestration Substitution

**Swap:** Replace sequential Plan→Implement→Review with a parallel fan-out where
Implementer and Test Agent run concurrently.

**Expected effect:** Execution order changes, but each agent's output quality is
identical.

**Must NOT require:** Changes to agent definitions, skills, or prompt templates.

**If it fails:** Agents embed assumptions about execution order (e.g., "I will
receive the Planner's output before running").

### 3.5 — Knowledge Base Substitution

**Swap:** Point the system at a different codebase or API spec.

**Expected effect:** Agents produce output adapted to the new codebase, following
the same conventions.

**Must NOT require:** Changes to skills (conventions are project-independent) or
agent definitions.

**If it fails:** Skills encode project-specific facts instead of general
conventions.

---

## 4. Deep Dive: Each Layer's Contract

### 4.1 — Prompt Layer Contract

**Owns:**
- Task description (what needs to happen)
- Context pointers (which files, endpoints, or features are in scope)
- Acceptance criteria (how to know it's done)
- Output format preferences (Markdown, JSON, code blocks)

**Does NOT own:**
- How the agent should reason about the task
- Which framework conventions to follow
- Which agent handles the task
- Execution order

**Interface pattern:**
```
TASK: {what to do}
CONTEXT: {pointers to relevant files, specs, or knowledge}
CONSTRAINTS: {task-specific requirements}
OUTPUT: {expected format and structure}
REFERENCES: {skill files or knowledge bases to consult}
```

The `REFERENCES` field is the bridge between prompts and skills — the prompt says
*which* skills are relevant, but does not duplicate their content.

### 4.2 — Agent Identity Contract

**Owns:**
- Role definition (who is this agent?)
- Reasoning strategy (how does it approach problems in its domain?)
- Quality heuristics (what does "good" look like, conceptually?)
- Constraints (what must it never do?)
- Output contracts (what structure does its output take?)

**Does NOT own:**
- Framework-specific conventions (belongs in skills)
- Project-specific facts (belongs in knowledge base)
- Task specification (belongs in prompt)
- Execution order or phase transitions (belongs in orchestration)

**Canonical structure of an agent identity file:**
```
ROLE: {one-sentence role definition}
REASONING APPROACH:
  - {heuristic 1: how the agent thinks about its domain}
  - {heuristic 2}
  - {heuristic N}
QUALITY CRITERIA:
  - {what the agent considers "good output"}
CONSTRAINTS:
  - {what the agent must never do}
INPUT CONTRACT: {what it expects to receive}
OUTPUT CONTRACT: {what it produces}
SKILL COMPOSITION: {categories of skills it can consume}
```

### 4.3 — Skill Layer Contract

**Owns:**
- Framework/domain conventions (naming, structure, idioms)
- Patterns to follow (with examples)
- Anti-patterns to avoid (with examples)
- Configuration norms (dependency declarations, build setup)
- Concrete code templates and snippets

**Does NOT own:**
- Reasoning strategies (belongs in agent identity)
- Task descriptions (belongs in prompt)
- Project-specific facts like table schemas (belongs in knowledge base)
- Orchestration logic (belongs in orchestrator)

**Canonical structure of a skill file:**
```
DOMAIN: {what this skill covers}
APPLIES WHEN: {signals that indicate this skill is relevant}
CONVENTIONS:
  - {convention 1 with rationale}
  - {convention 2 with rationale}
PATTERNS:
  - {pattern name}: {code example}
ANTI-PATTERNS:
  - {anti-pattern}: {why it's wrong} → {correct alternative}
DEPENDENCIES: {required libraries, versions, build config}
```

### 4.4 — Knowledge Base Contract

**Owns:**
- Verifiable facts about the current project or external systems
- API specifications (OpenAPI, GraphQL schemas)
- Database schemas and migration history
- Existing code patterns extracted from the codebase
- External documentation snapshots

**Does NOT own:**
- Opinions or conventions (belongs in skills)
- Reasoning heuristics (belongs in agent identity)
- Task specifications (belongs in prompts)

**Key property:** Knowledge base content should be **mechanically verifiable**.
If you can't verify it by reading source code or documentation, it's not knowledge
— it's a convention or heuristic.

### 4.5 — Orchestration Layer Contract

**Owns:**
- Agent selection (which agent handles which phase)
- Skill attachment (which skills to load for the current context)
- Phase sequencing (Plan → Implement → Review → Done)
- Retry and escalation policies
- Context detection pipeline (see [06 — Context Detection](06-context-detection-and-adaptation.md))
- Progress tracking and status reporting

**Does NOT own:**
- Domain reasoning (belongs in agents)
- Framework conventions (belongs in skills)
- Task definition (belongs in prompts)
- Reference material (belongs in knowledge base)

---

## 5. Boundary Violations: A Catalog

Understanding clean boundaries is easier when you can recognize violations.
Here are the most common ones, organized by which layer is being overloaded:

### 5.1 — Agent Definitions Absorbing Skill Knowledge

**Symptom:** Agent file contains framework-specific instructions like
"use `@WebMvcTest` for controller tests" or "configure `testcontainers`
with `@DynamicPropertySource`."

**Consequence:** Agent cannot be reused for a different framework without
editing its definition. You end up with `JUnit5TestAgent`, `VitestTestAgent`,
`CypressTestAgent` — defeating composability.

**Fix:** Extract all framework-specific content into skill files. Agent file
retains only: "I reason about test isolation, coverage completeness, assertion
quality, and test naming conventions. Load appropriate skill files for
framework-specific guidance."

### 5.2 — Skills Embedding Reasoning Instructions

**Symptom:** Skill file contains statements like "Think step by step about
edge cases" or "First analyze the code, then identify gaps, then generate tests."

**Consequence:** Reasoning strategy changes when you swap skills, which violates
the Substitution Principle. Different skill files impose conflicting reasoning
approaches.

**Fix:** Move reasoning instructions to the agent definition. Skills should only
contain *what* conventions to follow, not *how* to think.

### 5.3 — Prompts Duplicating Skill Content

**Symptom:** Prompt template includes "Remember to use constructor injection,
not field injection" alongside a reference to `java.skill.md` which already
contains the same instruction.

**Consequence:** Dual source of truth. When the convention changes, one location
gets updated and the other doesn't. Agent receives conflicting signals.

**Fix:** Prompt references the skill file. Prompt contains only task-specific
constraints that don't belong in any skill (e.g., "focus on the `OrderService`
class only").

### 5.4 — Orchestration Containing Domain Logic

**Symptom:** Orchestrator has rules like "if the Implementer generates a
`@RestController`, remind it to use DTOs instead of entities."

**Consequence:** Orchestrator must be updated whenever domain conventions
change. It becomes a dumping ground for "things we keep forgetting."

**Fix:** Convention belongs in `rest-api.skill.md`. Orchestrator's job is to
ensure the Reviewer checks for convention compliance — not to enforce
conventions itself.

### 5.5 — Knowledge Bases Containing Conventions

**Symptom:** A file in `knowledge/` says "All REST endpoints must return
structured error DTOs" alongside database schema definitions.

**Consequence:** Conventions mixed with facts create confusion about authority.
Is this a fact about the project or a rule to follow? When the convention changes,
is the knowledge base wrong?

**Fix:** Conventions → skill files. Knowledge bases contain only verifiable
facts: "The error DTO has fields `code` (String) and `message` (String)."

### 5.6 — Skills Encoding Project-Specific Facts

**Symptom:** `java.skill.md` says "The User entity has fields `id`, `email`,
`name`, `createdAt`."

**Consequence:** Skill file is coupled to one project. Cannot be reused across
repositories without editing.

**Fix:** Project facts → knowledge base. Skills contain conventions like
"Entity fields should use `camelCase`" — not specific field names.

---

## 6. The Boundary Decision Flowchart

When you're unsure where a piece of behavior belongs, follow this decision tree:

```
Is it about WHAT to do right now?
  ├── YES → PROMPT
  └── NO ↓

Is it about HOW to think about this type of problem?
  ├── YES → AGENT IDENTITY
  └── NO ↓

Is it a convention, pattern, or anti-pattern for a domain/framework?
  ├── YES → SKILL
  └── NO ↓

Is it a verifiable fact about the project or external system?
  ├── YES → KNOWLEDGE BASE
  └── NO ↓

Is it about WHEN things happen or WHO does them?
  ├── YES → ORCHESTRATION
  └── NO ↓

It may be a cross-cutting concern. Consider:
  - Security constraints → SKILL (security.skill.md) + AGENT (security.agent.md)
  - Logging conventions → SKILL (observability.skill.md)
  - Output formats → AGENT IDENTITY (output contract) + PROMPT (task-specific overrides)
```

---

## 7. Cross-Cutting Concerns

Some behaviors legitimately span multiple layers. The key is to split them
into their constituent parts, each placed in the correct layer:

### Example: "Never return JPA entities from REST endpoints"

| Aspect | Layer | Content |
|--------|-------|---------|
| **Convention** | `rest-api.skill.md` | "Map entities to response DTOs before returning from controllers. Never expose JPA entities in API responses." |
| **Reasoning** | Reviewer Agent | "Check that no controller method returns an entity class directly. Verify DTO mapping is present." |
| **Enforcement** | Orchestration | "Reviewer must check all REST-related code against `rest-api.skill.md` conventions." |
| **Task trigger** | Prompt | (Not present — this is a standing convention, not a task-specific constraint) |

Each layer handles its own aspect. The convention is stated once (in the skill).
The reasoning about *how to check for it* is in the agent. The enforcement *that
it gets checked* is in orchestration.

---

## 8. Practical Implications for Enterprise Design

### 8.1 — Skill Files Are the Primary Mechanism for Enterprise Standardization

In an enterprise setting with hundreds of repositories, skill files are what you
standardize and distribute. They encode your organization's conventions without
coupling them to specific agents or prompts.

A central team maintains canonical skill files (e.g., `enterprise-java.skill.md`).
Project teams can compose them with project-specific skills. Agent definitions
rarely need customization at the project level.

### 8.2 — Agent Definitions Should Be Few and Stable

You need fewer agents than you think. The six-agent model (Orchestrator, Planner,
Implementer, Reviewer, Debugger, Security) covers most enterprise workflows. New
use cases are addressed by composing these agents with new skills — not by creating
new agents.

Create a new agent only when the **reasoning strategy** is fundamentally different,
not just when the **domain** is different.

### 8.3 — Orchestration Absorbs Complexity So Agents Don't Have To

The orchestration layer is where integration complexity lives: context detection,
skill selection, phase management, retry logic, escalation. This complexity is
*administrative*, not *intellectual*. Keeping it out of agents means agents stay
focused on their reasoning domain.

### 8.4 — Prompts Are the User-Facing Surface

End users interact with prompts (either templates or free-form). They should
never need to understand agent internals or skill file contents to use the system.
A well-designed prompt surface makes the entire architecture invisible to users.

---

## Key Takeaways

1. **Each layer owns exactly one type of behavior.** The Responsibility Matrix
   makes this unambiguous.

2. **The Substitution Principle is the correctness test.** If swapping one
   component at a layer requires changes to other layers, the boundary is violated.

3. **Six common boundary violations** account for most design problems. Learn to
   recognize them.

4. **Cross-cutting concerns are split, not duplicated.** Each aspect lives in
   exactly one layer.

5. **Skill files are the enterprise standardization mechanism.** They encode
   conventions portably across projects, agents, and prompts.

---

*Next: [02 — Agent Taxonomy](02-agent-taxonomy.md) — Core vs. Domain vs. Composite agents, the cognitive load threshold, and when to split.*
