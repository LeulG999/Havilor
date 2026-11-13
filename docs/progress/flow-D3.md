# Flow D3: App shell (icon rail + top bar)

**Goal:** every logged-in page sits in the shell from the mockups: a slim 64px icon rail on the left (logo, one icon
per page, the user's avatar at the bottom) and a top bar with a breadcrumb.
**New idea:** **icon navigation with accessible names**: tooltips drawn with CSS from a `data-tip` attribute, and
`aria-label` so screen readers still hear "Alert queue", not nothing.

**Built on:** `main` · **Status:** done, `npm run build` passes, screenshot checked against a running backend, pushed

## Layout

```
┌────┬──────────────────────────────────────────────────────────────┐
│ ⬡  │ Alerts / Queue                          (top bar, 56px)      │
│    ├──────────────────────────────────────────────────────────────┤
│ ▦  │                                                              │
│ ☰ ◀ active: cyan soft background + cyan icon                       │
│ +  │   the page (<Outlet />), full width, 24px padding            │
│ ▤  │                                                              │
│ ── │   ← divider + manager links, only for role MANAGER           │
│ 👥 │                                                              │
│ ◷  │                                                              │
│    │                                                              │
│(DA)│ ← avatar: opens name · role · Log out                        │
└────┴──────────────────────────────────────────────────────────────┘
```

## Files

| File | What it does |
|---|---|
| `frontend/src/components/Sidebar.tsx` | Now the icon rail: logo (`/havilor-mark-white.svg`), `RailLink`s (Dashboard, Alert queue, New alert, Standup notes; Team standups and Audit log only for a MANAGER from `/users/me`), avatar button with a small account menu (name, role, Log out). |
| `frontend/src/components/TopBar.tsx` (new) | The breadcrumb, worked out from the URL (`/alerts` → "Alerts / Queue", `/alerts/new` → "Alerts / New alert", `/audit` → "Manager / Audit log", …). |
| `frontend/src/components/icons.tsx` (new) | Seven small inline SVG icons drawn with `currentColor`. |
| `frontend/src/components/Avatar.tsx` (new) | Initials on the navy-to-teal gradient ("Dana Analyst" → "DA"); reused for assignees in D4. |
| `frontend/src/components/LoggedInLayout.tsx` | Rail + (top bar over the page). |
| `frontend/src/index.css` | `.rail*`, tooltip (`.rail-link::after` with `content: attr(data-tip)`), `.avatar`, `.topbar`, `.crumb`; `.page` now uses the full width; the old sidebar rules are gone. |

## Decisions

- **Search box and bell left out:** there is no search endpoint and no notifications yet, and a control that does
  nothing would mislead. The settings gear from the mockup is left out for the same reason.
- **The breadcrumb comes from the URL only** (no extra request); the alert page says "Alerts / Alert", and the page
  itself shows the ticket.
- **Manager links hidden for analysts** as before; the backend still refuses them (403).
- **Logout moved into the avatar menu**, as in the mockup's avatar at the bottom of the rail.
- **Tooltips in CSS** (no JavaScript, no library); they also show on keyboard focus (`:focus-visible`).
- The logo is the white mark from `docs/design/logo` (the README's choice for dark backgrounds).

## How to verify

Log in → the rail on the left; hover an icon → its name appears; the current page's icon is cyan. As the manager →
two more icons under a divider. Click the avatar → name, role, "Log out" → back to the login page. The top bar shows
"Alerts / Queue" on the queue, "Alerts / New alert" on the new alert page.

## New concepts introduced

- **Accessible icon buttons** (`aria-label`, `aria-expanded`, `role="menu"`).
- **CSS tooltips with `attr()`.**
- **Deriving UI from the URL** (`useLocation`).
