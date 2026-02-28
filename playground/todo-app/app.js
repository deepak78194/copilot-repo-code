/**
 * Todo App — vanilla ES2023, no external dependencies.
 *
 * Features:
 *  - Add, delete, toggle-complete tasks
 *  - Edit task text in-place (double-click)
 *  - Filter: All / Active / Completed
 *  - Clear all completed at once
 *  - Drag-and-drop to reorder
 *  - Persist to localStorage
 */

'use strict';

/* ── Constants ──────────────────────────────────────────────── */
const STORAGE_KEY = 'copilot-todo';

/* ── State ──────────────────────────────────────────────────── */
const state = {
  /** @type {{ id: string, text: string, done: boolean }[]} */
  tasks: [],
  /** @type {'all' | 'active' | 'completed'} */
  filter: 'all',
};

/* ── Persistence helpers ────────────────────────────────────── */
function loadFromStorage() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return;
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed.tasks)) {
      state.tasks = parsed.tasks;
    }
    if (['all', 'active', 'completed'].includes(parsed.filter)) {
      state.filter = parsed.filter;
    }
  } catch {
    // Corrupt storage — start fresh
    localStorage.removeItem(STORAGE_KEY);
  }
}

function saveToStorage() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify({ tasks: state.tasks, filter: state.filter }));
}

/* ── ID generator ───────────────────────────────────────────── */
function uid() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

/* ── Derived data ───────────────────────────────────────────── */
function visibleTasks() {
  switch (state.filter) {
    case 'active':
      return state.tasks.filter((t) => !t.done);
    case 'completed':
      return state.tasks.filter((t) => t.done);
    default:
      return state.tasks;
  }
}

function activeTotalCount() {
  return state.tasks.filter((t) => !t.done).length;
}

/* ── DOM refs ───────────────────────────────────────────────── */
const addForm        = /** @type {HTMLFormElement}      */ (document.getElementById('add-form'));
const taskInput      = /** @type {HTMLInputElement}     */ (document.getElementById('task-input'));
const todoList       = /** @type {HTMLUListElement}     */ (document.getElementById('todo-list'));
const emptyState     = /** @type {HTMLParagraphElement} */ (document.getElementById('empty-state'));
const taskCountEl    = /** @type {HTMLSpanElement}      */ (document.getElementById('task-count'));
const appFooter      = /** @type {HTMLElement}          */ (document.getElementById('app-footer'));
const clearCompleted = /** @type {HTMLButtonElement}    */ (document.getElementById('clear-completed'));

/* ── Render ──────────────────────────────────────────────────── */
function render() {
  const visible = visibleTasks();

  // Empty state
  emptyState.hidden = visible.length > 0;

  // Footer visibility
  appFooter.style.display = state.tasks.length === 0 ? 'none' : '';

  // Task list
  todoList.innerHTML = '';
  visible.forEach((task) => {
    todoList.appendChild(buildTaskItem(task));
  });

  // Counter
  const n = activeTotalCount();
  taskCountEl.textContent = `${n} task${n !== 1 ? 's' : ''} left`;

  // Filter buttons
  document.querySelectorAll('.filter-btn').forEach((btn) => {
    btn.classList.toggle('active', btn.dataset.filter === state.filter);
  });
}

/* ── Build a single <li> task item ──────────────────────────── */
function buildTaskItem(task) {
  const li = document.createElement('li');
  li.className = `todo-item${task.done ? ' completed' : ''}`;
  li.dataset.id = task.id;
  li.setAttribute('draggable', 'true');

  // Drag handle
  const handle = document.createElement('span');
  handle.className = 'drag-handle';
  handle.setAttribute('aria-hidden', 'true');
  handle.textContent = '⠿';

  // Checkbox
  const checkbox = document.createElement('input');
  checkbox.type = 'checkbox';
  checkbox.className = 'task-checkbox';
  checkbox.checked = task.done;
  checkbox.setAttribute('aria-label', `Mark "${task.text}" as ${task.done ? 'incomplete' : 'complete'}`);
  checkbox.addEventListener('change', () => {
    toggleTask(task.id);
  });

  // Label
  const label = document.createElement('span');
  label.className = 'task-label';
  label.textContent = task.text;
  label.setAttribute('title', 'Double-click to edit');
  label.addEventListener('dblclick', () => {
    startEditing(li, task);
  });

  // Delete button
  const deleteBtn = document.createElement('button');
  deleteBtn.className = 'btn btn-delete';
  deleteBtn.setAttribute('aria-label', `Delete "${task.text}"`);
  deleteBtn.textContent = '✕';
  deleteBtn.addEventListener('click', () => {
    deleteTask(task.id);
  });

  // Drag events
  li.addEventListener('dragstart', onDragStart);
  li.addEventListener('dragover', onDragOver);
  li.addEventListener('dragleave', onDragLeave);
  li.addEventListener('drop', onDrop);
  li.addEventListener('dragend', onDragEnd);

  li.append(handle, checkbox, label, deleteBtn);
  return li;
}

/* ── In-place editing ────────────────────────────────────────── */
function startEditing(li, task) {
  const label = li.querySelector('.task-label');
  const editInput = document.createElement('input');
  editInput.type = 'text';
  editInput.className = 'task-edit-input';
  editInput.value = task.text;
  editInput.maxLength = 200;
  editInput.setAttribute('aria-label', 'Edit task text');

  label.replaceWith(editInput);
  editInput.focus();
  editInput.setSelectionRange(editInput.value.length, editInput.value.length);

  function commitEdit() {
    const newText = editInput.value.trim();
    if (newText && newText !== task.text) {
      const found = state.tasks.find((t) => t.id === task.id);
      if (found) found.text = newText;
      saveToStorage();
    }
    render();
  }

  function cancelEdit() {
    render();
  }

  editInput.addEventListener('blur', commitEdit);
  editInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      editInput.removeEventListener('blur', commitEdit);
      commitEdit();
    } else if (e.key === 'Escape') {
      editInput.removeEventListener('blur', commitEdit);
      cancelEdit();
    }
  });
}

/* ── Drag-and-drop ───────────────────────────────────────────── */
let dragSrcId = null;

function onDragStart(e) {
  dragSrcId = e.currentTarget.dataset.id;
  e.currentTarget.classList.add('dragging');
  e.dataTransfer.effectAllowed = 'move';
}

function onDragOver(e) {
  e.preventDefault();
  e.dataTransfer.dropEffect = 'move';
  e.currentTarget.classList.add('drag-over');
}

function onDragLeave(e) {
  e.currentTarget.classList.remove('drag-over');
}

function onDrop(e) {
  e.preventDefault();
  e.currentTarget.classList.remove('drag-over');
  const targetId = e.currentTarget.dataset.id;
  if (!dragSrcId || dragSrcId === targetId) return;

  // Reorder within the FULL state.tasks array (not just visible)
  const srcIndex    = state.tasks.findIndex((t) => t.id === dragSrcId);
  const targetIndex = state.tasks.findIndex((t) => t.id === targetId);
  if (srcIndex === -1 || targetIndex === -1) return;

  const [removed] = state.tasks.splice(srcIndex, 1);
  state.tasks.splice(targetIndex, 0, removed);
  saveToStorage();
  render();
}

function onDragEnd(e) {
  e.currentTarget.classList.remove('dragging', 'drag-over');
  dragSrcId = null;
}

/* ── Task mutations ──────────────────────────────────────────── */
function addTask(text) {
  state.tasks.push({ id: uid(), text, done: false });
  saveToStorage();
  render();
}

function deleteTask(id) {
  state.tasks = state.tasks.filter((t) => t.id !== id);
  saveToStorage();
  render();
}

function toggleTask(id) {
  const task = state.tasks.find((t) => t.id === id);
  if (task) task.done = !task.done;
  saveToStorage();
  render();
}

function clearCompletedTasks() {
  state.tasks = state.tasks.filter((t) => !t.done);
  if (state.filter === 'completed') state.filter = 'all';
  saveToStorage();
  render();
}

/* ── Event bindings ──────────────────────────────────────────── */
addForm.addEventListener('submit', (e) => {
  e.preventDefault();
  const text = taskInput.value.trim();
  if (!text) return;
  addTask(text);
  taskInput.value = '';
  taskInput.focus();
});

document.querySelector('.filter-nav').addEventListener('click', (e) => {
  const btn = e.target.closest('.filter-btn');
  if (!btn) return;
  state.filter = btn.dataset.filter;
  saveToStorage();
  render();
});

clearCompleted.addEventListener('click', clearCompletedTasks);

/* ── Init ────────────────────────────────────────────────────── */
loadFromStorage();
render();
