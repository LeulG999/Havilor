import { useNavigate } from 'react-router';
import type { AlertSummary } from '../api/types';
import { formatDateTime } from '../format';

// The 5 newest alerts on the dashboard; a click opens the alert.
export default function RecentAlerts({ alerts }: { alerts: AlertSummary[] }) {
  const navigate = useNavigate();
  if (alerts.length === 0) {
    return <p className="muted">No alerts yet.</p>;
  }
  return (
    <table>
      <tbody>
        {alerts.map((alert) => (
          <tr key={alert.id} className="clickable" onClick={() => navigate(`/alerts/${alert.id}`)}>
            <td><span className={`badge priority-${alert.priority.toLowerCase()}`}>{alert.priority}</span></td>
            <td>{alert.subject}</td>
            <td className="muted">{alert.status}</td>
            <td className="muted">{formatDateTime(alert.receivedAt)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
