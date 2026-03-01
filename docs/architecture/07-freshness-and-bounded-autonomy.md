# 07 — Freshness & Bounded Autonomy: Predictability Without Rigidity

> **Purpose:** Address the core tension between agent creativity and enterprise predictability. Define guardrails, drift detection, governance models, skill versioning policies, and the Innovation Budget concept that gives agents defined latitude for exploratory suggestions alongside mandatory compliance.

---

## 1. The Fundamental Tension

Enterprise teams want two things simultaneously:

1. **Predictability:** "Given the same inputs, the agent should produce similar
   outputs every time. I need to trust it before I stop reviewing every line."

2. **Adaptiveness:** "The agent should recognize novel situations, suggest
   improvements I didn't think of, and not be a rigid template machine."

These goals are in tension. Pure predictability means the agent never innovates.
Pure autonomy means the agent is unpredictable. The architecture must define
**where on this spectrum each behavior sits** — and make that boundary explicit.

---

## 2. Bounded Autonomy: The Model

Bounded autonomy means: **agents operate freely within explicitly defined
guardrails, and escalate when they approach or cross boundary conditions.**

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│                    MANDATORY ZONE                           │
│     Agent MUST follow. No exceptions. No creativity.        │
│     "Use parameterized queries. Never log PII."             │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                  CONVENTIONAL ZONE                          │
│     Agent SHOULD follow loaded skill conventions.           │
│     May deviate with explicit justification.                │
│     "Use @DisplayName" — agent can skip if inapplicable     │
│     but must note why.                                      │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                   INNOVATION ZONE                           │
│     Agent MAY suggest improvements, alternatives, or         │
│     novel approaches. Must label as suggestions.            │
│     "Consider using @ParameterizedTest for this case."      │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                   FORBIDDEN ZONE                            │
│     Agent MUST NOT enter. Always escalate.                  │
│     "Modify database schema without a migration."           │
│     "Disable security validation."                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Zone Definitions

| Zone | Compliance | Agent Behavior | Example |
|------|-----------|---------------|---------|
| **Mandatory** | 100% required | Follow without question | Structured error DTOs, parameterized queries |
| **Conventional** | Expected with justified exceptions | Follow unless inapplicable, then explain | Test naming conventions, DTO mapping patterns |
| **Innovation** | Optional, clearly labeled | Suggest improvements as addenda | Alternative assertion patterns, refactoring ideas |
| **Forbidden** | 100% prohibited | Escalate immediately if task requires | Direct DB writes without migration, disabling auth |

### How Zones Map to Layers

| Zone | Typically Defined In | Example Location |
|------|---------------------|-----------------|
| Mandatory | `copilot-instructions.md` + Agent constraints | "No hardcoded secrets" in global instructions |
| Conventional | Skill files | "Use @DisplayName" in junit5.skill.md |
| Innovation | Agent identity | "When you identify a potential improvement..." |
| Forbidden | Agent constraints + Orchestrator rules | "Never modify files outside the task scope" |

---

## 3. Guardrail Categories

Guardrails are the concrete mechanisms that enforce bounded autonomy.

### 3.1 — Output Format Guardrails

Agents must produce output in expected formats. Unexpected format changes
indicate drift.

```
GUARDRAIL: Output format compliance
RULE: Test Agent must produce compilable Java test classes when
      java.skill is loaded, or valid TypeScript test files when
      typescript.skill is loaded.
ENFORCEMENT: Orchestrator validates output format before passing
             to Reviewer. Malformed output triggers re-generation.
ESCAPE: Agent may produce additional commentary alongside code,
        clearly separated (before or after the code block).
```

### 3.2 — Scope Guardrails

Agents must stay within the task scope defined by the prompt and plan.

```
GUARDRAIL: Scope containment
RULE: Agent output must address only the files and components
      specified in the plan. Creating new files not in the plan
      requires Orchestrator approval.
ENFORCEMENT: Orchestrator compares planned artifacts vs. produced
             artifacts. Unplanned artifacts trigger a scope-creep
             escalation.
METRIC: planned_artifact_count vs. produced_artifact_count.
        Ratio > 1.5 triggers review. Ratio > 2.0 triggers escalation.
```

### 3.3 — Confidence Guardrails

Agents should signal when they're uncertain rather than producing
low-confidence output silently.

```
GUARDRAIL: Confidence signaling
RULE: When the agent encounters a situation not covered by loaded
      skills, it must flag the gap rather than improvising silently.
SIGNAL FORMAT:
  "[LOW CONFIDENCE] I'm generating this test without guidance from
   the loaded skills for {specific situation}. Please review carefully."
ESCALATION: Orchestrator collects low-confidence signals. If >30%
            of output is flagged, escalate rather than proceeding to review.
```

### 3.4 — Iteration Guardrails

Prevent infinite loops and excessive retries.

```
GUARDRAIL: Iteration caps
RULE: Maximum 2 review-and-revise cycles per task.
ENFORCEMENT: Orchestrator tracks iteration count. At max, escalate
             to user with full context.
TUNING: Teams can adjust the cap (1 for fast feedback, 3 for
        high-stakes code). Default: 2.
```

### 3.5 — Forbidden Action Guardrails

Hard stops on actions that agents must never take autonomously.

```
GUARDRAIL: Forbidden actions
RULES:
  - Never delete files without explicit user confirmation
  - Never modify files outside the current task scope
  - Never commit or push to version control
  - Never execute code that modifies production data
  - Never disable security controls, even temporarily
  - Never expose secrets, tokens, or PII in output
ENFORCEMENT: Agent identity file lists these as CONSTRAINTS.
             Orchestrator prevents file system writes to
             out-of-scope paths.
```

---

## 4. Freshness: Keeping the System Current

Freshness is the challenge of ensuring that skills, knowledge bases, and agent
definitions reflect current best practices, framework versions, and project state.

### 4.1 — What Goes Stale and How Fast

| Artifact | Staleness Rate | Symptoms | Impact |
|----------|---------------|----------|--------|
| **Knowledge bases** | Days–weeks | Agent references wrong table schemas, outdated API contracts | Incorrect output |
| **Framework skills** | Months | Deprecated patterns suggested, new features unknown | Suboptimal output |
| **Domain skills** | Months–years | Conventions drift from actual practice | Ignored conventions |
| **Agent definitions** | Years | Reasoning strategies still valid | Minimal impact |
| **Prompt templates** | Varies | Placeholder names don't match current project structure | Confusing UX |

**Key insight:** Agent definitions are the most stable artifacts. Skills and
knowledge bases are the most volatile. This reinforces the architectural decision
to keep agents generic and push specifics into skills.

### 4.2 — Skill Versioning Policy

Every skill file carries a semantic version and changelog:

```
VERSION: 1.5
LAST_UPDATED: 2026-02-15

CHANGELOG:
  1.5 (2026-02-15): Added @DataJpaTest vs @SpringBootTest guidance.
       Source: Reviewer finding from user-service experiment run #2.
  1.4 (2026-01-20): Added Testcontainers PostgreSQL pattern.
  1.3 (2025-12-01): Updated AssertJ examples to 3.25 API.
  1.2 (2025-10-15): Added @ParameterizedTest pattern.
  1.1 (2025-09-01): Added @Nested grouping convention.
  1.0 (2025-08-01): Initial version.
```

**Version bump rules:**
- **PATCH** (1.5.1): Typo fix, clarification, no behavioral change
- **MINOR** (1.6): New convention added, existing conventions unchanged
- **MAJOR** (2.0): Existing convention changed or removed, agent output will differ

### 4.3 — Freshness Detection Mechanisms

**Mechanism 1 — Timestamp-Based Alerts**

```
IF skill.last_updated < (today - 180 days):
  WARNING: "{skill} has not been updated in 180 days.
            Framework version may have changed."
```

**Mechanism 2 — Version Compatibility Checks**

```
IF detected_framework_version > skill.compatible_with.max_version:
  WARNING: "{skill} was written for {framework} {max_version} but
            this project uses {detected_version}. Some patterns
            may be outdated."
```

**Mechanism 3 — Experiment-Driven Validation**

Run the agent periodically against known test cases and compare output
to established baselines. Drift in output quality signals that either the
skill or the underlying model has changed.

```
FRESHNESS TEST:
  Input: Known UserService implementation
  Expected: Test class with specific patterns (from baseline)
  Actual: Run agent with current skills
  Compare: Diff against baseline
  Alert if: >20% structural divergence
```

**Mechanism 4 — Reviewer Feedback Aggregation**

Track which conventions the Reviewer most frequently flags as violated.
A sudden increase in violations for a specific convention suggests either:
- The convention is poorly worded (skill needs refinement)
- The convention is outdated (skill needs update)
- The model's behavior has changed (may need stronger wording)

### 4.4 — Knowledge Base Refresh Strategies

| Strategy | How It Works | When to Use |
|----------|-------------|-------------|
| **Generated** | CI pipeline extracts patterns from source code into knowledge base files | Large, frequently changing codebases |
| **On-demand** | Agent reads source files directly at runtime, no static knowledge base | Small projects or when token budget allows |
| **Snapshot + diff** | Knowledge base generated periodically; agent also reads recent diffs for changes since snapshot | Balance of freshness and token efficiency |
| **Hybrid** | Stable facts (schema) generated; volatile facts (recent changes) read on-demand | Enterprise standard for medium-large projects |

---

## 5. Enterprise Governance

### 5.1 — Ownership Model

| Artifact | Owner | Change Process | Approval Required |
|----------|-------|---------------|-------------------|
| **Agent definitions** | Platform architecture team | PR with architectural review | Yes (architecture lead) |
| **Domain skills (Level 1)** | Central engineering standards team | PR with standards review | Yes (standards lead) |
| **Framework skills (Level 2)** | Platform team | PR with framework expert review | Yes (platform lead) |
| **Project skills (Level 3)** | Project team | PR with team lead review | Yes (tech lead) |
| **Knowledge bases** | Automated (generated) or project team | Auto-generated or PR | No (for generated); Yes (for authored) |
| **Prompt templates** | Workflow designers + project teams | PR | Yes (team lead) |
| **Orchestration config** | Platform architecture team | PR with architectural review | Yes (architecture lead) |
| **copilot-instructions.md** | Central engineering standards team | PR with broad review | Yes (engineering director) |

### 5.2 — Change Management Process

```
1. IDENTIFY: Reviewer finding, experiment analysis, version upgrade,
   or team request identifies a needed change.

2. CLASSIFY: Which artifact needs to change? (Use the Boundary
   Decision Flowchart from 01 — Responsibility Boundaries §6.)

3. AUTHOR: Create the change as a PR to the appropriate file.
   Include:
   - The finding/rationale that motivates the change
   - The specific change (diff)
   - Expected impact on agent output
   - VERSION bump (for skill files)

4. REVIEW: Appropriate reviewer based on ownership model.
   Review criteria:
   - Does this change respect layer boundaries?
   - Does it break the Substitution Principle?
   - Is it specific and actionable (not vague)?
   - Does it include an example?

5. TEST: Run the agent with the updated artifact against a known
   test case. Compare output to baseline.

6. MERGE: Merge the PR. The change takes effect immediately for
   all subsequent agent runs.

7. COMMUNICATE: If the change is MAJOR (breaking), notify teams
   that agent output will change.
```

### 5.3 — Rollback Strategy

If a change to a skill or agent definition causes quality regression:

```
1. DETECT: Reviewer flags increase, user complaints, experiment
   regression.
2. IDENTIFY: Which change caused the regression? (Git blame on
   skill/agent files.)
3. REVERT: Git revert the PR. Version bump to indicate the revert.
4. ANALYZE: Why did the change cause regression? Was it poorly
   worded? Did it conflict with another skill? Was it wrong?
5. RE-ATTEMPT: Fix the root cause and try again through the
   full change management process.
```

### 5.4 — Audit Trail

Every agent run produces an orchestration log (see
[05 — Orchestration Patterns](05-orchestration-patterns.md) §8.3). For
enterprise governance, the audit trail should also capture:

```
AUDIT RECORD:
  run_id: abc-123
  user: deepak@company.com
  timestamp: 2026-03-01T10:00:00Z
  prompt_hash: sha256(prompt)
  agent_definitions_used:
    - test.agent.md @ commit abc1234
  skills_used:
    - junit5.skill.md v1.5 @ commit def5678
    - java.skill.md v2.1 @ commit ghi9012
  context_object: {full Context Object}
  orchestration_log: {full log}
  output_hash: sha256(combined_output)
  review_verdict: APPROVED
  escalations: 0
  feedback_items: ["@DataJpaTest suggestion"]
```

This enables traceability: "Which skill version was used when this code was
generated?" and "What changed between the run that worked and the run that
didn't?"

---

## 6. The Innovation Budget

The Innovation Budget is a framework for giving agents controlled creative
latitude. Without it, agents produce mechanical output that follows conventions
but never suggests improvements. With it, agents can propose enhancements —
clearly labeled and bounded.

### 6.1 — How It Works

Every agent invocation has an Innovation Budget — a defined allowance for
suggestions beyond the strict task requirements.

```
INNOVATION BUDGET:
  max_suggestions: 3
  scope: related to the current task
  format: clearly separated section at the end of output
  labeling: prefixed with [SUGGESTION]
  binding: none (user decides whether to accept)
```

### 6.2 — Rules for Innovation

**Rule 1 — Suggestions are additive, not substitutive.** The agent must
first produce the requested output following all mandatory and conventional
zone rules. Only then may it add suggestions.

**Rule 2 — Suggestions are clearly labeled.** They must not be mixed into
the primary output. A separate section, clearly bordered.

```
// ─── Primary output (mandatory + conventional zones) ───
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    // ... standard tests following all conventions ...
}

// ─── Innovation (suggestions, non-binding) ───
// [SUGGESTION 1] Consider using @ParameterizedTest for the
//   validation tests above. This would reduce 4 test methods
//   to 1 parameterized method with better coverage.
//
// [SUGGESTION 2] The existing UserService.updateUser catches
//   Exception broadly. A more specific catch would improve
//   test specificity. (Not in the current task scope, but
//   worth noting for a future refactor.)
```

**Rule 3 — Suggestions reference specific, actionable changes.** "Consider
improving the tests" is not actionable. "Consider using @ParameterizedTest
for the email validation cases" is.

**Rule 4 — Suggestion count is bounded.** Default: 3 per invocation. This
prevents the agent from spending its reasoning budget on creative exploration
instead of the actual task.

**Rule 5 — Suggestions are included in review.** The Reviewer evaluates
suggestions for accuracy and relevance. Bad suggestions erode trust in the
system.

### 6.3 — Configuring the Innovation Budget

The Innovation Budget is configurable per workflow, team, or task type:

| Situation | Budget | Rationale |
|-----------|--------|-----------|
| **Greenfield development** | max_suggestions: 5 | More room for architectural suggestions |
| **Maintenance/bug fix** | max_suggestions: 1 | Focus on the fix, minimal exploration |
| **Security-critical code** | max_suggestions: 0 | Zero creativity; follow conventions exactly |
| **Exploration/prototyping** | max_suggestions: 10 | Maximum creativity, labeled clearly |
| **Enterprise default** | max_suggestions: 3 | Balanced |

### 6.4 — Innovation Budget as a Trust Ramp

For organizations new to agentic workflows, the Innovation Budget provides a
trust ramp:

```
Phase 1 (Weeks 1-4):  max_suggestions: 0
  "Agent follows conventions only. We're validating that
   basic output is correct."

Phase 2 (Weeks 5-12): max_suggestions: 1
  "Agent can make one suggestion per task. We're evaluating
   whether suggestions add value."

Phase 3 (Months 4+):  max_suggestions: 3
  "Agent has earned trust. Suggestions are consistently
   valuable and well-labeled."

Phase 4 (Mature):     max_suggestions: configurable per task
  "Teams set their own budget based on task type."
```

---

## 7. Drift Detection

Drift is when agent behavior gradually diverges from expected patterns without
any explicit change to agent definitions, skills, or prompts. This can happen
due to model updates, subtle prompt interactions, or knowledge base staleness.

### 7.1 — Types of Drift

| Drift Type | Cause | Symptom |
|-----------|-------|---------|
| **Model drift** | Underlying LLM updated by provider | Output style or quality changes without config change |
| **Convention drift** | Team practice evolves but skill files don't | Agent follows outdated conventions that no one actually uses |
| **Knowledge drift** | Project evolves but knowledge base doesn't | Agent references classes, methods, or schemas that no longer exist |
| **Prompt drift** | Prompt templates accumulate revisions that subtly change meaning | Gradual quality degradation that no single change explains |

### 7.2 — Detection Strategies

**Strategy 1 — Baseline Comparison (most reliable)**

Maintain golden test cases: known inputs with expected outputs. Run periodically
(weekly or after model updates). Compare structural similarity.

```
DRIFT TEST:
  Input: "Generate tests for UserService" + fixed Context Object
  Baseline output: UserServiceTest.java from run-1 (known good)
  Current output: UserServiceTest.java from today's run

  Metrics:
    - Same number of test methods? (±10% acceptable)
    - Same conventions followed? (naming, annotations, assertions)
    - Same edge cases covered?
    - Any new convention violations?

  ALERT if: structural similarity < 80%
```

**Strategy 2 — Reviewer Finding Trend Analysis**

Track Reviewer findings over time. A sudden increase in a specific category
indicates drift.

```
TREND:
  Week 1: 2 findings (1 naming, 1 assertion style)
  Week 2: 1 finding  (1 naming)
  Week 3: 5 findings (3 missing @DisplayName, 1 naming, 1 structure)
  Week 4: 7 findings (5 missing @DisplayName, 2 assertion style)

  ALERT: "@DisplayName compliance dropped significantly.
          Possible causes: skill instruction deprioritized by model,
          or convention wording needs strengthening."
```

**Strategy 3 — Output Structural Analysis**

Analyze agent output structure (not content) over time. Track metrics like:
- Average test methods per class
- Import statement patterns
- Annotation usage frequency
- Code structure (nested classes, setup methods, etc.)

Sudden changes in structural metrics without corresponding skill changes
indicate drift.

### 7.3 — Drift Response Protocol

```
1. DETECT: Alert triggers from any detection strategy.

2. DIAGNOSE: What type of drift?
   - Model drift: Compare output across model versions.
   - Convention drift: Compare skill conventions to actual team practice.
   - Knowledge drift: Compare knowledge base to current source code.
   - Prompt drift: Review prompt template change history.

3. RESPOND:
   - Model drift → Strengthen skill file wording. Add more explicit
     examples. Consider pinning model version if possible.
   - Convention drift → Update skill files to match current practice.
     Or, if current practice is wrong, re-align the team.
   - Knowledge drift → Refresh knowledge base. Switch to on-demand
     knowledge if freshness is critical.
   - Prompt drift → Audit and simplify prompt templates. Remove
     accumulated cruft.

4. VALIDATE: Re-run baseline tests. Confirm metrics return to expected range.

5. DOCUMENT: Log the drift event, diagnosis, and response in the
   experiment history for organizational learning.
```

---

## 8. Putting It All Together: The Predictability-Adaptiveness Spectrum

Every behavior in the system sits somewhere on this spectrum. The architecture
makes the placement explicit:

```
PREDICTABLE ◄─────────────────────────────────────────► ADAPTIVE

Forbidden    Mandatory    Conventional    Innovation    Exploration
Zone         Zone         Zone            Zone          (future)

  │            │              │               │             │
  │            │              │               │             │
  Never     Always      Follow with       Suggest but     Full agent
  do this   do this     justified          label and      autonomy
                        exceptions         bound           (research
                                                           setting)
```

### Where Each Architectural Layer Sits

| Layer | Default Position | Adjustable? |
|-------|-----------------|-------------|
| **copilot-instructions.md** | Mandatory zone | No — these are non-negotiable baseline rules |
| **Agent constraints** | Mandatory + Forbidden zones | Rarely — agent identity is stable |
| **Skill conventions** | Conventional zone | Yes — teams can mark conventions as mandatory or advisory |
| **Prompt constraints** | Task-specific (any zone) | Yes — per-prompt basis |
| **Innovation Budget** | Innovation zone boundary | Yes — per-workflow, per-team, per-task-type |

### The Enterprise Configuration Surface

For a mature enterprise deployment, the tunable parameters are:

```
ENTERPRISE CONFIG:
  # Confidence thresholds for auto-proceed vs. confirm
  detection_auto_proceed_threshold: HIGH
  detection_confirm_threshold: MEDIUM

  # Review iteration limits
  max_review_iterations: 2

  # Innovation budget defaults
  default_innovation_budget: 3
  security_critical_budget: 0

  # Freshness alerts
  skill_staleness_threshold_days: 180
  knowledge_staleness_threshold_days: 30

  # Drift detection
  baseline_test_frequency: weekly
  drift_similarity_threshold: 0.80
  finding_trend_alert_threshold: 3x increase

  # Governance
  agent_definition_approval: architecture-lead
  domain_skill_approval: standards-lead
  framework_skill_approval: platform-lead
  project_skill_approval: tech-lead
```

---

## Key Takeaways

1. **Bounded autonomy** is the model: agents operate freely within defined zones
   (Mandatory, Conventional, Innovation, Forbidden). Each zone has clear rules
   about compliance and creativity.

2. **Guardrails** (output format, scope, confidence, iteration, forbidden actions)
   are the enforcement mechanisms. They're defined in agent constraints and
   orchestration rules, not in skills or prompts.

3. **Freshness** is maintained through skill versioning, timestamp alerts, version
   compatibility checks, experiment-driven validation, and Reviewer feedback
   aggregation. Agent definitions are the most stable; knowledge bases the most
   volatile.

4. **The Innovation Budget** gives agents controlled creative latitude: bounded,
   labeled, additive, and reviewable. It scales with organizational trust.

5. **Drift detection** (baseline comparison, finding trend analysis, structural
   analysis) catches silent quality degradation. The response protocol categorizes
   drift by type and patches the correct layer.

6. **Enterprise governance** defines ownership, change management, rollback, and
   audit trail requirements. Every run is traceable to specific artifact versions.

7. **The Predictability-Adaptiveness spectrum** is explicit. No behavior exists
   in ambiguous territory — every convention, constraint, and suggestion is placed
   in a defined zone.

---

*This concludes the architecture documentation series. For a map of all documents, see [00 — Overview](00-overview.md) §6.*
