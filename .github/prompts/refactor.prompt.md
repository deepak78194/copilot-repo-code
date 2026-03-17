---
mode: agent
agent: lead
description: "Review existing code, then refactor it. Invokes: reviewer → coder (2 agents)."
---

# /refactor — Review Then Refactor (2 agents: reviewer → coder)

> **Routing: lead → reviewer → coder**
> Use this to safely refactor existing code. The reviewer first identifies all issues, then the coder addresses them. No speculative changes — coder is scoped to exactly what reviewer flagged.

## What happens
1. `@lead` invokes `reviewer` on the target files — full OWASP + quality audit
2. `reviewer` output (issue list with severity) is passed to `coder`
3. `coder` addresses CRITICAL and HIGH issues only (unless scope is specified differently below)
4. `coder` verifies the build after changes

## Prompt
Refactor the following code:

**Target:** $target
**Scope of fixes:** $scope (default: CRITICAL + HIGH issues only)

Steps:
1. Reviewer: run full audit — OWASP Top 10, null safety, resource leaks, conventions
2. Coder: fix all issues at the requested severity level — do NOT change behaviour, only address the flagged issues

Do NOT add new features. Do NOT write tests. Match existing code style throughout.
