# stub-generation.skill.md

> Skill for generating interface stubs when stored procedure implementation is deferred.

---
domain: Repository Stub Generation
version: 1.0.0
applies_when:
  - User chose STUB for stored procedure handling
  - Implementation will be done later
  - Need working interface to continue migration
---

## Purpose

Generate interface stubs with `@TODO` markers so the API migration can proceed. The actual implementation of data access will be completed in a future iteration.

## Conventions

### 1. When to Use STUB

Choose STUB when:
- Time pressure requires faster migration
- SP complexity is high and needs dedicated analysis
- Team needs to discuss decomposition vs keep-sp strategy
- Migration can proceed with mockable interface

### 2. Stub Interface Pattern

```java
/**
 * Repository interface for password management.
 * 
 * @TODO: STUB - Implementation pending
 * @see PROC_CHANGE_PASSWORD in legacy Oracle DB
 * @migration-ticket: JIRA-1234
 */
public interface PasswordRepository {

    /**
     * Change user's password with validation.
     * 
     * @TODO: Implement one of:
     *   - DECOMPOSE: Native queries + service logic (see sp-to-jdbc.skill.md)
     *   - KEEP_SP: Oracle SP call with dual datasource (see dual-datasource.skill.md)
     *   - API_CLIENT: Call external service (see sp-to-api-client.skill.md)
     * 
     * Legacy SP: PROC_CHANGE_PASSWORD(p_user_id, p_old_pwd, p_new_pwd, p_result)
     * SP Location: APP_SCHEMA.USER_PKG.PROC_CHANGE_PASSWORD
     * 
     * @param userId User ID
     * @param oldPassword Current password (plaintext)
     * @param newPassword New password (plaintext)
     * @return Result indicating success or failure reason
     */
    ChangePasswordResult changePassword(Long userId, String oldPassword, String newPassword);
}
```

### 3. Stub Implementation Options

#### Option A: Throw NotImplementedException (Default)

```java
@Repository
public class PasswordRepositoryStub implements PasswordRepository {

    @Override
    public ChangePasswordResult changePassword(Long userId, String oldPassword, String newPassword) {
        // @TODO: Implement password change
        // Migration ticket: JIRA-1234
        // Original SP: PROC_CHANGE_PASSWORD
        throw new NotImplementedException(
            "PasswordRepository.changePassword is not yet implemented. " +
            "See JIRA-1234 for implementation plan."
        );
    }
}
```

#### Option B: Return Mock Response (For Testing)

```java
@Repository
@Profile("stub")  // Only active with stub profile
public class PasswordRepositoryStub implements PasswordRepository {

    private static final Logger log = LoggerFactory.getLogger(PasswordRepositoryStub.class);

    @Override
    public ChangePasswordResult changePassword(Long userId, String oldPassword, String newPassword) {
        log.warn("STUB: changePassword called for user {}. Returning mock success.", userId);
        
        // @TODO: Replace with real implementation
        // Migration ticket: JIRA-1234
        return ChangePasswordResult.SUCCESS;
    }
}
```

#### Option C: Feature Flag Controlled

```java
@Repository
@RequiredArgsConstructor
public class PasswordRepositoryImpl implements PasswordRepository {

    private final FeatureFlags featureFlags;

    @Override
    public ChangePasswordResult changePassword(Long userId, String oldPassword, String newPassword) {
        if (!featureFlags.isEnabled("password-change-implemented")) {
            // @TODO: Remove stub when JIRA-1234 is complete
            throw new FeatureNotImplementedException("password-change");
        }
        
        // Real implementation goes here when ready
        return doChangePassword(userId, oldPassword, newPassword);
    }
    
    private ChangePasswordResult doChangePassword(Long userId, String oldPassword, String newPassword) {
        // @TODO: Implement
        throw new NotImplementedException("Implementation pending");
    }
}
```

### 4. Result Object

```java
/**
 * Result of password change operation.
 * 
 * Maps to legacy SP output:
 *   0 = SUCCESS
 *   1 = WRONG_PASSWORD
 *   2 = POLICY_VIOLATION
 *   3 = USER_NOT_FOUND
 */
public enum ChangePasswordResult {
    SUCCESS,
    WRONG_PASSWORD,
    POLICY_VIOLATION,
    USER_NOT_FOUND,
    NOT_IMPLEMENTED;  // Used by stubs
    
    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
```

### 5. Exception Classes

```java
/**
 * Thrown when a stubbed feature is called.
 */
public class NotImplementedException extends RuntimeException {
    
    private final String feature;
    private final String ticket;
    
    public NotImplementedException(String message) {
        super(message);
        this.feature = null;
        this.ticket = null;
    }
    
    public NotImplementedException(String feature, String ticket) {
        super(String.format("Feature '%s' is not implemented. See %s", feature, ticket));
        this.feature = feature;
        this.ticket = ticket;
    }
}

/**
 * Thrown when a feature flag controlled feature is not enabled.
 */
public class FeatureNotImplementedException extends RuntimeException {
    
    public FeatureNotImplementedException(String feature) {
        super(String.format("Feature '%s' is not yet implemented", feature));
    }
}
```

### 6. Controller Handling

```java
@RestController
@RequestMapping("/api/v1/users")
public class PasswordController {

    @ExceptionHandler(NotImplementedException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotImplemented(NotImplementedException ex) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(ApiResponse.error(ex.getMessage()));
    }
}
```

### 7. Documentation Generation

When creating stubs, also generate a tracking document:

```markdown
# Stub Implementation Tracker

## PasswordRepository

| Method | Status | Ticket | Original SP | Notes |
|--------|--------|--------|-------------|-------|
| changePassword | STUB | JIRA-1234 | PROC_CHANGE_PASSWORD | Pending decomposition decision |

### Implementation Decision Required

For `changePassword`, team needs to decide:
- [ ] DECOMPOSE - Extract SP logic to service + native queries
- [ ] KEEP_SP - Configure dual datasource
- [ ] API_CLIENT - Delegate to external service

### Legacy SP Details

```sql
-- PROC_CHANGE_PASSWORD
-- Parameters:
--   p_user_id IN NUMBER
--   p_old_password IN VARCHAR2
--   p_new_password IN VARCHAR2  
--   p_result OUT NUMBER (0=success, 1=wrong pwd, 2=policy violation)
-- Tables: USERS, PASSWORD_HISTORY
-- Business Logic: Password history check (last 5), complexity validation
```
```

## Patterns

### Stub Generation Task List

```yaml
stub_generation:
  sp_name: PROC_CHANGE_PASSWORD
  
  interface_to_create:
    name: PasswordRepository
    package: com.company.identity.application.port.out
    methods:
      - signature: "ChangePasswordResult changePassword(Long userId, String oldPassword, String newPassword)"
        javadoc: |
          Change user's password with validation.
          @TODO: STUB - Implementation pending (JIRA-1234)
          Legacy SP: PROC_CHANGE_PASSWORD
          
  stub_implementation:
    name: PasswordRepositoryStub
    package: com.company.identity.infrastructure.persistence.stub
    profile: "stub"  # or null for always-active
    behavior: THROW_NOT_IMPLEMENTED  # or RETURN_MOCK
    
  result_enum:
    name: ChangePasswordResult
    values: [SUCCESS, WRONG_PASSWORD, POLICY_VIOLATION, USER_NOT_FOUND, NOT_IMPLEMENTED]
    
  exception_classes:
    - NotImplementedException
    - FeatureNotImplementedException (if using feature flags)
    
  tracker_document:
    path: docs/migration/stub-tracker.md
    include_sp_details: true
    
  ticket_reference: JIRA-1234
```

## Anti-Patterns

- ❌ DO NOT leave stubs without @TODO comments
- ❌ DO NOT use stubs in production without visible indicators
- ❌ DO NOT forget to create tracking tickets
- ❌ DO NOT silently succeed in stub - fail loudly or log warnings
- ❌ DO NOT let stubs accumulate without cleanup plan

## Verification

After stub generation, verify:
1. Interface compile correctly
2. Stub throws NotImplementedException or returns mock
3. Controller handles NotImplementedException with 501
4. Tracker document is created
5. JIRA ticket exists and links to this stub
