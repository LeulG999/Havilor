# Flow D6: The design pack in the repository

**Goal:** the design reference lives next to the code: `docs/design/` with its README (tokens, type, components),
the logo files and the mockups (PNG = target look, HTML = the exact CSS used to draw them).
**New idea:** design as a versioned artifact: the UI can be checked against a reference in the same commit history.

**Built on:** `main` · **Status:** done, pushed

## Files

| File | What it is |
|---|---|
| `docs/design/README.md` | "Midnight Signal": brand, colour tokens, type, components. |
| `docs/design/logo/*.svg` | Mark (white, navy, cyan), full lockups on dark / light, favicon. The app uses copies in `frontend/public/`. |
| `docs/design/mockups/*.png` + `*.html` | Login, queue, slide-over panel, alert detail, logos. |

## Decisions

- **Committed as it was staged,** nothing changed or added; the mockups' example data is fake (`example.com`/`.net`/`.org`).
- The app never copies the mockups' example data; it shows real API data.

## How to verify

`git show --stat` on the commit lists only `docs/design/` (16 files).
