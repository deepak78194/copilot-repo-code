# 03 — Migration Planner Design: From Discovery to Approvable Plan

> **Purpose:** Define the structure of the Migration Plan (Phase 2 output), the Planner's reasoning strategy when equipped with migration skills, the Approval Gate design, and the Innovation Budget that allows the Planner to suggest improvements without overstepping bounded autonomy.

---

## 1. The Planner's Dual Role in Migration

In the migration workflow, the Planner operates twice — once during Discovery
(Phase 1) and once during Planning (Phase 2). The agent is identical; only the
skills and prompt change:

| Phase | Skills Loaded | Output | Reasoning Mode |
|-------|--------------|--------|---------------|
| Discovery | `legacy-analysis.skill.md`, `clean-architecture.skill.md` | Discovery Report | Reverse engineering: *"What does this system do?"* |
| Planning | `migration-planning.skill.md`, `clean-architecture.skill.md`, `micronaut.skill.md` | Migration Plan | Forward design: *"How should the new system be structured?"* |

The transition from Discovery to Planning is a **perspective shift**, not an
agent swap. The Planner stops reading the legacy workspace and starts designing
the modern one — using the Discovery Report as its primary input.

---

## 2. Migration Plan Structure

The Migration Plan is the most important artifact in the workflow. It determines:
- What gets built (scope)
- How it's organized (architecture)
- In what order (sequencing)
- What risks exist (awareness)

### 2.1 — Required Sections

```
MIGRATION PLAN
==============

1. API REDESIGN
   Maps legacy endpoints to modern API design.
   Not a 1:1 copy — applies REST conventions, versioning, proper status codes.

2. CLEAN ARCHITECTURE MAPPING
   Maps legacy components to clean architecture layers.
   Defines use cases, ports, adapters, and domain entities.

3. DEPENDENCY RESOLUTION
   Maps legacy external dependencies to Micronaut equivalents.
   JMS → Micronaut Messaging, JNDI → Configuration injection, etc.

4. DATA ACCESS MIGRATION
   Maps legacy JPA/EntityManager patterns to Micronaut Data.
   Addresses native SQL, stored procedures, locking strategies.

5. TASK BREAKDOWN
   Ordered list of implementation tasks with dependencies.
   Each task produces a verifiable artifact.

6. RISK REGISTER
   Expanded from Discovery's preliminary register.
   Each risk has: severity, mitigation strategy, owner.

7. OPTIMIZATION SUGGESTIONS (Innovation Budget)
   Improvements the Planner recommends beyond 1:1 migration.
   Clearly labeled. Subject to human approval.
```

---

## 3. Section Details

### 3.1 — API Redesign

The Planner does not transliterate the legacy API. It redesigns it following
modern conventions while preserving behavioral equivalence.

**Input:** Endpoint inventory from Discovery Report (§3.1 of [02 — Discovery](02-dual-workspace-discovery.md))

**Transformation rules** (from `migration-planning.skill.md`):

| Legacy Pattern | Modern Pattern | Rationale |
|---------------|---------------|-----------|
| `/api/order/getAll` | `GET /api/v1/orders` | Plural nouns, no verbs in paths |
| `/api/order/getById?id=42` | `GET /api/v1/orders/42` | Path parameters for resource identity |
| Response: raw entity | Response: DTO with selected fields | Never expose persistence model |
| Status 200 for everything | 201 for creates, 204 for deletes, 422 for business rules | Correct HTTP semantics |
| Error: `{"error": "not found"}` | Error: `{"code": "ORDER_NOT_FOUND", "message": "No order with id 42 exists."}` | Structured error DTOs |
| No versioning | `/api/v1/...` prefix | API versioning from day one |

**Output structure:**

```yaml
api_redesign:
  - legacy_endpoint: "EP-001: POST /api/orders"
    modern_endpoint: "POST /api/v1/orders"
    changes:
      - "Added /v1/ version prefix"
      - "Response status: 200 → 201 Created"
      - "Response body: Order entity → OrderResponse DTO"
      - "Added Location header with created resource URI"
    behavioral_equivalence: "Same business logic, improved HTTP semantics"

  - legacy_endpoint: "EP-004: DELETE /api/orders/{id}"
    modern_endpoint: "DELETE /api/v1/orders/{id}"
    changes:
      - "Response status: 200 → 204 No Content"
      - "Response body: deleted entity → empty body"
    behavioral_equivalence: "Same deletion logic, correct DELETE semantics"
```

### 3.2 — Clean Architecture Mapping

This is the architectural core of the migration plan. Each legacy component
maps to one or more clean architecture layers.

**The Dependency Rule:** Dependencies point inward. Domain knows nothing about
infrastructure. Use cases know domain but not infrastructure. Only adapters
know external frameworks.

```
┌─────────────────────────────────────────────────────────┐
│                     INFRASTRUCTURE                       │
│  Inbound Adapters    ←──────→    Outbound Adapters      │
│  (Controllers)                   (Repository Impls)      │
│  (Event Listeners)               (HTTP Clients)          │
│  (Scheduled Tasks)               (Message Publishers)    │
├─────────────────────────────────────────────────────────┤
│                      APPLICATION                         │
│  Use Cases / Interactors                                 │
│  Inbound Ports (interfaces)                              │
│  Outbound Ports (interfaces)                             │
├─────────────────────────────────────────────────────────┤
│                        DOMAIN                            │
│  Entities    Value Objects    Domain Events               │
│  Domain Services    Aggregates                            │
└─────────────────────────────────────────────────────────┘
```

**Mapping structure:**

```yaml
clean_architecture_mapping:
  domain:
    entities:
      - name: "Order"
        source: "com.legacy.Order (JPA entity)"
        changes:
          - "Remove JPA annotations (domain entity is framework-free)"
          - "Add factory method: Order.create(customerId, items)"
          - "Add domain behavior: Order.cancel(), Order.addItem()"
          - "Status managed via enum with allowed transitions"
    value_objects:
      - name: "OrderNumber"
        source: "String field in legacy Order"
        rationale: "Encapsulates generation logic and format validation"
    domain_events:
      - name: "OrderCreatedEvent"
        source: "JMS message in OrderService.createOrder()"
        rationale: "Explicit domain event replaces infrastructure coupling"

  application:
    use_cases:
      - name: "CreateOrderUseCase"
        inbound_port: "CreateOrderPort"
        outbound_ports: ["SaveOrderPort", "CheckInventoryPort", "PublishEventPort"]
        source_method: "OrderService.createOrder()"
        business_rules:
          - "Validate item availability"
          - "Calculate tax by region"
          - "Generate order number"
          - "Publish creation event"
        transaction_boundary: "Use case method is the TX boundary"

      - name: "GetOrderUseCase"
        inbound_port: "GetOrderPort"
        outbound_ports: ["FindOrderPort"]
        source_method: "OrderService.getOrder()"
        business_rules: []
        notes: "Simple delegation — still worth a use case for consistency"

    inbound_ports:
      - name: "CreateOrderPort"
        method: "createOrder(CreateOrderCommand): OrderId"
        notes: "Command object, not entity. Returns ID, not full entity."

    outbound_ports:
      - name: "SaveOrderPort"
        method: "save(Order): Order"
        implementor: "JpaOrderAdapter (infrastructure)"

      - name: "CheckInventoryPort"
        method: "checkAvailability(List<ItemId>): AvailabilityResult"
        implementor: "InventoryClientAdapter (infrastructure)"

  infrastructure:
    inbound_adapters:
      - name: "OrderController"
        type: "Micronaut @Controller"
        maps_to: "CreateOrderPort, GetOrderPort, ..."
        framework_specific: true

    outbound_adapters:
      - name: "JpaOrderAdapter"
        type: "Micronaut Data @Repository wrapper"
        implements: "SaveOrderPort, FindOrderPort"
        technology: "Micronaut Data JDBC"

      - name: "InventoryClientAdapter"
        type: "Micronaut Declarative HTTP Client"
        implements: "CheckInventoryPort"
        replaces: "Direct JAX-RS Client in legacy"
```

### 3.3 — Dependency Resolution

```yaml
dependency_resolution:
  - legacy: "JMS 2.0 (ActiveMQ via JNDI)"
    modern: "Micronaut Messaging (RabbitMQ or Kafka)"
    migration_path: "Create PublishEventPort interface. Implement with Micronaut Messaging adapter."
    risk: "MEDIUM — message format compatibility"

  - legacy: "JavaMail (SMTP via JNDI)"
    modern: "Micronaut Email"
    migration_path: "Create SendEmailPort interface. Implement with Micronaut Email adapter."
    risk: "LOW — standard SMTP"

  - legacy: "JAX-RS Client (REST calls)"
    modern: "Micronaut Declarative HTTP Client (@Client)"
    migration_path: "Define client interface. Annotate with @Client. Inject via constructor."
    risk: "LOW — well-supported migration path"

  - legacy: "JNDI DataSource lookup"
    modern: "Micronaut DataSource configuration (application.yml)"
    migration_path: "Configure in application.yml. Inject DataSource via constructor."
    risk: "LOW — configuration change only"
```

### 3.4 — Data Access Migration

```yaml
data_access_migration:
  strategy: "Micronaut Data JDBC"
  rationale: "Lighter than JPA. Compile-time query validation. Better Micronaut integration."

  entity_mapping:
    - legacy: "com.legacy.Order (@Entity, JPA)"
      modern: "com.example.order.infrastructure.persistence.OrderJpaEntity (@MappedEntity)"
      notes: "Infrastructure entity, NOT domain entity. Maps to/from domain Order."

  repository_mapping:
    - legacy: "OrderDAO (EntityManager-based)"
      modern: "OrderRepository (Micronaut Data @JdbcRepository)"
      queries:
        - legacy: "JPQL: SELECT o FROM Order o WHERE o.status = :status"
          modern: "Method: findByStatus(OrderStatus status)"
          type: "Derived query (Micronaut Data generates SQL)"
        - legacy: "Native: SELECT * FROM orders WHERE created_at < NOW() - INTERVAL '7 days'"
          modern: "@Query(\"SELECT * FROM orders WHERE created_at < :cutoff\")"
          notes: "Parameterized. Calculate cutoff in use case. DB-agnostic."

  locking_migration:
    - legacy: "Optimistic locking via @Version"
      modern: "Micronaut Data @Version support"
      risk: "LOW — directly supported"
    - legacy: "Pessimistic locking via LockModeType.PESSIMISTIC_WRITE"
      modern: "@Query with FOR UPDATE clause"
      risk: "MEDIUM — requires explicit SQL"
```

### 3.5 — Task Breakdown

The task breakdown is the Implementer's roadmap. Tasks are ordered by dependency
and grouped by clean architecture layer — **domain first, infrastructure last**
(following the dependency rule).

```yaml
task_breakdown:
  # Domain layer first — no external dependencies
  - id: "T-001"
    layer: "domain"
    task: "Create Order domain entity with factory method and behavior"
    depends_on: []
    verifiable: "Unit tests for Order.create(), Order.cancel(), status transitions"

  - id: "T-002"
    layer: "domain"
    task: "Create OrderNumber value object with generation and validation"
    depends_on: []
    verifiable: "Unit tests for format validation and uniqueness contract"

  - id: "T-003"
    layer: "domain"
    task: "Create OrderCreatedEvent domain event"
    depends_on: ["T-001"]
    verifiable: "Event carries required data (orderId, timestamp)"

  # Application layer — depends on domain
  - id: "T-004"
    layer: "application"
    task: "Define inbound ports (CreateOrderPort, GetOrderPort, etc.)"
    depends_on: ["T-001"]
    verifiable: "Interfaces compile. Method signatures match the plan."

  - id: "T-005"
    layer: "application"
    task: "Define outbound ports (SaveOrderPort, CheckInventoryPort, etc.)"
    depends_on: ["T-001"]
    verifiable: "Interfaces compile. No framework imports."

  - id: "T-006"
    layer: "application"
    task: "Implement CreateOrderUseCase"
    depends_on: ["T-001", "T-003", "T-004", "T-005"]
    verifiable: "Unit test with mocked outbound ports. Business rules tested."

  - id: "T-007"
    layer: "application"
    task: "Implement GetOrderUseCase, UpdateOrderUseCase, DeleteOrderUseCase"
    depends_on: ["T-004", "T-005"]
    verifiable: "Unit tests for each use case"

  # Infrastructure layer — depends on application
  - id: "T-008"
    layer: "infrastructure"
    task: "Create OrderJpaEntity and Flyway migration"
    depends_on: ["T-001"]
    verifiable: "Migration runs. Entity maps to schema."

  - id: "T-009"
    layer: "infrastructure"
    task: "Implement OrderRepository (outbound adapter for SaveOrderPort)"
    depends_on: ["T-005", "T-008"]
    verifiable: "Integration test with Testcontainers"

  - id: "T-010"
    layer: "infrastructure"
    task: "Implement OrderController (inbound adapter)"
    depends_on: ["T-004", "T-006", "T-007"]
    verifiable: "Integration test for each endpoint"

  - id: "T-011"
    layer: "infrastructure"
    task: "Implement InventoryClientAdapter (outbound adapter)"
    depends_on: ["T-005"]
    verifiable: "Test with WireMock or Micronaut test HTTP client"

  - id: "T-012"
    layer: "infrastructure"
    task: "Implement EventPublisherAdapter (outbound adapter)"
    depends_on: ["T-003", "T-005"]
    verifiable: "Test with in-memory message broker"

  - id: "T-013"
    layer: "infrastructure"
    task: "Create GlobalExceptionHandler"
    depends_on: ["T-010"]
    verifiable: "Integration test: trigger each exception, verify response format"
```

### 3.6 — Risk Register

```yaml
risk_register:
  - id: "R-001"
    severity: "HIGH"
    description: "Bean-managed transactions in BatchProcessingService"
    impact: "Cannot use @Transactional. Requires manual TX management."
    mitigation: "Implement batch processing as a separate bounded context with explicit TX control."
    owner: "Team lead approval required for approach"

  - id: "R-002"
    severity: "HIGH"
    description: "Native SQL with database-specific syntax"
    impact: "Queries may not work with different database or Micronaut Data"
    mitigation: "Rewrite as parameterized queries. Push date calculation to application layer."
    owner: "Implementer (with Reviewer validation)"

  - id: "R-003"
    severity: "MEDIUM"
    description: "Pessimistic locking in InventoryService"
    impact: "Micronaut Data doesn't have native pessimistic lock support"
    mitigation: "Use @Query with FOR UPDATE clause. Document the pattern."
    owner: "Implementer"

  - id: "R-004"
    severity: "LOW"
    description: "JNDI-dependent lookups for external services"
    impact: "JNDI does not exist in Micronaut"
    mitigation: "Replace all JNDI lookups with configuration-injected beans"
    owner: "Implementer"
```

---

## 4. The Approval Gate (G2)

### 4.1 — Presentation Format

The Orchestrator presents the plan to the user in a structured format that
enables informed review:

```
═══════════════════════════════════════════════════
        MIGRATION PLAN — READY FOR REVIEW
═══════════════════════════════════════════════════

📋 SCOPE:
   - 5 legacy endpoints → 5 modern endpoints
   - 1 service → 4 use cases + 6 ports + 4 adapters
   - 1 entity → 1 domain entity + 1 persistence entity + 1 value object
   - 13 implementation tasks ordered by dependency

🏗️ ARCHITECTURE:
   - Clean architecture with ports-and-adapters
   - Domain layer: Order, OrderNumber, OrderCreatedEvent
   - Application layer: 4 use cases, 2 inbound ports, 3 outbound ports
   - Infrastructure: Micronaut controllers, Data repositories, HTTP clients

⚠️ RISKS:
   - 2 HIGH (bean-managed TX, native SQL)
   - 1 MEDIUM (pessimistic locking)
   - 1 LOW (JNDI replacement)

💡 OPTIMIZATION SUGGESTIONS (3):
   - [OPT-1] Introduce OrderNumber value object (not in legacy)
   - [OPT-2] Replace entity-as-response with dedicated DTOs
   - [OPT-3] Add domain events for cross-service communication

═══════════════════════════════════════════════════
   Reply APPROVE to proceed with implementation.
   Reply REVISE with specific feedback to modify.
═══════════════════════════════════════════════════
```

### 4.2 — Approval Semantics

| User Response | Orchestrator Action |
|--------------|-------------------|
| `APPROVE` | Set G2 = PASSED. Transition to Phase 3. Lock the plan. |
| `APPROVE with notes: "skip OPT-3"` | Set G2 = PASSED. Remove OPT-3 from plan. Lock. |
| `REVISE: "split OrderService into two bounded contexts"` | Return plan to Planner with feedback. Re-run Phase 2. |
| `REJECT` | Terminate workflow. Log reason. |
| (silence / no response) | G2 remains CLOSED. Orchestrator waits. No timeout. |

### 4.3 — Plan Immutability After Approval

Once approved, the plan is **locked**. The Implementer follows the plan
exactly. If the Implementer discovers that the plan is infeasible during
Phase 3, it does not modify the plan — it escalates to the Orchestrator,
which presents the issue to the user for a plan revision.

This prevents the "scope creep" problem where the Implementer quietly
changes architectural decisions during code generation.

---

## 5. Innovation Budget

The Innovation Budget (from [07 — Freshness](../architecture/07-freshness-and-bounded-autonomy.md) §5)
defines how many optimization suggestions the Planner may include beyond
strict 1:1 migration.

### 5.1 — Budget Configuration

```yaml
innovation_budget:
  max_suggestions: 5
  suggestion_types:
    allowed:
      - "New value objects not in legacy"
      - "Domain events replacing infrastructure coupling"
      - "DTO patterns replacing entity exposure"
      - "Query optimization replacing N+1 patterns"
      - "Error handling improvements"
    forbidden:
      - "New features not present in legacy"
      - "Technology changes beyond the target stack"
      - "Architectural patterns not in the plan template"
      - "Performance optimizations without evidence"
```

### 5.2 — Suggestion Format

Each suggestion must be:

1. **Labeled** — Clearly marked as `[OPTIMIZATION]` or `[SUGGESTION]`
2. **Justified** — Why is this better than the legacy pattern?
3. **Scoped** — What tasks does it add or modify?
4. **Optional** — The user can reject it without affecting the core migration.
5. **Independently removable** — Removing one suggestion doesn't break others.

```yaml
optimization_suggestions:
  - id: "OPT-001"
    type: "OPTIMIZATION"
    title: "Introduce OrderNumber value object"
    justification: >
      Legacy uses a plain String for order numbers. A value object
      encapsulates format validation and generation logic, preventing
      invalid order numbers from entering the domain.
    tasks_added: ["T-002 (new)"]
    tasks_modified: ["T-001 (uses OrderNumber instead of String)"]
    removable: true
    impact_if_removed: "Order uses String for order number (same as legacy)"
```

---

## 6. Handling Plan Revisions

When the user responds with `REVISE`, the plan re-enters Phase 2. The
Orchestrator provides the Planner with:

1. The current plan (as context)
2. The user's feedback (as the task)
3. The Discovery Report (unchanged)

The Planner produces a revised plan. The Orchestrator compares it with the
previous version and highlights changes for the user.

```
═══════════════════════════════════════════════════
        REVISED MIGRATION PLAN — DIFF VIEW
═══════════════════════════════════════════════════

CHANGES FROM v1 → v2:
  [ADDED]   Bounded context split: OrderContext + InventoryContext
  [MODIFIED] Task T-006: now scoped to OrderContext only
  [ADDED]   Tasks T-014 through T-019: InventoryContext implementation
  [MODIFIED] Risk R-003: severity upgraded HIGH (split affects locking strategy)
  [REMOVED] OPT-003: domain events (user rejected)

UNCHANGED: API redesign, data access migration, error handling

═══════════════════════════════════════════════════
   Reply APPROVE to proceed with implementation.
   Reply REVISE with specific feedback to modify.
═══════════════════════════════════════════════════
```

**Maximum revisions:** The Orchestrator allows up to **3 revision cycles** before
suggesting that the planning scope may be too large and recommending decomposition
into smaller migration batches.
