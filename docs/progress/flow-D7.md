# Flow D7: Alert detail page

**Goal:** the alert page looks like `docs/design/mockups/s3-detail.png`: the ticket and title with a line of facts,
the alert-wide buttons on the right, four summary cards, tabs with counts, and an Overview that shows the AI card and
the email beside the latest history.
**New idea:** **one component in two places with an option** (`AlertCards` with `withPriority`), and a
**collapsible section** driven by one piece of state.

**Built on:** `main` · **Status:** done, `npm run build` passes, screenshot checked against a running backend, pushed

## Layout

```
INC-GEN-…                                                     [Escalate] [Draft email] [Close alert]
Your password expires today
(Investigating) CRITICAL │ Detected phishing │ Source: test generator │ Nov 12, 2026, 22:38
┌ STATUS ─────────┐ ┌ ASSIGNED TO ────┐ ┌ PRIORITY ───────┐ ┌ VERDICT ─────────────────┐
│ Investigating   │ │ Dana Analyst    │ │ Critical ▾      │ │ Undecided                │
└─────────────────┘ └─────────────────┘ └─────────────────┘ │ AI suggests MALICIOUS    │
Overview · Investigation 2/8 · Evidence 2 · Actions 1 · AI · History 5
┌ AI assessment card (1.6fr) ──────────────┐  ┌ History            All 5 › ┐
├ Email                    Less detail ⌃ ──┤  │ newest 7 entries           │
│ From · Envelope sender · SPF/DKIM/DMARC  │  └────────────────────────────┘
│ Recipients (view → Evidence) · Phish score│
└──────────────────────────────────────────┘
```

## Files

| File | What it does |
|---|---|
| `src/pages/AlertDetailPage.tsx` | The new layout. Header buttons (Escalate, Draft email, Close alert) only for an analyst on an open alert. Tabs: Overview, Investigation x/y, Evidence n, Actions n, AI, History n. Overview: AI card (or "No assessment yet · Ask the AI ›") + Email card left, History side column right ("All n ›" opens the tab). |
| `src/components/AlertCards.tsx` (was `DrawerCards.tsx`) | Shared by the panel and the page. `withPriority` adds the Priority card: a select styled as the card's value (both roles may change priority); Assigned to as before (analyst: Assign to me; manager: Assign to…). |
| `src/components/EmailCard.tsx` (new) | From, envelope sender, Reply-To (if any), SPF/DKIM/DMARC tags, "N recipients, N delivered · view", phish score (red from 70). "Less detail ⌃ / More detail ⌄". |
| `src/components/QueueRowDetails.tsx` | `AuthTag` exported for reuse. |
| `src/components/AlertDrawer.tsx` | Uses `AlertCards`. |
| `src/components/AlertHeader.tsx`, `HeaderActions.tsx` | Removed: the header line and the cards replace them. |
| `src/index.css` | `.detail-head`, `.cards4`, `.pick-select`, `.priority-text-*`, `.overview-grid` (one column under 1000px), `.sec` / `.sec-head`, `.props`. |

## Decisions

- **Priority is edited in its card** (both roles, open alerts), **assignee in its card** (role rules from F25);
  **status and verdict are shown**: they change through assigning, escalating and closing (the backend has no
  "set status" endpoint, and the verdict is only set when closing).
- **The AI tab stays** for asking (again) and for the email drafts' AI; the Overview shows the saved card read-only,
  with a link to the AI tab when there is none yet.
- **The breadcrumb stays "Alerts / Alert"** (from the URL only); the ticket is right under it on the page.
- **"Useful?" thumbs, search and bell left out** as before (they wouldn't do anything).
- The History side column is styled in Flow D8.

## How to verify

Open an alert as the analyst: header line with status chip, priority tag, case type, source, time; Escalate / Draft
email / Close alert on the right; change the priority in its card → the tag in the header changes; tabs show counts;
"Less detail" folds the email; "view" opens Evidence; "All n ›" opens History. As the manager: no header buttons,
"Assign to" and priority in the cards.

## New concepts introduced

- **Component options** (`withPriority`) instead of copies.
- **Styling a native `<select>` as text** (no custom dropdown, still keyboard-accessible).
- **CSS grid with fractions** (`1.6fr 1fr`) and a single-column fallback.
