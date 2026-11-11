# Flow F12: App layout (dark sidebar, light main area)

**Goal:** a proper app frame: a dark sidebar on the left with the app name, the main links (active one highlighted)
and the user + Log out at the bottom; the pages in a light area on the right, in rounded white cards. After login,
the analyst lands on the dashboard.
**New idea:** a layout with flexbox, and links that know whether they are the current page (`NavLink`).

**Built on:** `main` · **Status:** done, `npm run build` passes, pushed

## Layout

```
┌──────────────┬──────────────────────────────────────────────────────────┐
│ Havilor      │                                                          │
│              │   main area (light grey)                                 │
│ ▦ Dashboard  │   ┌────────────────────────────────────────────────┐     │
│ ☰ Alert queue│   │ white card (.panel, 12px rounded corners)       │     │
│ + New alert  │   └────────────────────────────────────────────────┘     │
│              │                                                          │
│ ──────────── │                                                          │
│ Dana Analyst │                                                          │
│ ANALYST      │                                                          │
│ [ Log out ]  │                                                          │
└──────────────┴──────────────────────────────────────────────────────────┘
  .sidebar: 220px, dark, sticky (full height)      .app-main: flex: 1
```

## Files

| File | What it does |
|---|---|
| `src/components/Sidebar.tsx` (new) | App name; `NavLink`s to Dashboard, Alert queue (`end`: only exactly `/alerts`), New alert, with text icons (▦ ☰ +); at the bottom the user from `GET /users/me` and Log out. |
| `src/components/TopBar.tsx` (deleted) | Replaced by the sidebar. |
| `src/components/LoggedInLayout.tsx` | Still the login guard; now renders sidebar + main area around the page. |
| `src/App.tsx` | Routes `/dashboard` and `/alerts/new` (placeholders until F14 / F15); unknown URLs → `/dashboard`. |
| `src/pages/LoginPage.tsx` | After login → `/dashboard`. |
| `src/index.css` | Sidebar, nav links (hover, active), user block; rounded white cards with a soft shadow. |

## Decisions

- **`NavLink` for the highlight:** React Router passes `isActive` to the class function, so no own "current page" logic.
  `end` on the queue link keeps it from lighting up on `/alerts/new` or `/alerts/42`.
- **Text characters as icons** (no icon library, as asked).
- **Sticky sidebar, full height**, so the links are always reachable; no mobile layout (laptop screens only, as asked).

## How to verify

Log in → you land on Dashboard (placeholder for now), its link highlighted; click Alert queue / New alert → the
highlight follows; your name and role are at the bottom; Log out returns to the login page.

## New concepts introduced

- **Flexbox page layout** (fixed-width sidebar + flexible main area).
- **`NavLink`** and active-link styling.
