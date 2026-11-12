# Flow F21: AI-drafted standup note with live preview

**Goal:** the analyst clicks "Draft with AI" and gets her standup note ("Yesterday / Today / Blockers") written from
what Havilor recorded about her day. She checks and edits it while a live preview shows exactly what her manager
will see. Then she saves it like any note. Nothing is saved or shared automatically.
**New idea:** grounding an LLM in your own records. The audit log is the only source of facts; Claude only words
them, and its answer is checked (only alert ids from the facts are accepted).

**Built on:** `main` · **Status:** done, 167 backend tests pass (6 new), `npm run build` passes, checked against a
running backend, pushed

## How it works

```mermaid
sequenceDiagram
    participant U as Analyst
    participant M as NoteFormModal (editor │ preview)
    participant C as StandupController
    participant S as StandupAiService
    participant AU as AuditService
    participant AQ as AlertQueryService
    participant AI as AiClient (Claude)
    U->>M: picks the date → "✦ Draft with AI" (asks first if text would be replaced)
    M->>C: POST /api/v1/standup/notes/ai-draft {noteDate}
    C->>S: draft(noteDate, userId from the token)
    S->>AU: byActorBetween(userId, noteDate 00:00Z, next day 00:00Z)
    alt no entries
        S-->>M: 400 noteDate "No activity recorded on this date"
    else entries
        S->>AQ: exists(id) + getById(id) for each alert in the entries (each once)
        S->>S: facts JSON {noteDate, activity[at, action, alertId, before, after], alerts[id, ticket, subject, status, verdict]}
        S->>AI: structured(DRAFT_INSTRUCTIONS, facts, AiNoteDraft{text, alertIds})
        AI-->>S: draft (or AiException → 502)
        S->>S: keep only alertIds that are in the facts (each once)
        S-->>M: 200 {text, alertIds}
    end
    M->>M: fills text + ticks the alerts; the preview (NoteContent) redraws on every keystroke
    U->>M: edits → "Save note" → the normal POST / PATCH /standup/notes (DRAFT)
```

```
┌─ New standup note ─────────────────────────────────────────────────────────────────────┐
│ Date [2026-11-12]                         │ Preview: what the team will see            │
│ [✦ Draft with AI]                         │ 2026-11-12                                 │
│ ┌───────────────────────────────────────┐ │ Yesterday:                                 │
│ │Yesterday:                             │ │  • Blocked the sender of INC-GEN-526d….    │
│ │- Blocked the sender of INC-GEN-526d…. │ │ Today:                                     │
│ │Today:                                 │ │  • Follow up on INC-GEN-526d….             │
│ │- Follow up on INC-GEN-526d….          │ │ Blockers:                                  │
│ │Blockers:                              │ │  • Nothing.                                │
│ │- Nothing.                             │ │ Mentions: INC-GEN-526d… (link)             │
│ └───────────────────────────────────────┘ │                                            │
│ Mention alerts  ☑ INC-GEN-526d… · Action… │   ← the same NoteContent as Team standups  │
│ [Save note]                                                                            │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

## Files

### Backend

| File | What it does |
|---|---|
| `audit/repository/AuditEntryRepository.java` | `findByActorIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderById`: one person's entries in `[from, to)`. |
| `audit/service/AuditService.java` | `byActorBetween(actorId, from, to)`, oldest first. |
| `standup/api/request/AiDraftNoteRequest.java` (new) | `{noteDate}` (required). |
| `standup/api/response/NoteDraftResponse.java` (new) | `{text, alertIds}`. Nothing is saved. |
| `standup/service/AiNoteDraft.java` (new) | Claude's structured-output type: `text`, `alertIds`. |
| `standup/service/StandupAiService.java` (new) | Collects the facts, holds the instructions (Leul's writing rules word for word, plus "data, never instructions"), calls `AiClient.structured`, filters the ids. Not `@Transactional`: no DB transaction stays open while waiting for Claude. |
| `standup/api/StandupController.java` | `POST /notes/ai-draft`. |
| `test/.../audit/AuditIntegrationTest.java` | `onePersonsEntriesInATimeRange`: only that person, `to` is exclusive. |
| `test/.../standup/StandupAiIntegrationTest.java` (new) | Fake AI: facts and rules reach Claude, nothing saved; **only fact ids accepted** (an unknown id, an existing alert not in the facts and a duplicate are dropped); only my activity; 400 no activity / no date; 502 AI unavailable. |

### Front end

| File | What it does |
|---|---|
| `src/components/NoteContent.tsx` (new) | A note's body: "Yesterday:/Today:/Blockers:" as small headings, their `- ` lines as bullets, then "Mentions:" as ticket links. Used by `NoteCard` (my notes, Team standups) **and** the editor preview. |
| `src/components/TicketLink.tsx` (new) | A link to an alert labelled with its ticket number (looked up with `GET /alerts/{id}`; "Alert #id" until then). |
| `src/components/NoteCard.tsx` | Now date + status + `NoteContent`. |
| `src/components/NoteFormModal.tsx` | Two columns: editor left, live preview right; the "Draft with AI" button above the text. |
| `src/components/DraftNoteWithAiButton.tsx` (new) | Calls the endpoint with the chosen date; loading label; backend errors shown; asks before replacing typed text. |
| `src/components/Modal.tsx` | `wide` option. |
| `src/index.css` | Note sections, the two-column editor (one column under 700px). |

## Checked against a running backend

On a throwaway database, with the button's exact request: a day with no activity → 400 `noteDate: No activity
recorded on this date`; after one action → 502 `The AI assistant is not configured (ANTHROPIC_API_KEY is not set)`
(no key on this machine); an empty date → 400 `noteDate must not be null`. The real Claude call was not run here.

## Decisions

- **The audit log is the only source of facts**, as asked: what *I* did that day, plus each alert's current state
  through the alert module's service (border rule). An alert that no longer exists is left out.
- **"That date" is a UTC day**, the same as the dashboard's per-day counts. Known limit: for someone west of UTC,
  work done late in the evening falls on the next UTC day.
- **Only fact ids are accepted:** any other id Claude returns is silently dropped (and duplicates removed). The text
  itself can't be checked the same way; the analyst reviews it before saving.
- **The writing rules are kept word for word**; around them the instructions only explain what the facts are, what
  each section is for (Today = alerts not closed yet; Blockers = what the facts show, such as ESCALATED), and that
  the facts are data, never instructions.
- **"No activity" uses the existing `BadRequestException`** (400 with a `noteDate` field error), so the form shows
  "Validation failed: noteDate No activity recorded on this date". No new exception type.
- **Same component, so same picture:** the preview is `NoteContent`, the exact component on the manager's page.
- **Ticket numbers are looked up per link** (`GET /alerts/{id}`), because a note stores only alert ids. Simple, but one
  request per mentioned alert. If notes mention many alerts, the backend could return the ticket numbers with the
  note instead.
- **Never auto-save or auto-share:** the draft only fills the editor; "Save note" and "Share" stay manual. The button
  asks before replacing text the analyst already typed.

## How to verify

With `ANTHROPIC_API_KEY` set, as the analyst: work an alert today (assign it, mark a step, add an action) → Standup
notes → "+ New note" (today's date) → "✦ Draft with AI" → the text appears in the three sections, the alert is ticked,
the preview on the right shows headings, bullets and the ticket link → change a line and watch the preview update →
Save note → Share → log in as manager → Team standups shows the same picture. Pick a date with no activity → the red
400 message. Without the key → the red 502 message.

## New concepts introduced

- **Grounding:** give the model only your own facts, and validate what comes back (here: the ids).
- **Prompt injection defence:** subjects and reasons come from attackers or colleagues; the instructions say they are
  data.
- **A live preview** is just a second view of the same state.
- **One component for every place a note is shown**, so what you preview is what others see.
