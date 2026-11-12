import type { AlertSummary } from '../api/types';

// The 20 newest alerts with a checkbox each: tick to mention one, untick to drop it again.
// Opened from "Add an alert" in the note editor.
export default function AlertPicker({ alerts, chosen, onToggle }: {
  alerts: AlertSummary[];
  chosen: number[];
  onToggle: (alertId: number) => void;
}) {
  return (
    <ul className="check-list">
      {alerts.map((a) => (
        <li key={a.id}>
          <label className="inline">
            <input type="checkbox" checked={chosen.includes(a.id)} onChange={() => onToggle(a.id)} />
            {a.ticketNumber} · {a.subject}
          </label>
        </li>
      ))}
    </ul>
  );
}
