import { useState, type FormEvent } from 'react';
import { api } from '../api/api';
import ErrorMessage from './ErrorMessage';
import Modal from './Modal';

const VERDICTS = ['MALICIOUS', 'BENIGN', 'FALSE_POSITIVE'];

// "Close alert": the analyst's final decision → POST /api/v1/alerts/{alertId}/close {verdict}.
// The backend checks the rules: a real verdict (else 400), not closed already, no pending steps (else 409).
// Its message is shown inside the modal, e.g. "Alert 42 has 3 pending steps. Finish or skip every step before closing."
export default function CloseAlertButton({ alertId, onChanged }: { alertId: number; onChanged: () => void }) {
  const [open, setOpen] = useState(false);
  const [verdict, setVerdict] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);

  function openModal() {
    setVerdict('');
    setError(null);
    setOpen(true);
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await api(`/alerts/${alertId}/close`, { method: 'POST', body: { verdict } });
      setOpen(false);
      onChanged(); // the alert is now CLOSED: the page reloads and hides every action button
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <button type="button" className="danger" onClick={openModal}>Close alert</button>
      <Modal open={open} title="Close alert" onClose={() => setOpen(false)}>
        <form onSubmit={handleSubmit}>
          <p className="muted">Closing is final: the alert can't be changed afterwards.</p>
          <label>
            Verdict
            <select value={verdict} onChange={(e) => setVerdict(e.target.value)} required>
              <option value="">— choose a verdict —</option>
              {VERDICTS.map((v) => <option key={v} value={v}>{v}</option>)}
            </select>
          </label>
          <ErrorMessage error={error} />
          <button type="submit" className="danger" disabled={busy}>{busy ? 'Closing…' : 'Close alert'}</button>
        </form>
      </Modal>
    </>
  );
}
