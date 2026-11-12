import { useState } from 'react';
import { api } from '../api/api';
import type { AlertDetail, User } from '../api/types';
import CloseAlertButton from './CloseAlertButton';
import ErrorMessage from './ErrorMessage';
import EscalateButton from './EscalateButton';

const PRIORITIES = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];

// The actions on the whole alert. "Assign to me" and the priority dropdown send PATCH /api/v1/alerts/{id} with only
// the field that changes; "Escalate" and "Close alert" have their own modals. After any change the page loads the alert again
// (onChanged), so every part of the page shows the new state.
export default function HeaderActions({ alert, me, onChanged }: {
  alert: AlertDetail;
  me: User | null;
  onChanged: () => void;
}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);

  async function update(change: { assigneeId?: number; priority?: string }) {
    setBusy(true);
    setError(null);
    try {
      await api(`/alerts/${alert.id}`, { method: 'PATCH', body: change });
      onChanged();
    } catch (e) {
      setError(e); // e.g. 409 "Alert 42 is closed and can't be changed"
    } finally {
      setBusy(false);
    }
  }

  const assignedToMe = me !== null && alert.assigneeId === me.id;

  return (
    <div className="header-actions">
      {me && !assignedToMe && (
        <button type="button" disabled={busy} onClick={() => update({ assigneeId: me.id })}>Assign to me</button>
      )}
      <label className="inline">
        Priority
        <select value={alert.priority} disabled={busy} onChange={(e) => update({ priority: e.target.value })}>
          {PRIORITIES.map((p) => <option key={p} value={p}>{p}</option>)}
        </select>
      </label>
      <EscalateButton alertId={alert.id} onChanged={onChanged} />
      <CloseAlertButton alertId={alert.id} onChanged={onChanged} />
      <ErrorMessage error={error} />
    </div>
  );
}
