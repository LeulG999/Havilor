import { useEffect, useState, type FormEvent } from 'react';
import { api } from '../api/api';
import type { AlertSummary, Page, StandupNote } from '../api/types';
import ErrorMessage from './ErrorMessage';
import Modal from './Modal';

// Today as "2026-11-12" in the user's own time zone (the "en-CA" format happens to be exactly YYYY-MM-DD).
function today(): string {
  return new Date().toLocaleDateString('en-CA');
}

// The modal for a new note (note = null → POST /api/v1/standup/notes) or for changing a DRAFT note
// (PATCH /api/v1/standup/notes/{id}). Mentioned alerts are chosen with checkboxes from the 20 newest alerts.
export default function NoteFormModal({ open, note, onClose, onSaved }: {
  open: boolean;
  note: StandupNote | null;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [noteDate, setNoteDate] = useState(today());
  const [text, setText] = useState('');
  const [alertIds, setAlertIds] = useState<number[]>([]);
  const [recent, setRecent] = useState<AlertSummary[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);

  // Every time the modal opens: start from the note being edited (or empty), and load the alerts to choose from.
  useEffect(() => {
    if (!open) {
      return;
    }
    setNoteDate(note?.noteDate ?? today());
    setText(note?.text ?? '');
    setAlertIds(note?.alertIds ?? []);
    setError(null);
    api<Page<AlertSummary>>('/alerts?page=0&size=20').then((page) => setRecent(page.content)).catch(setError);
  }, [open, note]);

  // Tick → add the id; untick → a new list without it.
  function toggle(alertId: number) {
    setAlertIds(alertIds.includes(alertId) ? alertIds.filter((a) => a !== alertId) : [...alertIds, alertId]);
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const body = { noteDate, text, alertIds };
      if (note === null) {
        await api('/standup/notes', { method: 'POST', body });
      } else {
        await api(`/standup/notes/${note.id}`, { method: 'PATCH', body }); // alertIds replaces the whole list
      }
      onSaved();
    } catch (e) {
      setError(e); // e.g. 409 when the note was shared in the meantime, 400 for an unknown alert
    } finally {
      setBusy(false);
    }
  }

  // Alerts already mentioned but not among the 20 newest stay in the list, so editing never drops them silently.
  const olderIds = alertIds.filter((alertId) => !recent.some((a) => a.id === alertId));

  return (
    <Modal open={open} title={note === null ? 'New standup note' : 'Edit standup note'} onClose={onClose}>
      <form onSubmit={handleSubmit}>
        <label>
          Date
          <input type="date" value={noteDate} onChange={(e) => setNoteDate(e.target.value)} required />
        </label>
        <label>
          What did you work on? What's next? Anything blocking you?
          <textarea value={text} onChange={(e) => setText(e.target.value)} rows={6} maxLength={10000} required />
        </label>
        <h3>Mention alerts</h3>
        <ul className="check-list">
          {olderIds.map((alertId) => (
            <li key={alertId}>
              <label className="inline">
                <input type="checkbox" checked onChange={() => toggle(alertId)} /> Alert #{alertId}
              </label>
            </li>
          ))}
          {recent.map((a) => (
            <li key={a.id}>
              <label className="inline">
                <input type="checkbox" checked={alertIds.includes(a.id)} onChange={() => toggle(a.id)} />
                {a.ticketNumber} · {a.subject}
              </label>
            </li>
          ))}
        </ul>
        <ErrorMessage error={error} />
        <button type="submit" disabled={busy}>{busy ? 'Saving…' : 'Save note'}</button>
      </form>
    </Modal>
  );
}
