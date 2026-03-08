# API Migration Agent Architecture Plan

> A comprehensive agent system for migrating APIs across a monorepo with legacy Jakarta REST services and modern microservices.

---

## 1. The Problem Space

### 1.1 — Monorepo Structure

```
our-services/                          ← ROOT (Legacy services)
├── .github/
│   ├── agents/                        ← Agent definitions
│   ├── skills/                        ← Skills for legacy context
│   └── copilot-instructions.md       ← Global instructions
├── user-service/                      ← Legacy Jakarta REST (Java 11, Oracle)
├── order-service/                     ← Legacy Jakarta REST (Java 11, Oracle)
├── payment-service/                   ← Legacy Jakarta REST (Java 11, Oracle)
└── replatforming/
    └── services/
        ├── .github/
        │   ├── skills/                ← Skills for microservice context
        │   └── copilot-instructions.md
        ├── identity/                  ← Modern microservice (Spring Boot, Postgres)
        ├── orders/                    ← Modern microservice
        └── payments/                  ← Modern microservice
```

### 1.2 — Migration Scenarios

| Scenario | Source | Target | Example |
|----------|--------|--------|---------|
| **Legacy → Microservice** | Jakarta REST + Oracle SP | Spring Boot + Postgres/Oracle | `changePassword` API from `user-service` → `identity` |
| **Legacy → Legacy** | Jakarta REST service A | Jakarta REST service B | Move DB call from `order-service` → `payment-service` |
| **Stored Proc → API Client** | Oracle SP call | REST API client call | Replace SP call with call to microservice |
| **SP Decomposition** | Oracle Stored Procedure | Native queries + service logic | Decompose SP into JDBC queries |

### 1.3 — Key Challenges

1. **Multi-workspace Context**: Agent must work when:
   - Opened at root (`our-services/`)
   - Opened at microservice folder (`replatforming/services/identity/`)

2. **Skill Discovery**: Skills in root are invisible when workspace is microservice folder

3. **Context Length**: Can't load all skills always — need on-demand injection

4. **User Choices**: Must prompt user for stored procedure handling strategy

---

## 2. Architecture Decision: Skill Placement Strategy

### 2.1 — The Multi-Workspace Problem

```
When opened at ROOT:
  ✓ Can see .github/skills/* (legacy skills)
  ✓ Can see replatforming/services/.github/skills/* (microservice skills)
  
When opened at MICROSERVICE (e.g., identity/):
  ✗ Cannot see root .github/skills/*
  ✓ Can see own .github/skills/*
  ✓ Can see parent replatforming/services/.github/skills/*
```

### 2.2 — Solution: Layered Skill Distribution

```
LAYER 1: Root-level agent + orchestration
         our-services/.github/agents/api-migration.agent.md
         
LAYER 2: Legacy skills (only needed when root is workspace)
         our-services/.github/skills/legacy/
         
LAYER 3: Shared skills (in replatforming parent for all microservices)
         replatforming/services/.github/skills/migration/
         
LAYER 4: Microservice-specific skills (per-service customization)
         replatforming/services/identity/.github/skills/
```

### 2.3 — Skill Loading Mechanism

The agent detects context and loads appropriate skills:

```
┌─────────────────────────────────────────────────────────────────────────┐
│ WORKSPACE DETECTION FLOW                                                │
│                                                                         │
│  1. Check for markers:                                                  │
│     - pom.xml with jakarta.* imports → Legacy workspace                │
│     - build.gradle with Spring Boot → Microservice workspace           │
│     - Both present → Root monorepo workspace                           │
│                                                                         │
│  2. Load context-appropriate skills:                                    │
│     - Root: all skills available                                        │
│     - Legacy only: legacy skills + minimal migration skills            │
│     - Microservice: microservice skills + migration skills             │
│                                                                         │
│  3. Dynamic skill injection via #file: references                      │
│     - Agent doesn't embed procedures                                    │
│     - Agent references skill files on-demand                           │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Agent Composition Strategy

### 3.1 — Agent Hierarchy

```
┌─────────────────────────────────────────────────────────────────────────┐
│                                                                         │
│  API-MIGRATION ORCHESTRATOR (Composite Agent)                          │
│  ─────────────────────────────────────────────────────────────────     │
│  Coordinates all migration types. Thin orchestration layer.            │
│  Detects migration type and delegates to appropriate phase.            │
│                                                                         │
│  ┌──────────────────────┐  ┌──────────────────────┐                   │
│  │ DISCOVERY SUBAGENT   │  │ PLANNING PHASE       │                   │
│  │ (Fresh context)      │  │ (Main agent)         │                   │
│  │                      │  │                      │                   │
│  │ • Analyze source API │  │ • Design target API  │                   │
│  │ • Map dependencies   │  │ • Map SP handling    │                   │
│  │ • Catalog SP calls   │  │ • User approval gate │                   │
│  │ • Returns summary    │  │                      │                   │
│  └──────────────────────┘  └──────────────────────┘                   │
│                                                                         │
│  ┌──────────────────────┐  ┌──────────────────────┐                   │
│  │ IMPLEMENTATION       │  │ VERIFICATION         │                   │
│  │ (Main agent)         │  │ (Main agent)         │                   │
│  │                      │  │                      │                   │
│  │ • Generate code      │  │ • Build & test       │                   │
│  │ • Create adapters    │  │ • Fix errors         │                   │
│  │ • Wire dependencies  │  │ • Iterate until pass │                   │
│  └──────────────────────┘  └──────────────────────┘                   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.2 — Migration Type Detection

```
USER INPUT ANALYSIS:
─────────────────────────────────────────────────────────────────────────
"migrate changePassword from user-service to identity"
  → Legacy → Microservice migration
  → Source: our-services/user-service/
  → Target: replatforming/services/identity/

"move getUserOrders from order-service to user-service"
  → Legacy → Legacy migration
  → Source: our-services/order-service/
  → Target: our-services/user-service/

"replace SP call validatePayment with API call"
  → Stored Proc → API Client migration
  → Source: Oracle SP reference
  → Target: REST client to microservice
```

---

## 4. Stored Procedure Handling Matrix

### 4.1 — User Choice Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│ STORED PROCEDURE ENCOUNTERED                                            │
│                                                                         │
│ Agent asks:                                                             │
│ "I found stored procedure call: PROC_CHANGE_PASSWORD                   │
│  How would you like to handle this?                                    │
│                                                                         │
│  1. KEEP AS SP     - Keep Oracle SP, configure dual datasource         │
│  2. DECOMPOSE      - Convert to native queries (Postgres/Spring JDBC)  │
│  3. STUB           - Create interface stub, implement later            │
│  4. API CLIENT     - Replace with API call to another service"         │
│                                                                         │
│ User selects → Agent proceeds with selected strategy                   │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4.2 — Strategy Implementation Details

| Strategy | Generated Code | Configuration |
|----------|---------------|---------------|
| **KEEP AS SP** | `@Procedure` annotation, Oracle repository | Dual datasource (Postgres primary, Oracle for SPs) |
| **DECOMPOSE** | Native `@Query` with Postgres SQL, service layer logic | Single Postgres datasource |
| **STUB** | Interface + `@TODO` stub implementation | No datasource config yet |
| **API CLIENT** | Feign/WebClient to target microservice | Service URL configuration |

---

## 5. Skill Inventory

### 5.1 — Root-Level Skills (Legacy Context)

| Skill | Purpose | Location |
|-------|---------|----------|
| `legacy-jakarta-analysis.skill.md` | Analyze Jakarta REST endpoints, EJBs | `.github/skills/legacy/` |
| `oracle-sp-analysis.skill.md` | Catalog stored procedure calls | `.github/skills/legacy/` |
| `legacy-dependency-mapping.skill.md` | Map API-specific dependencies | `.github/skills/legacy/` |

### 5.2 — Shared Migration Skills (Replatforming Level)

| Skill | Purpose | Location |
|-------|---------|----------|
| `api-migration-planning.skill.md` | Design migration plan with user choices | `replatforming/services/.github/skills/` |
| `sp-to-jdbc.skill.md` | Decompose SP to native queries | `replatforming/services/.github/skills/` |
| `sp-to-api-client.skill.md` | Replace SP with REST client | `replatforming/services/.github/skills/` |
| `dual-datasource-config.skill.md` | Configure Oracle + Postgres | `replatforming/services/.github/skills/` |

### 5.3 — Microservice-Specific Skills

| Skill | Purpose | Location |
|-------|---------|----------|
| `spring-boot-patterns.skill.md` | Spring Boot 3 conventions | Per-microservice `.github/skills/` |
| `postgres-repository.skill.md` | JPA + native query patterns | Per-microservice `.github/skills/` |

---

## 6. Context Window Management

### 6.1 — Token Budget Allocation

```
TOTAL CONTEXT: 160K tokens
─────────────────────────────────────────────────────────────────────────
Reserved Output:        24K (fixed)
Tool Definitions:       24K (fixed)
System Instructions:     2K (copilot-instructions + agent file)
Skills (loaded):         4K (only active phase skills, ~80 lines each)
Conversation:           50K (accumulated messages)
File Contents:          30K (source/target code being analyzed)
Tool Results:           26K (subagent summaries, search results)
─────────────────────────────────────────────────────────────────────────
AVAILABLE:             ~160K utilized efficiently
```

### 6.2 — Skill Loading Strategy

```
PHASE-BASED SKILL INJECTION:
─────────────────────────────────────────────────────────────────────────
Discovery Phase:
  LOAD: legacy-jakarta-analysis.skill.md
  LOAD: oracle-sp-analysis.skill.md
  DEFER: all implementation skills

Planning Phase:
  LOAD: api-migration-planning.skill.md
  LOAD: sp-handling-options.skill.md (triggers user choice)
  DEFER: implementation skills

Implementation Phase:
  LOAD: based on user's SP choice:
    - sp-to-jdbc.skill.md OR
    - sp-to-api-client.skill.md OR
    - dual-datasource-config.skill.md
  LOAD: spring-boot-patterns.skill.md
```

---

## 7. File Artifacts to Create

### 7.1 — Agent Files

```
our-services/.github/agents/
├── api-migration.agent.md           ← Main orchestrator
└── api-discovery.agent.md           ← Subagent for analysis
```

### 7.2 — Skill Files

```
our-services/.github/skills/
└── legacy/
    ├── SKILL.md                     ← Skill index
    ├── jakarta-analysis.skill.md
    ├── oracle-sp-analysis.skill.md
    └── dependency-mapping.skill.md

replatforming/services/.github/skills/
└── migration/
    ├── SKILL.md                     ← Skill index
    ├── planning.skill.md
    ├── sp-to-jdbc.skill.md
    ├── sp-to-api-client.skill.md
    ├── dual-datasource.skill.md
    └── stub-generation.skill.md
```

### 7.3 — Instruction Files

```
our-services/.github/
├── copilot-instructions.md          ← Global conventions

replatforming/services/.github/
├── copilot-instructions.md          ← Microservice conventions
└── migration.instructions.md        ← Migration-specific rules
```

### 7.4 — Prompt Files

```
our-services/.github/prompts/
├── migrate-api.prompt.md            ← Entry point for migration
└── analyze-api.prompt.md            ← Entry point for analysis only
```

---

## 8. Handling the Multi-Workspace Challenge

### 8.1 — Skill Symlink Strategy (Optional)

For teams that frequently open microservices individually:

```bash
# Create symlinks from microservice to shared skills
cd replatforming/services/identity/.github/skills
ln -s ../../.github/skills/migration migration
```

### 8.2 — Instruction-Based Skill Reference

The microservice-level `copilot-instructions.md` can reference parent skills:

```markdown
# Microservice Copilot Instructions

## Migration Skills
When performing migrations, load skills from parent:
- See `../../.github/skills/migration/` for migration procedures
- These skills are authoritative for all API migration tasks
```

### 8.3 — Prompt-Based Context Loading

Migration prompts explicitly reference required skill files:

```markdown
# /migrate-api prompt
Load these skills before proceeding:
#file:../../.github/skills/migration/planning.skill.md
#file:../../.github/skills/legacy/jakarta-analysis.skill.md
```

---

## 9. Implementation Phases

### Phase 1: Core Agent Setup
- Create `api-migration.agent.md` orchestrator
- Create `api-discovery.agent.md` subagent
- Set up folder structure

### Phase 2: Legacy Skills
- Create Jakarta REST analysis skill
- Create Oracle SP analysis skill
- Create dependency mapping skill

### Phase 3: Migration Skills
- Create planning skill with user choice flow
- Create SP handling skills (4 variants)
- Create implementation templates

### Phase 4: Instructions & Prompts
- Create copilot-instructions at each level
- Create entry-point prompts
- Test multi-workspace scenarios

### Phase 5: Integration Testing
- Test root workspace migrations
- Test microservice workspace migrations
- Verify skill loading in each context
