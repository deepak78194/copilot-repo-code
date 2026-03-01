# 05 — Freshness & Optimization: Preventing Transliteration

> **Purpose:** Define the Anti-Transliteration Principle, the Optimization Mandate that ensures modern output quality, concrete legacy→modern improvement examples, and the tension between consistency (doing it the same way every time) and freshness (doing it the *right* way for the target stack).

---

## 1. The Transliteration Problem

Transliteration is the most common failure mode in automated migration. It
produces code that is *syntactically modern* but *architecturally legacy*.

```
TRANSLITERATION EXAMPLE:

Legacy (Jakarta EE):
  @Stateless
  public class OrderService {
      @PersistenceContext
      private EntityManager em;

      public OrderDTO getOrder(Long id) {
          Order order = em.find(Order.class, id);
          if (order == null) throw new NotFoundException("Order not found");
          return new OrderDTO(order.getId(), order.getStatus());
      }
  }

Transliterated (Micronaut — WRONG):
  @Singleton
  public class OrderService {
      private final EntityManager em;  // ← Same pattern, different annotation

      public OrderService(EntityManager em) {
          this.em = em;
      }

      public OrderDTO getOrder(Long id) {
          Order order = em.find(Order.class, id);  // ← Same code
          if (order == null) throw new NotFoundException("Order not found");
          return new OrderDTO(order.getId(), order.getStatus());
      }
  }
```

The transliterated version:
- Uses `EntityManager` directly instead of Micronaut Data repository
- Has no clean architecture layers (controller → service → database)
- Uses the same exception handling pattern (no structured errors)
- Returns a DTO that's just a flattened entity (no ports/adapters separation)
- Gained nothing from the migration except a different annotation

---

## 2. The Anti-Transliteration Principle

> **Anti-Transliteration Principle:** The legacy code is a *requirements source*,
> not a *template*. The migration must produce code that a skilled developer would
> write from scratch if given only the requirements and the target stack.

This principle operates at the **Conventional Zone** level (from
[07 — Freshness](../architecture/07-freshness-and-bounded-autonomy.md) §2):
agents SHOULD follow it, and deviations require explicit justification.

### 2.1 — The Requirements Extraction Test

For every piece of legacy code being migrated, the agent should mentally perform
this test:

```
REQUIREMENTS EXTRACTION TEST:
  1. Read the legacy code.
  2. Discard the implementation.
  3. Write down what the code DOES (not how it does it).
  4. Implement from the requirements using only target-stack knowledge.

Example:
  Legacy: EntityManager.find(Order.class, id) with null check
  Requirement: "Find an order by ID. Return it or signal 'not found'."
  Modern: repository.findById(id).orElseThrow(() ->
            new OrderNotFoundException(id))
```

### 2.2 — Enforcement Mechanism

The Anti-Transliteration Principle is enforced through three mechanisms:

1. **Skill-level conventions.** `clean-architecture.skill.md` and `micronaut.skill.md`
   define the *correct* way to write modern code. The Implementer follows skills,
   not legacy patterns.

2. **Planner design.** The Migration Plan maps legacy components to clean
   architecture layers (see [03 — Planner Design](03-migration-planner-design.md) §3.2).
   The Implementer implements the *plan*, not the legacy code.

3. **Reviewer verification.** The Reviewer checks generated code against the
   migration-checklist skill, which includes an anti-transliteration checklist.

---

## 3. The Optimization Mandate

The Optimization Mandate goes beyond preventing transliteration. It requires that
the migration **actively improves** the codebase in specific, measurable ways.

### 3.1 — Mandatory Optimizations

These improvements are required for every migration, not optional. They live in
the **Mandatory Zone** of bounded autonomy.

| # | Legacy Pattern | Modern Requirement | Rationale |
|---|---------------|-------------------|-----------|
| 1 | Entity returned from REST endpoint | Dedicated response DTO | Decouple API contract from persistence model |
| 2 | Field injection (`@Inject` on fields) | Constructor injection | Testability, immutability, explicit dependencies |
| 3 | Raw exception with message string | Structured error DTO with code + message | Machine-readable errors, consistent contract |
| 4 | `println` / `System.out` logging | SLF4J structured logging | Structured JSON logs for production observability |
| 5 | Hardcoded configuration values | Externalized configuration (`application.yml` + `@Value`) | Environment portability |
| 6 | SQL concatenation | Parameterized queries | SQL injection prevention |
| 7 | No API versioning | Versioned paths (`/api/v1/...`) | Forward compatibility |
| 8 | Generic HTTP 500 for all errors | Correct status codes (201, 204, 404, 409, 422) | Proper REST semantics |

### 3.2 — Encouraged Optimizations

These improvements are in the **Innovation Zone** — agents MAY suggest them,
and they count against the Innovation Budget
(see [03 — Planner Design](03-migration-planner-design.md) §5).

| # | Legacy Pattern | Modern Alternative | When to Suggest |
|---|---------------|-------------------|----------------|
| 1 | String for business identifiers | Value objects (e.g., `OrderNumber`, `CustomerId`) | When the string has validation rules or generation logic |
| 2 | Service-to-service direct calls | Domain events + event handlers | When services have side-effect coupling (email, audit, notification) |
| 3 | N+1 query patterns | Batch queries or join fetches | When discovery reveals lazy loading in loops |
| 4 | Monolithic error mapper | Per-domain exception hierarchy | When > 10 exception types exist |
| 5 | Manual audit columns | Framework audit support (`@DateCreated`, `@DateUpdated`) | When entities have `created_at` / `updated_at` columns |
| 6 | Synchronous external calls | Reactive or async patterns | When external call latency is documented as a problem |
| 7 | No health/readiness endpoints | `/health` and `/ready` endpoints | Always — observability baseline (from `copilot-instructions.md`) |
| 8 | No request tracing | Trace ID propagation | Always — observability baseline |

---

## 4. Concrete Migration Examples

### 4.1 — Data Access: EntityManager → Repository

```java
// LEGACY (Jakarta EE)
@Stateless
public class OrderDAO {
    @PersistenceContext
    private EntityManager em;

    public Order findById(Long id) {
        return em.find(Order.class, id);  // Returns null if not found
    }

    public List<Order> findByStatus(String status) {
        return em.createQuery(
            "SELECT o FROM Order o WHERE o.status = :status", Order.class)
            .setParameter("status", status)
            .getResultList();
    }

    public void save(Order order) {
        em.persist(order);
    }
}
```

```java
// MODERN (Micronaut Clean Architecture)

// Outbound Port (application layer — no framework dependency)
public interface FindOrderPort {
    Optional<Order> findById(OrderId id);
    List<Order> findByStatus(OrderStatus status);
}

public interface SaveOrderPort {
    Order save(Order order);
}

// Outbound Adapter (infrastructure layer — Micronaut-specific)
@Singleton
public class JpaOrderAdapter implements FindOrderPort, SaveOrderPort {

    private static final Logger log = LoggerFactory.getLogger(JpaOrderAdapter.class);

    private final OrderMicronautRepository repository;
    private final OrderEntityMapper mapper;

    public JpaOrderAdapter(OrderMicronautRepository repository,
                           OrderEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        log.debug("Finding order by id={}", id.value());
        return repository.findById(id.value())
            .map(mapper::toDomain);
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return repository.findByStatus(status.name()).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public Order save(Order order) {
        var entity = mapper.toEntity(order);
        var saved = repository.save(entity);
        log.info("Order persisted: id={}, orderNumber={}", saved.getId(), saved.getOrderNumber());
        return mapper.toDomain(saved);
    }
}
```

**What changed beyond syntax:**
- `null` return → `Optional<>` with explicit handling
- Direct `EntityManager` → port interface + adapter pattern
- JPA entity as domain object → separate domain and persistence entities
- No logging → structured SLF4J logging
- Tight coupling → dependency inversion via ports

### 4.2 — Error Handling: ExceptionMapper → Structured Handler

```java
// LEGACY (Jakarta EE — JAX-RS ExceptionMapper)
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {
    @Override
    public Response toResponse(Exception e) {
        if (e instanceof NotFoundException) {
            return Response.status(404).entity(
                Map.of("error", e.getMessage())).build();
        }
        return Response.status(500).entity(
            Map.of("error", "Internal server error")).build();
    }
}
```

```java
// MODERN (Micronaut — structured error handling)
@Singleton
@Requires(classes = OrderNotFoundException.class)
public class OrderNotFoundExceptionHandler
    implements ExceptionHandler<OrderNotFoundException, HttpResponse<?>> {

    private static final Logger log = LoggerFactory.getLogger(
        OrderNotFoundExceptionHandler.class);

    @Override
    public HttpResponse<?> handle(HttpRequest request,
                                   OrderNotFoundException exception) {
        log.warn("Order not found: {}", exception.getOrderId());
        return HttpResponse.notFound(new ErrorResponse(
            "ORDER_NOT_FOUND",
            "No order with id %s exists.".formatted(exception.getOrderId())
        ));
    }
}

// Structured error DTO
@Serdeable
public record ErrorResponse(String code, String message) {}
```

**What changed beyond syntax:**
- Catch-all mapper → specific handler per exception type
- Unstructured `Map.of("error", msg)` → typed `ErrorResponse` DTO with error code
- No logging → structured warning log with context
- Generic message → specific, actionable message with entity ID

### 4.3 — Configuration: JNDI → Application Config

```java
// LEGACY (Jakarta EE — JNDI lookup)
@Stateless
public class EmailService {
    @Resource(mappedName = "java:/mail/NotificationSession")
    private Session mailSession;

    public void sendConfirmation(String to, String orderNumber) {
        Message msg = new MimeMessage(mailSession);
        msg.setRecipients(Message.RecipientType.TO,
            InternetAddress.parse(to));
        msg.setSubject("Order Confirmation: " + orderNumber);
        // ...
        Transport.send(msg);
    }
}
```

```java
// MODERN (Micronaut — configuration-injected)

// Outbound Port (application layer)
public interface SendEmailPort {
    void sendOrderConfirmation(EmailAddress to, OrderNumber orderNumber);
}

// Outbound Adapter (infrastructure layer)
@Singleton
public class SmtpEmailAdapter implements SendEmailPort {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailAdapter.class);

    private final EmailSender emailSender;

    public SmtpEmailAdapter(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void sendOrderConfirmation(EmailAddress to, OrderNumber orderNumber) {
        log.info("Sending order confirmation to={}, order={}", to, orderNumber);
        emailSender.send(Email.builder()
            .to(to.value())
            .subject("Order Confirmation: " + orderNumber.value())
            .body("Your order %s has been confirmed.".formatted(orderNumber.value()))
            .build()
        );
    }
}
```

**What changed beyond syntax:**
- JNDI resource lookup → constructor-injected `EmailSender`
- Direct `javax.mail` API → Micronaut Email abstraction
- String parameters → value objects (`EmailAddress`, `OrderNumber`)
- No logging → structured logging
- Coupled to infrastructure → hidden behind `SendEmailPort`

### 4.4 — External Calls: JAX-RS Client → Declarative HTTP Client

```java
// LEGACY (Jakarta EE — manual JAX-RS Client)
@Stateless
public class PaymentService {
    public PaymentResult processPayment(Long orderId, BigDecimal amount) {
        Client client = ClientBuilder.newClient();
        try {
            Response response = client.target("https://payment-gateway.internal/api/v2")
                .path("charges")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(Map.of(
                    "orderId", orderId,
                    "amount", amount.toString()
                )));

            if (response.getStatus() == 200) {
                return response.readEntity(PaymentResult.class);
            }
            throw new PaymentFailedException("Payment failed: " + response.getStatus());
        } finally {
            client.close();
        }
    }
}
```

```java
// MODERN (Micronaut — declarative HTTP client)

// Outbound Port (application layer)
public interface ProcessPaymentPort {
    PaymentResult processPayment(OrderId orderId, Money amount);
}

// Declarative client interface (infrastructure layer)
@Client("${payment-gateway.url}/api/v2")
public interface PaymentGatewayClient {
    @Post("/charges")
    PaymentGatewayResponse charge(@Body ChargeRequest request);
}

// Adapter (infrastructure layer)
@Singleton
public class PaymentGatewayAdapter implements ProcessPaymentPort {

    private static final Logger log = LoggerFactory.getLogger(
        PaymentGatewayAdapter.class);

    private final PaymentGatewayClient client;

    public PaymentGatewayAdapter(PaymentGatewayClient client) {
        this.client = client;
    }

    @Override
    public PaymentResult processPayment(OrderId orderId, Money amount) {
        log.info("Processing payment: orderId={}, amount={}", orderId, amount);
        try {
            var response = client.charge(new ChargeRequest(
                orderId.value(), amount.value().toString()
            ));
            return new PaymentResult(response.transactionId(), true);
        } catch (HttpClientResponseException e) {
            log.error("Payment failed: orderId={}, status={}", orderId, e.getStatus());
            throw new PaymentFailedException(orderId, e.getStatus().getCode());
        }
    }
}
```

**What changed beyond syntax:**
- Manual `Client` creation → declarative `@Client` interface
- Hardcoded URL → externalized config (`${payment-gateway.url}`)
- `Map.of()` body → typed `ChargeRequest` record
- Manual resource cleanup → framework-managed client lifecycle
- Generic exception → domain-specific exception with context
- Behind a port → decoupled from framework

---

## 5. Pattern Freshness Across Migrations

### 5.1 — Consistency Within a Single Migration

Within one migration batch, consistency is paramount. All services should follow
the same patterns:

```
CONSISTENCY RULES (within one migration):
  - All controllers use the same error handling approach
  - All repositories use the same adapter pattern
  - All use cases follow the same port convention
  - All DTOs use the same serialization approach (@Serdeable records)
  - All tests follow the same naming and assertion patterns
```

### 5.2 — Freshness Across Multiple Migrations

Across multiple migration batches (e.g., migrating service by service over months),
**freshness should evolve:**

```
FRESHNESS EVOLUTION:
  Batch 1 (January): Establish patterns, create skills from experience.
  Batch 2 (March):   Skills updated with lessons from Batch 1.
                      E.g., "Testcontainers setup should use shared container"
  Batch 3 (June):    New Micronaut version available. Skills updated.
                      E.g., "Use @Serdeable.Serializable for GraalVM compatibility"
  Batch 4 (August):  New team patterns. Skills evolve.
                      E.g., "Add OpenAPI annotations for automatic API docs"
```

This is the **feedback loop** from [03 — Skill Design](../architecture/03-skill-and-knowledge-design.md) §6:
experiment results improve skills, which improve future agent output.

### 5.3 — The Consistency vs. Freshness Tension

```
TENSION:
  Consistency says: "Do it the same way as last time."
  Freshness says:   "Do it the right way for today."

RESOLUTION:
  Within a migration batch → CONSISTENCY wins.
    All services in this batch use the same patterns.

  Between migration batches → FRESHNESS wins.
    Update skills between batches. New batches use new patterns.
    Do NOT retroactively update already-migrated services
    (that's a separate refactoring task, not part of migration).
```

---

## 6. Reviewer's Anti-Transliteration Checklist

The Reviewer agent, equipped with `migration-checklist.skill.md`, verifies that
every piece of generated code passes the following checks:

```
ANTI-TRANSLITERATION CHECKLIST:
  □ Domain entities have NO framework annotations
  □ Use cases depend only on ports (interfaces), not implementations
  □ Controllers delegate to inbound ports, not directly to services
  □ Repository adapters implement outbound ports
  □ No EntityManager usage — Micronaut Data repositories only
  □ No JNDI lookups — all dependencies injected via constructor
  □ Error responses use structured DTOs with error codes
  □ HTTP status codes match REST semantics (201, 204, 404, 409, 422)
  □ API paths follow versioned, plural-noun convention
  □ Configuration values externalized, not hardcoded
  □ Logging uses SLF4J, not System.out.println
  □ No raw types — all generics parameterized
  □ Constructor injection everywhere — no field injection
  □ Tests exist for every public method
  □ Test names follow methodName_state_expectedBehavior pattern
```

If any check fails, the Reviewer provides specific feedback to the Implementer,
referencing the checklist item and the violating code location.
