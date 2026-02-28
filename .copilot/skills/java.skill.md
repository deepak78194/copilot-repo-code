# Java Skill

This skill defines Java coding conventions, patterns, and constraints for this repository. Reference it when implementing Java backend code.

## Language Version

Java 21. Use modern features where appropriate:
- Records for immutable data carriers
- Sealed interfaces for closed type hierarchies
- Pattern matching (`instanceof`, `switch`)
- Text blocks for multi-line strings
- `var` for local variables when the type is obvious from context

## Frameworks

### Micronaut 4.x
- Use constructor injection (`@Inject` on constructors, not fields).
- Annotate controllers with `@Controller("/path")`.
- Use `@Get`, `@Post`, `@Put`, `@Patch`, `@Delete` for HTTP methods.
- Return `HttpResponse<T>` for full control over status codes.
- Use `@Valid` + Bean Validation (`@NotNull`, `@NotBlank`, `@Size`) on request bodies.
- Use `@Singleton`, `@Prototype` appropriately for service beans.

### Spring Boot 3.x
- Use constructor injection (not `@Autowired` on fields).
- Annotate controllers with `@RestController` and `@RequestMapping("/path")`.
- Use `@GetMapping`, `@PostMapping`, etc. for HTTP methods.
- Return `ResponseEntity<T>` for full control over status codes.
- Use `@Valid` + Bean Validation on `@RequestBody` parameters.
- Use `@Service`, `@Repository`, `@Component` appropriately.

## Project Structure

```
src/
  main/
    java/
      com/example/
        controller/    # HTTP layer only — no business logic
        service/       # Business logic — no HTTP concerns
        repository/    # Data access — no business logic
        model/         # Entities, records, DTOs
        exception/     # Custom exceptions and error handlers
    resources/
      application.yml  # Config (not .properties)
      db/migration/    # Flyway SQL migrations
  test/
    java/
      com/example/
        controller/    # Integration tests using test client
        service/       # Unit tests with mocks
        repository/    # Repository tests with test database
```

## Naming Conventions

- Entities: `User`, `Order`, `Product` (nouns, PascalCase)
- DTOs: `CreateUserRequest`, `UserResponse` (noun + verb/noun, PascalCase)
- Services: `UserService`, `OrderService` (noun + Service)
- Repositories: `UserRepository` (noun + Repository)
- Controllers: `UserController` (noun + Controller)
- Exceptions: `UserNotFoundException`, `DuplicateEmailException`

## Error Handling

- Define custom exceptions in `exception/` package.
- Use a global exception handler (`@ControllerAdvice` or Micronaut `@Error`) to map exceptions to HTTP responses.
- Never expose stack traces in API responses.
- Return structured error responses:

```json
{
  "code": "USER_NOT_FOUND",
  "message": "No user with id 42 exists."
}
```

## Database

- Use JPA/Hibernate for ORM.
- All schema changes via Flyway migrations in `src/main/resources/db/migration/`.
- Migration naming: `V{version}__{description}.sql` (e.g., `V1__create_users_table.sql`).
- Use `@Transactional` on service methods that write to the database.
- Never write native SQL inside Java code — use JPQL or a repository method.

## Constraints

- No field injection (`@Autowired` on fields, `@Inject` on fields). Use constructor injection only.
- No raw types (`List` instead of `List<String>`).
- No `System.out.println` — use a logger (`org.slf4j.Logger`).
- No `e.printStackTrace()` — log the exception properly.
- No hardcoded configuration values — use `application.yml` and `@Value` / `@ConfigurationProperties`.
