# Playground

This directory contains hands-on example projects for experimenting with Copilot-driven workflows.

## How to Use

1. Create a subdirectory for your experiment (e.g., `playground/user-service/`).
2. Use the prompt templates in `.copilot/prompt-library/` to drive development.
3. Apply the orchestrator workflow (plan → implement → review) for each feature.

## Example Projects

Add example projects here as you experiment. Each project should include:
- A `README.md` describing the experiment's goal and what was learned.
- Source code generated during the experiment.
- Notes on what worked, what didn't, and how prompts were refined.

## Suggested Starting Points

- **hello-rest**: A minimal REST API with one endpoint (GET /health).
- **user-service**: A CRUD user management service with PostgreSQL.
- **tdd-kata**: A TDD exercise using the TDD cycle prompt template.
