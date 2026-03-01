# 00 — Migration Workflow Overview: Jakarta EE to Micronaut Clean Architecture

> **Purpose:** Define the complete agentic workflow for migrating legacy Jakarta EE APIs to Micronaut microservices using clean architecture. This document introduces the five-phase pipeline, the agent composition strategy, gate-based progression, and the Migration Orchestrator — the composite agent that coordinates the entire process.

---

## 1. The Migration Problem

Migrating a legacy Jakarta EE application to Micronaut with clean architecture is
not a mechanical translation. It requires understanding two codebases — one that
exists and one that must be created — and making intentional architectural decisions
at every boundary.

The common failure mode is **transliteration**: taking an EJB method body, wrapping
it in a Micronaut `@Controller`, and calling it "migrated." The result is a new
application with all the old application's problems plus new ones.

A well-designed migration workflow must:

1. **Understand the legacy system** deeply enough to identify what it does, not just how it does it.
2. **Design the modern system** with fresh patterns suited to the target framework.
3. **Prevent copy-paste migration** by treating the legacy code as a *requirements source*, not a template.
4. **Verify behavioral equivalence** while allowing structural divergence.
5. **Gate progression** so that no phase proceeds without validated output from the previous phase.

---

## 2. The Five-Phase Pipeline

The migration workflow proceeds through five ordered phases. Each phase has a
defined entry condition, executing agent(s), output artifact, and gate condition
for progression.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│  PHASE 1         PHASE 2          PHASE 3           PHASE 4      PHASE 5   │
│  DISCOVERY  ──→  PLANNING   ──→  IMPLEMENTATION ──→ VERIFICATION ──→ DELIVERY│
│                  (+ Gate)                           (+ Loop)               │
│                                                                             │
│  Understand      Design the       Write the          Build, test,   Generate │
│  the legacy      modern API       clean-arch         diagnose,      PR with  │
│  system          + approval       implementation     re-implement   docs     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Phase Summary Table

| Phase | Name | Primary Agent(s) | Entry Condition | Output Artifact | Gate |
|-------|------|-------------------|-----------------|-----------------|------|
| 1 | **Discovery** | Planner + legacy-analysis skills | User triggers migration prompt | Legacy Analysis Report + Modern Workspace Report | Orchestrator validates report completeness |
| 2 | **Planning** | Planner + migration-planning skills | Discovery report complete | Migration Plan (with API redesign, clean arch mapping, task breakdown) | **Human approval** (mandatory gate) |
| 3 | **Implementation** | Implementer + clean-arch skills + micronaut skills | Approved migration plan | Generated source code (ports, adapters, use cases, entities) | Compilation succeeds |
| 4 | **Verification** | Test Agent + Implementer + Debugger | Compilation passes | Passing test suite + build verification | All tests green, build clean |
| 5 | **Delivery** | Implementer (documentation mode) | Verification passes | Pull request with migration summary, decision log, test coverage | PR created |

---

## 3. Gate-Based Progression

Gates are the primary mechanism that prevents the "curious agent" problem — agents
that skip ahead and start coding before the plan is approved, or merge before
tests pass.

### 3.1 — The Curious Agent Problem

Without explicit gates, agents exhibit a consistent failure mode:

```
USER: "Migrate the OrderService from Jakarta EE to Micronaut"

UNCONTROLLED AGENT:
  1. Reads OrderService.java               ← Good
  2. Starts writing MicronautOrderService   ← WRONG: skipped planning
  3. Produces code that mirrors the old API  ← WRONG: transliteration
  4. Never writes tests                      ← WRONG: skipped verification
  5. Declares "done"                         ← Not actually done
```

This happens because the agent optimizes for *looking productive* over *being correct*.
Gates force a pause between phases, requiring validated output before progression.

### 3.2 — Gate Definitions

| Gate | Between Phases | Type | Validation |
|------|---------------|------|------------|
| **G1 — Discovery Complete** | 1 → 2 | Automated | Report contains all required sections (endpoints, services, data access, dependencies, transactions, error handling) |
| **G2 — Plan Approved** | 2 → 3 | **Human** | User explicitly approves the migration plan. No implicit approval. No timeout-based auto-approval. |
| **G3 — Build Passes** | 3 → 4 | Automated | `./gradlew build` succeeds with zero compilation errors |
| **G4 — Tests Green** | 4 → 5 | Automated | All generated tests pass. Coverage meets threshold. |

### 3.3 — The Human Gate (G2)

Gate G2 is the most important gate in the workflow. It exists because:

1. Migration plans involve **irreversible architectural decisions** (port boundaries,
   aggregate design, service decomposition).
2. The Planner may misunderstand legacy business logic.
3. The user may have preferences that aren't captured in skill files.

The Orchestrator **MUST** present the plan and wait. It must not:
- Auto-approve after a timeout
- Interpret silence as approval
- Proceed with a "draft" implementation
- Ask "should I proceed?" and treat any response as approval

The only acceptable progression signal is an explicit approval keyword or action.

```
ORCHESTRATOR → USER:
  "Migration plan ready for review. See the plan below.
   [19 tasks across 4 clean architecture layers]

   Reply APPROVE to proceed with implementation.
   Reply REVISE with feedback to modify the plan."

USER → ORCHESTRATOR:
  "APPROVE"                          ← G2 passes
  "REVISE: split OrderService..."    ← Plan re-enters Phase 2
  (silence)                          ← G2 remains closed. No action.
```

---

## 4. The Migration Orchestrator

The Migration Orchestrator is a **Composite agent** (Tier 3 in the taxonomy from
[02 — Agent Taxonomy](../architecture/02-agent-taxonomy.md) §2). It carries zero
migration-domain knowledge. Its entire identity is coordination.

### 4.1 — Identity Definition

```
ROLE: Migration Orchestrator
TYPE: Composite Agent (Tier 3)

PURPOSE:
  Coordinate the five-phase Jakarta EE → Micronaut migration workflow.
  Route to appropriate agents with appropriate skills at each phase.
  Enforce gate-based progression. Prevent premature implementation.

REASONING APPROACH:
  I track workflow state. I do not reason about code quality,
  architecture, security, or testing — those are the responsibilities
  of the agents I coordinate.

  My decisions are:
  1. Which phase are we in?
  2. Is the current phase's gate satisfied?
  3. Which agent and skills should handle the next phase?
  4. Has an error or ambiguity occurred that requires escalation?

CONSTRAINTS:
  - NEVER generate or modify source code directly.
  - NEVER approve a migration plan on behalf of the user.
  - NEVER skip a phase or gate.
  - NEVER proceed past G2 (Plan Approved) without explicit human approval.
  - Maximum 2 verification loops before escalating (Phase 4).
  - Always present phase transitions to the user.
```

### 4.2 — State Machine

```
                         ┌─────────────────────────────────────┐
                         │                                     │
                         ▼                                     │
┌──────┐  trigger  ┌──────────┐  G1  ┌──────────┐  G2    ┌────────────┐
│ IDLE │──────────→│ DISCOVERY│─────→│ PLANNING │───────→│IMPLEMENTING│
└──────┘           └──────────┘      └──────────┘        └────────────┘
                                          │                    │
                                     REVISE│               G3  │
                                          │                    ▼
                                          └──────┐      ┌──────────────┐
                                                 │      │ VERIFICATION │
                                                 │      └──────────────┘
                                                 │             │
                                            (re-plan)     G4   │  fail (≤2)
                                                 │             │      │
                                                 │             ▼      │
                                                 │      ┌──────────┐  │
                                                 │      │ DELIVERY │  │
                                                 │      └──────────┘  │
                                                 │                    │
                                                 │          ┌────────┘
                                                 │          ▼
                                                 │   ┌─────────────┐
                                                 └──→│  ESCALATE   │ (after 2 loops)
                                                     └─────────────┘
```

---

## 5. How This Workflow Maps to the Architecture

This migration workflow is a concrete application of the generic architecture
defined in [docs/architecture/](../architecture/). Every design decision maps
to an architectural principle:

| Migration Workflow Concept | Architecture Principle | Reference |
|---------------------------|----------------------|-----------|
| Five-phase pipeline | Sequencing pattern: Linear Pipeline | [05 — Orchestration](../architecture/05-orchestration-patterns.md) §3.1 |
| Gate-based progression | Gate-based sequencing | [05 — Orchestration](../architecture/05-orchestration-patterns.md) §3.2 |
| Migration Orchestrator | Tier 3 Composite Agent | [02 — Agent Taxonomy](../architecture/02-agent-taxonomy.md) §2 |
| Reusing Core agents over custom ones | Core Agent tier preference | [02 — Agent Taxonomy](../architecture/02-agent-taxonomy.md) §2.1 |
| Anti-Transliteration Principle | Innovation Zone + Innovation Budget | [07 — Freshness](../architecture/07-freshness-and-bounded-autonomy.md) §5 |
| Legacy analysis skills | Skill composition model | [03 — Skill Design](../architecture/03-skill-and-knowledge-design.md) §3 |
| Human approval gate | Escalation: human-required | [05 — Orchestration](../architecture/05-orchestration-patterns.md) §5 |
| Context detection for legacy workspace | Context detection pipeline | [06 — Context Detection](../architecture/06-context-detection-and-adaptation.md) §2 |

---

## 6. Reading Guide

| Document | What You'll Learn |
|----------|------------------|
| [01 — Agent Composition Strategy](01-agent-composition-strategy.md) | Why we reuse Core agents instead of creating migration-specific ones |
| [02 — Dual-Workspace Discovery](02-dual-workspace-discovery.md) | How Phase 1 analyzes both legacy and modern workspaces |
| [03 — Migration Planner Design](03-migration-planner-design.md) | How Phase 2 produces an approvable migration plan |
| [04 — Implementation & Verification](04-implementation-and-verification.md) | How Phases 3–4 generate code and verify it with a feedback loop |
| [05 — Freshness & Optimization](05-freshness-and-optimization.md) | How the workflow prevents transliteration and enforces modern patterns |
| [06 — Prompts, Skills & Knowledge Separation](06-prompts-skills-knowledge-separation.md) | What new skill files, prompts, and knowledge bases are needed |
| [07 — Trade-offs & Decisions](07-trade-offs-and-decisions.md) | Explicit architectural decisions, alternatives considered, and rationale |
