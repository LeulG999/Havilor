# Flow F17: Escalation on the alert detail page

**Goal:** the analyst hands an alert to another team (for example Incident Response) from the detail page, then
records what that team says. The alert's status follows: ESCALATED while waiting, INVESTIGATING again once the
outcome is in.
**New idea:** a list where every row has its own small form (its own state), reset with a React `key`.

**Built on:** `main` · **Status:** done, `npm run build` passes, checked against a running backend, pushed

## How it works

```mermaid
sequenceDiagram
    participant U as Analyst
    participant H as HeaderActions → EscalateButton (modal)
    participant L as Actions tab → EscalationList → EscalationForm (one per escalation)
    participant P as AlertDetailPage.load()
    participant B as Backend
    U->>H: "Escalate" → team + reason → submit
    H->>B: POST /api/v1/alerts/{id}/escalations {team, reason}
    B-->>H: 201 (alert → ESCALATED) · 400 blank fields · 409 closed alert
    H->>P: onChanged → reload alert, findings, steps, actions, escalations (Promise.all)
    U->>L: "Their status" IN_PROGRESS → Save
    L->>B: PATCH /api/v1/escalations/{eid} {externalStatus, outcome: null}
    Note over B: outcome null = unchanged; status stays ESCALATED
    U->>L: Outcome CONFIRMED_MALICIOUS → Save
    L->>B: PATCH /api/v1/escalations/{eid} {externalStatus, outcome}
    B-->>L: 200 (alert → INVESTIGATING)
    L->>P: onChanged → reload: header shows INVESTIGATING
```

## Files

| File | What it does |
|---|---|
| `src/api/types.ts` | `Escalation` (the backend's `EscalationResponse`). |
| `src/components/EscalateButton.tsx` (new) | "Escalate" button + modal (team, reason) → `POST /alerts/{id}/escalations`; backend errors shown in the modal. |
| `src/components/HeaderActions.tsx` | Shows `EscalateButton` next to "Close alert" (the header actions only exist on open alerts). |
| `src/components/EscalationList.tsx` (new) | The "Escalations" panel on the Actions tab: team, reason, their status, outcome; a form per escalation while the alert is open. |
| `src/components/EscalationForm.tsx` (new) | Two dropdowns (their status, outcome) + Save → `PATCH /escalations/{id}`. |
| `src/pages/AlertDetailPage.tsx` | Also loads `GET /alerts/{id}/escalations` in `load()`; shows `EscalationList` under the actions. |

## Checked against a running backend

Throwaway database, requests shaped like the page's: escalate → 201, alert NEW → ESCALATED; blank team and reason →
400 `Validation failed: reason must not be blank; team must not be blank`; list → the escalation; PATCH
`{externalStatus: IN_PROGRESS, outcome: null}` → 200, still ESCALATED; PATCH with outcome `CONFIRMED_MALICIOUS` →
200, alert INVESTIGATING.

## Decisions

- **"Escalate" on any open alert, also one already ESCALATED:** the backend allows a second team; only a closed alert
  is refused (409), and there the header shows no actions anyway.
- **"No answer yet" sends `outcome: null`**, which the backend reads as "don't change it", so the form can never
  erase an outcome that was already recorded.
- **`key` on each form = its saved values:** after a reload with new values the form starts again from them, instead
  of keeping old local state.
- **Reload everything after each change** (same `load()` as before), so the header status, the list and the history
  always agree with the backend.
- No forms on a closed alert: the list is read-only there.

## How to verify

Open an open alert → "Escalate" → team "Incident Response", a reason → status ESCALATED → Actions tab: the
escalation, "Their status" IN_PROGRESS → Save (still ESCALATED) → Outcome CONFIRMED_MALICIOUS → Save → the header shows
INVESTIGATING. Escalate with empty fields is blocked by the browser; the backend's 400 would show in the modal.

## New concepts introduced

- **Per-row state:** each `EscalationForm` owns its own `useState`, so rows don't interfere.
- **Resetting a component with `key`:** a new key makes React throw the old component away and start fresh.
- **Optional fields in a PATCH:** `null` = "leave it as it is".
