import { useEffect, useState } from 'react';
import { api } from '../api/api';
import type { User } from '../api/types';
import AddPersonButton from '../components/AddPersonButton';
import Avatar from '../components/Avatar';
import ErrorMessage from '../components/ErrorMessage';

// "Team" (managers only; the backend refuses everyone else with 403): everybody with an account (GET /api/v1/users),
// with their role and whether the account is on. A manager adds people, turns accounts off and on, and changes
// roles (PATCH /api/v1/users/{id}); every change is in the audit log. Their own account can't be turned off.
export default function TeamPage() {
  const [people, setPeople] = useState<User[] | null>(null);
  const [me, setMe] = useState<User | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [error, setError] = useState<unknown>(null);

  function load() {
    api<User[]>('/users').then(setPeople).catch(setError);
  }

  useEffect(() => {
    load();
    api<User>('/users/me').then(setMe).catch(setError);
  }, []);

  async function update(person: User, change: { active?: boolean; role?: string }) {
    setBusyId(person.id);
    setError(null);
    try {
      await api(`/users/${person.id}`, { method: 'PATCH', body: change });
      load();
    } catch (e) {
      setError(e); // e.g. 409 "You can't deactivate your own account"
    } finally {
      setBusyId(null);
    }
  }

  return (
    <main className="page">
      <div className="page-title">
        <div>
          <h1>Team</h1>
          {people && (
            <div className="page-sub">
              <b>{people.filter((p) => p.active).length}</b> active · {people.filter((p) => !p.active).length} turned off
            </div>
          )}
        </div>
        <AddPersonButton onAdded={load} />
      </div>
      <ErrorMessage error={error} />
      {people && (
        <div className="panel table-panel">
          <table className="queue-table">
            <thead>
              <tr><th>Name</th><th>Email</th><th>Role</th><th>Account</th><th className="right">Change</th></tr>
            </thead>
            <tbody>
              {people.map((person) => {
                const isMe = person.id === me?.id;
                const busy = busyId === person.id;
                return (
                  <tr key={person.id} className={person.active ? '' : 'inactive'}>
                    <td className="subject">
                      <span className="who"><Avatar name={person.name} small /> {person.name}{isMe && ' (you)'}</span>
                    </td>
                    <td className="mono muted">{person.email}</td>
                    <td>
                      <select value={person.role} disabled={busy} aria-label={`Role of ${person.name}`}
                              onChange={(e) => update(person, { role: e.target.value })}>
                        <option value="ANALYST">Analyst</option>
                        <option value="MANAGER">Manager</option>
                      </select>
                    </td>
                    <td>
                      {!person.active ? <span className="badge status-closed">Turned off</span>
                        : person.mustChangePassword ? <span className="badge status-waiting_on_external">Must set password</span>
                          : <span className="badge step-done">Active</span>}
                    </td>
                    <td className="right">
                      {!isMe && (
                        <button type="button" className={person.active ? 'danger small' : 'secondary small'} disabled={busy}
                                onClick={() => update(person, { active: !person.active })}>
                          {person.active ? 'Deactivate' : 'Reactivate'}
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </main>
  );
}
