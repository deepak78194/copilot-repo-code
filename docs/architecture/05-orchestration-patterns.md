# 05 — Orchestration Patterns: Routing, Sequencing, and Lifecycle

> **Purpose:** Define the Orchestrator's responsibilities, catalog the primary routing and sequencing patterns, explain sub-agent delegation, and establish escalation policies for enterprise workflows.

---

## 1. The Orchestrator's Three Jobs

The Orchestrator is a Composite agent (see [02 — Agent Taxonomy](02-agent-taxonomy.md) §2, Tier 3). It carries zero domain knowledge. Its entire value lies in three coordination functions:

| Job | Description | Analogy |
|-----|------------|---------|
| **Route** | Select the right agent and attach the right skills for each phase | Air traffic controller assigning runways |
| **Sequence** | Manage phase transitions and dependencies between work items | Project manager ordering tasks |
| **Escalate** | Handle failures, ambiguity, loops, and scope violations | Safety valve that involves a human |

If the Orchestrator is doing anything else — reviewing code quality, suggesting
architectural patterns, evaluating security — it has absorbed domain logic that
belongs in a Core or Domain agent.

---

## 2. Routing Strategies

Routing is the decision of *which agent* handles a task and *which skills* are
attached. There are three strategies, each appropriate for different maturity
levels and trust levels.

### 2.1 — Static Routing

Every task type maps to a predetermined agent + skill combination. The mapping
is a configuration table, not runtime logic.

```
ROUTING TABLE:
  task_type: "generate-code"
    agent: implementer
    skills: [{{language}}.skill, {{framework}}.skill]

  task_type: "generate-tests"
    agent: test-agent
    skills: [testing.skill, {{test_framework}}.skill, {{language}}.skill]

  task_type: "review-code"
    agent: reviewer
    skills: [{{language}}.skill, {{framework}}.skill, rest-api.skill]

  task_type: "security-audit"
    agent: security-agent
    skills: [security.skill, {{language}}.skill]
```

**Pros:** Maximum predictability. Easy to audit. No surprises.

**Cons:** Cannot handle novel task types. Requires manual updates when new
skill combinations are needed.

**Best for:** Early-stage adoption, high-compliance environments, workflows
where predictability matters more than flexibility.

### 2.2 — Context-Aware Routing

The Orchestrator inspects project context (detected via the pipeline described
in [06 — Context Detection](06-context-detection-and-adaptation.md)) and selects
skills dynamically.

```
ROUTING LOGIC:
  1. Detect project context → build Context Object
  2. Classify task type from user prompt
  3. Look up agent from task_type → agent mapping (still static)
  4. Select skills from Context Object:
     - Language skill from detected language
     - Framework skill from detected framework
     - Testing skill from detected test framework
  5. Verify: are all selected skills available?
     - YES → proceed
     - NO → escalate (missing skill file — cannot guarantee quality)
```

**Pros:** Adapts to project changes without config updates. Handles multi-language
repositories naturally.

**Cons:** Detection pipeline can make mistakes. Less auditable than static routing.

**Best for:** Mature teams, multi-technology repositories, workflows where
adaptability matters.

### 2.3 — User-Confirmed Routing

The Orchestrator proposes a routing plan and waits for user confirmation before
executing. This is the **Detect-and-Confirm** pattern from
[04 — Prompt Design](04-prompt-design.md) §5.2, applied to routing specifically.

```
ORCHESTRATOR PROPOSAL:
  "I'll generate unit tests for OrderService using:
   - Agent: Test Agent
   - Skills: junit5.skill (v1.5), java.skill (v2.1), spring-boot.skill (v1.3)
   - Output: OrderServiceTest.java

   Detected context: Gradle/Java 21/Spring Boot 3.2/JUnit 5.10

   [Confirm] [Modify skills] [Change agent]"
```

**Pros:** Maximum transparency. User maintains control. Errors caught before
execution.

**Cons:** Adds a confirmation step. Can feel heavy for routine tasks.

**Best for:** Enterprise environments where governance requires traceability,
sensitive codebases, or when trust in auto-detection is still being established.

### 2.4 — Hybrid Routing (Recommended)

Combine strategies based on confidence:

```
HIGH CONFIDENCE    → Context-aware routing (auto-proceed)
MEDIUM CONFIDENCE  → User-confirmed routing (propose + confirm)
LOW CONFIDENCE     → Escalate to user for full specification
```

This gives the best of all worlds: fast for routine tasks, cautious for
ambiguous ones, transparent for sensitive ones.

---

## 3. Sequencing Patterns

Sequencing determines the order in which phases execute and how outputs
flow between agents.

### 3.1 — Linear Sequence (The Baseline)

The standard Plan → Implement → Review pipeline. This repository's workflow
mandate.

```
┌──────────┐     ┌─────────────┐     ┌──────────┐
│ Planner  │────→│ Implementer │────→│ Reviewer  │
└──────────┘     └─────────────┘     └──────────┘
     │                  ↑                  │
     │                  │    CHANGES       │
     │                  │    REQUIRED      │
     │                  └──────────────────┘
     │
     │ Task list output → becomes Implementer's input
```

**Phase contracts:**

| Phase | Input | Output | Transition Trigger |
|-------|-------|--------|-------------------|
| Plan | User prompt + Context Object | Ordered task list with dependencies | Task list validated (non-empty, scoped) |
| Implement | Task list + skills | Code artifacts | All tasks addressed |
| Review | Code artifacts + skills | APPROVED or CHANGES REQUIRED + feedback | Verdict rendered |

**Invariant:** The Orchestrator never modifies an agent's output. It passes
outputs between phases verbatim. The Orchestrator adds metadata (timestamps,
phase markers, confidence scores) but never edits content.

### 3.2 — Iterative Sequence (Review Loop)

The Review phase can reject output, triggering re-implementation. This must
be bounded to prevent infinite loops.

```
ITERATION POLICY:
  max_iterations: 2
  on_max_reached: escalate_to_user

ITERATION FLOW:
  Implementer (attempt 1)
    → Reviewer: CHANGES REQUIRED (feedback: "missing null check on email")
    → Implementer (attempt 2, with feedback attached)
    → Reviewer: APPROVED ✓

  OR:

  Implementer (attempt 1)
    → Reviewer: CHANGES REQUIRED
    → Implementer (attempt 2)
    → Reviewer: CHANGES REQUIRED (still)
    → Orchestrator: MAX ITERATIONS REACHED → Escalate to user
```

**Why bound iterations?** Unbounded loops waste tokens and can indicate a
deeper problem: the agent lacks a skill entry for the convention it's
violating, or the Reviewer's expectations conflict with the Implementer's
skill set. Escalation surfaces these structural issues rather than
burning resources on increasingly desperate attempts.

### 3.3 — Parallel Fan-Out

When the Planner produces independent work items, the Orchestrator can
dispatch them concurrently.

```
Planner output:
  Task 1: Create UserDTO (Java, no dependencies)
  Task 2: Create UserController (Java, depends on Task 1)
  Task 3: Create user-list component (TypeScript, independent)

Orchestrator analysis:
  Task 1 and Task 3 are independent → parallel
  Task 2 depends on Task 1 → sequential after Task 1

Execution:
  ┌─→ Implementer + java.skill (Task 1) ──→ Implementer + java.skill (Task 2)
  │                                          ↘
  │                                           Reviewer (all Java artifacts)
  │
  └─→ Implementer + typescript.skill (Task 3)
                                          ↘
                                           Reviewer (TypeScript artifacts)
```

**Dependency analysis** is the Orchestrator's job. The Planner annotates
dependencies in its output; the Orchestrator builds the execution graph.

**Review aggregation:** Independent outputs can be reviewed independently
(faster) or aggregated for holistic review (more thorough). The choice
depends on whether the artifacts will interact at runtime.

### 3.4 — Conditional Sequence

Skip phases based on task characteristics.

```
CONDITIONAL RULES:
  IF task is "bug fix":
    Plan → Debugger → Implementer → Reviewer
    (Debugger replaces Planner for root-cause analysis)

  IF task is "documentation only":
    Plan → Implementer → Reviewer
    (No Test Agent phase needed)

  IF task is "security-critical":
    Plan → Implementer → Security Agent → Reviewer
    (Security review added before general review)

  IF task is "test only":
    Test Agent → Reviewer
    (Skip Plan and Implement — tests are the deliverable)
```

Conditional rules are part of the Orchestrator's configuration, not the
agents' definitions. Agents are unaware of whether other phases exist.

### 3.5 — Multi-Pass Review

For high-stakes changes, the same code passes through multiple specialized
review agents:

```
Implementer output
  → Reviewer + rest-api.skill         (API design review)
  → Reviewer + security.skill         (security review)
  → Reviewer + observability.skill    (observability review)
  → Orchestrator aggregates findings
  → Implementer (address all findings in one pass)
  → Reviewer (final verification)
```

This is the **skill rotation** pattern from
[03 — Skill & Knowledge Design](03-skill-and-knowledge-design.md) §3.3,
applied at the orchestration level. Each review pass examines the code through
a different lens without overloading a single Reviewer with all skills
simultaneously.

---

## 4. Sub-Agent Delegation

### 4.1 — When an Agent Needs Help

Sometimes a Core Agent identifies work that falls outside its responsibility.
Rather than attempting it (and doing it poorly), it signals the Orchestrator
to delegate.

```
Security Agent (during audit):
  "I identified that OrderController.createOrder() is vulnerable to
   mass assignment. A fix requires adding a validated DTO with explicit
   field mapping.

   DELEGATION REQUEST:
     agent: implementer
     task: Create OrderCreateRequest DTO with validated fields
     skills: [java.skill, spring-boot.skill, rest-api.skill]
     context: {vulnerability description, affected controller}"

Orchestrator:
  → Routes to Implementer with the specified skills and context
  → Returns Implementer output to Security Agent for verification
```

### 4.2 — Delegation Protocol

The delegation protocol prevents agents from running unchecked sub-workflows:

```
DELEGATION RULES:
  1. Only Domain Agents may request delegation.
     (Core Agents complete their own work; they don't create subtasks.)

  2. Delegation requests go through the Orchestrator.
     (Agents never invoke other agents directly.)

  3. Delegated work follows the same phase rules.
     (Delegated implementation still passes through Review.)

  4. Maximum delegation depth: 1.
     (A delegated agent cannot further delegate. Prevents unbounded recursion.)

  5. The requesting agent receives the output and may accept or reject it.
     (Maintains accountability: the Domain Agent owns the final assessment.)
```

### 4.3 — Delegation vs. Phase Transition

| Characteristic | Delegation | Phase Transition |
|---------------|-----------|-----------------|
| **Trigger** | Agent discovers work outside its scope | Orchestrator's pre-planned sequence |
| **Initiator** | Agent requests | Orchestrator executes |
| **Planned?** | Discovered at runtime | Known before execution |
| **Scope** | Subset of the original task | Complete phase of the workflow |
| **Accountability** | Requesting agent verifies result | Orchestrator verifies via Reviewer |

---

## 5. Escalation Policies

Escalation is the Orchestrator's safety mechanism. It triggers when the
automated workflow hits conditions it cannot resolve autonomously.

### 5.1 — Escalation Triggers

| Trigger | Condition | Example |
|---------|-----------|---------|
| **Review loop exhaustion** | Reviewer rejects N times (default: 2) | Implementer cannot satisfy Reviewer's feedback |
| **Ambiguous context** | Context detection confidence below threshold | Both Maven and Gradle files detected |
| **Missing skill** | Required skill file does not exist | Project uses Kotlin but no `kotlin.skill.md` exists |
| **Scope creep** | Agent output exceeds planned scope by >50% | Implementer creates 3 new files when plan specified 1 |
| **Confidence drop** | Agent explicitly signals low confidence | "I'm not confident about this approach because..." |
| **Error condition** | Agent encounters an unrecoverable error | Build fails, test compilation error, dependency resolution failure |

### 5.2 — Escalation Actions

| Severity | Action | User Impact |
|----------|--------|------------|
| **Low** | Log and continue | User sees a note in the output: "FYI: I made an assumption about X" |
| **Medium** | Pause and ask | User must answer a question before work continues |
| **High** | Stop and report | User receives a report of what was attempted and what failed |

### 5.3 — Escalation Message Format

Escalation messages to the user should be structured, not free-form:

```
ESCALATION:
  trigger: review_loop_exhaustion
  phase: review (attempt 3 of 2)
  summary: "The Reviewer identified a missing null check on line 42 of
            OrderService.java. The Implementer addressed it twice, but
            the Reviewer found a new issue each time."
  attempted: [
    "Attempt 1: Added null check. Reviewer found missing @NotNull annotation.",
    "Attempt 2: Added @NotNull. Reviewer found validation not triggered on nested objects."
  ]
  options: [
    "I can try once more with additional context from you",
    "You can fix this manually and I'll re-review",
    "You can skip the review and accept the current code"
  ]
  recommendation: "This may indicate a missing convention in spring-boot.skill.md
                   for nested object validation. Consider adding it."
```

### 5.4 — Post-Escalation Feedback Loop

Every escalation is an opportunity to improve the system. After resolution:

1. **Categorize the root cause:** Skill gap? Agent reasoning error? Bad prompt?
   Missing knowledge base entry? Orchestration bug?
2. **Patch the appropriate layer:** Amend the skill file, improve the agent's
   constraints, enhance the prompt template, or fix the routing logic.
3. **Log the escalation** for aggregate analysis (which trigger fires most often?
   which skill has the most gaps?).

This connects directly to the feedback loop described in
[03 — Skill & Knowledge Design](03-skill-and-knowledge-design.md) §7.

---

## 6. The Orchestration State Machine

The Orchestrator's behavior can be modeled as a state machine for clarity
and auditability:

```
                    ┌─────────────┐
                    │   IDLE      │
                    └──────┬──────┘
                           │ Prompt received
                           ▼
                    ┌─────────────┐
                    │  DETECTING  │◄── Context detection pipeline
                    └──────┬──────┘
                           │ Context Object ready
                           ▼
                    ┌─────────────┐
               ┌───→│  CONFIRMING │  (only if confidence < threshold)
               │    └──────┬──────┘
               │           │ User confirms (or auto-approved)
               │           ▼
               │    ┌─────────────┐
               │    │  PLANNING   │◄── Planner agent
               │    └──────┬──────┘
               │           │ Task list produced
               │           ▼
               │    ┌─────────────┐
               │    │ IMPLEMENTING│◄── Implementer/Test Agent
               │    └──────┬──────┘
               │           │ Artifacts produced
               │           ▼
               │    ┌─────────────┐
               │    │  REVIEWING  │◄── Reviewer agent
               │    └──────┬──────┘
               │           │
               │     ┌─────┴─────┐
               │     │           │
               │  APPROVED    CHANGES
               │     │        REQUIRED
               │     ▼           │
               │  ┌──────┐      │ iteration < max?
               │  │ DONE │   ┌──┴──┐
               │  └──────┘   │ YES │──→ back to IMPLEMENTING
               │             └──┬──┘
               │                │ NO
               │                ▼
               │         ┌────────────┐
               └─────────│ ESCALATING │──→ user intervention
                         └────────────┘
```

**Every state transition is logged** with a timestamp, the input that triggered
it, and the output produced. This audit trail is essential for enterprise
governance (see [07 — Freshness & Bounded Autonomy](07-freshness-and-bounded-autonomy.md) §5).

---

## 7. Orchestration for the Test Agent: Complete Walk-Through

To make orchestration patterns concrete, here is a complete walk-through using
the Test Agent across the full lifecycle:

### Step 1 — Prompt Received

```
User: "Add comprehensive test coverage for the user-service module"
```

### Step 2 — Context Detection (DETECTING state)

```
Scan project root:
  ✓ build.gradle.kts → Gradle project
  ✓ Spring Boot dependencies → Spring Boot framework
  ✓ src/test/java/ exists → Java tests present
  ✓ JUnit 5 + AssertJ + Mockito in test dependencies
  ✓ Existing tests: UserControllerTest.java, UserServiceTest.java
  ✓ Testcontainers dependency present

Context Object:
  build_system: gradle
  language: java (21)
  framework: spring-boot (3.x)
  test_framework: junit5
  test_libraries: [assertj, mockito, testcontainers]
  existing_tests: [UserControllerTest, UserServiceTest]
  confidence: HIGH
```

### Step 3 — Routing Decision (auto-approved, HIGH confidence)

```
Routing:
  Phase 1: Planner → analyze existing coverage, identify gaps
  Phase 2: Test Agent + junit5.skill + java.skill + spring-boot.skill
  Phase 3: Reviewer + testing.skill + java.skill
```

### Step 4 — Planning (PLANNING state)

```
Planner output:
  1. Analyze existing UserServiceTest → identify untested methods/paths
  2. Analyze existing UserControllerTest → identify untested endpoints
  3. Generate missing UserService tests (gap: error path for updateUser)
  4. Generate missing UserController tests (gap: validation errors, 404 handling)
  5. Generate UserRepository tests (integration, using Testcontainers — no existing tests)
```

### Step 5 — Implementation (IMPLEMENTING state)

```
Orchestrator dispatches tasks 3, 4, 5 to Test Agent:

  Task 3: Test Agent + junit5.skill + java.skill + spring-boot.skill
    → Generates additional tests for UserServiceTest.java

  Task 4: Test Agent + junit5.skill + java.skill + spring-boot.skill
    → Generates additional tests for UserControllerTest.java

  Task 5: Test Agent + junit5.skill + java.skill + spring-boot.skill
    → Generates new UserRepositoryTest.java with Testcontainers

  (Tasks 3 and 4 are independent → parallel execution possible)
  (Task 5 is independent → can also run in parallel)
```

### Step 6 — Review (REVIEWING state)

```
Reviewer + testing.skill + java.skill:
  - Checks all generated tests against testing conventions
  - Verifies naming: method_state_expected ✓
  - Verifies isolation: no shared mutable state ✓
  - Verifies assertions: specific, not overly broad ✓
  - Finds issue: Task 5 uses @SpringBootTest where @DataJpaTest suffices
  - Verdict: CHANGES REQUIRED
    Feedback: "UserRepositoryTest should use @DataJpaTest instead of
              @SpringBootTest to keep tests focused and fast."
```

### Step 7 — Re-Implementation (IMPLEMENTING state, iteration 2)

```
Test Agent receives feedback → regenerates Task 5 with @DataJpaTest
```

### Step 8 — Re-Review (REVIEWING state, iteration 2)

```
Reviewer: APPROVED ✓
```

### Step 9 — Complete (DONE state)

```
Orchestrator compiles output:
  - 3 test files generated/updated
  - 18 new test methods total
  - 1 iteration required (Reviewer caught @SpringBootTest overuse)
  - Recommendation: Add "@DataJpaTest vs @SpringBootTest" guidance
    to spring-boot.skill.md to prevent recurrence
```

---

## 8. Enterprise Orchestration Considerations

### 8.1 — Multi-Repository Orchestration

In enterprise settings, a single task may span multiple repositories:

```
User: "Add a new 'preferred name' field to the user profile"

Orchestrator:
  Repo 1: user-service (Java) → Add field to User entity, DTO, migration
  Repo 2: user-api-gateway (TypeScript) → Update API types
  Repo 3: user-frontend (TypeScript) → Update profile form

  → Three independent sub-workflows, each with its own
    context detection, skill selection, and review cycle
  → Final cross-repo review: are the API contracts consistent?
```

### 8.2 — Workflow Versioning

Orchestration configurations should be versioned alongside the codebase:

```
.copilot/
  workflows/
    v1-standard.workflow.md       ← Plan → Implement → Review
    v2-security-enhanced.workflow.md  ← Plan → Implement → Security → Review
    v3-tdd.workflow.md            ← Plan → Test → Implement → Review
```

Teams can select the workflow version appropriate for their risk level.

### 8.3 — Observability for Orchestration

Every orchestration run should produce a structured log:

```
ORCHESTRATION LOG:
  run_id: abc-123
  timestamp: 2026-03-01T10:00:00Z
  prompt: "Add test coverage for user-service"
  context_confidence: HIGH
  workflow: v1-standard
  phases:
    - phase: plan
      agent: planner
      duration: 12s
      output_size: 342 tokens
    - phase: implement
      agent: test-agent
      skills: [junit5.skill@v1.5, java.skill@v2.1, spring-boot.skill@v1.3]
      duration: 45s
      output_size: 2100 tokens
      iterations: 2
    - phase: review
      agent: reviewer
      skills: [testing.skill@v3.0, java.skill@v2.1]
      duration: 18s
      verdict: APPROVED (on iteration 2)
  total_duration: 87s
  escalations: 0
  skill_gap_findings: ["@DataJpaTest vs @SpringBootTest not in skill"]
```

This log feeds into enterprise dashboards for quality monitoring, cost
tracking, and continuous improvement.

---

## Key Takeaways

1. **Three jobs** — Route, Sequence, Escalate — are the Orchestrator's entire
   scope. Everything else belongs to agents.

2. **Hybrid routing** (auto-proceed on high confidence, confirm on medium, ask
   on low) balances speed with governance.

3. **Five sequencing patterns** (linear, iterative, parallel fan-out, conditional,
   multi-pass review) handle most enterprise workflow shapes.

4. **Bounded iteration** (max 2 review loops by default) prevents infinite loops
   and surfaces structural skill gaps rather than burning tokens.

5. **Sub-agent delegation** follows a strict protocol: only Domain Agents request
   it, maximum depth of 1, goes through the Orchestrator, and follows the same
   phase rules.

6. **Every escalation is a feedback opportunity.** Categorize the root cause and
   patch the appropriate layer.

7. **The state machine model** makes orchestration behavior auditable and
   debuggable — essential for enterprise governance.

---

*Next: [06 — Context Detection & Adaptation](06-context-detection-and-adaptation.md) — The environment sensing pipeline, signal taxonomy, and the Context Object specification.*
