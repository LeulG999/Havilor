# Flow D2: Login page

**Goal:** the first screen looks like `docs/design/mockups/login-white.png`: a dark brand panel on the left that
says what Havilor is, and a calm white sign-in panel on the right.
**New idea:** **decorative SVG drawn in code** (a pattern, a mask with a radial gradient, circles), and a password
field with a Show/Hide toggle.

**Built on:** `main` · **Status:** done, `npm run build` passes, screenshot compared with the mockup, pushed

## Layout

```
┌──────────────────── .login-brand (1.15fr, dark) ────────────┬──────── .login-side (1fr, white) ────────┐
│ [hexagon logo] HAVILOR                                      │                                          │
│                              ◯ radar rings, hexagon grid    │   Sign in                                │
│ PHISHING INVESTIGATION · SOC        (SVG, fades out)        │   Welcome back. Use your Havilor work…   │
│ Investigate faster.                                         │   Email     [                       ]    │
│ Learn as you go.                                            │   Password  [                 Show  ]    │
│ Havilor guides each phishing case…                          │   [         Sign in (navy)          ]    │
│ ☑ Guided playbooks · ☑ AI that explains itself ·            │   ⓘ Accounts are created by your admin.  │
│ ☑ Every action on the record                                │                                          │
│ © 2026 Havilor                                              │                                          │
└─────────────────────────────────────────────────────────────┴──────────────────────────────────────────┘
```

Under 900px wide only the sign-in panel shows.

## Files

| File | What it does |
|---|---|
| `frontend/src/components/LoginBrandPanel.tsx` (new) | The left half: SVG background (hexagon `<pattern>` faded by a `<mask>` with a radial gradient, three radar rings, a sweep, a cyan and a red "signal" dot), the logo lockup (`/havilor-mark-white.svg`), eyebrow, headline, text, three points with check marks, © year. |
| `frontend/src/pages/LoginPage.tsx` | The right half: "Sign in", email, password with Show/Hide, navy "Sign in", the admin note. Same login logic as before (`POST /auth/login` → token → `/dashboard`); backend errors still shown. |
| `frontend/src/index.css` | `.login-*` rules: the two-column grid, brand gradients, light inputs (`#F8FAFC`, `#D5DDE8` border, cyan-teal focus ring), navy button, the note; the old `.login` rule is gone. |

## Decisions

- **Privacy and Terms links removed** (there are no such pages); the footer is just "© <year> Havilor".
- **The version line ("v0.9 · local") is left out:** it's mockup text, not something the app knows.
- **The background SVG is copied from the mockup's drawing instructions** (pattern, rings, dots); it's decoration,
  not example data. `aria-hidden` keeps it away from screen readers.
- **The white panel uses its own light colours** and `color-scheme: light`, so the browser's autofill and caret look
  right on white while the rest of the app is dark.
- Show/Hide switches the input's `type` between `password` and `text`; `autoComplete` lets password managers fill it.

## How to verify

Open `/login`: dark panel left, white panel right, as in the mockup. Type a password → "Show" reveals it, "Hide"
hides it. A wrong password shows the backend's red message above the admin note. Narrow the window below 900px → only
the sign-in panel.

## New concepts introduced

- **SVG patterns, masks and gradients** for a lightweight background (no image file).
- **A toggle that changes an input's type.**
- **A local colour scheme** inside an otherwise dark app.
