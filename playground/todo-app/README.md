# Todo App — Playground Experiment

A fully client-side Todo application built with **plain HTML, CSS, and vanilla JavaScript** — no build step, no dependencies.

## Purpose

This experiment explores how well GitHub Copilot can scaffold a complete, production-quality frontend application in a single session, including responsive layout, interactive UX patterns (drag-and-drop, in-place editing), and local persistence.

## Features

| Feature | Description |
|---------|-------------|
| **Add tasks** | Type a task and press Enter or click **Add** |
| **Complete / uncomplete** | Click the circle checkbox on any task |
| **Edit in-place** | Double-click a task label to edit; press Enter to save or Escape to cancel |
| **Delete** | Hover a task row and click **✕** |
| **Filter** | Switch between **All / Active / Completed** |
| **Clear completed** | Remove all done tasks in one click |
| **Drag-and-drop reorder** | Drag the ⠿ handle to rearrange tasks |
| **Persist state** | All tasks and the active filter are saved to `localStorage` |
| **Responsive** | Works on mobile (≥ 320 px), tablet, and desktop |

## Running Locally

Open `index.html` directly in any modern browser, **or** serve with any static file server:

```bash
# Python (built-in)
python -m http.server 8000

# Node / npx
npx serve .

# Bun
bunx serve .
```

Then visit [http://localhost:8000](http://localhost:8000).

## File Structure

```
playground/todo-app/
├── index.html   ← semantic HTML5 shell
├── style.css    ← mobile-first responsive styles (CSS custom properties)
├── app.js       ← vanilla ES2023 state machine + DOM renderer
└── README.md    ← this file
```

## Design Decisions

- **No framework** — proves viability without toolchain complexity; easy to evaluate Copilot outputs line by line.
- **Single `render()` function** — state is the single source of truth; every mutation calls `render()` for predictable UI.
- **CSS custom properties** — all design tokens (`--clr-*`, `--radius-*`, `--shadow`) in `:root` for easy theming.
- **No `console.log` / `println`** — follows repository code-quality rules.
