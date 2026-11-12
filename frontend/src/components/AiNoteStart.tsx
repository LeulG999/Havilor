import { useState } from 'react';
import { api } from '../api/api';
import ErrorMessage from './ErrorMessage';

// Step 1 of a NEW standup note: AI first. Only the date and one button. The backend reads what I did that day
// (POST /api/v1/standup/notes/ai-draft, the audit log) and Claude writes the note from only those facts.
// Nothing is saved: the draft goes into the editor (step 2), where everything can still change.
// "Write it myself instead" opens the empty editor: the way out when there was no activity (400) or the AI is not
// available (502); those messages are shown right above the link.
export default function AiNoteStart({ noteDate, onDateChange, onDraft, onWriteMyself }: {
  noteDate: string;
  onDateChange: (noteDate: string) => void;
  onDraft: (text: string, alertIds: number[]) => void;
  onWriteMyself: () => void;
}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);

  async function draft() {
    setBusy(true);
    setError(null);
    try {
      const answer = await api<{ text: string; alertIds: number[] }>('/standup/notes/ai-draft', {
        method: 'POST',
        body: { noteDate },
      });
      onDraft(answer.text, answer.alertIds);
    } catch (e) {
      setError(e); // 400 "No activity recorded on this date", 502 "The AI assistant is not configured …"
      setBusy(false);
    }
  }

  return (
    <div className="ai-start">
      <label>
        Date
        <input type="date" value={noteDate} onChange={(e) => onDateChange(e.target.value)} required />
      </label>
      <p className="muted">Havilor drafts your standup from what you did that day. You check and edit it before saving.</p>
      <button type="button" disabled={busy || noteDate === ''} onClick={draft}>
        {busy ? 'Writing from your activity… (this can take several seconds)' : '✦ Write my standup with AI'}
      </button>
      <ErrorMessage error={error} />
      <p>
        <button type="button" className="link" onClick={onWriteMyself}>Write it myself instead</button>
      </p>
    </div>
  );
}
