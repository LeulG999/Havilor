import type { AlertStep, AuditEntry, User } from '../api/types';
import { timeline, type EventIcon } from '../historyEvents';

// An alert's history as a vertical timeline (docs/design/mockups/s3-detail.png): a round icon per kind of event, a
// title and a detail line in plain words, and the time in mono on the right. The entries come from the audit log
// (GET /api/v1/alerts/{id}/audit), which the backend writes itself on every change: nobody can edit or delete them.
// `limit`: only the newest n events (the Overview's side column); without it, the whole story (the History tab).
export default function HistoryTimeline({ entries, steps, me, received, limit }: {
  entries: AuditEntry[];
  steps: AlertStep[];
  me: User | null;
  received?: { at: string; source: string };
  limit?: number;
}) {
  const events = timeline(entries, steps, me, received);
  const shown = limit ? events.slice(-limit) : events;

  if (shown.length === 0) {
    return <p className="muted timeline-empty">No changes yet.</p>;
  }
  return (
    <ol className="tl">
      {shown.map((event) => (
        <li key={event.key}>
          <span className="tl-icon" aria-hidden="true"><Icon icon={event.icon} /></span>
          <div>
            <b>{event.title}</b>
            <small>{[event.detail, event.who].filter(Boolean).join(' · ')}</small>
          </div>
          <time className="mono" dateTime={event.at} title={new Date(event.at).toLocaleString()}>
            {timeLabel(event.at)}
          </time>
        </li>
      ))}
    </ol>
  );
}

// Today: "10:07"; another day: "Nov 10 10:07".
function timeLabel(iso: string): string {
  const date = new Date(iso);
  const time = date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
  return date.toDateString() === new Date().toDateString()
    ? time
    : `${date.toLocaleDateString([], { month: 'short', day: 'numeric' })} ${time}`;
}

// Small 14px icons, drawn in the accent colour (currentColor).
function Icon({ icon }: { icon: EventIcon }) {
  const stroke = { fill: 'none', stroke: 'currentColor', strokeWidth: 1.4, strokeLinecap: 'round' as const, strokeLinejoin: 'round' as const };
  return (
    <svg width="13" height="13" viewBox="0 0 14 14">
      {icon === 'bell' && <path d="M7 1.5a4 4 0 0 0-4 4v3l-1 1.5h10L11 8.5v-3a4 4 0 0 0-4-4zM5.5 12h3" {...stroke} />}
      {icon === 'check' && <path d="M3 7.5l2.5 2.5L11 4" {...stroke} strokeWidth={1.6} />}
      {icon === 'user' && <><circle cx="7" cy="4.8" r="2.3" {...stroke} /><path d="M2.8 12.2c.6-2.3 2.2-3.4 4.2-3.4s3.6 1.1 4.2 3.4" {...stroke} /></>}
      {icon === 'spark' && <path d="M7 1.5l1.3 4.2L12.5 7l-4.2 1.3L7 12.5 5.7 8.3 1.5 7l4.2-1.3z" fill="currentColor" />}
      {icon === 'flag' && <path d="M3 12.5V2m0 .5h7l-1.5 2.5L10 7.5H3" {...stroke} />}
      {icon === 'doc' && <><path d="M4 1.8h4l2.5 2.5v7.9H4z" {...stroke} /><path d="M5.8 7h3M5.8 9.3h3" {...stroke} /></>}
      {icon === 'bolt' && <path d="M8 1.5L3.5 8h3.2L6 12.5 10.5 6H7.3z" {...stroke} />}
      {icon === 'lock' && <><rect x="3" y="6.3" width="8" height="6" rx="1.3" {...stroke} /><path d="M5 6.3V4.6a2 2 0 0 1 4 0v1.7" {...stroke} /></>}
      {icon === 'dot' && <circle cx="7" cy="7" r="2" fill="currentColor" />}
    </svg>
  );
}
