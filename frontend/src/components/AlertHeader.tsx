import type { AlertDetail } from '../api/types';

// The header that stays at the top of the detail page while you scroll: what this alert is and where it stands.
export default function AlertHeader({ alert }: { alert: AlertDetail }) {
  return (
    <header className="alert-header">
      <div>
        <div className="muted">{alert.ticketNumber}</div>
        <h1>{alert.email.subject}</h1>
      </div>
      <dl className="header-facts">
        <div><dt>Status</dt><dd>{alert.status}</dd></div>
        <div>
          <dt>Priority</dt>
          <dd><span className={`badge priority-${alert.priority.toLowerCase()}`}>{alert.priority}</span></dd>
        </div>
        <div><dt>Assignee</dt><dd>{alert.assigneeId ? `User #${alert.assigneeId}` : 'Unassigned'}</dd></div>
      </dl>
    </header>
  );
}
