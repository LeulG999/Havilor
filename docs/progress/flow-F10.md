# Flow F10: AI tab (assessment and email drafts)

**Goal:** the analyst asks the AI for an assessment (summary, suggested verdict, reason, which past cases and playbook
steps it was given) and for email drafts she can copy.
**New idea:** slow requests: a clear loading state, and showing an upstream failure (502) honestly.

**Built on:** `main` · **Status:** done, `npm run build` passes, pushed

## How it works

```mermaid
sequenceDiagram
    participant U as Analyst
    participant P as AiAssessmentPanel
    participant D as DraftEmailButton (modal)
    participant S as Backend → Claude
    U->>P: "Get AI assessment"
    P->>P: loading = true → button: "Asking the AI…" (disabled)
    P->>S: POST /api/v1/alerts/42/ai/assessment
    alt 200
        S-->>P: {summary, suggestedVerdict, suggestionReason, references}
        P->>U: shows them; references link to past alerts
        P->>P: onChanged() → page reloads (the saved suggestion appears on Overview)
    else 502
        S-->>P: {"message": "The AI assistant is not configured (ANTHROPIC_API_KEY is not set)"}
        P->>U: the message in red
    end
    U->>D: "Draft email" → type → "Write draft"
    D->>S: POST /api/v1/alerts/42/ai/draft-email {type}
    S-->>D: {type, draft}
    D->>U: read-only text + "Copy" (navigator.clipboard) → "Copied ✓"
```

## Files

| File | What it does |
|---|---|
| `src/components/AiAssessmentPanel.tsx` (new) | Button with a loading label; shows summary, suggested verdict, reason and references (`CLOSED_ALERT` → link to that alert, `PLAYBOOK_STEP` → step id); reloads the page after a success; errors (e.g. 502) shown. |
| `src/components/DraftEmailButton.tsx` (new) | Modal: USER_NOTICE / ESCALATION, "Write draft" / "Write again", a read-only text area, and "Copy". Nothing is saved or sent. |
| `src/pages/AlertDetailPage.tsx` | AI tab: the panel (with `key={alert.id}`, so another alert starts empty) and the draft button; a closed alert shows a note instead. |
| `src/api/types.ts` | `AiAssessment`, `EmailDraft`. |
| `src/index.css` | AI result, plain list, draft text area. |

## Decisions

- **The assessment result lives in the panel's state**: only the suggested verdict + reason are saved by the backend;
  the summary and references are shown for this visit.
- **`key={alert.id}`:** clicking a referenced past alert opens it in the same page component; the key makes React
  start a fresh panel instead of showing the old alert's assessment.
- **Closed alerts get no AI buttons**, following F11's rule that a closed alert hides all action buttons
  (the assessment would get 409; drafts would still work in the backend: say if you want drafts on closed alerts).
- **Copy uses `navigator.clipboard`**, built into the browser (it works on `localhost` and HTTPS).

## How to verify

Without `ANTHROPIC_API_KEY`: "Get AI assessment" → red "The AI assistant is not configured (ANTHROPIC_API_KEY is not
set)"; "Draft email" → the same message in the modal. With a key: an assessment appears and Overview shows the
suggested verdict; a draft appears and "Copy" puts it on the clipboard.

## New concepts introduced

- **Loading states** for slow requests.
- **React keys to reset a component's state.**
- **The Clipboard API.**
