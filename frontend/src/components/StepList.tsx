import type { AlertStep } from '../api/types';

// The playbook steps of this alert, in order: what to check, why it matters, and how far the analyst got.
export default function StepList({ steps }: { steps: AlertStep[] }) {
  return (
    <section className="panel">
      <h2>Playbook steps</h2>
      {steps.length === 0 && <p className="muted">This alert has no playbook.</p>}
      <ol className="cards">
        {steps.map((s) => (
          <li key={s.stepId}>
            <strong>{s.title}</strong> <span className="badge">{s.status}</span>
            <p>{s.question}</p>
            {s.answer && <p>Answer: {s.answer}</p>}
            <p className="why">Why it matters: {s.whyItMatters}</p>
          </li>
        ))}
      </ol>
    </section>
  );
}
