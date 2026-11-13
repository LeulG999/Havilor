import { useState } from 'react';
import type { AlertDraft } from '../api/types';
import DraftForm from '../components/DraftForm';
import ExtractStep from '../components/ExtractStep';
import MissingHints from '../components/MissingHints';

// "New alert" (manual intake), two steps on one page:
//   1. ExtractStep: files and/or text → the AI returns a draft;
//   2. MissingHints + DraftForm: see what's missing, check and fix the draft → save it as a normal alert.
// Which step shows depends only on whether we have a draft. The text and files are kept here, so "Add more and
// extract again" (back to step 1) keeps what she already gave; "Start over" clears everything.
export default function NewAlertPage() {
  const [draft, setDraft] = useState<AlertDraft | null>(null);
  const [text, setText] = useState('');
  const [files, setFiles] = useState<File[]>([]);

  // The AI may not find when the email arrived (receivedAt = null). The backend requires it, so the form starts
  // with "now"; the analyst can still change it.
  function handleDraft(newDraft: AlertDraft) {
    setDraft({ ...newDraft, receivedAt: newDraft.receivedAt ?? new Date().toISOString() });
  }

  function startOver() {
    setDraft(null);
    setText('');
    setFiles([]);
  }

  return (
    <main className="page">
      <h1>New alert</h1>
      {draft === null
        ? <ExtractStep text={text} files={files} onText={setText} onFiles={setFiles} onDraft={handleDraft} />
        : (
          <>
            <MissingHints draft={draft} onAddMore={() => setDraft(null)} />
            <DraftForm draft={draft} onChange={setDraft} onStartOver={startOver} />
          </>
        )}
    </main>
  );
}
