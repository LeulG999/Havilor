import { useState, type FormEvent } from 'react';
import { api } from '../api/api';
import ErrorMessage from './ErrorMessage';
import Modal from './Modal';

// "Escalate": hand the alert to another team (for example the incident response team) when it needs more than the
// analyst can do → POST /api/v1/alerts/{alertId}/escalations {team, reason}. The backend sets the alert to
// ESCALATED; when the other team sends its outcome (on the Actions tab), it goes back to INVESTIGATING.
export default function EscalateButton({ alertId, onChanged }: { alertId: number; onChanged: () => void }) {
  const [open, setOpen] = useState(false);
  const [team, setTeam] = useState('');
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);

  function openModal() {
    setTeam('');
    setReason('');
    setError(null);
    setOpen(true);
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await api(`/alerts/${alertId}/escalations`, { method: 'POST', body: { team, reason } });
      setOpen(false);
      onChanged(); // the page reloads: status ESCALATED, and the escalation appears on the Actions tab
    } catch (e) {
      setError(e); // e.g. 400 "Validation failed: team must not be blank" or 409 when the alert is closed
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <button type="button" className="secondary" onClick={openModal}>Escalate</button>
      <Modal open={open} title="Escalate alert" onClose={() => setOpen(false)}>
        <form onSubmit={handleSubmit}>
          <label>
            Team
            <input value={team} onChange={(e) => setTeam(e.target.value)} placeholder="e.g. Incident Response"
                   maxLength={100} required />
          </label>
          <label>
            Reason
            <textarea value={reason} onChange={(e) => setReason(e.target.value)} rows={4} maxLength={2000} required
                      placeholder="Why does this alert need the other team?" />
          </label>
          <ErrorMessage error={error} />
          <button type="submit" disabled={busy}>{busy ? 'Escalating…' : 'Escalate'}</button>
        </form>
      </Modal>
    </>
  );
}
