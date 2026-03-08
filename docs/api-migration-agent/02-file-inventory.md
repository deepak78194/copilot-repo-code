# API Migration Agent - Complete File Inventory

> Summary of all artifacts created for the API migration agent system.

---

## Folder Structure

```
docs/api-migration-agent/
├── 00-architecture-plan.md              # Overall architecture and decisions
├── 01-skill-loading-strategy.md         # Multi-workspace skill handling
├── agents/
│   ├── api-migration.agent.md           # Main orchestrator agent
│   └── api-discovery.agent.md           # Subagent for analysis phase
├── skills/
│   ├── legacy/                          # Skills for legacy service analysis
│   │   ├── SKILL.md                     # Index
│   │   ├── jakarta-analysis.skill.md    # Jakarta REST endpoint analysis
│   │   ├── oracle-sp-analysis.skill.md  # Stored procedure cataloging
│   │   └── dependency-mapping.skill.md  # API-specific dependency mapping
│   └── migration/                       # Skills for migration implementation
│       ├── SKILL.md                     # Index
│       ├── planning.skill.md            # Migration planning with user choices
│       ├── spring-boot-patterns.skill.md # Target framework patterns
│       ├── sp-to-jdbc.skill.md          # DECOMPOSE: SP to native queries
│       ├── sp-to-api-client.skill.md    # API_CLIENT: SP to REST calls
│       ├── dual-datasource.skill.md     # KEEP_SP: Oracle + Postgres config
│       └── stub-generation.skill.md     # STUB: Interface stubs for later
├── prompts/
│   ├── migrate-api.prompt.md            # /migrate-api entry point
│   └── analyze-api.prompt.md            # /analyze-api entry point
└── instructions/
    ├── root-copilot-instructions.md     # For root monorepo workspace
    ├── replatforming-copilot-instructions.md # For microservices
    └── migration.instructions.md        # Migration-specific rules (applyTo)
```

## Deployment to Your Monorepo

Copy these files to your actual monorepo:

```bash
# From your monorepo root (our-services/)

# 1. Create folder structure
mkdir -p .github/agents
mkdir -p .github/skills/legacy
mkdir -p .github/prompts

# 2. Copy agent files
cp docs/api-migration-agent/agents/*.agent.md .github/agents/

# 3. Copy legacy skills
cp docs/api-migration-agent/skills/legacy/*.md .github/skills/legacy/

# 4. Copy prompts
cp docs/api-migration-agent/prompts/*.prompt.md .github/prompts/

# 5. Copy root instructions
cp docs/api-migration-agent/instructions/root-copilot-instructions.md .github/copilot-instructions.md

# 6. For replatforming folder
mkdir -p replatforming/services/.github/skills/migration

# 7. Copy migration skills
cp docs/api-migration-agent/skills/migration/*.md replatforming/services/.github/skills/migration/

# 8. Copy replatforming instructions
cp docs/api-migration-agent/instructions/replatforming-copilot-instructions.md replatforming/services/.github/copilot-instructions.md
```

## Usage

### Start Migration
```
/migrate-api
```
or
```
"migrate the changePassword API from user-service to identity"
```

### Analyze Only
```
/analyze-api
```
or
```
"analyze the getUserProfile API in user-service for migration"
```

## Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Agent per migration type? | No, single orchestrator | Migration type detection is simple, avoid agent proliferation |
| Skills in agent? | No, separate skill files | Keep agent thin, load skills on-demand |
| SP handling decision | User choice required | Too impactful to auto-decide; different trade-offs |
| Multi-workspace | Prompt-based loading | Most flexible, works from any workspace |
| Subagent for discovery | Yes | Preserves main context window for implementation |

## Token Budget

| Component | Estimated Tokens | Notes |
|-----------|-----------------|-------|
| copilot-instructions.md | ~200 | Always loaded |
| api-migration.agent.md | ~800 | Loaded when agent invoked |
| Active skill (1 at a time) | ~600 | Phase-appropriate skill only |
| Discovery subagent result | ~800 | Compact summary |
| **Total overhead** | **~2,400** | Leaves 157K+ for actual work |

## Extension Points

### Add New Migration Type

1. Update `api-migration.agent.md` to recognize new type
2. Create skill file in `skills/migration/`
3. Add to skill index

### Add New SP Handling Strategy

1. Create new skill file (e.g., `sp-to-kafka.skill.md`)
2. Update `planning.skill.md` to offer as choice
3. Update agent to load new skill when chosen

### Support New Target Framework

1. Create framework-specific patterns skill (e.g., `quarkus-patterns.skill.md`)
2. Update agent to detect and load appropriate framework skill
