import { useState } from 'react';
import { api } from '../api/api';
import type { AlertDetail, User } from '../api/types';
import { statusLabel } from '../format';
import AssignToSelect from './AssignToSelect';
import ErrorMessage from './ErrorMessage';

// The three cards at the top of the slide-over panel: Status, Assigned to, Verdict (with the AI's suggestion).
// "Assigned to" can be changed right here: an analyst takes the alert ("Assign to me"), a manager picks an analyst
// (the backend checks the same rules, Flow F25). Status and verdict change through the investigation itself
// (assigning, escalating, closing), so they are shown, not edited.
export default function DrawerCards({ alert, me, onChanged }: {
  alert: AlertDetail;
  me: User | null;
  onChanged: () => void;
}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const open = alert.status !== 'CLOSED';
  const isManager = me?.role === 'MANAGER';

  async function assign(assigneeId: number) {
    setBusy(true);
    setError(null);
    try {
      await api(`/alerts/${alert.id}`, { method: 'PATCH', body: { assigneeId } });
      onChanged();
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <div className="cards3">
        <div className="pick">
          <span>Status</span>
          <b className={alert.status === 'INVESTIGATING' ? 'acc' : ''}>{statusLabel(alert.status)}</b>
        </div>
        <div className="pick">
          <span>Assigned to</span>
          <b>{alert.assigneeName ?? 'Unassigned'}{alert.assigneeId === me?.id && ' (you)'}</b>
          {open && isManager && (
            <AssignToSelect assigneeId={alert.assigneeId} disabled={busy} onAssign={assign} />
          )}
          {open && me && !isManager && alert.assigneeId !== me.id && (
            <button type="button" className="link" disabled={busy} onClick={() => assign(me.id)}>Assign to me</button>
          )}
        </div>
        <div className="pick">
          <span>Verdict</span>
          <b>{statusLabel(alert.verdict)}</b>
          {alert.suggestedVerdict && (
            <small>AI suggests <em className={`suggests-${alert.suggestedVerdict.toLowerCase()}`}>
              {statusLabel(alert.suggestedVerdict)}</em></small>
          )}
        </div>
      </div>
      <ErrorMessage error={error} />
    </>
  );
}
