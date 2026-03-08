# Copilot Agent Architecture — The Big Picture
> How to layer your agent, skills, instructions, prompts, and subagents so each does one job well and nothing bloats the context window.

---

## The Core Problem You're Solving

You have a complex multi-phase task (Jakarta REST → Micronaut migration). You built a 500-line custom agent that tries to do everything: learn the codebase, research patterns, plan, implement, and test. It works — but it's expensive, brittle, and hard to evolve.

**The root cause:** You put *knowledge* and *procedure* and *orchestration* all in one place. They have different jobs, different update cycles, and different context costs.

---

## The Layered Architecture — Overlapping Responsibilities

```
╔══════════════════════════════════════════════════════════════════════════╗
║                        CONTEXT WINDOW (160K)                            ║
║                                                                          ║
║  ┌──────────────────────────────────────────────────────────────────┐   ║
║  │ LAYER 1 — ALWAYS ON  (copilot-instructions.md)                   │   ║
║  │ Global repo conventions. Costs tokens on EVERY request.          │   ║
║  │ < 200 tokens. Stack rules only. No procedures.                   │   ║
║  │                                                                   │   ║
║  │  ┌────────────────────────────────────────────────────────────┐  │   ║
║  │  │ LAYER 2 — AGENT  (.agent.md)                               │  │   ║
║  │  │ WHO the agent is + WHAT tools it has + WHICH skills exist. │  │   ║
║  │  │ Orchestration only. < 100 lines. No domain knowledge.      │  │   ║
║  │  │                                                             │  │   ║
║  │  │  ┌──────────────────────────────────────────────────────┐  │  │   ║
║  │  │  │ LAYER 3 — SKILLS  (SKILL.md per phase)               │  │  │   ║
║  │  │  │ HOW to do one phase. Loaded on-demand when invoked.  │  │  │   ║
║  │  │  │ Procedure steps + pattern refs. < 80 lines each.     │  │  │   ║
║  │  │  │                                                       │  │  │   ║
║  │  │  │  ┌────────────────────────────────────────────────┐  │  │  │   ║
║  │  │  │  │ LAYER 4 — TEMPLATE FILES  (referenced by skill)│  │  │  │   ║
║  │  │  │  │ WHAT the pattern looks like. Code examples,    │  │  │  │   ║
║  │  │  │  │ before/after pairs, test templates.            │  │  │  │   ║
║  │  │  │  │ Loaded into Files bucket only when needed.     │  │  │  │   ║
║  │  │  │  └────────────────────────────────────────────────┘  │  │  │   ║
║  │  │  └──────────────────────────────────────────────────────┘  │  │   ║
║  │  │                                                             │  │   ║
║  │  │  ┌──────────────────────────────────────────────────────┐  │  │   ║
║  │  │  │ LAYER 3b — INSTRUCTIONS  (*.instructions.md)         │  │  │   ║
║  │  │  │ RULES for a specific file area. Auto-loaded when     │  │  │   ║
║  │  │  │ working files match applyTo pattern.                 │  │  │   ║
║  │  │  └──────────────────────────────────────────────────────┘  │  │   ║
║  │  └────────────────────────────────────────────────────────────┘  │   ║
║  └──────────────────────────────────────────────────────────────────┘   ║
║                                                                          ║
║  ┌──────────────────────────────────────────────────────────────────┐   ║
║  │ SUBAGENTS — Separate context windows entirely                    │   ║
║  │ Heavy research, large file reads, codebase-wide scans.           │   ║
║  │ Results returned as a compact summary → Tool Results bucket.     │   ║
║  └──────────────────────────────────────────────────────────────────┘   ║
╚══════════════════════════════════════════════════════════════════════════╝
```

---

## The Overlap Zones — Where Responsibilities Touch

```
                    ┌─────────────────────────────────────────────┐
                    │                                             │
          ┌─────────┴──────────┐         ┌──────────────────┐   │
          │                    │         │                  │   │
          │   .agent.md        │         │  SKILL.md files  │   │
          │                    │         │                  │   │
          │  • WHO I am        │         │  • HOW to do     │   │
          │  • WHAT tools      │◄──────►│    each phase    │   │
          │  • WHICH skills    │  [A]   │  • Step sequence │   │
          │    exist           │         │  • Tool guidance │   │
          │  • Phase routing   │         │  • Pattern refs  │   │
          └──────┬─────────────┘         └────────┬─────────┘   │
                 │                                │              │
             [B] │                            [C] │              │
                 │                                │              │
          ┌──────▼─────────────────────────────────┐            │
          │                                         │            │
          │   .instructions.md files                │            │
          │                                         │            │
          │  • RULES per file type/area             │            │
          │  • Code style constraints               │            │
          │  • Framework-specific conventions       │            │
          │  • Auto-loaded, context-scoped          │            │
          └─────────────────────────────────────────┘            │
                                                                 │
    ┌───────────────────────────────────────────────────────┐   │
    │  Template / Example Files                              │   │
    │  (referenced via #file: in skills)                     │◄──┘
    │                                                        │
    │  • Actual code examples                               │
    │  • Before/after migration patterns                    │
    │  • Test case templates                                │
    │  • Loaded into Files bucket only when referenced      │
    └───────────────────────────────────────────────────────┘
```

**Overlap Zone [A] — Agent ↔ Skill:**  
The agent knows *which* skill to invoke for each phase; the skill knows *how* to execute that phase. Neither duplicates the other.

**Overlap Zone [B] — Agent ↔ Instructions:**  
The agent respects instructions passively (they're auto-loaded). No need to repeat instruction content inside the agent — instructions carry domain rules so the agent doesn't have to.

**Overlap Zone [C] — Skill ↔ Instructions:**  
Skills say *what steps to take*. Instructions say *what constraints to follow while taking those steps*. A migration skill says "convert the endpoint handler" — an instruction says "all handlers must use async/await and return `HttpResponse<T>`."

---

## The Phase-to-Layer Mapping

For your Jakarta → Micronaut migration, this is how phases map to layers:

```
┌──────────────────┬─────────────────┬─────────────────┬──────────────────┐
│ Phase            │ Lives In        │ Context Cost    │ Separate Window? │
├──────────────────┼─────────────────┼─────────────────┼──────────────────┤
│ Listen/Learn     │ SUBAGENT        │ 0 (own window)  │ YES              │
│ (codebase scan)  │                 │                 │                  │
├──────────────────┼─────────────────┼─────────────────┼──────────────────┤
│ Research         │ SUBAGENT        │ 0 (own window)  │ YES              │
│ (pattern find)   │                 │ summary returned│                  │
├──────────────────┼─────────────────┼─────────────────┼──────────────────┤
│ Report/Plan      │ research-skill  │ Low (on-demand) │ No               │
│ (decision docs)  │ .md             │                 │                  │
├──────────────────┼─────────────────┼─────────────────┼──────────────────┤
│ Implementation   │ migration-skill │ Low (on-demand) │ No               │
│ (code conversion)│ + template files│                 │                  │
├──────────────────┼─────────────────┼─────────────────┼──────────────────┤
│ Test generation  │ test-skill      │ Low (on-demand) │ No               │
│ (test patterns)  │ + test templates│                 │                  │
├──────────────────┼─────────────────┼─────────────────┼──────────────────┤
│ Orchestration    │ .agent.md       │ Fixed/always    │ N/A — main agent │
│ (phase routing)  │                 │                 │                  │
├──────────────────┼─────────────────┼─────────────────┼──────────────────┤
│ Code rules       │ .instructions   │ Low (scoped)    │ No               │
│ (per file type)  │ .md per area    │                 │                  │
└──────────────────┴─────────────────┴─────────────────┴──────────────────┘
```

---

## Your Current vs. Target Architecture

```
CURRENT (500-line agent doing everything):
┌──────────────────────────────────────────────────┐
│  migration-agent.agent.md (500 lines)            │
│                                                  │
│  • WHO I am           (20 lines)  ✅ correct here│
│  • WHAT tools I have  (50 lines)  ✅ correct here│
│  • Listen phase steps (80 lines)  ❌ → subagent  │
│  • Research phase     (100 lines) ❌ → subagent  │
│  • Planning rules     (80 lines)  ❌ → skill     │
│  • Code patterns      (100 lines) ❌ → templates │
│  • Test patterns      (90 lines)  ❌ → skill     │
│  • Output rules       (80 lines)  ❌ → instruc.  │
└──────────────────────────────────────────────────┘
Context cost: all 500 lines loaded on EVERY request


TARGET (thin agent + distributed layers):
┌──────────────────────────────────────────────────┐
│  migration-agent.agent.md (~80 lines)            │
│  • WHO + tools + skill index + phase routing     │
└────────┬─────────────────────────────────────────┘
         │ invokes
    ┌────┴────────────────────────────────────────────────────┐
    │                                                         │
    ▼                         ▼                     ▼        ▼
┌──────────┐          ┌──────────────┐     ┌──────────────┐ ┌──────────┐
│SUBAGENT  │          │SUBAGENT      │     │migration-    │ │test-     │
│(research)│          │(codebase scan│     │skill.md      │ │skill.md  │
│own window│          │ own window)  │     │+ templates/  │ │+ test-   │
│          │          │              │     │  patterns    │ │  templa. │
└──────────┘          └──────────────┘     └──────────────┘ └──────────┘
    Returns compact summary                On-demand only    On-demand
    into Tool Results bucket               when implementing  when testing
```

**Result:** The agent file drops from 500 → ~80 lines. Per-request context cost for System Instructions drops to ~8% of what it was.

---

## The Single Most Important Rule

> **The agent orchestrates. Skills know how. Instructions constrain. Templates show. Subagents research.**

If you find yourself writing *how to do X* inside the agent — move it to a skill.  
If you find yourself writing *code examples* inside the skill — move them to a template file.  
If you find yourself writing *file/folder scanning* inside the agent — move it to a subagent.

---

## File Structure for the Migration Agent

```
.github/
├── copilot-instructions.md              ← Global rules (< 200 tokens)
│
├── agents/
│   └── migration-agent.agent.md        ← Thin orchestrator (~80 lines)
│
├── skills/
│   ├── research/
│   │   ├── SKILL.md                    ← Research phase procedure
│   │   └── research-prompt.md          ← Subagent prompt template
│   ├── migration/
│   │   ├── SKILL.md                    ← Implementation procedure
│   │   ├── endpoint-pattern.ts         ← Jakarta → Micronaut template
│   │   └── dto-pattern.ts              ← DTO conversion template
│   └── testing/
│       ├── SKILL.md                    ← Test generation procedure
│       ├── unit-test-template.ts       ← Unit test pattern
│       └── integration-test-template.ts← Integration test pattern
│
├── instructions/
│   ├── micronaut-controllers.instructions.md ← applyTo: src/controllers/**
│   ├── micronaut-services.instructions.md    ← applyTo: src/services/**
│   └── test-conventions.instructions.md      ← applyTo: **/*.spec.ts
│
└── prompts/
    ├── start-migration.prompt.md       ← Entry point for the whole workflow
    └── migrate-single-endpoint.prompt.md ← Focused single-file migration
```

---

*Continue to [01-layer-responsibilities.md](01-layer-responsibilities.md) for what exactly to put in each file.*  
*Continue to [02-migration-agent-refactor.md](02-migration-agent-refactor.md) for how to refactor your 500-line agent.*  
*Continue to [03-subagent-strategy.md](03-subagent-strategy.md) for when and how to use subagents.*
