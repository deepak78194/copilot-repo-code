# 02 — Dual-Workspace Discovery: Analyzing Legacy and Modern Codebases

> **Purpose:** Define the complete Discovery phase (Phase 1) — the protocols for analyzing the legacy Jakarta EE workspace, sensing the modern target workspace, serializing findings into structured reports, and ensuring the Planner has sufficient context to design an accurate migration plan.

---

## 1. Why Discovery Is a Separate Phase

Discovery is the most overlooked phase in migration workflows. Teams jump
directly to "rewrite the controller" without understanding:

- What the legacy endpoint *actually does* (not what the documentation says)
- Which services are stateful vs. stateless
- Which data access patterns use transactions, optimistic locking, or stored procedures
- Which error handling patterns exist (and which should be preserved vs. redesigned)
- What external dependencies exist (JNDI lookups, remote EJBs, message queues)

Discovery must be a **separate, gated phase** because:

1. **Accuracy depends on completeness.** A partial discovery leads to a partial plan,
   which leads to a broken implementation.
2. **The Planner needs a stable input.** If discovery and planning happen simultaneously,
   the plan is built on shifting ground.
3. **The report is a referenceable artifact.** Later phases (implementation, verification)
   refer back to the discovery report for behavioral equivalence checks.

---

## 2. The Dual-Workspace Model

Migration operates across two workspaces simultaneously:

```
┌──────────────────────────────┐     ┌──────────────────────────────┐
│      LEGACY WORKSPACE        │     │      MODERN WORKSPACE        │
│                              │     │                              │
│  Jakarta EE / Java EE app    │     │  Micronaut clean-arch target │
│                              │     │                              │
│  ┌────────┐  ┌────────────┐  │     │  ┌────────┐  ┌──────────┐   │
│  │ EJBs   │  │ Servlets   │  │     │  │ build  │  │ src/     │   │
│  │        │  │ JSPs       │  │     │  │.gradle │  │ main/    │   │
│  └────────┘  └────────────┘  │     │  │.kts    │  │ test/    │   │
│  ┌────────┐  ┌────────────┐  │     │  └────────┘  └──────────┘   │
│  │ JPA    │  │ web.xml    │  │     │                              │
│  │Entities│  │ ejb-jar.xml│  │     │  May be empty or scaffolded  │
│  └────────┘  └────────────┘  │     │                              │
│  ┌────────────────────────┐  │     │  ┌────────────────────────┐  │
│  │ persistence.xml        │  │     │  │ application.yml        │  │
│  │ datasource configs     │  │     │  │ (target config)        │  │
│  └────────────────────────┘  │     │  └────────────────────────┘  │
└──────────────────────────────┘     └──────────────────────────────┘
         READ-ONLY                          READ-WRITE
```

**Key rule:** The legacy workspace is **read-only** during the entire workflow.
The agent never modifies legacy code. It reads, analyzes, and extracts requirements.

---

## 3. Legacy Analysis Protocol

The Planner, equipped with `legacy-analysis.skill.md`, follows a structured
protocol to analyze the legacy workspace. The protocol is organized by concern,
not by file type, because a single file may contain multiple concerns.

### 3.1 — Endpoint Discovery

**Goal:** Catalog every HTTP endpoint exposed by the legacy application.

**Signals to scan:**

| Signal | Pattern | Extracted Information |
|--------|---------|---------------------|
| JAX-RS annotations | `@Path`, `@GET`, `@POST`, `@PUT`, `@DELETE`, `@PATCH` | Path, HTTP method, path parameters |
| Servlet mappings | `web.xml` `<servlet-mapping>` entries | URL patterns, servlet classes |
| Filter chains | `@WebFilter`, `web.xml` `<filter-mapping>` | Applied filters per URL pattern |
| Custom annotations | Project-specific routing annotations | Custom routing logic |
| WSDL/SOAP endpoints | `@WebService`, `@WebMethod` | SOAP operations (flag for special handling) |

**Output structure:**

```yaml
endpoints:
  - path: "/api/orders"
    method: GET
    handler_class: "com.legacy.OrderResource"
    handler_method: "getOrders()"
    path_params: []
    query_params: ["status", "page", "size"]
    request_body: null
    response_type: "List<OrderDTO>"
    authentication: "ROLE_USER"
    notes: "Paginated. Uses custom pagination utility."

  - path: "/api/orders/{id}"
    method: PUT
    handler_class: "com.legacy.OrderResource"
    handler_method: "updateOrder(Long, OrderUpdateRequest)"
    path_params: ["id"]
    query_params: []
    request_body: "OrderUpdateRequest"
    response_type: "OrderDTO"
    authentication: "ROLE_ADMIN"
    notes: "Optimistic locking via @Version. Sends JMS event on success."
```

### 3.2 — Service Logic Analysis

**Goal:** Understand the business logic behind each endpoint.

**What to extract:**

| Concern | What to Catalog | Why It Matters |
|---------|----------------|---------------|
| **Business rules** | Validation logic, state transitions, calculations | Must be preserved in use cases |
| **Orchestration** | How services call other services | Reveals aggregate boundaries |
| **Side effects** | Email sends, event publishing, audit logging | Must be explicit in new design |
| **Conditional logic** | Feature flags, tenant-specific behavior | Must be modeled as explicit strategies |
| **Transaction boundaries** | `@TransactionAttribute`, programmatic TXN management | Clean arch must respect these boundaries |

**Output structure:**

```yaml
services:
  - class: "com.legacy.OrderService"
    type: "Stateless EJB"
    methods:
      - name: "createOrder"
        business_rules:
          - "Validates item availability before persisting"
          - "Calculates tax based on customer region"
          - "Generates order number using sequence"
        side_effects:
          - "Publishes OrderCreatedEvent to JMS queue"
          - "Sends confirmation email via SMTP"
        transaction: "REQUIRED (container-managed)"
        calls:
          - "InventoryService.checkAvailability()"
          - "TaxCalculator.calculate()"
          - "OrderRepository.save()"
        error_handling:
          - exception: "InsufficientInventoryException"
            action: "Rolls back transaction, returns 409"
          - exception: "InvalidRegionException"
            action: "Returns 422 with error details"
```

### 3.3 — Data Access Analysis

**Goal:** Map how the legacy application accesses and persists data.

**Patterns to identify:**

| Pattern | Signals | Migration Implication |
|---------|---------|----------------------|
| JPA/Hibernate entities | `@Entity`, `@Table`, `@Column` annotations | Map to Micronaut Data entities |
| Named queries | `@NamedQuery`, `orm.xml` queries | Convert to repository methods |
| Native SQL | `createNativeQuery()`, embedded SQL strings | Evaluate: convert to JPQL or keep as native |
| Stored procedures | `@NamedStoredProcedureQuery`, `StoredProcedureQuery` | Flag for special migration path |
| Optimistic locking | `@Version` field | Preserve in new entities |
| Audit columns | `@CreatedDate`, `@LastModifiedDate`, custom interceptors | Map to Micronaut Data audit |
| Multi-tenancy | Schema-per-tenant, discriminator columns | Design explicit tenant strategy |

**Output structure:**

```yaml
data_access:
  entities:
    - class: "com.legacy.Order"
      table: "orders"
      columns: ["id", "order_number", "customer_id", "status", "total", "created_at", "version"]
      relationships:
        - type: "OneToMany"
          target: "OrderItem"
          cascade: "ALL"
          fetch: "LAZY"
      locking: "OPTIMISTIC (@Version)"
      audit: "created_at managed by @PrePersist interceptor"

  repositories:
    - class: "com.legacy.OrderDAO"
      type: "EntityManager-based (injected via @PersistenceContext)"
      queries:
        - name: "findByStatus"
          type: "JPQL"
          query: "SELECT o FROM Order o WHERE o.status = :status"
        - name: "findOverdueOrders"
          type: "NATIVE SQL"
          query: "SELECT * FROM orders WHERE status = 'PENDING' AND created_at < NOW() - INTERVAL '7 days'"
          note: "Database-specific syntax — needs migration"
```

### 3.4 — External Dependencies Analysis

**Goal:** Identify all external systems the legacy application communicates with.

```yaml
external_dependencies:
  - type: "JMS Queue"
    identifier: "java:/jms/queue/OrderEvents"
    usage: "OrderService publishes OrderCreatedEvent, OrderCancelledEvent"
    protocol: "JMS 2.0 via ActiveMQ"
    migration_notes: "Consider Micronaut Messaging with RabbitMQ/Kafka adapter"

  - type: "SMTP"
    identifier: "java:/mail/NotificationSession"
    usage: "EmailService sends transactional emails"
    protocol: "JavaMail via JNDI lookup"
    migration_notes: "Replace JNDI lookup with Micronaut email configuration"

  - type: "LDAP"
    identifier: "java:/ldap/CompanyDirectory"
    usage: "UserService authenticates against corporate directory"
    protocol: "JNDI LDAP context"
    migration_notes: "Replace with Micronaut Security LDAP integration"

  - type: "REST Client"
    identifier: "https://payment-gateway.internal/api/v2"
    usage: "PaymentService calls external payment processor"
    protocol: "JAX-RS Client or HttpURLConnection"
    migration_notes: "Replace with Micronaut Declarative HTTP Client"
```

### 3.5 — Transaction and Concurrency Analysis

**Goal:** Map transaction boundaries, isolation levels, and concurrency controls.

```yaml
transactions:
  - service: "OrderService"
    method: "createOrder"
    type: "CONTAINER_MANAGED"
    attribute: "REQUIRED"
    isolation: "READ_COMMITTED (default)"
    propagation_notes: "Calls InventoryService within same TX boundary"

  - service: "BatchProcessingService"
    method: "processEndOfDay"
    type: "BEAN_MANAGED"
    notes: "Manually manages UserTransaction for batch commits every 100 records"
    migration_risk: "HIGH — bean-managed TX requires explicit migration"

concurrency:
  - pattern: "Optimistic locking"
    entities: ["Order", "Inventory"]
    mechanism: "@Version field"

  - pattern: "Pessimistic locking"
    location: "InventoryService.reserveStock()"
    mechanism: "LockModeType.PESSIMISTIC_WRITE"
    notes: "Used during stock reservation to prevent overselling"
```

### 3.6 — Error Handling Analysis

**Goal:** Catalog how errors are handled and communicated to clients.

```yaml
error_handling:
  global_handler:
    type: "JAX-RS ExceptionMapper"
    class: "com.legacy.GlobalExceptionMapper"
    mappings:
      - exception: "UserNotFoundException"
        status: 404
        body: '{"error": "User not found"}'
      - exception: "DuplicateEmailException"
        status: 409
        body: '{"error": "Email already exists"}'
      - exception: "ConstraintViolationException"
        status: 400
        body: "Raw Hibernate validation messages (needs improvement)"
      - exception: "Exception"
        status: 500
        body: '{"error": "Internal server error"}'
        notes: "Catches all — some specific exceptions may be lost"

  patterns_found:
    - "Mix of structured and unstructured error responses"
    - "Some endpoints return stack traces in development mode"
    - "No correlation IDs in error responses"
    - "Inconsistent HTTP status code usage (some use 400 for 'not found')"
```

---

## 4. Modern Workspace Analysis

The modern workspace may be empty (greenfield) or partially scaffolded. Either
way, the agent must understand what exists before generating code.

### 4.1 — Scaffolding Detection

Apply the context detection pipeline from
[06 — Context Detection](../architecture/06-context-detection-and-adaptation.md) §2
to the modern workspace:

```yaml
modern_workspace:
  build_system:
    detected: "Gradle (Kotlin DSL)"
    confidence: HIGH
    file: "build.gradle.kts"

  framework:
    detected: "Micronaut 4.x"
    confidence: HIGH
    signal: "io.micronaut:micronaut-http-server-netty in dependencies"

  language:
    detected: "Java 21"
    confidence: HIGH
    signal: "sourceCompatibility = JavaLanguageVersion.of(21)"

  existing_structure:
    packages: ["com.example.order"]
    existing_code: false
    test_framework: "JUnit 5 + Micronaut Test"
    database: "Micronaut Data JDBC (configured, no entities yet)"
    migration_tool: "Flyway (configured, V1 migration exists)"

  configuration:
    application_yml: true
    profiles: ["default", "test"]
    datasource_configured: true
```

### 4.2 — Clean Architecture Readiness

Check whether the modern workspace has clean architecture scaffolding:

```yaml
clean_architecture:
  structure_detected: false
  recommended_layout:
    domain:
      entities: "com.example.order.domain.entity"
      value_objects: "com.example.order.domain.vo"
    application:
      use_cases: "com.example.order.application.usecase"
      ports_inbound: "com.example.order.application.port.inbound"
      ports_outbound: "com.example.order.application.port.outbound"
    infrastructure:
      adapters_inbound: "com.example.order.infrastructure.adapter.inbound"
      adapters_outbound: "com.example.order.infrastructure.adapter.outbound"
      persistence: "com.example.order.infrastructure.persistence"
      configuration: "com.example.order.infrastructure.config"
```

---

## 5. The Discovery Report

The complete output of Phase 1 is a **Discovery Report** — a structured document
that becomes the primary input for the Planning phase (Phase 2).

### 5.1 — Report Structure

```
DISCOVERY REPORT
================

METADATA:
  generated_at: "2026-03-01T10:30:00Z"
  legacy_workspace: "/path/to/legacy-app"
  modern_workspace: "/path/to/modern-app"
  legacy_framework: "Jakarta EE 10 / WildFly 30"
  target_framework: "Micronaut 4.x / Clean Architecture"

SECTIONS:
  1. ENDPOINT INVENTORY          (§3.1)
  2. SERVICE LOGIC MAP           (§3.2)
  3. DATA ACCESS MAP             (§3.3)
  4. EXTERNAL DEPENDENCIES       (§3.4)
  5. TRANSACTION MAP             (§3.5)
  6. ERROR HANDLING MAP          (§3.6)
  7. MODERN WORKSPACE STATE      (§4)
  8. RISK REGISTER (preliminary)

RISK REGISTER (preliminary):
  - HIGH: Bean-managed transactions in BatchProcessingService
  - HIGH: Native SQL queries with database-specific syntax
  - MEDIUM: Pessimistic locking in InventoryService
  - MEDIUM: JNDI-dependent external service lookups
  - LOW: Standard JPA entity mappings
```

### 5.2 — Report Completeness Validation (Gate G1)

The Migration Orchestrator validates the Discovery Report before allowing
progression to Phase 2:

```
GATE G1 VALIDATION:
  required_sections:
    - endpoint_inventory:    MIN 1 endpoint documented
    - service_logic_map:     MIN 1 service with business rules
    - data_access_map:       MIN 1 entity and MIN 1 repository
    - external_dependencies: Present (may be empty with explanation)
    - transaction_map:       Present (may be empty if no TX concerns)
    - error_handling_map:    Present
    - modern_workspace:      Build system and framework detected
    - risk_register:         Present with at least one entry

  validation_result:
    all_present: TRUE → proceed to Phase 2
    missing_sections:
      - LOG: "Discovery Report missing: {sections}"
      - ACTION: Return to Planner with instruction to complete missing sections
      - MAX_RETRIES: 2
      - ON_EXHAUSTION: Escalate to human with partial report
```

---

## 6. Context Serialization

The Discovery Report must be serialized in a format that subsequent agents can
consume efficiently. This is the **Context Object** pattern from
[06 — Context Detection](../architecture/06-context-detection-and-adaptation.md) §4.

### 6.1 — Serialization Principles

1. **Structured over narrative.** Use YAML/JSON-like structures, not prose paragraphs.
   Agents parse structured data more reliably than narrative descriptions.

2. **Complete but bounded.** Include all facts, but don't include the legacy source
   code itself. Point to files; don't inline them.

3. **Cross-referenceable.** Use consistent identifiers so that the Planning phase
   can reference Discovery findings by ID:
   ```
   # In Discovery Report
   endpoints:
     - id: "EP-001"
       path: "/api/orders"

   # In Migration Plan
   tasks:
     - id: "T-003"
       description: "Create GetOrdersUseCase"
       source_endpoint: "EP-001"  ← cross-reference
   ```

4. **Version-stamped.** Include a generation timestamp so that stale reports can
   be detected if the legacy code changes mid-migration.

### 6.2 — Size Management

Large legacy applications may produce discovery reports that exceed practical
context window limits. Mitigation strategies:

| Strategy | When to Apply | Implementation |
|----------|--------------|----------------|
| **Service-level chunking** | > 20 services | Group by bounded context. Run discovery per context. |
| **Endpoint summarization** | > 50 endpoints | List paths and methods; detail only complex endpoints. |
| **Reference by file** | > 100 entities | List entities with table names; link to source files for details. |
| **Incremental discovery** | Very large apps | Discover and migrate one bounded context at a time. |

---

## 7. Worked Example: User Service Discovery

Using the reference `playground/user-service/` from this repository as a
simplified legacy source:

```yaml
DISCOVERY REPORT (simplified)
==============================

ENDPOINT INVENTORY:
  - id: "EP-001"
    path: "/api/v1/users"
    method: POST
    handler: "UserController.createUser(CreateUserRequest)"
    response: "UserResponse (201)"
    validation: "@Valid on request body"

  - id: "EP-002"
    path: "/api/v1/users/{id}"
    method: GET
    handler: "UserController.getUser(Long)"
    response: "UserResponse (200)"
    error: "UserNotFoundException → 404"

  - id: "EP-003"
    path: "/api/v1/users/{id}"
    method: PUT
    handler: "UserController.updateUser(Long, UpdateUserRequest)"
    response: "UserResponse (200)"
    error: "UserNotFoundException → 404, DuplicateEmailException → 409"

  - id: "EP-004"
    path: "/api/v1/users/{id}"
    method: DELETE
    handler: "UserController.deleteUser(Long)"
    response: "204 No Content"
    error: "UserNotFoundException → 404"

  - id: "EP-005"
    path: "/api/v1/users"
    method: GET
    handler: "UserController.getAllUsers()"
    response: "List<UserResponse> (200)"

SERVICE LOGIC MAP:
  - class: "UserService"
    methods:
      - name: "createUser"
        rules: ["Checks email uniqueness via repository"]
        side_effects: ["Persists user to database"]
        transaction: "Spring @Transactional"
        errors: ["DuplicateEmailException if email exists"]

DATA ACCESS MAP:
  entities:
    - class: "User"
      table: "users"
      columns: ["id (BIGINT, PK, GENERATED)", "name (VARCHAR)", "email (VARCHAR, UNIQUE)", "created_at (TIMESTAMP)"]
  repositories:
    - interface: "UserRepository extends JpaRepository<User, Long>"
      custom_queries:
        - "findByEmail(String) : Optional<User>"
        - "existsByEmail(String) : boolean"

ERROR HANDLING MAP:
  handler: "GlobalExceptionHandler (@RestControllerAdvice)"
  mappings:
    - "UserNotFoundException → 404 ProblemDetail"
    - "DuplicateEmailException → 409 ProblemDetail"
    - "MethodArgumentNotValidException → 400 ProblemDetail"

MODERN WORKSPACE STATE:
  framework: "Micronaut 4.x (not yet scaffolded)"
  target_package: "com.example.user"
```

This worked example shows how even a simple CRUD service produces a structured,
cross-referenceable discovery report that the Planning phase can consume directly.
