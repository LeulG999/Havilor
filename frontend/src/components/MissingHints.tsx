import type { AlertDraft } from '../api/types';

// Step 2 of "New alert", above the draft: what the AI could NOT find, worked out from the empty fields, and what
// would fill it. "Add more and extract again" goes back to step 1 with everything she gave still there.
export function missingHints(draft: AlertDraft): string[] {
  const hints: string[] = [];
  const { email } = draft;
  if (email.subject.trim() === '' || email.fromAddress.trim() === '') {
    hints.push('No subject or sender found: they are required. Type them below, or add the .eml file.');
  }
  if (!email.spfResult && !email.dkimResult && !email.dmarcResult) {
    hints.push('No SPF/DKIM/DMARC results: add the raw headers for a stronger assessment.');
  }
  if (draft.recipients.length === 0) {
    hints.push('No recipients found: add the .eml file or the raw headers (To / Cc).');
  }
  if (draft.urls.length === 0) {
    hints.push('No links found: if the email had links, add the .eml file or a screenshot that shows them.');
  }
  if (draft.attachments.length === 0) {
    hints.push('No attachments found: if it had any, the .eml file lists them.');
  }
  return hints;
}

export default function MissingHints({ draft, onAddMore }: { draft: AlertDraft; onAddMore: () => void }) {
  const hints = missingHints(draft);
  return (
    <section className={hints.length > 0 ? 'sec missing' : 'sec missing complete'}>
      <div className="sec-head">
        <b>{hints.length > 0 ? "What's missing" : 'Nothing obvious is missing'}</b>
        <button type="button" className="secondary small" onClick={onAddMore}>Add more and extract again</button>
      </div>
      {hints.length > 0 && <ul className="missing-list">{hints.map((hint) => <li key={hint}>{hint}</li>)}</ul>}
    </section>
  );
}
