# 04 — Prompt Design: Parameterized Task Triggers

> **Purpose:** Define the prompt's architectural role as a parameterized trigger (not a teaching mechanism), explain prompt layering and composition, establish the template system, and address the core design question of explicit vs. auto-detected specialization.

---

## 1. The Prompt's Singular Role

A prompt has one job: **specify what to do, with enough context for the
orchestration layer to route correctly and the agent to execute meaningfully.**

A prompt is NOT:
- A tutorial for the agent (that's the agent's identity)
- A conventions reference (that's a skill file)
- A project description (that's a knowledge base)
- A workflow specification (that's orchestration logic)

If you find yourself writing more than two paragraphs in a prompt, you're
likely embedding behavior that belongs in another layer.

---

## 2. Prompt Anatomy

Every effective prompt, whether a reusable template or a one-shot user message,
contains four sections (some may be implicit):

```
┌─────────────────────────────────────────────────┐
│ TASK                                            │
│ What needs to happen. Verb-first, scope-bounded.│
│ "Generate unit tests for the OrderService class"│
├─────────────────────────────────────────────────┤
│ CONTEXT                                         │
│ Pointers to relevant artifacts. Files, specs,   │
│ endpoints, or knowledge base entries.            │
│ "See src/main/java/.../OrderService.java"       │
├─────────────────────────────────────────────────┤
│ CONSTRAINTS                                     │
│ Task-specific requirements beyond standard       │
│ conventions. Acceptance criteria.                │
│ "Cover all error paths. Include edge cases for  │
│  null inputs and empty collections."             │
├─────────────────────────────────────────────────┤
│ REFERENCES                                      │
│ Skill files or knowledge bases to consult.       │
│ "Follow conventions in junit5.skill.md and       │
│  java.skill.md"                                  │
└─────────────────────────────────────────────────┘
```

### Section Responsibilities

| Section | Required? | Content Type | Anti-Pattern |
|---------|-----------|-------------|-------------|
| **TASK** | Always | Verb + scope + deliverable | Describing *how* to do it instead of *what* |
| **CONTEXT** | Usually | File paths, spec references, prior output | Inlining code instead of pointing to files |
| **CONSTRAINTS** | When needed | Task-specific acceptance criteria | Duplicating skill conventions |
| **REFERENCES** | When explicit | Skill file paths, knowledge base paths | Copying skill content into the prompt |

---

## 3. Prompt Layering: The Four-Layer Composition

A prompt's effective context is not just what the user types. It's a composition
of four layers, each contributing different types of instructions:

```
┌─────────────────────────────────────────────────────────┐
│ Layer 4: USER MESSAGE (immediate)                       │
│ "Generate tests for UserService"                        │
│ Dynamic · Task-specific · Minimal                       │
├─────────────────────────────────────────────────────────┤
│ Layer 3: PROMPT TEMPLATE (task-type)                    │
│ tdd-cycle.prompt.md, rest-endpoint.prompt.md            │
│ Reusable · Parameterized · Defines output shape         │
├─────────────────────────────────────────────────────────┤
│ Layer 2: SKILL FILES (attached at runtime)              │
│ junit5.skill.md + java.skill.md                         │
│ Convention-rich · Composable · Framework-specific        │
├─────────────────────────────────────────────────────────┤
│ Layer 1: COPILOT-INSTRUCTIONS.MD (always-on)            │
│ .github/copilot-instructions.md                         │
│ Global · Cross-cutting · Baseline quality rules          │
└─────────────────────────────────────────────────────────┘
```

### How Layers Compose

The agent receives all four layers simultaneously. Conceptually, they compose
by **specificity override**:

- **Layer 1** sets the baseline (e.g., "no field injection, structured logging")
- **Layer 2** adds domain-specific conventions (e.g., "use `@ExtendWith`")
- **Layer 3** shapes the task pattern (e.g., "first write the test, then implement to pass it")
- **Layer 4** specifies the immediate task (e.g., "focus on `OrderService.createOrder()`")

When layers conflict, **higher layers override lower layers** for their scope:
- A prompt constraint ("use only Mockito stubs, no real databases") overrides a
  skill suggestion ("use Testcontainers for repository tests") for this specific task.
- But the prompt cannot override Layer 1 global rules ("no hardcoded secrets") —
  those are non-negotiable.

### Layer Ownership

| Layer | Who Authors | When It Changes | Scope |
|-------|------------|----------------|-------|
| **1 — copilot-instructions.md** | Engineering standards team | Quarterly | All interactions |
| **2 — Skill files** | Platform team + project teams | Per convention evolution | When attached by orchestration |
| **3 — Prompt templates** | Workflow designers | Per workflow evolution | When selected by user/orchestration |
| **4 — User message** | End user or automation | Every interaction | This interaction only |

---

## 4. Prompt Templates vs. One-Shot Prompts

### 4.1 — Prompt Templates

Prompt templates are reusable patterns stored in `.copilot/prompt-library/`.
They encode **task patterns** — not domain knowledge.

A template uses placeholders for variable parts:

```
TASK: Generate {{test_type}} tests for {{target_class}}.

CONTEXT:
  - Source: {{source_file_path}}
  - Existing tests: {{existing_test_path}} (if any)
  - Related specs: {{api_spec_path}} (if applicable)

CONSTRAINTS:
  - Achieve minimum {{coverage_target}} coverage for public methods.
  - Cover: happy path, error paths, edge cases, boundary conditions.
  - {{additional_constraints}}

REFERENCES:
  - {{testing_skill}} (testing framework conventions)
  - {{language_skill}} (language conventions)
  - {{framework_skill}} (framework conventions, if applicable)

OUTPUT:
  - One test file per source class.
  - Follow naming conventions from referenced skills.
  - Include necessary imports and class-level setup.
```

### 4.2 — One-Shot Prompts

One-shot prompts are user-authored per task. They should follow the same
anatomy (TASK, CONTEXT, CONSTRAINTS, REFERENCES) but need not use placeholders.

```
Generate integration tests for the POST /api/v1/users endpoint.
Focus on validation errors (missing email, duplicate email) and
the happy path. Follow junit5.skill.md conventions. The endpoint
is defined in UserController.java and delegates to UserService.
```

### 4.3 — When to Templatize

Templatize when you see the same prompt structure repeated across tasks:

| Signal | Action |
|--------|--------|
| "I keep writing the same prompt with different class names" | Templatize with `{{target_class}}` |
| "Every REST endpoint prompt has the same structure" | Create `rest-endpoint.prompt.md` |
| "Our TDD workflow always follows the same steps" | Create `tdd-cycle.prompt.md` |
| "This prompt is unique to this one task" | Keep as one-shot |

### 4.4 — Template Design Principles

**Principle 1 — Templates define task shape, not domain knowledge.**

```
Good: "Generate {{test_type}} tests for {{target_class}}"
Bad:  "Generate JUnit 5 tests using @ExtendWith and AssertJ"
      ↑ This is skill content leaking into the template
```

**Principle 2 — Templates reference skills, not inline them.**

```
Good: "REFERENCES: {{testing_skill}}"
Bad:  "Use @DisplayName for all test methods, name them as..."
      ↑ This is a convention that belongs in junit5.skill.md
```

**Principle 3 — Templates have sensible defaults for optional parameters.**

```
coverage_target: 80% (default, overridable)
test_type: unit (default, overridable to integration, e2e, bdd)
additional_constraints: (empty default)
```

**Principle 4 — Templates include output format specification.**

The output section tells the agent what artifact(s) to produce and in what
structure. Without this, the agent guesses — and guesses inconsistently across
runs.

---

## 5. The Injection Strategy: Explicit vs. Auto-Detected Specialization

This addresses the core design question: **"Should the agent auto-detect
environment and ask for confirmation, or should specialization always be
injected explicitly via prompt?"**

### 5.1 — The Three Strategies

| Strategy | How It Works | Pros | Cons |
|----------|-------------|------|------|
| **Fully Explicit** | User specifies everything in the prompt: which agent, which skills, which framework | Maximum predictability; no false detections | Burden on user; requires deep system knowledge |
| **Fully Autonomous** | Agent auto-detects everything from project context and proceeds | Minimum user effort; seamless experience | Unpredictable; silent wrong decisions; enterprise risk |
| **Detect-and-Confirm** | System auto-detects, proposes skill selection, user confirms or overrides | Balanced: low effort + high predictability | Adds a confirmation step; slightly slower |

### 5.2 — Recommended: Detect-and-Confirm with Confidence Tiers

The recommended enterprise approach combines auto-detection with
**confidence-based confirmation**:

```
HIGH CONFIDENCE (auto-proceed):
  - Single build file present (build.gradle.kts → Gradle + Java)
  - Framework explicitly declared in dependencies
  - Test framework indicated by existing test files

MEDIUM CONFIDENCE (confirm):
  - Multiple build systems detected (both pom.xml and build.gradle.kts)
  - Framework version ambiguous
  - Mixed technology stack

LOW CONFIDENCE (always ask):
  - No build file detected (new project?)
  - Conflicting signals (Java source files but npm dependencies)
  - Task type unclear from prompt
```

### 5.3 — How This Works in Practice

```
User: "Generate tests for UserService"

Orchestrator (auto-detection):
  ✓ Found: build.gradle.kts
  ✓ Found: Spring Boot starter-test dependency
  ✓ Found: Existing JUnit 5 tests in src/test/java/
  ✓ Confidence: HIGH

  → Auto-selects: Test Agent + junit5.skill + java.skill + spring-boot.skill
  → Proceeds without confirmation.

---

User: "Generate tests for the payment module"

Orchestrator (auto-detection):
  ✓ Found: build.gradle.kts (Java/Gradle)
  ✓ Found: package.json (Node/TypeScript)
  ? Both backends detected. Which is the payment module?
  ? Confidence: MEDIUM

  → Presents: "I detected both Java (Gradle) and TypeScript (npm) in this
     project. The payment module — is it the Java backend or the TypeScript
     service? [Java] [TypeScript]"
  → User selects → proceeds.
```

### 5.4 — The Context Object

Auto-detected context is serialized into a structured **Context Object** that
flows through the orchestration chain. This ensures every layer works with the
same understanding:

```
DETECTED CONTEXT:
  build_system: gradle
  language: java
  java_version: 21
  framework: spring-boot
  framework_version: 3.x
  test_framework: junit5
  test_libraries: [assertj, mockito, testcontainers]
  existing_tests: true
  existing_test_count: 12
  ci_system: github-actions

SELECTED SKILLS:
  - java.skill.md (v2.1)
  - spring-boot.skill.md (v1.3)
  - junit5.skill.md (v1.5)
  - testing.skill.md (v3.0)

CONFIDENCE: HIGH
CONFIRMATION: auto-approved (all signals high-confidence)
```

This Context Object becomes part of the prompt that reaches the agent. The
agent doesn't need to re-detect the environment — it's already been done.

See [06 — Context Detection](06-context-detection-and-adaptation.md) for the
full detection pipeline design.

---

## 6. Prompt Anti-Patterns

### 6.1 — The Teaching Prompt

```
BAD: "You are a test expert. First, understand the class structure.
     Then identify all public methods. Then for each method, think
     about what could go wrong. Then write a test for each scenario.
     Make sure to use descriptive names..."

WHY: This is agent reasoning instruction. It belongs in the Test Agent's
     identity file, not in the prompt.

FIX: "Generate unit tests for UserService."
     (The Test Agent already knows how to reason about tests.)
```

### 6.2 — The Convention-Stuffed Prompt

```
BAD: "Generate tests. Use @ExtendWith(MockitoExtension.class) for
     mocking. Use AssertJ assertions. Name tests as method_state_expected.
     Add @DisplayName. Use constructor injection. Don't use field injection..."

WHY: These are all skill conventions. They belong in junit5.skill.md and
     java.skill.md.

FIX: "Generate tests for UserService. Follow conventions in
     junit5.skill.md and java.skill.md."
```

### 6.3 — The Ambiguous Task

```
BAD: "Make the user service better."

WHY: No specific task, no scope, no constraints. The agent will
     hallucinate a task.

FIX: "Add input validation to the POST /api/v1/users endpoint.
     Validate: email format, name length (1-100), no null fields.
     Return 422 with structured error DTO for validation failures."
```

### 6.4 — The Context-Free Prompt

```
BAD: "Write tests."

WHY: No target class, no file paths, no indication of what to test.
     The agent will either ask (wasting a round-trip) or guess wrong.

FIX: "Generate unit tests for OrderService (src/main/java/.../OrderService.java).
     Focus on createOrder and cancelOrder methods."
```

### 6.5 — The Micro-Managing Prompt

```
BAD: "Generate a test class with exactly 8 test methods. The first
     test should test createUser with valid input. The second should
     test createUser with null email. The third should..."

WHY: Over-specifying removes the agent's ability to identify gaps you
     didn't think of. You're doing the agent's job in the prompt.

FIX: "Generate unit tests for UserService. Cover all public methods
     including happy paths and error paths. I expect comprehensive
     edge case coverage."
```

---

## 7. Prompt Template Library: Organizational Design

### 7.1 — Template Categories

```
.copilot/prompt-library/
├── generation/                      # Create new artifacts
│   ├── rest-endpoint.prompt.md      # Generate a complete REST endpoint
│   ├── db-migration.prompt.md       # Generate a database migration
│   └── dto-mapping.prompt.md        # Generate DTO ↔ Entity mapping
│
├── testing/                         # Generate tests
│   ├── tdd-cycle.prompt.md          # Red-green-refactor cycle
│   ├── unit-tests.prompt.md         # Unit tests for a class
│   ├── integration-tests.prompt.md  # Integration test for an endpoint
│   └── bdd-scenarios.prompt.md      # BDD feature files
│
├── review/                          # Review existing code
│   ├── code-review.prompt.md        # General code review
│   ├── security-review.prompt.md    # OWASP-focused security review
│   └── api-review.prompt.md         # REST API design review
│
├── maintenance/                     # Fix or improve existing code
│   ├── bug-fix.prompt.md            # Diagnose and fix a bug
│   ├── refactor.prompt.md           # Refactor for quality
│   └── dependency-upgrade.prompt.md # Upgrade a dependency safely
│
└── documentation/                   # Generate documentation
    ├── api-docs.prompt.md           # OpenAPI spec generation
    └── adr.prompt.md                # Architecture Decision Record
```

### 7.2 — Template Naming Convention

```
{action}-{subject}.prompt.md

Examples:
  unit-tests.prompt.md
  rest-endpoint.prompt.md
  security-review.prompt.md
  bug-fix.prompt.md
```

### 7.3 — Template Metadata

Every template includes a metadata header for discoverability and orchestration:

```
---
name: Unit Test Generation
category: testing
agents: [test-agent]
required_skills: [testing.skill.md]
optional_skills: [junit5.skill.md, vitest.skill.md, cypress.skill.md]
parameters:
  - target_class (required): The class to generate tests for
  - coverage_target (optional, default: 80%): Minimum coverage goal
  - test_type (optional, default: unit): unit | integration | e2e
triggers:
  - "generate tests for"
  - "write unit tests"
  - "add test coverage"
---
```

The `triggers` field allows the orchestration layer to auto-select templates
based on natural language user messages, reducing the need for users to know
template names.

---

## 8. The Prompt-Orchestration Handshake

The boundary between prompts and orchestration deserves explicit definition
because it's where most confusion occurs:

| Responsibility | Prompt | Orchestration |
|---------------|--------|---------------|
| **Task specification** | ✓ Defines what to do | ✗ Does not interpret task intent |
| **Template selection** | ✗ User picks or natural language triggers | ✓ Maps triggers to templates |
| **Skill selection** | ✗ May suggest via REFERENCES | ✓ Final decision based on context detection |
| **Agent selection** | ✗ Never specifies which agent | ✓ Selects agent based on task type |
| **Parameter resolution** | ✓ Provides explicit values | ✓ Fills defaults for unspecified parameters |
| **Confirmation** | ✗ | ✓ Confirms with user when confidence is medium/low |

The prompt says *what*. Orchestration decides *how*, *who*, and *when*.

---

## Key Takeaways

1. **Prompts parameterize tasks** — they don't teach agents, encode conventions,
   or manage workflows. One job: specify what to do with enough context to proceed.

2. **Four-layer composition** — copilot-instructions (always-on) + skills (attached)
   + template (task-pattern) + user message (immediate) — composes into the agent's
   full context. Higher layers override lower for their scope.

3. **Detect-and-Confirm with confidence tiers** is the recommended enterprise
   strategy for environment-driven specialization: auto-proceed on high confidence,
   confirm on medium, always ask on low.

4. **The Context Object** serializes detected environment into a structured format
   that flows through the entire orchestration chain, eliminating redundant detection.

5. **Five anti-patterns** (teaching, convention-stuffing, ambiguity, context-free,
   micro-managing) cover the most common prompt design mistakes.

6. **Template libraries** use metadata headers and trigger keywords for
   discoverability, enabling the orchestration layer to auto-select appropriate
   templates from natural language user input.

---

*Next: [05 — Orchestration Patterns](05-orchestration-patterns.md) — Routing strategies, phase management, sub-agent delegation, and escalation policies.*
