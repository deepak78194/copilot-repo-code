# VS Code Agent Customization - Official Reference

> Consolidated from [VS Code Official Documentation](https://code.visualstudio.com/docs/copilot/customization)

## Decision Flow - When to Use What

| Primitive | When to Use |
|-----------|-------------|
| **Workspace Instructions** | Always-on, applies everywhere in the project |
| **File Instructions** | Explicit via `applyTo` patterns, or on-demand via `description` |
| **Prompts** | Single focused task with parameterized inputs |
| **Skills** | On-demand workflow with bundled assets (scripts/templates) |
| **Custom Agents** | Subagents for context isolation, or multi-stage workflows with tool restrictions |
| **Hooks** | Deterministic shell commands at agent lifecycle points (block tools, auto-format, inject context) |

---

## 1. Custom Agents (.agent.md)

> [Official Docs](https://code.visualstudio.com/docs/copilot/customization/custom-agents)

Custom personas with specific tools, instructions, and behaviors. Use for orchestrated workflows with role-based tool restrictions.

### Locations

| Path | Scope |
|------|-------|
| `.github/agents/*.agent.md` | Workspace |
| `<profile>/agents/*.agent.md` | User profile |

### Frontmatter Schema

```yaml
---
description: "<required>"    # For agent picker and subagent discovery
name: "Agent Name"           # Optional, defaults to filename
tools: [search, web]         # Optional: aliases, MCP (<server>/*), extension tools
model: "Claude Sonnet 4"     # Optional, uses picker default; supports array for fallback
argument-hint: "Task..."     # Optional, input guidance
agents: [agent1, agent2]     # Optional, restrict allowed subagents by name (omit = all, [] = none)
user-invocable: true         # Optional, show in agent picker (default: true)
disable-model-invocation: false  # Optional, prevent subagent invocation (default: false)
handoffs: [...]              # Optional, transitions to other agents
---
```

### Invocation Control

| Attribute | Default | Effect |
|-----------|---------|--------|
| `user-invocable: false` | `true` | Hide from agent picker, only accessible as subagent |
| `disable-model-invocation: true` | `false` | Prevent other agents from invoking as subagent |

### Model Fallback

```yaml
model: ['Claude Sonnet 4.5 (copilot)', 'GPT-5 (copilot)']  # First available model is used
```

### Tool Aliases

| Alias | Purpose |
|-------|---------|
| `execute` | Run shell commands |
| `read` | Read file contents |
| `edit` | Edit files |
| `search` | Search files or text |
| `agent` | Invoke custom agents as subagents |
| `web` | Fetch URLs and web search |
| `todo` | Manage task lists |

### Common Tool Patterns

```yaml
tools: [read, search]             # Read-only research
tools: [myserver/*]               # MCP server only
tools: [read, edit, search]       # No terminal access
tools: []                         # Conversational only
```

### Agent Template

```markdown
---
description: "{Use when... trigger phrases for subagent discovery}"
tools: [{minimal set of tool aliases}]
user-invocable: false
---
You are a specialist at {specific task}. Your job is to {clear purpose}.

## Constraints
- DO NOT {thing this agent should never do}
- DO NOT {another restriction}
- ONLY {the one thing this agent does}

## Approach
1. {Step one of how this agent works}
2. {Step two}
3. {Step three}

## Output Format
{Exactly what this agent should return}
```

### Best Practices

1. **Single role**: One persona with focused responsibilities per agent
2. **Minimal tools**: Only include what the role needs
3. **Clear boundaries**: Define what the agent should NOT do
4. **Keyword-rich description**: Include trigger words for delegation

### Anti-patterns

- **Swiss-army agents**: Too many tools, tries to do everything
- **Vague descriptions**: "A helpful agent" doesn't guide delegation
- **Role confusion**: Description doesn't match body persona
- **Circular handoffs**: A → B → A without progress criteria

---

## 2. Agent Skills (SKILL.md)

> [Official Docs](https://code.visualstudio.com/docs/copilot/customization/agent-skills)

Folders of instructions, scripts, and resources that agents load on-demand for specialized tasks.

### Folder Structure

```
.github/skills/<skill-name>/
├── SKILL.md           # Required (name must match folder)
├── scripts/           # Executable code
├── references/        # Docs loaded as needed
└── assets/            # Templates, boilerplate
```

### Locations

| Path | Scope |
|------|-------|
| `.github/skills/<name>/` | Project |
| `.agents/skills/<name>/` | Project |
| `.claude/skills/<name>/` | Project |
| `~/.copilot/skills/<name>/` | Personal |
| `~/.agents/skills/<name>/` | Personal |
| `~/.claude/skills/<name>/` | Personal |

### SKILL.md Frontmatter

```yaml
---
name: skill-name              # Required: 1-64 chars, lowercase alphanumeric + hyphens, must match folder
description: 'What and when to use. Max 1024 chars.'
argument-hint: 'Optional hint shown for slash invocation'
user-invocable: true          # Optional: show as slash command (default: true)
disable-model-invocation: false # Optional: disable automatic model-triggered loading
---
```

### Skill Template

```markdown
---
name: webapp-testing
description: 'Test web applications using Playwright. Use for verifying frontend, debugging UI, capturing screenshots.'
---

# Web Application Testing

## When to Use
- Verify frontend functionality
- Debug UI behavior

## Procedure
1. Start the web server
2. Run [test script](./scripts/test.js)
3. Review screenshots in `./screenshots/`
```

### Progressive Loading

1. **Discovery** (~100 tokens): Agent reads `name` and `description`
2. **Instructions** (<5000 tokens): Loads `SKILL.md` body when relevant
3. **Resources**: Additional files load only when referenced

### Slash Command Behavior

| Configuration | Slash command | Auto-loaded |
|---|---|---|
| Default (both omitted) | Yes | Yes |
| `user-invocable: false` | No | Yes |
| `disable-model-invocation: true` | Yes | No |
| Both set | No | No |

### Best Practices

1. **Keyword-rich descriptions**: Include trigger words for discovery
2. **Progressive loading**: Keep SKILL.md under 500 lines; use reference files
3. **Relative paths**: Always use `./` for skill resources
4. **Self-contained**: Include all procedural knowledge to complete the task

### Anti-patterns

- **Vague descriptions**: "A helpful skill" doesn't enable discovery
- **Monolithic SKILL.md**: Everything in one file instead of references
- **Name mismatch**: Folder name doesn't match `name` field
- **Missing procedures**: Descriptions without step-by-step guidance

---

## 3. File Instructions (.instructions.md)

> [Official Docs](https://code.visualstudio.com/docs/copilot/customization/custom-instructions)

Guidelines loaded on-demand when relevant to the current task, or explicitly when files match a pattern.

### Locations

| Path | Scope |
|------|-------|
| `.github/instructions/*.instructions.md` | Workspace |
| `<profile>/instructions/*.instructions.md` | User profile |

### Frontmatter Schema

```yaml
---
description: "<required>"    # For on-demand discovery—keyword-rich
name: "Instruction Name"     # Optional, defaults to filename
applyTo: "**/*.ts"           # Optional, auto-attach for matching files
---
```

### Discovery Modes

| Mode | Trigger | Use Case |
|------|---------|----------|
| **On-demand** (`description`) | Agent detects task relevance | Task-based: migrations, refactoring, API work |
| **Explicit** (`applyTo`) | Files matching glob in context | File-based: language standards, framework rules |
| **Manual** | `Add Context` → `Instructions` | Ad-hoc attachment |

### applyTo Patterns

```yaml
applyTo: "**"                           # ALWAYS included (use with caution)
applyTo: "**/*.py"                      # All Python files
applyTo: ["src/**", "lib/**"]           # Multiple patterns (OR)
applyTo: src/**, lib/**                 # Multiple patterns without array syntax (OR)
applyTo: "src/api/**/*.ts"              # Specific folder + extension
```

**Note**: Applied when creating or modifying matching files, not for read-only operations.

### Instructions Template

```markdown
---
description: "Use when writing database migrations, schema changes, or data transformations. Covers safety checks and rollback patterns."
---
# Migration Guidelines

- Always create reversible migrations
- Test rollback before merging
- Never drop columns in the same release as code removal
```

### Best Practices

1. **Keyword-rich descriptions**: Include trigger words for on-demand discovery
2. **One concern per file**: Separate files for testing, styling, documentation
3. **Concise and actionable**: Share context window—keep focused
4. **Show, don't tell**: Brief code examples over lengthy explanations

### Anti-patterns

- **Vague descriptions**: "Helpful coding tips" doesn't enable discovery
- **Overly broad applyTo**: `"**"` with content only relevant to specific files
- **Duplicating docs**: Copy README instead of linking
- **Mixing concerns**: Testing + API design + styling in one file

---

## 4. Prompts (.prompt.md)

> [Official Docs](https://code.visualstudio.com/docs/copilot/customization/prompt-files)

Reusable task templates triggered on-demand in chat. Single focused task with parameterized inputs.

### Locations

| Path | Scope |
|------|-------|
| `.github/prompts/*.prompt.md` | Workspace |
| `<profile>/prompts/*.prompt.md` | User profile |

### Frontmatter Schema

```yaml
---
description: "<recommended>" # Optional, but improves discoverability
name: "Prompt Name"          # Optional, defaults to filename
argument-hint: "Task..."     # Optional: hint shown in chat input
agent: "agent"               # Optional: ask, agent, plan, or custom agent
model: "GPT-5 (copilot)"     # Optional: selected model, or fallback array
tools: [search, web]         # Optional: built-in, tool sets, MCP (<server>/*), extension
---
```

### Model Fallback

```yaml
model: ['GPT-5 (copilot)', 'Claude Sonnet 4.5 (copilot)']
```

### Prompt Template

```markdown
---
description: "Generate test cases for selected code"
agent: "agent"
---
Generate comprehensive test cases for the provided code:
- Include edge cases and error scenarios
- Follow existing test patterns in the codebase
- Use descriptive test names
```

### Context References

- **Files**: Use Markdown links `[config](./config.json)`
- **Tools**: Use `#tool:<name>` syntax

### Invocation Methods

- **Chat**: Type `/` → select from prompts and skills
- **Command**: `Chat: Run Prompt...`
- **Editor**: Open prompt file → play button

### Tool Priority (when both prompt and agent define tools)

1. Tools from prompt file
2. Tools from referenced custom agent
3. Default tools for selected agent

### Best Practices

1. **Single task focus**: One prompt = one well-defined task
2. **Output examples**: Show expected format when quality depends on structure
3. **Reuse over duplication**: Reference instruction files instead of copying

### Anti-patterns

- **Multi-task prompts**: "create and test and deploy" in one prompt
- **Vague descriptions**: Descriptions that don't help users understand when to use
- **Over-tooling**: Many tools when the task only needs search or file access

---

## 5. Workspace Instructions (copilot-instructions.md / AGENTS.md)

> [Official Docs](https://code.visualstudio.com/docs/copilot/customization/custom-instructions)

Guidelines that automatically apply to all chat requests across your entire workspace.

### File Types (Choose One)

| File | Location | Purpose |
|------|----------|---------|
| `copilot-instructions.md` | `.github/` | Project-wide standards (recommended, cross-editor) |
| `AGENTS.md` | Root or subfolders | Open standard, monorepo hierarchy support |

**Use only one—not both.**

### AGENTS.md Hierarchy (for Monorepos)

```
/AGENTS.md              # Root defaults
/frontend/AGENTS.md     # Frontend-specific (overrides root)
/backend/AGENTS.md      # Backend-specific (overrides root)
```

### Workspace Instructions Template

```markdown
# Project Guidelines

## Code Style
{Language and formatting preferences—reference key files that exemplify patterns}

## Architecture
{Major components, service boundaries, the "why" behind structural decisions}

## Build and Test
{Commands to install, build, test—agents will attempt to run these}

## Conventions
{Patterns that differ from common practices—include specific examples}
```

### Best Practices

1. **Minimal by default**: Only what's relevant to *every* task
2. **Concise and actionable**: Every line should guide behavior
3. **Link, don't embed**: Reference docs instead of copying
4. **Keep current**: Update when practices change

### Anti-patterns

- **Using both file types**: Having both `copilot-instructions.md` and `AGENTS.md`
- **Kitchen sink**: Everything instead of what matters most
- **Duplicating docs**: Copying README instead of linking
- **Obvious instructions**: Conventions already enforced by linters

---

## 6. Hooks (.json)

> [Official Docs](https://code.visualstudio.com/docs/copilot/customization/hooks)

Deterministic lifecycle automation for agent sessions. Use hooks to enforce policy, automate validation, and inject runtime context.

### Locations

| Path | Scope |
|------|-------|
| `.github/hooks/*.json` | Workspace (team-shared) |
| `.claude/settings.local.json` | Workspace local (not committed) |
| `.claude/settings.json` | Workspace |
| `~/.claude/settings.json` | User profile |

### Hook Events

| Event | Trigger |
|------|-------|
| `SessionStart` | First prompt of a new agent session |
| `UserPromptSubmit` | User submits a prompt |
| `PreToolUse` | Before tool invocation |
| `PostToolUse` | After successful tool invocation |
| `PreCompact` | Before context compaction |
| `SubagentStart` | Subagent starts |
| `SubagentStop` | Subagent ends |
| `Stop` | Agent session ends |

### Configuration Format

```json
{
  "hooks": {
    "PreToolUse": [
      {
        "type": "command",
        "command": "./scripts/validate-tool.sh",
        "timeout": 15
      }
    ]
  }
}
```

Each hook command supports:
- `type` (must be `command`)
- `command` (default)
- `windows`, `linux`, `osx` (platform overrides)
- `cwd`, `env`, `timeout`

### Input / Output Contract

Hooks receive JSON on stdin and can return JSON on stdout.

**PreToolUse permission output example:**

```json
{
  "hookSpecificOutput": {
    "hookEventName": "PreToolUse",
    "permissionDecision": "ask",
    "permissionDecisionReason": "Needs user confirmation"
  }
}
```

**Permission decisions**: `allow` | `ask` | `deny`

**Exit codes:**
- `0` success
- `2` blocking error
- Other values: non-blocking warnings

### Hooks vs Instructions

| Primitive | Behavior |
|------|-------|
| Instructions / Prompts / Skills / Agents | Guidance (non-deterministic) |
| Hooks | Runtime enforcement and deterministic automation |

### Best Practices

1. Keep hooks small and auditable
2. Validate and sanitize hook inputs
3. Avoid hardcoded secrets in scripts
4. Prefer workspace hooks for team policy, user hooks for personal automation

### Anti-patterns

- Running long hooks that block normal flow
- Using hooks where plain instructions are sufficient
- Letting agents edit hook scripts without approval controls

---

## Quick Reference: Edge Cases

| Question | Answer |
|----------|--------|
| **Instructions vs Skill?** | Does this apply to *most* work, or *specific* tasks? Most → Instructions. Specific → Skill. |
| **Skill vs Prompt?** | Both appear as slash commands. Multi-step workflow with bundled assets → Skill. Single focused task → Prompt. |
| **Skill vs Custom Agent?** | Same capabilities for all steps → Skill. Need context isolation or different tool restrictions per stage → Custom Agent. |
| **Hooks vs Instructions?** | Instructions *guide* agent behavior (non-deterministic). Hooks *enforce* behavior deterministically via shell commands. |

---

## Common Pitfalls

### Description is the Discovery Surface
The `description` field is how the agent decides whether to load a skill, instruction, or agent. If trigger phrases aren't IN the description, the agent won't find it.

**Use the "Use when..." pattern:**
```yaml
description: "Use when writing database migrations, schema changes, or data transformations."
```

### YAML Frontmatter Silent Failures
Unescaped colons, tabs instead of spaces, or `name` that doesn't match folder name cause silent failures.

**Always quote descriptions with colons:**
```yaml
description: "Use when: doing X"
```

### `applyTo: "**"` Burns Context
This means "always included for every file request." Use specific globs unless the instruction truly applies to all files:
```yaml
applyTo: "**/*.py"     # Better: specific to Python files
applyTo: "src/api/**"  # Better: specific to API folder
```

---

## File Locations Summary

| Type | File | Location |
|------|------|----------|
| Workspace Instructions | `copilot-instructions.md` | `.github/` |
| Workspace Instructions | `AGENTS.md` | Root or subfolders |
| File Instructions | `*.instructions.md` | `.github/instructions/` |
| Prompts | `*.prompt.md` | `.github/prompts/` |
| Skills | `SKILL.md` | `.github/skills/<name>/` |
| Custom Agents | `*.agent.md` | `.github/agents/` |
| Hooks | `*.json` | `.github/hooks/` |

**User-level locations:** `%APPDATA%\Code\User\prompts/` (Windows)
