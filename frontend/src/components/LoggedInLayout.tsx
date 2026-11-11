import { Navigate, Outlet } from 'react-router';
import { getToken } from '../api/api';
import TopBar from './TopBar';

// Wraps every page that needs a login. No token → straight to the login page.
// <Outlet /> is where React Router puts the page for the current URL (queue, detail).
export default function LoggedInLayout() {
  if (!getToken()) {
    return <Navigate to="/login" replace />;
  }
  return (
    <>
      <TopBar />
      <Outlet />
    </>
  );
}
