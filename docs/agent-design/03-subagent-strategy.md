# Subagent Strategy for Migration Workflows
> When to use subagents, how they work with context windows, how to design handoffs, and how the Discovery phase in your Jakarta→Micronaut workflow benefits most.

---

## What Is a Subagent?

When the main agent calls `runSubagent`, it launches a **completely separate agent instance** with its **own isolated context window**.

```
┌─────────────────────────────────────────────────────────────────┐
│                    MAIN AGENT CONTEXT                           │
│  160K tokens                                                    │
│                                                                 │
│  [System Instructions]  [Tool Defs]  [Messages]  [Files]       │
│         ~1K                ~24K       growing     growing       │
│                                                                 │
│  At turn 15, you've consumed:                                   │
│    Reserved Output: 24.4K  Tool Defs: 23.6K  Messages: 35K     │
│    = 83K already used, only 77K remaining                       │
│                                                                 │
│           ┌─────────────────────────────────────┐              │
│  SPAWNS   │       SUBAGENT CONTEXT              │              │
│  ────────►│  160K FRESH tokens                  │              │
│           │  [System Instructions for sub]       │              │
│           │  [Its own tool calls and results]    │              │
│           │  Can read 50+ files freely           │              │
│           │  Returns ONE compact summary         │              │
│           │  ──────────────────────────────────  │              │
│           │  Only the summary lands in main ctx  │              │
│           └─────────────────────────────────────┘              │
│                           │                                     │
│           compact summary ▼  (Tool Results bucket, ~1-2K tok)   │
│  [Tool Result: subagent returned 800-token discovery summary]   │
└─────────────────────────────────────────────────────────────────┘
```

**Key insight:** A subagent reads 50 files and runs 10 searches — but only its **summary output** appears in your main agent's context. You get all the information, for a tiny context cost.

---

## Context Window Math: Discovery Phase Example

### Without a Subagent

The main agent does the discovery itself:

```
Discovery phase activities:
  read_file × 8 endpoint + DTO files    = ~12,000 tokens (Tool Results)
  grep_search × 6 dependency searches  =  ~6,000 tokens (Tool Results)
  semantic_search × 4 pattern searches =  ~4,000 tokens (Tool Results)
  ─────────────────────────────────────────────────────
  Total consumed from context budget:     ~22,000 tokens

  These Tool Results STAY in context for the rest of the conversation.
  By the time you reach test generation (6 phases later), those 22K tokens
  of raw search results are still sitting there, unused but still counted.
```

### With a Subagent

```
Discovery phase activities (in subagent, separate 160K window):
  Same reads, searches — consumes subagent's context, NOT yours
  Subagent returns: 800-token structured markdown summary

  Total consumed from YOUR context budget:   ~800 tokens (Tool Results)
  Savings vs doing it yourself:             ~21,200 tokens preserved
  That's ~13% of your 160K window freed up for code generation
```

---

## Which Phases Should Use Subagents?

Use this decision matrix to decide:

```
PHASE               SUBAGENT?   REASON
─────────────────────────────────────────────────────────────────────
Discovery/Listen    YES ✓       Reads many files, generates broad analysis
                                You only need the summary, not all raw reads

Research            YES ✓       Pattern matching across codebase
                                Dependency graph exploration
                                High file-read volume, low output volume

Report generation   MAYBE       If report writes to a file (not chat):
                                subagent writes the file, main agent proceeds
                                If report is inline chat: main agent handles it

Planning            NO ✗        Needs full conversation history
                                Needs user's confirmed scope
                                Output becomes part of the ongoing conversation

Implementation      NO ✗        Must coordinate with main agent's edit session
                                Needs shared file state

Test generation     NO ✗        Must reference the files just changed
                                Tight coupling to implementation phase output
```

---

## How to Invoke a Subagent

In your `.agent.md`, the Discovery phase becomes:

```markdown
### Phase 1 — Discovery
Run a research subagent to scan the target endpoint without consuming
main context with raw file reads.

Prompt the subagent with:
  - The endpoint file path provided by the user
  - The research objective from the discovery skill
  - Instruction to return a compact structured summary

The subagent will use the research/SKILL.md approach internally.
When it returns, add the summary to the conversation before Phase 2.
```

The subagent prompt template (what gets sent to the subagent) lives in a `.prompt.md` file:

---

## The Discovery Subagent Prompt File

### `skills/research/discovery-prompt.prompt.md`

```markdown
---
description: Run a discovery scan on a Jakarta endpoint before migration
---

You are a code analysis agent. Your job is to scan one Jakarta REST endpoint
and produce a concise migration discovery summary. You do NOT migrate any code.

## Target
Endpoint file: {{endpoint_path}}

## What to Discover

### 1. Endpoint Definition
- Read the controller/resource file
- List: HTTP method, URL path, method signature, parameter annotations

### 2. DTO Classes
- Find all DTO/POJO classes used in request/response bodies
- For each: package path, field list, any validation annotations

### 3. Service Dependencies
- Find service interfaces called by this controller
- For each: interface package, methods used by the controller

### 4. Existing Tests
- Search for test files that reference this controller by name
- Note: file path, framework used (JUnit 4/5, Mockito, RestAssured?)

### 5. Build System
- Check pom.xml or build.gradle for: Jakarta EE version, test framework versions

### 6. Risk Flags
- Any @Provider, @Filter, @ContainerRequestFilter classes
- Any multipart form endpoints
- Any SSE/streaming routes
- Any custom exception mappers referenced

## Output Format
Return ONLY the following structured markdown. Be concise.

---
## Discovery: {{endpoint_path}}

### Endpoint
- Method + Path: [e.g., GET /api/users/{id}]
- Parameters: [list]
- Produces: [media type]

### DTOs
| Class | Package | Fields |
|-------|---------|--------|
| ...   | ...     | ...    |

### Services
| Interface | Package | Methods Used |
|-----------|---------|--------------|
| ...       | ...     | ...          |

### Tests
| File | Framework | Coverage |
|------|-----------|----------|
| ...  | ...       | ...      |

### Build
- EE version: [version]
- Test framework: [version]

### Risk Flags
[list any, or "None identified"]
---
```

---

## Subagent Design Principles

### 1. Give the Subagent a Clear Exit Contract

The most important thing about a subagent is that you control its **output format**.

```
Good subagent prompt:
  "Return a markdown table with exactly these 4 columns: ..."
  "Your entire response must fit in a code block under 1,000 tokens"
  "Do NOT include raw file contents, only extracted facts"

Bad subagent prompt:
  "Analyze the codebase and tell me what you find"
  → Subagent returns 3,000 tokens of prose
  → That entire prose lands in your Tool Results bucket
  → You've wasted context on verbosity you didn't need
```

### 2. The Subagent Should NOT Return Raw File Contents

```
Subagent reads 8 files to learn about your endpoint = 8,000 raw tokens
Subagent summarizes what it found in structured form = 600 tokens

If it returns the raw content: you spend 8,000 tokens in Tool Results
If it returns the summary: you spend 600 tokens in Tool Results

Always instruct subagents: "Do NOT include raw file/code content.
Extract the facts. Return only the extracted information."
```

### 3. Token Budget for Subagent Output

Aim for subagent output that fits within this budget:

```
Task type                        Target output size
────────────────────────────────────────────────────
Single endpoint discovery        500-800 tokens
Full module analysis             1,000-1,500 tokens
Multi-file dependency map        800-1,200 tokens
Test coverage report             400-600 tokens
```

If a subagent output exceeds 2,000 tokens, your prompt design needs tightening.

---

## When NOT to Use Subagents

### Avoid Subagents When:

**The task needs access to conversation history**
- User approved a specific plan in turn 8
- Next step must implement exactly what was approved
- A subagent has no memory of turn 8 — it starts fresh

**The task does file edits**
- File edits must be in the main agent's session for VS Code to track them
- A subagent editing files and a main agent editing files can create conflicts

**The task is short (under 5 file reads)**
- Subagent overhead isn't worth it for small tasks
- Just do it in the main agent

**The task needs tight coordination with ongoing changes**
- Test generation after implementation needs to see the newly edited files
- Subagent file state may be stale if files were just changed

### The Rule of Thumb

```
Use a subagent when:
  (estimate of Tool Results tokens if done inline) > 5,000 tokens
  AND
  (output can be expressed as a structured summary)
  AND
  (task does not need conversation history or current edit session)
```

---

## Context Window Impact Over a Full Migration Session

Here's how context is consumed across a 20-turn migration session with and without subagents:

```
Without Subagents (20 turns):                    With Subagents (20 turns):
─────────────────────────────────                ────────────────────────────
Turn  1-3:  Discovery reads     22K              Turn  1-3:  Subagent runs    0.8K
Turn  4-5:  Planning             4K              Turn  4-5:  Planning          4K
Turn  6-12: Implementation       12K             Turn  6-12: Implementation    12K
Turn 13-18: Test gen             10K             Turn 13-18: Test gen          10K
Turn 19-20: Fixes                 5K             Turn 19-20: Fixes              5K
                                                 
Tool Results total:              53K             Tool Results total:            32K
Messages total:                  30K             Messages total:               30K
System Instructions:              4K             System Instructions:           1K
Tool Definitions:                24K             Tool Definitions:             24K
─────────────────────────────────                ────────────────────────────
TOTAL:                          111K             TOTAL:                        87K
Available for output:            49K             Available for output:          73K
                                                 
⚠️ Truncation risk at turn 15+   HIGH            ✅ Comfortable throughout     LOW
```

**Subagents effectively extend your main context window** by offloading the high-volume-read / low-value-retain phases.

---

## Putting It All Together: The Full Phase Sequence

```
User: "migrate src/main/java/com/example/UserResource.java"
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│  MAIN AGENT  (jakarta-migration-agent)                          │
│                                                                  │
│  1. SPAWN SUBAGENT ──────────────────────────────────────┐      │
│     prompt: discovery-prompt.prompt.md                   │      │
│     with: endpoint_path = UserResource.java              │      │
│                                                          ▼      │
│                          ┌──────────────────────────────────┐   │
│                          │  DISCOVERY SUBAGENT              │   │
│                          │  - reads UserResource.java       │   │
│                          │  - reads UserDTO.java            │   │
│                          │  - reads UserService.java        │   │
│                          │  - greps for @Path, @GET, @POST  │   │
│                          │  - finds UserResourceTest.java   │   │
│                          │  - checks pom.xml                │   │
│                          │  Returns: 600-token summary      │   │
│                          └────────────┬─────────────────────┘   │
│                                       │                          │
│  [Tool Result: discovery summary] ◄───┘                          │
│                                                                  │
│  2. INVOKE #migration-planning SKILL                              │
│     Reads SKILL.md (once, ~400 tokens)                           │
│     Uses discovery summary + annotation map from skill           │
│     Outputs: migration plan in chat                              │
│     → User confirms: "yes, proceed"                              │
│                                                                  │
│  3. INVOKE #migration-impl SKILL                                 │
│     References endpoint-pattern.java template via #file:         │
│     Makes edits, runs compile check                              │
│                                                                  │
│  4. INVOKE #test-gen SKILL                                       │
│     References integration-test-template.java via #file:         │
│     Creates/updates test file, runs tests                        │
│                                                                  │
│  5. REPORT complete in chat                                      │
└─────────────────────────────────────────────────────────────────┘
```

---

## Quick Reference

| Decision | Answer |
|----------|--------|
| Discovery/scan needs 10+ file reads | Use subagent |
| Task needs conversation history | Use main agent |
| Task makes file edits | Use main agent |
| Output can be summarized under 1,500 tokens | Use subagent |
| Task tightly coupled to next step | Use main agent |
| Research that produces a report file | Use subagent |
| User-facing interactive planning | Use main agent |

---

*You now have the complete architecture guidance series:*
- *[00-architecture-overview.md](00-architecture-overview.md) — Layered diagram + overlap zones*
- *[01-layer-responsibilities.md](01-layer-responsibilities.md) — What goes where + decision tree*
- *[02-migration-agent-refactor.md](02-migration-agent-refactor.md) — How to break up your 500-line agent*
- *[03-subagent-strategy.md](03-subagent-strategy.md) — This file*
