import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { api, clearToken } from '../api/api';
import type { User } from '../api/types';
import ErrorMessage from './ErrorMessage';

// The bar at the top of every logged-in page: who is logged in (GET /api/v1/users/me) and a logout button.
export default function TopBar() {
  const navigate = useNavigate();
  const [user, setUser] = useState<User | null>(null);
  const [error, setError] = useState<unknown>(null);

  // Runs once, when the bar first appears.
  useEffect(() => {
    api<User>('/users/me').then(setUser).catch(setError);
  }, []);

  function logout() {
    clearToken(); // the token is only in this tab's sessionStorage, so forgetting it logs out
    navigate('/login');
  }

  return (
    <header className="topbar">
      <Link to="/alerts" className="brand">Havilor</Link>
      <span className="topbar-user">
        {user && <>{user.name} <span className="muted">({user.role})</span></>}
        <ErrorMessage error={error} />
      </span>
      <button type="button" className="secondary" onClick={logout}>Log out</button>
    </header>
  );
}
