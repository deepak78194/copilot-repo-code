# Multi-Agent Architecture Overview

> A Lead orchestrator agent that routes tasks to specialized sub-agents — activating only the minimum set needed for each task.

---

## System Map

```
                         ┌─────────────────────────────────────────────────┐
                         │           @lead  (Lead Orchestrator)            │
                         │                                                 │
                         │  Model: Claude Sonnet 4.6  │  Context: 160K    │
                         │  Tools: agent, read, search, todo, askQuestions │
                         │                                                 │
                         │  ┌─────────────────────────────────────────┐   │
                         │  │         <dispatch_matrix>               │   │
                         │  │  Reads task → selects minimum agents    │   │
                         │  └──────────────────┬──────────────────────┘   │
                         └─────────────────────┼───────────────────────────┘
                                               │
             ┌─────────────┬──────────────┬────┴────────┬──────────────┬──────────────┐
             │             │              │             │              │              │
        ┌────▼────┐  ┌─────▼────┐  ┌─────▼────┐  ┌────▼─────┐  ┌────▼────┐  ┌──────▼──────┐
        │ planner │  │ designer │  │  coder   │  │  tester  │  │reviewer │  │   devops    │
        │         │  │          │  │          │  │          │  │         │  │             │
        │ Sonnet  │  │ Opus 4.5 │  │GPT-5.2-  │  │GPT-5.1-  │  │ Sonnet  │  │ Haiku 4.5   │
        │  4.6    │  │  (3x)    │  │Codex(1x) │  │Codex(1x) │  │  4.6    │  │  (0.33x)    │
        │  1x     │  │          │  │  400K    │  │  256K    │  │  1x     │  │             │
        │         │  │          │  │          │  │          │  │         │  │             │
        │ read    │  │ read     │  │ read     │  │ read     │  │ read    │  │ read        │
        │ search  │  │ search   │  │ edit     │  │ edit     │  │ search  │  │ edit        │
        │ todo    │  │ web      │  │ execute  │  │ execute  │  │ todo    │  │ execute     │
        │         │  │          │  │ search   │  │ search   │  │         │  │ search      │
        │ NO edit │  │ NO edit  │  │ todo     │  │ todo     │  │ NO edit │  │ todo        │
        └─────────┘  └──────────┘  └──────────┘  └──────────┘  └─────────┘  └─────────────┘
```

---

## Files Created

```
.github/
  agents/
    lead.agent.md        ← Entry point. User-invocable. Routes to sub-agents.
    planner.agent.md     ← Task decomposition. Read-only.
    designer.agent.md    ← API/schema/architecture design. Read-only.
    coder.agent.md       ← Implementation. Writes and verifies code.
    tester.agent.md      ← Test writing and execution. No prod code changes.
    reviewer.agent.md    ← Code review + OWASP security audit. Read-only.
    devops.agent.md      ← Dockerfile, CI/CD, infrastructure. No src changes.
```

---

## The Core Design Principle: Minimum Agent Selection

The Lead agent uses a **dispatch matrix** to select only the agents needed. It does NOT call all agents for every task.

```
User: "write tests for the AuthService"
Lead selects: [tester]               ← 1 agent only

User: "build a user registration feature"
Lead selects: [planner → coder]      ← 2 agents

User: "implement + test"
Lead selects: [planner → coder → tester]   ← 3 agents

User: "full feature end-to-end"
Lead selects: [planner → designer → coder → tester → reviewer]  ← 5 agents
```

---

## Routing Decision Tree

```
User request arrives at @lead
         │
         ▼
  ┌─────────────────────────────────────────┐
  │  Is the task scope clear?               │
  │  (target, language, expected output)    │
  └──────────────────┬──────────────────────┘
         │                    │
        YES                   NO
         │                    │
         │            Ask via vscode/askQuestions
         │            then continue
         ▼
  ┌─────────────────────────────────────────┐
  │  Match task signal to dispatch_matrix   │
  └──────────────────┬──────────────────────┘
         │
         ▼
  ┌──────────────────────────────────────────────────────────┐
  │  Single-signal tasks (one agent):                        │
  │   "plan", "review", "write tests", "deploy", "debug"     │
  │   → Invoke exactly ONE sub-agent                         │
  │                                                          │
  │  Multi-signal tasks (agent chain):                       │
  │   "build", "implement + test", "full feature"            │
  │   → Invoke agents sequentially, passing compact context  │
  └──────────────────────────────────────────────────────────┘
```

---

## Context Handoff Architecture

Each sub-agent returns a **compact report** (≤600 tokens). Lead extracts only the relevant fragment before passing to the next agent.

```
┌─────────┐    Full plan table     ┌────────┐    Files + key funcs   ┌────────┐
│ planner │ ─────────────────────► │ coder  │ ──────────────────────► │ tester │
│ ~300tok │                        │~500tok │                         │~400tok │
└─────────┘                        └────────┘                         └────────┘
                                                                           │
                                                           Test results + bugs
                                                                           │
                                                                           ▼
                                                                    ┌──────────┐
                                                                    │ reviewer │
                                                                    │  (opt.)  │
                                                                    └──────────┘
```

Lead never passes full file contents between agents — only structured summaries.

---

## Model Selection Rationale

| Agent | Model | Cost Multiplier | Why |
|-------|-------|----------------|-----|
| lead | Claude Sonnet 4.6 | 1x | Routing + orchestration; reasoning quality matters |
| planner | Claude Sonnet 4.6 | 1x | Task decomposition needs structured reasoning |
| designer | Claude Opus 4.5 | 3x | Architecture decisions are high-stakes; worth the best model |
| coder | GPT-5.2-Codex | 1x | Best code generation; 400K context fits large codebases |
| tester | GPT-5.1-Codex | 1x | Strong test generation; 256K context sufficient |
| reviewer | Claude Sonnet 4.6 | 1x | Analysis and reasoning at balanced cost |
| devops | Claude Haiku 4.5 | 0.33x | Infrastructure is well-defined; fast + cheap is ideal |

**Cost optimization rule:** Designer (Opus) is only invoked for tasks that explicitly require architecture/design decisions. For pure "build it" tasks, it is skipped entirely.

---

## Security Design

- `reviewer` always runs the OWASP Top 10 checklist (A01–A10) on any code it reviews
- `coder` never introduces new dependencies without matching existing project patterns
- `devops` always pins base image versions and never hardcodes secrets
- `tester` never fabricates test results — runs real commands via `execute` tool
- All sub-agents are `user-invocable: false` — they can only be invoked by `lead`

---

## How to Use

Open GitHub Copilot Chat in VS Code and invoke the Lead agent:

```
@lead build a user registration feature with email + password
@lead review the code in src/auth/
@lead write tests for UserService
@lead deploy the order-service to Kubernetes
@lead design the payment API schema
@lead plan migration of the analytics module
```

Lead will announce which agents it will call and then execute the chain.
