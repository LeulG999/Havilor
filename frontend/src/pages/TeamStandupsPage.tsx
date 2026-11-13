import { useEffect, useState } from 'react';
import { api } from '../api/api';
import type { Page, StandupNote } from '../api/types';
import ErrorMessage from '../components/ErrorMessage';
import NoteCard from '../components/NoteCard';
import Pager from '../components/Pager';

const PAGE_SIZE = 10;

// For managers: the team's SHARED standup notes (GET /api/v1/standup/team), newest day first, read-only.
// Drafts never show here. The backend allows this only for role MANAGER: anyone else who opens /team-standups
// gets its 403 message on the page.
export default function TeamStandupsPage() {
  const [pageNumber, setPageNumber] = useState(0);
  const [page, setPage] = useState<Page<StandupNote> | null>(null);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    setError(null);
    api<Page<StandupNote>>(`/standup/team?page=${pageNumber}&size=${PAGE_SIZE}`).then(setPage).catch(setError);
  }, [pageNumber]);

  return (
    <main className="page">
      <h1>Team standups</h1>
      <ErrorMessage error={error} />
      {page && (
        <section className="panel">
          {page.content.length === 0 && <p className="muted">No shared notes yet.</p>}
          <ul className="cards notes-list">
            {page.content.map((note) => <NoteCard key={note.id} note={note} author={`User #${note.authorId}`} />)}
          </ul>
          <Pager page={page} noun="notes" onPage={setPageNumber} />
        </section>
      )}
    </main>
  );
}
