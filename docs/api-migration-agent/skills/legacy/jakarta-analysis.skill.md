# jakarta-analysis.skill.md

> Skill for analyzing Jakarta REST (JAX-RS) endpoints in legacy services.

---
domain: Legacy Jakarta REST API Analysis
version: 1.0.0
applies_when:
  - Source contains javax.ws.rs.* or jakarta.ws.rs.* imports
  - pom.xml contains jakarta.platform or javax dependencies
  - Project uses JAX-RS annotations (@Path, @GET, @POST, etc.)
---

## Purpose

Analyze Jakarta REST endpoints to extract API contract, handler logic, and dependencies for a specific API (not the entire service).

## Conventions

### 1. Endpoint Discovery

Scan for JAX-RS annotations to identify endpoints:

```java
// Look for these patterns
@Path("/users")
public class UserResource {
    
    @POST
    @Path("/change-password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changePassword(ChangePasswordRequest request) {
        // Handler implementation
    }
}
```

**Extract:**
- Base path from class-level `@Path`
- Method path from method-level `@Path`
- HTTP method from `@GET`, `@POST`, `@PUT`, `@DELETE`, `@PATCH`
- Media types from `@Consumes`, `@Produces`

### 2. Parameter Extraction

Identify all parameter sources:

| Annotation | Source | Example |
|------------|--------|---------|
| `@PathParam` | URL path | `/users/{id}` → `@PathParam("id")` |
| `@QueryParam` | Query string | `?status=active` → `@QueryParam("status")` |
| `@HeaderParam` | HTTP header | `Authorization` → `@HeaderParam("Authorization")` |
| `@FormParam` | Form data | Form field → `@FormParam("field")` |
| `@BeanParam` | Multiple params | Aggregated DTO |
| (no annotation) | Request body | JSON body mapped to DTO |

### 3. Response Analysis

Determine response structure:

```java
// Explicit Response builder
return Response.ok(userDto).build();
return Response.status(Status.NOT_FOUND).entity(error).build();

// Direct return (implicit 200)
public UserDto getUser(@PathParam("id") Long id) {
    return userService.findById(id);
}
```

### 4. Service Injection Points

Identify injected services:

```java
@Inject
private UserService userService;

@EJB
private LegacyUserBean legacyBean;

@Resource
private DataSource dataSource;
```

### 5. Transaction Boundaries

Detect transaction management:

```java
// Container-managed (CMT) - default for EJBs
@TransactionAttribute(TransactionAttributeType.REQUIRED)

// Bean-managed (BMT)
@TransactionManagement(TransactionManagementType.BEAN)
@Resource
private UserTransaction userTransaction;
```

## Patterns

### Endpoint Catalog Entry

```yaml
endpoint:
  method: POST
  path: /users/change-password
  full_path: /api/v1/users/change-password  # including context path
  handler_class: com.company.users.UserResource
  handler_method: changePassword
  
parameters:
  - name: request
    type: ChangePasswordRequest
    source: BODY
    validation: "@Valid"
    
response:
  success_type: ChangePasswordResponse
  success_status: 200
  error_types:
    - type: ErrorResponse
      status: 400
      condition: "validation failure"
    - type: ErrorResponse  
      status: 404
      condition: "user not found"

security:
  authentication: JWT  # or BASIC, NONE
  roles: ["USER", "ADMIN"]
  
transaction:
  type: CONTAINER_MANAGED
  attribute: REQUIRED
```

## Anti-Patterns

- ❌ DO NOT analyze all endpoints in the service - focus on the requested API only
- ❌ DO NOT execute the code - this is static analysis only
- ❌ DO NOT modify any legacy files
- ❌ DO NOT assume behavior from method names - read the actual implementation
- ❌ DO NOT follow every dependency chain - stop at direct service calls

## Search Patterns

```bash
# Find endpoint by path
grep -r "@Path.*change-password" --include="*.java"

# Find all REST resources
grep -r "@Path" --include="*.java" | grep "class"

# Find injected services in a resource
grep -rA5 "@Inject\|@EJB" UserResource.java
```

## Output

After analysis, produce an endpoint catalog entry in the format above, ready for the discovery report.
