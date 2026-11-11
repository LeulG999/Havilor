import { useEffect, useState } from 'react';
import { NavLink, useNavigate } from 'react-router';
import { api, clearToken } from '../api/api';
import type { User } from '../api/types';
import ErrorMessage from './ErrorMessage';

// The dark bar on the left of every logged-in page: the app name, the main links, and at the bottom who is
// logged in (GET /api/v1/users/me) with a logout button. The "icons" are plain text characters.
export default function Sidebar() {
  const navigate = useNavigate();
  const [user, setUser] = useState<User | null>(null);
  const [error, setError] = useState<unknown>(null);

  // Runs once, when the sidebar first appears.
  useEffect(() => {
    api<User>('/users/me').then(setUser).catch(setError);
  }, []);

  function logout() {
    clearToken(); // the token is only in this tab's sessionStorage, so forgetting it logs out
    navigate('/login');
  }

  // NavLink knows whether its link is the current page, and tells us through isActive.
  // `end` on "/alerts": only exactly /alerts counts, not /alerts/new or /alerts/42.
  const linkClass = ({ isActive }: { isActive: boolean }) => (isActive ? 'nav-link active' : 'nav-link');

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">Havilor</div>
      <nav className="sidebar-nav">
        <NavLink to="/dashboard" className={linkClass}><span className="icon">▦</span> Dashboard</NavLink>
        <NavLink to="/alerts" end className={linkClass}><span className="icon">☰</span> Alert queue</NavLink>
        <NavLink to="/alerts/new" className={linkClass}><span className="icon">+</span> New alert</NavLink>
      </nav>
      <div className="sidebar-user">
        {user && (
          <div>
            <div>{user.name}</div>
            <div className="sidebar-role">{user.role}</div>
          </div>
        )}
        <ErrorMessage error={error} />
        <button type="button" className="sidebar-logout" onClick={logout}>Log out</button>
      </div>
    </aside>
  );
}
