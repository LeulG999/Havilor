import type { ReactNode } from 'react';
import { Link } from 'react-router';
import type { StandupNote } from '../api/types';

// One standup note: its day, status, text and the alerts it mentions (each a link to the alert).
// `children` = buttons the page adds (Edit / Share on my own drafts); none on read-only notes.
export default function NoteCard({ note, author, children }: {
  note: StandupNote;
  author?: string;
  children?: ReactNode;
}) {
  return (
    <li>
      <div className="note-head">
        <strong>{note.noteDate}</strong>
        <span className={`badge note-${note.status.toLowerCase()}`}>{note.status}</span>
        {author && <span className="muted">{author}</span>}
      </div>
      <p className="note-text">{note.text}</p>
      {note.alertIds.length > 0 && (
        <p className="muted">
          Mentions:{' '}
          {note.alertIds.map((alertId) => (
            <Link key={alertId} to={`/alerts/${alertId}`} className="mention">Alert #{alertId}</Link>
          ))}
        </p>
      )}
      {children && <div className="buttons">{children}</div>}
    </li>
  );
}
