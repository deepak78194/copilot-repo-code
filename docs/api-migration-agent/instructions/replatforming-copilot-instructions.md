# Replatforming Copilot Instructions

> Instructions for microservices under replatforming/services/

## Microservice Stack

- **Java**: 17+
- **Framework**: Spring Boot 3.x
- **Build**: Gradle
- **Primary Database**: PostgreSQL
- **Secondary Database**: Oracle (for retained stored procedures, if configured)

## Clean Architecture

All services follow clean architecture with package structure:

```
com.company.{service}/
├── domain/           # Business entities, value objects (NO framework annotations)
├── application/      # Use cases, ports (interfaces)
│   ├── port/in/      # Inbound ports (use case interfaces)
│   └── port/out/     # Outbound ports (repository interfaces)
└── infrastructure/   # Adapters (controllers, repositories, configs)
    ├── web/
    ├── persistence/
    └── config/
```

## Code Style

- Use records for DTOs and Value Objects
- Constructor injection via `@RequiredArgsConstructor`
- Use `Optional` for nullable returns from repositories
- Native queries over JPQL when complex joins needed
- API versioning: `/api/v1/...`

## Migration Skills

When performing API migrations, reference these skills from parent:
- `../../.github/skills/migration/` - Migration-specific skills

For legacy analysis when root workspace is not available:
- Legacy skills may need manual reference or symlink setup

## Data Access

### PostgreSQL (Primary)
```java
@Query(value = "SELECT ... FROM ...", nativeQuery = true)
```

### Oracle Stored Procedures (if dual-datasource configured)
```java
@Qualifier("oracleJdbcTemplate")
private final JdbcTemplate oracleJdbcTemplate;
```

## Testing

- Unit tests: JUnit 5 + Mockito
- Integration tests: `@SpringBootTest` + Testcontainers
- Controller tests: `@WebMvcTest` + MockMvc
