import { useNavigate } from 'react-router';
import type { AlertSummary } from '../api/types';
import { formatQueueTime } from '../format';
import { AssigneeCell, PriorityTag, StatusChip } from './QueueBits';

// The 5 newest alerts on the dashboard, in the queue's look; a click opens the alert.
export default function RecentAlerts({ alerts }: { alerts: AlertSummary[] }) {
  const navigate = useNavigate();
  if (alerts.length === 0) {
    return <p className="muted">No alerts yet.</p>;
  }
  return (
    <table className="queue-table">
      <tbody>
        {alerts.map((alert) => (
          <tr key={alert.id} className="clickable" onClick={() => navigate(`/alerts/${alert.id}`)}>
            <td><PriorityTag priority={alert.priority} /></td>
            <td className="mono">{alert.ticketNumber}</td>
            <td className="subject">{alert.subject}</td>
            <td><AssigneeCell name={alert.assigneeName} /></td>
            <td><StatusChip status={alert.status} /></td>
            <td className="mono muted right">{formatQueueTime(alert.receivedAt)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
