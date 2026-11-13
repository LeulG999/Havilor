import type { AlertDetail, User } from '../api/types';
import HeaderActions from './HeaderActions';

// The header that stays at the top of the detail page while you scroll: what this alert is, where it stands,
// and the actions on the whole alert. A closed alert can't change, so it shows no actions.
export default function AlertHeader({ alert, me, onChanged }: {
  alert: AlertDetail;
  me: User | null;
  onChanged: () => void;
}) {
  const closed = alert.status === 'CLOSED';
  // The backend sends the assignee's name; "(you)" when it's the logged-in user.
  const assignee = alert.assigneeId === null ? 'Unassigned'
    : `${alert.assigneeName ?? `User #${alert.assigneeId}`}${alert.assigneeId === me?.id ? ' (you)' : ''}`;

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
        <div><dt>Assignee</dt><dd>{assignee}</dd></div>
      </dl>
      {!closed && <HeaderActions alert={alert} me={me} onChanged={onChanged} />}
    </header>
  );
}
