# planning.skill.md

> Skill for designing migration plans with interactive user choice flow.

---
domain: API Migration Planning
version: 1.0.0
applies_when:
  - Discovery report is complete
  - Planning migration from legacy to target
  - User choices needed for stored procedure handling
---

## Purpose

Create a comprehensive migration plan based on the discovery report, with explicit user approval gates and interactive choices for stored procedure handling.

## Conventions

### 1. Plan Structure

Every migration plan follows this structure:

```markdown
# Migration Plan: [API Name]

## 1. Summary
- Source: [service/path]
- Target: [service/path]
- Migration Type: [Legacy→Microservice | Legacy→Legacy | SP→API | SP Decompose]
- Estimated Complexity: [Low | Medium | High]

## 2. API Redesign
[Mapping of old API contract to new]

## 3. Stored Procedure Decisions
[User choices for each SP encountered]

## 4. Implementation Tasks
[Ordered task breakdown]

## 5. Risk Register
[Identified risks and mitigations]

## 6. Configuration Changes
[Required config in target service]
```

### 2. User Choice Flow for Stored Procedures

For EACH stored procedure found in discovery, present this choice:

```markdown
### Stored Procedure: PROC_CHANGE_PASSWORD

**Current Usage:**
- Called from: UserRepository.changePassword()
- Parameters: p_user_id (IN), p_old_password (IN), p_new_password (IN), p_result (OUT)
- Complexity: MODERATE
- Contains Business Logic: Yes (password validation rules)

**How would you like to handle this?**

1. **KEEP_SP** - Keep calling Oracle stored procedure
   - Pros: No logic migration, fastest path
   - Cons: Requires dual datasource, Oracle dependency remains
   - Config: Add Oracle datasource to target service

2. **DECOMPOSE** - Convert to native SQL + service logic
   - Pros: Full Postgres, no Oracle dependency
   - Cons: Must extract and rewrite business logic
   - Work: Create PasswordService with validation rules, native queries

3. **STUB** - Create interface stub for later implementation
   - Pros: Fastest, defers decision
   - Cons: API won't fully work until implemented
   - Work: Generate repository interface with @TODO

4. **API_CLIENT** - Replace with call to another service
   - Pros: Delegates to service that already handles this
   - Cons: Network hop, requires target service to exist
   - Work: Create Feign/WebClient to target service

**Please reply with your choice: KEEP_SP, DECOMPOSE, STUB, or API_CLIENT**
```

### 3. Task Breakdown Rules

Order tasks by dependency:

```
1. Domain Layer (no dependencies)
   - Entities
   - Value Objects
   - Domain Events

2. Application Layer (depends on Domain)
   - Port interfaces (inbound)
   - Port interfaces (outbound)
   - Use case implementations

3. Infrastructure Layer (depends on Application)
   - Controllers (inbound adapters)
   - Repositories (outbound adapters)
   - Configuration
   - API clients (if API_CLIENT chosen)

4. Integration
   - Wire dependencies
   - Update application config
   - Migration tests
```

### 4. API Redesign Table

```markdown
| Legacy | Target | Changes |
|--------|--------|---------|
| POST /users/change-password | PUT /api/v1/users/{userId}/password | REST verb fix, path param |
| ChangePasswordRequest (body) | UpdatePasswordRequest (body) | Renamed DTO |
| Response with status code | Standard response envelope | Added wrapper |
```

### 5. Risk Register Format

```markdown
| Risk | Severity | Mitigation |
|------|----------|------------|
| Password validation logic in SP may be incomplete in extraction | HIGH | Review SP source before DECOMPOSE |
| Dual datasource adds complexity | MEDIUM | Use Spring's AbstractRoutingDataSource |
| JMS notification won't work in new service | MEDIUM | Replace with Kafka or REST callback |
```

## Patterns

### Complete Migration Plan Template

```yaml
migration_plan:
  metadata:
    api_name: changePassword
    source_service: user-service
    source_path: /users/change-password
    target_service: identity
    target_path: /api/v1/users/{userId}/password
    migration_type: LEGACY_TO_MICROSERVICE
    created_at: [timestamp]
    
  api_redesign:
    method_change: POST → PUT
    path_change: "/users/change-password" → "/api/v1/users/{userId}/password"
    request_changes:
      - "userId moves from body to path parameter"
      - "Request DTO renamed to UpdatePasswordRequest"
    response_changes:
      - "Wrap in standard ApiResponse envelope"
      - "Add correlationId for tracing"
      
  stored_procedure_decisions:
    - sp_name: PROC_CHANGE_PASSWORD
      user_choice: DECOMPOSE  # filled after user responds
      implementation_tasks:
        - "Extract password validation rules to PasswordPolicyService"
        - "Create native queries for password update"
        - "Create PasswordHistoryRepository with Postgres"
        
  implementation_tasks:
    - id: 1
      layer: DOMAIN
      task: "Create User entity (id, email, passwordHash)"
      depends_on: []
      
    - id: 2
      layer: DOMAIN
      task: "Create PasswordHistory entity"
      depends_on: []
      
    - id: 3
      layer: APPLICATION
      task: "Create UpdatePasswordUseCase interface (inbound port)"
      depends_on: [1, 2]
      
    - id: 4
      layer: APPLICATION
      task: "Create UserRepository interface (outbound port)"
      depends_on: [1]
      
    - id: 5
      layer: APPLICATION
      task: "Implement UpdatePasswordUseCaseImpl"
      depends_on: [3, 4]
      
    - id: 6
      layer: INFRASTRUCTURE
      task: "Create PasswordController (PUT /api/v1/users/{userId}/password)"
      depends_on: [3]
      
    - id: 7
      layer: INFRASTRUCTURE
      task: "Create JpaUserRepository implementing UserRepository"
      depends_on: [4]
      
    - id: 8
      layer: INFRASTRUCTURE
      task: "Add configuration for password policy"
      depends_on: []
      
  risks:
    - risk: "Password validation logic incomplete"
      severity: HIGH
      mitigation: "Compare with legacy PasswordValidator class"
      
  configuration_required:
    - key: "password.policy.min-length"
      value: "8"
      file: "application.yml"
      
    - key: "password.policy.history-count"
      value: "5"
      file: "application.yml"
      
  approval_status: PENDING  # changes to APPROVED after user confirms
```

## Anti-Patterns

- ❌ DO NOT proceed to implementation without explicit user approval
- ❌ DO NOT make SP handling decisions automatically - always ask user
- ❌ DO NOT skip the risk register
- ❌ DO NOT create tasks without clear dependencies
- ❌ DO NOT exceed 5 optimization suggestions (Innovation Budget)

## Gate: Plan Approval

After presenting the plan:

```markdown
---

## Approval Required

Please review the migration plan above.

**To approve and proceed to implementation, reply:**
"Approve migration plan" or "LGTM" or "Proceed"

**To request changes:**
Describe what you'd like changed and I'll update the plan.

**To cancel:**
"Cancel migration"

---
```

Do NOT proceed until user explicitly approves.
