import { useState } from 'react';
import { api } from '../api/api';
import ErrorMessage from './ErrorMessage';

// "Share": POST /api/v1/standup/notes/{id}/share. The note becomes SHARED: the managers see it in "Team standups",
// and it can't be changed any more. The browser asks first, because sharing can't be undone.
export default function ShareNoteButton({ noteId, onShared }: { noteId: number; onShared: () => void }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);

  async function share() {
    if (!window.confirm('Share this note with the team? It can\'t be changed afterwards.')) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await api(`/standup/notes/${noteId}/share`, { method: 'POST' });
      onShared();
    } catch (e) {
      setError(e); // e.g. 409 when it was already shared
      setBusy(false);
    }
  }

  return (
    <>
      <button type="button" className="small" disabled={busy} onClick={share}>{busy ? 'Sharing…' : 'Share'}</button>
      <ErrorMessage error={error} />
    </>
  );
}
