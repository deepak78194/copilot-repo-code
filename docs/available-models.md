# Available Models Reference

> Last updated: March 11, 2026
> Use this file to select the appropriate model for agent tasks based on context size, capabilities, and cost multiplier.

## Model Catalog

| Name | Context Size | Capabilities | Request Multiplier |
|------|-------------|--------------|-------------------|
| Claude Haiku 4.5 | 160K | Tools, Vision | 0.33x |
| Claude Opus 4.5 | 160K | Tools, Vision | 3x |
| Claude Opus 4.6 | 192K | Tools, Vision | 3x |
| Claude Sonnet 4 | 144K | Tools, Vision | 1x |
| Claude Sonnet 4.5 | 160K | Tools, Vision | 1x |
| Claude Sonnet 4.6 | 160K | Tools, Vision | 1x |
| Gemini 2.5 Pro | 173K | Tools, Vision | 1x |
| Gemini 3 Flash (Preview) | 173K | Tools, Vision | 0.33x |
| Gemini 3 Pro (Preview) ⚠️ | 173K | Tools, Vision | 1x |
| Gemini 3.1 Pro (Preview) | 173K | Tools, Vision | 1x |
| GPT-4.1 | 128K | Tools, Vision | 0x |
| GPT-4o | 68K | Tools, Vision | 0x |
| GPT-5 mini | 192K | Tools, Vision | 0x |
| GPT-5.1 | 192K | Tools, Vision | 1x |
| GPT-5.1-Codex ⚠️ | 256K | Tools, Vision | 1x |
| GPT-5.1-Codex-Max ⚠️ | 256K | Tools, Vision | 1x |
| GPT-5.1-Codex-Mini (Preview) ⚠️ | 256K | Tools, Vision | 0.33x |
| GPT-5.2 | 192K | Tools, Vision | 1x |
| GPT-5.2-Codex | 400K | Tools, Vision | 1x |
| GPT-5.3-Codex | 400K | Tools, Vision | 1x |
| GPT-5.4 | 400K | Tools, Vision | 1x |
| Grok Code Fast 1 | 173K | Tools | 0.25x |
| Raptor mini (Preview) | 264K | Tools, Vision | 0x |

> ⚠️ = model has a known warning/limitation in the current environment.

## Selection Guide

| Goal | Recommended Model |
|------|-------------------|
| Lowest cost, fast tasks | Claude Haiku 4.5 (0.33x), Gemini 3 Flash (0.33x), GPT-5.1-Codex-Mini (0.33x) |
| Largest context window | GPT-5.2-Codex, GPT-5.3-Codex, GPT-5.4 (400K) |
| Free / zero-cost | GPT-4.1, GPT-4o, GPT-5 mini, Raptor mini (0x) |
| High reasoning, vision tasks | Claude Opus 4.6 (192K, 3x), Claude Opus 4.5 (160K, 3x) |
| Balanced cost + context | Claude Sonnet 4.6, GPT-5.1, GPT-5.2 (1x) |
| Code-focused large context | GPT-5.1-Codex, GPT-5.1-Codex-Max, GPT-5.2-Codex (256K–400K) |
| No vision needed, fast | Grok Code Fast 1 (0.25x, Tools only) |
