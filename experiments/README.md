# Experiments

This directory is for evaluating agent behavior, measuring output consistency, and testing determinism across workflow runs.

## Purpose

Use this directory to:
- Run the same prompt multiple times and compare outputs.
- Measure how well agents follow constraints and conventions.
- Evaluate prompt variations to find the most effective formulations.
- Document regressions when prompts or agent definitions are changed.

## Experiment Structure

Each experiment lives in its own subdirectory:

```
experiments/
  {experiment-name}/
    README.md        # Goal, setup, methodology
    prompt.md        # Exact prompt used
    run-1/           # Output from run 1
    run-2/           # Output from run 2
    analysis.md      # Comparison, findings, conclusions
```

## Evaluation Criteria

When evaluating agent outputs, consider:

| Criterion | Description |
|-----------|-------------|
| **Correctness** | Does the output satisfy the acceptance criteria? |
| **Consistency** | Are outputs similar across multiple runs? |
| **Constraint adherence** | Did the agent respect all stated constraints? |
| **Minimal output** | Did the agent avoid adding unrequested features? |
| **Convention compliance** | Does the code follow repository conventions? |

## Getting Started

1. Pick a prompt template from `.copilot/prompt-library/`.
2. Define a clear evaluation goal in `experiments/{name}/README.md`.
3. Run the prompt 2–3 times, saving outputs to separate `run-N/` directories.
4. Write an `analysis.md` comparing the results.
5. Use findings to improve agent definitions or prompt templates.
