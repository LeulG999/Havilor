import { useEffect, useState } from 'react';
import { api } from '../api/api';
import type { Page, StandupNote } from '../api/types';
import ErrorMessage from '../components/ErrorMessage';
import NoteCard from '../components/NoteCard';
import NoteFormModal from '../components/NoteFormModal';
import ShareNoteButton from '../components/ShareNoteButton';
import Pager from '../components/Pager';

const PAGE_SIZE = 10;

// My standup notes: GET /api/v1/standup/notes, newest day first (the backend sorts), one page at a time.
// A DRAFT note is mine only and can still change; a SHARED note is visible to the managers and read-only.
export default function StandupNotesPage() {
  const [pageNumber, setPageNumber] = useState(0);
  const [page, setPage] = useState<Page<StandupNote> | null>(null);
  const [error, setError] = useState<unknown>(null);
  // The modal: closed, open for a new note (editing = null), or open for one draft (editing = that note).
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<StandupNote | null>(null);

  function load() {
    setError(null);
    api<Page<StandupNote>>(`/standup/notes?page=${pageNumber}&size=${PAGE_SIZE}`).then(setPage).catch(setError);
  }

  useEffect(load, [pageNumber]); // again whenever the page number changes

  function openModal(note: StandupNote | null) {
    setEditing(note);
    setModalOpen(true);
  }

  function handleSaved() {
    setModalOpen(false);
    load();
  }

  return (
    <main className="page">
      <div className="page-title">
        <h1>Standup notes</h1>
        <button type="button" onClick={() => openModal(null)}>+ New note</button>
      </div>
      <ErrorMessage error={error} />
      {page && (
        <section className="panel">
          {page.content.length === 0 && <p className="muted">No notes yet.</p>}
          <ul className="cards notes-list">
            {page.content.map((note) => (
              <NoteCard key={note.id} note={note}>
                {/* Only a DRAFT can change; a SHARED note shows no buttons (read-only). */}
                {note.status === 'DRAFT' && (
                  <>
                    <button type="button" className="secondary small" onClick={() => openModal(note)}>Edit</button>
                    <ShareNoteButton noteId={note.id} onShared={load} />
                  </>
                )}
              </NoteCard>
            ))}
          </ul>
          <Pager page={page} noun="notes" onPage={setPageNumber} />
        </section>
      )}
      <NoteFormModal open={modalOpen} note={editing} onClose={() => setModalOpen(false)} onSaved={handleSaved} />
    </main>
  );
}
