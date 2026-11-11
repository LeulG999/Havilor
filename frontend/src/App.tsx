import { Navigate, Route, Routes } from 'react-router';
import LoggedInLayout from './components/LoggedInLayout';
import LoginPage from './pages/LoginPage';

// Which page shows for which URL. Everything except /login sits inside LoggedInLayout (login check + top bar).
export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<LoggedInLayout />}>
        {/* The alert queue arrives in Flow F3. */}
        <Route path="/alerts" element={<main className="page"><h1>Alert queue</h1></main>} />
      </Route>
      <Route path="*" element={<Navigate to="/alerts" replace />} />
    </Routes>
  );
}
