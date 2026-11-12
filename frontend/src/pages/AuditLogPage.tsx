import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { api } from '../api/api';
import type { AuditEntry, Page } from '../api/types';
import ErrorMessage from '../components/ErrorMessage';
import Pager from '../components/Pager';
import { formatDateTime } from '../format';

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
        <section className="panel">
          <table>
            <thead>
              <tr><th>When</th><th>Who</th><th>Action</th><th>On</th><th>Old → new</th></tr>
            </thead>
            <tbody>
              {page.content.map((entry) => (
                <tr key={entry.id}>
                  <td>{formatDateTime(entry.createdAt)}</td>
                  <td>{entry.actorId === null ? 'System' : `User #${entry.actorId}`}</td>
                  <td>{entry.action}</td>
                  <td>
                    {/* Every entry so far is about an alert: link to it. */}
                    {entry.entityType === 'ALERT'
                      ? <Link to={`/alerts/${entry.entityId}`}>Alert #{entry.entityId}</Link>
                      : `${entry.entityType} #${entry.entityId}`}
                  </td>
                  <td className="wrap">
                    {entry.oldValue && <span className="muted">{entry.oldValue} → </span>}
                    {entry.newValue ?? '—'}
                  </td>
                </tr>
              ))}
              {page.content.length === 0 && <tr><td colSpan={5} className="muted">No entries yet.</td></tr>}
            </tbody>
          </table>
          <Pager page={page} noun="entries" onPage={setPageNumber} />
        </section>
      )}
    </main>
  );
}
