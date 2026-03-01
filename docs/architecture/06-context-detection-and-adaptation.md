# 06 — Context Detection & Adaptation: Environment Sensing and Dynamic Specialization

> **Purpose:** Define the complete context detection pipeline — from file-system signals through project type inference to skill selection — and establish the confirmation strategies and Context Object specification that enable agents to remain generic while dynamically adapting to any project.

---

## 1. Why Context Detection Matters

The core design goal is agents that remain generic in reasoning while
dynamically adapting to project context. Context detection is what bridges
the gap. Without it, either:

- **Users must specify everything explicitly** (burdensome, error-prone), or
- **Agents must guess** (unreliable, unpredictable)

A well-designed detection pipeline gives the system accurate environmental
awareness with minimal user burden and maximum predictability.

---

## 2. The Context Detection Pipeline

Context detection follows a four-stage pipeline:

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────────┐
│  SCAN    │───→│  INFER   │───→│  SELECT  │───→│   CONFIRM    │
│ Signals  │    │ Project  │    │  Skills  │    │  (if needed) │
│          │    │  Type    │    │          │    │              │
└──────────┘    └──────────┘    └──────────┘    └──────────────┘
```

### Stage 1 — SCAN: Collect File-System Signals

Read the project's file system to collect signals. This is a read-only,
deterministic operation.

**What to scan:**
- Root directory (build files, config files, lock files)
- Source directories (language detection)
- Test directories (test framework detection)
- CI/CD configuration (`.github/workflows/`, `Jenkinsfile`, etc.)
- Dependency declarations (parsed from build files)
- Existing agent/skill configuration (`.copilot/` directory)

**Scan depth:** Limit to 3 directory levels from root plus key known paths.
Deeper scanning increases latency without proportional signal gain.

### Stage 2 — INFER: Determine Project Type

Map collected signals to project type assertions. Each assertion has a
confidence level.

```
SIGNALS:
  build.gradle.kts present: YES
  pom.xml present: NO
  package.json present: NO
  src/main/java/ exists: YES
  Spring Boot starter in dependencies: YES
  JUnit Jupiter in test dependencies: YES

INFERENCES:
  build_system: gradle (confidence: HIGH — single build file)
  language: java (confidence: HIGH — src/main/java/ convention)
  framework: spring-boot (confidence: HIGH — explicit dependency)
  test_framework: junit5 (confidence: HIGH — explicit dependency)
```

### Stage 3 — SELECT: Choose Skills

Map inferred project type to available skill files. Check that all selected
skills exist.

```
SKILL SELECTION:
  language=java         → java.skill.md ✓ (exists)
  framework=spring-boot → spring-boot.skill.md ✓ (exists)
  test_framework=junit5 → junit5.skill.md ✓ (exists)
  domain=testing        → testing.skill.md ✓ (exists)

  ALL SKILLS AVAILABLE → proceed
```

If a skill is missing:

```
  language=kotlin → kotlin.skill.md ✗ (NOT FOUND)

  ACTION: escalate
  MESSAGE: "Detected Kotlin project but no kotlin.skill.md exists.
            I can proceed with generic conventions, or you can provide
            a Kotlin skill file. [Proceed generically] [Provide skill]"
```

### Stage 4 — CONFIRM: Validate with User (Conditional)

Apply the confidence-based confirmation strategy from
[04 — Prompt Design](04-prompt-design.md) §5.2:

```
if (all_confidences == HIGH):
    auto_proceed()
elif (any_confidence == LOW or conflicts_detected):
    full_confirmation_required()
else:
    targeted_confirmation(ambiguous_aspects_only)
```

---

## 3. Signal Taxonomy

### 3.1 — Build System Signals

| Signal | Inference | Confidence |
|--------|-----------|-----------|
| `build.gradle.kts` present | Gradle with Kotlin DSL | HIGH |
| `build.gradle` present | Gradle with Groovy DSL | HIGH |
| `pom.xml` present | Maven | HIGH |
| `package.json` present | Node.js / npm | HIGH |
| `bun.lockb` present | Bun runtime | HIGH |
| `Cargo.toml` present | Rust / Cargo | HIGH |
| `go.mod` present | Go modules | HIGH |
| Both `pom.xml` and `build.gradle.kts` | Build system ambiguity | LOW (conflict) |
| No build file found | Unknown or new project | LOW |

### 3.2 — Language Signals

| Signal | Inference | Confidence |
|--------|-----------|-----------|
| `src/main/java/` convention | Java | HIGH |
| `src/main/kotlin/` convention | Kotlin | HIGH |
| `.ts` files with `tsconfig.json` | TypeScript | HIGH |
| `.js` files without `tsconfig.json` | JavaScript | MEDIUM |
| `.py` files with `pyproject.toml` | Python | HIGH |
| Mixed `.java` and `.kt` files | Java + Kotlin (multi-language) | MEDIUM |

### 3.3 — Framework Signals

| Signal | Inference | Confidence |
|--------|-----------|-----------|
| `spring-boot-starter-*` in dependencies | Spring Boot | HIGH |
| `io.micronaut:*` in dependencies | Micronaut | HIGH |
| `@SpringBootApplication` annotation found | Spring Boot | HIGH |
| `hono` in `package.json` dependencies | Hono framework | HIGH |
| `express` in `package.json` dependencies | Express.js | HIGH |
| `next` in `package.json` dependencies | Next.js | HIGH |
| `@angular/core` in `package.json` | Angular | HIGH |
| `react` in `package.json` | React | HIGH (but framework may be Next/Remix/vanilla) |

### 3.4 — Test Framework Signals

| Signal | Inference | Confidence |
|--------|-----------|-----------|
| `junit-jupiter` in test dependencies | JUnit 5 | HIGH |
| `vitest` in devDependencies | Vitest | HIGH |
| `jest` in devDependencies | Jest | HIGH |
| `cypress` in devDependencies | Cypress | HIGH |
| `*.feature` files in test resources | Cucumber/BDD | HIGH |
| `*.spec.ts` naming convention | Likely Vitest or Jest | MEDIUM (need package.json) |
| `*.test.java` naming convention | Likely JUnit | MEDIUM |

### 3.5 — Infrastructure Signals

| Signal | Inference | Confidence |
|--------|-----------|-----------|
| `.github/workflows/*.yml` | GitHub Actions CI | HIGH |
| `Jenkinsfile` | Jenkins CI | HIGH |
| `Dockerfile` | Containerized deployment | HIGH |
| `docker-compose.yml` | Multi-service local development | HIGH |
| `application.yml` in resources | Spring Boot config | HIGH |
| `flyway` or `liquibase` in dependencies | Database migration framework | HIGH |
| `V*__*.sql` files | Flyway migrations | HIGH |

### 3.6 — Existing Code Pattern Signals

| Signal | Inference | Confidence |
|--------|-----------|-----------|
| Existing test files use `@ExtendWith(MockitoExtension.class)` | Mockito mocking style | HIGH |
| Existing tests use `assertThat()` imports from AssertJ | AssertJ assertion style | HIGH |
| Existing tests use `@WebMvcTest` | Controller slice testing pattern | HIGH |
| Existing code uses records for DTOs | Record-based DTO convention | HIGH |
| Existing code uses `sealed interface` | Modern Java patterns | HIGH |

**Existing code pattern signals are the highest-value signals** because they
reveal not just what tools are in use, but what conventions the team follows.
An agent that matches existing patterns produces output that feels native to the
codebase.

---

## 4. The Context Object Specification

All detection results are serialized into a structured Context Object that
flows through the entire orchestration chain. This is the single source of
truth for project context.

### 4.1 — Schema

```
CONTEXT OBJECT:
  # Detection metadata
  detected_at: {ISO timestamp}
  scan_root: {project root path}
  scan_depth: {max directory depth scanned}

  # Build system
  build_system:
    type: {gradle | maven | npm | bun | cargo | go | unknown}
    build_file: {path to primary build file}
    confidence: {HIGH | MEDIUM | LOW}

  # Language(s)
  languages:
    primary:
      name: {java | kotlin | typescript | javascript | python | go | rust}
      version: {detected version or "unknown"}
      confidence: {HIGH | MEDIUM | LOW}
    secondary: [{same structure}]  # for multi-language projects

  # Framework(s)
  frameworks:
    primary:
      name: {spring-boot | micronaut | hono | express | next | angular | ...}
      version: {detected version or "unknown"}
      confidence: {HIGH | MEDIUM | LOW}
    secondary: [{same structure}]

  # Test setup
  testing:
    framework: {junit5 | vitest | jest | cypress | cucumber | ...}
    libraries: [{name, version}]
    existing_tests:
      count: {number}
      paths: [{path to test files}]
      patterns_detected: [{e.g., "mockito-extension", "assertj-assertions"}]
    confidence: {HIGH | MEDIUM | LOW}

  # Infrastructure
  infrastructure:
    ci: {github-actions | jenkins | gitlab-ci | none | unknown}
    containerization: {docker | none | unknown}
    database_migration: {flyway | liquibase | none | unknown}

  # Skill mapping
  selected_skills:
    - skill: {skill file name}
      version: {skill file version}
      reason: {why this skill was selected}
      required: {true | false}

  # Overall confidence
  overall_confidence: {HIGH | MEDIUM | LOW}
  conflicts: [{description of conflicting signals}]
  missing_skills: [{skill files that should exist but don't}]
```

### 4.2 — Context Object Lifecycle

```
1. CREATED by the detection pipeline (Stage 1-2)
2. ENRICHED with skill selections (Stage 3)
3. CONFIRMED by user (Stage 4, if needed)
4. ATTACHED to the prompt that reaches each agent
5. LOGGED in the orchestration audit trail
6. DISCARDED after the workflow completes
   (re-detected fresh for each new workflow)
```

Re-detecting for each workflow ensures freshness — if the user just added a
dependency, the next workflow sees it. Caching detection results creates
staleness risks that outweigh the latency savings.

---

## 5. Handling Ambiguity and Conflicts

### 5.1 — Multi-Language Projects

Many enterprise projects contain multiple languages. The detection pipeline
must handle this gracefully:

```
Detected:
  src/main/java/     → Java (backend)
  frontend/src/*.ts  → TypeScript (frontend)
  scripts/*.py       → Python (build/deploy scripts)

Strategy:
  1. Identify primary language by source file count and build file
  2. Set secondary languages
  3. When the user's prompt is ambiguous ("add tests"), ask which module
  4. When the prompt is specific ("add tests for UserService"), use the
     module detection from the source path
```

### 5.2 — Framework Version Ambiguity

```
Detected:
  spring-boot-starter-parent: version not in build.gradle.kts
  Spring Boot plugin version: 3.x (from plugin block)
  But application.yml uses Spring Boot 2.x conventions

Strategy:
  - Trust the build file over configuration conventions
  - Confidence: MEDIUM (signal conflict)
  - Confirm: "I detected Spring Boot 3.x from your build configuration,
    but some configuration patterns suggest 2.x. Should I use Spring Boot
    3.x conventions? [Yes, use 3.x] [No, use 2.x]"
```

### 5.3 — Missing Build Files (New Projects)

```
Detected:
  No build file found
  A few .java files exist
  No test infrastructure

Strategy:
  - Confidence: LOW
  - Must confirm: "I found Java source files but no build system.
    Which setup should I target?
    [Gradle with Kotlin DSL] [Maven] [Let me set up the project first]"
```

### 5.4 — Conflicting Test Frameworks

```
Detected:
  junit-jupiter in dependencies AND
  testng in dependencies

Strategy:
  - Confidence: LOW (conflict)
  - Check existing test files: which framework do they actually use?
  - If existing tests use JUnit 5 exclusively, infer TestNG is a legacy
    dependency → confidence bumps to MEDIUM
  - If both are actively used in different modules, confirm with user
```

---

## 6. Context-Driven Skill Selection Rules

### 6.1 — The Selection Algorithm

```
FOR EACH detected technology:
  1. Map to candidate skill file(s)
  2. Check skill file exists in .copilot/skills/ or central skill repo
  3. Check skill file version compatibility with detected technology version
  4. Add to selected_skills with reason

THEN:
  1. Add domain-level skills that always apply (e.g., testing.skill.md
     when task is test generation)
  2. Add project-level skills if .copilot/skills/project.skill.md exists
  3. Check total skill count against composition limits
     (see 03 — Skill Design §3.3)
  4. If over limit, prioritize:
     a. Task-specific skills first (junit5.skill for test generation)
     b. Language skills second (java.skill)
     c. Framework skills third (spring-boot.skill)
     d. Domain skills last (testing.skill — most generic)
     → Use skill rotation for deprioritized skills
```

### 6.2 — Skill Version Compatibility

Skills should declare which technology versions they support:

```
# In junit5.skill.md header:
DOMAIN: JUnit 5 test authoring
VERSION: 1.5
COMPATIBLE_WITH:
  junit-jupiter: 5.9+
  java: 17+
  assertj: 3.24+
  mockito: 5.x
```

If the detected project uses `junit-jupiter: 5.7`, the skill file may still
apply but with reduced confidence. The Orchestrator can note this:

```
WARNING: junit5.skill.md (v1.5) is optimized for JUnit 5.9+.
Your project uses 5.7. Some patterns may not be available.
```

---

## 7. Adaptation Without Detection: The Skill Override Pattern

Sometimes auto-detection is unnecessary or undesirable. Users should be able
to override any detection decision explicitly:

### 7.1 — In Prompt Templates

```
REFERENCES:
  - {{testing_skill | default: auto-detect}}
  - {{language_skill | default: auto-detect}}
  - {{framework_skill | default: auto-detect}}

# User can override:
REFERENCES:
  - vitest.skill.md        ← explicit override, skip detection
  - typescript.skill.md    ← explicit override
  - auto-detect             ← let detection handle framework skill
```

### 7.2 — In Project Configuration

A project can pin skill selections in `.copilot/instructions.md`:

```
# .copilot/instructions.md
SKILL OVERRIDES:
  language: java.skill.md (v2.1)
  framework: spring-boot.skill.md (v1.3)
  testing: junit5.skill.md (v1.5)

# Skip auto-detection for these categories. Always use the specified skills.
```

This is useful when auto-detection is unreliable for a particular project
(e.g., complex monorepos) or when the team wants guaranteed consistency.

### 7.3 — Priority Order

When both detection and overrides exist:

```
1. Explicit prompt override (highest priority)
2. Project configuration override (.copilot/instructions.md)
3. Auto-detected skill selection
4. Enterprise default skills (lowest priority)
```

---

## 8. The Detection Pipeline for the Test Agent: Walk-Through

Continuing the Test Agent example from
[02 — Agent Taxonomy](02-agent-taxonomy.md) §6:

### Input

```
User: "Generate tests for UserService"
Project: playground/user-service/ (from this repository)
```

### Stage 1 — SCAN

```
Root scan:
  ✓ build.gradle.kts                → build system signal
  ✓ gradlew                         → Gradle wrapper present
  ✓ src/main/java/                  → Java source convention
  ✓ src/test/java/                  → Java test convention
  ✓ src/main/resources/application.yml → Spring config

Dependency scan (from build.gradle.kts):
  ✓ org.springframework.boot:spring-boot-starter-web
  ✓ org.springframework.boot:spring-boot-starter-data-jpa
  ✓ org.springframework.boot:spring-boot-starter-validation
  ✓ org.junit.jupiter:junit-jupiter (test)
  ✓ org.assertj:assertj-core (test)
  ✓ org.mockito:mockito-junit-jupiter (test)
  ✓ org.flywaydb:flyway-core

Existing test scan:
  ✓ UserControllerTest.java (uses @WebMvcTest, @MockBean, MockMvc)
  ✓ UserServiceTest.java (uses @ExtendWith(MockitoExtension.class))

File pattern scan:
  ✓ V1__create_users_table.sql → Flyway migration
  ✓ Model classes use records: CreateUserRequest, UpdateUserRequest, UserResponse
  ✓ Global exception handler present
```

### Stage 2 — INFER

```
build_system: gradle (HIGH — build.gradle.kts found, single build file)
language: java (HIGH — src/main/java/ convention)
  version: 21 (from sourceCompatibility in build.gradle.kts)
framework: spring-boot (HIGH — starter dependencies)
  version: 3.x (from plugin version)
test_framework: junit5 (HIGH — junit-jupiter dependency)
test_libraries: assertj (HIGH), mockito (HIGH)
test_patterns: mockito-extension, webmvc-test, assertj-fluent
database_migration: flyway (HIGH — flyway-core + V1__ migration)
dto_pattern: java-records (HIGH — existing record classes)
```

### Stage 3 — SELECT

```
Selected skills:
  1. java.skill.md (v2.1)           ← language=java
  2. spring-boot.skill.md (v1.3)    ← framework=spring-boot
  3. junit5.skill.md (v1.5)         ← test_framework=junit5
  4. testing.skill.md (v3.0)        ← task_type=test-generation

Total: 4 skills (within composition limit)
All exist: ✓
```

### Stage 4 — CONFIRM

```
Overall confidence: HIGH (all signals HIGH, no conflicts)
Action: auto-proceed (no confirmation needed)
```

### Result: Context Object

```
CONTEXT OBJECT:
  detected_at: 2026-03-01T10:00:00Z
  scan_root: playground/user-service/
  build_system: { type: gradle, confidence: HIGH }
  languages: { primary: { name: java, version: 21, confidence: HIGH } }
  frameworks: { primary: { name: spring-boot, version: 3.x, confidence: HIGH } }
  testing:
    framework: junit5
    libraries: [assertj, mockito]
    existing_tests: { count: 2, patterns: [mockito-extension, webmvc-test] }
    confidence: HIGH
  selected_skills:
    - { skill: java.skill.md, version: v2.1, required: true }
    - { skill: spring-boot.skill.md, version: v1.3, required: true }
    - { skill: junit5.skill.md, version: v1.5, required: true }
    - { skill: testing.skill.md, version: v3.0, required: true }
  overall_confidence: HIGH
  conflicts: []
  missing_skills: []
```

This Context Object is now attached to the prompt and flows to the Test Agent.
The agent knows exactly what environment it's working in, which conventions to
follow, and what existing patterns to match — **without having to detect any
of this itself.**

---

## Key Takeaways

1. **Four-stage pipeline** (Scan → Infer → Select → Confirm) provides structured,
   auditable context detection.

2. **Signal taxonomy** covers build systems, languages, frameworks, test setups,
   infrastructure, and existing code patterns. Existing code patterns are the
   highest-value signals.

3. **The Context Object** is the single source of truth for project context.
   It's created once per workflow, attached to every agent invocation, and logged
   for auditability.

4. **Confidence-based confirmation** — auto-proceed on HIGH, targeted confirm on
   MEDIUM, full ask on LOW — balances speed with governance.

5. **Skill overrides** (prompt-level, project-level, enterprise-level) provide
   escape hatches when detection is unreliable or undesirable.

6. **Re-detect per workflow** — never cache context across workflows. Freshness
   outweighs the latency cost.

---

*Next: [07 — Freshness & Bounded Autonomy](07-freshness-and-bounded-autonomy.md) — Guardrails, drift detection, governance, and the innovation budget.*
