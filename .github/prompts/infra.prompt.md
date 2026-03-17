---
mode: agent
agent: lead
description: "Create infrastructure only — Dockerfile, CI/CD, Kubernetes. Invokes: devops only."
---

# /infra — Infrastructure Setup (1 agent: devops)

> **Routing: lead → devops**
> Use this when you need infrastructure files for an already-built service: Dockerfiles, CI/CD pipelines, Kubernetes manifests, or IaC configs.

## What happens
1. `@lead` routes directly to `devops` (no coding needed — the service already exists)
2. `devops` reads the project to understand the language, port, and dependencies
3. Creates: Dockerfile (multi-stage), docker-compose.yml, CI pipeline, .env.example
4. Validates what it can (docker build, yamllint) before reporting

## Prompt
Set up infrastructure for:

**Service:** $service
**Target environment:** $environment (local dev / staging / production / Kubernetes)
**Language & framework:** $stack

Create:
- Dockerfile with multi-stage build (non-root user, pinned base image tag)
- docker-compose.yml for local development
- CI/CD pipeline config (lint → test → build steps)
- .env.example documenting all required environment variables

Do NOT modify any production source code (src/, lib/, app/).
