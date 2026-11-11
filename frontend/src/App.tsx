import { Navigate, Route, Routes } from 'react-router';
import LoggedInLayout from './components/LoggedInLayout';
import AlertDetailPage from './pages/AlertDetailPage';
import DashboardPage from './pages/DashboardPage';
import LoginPage from './pages/LoginPage';
import QueuePage from './pages/QueuePage';

// Which page shows for which URL. Everything except /login sits inside LoggedInLayout (login check + sidebar).
// "/alerts/new" and "/alerts/:id" don't clash: React Router prefers the exact text "new" over the ":id" placeholder.
export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<LoggedInLayout />}>
        {/* The new-alert page arrives in Flow F15. */}
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/alerts" element={<QueuePage />} />
        <Route path="/alerts/new" element={<main className="page"><h1>New alert</h1></main>} />
        <Route path="/alerts/:id" element={<AlertDetailPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}
