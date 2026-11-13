import { Navigate, Outlet } from 'react-router';
import { getToken } from '../api/api';
import Sidebar from './Sidebar';
import TopBar from './TopBar';

// Wraps every page that needs a login. No token → straight to the login page.
// Layout: the 64px icon rail on the left; on the right the top bar (breadcrumb) and under it the page. <Outlet /> is
// where React Router puts the page for the current URL (dashboard, queue, detail, new alert, …).
export default function LoggedInLayout() {
  if (!getToken()) {
    return <Navigate to="/login" replace />;
  }
  return (
    <div className="app-layout">
      <Sidebar />
      <div className="app-main">
        <TopBar />
        <Outlet />
      </div>
    </div>
  );
}
