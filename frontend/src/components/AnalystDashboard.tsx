import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { api } from '../api/api';
import type { Dashboard } from '../api/types';
import BarChart from './BarChart';
import ErrorMessage from './ErrorMessage';
import RecentAlerts from './RecentAlerts';
import StatCard from './StatCard';

// The analyst's start page: GET /api/v1/alerts/dashboard, shown as stat cards, a chart and the newest alerts.
export default function AnalystDashboard() {
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    api<Dashboard>('/alerts/dashboard').then(setDashboard).catch(setError);
  }, []);

  return (
    <>
      <div className="page-title">
        <h1>Dashboard</h1>
        <div className="buttons">
          {/* Links that look like buttons: they go to another page, they don't send anything. */}
          <Link to="/alerts/new" className="button">+ New alert</Link>
          <Link to="/alerts" className="button secondary">My queue</Link>
        </div>
      </div>
      <ErrorMessage error={error} />
      {dashboard && (
        <>
          <div className="stat-grid">
            <StatCard label="Open alerts" value={dashboard.openAlerts} />
            <StatCard label="Assigned to me" value={dashboard.assignedToMe} />
            <StatCard label="Critical open" value={dashboard.criticalOpen} tone="danger" />
            <StatCard label="Closed last 7 days" value={dashboard.closedLast7Days} />
          </div>
          <section className="sec dash-section">
            <div className="sec-head"><b>Alerts per day</b><span className="muted">last 14 days, UTC</span></div>
            <div className="chart-box"><BarChart days={dashboard.alertsPerDay} /></div>
          </section>
          <section className="sec dash-section">
            <div className="sec-head">
              <b>Recent alerts</b>
              <Link to="/alerts" className="quiet-link">Open the queue ›</Link>
            </div>
            <RecentAlerts alerts={dashboard.recent} />
          </section>
        </>
      )}
    </>
  );
}
