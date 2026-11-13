import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { api } from '../api/api';
import type { AlertSummary, Page } from '../api/types';
import ErrorMessage from '../components/ErrorMessage';
import { formatDateTime } from '../format';

const PAGE_SIZE = 20;

// The alert queue: GET /api/v1/alerts, newest first (the backend sorts), one page at a time.
export default function QueuePage() {
  const navigate = useNavigate();
  const [pageNumber, setPageNumber] = useState(0);
  const [page, setPage] = useState<Page<AlertSummary> | null>(null);
  const [error, setError] = useState<unknown>(null);

  // Runs again every time pageNumber changes (that's what [pageNumber] means).
  useEffect(() => {
    setError(null);
    api<Page<AlertSummary>>(`/alerts?page=${pageNumber}&size=${PAGE_SIZE}`).then(setPage).catch(setError);
  }, [pageNumber]);

  return (
    <main className="page">
      <h1>Alert queue</h1>
      <ErrorMessage error={error} />
      {page && (
        <>
          <table>
            <thead>
              <tr>
                <th>Ticket</th>
                <th>Subject</th>
                <th>Priority</th>
                <th>Status</th>
                <th>Assignee</th>
                <th>Received</th>
              </tr>
            </thead>
            <tbody>
              {page.content.map((alert) => (
                // A click anywhere on the row opens the alert.
                <tr key={alert.id} className="clickable" onClick={() => navigate(`/alerts/${alert.id}`)}>
                  <td>{alert.ticketNumber}</td>
                  <td>{alert.subject}</td>
                  <td><span className={`badge priority-${alert.priority.toLowerCase()}`}>{alert.priority}</span></td>
                  <td>{alert.status}</td>
                  <td>{alert.assigneeName ?? <span className="muted">Unassigned</span>}</td>
                  <td>{formatDateTime(alert.receivedAt)}</td>
                </tr>
              ))}
              {page.content.length === 0 && (
                <tr><td colSpan={6} className="muted">No alerts.</td></tr>
              )}
            </tbody>
          </table>
          <div className="pager">
            <button type="button" className="secondary" disabled={page.page === 0}
                    onClick={() => setPageNumber(page.page - 1)}>Previous</button>
            <span className="muted">
              Page {page.totalPages === 0 ? 0 : page.page + 1} of {page.totalPages} ({page.totalElements} alerts)
            </span>
            <button type="button" className="secondary" disabled={page.page + 1 >= page.totalPages}
                    onClick={() => setPageNumber(page.page + 1)}>Next</button>
          </div>
        </>
      )}
    </main>
  );
}
