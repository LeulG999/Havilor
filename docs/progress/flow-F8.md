# Flow F8: Investigation tab (work the playbook steps)

**Goal:** the analyst works through the playbook right on the page: answer each pending step and mark it DONE or
SKIPPED.
**New idea:** a small form per list item, with its own state.

**Built on:** `main` · **Status:** done, `npm run build` passes, pushed

## How it works

```mermaid
sequenceDiagram
    participant U as Analyst
    participant F as StepForm (one per PENDING step)
    participant A as api.ts
    participant B as PATCH /api/v1/alerts/{id}/steps/{stepId}
    participant P as AlertDetailPage
    U->>F: picks "Malicious" (step has answer options) or types an answer
    U->>F: "Done" (or Enter)  /  "Skip"
    F->>A: api(..., PATCH {status: "DONE" | "SKIPPED", answer})
    A->>B:
    B-->>F: 200 · 404 unknown step · 409 closed alert
    F->>P: onChanged() = load()
    P->>U: the step now shows DONE + the answer; its form is gone; a NEW alert became INVESTIGATING
```

## Files

| File | What it does |
|---|---|
| `src/components/StepForm.tsx` (new) | Answer state; a `<select>` of the step's `answerOptions`, or a `<textarea>` (max 2000, like the backend); "Done" (submit, so Enter works) and "Skip"; errors shown; on success the page reloads. |
| `src/components/StepList.tsx` | Takes `alertId`, `editable` and `onChanged`; a PENDING step of an editable alert gets a `StepForm`; status badges coloured. |
| `src/pages/AlertDetailPage.tsx` | Investigation tab: `editable` = alert not CLOSED. |
| `src/api/types.ts` | `AlertStep.answerOptions`. |
| `src/index.css` | Step form, button row, DONE / SKIPPED / PENDING colours. |

## Decisions

- **Only PENDING steps get a form** (as asked). Undoing a step (back to PENDING) isn't in the UI; the backend allows
  it through the API.
- **The answer is optional**, like in the backend; the options list starts with "— choose an answer —".
- **One form per step, each with its own state**, so typing in one step never touches another.

## How to verify

Open a phishing alert → Investigation: each step shows a form. Pick an answer and "Done" → the step shows DONE and the
answer, and the alert's status in the header becomes INVESTIGATING if it was NEW. "Skip" marks it SKIPPED.

## New concepts introduced

- **State per list item** (a component per row).
- **Form submit vs button click** (Enter submits; `type="button"` doesn't).
