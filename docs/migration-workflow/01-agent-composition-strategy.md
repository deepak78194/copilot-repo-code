# 01 — Agent Composition Strategy: Reuse Over Reinvention

> **Purpose:** Justify why the migration workflow reuses Core agents with migration-specific skills rather than creating custom migration agents. Establish the agent-to-skill mapping for each phase, apply the Substitution Principle as a correctness test, and address the "curious agent" problem through agent constraints.

---

## 1. The Core Trade-off: Custom Agents vs. Skilled Core Agents

When designing a migration workflow, the first architectural question is:

> *"Should I create a Migration Agent, or should I use the Planner with
> migration skills?"*

### 1.1 — Option A: Custom Migration Agents

Create purpose-built agents for the migration domain:  
`legacy-analyzer.agent.md`, `migration-planner.agent.md`, `migration-implementer.agent.md`

**Pros:**
- Agent identity can embed migration-specific reasoning heuristics
- Tighter coupling between identity and task
- Easier to explain to stakeholders ("the Migration Agent does migrations")

**Cons:**
- Duplicates reasoning that already exists in Core agents (task decomposition → Planner,
  code production → Implementer, correctness verification → Reviewer)
- Increases the agent count (maintenance burden)
- Violates the Cognitive Load Threshold: a "migration analyzer" is just a Planner
  reasoning about a specific domain
- Cannot be reused for non-migration work

### 1.2 — Option B: Core Agents + Migration Skills

Give existing Core agents migration-specific skills:  
Planner + `legacy-analysis.skill.md` + `migration-planning.skill.md`

**Pros:**
- Zero new agents to maintain
- Core reasoning (decomposition, production, verification) is battle-tested
- Skills are composable — `legacy-analysis.skill.md` can be used by *any* agent that
  needs to understand legacy code
- Satisfies the Substitution Principle (swap skills, not agents)

**Cons:**
- Skills must be comprehensive enough to cover migration-specific patterns
- Agent identity doesn't explicitly mention "migration" (cosmetic concern)

### 1.3 — Decision: Option B

We use **Core agents with migration-specific skills**. This aligns with:

- **[02 — Agent Taxonomy](../architecture/02-agent-taxonomy.md) §2.1:** Core agents
  understand *what good looks like* universally. A Planner decomposes problems; it
  doesn't need to know it's a "migration" to decompose well.
- **[03 — Skill Design](../architecture/03-skill-and-knowledge-design.md) §1:** Skills
  are the strongest predictor of output quality. Better skills → better migration output.
- **[07 — Freshness](../architecture/07-freshness-and-bounded-autonomy.md) §2:** Fewer
  agents means less drift surface area.

The only new Composite agent is the **Migration Orchestrator**, which coordinates the
five-phase pipeline. Coordination is a Tier 3 concern — it cannot be delegated to skills.

---

## 2. Agent-to-Phase Mapping

### 2.1 — Phase 1: Discovery

| Component | Value |
|-----------|-------|
| **Agent** | Planner |
| **Skills** | `legacy-analysis.skill.md`, `clean-architecture.skill.md` |
| **Knowledge** | Legacy codebase (read-only), Modern workspace layout |
| **Reasoning** | Task decomposition applied to reverse engineering: identify endpoints, extract business logic, map data access patterns, catalog dependencies |

The Planner's native reasoning — *"break complex work into ordered, independently
testable steps"* — maps perfectly to legacy analysis. The Planner doesn't scan
EJB annotations because it "knows EJBs." It scans them because
`legacy-analysis.skill.md` tells it: "when you encounter `@Stateless`, this marks
a transactional service boundary."

### 2.2 — Phase 2: Planning

| Component | Value |
|-----------|-------|
| **Agent** | Planner |
| **Skills** | `migration-planning.skill.md`, `clean-architecture.skill.md`, `micronaut.skill.md` |
| **Knowledge** | Discovery Report (from Phase 1), API design conventions |
| **Reasoning** | Decompose the migration into ordered tasks. Map legacy components to clean architecture layers. Design the target API. |

Same agent, different skills. The Planner produces a migration plan because
`migration-planning.skill.md` defines the plan structure and
`clean-architecture.skill.md` defines the target patterns.

### 2.3 — Phase 3: Implementation

| Component | Value |
|-----------|-------|
| **Agent** | Implementer |
| **Skills** | `java.skill.md`, `micronaut.skill.md`, `clean-architecture.skill.md` |
| **Knowledge** | Approved Migration Plan, Legacy Analysis Report |
| **Reasoning** | Code production: write minimal code that satisfies the plan. Follow conventions from loaded skills. |

### 2.4 — Phase 4: Verification

| Component | Value |
|-----------|-------|
| **Agent(s)** | Test Agent → Implementer → Debugger (feedback loop) |
| **Skills** | `junit5.skill.md`, `java.skill.md`, `micronaut-testing.skill.md`, `clean-architecture.skill.md` |
| **Knowledge** | Generated source code, Migration Plan |
| **Reasoning** | Test Agent generates tests. Implementer runs build. Debugger diagnoses failures. Loop up to 2 times. |

### 2.5 — Phase 5: Delivery

| Component | Value |
|-----------|-------|
| **Agent** | Implementer (documentation mode) |
| **Skills** | `migration-documentation.skill.md` |
| **Knowledge** | All prior artifacts (plan, code, tests, build results) |
| **Reasoning** | Generate PR description, migration summary, decision log. |

---

## 3. Substitution Principle Verification

The Substitution Principle (from [01 — Responsibility Boundaries](../architecture/01-responsibility-boundaries.md) §3)
is the correctness test. If swapping a component at any layer changes behavior at
that layer's abstraction level *without requiring changes to other layers*, the
boundaries are correctly drawn.

### 3.1 — Skill Substitution: Micronaut → Spring Boot

**Swap:** Replace `micronaut.skill.md` with `spring-boot.skill.md`

**Expected effect:** Implementer produces Spring Boot code instead of Micronaut
code. Controllers use `@RestController` instead of `@Controller`. DI uses
`@Autowired` (constructor) instead of `@Inject`.

**Must NOT require:** Changes to the Planner, Implementer, Reviewer, Test Agent,
or Migration Orchestrator definitions.

**Verification:** The Migration Orchestrator's skill selection in its routing
table changes one entry. No agent identity changes.

```
# Before
phase: implementation
  agent: implementer
  skills: [java.skill, micronaut.skill, clean-architecture.skill]

# After
phase: implementation
  agent: implementer
  skills: [java.skill, spring-boot.skill, clean-architecture.skill]
```

### 3.2 — Skill Substitution: Jakarta EE → .NET Legacy

**Swap:** Replace `legacy-analysis.skill.md` (Jakarta EE edition) with
`legacy-analysis-dotnet.skill.md` (ASP.NET / WCF edition)

**Expected effect:** Discovery phase scans for `.csproj`, `Controller` classes,
Entity Framework contexts instead of `@Stateless`, `persistence.xml`, EJBs.

**Must NOT require:** Changes to the Planner agent or Migration Orchestrator.

**Verification:** The Planner's reasoning — *decompose, identify endpoints,
extract business logic, map data access* — is identical. Only the signals and
patterns in the skill file change.

### 3.3 — Agent Substitution: Planner → Enhanced Planner

**Swap:** Replace the standard Planner with an enhanced Planner that uses
chain-of-thought reasoning.

**Expected effect:** More detailed migration plans with explicit reasoning traces.

**Must NOT require:** Changes to skill files, orchestration logic, or prompts.

---

## 4. Solving the Curious Agent Problem

### 4.1 — The Problem Restated

The "curious agent" is an agent that begins implementing before the plan is approved.
This happens when:

1. The agent's context includes both the legacy code and the implementation skills.
2. The agent's reasoning chain naturally flows from "I understand the legacy code" to
   "I know how to write the modern code" to "Let me write it."
3. Nothing in the agent's identity or the orchestration logic explicitly stops it.

### 4.2 — Three-Layer Defense

The defense is layered across three architectural levels:

#### Layer 1: Agent Constraints (Identity Level)

Each agent's definition explicitly limits its scope:

```
# In planner.agent.md — phase-specific constraints
PHASE CONSTRAINTS:
  When operating in DISCOVERY phase:
    - DO: Analyze, decompose, catalog, report.
    - DO NOT: Suggest implementation code.
    - DO NOT: Write any file to the modern workspace.

  When operating in PLANNING phase:
    - DO: Design API structure, map to clean architecture, create task breakdown.
    - DO NOT: Write implementation code.
    - DO NOT: Assume the plan will be approved.
```

#### Layer 2: Orchestrator Skill Gating (Orchestration Level)

The Migration Orchestrator only attaches implementation skills *after* the plan is
approved:

```
SKILL GATING POLICY:
  Phase 1 (Discovery):
    Attach: [legacy-analysis.skill, clean-architecture.skill]
    Withhold: [micronaut.skill, java.skill]  ← no implementation tools

  Phase 2 (Planning):
    Attach: [migration-planning.skill, clean-architecture.skill, micronaut.skill]
    Note: micronaut.skill is attached for reference (API design) but
          Planner's constraints prevent code generation.

  Phase 3 (Implementation):
    Attach: [java.skill, micronaut.skill, clean-architecture.skill]
    Note: NOW the Implementer has both the approved plan and the skills
          to execute it.
```

This is the **skill gating** pattern: the Orchestrator uses skill attachment as a
control mechanism. An agent cannot produce Micronaut code if it has never seen
`micronaut.skill.md`.

#### Layer 3: Output Validation (Gate Level)

Even if the agent produces code despite constraints, the gate validation catches it:

```
GATE G1 (Discovery → Planning):
  VALIDATE:
    - Output is a Discovery Report, not code files.
    - No .java files were generated.
    - Report contains required sections: endpoints, services,
      data_access, dependencies, transactions, error_handling.
  ON FAILURE:
    - Reject output.
    - Log violation: "Agent attempted code generation during Discovery."
    - Re-run the phase with reinforced constraints.
```

### 4.3 — Why Three Layers?

No single layer is sufficient:

- **Agent constraints alone:** The agent may "forget" constraints in a long context
  window or rationalize that "just a little code example" isn't violating the rule.
- **Skill gating alone:** The agent might produce pseudo-code or framework-agnostic
  code without specific skills.
- **Output validation alone:** The damage is already done — the agent spent tokens
  and time producing output that will be rejected.

The three layers create defense in depth:
1. Constraints make the agent *unlikely* to try.
2. Skill gating makes it *unable* to produce quality violations.
3. Output validation *catches* anything that slips through.

---

## 5. Agent Count Summary

| Agent | Type | Pre-existing? | Migration-Specific Changes |
|-------|------|-------------|---------------------------|
| **Migration Orchestrator** | Composite (Tier 3) | **New** | Entire definition is new. Coordinates 5-phase pipeline. |
| **Planner** | Core (Tier 1) | Existing | Zero identity changes. Receives migration skills. |
| **Implementer** | Core (Tier 1) | Existing | Zero identity changes. Receives clean-arch + micronaut skills. |
| **Reviewer** | Core (Tier 1) | Existing | Zero identity changes. Receives migration-checklist skill. |
| **Test Agent** | Core (Tier 1) | Existing | Zero identity changes. Receives micronaut-testing skill. |
| **Debugger** | Core (Tier 1) | Existing | Zero identity changes. Participates in verification loop. |

**Total new agents:** 1 (the Migration Orchestrator)  
**Total new skills:** 6 (see [06 — Prompts, Skills & Knowledge](06-prompts-skills-knowledge-separation.md))  
**Core agents modified:** 0

This validates the architecture's claim from [02 — Agent Taxonomy](../architecture/02-agent-taxonomy.md) §4:
*"Start any new workflow design with the 5 Core agents. Add skills first.
Create new agents only when the Cognitive Load Threshold is crossed."*
