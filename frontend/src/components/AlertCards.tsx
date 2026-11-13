import { useState } from 'react';
import { api } from '../api/api';
import type { AlertDetail, User } from '../api/types';
import { statusLabel } from '../format';
import AssignToSelect from './AssignToSelect';
import ErrorMessage from './ErrorMessage';

const PRIORITIES = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];

// The summary cards under an alert's title (slide-over panel and detail page): Status, Assigned to, Priority (detail
// page only) and Verdict with the AI's suggestion. What can change does so right here, with PATCH /api/v1/alerts/{id}:
//   - Assigned to: an analyst takes the alert ("Assign to me"), a manager picks an analyst (backend rules, Flow F25);
//   - Priority: both roles.
// Status and verdict change through the investigation itself (assigning, escalating, closing), so they are shown.
// canChange comes from alertRules (open alert): on a CLOSED alert every card is read-only.
export default function AlertCards({ alert, me, canChange, withPriority = false, onChanged }: {
  alert: AlertDetail;
  me: User | null;
  canChange: boolean;
  withPriority?: boolean;
  onChanged: () => void;
}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const open = canChange;
  const isManager = me?.role === 'MANAGER';

  async function update(change: { assigneeId?: number; priority?: string }) {
    setBusy(true);
    setError(null);
    try {
      await api(`/alerts/${alert.id}`, { method: 'PATCH', body: change });
      onChanged();
    } catch (e) {
      setError(e); // e.g. 409 when the alert was closed in the meantime
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <div className={withPriority ? 'cards3 cards4' : 'cards3'}>
        <div className="pick">
          <span>Status</span>
          <b className={alert.status === 'INVESTIGATING' ? 'acc' : ''}>{statusLabel(alert.status)}</b>
        </div>
        <div className="pick">
          <span>Assigned to</span>
          <b>{alert.assigneeName ?? 'Unassigned'}{alert.assigneeId === me?.id && ' (you)'}</b>
          {open && isManager && (
            <AssignToSelect assigneeId={alert.assigneeId} disabled={busy} onAssign={(id) => update({ assigneeId: id })} />
          )}
          {open && me && !isManager && alert.assigneeId !== me.id && (
            <button type="button" className="link" disabled={busy} onClick={() => update({ assigneeId: me.id })}>
              Assign to me
            </button>
          )}
        </div>
        {withPriority && (
          <div className="pick">
            <span>Priority</span>
            {open ? (
              // A select styled as the card's value: the colour follows the priority.
              <select className={`pick-select priority-text-${alert.priority.toLowerCase()}`} value={alert.priority}
                      disabled={busy} aria-label="Priority" onChange={(e) => update({ priority: e.target.value })}>
                {PRIORITIES.map((p) => <option key={p} value={p}>{statusLabel(p)}</option>)}
              </select>
            ) : <b className={`priority-text-${alert.priority.toLowerCase()}`}>{statusLabel(alert.priority)}</b>}
          </div>
        )}
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
