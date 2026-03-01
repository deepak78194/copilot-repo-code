# 02 — Agent Taxonomy: Generic, Specialized, and Composite Agents

> **Purpose:** Define the three tiers of agents, establish when generic reasoning suffices versus when specialization is warranted, and provide the cognitive load threshold as the decision criterion for splitting agents.

---

## 1. The Spectrum of Agent Specialization

Agents are not all alike. They sit on a spectrum from generic (reusable across
many contexts) to specialized (deeply adapted to one context). The key insight
is that **most enterprise workflows need far fewer agents than they think**, and
the right decomposition is along *reasoning strategy* — not along *domains* or
*frameworks*.

```
GENERIC ◄──────────────────────────────────────────────► SPECIALIZED

Core Agents           Domain Agents          Composite Agents
(reasoning is         (reasoning carries      (no domain reasoning;
 universal)            domain heuristics)      pure coordination)

Planner               Security Agent          Orchestrator
Implementer           Performance Agent
Reviewer              Accessibility Agent
Test Agent
Debugger
```

---

## 2. The Three Agent Tiers

### Tier 1 — Core Agents (Generic Reasoning)

Core agents understand *what good looks like* for a broad category of work.
Their reasoning is language-agnostic, framework-agnostic, and project-agnostic.

| Agent | Reasoning Domain | Example Heuristics |
|-------|-----------------|-------------------|
| **Planner** | Task decomposition | Break complex work into ordered, independently testable steps. Identify dependencies. Estimate scope. |
| **Implementer** | Code production | Write minimal code that satisfies requirements. Follow conventions from loaded skills. Prefer clarity over cleverness. |
| **Reviewer** | Correctness verification | Check for logic errors, convention violations, missing edge cases. Verify against acceptance criteria. |
| **Test Agent** | Test quality | Reason about coverage completeness, test isolation, assertion specificity, naming clarity, edge case coverage. |
| **Debugger** | Root-cause analysis | Reproduce first, hypothesize second, verify third. Isolate variables systematically. Prefer minimal fixes. |

**Key property:** A Core Agent's definition file contains **zero** framework-specific
instructions. All framework knowledge comes from skills attached at runtime.

**Why this matters:** Core agents are your most reusable assets. A well-defined
Reviewer agent works for Java, TypeScript, Python, Go — any language — as long as
appropriate skills are attached. You invest once in defining good reviewing heuristics
and reuse them across your entire enterprise.

#### Worked Example: The Test Agent

The Test Agent is the canonical example of a Core Agent. Its identity contains:

```
ROLE: Generate and evaluate tests for correctness, completeness, and maintainability.

REASONING APPROACH:
  1. Analyze the code-under-test to identify its public contract
     (inputs, outputs, side effects, error conditions).
  2. Enumerate test scenarios: happy path, edge cases, error paths,
     boundary conditions, state transitions.
  3. For each scenario, determine the appropriate test isolation level
     (unit, integration, end-to-end).
  4. Apply the testing pyramid: prefer unit tests; use integration tests
     for collaboration boundaries; reserve E2E for critical user flows.
  5. Verify test naming communicates intent:
     {method}_{state}_{expected behavior}.

QUALITY CRITERIA:
  - Every public method has at least one test.
  - Tests are isolated: no test depends on another's execution.
  - Assertions are specific: test exactly what matters, not more.
  - No logic in tests: no if/else, no loops, no complex setup.

CONSTRAINTS:
  - Never mock what you don't own (mock interfaces, not third-party classes).
  - Never assert on toString() representations.
  - Never use Thread.sleep() for synchronization.
```

Notice: **nothing in this definition mentions JUnit, Vitest, Cypress, Cucumber,
Mockito, or any specific framework.** These are universal testing principles. The
Test Agent thinks about testing *conceptually*. Framework-specific patterns come
from skills.

When the Test Agent receives `junit5.skill.md`, it maps its concepts to JUnit 5:
- "test isolation" → `@ExtendWith(MockitoExtension.class)`, `@MockBean`
- "test naming" → `@DisplayName("should return 404 when user not found")`
- "assertion specificity" → AssertJ's fluent assertions

When it receives `cypress.skill.md` instead, the same concepts map differently:
- "test isolation" → `cy.intercept()` for API mocking, `beforeEach` for state reset
- "test naming" → `describe`/`it` blocks with behavior descriptions
- "assertion specificity" → Cypress assertions, `should('have.text', ...)`

**Same agent. Same reasoning. Different output. That is the power of the Core tier.**

---

### Tier 2 — Domain Agents (Specialized Reasoning)

Domain agents carry reasoning heuristics that are specific to a professional
domain. You cannot encode their expertise in skill files because it's not about
*conventions* — it's about *how to think* about a specific class of problems.

| Agent | Why It Needs Specialized Reasoning |
|-------|------------------------------------|
| **Security Agent** | Must reason about threat models, attack surfaces, trust boundaries, privilege escalation chains — not just follow a checklist |
| **Performance Agent** | Must reason about algorithmic complexity, memory allocation patterns, I/O bottlenecks, concurrency hazards |
| **Accessibility Agent** | Must reason about WCAG conformance levels, assistive technology interaction models, semantic HTML structure |
| **Data Migration Agent** | Must reason about backward compatibility, rollback strategies, data integrity constraints across schema versions |

**Key property:** Domain agents carry *domain-specific reasoning heuristics* in
their identity file. They still receive skills for framework-specific conventions,
but their reasoning approach is fundamentally different from a Core agent.

#### The Litmus Test: Reasoning vs. Knowledge

Ask yourself: **"Can this expertise be fully captured as a list of conventions
and patterns (do X, don't do Y)?"**

- **YES** → It's a skill. Attach it to a Core agent.
- **NO, it requires judgment and domain-specific reasoning chains** → It's a
  Domain agent.

**Example — Security:**

A skill file can list: "Parameterize all SQL queries. Validate input at the
boundary. Never log PII." These are *conventions*.

But a Security Agent also reasons: "This endpoint accepts user-controlled input
that flows through three function calls before reaching a database query. Even
though each function validates its own input, the composition of validations
leaves a gap for injection between step 2 and step 3." This is *reasoning* that
requires understanding threat modeling, data flow analysis, and trust boundaries.
No skill file can capture this — it must be in the agent's identity.

---

### Tier 3 — Composite Agents (Pure Coordination)

Composite agents do not produce domain work. They coordinate other agents.
The Orchestrator is the canonical (and often only) Composite agent.

**Key property:** A Composite agent's definition file contains **zero** domain
heuristics. It knows about *lifecycle management*, not about code quality, testing
patterns, or security threats.

```
ROLE: Coordinate the execution of a multi-phase workflow.

REASONING APPROACH:
  1. Receive a task from the prompt layer.
  2. Invoke the Planner to decompose the task.
  3. For each planned item, select the appropriate agent and skills
     based on detected project context.
  4. Route work to selected agents, collect their outputs.
  5. Route outputs to the Reviewer for verification.
  6. If Reviewer returns CHANGES REQUIRED, re-route to the Implementer
     with the Reviewer's feedback. Max 2 retries before escalation.
  7. If Reviewer returns APPROVED, compile final output.

CONSTRAINTS:
  - Never produce domain work (code, tests, reviews) directly.
  - Never override an agent's output based on domain judgment.
  - Escalate to the user when: confidence is low, retries exhausted,
    scope exceeds plan, or ambiguous context detected.
```

**Why the Orchestrator should not be a "super-agent":** The temptation is to make
the Orchestrator smart — give it domain knowledge so it can make better routing
decisions. Resist this. An Orchestrator that understands Java vs. TypeScript
tradeoffs is an Orchestrator that must be updated when you add Kotlin support.
Keep it dumb about domains but smart about coordination.

---

## 3. The Cognitive Load Threshold: When to Split

The most common design question is: **"Should this be one agent with multiple
skills, or should I split it into multiple agents?"**

The answer follows the **Cognitive Load Threshold (CLT)** principle:

> **Split an agent when its instruction set exceeds what can be reliably followed
> in a single reasoning pass.**

### 3.1 — Measuring Cognitive Load

Cognitive load for an LLM-based agent correlates with:

| Factor | High Load | Low Load |
|--------|-----------|----------|
| **Instruction volume** | Agent + 4 skills = 3000+ words of instructions | Agent + 1 skill = 500 words |
| **Conflicting priorities** | "Maximize coverage" + "Minimize test count" | Single clear objective |
| **Context switching** | "Generate code, then review it, then write tests" | "Generate code" (one phase) |
| **Decision branching** | "If Java, do X; if TypeScript, do Y; if Kotlin, do Z" | Skills handle branching |
| **Output complexity** | Multi-file output with interdependencies | Single coherent artifact |

### 3.2 — Split Decision Framework

```
Does the agent carry a single, coherent reasoning strategy?
  ├── NO → SPLIT into agents with distinct reasoning strategies
  │         (e.g., don't combine Implementing and Reviewing)
  └── YES ↓

Does the agent need to produce one coherent artifact per invocation?
  ├── NO → SPLIT by artifact type
  │         (e.g., separate agent for code vs. tests if they're independent)
  └── YES ↓

Would attaching all necessary skills exceed ~1500 words of instructions?
  ├── YES → Consider splitting skills into skill groups, each handled
  │          by a separate invocation of the same agent
  └── NO ↓

KEEP as one agent with multiple skills.
```

### 3.3 — When NOT to Split

**Don't split by framework.** "JUnit5TestAgent" and "VitestTestAgent" are the
same agent with different skills. Splitting by framework creates maintenance
overhead without reducing cognitive load — each agent has roughly the same
instruction set, just with different framework conventions.

**Don't split by language.** A Reviewer agent that understands "correctness, edge
cases, convention compliance" works for any language. Language-specific conventions
come from skill files.

**Don't split by task complexity.** A "SimpleImplementer" and "ComplexImplementer"
is a sign that the Planner isn't decomposing tasks well enough. Fix the Planner's
output, not the Implementer's identity.

### 3.4 — When to Split

**Split when reasoning strategies conflict:**
- Implementing and Reviewing represent opposing cognitive modes (creation vs.
  criticism). One agent doing both will compromise on both.
- Security auditing and feature implementation have inherently tension-filled
  priorities. Split them.

**Split when domain expertise is deep and non-transferable:**
- A Performance Agent reasons about algorithmic complexity, memory patterns,
  and I/O characteristics. These heuristics are deep enough to warrant a
  dedicated reasoning context.

**Split when output artifacts are independently valuable:**
- If code and documentation can be produced independently, separate agents avoid
  the cognitive overhead of managing both in one pass.

---

## 4. Agent Composition Patterns

### 4.1 — Sequential Composition

The standard workflow pattern. One agent's output becomes the next agent's input.

```
Planner → Implementer → Reviewer
                ↑           │
                └── CHANGES ─┘
```

Each agent has no knowledge of the others. The Orchestrator manages handoffs.
This is the simplest and most predictable pattern.

### 4.2 — Parallel Fan-Out

Independent work items are processed concurrently by the same agent type with
different skill sets.

```
                 ┌─→ Implementer + java.skill (backend module)
Planner output ──┤
                 └─→ Implementer + typescript.skill (frontend module)
```

The Orchestrator recombines results. Agents are unaware of each other.
Useful when a plan has independent work items in different technology stacks.

### 4.3 — Agent Chaining with Skill Rotation

The same agent is invoked multiple times with different skills for different
aspects of the same artifact.

```
Reviewer + rest-api.skill → Reviewer + security.skill → Reviewer + observability.skill
```

Each pass reviews the same code from a different perspective. The Orchestrator
aggregates findings across passes. This avoids giving one Reviewer agent three
heavy skill files simultaneously.

### 4.4 — Hierarchical Delegation

A Domain Agent delegates subtasks to Core Agents.

```
Security Agent (reasoning about threat model)
  ├── delegates to Implementer: "Add input validation to OrderController"
  └── delegates to Test Agent: "Generate tests for the new validation logic"
```

The Security Agent provides domain reasoning; the Core Agents provide execution.
This pattern is appropriate when a Domain Agent identifies required changes but
shouldn't produce the code itself (separation of concerns).

---

## 5. Enterprise Scaling: How Many Agents?

### The Recommended Baseline (6 agents)

| Agent | Tier | Quantity Justification |
|-------|------|----------------------|
| Orchestrator | Composite | One per workflow type |
| Planner | Core | One, reused across all workflows |
| Implementer | Core | One, specialized via skills |
| Reviewer | Core | One, specialized via skills per review pass |
| Test Agent | Core | One, specialized via skills per framework |
| Debugger | Core | One, invoked on failure |

### When to Add Beyond the Baseline

- **Security Agent (Domain):** When security is a first-class concern with
  dedicated review requirements (most enterprises).
- **Performance Agent (Domain):** When performance SLAs are explicit and
  measurable.
- **Documentation Agent (Core):** When API documentation is a formal deliverable
  (OpenAPI specs, Architecture Decision Records).
- **Migration Agent (Domain):** When database schema changes require specialized
  backward-compatibility reasoning.

### When NOT to Add

- **"FastImplementer" and "CarefulImplementer"** — This is orchestration concern
  about how much review to apply, not an agent concern.
- **"JavaAgent" and "TypeScriptAgent"** — This is a skill concern, not an agent
  concern.
- **"FrontendAgent" and "BackendAgent"** — Usually a skill concern. Split only if
  the reasoning strategy genuinely differs (e.g., if frontend work requires UI/UX
  heuristics that are Domain-level reasoning).

---

## 6. The Test Agent Through the Full Lifecycle

To make this concrete, here is how the Test Agent participates in a complete
enterprise workflow — from prompt to output — across three different scenarios:

### Scenario A: Java Unit Tests

```
User prompt: "Generate tests for UserService"
  │
  ▼
Orchestrator:
  - Detects: build.gradle.kts, Spring Boot dependencies
  - Selects: Test Agent + junit5.skill + java.skill + spring-boot.skill
  │
  ▼
Test Agent:
  - Reads UserService (from knowledge/codebase or direct access)
  - Applies REASONING: identifies 4 public methods, 3 error paths
  - Applies junit5.skill: @ExtendWith, @Mock, @DisplayName patterns
  - Applies java.skill: var, records, Optional conventions
  - Produces: UserServiceTest.java (12 test methods)
  │
  ▼
Reviewer + testing.skill + java.skill:
  - Checks coverage, naming, isolation, assertion quality
  - Returns: APPROVED (or CHANGES REQUIRED with specific feedback)
```

### Scenario B: Cypress E2E Tests

```
User prompt: "Generate E2E tests for the login flow"
  │
  ▼
Orchestrator:
  - Detects: cypress.config.ts, package.json with cypress dependency
  - Selects: Test Agent + cypress.skill + typescript.skill
  │
  ▼
Test Agent:
  - Reads login page components (from knowledge/codebase)
  - Applies REASONING: identifies user flows, error states, edge cases
  - Applies cypress.skill: cy.visit, cy.intercept, cy.get patterns
  - Applies typescript.skill: strict mode, ES module syntax
  - Produces: login.cy.ts (6 test cases)
  │
  ▼
Reviewer + testing.skill + typescript.skill:
  - Checks E2E-specific concerns: flakiness, selector stability, wait strategies
  - Returns: APPROVED
```

### Scenario C: Cucumber BDD Tests

```
User prompt: "Write BDD scenarios for the order placement workflow"
  │
  ▼
Orchestrator:
  - Detects: .feature files in src/test/resources, cucumber dependency
  - Selects: Test Agent + cucumber.skill + java.skill
  │
  ▼
Test Agent:
  - Reads OrderService contract
  - Applies REASONING: identifies business scenarios, acceptance criteria
  - Applies cucumber.skill: Given/When/Then structure, step definition patterns
  - Produces: order_placement.feature + OrderPlacementSteps.java
  │
  ▼
Reviewer + testing.skill + cucumber.skill:
  - Checks scenario clarity, step reusability, business language alignment
  - Returns: APPROVED
```

**Observation:** The Test Agent's *reasoning* was identical across all three
scenarios. Only the skills changed. The Orchestrator made the routing decision.
The prompt specified the task. Every layer did its job — nothing more.

---

## Key Takeaways

1. **Three tiers** — Core (generic reasoning), Domain (specialized reasoning),
   Composite (pure coordination) — provide a clean taxonomy for agent design.

2. **Core agents are your most reusable asset.** Invest in their reasoning
   heuristics. Specialize via skills, not via new agents.

3. **The Cognitive Load Threshold** determines when to split: conflicting
   reasoning strategies, excessive instruction volume, or incompatible output
   types. **Never split by framework or language.**

4. **Four composition patterns** (sequential, parallel fan-out, skill rotation,
   hierarchical delegation) handle most enterprise workflows.

5. **Start with 6 agents.** Add Domain agents only when deep, non-transferable
   reasoning expertise is required.

---

*Next: [03 — Skill & Knowledge Design](03-skill-and-knowledge-design.md) — Skill contracts, composition rules, granularity decisions, and the feedback loop.*
