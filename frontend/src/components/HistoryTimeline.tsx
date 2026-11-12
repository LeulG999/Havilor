import type { AuditEntry, User } from '../api/types';
import { formatDateTime } from '../format';

// The History tab: the alert's audit trail (GET /api/v1/alerts/{id}/audit), oldest first, as a simple timeline.
// Each change shows when, who, what (the action) and old → new. The backend writes these entries itself on every
// change, so nobody can edit or delete them: that is what makes it an audit trail.
export default function HistoryTimeline({ entries, me }: { entries: AuditEntry[]; me: User | null }) {
  const who = (actorId: number | null) =>
    actorId === null ? 'System' : actorId === me?.id ? 'You' : `User #${actorId}`;

  return (
    <section className="panel">
      <h2>History</h2>
      {entries.length === 0 && <p className="muted">No changes yet.</p>}
      <ol className="timeline">
        {entries.map((entry) => (
          <li key={entry.id}>
            <div className="muted">{formatDateTime(entry.createdAt)} · {who(entry.actorId)}</div>
            <strong>{entry.action}</strong>
            {(entry.oldValue || entry.newValue) && (
              <div className="wrap">
                {entry.oldValue && <span className="muted">{entry.oldValue} → </span>}
                {entry.newValue ?? '—'}
              </div>
            )}
          </li>
        ))}
      </ol>
    </section>
  );
}
