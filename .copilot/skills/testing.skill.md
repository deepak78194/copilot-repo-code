# Testing Skill

This skill defines testing standards, patterns, and constraints for this repository. Reference it when writing or reviewing tests.

## Framework

- **JUnit 5** (`junit-jupiter`) for all tests.
- **AssertJ** for fluent, readable assertions (`assertThat(...).isEqualTo(...)`).
- **Mockito** for mocking dependencies in unit tests.
- **Testcontainers** for integration tests requiring a real database.

## Test Categories

### Unit Tests
- Test a single class in isolation.
- Mock all dependencies with Mockito.
- Fast — no I/O, no database, no HTTP.
- Located in the same package as the class under test.

### Integration Tests
- Test a slice of the application (e.g., controller + service + repository).
- Use a real (or in-memory/containerized) database.
- Use the framework's test client to make HTTP requests.
- Annotated with `@MicronautTest` (Micronaut) or `@SpringBootTest` (Spring Boot).

### Repository Tests
- Test database queries against a real schema.
- Use Testcontainers with a PostgreSQL container.
- Run migrations before tests (Flyway auto-runs on startup).

## Test Method Naming

Pattern: `methodName_stateUnderTest_expectedBehavior`

Examples:
- `createUser_withValidInput_returns201`
- `createUser_withDuplicateEmail_returns409`
- `findById_withNonExistentId_throwsNotFoundException`

Use `@DisplayName` for human-readable descriptions:
```java
@Test
@DisplayName("POST /users returns 201 when input is valid")
void createUser_withValidInput_returns201() { ... }
```

## TDD Cycle

Follow the Red → Green → Refactor cycle:

1. **Red**: Write a failing test that describes the desired behavior.
2. **Green**: Write the minimal code to make the test pass.
3. **Refactor**: Clean up the code without breaking the test.

When using an AI agent:
- Provide the failing test to the Implementer as the specification.
- The Implementer writes only enough code to pass the test.
- The Reviewer checks that the test is meaningful (not trivially passing).

## Assertions

Prefer AssertJ over JUnit assertions for readability:
```java
// Preferred
assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
assertThat(user.getEmail()).isEqualTo("alice@example.com");

// Avoid
assertEquals(HttpStatus.CREATED, response.getStatus());
```

For exceptions:
```java
assertThatThrownBy(() -> service.findById(-1L))
    .isInstanceOf(UserNotFoundException.class)
    .hasMessageContaining("-1");
```

## Test Data

- Use descriptive constants or builder methods for test data.
- Do not reuse production data or live database connections.
- Reset state between tests (`@Transactional` + rollback, or `@BeforeEach` cleanup).

```java
private static final String VALID_EMAIL = "alice@example.com";
private static final String VALID_NAME = "Alice";
```

## Mocking

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private UserService service;

    @Test
    void findById_withNonExistentId_throwsNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(99L))
            .isInstanceOf(UserNotFoundException.class);
    }
}
```

## Coverage Expectations

- Every public method must have at least one test.
- Happy path + at least one error/edge case per method.
- All acceptance criteria from the Planner must map to at least one test.

## Constraints

- No `Thread.sleep()` in tests — use `Awaitility` for async assertions.
- No shared mutable state between tests.
- No tests that always pass (i.e., no assertions = not a test).
- Do not mock the class under test.
- Do not suppress test failures with empty catch blocks.
