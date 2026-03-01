# 03 — Skill & Knowledge Design: Composable Knowledge Modules

> **Purpose:** Define the skill contract (what a skill file must and must not contain), establish composition rules, address granularity decisions, distinguish skills from knowledge bases, and design the feedback loop that keeps skills accurate over time.

---

## 1. Skills Are the Secret Weapon

In the layered architecture described in [00 — Overview](00-overview.md), skills
are the layer that makes everything else work. Agents without skills are
generalists who produce generic output. Agents with skills produce output that
matches your organization's exact conventions, patterns, and quality standards.

The experiment data from this repository validates this directly: when a convention
was **present** in the skill file, agents followed it consistently. When it was
**missing**, agents diverged — the Reviewer had to catch the gap. The completeness
of skill files is the single strongest predictor of agent output quality.

This means **designing great skill files is more important than designing great
agent definitions.** The agent provides reasoning; the skill provides the raw
material for that reasoning to operate on.

---

## 2. The Skill Contract

Every skill file must follow a contract that defines what it contains, what it
excludes, and how it interfaces with agents.

### 2.1 — Required Sections

```
DOMAIN: {what this skill covers — one line}
VERSION: {semantic version for change tracking}
APPLIES WHEN: {file-system signals or context indicators}

CONVENTIONS:
  {Numbered list of conventions, each with rationale}

PATTERNS:
  {Named patterns with concrete code examples}

ANTI-PATTERNS:
  {What to avoid, why it's wrong, and the correct alternative}

DEPENDENCIES:
  {Libraries, versions, build configuration required}

EXAMPLES:
  {Complete, runnable examples that demonstrate the conventions in action}
```

### 2.2 — Required Properties of Each Section

**DOMAIN** — Single sentence. Used by the Orchestrator to match skills to tasks.

**VERSION** — Semantic version (`MAJOR.MINOR.PATCH`). MAJOR increments when
conventions change in breaking ways (agent output will differ). MINOR for
additions. PATCH for clarifications. Used for freshness tracking
(see [07 — Freshness](07-freshness-and-bounded-autonomy.md)).

**APPLIES WHEN** — File-system signals that indicate this skill is relevant.
Used by the context detection pipeline
(see [06 — Context Detection](06-context-detection-and-adaptation.md)).

```
APPLIES WHEN:
  - build.gradle.kts or build.gradle is present
  - Dependencies include org.springframework.boot:spring-boot-starter-test
  - Test files exist under src/test/java/
```

**CONVENTIONS** — Actionable instructions, not aspirational goals. Each convention
answers: "When the agent encounters situation X, it should do Y."

```
Good:  "Name test methods using the pattern:
        methodName_stateUnderTest_expectedBehavior"
Bad:   "Use meaningful test names"  ← Too vague, agent will interpret freely
```

**PATTERNS** — Concrete code examples showing the convention applied correctly.
Patterns should be copy-paste-adaptable, not abstract descriptions.

**ANTI-PATTERNS** — Equally concrete. Show the wrong code, explain why it's wrong,
and show the correction. Anti-patterns are often more valuable than patterns
because they prevent the most common mistakes.

**DEPENDENCIES** — Exact library coordinates and versions. The agent should
not guess dependency versions.

**EXAMPLES** — Complete, self-contained code that demonstrates multiple
conventions working together. This is the agent's primary reference point when
generating output.

### 2.3 — What a Skill File Must NOT Contain

| Excluded Content | Reason | Correct Location |
|----------------|--------|-----------------|
| Reasoning instructions ("think step by step...") | Reasoning belongs to agents | Agent identity file |
| Task descriptions ("generate tests for...") | Tasks belong to prompts | Prompt template |
| Project-specific facts ("User table has id, email...") | Facts change per project | Knowledge base |
| Orchestration logic ("after generating code, run tests") | Lifecycle belongs to orchestration | Orchestrator definition |
| Other skills' concerns ("also ensure REST conventions") | Skills are composable, not interdependent | Separate skill file |

---

## 3. Skill Composition

### 3.1 — The Composition Model

An agent can load **multiple skills simultaneously**. This is the primary
mechanism for building rich, context-specific behavior from reusable parts.

```
Test Agent
  + testing.skill.md     (general testing conventions)
  + junit5.skill.md      (JUnit 5–specific patterns)
  + java.skill.md         (Java 21 language conventions)
  + spring-boot.skill.md  (Spring Boot testing patterns)
  = Agent that generates Spring Boot JUnit 5 tests following
    Java 21 conventions and general testing best practices
```

### 3.2 — Composition Rules

**Rule 1 — No Duplicated Conventions**

If `testing.skill.md` says "every public method needs at least one test" and
`junit5.skill.md` also says the same thing, you have duplication. When the
convention changes, you must update both. One will be missed.

**Fix:** `testing.skill.md` owns the principle. `junit5.skill.md` owns the
JUnit 5–specific implementation of that principle. No overlap.

```
testing.skill.md:
  "Every public method → at least one test method."

junit5.skill.md:
  "Structure: one @Test method per scenario. Use @Nested for grouping
   by method-under-test when a class has many public methods."
```

**Rule 2 — Skills Must Be Independent**

Skill A must not reference Skill B. Each skill must make sense in isolation.
If `junit5.skill.md` says "see also `java.skill.md` for variable conventions,"
you've created a dependency. The agent might receive `junit5.skill.md` without
`java.skill.md` in a different context.

**Fix:** Each skill is self-contained for its domain. If two skills cover
related ground, the overlap is handled by the agent's reasoning (the agent
synthesizes multiple independent skill files into coherent output).

**Rule 3 — Composition Order Does Not Matter**

Loading `java.skill.md` before `junit5.skill.md` must produce the same result as
the reverse. If order matters, the skills have implicit dependencies between them.

**Rule 4 — Conflicts Are Resolved by Specificity**

When two skills provide conflicting guidance for the same situation, the more
specific skill wins. `junit5.skill.md` is more specific than `testing.skill.md`
for JUnit 5 concerns. The agent applies the more specific convention.

This should be stated in the agent's identity file as a conflict resolution
principle:

```
CONFLICT RESOLUTION:
  When loaded skills provide conflicting guidance, apply the more
  specific skill's convention. Framework-specific > language-specific
  > general-domain.
```

### 3.3 — Composition Limits

Based on the Cognitive Load Threshold (see [02 — Agent Taxonomy](02-agent-taxonomy.md)),
a single agent invocation should not load more than **3–4 skill files** without
consideration. Beyond that:

| Skill Count | Risk | Mitigation |
|-------------|------|-----------|
| 1–2 | Low | Agent follows conventions reliably |
| 3–4 | Moderate | Agent may miss conventions from less-prominent skills |
| 5+ | High | Use skill rotation (multiple agent passes) instead |

**Skill rotation** involves invoking the same agent multiple times, each time with
a different subset of skills, then aggregating results. See
[05 — Orchestration Patterns](05-orchestration-patterns.md) §4.3.

---

## 4. Skill Granularity

### 4.1 — Three Levels of Granularity

Skills can be authored at three levels, each serving a different purpose:

```
┌──────────────────────────────────────────┐
│ LEVEL 3: Project-Specific Skills         │
│ our-user-service-conventions.skill.md    │
│ Scope: One repository                    │
│ Owner: Project team                      │
│ Changes: Frequently                      │
├──────────────────────────────────────────┤
│ LEVEL 2: Framework-Specific Skills       │
│ junit5.skill.md, spring-boot.skill.md    │
│ Scope: All projects using that framework │
│ Owner: Platform / architecture team      │
│ Changes: When framework conventions evolve│
├──────────────────────────────────────────┤
│ LEVEL 1: Domain-Specific Skills          │
│ testing.skill.md, rest-api.skill.md      │
│ Scope: All projects in the enterprise    │
│ Owner: Central engineering standards team │
│ Changes: Rarely                          │
└──────────────────────────────────────────┘
```

### 4.2 — When to Use Each Level

**Level 1 (Domain)** — Universal principles that transcend frameworks. Use when
a convention applies regardless of technology choice.

```
testing.skill.md:
  "Test one behavior per test method."
  "Never share mutable state between tests."
  "Name tests to describe the expected behavior, not the implementation."
```

**Level 2 (Framework)** — Framework-specific implementations of universal
principles. Use when a convention is tied to a particular framework's APIs.

```
junit5.skill.md:
  "Use @DisplayName for human-readable descriptions."
  "Use @Nested to group tests by method-under-test."
  "Use AssertJ's fluent API: assertThat(result).isEqualTo(expected)."
```

**Level 3 (Project)** — Team conventions that go beyond or override enterprise
standards. Use sparingly — only when a project has legitimate reasons to deviate.

```
our-user-service-conventions.skill.md:
  "All DTOs use Java records."
  "Repository tests use Testcontainers with PostgreSQL 15."
  "Controller tests use @WebMvcTest, not @SpringBootTest."
```

### 4.3 — The Granularity Decision

```
Is the convention universal (any framework, any language)?
  ├── YES → Level 1 (Domain skill)
  └── NO ↓

Is it specific to a framework but applicable across projects?
  ├── YES → Level 2 (Framework skill)
  └── NO ↓

Is it specific to this project or team?
  ├── YES → Level 3 (Project skill)
  └── NO ↓

It might be a knowledge base entry (project-specific fact),
not a skill convention. See §5.
```

### 4.4 — Enterprise Distribution Pattern

In a GitHub Enterprise setting with hundreds of repositories:

```
github.com/org/.copilot-skills/         ← Central skill repository
  ├── domain/
  │   ├── testing.skill.md               ← Level 1, enterprise-wide
  │   ├── rest-api.skill.md
  │   ├── security.skill.md
  │   └── observability.skill.md
  └── framework/
      ├── junit5.skill.md                ← Level 2, framework-wide
      ├── spring-boot.skill.md
      ├── micronaut.skill.md
      ├── vitest.skill.md
      └── cypress.skill.md

github.com/org/user-service/.copilot/   ← Project repository
  └── skills/
      └── project.skill.md              ← Level 3, project-specific
```

The central repository is the source of truth for Levels 1 and 2. Projects
import or reference them and add Level 3 overrides as needed. This gives the
enterprise consistency while allowing project-level flexibility.

---

## 5. Skills vs. Knowledge Bases

### 5.1 — The Fundamental Distinction

| Aspect | Skill | Knowledge Base |
|--------|-------|---------------|
| **Nature** | Prescriptive (instructions) | Descriptive (facts) |
| **Content** | "Do X, don't do Y" | "X exists, Y has property Z" |
| **Stability** | Changes when conventions evolve | Changes when the project changes |
| **Verification** | Requires judgment (is this a good convention?) | Mechanically verifiable (does this table exist?) |
| **Reusability** | Cross-project (Level 1–2) | Project-specific |
| **Example** | "Name test methods as `method_state_expected`" | "UserService has methods: create, findById, update, delete" |

### 5.2 — How They Interact

Skills tell agents *what conventions to follow*. Knowledge bases tell agents
*what facts to work with*. The agent combines both:

```
Skill (junit5.skill.md):
  "Create one test class per service class."
  "One @Test method per behavior scenario."

Knowledge Base (codebase-patterns/UserService.md):
  "UserService has 4 public methods: createUser, findById, updateUser, deleteUser."
  "createUser throws DuplicateEmailException if email exists."
  "findById throws UserNotFoundException if id not found."

Agent output (combining both):
  UserServiceTest.java with:
    - createUser_validInput_returnsCreatedUser
    - createUser_duplicateEmail_throwsDuplicateEmailException
    - findById_existingId_returnsUser
    - findById_nonExistentId_throwsUserNotFoundException
    - updateUser_validInput_returnsUpdatedUser
    - deleteUser_existingId_deletesSuccessfully
```

### 5.3 — Knowledge Base Types

| Type | Content | Source | Refresh Frequency |
|------|---------|--------|------------------|
| **API Specifications** | OpenAPI/Swagger definitions | Generated from code or authored | Per API change |
| **Database Schemas** | Table definitions, relationships, constraints | DDL scripts, migration files | Per migration |
| **Codebase Patterns** | Extracted patterns from existing code (class structures, method signatures) | Static analysis or manual extraction | Per significant refactor |
| **External Docs** | Framework documentation snapshots, library APIs | Vendor docs | Per dependency upgrade |
| **Architecture Decision Records** | Why specific decisions were made | Team-authored | Per significant decision |

### 5.4 — Knowledge Base Freshness

Knowledge bases go stale faster than skills because they reflect project state.
Strategies for maintaining freshness:

1. **Generated knowledge bases:** Automate extraction from source code.
   A CI step generates `codebase-patterns/` from actual class files.

2. **Schema-linked knowledge:** Point at migration files rather than duplicating
   schema definitions. The agent reads the migration directly.

3. **Timestamped entries:** Each knowledge base entry includes a `LAST_VERIFIED`
   date. Agents treat entries older than a threshold with lower confidence.

4. **On-demand discovery:** Instead of maintaining a static knowledge base, the
   agent reads source code directly at runtime. This is the freshest approach but
   increases token cost and latency.

---

## 6. Designing a Skill File: Step by Step

### Step 1 — Define the Domain Boundary

What does this skill cover? Be precise. Vague domains lead to vague conventions.

```
Good:  "JUnit 5 test authoring with AssertJ and Mockito"
Bad:   "Testing in Java"  ← Too broad; overlaps with testing.skill.md,
                             integration-testing.skill.md, etc.
```

### Step 2 — List the Signals

What file-system or project signals indicate this skill is relevant? The
Orchestrator uses these for context-aware skill selection.

```
APPLIES WHEN:
  - build.gradle.kts contains 'org.junit.jupiter:junit-jupiter'
  - pom.xml contains <artifactId>junit-jupiter</artifactId>
  - Test files use @Test annotation from org.junit.jupiter.api
```

### Step 3 — Enumerate Conventions (Aim for 10–20)

Each convention should be:
- **Specific** enough that an agent can follow it without interpretation
- **Justified** with a brief rationale (so the agent understands *why*)
- **Testable** — a Reviewer can verify compliance

```
1. NAMING: Test methods follow `methodName_stateUnderTest_expectedBehavior`.
   Rationale: communicates intent without reading the test body.

2. DISPLAY NAMES: Every @Test has a @DisplayName with a human-readable sentence.
   Rationale: test reports become self-documenting.

3. ASSERTIONS: Use AssertJ fluent API (assertThat), not JUnit assertions.
   Rationale: fail messages are clearer; chained assertions read naturally.
```

### Step 4 — Provide Patterns with Complete Examples

Each pattern includes enough code to be copy-paste-adaptable. Show imports,
annotations, class structure — not just the assertion line.

### Step 5 — Catalog Anti-Patterns

For each anti-pattern, show the wrong code, explain the problem, and show the
fix. Anti-patterns from experiment results (like the `Optional.get()` divergence
found in this repository's analysis) are especially valuable.

### Step 6 — Specify Dependencies

Exact coordinates, version ranges, and build configuration. The agent should
not need to search for version numbers.

### Step 7 — Version and Date

Tag with a semantic version and last-updated date. This feeds the freshness
mechanism described in [07 — Freshness](07-freshness-and-bounded-autonomy.md).

---

## 7. The Feedback Loop: Skills Evolve from Agent Runs

Skills are not write-once documents. They improve through a systematic
feedback loop:

```
┌─────────────────────────────────────────────────────┐
│                                                     │
│  Agent Run → Reviewer Findings → Skill Amendments   │
│       ↑                                  │          │
│       └──────────────────────────────────┘          │
│                                                     │
│  Experiment → Evaluation → Analysis → Skill Patch   │
│       ↑                                  │          │
│       └──────────────────────────────────┘          │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### Real-World Example from This Repository

The `experiments/agent-consistency/` experiment ran the same workflow twice and
found:

1. **Run 1:** Agent used `Optional.get()` instead of `orElseThrow()`. Reviewer
   caught it. **Root cause:** `java.skill.md` did not list `orElseThrow` as
   the required pattern for Optional unwrapping.

2. **Run 2:** Agent returned a plain string for 409 conflict responses instead
   of a structured error DTO. Reviewer caught it. **Root cause:** `rest-api.skill.md`
   did not include an explicit example of error response bodies.

**Both findings feed back into skill files as new conventions:**

```
java.skill.md (amendment):
  "ANTI-PATTERN: Never call Optional.get(). Always use
   orElseThrow(() -> new XNotFoundException(...))."

rest-api.skill.md (amendment):
  "PATTERN: Error responses must use the structured DTO format:
   { \"code\": \"DUPLICATE_EMAIL\", \"message\": \"...\" }
   Example for 409 Conflict: [complete code example]"
```

### Formalizing the Feedback Process

1. **After every agent run:** The Reviewer's findings are collected.
2. **Findings are categorized:** Convention violation (skill gap) vs. reasoning
   error (agent issue) vs. one-off mistake (noise).
3. **Skill gaps become PRs** to the relevant skill file, with the finding as
   justification.
4. **The skill's VERSION is incremented** to signal the change.
5. **Subsequent agent runs** benefit from the amended skill immediately.

This creates a **virtuous cycle** where the system gets better with use. Each
Reviewer finding is an opportunity to make the skill file more complete, which
reduces future Reviewer findings.

---

## 8. Skill File Sizing Guidelines

| Size | Word Count | Risk | Guidance |
|------|-----------|------|----------|
| **Minimal** | < 200 words | Agent has insufficient guidance; output will be generic | Add more conventions and examples |
| **Optimal** | 300–800 words | Agent has clear, followable instructions without overload | Target this range |
| **Heavy** | 800–1500 words | Agent may miss conventions toward the end of the file | Consider splitting into sub-skills |
| **Overloaded** | > 1500 words | Conventions will be dropped; reliability degrades | Must split or use skill rotation pattern |

**The optimal skill file is comprehensive enough to prevent common mistakes but
concise enough that every convention gets followed.** When in doubt, add more
anti-patterns (they prevent mistakes) and fewer aspirational guidelines (they
add length without preventing specific errors).

---

## Key Takeaways

1. **Skills are prescriptive instruction sets** — not reference documentation,
   not reasoning guides, not task descriptions. They answer "what conventions
   to follow."

2. **The skill contract** (DOMAIN, VERSION, APPLIES WHEN, CONVENTIONS, PATTERNS,
   ANTI-PATTERNS, DEPENDENCIES, EXAMPLES) ensures consistency and composability.

3. **Composition rules** — no duplication, no dependencies between skills, order
   independence, specificity-based conflict resolution — keep the system clean.

4. **Three granularity levels** (Domain, Framework, Project) enable enterprise
   standardization with project-level flexibility.

5. **Skills are NOT knowledge bases.** Skills prescribe; knowledge bases describe.
   Mixing them creates staleness and coupling.

6. **The feedback loop** — Reviewer findings → skill amendments → better agent
   runs — is the primary mechanism for continuous improvement. Experiment data
   from this repository confirms: skill completeness is the strongest predictor
   of agent output quality.

---

*Next: [04 — Prompt Design](04-prompt-design.md) — Prompts as parameterized task triggers, the layering model, and the injection strategy for environment-detected context.*
