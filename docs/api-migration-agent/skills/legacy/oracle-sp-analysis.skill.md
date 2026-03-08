# oracle-sp-analysis.skill.md

> Skill for analyzing Oracle stored procedure calls in legacy services.

---
domain: Oracle Stored Procedure Analysis
version: 1.0.0
applies_when:
  - Code contains CallableStatement usage
  - Code contains @Procedure annotations
  - Code contains StoredProcedureQuery usage
  - Oracle JDBC driver in dependencies
---

## Purpose

Identify and catalog all Oracle stored procedure calls made by a specific API, including parameters, return types, and complexity assessment.

## Conventions

### 1. SP Call Detection Patterns

#### Pattern A: JPA @Procedure Annotation

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    @Procedure(name = "PROC_CHANGE_PASSWORD")
    void changePassword(
        @Param("p_user_id") Long userId,
        @Param("p_old_password") String oldPassword,
        @Param("p_new_password") String newPassword
    );
}
```

#### Pattern B: StoredProcedureQuery (JPA)

```java
StoredProcedureQuery query = entityManager
    .createStoredProcedureQuery("PROC_GET_USER_PROFILE")
    .registerStoredProcedureParameter("p_user_id", Long.class, ParameterMode.IN)
    .registerStoredProcedureParameter("p_result", Class.class, ParameterMode.REF_CURSOR);
    
query.setParameter("p_user_id", userId);
query.execute();
```

#### Pattern C: CallableStatement (JDBC)

```java
try (CallableStatement cs = connection.prepareCall("{call PROC_VALIDATE_USER(?, ?, ?)}")) {
    cs.setLong(1, userId);
    cs.setString(2, password);
    cs.registerOutParameter(3, Types.INTEGER);
    cs.execute();
    int result = cs.getInt(3);
}
```

#### Pattern D: Spring SimpleJdbcCall

```java
SimpleJdbcCall jdbcCall = new SimpleJdbcCall(dataSource)
    .withProcedureName("PROC_UPDATE_USER")
    .declareParameters(
        new SqlParameter("p_user_id", Types.BIGINT),
        new SqlParameter("p_email", Types.VARCHAR),
        new SqlOutParameter("p_result", Types.INTEGER)
    );

Map<String, Object> result = jdbcCall.execute(userId, email);
```

### 2. Parameter Analysis

For each SP, document:

| Parameter | Direction | Oracle Type | Java Type | Notes |
|-----------|-----------|-------------|-----------|-------|
| p_user_id | IN | NUMBER | Long | Required |
| p_email | IN | VARCHAR2(100) | String | Optional, nullable |
| p_result | OUT | NUMBER | Integer | Status code |
| p_cursor | OUT | REF_CURSOR | ResultSet | Multi-row result |

### 3. Complexity Assessment

Rate SP complexity for migration planning:

| Complexity | Criteria | Migration Approach |
|------------|----------|-------------------|
| **SIMPLE** | Single table, basic CRUD, no cursor | Easy DECOMPOSE |
| **MODERATE** | Multi-table joins, single cursor, simple logic | DECOMPOSE with effort |
| **COMPLEX** | Multiple cursors, business logic, dynamic SQL | Consider KEEP_SP or STUB |
| **CRITICAL** | Transaction coordination, distributed logic | KEEP_SP recommended |

### 4. Business Logic Detection

Look for embedded business logic in SP calls:

```java
// Sign of business logic in SP
if (spResult.getStatusCode() == 2) {
    // SP returned "password policy violation"
    throw new PasswordPolicyException(spResult.getMessage());
}
```

This indicates the SP contains business rules that must be:
- Extracted to service layer (if DECOMPOSE)
- Replicated in application code (if API_CLIENT)
- Left as-is (if KEEP_SP)

## Patterns

### SP Catalog Entry

```yaml
stored_procedure:
  name: PROC_CHANGE_PASSWORD
  schema: APP_SCHEMA
  package: USER_PKG  # if in a package
  
  location:
    called_from_class: com.company.users.UserRepository
    called_from_method: changePassword
    call_pattern: JPA_PROCEDURE  # or CALLABLE_STATEMENT, STORED_PROCEDURE_QUERY
  
  parameters:
    - name: p_user_id
      direction: IN
      oracle_type: NUMBER
      java_type: Long
      nullable: false
      
    - name: p_old_password
      direction: IN
      oracle_type: VARCHAR2
      java_type: String
      nullable: false
      
    - name: p_new_password
      direction: IN
      oracle_type: VARCHAR2
      java_type: String
      nullable: false
      
    - name: p_result
      direction: OUT
      oracle_type: NUMBER
      java_type: Integer
      description: "0=success, 1=wrong password, 2=policy violation"
  
  returns:
    type: SCALAR  # or CURSOR, MULTIPLE_OUT, VOID
    description: "Status code indicating result"
  
  complexity: MODERATE
  complexity_reason: "Contains password validation rules, single table update"
  
  business_logic_detected: true
  business_logic_notes:
    - "Password history check (last 5 passwords)"
    - "Password complexity validation"
    - "Account lockout after failures"
    
  migration_recommendation: DECOMPOSE
  migration_notes: "Business logic should move to PasswordService"
```

## Anti-Patterns

- ❌ DO NOT attempt to read the actual SP source from Oracle - analyze from Java side only
- ❌ DO NOT execute SPs to understand behavior - static analysis only
- ❌ DO NOT assume all CallableStatements are SP calls - check for functions too
- ❌ DO NOT catalog SPs not called by the target API

## Search Patterns

```bash
# Find @Procedure annotations
grep -r "@Procedure" --include="*.java"

# Find CallableStatement usage
grep -r "CallableStatement\|prepareCall" --include="*.java"

# Find StoredProcedureQuery
grep -r "createStoredProcedureQuery\|StoredProcedureQuery" --include="*.java"

# Find SimpleJdbcCall
grep -r "SimpleJdbcCall\|withProcedureName" --include="*.java"
```

## Output

After analysis, produce SP catalog entries for all stored procedures called by the target API.
