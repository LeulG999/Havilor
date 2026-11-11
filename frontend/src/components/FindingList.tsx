import type { Finding } from '../api/types';

// What Havilor (AUTOMATIC) or the analyst (MANUAL) found, each with "why this matters".
export default function FindingList({ findings }: { findings: Finding[] }) {
  return (
    <section className="panel">
      <h2>Findings</h2>
      {findings.length === 0 && <p className="muted">No findings.</p>}
      <ul className="cards">
        {findings.map((f) => (
          <li key={f.id}>
            <strong>{f.title}</strong>{' '}
            <span className="badge">{f.severity}</span>{' '}
            <span className="muted">{f.source}</span>
            {f.teachingText && <p className="why">Why this matters: {f.teachingText}</p>}
          </li>
        ))}
      </ul>
    </section>
  );
}
