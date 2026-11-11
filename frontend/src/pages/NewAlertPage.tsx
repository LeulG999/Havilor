import { useState } from 'react';
import type { AlertDraft } from '../api/types';
import DraftForm from '../components/DraftForm';
import ExtractStep from '../components/ExtractStep';

// "New alert" (manual intake), two steps on one page:
//   1. ExtractStep: text or a file → the AI returns a draft;
//   2. DraftForm: check and fix the draft → save it as a normal alert.
// Which step shows depends only on whether we have a draft yet.
export default function NewAlertPage() {
  const [draft, setDraft] = useState<AlertDraft | null>(null);

  return (
    <main className="page">
      <h1>New alert</h1>
      {draft === null
        ? <ExtractStep onDraft={setDraft} />
        : <DraftForm draft={draft} onChange={setDraft} onStartOver={() => setDraft(null)} />}
    </main>
  );
}
