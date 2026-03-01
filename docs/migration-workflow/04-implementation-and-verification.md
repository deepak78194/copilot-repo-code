# 04 — Implementation & Verification: Code Generation with Feedback Loop

> **Purpose:** Define how Phases 3 (Implementation) and 4 (Verification) work together as a code-generation-and-validation cycle. Establish the Implementer's strategy for producing clean architecture code, the Verification Agent's feedback loop, build validation specific to Micronaut, and the bounded retry policy that prevents infinite loops.

---

## 1. Phase 3: Implementation

### 1.1 — Entry Conditions

Phase 3 begins when:
- Gate G2 (Plan Approved) has passed
- The Migration Plan is locked and immutable
- The Implementer receives the plan + discovery report + clean-arch skills + micronaut skills

### 1.2 — Implementation Strategy: Inside-Out

The Implementer follows the task breakdown from the Migration Plan, which is
ordered **inside-out** following the dependency rule:

```
IMPLEMENTATION ORDER:
  1. DOMAIN layer     ← No external dependencies. Pure Java.
  2. APPLICATION layer ← Depends on domain only. Defines ports.
  3. INFRASTRUCTURE   ← Depends on application. Framework-specific.

This order ensures:
  - Domain code compiles independently.
  - Application code compiles with domain on the classpath.
  - Infrastructure code compiles with everything on the classpath.
  - Each layer can be tested before the next is built.
```

### 1.3 — Code Generation per Layer

#### Domain Layer

The Implementer generates domain entities, value objects, and domain events.
These are **plain Java** — no Micronaut annotations, no JPA annotations,
no Spring annotations.

```java
// DOMAIN ENTITY — framework-free
public record Order(
    OrderId id,
    OrderNumber orderNumber,
    CustomerId customerId,
    List<OrderItem> items,
    OrderStatus status,
    Money total,
    Instant createdAt
) {
    public static Order create(CustomerId customerId, List<OrderItem> items) {
        // Domain logic: validate items, calculate total
        var total = items.stream()
            .map(OrderItem::subtotal)
            .reduce(Money.ZERO, Money::add);

        return new Order(
            OrderId.generate(),
            OrderNumber.generate(),
            customerId,
            List.copyOf(items),
            OrderStatus.CREATED,
            total,
            Instant.now()
        );
    }

    public Order cancel() {
        if (status != OrderStatus.CREATED && status != OrderStatus.PENDING) {
            throw new IllegalStateException(
                "Cannot cancel order in status: " + status
            );
        }
        return new Order(id, orderNumber, customerId, items,
            OrderStatus.CANCELLED, total, createdAt);
    }
}
```

**Key constraint:** The Implementer must not add framework annotations to domain
code. If `clean-architecture.skill.md` is loaded, this is enforced by the skill:

```
ANTI-PATTERN: Framework annotations in domain layer
  WRONG: @Entity, @Table, @MappedEntity on domain classes
  RIGHT: Domain classes are plain Java records or classes.
         Infrastructure entities are separate classes that map to/from domain.
```

#### Application Layer

The Implementer generates use cases, inbound ports, and outbound ports.

```java
// INBOUND PORT — interface
public interface CreateOrderPort {
    OrderId createOrder(CreateOrderCommand command);
}

// OUTBOUND PORT — interface
public interface SaveOrderPort {
    Order save(Order order);
}

// USE CASE — implements inbound port, depends on outbound ports
public class CreateOrderUseCase implements CreateOrderPort {

    private final SaveOrderPort saveOrderPort;
    private final CheckInventoryPort checkInventoryPort;
    private final PublishEventPort publishEventPort;

    public CreateOrderUseCase(
        SaveOrderPort saveOrderPort,
        CheckInventoryPort checkInventoryPort,
        PublishEventPort publishEventPort
    ) {
        this.saveOrderPort = saveOrderPort;
        this.checkInventoryPort = checkInventoryPort;
        this.publishEventPort = publishEventPort;
    }

    @Override
    public OrderId createOrder(CreateOrderCommand command) {
        // 1. Check inventory
        var availability = checkInventoryPort.checkAvailability(command.itemIds());
        if (!availability.allAvailable()) {
            throw new InsufficientInventoryException(availability.unavailableItems());
        }

        // 2. Create domain entity
        var order = Order.create(command.customerId(), command.items());

        // 3. Persist
        var saved = saveOrderPort.save(order);

        // 4. Publish domain event
        publishEventPort.publish(new OrderCreatedEvent(saved.id(), Instant.now()));

        return saved.id();
    }
}
```

**Key constraint:** Use cases depend only on ports (interfaces), never on
infrastructure implementations. Constructor injection only.

#### Infrastructure Layer

The Implementer generates framework-specific code: controllers, repository
implementations, HTTP clients, configuration.

```java
// INBOUND ADAPTER — Micronaut controller
@Controller("/api/v1/orders")
public class OrderController {

    private final CreateOrderPort createOrderPort;
    private final GetOrderPort getOrderPort;

    public OrderController(
        CreateOrderPort createOrderPort,
        GetOrderPort getOrderPort
    ) {
        this.createOrderPort = createOrderPort;
        this.getOrderPort = getOrderPort;
    }

    @Post
    @Status(HttpStatus.CREATED)
    public OrderResponse createOrder(@Body @Valid CreateOrderRequest request) {
        var command = CreateOrderCommand.from(request);
        var orderId = createOrderPort.createOrder(command);
        return getOrderPort.getOrder(orderId).toResponse();
    }
}

// OUTBOUND ADAPTER — Micronaut Data repository
@JdbcRepository(dialect = Dialect.POSTGRES)
public interface OrderMicronautRepository
    extends CrudRepository<OrderJpaEntity, Long> {

    Optional<OrderJpaEntity> findByOrderNumber(String orderNumber);

    @Query("SELECT * FROM orders WHERE status = :status")
    List<OrderJpaEntity> findByStatus(String status);
}

// ADAPTER WRAPPER — implements port, delegates to Micronaut repository
@Singleton
public class JpaOrderAdapter implements SaveOrderPort, FindOrderPort {

    private final OrderMicronautRepository repository;
    private final OrderEntityMapper mapper;

    public JpaOrderAdapter(
        OrderMicronautRepository repository,
        OrderEntityMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Order save(Order domainOrder) {
        var entity = mapper.toEntity(domainOrder);
        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }
}
```

### 1.4 — The Implementer's Constraints During Phase 3

```
IMPLEMENTER CONSTRAINTS (Phase 3):
  - Follow the approved Migration Plan exactly. Do not add tasks.
  - Generate code for each task in the plan's specified order.
  - Use only conventions from loaded skills.
  - If a task is infeasible, STOP and report to the Orchestrator.
    Do NOT attempt a workaround without plan revision.
  - Apply constructor injection everywhere.
  - Never expose domain entities through REST endpoints.
  - Never add framework annotations to domain or application layers.
```

---

## 2. Phase 4: Verification

### 2.1 — The Verification Feedback Loop

Phase 4 is not a single pass. It's a **feedback loop** that iterates until
the code is verified or the retry budget is exhausted.

```
┌─────────────────────────────────────────────────────────────────┐
│                    VERIFICATION LOOP                             │
│                                                                 │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐  │
│  │ GENERATE │───→│  BUILD   │───→│   RUN    │───→│ DIAGNOSE │  │
│  │  TESTS   │    │          │    │  TESTS   │    │          │  │
│  └──────────┘    └──────────┘    └──────────┘    └──────────┘  │
│       ▲               │              │               │          │
│       │          fail │         fail │          fix  │          │
│       │               ▼              ▼               ▼          │
│       │         ┌──────────┐   ┌──────────┐   ┌──────────┐     │
│       │         │ DIAGNOSE │   │ DIAGNOSE │   │   FIX    │     │
│       │         │ BUILD    │   │ TEST     │   │  CODE    │     │
│       │         │ ERRORS   │   │ FAILURES │   │          │     │
│       │         └──────────┘   └──────────┘   └──────────┘     │
│       │               │              │               │          │
│       └───────────────┴──────────────┴───────────────┘          │
│                          (retry)                                │
│                                                                 │
│  MAX ITERATIONS: 2                                              │
│  ON EXHAUSTION: Escalate to human with diagnostic report        │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 — Step 1: Generate Tests

The **Test Agent** generates tests for the implementation, organized by
clean architecture layer:

| Layer | Test Type | Test Strategy |
|-------|-----------|--------------|
| **Domain** | Unit tests | Test entity behavior, value object validation, state transitions. No mocks needed — domain is pure. |
| **Application** | Unit tests with mocked ports | Test use case logic. Mock outbound ports. Verify correct port calls. |
| **Infrastructure (adapters)** | Integration tests | Test repository implementations with Testcontainers. Test controllers with Micronaut test HTTP client. |
| **End-to-end** | Full stack tests | Start Micronaut app, call endpoints, verify responses. Minimal — follow testing pyramid. |

**Test naming convention:**

```java
@DisplayName("CreateOrderUseCase")
class CreateOrderUseCaseTest {

    @Test
    @DisplayName("should create order when all items are available")
    void createOrder_allItemsAvailable_createsAndPersistsOrder() { ... }

    @Test
    @DisplayName("should throw when inventory is insufficient")
    void createOrder_insufficientInventory_throwsException() { ... }

    @Test
    @DisplayName("should publish OrderCreatedEvent after persistence")
    void createOrder_successfulCreation_publishesEvent() { ... }
}
```

### 2.3 — Step 2: Build

The Implementer (or the Orchestrator via tool invocation) runs the build:

```
BUILD COMMAND: ./gradlew build

EXPECTED OUTCOME:
  - All source files compile
  - All tests compile
  - No annotation processing errors
  - Micronaut bean definitions generated correctly

VALIDATION:
  exit_code == 0 → proceed to test execution
  exit_code != 0 → enter diagnostic flow
```

### 2.4 — Step 3: Run Tests

```
TEST COMMAND: ./gradlew test

EXPECTED OUTCOME:
  - All tests pass
  - No flaky tests (verify with --no-parallel if needed)

VALIDATION:
  all_tests_pass → G4 satisfied → proceed to Phase 5
  any_test_fails → enter diagnostic flow
```

### 2.5 — Step 4: Diagnose Failures

When build or tests fail, the **Debugger** agent analyzes the failure:

```
DEBUGGER INPUT:
  - Build/test output (error messages, stack traces)
  - Source code that triggered the failure
  - Migration Plan (to understand intent)
  - Test code (to verify test correctness)

DEBUGGER OUTPUT:
  - Root cause classification:
    a) COMPILATION_ERROR: missing import, type mismatch, annotation issue
    b) BEAN_DEFINITION_ERROR: Micronaut DI cannot resolve a bean
    c) TEST_LOGIC_ERROR: the test is wrong (incorrect assertion, wrong mock setup)
    d) IMPLEMENTATION_BUG: the production code has a bug
    e) PLAN_INFEASIBLE: the task cannot be implemented as planned

  - Fix recommendation:
    For (a-d): specific code change to apply
    For (e): escalate to Orchestrator for plan revision
```

### 2.6 — Step 5: Fix and Retry

Based on the Debugger's diagnosis:

| Root Cause | Who Fixes | Action |
|-----------|----------|--------|
| COMPILATION_ERROR | Implementer | Apply fix, re-build |
| BEAN_DEFINITION_ERROR | Implementer | Fix DI configuration, re-build |
| TEST_LOGIC_ERROR | Test Agent | Correct the test, re-run |
| IMPLEMENTATION_BUG | Implementer | Fix the code, re-build, re-test |
| PLAN_INFEASIBLE | **Orchestrator** | Escalate to human. Plan may need revision. |

---

## 3. Micronaut-Specific Build Validation

Micronaut has unique build characteristics that the verification phase must account for.

### 3.1 — Ahead-of-Time (AOT) Compilation

Micronaut generates bean definitions, HTTP route metadata, and injection points
at compile time. This means **build failures in Micronaut often reveal issues
that would be runtime failures in Spring Boot.**

```
MICRONAUT BUILD CHECKS:
  1. Bean definition generation:
     - Every @Singleton, @Controller, @Repository must have valid injection points
     - All constructor parameters must be resolvable beans or @Value properties
     - Missing beans fail at compile time (not runtime)

  2. HTTP route validation:
     - @Controller paths must not conflict
     - @Body parameters must be deserializable
     - Return types must be serializable

  3. Configuration validation:
     - @ConfigurationProperties classes must match application.yml structure
     - Missing required properties fail at startup, not at call time
```

### 3.2 — Common Micronaut Build Failures and Fixes

| Failure | Symptom | Root Cause | Fix |
|---------|---------|-----------|-----|
| Bean not found | `NoSuchBeanException` at compile | Port interface has no `@Singleton` implementor | Add `@Singleton` to the adapter class |
| Multiple beans | `NonUniqueBeanException` | Two adapters implement the same port | Use `@Primary` or `@Named` qualifiers |
| Missing config | `ConfigurationException` at startup | `application.yml` missing datasource config | Add configuration under `datasources.default` |
| Route conflict | `RoutingConflictException` | Two controllers claim the same path | Fix path or HTTP method overlap |
| Serialization | `CodecException` during test | Record doesn't have Jackson annotations | Add `@Serdeable` (Micronaut serialization) |

### 3.3 — Integration Test Infrastructure

```java
// Micronaut integration test pattern
@MicronautTest
class OrderControllerIntegrationTest {

    @Inject
    HttpClient httpClient;

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    @DisplayName("POST /api/v1/orders should create order and return 201")
    void createOrder_validRequest_returns201() {
        var request = HttpRequest.POST("/api/v1/orders", new CreateOrderRequest(
            "customer-1",
            List.of(new OrderItemRequest("item-1", 2))
        ));

        var response = client.toBlocking().exchange(request, OrderResponse.class);

        assertThat(response.status().getCode()).isEqualTo(201);
        assertThat(response.body()).isNotNull();
        assertThat(response.body().orderNumber()).isNotBlank();
    }
}

// Testcontainers for database tests
@MicronautTest
@Testcontainers
class OrderRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Inject
    OrderMicronautRepository repository;

    @Test
    @DisplayName("should persist and retrieve order by status")
    void findByStatus_existingOrders_returnsMatchingOrders() {
        // Arrange
        repository.save(OrderJpaEntity.of("ORD-001", "CREATED"));
        repository.save(OrderJpaEntity.of("ORD-002", "SHIPPED"));

        // Act
        var created = repository.findByStatus("CREATED");

        // Assert
        assertThat(created).hasSize(1);
        assertThat(created.get(0).getOrderNumber()).isEqualTo("ORD-001");
    }
}
```

---

## 4. Retry Budget and Escalation

### 4.1 — Maximum Iterations

The verification loop runs at most **2 complete iterations** (generate → build →
test → diagnose → fix). This budget is intentionally low because:

1. **Most failures are fixable in 1 iteration.** Compilation errors and simple
   test failures have clear fixes.
2. **Persistent failures indicate a plan problem.** If the same issue recurs
   after one fix attempt, the plan may be infeasible.
3. **Infinite loops waste resources.** An unbounded loop could spin on a
   fundamental design flaw that no code-level fix can resolve.

### 4.2 — Iteration Tracking

```
VERIFICATION STATE:
  iteration: 1
  max_iterations: 2
  history:
    - iteration_1:
        build: PASS
        tests: FAIL (3 of 15)
        diagnosis: [IMPLEMENTATION_BUG x2, TEST_LOGIC_ERROR x1]
        fixes_applied: 3
    - iteration_2:
        build: PASS
        tests: FAIL (1 of 15)    ← improvement but not resolved
        diagnosis: [PLAN_INFEASIBLE x1]
        action: ESCALATE

  ESCALATION REPORT:
    "After 2 verification iterations, 1 test still fails.
     Root cause: The Migration Plan specifies pessimistic locking for
     InventoryService.reserveStock(), but Micronaut Data JDBC does not
     support LockModeType.PESSIMISTIC_WRITE directly.

     Options:
     1. Use native @Query with FOR UPDATE clause
     2. Revise the plan to use optimistic locking instead
     3. Implement a custom repository without Micronaut Data for this method

     Human decision required."
```

### 4.3 — Escalation Format

When the retry budget is exhausted, the Orchestrator presents a diagnostic
report to the user:

```
═══════════════════════════════════════════════════
     VERIFICATION FAILED — HUMAN INPUT NEEDED
═══════════════════════════════════════════════════

ITERATION HISTORY:
  Iteration 1: 12/15 tests pass → 3 fixes applied
  Iteration 2: 14/15 tests pass → 1 unresolvable issue

UNRESOLVED ISSUE:
  Test: reserveStock_concurrentRequests_preventsOverselling
  Root Cause: PLAN_INFEASIBLE
  Details: Micronaut Data JDBC lacks native pessimistic locking.
           The plan assumed JPA-like LockModeType support.

OPTIONS:
  [1] Revise plan: change to optimistic locking strategy
  [2] Revise plan: use native SQL with FOR UPDATE
  [3] Revise plan: exclude this feature from migration scope
  [4] Provide guidance for a custom implementation

═══════════════════════════════════════════════════
```

---

## 5. Phase Transition: Verification → Delivery

When Gate G4 passes (all tests green, build clean), the Orchestrator transitions
to Phase 5 (Delivery).

```
GATE G4 VALIDATION:
  checks:
    - build_exit_code == 0
    - test_exit_code == 0
    - zero_compilation_warnings (configurable — may allow warnings)
    - test_count >= plan_expected_tests (at least as many tests as planned)

  on_pass:
    - Log: "G4 PASSED — verification complete after {n} iteration(s)"
    - Transition to Phase 5 (Delivery)
    - Pass all artifacts to delivery: source code, tests, build output,
      migration plan, discovery report

  on_fail:
    - If iterations < max_iterations: re-enter loop
    - If iterations >= max_iterations: escalate
```
