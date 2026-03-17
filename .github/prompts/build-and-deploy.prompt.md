---
mode: agent
agent: lead
description: "Plan, implement, test, then containerize and add CI/CD. Invokes: planner → coder → tester → devops (4 agents)."
---

# /build-and-deploy — Implement + Test + Infrastructure (4 agents: planner → coder → tester → devops)

> **Routing: lead → planner → coder → tester → devops**
> Use this when you want to go from feature description to a deployable service with a working CI pipeline and containerization.

## What happens
1. `planner` decomposes the feature into tasks
2. `coder` implements all tasks and verifies the build
3. `tester` writes and runs tests on the new code
4. `devops` creates the Dockerfile, docker-compose.yml, CI pipeline, and .env.example for the service

## Prompt
Build and deploy-ready the following:

**Feature / service:** $feature
**Target environment:** $environment (local dev / staging / production)
**Stack:** $stack

Steps:
1. Planner: decompose into ordered tasks with file targets
2. Coder: implement all tasks, verify build
3. Tester: write + run tests (happy path + error cases)
4. DevOps: create Dockerfile (multi-stage, non-root), docker-compose for local dev, CI pipeline (lint → test → build), .env.example

Stop after infrastructure is created. Do NOT run a code review.
