import { useState, type FormEvent } from 'react';
import { api } from '../api/api';
import type { Escalation } from '../api/types';
import ErrorMessage from './ErrorMessage';

const EXTERNAL_STATUSES = ['OPEN', 'IN_PROGRESS', 'RESOLVED'];
const OUTCOMES = ['CONFIRMED_MALICIOUS', 'NOT_MALICIOUS', 'INCONCLUSIVE'];

// The small form under one escalation: what the other team told us → PATCH /api/v1/escalations/{id}.
// externalStatus = how far they are; outcome = their answer. Sending an outcome ends the escalation:
// the backend sets the alert back to INVESTIGATING.
export default function EscalationForm({ escalation, onChanged }: {
  escalation: Escalation;
  onChanged: () => void;
}) {
  const [externalStatus, setExternalStatus] = useState(escalation.externalStatus);
  const [outcome, setOutcome] = useState(escalation.outcome ?? ''); // '' = no answer yet
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      // null outcome = "don't change it" for the backend, so "no answer yet" never removes an answer.
      await api(`/escalations/${escalation.id}`, {
        method: 'PATCH',
        body: { externalStatus, outcome: outcome === '' ? null : outcome },
      });
      onChanged(); // the page reloads: the alert's status may have changed
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="step-form" onSubmit={handleSubmit}>
      <label className="inline">
        Their status
        <select value={externalStatus} onChange={(e) => setExternalStatus(e.target.value)}>
          {EXTERNAL_STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
      </label>
      <label className="inline">
        Outcome
        <select value={outcome} onChange={(e) => setOutcome(e.target.value)}>
          <option value="">— no answer yet —</option>
          {OUTCOMES.map((o) => <option key={o} value={o}>{o}</option>)}
        </select>
      </label>
      <button type="submit" className="small" disabled={busy}>{busy ? 'Saving…' : 'Save'}</button>
      <ErrorMessage error={error} />
    </form>
  );
}
