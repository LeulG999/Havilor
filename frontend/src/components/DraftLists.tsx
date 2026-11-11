import type { AlertDraft } from '../api/types';

// The recipients, URLs and attachments the AI found, read only, each with a Remove button
// (to drop something it got wrong). Removing gives the page a new draft without that item.
export default function DraftLists({ draft, onChange }: { draft: AlertDraft; onChange: (draft: AlertDraft) => void }) {
  // A copy of the list without item number `index` (React state is never changed in place).
  function without<T>(list: T[], index: number): T[] {
    return list.filter((_, i) => i !== index);
  }

  return (
    <>
      <h3>Recipients</h3>
      {draft.recipients.length === 0 && <p className="muted">None. An alert needs at least one recipient.</p>}
      <ul className="draft-list">
        {draft.recipients.map((r, i) => (
          <li key={i}>
            {r.address}
            <button type="button" className="secondary small"
                    onClick={() => onChange({ ...draft, recipients: without(draft.recipients, i) })}>Remove</button>
          </li>
        ))}
      </ul>

      <h3>URLs</h3>
      {draft.urls.length === 0 && <p className="muted">None.</p>}
      <ul className="draft-list">
        {draft.urls.map((u, i) => (
          <li key={i}>
            <span className="wrap">{u.address}</span>
            <button type="button" className="secondary small"
                    onClick={() => onChange({ ...draft, urls: without(draft.urls, i) })}>Remove</button>
          </li>
        ))}
      </ul>

      <h3>Attachments</h3>
      {draft.attachments.length === 0 && <p className="muted">None.</p>}
      <ul className="draft-list">
        {draft.attachments.map((a, i) => (
          <li key={i}>
            {a.fileName}
            <button type="button" className="secondary small"
                    onClick={() => onChange({ ...draft, attachments: without(draft.attachments, i) })}>Remove</button>
          </li>
        ))}
      </ul>
    </>
  );
}
