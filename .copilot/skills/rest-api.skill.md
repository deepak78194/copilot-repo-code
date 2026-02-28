# REST API Skill

This skill defines REST API design rules, conventions, and constraints for this repository. Reference it when designing, implementing, or reviewing REST endpoints.

## HTTP Methods

| Method | Use Case | Idempotent | Body |
|--------|----------|------------|------|
| `GET` | Read a resource or collection | Yes | No |
| `POST` | Create a new resource | No | Yes |
| `PUT` | Replace a resource entirely | Yes | Yes |
| `PATCH` | Update a resource partially | No | Yes |
| `DELETE` | Remove a resource | Yes | No |

Never use `GET` to mutate state. Never use `POST` for idempotent operations.

## URL Design

- Use **nouns**, not verbs: `/users`, not `/getUsers` or `/createUser`.
- Use **plural nouns** for collections: `/users`, `/orders`.
- Use **kebab-case**: `/user-profiles`, not `/userProfiles` or `/user_profiles`.
- Nest related resources: `/users/{id}/orders` (max 2 levels deep).
- Use path parameters for resource identity: `/users/{id}`.
- Use query parameters for filtering, sorting, pagination: `/users?status=active&page=0&size=20`.

## HTTP Status Codes

| Code | Meaning | When to use |
|------|---------|-------------|
| `200 OK` | Success | GET, PUT, PATCH success |
| `201 Created` | Resource created | POST success |
| `204 No Content` | Success, no body | DELETE success |
| `400 Bad Request` | Invalid input | Validation failure |
| `401 Unauthorized` | Not authenticated | Missing/invalid credentials |
| `403 Forbidden` | Not authorized | Authenticated but lacks permission |
| `404 Not Found` | Resource missing | Resource does not exist |
| `409 Conflict` | State conflict | Duplicate resource, concurrency conflict |
| `422 Unprocessable Entity` | Semantic validation failure | Business rule violation |
| `500 Internal Server Error` | Unexpected server error | Unhandled exception |

Never return `200` for errors. Never return `500` for client errors.

## Request and Response Design

### Request Body (POST/PUT/PATCH)
- Use a dedicated request DTO (e.g., `CreateUserRequest`, `UpdateUserRequest`).
- Validate all fields with Bean Validation annotations.
- Do not expose internal IDs or database keys in request bodies.

### Response Body
- Use a dedicated response DTO (e.g., `UserResponse`).
- Never return JPA entities directly — always map to a DTO.
- Include `id` and all client-relevant fields.
- Use consistent field naming: `camelCase` in JSON.

### Error Response
All error responses must use this structure:
```json
{
  "code": "USER_NOT_FOUND",
  "message": "No user with id 42 exists."
}
```

For validation errors (400):
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed.",
  "errors": [
    { "field": "email", "message": "must not be blank" }
  ]
}
```

## Pagination

For collection endpoints, support cursor or page-based pagination:
```json
{
  "data": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8
}
```

Query parameters: `page` (0-indexed), `size` (default 20, max 100), `sort` (e.g., `name,asc`).

## Versioning

- Version APIs via the URL path: `/api/v1/users`.
- Do not version via headers or query params.
- Maintain backward compatibility within a major version.

## Content Negotiation

- Always produce and consume `application/json`.
- Set `Content-Type: application/json` on all responses with a body.
- Accept `Accept: application/json` from clients.

## Constraints

- Never return raw entity objects — always use DTOs.
- Never put business logic in controllers — delegate to services.
- Never silently swallow errors — always return an appropriate error response.
- Do not expose stack traces or internal exception messages to clients.
- Do not use query parameters for resource identity (use path parameters).
- Do not break existing API contracts within a major version.
