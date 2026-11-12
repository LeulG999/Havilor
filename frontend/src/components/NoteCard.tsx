import type { ReactNode } from 'react';
import type { StandupNote } from '../api/types';
import NoteContent from './NoteContent';

// One standup note: its day and status, then its content (NoteContent: sections + ticket links).
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
      <NoteContent text={note.text} alertIds={note.alertIds} />
      {children && <div className="buttons">{children}</div>}
    </li>
  );
}
