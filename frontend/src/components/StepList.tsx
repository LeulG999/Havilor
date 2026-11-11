import type { AlertStep } from '../api/types';
import StepForm from './StepForm';

// The playbook steps of this alert, in order: what to check, why it matters, and how far the analyst got.
// When `editable` (an open alert), every PENDING step has its answer form underneath.
export default function StepList({ alertId, steps, editable, onChanged }: {
  alertId: number;
  steps: AlertStep[];
  editable: boolean;
  onChanged: () => void;
}) {
  return (
    <section className="panel">
      <h2>Playbook steps</h2>
      {steps.length === 0 && <p className="muted">This alert has no playbook.</p>}
      <ol className="cards">
        {steps.map((s) => (
          <li key={s.stepId}>
            <strong>{s.title}</strong> <span className={`badge step-${s.status.toLowerCase()}`}>{s.status}</span>
            <p>{s.question}</p>
            {s.answer && <p>Answer: {s.answer}</p>}
            <p className="why">Why it matters: {s.whyItMatters}</p>
            {editable && s.status === 'PENDING' && <StepForm alertId={alertId} step={s} onChanged={onChanged} />}
          </li>
        ))}
      </ol>
    </section>
  );
}
