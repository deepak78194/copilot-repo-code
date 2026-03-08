# sp-to-jdbc.skill.md

> Skill for decomposing Oracle stored procedures to native SQL queries with service logic.

---
domain: Stored Procedure to JDBC Decomposition
version: 1.0.0
applies_when:
  - User chose DECOMPOSE for stored procedure handling
  - Target database is PostgreSQL
  - SP logic needs to be extracted to application code
---

## Purpose

Convert Oracle stored procedure calls into:
1. Native PostgreSQL queries (using Spring Data JPA `@Query`)
2. Service layer business logic (extracted from SP)
3. Transaction management in application code

## Conventions

### 1. Analysis Before Decomposition

Before decomposing, verify you understand:

```
SP ANALYSIS CHECKLIST:
□ What tables are accessed (SELECT, INSERT, UPDATE, DELETE)?
□ What business logic is embedded (validations, calculations)?
□ What is the transaction boundary?
□ Are there cursors returning result sets?
□ Are there OUT parameters beyond status codes?
□ Is there dynamic SQL?
□ Are there calls to other SPs?
```

### 2. Query Extraction Patterns

#### Simple SELECT → Native Query

```java
// BEFORE: Oracle SP
// CREATE PROCEDURE GET_USER_BY_EMAIL(p_email IN VARCHAR2, p_cursor OUT SYS_REFCURSOR)
// AS BEGIN
//   OPEN p_cursor FOR SELECT * FROM users WHERE email = p_email;
// END;

// AFTER: Spring Data native query
@Query(value = "SELECT * FROM users WHERE email = :email", nativeQuery = true)
Optional<UserJpaEntity> findByEmail(@Param("email") String email);
```

#### INSERT/UPDATE → Native Query with Modifying

```java
// BEFORE: Oracle SP
// CREATE PROCEDURE UPDATE_PASSWORD(p_user_id IN NUMBER, p_password IN VARCHAR2)
// AS BEGIN
//   UPDATE users SET password_hash = p_password, updated_at = SYSDATE 
//   WHERE id = p_user_id;
// END;

// AFTER: Spring Data modifying query
@Modifying
@Query(value = """
    UPDATE users 
    SET password_hash = :passwordHash, updated_at = NOW() 
    WHERE id = :userId
    """, nativeQuery = true)
int updatePassword(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);
```

#### Complex Logic → Multiple Queries + Service

```java
// BEFORE: Oracle SP with business logic
// CREATE PROCEDURE CHANGE_PASSWORD(
//   p_user_id IN NUMBER, 
//   p_old_pwd IN VARCHAR2, 
//   p_new_pwd IN VARCHAR2,
//   p_result OUT NUMBER)
// AS
//   v_current_hash VARCHAR2(100);
//   v_history_count NUMBER;
// BEGIN
//   SELECT password_hash INTO v_current_hash FROM users WHERE id = p_user_id;
//   IF v_current_hash != hash_password(p_old_pwd) THEN
//     p_result := 1; -- wrong password
//     RETURN;
//   END IF;
//   SELECT COUNT(*) INTO v_history_count FROM password_history 
//     WHERE user_id = p_user_id AND password_hash = hash_password(p_new_pwd);
//   IF v_history_count > 0 THEN
//     p_result := 2; -- password already used
//     RETURN;
//   END IF;
//   UPDATE users SET password_hash = hash_password(p_new_pwd) WHERE id = p_user_id;
//   INSERT INTO password_history(user_id, password_hash) VALUES(p_user_id, hash_password(p_new_pwd));
//   p_result := 0; -- success
// END;

// AFTER: Service + Repository methods

// Repository
public interface UserRepository {
    Optional<User> findById(Long id);
    void updatePasswordHash(Long userId, String passwordHash);
}

public interface PasswordHistoryRepository {
    boolean existsByUserIdAndPasswordHash(Long userId, String passwordHash);
    void save(Long userId, String passwordHash);
}

// Service with extracted business logic
@Service
@RequiredArgsConstructor
@Transactional
public class ChangePasswordService {
    
    private final UserRepository userRepository;
    private final PasswordHistoryRepository historyRepository;
    private final PasswordEncoder passwordEncoder;
    
    public ChangePasswordResult changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        
        // Business logic: verify current password
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            return ChangePasswordResult.WRONG_PASSWORD;
        }
        
        // Business logic: check password history
        String newHash = passwordEncoder.encode(newPassword);
        if (historyRepository.existsByUserIdAndPasswordHash(userId, newHash)) {
            return ChangePasswordResult.PASSWORD_ALREADY_USED;
        }
        
        // Update password
        userRepository.updatePasswordHash(userId, newHash);
        historyRepository.save(userId, newHash);
        
        return ChangePasswordResult.SUCCESS;
    }
}
```

### 3. Oracle to PostgreSQL Query Translation

| Oracle | PostgreSQL |
|--------|------------|
| `SYSDATE` | `NOW()` or `CURRENT_TIMESTAMP` |
| `NVL(a, b)` | `COALESCE(a, b)` |
| `DECODE(a, b, c, d)` | `CASE WHEN a = b THEN c ELSE d END` |
| `ROWNUM <= n` | `LIMIT n` |
| `TO_DATE(str, fmt)` | `TO_DATE(str, fmt)` (formats differ) |
| `TO_CHAR(date, fmt)` | `TO_CHAR(date, fmt)` (formats differ) |
| `SUBSTR(s, start, len)` | `SUBSTRING(s FROM start FOR len)` |
| `INSTR(s, sub)` | `POSITION(sub IN s)` |
| `||` (concat) | `||` (same) |
| `NUMBER` | `NUMERIC` or `BIGINT` |
| `VARCHAR2(n)` | `VARCHAR(n)` |
| `CLOB` | `TEXT` |

### 4. Handling Cursors

```java
// Oracle cursor returning multiple rows
// OPEN p_cursor FOR SELECT id, email FROM users WHERE status = 'ACTIVE';

// PostgreSQL with Spring Data
@Query(value = "SELECT id, email FROM users WHERE status = 'ACTIVE'", nativeQuery = true)
List<Object[]> findActiveUsers();

// Or better, use projection
@Query(value = "SELECT u.id, u.email FROM users u WHERE u.status = 'ACTIVE'", nativeQuery = true)
List<UserIdEmailProjection> findActiveUsers();

public interface UserIdEmailProjection {
    Long getId();
    String getEmail();
}
```

### 5. Transaction Management

```java
// SP handled transactions internally - now we manage in service
@Service
@Transactional  // Default: REQUIRED
public class ChangePasswordService {
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPasswordChange(Long userId) {
        // Separate transaction for audit
    }
}
```

## Patterns

### Decomposition Task List Template

```yaml
decomposition_tasks:
  sp_name: PROC_CHANGE_PASSWORD
  
  queries_to_create:
    - name: findById
      type: SELECT
      table: users
      repository_method: "Optional<User> findById(Long id)"
      
    - name: updatePasswordHash
      type: UPDATE  
      table: users
      repository_method: "@Modifying void updatePasswordHash(Long userId, String hash)"
      
    - name: checkPasswordHistory
      type: SELECT
      table: password_history
      repository_method: "boolean existsByUserIdAndHash(Long userId, String hash)"
      
    - name: savePasswordHistory
      type: INSERT
      table: password_history
      repository_method: "void save(PasswordHistory history)"
  
  business_logic_to_extract:
    - name: "Password verification"
      description: "Compare old password with stored hash"
      target_class: ChangePasswordService
      
    - name: "Password history check"
      description: "Ensure password not recently used"
      target_class: ChangePasswordService
      
  transaction_boundary:
    scope: SERVICE_METHOD
    rollback_on: [RuntimeException]
```

## Anti-Patterns

- ❌ DO NOT execute SP to understand behavior - analyze from call site
- ❌ DO NOT translate Oracle PL/SQL syntax directly to Java
- ❌ DO NOT ignore transaction boundaries from the SP
- ❌ DO NOT put business logic in repository methods
- ❌ DO NOT use repository methods that return raw Object[] when projections exist

## Verification

After decomposition, verify:
1. All SP functionality is covered by new code
2. Transaction boundaries match original behavior
3. Error conditions are handled equivalently
4. Performance is acceptable (may need indexes)
