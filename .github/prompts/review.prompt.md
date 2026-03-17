---
mode: agent
agent: lead
description: "Review code only — quality audit + OWASP security check. Invokes: reviewer only."
---

# /review — Code Review + Security Audit (1 agent: reviewer)

> **Routing: lead → reviewer**
> Use this when you want a structured code review on existing files without making any changes.

## What happens
1. `@lead` routes directly to `reviewer`
2. `reviewer` reads the target files and runs the full OWASP Top 10 checklist plus quality checks
3. Returns a severity-tiered issue table (CRITICAL → HIGH → MEDIUM → LOW)

## Prompt
Review the following code for correctness, security, and maintainability:

**Target:** $target

Apply:
- OWASP Top 10 security checks (A01–A10)
- Null/exception safety, resource leaks, thread safety
- Input validation at public API boundaries
- Hardcoded secrets or config values
- Adherence to project conventions

Do NOT modify any files. Report issues only with severity and fix hints.
