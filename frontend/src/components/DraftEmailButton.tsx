import { useState } from 'react';
import { api } from '../api/api';
import type { EmailDraft } from '../api/types';
import ErrorMessage from './ErrorMessage';
import Modal from './Modal';

// "Draft email": the AI writes a draft (a notice to the recipients, or a hand-over to another team).
// POST /api/v1/alerts/{alertId}/ai/draft-email. Nothing is saved or sent: the analyst copies it and sends it herself.
export default function DraftEmailButton({ alertId }: { alertId: number }) {
  const [open, setOpen] = useState(false);
  const [type, setType] = useState('USER_NOTICE');
  const [draft, setDraft] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [copied, setCopied] = useState(false);
  const [error, setError] = useState<unknown>(null);

  function openModal() {
    setDraft(null);
    setCopied(false);
    setError(null);
    setOpen(true);
  }

  async function writeDraft() {
    setLoading(true);
    setError(null);
    setCopied(false);
    try {
      const answer = await api<EmailDraft>(`/alerts/${alertId}/ai/draft-email`, { method: 'POST', body: { type } });
      setDraft(answer.draft);
    } catch (e) {
      setError(e); // e.g. 502 when the AI is not available
    } finally {
      setLoading(false);
    }
  }

  // The browser's clipboard API: puts the text where Ctrl+V finds it.
  async function copy() {
    if (draft) {
      await navigator.clipboard.writeText(draft);
      setCopied(true);
    }
  }

  return (
    <>
      <button type="button" className="secondary" onClick={openModal}>Draft email</button>
      <Modal open={open} title="Draft email" onClose={() => setOpen(false)}>
        <label>
          Email for
          <select value={type} onChange={(e) => setType(e.target.value)}>
            <option value="USER_NOTICE">The recipients (user notice)</option>
            <option value="ESCALATION">Another team (escalation)</option>
          </select>
        </label>
        <button type="button" disabled={loading} onClick={writeDraft}>
          {loading ? 'Writing…' : draft ? 'Write again' : 'Write draft'}
        </button>
        <ErrorMessage error={error} />
        {draft && (
          <>
            <textarea className="draft" value={draft} readOnly rows={12} aria-label="Draft" />
            <button type="button" className="secondary" onClick={copy}>{copied ? 'Copied ✓' : 'Copy'}</button>
          </>
        )}
      </Modal>
    </>
  );
}
