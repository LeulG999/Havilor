import type { ReactNode } from 'react';

// One number on a dashboard, like the summary cards: a small uppercase label, then the value. `tone` colours the
// value (red when a "danger" number is above 0). `children`: an extra line under the value (e.g. a ticket link).
export default function StatCard({ label, value, tone, children }: {
  label: string;
  value: number | string;
  tone?: 'danger';
  children?: ReactNode;
}) {
  const danger = tone === 'danger' && typeof value === 'number' && value > 0;
  return (
    <div className="panel stat-card">
      <span className="stat-label">{label}</span>
      <div className={danger ? 'stat-value danger-text' : 'stat-value'}>{value}</div>
      {children}
    </div>
  );
}
