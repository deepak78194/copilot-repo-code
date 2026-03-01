# 07 — Trade-offs & Decisions: Explicit Architectural Choices

> **Purpose:** Document every significant architectural decision in the migration workflow, the alternatives considered, the rationale for the chosen approach, and the conditions under which the decision should be revisited. This is the decision log for the architecture team.

---

## 1. Why Document Decisions

Every non-trivial design involves trade-offs. Documenting them serves three audiences:

1. **Future maintainers** who will ask "why was it done this way?"
2. **The architecture team** who needs to evaluate whether the decisions still hold.
3. **Agent authors** who need to understand the constraints their skills and prompts operate within.

Each decision follows the format:

```
DECISION:     What was decided
ALTERNATIVES: What else was considered
RATIONALE:    Why this option, specifically
TRADE-OFF:    What we gave up
REVISIT WHEN: Conditions that should trigger re-evaluation
```

---

## 2. Decision Catalog

### D-001: Reuse Core Agents Instead of Creating Migration Agents

**Decision:** Use the existing 5 Core agents (Planner, Implementer, Reviewer,
Test Agent, Debugger) with migration-specific skills, rather than creating
custom migration agents.

**Alternatives considered:**

| Option | Description | Rejected Because |
|--------|------------|-----------------|
| A. Custom migration agents | Create `legacy-analyzer.agent.md`, `migration-planner.agent.md`, etc. | Duplicates reasoning already in Core agents. Increases maintenance surface. Violates Cognitive Load Threshold. |
| B. Single migration agent | One all-purpose `migration.agent.md` | Exceeds Cognitive Load Threshold. Cannot reason about discovery, planning, implementation, testing, AND debugging in one identity. |
| C. **Core agents + skills** | Attach migration skills to existing Core agents | **Chosen.** |

**Rationale:**
- Core agents' reasoning (decomposition, production, verification) is universal
  and already validated by experiment data.
- Skills are the strongest lever for output quality
  (see [03 — Skill Design](../architecture/03-skill-and-knowledge-design.md) §1).
- The Substitution Principle holds: swapping `micronaut.skill` for
  `spring-boot.skill` requires zero agent changes.

**Trade-off:** Agent identities don't explicitly mention "migration," which makes
it less obvious to stakeholders which agents participate. Mitigated by the Migration
Orchestrator, which names and coordinates the participants.

**Revisit when:** A future migration domain introduces reasoning heuristics that
genuinely cannot be expressed as skill conventions (e.g., a "migration impact
scoring" reasoning mode that doesn't fit decomposition, production, or verification).

---

### D-002: New Migration Orchestrator (Not Extend Existing Orchestrator)

**Decision:** Create a new `migration-orchestrator.agent.md` rather than adding
migration logic to the existing general-purpose Orchestrator.

**Alternatives considered:**

| Option | Description | Rejected Because |
|--------|------------|-----------------|
| A. Extend existing Orchestrator | Add migration phases to the general Orchestrator's routing table | Violates separation of concerns. The general Orchestrator handles Plan→Implement→Review; migration needs a 5-phase pipeline with a human gate. |
| B. **New composite agent** | Dedicated Migration Orchestrator | **Chosen.** |
| C. No orchestrator | Let the user manually trigger each phase | Loses the gate enforcement that prevents the curious agent problem. |

**Rationale:**
- The 5-phase pipeline with a mandatory human gate (G2) is fundamentally
  different from the standard Plan→Implement→Review workflow.
- A dedicated Orchestrator cleanly owns the migration state machine.
- The general Orchestrator can delegate to the Migration Orchestrator as a
  sub-agent if needed (hierarchical delegation pattern from
  [05 — Orchestration](../architecture/05-orchestration-patterns.md) §3.5).

**Trade-off:** One additional agent to maintain. Acceptable because its definition
is pure coordination — no domain content that can drift.

**Revisit when:** The standard Orchestrator gains native support for custom
multi-phase pipelines with configurable gates.

---

### D-003: Skills Over Monolithic Instruction Sets

**Decision:** Create 6 separate, composable skill files rather than one large
`migration.skill.md`.

**Alternatives considered:**

| Option | Description | Rejected Because |
|--------|------------|-----------------|
| A. Monolithic skill | One `migration.skill.md` with everything | Violates skill composition rules. Too large for effective context window usage. Cannot be selectively attached per phase. |
| B. Two skills (source + target) | `legacy.skill.md` + `modern.skill.md` | Better but still too coarse. The Planner needs different conventions than the Reviewer. |
| C. **6 composable skills** | Separated by concern and phase | **Chosen.** |

**Rationale:**
- Different phases need different skill combinations. Discovery needs
  `legacy-analysis.skill` but not `migration-checklist.skill`. Verification
  needs the opposite.
- Smaller skills are more effective in the context window — the agent sees
  only what's relevant to its current task.
- Each skill can be versioned independently (important for the feedback loop
  from [03 — Skill Design](../architecture/03-skill-and-knowledge-design.md) §6).

**Trade-off:** More files to manage. Mitigated by clear naming and the skill
contract structure which makes each file self-describing.

**Revisit when:** Context window sizes grow large enough that a single comprehensive
skill file (< 2000 tokens) is practical without relevance dilution.

---

### D-004: Separate Discovery Phase (Not Inline with Planning)

**Decision:** Discovery is a standalone phase (Phase 1) with its own gate (G1),
not merged into the Planning phase.

**Alternatives considered:**

| Option | Description | Rejected Because |
|--------|------------|-----------------|
| A. Inline discovery | Planner reads legacy code while planning | The plan is built on shifting analysis. Discovery findings aren't validated before planning begins. Harder to debug when the plan is wrong — was the analysis wrong or the planning wrong? |
| B. **Separate phase** | Discovery Report as a distinct, gated artifact | **Chosen.** |
| C. User-provided analysis | User manually writes the legacy analysis | Burdensome. Inconsistent format. Missing details. |

**Rationale:**
- Separation of concerns: analysis (what exists) vs. design (what to build) are
  different cognitive tasks.
- The Discovery Report is referenceable by all subsequent phases, providing a
  single source of truth about the legacy system.
- Gate G1 validates completeness before planning begins, catching incomplete
  analysis early.
- Debugging is clearer: if the plan is wrong, check the Discovery Report first.

**Trade-off:** Adds latency — the workflow has 5 phases instead of 4. Acceptable
because the accuracy improvement from validated discovery far outweighs the
time cost.

**Revisit when:** Discovery can be reliably performed incrementally (e.g., the
agent can discover and plan in parallel without quality loss).

---

### D-005: Anti-Transliteration as Conventional Zone (Not Mandatory)

**Decision:** The Anti-Transliteration Principle operates at the **Conventional Zone**
level — agents SHOULD follow it with justified exceptions — rather than the
Mandatory Zone.

**Alternatives considered:**

| Option | Description | Rejected Because |
|--------|------------|-----------------|
| A. **Conventional Zone** | SHOULD follow, with justified deviation | **Chosen.** |
| B. Mandatory Zone | MUST follow, zero exceptions | Sometimes a 1:1 translation is genuinely the best approach (e.g., a simple CRUD operation where the legacy pattern is already correct). Making it mandatory forces unnecessary rewrites. |
| C. Innovation Zone | MAY follow, fully optional | Too weak. Most legacy patterns should be improved. Optional guidance leads to transliteration by default. |

**Rationale:**
- Most legacy patterns should be modernized (Conventional Zone is the right default).
- Some legacy patterns are already well-designed and a 1:1 mapping is correct.
  The agent must be able to make this judgment and explain it.
- Specific sub-rules *within* anti-transliteration (e.g., "never expose JPA entities")
  are in the Mandatory Zone. The overall principle is Conventional.

**Trade-off:** Agents may occasionally transliterate when they shouldn't. Mitigated
by the Reviewer's anti-transliteration checklist
(see [05 — Freshness](05-freshness-and-optimization.md) §6).

**Revisit when:** Experiment data shows that agents transliterate too frequently even
with Conventional Zone enforcement. If transliteration rate exceeds 20%, escalate
to Mandatory.

---

### D-006: Maximum 2 Verification Loop Iterations

**Decision:** The verification feedback loop (Phase 4) runs at most **2 complete
iterations** before escalating to a human.

**Alternatives considered:**

| Option | Description | Rejected Because |
|--------|------------|-----------------|
| A. 1 iteration | Fix once, then escalate | Too aggressive. Many issues are fixable on second attempt after better diagnosis. |
| B. **2 iterations** | Fix twice, then escalate | **Chosen.** |
| C. 3+ iterations | Multiple retry attempts | Diminishing returns. If 2 fix attempts fail, the issue is likely structural (plan-level). More iterations waste resources on code-level fixes for plan-level problems. |
| D. Unlimited | Loop until success | Risk of infinite loops. Agents can chase their tail, making complementary errors that never converge. |

**Rationale:**
- Empirical observation: most compilation errors and simple test failures resolve
  in 1 iteration. If the second iteration fails, the root cause is typically a
  plan-level issue (wrong assumption about framework capability, missing
  dependency, incorrect architecture mapping).
- 2 iterations provide enough data for a meaningful escalation report (iteration 1
  results + iteration 2 results = trend).
- Budget is configurable via the prompt template
  (`max_verification_iterations` parameter) for teams that want different behavior.

**Trade-off:** Some fixable issues may be escalated unnecessarily. Acceptable
because human review at this stage is cheap (the escalation report contains
full diagnostic context) and false escalation is safer than infinite loops.

**Revisit when:** Agent diagnostic capabilities improve significantly (e.g., the
Debugger can reliably distinguish plan-infeasible from implementation-bug in
one pass). With better diagnosis, iteration 2 becomes more productive and the
budget could increase to 3.

---

### D-007: PR Documentation Scope — Comprehensive Over Minimal

**Decision:** Phase 5 (Delivery) generates a comprehensive PR with migration
summary, decision log, architecture diagram, and test coverage — not just a
diff description.

**Alternatives considered:**

| Option | Description | Rejected Because |
|--------|------------|-----------------|
| A. Minimal PR | Standard diff description + brief summary | Loses the rich context from the migration process. Reviewers can't understand *why* decisions were made. |
| B. **Comprehensive PR** | Full migration summary, decision log, architecture diagram | **Chosen.** |
| C. Separate documentation | PR is minimal, full docs in a separate wiki | Disconnects documentation from the code change. Documentation drifts. |

**Rationale:**
- The migration workflow generates significant decision context (Discovery Report,
  Migration Plan, verification iterations, escalation resolutions) that is
  invaluable for reviewers.
- A comprehensive PR serves as the permanent record of the migration, tied to
  the code change it describes.
- The `migration-documentation.skill.md` ensures consistent documentation format
  across all migrations.

**Trade-off:** Larger PRs. Mitigated by clear section structure with a table of
contents, allowing reviewers to read the summary and dive deeper only where needed.

**Revisit when:** The team establishes a separate migration documentation system
(e.g., Architecture Decision Records in a dedicated repository) that makes
PR-embedded documentation redundant.

---

## 3. Enterprise Configuration Surface

These decisions define the configuration knobs available to enterprise teams
adopting the migration workflow:

| Parameter | Default | Configurable Where | Range |
|-----------|---------|-------------------|-------|
| `innovation_budget` | 5 | Prompt template parameter | 0–10 |
| `max_verification_iterations` | 2 | Prompt template parameter | 1–5 |
| `target_framework` | micronaut | Prompt template parameter | micronaut, spring-boot |
| `max_plan_revisions` | 3 | Orchestrator constraint | 1–5 |
| `discovery_scan_depth` | 3 directories | `legacy-analysis.skill.md` | 1–5 |
| `endpoint_detail_threshold` | 50 | `legacy-analysis.skill.md` | 10–200 |
| `anti_transliteration_zone` | Conventional | `migration-checklist.skill.md` | Mandatory, Conventional |
| `pr_documentation_level` | Comprehensive | `migration-documentation.skill.md` | Minimal, Standard, Comprehensive |

### 3.1 — High-Compliance Configuration

For regulated industries or high-risk migrations:

```yaml
innovation_budget: 0              # No suggestions — strict 1:1 migration
max_verification_iterations: 3    # More retry attempts before escalation
anti_transliteration_zone: Mandatory  # Zero tolerance for pattern copying
pr_documentation_level: Comprehensive # Full audit trail
```

### 3.2 — Agile Team Configuration

For experienced teams with high trust:

```yaml
innovation_budget: 10             # Maximum suggestions
max_verification_iterations: 1    # Fast escalation
anti_transliteration_zone: Conventional  # Allow justified exceptions
pr_documentation_level: Standard  # Balanced documentation
```

---

## 4. Decision Dependencies

Some decisions depend on or reinforce others. This map shows the relationships:

```
D-001 (Core agents)
  ├──→ D-003 (Composable skills) — Core agents need composable skills to specialize
  └──→ D-002 (New Orchestrator) — Core agents need coordination for 5-phase pipeline

D-002 (New Orchestrator)
  ├──→ D-004 (Separate Discovery) — Orchestrator manages the gate between Discovery and Planning
  └──→ D-006 (2 iterations) — Orchestrator enforces the retry budget

D-003 (Composable skills)
  └──→ D-005 (Anti-transliteration zone) — Skills define what zone each convention operates in

D-004 (Separate Discovery)
  └──→ D-001 (Core agents) — Same Planner agent, different skills per phase

D-005 (Anti-transliteration zone)
  └──→ D-006 (2 iterations) — Reviewer catches transliteration in verification loop

D-006 (2 iterations)
  └──→ D-002 (New Orchestrator) — Orchestrator owns the retry budget

D-007 (PR documentation)
  └──→ D-004 (Separate Discovery) — PR references the Discovery Report and Plan
```

**Key insight:** Decisions D-001, D-002, and D-003 form a reinforcing triangle.
Changing any one of them has implications for the other two. They should be
re-evaluated together if any one is revisited.

---

## 5. Open Questions for Future Resolution

These are questions identified during design that do not yet have definitive
answers. They are logged here for future experimentation:

| # | Question | Current Assumption | How to Resolve |
|---|---------|-------------------|----------------|
| Q-001 | Should the Discovery phase run automatically or require user trigger? | User triggers. Agent doesn't discover without being asked. | Experiment: run auto-discovery on 5 projects, measure accuracy vs. triggered discovery. |
| Q-002 | Can the same workflow handle database schema migration (Flyway)? | Schema migration is included as a task in the plan. | Validate: does the Implementer reliably produce correct Flyway migration scripts? |
| Q-003 | How do we handle legacy code with no tests? | Discovery reports "no tests found" as a risk. Verification still generates tests for the modern code. | Experiment: compare test quality for migrated code with vs. without legacy test reference. |
| Q-004 | Should the Reviewer run inline (during Implementation) or as a separate phase? | Separate — runs during Verification (Phase 4). | Experiment: run inline review on 3 migrations, measure error catch rate vs. rework cost. |
| Q-005 | At what scale does single-batch migration break down? | Assumption: > 20 services requires batching. | Measure: run single-batch migration at 10, 20, 50 service scale. Track context window exhaustion. |
