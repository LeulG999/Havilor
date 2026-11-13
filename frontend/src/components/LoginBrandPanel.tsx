// The dark left half of the login page (docs/design/mockups/login-white.png): logo, what Havilor is, three points,
// and a quiet background drawn with SVG (a hexagon grid fading out, radar rings, two "signal" dots).
// Pure decoration and text: no data, no state.

const POINTS = [
  { title: 'Guided playbooks', text: 'Every phishing case follows the same checks, and each step explains why it matters.' },
  { title: 'AI that explains itself', text: 'A suggested verdict with the evidence behind it. You make the call.' },
  { title: 'Every action on the record', text: 'Each change is written to an audit trail your team can trust.' },
];

export default function LoginBrandPanel() {
  return (
    <section className="login-brand">
      {/* aria-hidden: screen readers skip the decoration */}
      <svg className="login-bg" viewBox="0 0 800 880" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
        <defs>
          {/* One hexagon, repeated as a pattern. */}
          <pattern id="hex" width="56" height="48.5" patternUnits="userSpaceOnUse">
            <path d="M14 0 L42 0 L56 24.25 L42 48.5 L14 48.5 L0 24.25 Z" fill="none"
                  stroke="rgba(34,211,238,.06)" strokeWidth="1" />
          </pattern>
          {/* The grid is visible around the radar and fades out towards the edges. */}
          <radialGradient id="fade" cx=".75" cy=".45" r=".6">
            <stop offset="0" stopColor="#fff" stopOpacity="1" />
            <stop offset="1" stopColor="#fff" stopOpacity="0" />
          </radialGradient>
          <mask id="fade-mask"><rect width="800" height="880" fill="url(#fade)" /></mask>
        </defs>
        <rect width="800" height="880" fill="url(#hex)" mask="url(#fade-mask)" />
        <g fill="none" stroke="rgba(34,211,238,.18)">
          <circle cx="620" cy="380" r="120" />
          <circle cx="620" cy="380" r="200" strokeOpacity=".6" />
          <circle cx="620" cy="380" r="290" strokeOpacity=".35" />
        </g>
        <path d="M620 380 L620 90 A290 290 0 0 1 870 235 Z" fill="rgba(34,211,238,.05)" />
        <circle cx="700" cy="300" r="4" fill="#22D3EE" />
        <circle cx="700" cy="300" r="10" fill="none" stroke="#22D3EE" strokeOpacity=".4" />
        <circle cx="540" cy="470" r="3" fill="#F0525A" />
        <circle cx="540" cy="470" r="9" fill="none" stroke="#F0525A" strokeOpacity=".35" />
      </svg>

      <div className="login-lockup">
        <img src="/havilor-mark-white.svg" alt="" width="44" height="44" />
        <b>HAVILOR</b>
      </div>

      <div className="login-copy">
        <div className="eyebrow">Phishing investigation · SOC</div>
        <h1>Investigate faster.<br /><em>Learn</em> as you go.</h1>
        <p>Havilor guides each phishing case from first signal to final verdict, and explains every step along the way.</p>
        <ul>
          {POINTS.map((point) => (
            <li key={point.title}>
              <span className="tick" aria-hidden="true">
                <svg viewBox="0 0 14 14"><path d="M3 7.5l2.5 2.5L11 4" fill="none" stroke="#22D3EE" strokeWidth="1.8"
                                               strokeLinecap="round" strokeLinejoin="round" /></svg>
              </span>
              <div><b>{point.title}</b><span>{point.text}</span></div>
            </li>
          ))}
        </ul>
      </div>

      <div className="login-foot">© {new Date().getFullYear()} Havilor</div>
    </section>
  );
}
