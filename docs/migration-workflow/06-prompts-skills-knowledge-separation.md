# 06 — Prompts, Skills & Knowledge Separation: What to Create

> **Purpose:** Define the complete inventory of new skills, prompts, and knowledge bases required for the migration workflow. Establish which existing artifacts are reused, what new ones must be created, how the Substitution Principle applies to the migration-specific assets, and provide the `api-migration.prompt.md` template.

---

## 1. Asset Inventory Summary

| Category | New | Reused | Total |
|----------|-----|--------|-------|
| **Skills** | 6 | 4 | 10 |
| **Prompts** | 1 | 0 | 1 |
| **Knowledge Bases** | 2 | 0 | 2 |
| **Agent Identity** | 1 (Migration Orchestrator) | 5 (Core agents) | 6 |

---

## 2. New Migration-Specific Skills

These skills are the primary enablers of the migration workflow. They follow the
skill contract from [03 — Skill Design](../architecture/03-skill-and-knowledge-design.md) §2.

### 2.1 — `legacy-analysis.skill.md`

```
DOMAIN: Analyzing legacy Jakarta EE applications for migration
VERSION: 1.0.0
APPLIES WHEN:
  - Project contains @Stateless, @Stateful, @Singleton EJBs
  - persistence.xml or web.xml is present
  - Source contains javax.*/jakarta.* imports

CONVENTIONS:
  1. Scan for JAX-RS annotations (@Path, @GET, @POST, etc.) to catalog endpoints.
  2. Scan for EJB annotations (@Stateless, @Stateful) to identify service boundaries.
  3. Identify data access via @PersistenceContext, EntityManager, NamedQuery.
  4. Catalog JNDI lookups (@Resource with mappedName, InitialContext.lookup()).
  5. Map transaction management: @TransactionAttribute (container-managed) vs
     UserTransaction (bean-managed).
  6. Identify error handling: ExceptionMapper implementations, try-catch patterns.
  7. Catalog external system integrations: JMS, SMTP, LDAP, REST clients.

PATTERNS:
  Endpoint Catalog Format:
    - path, method, handler_class, handler_method, params, response_type, auth
  Service Catalog Format:
    - class, type, methods[{name, business_rules, side_effects, transaction, errors}]
  Data Access Catalog Format:
    - entities, repositories, queries[{name, type, query_string, notes}]

ANTI-PATTERNS:
  - DO NOT execute legacy code or tests. Discovery is read-only analysis.
  - DO NOT modify any legacy file.
  - DO NOT assume business logic from method names — read the implementation.
  - DO NOT catalog private methods unless they contain significant business logic
    called by public methods.

DEPENDENCIES: None (read-only analysis)

EXAMPLES:
  See docs/migration-workflow/02-dual-workspace-discovery.md §3 for
  complete analysis protocol examples.
```

### 2.2 — `migration-planning.skill.md`

```
DOMAIN: Designing migration plans from legacy analysis to clean architecture
VERSION: 1.0.0
APPLIES WHEN:
  - A Discovery Report exists as input
  - Target framework is Micronaut or Spring Boot
  - Clean architecture is the target pattern

CONVENTIONS:
  1. API Redesign: map legacy endpoints to RESTful modern endpoints.
     Apply versioned paths, plural nouns, correct status codes.
  2. Clean Architecture Mapping: every legacy component maps to exactly one
     clean architecture layer (domain, application, infrastructure).
  3. Task Breakdown: order tasks inside-out (domain → application → infrastructure).
     Each task must produce a verifiable artifact.
  4. Risk Register: every HIGH and MEDIUM risk gets a mitigation strategy.
  5. Innovation Budget: maximum 5 optimization suggestions per migration batch.
     Each suggestion must be labeled, justified, scoped, and independently removable.

PATTERNS:
  Migration Plan Structure:
    1. API Redesign Table
    2. Clean Architecture Mapping (domain → application → infrastructure)
    3. Dependency Resolution
    4. Data Access Migration
    5. Task Breakdown (ordered by dependency)
    6. Risk Register
    7. Optimization Suggestions

  Task Format:
    - id, layer, task description, depends_on[], verifiable outcome

ANTI-PATTERNS:
  - DO NOT produce a 1:1 mapping of legacy structure to modern structure.
    The plan must reflect clean architecture, not the legacy organization.
  - DO NOT include implementation code in the plan. Implementation is Phase 3.
  - DO NOT auto-approve the plan. Approval is a human gate.
  - DO NOT exceed the Innovation Budget (max_suggestions: 5).

DEPENDENCIES: Discovery Report (Phase 1 output)

EXAMPLES:
  See docs/migration-workflow/03-migration-planner-design.md §3 for
  complete plan section examples.
```

### 2.3 — `clean-architecture.skill.md`

```
DOMAIN: Ports-and-adapters clean architecture for Java microservices
VERSION: 1.0.0
APPLIES WHEN:
  - Clean architecture or hexagonal architecture is specified as target pattern
  - Project structure includes domain/application/infrastructure layers

CONVENTIONS:
  1. The Dependency Rule: dependencies point inward. Domain knows nothing about
     infrastructure. Application knows domain but not infrastructure.
  2. Domain Layer:
     - Entities are plain Java (records or classes). NO framework annotations.
     - Value objects encapsulate validation and equality.
     - Domain events represent state changes.
  3. Application Layer:
     - Use cases implement inbound ports (interfaces).
     - Use cases depend on outbound ports (interfaces), never implementations.
     - One use case per business operation. No "god service" anti-pattern.
     - Commands and queries are separate objects (not entity reuse).
  4. Infrastructure Layer:
     - Inbound adapters (controllers) call inbound ports, not use cases directly.
     - Outbound adapters implement outbound ports.
     - Framework annotations live ONLY in infrastructure.
     - Persistence entities are separate from domain entities.
  5. Mapping:
     - Mappers convert between domain entities and persistence entities.
     - Mappers convert between domain entities and response DTOs.
     - Mappers are infrastructure concerns.

PATTERNS:
  Package Structure:
    com.example.{context}
    ├── domain
    │   ├── entity        (Order, OrderItem)
    │   ├── vo            (OrderNumber, Money, EmailAddress)
    │   └── event         (OrderCreatedEvent)
    ├── application
    │   ├── port
    │   │   ├── inbound   (CreateOrderPort, GetOrderPort)
    │   │   └── outbound  (SaveOrderPort, FindOrderPort, SendEmailPort)
    │   └── usecase       (CreateOrderUseCase, GetOrderUseCase)
    └── infrastructure
        ├── adapter
        │   ├── inbound   (OrderController)
        │   └── outbound  (JpaOrderAdapter, SmtpEmailAdapter)
        ├── persistence   (OrderJpaEntity, OrderMicronautRepository)
        └── config        (BeanFactory, configuration classes)

ANTI-PATTERNS:
  - WRONG: @Entity on a domain class
  - WRONG: Use case calling another use case (go through ports)
  - WRONG: Controller instantiating a use case (inject the port)
  - WRONG: Domain entity having getId() that returns a Long (use typed ID: OrderId)
  - WRONG: Repository interface in domain layer (it's an outbound port in application)

DEPENDENCIES: None (pattern, not library)

EXAMPLES:
  See docs/migration-workflow/04-implementation-and-verification.md §1.3
  for complete layer-by-layer code examples.
```

### 2.4 — `micronaut-testing.skill.md`

```
DOMAIN: Testing conventions for Micronaut 4.x applications
VERSION: 1.0.0
APPLIES WHEN:
  - io.micronaut:micronaut-test-junit5 in test dependencies
  - Test files under src/test/java/

CONVENTIONS:
  1. Use @MicronautTest for integration tests that need DI context.
  2. Use plain JUnit 5 for unit tests (domain, use cases with mocked ports).
  3. Inject HttpClient with @Client("/") for controller tests.
  4. Use Testcontainers for database integration tests.
  5. Use @MockBean for replacing beans in integration tests.
  6. Test naming: methodName_stateUnderTest_expectedBehavior.
  7. Use @DisplayName for human-readable descriptions.
  8. Use AssertJ for fluent assertions.
  9. Follow the testing pyramid: many unit tests, fewer integration, minimal E2E.
  10. Add @Serdeable to all DTOs used in HTTP client tests.

PATTERNS:
  Controller Integration Test:
    @MicronautTest
    class OrderControllerTest {
        @Inject @Client("/") HttpClient client;
        @Test void createOrder_validRequest_returns201() { ... }
    }

  Repository Integration Test:
    @MicronautTest @Testcontainers
    class OrderRepositoryTest {
        @Container static PostgreSQLContainer<?> pg = ...;
        @Inject OrderRepository repository;
        @Test void findByStatus_existingOrders_returnsMatching() { ... }
    }

  Use Case Unit Test:
    class CreateOrderUseCaseTest {
        // No @MicronautTest — pure unit test
        private SaveOrderPort savePort = mock(SaveOrderPort.class);
        private CreateOrderUseCase useCase = new CreateOrderUseCase(savePort, ...);
        @Test void createOrder_validItems_persistsOrder() { ... }
    }

ANTI-PATTERNS:
  - DO NOT use @MicronautTest for pure domain/use case tests (unnecessary overhead).
  - DO NOT mock what you don't own (mock ports, not framework classes).
  - DO NOT use Thread.sleep() for async testing (use test awaitility patterns).
  - DO NOT assert on toString() for behavior verification.

DEPENDENCIES:
  - io.micronaut.test:micronaut-test-junit5
  - org.assertj:assertj-core
  - org.testcontainers:postgresql (for DB tests)
  - org.mockito:mockito-core (for unit tests)
```

### 2.5 — `migration-checklist.skill.md`

```
DOMAIN: Verification checklist for migration output quality
VERSION: 1.0.0
APPLIES WHEN:
  - Reviewer agent is reviewing migration-generated code
  - Migration workflow Phase 4 (Verification) is active

CONVENTIONS:
  1. Anti-Transliteration Checks:
     □ Domain entities have NO framework annotations
     □ Use cases depend only on port interfaces
     □ Controllers delegate to inbound ports
     □ Repository adapters implement outbound ports
     □ No EntityManager usage — framework repository only
     □ No JNDI lookups
  2. REST API Quality:
     □ Structured error DTOs with error codes
     □ Correct HTTP status codes (201/204/404/409/422)
     □ Versioned paths (/api/v1/...)
     □ Plural nouns, no verbs in paths
  3. Code Quality:
     □ Constructor injection everywhere
     □ No field injection
     □ No raw types
     □ Structured SLF4J logging
     □ Externalized configuration
     □ Parameterized queries (no SQL concatenation)
  4. Test Quality:
     □ Every public method has at least one test
     □ Tests follow naming convention
     □ @DisplayName on every test
     □ Domain tests are framework-free (no @MicronautTest)
     □ Integration tests use Testcontainers

PATTERNS:
  Review Output Format:
    PASS: All checks satisfied.
    FAIL: {checklist_item} — {specific violation} — {file:line}

ANTI-PATTERNS:
  - DO NOT approve code that uses EntityManager directly.
  - DO NOT approve domain entities with @Entity or @MappedEntity.
  - DO NOT approve controllers that bypass ports and call use cases directly
    (unless the port and use case are the same interface).
```

### 2.6 — `migration-documentation.skill.md`

```
DOMAIN: Generating migration documentation for pull requests
VERSION: 1.0.0
APPLIES WHEN:
  - Migration workflow Phase 5 (Delivery) is active
  - All verification gates have passed

CONVENTIONS:
  1. PR Description must include:
     - Migration scope (which services/endpoints were migrated)
     - Architecture diagram (clean architecture layers)
     - Breaking changes (if any)
     - New dependencies added
     - Configuration changes required
     - Test coverage summary
  2. Migration Summary includes:
     - Legacy components decommissioned
     - Modern components created
     - Mapping table (legacy → modern)
     - Risks addressed and mitigations applied
  3. Decision Log records:
     - Each architectural decision made during planning
     - Alternatives considered
     - Rationale for the chosen approach
     - References to the Migration Plan sections

PATTERNS:
  PR Title: "feat: migrate {service} from Jakarta EE to Micronaut clean-arch"
  PR Labels: ["migration", "clean-architecture", "micronaut"]
```

---

## 3. Reused Existing Skills

These skills already exist (or are assumed to exist in the enterprise skill library)
and are used without modification:

| Skill | Used By | In Phase(s) | Purpose |
|-------|---------|------------|---------|
| `java.skill.md` | Implementer, Test Agent, Reviewer | 3, 4 | Java 21 language conventions (records, sealed interfaces, pattern matching, var) |
| `micronaut.skill.md` | Implementer, Planner (reference) | 2, 3 | Micronaut 4.x framework conventions (DI, controllers, configuration, data) |
| `junit5.skill.md` | Test Agent | 4 | JUnit 5 test conventions (@DisplayName, AssertJ, Mockito patterns) |
| `rest-api.skill.md` | Planner, Reviewer | 2, 4 | REST API conventions from copilot-instructions.md (versioned paths, status codes, structured errors) |

---

## 4. The API Migration Prompt Template

### 4.1 — `api-migration.prompt.md`

```yaml
---
name: api-migration
version: 1.0.0
description: Trigger a full Jakarta EE → Micronaut migration workflow
agent: migration-orchestrator
phases: [discovery, planning, implementation, verification, delivery]
parameters:
  - name: legacy_workspace
    type: path
    required: true
    description: Path to the legacy Jakarta EE project root
  - name: modern_workspace
    type: path
    required: true
    description: Path to the target Micronaut project root
  - name: scope
    type: string
    required: true
    description: Which service(s) or endpoint(s) to migrate
  - name: target_framework
    type: enum
    values: [micronaut, spring-boot]
    default: micronaut
    description: Target framework for the migration
  - name: innovation_budget
    type: integer
    default: 5
    description: Maximum optimization suggestions allowed
  - name: max_verification_iterations
    type: integer
    default: 2
    description: Maximum verification loop retries before escalation
---

TASK:
  Migrate the specified legacy Jakarta EE service to a Micronaut microservice
  using clean architecture (ports and adapters).

CONTEXT:
  Legacy workspace: {{legacy_workspace}}
  Modern workspace: {{modern_workspace}}
  Scope: {{scope}}

CONSTRAINTS:
  - Follow the five-phase pipeline: Discovery → Planning → Implementation →
    Verification → Delivery.
  - Do NOT proceed past Planning without explicit human approval.
  - Apply the Anti-Transliteration Principle: treat legacy code as requirements,
    not as a template.
  - Maximum {{innovation_budget}} optimization suggestions in the plan.
  - Maximum {{max_verification_iterations}} verification iterations before
    escalation.

REFERENCES:
  - docs/migration-workflow/ (this workflow documentation)
  - .copilot/skills/legacy-analysis.skill.md
  - .copilot/skills/migration-planning.skill.md
  - .copilot/skills/clean-architecture.skill.md
  - .copilot/skills/micronaut.skill.md (or spring-boot.skill.md per target)
```

### 4.2 — Prompt Usage Examples

**Full migration:**
```
@migration-orchestrator Migrate the OrderService from Jakarta EE to Micronaut.
Legacy workspace: /projects/legacy-order-app
Modern workspace: /projects/order-service
Scope: All endpoints in OrderResource
```

**Scoped migration:**
```
@migration-orchestrator Migrate only the GET /api/orders endpoint.
Legacy workspace: /projects/legacy-order-app
Modern workspace: /projects/order-service
Scope: GET /api/orders and GET /api/orders/{id}
```

---

## 5. Knowledge Bases

### 5.1 — Legacy Codebase Knowledge Base

This is not a skill file — it's a **knowledge base** (read-only reference material).

```
TYPE: Knowledge Base (dynamically populated)
NAME: legacy-codebase
POPULATED BY: Phase 1 (Discovery)

CONTENTS:
  - Discovery Report (structured YAML-like analysis)
  - File references to legacy source code
  - Dependency information from build files

CONSUMED BY:
  - Planner (Phase 2: uses report to design migration plan)
  - Implementer (Phase 3: references legacy behavior for requirements)
  - Reviewer (Phase 4: compares modern output against legacy behavior)

LIFECYCLE:
  - Created: Phase 1
  - Immutable after Phase 1 (legacy workspace is read-only)
  - Archived: After Phase 5 delivery
```

### 5.2 — Migration Decisions Knowledge Base

```
TYPE: Knowledge Base (incrementally populated)
NAME: migration-decisions
POPULATED BY: Phase 2 (Planning) + Phase 4 (Verification feedback)

CONTENTS:
  - Approved Migration Plan
  - User feedback and revision history
  - Verification iteration history (if failures occurred)
  - Escalation decisions and resolutions

CONSUMED BY:
  - Implementer (Phase 3: follows the approved plan)
  - Test Agent (Phase 4: generates tests matching plan expectations)
  - Implementer (Phase 5: generates PR documentation from decision history)

LIFECYCLE:
  - Created: Phase 2
  - Updated: Phase 2 (revisions), Phase 4 (iteration feedback)
  - Archived: After Phase 5 delivery
```

---

## 6. The Substitution Principle Applied to Assets

### 6.1 — Swapping Skills: Micronaut → Spring Boot

To adapt this workflow for Spring Boot instead of Micronaut:

| Asset | Change Required |
|-------|----------------|
| `micronaut.skill.md` | Replace with `spring-boot.skill.md` |
| `micronaut-testing.skill.md` | Replace with `spring-boot-testing.skill.md` |
| `clean-architecture.skill.md` | **No change** (framework-agnostic) |
| `legacy-analysis.skill.md` | **No change** (source-specific, not target-specific) |
| `migration-planning.skill.md` | **No change** (plan structure is target-agnostic) |
| `migration-checklist.skill.md` | Minor updates (change Micronaut-specific checks to Spring Boot) |
| `api-migration.prompt.md` | Change `target_framework` parameter default |
| Agent identities | **No change** |
| Orchestrator | **No change** (skill routing table updated) |

**Validation:** Only 2 skill files and 1 prompt parameter change. Zero agent
identity changes. The Substitution Principle holds.

### 6.2 — Swapping Skills: Jakarta EE → ASP.NET Legacy

To adapt this workflow for a .NET legacy source:

| Asset | Change Required |
|-------|----------------|
| `legacy-analysis.skill.md` | Replace with `legacy-analysis-dotnet.skill.md` |
| `java.skill.md` | Replace with `csharp.skill.md` |
| Target skills (micronaut) | Replace with `aspnet-core.skill.md` |
| `clean-architecture.skill.md` | **No change** |
| `migration-planning.skill.md` | **No change** |
| Agent identities | **No change** |

**Validation:** Skills swap, everything else stays. The architecture's composability
claim is validated.

---

## 7. File System Layout

All migration-specific assets live under `.copilot/`:

```
.copilot/
├── agents/
│   └── migration-orchestrator.agent.md       ← NEW (Phase 0 of docs)
├── skills/
│   ├── legacy-analysis.skill.md              ← NEW (§2.1)
│   ├── migration-planning.skill.md           ← NEW (§2.2)
│   ├── clean-architecture.skill.md           ← NEW (§2.3)
│   ├── micronaut-testing.skill.md            ← NEW (§2.4)
│   ├── migration-checklist.skill.md          ← NEW (§2.5)
│   ├── migration-documentation.skill.md      ← NEW (§2.6)
│   ├── java.skill.md                         ← EXISTING
│   ├── micronaut.skill.md                    ← EXISTING
│   ├── junit5.skill.md                       ← EXISTING
│   └── rest-api.skill.md                     ← EXISTING
├── prompt-library/
│   └── api-migration.prompt.md               ← NEW (§4)
└── knowledge/
    ├── legacy-codebase/                      ← Dynamic (per migration run)
    └── migration-decisions/                  ← Dynamic (per migration run)
```
