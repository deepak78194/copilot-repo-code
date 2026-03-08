# VS Code February 2026 — v1.110 Release Notes
> **Release date:** March 4, 2026  
> **Theme:** Agents for longer-running and complex tasks, with more control, visibility, new extensibility options, and smarter session management.

---

## Table of Contents

1. [Agent Controls](#1-agent-controls)
   - [Background Agents](#11-background-agents)
   - [Claude Agents](#12-claude-agents)
   - [Agent Debug Panel (Preview)](#13-agent-debug-panel-preview)
   - [Slash Commands for Auto Approval](#14-slash-commands-for-auto-approval)
   - [Edit and Ask Mode Changes](#15-edit-and-ask-mode-changes)
   - [Ask Questions Tool](#16-ask-questions-tool)
   - [Prevent Auto-Suspend During Chat](#17-prevent-auto-suspend-during-chat)
2. [Agent Extensibility](#2-agent-extensibility)
   - [Agent Plugins (Experimental)](#21-agent-plugins-experimental)
   - [Agentic Browser Tools (Experimental)](#22-agentic-browser-tools-experimental)
   - [Create Agent Customizations from Chat](#23-create-agent-customizations-from-chat)
   - [Tools for Usages and Rename](#24-tools-for-usages-and-rename)
3. [Smarter Sessions](#3-smarter-sessions)
   - [Session Memory for Plans](#31-session-memory-for-plans)
   - [Context Compaction](#32-context-compaction)
   - [Codebase Search with Explore Subagent](#33-codebase-search-with-explore-subagent)
   - [Inline Chat and Chat Session](#34-inline-chat-and-chat-session)
   - [Fork a Chat Session](#35-fork-a-chat-session)
4. [Chat Experience](#4-chat-experience)
   - [Redesigned Model Picker](#41-redesigned-model-picker)
   - [Discover Features with Contextual Tips (Experimental)](#42-discover-features-with-contextual-tips-experimental)
   - [Custom Thinking Phrases](#43-custom-thinking-phrases)
   - [Collapsible Terminal Tool Calls](#44-collapsible-terminal-tool-calls)
   - [OS Notifications for Chat Responses](#45-os-notifications-for-chat-responses)
   - [Inline Chat Hover Mode](#46-inline-chat-hover-mode)
   - [Inline Chat Affordance](#47-inline-chat-affordance)
5. [Accessibility](#5-accessibility)
6. [Editor Experience](#6-editor-experience)
   - [Modal Editors (Experimental)](#61-modal-editors-experimental)
   - [Configurable Notification Position](#62-configurable-notification-position)
   - [Settings Editor Cleanup](#63-settings-editor-cleanup)
7. [Code Editing](#7-code-editing)
   - [Long-Distance Next Edit Suggestions](#71-long-distance-next-edit-suggestions)
   - [NES Eagerness Control](#72-nes-eagerness-control)
8. [Source Control](#8-source-control)
   - [AI Co-Author Attribution for Commits](#81-ai-co-author-attribution-for-commits)
9. [Debugging](#9-debugging)
   - [Custom Property Replacements (JS Debugger)](#91-custom-property-replacements-js-debugger)
   - [Emulate Focused Window](#92-emulate-focused-window)
10. [Terminal](#10-terminal)
    - [Kitty Graphics Protocol](#101-kitty-graphics-protocol)
    - [Ghostty Support for External Terminal](#102-ghostty-support-for-external-terminal)
    - [Workspace Folder Selection for External Terminals](#103-workspace-folder-selection-for-external-terminals)
    - [Terminal Sandboxing Improvements](#104-terminal-sandboxing-improvements-preview)
11. [Languages](#11-languages)
    - [Unified JavaScript and TypeScript Settings](#111-unified-javascript-and-typescript-settings)
    - [Python Environments Extension](#112-python-environments-extension)
12. [Extension Authoring](#12-extension-authoring)
13. [Deprecated Features](#13-deprecated-features)

---

## 1. Agent Controls

### 1.1 Background Agents

**What is it?**  
Background agents let you hand off long-running tasks to the Copilot CLI while you continue working in VS Code. You can monitor and interact with them without blocking your primary workflow.

**Why use it?**  
Useful for tasks that take time — like running tests, refactoring a large codebase, or performing batch operations — where you don't want to wait in place. You stay productive while the agent works.

**What's new in v1.110:**
- **Context compaction:** The session automatically compacts history when the context window fills up. You can also manually trigger it with `/compact`, optionally followed by instructions like `/compact focus on database schema decisions`.
- **Slash commands:** Prompt files, hooks, and skills are now available as slash commands within background agent sessions.
- **Rename sessions:** Give meaningful names to background agent sessions to keep track of multiple parallel tasks.

**How to use:**
1. Start a Copilot Chat session and switch to **Agent mode**.
2. Delegate a long-running task — Copilot CLI will pick it up as a background agent.
3. Track sessions from the Chat view; rename them by right-clicking.
4. Type `/compact` in the chat input to manually compact history when needed.

---

### 1.2 Claude Agents

**What is it?**  
Claude agents enable you to interact with the Claude Agent SDK using Claude models included in your GitHub Copilot subscription. This is a premium agentic experience powered by Anthropic's Claude.

**Why use it?**  
Claude offers strong reasoning capabilities for complex, multi-step software engineering tasks. The tight VS Code integration means you get the full agent loop (planning, tool use, file edits) directly inside the editor.

**What's new in v1.110:**
- **Steering and queuing:** Send follow-up messages mid-conversation to redirect the agent's approach, or queue additional requests while the agent is still running.
- **Session renaming:** Rename Claude agent sessions just like background agent sessions.
- **Context window rendering with compaction:** Visual indicator shows how full the context window is, with a one-click compact option.
- **Additional slash commands:**
  - `/compact` — on-demand context compaction
  - `/agents` — manage custom agents
  - `/hooks` — manage Claude hooks
- **`getDiagnostics` tool:** The agent can now read editor and workspace problem/diagnostic information.
- **Significant performance improvements.**

**How to use:**
1. Make sure you have a GitHub Copilot subscription that includes Claude models.
2. Open Chat (`Ctrl+Alt+I`) and select Claude from the model picker.
3. Switch to **Agent mode** and start a task.
4. Use `/compact`, `/agents`, and `/hooks` slash commands as needed.
5. Send a steering message mid-response to course-correct the agent without starting over.

---

### 1.3 Agent Debug Panel (Preview)

**What is it?**  
A new panel that gives you real-time, deep visibility into everything happening inside a chat agent session — which prompt files loaded, which tools were called, system prompts, and more.

**Why use it?**  
When agents behave unexpectedly, it's hard to know _why_. Did the right skill load? Did the hook fire? Was the correct prompt injected? The Agent Debug Panel answers all of this, replacing the old Diagnostics chat action with a far richer view.

**How to use:**
1. Open the **Command Palette** (`Ctrl+Shift+P`) and run `Developer: Open Agent Debug Panel`.
2. Alternatively, click the **gear icon** at the top of the Chat view and choose **View Agent Logs**.
3. Watch chat events stream in real time: loaded customizations, system prompts, tool calls, etc.
4. Switch to the **chart view** for a visual hierarchy/flowchart of the session's event sequence.

> **Note:** Currently available for local chat sessions only. Logs are not persisted — they only exist for the current VS Code session.

---

### 1.4 Slash Commands for Auto Approval

**What is it?**  
Toggle global auto-approve (previously called "YOLO mode") directly from the chat input, without going to Settings.

**Why use it?**  
For long, trusted multi-step agent tasks, you may want to skip individual tool confirmation prompts for speed. These slash commands make toggling much faster.

**Commands:**
| Command | Alias | Effect |
|---|---|---|
| `/autoApprove` | `/yolo` | Enables global auto-approve for ALL tools |
| `/disableAutoApprove` | `/disableYolo` | Disables global auto-approve |

**How to use:**
1. In the chat input field, type `/autoApprove` (or `/yolo`) and press Enter.
2. The agent will now run all tools without asking for confirmation.
3. Type `/disableAutoApprove` when done.

> **CAUTION:** This skips all tool confirmation prompts, including terminal commands that could be destructive. Understand the security implications before enabling. Consider also enabling **terminal sandboxing** (see Section 10.4) for protection.

---

### 1.5 Edit and Ask Mode Changes

**What is it?**  
Edit mode is now hidden by default, and Ask mode is now backed by a custom agent definition — making both fully agentic.

**Why does it matter?**  
Agent mode can now do everything Edit mode does, and more — with better performance. This simplifies the mode picker and prevents confusion. Ask mode no longer requires a new session when switching from Agent mode.

**How to use:**
- **Edit mode** is hidden by default. To restore it, disable the `chat.editMode.hidden` setting (`File > Preferences > Settings`, search for `chat.editMode.hidden`).
- To see the agent definition powering Edit mode: click the agent picker, select **View edit agent** to see the underlying `.agent.md` definition, which you can use as a template for your own custom agent.
- Create custom agents by following the [custom agents documentation](https://code.visualstudio.com/docs/copilot/customization/custom-agents).

> **Deprecation Notice:** Edit Mode is officially deprecated as of v1.110. It will be fully removed in v1.125.

---

### 1.6 Ask Questions Tool

**What is it?**  
The `askQuestions` tool presents an interactive question carousel UI during chat sessions, now built into VS Code core.

**Why use it?**  
Agents sometimes need clarification before proceeding. This tool surfaces structured questions in a clean UI. Moving it into VS Code core improves reliability (especially for cancellation) and makes it work consistently in subagents too.

**How to use:**
- When an agent presents a question carousel, use the keyboard:
  - `Alt+N` — next question
  - `Alt+P` — previous question
- You can send a **steering message** while the carousel is active without having to dismiss it first — this lets you redirect the agent on the fly.

---

### 1.7 Prevent Auto-Suspend During Chat

**What is it?**  
VS Code now tells the OS not to automatically suspend (sleep) the machine while a chat request is actively running.

**Why use it?**  
If you step away mid-task, your machine won't go to sleep and interrupt the agent. Long-running tasks complete reliably.

**How to use:**  
Nothing to configure — this is automatic. Just start an agent task and step away.

> **Note:** Closing a laptop lid while unplugged will still trigger suspension.

---

## 2. Agent Extensibility

### 2.1 Agent Plugins (Experimental)

**What is it?**  
Agent plugins are prepackaged bundles of chat customizations — skills, commands, agents, MCP servers, and hooks — that you can install directly from the Extensions view.

**Why use it?**  
Instead of manually creating and wiring up skills, hooks, and agents, you can install a plugin that packages everything together. This dramatically lowers the bar for advanced agent customization.

**How to use:**
1. Open the Extensions view (`Ctrl+Shift+X`).
2. Type `@agentPlugins` in the search box, OR open the Command Palette (`Ctrl+Shift+P`) and run `Chat: Plugins`.
3. Browse and install plugins.
4. Configure plugin sources in Settings:
   - `chat.plugins.marketplaces` — add GitHub repos or Claude-style marketplaces (e.g., `anthropics/claude-code`)
   - `chat.plugins.paths` — register local plugin directories

> **Enable the feature:** Set `chat.plugins.enabled` to `true` in Settings.

---

### 2.2 Agentic Browser Tools (Experimental)

**What is it?**  
A set of tools that let a Copilot agent autonomously drive the integrated browser — navigating pages, reading content, clicking elements, and running Playwright code — to verify web app changes as it builds them.

**Why use it?**  
This closes the development loop for web apps: the agent can write a change, open the browser, visually check the result, see console errors, and iterate — all without human intervention.

**Available tools:**
| Category | Tools |
|---|---|
| Navigation | `openBrowserPage`, `navigatePage` |
| Content/Screenshot | `readPage`, `screenshotPage` |
| User Interaction | `clickElement`, `hoverElement`, `dragElement`, `typeInPage`, `handleDialog` |
| Custom Automation | `runPlaywrightCode` |

**How to use:**
1. Enable the setting `workbench.browser.enableChatTools`.
2. In the Chat view, open the **tools picker** and enable the browser tools.
3. Ask the agent to build and verify a UI change — it will use the browser autonomously.
4. To give the agent access to a specific page with saved data, explicitly share that page/tab from the browser.

> See the [browser agent testing guide](https://code.visualstudio.com/docs/copilot/guides/browser-agent-testing-guide) for a step-by-step tutorial.

---

### 2.3 Create Agent Customizations from Chat

**What is it?**  
New `/create-*` slash commands let you generate agent customization files — prompts, instructions, skills, agents, hooks — directly from a chat conversation.

**Why use it?**  
Instead of writing customization files from scratch, you can have the agent extract patterns from your current conversation and save them as reusable files. Great for turning debugging workflows into documented skills, or conversation corrections into project instructions.

**Slash commands:**
| Command | Creates |
|---|---|
| `/create-prompt` | A reusable prompt file |
| `/create-instruction` | An instruction file for project conventions |
| `/create-skill` | A multi-step skill package from a workflow |
| `/create-agent` | A specialized custom agent persona |
| `/create-hook` | A hook configuration for lifecycle automation |

**How to use:**
1. In Agent mode chat, type a `/create-*` command (e.g., `/create-skill`).
2. The agent guides you through the process, including choosing user-level (account-wide) or workspace-level storage.
3. Alternatively, use natural language: _"save this workflow as a skill"_ or _"extract an instruction from this"_ — the agent will recognize your intent.
4. The same options are available from the sparkle icon (✨) in quick pick menus for prompts, instructions, skills, and agents.

---

### 2.4 Tools for Usages and Rename

**What is it?**  
Updated `usages` tool and a new `rename` tool for agents to perform precise code navigation and symbol renaming using IDE/LSP capabilities — not grep.

**Why use it?**  
Using language-aware rename/find-usages is far more accurate than text search. The agent can now safely rename symbols across an entire codebase with full IDE intelligence (respecting scope, imports, references).

**How to use:**
- Agents pick up these tools automatically, but since they sometimes prefer grep, explicitly mention the tools:
  - `Use #rename to change the name of fib to fibonacci`
  - `Use #usages to find all callers of the processPayment function`
- For consistent behavior, add instructions to your `SKILL.md` or `.instructions.md` to prefer these tools for rename/find-usages tasks.

---

## 3. Smarter Sessions

### 3.1 Session Memory for Plans

**What is it?**  
Plans created by the Plan agent now persist to session memory and remain available across all turns in a conversation — even after context compaction.

**Why use it?**  
Without this, long coding sessions lose track of early plans when context is compacted. Now the agent always builds on the existing plan, delivering more coherent, incremental results.

**How to use:**
- Ask the agent to create a plan: _"Create a plan to refactor our authentication module."_
- Continue working; the plan is recalled automatically even after unrelated turns.
- Ask for refinements: _"Update the plan to also handle OAuth."_ — the agent amends rather than replaces.
- No setup needed — this is automatic for the Plan agent.

---

### 3.2 Context Compaction

**What is it?**  
As a conversation grows, it can fill the model's context window. Context compaction summarizes conversation history to free up space, letting you continue in the same session without losing key decisions.

**Why use it?**  
Long coding sessions with lots of back-and-forth were previously limited by the context window. Now you can work in a single session indefinitely, with the agent continuing to have access to the most important context.

**How to use (manual compaction):**

**Option 1 — Slash command:**
```
/compact
```
With optional guidance:
```
/compact focus on the database schema decisions and API contracts
```

**Option 2 — Context window control:**
1. Look for the context window indicator in the chat input box.
2. Click it and select **Compact Conversation**.

> Compaction is also available for background agents and Claude agents.

---

### 3.3 Codebase Search with Explore Subagent

**What is it?**  
The Plan agent now always delegates codebase research to a dedicated **Explore subagent** — a read-only agent that uses only search and file-read tools, optimized for fast, parallelized codebase exploration.

**Why use it?**  
Separating exploration from planning means: faster research (Explore uses fast models like Claude Haiku 4.5, Gemini 3 Flash), and better plans (the Plan agent receives specific file paths and code references rather than general descriptions).

**How to use:**
- It's automatic — the Plan agent delegates to Explore behind the scenes.
- Hover over the explore task in chat to see which model is being used for research.
- Override the Explore model with the setting `chat.exploreAgent.defaultModel`.

> Explore is not directly invokable — it only runs as a subagent of the Plan agent.

---

### 3.4 Inline Chat and Chat Session

**What is it?**  
When an agent session has already modified a file, **inline chat** now automatically queues new messages into that same session rather than making isolated changes.

**Why use it?**  
This ensures inline chat edits are made with full context of prior agent changes — preventing conflicts and making it much easier to review agent edits in context.

**How to use:**
- Use inline chat (`Ctrl+I`) as normal in a file that the agent has already edited.
- VS Code automatically routes it through the existing agent session.
- No configuration needed.

---

### 3.5 Fork a Chat Session

**What is it?**  
Create a new, independent chat session that inherits the conversation history from an existing session — like Git branching for conversations.

**Why use it?**  
When you want to explore an alternative approach, ask a tangential question, or try a different solution path without losing or polluting your main conversation.

**How to use:**

**Fork the entire session:**
```
/fork
```
A new session opens with the full conversation history.

**Fork from a specific point:**
1. Hover over any previous chat request in the conversation.
2. Click the **Fork Conversation** button that appears.
3. A new session opens with history up to that checkpoint.

> Changes in forked sessions are fully independent — they do not affect each other.

---

## 4. Chat Experience

### 4.1 Redesigned Model Picker

**What is it?**  
A completely redesigned language model dropdown with better organization, search, and contextual information.

**Why use it?**  
With many models available (GPT-4o, Claude, Gemini, etc.), finding the right one for a task was difficult. The new picker makes it fast and informative.

**New structure:**
| Section | Contents |
|---|---|
| **Auto** | Always at the top — lets Copilot pick the best model |
| **Featured / Recent** | Up to 4 recently used models + curated picks |
| **Other models** | All remaining models (collapsible) |
| **Search box** | Filter models by name |

**How to use:**
1. Click the model name at the bottom of the Chat view.
2. Hover over any model to see its capabilities and context window size.
3. Use the search box to quickly filter by model name.
4. Models you don't have access to are shown but grayed out.

---

### 4.2 Discover Features with Contextual Tips (Experimental)

**What is it?**  
VS Code now shows contextual, personalized tips in the Chat view to help you discover features you haven't used yet.

**Why use it?**  
VS Code has many powerful AI features that are easy to miss. Tips appear proactively, are tailored to your usage patterns, and automatically disappear once you've used the suggested feature.

**Tips cover:**
- Creating custom agents, prompts, and skills
- Message queueing and steering
- Switching to better models
- Enabling experimental features like YOLO mode and custom thinking phrases

**How to use:**
1. Open a new chat session — tips appear at the top.
2. Use the navigation controls to browse tips, or dismiss ones you're not interested in.
3. To disable tips entirely: set `chat.tips.enabled` to `false`.

---

### 4.3 Custom Thinking Phrases

**What is it?**  
Customize the loading text shown while the agent is reasoning or calling tools.

**Why use it?**  
Purely for fun/personalization — replace the default "Thinking..." spinner text with something more meaningful or entertaining for your team.

**How to use:**

Open `settings.json` (`Ctrl+Shift+P` → "Open User Settings JSON") and add:

```json
"chat.agent.thinking.phrases": {
  "mode": "replace",
  "phrases": [
    "Bribing the hamster",
    "Reticulating splines",
    "Untangling the spaghetti"
  ]
}
```

- `"mode": "replace"` — replaces all default phrases with yours.
- `"mode": "append"` — adds your phrases to the existing defaults.

---

### 4.4 Collapsible Terminal Tool Calls

**What is it?**  
Terminal output from agent tool calls is now displayed as collapsible sections in chat — a summary header you can expand to reveal the full output.

**Why use it?**  
Long terminal outputs (e.g., `npm install`, test runs, builds) used to flood the chat and make it hard to scan multi-step interactions. This keeps the chat clean while still giving you full access to output.

**How to use:**
- It's on by default. Terminal calls appear as collapsed summaries.
- Click the summary header to expand the full output.
- To disable: set `chat.tools.terminal.simpleCollapsible` to `false`.

---

### 4.5 OS Notifications for Chat Responses

**What is it?**  
Configure VS Code to show OS-level notifications for chat events even when the VS Code window is in focus.

**Why use it?**  
Previously, notifications only appeared when VS Code was unfocused. If you were working in another VS Code file while the agent ran, you might miss when it finished — or when it needed your approval to proceed.

**How to use:**

In Settings, set these to `"always"`:
- `chat.notifyWindowOnResponseReceived` — notifies when a chat response is received
- `chat.notifyWindowOnConfirmation` — notifies when the agent needs your confirmation

```json
"chat.notifyWindowOnResponseReceived": "always",
"chat.notifyWindowOnConfirmation": "always"
```

---

### 4.6 Inline Chat Hover Mode

**What is it?**  
A new rendering mode for inline chat where the input box hovers over the code (like the rename experience), and progress/results appear in the upper-right corner instead of between lines.

**Why use it?**  
The traditional "inline between lines" mode can push code around and feel disruptive. Hover mode is less intrusive for quick edits.

**How to use:**
1. Open Settings and search for `inlineChat.renderMode`.
2. Set it to `hover`.
3. Trigger inline chat (`Ctrl+I`) — the input appears as a floating overlay.
4. Press `Escape` to dismiss without applying.

---

### 4.7 Inline Chat Affordance

**What is it?**  
A small menu that appears alongside your text selection, providing quick access to start inline chat — similar to a Quick Action button.

**Why use it?**  
Reduces friction for starting inline chat — no need to remember the keyboard shortcut. The affordance appears right where you've selected code.

**How to use:**

Set `inlineChat.affordance` in Settings:

| Value | Behavior |
|---|---|
| `off` | No affordance shown (default) |
| `editor` | Menu appears beside your selection in the editor |
| `gutter` | Menu appears in the gutter (line number area) |

---

## 5. Accessibility

VS Code v1.110 delivers the most comprehensive set of accessibility improvements for AI features to date.

### 5.1 Toggle Thinking Content — `Alt+T`
In the accessible chat view (screen reader mode), press `Alt+T` to toggle whether the model's reasoning/thinking content is included when reading responses. Focus on the final output or follow the full chain of thought.

### 5.2 Question Carousel Accessibility
- Questions announced with position: _"Question 1 of 3"_
- `Alt+N` / `Alt+P` to navigate between questions
- `Ctrl+Shift+A` to toggle focus between carousel and chat input
- Focus no longer auto-moves in screen reader mode

### 5.3 Notifications for Chat Questions
- Set `chat.notifyWindowOnConfirmation` to receive OS notifications when chat requires your input.
- Set `accessibility.signals.chatUserActionRequired` to play an audio signal.

### 5.4 Keybinding to Toggle TODO List Focus — `Ctrl+Shift+T`
Quickly toggle focus between the agent TODO list and chat input. Useful for screen readers to get a task overview without losing chat input context.

### 5.5 Cursor Position Preserved in Accessible View
When you close the accessible view while content is streaming, reopening it returns your cursor to where you left off — no more jumping back to the top.

### 5.6 Find/Filter Accessibility Help — `Alt+F1`
Press `Alt+F1` in any find or filter dialog for contextual keyboard shortcut help. Available in:
- Editor find and replace
- Terminal find
- Search across files
- Output, Problems, Debug Console filters

### 5.7 Quick Input Screen Reader Improvements
- `Ctrl+G` (Go to Line) and other quick inputs now announce characters as you type.
- Arrow key navigation works correctly.
- Line and column position announced after navigation.

### 5.8 Accessibility Skill
A new built-in accessibility skill helps the agent follow accessibility guidelines when you ask it to build new features. Just ask: _"Create this UI component and make it accessible."_

### 5.9 Checkmarks in Chat
Checkmarks before tool calls and collapsible items are now hidden by default for a cleaner view. To restore them: set `accessibility.chat.showCheckmarks` to `true`.

---

## 6. Editor Experience

### 6.1 Modal Editors (Experimental)

**What is it?**  
A new "floating modal" editor experience for utility editors (Settings, Keyboard Shortcuts, Profiles, etc.) that opens them as overlays instead of new tabs — similar to a modal dialog.

**Why use it?**  
These editors are typically opened briefly, checked, then closed. Opening them in tabs clutters the tab bar and disrupts your layout. Modal editors float on top and close with `Escape`, keeping your workspace clean.

**Applies to:**
- Settings Editor
- Keyboard Shortcuts
- Profiles Management
- AI and Language Models Management
- Workspace Trust Management

**How to use:**
1. Open Settings (`Ctrl+,`), search for `workbench.editor.useModal`.
2. Set it to `some` to enable modal editors for the listed editors.
3. Press `Escape` to close the modal, or use the action button to move it back to a tab.
4. For Extensions modal support, also enable `extensions.allowOpenInModalEditor`.

---

### 6.2 Configurable Notification Position

**What is it?**  
You can now choose where VS Code notification toasts appear on screen.

**Why use it?**  
The default bottom-right position overlaps the Chat view for many users. Moving notifications to a different corner avoids this conflict.

**How to use:**

Set `workbench.notifications.position` in Settings to one of:
- `bottom-right` (default)
- `bottom-left`
- `top-right`

---

### 6.3 Settings Editor Cleanup

**What is it?**  
VS Code chat settings now have their own top-level category in the Settings editor with subcategories for easier navigation.

**Improvements:**
- Chat settings are organized under a dedicated section.
- Settings are scoped to the selected table-of-contents entry — no accidental scrolling into adjacent sections.
- Experimental settings moved to the end of each section so stable settings appear first.

**How to use:**
- Open Settings (`Ctrl+,`) and look for the new **Chat** entry in the left sidebar.

---

## 7. Code Editing

### 7.1 Long-Distance Next Edit Suggestions

**What is it?**  
Next Edit Suggestions (NES) now suggests edits anywhere in your file — not just near your cursor. VS Code predicts what changes you'd need to make elsewhere as a consequence of your current edit.

**Why use it?**  
When you rename a variable or change a function signature at the top of a file, NES can now suggest the corresponding changes at the bottom of the file — or anywhere else. It behaves like a context-aware "also change this" assistant.

**How to use:**
1. Enable NES: set `github.copilot.nextEditSuggestions.enabled` to `true`.
2. Enable extended range: set `github.copilot.nextEditSuggestions.extendedRange` to `true`.
3. Make an edit — NES suggestions will appear as ghost text, potentially in distant parts of the file.
4. Press `Tab` to accept, or `Escape` to dismiss.

---

### 7.2 NES Eagerness Control

**What is it?**  
An eagerness option in the Copilot Status Bar item that lets you control how frequently NES offers suggestions.

**Why use it?**  
- **Higher eagerness** = more suggestions, some less relevant.
- **Lower eagerness** = fewer suggestions, higher precision.
Match your preference to your workflow (exploratory vs. focused coding).

**How to use:**
1. Click the **Copilot icon** in the status bar (bottom right).
2. Find the **NES Eagerness** option in the menu.
3. Adjust the slider/option to your preference.

---

## 8. Source Control

### 8.1 AI Co-Author Attribution for Commits

**What is it?**  
VS Code can automatically append a `Co-authored-by:` trailer to commits that include AI-generated code, attributing the AI contribution in the Git history.

**Why use it?**  
For transparency, compliance, and team awareness, it's valuable to know which commits contain AI-generated code. This also shows in Git blame hover tooltips.

**How to use:**

Set `git.addAICoAuthor` in Settings:

| Value | Behavior |
|---|---|
| `off` (default) | No co-author trailer |
| `chatAndAgent` | Adds trailer for code from Chat / Agent mode |
| `all` | Adds trailer for all AI-generated code including inline completions |

**Example commit message result:**
```
feat: add user authentication

Co-authored-by: GitHub Copilot <copilot@github.com>
```

> Only applies to commits made from within VS Code. External Git tools are not affected.

---

## 9. Debugging

### 9.1 Custom Property Replacements (JS Debugger)

**What is it?**  
JavaScript objects can now define a `Symbol.for('debug.properties')` method. When defined, the debugger displays those custom properties by default instead of the raw object properties.

**Why use it?**  
Complex objects (like custom collections, state machines, or domain models) are much easier to inspect when they show domain-relevant properties instead of raw internal state.

**How to use:**

Add this to your JavaScript/TypeScript class:
```js
class MyModel {
  constructor(data) {
    this._raw = data;
  }

  [Symbol.for('debug.properties')]() {
    return {
      id: this._raw.id,
      status: this._raw.metadata.status,
      itemCount: this._raw.items.length
    };
  }
}
```

In the debugger, `MyModel` instances will show `id`, `status`, and `itemCount`. The raw `_raw` property is folded under a `...` item.

---

### 9.2 Emulate Focused Window

**What is it?**  
A new option in the renamed **Browser Options** view (previously "Event Listener Breakpoints") to emulate a focused browser page during debugging.

**Why use it?**  
When debugging focus-dependent code (hover states, tooltips, focus traps), clicking into DevTools or VS Code would blur the browser element and hide the state you wanted to inspect. This option prevents that.

**How to use:**
1. Open the **Run and Debug** view (`Ctrl+Shift+D`).
2. Find the **Browser Options** section in the sidebar.
3. Check **Emulate Focused Page**.
4. Now clicking outside the browser window no longer removes focus from browser elements.

---

## 10. Terminal

### 10.1 Kitty Graphics Protocol

**What is it?**  
The VS Code terminal now supports the [Kitty graphics protocol](https://sw.kovidgoyal.net/kitty/graphics-protocol/), enabling high-fidelity image rendering directly in the integrated terminal.

**Why use it?**  
Visualize data, images, plots, and diagrams directly in the terminal — essential for data science workflows, CLI tools that render images, and rich terminal UIs.

**Supported capabilities:**
- PNG, 24-bit RGB, and 32-bit RGBA image formats
- Scaling to specific column/row dimensions, source cropping, z-index stacking
- Chunked base64 transmission with zlib compression
- Image management: store, display later, delete by ID, update existing images
- Images scroll with terminal text; cleaned up on reset/clear

**How to enable:**
```json
"terminal.integrated.enableImages": true,
"terminal.integrated.gpuAcceleration": "on"
```
On Windows, also enable:
```json
"terminal.integrated.windowsUseConptyDll": true
```

**To display images:**
- macOS/Linux: use `kitten icat image.png`
- Any OS: use the [VT CLI](https://github.com/xtermjs/vtc)

> **Not yet supported:** animations, relative placements, Unicode placeholders, file-based transmission.

---

### 10.2 Ghostty Support for External Terminal

**What is it?**  
[Ghostty](https://ghostty.org/), a modern GPU-accelerated terminal, is now supported as an external terminal on macOS and Linux.

**Why use it?**  
Ghostty offers excellent performance and rendering quality. If it's your default terminal, you can now open it directly from VS Code for external terminal launches.

**How to configure:**

**macOS:**
```json
"terminal.external.osxExec": "Ghostty.app"
```

**Linux:**
```json
"terminal.external.linuxExec": "ghostty"
```

Once set, the commands **Terminal: Open New External Terminal** and debug configurations that launch external terminals will open in Ghostty.

---

### 10.3 Workspace Folder Selection for External Terminals

**What is it?**  
In a multi-root workspace, opening an external terminal now prompts you to select which workspace folder to use as the working directory.

**Why use it?**  
In multi-root workspaces, it was unclear which folder's context the external terminal would open in. Now you control it explicitly.

**How to use:**
1. Use `Ctrl+Shift+C` or run **Terminal: Open New External Terminal** from the Command Palette.
2. A folder picker appears — select the desired workspace root.
3. The external terminal opens in that folder.

---

### 10.4 Terminal Sandboxing Improvements (Preview)

**What is it?**  
Enhanced isolation for terminals created by agents — restricting file system access and network access to protect your system from potentially dangerous agent commands.

**Why use it?**  
When using `/autoApprove` or long-running agents, you want confidence that the agent can't accidentally access files outside the project or make unexpected network requests.

**What's new:**
- **Trusted domain network isolation:** Set `allowTrustedDomains` in `chat.tools.terminal.sandbox.network` to allow only specific domains.
- **Improved restricted domain detection** with clear feedback about which domain is blocked.
- **macOS:** No installation required to enable sandboxing.
- **Linux:** Can enable without installing ripgrep.

**How to enable:**
```json
"chat.tools.terminal.sandbox.enabled": true
```

---

## 11. Languages

### 11.1 Unified JavaScript and TypeScript Settings

**What is it?**  
All built-in JavaScript and TypeScript settings have been consolidated under the new `js/ts.*` prefix, replacing the previous duplicate `javascript.*` / `typescript.*` settings.

**Why use it?**  
Previously, changing a formatting or linting behavior required setting it twice — once for JS and once for TS. The unified prefix means one setting change applies to both by default, with language-specific overrides available when needed.

**Migration example:**

_Before:_
```json
"javascript.format.enable": false,
"typescript.format.enable": true
```

_After (with language-specific override):_
```json
"[javascript][javascriptreact]": {
    "js/ts.format.enabled": false
},
"[typescript][typescriptreact]": {
    "js/ts.format.enabled": true
}
```

_Or, to apply to both at once:_
```json
"js/ts.format.enabled": false
```

> **Compatibility:** Old `javascript.*` and `typescript.*` settings continue to work but are now **deprecated**. They will be overridden if the new unified `js/ts.*` settings are set. Migrate gradually.

---

### 11.2 Python Environments Extension

**What is it?**  
The [Python Environments extension](https://marketplace.visualstudio.com/items?itemName=ms-python.vscode-python-envs) is now rolling out to all users after a year in preview. It provides a unified interface for managing Python environments, packages, and interpreters inside VS Code.

**Why use it?**  
Managing virtual environments, conda environments, poetry projects, etc., was fragmented. This extension unifies everything in one view with smart features.

**Key capabilities:**
| Feature | Description |
|---|---|
| **Quick Create** | Create an env with one click using your default manager and latest Python |
| **Python Projects** | Assign environments to specific folders (monorepo support) |
| **uv integration** | Much faster environment creation and package installation |
| **Package management** | Search, install, and uninstall packages from the Environment Managers view |
| **Portable settings** | Uses manager types (not hardcoded paths) — `settings.json` works across machines |

**How to use:**
1. The extension will auto-enable over the coming weeks, OR:
2. Opt in immediately: set `python.useEnvsExtension` to `true`.
3. Open the **Environment Managers** view in the Activity Bar to manage envs and packages.

---

## 12. Extension Authoring

### 12.1 ThemeIcons for Webview/Custom Editor Tab Icons
Extensions can now use `ThemeIcon` for WebviewPanel and custom editor tab icons:

```typescript
webviewPanel.iconPath = new vscode.ThemeIcon('octoface');
```

This means the tab icon automatically adapts to the user's color theme.

### 12.2 Portable Mode Detection API (Stable)
The `env.isAppPortable` API is now stable and available without `enabledApiProposals`:

```typescript
if (vscode.env.isAppPortable) {
  // VS Code is running in portable mode
  // Adjust paths, storage, etc. accordingly
}
```

---

## 13. Deprecated Features

### Edit Mode Deprecation
Edit Mode is officially deprecated as of v1.110.

| Timeline | Status |
|---|---|
| v1.110 (now) | Edit Mode hidden by default; re-enable via `chat.editMode.hidden` |
| v1.125 (future) | Edit Mode fully removed; setting no longer functional |

**Recommendation:** Migrate to Agent mode. If you liked Edit mode's constrained behavior, create a [custom agent](https://code.visualstudio.com/docs/copilot/customization/custom-agents) that mimics it — you can inspect the current Edit mode agent definition by disabling `chat.editMode.hidden` and selecting **View edit agent** in the picker.

---

## Quick Settings Reference

| Feature | Setting | Recommended Value |
|---|---|---|
| Background agent context compaction | automatic | — |
| Auto-approve agents | `/autoApprove` slash command | use when needed |
| Edit mode visibility | `chat.editMode.hidden` | `true` (default) |
| Agent plugins | `chat.plugins.enabled` | `true` |
| Browser agent tools | `workbench.browser.enableChatTools` | `true` |
| Contextual tips | `chat.tips.enabled` | `true` |
| Custom thinking phrases | `chat.agent.thinking.phrases` | customizable |
| Collapsible terminal output | `chat.tools.terminal.simpleCollapsible` | `true` (default) |
| OS notification on response | `chat.notifyWindowOnResponseReceived` | `"always"` |
| OS notification on confirmation | `chat.notifyWindowOnConfirmation` | `"always"` |
| Inline chat render mode | `inlineChat.renderMode` | `hover` |
| Inline chat affordance | `inlineChat.affordance` | `editor` or `gutter` |
| Modal editors | `workbench.editor.useModal` | `some` |
| Notification position | `workbench.notifications.position` | `top-right` |
| NES (Next Edit Suggestions) | `github.copilot.nextEditSuggestions.enabled` | `true` |
| NES extended range | `github.copilot.nextEditSuggestions.extendedRange` | `true` |
| AI co-author in commits | `git.addAICoAuthor` | `chatAndAgent` |
| Terminal images | `terminal.integrated.enableImages` | `true` |
| Terminal GPU acceleration | `terminal.integrated.gpuAcceleration` | `on` |
| Terminal sandboxing | `chat.tools.terminal.sandbox.enabled` | `true` |
| Python Environments extension | `python.useEnvsExtension` | `true` |
| Unified JS/TS settings | use `js/ts.*` prefix | migrate from `javascript.*`/`typescript.*` |
| Explore subagent model | `chat.exploreAgent.defaultModel` | fast model |
| Checkmarks in chat | `accessibility.chat.showCheckmarks` | per preference |

---

*Generated from [VS Code v1.110 Release Notes](https://code.visualstudio.com/updates/v1_110) — February 2026 release (March 4, 2026)*
