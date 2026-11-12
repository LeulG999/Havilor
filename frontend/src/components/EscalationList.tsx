import type { Escalation } from '../api/types';
import EscalationForm from './EscalationForm';

// The Escalations part of the Actions tab: every time this alert was handed to another team, oldest first,
// with a form to record their progress and answer (only while the alert is open).
export default function EscalationList({ escalations, editable, onChanged }: {
  escalations: Escalation[];
  editable: boolean;
  onChanged: () => void;
}) {
  return (
    <section className="panel">
      <h2>Escalations</h2>
      {escalations.length === 0 && <p className="muted">Not escalated. Use "Escalate" in the header if needed.</p>}
      <ul className="cards">
        {escalations.map((e) => (
          <li key={e.id}>
            <strong>{e.team}</strong>
            <p>{e.reason}</p>
            <p className="muted">Their status: {e.externalStatus} · Outcome: {e.outcome ?? 'no answer yet'}</p>
            {/* key: after a reload with new values, the form starts again from them */}
            {editable && <EscalationForm key={`${e.externalStatus}-${e.outcome}`} escalation={e} onChanged={onChanged} />}
          </li>
        ))}
      </ul>
    </section>
  );
}
