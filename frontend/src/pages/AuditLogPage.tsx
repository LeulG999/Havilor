import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { api } from '../api/api';
import type { AuditEntry, Page } from '../api/types';
import ErrorMessage from '../components/ErrorMessage';
import Pager from '../components/Pager';
import { formatDateTime } from '../format';
import { describe } from '../historyEvents';

const PAGE_SIZE = 20;

// For managers: every change in Havilor (GET /api/v1/audit), newest first, one page at a time.
// The backend allows this only for role MANAGER; anyone else sees its 403 message here.
export default function AuditLogPage() {
  const [pageNumber, setPageNumber] = useState(0);
  const [page, setPage] = useState<Page<AuditEntry> | null>(null);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    setError(null);
    api<Page<AuditEntry>>(`/audit?page=${pageNumber}&size=${PAGE_SIZE}`).then(setPage).catch(setError);
  }, [pageNumber]);

  return (
    <main className="page">
      <h1>Audit log</h1>
      <ErrorMessage error={error} />
      {page && (
        <>
          <div className="panel table-panel">
            <table className="queue-table">
              <thead>
                <tr><th>When</th><th>Who</th><th>What</th><th>On</th></tr>
              </thead>
              <tbody>
                {page.content.map((entry) => {
                  // The same plain-words description as an alert's History (historyEvents.ts); the exact audit
                  // texts stay available as a tooltip.
                  const event = describe(entry, [], null);
                  return (
                    <tr key={entry.id}>
                      <td className="mono muted">{formatDateTime(entry.createdAt)}</td>
                      <td>{entry.actorId === null ? 'System' : `User #${entry.actorId}`}</td>
                      <td className="subject audit-event"
                          title={[entry.oldValue, entry.newValue].filter(Boolean).join(' → ')}>
                        <b>{event.title}</b>
                        {event.detail && <small>{event.detail}</small>}
                      </td>
                      <td className="mono">
                        {/* Every entry so far is about an alert: link to it. */}
                        {entry.entityType === 'ALERT'
                          ? <Link to={`/alerts/${entry.entityId}`}>Alert #{entry.entityId}</Link>
                          : `${entry.entityType} #${entry.entityId}`}
                      </td>
                    </tr>
                  );
                })}
                {page.content.length === 0 && <tr><td colSpan={4} className="muted empty">No entries yet.</td></tr>}
              </tbody>
            </table>
          </div>
          {page.totalPages > 1 && <Pager page={page} noun="entries" onPage={setPageNumber} />}
        </>
      )}
    </main>
  );
}
