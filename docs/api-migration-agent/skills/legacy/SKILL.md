# Legacy Skills Index

> Skills for analyzing legacy Jakarta REST services in the monorepo.

---

## Available Skills

| Skill | Purpose | When Loaded |
|-------|---------|-------------|
| [jakarta-analysis.skill.md](jakarta-analysis.skill.md) | Analyze Jakarta REST endpoints, controllers, EJBs | Discovery phase |
| [oracle-sp-analysis.skill.md](oracle-sp-analysis.skill.md) | Catalog Oracle stored procedure calls | Discovery phase |
| [dependency-mapping.skill.md](dependency-mapping.skill.md) | Map API-specific dependencies | Discovery phase |

## Workspace Context

These skills are designed for:
- Root monorepo workspace (full access)
- Legacy service workspace (direct access)

When working from a microservice workspace, reference these skills via relative path:
```
#file:../../../.github/skills/legacy/jakarta-analysis.skill.md
```

## Legacy Stack Context

Our legacy services use:
- **Java Version**: 11
- **REST Framework**: Jakarta REST (JAX-RS)
- **Database**: Oracle with stored procedures
- **Build**: Maven
- **Container**: EJB with @Stateless, @Stateful services
