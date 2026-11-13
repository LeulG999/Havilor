import type { QueueCounts } from '../api/types';

export const PRIORITIES = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'] as const;
export type PriorityFilter = (typeof PRIORITIES)[number] | null; // null = all
export const STATUSES = [
  { value: 'OPEN', label: 'Open' },             // everything not closed (the backend's "OPEN")
  { value: 'NEW', label: 'New' },
  { value: 'INVESTIGATING', label: 'Investigating' },
  { value: 'ESCALATED', label: 'Escalated' },
  { value: 'CLOSED', label: 'Closed' },
];

// The row of filters above the queue: a segmented priority filter with counts, a segmented status filter and the
// "Assigned to me" switch. It only shows the current choice and reports clicks; the page loads the data.
export default function QueueFilters({ counts, priority, status, mine, onPriority, onStatus, onMine }: {
  counts: QueueCounts | null;
  priority: PriorityFilter;
  status: string;
  mine: boolean;
  onPriority: (priority: PriorityFilter) => void;
  onStatus: (status: string) => void;
  onMine: (mine: boolean) => void;
}) {
  const count = (p: (typeof PRIORITIES)[number]) => counts?.byPriority[p] ?? 0;
  const all = PRIORITIES.reduce((sum, p) => sum + count(p), 0);

  return (
    <div className="queue-filters">
      <div className="seg" role="group" aria-label="Priority">
        <button type="button" className={priority === null ? 'on' : ''} aria-pressed={priority === null}
                onClick={() => onPriority(null)}>All <em>{all}</em></button>
        {PRIORITIES.map((p) => (
          <button key={p} type="button" className={priority === p ? 'on' : ''} aria-pressed={priority === p}
                  onClick={() => onPriority(p)}>
            <i className={`dot dot-${p.toLowerCase()}`} />
            {p.charAt(0) + p.slice(1).toLowerCase()} <em>{count(p)}</em>
          </button>
        ))}
      </div>

      <div className="seg" role="group" aria-label="Status">
        {STATUSES.map((s) => (
          <button key={s.value} type="button" className={status === s.value ? 'on' : ''} aria-pressed={status === s.value}
                  onClick={() => onStatus(s.value)}>{s.label}</button>
        ))}
      </div>

      {/* A switch is a checkbox that looks like a toggle; role="switch" tells screen readers what it is. */}
      <label className="switch">
        Assigned to me
        <input type="checkbox" role="switch" checked={mine} onChange={(e) => onMine(e.target.checked)} />
        <span className="switch-track" aria-hidden="true" />
      </label>
    </div>
  );
}
