import type { Action } from '../api/types';
import { formatDateTime } from '../format';

// The Actions tab: what was done about this alert, oldest first, by whom and when.
export default function ActionList({ actions }: { actions: Action[] }) {
  return (
    <section className="panel">
      <h2>Actions</h2>
      {actions.length === 0 ? <p className="muted">No actions yet.</p> : (
        <table>
          <thead><tr><th>Action</th><th>Result</th><th>Note</th><th>By</th><th>When</th></tr></thead>
          <tbody>
            {actions.map((a) => (
              <tr key={a.id}>
                <td>{a.type}</td>
                <td>{a.result}</td>
                <td>{a.note ?? '—'}</td>
                <td>User #{a.performedBy}</td>
                <td>{formatDateTime(a.performedAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
