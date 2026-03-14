# Sub-Agent Model Benchmark — Orchestrator Prompt

> **Purpose:** This is a self-contained orchestrator prompt. Pass this entire file to a high-capability agent (Claude Opus 4.6 recommended). It will autonomously execute all benchmark scenarios across all contestant models, evaluate every output, and produce a final ranked scorecard — no human involvement required.

---

## YOUR ROLE

You are the **Benchmark Orchestrator**. Your job is to:

1. Run 4 test scenarios across 9 contestant models using sub-agents
2. Collect every sub-agent's output
3. Evaluate each output against a detailed rubric with 10 scoring dimensions
4. Produce a final comparison report with rankings and a recommendation

**You must complete all of this autonomously. Do not ask the user for input at any point.**

---

## PHASE 1: CONTESTANT MODELS & AGENT MAPPING

Each contestant model has a dedicated custom agent file in `.github/agents/`. Each agent file pins the `model` field in YAML frontmatter to the specific model. You invoke them via `runSubagent` with the `agentName` parameter.

| ID | Model | Agent Name (`agentName`) | Multiplier | Context | Tier |
|----|-------|--------------------------|-----------|---------|------|
| M1 | GPT-4.1 | `bench-gpt41` | 0x | 128K | Free |
| M2 | GPT-5 mini | `bench-gpt5mini` | 0x | 192K | Free |
| M3 | Raptor mini (Preview) | `bench-raptor` | 0x | 264K | Free |
| M4 | Claude Haiku 4.5 | `bench-haiku` | 0.33x | 160K | Budget |
| M5 | Gemini 3 Flash (Preview) | `bench-gemini-flash` | 0.33x | 173K | Budget |
| M6 | GPT-5.1-Codex-Mini (Preview) | `bench-codex-mini` | 0.33x | 256K | Budget |
| M7 | Claude Sonnet 4.6 | `bench-sonnet` | 1x | 160K | Reference |
| M8 | GPT-5.2 | `bench-gpt52` | 1x | 192K | Reference |
| M9 | GPT-5.4 | `bench-gpt54` | 1x | 400K | Reference |

### Agent File Location

All benchmark agent files are at:
```
.github/agents/
  bench-gpt41.agent.md
  bench-gpt5mini.agent.md
  bench-raptor.agent.md
  bench-haiku.agent.md
  bench-gemini-flash.agent.md
  bench-codex-mini.agent.md
  bench-sonnet.agent.md
  bench-gpt52.agent.md
  bench-gpt54.agent.md
```

Each file has `user-invocable: false` (hidden from picker, only callable as sub-agent) and a minimal system prompt that instructs the model to follow task instructions exactly.

---

## PHASE 2: EXECUTION STRATEGY — PARALLEL BATCHING

### Why Batching?

Running all 36 calls (9 models × 4 scenarios) at once risks context overflow and makes output tracking harder. Instead, execute in **4 batches** — one batch per scenario. Within each batch, run all 9 models in parallel.

### Execution Order

```
BATCH 1: Scenario S1 (Context Gathering)
  └─ Run sub-agents: M1-S1, M2-S1, M3-S1, M4-S1, M5-S1, M6-S1, M7-S1, M8-S1, M9-S1
  └─ Collect all 9 outputs
  └─ Score all 9 outputs against S1 rubric
  └─ Store scores

BATCH 2: Scenario S2 (Code Analysis)
  └─ Run sub-agents: M1-S2, M2-S2, M3-S2, M4-S2, M5-S2, M6-S2, M7-S2, M8-S2, M9-S2
  └─ Collect all 9 outputs
  └─ Score all 9 outputs against S2 rubric
  └─ Store scores

BATCH 3: Scenario S3 (Code Generation)
  └─ Run sub-agents: M1-S3, M2-S3, M3-S3, M4-S3, M5-S3, M6-S3, M7-S3, M8-S3, M9-S3
  └─ Collect all 9 outputs
  └─ Score all 9 outputs against S3 rubric
  └─ Store scores

BATCH 4: Scenario S4 (Architecture Design)
  └─ Run sub-agents: M1-S4, M2-S4, M3-S4, M4-S4, M5-S4, M6-S4, M7-S4, M8-S4, M9-S4
  └─ Collect all 9 outputs
  └─ Score all 9 outputs against S4 rubric
  └─ Store scores

FINAL: Aggregate all scores → Produce report
```

### Sub-Agent Invocation Rules

For each sub-agent call, use the `runSubagent` tool with these parameters:

```
runSubagent(
  agentName: "bench-gpt41",         ← from the Agent Name column above
  description: "M1-S1 GPT-4.1 Extract",  ← short label for tracking
  prompt: "<exact scenario prompt>"       ← the FULL prompt text from Phase 3
)
```

**Rules:**

1. **Set `agentName`** to the exact value from the table above (e.g., `bench-gpt41`, `bench-haiku`, etc.)
2. **Set `description`** to `{ModelID}-{ScenarioID} {ModelName} {ScenarioShortName}` (e.g., `M1-S1 GPT-4.1 Extract`)
3. **Set `prompt`** to the EXACT prompt text from the scenario section below — do not modify it
4. **Each sub-agent gets ONLY the scenario prompt** — no benchmark context, no scoring rubric, no awareness of the benchmarking process
5. **Within a batch, launch all 9 sub-agents in parallel** — they have zero dependencies on each other
6. **Between batches, wait for ALL outputs** before starting the next batch
7. **Record the approximate time** each sub-agent takes (if observable) or note "not measurable" if the platform doesn't expose timing

### Example: Batch 1 Parallel Invocation

For Batch 1 (Scenario S1), launch ALL 9 of these `runSubagent` calls simultaneously:

```
runSubagent(agentName: "bench-gpt41",       description: "M1-S1", prompt: "<S1 prompt>")
runSubagent(agentName: "bench-gpt5mini",    description: "M2-S1", prompt: "<S1 prompt>")
runSubagent(agentName: "bench-raptor",      description: "M3-S1", prompt: "<S1 prompt>")
runSubagent(agentName: "bench-haiku",       description: "M4-S1", prompt: "<S1 prompt>")
runSubagent(agentName: "bench-gemini-flash",description: "M5-S1", prompt: "<S1 prompt>")
runSubagent(agentName: "bench-codex-mini",  description: "M6-S1", prompt: "<S1 prompt>")
runSubagent(agentName: "bench-sonnet",      description: "M7-S1", prompt: "<S1 prompt>")
runSubagent(agentName: "bench-gpt52",       description: "M8-S1", prompt: "<S1 prompt>")
runSubagent(agentName: "bench-gpt54",       description: "M9-S1", prompt: "<S1 prompt>")
```

Wait for ALL 9 to return, then score, then proceed to Batch 2.

### Anti-Overlap Guarantee

- Each sub-agent runs in its own isolated context — no shared state
- Sub-agents within the same batch receive identical prompts — only the model differs
- No sub-agent in Batch N depends on output from Batch N-1
- The orchestrator (you) is the only entity that sees all outputs

---

## PHASE 3: THE 4 TEST SCENARIOS

### Scenario S1: Jakarta REST Endpoint Extraction

**Category:** Context Gathering — Can the sub-agent read code and return structured data?

**Exact prompt to send to each sub-agent:**

```
Analyze the following Jakarta REST controller and return a JSON object with this exact structure:

{
  "basePath": "string — class-level @Path value",
  "endpoints": [
    {
      "httpMethod": "GET|POST|PUT|DELETE|PATCH",
      "path": "method-level @Path value",
      "consumes": "media type (inherit from class if not on method)",
      "produces": "media type (inherit from class if not on method)",
      "parameters": [
        { "name": "param name", "source": "@PathParam|@QueryParam|@HeaderParam|@FormParam|body", "type": "Java type" }
      ],
      "returnType": "return type"
    }
  ],
  "injectedServices": [
    { "name": "field name", "type": "class type", "injection": "@Inject|@EJB|@Resource" }
  ]
}

Return ONLY the JSON. No explanation, no markdown, no commentary.

Here is the code:

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    private UserService userService;

    @EJB
    private LegacyUserBean legacyBean;

    @Resource
    private DataSource dataSource;

    @POST
    @Path("/change-password")
    public Response changePassword(ChangePasswordRequest request) {
        int result = legacyBean.changePassword(
            request.getUserId(), request.getOldPassword(), request.getNewPassword());
        if (result == 0) return Response.ok().build();
        if (result == 1) return Response.status(Status.UNAUTHORIZED).entity("Wrong password").build();
        return Response.status(Status.CONFLICT).entity("Password already used").build();
    }

    @GET
    @Path("/{id}")
    public Response getUser(@PathParam("id") Long id, @QueryParam("include") String include) {
        UserDto user = userService.findById(id);
        if (user == null) return Response.status(Status.NOT_FOUND).build();
        return Response.ok(user).build();
    }

    @PUT
    @Path("/{id}/email")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateEmail(@PathParam("id") Long id, @FormParam("email") String email,
                                @HeaderParam("X-Request-Id") String requestId) {
        userService.updateEmail(id, email);
        return Response.ok().build();
    }
}
```

**S1 Ground Truth (for orchestrator evaluation only — NOT sent to sub-agent):**

```json
{
  "basePath": "/users",
  "endpoints": [
    {
      "httpMethod": "POST",
      "path": "/change-password",
      "consumes": "application/json",
      "produces": "application/json",
      "parameters": [
        { "name": "request", "source": "body", "type": "ChangePasswordRequest" }
      ],
      "returnType": "Response"
    },
    {
      "httpMethod": "GET",
      "path": "/{id}",
      "consumes": "application/json",
      "produces": "application/json",
      "parameters": [
        { "name": "id", "source": "@PathParam", "type": "Long" },
        { "name": "include", "source": "@QueryParam", "type": "String" }
      ],
      "returnType": "Response"
    },
    {
      "httpMethod": "PUT",
      "path": "/{id}/email",
      "consumes": "application/x-www-form-urlencoded",
      "produces": "application/json",
      "parameters": [
        { "name": "id", "source": "@PathParam", "type": "Long" },
        { "name": "email", "source": "@FormParam", "type": "String" },
        { "name": "requestId", "source": "@HeaderParam", "type": "String" }
      ],
      "returnType": "Response"
    }
  ],
  "injectedServices": [
    { "name": "userService", "type": "UserService", "injection": "@Inject" },
    { "name": "legacyBean", "type": "LegacyUserBean", "injection": "@EJB" },
    { "name": "dataSource", "type": "DataSource", "injection": "@Resource" }
  ]
}
```

---

### Scenario S2: Oracle SP Call Pattern Analysis

**Category:** Code Analysis — Can the sub-agent detect patterns, classify complexity, and recommend strategies?

**Exact prompt to send to each sub-agent:**

```
Analyze the following Java EJB service class. Identify ALL Oracle stored procedure calls.

For each stored procedure call, return a JSON array with objects containing:
- "spName": the stored procedure name
- "callPattern": one of "JPA_PROCEDURE", "STORED_PROCEDURE_QUERY", "CALLABLE_STATEMENT"
- "parameters": array of { "name", "type", "direction": "IN|OUT|INOUT|REF_CURSOR" }
- "complexity": "SIMPLE" | "MODERATE" | "COMPLEX" with reasoning
- "tablesAccessed": best guess of tables the SP likely touches
- "recommendedStrategy": "DECOMPOSE" or "KEEP_SP" with one-sentence justification

Return ONLY the JSON array. No explanation, no markdown.

Here is the code:

@Stateless
public class UserServiceBean {

    @PersistenceContext
    private EntityManager entityManager;

    @Resource
    private DataSource dataSource;

    @Inject
    private AuditService auditService;

    // SP Call 1: User profile lookup via JPA StoredProcedureQuery
    public UserProfile getUserProfile(Long userId) {
        StoredProcedureQuery query = entityManager
            .createStoredProcedureQuery("PROC_GET_USER_PROFILE")
            .registerStoredProcedureParameter("p_user_id", Long.class, ParameterMode.IN)
            .registerStoredProcedureParameter("p_result", void.class, ParameterMode.REF_CURSOR);
        query.setParameter("p_user_id", userId);
        query.execute();
        List results = query.getResultList();
        return mapToProfile(results);
    }

    // SP Call 2: Complex password change with business logic and OUT param
    public int changePassword(Long userId, String oldPwd, String newPwd) {
        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall("{call PROC_CHANGE_PASSWORD(?, ?, ?, ?)}")) {
            cs.setLong(1, userId);
            cs.setString(2, oldPwd);
            cs.setString(3, newPwd);
            cs.registerOutParameter(4, Types.INTEGER);
            cs.execute();
            return cs.getInt(4);
        } catch (SQLException e) {
            throw new ServiceException("Password change failed", e);
        }
    }

    // SP Call 3: Audit logging - fire and forget INSERT
    public void logAuditEvent(Long userId, String action) {
        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall("{call PROC_LOG_AUDIT(?, ?, ?)}")) {
            cs.setLong(1, userId);
            cs.setString(2, action);
            cs.setTimestamp(3, Timestamp.from(Instant.now()));
            cs.execute();
        } catch (SQLException e) {
            // Fire-and-forget: log error but don't throw
            logger.warn("Audit logging failed for user {}", userId, e);
        }
    }

    // SP Call 4: Batch user deactivation with cursor + DML
    public List<Long> deactivateInactiveUsers(int daysThreshold) {
        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall("{call PROC_DEACTIVATE_INACTIVE(?, ?, ?)}")) {
            cs.setInt(1, daysThreshold);
            cs.registerOutParameter(2, OracleTypes.CURSOR);
            cs.registerOutParameter(3, Types.INTEGER);
            cs.execute();
            ResultSet rs = (ResultSet) cs.getObject(2);
            List<Long> deactivatedIds = new ArrayList<>();
            while (rs.next()) {
                deactivatedIds.add(rs.getLong("user_id"));
            }
            return deactivatedIds;
        } catch (SQLException e) {
            throw new ServiceException("Deactivation failed", e);
        }
    }
}
```

**S2 Ground Truth (for orchestrator evaluation only):**

```json
[
  {
    "spName": "PROC_GET_USER_PROFILE",
    "callPattern": "STORED_PROCEDURE_QUERY",
    "parameters": [
      { "name": "p_user_id", "type": "Long", "direction": "IN" },
      { "name": "p_result", "type": "void", "direction": "REF_CURSOR" }
    ],
    "complexity": "SIMPLE",
    "complexityReasoning": "Single SELECT returning a cursor, no DML, no business logic",
    "tablesAccessed": ["users (or user_profiles)"],
    "recommendedStrategy": "DECOMPOSE",
    "strategyJustification": "Simple SELECT easily replaced with Spring Data native query"
  },
  {
    "spName": "PROC_CHANGE_PASSWORD",
    "callPattern": "CALLABLE_STATEMENT",
    "parameters": [
      { "name": "userId", "type": "Long", "direction": "IN" },
      { "name": "oldPwd", "type": "String", "direction": "IN" },
      { "name": "newPwd", "type": "String", "direction": "IN" },
      { "name": "result", "type": "Integer", "direction": "OUT" }
    ],
    "complexity": "COMPLEX",
    "complexityReasoning": "Contains validation logic, history check, UPDATE + INSERT — multiple operations with branching",
    "tablesAccessed": ["users", "password_history"],
    "recommendedStrategy": "DECOMPOSE",
    "strategyJustification": "Business logic (validation, history) should live in application code, not in database"
  },
  {
    "spName": "PROC_LOG_AUDIT",
    "callPattern": "CALLABLE_STATEMENT",
    "parameters": [
      { "name": "userId", "type": "Long", "direction": "IN" },
      { "name": "action", "type": "String", "direction": "IN" },
      { "name": "timestamp", "type": "Timestamp", "direction": "IN" }
    ],
    "complexity": "SIMPLE",
    "complexityReasoning": "Fire-and-forget INSERT, no output, no branching",
    "tablesAccessed": ["audit_log (or audit_events)"],
    "recommendedStrategy": "DECOMPOSE",
    "strategyJustification": "Simple INSERT trivially replaced with JPA save or native query"
  },
  {
    "spName": "PROC_DEACTIVATE_INACTIVE",
    "callPattern": "CALLABLE_STATEMENT",
    "parameters": [
      { "name": "daysThreshold", "type": "Integer", "direction": "IN" },
      { "name": "cursor", "type": "ResultSet", "direction": "REF_CURSOR" },
      { "name": "count", "type": "Integer", "direction": "OUT" }
    ],
    "complexity": "COMPLEX",
    "complexityReasoning": "Combines DML (deactivation UPDATE/DELETE) with cursor output and a count — multi-step with side effects",
    "tablesAccessed": ["users"],
    "recommendedStrategy": "DECOMPOSE",
    "strategyJustification": "Batch operation with cursor can be replaced with native query + @Modifying, keeping logic visible in application layer"
  }
]
```

---

### Scenario S3: SP-to-JDBC Migration Code Generation

**Category:** Code Generation — Can the sub-agent produce correct, convention-following Spring Boot code?

**Exact prompt to send to each sub-agent:**

```
You are a Java migration specialist. Migrate the following Oracle stored procedure to Spring Boot 3 application code.

REQUIREMENTS:
- Target database: PostgreSQL
- Strategy: DECOMPOSE (extract all logic to application code, no stored procedures)
- Architecture: Hexagonal — repositories in infrastructure/persistence/, services in application/service/
- Use @RequiredArgsConstructor (Lombok) for dependency injection
- Use Java records for command/result objects
- Use PasswordEncoder (Spring Security) for hashing — never hash in raw SQL
- Use @Transactional on the service method
- Repository must use Spring Data JPA with @Query(nativeQuery = true) for PostgreSQL
- Use PostgreSQL syntax: NOW() not SYSDATE, standard SQL types
- Return a ChangePasswordResult enum: SUCCESS, WRONG_PASSWORD, PASSWORD_ALREADY_USED, USER_NOT_FOUND

Generate these files with COMPLETE code (all imports, annotations, no placeholders, no "// TODO"):
1. ChangePasswordCommand.java (record)
2. ChangePasswordResult.java (enum)
3. UserRepository.java (Spring Data interface)
4. PasswordHistoryRepository.java (Spring Data interface)
5. ChangePasswordService.java (service with business logic)

Here is the Oracle stored procedure to decompose:

CREATE OR REPLACE PROCEDURE PROC_CHANGE_PASSWORD(
  p_user_id   IN  NUMBER,
  p_old_pwd   IN  VARCHAR2,
  p_new_pwd   IN  VARCHAR2,
  p_result    OUT NUMBER
) AS
  v_current_hash VARCHAR2(100);
  v_history_count NUMBER;
BEGIN
  -- Step 1: Get current password hash
  SELECT password_hash INTO v_current_hash FROM users WHERE id = p_user_id;

  -- Step 2: Verify old password
  IF v_current_hash != hash_password(p_old_pwd) THEN
    p_result := 1;  -- Wrong password
    RETURN;
  END IF;

  -- Step 3: Check password history
  SELECT COUNT(*) INTO v_history_count
    FROM password_history
    WHERE user_id = p_user_id AND password_hash = hash_password(p_new_pwd);

  IF v_history_count > 0 THEN
    p_result := 2;  -- Password already used
    RETURN;
  END IF;

  -- Step 4: Update password
  UPDATE users SET password_hash = hash_password(p_new_pwd), updated_at = SYSDATE
    WHERE id = p_user_id;

  -- Step 5: Record in password history
  INSERT INTO password_history(user_id, password_hash, created_at)
    VALUES(p_user_id, hash_password(p_new_pwd), SYSDATE);

  p_result := 0;  -- Success
  COMMIT;

EXCEPTION
  WHEN NO_DATA_FOUND THEN
    p_result := 3;  -- User not found
END;
```

**S3 Ground Truth Checklist (for orchestrator evaluation only):**

The output must satisfy ALL of these:

**Structural requirements:**
- [ ] 5 separate Java files/classes produced
- [ ] ChangePasswordCommand is a `record` with fields: userId (Long), oldPassword (String), newPassword (String)
- [ ] ChangePasswordResult is an `enum` with values: SUCCESS, WRONG_PASSWORD, PASSWORD_ALREADY_USED, USER_NOT_FOUND
- [ ] UserRepository extends JpaRepository or CrudRepository
- [ ] PasswordHistoryRepository extends JpaRepository or CrudRepository
- [ ] ChangePasswordService is annotated with @Service and @RequiredArgsConstructor

**Logic correctness:**
- [ ] Step 1: Fetch user by ID, handle USER_NOT_FOUND
- [ ] Step 2: Use `passwordEncoder.matches(oldPassword, user.getPasswordHash())` — NOT raw string comparison
- [ ] Step 3: Check password history — encode new password then check if it exists
- [ ] Step 4: Update password hash in users table
- [ ] Step 5: Insert into password_history
- [ ] Returns correct enum value for each branch
- [ ] Order of operations matches the SP: fetch → verify → history check → update → insert history

**Convention compliance:**
- [ ] @Transactional on service method or class
- [ ] @Query with nativeQuery = true (if using native queries) OR Spring Data derived queries
- [ ] NOW() used instead of SYSDATE in any native SQL
- [ ] No raw SQL string comparison for password — uses PasswordEncoder
- [ ] @RequiredArgsConstructor for constructor injection (no @Autowired)
- [ ] Complete imports — no missing imports

**Safety:**
- [ ] No password logged or exposed in exceptions
- [ ] Parameterized queries (no string concatenation in SQL)
- [ ] PasswordEncoder used for both encoding AND matching

---

### Scenario S4: Dual-Datasource Architecture Design

**Category:** Architecture & Reasoning — Can the sub-agent design a production-quality Spring configuration?

**Exact prompt to send to each sub-agent:**

```
You are a Spring Boot architect. Design a dual-datasource configuration for a microservice that is being migrated from Oracle to PostgreSQL.

CONTEXT:
- During migration, the service needs BOTH databases:
  - PostgreSQL (PRIMARY): All new entities, native queries, application data
  - Oracle (SECONDARY): Retained stored procedure calls only (temporary, until all SPs are decomposed)
- Framework: Spring Boot 3.2+, Java 21, HikariCP connection pool

REQUIREMENTS:
1. DatasourceConfig.java — Complete @Configuration class with:
   - @Primary on all PostgreSQL beans
   - Separate DataSourceProperties, DataSource, EntityManagerFactory, TransactionManager for PostgreSQL
   - Separate DataSourceProperties, DataSource, JdbcTemplate, TransactionManager for Oracle
   - @Qualifier annotations to prevent ambiguity
   - @ConfigurationProperties binding for each datasource
   - HikariDataSource as pool implementation

2. application.yml — Complete configuration with:
   - Environment variable placeholders (${POSTGRES_HOST}, ${ORACLE_HOST}, etc.) with sensible defaults
   - Separate HikariCP pool configuration (PostgreSQL: max 10, Oracle: max 5)
   - PostgreSQL dialect for Hibernate

3. OracleStoredProcRepository.java — Example repository showing:
   - How to use @Qualifier("oracleJdbcTemplate") to get the Oracle JdbcTemplate
   - A sample stored procedure call using SimpleJdbcCall
   - @Transactional("oracleTransactionManager") annotation

Generate COMPLETE code for all 3 files. No placeholders, no TODOs, all imports included.
```

**S4 Ground Truth Checklist (for orchestrator evaluation only):**

**DatasourceConfig.java must have:**
- [ ] @Configuration and @EnableTransactionManagement annotations
- [ ] postgresDataSourceProperties() — @Primary @Bean @ConfigurationProperties("spring.datasource.postgres")
- [ ] postgresDataSource() — @Primary @Bean, returns HikariDataSource
- [ ] entityManagerFactory() — @Primary @Bean, uses @Qualifier("postgresDataSource"), configures packages to scan
- [ ] transactionManager() — @Primary @Bean, JpaTransactionManager
- [ ] oracleDataSourceProperties() — @Bean @ConfigurationProperties("spring.datasource.oracle")
- [ ] oracleDataSource() — @Bean, returns HikariDataSource
- [ ] oracleJdbcTemplate() — @Bean, uses @Qualifier("oracleDataSource")
- [ ] oracleTransactionManager() — @Bean, DataSourceTransactionManager with @Qualifier("oracleDataSource")

**application.yml must have:**
- [ ] spring.datasource.postgres.* with ${POSTGRES_HOST}, ${POSTGRES_DB}, ${POSTGRES_USER}, ${POSTGRES_PASSWORD} and defaults
- [ ] spring.datasource.oracle.* with ${ORACLE_HOST}, ${ORACLE_SID}, ${ORACLE_USER}, ${ORACLE_PASSWORD} and defaults
- [ ] driver-class-name for both (org.postgresql.Driver, oracle.jdbc.OracleDriver)
- [ ] Separate hikari pool configs — postgres max 10, oracle max 5 (approximately)
- [ ] spring.jpa.properties.hibernate.dialect = PostgreSQLDialect

**OracleStoredProcRepository.java must have:**
- [ ] @Repository annotation
- [ ] @Qualifier("oracleJdbcTemplate") on JdbcTemplate injection
- [ ] @Transactional("oracleTransactionManager") on method
- [ ] SimpleJdbcCall usage with declareParameters and SqlParameter/SqlOutParameter
- [ ] Proper parameter binding with MapSqlParameterSource

**Architecture quality:**
- [ ] No circular dependencies between beans
- [ ] Clear separation: JPA/EntityManager for PostgreSQL, JdbcTemplate for Oracle
- [ ] Would compile and start without ambiguous bean errors

---

## PHASE 4: EVALUATION RUBRIC — 10 SCORING DIMENSIONS

After collecting each sub-agent's output, score it on these 10 dimensions. Each dimension is scored 1–5 (1 = terrible, 5 = excellent). Total possible: 50 per scenario, 200 overall.

### Dimension Definitions

| # | Dimension | Weight | 1 (Terrible) | 3 (Acceptable) | 5 (Excellent) |
|---|-----------|--------|---------------|-----------------|----------------|
| D1 | **Code Correctness** | 2x | Won't compile, syntax errors, wrong logic | Compiles with minor fixes, logic mostly right | Compiles as-is, logic exactly matches requirements |
| D2 | **Architecture Correctness** | 2x | Wrong patterns, violations of requested architecture | Mostly correct structure, minor deviations | Exact match to requested architecture (hexagonal, annotations, etc.) |
| D3 | **Completeness** | 1.5x | Major pieces missing (no imports, placeholder code, TODOs) | All pieces present but some gaps | Everything requested is present, nothing extra needed |
| D4 | **Instruction Following** | 1.5x | Ignored format/structure requirements, added commentary when told not to | Followed most instructions, minor deviations | Followed every instruction precisely (format, naming, structure) |
| D5 | **Safety & Security** | 1.5x | SQL injection risks, passwords in logs, hardcoded secrets | No obvious vulnerabilities but not hardened | Parameterized queries, PasswordEncoder used, no secrets exposed, env vars |
| D6 | **Convention Compliance** | 1x | Ignores stated conventions (annotations, patterns) | Follows most conventions | Matches all stated conventions exactly |
| D7 | **PostgreSQL Correctness** | 1x | Uses Oracle syntax (SYSDATE, NUMBER, VARCHAR2) | Mostly PostgreSQL but slips in Oracle syntax | Pure PostgreSQL syntax throughout |
| D8 | **Reasoning Quality** | 1x | No explanation or wrong reasoning for decisions | Reasoning present but superficial | Clear, correct reasoning for every decision (complexity, strategy) |
| D9 | **Output Structure** | 0.5x | Unstructured dump, hard to parse | Readable but not in requested format | Exact requested format (JSON, file separation, etc.) |
| D10 | **Conciseness** | 0.5x | Massive preamble/postamble, repeated explanations | Some unnecessary content but mostly focused | Only what was asked, no filler |

### Weighted Scoring Formula

```
Weighted Score = (D1×2 + D2×2 + D3×1.5 + D4×1.5 + D5×1.5 + D6×1 + D7×1 + D8×1 + D9×0.5 + D10×0.5) 
Maximum weighted score per scenario = (5×2 + 5×2 + 5×1.5 + 5×1.5 + 5×1.5 + 5×1 + 5×1 + 5×1 + 5×0.5 + 5×0.5) = 62.5
```

### Scenario-Specific Dimension Applicability

Not every dimension applies equally to every scenario. Use this matrix:

| Dimension | S1 (Extract) | S2 (Analyze) | S3 (Generate) | S4 (Architect) |
|-----------|:---:|:---:|:---:|:---:|
| D1 Code Correctness | Low (JSON) | Low (JSON) | **Critical** | **Critical** |
| D2 Architecture Correctness | N/A | Low | **Critical** | **Critical** |
| D3 Completeness | High | High | **Critical** | **Critical** |
| D4 Instruction Following | **Critical** | **Critical** | High | High |
| D5 Safety & Security | N/A | Low | **Critical** | High |
| D6 Convention Compliance | Low | Low | **Critical** | **Critical** |
| D7 PostgreSQL Correctness | N/A | N/A | **Critical** | High |
| D8 Reasoning Quality | Low | **Critical** | Low | High |
| D9 Output Structure | **Critical** | **Critical** | High | High |
| D10 Conciseness | **Critical** | **Critical** | Medium | Medium |

When a dimension is "N/A" for a scenario, score it 3 (neutral) and note "N/A — scored neutral".

---

## PHASE 5: OUTPUT REPORT FORMAT

After all 4 batches are complete, produce a single comprehensive report with these sections:

### Section A: Raw Scores Table

```
| Model | Tier | S1 Score | S2 Score | S3 Score | S4 Score | Total | Weighted Total |
|-------|------|----------|----------|----------|----------|-------|----------------|
| ...   | ...  | .../62.5 | .../62.5 | .../62.5 | .../62.5 | .../250 | ...          |
```

### Section B: Dimension Breakdown Per Model

For each model, show scores across all 10 dimensions for all 4 scenarios:

```
### Model: GPT-4.1 (0x, Free)

| Dimension | S1 | S2 | S3 | S4 | Avg |
|-----------|----|----|----|----|-----|
| D1 Code Correctness | _ | _ | _ | _ | _ |
| D2 Architecture | _ | _ | _ | _ | _ |
| ... | | | | | |
```

### Section C: Cost-Efficiency Ranking

```
Cost-Adjusted Score = Weighted Total ÷ Cost Factor

Where Cost Factor:
  0x models  → 0.1 (not zero, to avoid divide-by-zero)
  0.33x      → 0.33
  1x         → 1.0

Higher = better value.
```

| Rank | Model | Weighted Total | Multiplier | Cost-Adjusted Score |
|------|-------|---------------|------------|---------------------|
| 1 | ... | ... | ... | ... |

### Section D: Tier Winners

```
Best Free Model (0x):        _____ (score: _____)
Best Budget Model (0.33x):   _____ (score: _____)
Best Reference Model (1x):   _____ (score: _____)
Overall Best Value:           _____ (score: _____ at _____x cost)
```

### Section E: Scenario Winners

```
Best for Context Gathering (S1):    _____ (cheapest model scoring ≥ 50/62.5)
Best for Code Analysis (S2):        _____ (cheapest model scoring ≥ 50/62.5)
Best for Code Generation (S3):      _____ (cheapest model scoring ≥ 50/62.5)
Best for Architecture Design (S4):  _____ (cheapest model scoring ≥ 50/62.5)
```

### Section F: Specific Failure Analysis

For each model that scored below 40/62.5 on any scenario, document:
- What specifically went wrong
- Which dimensions failed
- Whether the failure is systematic (will happen every time) or random

### Section G: Final Recommendation

Produce a concrete recommendation in this format:

```
RECOMMENDED SUB-AGENT CONFIGURATION:

For "Reader" sub-agents (context gathering, code analysis):
  → Model: _____
  → Cost: _____ multiplier
  → Reason: _____

For "Worker" sub-agents (code generation, architecture design):  
  → Model: _____
  → Cost: _____ multiplier
  → Reason: _____

Estimated cost savings vs using Opus 4.6 for everything:
  → Reader savings: _____% 
  → Worker savings: _____%
  → Blended savings: _____% (assuming 60% reader, 40% worker split)
```

---

## PHASE 6: EXECUTION CHECKLIST

Use this checklist to track your progress. Mark each item as you complete it.

```
BATCH 1 — S1 (Context Gathering):
  [ ] M1-S1: bench-gpt41 (GPT-4.1)
  [ ] M2-S1: bench-gpt5mini (GPT-5 mini)
  [ ] M3-S1: bench-raptor (Raptor mini)
  [ ] M4-S1: bench-haiku (Claude Haiku 4.5)
  [ ] M5-S1: bench-gemini-flash (Gemini 3 Flash)
  [ ] M6-S1: bench-codex-mini (GPT-5.1-Codex-Mini)
  [ ] M7-S1: bench-sonnet (Claude Sonnet 4.6)
  [ ] M8-S1: bench-gpt52 (GPT-5.2)
  [ ] M9-S1: bench-gpt54 (GPT-5.4)
  [ ] All S1 outputs scored

BATCH 2 — S2 (Code Analysis):
  [ ] M1-S2: bench-gpt41    [ ] M2-S2: bench-gpt5mini  [ ] M3-S2: bench-raptor
  [ ] M4-S2: bench-haiku     [ ] M5-S2: bench-gemini-flash  [ ] M6-S2: bench-codex-mini
  [ ] M7-S2: bench-sonnet    [ ] M8-S2: bench-gpt52     [ ] M9-S2: bench-gpt54
  [ ] All S2 outputs scored

BATCH 3 — S3 (Code Generation):
  [ ] M1-S3: bench-gpt41    [ ] M2-S3: bench-gpt5mini  [ ] M3-S3: bench-raptor
  [ ] M4-S3: bench-haiku     [ ] M5-S3: bench-gemini-flash  [ ] M6-S3: bench-codex-mini
  [ ] M7-S3: bench-sonnet    [ ] M8-S3: bench-gpt52     [ ] M9-S3: bench-gpt54
  [ ] All S3 outputs scored

BATCH 4 — S4 (Architecture Design):
  [ ] M1-S4: bench-gpt41    [ ] M2-S4: bench-gpt5mini  [ ] M3-S4: bench-raptor
  [ ] M4-S4: bench-haiku     [ ] M5-S4: bench-gemini-flash  [ ] M6-S4: bench-codex-mini
  [ ] M7-S4: bench-sonnet    [ ] M8-S4: bench-gpt52     [ ] M9-S4: bench-gpt54
  [ ] All S4 outputs scored

FINAL:
  [ ] Raw scores table produced
  [ ] Dimension breakdown per model produced
  [ ] Cost-efficiency ranking produced
  [ ] Tier winners identified
  [ ] Scenario winners identified
  [ ] Failure analysis documented
  [ ] Final recommendation produced
```

---

## IMPORTANT RULES FOR THE ORCHESTRATOR

1. **DO NOT modify the sub-agent prompts** — use them exactly as written above
2. **DO NOT reveal the benchmark context** to sub-agents — they should think this is a real task
3. **DO NOT give sub-agents extra context** like "this is a test" or "compare with..."
4. **Score STRICTLY against the ground truth** — do not give credit for "creative" deviations
5. **If a sub-agent refuses or errors**, score it 1 on all dimensions and note the failure mode
6. **If timing data is available**, record it. If not, note "timing not available"
7. **Produce the full report even if some models fail** — failure data is valuable
8. **Do not stop early** — even if one model clearly dominates, complete all runs for full data
9. **Save the final report** to `docs/subagent-benchmark/benchmark-results.md` in the workspace
