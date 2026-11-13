import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { api } from '../api/api';
import type { TeamDashboard as TeamDashboardData } from '../api/types';
import { formatAge } from '../format';
import ErrorMessage from './ErrorMessage';
import StatCard from './StatCard';

// The manager's start page: GET /api/v1/alerts/team-dashboard (managers only). What needs attention (unassigned,
// critical, the oldest open alert), who has how much work, and how many standups are in today.
export default function TeamDashboard() {
  const [dashboard, setDashboard] = useState<TeamDashboardData | null>(null);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    api<TeamDashboardData>('/alerts/team-dashboard').then(setDashboard).catch(setError);
  }, []);

  return (
    <>
      <div className="page-title">
        <h1>Team dashboard</h1>
        <div className="buttons">
          <Link to="/alerts" className="button secondary">Alert queue</Link>
          <Link to="/team-standups" className="button secondary">Team standups</Link>
        </div>
      </div>
      <ErrorMessage error={error} />
      {dashboard && (
        <>
          <div className="stat-grid">
            <StatCard label="Unassigned open" value={dashboard.unassignedOpen} tone="danger" />
            <StatCard label="Critical open" value={dashboard.criticalOpen} tone="danger" />
            <StatCard label="Standups shared today" value={dashboard.standupsSharedToday} />
            <div className="panel stat-card">
              {dashboard.oldestOpen === null ? <div className="stat-value">—</div> : (
                <>
                  <div className="stat-value">{formatAge(dashboard.oldestOpen.ageHours)}</div>
                  <Link to={`/alerts/${dashboard.oldestOpen.alertId}`}>{dashboard.oldestOpen.ticketNumber}</Link>
                </>
              )}
              <div className="muted">Oldest open alert</div>
            </div>
          </div>
          <section className="panel">
            <h2>Analysts</h2>
            <table>
              <thead><tr><th>Analyst</th><th>Open now</th><th>Closed in the last 7 days</th></tr></thead>
              <tbody>
                {dashboard.analysts.map((a) => (
                  <tr key={a.analystId}>
                    <td>{a.name}</td>
                    <td>{a.openAlerts}</td>
                    <td>{a.closedLast7Days}</td>
                  </tr>
                ))}
                {dashboard.analysts.length === 0 && <tr><td colSpan={3} className="muted">No active analysts.</td></tr>}
              </tbody>
            </table>
          </section>
        </>
      )}
    </>
  );
}
