import { useState, type FormEvent } from 'react';
import { api } from '../api/api';
import type { AlertDraft } from '../api/types';
import DropZone from './DropZone';
import ErrorMessage from './ErrorMessage';
import SourceCards from './SourceCards';

// Step 1 of "New alert": what she has (the four source cards), then the files (drop zone) and/or pasted text, and a
// short checklist. "Extract the alert" sends everything in ONE request (POST /api/v1/alerts/manual/extract, multipart:
// "text" + up to 5 "files"); the AI reads it all together and returns a draft. Nothing is saved yet.
// The text and files live in the page (props), so "Add more and extract again" in step 2 keeps them.
export default function ExtractStep({ text, files, onText, onFiles, onDraft }: {
  text: string;
  files: File[];
  onText: (text: string) => void;
  onFiles: (files: File[]) => void;
  onDraft: (draft: AlertDraft) => void;
}) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const nothing = text.trim() === '' && files.length === 0;

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      // FormData = the parts of a multipart request. A .txt file (e.g. saved headers) is just text: its content
      // joins the "text" part; .eml, images and PDFs go as "files" parts (the backend reads them).
      const form = new FormData();
      let allText = text;
      for (const file of files) {
        if (file.name.toLowerCase().endsWith('.txt')) {
          allText += `\n\n${await file.text()}`;
        } else {
          form.append('files', file);
        }
      }
      if (allText.trim() !== '') {
        form.append('text', allText);
      }
      onDraft(await api<AlertDraft>('/alerts/manual/extract', { method: 'POST', body: form }));
    } catch (e) {
      setError(e); // e.g. 400 "files: a.zip: must be an .eml file, …" or 502 when the AI is not available
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <section className="sec intake">
        <div className="sec-head"><b>1. What do you have?</b><span className="muted">more is better: add everything you have</span></div>
        <div className="intake-body">
          <SourceCards />
          <DropZone files={files} onChange={onFiles} />
          <label>
            Pasted text or raw headers
            <textarea value={text} onChange={(e) => onText(e.target.value)} rows={7}
                      placeholder={'From: …\nSubject: …\n(or the raw headers)'} />
          </label>
          <div className="checklist">
            <div><span className="checklist-label">Required</span> subject and sender</div>
            <div><span className="checklist-label helpful">Very helpful</span> headers, links, attachments, recipients</div>
          </div>
          <ErrorMessage error={error} />
          <button type="submit" disabled={loading || nothing}>
            {loading ? 'Reading everything with the AI… (this can take several seconds)' : 'Extract the alert'}
          </button>
        </div>
      </section>
    </form>
  );
}
