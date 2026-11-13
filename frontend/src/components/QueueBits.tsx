import type { Signal } from '../api/types';
import { shortName, statusLabel } from '../format';
import Avatar from './Avatar';

// Small pieces of a queue row, each one look from the design (docs/design/mockups/s1-queue.png).

// Severity tag: mono uppercase, outlined and tinted (CRITICAL red, HIGH orange, MEDIUM yellow, LOW blue).
export function PriorityTag({ priority }: { priority: string }) {
  return <span className={`badge priority-${priority.toLowerCase()}`}>{priority}</span>;
}

// Status chip: a rounded pill (Investigating cyan, Escalated orange, New neutral, Closed muted).
export function StatusChip({ status }: { status: string }) {
  return <span className={`badge status-${status.toLowerCase()}`}>{statusLabel(status)}</span>;
}

// The Signals column: small mono tags; red when the signal points to a threat. "—" when there are none.
export function SignalTags({ signals }: { signals: Signal[] }) {
  if (signals.length === 0) {
    return <span className="faint">—</span>;
  }
  return (
    <span className="signals">
      {signals.map((s) => <span key={s.name} className={s.bad ? 'signal bad' : 'signal'}>{s.name}</span>)}
    </span>
  );
}

// The assignee: avatar + short name, or a faint "Unassigned".
export function AssigneeCell({ name }: { name: string | null }) {
  if (name === null) {
    return <span className="faint">Unassigned</span>;
  }
  return <span className="who"><Avatar name={name} small /> {shortName(name)}</span>;
}
