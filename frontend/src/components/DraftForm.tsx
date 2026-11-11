import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router';
import { api } from '../api/api';
import type { AlertDetail, AlertDraft } from '../api/types';
import DraftLists from './DraftLists';
import ErrorMessage from './ErrorMessage';

const CASE_TYPES = ['USER_REPORTED_EMAIL', 'DETECTED_PHISHING', 'RELEASE_REQUEST'];
const PRIORITIES = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
const AUTH_RESULTS = ['PASS', 'FAIL', 'NONE'];

// An ISO time ("2026-10-31T09:15:00Z") ↔ the value of <input type="datetime-local"> ("2026-10-31T10:15"),
// which is in the user's own time zone and has no "Z".
function toLocalInput(iso: string | null): string {
  if (!iso) {
    return '';
  }
  const date = new Date(iso);
  return new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 16);
}

function fromLocalInput(value: string): string | null {
  return value ? new Date(value).toISOString() : null;
}

// Empty text boxes mean "not known": send null, not "".
function orNull(text: string | null): string | null {
  return text && text.trim() !== '' ? text.trim() : null;
}

// Step 2 of "New alert": the draft the AI made, for the analyst to check and fix. "Save alert" sends it to the normal
// POST /api/v1/alerts (source MANUAL); the backend validates it like any alert and generates the ticket number.
export default function DraftForm({ draft, onChange, onStartOver }: {
  draft: AlertDraft;
  onChange: (draft: AlertDraft) => void;
  onStartOver: () => void;
}) {
  const navigate = useNavigate();
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<unknown>(null);

  // Small helpers: change one field of the draft, or one field of its email (always as a new object).
  const set = (field: Partial<AlertDraft>) => onChange({ ...draft, ...field });
  const setEmail = (field: Partial<AlertDraft['email']>) => onChange({ ...draft, email: { ...draft.email, ...field } });

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const alert = await api<AlertDetail>('/alerts', {
        method: 'POST',
        body: {
          ...draft,
          source: 'MANUAL',
          ticketNumber: orNull(draft.ticketNumber),
          email: {
            ...draft.email,
            replyTo: orNull(draft.email.replyTo),
            envelopeSender: orNull(draft.email.envelopeSender),
          },
        },
      });
      navigate(`/alerts/${alert.id}`); // the new alert's detail page
    } catch (e) {
      setError(e); // e.g. 400 "Validation failed: receivedAt must not be null"
      setSaving(false);
    }
  }

  return (
    <form className="panel" onSubmit={handleSubmit}>
      <h2>2. Check the draft</h2>
      <p className="muted">The AI filled this in from what you gave it. Fix anything that's wrong, then save.</p>

      <div className="form-grid">
        <label>
          Ticket number (optional)
          <input value={draft.ticketNumber ?? ''} onChange={(e) => set({ ticketNumber: e.target.value })}
                 placeholder="generated: MAN-<date>-<n>" maxLength={50} />
        </label>
        <label>
          Received at
          <input type="datetime-local" value={toLocalInput(draft.receivedAt)} required
                 onChange={(e) => set({ receivedAt: fromLocalInput(e.target.value) })} />
        </label>
        <label>
          Case type
          <select value={draft.caseType} onChange={(e) => set({ caseType: e.target.value })}>
            {CASE_TYPES.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
        </label>
        <label>
          Priority
          <select value={draft.priority} onChange={(e) => set({ priority: e.target.value })}>
            {PRIORITIES.map((p) => <option key={p} value={p}>{p}</option>)}
          </select>
        </label>
        <label className="wide">
          Subject
          <input value={draft.email.subject} onChange={(e) => setEmail({ subject: e.target.value })} required />
        </label>
        <label>
          From
          <input value={draft.email.fromAddress} onChange={(e) => setEmail({ fromAddress: e.target.value })} required />
        </label>
        <label>
          Reply-To
          <input value={draft.email.replyTo ?? ''} onChange={(e) => setEmail({ replyTo: e.target.value })} />
        </label>
        <label>
          Envelope sender
          <input value={draft.email.envelopeSender ?? ''}
                 onChange={(e) => setEmail({ envelopeSender: e.target.value })} />
        </label>
        {(['spfResult', 'dkimResult', 'dmarcResult'] as const).map((check) => (
          <label key={check}>
            {check.replace('Result', '').toUpperCase()}
            <select value={draft.email[check] ?? ''} onChange={(e) => setEmail({ [check]: e.target.value || null })}>
              <option value="">unknown</option>
              {AUTH_RESULTS.map((r) => <option key={r} value={r}>{r}</option>)}
            </select>
          </label>
        ))}
      </div>

      <DraftLists draft={draft} onChange={onChange} />

      <ErrorMessage error={error} />
      <div className="buttons">
        <button type="submit" disabled={saving}>{saving ? 'Saving…' : 'Save alert'}</button>
        <button type="button" className="secondary" onClick={onStartOver}>Start over</button>
      </div>
    </form>
  );
}
