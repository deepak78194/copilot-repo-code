# dependency-mapping.skill.md

> Skill for mapping API-specific dependencies (not entire service dependencies).

---
domain: API-Scoped Dependency Analysis
version: 1.0.0
applies_when:
  - Performing migration discovery
  - Need to identify what an API depends on
  - Planning migration scope
---

## Purpose

Map the complete dependency tree for a specific API endpoint, stopping at service boundaries. This is NOT a full service dependency analysis - we only care about what THIS API needs.

## Conventions

### 1. Dependency Layers

```
┌─────────────────────────────────────────────────────────────────┐
│ API ENDPOINT                                                    │
│ UserResource.changePassword()                                   │
│                                                                 │
│   │                                                             │
│   ▼                                                             │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ SERVICE LAYER                                               │ │
│ │ UserService, PasswordService                                │ │
│ │                                                             │ │
│ │   │                                                         │ │
│ │   ▼                                                         │ │
│ │ ┌─────────────────────────────────────────────────────────┐ │ │
│ │ │ DATA ACCESS LAYER                                       │ │ │
│ │ │ UserRepository, PasswordHistoryRepository               │ │ │
│ │ │                                                         │ │ │
│ │ │   │                                                     │ │ │
│ │ │   ▼                                                     │ │ │
│ │ │ ┌─────────────────────────────────────────────────────┐ │ │ │
│ │ │ │ DATABASE                                            │ │ │ │
│ │ │ │ Tables: USER, PASSWORD_HISTORY                      │ │ │ │
│ │ │ │ SPs: PROC_CHANGE_PASSWORD                          │ │ │ │
│ │ │ └─────────────────────────────────────────────────────┘ │ │ │
│ │ └─────────────────────────────────────────────────────────┘ │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ EXTERNAL DEPENDENCIES                                       │ │
│ │ AuditService (via REST), NotificationService (via JMS)     │ │
│ └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### 2. Dependency Categories

| Category | Examples | Migration Impact |
|----------|----------|------------------|
| **INTERNAL_SERVICE** | UserService, PasswordValidator | Must migrate or reference |
| **REPOSITORY** | UserRepository, custom DAOs | Must recreate in target |
| **ENTITY** | User, PasswordHistory | Must map to target entities |
| **DTO** | ChangePasswordRequest, Response | May need new DTOs |
| **EXTERNAL_SERVICE** | AuditService via REST | Configure client in target |
| **MESSAGING** | JMS queues, Kafka topics | Configure messaging in target |
| **CONFIGURATION** | Properties, environment vars | Map to target config |
| **UTILITY** | StringUtils, DateUtils | Usually available or trivial |

### 3. Tracing Algorithm

```
START: API handler method

1. List direct dependencies (injected fields, method parameters)
2. For each SERVICE dependency:
   - List its injected dependencies
   - List methods called by our API path only
   - STOP at other APIs that use the same service
   
3. For each REPOSITORY dependency:
   - List methods called
   - List entities involved
   - List SP calls made
   
4. For EXTERNAL dependencies:
   - Document the integration point
   - Note the protocol (REST, JMS, JDBC to other DB)
   - DO NOT trace into external systems
```

### 4. Boundary Rules

**DO trace:**
- Services directly called by the API
- Repositories used by those services (for this API's code path)
- Entities loaded/saved
- DTOs used in requests/responses

**DO NOT trace:**
- Other APIs on the same service
- Services not called by this API
- Transitive dependencies beyond 2 levels
- Framework internals

## Patterns

### Dependency Map Entry

```yaml
api_dependency_map:
  api: POST /users/change-password
  handler: UserResource.changePassword
  
  services:
    - class: com.company.users.UserService
      methods_used:
        - changePassword(Long, String, String)
      injects:
        - UserRepository
        - PasswordHistoryRepository
        - PasswordValidator
        
    - class: com.company.users.PasswordValidator
      methods_used:
        - validate(String)
      injects: []
      notes: "Stateless utility, can be moved as-is"
  
  repositories:
    - class: com.company.users.UserRepository
      methods_used:
        - findById(Long)
        - updatePassword(Long, String)
      entities:
        - User
      stored_procedures:
        - PROC_CHANGE_PASSWORD
        
    - class: com.company.users.PasswordHistoryRepository
      methods_used:
        - findLastN(Long, int)
        - save(PasswordHistory)
      entities:
        - PasswordHistory
      stored_procedures: []
  
  entities:
    - class: com.company.users.entity.User
      table: APP_USER
      fields_used:
        - id
        - passwordHash
        - email
        
    - class: com.company.users.entity.PasswordHistory
      table: PASSWORD_HISTORY
      fields_used:
        - id
        - userId
        - passwordHash
        - createdAt
  
  dtos:
    - class: com.company.users.dto.ChangePasswordRequest
      fields: [userId, oldPassword, newPassword]
      validation: "@Valid with @NotNull, @Size"
      
    - class: com.company.users.dto.ChangePasswordResponse
      fields: [success, message]
  
  external_dependencies:
    - name: AuditService
      type: REST_CLIENT
      endpoint: http://audit-service/audit/log
      method: POST
      payload: AuditEvent
      
    - name: NotificationQueue
      type: JMS
      destination: queue/password-change-notifications
      payload: PasswordChangeEvent
  
  configuration:
    - key: password.history.count
      source: application.properties
      value: "5"
      description: "Number of historical passwords to check"
      
    - key: password.min.length
      source: application.properties
      value: "8"
      description: "Minimum password length"
  
  transitive_count:
    services: 2
    repositories: 2
    entities: 2
    external: 2
    
  migration_scope: MODERATE
  migration_notes:
    - "PasswordValidator is pure logic, easy to move"
    - "AuditService call needs REST client setup"
    - "JMS notification may need Kafka equivalent"
```

## Anti-Patterns

- ❌ DO NOT map the entire service's dependencies
- ❌ DO NOT follow dependencies for methods not called by this API
- ❌ DO NOT include dev/test dependencies
- ❌ DO NOT trace into framework code (Spring, Jakarta)
- ❌ DO NOT recursively analyze external services

## Search Patterns

```bash
# Find injected dependencies in a class
grep -rB2 -A10 "class UserService" --include="*.java" | grep "@Inject\|@Autowired\|@EJB"

# Find method calls from handler
grep -A50 "changePassword" UserResource.java | grep "\."

# Find entity usage
grep -r "User\." --include="*.java" UserService.java

# Find external calls
grep -r "RestTemplate\|WebClient\|@JmsListener\|jmsTemplate" --include="*.java"
```

## Output

After analysis, produce a dependency map in the format above, scoped to the specific API being migrated.
