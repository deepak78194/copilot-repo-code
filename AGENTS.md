# AGENTS.md — Copilot Experimentation Lab

This guide distills the essential knowledge for AI coding agents to be productive in this codebase. It covers architecture, workflows, conventions, and integration points unique to this project.

---

## 1. Architecture & Layering

- **Layered Knowledge Model:**
  - **Layer 1:** Always-on global rules (`copilot-instructions.md`), <200 tokens, no procedures.
  - **Layer 2:** Agent definitions (`*.agent.md`), describe *who* the agent is, *what* tools it has, and *which* skills exist. No domain knowledge.
  - **Layer 3:** Skills (`*.skill.md`), encode *how* to perform a phase (e.g., migration, planning). Loaded on-demand, <80 lines each.
  - **Layer 4:** Templates/examples, referenced by skills, loaded only when needed.
- **Orchestration:**
  - Agents operate in a strict workflow: **Planner → Implementer → Reviewer**. Debugger and Security agents are invoked as needed.
  - Subagents (e.g., `api-discovery`) run in isolated contexts for codebase scanning or analysis.

## 2. Developer Workflows

- **Workflow Phases:**
  - **Plan:** Decompose requirements into tasks with acceptance criteria.
  - **Implement:** Write code strictly from the plan.
  - **Review:** Check for correctness, style, and security. Reviewer must approve before completion.
  - **Debug:** Add minimal failing test, then fix.
  - **Security:** Run OWASP-aligned audit before merge.
- **Prompt Library:** Use ready-made prompts in `.copilot/prompt-library/` for common tasks (TDD, REST endpoint, bug fix, refactor, security review).
- **Playground:** `playground/user-service/` is a reference Spring Boot 3 + PostgreSQL CRUD service built using these workflows.

## 3. Project-Specific Conventions

- **Skill Loading:**
  - Skills are loaded per phase and user choice (see `docs/api-migration-agent/skills/migration/SKILL.md`).
  - Example: For stored procedure migration, load `sp-to-jdbc.skill.md`, `sp-to-api-client.skill.md`, or `dual-datasource.skill.md` based on user selection.
- **Spring Boot Patterns:**
  - Follow conventions in `spring-boot-patterns.skill.md` for structure, dependency injection, and transactional boundaries.
  - Use constructor injection, DTOs, structured errors, and `@Transactional` service layers.
- **Monorepo Context:**
  - Skills and agents are designed for both root and microservice workspaces.
  - Target stack: Java 17+, Spring Boot 3.x, PostgreSQL (primary), Oracle (secondary), Gradle build.

## 4. Integration & Communication

- **Subagents:**
  - Use subagents for codebase discovery and analysis to avoid bloating the main context window.
  - Example: `api-discovery.agent.md` analyzes API endpoints, handler logic, and dependencies, returning compact summaries.
- **Explicit Escalation:**
  - If Reviewer returns `CHANGES REQUIRED` twice, Orchestrator escalates to the user.
- **Security:**
  - Security agent runs before every merge; findings are addressed early.

## 5. Key Files & Directories

- `README.md`: Quick start, agent/skill directory, workflow overview.
- `docs/agent-design/`: Architecture, layering, and responsibility boundaries.
- `docs/api-migration-agent/skills/migration/`: Migration skills and loading rules.
- `playground/user-service/`: End-to-end Spring Boot reference implementation.
- `.copilot/prompt-library/`: Prompt templates for common workflows.

---

**For more, see:** [https://agents.md/](https://agents.md/) and referenced skill/agent files for concrete examples.

