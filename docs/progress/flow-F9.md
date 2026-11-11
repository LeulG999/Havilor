# Flow F9: Evidence and Actions tabs (add finding, add action)

**Goal:** the analyst records what she found (a manual finding) and what she did (an action), each through a small
form in a modal; the Actions tab lists every action.
**New idea:** forms inside the reusable Modal, with the error shown where the user can fix it.

**Built on:** `main` · **Status:** done, `npm run build` passes, pushed

## How it works

```mermaid
sequenceDiagram
    participant U as Analyst
    participant B as AddFindingButton / AddActionButton
    participant M as Modal (<dialog>)
    participant A as api.ts
    participant S as Backend
    participant P as AlertDetailPage
    U->>B: "Add finding"
    B->>B: reset the form fields, open = true
    B->>M: showModal()
    U->>M: fills type, severity, title, why it matters → submit
    B->>A: api('/alerts/42/findings', POST {...})
    A->>S: POST /api/v1/alerts/42/findings
    alt 201
        B->>M: open = false (closes)
        B->>P: onChanged() = load()   (alert, findings, steps, actions)
    else 400 / 409
        S-->>B: {"message": "..."}
        B->>M: error shown inside the modal; the form keeps what was typed
    end
```

The Actions tab works the same way with `POST /api/v1/alerts/{id}/actions` (`type`, `result`, `note`); `performedBy`
and `performedAt` are set by the backend.

## Files

| File | What it does |
|---|---|
| `src/components/AddFindingButton.tsx` (new) | Button + Modal + form: type, severity, title (required, ≤ 300), "why this matters" (optional). |
| `src/components/AddActionButton.tsx` (new) | Button + Modal + form: type, result, note (optional, ≤ 2000). |
| `src/components/ActionList.tsx` (new) | Table: action, result, note, by (`User #id`), when. |
| `src/pages/AlertDetailPage.tsx` | `load()` also fetches `/actions`; Evidence tab has "Add finding", Actions tab has "Add action" + the list; `alertIsOpen` hides the buttons on a closed alert. |
| `src/api/types.ts` | `Action`. |

## Decisions

- **Each "Add …" component owns its button, modal and form state.** The page only says where it goes and what to do
  after a change.
- **The form is reset when the modal opens**, and keeps its values after a failed save so nothing typed is lost.
- **Errors appear inside the modal** (the backend's message), not behind it on the page.
- **Hidden on closed alerts.** For actions the backend refuses anyway (409); for findings it would accept them, but a
  closed case is final in the UI (F11 makes this the rule for every action button).
- **"By" shows `User #id`:** there is no "user by id" endpoint for the front end.

## How to verify

Evidence → "Add finding" → type OTHER, severity LOW, a title → it appears in Findings as MANUAL. Clear the title and
submit: the browser asks for it. Actions → "Add action" → BLOCK_SENDER / DONE → it appears in the table with you and
the time.

## New concepts introduced

- **Forms in modals**, and where to show errors.
- **Components that own their UI state** (open/closed, field values) while the page owns the data.
