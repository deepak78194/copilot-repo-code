# /migrate-api

> Entry point prompt for API migration.

---
mode: agent
agent: api-migration
description: Migrate an API from legacy service to target (microservice or another legacy service)
---

# API Migration

I'll help you migrate an API from a legacy Jakarta REST service to your target.

## What I Need From You

1. **Source API**: Which API endpoint to migrate
   - Service name (e.g., `user-service`)
   - API path (e.g., `/users/change-password`)
   - Or just describe it: "the change password API"

2. **Target Location**: Where to migrate
   - Microservice name (e.g., `identity` in replatforming)
   - Or another legacy service name

## Migration Types I Handle

- **Legacy → Microservice**: Jakarta REST to Spring Boot
- **Legacy → Legacy**: Move between legacy services
- **Stored Proc → API**: Replace SP call with REST client
- **SP Decomposition**: Convert SP to native queries

## What Happens Next

1. **Discovery Phase**: I'll analyze the source API, its dependencies, and any stored procedure calls
2. **Planning Phase**: I'll present a migration plan and ask for your approval on key decisions (especially how to handle stored procedures)
3. **Implementation**: After your approval, I'll generate the migrated code
4. **Verification**: I'll ensure the code compiles and basic structure is correct

## Example Commands

```
"migrate the changePassword API from user-service to identity"
"move getUserOrders from order-service to user-service"
"analyze the login API in auth-service for migration"
"replace PROC_VALIDATE_PAYMENT stored procedure with API call"
```

## Ready?

Tell me what you'd like to migrate, and I'll get started with the discovery phase.
