# dual-datasource.skill.md

> Skill for configuring dual datasource (PostgreSQL + Oracle) to keep stored procedure calls.

---
domain: Dual Datasource Configuration
version: 1.0.0
applies_when:
  - User chose KEEP_SP for stored procedure handling
  - Need to retain Oracle stored procedure calls
  - New data goes to PostgreSQL, SP calls go to Oracle
---

## Purpose

Configure Spring Boot application with two datasources:
- **Primary (PostgreSQL)**: For new entities, native queries, application data
- **Secondary (Oracle)**: For retained stored procedure calls only

## Conventions

### 1. Datasource Routing Strategy

```
┌─────────────────────────────────────────────────────────────────────────┐
│ APPLICATION                                                             │
│                                                                         │
│  ┌─────────────────────┐                                               │
│  │ Service Layer       │                                               │
│  │                     │                                               │
│  │  userRepository     ├──────┐                                        │
│  │  .save(user)        │      │                                        │
│  │                     │      ▼                                        │
│  │  oracleSpRepository │   ┌──────────────────┐                        │
│  │  .changePassword()  │   │ Routing Logic    │                        │
│  └─────────────────────┘   │                  │                        │
│                            │ @PostgresDS      │──► PostgreSQL          │
│                            │ @OracleDS        │──► Oracle              │
│                            └──────────────────┘                        │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2. Configuration Properties

```yaml
# application.yml
spring:
  datasource:
    # Primary datasource (PostgreSQL) - used by default
    postgres:
      url: jdbc:postgresql://${POSTGRES_HOST:localhost}:5432/${POSTGRES_DB:identity}
      username: ${POSTGRES_USER:identity}
      password: ${POSTGRES_PASSWORD:secret}
      driver-class-name: org.postgresql.Driver
      hikari:
        pool-name: postgres-pool
        maximum-pool-size: 10
        minimum-idle: 5
        
    # Secondary datasource (Oracle) - for stored procedures only
    oracle:
      url: jdbc:oracle:thin:@${ORACLE_HOST:localhost}:1521:${ORACLE_SID:ORCL}
      username: ${ORACLE_USER:app_user}
      password: ${ORACLE_PASSWORD:secret}
      driver-class-name: oracle.jdbc.OracleDriver
      hikari:
        pool-name: oracle-pool
        maximum-pool-size: 5  # smaller pool, only for SPs
        minimum-idle: 2

  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect  # Primary dialect
```

### 3. Datasource Configuration Class

```java
@Configuration
@EnableTransactionManagement
public class DatasourceConfig {

    // ========== PostgreSQL (Primary) ==========
    
    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource.postgres")
    public DataSourceProperties postgresDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean
    public DataSource postgresDataSource() {
        return postgresDataSourceProperties()
            .initializeDataSourceBuilder()
            .type(HikariDataSource.class)
            .build();
    }

    @Primary
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("postgresDataSource") DataSource dataSource) {
        return builder
            .dataSource(dataSource)
            .packages("com.company.identity.infrastructure.persistence.entity")
            .persistenceUnit("postgres")
            .build();
    }

    @Primary
    @Bean
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    // ========== Oracle (Secondary - for SPs only) ==========
    
    @Bean
    @ConfigurationProperties("spring.datasource.oracle")
    public DataSourceProperties oracleDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource oracleDataSource() {
        return oracleDataSourceProperties()
            .initializeDataSourceBuilder()
            .type(HikariDataSource.class)
            .build();
    }

    @Bean
    public JdbcTemplate oracleJdbcTemplate(
            @Qualifier("oracleDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
    
    @Bean
    public PlatformTransactionManager oracleTransactionManager(
            @Qualifier("oracleDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
```

### 4. Stored Procedure Repository (Oracle)

```java
@Repository
@RequiredArgsConstructor
public class OracleStoredProcRepository {

    @Qualifier("oracleJdbcTemplate")
    private final JdbcTemplate oracleJdbcTemplate;

    @Transactional("oracleTransactionManager")
    public ChangePasswordResult changePassword(Long userId, String oldPassword, String newPassword) {
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(oracleJdbcTemplate)
            .withProcedureName("PROC_CHANGE_PASSWORD")
            .declareParameters(
                new SqlParameter("p_user_id", Types.BIGINT),
                new SqlParameter("p_old_password", Types.VARCHAR),
                new SqlParameter("p_new_password", Types.VARCHAR),
                new SqlOutParameter("p_result", Types.INTEGER)
            );

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("p_user_id", userId)
            .addValue("p_old_password", oldPassword)
            .addValue("p_new_password", newPassword);

        Map<String, Object> result = jdbcCall.execute(params);
        int resultCode = (Integer) result.get("p_result");

        return switch (resultCode) {
            case 0 -> ChangePasswordResult.SUCCESS;
            case 1 -> ChangePasswordResult.WRONG_PASSWORD;
            case 2 -> ChangePasswordResult.POLICY_VIOLATION;
            default -> ChangePasswordResult.UNKNOWN_ERROR;
        };
    }
}
```

### 5. PostgreSQL Repository (Primary)

```java
// Standard JPA repository on PostgreSQL
@Repository
public interface UserRepository extends JpaRepository<UserJpaEntity, Long> {
    
    // This uses PostgreSQL
    Optional<UserJpaEntity> findByEmail(String email);
    
    // Native query on PostgreSQL
    @Query(value = "SELECT * FROM users WHERE status = :status", nativeQuery = true)
    List<UserJpaEntity> findByStatus(@Param("status") String status);
}
```

### 6. Service Using Both Datasources

```java
@Service
@RequiredArgsConstructor
public class PasswordService {

    private final UserRepository userRepository;  // PostgreSQL
    private final OracleStoredProcRepository oracleSpRepository;  // Oracle

    @Transactional  // Defaults to PostgreSQL transaction
    public ChangePasswordResult changePassword(Long userId, String oldPassword, String newPassword) {
        // Read user from PostgreSQL (if needed for validation)
        UserJpaEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        
        // Call Oracle SP for password change
        // Note: This runs in separate Oracle transaction
        ChangePasswordResult result = oracleSpRepository.changePassword(
            userId, oldPassword, newPassword);
        
        if (result == ChangePasswordResult.SUCCESS) {
            // Update PostgreSQL timestamp
            user.setPasswordChangedAt(Instant.now());
            userRepository.save(user);
        }
        
        return result;
    }
}
```

### 7. Transaction Considerations

```java
// When operations span both datasources, you may need:

// Option A: Accept eventual consistency (simpler)
@Service
public class PasswordService {
    @Transactional  // PostgreSQL transaction
    public ChangePasswordResult changePassword(...) {
        // Oracle SP call (separate transaction)
        // PostgreSQL update (this transaction)
        // If PostgreSQL fails after Oracle succeeds, data is inconsistent
    }
}

// Option B: Use ChainedTransactionManager (complex)
@Configuration
public class TransactionConfig {
    @Bean
    public PlatformTransactionManager chainedTransactionManager(
            PlatformTransactionManager postgresTransactionManager,
            PlatformTransactionManager oracleTransactionManager) {
        return new ChainedTransactionManager(
            postgresTransactionManager,
            oracleTransactionManager
        );
    }
}

// Option C: Saga pattern with compensating transactions (recommended for critical paths)
@Service
public class PasswordService {
    public ChangePasswordResult changePassword(...) {
        try {
            // 1. Call Oracle SP
            // 2. Update PostgreSQL
        } catch (PostgresException e) {
            // 3. Compensate: Call SP to revert (if possible)
        }
    }
}
```

### 8. Dependencies

```groovy
// build.gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    
    // PostgreSQL
    runtimeOnly 'org.postgresql:postgresql'
    
    // Oracle
    runtimeOnly 'com.oracle.database.jdbc:ojdbc11:23.2.0.0'
}
```

## Patterns

### Dual Datasource Task List

```yaml
dual_datasource_config:
  sp_name: PROC_CHANGE_PASSWORD
  
  postgres_config:
    datasource_bean: postgresDataSource
    entity_manager_factory: entityManagerFactory
    transaction_manager: transactionManager
    packages_to_scan:
      - com.company.identity.infrastructure.persistence.entity
    
  oracle_config:
    datasource_bean: oracleDataSource
    jdbc_template: oracleJdbcTemplate
    transaction_manager: oracleTransactionManager
    
  repositories_to_create:
    - name: OracleStoredProcRepository
      type: JDBC_TEMPLATE
      datasource: oracleDataSource
      methods:
        - changePassword(Long, String, String) → ChangePasswordResult
        
  services_to_update:
    - name: PasswordService
      inject:
        - UserRepository (PostgreSQL)
        - OracleStoredProcRepository (Oracle)
        
  transaction_strategy: EVENTUAL_CONSISTENCY
  transaction_notes: "SP runs in Oracle transaction, PostgreSQL updates are best-effort"
```

## Anti-Patterns

- ❌ DO NOT try to use JPA @Procedure with secondary datasource
- ❌ DO NOT mix EntityManager between datasources
- ❌ DO NOT assume distributed transactions work automatically
- ❌ DO NOT use same transaction manager for both datasources
- ❌ DO NOT put Oracle-specific code in standard repositories

## Verification

After configuration, verify:
1. PostgreSQL connection works (test repositories)
2. Oracle connection works (test SP call)
3. Both connection pools are created
4. Transactions are isolated per datasource
5. Failover behavior when Oracle is unavailable
