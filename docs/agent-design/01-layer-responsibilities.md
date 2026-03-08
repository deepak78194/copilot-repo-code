# Layer Responsibilities — What Exactly Goes Where
> The decision rules for placing content in each layer, with real examples from a Jakarta REST → Micronaut migration agent.

---

## The Decision Tree

When you write something for your agent, ask these questions in order:

```
Is it a RULE the agent must always follow, no matter what?
├── YES → copilot-instructions.md (global) or .instructions.md (scoped)
│         (but only if it's < 2 sentences — otherwise it's a procedure)
└── NO ↓

Is it WHO the agent is + WHAT tools it has access to?
├── YES → .agent.md  (persona + tool list only)
└── NO ↓

Is it HOW to do a specific phase or workflow?
├── YES → SKILL.md  (procedure steps, max 80 lines)
└── NO ↓

Is it a CODE EXAMPLE, template, or before/after pattern?
├── YES → Separate .ts/.java file in the skill folder, referenced by SKILL.md
└── NO ↓

Is it READING or SCANNING the codebase (many files, grep, discovery)?
├── YES → Subagent (gets its own clean context window)
└── NO ↓

Is it a reusable ENTRY POINT (how a user starts the workflow)?
    YES → .prompt.md (slash command that kicks off the agent + skill)
```

---

## Layer 1: `copilot-instructions.md` — The Always-On Contract

### What belongs here
Only short, universal rules that apply to **every** file edit in the repo — regardless of task or agent.

### What does NOT belong here
- Procedures ("first do X, then do Y")
- Code examples or patterns
- Phase descriptions
- Agent persona

### Template for a migration repo

```markdown
# Repo-Wide Copilot Instructions

## Code Style
- All new Java uses Micronaut 4.x annotations, not Jakarta REST
- All service classes must be annotated with @Singleton
- Use Project Reactor (Mono/Flux) for async, never CompletableFuture

## Commit conventions
- Prefix commits: feat/fix/refactor/test/chore
- Migration commits: use prefix `migrate:` followed by endpoint path

## Testing
- Every new controller requires a matching *ControllerSpec.java or *Spec.groovy
- Always mock external HTTP clients with @MockBean in tests
```

**Token cost:** ~150 tokens. Loaded every request. Justified.

---

## Layer 2: `.agent.md` — The Thin Orchestrator

### What belongs here

| Belongs | Does NOT belong |
|---|---|
| Agent name + one-line description | Step-by-step instructions |
| Tool list (restricted to what's needed) | Code patterns or examples |
| Which skills exist + when to invoke them | Research procedures |
| Phase routing logic (1–2 lines per phase) | Domain knowledge |
| Model preference if needed | Test generation patterns |

### What it should look like (example)

```yaml
---
name: jakarta-migration-agent
description: >
  Migrates Jakarta REST API endpoints to Micronaut microservice controllers.
  Invoke for one endpoint at a time. Use /start-migration to begin.
tools:
  - read_file
  - grep_search
  - semantic_search
  - list_dir
  - run_in_terminal
  - replace_string_in_file
  - create_file
  - rename
  - usages
  # NOT included: browser tools, screenshot, fetch_webpage
---

## Phases

You work in phases. For each task, identify the phase and invoke the correct skill:

- **Discovery**: Use the `#research` skill to scan the endpoint and its dependencies
- **Planning**: Use the `#migration-planning` skill to produce a migration plan
- **Implementation**: Use the `#migration-impl` skill to perform the code changes
- **Testing**: Use the `#test-gen` skill to generate or update tests

## Rules
- Never skip the Discovery phase for an endpoint you haven't seen yet
- Always confirm the plan with the user before Implementation
- Invoke one skill at a time; do not combine phases in a single response
```

**Token cost:** ~200–250 tokens. Lean, orchestration-only.

### The most common mistake
Writing phase procedures inside the agent:

```markdown
❌ DON'T DO THIS in .agent.md:
## Research Phase
1. First, read the legacy endpoint file
2. Then find all DTOs it uses by searching for import statements
3. Then identify the service layer it calls
4. Then check if there are existing tests
5. Then examine the database access pattern...
(This is skill content. Move it to SKILL.md)
```

---

## Layer 3a: `SKILL.md` — Phase Procedures

### What belongs here

| Belongs | Does NOT belong |
|---|---|
| Numbered steps for the phase | Agent-level routing logic |
| Which tools to call in which order | Code examples (→ template files) |
| What output to produce | Global rules (→ instructions) |
| References to template files via `#file:` | Other phase procedures |
| Decision criteria for the phase | Long explanations |

### One skill per phase — keep them independent

```
skills/
  research/SKILL.md       ← only knows how to research
  migration/SKILL.md      ← only knows how to convert code
  testing/SKILL.md        ← only knows how to generate tests
  planning/SKILL.md       ← only knows how to produce a plan doc
```

### Example: `skills/migration/SKILL.md`

```markdown
# Migration Implementation Skill

When performing the code migration for an endpoint, follow these steps:

## Step 1: Confirm Input
Verify you have the migration plan from the planning phase. If not, stop and
invoke the planning skill first.

## Step 2: Convert the Controller
- Read the legacy endpoint with read_file
- Apply the Micronaut controller pattern from
  #file:.github/skills/migration/endpoint-pattern.java
- Replace @Path with @Controller, @GET/@POST with @Get/@Post
- Replace Response with HttpResponse<T>
- Replace @QueryParam with @QueryValue, @PathParam with @PathVariable

## Step 3: Convert the DTOs
- For each DTO class referenced, apply the pattern from
  #file:.github/skills/migration/dto-pattern.java
- Add @Introspected annotation to all DTOs
- Replace Jackson annotations with Micronaut Serde where listed in the plan

## Step 4: Update Dependency Injection
- Replace @Inject (Jakarta) with @Inject (still works) but verify imports point
  to jakarta.inject, not javax.inject
- Replace @ApplicationScoped with @Singleton

## Step 5: Verify Compilation
- Run: `./gradlew compileJava` or `mvn compile`
- Fix any compilation errors before proceeding to testing

## Output
Report which files were changed and confirm readiness for test generation.
```

**Token cost:** ~250 tokens. Loaded only when this skill is invoked.

### Code examples DO belong in template files, not SKILL.md

```markdown
✅ RIGHT — reference, don't inline:
Apply the pattern from #file:.github/skills/migration/endpoint-pattern.java

❌ WRONG — inlining code in SKILL.md:
Apply this pattern:
```java
@Controller("/api")
public class UserController {
    @Get("/{id}")
    public HttpResponse<UserDto> getUser(@PathVariable Long id) {
        ...
    }
}
```
(This balloons the skill from 60 lines to 150+ lines. Move it to a .java file.)
```

---

## Layer 3b: `.instructions.md` — Scoped Constraints

### What belongs here

| Belongs | Does NOT belong |
|---|---|
| Rules that apply to a specific file area | Rules that apply everywhere (→ global instructions) |
| Framework-specific constraints per layer | Procedures (→ skills) |
| Required annotations for a layer | Code examples (→ templates) |
| Naming conventions for a folder | Agent routing logic |

### Pattern: one instructions file per architectural layer

```
.github/instructions/
  controllers.instructions.md   → applyTo: "src/main/java/**/controller/**"
  services.instructions.md      → applyTo: "src/main/java/**/service/**"
  repositories.instructions.md  → applyTo: "src/main/java/**/repository/**"
  tests.instructions.md         → applyTo: "src/test/**"
  dtos.instructions.md          → applyTo: "src/main/java/**/dto/**"
```

### Example: `controllers.instructions.md`

```markdown
---
applyTo: "src/main/java/**/controller/**"
---

## Micronaut Controller Rules
- Every controller must be annotated with both @Controller and @ExecuteOn
- All route methods must declare explicit return type HttpResponse<T>
- Use @Error for exception handling, never try/catch at the controller level
- Controller constructors use constructor injection only, never field injection
- All path variables must have explicit @PathVariable annotation with name
```

**Token cost:** ~120 tokens. Loaded only when editing controller files. ✅

### Critical rule: tight `applyTo` patterns

```yaml
❌ Too broad — loads for every file:
applyTo: "**"

❌ Still broad — loads for all Java:
applyTo: "**/*.java"

✅ Scoped — loads only for controllers:
applyTo: "src/main/java/**/controller/**"

✅ Very specific:
applyTo: "src/main/java/**/controller/**/*.java"
```

---

## Layer 4: Template Files — The Code Examples

### What belongs here
Actual code, complete patterns, before/after examples. These are referenced by skills via `#file:` and land in the **Files bucket** only when invoked.

### Naming convention

```
skills/
  migration/
    SKILL.md                          ← procedure (no code)
    endpoint-pattern.java             ← after: what the controller should look like
    endpoint-legacy-pattern.java      ← before: what legacy Jakarta looks like
    dto-pattern.java                  ← DTO conversion example
  testing/
    SKILL.md                          ← test procedure (no code)
    unit-test-template.java           ← unit test structure
    integration-test-template.groovy  ← integration test structure
    mock-client-template.java         ← mocking external HTTP clients
```

### Example: `skills/migration/endpoint-pattern.java`

```java
// TARGET PATTERN: Micronaut controller (reference only — adapt to actual code)
@Controller("/api/v1/users")
@ExecuteOn(TaskExecutors.BLOCKING)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {  // constructor injection
        this.userService = userService;
    }

    @Get("/{id}")
    public HttpResponse<UserDto> getUser(@PathVariable("id") Long id) {
        return userService.findById(id)
            .map(HttpResponse::ok)
            .orElse(HttpResponse.notFound());
    }

    @Post
    @Status(HttpStatus.CREATED)
    public HttpResponse<UserDto> createUser(@Body @Valid CreateUserRequest request) {
        UserDto created = userService.create(request);
        return HttpResponse.created(created);
    }

    @Error(UserNotFoundException.class)
    public HttpResponse<ErrorResponse> handleNotFound(UserNotFoundException e) {
        return HttpResponse.notFound(new ErrorResponse(e.getMessage()));
    }
}
```

**Token cost:** ~250 tokens. Loaded into Files bucket only when the migration skill references it. Not always loaded.

---

## Layer 5: `.prompt.md` — Entry Points

### What belongs here

| Belongs | Does NOT belong |
|---|---|
| The task framing for the user's slash command | Skill procedures |
| Which agent/mode to use | Code examples |
| Required inputs the user should provide | Rules or constraints |
| Which phase to start with | Long descriptions |

### Example: `prompts/start-migration.prompt.md`

```markdown
---
name: start-migration
description: Start a full Jakarta REST endpoint migration to Micronaut
mode: agent
agent: jakarta-migration-agent
---

# Jakarta → Micronaut Migration

I will guide you through a structured migration. Before we begin, please tell me:

1. **Which endpoint/file** do you want to migrate?
   (e.g., `src/main/java/com/example/users/UserResource.java`)

2. **Is this a full class** or a single method migration?

3. **Are there existing tests** for this endpoint? (yes/no)

Once you provide this, I will start with the Discovery phase.
```

**Token cost:** ~100 tokens. Lands in Messages bucket when run.

---

## Summary: The Content Allocation Table

| Content Type | Layer | File | Context Bucket | Loaded When |
|---|---|---|---|---|
| Universal repo rules | Layer 1 | `copilot-instructions.md` | System Instructions | Always |
| Agent persona + tools | Layer 2 | `.agent.md` | System Instructions | Agent selected |
| Phase procedures | Layer 3a | `SKILL.md` | System Instructions | Skill invoked |
| Scoped code rules | Layer 3b | `.instructions.md` | System Instructions | File pattern match |
| Code templates/examples | Layer 4 | `.java/.ts` in skill folder | Files | `#file:` referenced |
| Task entry points | Layer 5 | `.prompt.md` | Messages | Slash command run |
| Codebase scanning | Subagent | — | Tool Results (summary) | Phase 1 triggered |

---

## The Test Skill — Your Specific Question

> *"I created a test skill because Copilot gets test cases wrong. I put code examples in the skill. Is that allowed?"*

**Yes — with one refinement: move the code examples out of SKILL.md into separate files.**

### Current (typical — too dense):
```markdown
# Testing Skill (SKILL.md with inlined examples)

When writing tests, use this pattern:
```java
@MicronautTest
class UserControllerSpec {
    @Inject
    HttpClient client;
    
    @Test
    void testGetUser() {
        HttpResponse<UserDto> response = client.toBlocking()
            .exchange(HttpRequest.GET("/api/v1/users/1"), UserDto.class);
        assertEquals(200, response.status().getCode());
    }
}
```
And for mocking services use:
```java
@MockBean(UserService.class)
UserService userService() {
    return mock(UserService.class);
}
```
(100+ lines of examples in SKILL.md — all load as System Instructions)
```

### Better (lean SKILL.md + external templates):
```markdown
# Testing Skill (SKILL.md — procedure only)

## Step 1: Choose test type
- Controller integration tests → apply #file:.github/skills/testing/integration-test-template.java
- Service unit tests → apply #file:.github/skills/testing/unit-test-template.java
- Repository tests → apply #file:.github/skills/testing/repository-test-template.java

## Step 2: Adapt the template
Replace placeholder class names, inject the correct service, add test cases
for the happy path and each error condition in the endpoint.

## Step 3: Verify
Run `./gradlew test --tests "*.<ClassName>*"` and fix any failures.
```

**Result:** SKILL.md = 30 lines in System Instructions. Templates = ~80 lines in Files bucket, but only loaded when the skill is actually invoked and references them.

---

*Continue to [02-migration-agent-refactor.md](02-migration-agent-refactor.md) — how to refactor a 500-line agent using these rules.*
