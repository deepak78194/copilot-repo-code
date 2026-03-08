# /analyze-api

> Entry point prompt for API analysis only (no migration).

---
mode: agent
agent: api-discovery
description: Analyze an API for migration readiness without performing migration
---

# API Analysis

I'll analyze an API to understand its structure, dependencies, and migration complexity.

## What I Need From You

- **API to analyze**: Service name and API path
  - Example: "analyze getUserProfile in user-service"
  - Example: "what does the /orders/create endpoint depend on?"

## What I'll Report

1. **Endpoint Details**: HTTP method, path, parameters, response
2. **Service Dependencies**: What services and repositories this API uses
3. **Database Calls**: JDBC queries and stored procedure calls
4. **External Integrations**: REST calls, message queues, etc.
5. **Migration Complexity**: Assessment of how hard this will be to migrate

## Output Format

I'll provide a structured Discovery Report that can be used later for migration planning.

## Note

This is **read-only analysis**. I won't modify any files or perform the actual migration. Use `/migrate-api` when you're ready to proceed with migration.

## Ready?

Tell me which API you'd like me to analyze.
