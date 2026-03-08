# Monorepo Copilot Instructions

> Global instructions for the entire monorepo (root level).

## Repository Structure

This is a monorepo with:
- **Legacy services** (root level): Jakarta REST, Java 11, Oracle
- **Modern microservices** (`replatforming/services/`): Spring Boot 3, Java 17+, PostgreSQL

## Code Style

### Legacy Services
- Jakarta REST annotations (`@Path`, `@GET`, `@POST`)
- EJB services (`@Stateless`, `@Stateful`)
- Oracle stored procedures via `CallableStatement` or `@Procedure`
- Maven builds

### Modern Microservices  
- Spring Boot 3.x with Spring Web
- Clean architecture (domain/application/infrastructure layers)
- JPA with PostgreSQL, native queries preferred over JPQL
- Gradle builds

## Migration Conventions

When migrating APIs:
- Use `/migrate-api` prompt to start migration workflow
- Always get explicit approval before implementation phase
- Document stored procedure handling decisions

## Commit Conventions

```
feat: Add new feature
fix: Bug fix
refactor: Code restructuring
migrate: API migration (source → target)
stub: Add stub implementation for later
```

## Testing

- Legacy: JUnit 4/5 with Mockito
- Microservices: JUnit 5, MockMvc, Testcontainers for integration tests
