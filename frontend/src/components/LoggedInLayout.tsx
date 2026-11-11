import { Navigate, Outlet } from 'react-router';
import { getToken } from '../api/api';
import Sidebar from './Sidebar';

// Wraps every page that needs a login. No token → straight to the login page.
// Layout: the sidebar on the left, the page on the right. <Outlet /> is where React Router puts the page for the
// current URL (dashboard, queue, detail, new alert).
export default function LoggedInLayout() {
  if (!getToken()) {
    return <Navigate to="/login" replace />;
  }
  return (
    <div className="app-layout">
      <Sidebar />
      <div className="app-main">
        <Outlet />
      </div>
    </div>
  );
}
