# Observability Skill

This skill defines structured logging, distributed tracing, metrics, and health-check conventions
for this repository. Reference it when implementing or reviewing any backend service.

## Principles

1. **Structured over unstructured** — every log line is a JSON object with typed fields.
2. **Correlated** — every log line and span carries the same `traceId` / `requestId`.
3. **Actionable** — log what is needed to diagnose a problem in production; nothing more.
4. **Safe** — never log PII, passwords, tokens, or financial data.

---

## Structured Logging

### Java (Logback + SLF4J + Logstash encoder)

Configuration (`logback-spring.xml`):
```xml
<configuration>
  <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
      <includeMdcKeyName>traceId</includeMdcKeyName>
      <includeMdcKeyName>spanId</includeMdcKeyName>
      <includeMdcKeyName>userId</includeMdcKeyName>
    </encoder>
  </appender>
  <root level="INFO">
    <appender-ref ref="JSON"/>
  </root>
</configuration>
```

Usage:
```java
private static final Logger log = LoggerFactory.getLogger(UserService.class);

// Good — structured, parameterised
log.info("User created userId={} email={}", userId, maskEmail(email));

// Bad — unstructured, PII exposed
log.info("Created user: " + user.toString());
```

### Log Levels

| Level | When to use |
|-------|-------------|
| `ERROR` | Unhandled exception; service cannot fulfil request |
| `WARN` | Recoverable problem; degraded behaviour; retry triggered |
| `INFO` | Significant lifecycle events (startup, shutdown, user created) |
| `DEBUG` | Diagnostic details useful during development; **off in production** |
| `TRACE` | Fine-grained flow tracing; **never in production** |

---

## Distributed Tracing

Use **OpenTelemetry** (Java agent or SDK) for distributed traces.

### Trace Propagation

Propagate trace context via **W3C Trace Context** headers (`traceparent`, `tracestate`):

```java
// Spring Boot — add the OTel Java agent via JVM args:
// -javaagent:opentelemetry-javaagent.jar
// -Dotel.exporter.otlp.endpoint=http://collector:4317
// -Dotel.service.name=user-service
```

### MDC Integration

Populate MDC so that every log line within a request carries the trace context:

```java
@Component
public class TraceContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String traceId = Span.current().getSpanContext().getTraceId();
        MDC.put("traceId", traceId);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.clear();
        }
    }
}
```

### Custom Spans

Add spans around expensive or critical operations:

```java
private final Tracer tracer = GlobalOpenTelemetry.getTracer("user-service");

public User createUser(CreateUserRequest req) {
    Span span = tracer.spanBuilder("UserService.createUser").startSpan();
    try (Scope scope = span.makeCurrent()) {
        span.setAttribute("user.email_domain", extractDomain(req.email()));
        // ... business logic ...
        return user;
    } catch (Exception e) {
        span.recordException(e);
        span.setStatus(StatusCode.ERROR, e.getMessage());
        throw e;
    } finally {
        span.end();
    }
}
```

---

## Metrics

Use **Micrometer** (Spring Boot Actuator) or Micronaut Micrometer.

### Standard Metrics to Expose

| Metric | Type | When to emit |
|--------|------|--------------|
| `http.server.requests` | Timer | Auto via framework |
| `db.query.duration` | Timer | Around every repository call |
| `user.created.total` | Counter | On successful user creation |
| `cache.hit` / `cache.miss` | Counter | On cache access |
| `jvm.memory.used` | Gauge | Auto via JVM metrics |

Custom metrics:
```java
@Autowired
private MeterRegistry registry;

public User createUser(CreateUserRequest req) {
    User user = // ... create ...
    registry.counter("user.created.total",
        "status", "success",
        "source", req.source()
    ).increment();
    return user;
}
```

---

## Health & Readiness Endpoints

Every service must expose:

| Endpoint | Path | Purpose |
|----------|------|---------|
| **Liveness** | `GET /health` | Is the process alive? Returns `200` or `503`. |
| **Readiness** | `GET /ready` | Can the service handle traffic? Returns `200` or `503`. |

Readiness must check:
- Database connectivity
- Any required downstream service connectivity (fail open if non-critical)

```java
// Spring Boot Actuator — add to application.yml:
// management.endpoint.health.show-details: always
// management.health.db.enabled: true
```

Response format:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "cache": { "status": "UP" }
  }
}
```

---

## Alerting Hooks (documentation only)

Define SLOs and alert thresholds in your monitoring platform:

| SLO | Threshold | Alert |
|-----|-----------|-------|
| p99 latency | < 500 ms | Page on-call if exceeded for > 5 min |
| Error rate | < 1% | Page on-call if exceeded for > 2 min |
| Availability | > 99.9% | Page on-call immediately |

---

## Constraints

- Never log raw exception messages that may contain user data — log a sanitised message + error code.
- Never log authentication tokens, cookies, or `Authorization` header values.
- Always propagate `traceId` to downstream HTTP calls (via `traceparent` header).
- Emit structured logs in **all environments** (not just production) to keep the format consistent.
- Do not use `System.out.println` or `e.printStackTrace()` anywhere in production code.
