# Flow F14: Dashboard page

**Goal:** the start page after login: four numbers that matter, alerts per day for the last two weeks, the newest
alerts, and two quick actions.
**New idea:** drawing a chart yourself with SVG (shapes and text in coordinates), no chart library.

**Built on:** `main` · **Status:** done, `npm run build` passes, pushed

## Layout

```
Dashboard                                             [+ New alert] [My queue]
┌──────────┐ ┌──────────────┐ ┌──────────────┐ ┌────────────────────┐
│ 12       │ │ 3            │ │ 1 (red)      │ │ 5                  │
│ Open     │ │ Assigned to me│ │ Critical open│ │ Closed last 7 days │
└──────────┘ └──────────────┘ └──────────────┘ └────────────────────┘
┌ Alerts per day (last 14 days, UTC) ───────────────────────────────┐
│        4                                                           │
│   2    ▇         1                                   3             │
│   ▇    ▇    0    ▇    ...                            ▇             │
│ Oct 29 Oct 30 Oct 31 ...                          Nov 11           │
└────────────────────────────────────────────────────────────────────┘
┌ Recent alerts ────────────────────────────────────────────────────┐
│ [HIGH] Invoice overdue        NEW            11/11/2026, 10:02     │  ← click → detail
└────────────────────────────────────────────────────────────────────┘
```

## How the SVG chart works

The chart is drawn in its own coordinate box (`viewBox="0 0 700 220"`); the browser scales it to the card width.

- Each of the 14 days gets a slot of `700 / 14 = 50` units; the bar is 60 % of it, centred.
- Bar height = `count / max × 170` (170 = the drawing height between the count labels and the date labels);
  `max` is at least 1, so an all-zero chart doesn't divide by zero.
- SVG's y grows **downwards**, so a bar starts at `y = top + 170 − height` and goes down to the base line.
- The count is a `<text>` 5 units above the bar; the date (`Nov 1`, read as a UTC day) sits under the base line.
- `role="img"` and an `aria-label` with all numbers describe it for screen readers.

## Files

| File | What it does |
|---|---|
| `src/pages/DashboardPage.tsx` (new) | `GET /alerts/dashboard` once; title + quick actions ("+ New alert" → `/alerts/new`, "My queue" → `/alerts`); the four cards, the chart, the recent list; errors shown. |
| `src/components/StatCard.tsx` (new) | A number and its label; `tone="danger"` makes a non-zero number red (critical open). |
| `src/components/BarChart.tsx` (new) | The SVG chart above. |
| `src/components/RecentAlerts.tsx` (new) | The 5 newest: priority badge, subject, status, received; a row click opens the detail page. |
| `src/api/types.ts` | `Dashboard`. |
| `src/App.tsx` | `/dashboard` → `DashboardPage`. |
| `src/index.css` | Page title row, button-looking links, 4-column stat grid, chart colours. |

## Decisions

- **Quick actions are links styled as buttons:** they go to another page and send nothing, so `<Link>` is the right
  element.
- **"My queue" opens the alert queue:** the queue doesn't filter by assignee (no such backend filter yet); "Assigned to
  me" on the card shows how many are yours.
- **Dates on the chart are UTC days**, matching the backend; the "Recent" times are in the user's local time, like
  the queue.

## How to verify

Log in → the dashboard shows the four numbers, 14 bars (zero days included) and the 5 newest alerts. Assign an alert to
yourself and come back → "Assigned to me" is one higher. Click a recent alert → its detail page. The two buttons open
New alert and the queue.

## New concepts introduced

- **SVG**: `viewBox`, `rect`, `line`, `text`, and the downward y axis.
- **Scaling data to pixels** (value / max × height).
- **CSS grid** for a row of equal cards.
