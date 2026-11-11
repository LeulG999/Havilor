import { Navigate, Route, Routes } from 'react-router';
import LoggedInLayout from './components/LoggedInLayout';
import AlertDetailPage from './pages/AlertDetailPage';
import DashboardPage from './pages/DashboardPage';
import LoginPage from './pages/LoginPage';
import NewAlertPage from './pages/NewAlertPage';
import QueuePage from './pages/QueuePage';

// Which page shows for which URL. Everything except /login sits inside LoggedInLayout (login check + sidebar).
// "/alerts/new" and "/alerts/:id" don't clash: React Router prefers the exact text "new" over the ":id" placeholder.
export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<LoggedInLayout />}>
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/alerts" element={<QueuePage />} />
        <Route path="/alerts/new" element={<NewAlertPage />} />
        <Route path="/alerts/:id" element={<AlertDetailPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}
