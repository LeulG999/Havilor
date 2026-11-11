import { useState, type FormEvent } from 'react';
import { api } from '../api/api';
import type { AlertDraft } from '../api/types';
import ErrorMessage from './ErrorMessage';

// Step 1 of "New alert": paste the email (or its headers) OR choose a file, and let the AI read it.
// → POST /api/v1/alerts/manual/extract as multipart/form-data. Nothing is saved yet; we get a draft back.
export default function ExtractStep({ onDraft }: { onDraft: (draft: AlertDraft) => void }) {
  const [text, setText] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<unknown>(null);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      // FormData = the parts of a multipart request, like an HTML form with a file field.
      const form = new FormData();
      if (file && file.name.toLowerCase().endsWith('.txt')) {
        // A .txt file (e.g. saved headers) is just text: send its content as the "text" field.
        form.append('text', await file.text());
      } else if (file) {
        form.append('file', file); // .eml, image or PDF: the backend reads it (Claude's vision for images/PDFs)
      } else {
        form.append('text', text);
      }
      onDraft(await api<AlertDraft>('/alerts/manual/extract', { method: 'POST', body: form }));
    } catch (e) {
      setError(e); // e.g. 400 "file: must be an .eml file, …" or 502 when the AI is not available
    } finally {
      setLoading(false);
    }
  }

  return (
    <form className="panel" onSubmit={handleSubmit}>
      <h2>1. What do you have?</h2>
      <label>
        Paste the email text or its raw headers
        {/* One or the other: the text box is off while a file is chosen. */}
        <textarea value={text} onChange={(e) => setText(e.target.value)} rows={8} disabled={file !== null}
                  placeholder="From: …&#10;Subject: …" />
      </label>
      <label>
        … or choose a file (.eml, .txt with headers, image, PDF)
        <input type="file" accept=".eml,.txt,image/png,image/jpeg,image/gif,image/webp,application/pdf"
               disabled={text.trim() !== ''} onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
      </label>
      <ErrorMessage error={error} />
      <button type="submit" disabled={loading || (file === null && text.trim() === '')}>
        {loading ? 'Reading the email with the AI… (this can take several seconds)' : 'Extract the alert'}
      </button>
    </form>
  );
}
