# Multi-Workspace Skill Loading Strategy

> How to ensure skills are discoverable regardless of which folder the workspace is opened at.

---

## The Problem

In a monorepo with this structure:

```
our-services/                    ← Can open workspace here
├── .github/
│   ├── agents/
│   └── skills/legacy/           ← Legacy skills
├── user-service/
└── replatforming/
    └── services/                ← OR open workspace here
        ├── .github/
        │   └── skills/migration/ ← Migration skills
        └── identity/            ← OR open workspace here
```

Skills are only auto-discovered when they're inside the workspace root's `.github/skills/` folder.

| Workspace Opened At | Sees Legacy Skills | Sees Migration Skills |
|--------------------|--------------------|-----------------------|
| `our-services/` | ✓ Yes | ✓ Yes (nested) |
| `replatforming/services/` | ✗ No | ✓ Yes |
| `replatforming/services/identity/` | ✗ No | Via parent |

## Solution Strategies

### Strategy 1: Prompt-Based Skill References (Recommended)

In migration prompts, explicitly load required skills using `#file:` references:

```markdown
# /migrate-api prompt

Before proceeding, load these skill files:

## Legacy Analysis Skills
#file:../../../.github/skills/legacy/jakarta-analysis.skill.md
#file:../../../.github/skills/legacy/oracle-sp-analysis.skill.md
#file:../../../.github/skills/legacy/dependency-mapping.skill.md

## Migration Skills  
#file:../../.github/skills/migration/planning.skill.md
#file:../../.github/skills/migration/spring-boot-patterns.skill.md
```

**Pros:**
- Works from any workspace location
- Explicit control over which skills are loaded
- No file system changes needed

**Cons:**
- Relative paths can be fragile
- Must maintain correct paths in prompts

### Strategy 2: Skill Symlinks

Create symbolic links from microservice folders to shared skills:

```bash
# From identity service
cd replatforming/services/identity/.github/skills
ln -s ../../../.github/skills/migration migration
ln -s ../../../../.github/skills/legacy legacy
```

Resulting structure:
```
identity/.github/skills/
├── migration -> ../../../.github/skills/migration
└── legacy -> ../../../../.github/skills/legacy
```

**Pros:**
- Skills appear local to each microservice
- Standard skill discovery works

**Cons:**
- Symlinks need maintenance
- Windows support (requires admin or Developer Mode)
- Git handling of symlinks varies

### Strategy 3: Skill Registry in Instructions

Reference skills via instructions file that gets auto-loaded:

```markdown
# .github/copilot-instructions.md (in identity service)

## Migration Skills Reference

When performing API migrations, these skills are available:

| Skill | Path | Purpose |
|-------|------|---------|
| Legacy Analysis | `../../../.github/skills/legacy/` | Analyze Jakarta REST endpoints |
| Migration Planning | `../../.github/skills/migration/` | Create migration plans |

Load skills with: #file:{path}
```

**Pros:**
- Documents skill locations
- Auto-loaded with any request

**Cons:**
- Doesn't auto-load skills
- User must manually reference

### Strategy 4: Root-Level Agent for Migrations Only

Keep the migration agent at root level only. When opening microservice workspace:

```markdown
# identity/.github/copilot-instructions.md

## API Migration

For API migration workflow, open the root monorepo workspace (`our-services/`)
and use the `/migrate-api` prompt.

Migration agents are not available when opening individual microservices.
```

**Pros:**
- Simplest to implement
- No path management issues

**Cons:**
- Requires switching workspaces
- May not fit team workflow

## Strategy 5: Composite Skill Loader (Advanced)

Create a "skill loader" prompt that detects context and loads appropriate skills:

```markdown
# /load-migration-skills prompt

I'll detect your workspace context and load the appropriate migration skills.

Detecting workspace...

If at ROOT (our-services/):
  - Load: .github/skills/legacy/*
  - Load: replatforming/services/.github/skills/migration/*

If at REPLATFORMING (replatforming/services/):
  - Load: .github/skills/migration/*
  - Reference: ../../.github/skills/legacy/* (may require root access)

If at MICROSERVICE (e.g., identity/):
  - Load: ../.github/skills/migration/* (from parent)
  - Note: Legacy skills unavailable without root workspace
```

## Recommended Approach

**For most teams:**

1. **Primary migrations from root workspace** (Strategy 4)
   - Open `our-services/` for migration work
   - All skills discoverable
   - Prompts work without path gymnastics

2. **Prompt-based loading for flexibility** (Strategy 1)
   - When must work from microservice workspace
   - Add explicit `#file:` references in prompt
   - Accept that legacy skills may be harder to reach

3. **Symlinks for heavy microservice work** (Strategy 2)
   - If team frequently opens microservices directly
   - One-time setup per microservice
   - Best discovery experience

## Skill Isolation

To prevent migration skills from interfering with other agents:

### Use Specific applyTo Patterns

```yaml
# In skill file
---
applyTo: "**/migration/**,**/*Migration*"
---
```

### Namespace with Agent Name

Place skills in agent-specific folders:
```
.github/skills/
├── api-migration/     # Only used by api-migration agent
│   ├── legacy/
│   └── migration/
└── general/           # Used by all agents
```

### Explicit Skill Invocation

Skills use descriptions that match specific intents:
```yaml
# planning.skill.md
---
description: "Design migration plans for API migration from legacy to microservice"
applies_when:
  - User explicitly requested API migration
  - Discovery report exists
  - Migration type is determined
---
```

This prevents accidental invocation during general coding tasks.
