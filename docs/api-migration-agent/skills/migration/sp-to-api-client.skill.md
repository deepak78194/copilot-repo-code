# sp-to-api-client.skill.md

> Skill for replacing stored procedure calls with REST API client calls.

---
domain: Stored Procedure to API Client Migration
version: 1.0.0
applies_when:
  - User chose API_CLIENT for stored procedure handling
  - Another service already provides the functionality
  - Decoupling from database layer is desired
---

## Purpose

Replace Oracle stored procedure calls with REST API calls to another microservice that handles the same business logic.

## Conventions

### 1. Prerequisites Check

Before implementing API_CLIENT:

```
PREREQUISITES CHECKLIST:
□ Target service exists and exposes the required API
□ Target API contract is documented
□ Network connectivity between services is configured
□ Authentication mechanism is understood (JWT, API key, mTLS)
□ Timeout and retry policy defined
□ Circuit breaker strategy defined
```

### 2. Client Technology Choice

| Approach | Use When | Setup Complexity |
|----------|----------|------------------|
| **OpenFeign** | Simple declarative HTTP clients | Low |
| **WebClient** | Reactive, non-blocking calls | Medium |
| **RestTemplate** | Legacy, synchronous (avoid in new code) | Low |

**Recommendation:** Use OpenFeign for most migrations.

### 3. Feign Client Pattern

```java
// Define the client interface
@FeignClient(
    name = "user-service",
    url = "${services.user-service.url}",
    configuration = UserServiceClientConfig.class
)
public interface UserServiceClient {

    @PostMapping("/api/v1/users/{userId}/password/validate")
    PasswordValidationResponse validatePassword(
        @PathVariable("userId") Long userId,
        @RequestBody PasswordValidationRequest request
    );
    
    @PutMapping("/api/v1/users/{userId}/password")
    UpdatePasswordResponse updatePassword(
        @PathVariable("userId") Long userId,
        @RequestBody UpdatePasswordRequest request
    );
}
```

### 4. Client Configuration

```java
@Configuration
public class UserServiceClientConfig {

    @Bean
    public RequestInterceptor authInterceptor(TokenProvider tokenProvider) {
        return template -> {
            template.header("Authorization", "Bearer " + tokenProvider.getServiceToken());
        };
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new UserServiceErrorDecoder();
    }

    @Bean
    public Retryer retryer() {
        // Retry 3 times with 100ms initial interval, 1s max
        return new Retryer.Default(100, 1000, 3);
    }
}
```

### 5. Error Handling

```java
public class UserServiceErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 404) {
            return new UserNotFoundException("User not found in user-service");
        }
        if (response.status() == 400) {
            return new InvalidRequestException("Invalid request to user-service");
        }
        if (response.status() >= 500) {
            return new UserServiceUnavailableException("User service unavailable");
        }
        return defaultDecoder.decode(methodKey, response);
    }
}
```

### 6. Circuit Breaker Pattern

```java
// Using Resilience4j with Feign
@FeignClient(
    name = "user-service",
    url = "${services.user-service.url}",
    fallback = UserServiceClientFallback.class
)
public interface UserServiceClient {
    // methods
}

@Component
public class UserServiceClientFallback implements UserServiceClient {

    @Override
    public PasswordValidationResponse validatePassword(Long userId, PasswordValidationRequest request) {
        throw new ServiceUnavailableException("User service is currently unavailable");
    }
    
    @Override
    public UpdatePasswordResponse updatePassword(Long userId, UpdatePasswordRequest request) {
        throw new ServiceUnavailableException("User service is currently unavailable");
    }
}
```

### 7. Request/Response DTOs

```java
// Request to the target service
public record PasswordValidationRequest(
    String password
) {}

// Response from the target service
public record PasswordValidationResponse(
    boolean valid,
    String message,
    List<String> violations
) {}

// Request for password update
public record UpdatePasswordRequest(
    String oldPassword,
    String newPassword
) {}

// Response for password update
public record UpdatePasswordResponse(
    boolean success,
    String message
) {}
```

### 8. Service Integration

```java
// Replace SP call with API client call
@Service
@RequiredArgsConstructor
public class PasswordService {

    private final UserServiceClient userServiceClient;

    // BEFORE: Called Oracle SP
    // CallableStatement cs = conn.prepareCall("{call PROC_CHANGE_PASSWORD(?, ?, ?, ?)}");
    
    // AFTER: Call REST API
    public ChangePasswordResult changePassword(Long userId, String oldPassword, String newPassword) {
        try {
            UpdatePasswordRequest request = new UpdatePasswordRequest(oldPassword, newPassword);
            UpdatePasswordResponse response = userServiceClient.updatePassword(userId, request);
            
            return response.success() 
                ? ChangePasswordResult.success()
                : ChangePasswordResult.failure(response.message());
                
        } catch (UserNotFoundException e) {
            return ChangePasswordResult.userNotFound();
        } catch (ServiceUnavailableException e) {
            throw new PasswordServiceException("Unable to change password at this time", e);
        }
    }
}
```

### 9. Configuration Properties

```yaml
# application.yml
services:
  user-service:
    url: ${USER_SERVICE_URL:http://localhost:8081}
    connect-timeout: 5000
    read-timeout: 10000

feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 10000
        loggerLevel: BASIC
  circuitbreaker:
    enabled: true

resilience4j:
  circuitbreaker:
    instances:
      user-service:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10000
```

### 10. Dependencies

```groovy
// build.gradle
dependencies {
    implementation 'org.springframework.cloud:spring-cloud-starter-openfeign'
    implementation 'org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j'
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
    }
}
```

## Patterns

### API Client Migration Task List

```yaml
api_client_migration:
  sp_name: PROC_CHANGE_PASSWORD
  target_service: user-service
  
  client_to_create:
    name: UserServiceClient
    package: com.company.identity.infrastructure.client
    base_url_property: services.user-service.url
    
  endpoints_to_call:
    - sp_operation: "validate old password"
      http_method: POST
      path: "/api/v1/users/{userId}/password/validate"
      request_dto: PasswordValidationRequest
      response_dto: PasswordValidationResponse
      
    - sp_operation: "update password"
      http_method: PUT
      path: "/api/v1/users/{userId}/password"
      request_dto: UpdatePasswordRequest
      response_dto: UpdatePasswordResponse
  
  error_mapping:
    - sp_result: 1  # wrong password
      api_status: 400
      exception: InvalidPasswordException
      
    - sp_result: 2  # policy violation
      api_status: 400
      exception: PasswordPolicyException
      
  fallback_strategy: THROW_EXCEPTION
  circuit_breaker: ENABLED
```

## Anti-Patterns

- ❌ DO NOT call external APIs in a loop without batching
- ❌ DO NOT ignore timeout configuration
- ❌ DO NOT skip circuit breaker for critical paths
- ❌ DO NOT expose internal correlation IDs to external services
- ❌ DO NOT use RestTemplate for new code

## Verification

After implementation, verify:
1. API client successfully calls target service
2. Error scenarios are handled gracefully
3. Circuit breaker trips on failures
4. Timeouts prevent hanging requests
5. Logs show correlation for debugging
