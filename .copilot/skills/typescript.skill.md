# TypeScript Skill

This skill defines TypeScript coding conventions, patterns, and constraints for this repository.
Reference it when implementing TypeScript services, CLIs, or tooling.

## Runtime & Toolchain

- **Node 20 LTS** (or **Bun 1.x** — preferred for speed)
- **TypeScript 5.x**, `strict: true`, `noUncheckedIndexedAccess: true`
- Module system: **ESM** (`"module": "ESNext"`, `"moduleResolution": "bundler"`)
- Formatter: **Prettier** (default config). Linter: **ESLint** with `@typescript-eslint`.
- Package manager: **pnpm** preferred.

## HTTP Framework

Use **Hono** for all HTTP services:

```typescript
import { Hono } from 'hono'
import { zValidator } from '@hono/zod-validator'
import { z } from 'zod'

const app = new Hono()

const createUserSchema = z.object({
  email: z.string().email(),
  name: z.string().min(1).max(100),
})

app.post('/api/v1/users', zValidator('json', createUserSchema), async (c) => {
  const body = c.req.valid('json')
  const user = await userService.create(body)
  return c.json(user, 201)
})
```

## Project Structure

```
src/
  routes/          # Hono route handlers — HTTP boundary only
  services/        # Business logic — no HTTP concerns
  repositories/    # Data access — no business logic
  models/          # Zod schemas, TypeScript types, DTOs
  middleware/       # Auth, logging, error handling
  config/          # Environment variable parsing (zod-env or @t3-oss/env-core)
  errors/          # Domain errors and HTTP error mapping
test/
  routes/          # Integration tests (Hono test client)
  services/        # Unit tests (vitest)
  repositories/    # DB tests (Testcontainers + postgres)
```

## Validation

Use **Zod** for all runtime validation — request bodies, env vars, and external API responses:

```typescript
// models/user.model.ts
import { z } from 'zod'

export const CreateUserRequest = z.object({
  email: z.string().email('Invalid email format'),
  name: z.string().min(1, 'Name is required').max(100),
})
export type CreateUserRequest = z.infer<typeof CreateUserRequest>

export const UserResponse = z.object({
  id: z.string().uuid(),
  email: z.string(),
  name: z.string(),
  createdAt: z.string().datetime(),
})
export type UserResponse = z.infer<typeof UserResponse>
```

## Error Handling

Define domain errors and map them to HTTP responses in a central error handler:

```typescript
// errors/domain-errors.ts
export class UserNotFoundException extends Error {
  constructor(id: string) {
    super(`No user with id ${id} exists.`)
    this.name = 'UserNotFoundException'
  }
}

// middleware/error-handler.ts
import type { Context } from 'hono'
import { UserNotFoundException } from '../errors/domain-errors.js'

export function errorHandler(err: Error, c: Context) {
  if (err instanceof UserNotFoundException) {
    return c.json({ code: 'USER_NOT_FOUND', message: err.message }, 404)
  }
  console.error({ err }, 'Unhandled error')
  return c.json({ code: 'INTERNAL_ERROR', message: 'An unexpected error occurred.' }, 500)
}
```

## Database

Use **Drizzle ORM** with PostgreSQL:

```typescript
// repositories/user.repository.ts
import { db } from '../config/database.js'
import { users } from '../models/schema.js'
import { eq } from 'drizzle-orm'

export async function findUserById(id: string) {
  const [user] = await db.select().from(users).where(eq(users.id, id))
  return user ?? null
}
```

Schema migrations via **Drizzle Kit** (`drizzle-kit generate` / `drizzle-kit migrate`).

## Testing

Use **Vitest** + Hono's test client:

```typescript
// test/routes/users.test.ts
import { describe, it, expect, vi } from 'vitest'
import { testClient } from 'hono/testing'
import app from '../../src/app.js'

describe('POST /api/v1/users', () => {
  it('returns 201 with the created user when input is valid', async () => {
    const client = testClient(app)
    const res = await client.api.v1.users.$post({
      json: { email: 'alice@example.com', name: 'Alice' },
    })
    expect(res.status).toBe(201)
    const body = await res.json()
    expect(body).toMatchObject({ email: 'alice@example.com', name: 'Alice' })
  })

  it('returns 400 when email is missing', async () => {
    const client = testClient(app)
    const res = await client.api.v1.users.$post({ json: { name: 'Alice' } as any })
    expect(res.status).toBe(400)
  })
})
```

## Naming Conventions

| Artefact | Convention | Example |
|----------|-----------|---------|
| Files | `kebab-case` | `user-service.ts` |
| Types / Interfaces | `PascalCase` | `CreateUserRequest` |
| Variables / Functions | `camelCase` | `findUserById` |
| Constants | `UPPER_SNAKE_CASE` | `MAX_PAGE_SIZE` |
| Zod schemas | `PascalCase` (same as the type) | `UserResponse` |

## Constraints

- `strict: true` — no implicit `any`, no unchecked nullable access.
- No `as any` casts — use `z.unknown()` + Zod parse or a type guard instead.
- No `console.log` in production code — use a structured logger (`pino` recommended).
- No raw SQL string building — use Drizzle's query builder.
- No hardcoded configuration — parse env vars with Zod at startup; fail fast if invalid.
- Every public function must have a JSDoc comment.
