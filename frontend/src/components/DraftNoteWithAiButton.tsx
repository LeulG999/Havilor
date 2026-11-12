import { useState } from 'react';
import { api } from '../api/api';
import ErrorMessage from './ErrorMessage';

// "Draft with AI": POST /api/v1/standup/notes/ai-draft {noteDate}. The backend reads what I did that day (the
// audit log) and Claude writes the note from only those facts. Nothing is saved: the answer goes into the editor,
// where the analyst can change everything before "Save note".
export default function DraftNoteWithAiButton({ noteDate, hasText, onDraft }: {
  noteDate: string;
  hasText: boolean;
  onDraft: (text: string, alertIds: number[]) => void;
}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);

  async function draft() {
    // The draft replaces the text, so ask before throwing away what she already typed.
    if (hasText && !window.confirm('Replace the text you wrote with an AI draft?')) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const answer = await api<{ text: string; alertIds: number[] }>('/standup/notes/ai-draft', {
        method: 'POST',
        body: { noteDate },
      });
      onDraft(answer.text, answer.alertIds);
    } catch (e) {
      setError(e); // e.g. 400 "No activity recorded on this date", or 502 when the AI is not available
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="ai-draft">
      <button type="button" className="secondary" disabled={busy || noteDate === ''} onClick={draft}>
        {busy ? 'Drafting from your activity… (this can take several seconds)' : '✦ Draft with AI'}
      </button>
      <ErrorMessage error={error} />
    </div>
  );
}
