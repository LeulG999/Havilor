import { Fragment, useEffect, useState } from 'react';
import { Link } from 'react-router';
import { api } from '../api/api';
import type { AlertSummary, Page, QueueCounts } from '../api/types';
import ErrorMessage from '../components/ErrorMessage';
import Pager from '../components/Pager';
import { AssigneeCell, PriorityTag, SignalTags, StatusChip } from '../components/QueueBits';
import QueueFilters, { type PriorityFilter } from '../components/QueueFilters';
import QueueRowDetails from '../components/QueueRowDetails';
import { formatQueueTime } from '../format';

const PAGE_SIZE = 20;
const COLUMNS = 9;

// The alert queue (docs/design/mockups/s1-queue.png): GET /api/v1/alerts with the chosen filters, newest first,
// one page at a time, plus GET /api/v1/alerts/counts for the numbers. The backend does the filtering, so the counts
// and pages are right however many alerts there are. Click a row to see its key facts under it.
export default function QueuePage() {
  const [priority, setPriority] = useState<PriorityFilter>(null);
  const [status, setStatus] = useState('OPEN');
  const [mine, setMine] = useState(false);
  const [pageNumber, setPageNumber] = useState(0);
  const [page, setPage] = useState<Page<AlertSummary> | null>(null);
  const [counts, setCounts] = useState<QueueCounts | null>(null);
  const [expanded, setExpanded] = useState<number | null>(null); // the open row's alert id
  const [error, setError] = useState<unknown>(null);

  // The query string for the current filters, e.g. "status=OPEN&priority=CRITICAL&assignedToMe=true".
  const filters = new URLSearchParams({ status, ...(priority ? { priority } : {}), ...(mine ? { assignedToMe: 'true' } : {}) });
  const filterKey = filters.toString();

  // Runs again whenever a filter or the page number changes.
  useEffect(() => {
    setError(null);
    api<Page<AlertSummary>>(`/alerts?page=${pageNumber}&size=${PAGE_SIZE}&${filterKey}`).then(setPage).catch(setError);
  }, [filterKey, pageNumber]);

  // The counts don't depend on the priority filter (the buttons show every priority's count).
  useEffect(() => {
    const countFilters = new URLSearchParams({ status, ...(mine ? { assignedToMe: 'true' } : {}) });
    api<QueueCounts>(`/alerts/counts?${countFilters}`).then(setCounts).catch(setError);
  }, [status, mine]);

  // A new filter starts at the first page with every row closed.
  function changeFilter(change: () => void) {
    change();
    setPageNumber(0);
    setExpanded(null);
  }

  return (
    <main className="page">
      <div className="page-title">
        <div>
          <h1>Alert queue</h1>
          {counts && (
            <div className="page-sub">
              <b>{counts.open}</b> open · <b className="crit-text">{counts.criticalOpen}</b> critical ·{' '}
              <b className="high-text">{counts.unassignedOpen}</b> unassigned
            </div>
          )}
        </div>
        <Link to="/alerts/new" className="button">+ New alert</Link>
      </div>

      <QueueFilters counts={counts} priority={priority} status={status} mine={mine}
                    onPriority={(p) => changeFilter(() => setPriority(p))}
                    onStatus={(s) => changeFilter(() => setStatus(s))}
                    onMine={(m) => changeFilter(() => setMine(m))} />
      <ErrorMessage error={error} />

      {page && (
        <div className="panel table-panel">
          <table className="queue-table">
            <thead>
              <tr>
                <th aria-label="Expand" />
                <th>Priority</th>
                <th>Ticket</th>
                <th>Subject</th>
                <th>Sender</th>
                <th>Signals</th>
                <th>Assignee</th>
                <th>Status</th>
                <th className="right">Received</th>
              </tr>
            </thead>
            <tbody>
              {page.content.map((alert) => {
                const open = expanded === alert.id;
                return (
                  // Fragment: one alert = its row, plus the details row when it is open.
                  <Fragment key={alert.id}>
                    <tr className={open ? 'clickable open' : 'clickable'}
                        onClick={() => setExpanded(open ? null : alert.id)}>
                      <td className="chev">
                        {/* A real button, so the row also opens from the keyboard; stopPropagation: the row's own
                            click would toggle it straight back. */}
                        <button type="button" className="chev-button" aria-expanded={open}
                                aria-label={open ? 'Hide details' : 'Show details'}
                                onClick={(e) => { e.stopPropagation(); setExpanded(open ? null : alert.id); }}>
                          {open ? '⌄' : '›'}
                        </button>
                      </td>
                      <td><PriorityTag priority={alert.priority} /></td>
                      <td className="mono">{alert.ticketNumber}</td>
                      <td className="subject">{alert.subject}</td>
                      <td className="mono muted">{alert.fromAddress}</td>
                      <td><SignalTags signals={alert.signals} /></td>
                      <td><AssigneeCell name={alert.assigneeName} /></td>
                      <td><StatusChip status={alert.status} /></td>
                      <td className="mono muted right">{formatQueueTime(alert.receivedAt)}</td>
                    </tr>
                    {open && <QueueRowDetails alertId={alert.id} columns={COLUMNS} />}
                  </Fragment>
                );
              })}
              {page.content.length === 0 && (
                <tr><td colSpan={COLUMNS} className="muted empty">No alerts match these filters.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}
      {page && page.totalPages > 1 && <Pager page={page} noun="alerts" onPage={setPageNumber} />}
    </main>
  );
}
