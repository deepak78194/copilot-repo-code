# Refactoring a 500-Line Agent — Step by Step
> How to take a monolithic custom agent and redistribute its content into the right layers, applied to the Jakarta REST → Micronaut migration use case.

---

## Why the 500-Line Agent Is a Problem

Before fixing it, understand the exact cost. Every single line of your `.agent.md` is loaded into **System Instructions on every request** — even if you're only asking a quick follow-up question.

```
500 lines ≈ 3,500–5,000 tokens
System Instructions at 3,500 tokens extra =
  ~2.2% of a 160K context window, permanently occupied

Over a 20-turn migration session:
  3,500 tokens × 20 turns = 70,000 tokens consumed just re-reading
  the agent instructions that never change turn to turn
```

Worse: **all phases are always loaded**, even though you're only in one phase at a time. You're paying for the test patterns during the research phase, and for the research procedure during the test generation phase.

---

## The Refactoring Process

### Step 1: Audit What's in Your Agent

Read through your 500-line agent and tag each section:

```
Tag   Meaning
[G]   Global rule → copilot-instructions.md
[A]   Agent identity/tools → keep in .agent.md
[SK]  Phase procedure → create/move to SKILL.md
[IN]  Scoped code rule → create .instructions.md
[TF]  Code example/template → extract to separate file
[SA]  Codebase scanning/discovery → subagent
[PR]  Task entry point → .prompt.md
```

### Step 2: Common Patterns in a Migration Agent

Here's what a 500-line migration agent typically contains and where it should go:

```
SECTION                           LINES    TAG    DESTINATION
─────────────────────────────────────────────────────────────
Agent name, description           5        [A]    Keep
Tool list                         30       [A]    Keep (trim to only needed)
Phase overview (1-line each)      10       [A]    Keep (condensed)
─────────────────────────────────────────────────────────────
Listen/Learn phase steps          80       [SA]   → Subagent prompt
Research phase steps              100      [SA]   → Subagent prompt
─────────────────────────────────────────────────────────────
Planning phase steps              60       [SK]   → planning/SKILL.md
Migration impl steps              80       [SK]   → migration/SKILL.md
Test generation steps             60       [SK]   → testing/SKILL.md
─────────────────────────────────────────────────────────────
Controller code example           40       [TF]   → endpoint-pattern.java
DTO code example                  30       [TF]   → dto-pattern.java
Test code example                 50       [TF]   → unit-test-template.java
─────────────────────────────────────────────────────────────
Micronaut annotation rules        25       [IN]   → controllers.instructions.md
Test conventions                  20       [IN]   → tests.instructions.md
─────────────────────────────────────────────────────────────
TOTAL EXTRACTED                   ~445     ────   Out of agent
REMAINING IN AGENT                ~55      [A]    The lean orchestrator
─────────────────────────────────────────────────────────────
```

---

## The Target: What a Refactored Agent Looks Like

```yaml
# .github/agents/jakarta-migration-agent.agent.md
---
name: jakarta-migration-agent
description: >
  Migrates Jakarta REST endpoints to Micronaut microservice controllers.
  Works one endpoint at a time through structured phases.
  Start with /start-migration or say "migrate <file path>".
tools:
  - read_file
  - grep_search
  - semantic_search
  - list_dir
  - run_in_terminal
  - replace_string_in_file
  - multi_replace_string_in_file
  - create_file
  - rename
  - usages
  - file_search
---

## How I Work

I use a phased approach. Each phase has a dedicated skill.
I invoke one phase at a time and confirm with you before proceeding.

### Phase 1 — Discovery (Subagent)
Scan the target endpoint and its dependencies using a research subagent.
Summarize: file structure, DTOs used, services called, existing tests, DB access.

### Phase 2 — Planning
Invoke the `#migration-planning` skill.
Produce a written migration plan and confirm with the user before proceeding.

### Phase 3 — Implementation
Invoke the `#migration-impl` skill.
Convert controller, DTOs, and service bindings one file at a time.

### Phase 4 — Test Generation
Invoke the `#test-gen` skill.
Generate or update tests. Run them and fix failures before completing.

## Rules
- Never combine phases in one response
- Always confirm the plan before implementation
- If compilation fails, fix it before moving to tests
- Do not modify files outside the endpoint's dependency graph
```

**Line count: ~55 lines. Token cost: ~400 tokens.**  
Compare to 500 lines (~3,500 tokens). **88% reduction in always-on context cost.**

---

## The Four Skills to Create

### `skills/planning/SKILL.md`

```markdown
# Migration Planning Skill

Input: Discovery summary from Phase 1 (should be in the conversation already)

## Step 1: Inventory
List every file that needs to change:
- The controller/resource Java file
- Each DTO class referenced
- The service interface + implementation
- Any existing test files

## Step 2: Annotation Map
For each file, produce a table of "old annotation → new annotation":
| Legacy Jakarta        | Micronaut Replacement       |
|-----------------------|-----------------------------|
| @Path("/api/users")   | @Controller("/api/users")   |
| @GET                  | @Get                        |
| @POST                 | @Post                       |
| @PathParam("id")      | @PathVariable("id")         |
| @QueryParam("page")   | @QueryValue("page")         |
| @Consumes             | remove (auto-detected)      |
| @Produces             | remove (auto-detected)      |
| Response              | HttpResponse<T>             |
| @ApplicationScoped    | @Singleton                  |

## Step 3: Risk Flags
Note any items that need human review:
- Custom interceptors/filters
- Multi-part form uploads
- SSE or streaming endpoints
- Non-standard exception mappers

## Output
Write the plan as a markdown block in chat. Wait for user confirmation.
Do NOT proceed to implementation until the user explicitly approves.
```

---

### `skills/migration/SKILL.md`

```markdown
# Migration Implementation Skill

Input: Confirmed migration plan from planning phase.

## Step 1: Controller Conversion
- Read current file with read_file
- Apply the Micronaut pattern from
  #file:.github/skills/migration/endpoint-pattern.java
- Replace annotations per the approved annotation map
- Update method return types to HttpResponse<T>
- Move exception handling to @Error methods

## Step 2: DTO Updates
- For each DTO in the plan:
  - Add @Introspected to the class
  - Verify Serde compatibility
  - Apply pattern from #file:.github/skills/migration/dto-pattern.java

## Step 3: Service Layer
- Update @Inject import paths if needed (jakarta.inject vs javax.inject)
- Replace @ApplicationScoped with @Singleton
- No other service layer changes unless flagged in the plan

## Step 4: Compile Check
Run: `./gradlew compileJava` (or `mvn compile` for Maven)
Fix any compile errors. Do not proceed to tests if compilation fails.

## Output
List every file changed with a one-line summary of what changed.
Confirm readiness for Phase 4 (test generation).
```

---

### `skills/testing/SKILL.md`

```markdown
# Test Generation Skill

Input: List of changed files from implementation phase.

## Step 1: Identify Test Type Needed
For each changed file decide:
- Controller file → integration test using Micronaut test client
  Template: #file:.github/skills/testing/integration-test-template.java
- Service file → unit test with mocked dependencies
  Template: #file:.github/skills/testing/unit-test-template.java
- Repository file → data test with @MicronautTest
  Template: #file:.github/skills/testing/repository-test-template.java

## Step 2: Check Existing Tests
Use read_file to read any existing test files identified in Phase 1.
Update them to use Micronaut test annotations — do not delete existing
test logic, only update the framework-specific parts.

## Step 3: Generate Missing Tests
If no test file exists, create one using the appropriate template.
Cover: happy path, not-found case, validation failure case.

## Step 4: Run Tests
Run: `./gradlew test --tests "*.<ControllerClassName>*"`
Fix any failures. Report final pass/fail status.

## Output
List of test files created or updated, and final test run result.
```

---

### `skills/research/SKILL.md`

```markdown
# Research / Discovery Skill

Used by the Discovery subagent to scan an endpoint before migration.

## What to Discover
Given an endpoint file path, collect:

1. **Endpoint definition**
   - HTTP method, path, produces/consumes media types
   - All method signatures and their parameter types

2. **DTO dependencies**
   - All DTO/POJO classes used as request/response bodies
   - Their package locations

3. **Service dependencies**
   - Service interfaces called (not implementations)
   - Each method called and its return type

4. **Existing test coverage**
   - Any *Test.java or *Spec.groovy files that reference this controller

5. **Database access pattern**
   - Does the service extend JpaRepository or similar?
   - Any native queries present?

6. **Known risks**
   - Any custom filters, interceptors, or exception mappers
   - Any file-based or streaming operations

## Output Format
Return a structured markdown summary with a section for each of the 6 items above.
Keep it concise — this is input to the planning phase, not a full report.
```

---

## The Instructions Files

### `instructions/controllers.instructions.md`

```markdown
---
applyTo: "src/main/java/**/controller/**"
---

## Micronaut Controller Conventions
- Class annotation: @Controller("path") — path must be lowercase kebab-case
- Method injection only via constructor — no @Inject on fields
- All route methods must declare return type explicitly as HttpResponse<T>
- Use @ExecuteOn(TaskExecutors.BLOCKING) on any method doing synchronous I/O
- @Error methods must be in the same class as the route that can throw them
- No try/catch blocks inside route methods — use @Error handlers
```

### `instructions/tests.instructions.md`

```markdown
---
applyTo: "src/test/**"
---

## Micronaut Test Conventions
- Integration tests: annotate class with @MicronautTest
- Use @Inject HttpClient client — never construct clients manually
- Mock beans: declare @MockBean(ServiceClass.class) as a method in the test class
- Test method naming: should_<expected>_when_<condition>
- Always assert both status code and response body — never just one
- Use .toBlocking().exchange() for synchronous test assertions
```

---

## The Template Files

### `skills/migration/endpoint-pattern.java`
A complete, working Micronaut controller showing:
- Class-level `@Controller` with path
- Constructor injection
- `@Get`, `@Post` methods with `HttpResponse<T>` return
- `@Error` handler
- `@PathVariable`, `@QueryValue`, `@Body` parameters

### `skills/testing/integration-test-template.java`
A complete `@MicronautTest` test showing:
- `@Inject HttpClient client`
- `@MockBean` declaration
- `Mockito.when()` setup
- `client.toBlocking().exchange()` assertion
- Test for 200 OK, 404 Not Found, 400 Bad Request

---

## Context Cost Comparison

```
┌────────────────────────────────────────────────────────────────────────┐
│                    BEFORE REFACTORING                                  │
├──────────────────────────────────────────────────────────────────────┤
│  System Instructions per request:                                      │
│    migration-agent.agent.md: 500 lines ≈ 3,500 tokens                 │
│    copilot-instructions.md:  50 lines  ≈   350 tokens                 │
│    ────────────────────────────────────────────────────                │
│    TOTAL always-on:                     ~3,850 tokens  (2.4%)          │
│                                                                        │
│  But: ALL phase knowledge loaded even when working in just one phase   │
└────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────┐
│                    AFTER REFACTORING                                   │
├──────────────────────────────────────────────────────────────────────┤
│  System Instructions per request (base):                               │
│    jakarta-migration-agent.agent.md: 55 lines ≈ 400 tokens            │
│    copilot-instructions.md:          50 lines ≈ 350 tokens            │
│    Matching .instructions.md:        30 lines ≈ 210 tokens  (scoped)  │
│    ────────────────────────────────────────────────────                │
│    TOTAL always-on:                     ~960 tokens  (0.6%)            │
│                                                                        │
│  On-demand (loaded only when phase is active):                         │
│    Active skill SKILL.md:             60 lines ≈ 420 tokens            │
│    Referenced template file:          80 lines ≈ 560 tokens  (Files)  │
│    ────────────────────────────────────────────────────                │
│    TOTAL per-phase addition:            ~980 tokens                    │
│                                                                        │
│  NET per-request cost vs before:  960 vs 3,850 = 75% reduction        │
│  And: only the ACTIVE phase is present in context                      │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Migration Checklist

Use this to track your agent refactoring:

- [ ] Read through entire `.agent.md` and tag each section with [A]/[SK]/[IN]/[TF]/[SA]
- [ ] Keep only [A]-tagged content in the agent file
- [ ] Create `skills/planning/SKILL.md` from [SK] planning content
- [ ] Create `skills/migration/SKILL.md` from [SK] implementation content
- [ ] Create `skills/testing/SKILL.md` from [SK] test content
- [ ] Extract all [TF] code examples to `.java` template files
- [ ] Create `.instructions.md` for each [IN] rule block with tight `applyTo`
- [ ] Verify agent references the skills by name so it knows to invoke them
- [ ] Test: open Agent Debug Panel, run a task, confirm only active skill is loaded
- [ ] Verify context window indicator — System Instructions should drop significantly

---

*Continue to [03-subagent-strategy.md](03-subagent-strategy.md) — using subagents for discovery phases without consuming your main context window.*
