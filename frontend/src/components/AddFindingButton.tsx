import { useState, type FormEvent } from 'react';
import { api } from '../api/api';
import ErrorMessage from './ErrorMessage';
import Modal from './Modal';

const TYPES = ['SPF_FAIL', 'DKIM_FAIL', 'DMARC_FAIL', 'BAD_LIST_URL', 'MALICIOUS_ATTACHMENT', 'RECIPIENT_CLICKED', 'OTHER'];
const SEVERITIES = ['LOW', 'MEDIUM', 'HIGH'];

// "Add finding": a button that opens a modal form → POST /api/v1/alerts/{alertId}/findings (source MANUAL).
export default function AddFindingButton({ alertId, onChanged }: { alertId: number; onChanged: () => void }) {
  const [open, setOpen] = useState(false);
  const [type, setType] = useState('OTHER');
  const [severity, setSeverity] = useState('MEDIUM');
  const [title, setTitle] = useState('');
  const [teachingText, setTeachingText] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);

  function openForm() {
    setType('OTHER');
    setSeverity('MEDIUM');
    setTitle('');
    setTeachingText('');
    setError(null);
    setOpen(true);
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await api(`/alerts/${alertId}/findings`, { method: 'POST', body: { type, severity, title, teachingText } });
      setOpen(false);
      onChanged();
    } catch (e) {
      setError(e); // shown inside the modal, so the analyst can fix the form
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <button type="button" onClick={openForm}>Add finding</button>
      <Modal open={open} title="Add finding" onClose={() => setOpen(false)}>
        <form onSubmit={handleSubmit}>
          <label>
            Type
            <select value={type} onChange={(e) => setType(e.target.value)}>
              {TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
            </select>
          </label>
          <label>
            Severity
            <select value={severity} onChange={(e) => setSeverity(e.target.value)}>
              {SEVERITIES.map((s) => <option key={s} value={s}>{s}</option>)}
            </select>
          </label>
          <label>
            Title
            <input value={title} onChange={(e) => setTitle(e.target.value)} required maxLength={300} />
          </label>
          <label>
            Why this matters (optional)
            <textarea value={teachingText} onChange={(e) => setTeachingText(e.target.value)} rows={3} />
          </label>
          <ErrorMessage error={error} />
          <button type="submit" disabled={busy}>{busy ? 'Saving…' : 'Add finding'}</button>
        </form>
      </Modal>
    </>
  );
}
