# VS Code Copilot Agent File Format — Comprehensive Reference

## Why it looks different from examples you find online

Most tutorials show the **basic** `.agent.md` format: a YAML frontmatter block with just `name`,
`description`, and a body prompt. The `PlanCopy.agent.md` file you copied uses the **full**
VS Code Copilot agent feature set — including tool declarations, handoff buttons, and structured
XML-like prompt sections. These are VS Code-specific extensions on top of the base format.

---

## File Naming & Location

| Convention | Detail |
|---|---|
| File extension | `.agent.md` |
| Location | `.github/` (project-scoped) or `~/.github/` (user-scoped) |
| Discovery | VS Code automatically discovers all `*.agent.md` files in `.github/` |

---

## Full Anatomy of an Agent File

```
---                          ← YAML frontmatter start
<frontmatter fields>
---                          ← YAML frontmatter end
<system prompt body>         ← Everything below is the agent's system prompt
```

---

## Part 1 — Frontmatter Fields (YAML between `---`)

### Required Fields

| Field | Type | Description |
|---|---|---|
| `name` | string | Display name shown in the VS Code agent picker (e.g., `@Plan`) |
| `description` | string | Short description shown in the UI and used for agent discovery |

### Optional Fields

| Field | Type | Description |
|---|---|---|
| `argument-hint` | string | Placeholder hint text shown in the chat input when invoking the agent. Guides the user on what to type. |
| `target` | `vscode` | Declares the agent is for VS Code only. Currently the only accepted value. Omitting it is also valid. |
| `disable-model-invocation` | boolean | When `true`, the agent itself never calls the LLM directly — it only orchestrates subagents and tools. Useful for pure router/dispatcher agents. |
| `tools` | string[] | Whitelisted tool IDs the agent is allowed to invoke. If omitted, all tools are available. |
| `agents` | string[] | Other agents this agent is allowed to delegate to. Empty array `[]` means no agent delegation. |
| `handoffs` | object[] | Buttons rendered at the end of the agent's response that let the user trigger the next step. See detail below. |

---

### `tools` — Allowed Tool IDs

Tools are declared as an array of string IDs. VS Code Copilot has a built-in tool namespace system:

```yaml
tools:
  - 'agent'                                          # Delegate to subagents
  - 'search'                                         # Semantic workspace search
  - 'read'                                           # Read files
  - 'execute/getTerminalOutput'                      # Read terminal output
  - 'execute/testFailure'                            # Read test failure details
  - 'web'                                            # Fetch web content
  - 'github/issue_read'                              # Read GitHub issues
  - 'github.vscode-pull-request-github/issue_fetch' # Extension-contributed tool
  - 'github.vscode-pull-request-github/activePullRequest'
  - 'vscode/askQuestions'                            # Ask user clarifying questions
```

**Format**: `namespace/toolName` or `extensionId/toolName` for extension-contributed tools.

**Can you use these in your own agents?** Yes — you can declare any subset of these in your own
agent's `tools` list. You cannot invent new tool IDs; the tool must already exist in VS Code or
be contributed by an installed extension.

---

### `handoffs` — Chain-of-Agent Buttons

Handoffs render as clickable buttons after the agent's response, enabling multi-agent workflows.

```yaml
handoffs:
  - label: Start Implementation      # Button label shown in the UI
    agent: agent                     # Target: 'agent' = default Copilot chat agent
    prompt: 'Start implementation'   # Pre-filled message sent when user clicks
    send: true                       # true = send immediately; false = just fill input box

  - label: Open in Editor
    agent: agent
    prompt: '#createFile the plan...'
    send: true
    showContinueOn: false            # Hides the "Continue" option on the handoff
```

| Handoff Property | Type | Description |
|---|---|---|
| `label` | string | Button text displayed to the user |
| `agent` | string | Agent to hand off to. `agent` means the default chat agent |
| `prompt` | string | Message pre-filled or auto-sent to the target agent |
| `send` | boolean | `true` = auto-sends the prompt; `false` = only pre-fills the input |
| `showContinueOn` | boolean | Controls whether a "Continue" option is appended. Default `true`. |

**Can you use handoffs in your own agents?** Yes — this is a first-class VS Code feature. You can
create any handoff chain between your own agents or to the default agent.

---

## Part 2 — Prompt Body (System Prompt)

Everything below the closing `---` is the agent's system prompt fed to the model. It is plain
Markdown, but the `PlanCopy.agent.md` uses several **XML-like structural tags** as a
prompt-engineering pattern.

> **Are these tags a VS Code feature or a prompting convention?**
>
> They are a **prompting convention**, not a VS Code syntax. VS Code does not parse or process
> these tags specially — the LLM sees them as text. They work because modern LLMs (GPT-4, Claude,
> etc.) understand XML-like structure as section delimiters.
>
> You can use the same tags in **any** system prompt on any platform (OpenAI API,
> Claude API, Gemini, etc.).

---

### Common XML-like Tags Used in This File

#### `<rules>` — Hard constraints
```xml
<rules>
- STOP if you consider running file editing tools
- Use #tool:vscode/askQuestions freely
</rules>
```
Lists non-negotiable behavioral rules. The LLM treats these as the highest-priority instructions.
Use this for guardrails you never want the agent to violate.

---

#### `<workflow>` — Multi-phase process definition
```xml
<workflow>
## 1. Discovery
...
## 2. Alignment
...
</workflow>
```
Defines the step-by-step operating procedure. The agent cycles through these phases. This is the
core "brain" of a complex agent — it replaces a flat prompt with structured decision logic.

---

#### `<research_instructions>` — Subagent task scoping (nested tag)
```xml
<research_instructions>
- Research comprehensively using read-only tools.
- Start with high-level code searches.
- DO NOT draft a full plan yet.
</research_instructions>
```
Used inside `<workflow>` to instruct a subagent exactly what to do. Nesting tags like this
creates scoped instructions that apply only in a specific context.

---

#### `<plan_style_guide>` — Output format template
```xml
<plan_style_guide>
```markdown
## Plan: {Title}
...
```
Rules:
- NO code blocks
</plan_style_guide>
```
Defines the exact format the agent must use for its output. Embedding a template inside a
named tag makes it easy to reference it in the prompt body (`per <plan_style_guide>`).

---

### `#tool:` References in the Body

In the prompt body, tools are referenced with the `#tool:` prefix:

```
Run #tool:agent/runSubagent to gather context.
Use #tool:vscode/askQuestions to clarify intent.
```

This is a VS Code Copilot convention that tells the model **which specific tool to prefer** for a
given task. It also makes the prompt self-documenting. These references only work meaningfully
inside VS Code where those tools exist; in other platforms they are just hint text.

---

## Part 3 — Comparison: Basic vs Full Format

| Feature | Basic (online examples) | Full VS Code format |
|---|---|---|
| `name` + `description` | Yes | Yes |
| `argument-hint` | Rarely shown | Yes |
| `target` | No | Yes |
| `disable-model-invocation` | No | Yes |
| `tools` whitelist | No | Yes |
| `agents` list | No | Yes |
| `handoffs` buttons | No | Yes (VS Code only) |
| XML structural tags in body | Sometimes | Yes (prompting convention) |
| `#tool:` references in body | No | Yes |

---

## Part 4 — Can You Use These in Your Own Agents?

| Feature | Can you use it? | Notes |
|---|---|---|
| All frontmatter fields | **Yes** | These are the VS Code agent spec |
| `tools` whitelist | **Yes** | List only existing VS Code tool IDs |
| `handoffs` | **Yes** | First-class VS Code feature |
| `disable-model-invocation` | **Yes** | Useful for pure orchestrator agents |
| XML-like body tags | **Yes — anywhere** | A prompting convention; works in any LLM |
| `#tool:` references | **VS Code only** | Only meaningful where those tools exist |
| `target: vscode` | **VS Code only** | Ignored on other platforms |

---

## Part 5 — Minimal Agent Template

```markdown
---
name: MyAgent
description: Does X given Y
argument-hint: Describe what you want to do
tools: ['read', 'search']
---
You are a [ROLE] agent.

<rules>
- Rule 1
- Rule 2
</rules>

<workflow>
## 1. Discovery
...

## 2. Output
Produce output per <output_format>.
</workflow>

<output_format>
[Define expected output structure here]
</output_format>
```

---

## Part 6 — Full Agent Template (with handoffs)

```markdown
---
name: MyAgent
description: Does X
argument-hint: Describe the goal
target: vscode
disable-model-invocation: false
tools: ['read', 'search', 'vscode/askQuestions', 'agent']
agents: []
handoffs:
  - label: Next Step
    agent: agent
    prompt: 'Proceed with implementation'
    send: true
---
You are a [ROLE] agent.

<rules>
- Never do X.
- Always do Y.
</rules>

<workflow>
## 1. Phase One
Use #tool:search to explore the codebase.

## 2. Phase Two
Use #tool:vscode/askQuestions if requirements are unclear.

## 3. Output
Produce output per <output_format>.
</workflow>

<output_format>
Define your expected output structure here.
</output_format>
```

---

## References

- [VS Code Copilot Agent Mode docs](https://code.visualstudio.com/docs/copilot/chat/chat-agent-mode)
- [`.github/copilot-instructions.md` spec](https://docs.github.com/en/copilot/customizing-copilot/adding-repository-custom-instructions-for-github-copilot)
- [Prompt engineering with XML tags (Anthropic)](https://docs.anthropic.com/en/docs/build-with-claude/prompt-engineering/use-xml-tags)
