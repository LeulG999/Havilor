import { Navigate, Route, Routes } from 'react-router';
import LoggedInLayout from './components/LoggedInLayout';
import LoginPage from './pages/LoginPage';
import QueuePage from './pages/QueuePage';

// Which page shows for which URL. Everything except /login sits inside LoggedInLayout (login check + top bar).
export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<LoggedInLayout />}>
        <Route path="/alerts" element={<QueuePage />} />
      </Route>
      <Route path="*" element={<Navigate to="/alerts" replace />} />
    </Routes>
  );
}
