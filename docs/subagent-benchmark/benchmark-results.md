# Sub-Agent Model Benchmark Results

**Date:** 2026-03-11  
**Orchestrator Model:** Claude Opus 4.6  
**Scenarios:** 4 | **Models:** 9 | **Total Runs:** 36  
**Timing:** Not measurable (platform does not expose per-call latency)

---

## Section A: Raw Scores Table

| Model | Tier | Cost | S1 /62.5 | S2 /62.5 | S3 /62.5 | S4 /62.5 | Weighted Total /250 |
|-------|------|------|----------|----------|----------|----------|---------------------|
| GPT-4.1 | Free | 0x | 49.5 | 40.5 | 50.0 | 53.0 | **193.0** |
| GPT-5 mini | Free | 0x | 46.0 | 43.5 | 45.5 | 53.5 | **188.5** |
| Raptor mini | Free | 0x | 42.0 | 31.0 | 43.5 | 38.5 | **155.0** |
| Claude Haiku 4.5 | Budget | 0.33x | 45.0 | 44.0 | 39.0 | 33.0 | **161.0** |
| Gemini 3 Flash | Budget | 0.33x | 42.0 | 41.0 | 44.0 | 53.0 | **180.0** |
| GPT-5.1-Codex-Mini | Budget | 0.33x | 49.5 | 40.0 | 12.5 | 12.5 | **114.5** |
| Claude Sonnet 4.6 | Reference | 1x | 49.5 | 46.5 | 57.5 | 58.5 | **212.0** |
| GPT-5.2 | Reference | 1x | 43.0 | 46.5 | 57.5 | 56.5 | **203.5** |
| GPT-5.4 | Reference | 1x | 49.5 | 46.5 | 50.5 | 57.0 | **203.5** |

---

## Section B: Dimension Breakdown Per Model

### Model: GPT-4.1 (0x, Free)

| Dimension | S1 | S2 | S3 | S4 | Avg |
|-----------|:--:|:--:|:--:|:--:|:---:|
| D1 Code Correctness (2x) | 5 | 4 | 4 | 4 | 4.25 |
| D2 Architecture (2x) | 3 | 3 | 4 | 5 | 3.75 |
| D3 Completeness (1.5x) | 5 | 3 | 5 | 4 | 4.25 |
| D4 Instruction Following (1.5x) | 5 | 3 | 4 | 4 | 4.00 |
| D5 Safety & Security (1.5x) | 3 | 3 | 4 | 4 | 3.50 |
| D6 Convention Compliance (1x) | 3 | 3 | 4 | 5 | 3.75 |
| D7 PostgreSQL Correctness (1x) | 3 | 3 | 4 | 4 | 3.50 |
| D8 Reasoning Quality (1x) | 3 | 2 | 3 | 4 | 3.00 |
| D9 Output Structure (0.5x) | 5 | 5 | 4 | 4 | 4.50 |
| D10 Conciseness (0.5x) | 5 | 5 | 3 | 4 | 4.25 |

### Model: GPT-5 mini (0x, Free)

| Dimension | S1 | S2 | S3 | S4 | Avg |
|-----------|:--:|:--:|:--:|:--:|:---:|
| D1 Code Correctness (2x) | 4 | 3 | 3 | 4 | 3.50 |
| D2 Architecture (2x) | 3 | 3 | 3 | 5 | 3.50 |
| D3 Completeness (1.5x) | 5 | 5 | 4 | 5 | 4.75 |
| D4 Instruction Following (1.5x) | 4 | 4 | 3 | 4 | 3.75 |
| D5 Safety & Security (1.5x) | 3 | 3 | 5 | 4 | 3.75 |
| D6 Convention Compliance (1x) | 3 | 3 | 3 | 4 | 3.25 |
| D7 PostgreSQL Correctness (1x) | 3 | 3 | 5 | 4 | 3.75 |
| D8 Reasoning Quality (1x) | 3 | 3 | 3 | 4 | 3.25 |
| D9 Output Structure (0.5x) | 5 | 5 | 4 | 4 | 4.50 |
| D10 Conciseness (0.5x) | 5 | 4 | 5 | 4 | 4.50 |

### Model: Raptor mini (0x, Free)

| Dimension | S1 | S2 | S3 | S4 | Avg |
|-----------|:--:|:--:|:--:|:--:|:---:|
| D1 Code Correctness (2x) | 3 | 2 | 3 | 2 | 2.50 |
| D2 Architecture (2x) | 3 | 3 | 3 | 4 | 3.25 |
| D3 Completeness (1.5x) | 5 | 2 | 4 | 3 | 3.50 |
| D4 Instruction Following (1.5x) | 3 | 2 | 3 | 3 | 2.75 |
| D5 Safety & Security (1.5x) | 3 | 3 | 5 | 3 | 3.50 |
| D6 Convention Compliance (1x) | 3 | 2 | 3 | 3 | 2.75 |
| D7 PostgreSQL Correctness (1x) | 3 | 3 | 5 | 4 | 3.75 |
| D8 Reasoning Quality (1x) | 3 | 1 | 3 | 3 | 2.50 |
| D9 Output Structure (0.5x) | 4 | 4 | 3 | 3 | 3.50 |
| D10 Conciseness (0.5x) | 5 | 5 | 2 | 3 | 3.75 |

### Model: Claude Haiku 4.5 (0.33x, Budget)

| Dimension | S1 | S2 | S3 | S4 | Avg |
|-----------|:--:|:--:|:--:|:--:|:---:|
| D1 Code Correctness (2x) | 5 | 4 | 2 | 2 | 3.25 |
| D2 Architecture (2x) | 3 | 3 | 3 | 3 | 3.00 |
| D3 Completeness (1.5x) | 5 | 5 | 4 | 4 | 4.50 |
| D4 Instruction Following (1.5x) | 3 | 3 | 3 | 2 | 2.75 |
| D5 Safety & Security (1.5x) | 3 | 3 | 3 | 1 | 2.50 |
| D6 Convention Compliance (1x) | 3 | 3 | 3 | 3 | 3.00 |
| D7 PostgreSQL Correctness (1x) | 3 | 3 | 4 | 4 | 3.50 |
| D8 Reasoning Quality (1x) | 3 | 4 | 3 | 3 | 3.25 |
| D9 Output Structure (0.5x) | 3 | 3 | 4 | 3 | 3.25 |
| D10 Conciseness (0.5x) | 4 | 4 | 4 | 2 | 3.50 |

### Model: Gemini 3 Flash (0.33x, Budget)

| Dimension | S1 | S2 | S3 | S4 | Avg |
|-----------|:--:|:--:|:--:|:--:|:---:|
| D1 Code Correctness (2x) | 3 | 3 | 3 | 4 | 3.25 |
| D2 Architecture (2x) | 3 | 3 | 3 | 5 | 3.50 |
| D3 Completeness (1.5x) | 5 | 4 | 4 | 4 | 4.25 |
| D4 Instruction Following (1.5x) | 3 | 3 | 4 | 4 | 3.50 |
| D5 Safety & Security (1.5x) | 3 | 3 | 3 | 4 | 3.25 |
| D6 Convention Compliance (1x) | 3 | 3 | 4 | 5 | 3.75 |
| D7 PostgreSQL Correctness (1x) | 3 | 3 | 5 | 4 | 3.75 |
| D8 Reasoning Quality (1x) | 3 | 3 | 3 | 4 | 3.25 |
| D9 Output Structure (0.5x) | 4 | 5 | 4 | 4 | 4.25 |
| D10 Conciseness (0.5x) | 5 | 5 | 3 | 4 | 4.25 |

### Model: GPT-5.1-Codex-Mini (0.33x, Budget)

| Dimension | S1 | S2 | S3 | S4 | Avg |
|-----------|:--:|:--:|:--:|:--:|:---:|
| D1 Code Correctness (2x) | 5 | 3 | 1 | 1 | 2.50 |
| D2 Architecture (2x) | 3 | 3 | 1 | 1 | 2.00 |
| D3 Completeness (1.5x) | 5 | 4 | 1 | 1 | 2.75 |
| D4 Instruction Following (1.5x) | 5 | 3 | 1 | 1 | 2.50 |
| D5 Safety & Security (1.5x) | 3 | 3 | 1 | 1 | 2.00 |
| D6 Convention Compliance (1x) | 3 | 3 | 1 | 1 | 2.00 |
| D7 PostgreSQL Correctness (1x) | 3 | 3 | 1 | 1 | 2.00 |
| D8 Reasoning Quality (1x) | 3 | 3 | 1 | 1 | 2.00 |
| D9 Output Structure (0.5x) | 5 | 4 | 1 | 1 | 2.75 |
| D10 Conciseness (0.5x) | 5 | 4 | 1 | 1 | 2.75 |

### Model: Claude Sonnet 4.6 (1x, Reference)

| Dimension | S1 | S2 | S3 | S4 | Avg |
|-----------|:--:|:--:|:--:|:--:|:---:|
| D1 Code Correctness (2x) | 5 | 4 | 5 | 5 | 4.75 |
| D2 Architecture (2x) | 3 | 3 | 5 | 5 | 4.00 |
| D3 Completeness (1.5x) | 5 | 5 | 5 | 5 | 5.00 |
| D4 Instruction Following (1.5x) | 5 | 4 | 4 | 4 | 4.25 |
| D5 Safety & Security (1.5x) | 3 | 3 | 5 | 5 | 4.00 |
| D6 Convention Compliance (1x) | 3 | 3 | 5 | 5 | 4.00 |
| D7 PostgreSQL Correctness (1x) | 3 | 3 | 5 | 4 | 3.75 |
| D8 Reasoning Quality (1x) | 3 | 4 | 3 | 5 | 3.75 |
| D9 Output Structure (0.5x) | 5 | 5 | 4 | 4 | 4.50 |
| D10 Conciseness (0.5x) | 5 | 4 | 3 | 3 | 3.75 |

### Model: GPT-5.2 (1x, Reference)

| Dimension | S1 | S2 | S3 | S4 | Avg |
|-----------|:--:|:--:|:--:|:--:|:---:|
| D1 Code Correctness (2x) | 4 | 4 | 5 | 5 | 4.50 |
| D2 Architecture (2x) | 3 | 3 | 5 | 5 | 4.00 |
| D3 Completeness (1.5x) | 5 | 5 | 5 | 5 | 5.00 |
| D4 Instruction Following (1.5x) | 3 | 4 | 4 | 4 | 3.75 |
| D5 Safety & Security (1.5x) | 3 | 3 | 5 | 4 | 3.75 |
| D6 Convention Compliance (1x) | 3 | 3 | 5 | 5 | 4.00 |
| D7 PostgreSQL Correctness (1x) | 3 | 3 | 5 | 4 | 3.75 |
| D8 Reasoning Quality (1x) | 3 | 4 | 3 | 4 | 3.50 |
| D9 Output Structure (0.5x) | 3 | 5 | 4 | 4 | 4.00 |
| D10 Conciseness (0.5x) | 4 | 4 | 3 | 4 | 3.75 |

### Model: GPT-5.4 (1x, Reference)

| Dimension | S1 | S2 | S3 | S4 | Avg |
|-----------|:--:|:--:|:--:|:--:|:---:|
| D1 Code Correctness (2x) | 5 | 4 | 4 | 5 | 4.50 |
| D2 Architecture (2x) | 3 | 3 | 4 | 5 | 3.75 |
| D3 Completeness (1.5x) | 5 | 5 | 5 | 5 | 5.00 |
| D4 Instruction Following (1.5x) | 5 | 4 | 3 | 4 | 4.00 |
| D5 Safety & Security (1.5x) | 3 | 3 | 5 | 4 | 3.75 |
| D6 Convention Compliance (1x) | 3 | 3 | 4 | 5 | 3.75 |
| D7 PostgreSQL Correctness (1x) | 3 | 3 | 5 | 5 | 4.00 |
| D8 Reasoning Quality (1x) | 3 | 4 | 3 | 4 | 3.50 |
| D9 Output Structure (0.5x) | 5 | 5 | 4 | 4 | 4.50 |
| D10 Conciseness (0.5x) | 5 | 4 | 2 | 3 | 3.50 |

---

## Section C: Cost-Efficiency Ranking

```
Cost-Adjusted Score = Weighted Total / Cost Factor
  0x  -> factor 0.1   (not zero, to avoid divide-by-zero)
  0.33x -> factor 0.33
  1x  -> factor 1.0
```

| Rank | Model | Weighted Total | Multiplier | Cost-Adjusted Score |
|------|-------|:--------------:|:----------:|:-------------------:|
| 1 | **GPT-4.1** | 193.0 | 0x | **1930.0** |
| 2 | GPT-5 mini | 188.5 | 0x | 1885.0 |
| 3 | Raptor mini | 155.0 | 0x | 1550.0 |
| 4 | Gemini 3 Flash | 180.0 | 0.33x | 545.5 |
| 5 | Claude Haiku 4.5 | 161.0 | 0.33x | 487.9 |
| 6 | GPT-5.1-Codex-Mini | 114.5 | 0.33x | 347.0 |
| 7 | **Claude Sonnet 4.6** | **212.0** | 1x | 212.0 |
| 8 | GPT-5.2 | 203.5 | 1x | 203.5 |
| 9 | GPT-5.4 | 203.5 | 1x | 203.5 |

---

## Section D: Tier Winners

```
Best Free Model (0x):        GPT-4.1          (193.0/250)
Best Budget Model (0.33x):   Gemini 3 Flash   (180.0/250)
Best Reference Model (1x):   Claude Sonnet 4.6 (212.0/250)
Overall Best Value:           GPT-4.1          (193.0 at 0x cost)
```

---

## Section E: Scenario Winners

```
Best for Context Gathering (S1):   GPT-4.1       (49.5/62.5 at 0x — tied with Codex-Mini, Sonnet, GPT-5.4)
Best for Code Analysis (S2):       Claude Sonnet 4.6 (46.5/62.5 at 1x — no model reached 50; cheapest ≥45 = GPT-5 mini at 43.5/0x)
Best for Code Generation (S3):     GPT-4.1       (50.0/62.5 at 0x — cheapest model scoring ≥50)
Best for Architecture Design (S4): GPT-4.1       (53.0/62.5 at 0x — cheapest model scoring ≥50)
```

**Note:** For S2 (Code Analysis), no model scored ≥ 50/62.5. All models struggled with the "recommendedStrategy" dimension — the ground truth mandates DECOMPOSE for all 4 SPs, but most models recommended KEEP_SP for the complex procedures (a defensible real-world judgment, but scored strictly against the rubric).

---

## Section F: Specific Failure Analysis

### GPT-5.1-Codex-Mini — Catastrophic failure on S3 and S4 (12.5/62.5 each)

- **S3 (Code Generation):** Model did not produce any code. Instead, it analyzed the workspace directory structure and reported that "Change-password Java artifacts still missing." It treated the prompt as a status query rather than a generation task.
- **S4 (Architecture Design):** Returned empty/no output. Agent completed with no response.
- **Failure mode:** Systematic. Codex-Mini appears to be optimized for in-file editing tasks and struggles with standalone code generation prompts that don't reference existing workspace files. It defaulted to workspace exploration instead of following generation instructions.
- **Failed dimensions:** All 10 dimensions scored 1 on both S3 and S4.

### Claude Haiku 4.5 — Weak on S3 (39.0/62.5) and S4 (33.0/62.5)

- **S3:** Missing `@Modifying` annotations on repository DML methods. BCrypt history check uses SQL equality instead of `passwordEncoder.matches()`. Generic `try/catch` catches all exceptions and returns USER_NOT_FOUND.
- **S4:** Used `HikariConfig` beans directly instead of `DataSourceProperties` (as required). Ambiguous bean resolution between two unqualified `HikariConfig` beans. **Critical: SQL injection vulnerability** in `queryLegacyTable` method (`String sql = "SELECT * FROM " + tableName + " WHERE " + whereClause`).
- **Failure mode:** Partially systematic. Haiku produces verbose output with extra functionality that introduces bugs and security vulnerabilities.

### Raptor mini — Consistently below average, worst on S2 (31.0/62.5)

- **S2:** No reasoning provided for complexity or strategy classifications. Used positional parameter names ("1", "2", "3") instead of meaningful names. Wrong `callPattern` for SP1 (JPA_PROCEDURE instead of STORED_PROCEDURE_QUERY).
- **S4:** Used `javax.persistence` imports (won't compile on Spring Boot 3 which requires `jakarta.persistence`). Missing driver-class-name in YAML.
- **Failure mode:** Systematic. Raptor produces minimal, low-detail output. Good for simple extraction, poor for nuanced analysis or complex generation.

### GPT-5.2 — Weak on S1 (43.0/62.5)

- **S1:** Wrapped JSON in markdown code fences (violating "no markdown" instruction). Used "X-Request-Id" (header name) instead of "requestId" (Java variable name) for the `@HeaderParam` parameter.
- **Failure mode:** Random. GPT-5.2 performs excellently on S3/S4 but occasionally adds markdown formatting when told not to.

---

## Section G: Final Recommendation

```
RECOMMENDED SUB-AGENT CONFIGURATION:

For "Reader" sub-agents (context gathering, code analysis):
  → Model: GPT-4.1
  → Cost: 0x (free)
  → Reason: Scored 49.5/62.5 on S1 (tied for #1) and 40.5/62.5 on S2,
    with clean JSON output and zero formatting violations. At 0x cost,
    it delivers reference-tier quality for extraction tasks. GPT-5 mini
    is a viable alternative with slightly better S2 scores (43.5).

For "Worker" sub-agents (code generation, architecture design):
  → Model: Claude Sonnet 4.6
  → Cost: 1x
  → Reason: Highest overall score (212.0/250), top-ranked on S3 (57.5,
    tied with GPT-5.2) and S4 (58.5, best overall). Correctly handles
    BCrypt non-deterministic hashing in password history checks — a 
    critical safety requirement that most cheaper models miss. Produces
    complete, compilable code with proper Spring Boot 3 conventions.
    GPT-5.2 is an equally strong alternative (tied on S3, close on S4).

  → Budget alternative: GPT-4.1 at 0x
    Scored 50.0/62.5 on S3 and 53.0/62.5 on S4 — both above the 50-point
    threshold. If cost is paramount, GPT-4.1 delivers >80% of Sonnet's
    quality at zero cost.

Estimated cost savings vs using Opus 4.6 for everything:
  → Reader savings: 100% (GPT-4.1 at 0x vs Opus at >1x)
  → Worker savings: 0% (Sonnet at 1x vs Opus — same tier)
  → Worker savings (budget): 100% (GPT-4.1 at 0x if quality threshold met)
  → Blended savings (Sonnet workers): ~60% (assuming 60% reader, 40% worker)
  → Blended savings (GPT-4.1 for all): ~100% (free tier throughout)
```

### Summary Matrix

| Role | Top Pick | Cost | Score | Runner-Up | Cost |
|------|----------|------|-------|-----------|------|
| Reader (S1/S2) | GPT-4.1 | 0x | 193.0 | GPT-5 mini | 0x |
| Worker (S3/S4) | Claude Sonnet 4.6 | 1x | 212.0 | GPT-5.2 | 1x |
| All-rounder | GPT-4.1 | 0x | 193.0 | Gemini 3 Flash | 0.33x |
| Avoid | GPT-5.1-Codex-Mini | 0.33x | 114.5 | — | — |

### Key Takeaways

1. **GPT-4.1 is the standout value pick.** At 0x cost, it scores within 10% of 1x reference models on extraction and architecture tasks.
2. **Claude Sonnet 4.6 leads on quality.** It's the only model that consistently produces compilable, convention-correct, BCrypt-safe code across all scenarios.
3. **GPT-5.2 matches Sonnet on code generation** but slightly trails on architecture design and S1 formatting discipline.
4. **GPT-5.1-Codex-Mini should not be used as a sub-agent.** It fails catastrophically on standalone generation prompts, likely due to its optimization for in-editor code editing rather than zero-context generation.
5. **The Budget tier is uneven.** Gemini 3 Flash is the clear Budget winner (180.0), while Codex-Mini (114.5) and Haiku (161.0, with security concerns) trail significantly.
6. **S2 (Code Analysis) is the hardest scenario.** No model scored above 46.5/62.5 — all struggled with matching the ground truth's "DECOMPOSE everything" strategy, since real-world judgment favors KEEP_SP for complex SPs.
