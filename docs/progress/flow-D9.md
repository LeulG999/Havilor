# Flow D9: Remaining pages in the design

**Goal:** every page uses the Midnight Signal look: dashboards (analyst + manager), new alert, standup notes (with the
note modal and its preview), team standups, audit log and the modals.

**Built on:** `main` · **Status:** done, `npm run build` passes, screenshots checked against a running backend, pushed

## Files

| File | What changed |
|---|---|
| `src/components/StatCard.tsx` | Small uppercase label first, then the value (like the summary cards); optional extra line (`children`). |
| `src/components/BarChart.tsx` | Wide, low viewBox (1200 × 200) so the chart isn't huge at full width; no "0" over empty days. |
| `src/components/RecentAlerts.tsx`, `AnalystDashboard.tsx`, `TeamDashboard.tsx` | Sections with a header line; recent alerts and analysts with the queue's tags, chips, avatars and mono times. |
| `src/pages/AuditLogPage.tsx` | When (mono), who, what in plain words (same `describe()` as the history), on (alert link); the exact audit texts as a tooltip. |
| `src/pages/StandupNotesPage.tsx`, `TeamStandupsPage.tsx` | Note list spacing. |
| `src/index.css` | Note bullets: the browser's nested-list markers (the stray hollow dots) replaced by small cyan dots; styled file picker; form text weight 400 inside labels; dashboard sections. |

## Decisions

- **The stray dot** was the browser's default marker for a list inside a list item; now `list-style: none` with a
  drawn dot.
- **Readable audit log** uses the same descriptions as an alert's history, so the two never disagree.
- Modals already used the design tokens (D1); only the text weight inside labels needed fixing.

## How to verify

Dashboard (analyst and manager), New alert, Standup notes (+ New note, Edit → editor with preview), Team standups and
Audit log (as the manager): dark cards, uppercase labels, mono data, no stray bullets.
