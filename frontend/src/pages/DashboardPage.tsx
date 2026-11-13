import { useEffect, useState } from 'react';
import { api } from '../api/api';
import type { User } from '../api/types';
import AnalystDashboard from '../components/AnalystDashboard';
import ErrorMessage from '../components/ErrorMessage';
import TeamDashboard from '../components/TeamDashboard';

// /dashboard, the start page after login. Who is logged in (GET /api/v1/users/me) decides which view:
// a MANAGER gets the team view, an ANALYST her own. (The team endpoint itself is MANAGER-only in the backend.)
export default function DashboardPage() {
  const [me, setMe] = useState<User | null>(null);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    api<User>('/users/me').then(setMe).catch(setError);
  }, []);

  return (
    <main className="page">
      <ErrorMessage error={error} />
      {me && (me.role === 'MANAGER' ? <TeamDashboard /> : <AnalystDashboard />)}
    </main>
  );
}
