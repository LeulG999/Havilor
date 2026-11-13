import { useEffect, useState } from 'react';
import { Navigate, Outlet } from 'react-router';
import { api, getToken } from '../api/api';
import type { User } from '../api/types';
import SetPasswordPage from '../pages/SetPasswordPage';
import ErrorMessage from './ErrorMessage';
import Sidebar from './Sidebar';
import TopBar from './TopBar';

// Wraps every page that needs a login. No token → straight to the login page.
// Who is logged in (GET /api/v1/users/me) is loaded once here and given to the rail. With a temporary password
// (mustChangePassword), the person sees only the "Set your password" screen until it's done (Flow F30).
// Layout: the 64px icon rail on the left; on the right the top bar (breadcrumb) and under it the page. <Outlet /> is
// where React Router puts the page for the current URL.
export default function LoggedInLayout() {
  const [me, setMe] = useState<User | null>(null);
  const [error, setError] = useState<unknown>(null);
  const loggedIn = getToken() !== null;

  function loadMe() {
    api<User>('/users/me').then(setMe).catch(setError);
  }

  useEffect(() => {
    if (loggedIn) {
      loadMe();
    }
  }, [loggedIn]);

  if (!loggedIn) {
    return <Navigate to="/login" replace />;
  }
  if (me?.mustChangePassword) {
    return <SetPasswordPage me={me} onDone={loadMe} />;
  }
  return (
    <div className="app-layout">
      <Sidebar user={me} />
      <div className="app-main">
        <TopBar />
        <ErrorMessage error={error} />
        {/* The pages wait for /users/me, so a temporary-password user never sees one flash by. */}
        {me && <Outlet />}
      </div>
    </div>
  );
}
