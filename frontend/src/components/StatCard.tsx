// One number on the dashboard, with what it means. `tone` colours the number (e.g. red for critical).
export default function StatCard({ label, value, tone }: { label: string; value: number; tone?: 'danger' }) {
  return (
    <div className="panel stat-card">
      <div className={tone === 'danger' && value > 0 ? 'stat-value danger-text' : 'stat-value'}>{value}</div>
      <div className="muted">{label}</div>
    </div>
  );
}
