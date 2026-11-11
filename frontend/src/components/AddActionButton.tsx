import { useState, type FormEvent } from 'react';
import { api } from '../api/api';
import ErrorMessage from './ErrorMessage';
import Modal from './Modal';

const TYPES = ['BLOCK_SENDER', 'PURGE_EMAIL', 'RESET_PASSWORD', 'BLOCK_URL', 'NOTIFY_USER', 'OTHER'];
const RESULTS = ['DONE', 'FAILED', 'PENDING'];

// "Add action": what the analyst did → POST /api/v1/alerts/{alertId}/actions.
// Who did it and when are filled in by the backend (from the token and its clock).
export default function AddActionButton({ alertId, onChanged }: { alertId: number; onChanged: () => void }) {
  const [open, setOpen] = useState(false);
  const [type, setType] = useState('BLOCK_SENDER');
  const [result, setResult] = useState('DONE');
  const [note, setNote] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);

  function openForm() {
    setType('BLOCK_SENDER');
    setResult('DONE');
    setNote('');
    setError(null);
    setOpen(true);
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await api(`/alerts/${alertId}/actions`, { method: 'POST', body: { type, result, note } });
      setOpen(false);
      onChanged();
    } catch (e) {
      setError(e); // e.g. 409 when the alert was closed in the meantime
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <button type="button" onClick={openForm}>Add action</button>
      <Modal open={open} title="Add action" onClose={() => setOpen(false)}>
        <form onSubmit={handleSubmit}>
          <label>
            What did you do?
            <select value={type} onChange={(e) => setType(e.target.value)}>
              {TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
            </select>
          </label>
          <label>
            Result
            <select value={result} onChange={(e) => setResult(e.target.value)}>
              {RESULTS.map((r) => <option key={r} value={r}>{r}</option>)}
            </select>
          </label>
          <label>
            Note (optional)
            <textarea value={note} onChange={(e) => setNote(e.target.value)} rows={3} maxLength={2000} />
          </label>
          <ErrorMessage error={error} />
          <button type="submit" disabled={busy}>{busy ? 'Saving…' : 'Add action'}</button>
        </form>
      </Modal>
    </>
  );
}
