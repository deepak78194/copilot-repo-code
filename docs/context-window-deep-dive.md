# VS Code Copilot Context Window — Deep Dive
> Understanding the context window indicator, what fills it, and how to design lean, effective customizations.

---

## What Is the Context Window?

When you send a message to Copilot, the model doesn't just see your latest message. It receives a single, merged block of text called the **context window** — containing everything it needs to respond: instructions, tool schemas, conversation history, file contents, and prior tool outputs.

The indicator in VS Code shows you exactly how that window is being consumed in real time.

```
Context Window
84.4K / 160K tokens • 53%
━━━━━━━━━━━━━━━━━━━━━━━━

System
  System Instructions    3.2%
  Tool Definitions      14.8%
  Reserved Output       15.3%

User Context
  Messages               8.4%
  Files                  3.2%
  Tool Results           7.9%

Actions
  [ Compact Conversation ]
```

**In this screenshot:**
- Total model limit: **160,000 tokens**
- Currently used: **84,400 tokens (53%)**
- Remaining usable space: ~**75,600 tokens**

> **1 token ≈ 0.75 words** (rough rule of thumb). 160K tokens ≈ ~120,000 words or ~240 pages of text.

---

## Breaking Down Each Section

### SYSTEM (33.3% of total — always present)

These are loaded before your first message and are largely outside your direct conversation. They establish the environment the model operates in.

---

#### System Instructions — 3.2% (≈ 5,120 tokens)

**What it is:**  
Every instruction file, custom agent persona, and built-in guidance that VS Code injects as the foundational "how to behave" layer. This is the first thing the model reads.

**What fills this bucket:**

| Source File | Where It Lives | When It Loads |
|---|---|---|
| `copilot-instructions.md` | `.github/` or workspace root | Always — every session |
| `.instructions.md` files | `.github/instructions/` | When `applyTo` pattern matches current file |
| Custom agent `.agent.md` | `.github/agents/` | When that agent is selected in the picker |
| Skill SKILL.md content | `.github/skills/` | When the skill is invoked or referenced |
| Built-in VS Code guidance | Internal | Always |

**Why it matters for design:**  
Everything here is loaded on **every request**. A 2,000-word `copilot-instructions.md` costs you roughly 2,700 tokens on every single message. If you have multiple `.instructions.md` files with broad `applyTo` patterns, they all stack.

**Design principle: Keep system instructions surgical.**
```
✅ Good: Short, specific rules — "Always use async/await, never callbacks"
❌ Bad:  Long narrative context — "We are a fintech company founded in 2018..."
```

---

#### Tool Definitions — 14.8% (≈ 23,680 tokens)

**What it is:**  
The JSON schema for every tool the agent has access to. The model needs to "read" each tool's description, parameters, and types before it can decide to call them.

**What fills this bucket:**

| Tool Source | Tokens (approx) |
|---|---|
| Built-in agent tools (file read, terminal, grep, etc.) | ~3,000–5,000 |
| Browser tools (`clickElement`, `screenshotPage`, etc.) | ~2,000–3,000 |
| `rename` + `usages` tools | ~500–800 |
| Each MCP server you connect | ~1,000–3,000 per server |
| Each Extension-provided tool | ~500–1,500 per tool |
| Custom agent tool restrictions | Reduces this if tools are disabled |

**In the screenshot, 14.8% = ~23,680 tokens just for tool schemas.** This is a fixed overhead per session — before you've typed a single word.

**Why it matters:**  
This is the single largest "silent" consumer of context space. Enabling every available tool (browser tools, multiple MCP servers, terminal sandbox, all language model tools) can easily push this to 20%+ of your total context window.

**Design principle: Only enable the tools you need.**
```json
// In a custom agent .agent.md — restrict tools to what's relevant:
tools:
  - read_file
  - grep_search
  - run_in_terminal
// Don't enable browser tools for a backend-only agent
```

---

#### Reserved Output — 15.3% (≈ 24,480 tokens)

**What it is:**  
A chunk of the context window that is **permanently locked** and never available for input. It is pre-reserved to guarantee the model has enough space to write its response.

**This space is:**
- ❌ Not usable for your instructions
- ❌ Not usable for file context
- ❌ Not usable for conversation history
- ✅ The model's guaranteed writing space

**Why it matters:**  
Your effective usable context window is not 160K — it's:
```
160K - Reserved Output (15.3%) - Tool Definitions (14.8%)
= 160K - 24,480 - 23,680
= ~111,840 tokens of actual usable space
```

This is the real budget you're working with across System Instructions, Messages, Files, and Tool Results combined.

---

### USER CONTEXT (19.5% of total — grows during conversation)

These sections grow as you use the session. They hold everything from your actual work.

---

#### Messages — 8.4% (≈ 13,440 tokens)

**What it is:**  
The entire conversation history — every user message and every assistant response accumulated in this session.

**What fills this bucket:**

| Source | Example |
|---|---|
| Your chat messages | "Refactor the auth module to use JWT" |
| Agent responses | The full text of every reply |
| Inline chat turns | Edits and explanations in editor |
| Steering messages | Mid-response redirections |
| TODO list content | Task plans generated by the agent |

**Why it matters:**  
This grows continuously. A long back-and-forth session where the agent explains code in detail can easily consume 30,000–50,000 tokens in Messages alone — eating up the remaining usable budget.

**This is the primary reason `/compact` exists.** Compaction summarizes old turns into a short summary, freeing this space without losing the key decisions made earlier.

**Design principle: Compact proactively during long sessions.**
```
/compact focus on the architectural decisions and API contract
```

---

#### Files — 3.2% (≈ 5,120 tokens)

**What it is:**  
File contents that have been explicitly brought into context — either by you or by the agent's tools.

**What fills this bucket:**

| How Files Enter Context | When |
|---|---|
| `#file:src/auth.ts` in chat | Immediately when you use the `#file:` reference |
| Files opened in the active editor | When you use `#editor` or inline chat |
| `#codebase` search results | When the agent queries your workspace |
| Skill files loaded by reference | When a skill is invoked and loads its own files |
| Files the agent reads with read_file tool | **Goes to Tool Results, not here** |

> **Important distinction:** Files you explicitly attach (`#file:`) → **Files bucket**. Files the agent reads via tools → **Tool Results bucket**.

**Why it matters for skills and prompts:**  
If your skill or prompt file uses `#file:src/schema.sql` to embed a schema, that entire file's contents land here. A single large schema file can consume this entire 3.2% budget.

**Design principle: Reference small, targeted files. Avoid referencing large files unless essential.**

---

#### Tool Results — 7.9% (≈ 12,640 tokens)

**What it is:**  
The output returned to the model from every tool call it has made during this session. This is often the most unpredictable bucket.

**What fills this bucket:**

| Tool Call | Output That Lands Here |
|---|---|
| `read_file` | The contents of the file read |
| `run_in_terminal` | All terminal stdout/stderr |
| `grep_search` | All matching lines from the search |
| `semantic_search` | Returned code snippets |
| `list_dir` | Directory listing output |
| `fetch_webpage` | Page content fetched |
| `screenshotPage` | Image data (can be very large) |
| MCP tool calls | Whatever the MCP server returns |

**Why it matters:**  
A single terminal command like `npm install` or `git log --all` can return thousands of lines. The agent's grep searches across a large codebase can return hundreds of matches. All of this accumulates here over a session.

**This is why collapsible terminal output (v1.110) matters visually, but the underlying tokens are still consumed.**

**Design principle: Be specific with tool calls. `grep_search` with a tight pattern uses far fewer tokens than `grep_search` for a common word across the whole codebase.**

---

## How Each Customization File Maps to the Context Window

### The Full Loading Picture

```
┌─────────────────────────────────────────────────────────────────┐
│                    CONTEXT WINDOW (160K)                        │
├──────────────────────┬──────────────────────────────────────────┤
│   SYSTEM (fixed)     │         USER CONTEXT (grows)             │
├──────────────────────┼──────────────────────────────────────────┤
│                      │                                          │
│  System Instructions │  Messages                                │
│  ┌────────────────┐  │  ┌──────────────────────────────────┐    │
│  │copilot-        │  │  │ Turn 1: User message             │    │
│  │instructions.md │  │  │ Turn 1: Agent response           │    │
│  │                │  │  │ Turn 2: User message             │    │
│  │.instructions.md│  │  │ Turn 2: Agent response + plan    │    │
│  │(matched files) │  │  │ ...                              │    │
│  │                │  │  └──────────────────────────────────┘    │
│  │.agent.md       │  │                                          │
│  │(active agent)  │  │  Files                                   │
│  │                │  │  ┌──────────────────────────────────┐    │
│  │SKILL.md        │  │  │ #file: references                │    │
│  │(invoked skill) │  │  │ #editor content                  │    │
│  └────────────────┘  │  │ Skill-referenced files           │    │
│                      │  └──────────────────────────────────┘    │
│  Tool Definitions    │                                          │
│  ┌────────────────┐  │  Tool Results                            │
│  │ Built-in tools │  │  ┌──────────────────────────────────┐    │
│  │ MCP tools      │  │  │ read_file output                 │    │
│  │ Browser tools  │  │  │ terminal output                  │    │
│  │ Extension tools│  │  │ search results                   │    │
│  └────────────────┘  │  │ MCP responses                    │    │
│                      │  └──────────────────────────────────┘    │
│  Reserved Output     │                                          │
│  (LOCKED — never     │                                          │
│   usable for input)  │                                          │
└──────────────────────┴──────────────────────────────────────────┘
```

---

### `copilot-instructions.md` → System Instructions

**File location:** `.github/copilot-instructions.md`  
**Loads:** Every session, every request — always present  
**Bucket:** System Instructions

```markdown
<!-- This entire file is injected into System Instructions on every request -->
<!-- Every word costs tokens on EVERY message you send -->

You are a senior TypeScript developer. Always:
- Use strict null checks
- Prefer functional patterns over imperative
- Write JSDoc for public APIs
```

**Context window impact:**
- Loaded once per session but counted on every request
- 500-word instructions file ≈ 670 tokens permanent overhead
- **Recommendation:** Keep under 300 words / 400 tokens. Every sentence here costs you across ALL future messages in this session.

---

### `.instructions.md` files → System Instructions (conditional)

**File location:** `.github/instructions/*.instructions.md`  
**Loads:** Only when `applyTo` pattern matches the current file  
**Bucket:** System Instructions

```markdown
---
applyTo: "src/api/**/*.ts"
---
<!-- Only loaded when you're working in src/api/ files -->
<!-- Does NOT load when working on frontend files -->

All API handlers must:
- Validate input with Zod schemas
- Return typed APIResponse<T> objects
- Log errors with structured logging
```

**Context window impact:**
- Smart — only costs tokens when relevant
- Multiple `.instructions.md` files can stack if you have overlapping `applyTo` patterns
- **Watch out for:** `applyTo: "**"` — this is effectively the same as `copilot-instructions.md` and always loads

**Practical tip:** Check how many instructions files are loading by watching the System Instructions % rise or fall as you switch between files in different folders.

---

### Custom Agent `.agent.md` → System Instructions + Tool Definitions

**File location:** `.github/agents/my-agent.agent.md`  
**Loads:** When you select this agent from the agent picker  
**Bucket:** System Instructions (persona) + Tool Definitions (its tool list)

```yaml
---
name: backend-agent
description: Focused backend TypeScript agent
tools:
  - read_file
  - grep_search
  - run_in_terminal
  - rename
  - usages
# NOT including: browser tools, MCP tools, screenshot tools
---
You are a focused backend API developer...
```

**Context window impact — two effects:**

1. **System Instructions:** The agent's persona/prompt text is added here. A detailed agent persona of 200 words adds ~270 tokens.

2. **Tool Definitions:** By listing only a subset of tools, you **reduce** the Tool Definitions bucket. Comparing:
   - Full agent with all tools: Tool Definitions ≈ 14.8% (as in screenshot)
   - Focused agent with 5 specific tools: Tool Definitions could drop to ≈ 5–8%
   - **That's 10,000+ tokens freed up just by restricting tools.**

**This is the most powerful lever you have over the context window.**

---

### Skill `SKILL.md` → System Instructions (when invoked)

**File location:** `.github/skills/my-skill/SKILL.md`  
**Loads:** When the skill is invoked (via prompt file or agent reference)  
**Bucket:** System Instructions (skill instructions) + Files (any files the skill references)

```markdown
# API Migration Skill

When asked to migrate an API endpoint:
1. Read the existing handler with read_file
2. Check usages with the #usages tool
3. Apply the new pattern from #file:.github/skills/api-migration/template.ts
4. Run tests with run_in_terminal
```

**Context window impact:**
- The SKILL.md content itself → System Instructions
- Any `#file:` references in the skill → Files bucket
- When the skill runs tools (read_file, etc.) → Tool Results bucket

**Design principle:** A skill file that references a large template file or example file can silently load thousands of tokens. Keep referenced files lean.

---

### Prompt Files `.prompt.md` → Messages or System Instructions

**File location:** `.github/prompts/*.prompt.md`  
**Loads:** When you use the slash command (e.g., `/refactor-api`)  
**Bucket:** Appears in Messages (as a user message) or System Instructions depending on usage

```markdown
---
name: refactor-api
description: Refactor an API endpoint to use the new pattern
---
Please refactor the currently open API endpoint:
1. Update the handler signature to match APIHandler<T>
2. Add Zod input validation
3. Use the new structured logger
4. Update the associated test file
```

**Context window impact:**
- The prompt text is injected as a user message → Messages bucket
- If the prompt file references other files with `#file:`, those go to Files bucket
- Running a prompt with 5 `#file:` attachments can rapidly fill the Files budget

**Design principle:** Prompt files should be task-scoped instructions, not documentation dumps. Link to small, focused helper files rather than entire modules.

---

## Practical Guidance: Reading the Context Window Indicator

### Scenario 1: System Instructions is unusually high (>10%)

```
System Instructions   10.2% ← HIGH
```

**Likely causes:**
- Multiple `.instructions.md` files all matching your current file (broad `applyTo` patterns)
- A large `copilot-instructions.md` file
- A verbose custom agent persona
- A skill with detailed instructions has been loaded

**What to do:**
1. Open the **Agent Debug Panel** (`Ctrl+Shift+P` → `Developer: Open Agent Debug Panel`)
2. Identify which instruction files loaded this session
3. Tighten `applyTo` patterns or trim instruction content

---

### Scenario 2: Tool Definitions is very high (>20%)

```
Tool Definitions     22.4% ← HIGH
```

**Likely causes:**
- Multiple MCP servers connected (each adds 1,000–3,000 tokens)
- Browser tools enabled but not needed
- Running in a full agent mode with all tools enabled

**What to do:**
1. Switch to a **custom agent** that only lists required tools
2. Disable unused MCP servers in the MCP settings
3. Toggle off browser tools in the chat tools picker if not needed

---

### Scenario 3: Tool Results is growing fast

```
Tool Results       18.3% ← GROWING
```

**Likely causes:**
- Agent ran many broad searches (grep across entire codebase)
- Terminal commands with long output (build logs, test output)
- Multiple file reads of large files

**What to do:**
1. Type `/compact` — this is the primary use case for compaction
2. Add guidance: `/compact focus on the changes made so far and ignore build output`
3. In post-compaction sessions, guide the agent to be more targeted: "Search only in `src/api/` not the whole codebase"

---

### Scenario 4: Messages growing and approaching 50%+

```
Messages           38.7% ← VERY HIGH
```

**Likely causes:**
- Long session with many back-and-forth turns
- Agent explained a lot of code in detail
- Multiple plan revisions

**What to do:**
1. Use `/compact` before adding more requests
2. For a new direction: use `/fork` to branch the session (keeping history intact in the original)
3. For a fresh start: open a new chat session and use `/create-instruction` to capture the key decisions as an instruction file first

---

## The Token Budget Formula

For any given session, think of your context window like this:

```
Total Budget = 160,000 tokens

Fixed Costs (always consumed, before you type anything):
  - Reserved Output:    ~24,500 tokens (15.3%)  [LOCKED]
  - Tool Definitions:   ~23,700 tokens (14.8%)  [varies by tools enabled]
  - System Instructions: ~5,100 tokens  (3.2%)  [varies by loaded files]
  ─────────────────────────────────────────────
  Fixed total:          ~53,300 tokens (33.3%)

Your actual working budget:
  160,000 - 53,300 = ~106,700 tokens

This is split across:
  - Messages (conversation history)
  - Files (explicitly referenced)
  - Tool Results (tool call outputs)
```

When those three combined approach ~106,700 tokens, you've hit the wall and the session will auto-compact or fail to proceed.

---

## Designing for Context Efficiency

### Rule 1: Minimize always-on instructions
Files that load every session (`copilot-instructions.md`, broad `applyTo` instructions) cost tokens on every single request. Keep them under 400 tokens combined.

### Rule 2: Use scoped instructions instead of global ones
Instead of one massive `copilot-instructions.md`, create multiple `.instructions.md` files with tight `applyTo` patterns. Each one only loads when relevant.

```
.github/instructions/
  api-patterns.instructions.md      → applyTo: "src/api/**"
  ui-patterns.instructions.md       → applyTo: "src/components/**"
  test-patterns.instructions.md     → applyTo: "**/*.test.ts"
  database-patterns.instructions.md → applyTo: "src/db/**"
```

### Rule 3: Create tool-restricted custom agents
The single biggest win for Tool Definitions cost is a focused custom agent:

```yaml
# .github/agents/code-reviewer.agent.md
tools:
  - read_file
  - grep_search
  - usages
# 3 tools instead of 30 = dramatic reduction in Tool Definitions %
```

### Rule 4: Use `/compact` with focus instructions
Don't compact blindly. Guide the summary:
```
/compact keep: architecture decisions, API contracts, file paths modified, and pending TODOs
       discard: long code explanations and build output
```

### Rule 5: Reference small files, not large ones
In skills and prompts, every `#file:` you reference adds to the Files bucket. Prefer:
- Template/snippet files (50–100 lines) over full module files
- Interface/type definition files over implementation files
- Configuration schemas rather than full config files

### Rule 6: Monitor before compacting
Check the indicator before starting a major task. If you're already at 50%+ before your main work, compact first — otherwise the agent compacts mid-task, potentially losing in-progress context.

---

## Quick Reference: What Goes Where

| Source | Bucket | Loaded When |
|---|---|---|
| `copilot-instructions.md` | System Instructions | Always |
| `.instructions.md` (matched) | System Instructions | File pattern matches |
| `.agent.md` persona text | System Instructions | Agent selected |
| `SKILL.md` content | System Instructions | Skill invoked |
| `.prompt.md` text | Messages | Slash command used |
| Enabled tool schemas | Tool Definitions | Session starts |
| MCP server tool schemas | Tool Definitions | MCP server connected |
| `#file:path` attachments | Files | Explicitly referenced |
| Skill `#file:` references | Files | Skill invoked |
| `read_file` tool output | Tool Results | Agent reads a file |
| Terminal command output | Tool Results | Agent runs terminal |
| `grep_search` results | Tool Results | Agent searches |
| Chat turn content | Messages | Every message sent |
| Model's response text | Messages | Every response |
| Session memory content | Messages | Plan agent recalls plan |

---

*Understanding the context window helps you design better skills, leaner agents, and more efficient sessions — so the model always has the right information, with room to think and respond.*
