# Havilor design — Midnight Signal

Reference for building the UI. The PNGs in mockups/ are the target look; the .html files next to them hold the exact CSS
(colors, spacing, sizes) used to draw them. Match them; do not copy their fake example data into the app.

## Brand
- Name: HAVILOR (uppercase in the logo, letter-spacing ~0.22em, Space Grotesk 600). In text: "Havilor".
- Logo: logo/havilor-mark-white.svg on dark (white hexagon + H, cyan signal dot). havilor-mark-navy.svg on light.
  Full lockups: havilor-logo-on-dark.svg / havilor-logo-on-light.svg. Browser tab: favicon.svg.
- Tagline (login): "Investigate faster. Learn as you go."

## Color tokens (dark app)
--bg #0A1120 · --side #070C17 · --surface #101A2C · --surface2 #0D1626 · --raise #14213A
--border #1C2940 · --border2 #253553 · --text #E6EDF7 · --muted #8494AD · --faint #5E6C84
--accent #22D3EE (cyan) · --accentSoft rgba(34,211,238,.12) · --accentText #04202A
Severity: --crit #F0525A · --high #F97316 · --med #EAB308 · --low #38BDF8 · success --ok #34D399
Login right panel is white (#FFFFFF), inputs #F8FAFC with #D5DDE8 border, button navy #0A1120 with white text.

## Type
- Display/headings: Space Grotesk (500/600). Body/UI: Inter (400/500/600). Data (tickets, emails, hashes, times): JetBrains Mono.
- Self-host with @fontsource packages (@fontsource/space-grotesk, @fontsource/inter, @fontsource/jetbrains-mono).

## Components (see mockups)
- Icon rail sidebar (64px), active item = cyan soft background + cyan icon.
- Top bar: breadcrumb, search field, notification bell.
- Severity tags: small mono uppercase, outlined, tinted (CRITICAL/HIGH/MEDIUM/LOW).
- Status chips: rounded pill; Investigating = cyan; Escalated = orange; New = neutral; Closed = muted.
- Segmented filters with counts; "Assigned to me" switch.
- Panels: surface color, 1px border, 12px radius. Use semantic colors only for meaning, never decoration.
- AI card: cyan border + faint cyan top glow, verdict badge, 3-step confidence meter, key evidence with severity dots,
  next steps checklist (display only), "would change my mind if", Useful? thumbs.
- History: vertical timeline with icon circles and mono times.
