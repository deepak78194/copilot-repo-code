# Migration-Specific Instructions

> Additional instructions applied when migration files are being edited.

---
applyTo: "**/migration/**,**/*Migration*,**/*Migrator*"
---

## Migration Workflow Rules

### Phase Gates

1. **Discovery → Planning**: Requires complete discovery report
2. **Planning → Implementation**: Requires explicit user approval
3. **Implementation → Verification**: Requires code compilation success
4. **Verification → Complete**: Requires all tests passing

### Stored Procedure Decisions

When encountering stored procedures, ALWAYS present these options:

| Choice | Description |
|--------|-------------|
| KEEP_SP | Retain Oracle SP call, configure dual datasource |
| DECOMPOSE | Convert to native PostgreSQL queries + service logic |
| STUB | Create interface stub with @TODO for later |
| API_CLIENT | Replace with REST call to another service |

**NEVER auto-select** stored procedure handling. Always ask the user.

### Code Generation

When generating migrated code:
- Separate domain entities from JPA entities
- Use ports and adapters pattern
- Generate DTOs for API boundaries
- Include validation annotations
- Add proper exception handling

### Documentation

Every migration should produce:
- Migration plan (before implementation)
- Updated README if new dependencies added
- Stub tracker if STUB option chosen

## Anti-Patterns to Avoid

- ❌ 1:1 translation of legacy code structure
- ❌ Copying business logic from stored procedures without review
- ❌ Skipping the human approval gate
- ❌ Leaving stubs without tracking tickets
- ❌ Mixing datasource transactions without explicit handling
