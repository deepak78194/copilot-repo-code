# spring-boot-patterns.skill.md

> Skill for Spring Boot 3.x patterns in target microservices.

---
domain: Spring Boot 3 Microservice Patterns
version: 1.0.0
applies_when:
  - Target is Spring Boot microservice
  - Implementation phase of migration
  - Creating controllers, services, repositories
---

## Purpose

Define the standard patterns for implementing migrated APIs in Spring Boot 3 microservices.

## Conventions

### 1. Project Structure

```
identity/
├── src/main/java/com/company/identity/
│   ├── IdentityApplication.java
│   ├── domain/
│   │   ├── entity/
│   │   │   └── User.java
│   │   ├── vo/
│   │   │   └── EmailAddress.java
│   │   └── event/
│   │       └── PasswordChangedEvent.java
│   ├── application/
│   │   ├── port/
│   │   │   ├── in/
│   │   │   │   └── UpdatePasswordUseCase.java
│   │   │   └── out/
│   │   │       └── UserRepository.java
│   │   └── service/
│   │       └── UpdatePasswordService.java
│   └── infrastructure/
│       ├── web/
│       │   ├── controller/
│       │   │   └── PasswordController.java
│       │   └── dto/
│       │       ├── UpdatePasswordRequest.java
│       │       └── UpdatePasswordResponse.java
│       ├── persistence/
│       │   ├── entity/
│       │   │   └── UserJpaEntity.java
│       │   ├── repository/
│       │   │   └── JpaUserRepository.java
│       │   └── mapper/
│       │       └── UserMapper.java
│       └── config/
│           └── PersistenceConfig.java
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
│       └── V1__create_users.sql
└── build.gradle
```

### 2. Controller Pattern

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class PasswordController {

    private final UpdatePasswordUseCase updatePasswordUseCase;

    @PutMapping("/{userId}/password")
    public ResponseEntity<ApiResponse<UpdatePasswordResponse>> updatePassword(
            @PathVariable Long userId,
            @Valid @RequestBody UpdatePasswordRequest request) {
        
        UpdatePasswordCommand command = UpdatePasswordCommand.builder()
            .userId(userId)
            .oldPassword(request.oldPassword())
            .newPassword(request.newPassword())
            .build();
            
        UpdatePasswordResult result = updatePasswordUseCase.execute(command);
        
        return ResponseEntity.ok(ApiResponse.success(
            UpdatePasswordResponse.from(result)
        ));
    }
}
```

### 3. Use Case Pattern (Inbound Port)

```java
public interface UpdatePasswordUseCase {
    UpdatePasswordResult execute(UpdatePasswordCommand command);
}

// Command object
public record UpdatePasswordCommand(
    Long userId,
    String oldPassword,
    String newPassword
) {
    public static UpdatePasswordCommandBuilder builder() {
        return new UpdatePasswordCommandBuilder();
    }
}

// Result object
public record UpdatePasswordResult(
    boolean success,
    String message
) {}
```

### 4. Service Implementation

```java
@Service
@RequiredArgsConstructor
@Transactional
public class UpdatePasswordService implements UpdatePasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator policyValidator;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public UpdatePasswordResult execute(UpdatePasswordCommand command) {
        User user = userRepository.findById(command.userId())
            .orElseThrow(() -> new UserNotFoundException(command.userId()));
            
        if (!passwordEncoder.matches(command.oldPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }
        
        policyValidator.validate(command.newPassword(), user);
        
        user.updatePassword(passwordEncoder.encode(command.newPassword()));
        userRepository.save(user);
        
        eventPublisher.publishEvent(new PasswordChangedEvent(user.getId()));
        
        return new UpdatePasswordResult(true, "Password updated successfully");
    }
}
```

### 5. Repository Pattern (Outbound Port)

```java
// Port interface (application layer)
public interface UserRepository {
    Optional<User> findById(Long id);
    User save(User user);
    List<String> findPasswordHistory(Long userId, int count);
}

// JPA implementation (infrastructure layer)
@Repository
@RequiredArgsConstructor
public class JpaUserRepository implements UserRepository {

    private final SpringDataUserRepository springDataRepository;
    private final UserMapper mapper;

    @Override
    public Optional<User> findById(Long id) {
        return springDataRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = mapper.toEntity(user);
        UserJpaEntity saved = springDataRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<String> findPasswordHistory(Long userId, int count) {
        return springDataRepository.findPasswordHistory(userId, count);
    }
}

// Spring Data interface (infrastructure layer)
public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, Long> {
    
    @Query(value = """
        SELECT ph.password_hash 
        FROM password_history ph 
        WHERE ph.user_id = :userId 
        ORDER BY ph.created_at DESC 
        LIMIT :count
        """, nativeQuery = true)
    List<String> findPasswordHistory(@Param("userId") Long userId, @Param("count") int count);
}
```

### 6. Entity Separation

```java
// Domain entity (domain layer) - NO JPA annotations
public class User {
    private Long id;
    private String email;
    private String passwordHash;
    private Instant updatedAt;
    
    public void updatePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.updatedAt = Instant.now();
    }
}

// JPA entity (infrastructure layer)
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
}
```

### 7. DTO Pattern

```java
// Request DTO with validation
public record UpdatePasswordRequest(
    @NotBlank(message = "Current password is required")
    String oldPassword,
    
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String newPassword
) {}

// Response DTO
public record UpdatePasswordResponse(
    boolean success,
    String message,
    Instant timestamp
) {
    public static UpdatePasswordResponse from(UpdatePasswordResult result) {
        return new UpdatePasswordResponse(
            result.success(),
            result.message(),
            Instant.now()
        );
    }
}

// Standard API envelope
public record ApiResponse<T>(
    boolean success,
    T data,
    String error,
    String correlationId
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, UUID.randomUUID().toString());
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, UUID.randomUUID().toString());
    }
}
```

### 8. Exception Handling

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidPassword(InvalidPasswordException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(message));
    }
}
```

## Anti-Patterns

- ❌ DO NOT put JPA annotations on domain entities
- ❌ DO NOT inject repositories directly into controllers
- ❌ DO NOT return JPA entities from controllers
- ❌ DO NOT put business logic in controllers
- ❌ DO NOT use @Autowired field injection - use constructor injection

## Build Configuration

```groovy
// build.gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

java {
    sourceCompatibility = '17'
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    
    runtimeOnly 'org.postgresql:postgresql'
    
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```
