# Migration Skills Index

> Skills for API migration planning and implementation in the replatforming microservices.

---

## Available Skills

| Skill | Purpose | When Loaded |
|-------|---------|-------------|
| [planning.skill.md](planning.skill.md) | Design migration plan with user choice flow | Planning phase |
| [sp-to-jdbc.skill.md](sp-to-jdbc.skill.md) | Decompose SP to native queries | Implementation (DECOMPOSE choice) |
| [sp-to-api-client.skill.md](sp-to-api-client.skill.md) | Replace SP with REST client | Implementation (API_CLIENT choice) |
| [dual-datasource.skill.md](dual-datasource.skill.md) | Configure Oracle + Postgres | Implementation (KEEP_SP choice) |
| [stub-generation.skill.md](stub-generation.skill.md) | Generate interface stubs | Implementation (STUB choice) |
| [spring-boot-patterns.skill.md](spring-boot-patterns.skill.md) | Spring Boot 3 conventions | All implementation |

## Workspace Context

These skills are designed for:
- Root monorepo workspace (via `replatforming/services/.github/skills/`)
- Microservice workspace (via `../../.github/skills/migration/` or direct access)

## Target Stack Context

Our microservices use:
- **Java Version**: 17+
- **Framework**: Spring Boot 3.x
- **Primary Database**: PostgreSQL (for new data, decomposed queries)
- **Secondary Database**: Oracle (for retained stored procedures)
- **Build**: Gradle

## Skill Loading Rules

1. **Planning skill** - Always loaded during planning phase
2. **SP handling skills** - Loaded based on user choice:
   - User chooses DECOMPOSE → load `sp-to-jdbc.skill.md`
   - User chooses API_CLIENT → load `sp-to-api-client.skill.md`
   - User chooses KEEP_SP → load `dual-datasource.skill.md`
   - User chooses STUB → load `stub-generation.skill.md`
3. **spring-boot-patterns** - Always loaded during implementation
