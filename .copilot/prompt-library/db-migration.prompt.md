# Database Migration Prompt Template

Use this prompt to write, validate, and test a PostgreSQL migration using Flyway and the Plan → Implement → Review workflow.

## When to Use

- When adding or modifying database tables.
- When you want a safe, reviewed migration with rollback considerations.

---

## Prompt

```
You are the Orchestrator agent. Run the Plan → Implement → Review workflow for the following database migration.

## Migration Description
{migration_description}

## Current Schema (relevant tables)
{current_schema_or_none}

## Skills
- See `.copilot/skills/java.skill.md` (database section)

## Phase 1 — Plan
Invoke the Planner agent to produce a task list. Include tasks for:
- SQL migration file
- Java entity/model updates (if any)
- Repository method updates (if any)
- Tests to verify the migration

## Phase 2 — Implement
For each task, invoke the Implementer agent.

### Migration File Conventions
- File name: `V{next_version}__{description}.sql`
- Location: `src/main/resources/db/migration/`
- Always write in standard ANSI SQL (PostgreSQL dialect).
- Include only DDL (CREATE, ALTER, DROP) and DML for seed data.
- Do not include transaction control (`BEGIN`, `COMMIT`) — Flyway manages transactions.

### Migration Checklist
For every migration, verify:
- [ ] All new columns have explicit types and NOT NULL or DEFAULT constraints.
- [ ] Foreign keys reference valid tables and columns.
- [ ] Indexes are added for frequently queried foreign key columns.
- [ ] No data is lost for existing rows (use DEFAULT or backfill for new NOT NULL columns).
- [ ] Migration is idempotent if possible (use `IF NOT EXISTS`, `IF EXISTS`).

## Phase 3 — Review
Invoke the Reviewer agent on the migration file and any Java changes.
Check: naming conventions, constraint correctness, data safety, index coverage.

## Constraints
- Never drop a column or table without confirming no application code references it.
- Never use `TRUNCATE` or `DELETE` in a migration without explicit justification.
- Never use `SELECT` in a migration — migrations are DDL/DML only.
- Test migrations against a Testcontainers PostgreSQL instance.

## Output Format
(Follow the Orchestrator workflow log format from `orchestrator.agent.md`)
```

---

## Example

**Migration Description:** Add `phone_number` (nullable VARCHAR(20)) column to the `users` table.

**Current Schema:**
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

Fill in the template and send to the Orchestrator agent.
