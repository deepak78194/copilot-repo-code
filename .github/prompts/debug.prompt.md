---
mode: agent
agent: lead
description: "Fix a specific bug or targeted issue. Invokes: coder only."
---

# /debug — Targeted Bug Fix (1 agent: coder)

> **Routing: lead → coder**
> Use this when you have a specific bug, error, or broken behaviour. No planning overhead — goes straight to implementation.

## What happens
1. `@lead` routes directly to `coder` (no planner needed — scope is already defined by the bug)
2. `coder` reads the affected file(s), diagnoses the issue, and fixes it
3. Runs the build/lint to verify the fix compiles
4. Reports exactly what changed

## Prompt
Fix the following issue:

**Issue:** $issue
**File (if known):** $file
**Error message or symptom:** $symptom

Fix only the reported issue. Do not refactor surrounding code. Match the existing code style. Verify the fix compiles/lints before reporting done.
